package com.example.domain

enum class EngineMode(val displayName: String, val shortLabel: String) {
    CLOUD_PLACEHOLDER(
        displayName = "Cloud AI (Sample Mode)",
        shortLabel = "Sample Cloud"
    ),
    OPENJARVIS_SERVER(
        displayName = "OpenJarvis Local Server",
        shortLabel = "jarvis serve"
    )
}

data class JarvisModelSpec(
    val id: String,
    val displayName: String,
    val engineTag: String,
    val parameters: String,
    val contextWindow: String,
    val isLocalCompatible: Boolean
)

object BuiltinModelsCatalog {
    val models: List<JarvisModelSpec> = listOf(
        JarvisModelSpec(
            id = "gemini-3.8-flash",
            displayName = "Gemini 3.8 Flash",
            engineTag = "Firebase AI Logic (Requires Setup)",
            parameters = "Cloud",
            contextWindow = "1M ctx",
            isLocalCompatible = false
        ),
        JarvisModelSpec(
            id = "qwen2.5:7b-instruct",
            displayName = "Qwen 2.5 7B Instruct",
            engineTag = "jarvis serve / Ollama",
            parameters = "7B Q4_K_M",
            contextWindow = "32K ctx",
            isLocalCompatible = true
        ),
        JarvisModelSpec(
            id = "llama3.2:3b",
            displayName = "Llama 3.2 3B",
            engineTag = "jarvis serve / llama.cpp",
            parameters = "3B Q4_K_M",
            contextWindow = "128K ctx",
            isLocalCompatible = true
        ),
        JarvisModelSpec(
            id = "deepseek-r1:8b",
            displayName = "DeepSeek R1 8B",
            engineTag = "jarvis serve / vLLM",
            parameters = "8B Q4_K_M",
            contextWindow = "64K ctx",
            isLocalCompatible = true
        ),
        JarvisModelSpec(
            id = "mistral:7b",
            displayName = "Mistral 7B v0.3",
            engineTag = "jarvis serve / Ollama",
            parameters = "7B Q4_0",
            contextWindow = "32K ctx",
            isLocalCompatible = true
        )
    )
}

data class EngineConfig(
    val engineMode: EngineMode = EngineMode.CLOUD_PLACEHOLDER,
    val serverBaseUrl: String = "http://10.0.2.2:8000/v1/",
    val selectedModelId: String = "gemini-3.8-flash",
    val temperature: Float = 0.6f,
    val maxTokens: Int = 1024,
    val autoSpeakEnabled: Boolean = true,
    val toolRoutingEnabled: Boolean = true,
    val darkThemeEnabled: Boolean = true
)

sealed interface VoiceUiState {
    data object Idle : VoiceUiState
    data class Listening(val partialTranscript: String, val rmsLevel: Float) : VoiceUiState
    data object Thinking : VoiceUiState
    data class Speaking(val utteranceId: String) : VoiceUiState
    data class Error(val message: String) : VoiceUiState
}

data class ToolExecutionRecord(
    val toolName: String,
    val summary: String,
    val succeeded: Boolean = true
)

data class AiReplyResult(
    val text: String,
    val engineUsed: String,
    val modelUsed: String,
    val toolsInvoked: List<ToolExecutionRecord>,
    val latencyMs: Long,
    val estimatedTokens: Int,
    val isPlaceholderResponse: Boolean,
    val status: String
)

sealed interface ServerProbeState {
    data object Idle : ServerProbeState
    data object Probing : ServerProbeState
    data class Healthy(
        val discoveredModels: List<String>,
        val latencyMs: Long
    ) : ServerProbeState
    data class Unreachable(val reason: String) : ServerProbeState
}
