package com.kenjudaw.projectclaw

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

// ── Request / Response data classes ───────────────────────────────────────────

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val max_tokens: Int = 512,
    val temperature: Float = 0.7f
)

@Serializable
data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finish_reason: String
)

@Serializable
data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val `object`: String = "chat.completion",
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage
)

// SSE delta wrapper for streaming responses
@Serializable
data class DeltaContent(val content: String)

@Serializable
data class StreamChoice(val index: Int, val delta: DeltaContent, val finish_reason: String?)

@Serializable
data class ChatCompletionChunk(
    val id: String,
    val `object`: String = "chat.completion.chunk",
    val model: String,
    val choices: List<StreamChoice>
)

// ── Ktor Server ────────────────────────────────────────────────────────────────

/**
 * KtorServer — OpenAI-compatible HTTP server
 *
 * Binds to 127.0.0.1:8080 (loopback only — not exposed to network).
 * Routes:
 *   GET  /health                    → 200 OK
 *   POST /v1/chat/completions       → OpenAI-compatible inference endpoint
 */
object KtorServer {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val server: ApplicationEngine = embeddedServer(
        factory = Netty,
        host = "127.0.0.1",
        port = 8080
    ) {
        install(ContentNegotiation) {
            json(json)
        }

        routing {
            get("/health") {
                call.respondText("Project Claw OK", status = HttpStatusCode.OK)
            }

            post("/v1/chat/completions") {
                val request = call.receive<ChatCompletionRequest>()
                val requestId = "chatcmpl-${UUID.randomUUID()}"

                // Build prompt from messages
                // TODO Phase 3: Apply Gemma 4 control token formatting here
                val prompt = request.messages.joinToString("\n") { "${it.role}: ${it.content}" }

                if (request.stream) {
                    // ── SSE Streaming Response ──────────────────────────────
                    call.response.header(HttpHeaders.CacheControl, "no-cache")
                    call.respondTextWriter(
                        contentType = ContentType.Text.EventStream,
                        status = HttpStatusCode.OK
                    ) {
                        // TODO Phase 3: Replace with LlmEngine.generate() token stream
                        val stubTokens = listOf("Hello", " from", " Project", " Claw", ".")

                        stubTokens.forEach { token ->
                            val chunk = ChatCompletionChunk(
                                id = requestId,
                                model = request.model,
                                choices = listOf(
                                    StreamChoice(
                                        index = 0,
                                        delta = DeltaContent(content = token),
                                        finish_reason = null
                                    )
                                )
                            )
                            write("data: ${json.encodeToString(chunk)}\n\n")
                            flush()
                        }

                        // Final [DONE] sentinel — required by OpenAI SSE spec
                        write("data: [DONE]\n\n")
                        flush()
                    }
                } else {
                    // ── Standard JSON Response ──────────────────────────────
                    var fullResponse = ""

                    // TODO Phase 3: Replace with LlmEngine.generate()
                    LlmEngine.generate(prompt) { token -> fullResponse += token }

                    val response = ChatCompletionResponse(
                        id = requestId,
                        model = request.model,
                        choices = listOf(
                            ChatChoice(
                                index = 0,
                                message = ChatMessage(role = "assistant", content = fullResponse),
                                finish_reason = "stop"
                            )
                        ),
                        usage = Usage(
                            prompt_tokens = prompt.length / 4,
                            completion_tokens = fullResponse.length / 4,
                            total_tokens = (prompt.length + fullResponse.length) / 4
                        )
                    )
                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }

    fun start() {
        server.start(wait = true)
    }

    fun stop() {
        server.stop(gracePeriodMillis = 1000, timeoutMillis = 3000)
    }
}
