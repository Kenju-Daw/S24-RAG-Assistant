package com.kenjudaw.projectclaw

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.res.AssetManager
import android.os.IBinder
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * InferenceService — Android Foreground Service
 *
 * Lifecycle:
 *   onCreate  → acquires PARTIAL_WAKE_LOCK
 *   onStart   → shows notification, verifies model file, initializes LlmEngine, starts Ktor
 *   onDestroy → stops Ktor, closes LlmEngine, releases wake lock
 */
class InferenceService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "inference_channel"
        private const val CHANNEL_NAME = "Inference Engine"
    }

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Guard: check model file exists before doing anything
        if (!LlmEngine.modelFileExists()) {
            startForeground(NOTIFICATION_ID, buildNotification(
                title = "Project Claw — Model Missing",
                text = "Place gemma-4-e2b.litertlm in /sdcard/Download/"
            ))
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification(
            title = "Project Claw",
            text = "Loading Gemma 4 E2B (GPU)..."
        ))

        serviceScope.launch {
            try {
                // Phase 3: Initialize engine on IO thread (blocks for 5-15s on first run)
                LlmEngine.init()

                // Update notification to show ready state
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIFICATION_ID, buildNotification(
                    title = "Project Claw",
                    text = "Ready · 127.0.0.1:8080"
                ))

                // Start Ktor HTTP server (blocks until stopped)
                KtorServer.start(assets)
            } catch (e: Exception) {
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIFICATION_ID, buildNotification(
                    title = "Project Claw Error",
                    text = "Engine failed: ${e.message}"
                ))
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        KtorServer.stop()
        LlmEngine.close()
        wakeLock?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ProjectClaw:InferenceLock"
        ).apply { acquire(10 * 60 * 1000L) } // 10 min safety timeout
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Project Claw inference engine status" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(title: String, text: String): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
}
