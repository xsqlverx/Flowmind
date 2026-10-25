package com.example

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.components.FeedbackTopPosition
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackPlacementTest {
    @Test fun capsule_sits_below_the_safe_top_inset_instead_of_at_screen_center() {
        val position = FeedbackTopPosition(topInset = 72, gap = 24).calculatePosition(
            IntRect(0, 0, 1080, 2400), IntSize(1080, 2400), LayoutDirection.Ltr, IntSize(840, 168))
        assertEquals(IntOffset(120, 96), position)
    }

    @Test fun opening_a_sheet_or_keyboard_does_not_move_the_capsule_down() {
        val provider = FeedbackTopPosition(topInset = 72, gap = 24)
        val screen = provider.calculatePosition(IntRect(0, 0, 1080, 2400), IntSize(1080, 2400),
            LayoutDirection.Ltr, IntSize(840, 168))
        val sheet = provider.calculatePosition(IntRect(0, 600, 1080, 1300), IntSize(1080, 1300),
            LayoutDirection.Ltr, IntSize(840, 336))
        assertEquals(screen, sheet)
    }

    @Test fun right_to_left_layout_keeps_the_capsule_horizontally_centered() {
        val position = FeedbackTopPosition(topInset = 0, gap = 12).calculatePosition(
            IntRect(700, 500, 1000, 800), IntSize(1000, 800), LayoutDirection.Rtl, IntSize(600, 112))
        assertEquals(IntOffset(200, 12), position)
    }
}
