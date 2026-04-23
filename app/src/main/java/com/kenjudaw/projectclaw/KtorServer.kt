package com.kenjudaw.projectclaw

import android.content.res.AssetManager
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
data class ToolFunction(val name: String, val description: String? = null, val parameters: kotlinx.serialization.json.JsonObject? = null)

@Serializable
data class Tool(val type: String, val function: ToolFunction)

@Serializable
data class ToolCall(val id: String, val type: String, val function: ToolFunctionCall)

@Serializable
data class ToolFunctionCall(val name: String, val arguments: String)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String? = null,
    val tool_calls: List<ToolCall>? = null,
    val tool_call_id: String? = null
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val tools: List<Tool>? = null,
    val tool_choice: String? = "auto",
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
    private var assetManager: AssetManager? = null

    private val server: ApplicationEngine = embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = 8080
    ) {
        install(ContentNegotiation) {
            json(json)
        }

        routing {
            get("/") {
                call.respondText(
                    assetManager?.open("web/index.html")?.bufferedReader()?.use { it.readText() } ?: "Index not found",
                    ContentType.Text.Html
                )
            }

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

                if (!LlmEngine.isInitialized) {
                    call.respond(HttpStatusCode.ServiceUnavailable, "Engine not initialized")
                    return@post
                }

                // Building the Gemma 4 formatted prompt with context and tools
                val formattedPrompt = buildString {
                    // Inject Tool Definitions if present
                    request.tools?.let {
                        append("[AVAILABLE_TOOLS]\n")
                        append(json.encodeToString(it))
                        append("\n")
                    }

                    // Append Conversation History
                    request.messages.forEach { msg ->
                        append("<start_of_turn>${msg.role}\n")
                        msg.content?.let { append(it) }
                        msg.tool_calls?.forEach { tc ->
                            append("\n<call:${tc.function.name}${tc.function.arguments}>")
                        }
                        append("<end_of_turn>\n")
                    }
                    append("<start_of_turn>model\n")
                }

                if (request.stream) {
                    call.response.header(HttpHeaders.CacheControl, "no-cache")
                    call.respondTextWriter(
                        contentType = ContentType.Text.EventStream,
                        status = HttpStatusCode.OK
                    ) {
                        try {
                            LlmEngine.generate(
                                formattedPrompt = formattedPrompt,
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
                        val fullResponse = LlmEngine.generateBlocking(formattedPrompt)

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
                            usage = Usage(0, 0, 0)
                        )
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "Inference failed: ${e.message}")
                    }
                }
            }
        }
    }

    fun start(assets: AssetManager) {
        assetManager = assets
        server.start(wait = true)
    }

    fun stop() {
        server.stop(gracePeriodMillis = 1000, timeoutMillis = 3000)
    }
}
