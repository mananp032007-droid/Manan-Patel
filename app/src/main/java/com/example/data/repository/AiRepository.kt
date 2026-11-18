package com.example.data.repository

import android.util.Log
import com.example.data.local.EnginePreferencesRepository
import com.example.data.local.MemoryEntity
import com.example.data.local.MessageEntity
import com.example.data.remote.ChatCompletionRequest
import com.example.data.remote.OpenAiChatMessage
import com.example.data.remote.OpenJarvisApiFactory
import com.example.domain.AiReplyResult
import com.example.domain.EngineConfig
import com.example.domain.EngineMode
import com.example.domain.LocalToolExecutor
import com.example.domain.ServerProbeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

interface AiRepository {
    suspend fun generateReply(
        prompt: String,
        recentHistory: List<MessageEntity>,
        memories: List<MemoryEntity>,
        config: EngineConfig
    ): AiReplyResult

    suspend fun probeLocalServer(baseUrl: String): ServerProbeState
}

class HybridAiRepository(
    private val toolExecutor: LocalToolExecutor
) : AiRepository {

    companion object {
        private const val TAG = "HybridAiRepository"
    }

    override suspend fun generateReply(
        prompt: String,
        recentHistory: List<MessageEntity>,
        memories: List<MemoryEntity>,
        config: EngineConfig
    ): AiReplyResult {
        val startNs = System.nanoTime()

        // 1. Run OpenJarvis local ToolUsingAgent steps first if enabled
        val toolOutcome = if (config.toolRoutingEnabled) {
            toolExecutor.evaluateAndExecute(prompt)
        } else {
            null
        }
        val toolRecords = toolOutcome?.toolRecords.orEmpty()
        val toolContext = toolOutcome?.contextNotes.orEmpty()

        // 2. Route to selected inference backend
        return when (config.engineMode) {
            EngineMode.OPENJARVIS_SERVER -> {
                inferViaOpenJarvisServer(
                    prompt = prompt,
                    recentHistory = recentHistory,
                    memories = memories,
                    toolContext = toolContext,
                    toolRecords = toolRecords,
                    config = config,
                    startNs = startNs
                )
            }

            EngineMode.CLOUD_PLACEHOLDER -> {
                inferViaCloudPlaceholder(
                    prompt = prompt,
                    memories = memories,
                    toolContext = toolContext,
                    toolRecords = toolRecords,
                    config = config,
                    startNs = startNs
                )
            }
        }
    }

    override suspend fun probeLocalServer(baseUrl: String): ServerProbeState {
        val normalized = EnginePreferencesRepository.normalizeBaseUrl(baseUrl)
        val startNs = System.nanoTime()
        return try {
            val api = OpenJarvisApiFactory.create(normalized)
            val response = withTimeout(10_000L) {
                api.listModels()
            }
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
            ServerProbeState.Healthy(
                discoveredModels = response.data.map { it.id },
                latencyMs = elapsedMs
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Server probe failed for $normalized", e)
            ServerProbeState.Unreachable(
                e.localizedMessage ?: "Unable to connect to $normalized"
            )
        }
    }

    private suspend fun inferViaOpenJarvisServer(
        prompt: String,
        recentHistory: List<MessageEntity>,
        memories: List<MemoryEntity>,
        toolContext: List<String>,
        toolRecords: List<com.example.domain.ToolExecutionRecord>,
        config: EngineConfig,
        startNs: Long
    ): AiReplyResult {
        val normalizedUrl = EnginePreferencesRepository.normalizeBaseUrl(config.serverBaseUrl)
        val api = OpenJarvisApiFactory.create(normalizedUrl)

        val systemPrompt = buildString {
            appendLine("You are OpenJarvis, a concise, capable personal AI voice assistant.")
            appendLine("Keep responses clear and natural for voice playback unless the user asks for detailed output.")
            if (memories.isNotEmpty()) {
                appendLine("\nPersistent Local User Memories:")
                memories.take(8).forEach { mem ->
                    appendLine("- [${mem.category}] ${mem.fact}")
                }
            }
            if (toolContext.isNotEmpty()) {
                appendLine("\nLocal Tool Execution Results (already verified on-device):")
                toolContext.forEach { appendLine("- $it") }
            }
        }

        val chatMessages = buildList {
            add(OpenAiChatMessage(role = "system", content = systemPrompt))
            recentHistory.takeLast(8).forEach { msg ->
                if (msg.role == "user" || msg.role == "assistant") {
                    add(OpenAiChatMessage(role = msg.role, content = msg.content))
                }
            }
            add(OpenAiChatMessage(role = "user", content = prompt))
        }

        val response = withTimeout(45_000L) {
            api.chatCompletions(
                ChatCompletionRequest(
                    model = config.selectedModelId,
                    messages = chatMessages,
                    temperature = config.temperature,
                    maxTokens = config.maxTokens,
                    stream = false
                )
            )
        }

        val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
        val replyText = response.choices.firstOrNull()?.message?.content?.trim()
            ?: throw IllegalStateException("OpenJarvis server returned an empty completion.")
        val tokens = response.usage?.totalTokens ?: estimateTokens(prompt + replyText)

        return AiReplyResult(
            text = replyText,
            engineUsed = EngineMode.OPENJARVIS_SERVER.shortLabel,
            modelUsed = response.model ?: config.selectedModelId,
            toolsInvoked = toolRecords,
            latencyMs = elapsedMs,
            estimatedTokens = tokens,
            isPlaceholderResponse = false,
            status = "OK"
        )
    }

    private fun inferViaCloudPlaceholder(
        prompt: String,
        memories: List<MemoryEntity>,
        toolContext: List<String>,
        toolRecords: List<com.example.domain.ToolExecutionRecord>,
        config: EngineConfig,
        startNs: Long
    ): AiReplyResult {
        val replyText = buildString {
            if (toolContext.isNotEmpty()) {
                appendLine(toolContext.joinToString(" "))
                appendLine()
                append("[Sample Cloud Response] Firebase AI Logic is not enabled yet. Local OpenJarvis tools executed above; enable Firebase AI Logic for full Gemini reasoning or connect `jarvis serve` in Engine.")
            } else {
                val memoryHint = if (memories.isNotEmpty()) {
                    " I currently have ${memories.size} local memory item(s) loaded in context."
                } else {
                    ""
                }
                append(
                    "[Sample Cloud Response] I received \"$prompt\".$memoryHint " +
                        "Live Gemini responses require enabling Firebase AI Logic. " +
                        "You can also run local OpenJarvis tools (remember facts, add tasks, calculate math, check time) " +
                        "or connect your own `jarvis serve` / Ollama endpoint in Engine & Traces."
                )
            }
        }

        val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(12L)
        return AiReplyResult(
            text = replyText,
            engineUsed = EngineMode.CLOUD_PLACEHOLDER.shortLabel,
            modelUsed = config.selectedModelId,
            toolsInvoked = toolRecords,
            latencyMs = elapsedMs,
            estimatedTokens = estimateTokens(prompt + replyText),
            isPlaceholderResponse = true,
            status = "PLACEHOLDER"
        )
    }

    private fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)
}
