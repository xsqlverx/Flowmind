package com.example.notification

/** Scheduling policy shared by normal alerts, restart recovery, and snooze. */
object NotificationTiming {
    const val CATCH_UP_WINDOW_MILLIS = 24 * 60 * 60_000L
    const val MINIMUM_DELAY_MILLIS = 1_500L

    fun triggerAt(dueMillis: Long, leadMinutes: Int, now: Long, snoozed: Boolean = false): Long? {
        if (dueMillis < now - CATCH_UP_WINDOW_MILLIS) return null
        val lead = if (snoozed) 0L else leadMinutes.coerceIn(0, 1440) * 60_000L
        return maxOf(dueMillis - lead, now + MINIMUM_DELAY_MILLIS)
    }
}
