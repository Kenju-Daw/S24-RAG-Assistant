package com.kenjudaw.projectclaw

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * InferenceService — Android Foreground Service
 *
 * Keeps the LiteRT-LM inference engine and Ktor HTTP server alive
 * across app switches and screen-off. Uses PARTIAL_WAKE_LOCK to
 * prevent CPU from sleeping during active inference.
 *
 * Exposes: POST http://127.0.0.1:8080/v1/chat/completions
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
        startForeground(NOTIFICATION_ID, buildNotification())

        serviceScope.launch {
            // TODO Phase 3: Initialize LiteRT-LM engine before starting server
            // val modelPath = getExternalFilesDir(null)?.absolutePath + "/gemma-4-e2b.litertlm"
            // LlmEngine.init(applicationContext, modelPath)

            KtorServer.start()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        KtorServer.stop()
        // TODO Phase 3: LlmEngine.close()
        wakeLock?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ProjectClaw:InferenceLock"
        ).apply { acquire(/* timeout= */ 10 * 60 * 1000L) } // 10 min safety timeout
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW // Silent — no sound/vibration
        ).apply {
            description = "Project Claw inference engine status"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Project Claw")
            .setContentText("Inference engine running · 127.0.0.1:8080")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
}
