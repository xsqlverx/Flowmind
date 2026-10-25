package com.example.data.time

import com.example.data.model.ReminderItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ReminderTime {
    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    fun addDays(millis: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = millis; add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis
    fun sameDay(first: Long, second: Long) = startOfDay(first) == startOfDay(second)
    fun format(millis: Long, pattern: String) = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    fun time(millis: Long, protocol: String = "12h") = format(millis, if (protocol == "24h") "HH:mm" else "h:mm a")
    fun dayLabel(millis: Long, now: Long = System.currentTimeMillis()): String = when {
        sameDay(millis, now) -> "Today"
        sameDay(millis, addDays(now, 1)) -> "Tomorrow"
        sameDay(millis, addDays(now, -1)) -> "Yesterday"
        else -> format(millis, "EEE, d MMM")
    }
    fun label(millis: Long, protocol: String = "12h") = "${dayLabel(millis)} · ${time(millis, protocol)}"
    fun recurrenceLabel(recurrence: String) = when (recurrence) {
        "DAILY" -> "Repeats every day"
        "WEEKDAYS" -> "Repeats on weekdays"
        "WEEKLY" -> "Repeats every week"
        "MONTHLY" -> "Repeats every month"
        else -> "Does not repeat"
    }
    fun snoozeMillis(setting: String): Long = when (setting) {
        "15 min" -> 15 * 60_000L
        "30 min" -> 30 * 60_000L
        "2 hours" -> 2 * 60 * 60_000L
        else -> 60 * 60_000L
    }
    fun filter(reminders: List<ReminderItem>, query: String, filter: String, day: Long?, now: Long): List<ReminderItem> = reminders.filter { item ->
        val matchesQuery = listOf(item.title, item.notes, item.tag, item.category, item.participant).any { it.contains(query.trim(), true) }
        val matchesFilter = when (filter) {
            "Today" -> sameDay(item.dueDateMillis, now)
            "Tomorrow" -> sameDay(item.dueDateMillis, addDays(now, 1))
            "This week" -> item.dueDateMillis >= startOfDay(now) && item.dueDateMillis < startOfDay(addDays(now, 7))
            "Priority" -> item.priority == "HIGH"
            else -> true
        }
        matchesQuery && matchesFilter && (day == null || sameDay(day, item.dueDateMillis))
    }.sortedBy { it.dueDateMillis }
    fun nextOccurrence(item: ReminderItem, now: Long = System.currentTimeMillis()): ReminderItem? {
        if (item.recurrence == "NONE") return null
        val calendar = Calendar.getInstance().apply { timeInMillis = item.dueDateMillis }
        do {
            when (item.recurrence) {
                "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKDAYS" -> do { calendar.add(Calendar.DAY_OF_YEAR, 1) } while (calendar.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY))
                "WEEKLY" -> calendar.add(Calendar.DAY_OF_YEAR, 7)
                "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
                else -> return null
            }
        } while (calendar.timeInMillis <= now)
        return item.copy(id = 0, dueDateMillis = calendar.timeInMillis, dueTimeString = label(calendar.timeInMillis),
            isCompleted = false, completedAtMillis = null, createdAtMillis = now)
    }
}
