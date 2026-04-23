package com.kenjudaw.projectclaw

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * MainActivity — Minimal launcher + Simple Chat UI
 */
class MainActivity : AppCompatActivity() {

    private lateinit var chatHistory: TextView
    private lateinit var inputField: EditText
    private lateinit var sendButton: Button
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request MANAGE_EXTERNAL_STORAGE on Android R+ (30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }

        // Start the inference engine as a foreground service
        startForegroundService(Intent(this, InferenceService::class.java))

        setupUI()
    }

    private fun setupUI() {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Header
        rootLayout.addView(TextView(this).apply {
            text = "🦾 Project Claw AI"
            textSize = 22f
            setPadding(0, 0, 0, 16)
            gravity = Gravity.CENTER_HORIZONTAL
        })

        // Chat History Area
        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        chatHistory = TextView(this).apply {
            text = "Welcome! Type a prompt below to start.\n\n"
            textSize = 16f
        }
        scrollView.addView(chatHistory)
        rootLayout.addView(scrollView)

        // Input Area
        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        inputField = EditText(this).apply {
            hint = "Ask me anything..."
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        inputLayout.addView(inputField)

        sendButton = Button(this).apply {
            text = "Send"
            setOnClickListener { sendMessage() }
        }
        inputLayout.addView(sendButton)

        rootLayout.addView(inputLayout)
        setContentView(rootLayout)
    }

    private fun sendMessage() {
        val prompt = inputField.text.toString().trim()
        if (prompt.isEmpty()) return

        if (!LlmEngine.isInitialized) {
            appendChat("System: Engine is still initializing... please wait.")
            return
        }

        appendChat("\nYou: $prompt")
        appendChat("AI: ")
        inputField.text.clear()
        sendButton.isEnabled = false

        LlmEngine.generate(
            formattedPrompt = prompt,
            onToken = { token ->
                runOnUiThread {
                    chatHistory.append(token)
                    scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
                }
            },
            onDone = {
                runOnUiThread {
                    chatHistory.append("\n")
                    sendButton.isEnabled = true
                }
            },
            onError = { t ->
                runOnUiThread {
                    chatHistory.append("\n[Error: ${t.message}]\n")
                    sendButton.isEnabled = true
                }
            }
        )
    }

    private fun appendChat(text: String) {
        chatHistory.append("$text\n")
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }
}
