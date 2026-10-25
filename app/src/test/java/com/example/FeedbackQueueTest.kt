package com.example

import com.example.ui.feedback.FeedbackKind
import com.example.ui.feedback.FeedbackQueue
import org.junit.Assert.*
import org.junit.Test

class FeedbackQueueTest {
    @Test fun repeated_identical_messages_keep_distinct_ids_and_order() {
        val queue = FeedbackQueue()
        queue.enqueue("Reminder deleted")
        val first = queue.current.value!!
        queue.enqueue("Reminder deleted")
        queue.enqueue("Changes saved")
        assertEquals(first, queue.current.value)
        queue.finish(first.id)
        val second = queue.current.value!!
        assertEquals(first.message, second.message)
        assertNotEquals(first.id, second.id)
        queue.finish(second.id)
        assertEquals("Changes saved", queue.current.value!!.message)
        queue.finish(queue.current.value!!.id)
        assertNull(queue.current.value)
    }
    @Test fun late_animation_acknowledgement_cannot_consume_the_next_event() {
        val queue = FeedbackQueue()
        queue.enqueue("Reminder saved"); val first = queue.current.value!!
        queue.enqueue("Reminder archived")
        queue.finish(first.id); val second = queue.current.value
        queue.finish(first.id)
        assertEquals(second, queue.current.value)
    }
    @Test fun warnings_take_priority_over_success_words_and_actions_use_their_icons() {
        assertEquals(FeedbackKind.ERROR, FeedbackQueue.kindFor("Reminder saved. Allow notifications to receive alerts."))
        assertEquals(FeedbackKind.ERROR, FeedbackQueue.kindFor("Couldn't save the export."))
        assertEquals(FeedbackKind.DELETE, FeedbackQueue.kindFor("Reminder deleted"))
        assertEquals(FeedbackKind.ARCHIVE, FeedbackQueue.kindFor("Reminder archived"))
        assertEquals(FeedbackKind.SNOOZE, FeedbackQueue.kindFor("Snoozed for 10 minutes"))
        assertEquals(FeedbackKind.RESTORE, FeedbackQueue.kindFor("Reminder restored"))
        assertEquals(FeedbackKind.CALENDAR, FeedbackQueue.kindFor("Moved to tomorrow"))
        assertEquals(FeedbackKind.SUCCESS, FeedbackQueue.kindFor("Consider it done."))
    }
    @Test fun blank_messages_are_ignored_and_custom_messages_still_have_a_presentation() {
        val queue = FeedbackQueue()
        queue.enqueue("  "); assertNull(queue.current.value)
        queue.enqueue("Check your preview")
        assertEquals(FeedbackKind.INFO, queue.current.value!!.kind)
    }
}
