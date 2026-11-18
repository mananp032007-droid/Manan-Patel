package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.EnginePreferencesRepository
import com.example.data.local.MemoryEntity
import com.example.data.local.MessageEntity
import com.example.data.local.OpenJarvisDatabase
import com.example.data.local.TaskEntity
import com.example.data.local.TraceEntity
import com.example.data.repository.AiRepository
import com.example.data.repository.HybridAiRepository
import com.example.data.repository.JarvisWorkspaceRepository
import com.example.domain.EngineConfig
import com.example.domain.EngineMode
import com.example.domain.LocalToolExecutor
import com.example.domain.ServerProbeState
import com.example.domain.VoiceUiState
import com.example.voice.SpeechEvent
import com.example.voice.VoiceController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TraceSummaryMetrics(
    val totalTraces: Int = 0,
    val avgLatencyMs: Long = 0L,
    val toolHitPercent: Int = 0,
    val lastStatus: String = "IDLE"
)

data class OpenJarvisUiState(
    val messages: List<MessageEntity> = emptyList(),
    val memories: List<MemoryEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val traces: List<TraceEntity> = emptyList(),
    val traceMetrics: TraceSummaryMetrics = TraceSummaryMetrics(),
    val engineConfig: EngineConfig = EngineConfig(),
    val voiceState: VoiceUiState = VoiceUiState.Idle,
    val speakingMessageId: Long? = null,
    val serverProbeState: ServerProbeState = ServerProbeState.Idle,
    val errorBanner: String? = null,
    val isInferring: Boolean = false
)

class OpenJarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val database = OpenJarvisDatabase.getInstance(application)
    private val workspaceRepo = JarvisWorkspaceRepository(
        messageDao = database.messageDao(),
        memoryDao = database.memoryDao(),
        taskDao = database.taskDao(),
        traceDao = database.traceDao()
    )
    private val prefsRepo = EnginePreferencesRepository(application)
    private val toolExecutor = LocalToolExecutor(
        memoryDao = database.memoryDao(),
        taskDao = database.taskDao()
    )
    private val aiRepository: AiRepository = HybridAiRepository(toolExecutor)
    private val voiceController = VoiceController(application)

    private val _isInferring = MutableStateFlow(false)
    private val _serverProbeState = MutableStateFlow<ServerProbeState>(ServerProbeState.Idle)
    private val _errorBanner = MutableStateFlow<String?>(null)

    private val voiceUiStateFlow: StateFlow<VoiceUiState> = combine(
        voiceController.speechEvent,
        voiceController.isSpeaking,
        voiceController.speakingMessageId,
        _isInferring
    ) { speechEvent, isSpeaking, speakingId, inferring ->
        when {
            inferring -> VoiceUiState.Thinking
            speechEvent is SpeechEvent.Listening -> VoiceUiState.Listening(
                partialTranscript = speechEvent.partialText,
                rmsLevel = speechEvent.rmsNormalized
            )
            isSpeaking -> VoiceUiState.Speaking(utteranceId = speakingId?.toString().orEmpty())
            speechEvent is SpeechEvent.Error -> VoiceUiState.Error(speechEvent.message)
            else -> VoiceUiState.Idle
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VoiceUiState.Idle
    )

    val uiState: StateFlow<OpenJarvisUiState> = combine(
        workspaceRepo.messages,
        workspaceRepo.memories,
        workspaceRepo.tasks,
        workspaceRepo.traces,
        prefsRepo.configFlow,
        voiceUiStateFlow,
        voiceController.speakingMessageId,
        _serverProbeState,
        _errorBanner,
        _isInferring
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val messages = values[0] as List<MessageEntity>
        @Suppress("UNCHECKED_CAST")
        val memories = values[1] as List<MemoryEntity>
        @Suppress("UNCHECKED_CAST")
        val tasks = values[2] as List<TaskEntity>
        @Suppress("UNCHECKED_CAST")
        val traces = values[3] as List<TraceEntity>
        val config = values[4] as EngineConfig
        val voiceState = values[5] as VoiceUiState
        val speakingMsgId = values[6] as Long?
        val probeState = values[7] as ServerProbeState
        val errorBanner = values[8] as String?
        val inferring = values[9] as Boolean

        val metrics = computeTraceMetrics(traces)
        OpenJarvisUiState(
            messages = messages,
            memories = memories,
            tasks = tasks,
            traces = traces,
            traceMetrics = metrics,
            engineConfig = config,
            voiceState = voiceState,
            speakingMessageId = speakingMsgId,
            serverProbeState = probeState,
            errorBanner = errorBanner,
            isInferring = inferring
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OpenJarvisUiState()
    )

    init {
        viewModelScope.launch {
            voiceController.speechEvent.collect { event ->
                when (event) {
                    is SpeechEvent.FinalTranscript -> {
                        voiceController.consumeSpeechEvent()
                        sendPrompt(event.text)
                    }
                    is SpeechEvent.Error -> {
                        _errorBanner.value = event.message
                    }
                    else -> Unit
                }
            }
        }
    }

    fun startVoiceCapture() {
        _errorBanner.value = null
        voiceController.startListening()
    }

    fun stopVoiceCapture() {
        voiceController.stopListening()
    }

    fun speakMessage(messageId: Long, text: String) {
        voiceController.speak(text, messageId)
    }

    fun stopSpeaking() {
        voiceController.stopSpeaking()
    }

    fun dismissErrorBanner() {
        _errorBanner.value = null
        if (voiceController.speechEvent.value is SpeechEvent.Error) {
            voiceController.consumeSpeechEvent()
        }
    }

    fun sendPrompt(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() || _isInferring.value) return

        viewModelScope.launch {
            _errorBanner.value = null
            _isInferring.value = true
            voiceController.stopSpeaking()

            val currentState = uiState.value
            val config = currentState.engineConfig

            workspaceRepo.addMessage(
                MessageEntity(
                    role = "user",
                    content = prompt
                )
            )

            val startNs = System.nanoTime()
            try {
                val memoriesSnapshot = workspaceRepo.getMemoriesSnapshot()
                val reply = aiRepository.generateReply(
                    prompt = prompt,
                    recentHistory = currentState.messages,
                    memories = memoriesSnapshot,
                    config = config
                )

                val toolsCsv = reply.toolsInvoked.joinToString(",") { it.toolName }
                val insertedId = workspaceRepo.addMessage(
                    MessageEntity(
                        role = "assistant",
                        content = reply.text,
                        toolsUsed = toolsCsv,
                        engineLabel = "${reply.engineUsed} · ${reply.modelUsed}",
                        latencyMs = reply.latencyMs,
                        isPlaceholder = reply.isPlaceholderResponse
                    )
                )

                workspaceRepo.recordTrace(
                    TraceEntity(
                        promptPreview = prompt.take(80),
                        engineName = reply.engineUsed,
                        modelName = reply.modelUsed,
                        toolsInvoked = toolsCsv.ifBlank { "none" },
                        latencyMs = reply.latencyMs,
                        estimatedTokens = reply.estimatedTokens,
                        status = reply.status
                    )
                )

                if (config.autoSpeakEnabled) {
                    voiceController.speak(reply.text, insertedId)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "OpenJarvis inference timed out", e)
                val latency = (System.nanoTime() - startNs) / 1_000_000L
                val msg = "Inference timed out after 45s. Check your OpenJarvis server or try again."
                _errorBanner.value = msg
                recordErrorTurn(prompt, config, latency, msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "OpenJarvis inference failed", e)
                val latency = (System.nanoTime() - startNs) / 1_000_000L
                val msg = e.localizedMessage ?: "Failed to complete request."
                _errorBanner.value = msg
                recordErrorTurn(prompt, config, latency, msg)
            } finally {
                _isInferring.value = false
            }
        }
    }

    private suspend fun recordErrorTurn(
        prompt: String,
        config: EngineConfig,
        latencyMs: Long,
        errorMessage: String
    ) {
        workspaceRepo.addMessage(
            MessageEntity(
                role = "system_error",
                content = "Engine error (${config.engineMode.shortLabel}): $errorMessage",
                engineLabel = config.selectedModelId,
                latencyMs = latencyMs
            )
        )
        workspaceRepo.recordTrace(
            TraceEntity(
                promptPreview = prompt.take(80),
                engineName = config.engineMode.shortLabel,
                modelName = config.selectedModelId,
                toolsInvoked = "none",
                latencyMs = latencyMs.coerceAtLeast(1L),
                estimatedTokens = 0,
                status = "ERROR"
            )
        )
    }

    fun clearConversation() {
        viewModelScope.launch {
            voiceController.stopSpeaking()
            workspaceRepo.clearConversation()
        }
    }

    fun addMemory(category: String, fact: String) {
        viewModelScope.launch {
            workspaceRepo.addMemory(category, fact)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            workspaceRepo.deleteMemory(id)
        }
    }

    fun addTask(title: String) {
        viewModelScope.launch {
            workspaceRepo.addTask(title)
        }
    }

    fun toggleTask(id: Long, completed: Boolean) {
        viewModelScope.launch {
            workspaceRepo.toggleTask(id, completed)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            workspaceRepo.deleteTask(id)
        }
    }

    fun setEngineMode(mode: EngineMode) {
        viewModelScope.launch {
            prefsRepo.setEngineMode(mode)
        }
    }

    fun setServerBaseUrl(url: String) {
        viewModelScope.launch {
            prefsRepo.setServerBaseUrl(url)
        }
    }

    fun setSelectedModel(modelId: String) {
        viewModelScope.launch {
            prefsRepo.setSelectedModel(modelId)
        }
    }

    fun setTemperature(temperature: Float) {
        viewModelScope.launch {
            prefsRepo.setTemperature(temperature)
        }
    }

    fun setAutoSpeak(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAutoSpeak(enabled)
            if (!enabled) voiceController.stopSpeaking()
        }
    }

    fun setToolRouting(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setToolRouting(enabled)
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setDarkTheme(enabled)
        }
    }

    fun probeLocalServer(url: String) {
        viewModelScope.launch {
            _serverProbeState.value = ServerProbeState.Probing
            val normalized = EnginePreferencesRepository.normalizeBaseUrl(url)
            prefsRepo.setServerBaseUrl(normalized)
            val result = aiRepository.probeLocalServer(normalized)
            _serverProbeState.value = result
            if (result is ServerProbeState.Healthy && result.discoveredModels.isNotEmpty()) {
                val currentModel = uiState.value.engineConfig.selectedModelId
                if (currentModel == "gemini-3.8-flash") {
                    prefsRepo.setSelectedModel(result.discoveredModels.first())
                }
            }
        }
    }

    fun clearTraces() {
        viewModelScope.launch {
            workspaceRepo.clearTraces()
        }
    }

    private fun computeTraceMetrics(traces: List<TraceEntity>): TraceSummaryMetrics {
        if (traces.isEmpty()) return TraceSummaryMetrics()
        val avgLatency = traces.map { it.latencyMs }.average().toLong()
        val withTools = traces.count { it.toolsInvoked != "none" && it.toolsInvoked.isNotBlank() }
        val hitPct = ((withTools * 100f) / traces.size).toInt()
        return TraceSummaryMetrics(
            totalTraces = traces.size,
            avgLatencyMs = avgLatency,
            toolHitPercent = hitPct,
            lastStatus = traces.first().status
        )
    }

    override fun onCleared() {
        voiceController.release()
        super.onCleared()
    }

    companion object {
        private const val TAG = "OpenJarvisViewModel"
    }
}
