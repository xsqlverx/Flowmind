package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.*
import com.example.ui.feedback.FeedbackEvent
import com.example.ui.feedback.FeedbackKind
import com.example.ui.feedback.FeedbackQueue
import com.example.data.model.ReminderItem
import com.example.ui.theme.FlowMindTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp-xhdpi", sdk = [35])
class FeedbackUiTest {
    @get:Rule val compose = createComposeRule()

    private fun phase(expected: FeedbackPhase) {
        compose.onNodeWithTag("app_feedback").assert(SemanticsMatcher.expectValue(FeedbackPhaseKey, expected))
    }
    @Test fun feedback_runs_icon_message_compact_and_upward_exit_in_order() {
        compose.mainClock.autoAdvance = false
        var finished = false
        compose.setContent { FlowMindTheme { Box(Modifier.fillMaxSize()) {
            FeedbackOverlay(FeedbackEvent(1, "Reminder deleted", FeedbackKind.DELETE)) { finished = true }
        } } }
        compose.waitForIdle()
        phase(FeedbackPhase.ICON)
        compose.mainClock.advanceTimeBy(700)
        phase(FeedbackPhase.MESSAGE)
        val before = compose.onNodeWithTag("app_feedback").fetchSemanticsNode().boundsInRoot
        File("build/outputs/flowmind-review").mkdirs()
        compose.onNodeWithTag("app_feedback").captureRoboImage("build/outputs/flowmind-review/feedback-top-expanded.png")
        compose.mainClock.advanceTimeBy(2_100)
        phase(FeedbackPhase.COMPACT)
        compose.mainClock.advanceTimeBy(400)
        phase(FeedbackPhase.EXIT)
        val after = compose.onNodeWithTag("app_feedback").fetchSemanticsNode().boundsInRoot
        assertTrue("The compact icon should leave upward", after.top < before.top)
        compose.mainClock.advanceTimeBy(700)
        compose.runOnIdle { assertTrue(finished) }
    }

    @Test fun identical_confirmations_are_presented_separately_in_still_mode() {
        compose.mainClock.autoAdvance = false
        val queue = FeedbackQueue().apply { enqueue("Reminder deleted"); enqueue("Reminder deleted") }
        val firstId = queue.current.value!!.id
        compose.setContent {
            val event by queue.current.collectAsState()
            CompositionLocalProvider(LocalMotionEnabled provides false) {
                FlowMindTheme { Box(Modifier.fillMaxSize()) { FeedbackOverlay(event, queue::finish) } }
            }
        }
        compose.waitForIdle()
        phase(FeedbackPhase.MESSAGE)
        compose.mainClock.advanceTimeBy(2_600)
        compose.runOnIdle { assertNotEquals(firstId, queue.current.value!!.id) }
        phase(FeedbackPhase.MESSAGE)
        compose.mainClock.advanceTimeBy(2_600)
        compose.runOnIdle { assertNull(queue.current.value) }
        compose.onNodeWithTag("app_feedback").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w320dp-h740dp-xhdpi", sdk = [35])
    fun long_error_is_fully_announced_and_wraps_with_large_text() {
        compose.mainClock.autoAdvance = false
        val text = "Reminder saved, but its alert couldn't be scheduled. Allow notifications in Android settings, then try again."
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalMotionEnabled provides false, LocalDensity provides Density(density.density, 1.5f)) {
                FlowMindTheme { Box(Modifier.fillMaxSize()) { FeedbackOverlay(FeedbackEvent(1, text, FeedbackKind.ERROR), {}) } }
            }
        }
        compose.waitForIdle()
        val feedback = compose.onNodeWithTag("app_feedback").assertIsDisplayed()
        feedback.assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(text)))
        val bounds = feedback.fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.height > 56 * compose.density.density)
        File("build/outputs/flowmind-review").mkdirs()
        feedback.captureRoboImage("build/outputs/flowmind-review/feedback-320-large-text.png")
    }

    @Test fun background_time_does_not_consume_feedback_before_the_user_returns() {
        compose.mainClock.autoAdvance = false
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle get() = registry
        }
        owner.registry.currentState = Lifecycle.State.RESUMED
        val queue = FeedbackQueue().apply { enqueue("Reminder saved") }
        val id = queue.current.value!!.id
        compose.setContent {
            val event by queue.current.collectAsState()
            CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalMotionEnabled provides false) {
                FlowMindTheme { Box(Modifier.fillMaxSize()) { FeedbackOverlay(event, queue::finish) } }
            }
        }
        compose.waitForIdle()
        phase(FeedbackPhase.MESSAGE)
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        compose.mainClock.advanceTimeBy(50)
        compose.onNodeWithTag("app_feedback").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(60_000)
        compose.runOnIdle { assertEquals(id, queue.current.value!!.id) }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeBy(50)
        phase(FeedbackPhase.MESSAGE)
        compose.mainClock.advanceTimeBy(3_000)
        compose.runOnIdle { assertNull(queue.current.value) }
    }

    @Test fun editing_errors_use_the_same_capsule_inside_the_sheet_window() {
        compose.mainClock.autoAdvance = false
        val text = "Couldn't update this reminder. Please try again."
        compose.setContent {
            var event by remember { mutableStateOf<FeedbackEvent?>(null) }
            CompositionLocalProvider(LocalMotionEnabled provides false) {
                FlowMindTheme {
                    EditReminderDialog(ReminderItem(id = 1, title = "Read"), {},
                        { event = FeedbackEvent(1, text, FeedbackKind.ERROR) }) {
                        FeedbackOverlay(event, {})
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithTag("save_edit_button").performScrollTo().performClick()
        compose.mainClock.advanceTimeByFrame()
        phase(FeedbackPhase.MESSAGE)
        compose.onNodeWithTag("app_feedback").assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(text)))
        File("build/outputs/flowmind-review").mkdirs()
        compose.onNodeWithTag("app_feedback").captureRoboImage("build/outputs/flowmind-review/feedback-edit-sheet.png")
    }
}
