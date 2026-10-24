package com.example.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * SobiVoiceListener: Handles live microphone speech recognition during voice & video calls.
 * Automatically detects user voice, computes RMS audio waveforms, and triggers callbacks
 * when the user finishes speaking.
 */
class SobiVoiceListener(
    private val context: Context,
    private val onSpeechStarted: () -> Unit,
    private val onPartialTranscript: (String) -> Unit,
    private val onFinalTranscript: (String) -> Unit,
    private val onRmsChangedCallback: (Float) -> Unit = {}
) {
    private val TAG = "SobiVoiceListener"
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isUserSpeaking = MutableStateFlow(false)
    val isUserSpeaking: StateFlow<Boolean> = _isUserSpeaking.asStateFlow()

    private val _lastRmsDb = MutableStateFlow(0f)
    val lastRmsDb: StateFlow<Float> = _lastRmsDb.asStateFlow()

    private var isContinuous = false
    private var isPausedForAgentSpeech = false
    private var lastRecordedPartial = ""

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer is not available on this device/emulator.")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        _isUserSpeaking.value = true
                        lastRecordedPartial = ""
                        onSpeechStarted()
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _lastRmsDb.value = rmsdB.coerceAtLeast(0f)
                        onRmsChangedCallback(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isUserSpeaking.value = false
                    }

                    override fun onError(error: Int) {
                        _isUserSpeaking.value = false
                        Log.d(TAG, "SpeechRecognizer onError: $error")

                        // If user spoke partial words and then an error/pause occurred, treat partial as query
                        if (lastRecordedPartial.isNotBlank()) {
                            val captured = lastRecordedPartial.trim()
                            lastRecordedPartial = ""
                            onFinalTranscript(captured)
                        } else if (isContinuous && !isPausedForAgentSpeech) {
                            // Automatically restart listening after short silence or transient timeout
                            mainHandler.postDelayed({
                                if (isContinuous && !isPausedForAgentSpeech) {
                                    startListeningInternal()
                                }
                            }, 500L)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isUserSpeaking.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: lastRecordedPartial
                        lastRecordedPartial = ""
                        if (!text.isNullOrBlank()) {
                            onFinalTranscript(text.trim())
                        } else if (isContinuous && !isPausedForAgentSpeech) {
                            mainHandler.postDelayed({
                                if (isContinuous && !isPausedForAgentSpeech) {
                                    startListeningInternal()
                                }
                            }, 400L)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: ""
                        if (partial.isNotBlank()) {
                            lastRecordedPartial = partial
                            onPartialTranscript(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SpeechRecognizer", e)
        }
    }

    fun startListening() {
        isContinuous = true
        isPausedForAgentSpeech = false
        mainHandler.post {
            startListeningInternal()
        }
    }

    private fun startListeningInternal() {
        if (speechRecognizer == null) {
            initRecognizer()
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            }
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech listening", e)
            _isListening.value = false
        }
    }

    /**
     * Pauses listening while the AI Agent is speaking its response
     * to prevent audio loop / echo.
     */
    fun pauseForAgentSpeech() {
        isPausedForAgentSpeech = true
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                _isListening.value = false
                _isUserSpeaking.value = false
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Resumes listening once the AI Agent has finished speaking its response.
     */
    fun resumeAfterAgentSpeech() {
        isPausedForAgentSpeech = false
        if (isContinuous) {
            mainHandler.postDelayed({
                if (isContinuous && !isPausedForAgentSpeech) {
                    startListeningInternal()
                }
            }, 300L)
        }
    }

    fun stopListening() {
        isContinuous = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // Ignore
            }
            _isListening.value = false
            _isUserSpeaking.value = false
        }
    }

    fun destroy() {
        isContinuous = false
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // Ignore
            }
            speechRecognizer = null
            _isListening.value = false
            _isUserSpeaking.value = false
        }
    }
}
