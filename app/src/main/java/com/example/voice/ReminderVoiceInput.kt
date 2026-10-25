package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

internal interface SpeechEngine {
    fun start(intent: Intent)
    fun cancel()
    fun destroy()
}

internal interface SpeechEngineFactory {
    fun isAvailable(): Boolean
    fun create(listener: RecognitionListener): SpeechEngine
}

internal interface VoiceScheduler {
    val now: Long
    fun post(task: Runnable, delayMillis: Long)
    fun remove(task: Runnable)
}

private class MainVoiceScheduler : VoiceScheduler {
    private val handler = Handler(Looper.getMainLooper())
    override val now get() = SystemClock.uptimeMillis()
    override fun post(task: Runnable, delayMillis: Long) { handler.postDelayed(task, delayMillis) }
    override fun remove(task: Runnable) { handler.removeCallbacks(task) }
}

private class AndroidSpeechEngineFactory(private val context: Context) : SpeechEngineFactory {
    private fun onDevice() = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    override fun isAvailable() = onDevice() || SpeechRecognizer.isRecognitionAvailable(context)
    override fun create(listener: RecognitionListener): SpeechEngine {
        val recognizer = if (onDevice() && Build.VERSION.SDK_INT >= 31) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            else SpeechRecognizer.createSpeechRecognizer(context)
        try { recognizer.setRecognitionListener(listener) }
        catch (error: Exception) { recognizer.destroy(); throw error }
        return object : SpeechEngine {
            override fun start(intent: Intent) = recognizer.startListening(intent)
            override fun cancel() = recognizer.cancel()
            override fun destroy() = recognizer.destroy()
        }
    }
}

/** Main-thread owner of one microphone session. Final results alone can change the draft. */
class ReminderVoiceInput internal constructor(
    private val factory: SpeechEngineFactory,
    private val scheduler: VoiceScheduler,
    private val onTranscript: (String) -> Unit,
    private val onErrorMessage: (String) -> Unit
) {
    constructor(context: Context, onTranscript: (String) -> Unit, onErrorMessage: (String) -> Unit) :
        this(AndroidSpeechEngineFactory(context.applicationContext), MainVoiceScheduler(), onTranscript, onErrorMessage)

    var isListening by mutableStateOf(false)
        private set
    var isProcessing by mutableStateOf(false)
        private set
    var partialTranscript by mutableStateOf("")
        private set
    var level by mutableStateOf(0f)
        private set
    var isActive by mutableStateOf(false)
        private set

    private var engine: SpeechEngine? = null
    private var generation = 0L
    private var released = false
    private var retriedBusy = false
    private var availableAfter = 0L
    private var pendingStart: Runnable? = null
    private var timeout: Runnable? = null

    fun start() {
        if (released) return
        if (isActive) { cancel(); return }
        isActive = true
        isProcessing = true
        retriedBusy = false
        val session = ++generation
        val watchdog = Runnable {
            if (owns(session)) {
                finish()
                onErrorMessage("Voice input timed out. Your draft is unchanged; tap the microphone to try again.")
            }
        }
        timeout = watchdog
        scheduler.post(watchdog, 30_000)
        scheduleStart(session, (availableAfter - scheduler.now).coerceAtLeast(0))
    }

    private fun owns(session: Long) = !released && isActive && generation == session

    private fun scheduleStart(session: Long, wait: Long) {
        val task = Runnable {
            pendingStart = null
            if (owns(session)) begin(session)
        }
        pendingStart = task
        scheduler.post(task, wait)
    }

    private fun begin(session: Long) {
        try {
            if (!factory.isAvailable()) {
                finish()
                onErrorMessage("Voice input is unavailable. Enable a speech service in your phone settings, or type your reminder.")
                return
            }
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (owns(session)) { isListening = true; isProcessing = false }
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) { if (owns(session)) level = (rmsdB / 10f).coerceIn(0f, 1f) }
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { if (owns(session)) { isListening = false; isProcessing = true; level = 0f } }
                override fun onError(error: Int) {
                    if (!owns(session)) return
                    if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY && !retriedBusy) {
                        // Services may finish unbinding after their terminal callback. Recover once.
                        retriedBusy = true
                        val retry = ++generation
                        disposeEngine()
                        isListening = false; isProcessing = true; partialTranscript = ""; level = 0f
                        timeout?.let(scheduler::remove)
                        val watchdog = Runnable {
                            if (owns(retry)) { finish(); onErrorMessage(errorMessage(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) }
                        }
                        timeout = watchdog
                        scheduler.post(watchdog, 30_000)
                        scheduleStart(retry, 450)
                    } else {
                        finish()
                        onErrorMessage(errorMessage(error))
                    }
                }
                override fun onResults(results: Bundle?) {
                    if (!owns(session)) return
                    val transcript = results.transcript().trim()
                    finish()
                    if (transcript.isNotBlank()) onTranscript(transcript)
                    else onErrorMessage("No speech was recognized. Try again, or type your reminder.")
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if (owns(session)) partialTranscript = partialResults.transcript()
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            }
            engine = factory.create(listener)
            isListening = true; isProcessing = false
            engine?.start(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            })
        } catch (error: Exception) {
            if (!owns(session)) return
            finish()
            onErrorMessage(if (error is SecurityException) "Microphone access is needed for voice input. Allow it in app settings."
                else "Couldn't start voice input. Try again, or type your reminder.")
        }
    }

    private fun Bundle?.transcript() = this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()

    private fun disposeEngine() {
        val previous = engine
        engine = null
        if (previous != null) {
            availableAfter = scheduler.now + 250
            runCatching { previous.cancel() }
            runCatching { previous.destroy() }
        }
    }

    private fun finish() {
        ++generation
        isActive = false
        pendingStart?.let(scheduler::remove); pendingStart = null
        timeout?.let(scheduler::remove); timeout = null
        isListening = false; isProcessing = false; partialTranscript = ""; level = 0f
        disposeEngine()
    }

    fun cancel() = finish()
    fun release() { released = true; finish() }

    companion object {
        fun errorMessage(code: Int) = when (code) {
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Allow microphone access in app settings to use voice input."
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that. Try speaking again, or type your reminder."
            SpeechRecognizer.ERROR_AUDIO -> "The microphone couldn't be opened. Close other recording apps and try again."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Your phone's speech service needs a connection. You can still type reminders offline."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The speech service is still busy. Close other recording apps, then try again."
            else -> "Voice input stopped. Try again, or type your reminder."
        }
    }
}
