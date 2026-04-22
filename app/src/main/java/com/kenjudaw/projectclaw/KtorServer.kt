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

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val max_tokens: Int = 4096,
    val temperature: Float = 0.7f
)

@Serializable
data class ChatChoice(val index: Int, val message: ChatMessage, val finish_reason: String)

@Serializable
data class Usage(val prompt_tokens: Int, val completion_tokens: Int, val total_tokens: Int)

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val `object`: String = "chat.completion",
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage
)

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
                if (LlmEngine.isInitialized) {
                    call.respondText("Project Claw OK - Engine Ready", status = HttpStatusCode.OK)
                } else {
                    call.respondText("Project Claw OK - Engine Not Initialized", status = HttpStatusCode.ServiceUnavailable)
                }
            }

            post("/v1/chat/completions") {
                val request = call.receive<ChatCompletionRequest>()
                val requestId = "chatcmpl-${UUID.randomUUID()}"

                // Simplified prompt extraction - assume last message is user for now
                // In Phase 5 we will build the full conversation history
                val prompt = request.messages.lastOrNull { it.role == "user" }?.content ?: ""

                if (!LlmEngine.isInitialized) {
                    call.respond(HttpStatusCode.ServiceUnavailable, "Engine not initialized")
                    return@post
                }

                if (request.stream) {
                    call.response.header(HttpHeaders.CacheControl, "no-cache")
                    call.respondTextWriter(
                        contentType = ContentType.Text.EventStream,
                        status = HttpStatusCode.OK
                    ) {
                        try {
                            LlmEngine.generate(
                                userMessage = prompt,
                                onToken = { token ->
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
                                },
                                onDone = {
                                    write("data: [DONE]\n\n")
                                    flush()
                                },
                                onError = { t ->
                                    // Log or handle error here
                                    write("event: error\ndata: ${t.message}\n\n")
                                    flush()
                                }
                            )
                        } catch (e: Exception) {
                            write("event: error\ndata: ${e.message}\n\n")
                            flush()
                        }
                    }
                } else {
                    try {
                        val fullResponse = LlmEngine.generateBlocking(prompt)

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
                            usage = Usage(0, 0, 0) // Usage stats mock
                        )
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "Inference failed: ${e.message}")
                    }
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
