package com.example.data.voice

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

class VoiceSpeechRecognizer(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        mainHandler.post {
            try {
                stopListening()

                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    onError("Speech recognition service is not available on this device")
                    return@post
                }

                val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListening.value = true
                            _statusMessage.value = "Listening... Speak your reminder"
                        }

                        override fun onBeginningOfSpeech() {
                            _statusMessage.value = "Recording voice..."
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            _audioRms.value = rmsdB.coerceIn(0f, 10f)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _statusMessage.value = "Processing audio..."
                        }

                        override fun onError(error: Int) {
                            _isListening.value = false
                            _audioRms.value = 0f
                            val message = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_CLIENT -> "Client speech error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                                SpeechRecognizer.ERROR_NETWORK -> "Network connection error during recognition"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking closer to the mic."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer busy"
                                SpeechRecognizer.ERROR_SERVER -> "Voice server error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                                else -> "Recognition error ($error)"
                            }
                            _statusMessage.value = null
                            onError(message)
                        }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            _audioRms.value = 0f
                            _statusMessage.value = null
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognizedText = matches?.firstOrNull().orEmpty().trim()
                            if (recognizedText.isNotEmpty()) {
                                onFinalResult(recognizedText)
                            } else {
                                onError("No words captured")
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull().orEmpty().trim()
                            if (partial.isNotEmpty()) {
                                onPartialResult(partial)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                speechRecognizer = recognizer

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your reminder...")
                }

                recognizer.startListening(intent)
                _isListening.value = true
                _statusMessage.value = "Starting microphone..."
            } catch (e: Exception) {
                Log.e("VoiceSpeechRecognizer", "Error starting speech recognition", e)
                _isListening.value = false
                _audioRms.value = 0f
                _statusMessage.value = null
                onError(e.message ?: "Failed to initialize microphone")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e("VoiceSpeechRecognizer", "Error stopping speech recognition", e)
            } finally {
                _isListening.value = false
                _audioRms.value = 0f
                _statusMessage.value = null
            }
        }
    }

    fun destroy() {
        stopListening()
    }
}
