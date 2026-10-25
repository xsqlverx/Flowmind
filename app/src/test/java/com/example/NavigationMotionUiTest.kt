package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.nlp.NlpReminderParser
import com.example.data.repository.FlowMindSettings
import com.example.ui.components.*
import com.example.ui.screens.*
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
class NavigationMotionUiTest {
    @get:Rule val compose = createComposeRule()
    private val selections = mutableListOf<String>()

    private fun show(motion: Boolean = false, rtl: Boolean = false) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            var tab by remember { mutableStateOf("home") }
            CompositionLocalProvider(LocalMotionEnabled provides motion,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                FlowMindTheme { FlowMindFrame(tab, { tab = it; selections += it }) { current ->
                    when (current) {
                        "upcoming" -> ScheduleContent(emptyList())
                        "completed" -> DoneContent(emptyList())
                        "settings" -> SettingsContent(FlowMindSettings())
                        else -> HomeContent(emptyList(), "", NlpReminderParser.parse(""))
                    }
                } }
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
    }

    private fun tabCenter(tab: String): Offset {
        val dock = compose.onNodeWithTag("navigation_dock").fetchSemanticsNode().boundsInRoot
        return compose.onNodeWithTag("tab_$tab").fetchSemanticsNode().boundsInRoot.center - dock.topLeft
    }

    private fun holdAndMove(from: Offset, to: Offset) {
        compose.onNodeWithTag("navigation_dock").performTouchInput { down(from) }
        compose.mainClock.advanceTimeBy(650)
        compose.onNodeWithTag("navigation_dock").performTouchInput { advanceEventTime(650); moveTo(to) }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun release() {
        compose.onNodeWithTag("navigation_dock").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(900)
    }

    @Test fun held_bubble_tracks_the_drag_and_switches_only_after_release() {
        show()
        holdAndMove(tabCenter("home"), tabCenter("completed"))
        compose.onNodeWithTag("navigation_dock").assert(SemanticsMatcher.expectValue(NavigationDraggingKey, true))
        val position = compose.onNodeWithTag("navigation_dock").fetchSemanticsNode().config[NavigationDragPositionKey]
        assertTrue("The glass bubble must follow the finger", position in 1.8f..2.2f)
        compose.onNodeWithTag("tab_home").assertIsSelected()
        compose.onNodeWithTag("tab_completed").assertIsNotSelected()
        assertTrue(selections.isEmpty())
        File("build/outputs/flowmind-review").mkdirs()
        compose.onNodeWithTag("navigation_dock").captureRoboImage("build/outputs/flowmind-review/dock-during-drag.png")
        release()
        compose.onNodeWithTag("tab_completed").assertIsSelected()
        compose.onNodeWithText("Look at you go.").assertIsDisplayed()
        assertEquals(listOf("completed"), selections)
    }

    @Test fun cancelled_drag_returns_to_the_current_tab_without_navigation() {
        show()
        holdAndMove(tabCenter("home"), tabCenter("settings"))
        compose.onNodeWithTag("navigation_dock").performTouchInput { cancel() }
        compose.mainClock.advanceTimeBy(900)
        compose.onNodeWithTag("tab_home").assertIsSelected()
        compose.onNodeWithTag("navigation_dock").assert(SemanticsMatcher.expectValue(NavigationDraggingKey, false))
        assertEquals(0f, compose.onNodeWithTag("navigation_dock").fetchSemanticsNode().config[NavigationDragPositionKey], .01f)
        assertTrue(selections.isEmpty())
    }

    @Test fun bubble_remains_draggable_after_a_normal_tab_tap() {
        show()
        compose.onNodeWithTag("tab_settings").performClick()
        compose.mainClock.advanceTimeBy(900)
        holdAndMove(tabCenter("settings"), tabCenter("upcoming"))
        compose.onNodeWithTag("tab_settings").assertIsSelected()
        release()
        compose.onNodeWithTag("tab_upcoming").assertIsSelected()
        assertEquals(listOf("settings", "upcoming"), selections)
    }

    @Test fun dragging_in_right_to_left_layout_selects_the_tab_under_the_bubble() {
        show(rtl = true)
        holdAndMove(tabCenter("home"), tabCenter("completed"))
        release()
        compose.onNodeWithTag("tab_completed").assertIsSelected()
        assertEquals(listOf("completed"), selections)
    }

    @Test fun bubble_is_clamped_at_the_last_cell_when_dragged_to_the_edge() {
        show()
        val start = tabCenter("home")
        val dock = compose.onNodeWithTag("navigation_dock").fetchSemanticsNode().boundsInRoot
        holdAndMove(start, Offset(dock.width - 1f, start.y))
        assertEquals(3f, compose.onNodeWithTag("navigation_dock").fetchSemanticsNode().config[NavigationDragPositionKey], .01f)
        release()
        compose.onNodeWithTag("tab_settings").assertIsSelected()
    }

    @Test fun page_heading_settles_into_place_instead_of_appearing_at_its_final_position() {
        show(motion = true)
        compose.onNodeWithTag("tab_upcoming").performClick()
        compose.mainClock.advanceTimeBy(80)
        val entering = compose.onNodeWithText("A little ahead.").fetchSemanticsNode().boundsInRoot.top
        compose.mainClock.advanceTimeBy(800)
        val settled = compose.onNodeWithText("A little ahead.").fetchSemanticsNode().boundsInRoot.top
        assertTrue("The incoming heading should visibly settle upward", entering > settled + 6 * compose.density.density)
        compose.onNodeWithTag("search_input").assertExists()
    }

    @Test fun reduced_motion_reveals_the_final_page_without_spatial_travel() {
        show()
        compose.onNodeWithTag("tab_upcoming").performClick()
        compose.mainClock.advanceTimeByFrame()
        val before = compose.onNodeWithText("A little ahead.").fetchSemanticsNode().boundsInRoot.top
        compose.mainClock.advanceTimeBy(800)
        val after = compose.onNodeWithText("A little ahead.").fetchSemanticsNode().boundsInRoot.top
        assertEquals(before, after, .1f)
    }

    @Test fun tapping_back_on_the_bubble_during_its_travel_still_navigates() {
        show(motion = true)
        compose.onNodeWithTag("tab_settings").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("tab_home").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(900)
        compose.onNodeWithTag("tab_home").assertIsSelected()
        assertEquals(listOf("settings", "home"), selections)
    }
}
