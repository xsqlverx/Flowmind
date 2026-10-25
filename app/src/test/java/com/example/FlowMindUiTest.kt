package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.example.data.model.ReminderItem
import com.example.data.nlp.NlpReminderParser
import com.example.data.repository.FlowMindSettings
import com.example.ui.screens.*
import com.example.ui.theme.FlowMindTheme
import com.example.ui.components.LocalMotionEnabled
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar

/** Runs in cloud CI. Fixtures never enter the production database. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp-xhdpi", sdk = [35])
class FlowMindUiTest {
    @get:Rule val compose = createComposeRule()
    private val now = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0) }.timeInMillis
    private val reminders = listOf(
        ReminderItem(id = 1, title = "Review the design direction", category = "Work", dueDateMillis = now + 3_600_000),
        ReminderItem(id = 2, title = "A walk, and a little fresh air", category = "Personal", dueDateMillis = now + 6 * 3_600_000),
        ReminderItem(id = 3, title = "Send the reading notes", category = "Study", isCompleted = true, completedAtMillis = now - 3_600_000)
    )
    private fun capture(name: String) {
        compose.waitForIdle()
        File("build/outputs/flowmind-review").mkdirs()
        compose.onRoot().captureRoboImage("build/outputs/flowmind-review/$name.png")
    }
    @Test fun today_screenshot() {
        compose.setContent { CompositionLocalProvider(LocalMotionEnabled provides false) { FlowMindTheme { FlowMindFrame("home", {}) { HomeContent(reminders, "", NlpReminderParser.parse(""), now = now) } } } }
        capture("today")
    }
    @Test fun schedule_screenshot() {
        compose.setContent { CompositionLocalProvider(LocalMotionEnabled provides false) { FlowMindTheme { FlowMindFrame("upcoming", {}) { ScheduleContent(reminders.filter { !it.isCompleted }, now = now) } } } }
        capture("schedule")
    }
    @Test fun completed_screenshot() {
        compose.setContent { CompositionLocalProvider(LocalMotionEnabled provides false) { FlowMindTheme { FlowMindFrame("completed", {}) { DoneContent(reminders.filter { it.isCompleted }, now = now) } } } }
        capture("done")
    }
    @Test fun settings_screenshot() {
        compose.setContent { CompositionLocalProvider(LocalMotionEnabled provides false) { FlowMindTheme { FlowMindFrame("settings", {}) { SettingsContent(FlowMindSettings(), 3) } } } }
        capture("settings")
    }
    @Test fun navigation_reaches_each_screen_and_exposes_selected_state() {
        compose.setContent {
            var tab by remember { mutableStateOf("home") }
            CompositionLocalProvider(LocalMotionEnabled provides false) { FlowMindTheme { FlowMindFrame(tab, { tab = it }) { current ->
                when (current) {
                    "upcoming" -> ScheduleContent(emptyList(), now = now)
                    "completed" -> DoneContent(emptyList(), now = now)
                    "settings" -> SettingsContent(FlowMindSettings())
                    else -> HomeContent(emptyList(), "", NlpReminderParser.parse(""), now = now)
                }
            } } }
        }
        compose.onNodeWithTag("tab_upcoming").performClick().assertIsSelected()
        compose.onNodeWithText("A little ahead.").assertIsDisplayed()
        compose.onNodeWithTag("tab_completed").performClick().assertIsSelected()
        compose.onNodeWithText("Look at you go.").assertIsDisplayed()
        compose.onNodeWithTag("tab_settings").performClick().assertIsSelected()
        compose.onNodeWithText("Make it yours.").assertIsDisplayed()
    }
    @Test fun blank_draft_cannot_be_saved_and_typed_draft_enables_saving() {
        var saves = 0
        compose.setContent {
            var draft by remember { mutableStateOf("") }
            CompositionLocalProvider(LocalMotionEnabled provides false) { FlowMindTheme { FlowMindFrame("home", {}) {
                HomeContent(emptyList(), draft, NlpReminderParser.parse(draft, now), now = now, onText = { draft = it }, onAdd = { saves++ })
            } } }
        }
        compose.onNodeWithTag("home_list").performScrollToNode(hasTestTag("add_reminder_button"))
        compose.onNodeWithTag("add_reminder_button").assertIsNotEnabled()
        compose.onNodeWithTag("composer_input").performTextInput("Read tomorrow at 5pm")
        compose.onNodeWithTag("add_reminder_button").assertIsEnabled().performClick()
        assertEquals(1, saves)
    }
    @Test
    @Config(qualifiers = "w320dp-h740dp-xhdpi", sdk = [35])
    fun narrow_large_text_keeps_composer_actions_separate() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                CompositionLocalProvider(LocalMotionEnabled provides false) { FlowMindTheme { FlowMindFrame("home", {}) {
                    HomeContent(reminders, "", NlpReminderParser.parse(""), now = now)
                } } }
            }
        }
        compose.onNodeWithTag("home_list").performScrollToNode(hasTestTag("add_reminder_button"))
        val mic = compose.onNodeWithTag("voice_button").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val save = compose.onNodeWithTag("add_reminder_button").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("Microphone and save controls overlap", mic.right <= save.left)
        capture("today-320-large-text")
    }
}
