package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

sealed interface SpeechEvent {
    data object Idle : SpeechEvent
    data class Listening(val partialText: String, val rmsNormalized: Float) : SpeechEvent
    data class FinalTranscript(val text: String) : SpeechEvent
    data class Error(val message: String) : SpeechEvent
}

class VoiceController(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val _speechEvent = MutableStateFlow<SpeechEvent>(SpeechEvent.Idle)
    val speechEvent: StateFlow<SpeechEvent> = _speechEvent.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<Long?>(null)
    val speakingMessageId: StateFlow<Long?> = _speakingMessageId.asStateFlow()

    init {
        tts = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            tts?.setSpeechRate(1.03f)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _speakingMessageId.value = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _speakingMessageId.value = null
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    _speakingMessageId.value = null
                }
            })
        } else {
            Log.w(TAG, "TextToSpeech init failed with status=$status")
        }
    }

    fun startListening() {
        mainHandler.post {
            stopSpeaking()
            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                _speechEvent.value = SpeechEvent.Error(
                    "On-device Speech Recognition service is unavailable on this device. Use the text input below."
                )
                return@post
            }

            try {
                speechRecognizer?.destroy()
                val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _speechEvent.value = SpeechEvent.Listening(partialText = "", rmsNormalized = 0.1f)
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        val current = _speechEvent.value
                        val partial = (current as? SpeechEvent.Listening)?.partialText.orEmpty()
                        // Typical SpeechRecognizer RMS ranges roughly -2f .. 10f
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                        _speechEvent.value = SpeechEvent.Listening(
                            partialText = partial,
                            rmsNormalized = normalized
                        )
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network error."
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Try speaking closer to the mic."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please try again."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out before speech was heard."
                            else -> "Speech recognition unavailable (code $error). Try typing below."
                        }
                        _speechEvent.value = SpeechEvent.Error(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = matches?.firstOrNull()?.trim().orEmpty()
                        if (best.isNotEmpty()) {
                            _speechEvent.value = SpeechEvent.FinalTranscript(best)
                        } else {
                            _speechEvent.value = SpeechEvent.Error("Couldn't catch that. Please try again.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        val currentRms = (_speechEvent.value as? SpeechEvent.Listening)?.rmsNormalized ?: 0.2f
                        if (partial.isNotBlank()) {
                            _speechEvent.value = SpeechEvent.Listening(partial, currentRms)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                _speechEvent.value = SpeechEvent.Listening(partialText = "", rmsNormalized = 0.08f)
                recognizer.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognizer", e)
                _speechEvent.value = SpeechEvent.Error(
                    e.localizedMessage ?: "Failed to start voice capture."
                )
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }
            if (_speechEvent.value is SpeechEvent.Listening) {
                _speechEvent.value = SpeechEvent.Idle
            }
        }
    }

    fun consumeSpeechEvent() {
        _speechEvent.value = SpeechEvent.Idle
    }

    fun speak(text: String, messageId: Long? = null) {
        if (!ttsReady || text.isBlank()) return
        val cleanText = text
            .replace("[Sample Cloud Response]", "")
            .replace("`", "")
            .trim()
        val utteranceId = UUID.randomUUID().toString()
        _speakingMessageId.value = messageId
        _isSpeaking.value = true
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
        _speakingMessageId.value = null
    }

    fun release() {
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        private const val TAG = "VoiceController"
    }
}
