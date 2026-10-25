package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Owned by the visible screen. Only final results change the draft; cancellation never erases it. */
class ReminderVoiceInput(private val context: Context, private val onTranscript: (String) -> Unit, private val onErrorMessage: (String) -> Unit) {
    var isListening by mutableStateOf(false)
        private set
    var isProcessing by mutableStateOf(false)
        private set
    var partialTranscript by mutableStateOf("")
        private set
    var level by mutableStateOf(0f)
        private set
    private var recognizer: SpeechRecognizer? = null
    private var active = false
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable {
        if (active) { cancel(); onErrorMessage("Voice input timed out. Your draft is unchanged; tap the microphone to try again.") }
    }
    val isActive get() = active

    fun start() {
        if (active) { cancel(); return }
        val onDevice = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        if (!onDevice && !SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorMessage("Voice input is unavailable. Enable a speech service in your phone settings, or type your reminder.")
            return
        }
        try {
            recognizer?.destroy()
            recognizer = if (onDevice && Build.VERSION.SDK_INT >= 31) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                else SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { if (active) { isListening = true; isProcessing = false } }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) { if (active) level = (rmsdB / 10f).coerceIn(0f, 1f) }
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { if (active) { isListening = false; isProcessing = true } }
                override fun onError(error: Int) {
                    if (!active) return
                    reset()
                    onErrorMessage(errorMessage(error))
                }
                override fun onResults(results: Bundle?) {
                    if (!active) return
                    val transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                    reset()
                    if (transcript.isNotBlank()) onTranscript(transcript) else onErrorMessage("No speech was recognized. Try again, or type your reminder.")
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if (active) partialTranscript = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            active = true
            isListening = true
            partialTranscript = ""
            handler.postDelayed(timeout, 30_000)
            recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            })
        } catch (error: Exception) {
            cancel()
            onErrorMessage(if (error is SecurityException) "Microphone access is needed for voice input. Allow it in app settings."
                else "Couldn't start voice input. Try again, or type your reminder.")
        }
    }
    private fun reset() { handler.removeCallbacks(timeout); active = false; isListening = false; isProcessing = false; partialTranscript = ""; level = 0f }
    fun cancel() { reset(); runCatching { recognizer?.cancel() } }
    fun release() { cancel(); runCatching { recognizer?.destroy() }; recognizer = null }

    companion object {
        fun errorMessage(code: Int) = when (code) {
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Allow microphone access in app settings to use voice input."
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that. Try speaking again, or type your reminder."
            SpeechRecognizer.ERROR_AUDIO -> "The microphone couldn't be opened. Close other recording apps and try again."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Your phone's speech service needs a connection. You can still type reminders offline."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The speech service is busy. Wait a moment and try again."
            else -> "Voice input stopped. Try again, or type your reminder."
        }
    }
}
