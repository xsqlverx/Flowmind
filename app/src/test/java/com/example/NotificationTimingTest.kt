package com.example

import com.example.notification.NotificationTiming
import org.junit.Assert.*
import org.junit.Test

class NotificationTimingTest {
    private val now = 1_800_000_000_000L
    @Test fun `normal alerts use the selected lead time`() {
        assertEquals(now + 45 * 60_000L, NotificationTiming.triggerAt(now + 60 * 60_000L, 15, now))
    }
    @Test fun `missed early window catches up immediately while the reminder is still ahead`() {
        assertEquals(now + 1_500, NotificationTiming.triggerAt(now + 5 * 60_000L, 15, now))
    }
    @Test fun `recent overdue reminder catches up after restart`() {
        assertEquals(now + 1_500, NotificationTiming.triggerAt(now - 20 * 60_000L, 0, now))
    }
    @Test fun `old overdue reminders do not produce an alert avalanche`() {
        assertNull(NotificationTiming.triggerAt(now - NotificationTiming.CATCH_UP_WINDOW_MILLIS - 1, 0, now))
    }
    @Test fun `snooze keeps its full interval despite global early alert setting`() {
        assertEquals(now + 30 * 60_000L, NotificationTiming.triggerAt(now + 30 * 60_000L, 15, now, snoozed = true))
    }
}
