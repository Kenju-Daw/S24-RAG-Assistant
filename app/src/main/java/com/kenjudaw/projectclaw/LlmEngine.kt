package com.kenjudaw.projectclaw

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.ResponseCallback
import com.google.ai.edge.litertlm.Session
import com.google.ai.edge.litertlm.SessionConfig
import java.io.File

/**
 * LlmEngine — LiteRT-LM Inference Wrapper
 *
 * Verified against LiteRT-LM source:
 *   kotlin/java/com/google/ai/edge/litertlm/Engine.kt
 *   kotlin/java/com/google/ai/edge/litertlm/Session.kt
 *   kotlin/java/com/google/ai/edge/litertlm/Config.kt
 *
 * API package: com.google.ai.edge.litertlm (NOT litert.lm)
 * Key classes: Engine, Session, EngineConfig, Backend, InputData, ResponseCallback
 */
object LlmEngine {

    /** Default model path — place .litertlm file here before starting the service */
    const val DEFAULT_MODEL_PATH = "/sdcard/Download/gemma-4-e2b.litertlm"

    private var engine: Engine? = null
    private var session: Session? = null

    @Volatile
    var isInitialized: Boolean = false
        private set

    /**
     * Initialize the engine. MUST be called on a background thread — can take 5-15 seconds
     * on first run while the GPU shader cache is built.
     *
     * @param modelPath Absolute path to the .litertlm file on device storage
     */
    fun init(modelPath: String = DEFAULT_MODEL_PATH) {
        if (isInitialized) return

        val config = EngineConfig(
            modelPath = modelPath,
            backend = Backend.GPU(),                // ML Drift delegate — Adreno 750
            // Phase 5: add visionBackend = Backend.GPU() for multimodal
            maxNumTokens = 4096,                    // conservative; model supports 128K
            cacheDir = File(modelPath).parent       // GPU shader cache alongside model
        )

        engine = Engine(config).also { it.initialize() }
        session = engine!!.createSession(SessionConfig())
        isInitialized = true
    }

    /**
     * Generate a streaming response. Each token is delivered via [onToken] as it arrives.
     * Call ONLY after [init] completes.
     *
     * @param userMessage Raw user message (Gemma 4 control tokens applied internally)
     * @param onToken Called per-token during streaming
     * @param onDone Called when generation is complete
     * @param onError Called if inference fails
     */
    fun generate(
        userMessage: String,
        onToken: (String) -> Unit,
        onDone: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        check(isInitialized) { "LlmEngine not initialized. Call init() first." }

        // Gemma 4 control token format (verified from AI Edge Gallery source)
        val formatted = "<start_of_turn>user\n$userMessage<end_of_turn>\n<start_of_turn>model\n"

        session!!.generateContentStream(
            inputData = listOf(InputData.Text(formatted)),
            responseCallback = object : ResponseCallback {
                override fun onNext(response: String) = onToken(response)
                override fun onDone() = onDone()
                override fun onError(throwable: Throwable) = onError(throwable)
            }
        )
    }

    /**
     * Generate a full response without streaming (blocking).
     * Useful for non-SSE requests.
     */
    fun generateBlocking(userMessage: String): String {
        check(isInitialized) { "LlmEngine not initialized. Call init() first." }
        val formatted = "<start_of_turn>user\n$userMessage<end_of_turn>\n<start_of_turn>model\n"
        return session!!.generateContent(listOf(InputData.Text(formatted)))
    }

    /** Release all native resources. Call from InferenceService.onDestroy() */
    fun close() {
        session?.close()
        engine?.close()
        session = null
        engine = null
        isInitialized = false
    }

    /** Check if the model file exists at the expected path */
    fun modelFileExists(path: String = DEFAULT_MODEL_PATH): Boolean =
        File(path).exists()
}
