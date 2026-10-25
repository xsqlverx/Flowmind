package com.example

import com.example.data.model.ReminderItem
import com.example.data.time.ReminderTime
import com.example.data.nlp.NlpReminderParser
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class ReminderTimeTest {
    private val originalZone = TimeZone.getDefault()
    private val originalLocale = Locale.getDefault()
    @Before fun setup() { TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata")); Locale.setDefault(Locale.US) }
    @After fun restore() { TimeZone.setDefault(originalZone); Locale.setDefault(originalLocale) }
    private fun time(day: Int, hour: Int = 12, minute: Int = 0): Long = Calendar.getInstance().apply {
        clear(); set(2026, Calendar.OCTOBER, day, hour, minute)
    }.timeInMillis

    @Test fun `schedule filters timestamps even when stored labels are stale`() {
        val now = time(25)
        val today = ReminderItem(id = 1, title = "Read", dueDateMillis = time(25, 18), dueTimeString = "Tomorrow")
        val tomorrow = ReminderItem(id = 2, title = "Call", dueDateMillis = time(26, 9), dueTimeString = "Today")
        assertEquals(listOf(today), ReminderTime.filter(listOf(today, tomorrow), "", "Today", null, now))
        assertEquals(listOf(tomorrow), ReminderTime.filter(listOf(today, tomorrow), "", "All", time(26), now))
        assertEquals(listOf(tomorrow), ReminderTime.filter(listOf(today, tomorrow), "call", "All", null, now))
    }
    @Test fun `week filter spans calendar days and excludes next week boundary`() {
        val now = time(25)
        val inside = ReminderItem(id = 1, title = "Inside", dueDateMillis = time(31, 23, 59))
        val outside = inside.copy(id = 2, dueDateMillis = time(32, 0, 0))
        assertEquals(listOf(inside), ReminderTime.filter(listOf(outside, inside), "", "This week", null, now))
    }
    @Test fun `weekday recurrence skips weekends and missed occurrences`() {
        val friday = ReminderItem(id = 10, title = "Standup", dueDateMillis = time(23, 9), recurrence = "WEEKDAYS")
        val next = ReminderTime.nextOccurrence(friday, time(25))!!
        assertEquals(time(26, 9), next.dueDateMillis)
        assertEquals(0L, next.id)
        assertFalse(next.isCompleted)
        assertNull(next.completedAtMillis)
    }
    @Test fun `snooze settings map to actual durations`() {
        assertEquals(900_000L, ReminderTime.snoozeMillis("15 min"))
        assertEquals(1_800_000L, ReminderTime.snoozeMillis("30 min"))
        assertEquals(3_600_000L, ReminderTime.snoozeMillis("1 hour"))
        assertEquals(7_200_000L, ReminderTime.snoozeMillis("2 hours"))
    }
    @Test fun `24 hour format is applied to actual timestamp`() {
        assertEquals("18:30", ReminderTime.time(time(25, 18, 30), "24h"))
        assertEquals("6:30 PM", ReminderTime.time(time(25, 18, 30), "12h"))
    }
    @Test fun `time only in the past rolls forward rather than becoming an unscheduled overdue reminder`() {
        val result = NlpReminderParser.parse("Call dentist at 9am", time(25, 18))
        assertEquals(time(26, 9), result.dueDateMillis)
    }
    @Test fun `tomorrow at a bare hour retains both the date and the chosen hour`() {
        val result = NlpReminderParser.parse("Read tomorrow at 5", time(25, 12))
        assertEquals(time(26, 17), result.dueDateMillis)
    }
    @Test fun `next week resolves to the upcoming Monday`() {
        val result = NlpReminderParser.parse("Review next week", time(25, 12))
        assertEquals(time(26, 9), result.dueDateMillis)
    }
}
