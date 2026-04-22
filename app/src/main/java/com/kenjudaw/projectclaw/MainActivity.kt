package com.kenjudaw.projectclaw

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * MainActivity — Minimal launcher
 *
 * Starts InferenceService on launch. The real action happens in
 * the service — this activity is just the entry point.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the inference engine as a foreground service
        startForegroundService(Intent(this, InferenceService::class.java))

        // Minimal status UI
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }

        layout.addView(TextView(this).apply {
            text = "🦾 Project Claw"
            textSize = 24f
        })

        layout.addView(TextView(this).apply {
            text = "Inference engine running\n127.0.0.1:8080"
            textSize = 16f
            setPadding(0, 24, 0, 0)
        })

        layout.addView(TextView(this).apply {
            text = "Model: Gemma 4 E2B · GPU (ML Drift)"
            textSize = 14f
            setPadding(0, 8, 0, 0)
        })

        setContentView(layout)
    }
}
