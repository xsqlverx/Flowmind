package com.example

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import com.example.voice.ReminderVoiceInput
import com.example.voice.SpeechEngine
import com.example.voice.SpeechEngineFactory
import com.example.voice.VoiceScheduler
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReminderVoiceInputTest {
    private class Clock : VoiceScheduler {
        override var now = 0L
        private val tasks = mutableListOf<Pair<Long, Runnable>>()
        override fun post(task: Runnable, delayMillis: Long) { tasks += (now + delayMillis) to task }
        override fun remove(task: Runnable) { tasks.removeAll { it.second === task } }
        fun advance(millis: Long = 0) {
            val end = now + millis
            while (true) {
                val next = tasks.minByOrNull { it.first } ?: break
                if (next.first > end) break
                tasks.remove(next)
                now = next.first
                next.second.run()
            }
            now = end
        }
    }
    private class Engine(val listener: RecognitionListener) : SpeechEngine {
        var starts = 0
        var cancels = 0
        var destroys = 0
        var failStart = false
        var failCancel = false
        override fun start(intent: Intent) { starts++; if (failStart) throw SecurityException() }
        override fun cancel() { cancels++; if (failCancel) throw IllegalStateException() }
        override fun destroy() { destroys++ }
        fun result(text: String) = listener.onResults(Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(text))
        })
    }
    private class Factory : SpeechEngineFactory {
        var available = true
        var failStart = false
        val engines = mutableListOf<Engine>()
        override fun isAvailable() = available
        override fun create(listener: RecognitionListener) = Engine(listener).also { it.failStart = failStart; engines += it }
    }
    private lateinit var clock: Clock
    private lateinit var factory: Factory
    private lateinit var voice: ReminderVoiceInput
    private val transcripts = mutableListOf<String>()
    private val errors = mutableListOf<String>()

    @Before fun setup() {
        clock = Clock(); factory = Factory()
        voice = ReminderVoiceInput(factory, clock, { transcripts += it }, { errors += it })
    }
    private fun start(): Engine {
        voice.start(); clock.advance(250)
        return factory.engines.last()
    }

    @Test fun consecutive_dictations_destroy_both_sessions_and_deliver_once_each() {
        val first = start()
        first.result("First reminder")
        assertFalse(voice.isActive)
        assertEquals(1, first.cancels)
        assertEquals(1, first.destroys)
        val second = start()
        assertNotSame(first, second)
        second.result("Second reminder")
        assertEquals(listOf("First reminder", "Second reminder"), transcripts)
        assertEquals(1, second.destroys)
        assertTrue(errors.isEmpty())
    }
    @Test fun old_callbacks_cannot_end_or_change_the_new_session() {
        val first = start(); first.result("First")
        val second = start()
        first.listener.onError(SpeechRecognizer.ERROR_CLIENT)
        first.listener.onEndOfSpeech()
        first.result("Duplicate")
        assertTrue(voice.isListening)
        second.result("Second")
        assertEquals(listOf("First", "Second"), transcripts)
        assertTrue(errors.isEmpty())
    }
    @Test fun busy_service_releases_and_retries_once_with_a_new_listener() {
        val first = start()
        first.listener.onError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
        assertEquals(1, first.destroys)
        assertTrue(voice.isProcessing)
        clock.advance(449)
        assertEquals(1, factory.engines.size)
        clock.advance(1)
        val retry = factory.engines.last()
        first.listener.onError(SpeechRecognizer.ERROR_CLIENT)
        retry.result("Recovered")
        assertEquals(listOf("Recovered"), transcripts)
        assertTrue(errors.isEmpty())
    }
    @Test fun persistent_busy_has_one_error_and_no_retry_loop() {
        val first = start()
        first.listener.onError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
        clock.advance(450)
        factory.engines.last().listener.onError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
        clock.advance(60_000)
        assertEquals(2, factory.engines.size)
        assertEquals(1, errors.size)
        assertFalse(voice.isActive)
    }
    @Test fun cancelling_pending_recovery_cannot_reopen_the_microphone() {
        val first = start()
        first.listener.onError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
        voice.cancel(); clock.advance(60_000)
        assertEquals(1, factory.engines.size)
        assertTrue(transcripts.isEmpty())
        assertTrue(errors.isEmpty())
    }
    @Test fun cancellation_and_release_ignore_late_results_and_clear_live_state() {
        val first = start()
        first.listener.onRmsChanged(8f)
        voice.cancel(); first.result("Must not append")
        assertFalse(voice.isActive)
        assertEquals(0f, voice.level, 0f)
        val second = start()
        voice.release(); second.result("Also stale")
        voice.start(); clock.advance(60_000)
        assertEquals(2, factory.engines.size)
        assertEquals(1, second.destroys)
        assertTrue(transcripts.isEmpty())
    }
    @Test fun missing_terminal_callback_times_out_releases_and_allows_next_attempt() {
        val first = start()
        first.listener.onEndOfSpeech()
        assertTrue(voice.isProcessing)
        clock.advance(30_000)
        assertFalse(voice.isActive)
        assertEquals(1, first.destroys)
        assertEquals(1, errors.size)
        start().result("After timeout")
        assertEquals(listOf("After timeout"), transcripts)
    }
    @Test fun permission_exception_releases_engine_and_next_start_can_succeed() {
        factory.failStart = true
        val first = start()
        assertEquals(1, first.destroys)
        assertFalse(voice.isActive)
        assertTrue(errors.single().contains("Microphone"))
        factory.failStart = false
        start().result("Allowed")
        assertEquals(listOf("Allowed"), transcripts)
    }
    @Test fun destroy_still_runs_if_the_service_throws_while_cancelling() {
        val first = start(); first.failCancel = true
        first.result("Captured")
        assertEquals(1, first.destroys)
        assertEquals(listOf("Captured"), transcripts)
    }
    @Test fun partials_and_empty_results_never_change_the_draft() {
        val first = start()
        first.listener.onPartialResults(Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("Partial"))
        })
        assertEquals("Partial", voice.partialTranscript)
        assertTrue(transcripts.isEmpty())
        first.result("  ")
        assertEquals("", voice.partialTranscript)
        assertEquals(1, first.destroys)
        assertEquals(1, errors.size)
    }
    @Test fun unavailable_service_never_creates_an_engine() {
        factory.available = false
        voice.start(); clock.advance()
        assertFalse(voice.isActive)
        assertTrue(factory.engines.isEmpty())
        assertEquals(1, errors.size)
    }
}
