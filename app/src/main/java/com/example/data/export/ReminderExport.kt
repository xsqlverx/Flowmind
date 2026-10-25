package com.example.data.export

import com.example.data.model.ReminderItem
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ReminderExportPayload(val content: String, val fileName: String, val mimeType: String)

object ReminderExport {
    fun create(reminders: List<ReminderItem>, format: String): ReminderExportPayload {
        val extension = when (format) { "MD" -> "md"; ".ICS" -> "ics"; else -> "json" }
        val mime = when (format) { "MD" -> "text/markdown"; ".ICS" -> "text/calendar"; else -> "application/json" }
        val content = when (format) {
            "MD" -> buildString {
                appendLine("# FlowMind reminders"); appendLine()
                reminders.forEach {
                    appendLine("- [${if (it.isCompleted) "x" else " "}] ${it.title.replace("\n", " ")}")
                    appendLine("  - Due: ${it.dueTimeString}")
                    appendLine("  - Category: ${it.category}; Priority: ${it.priority}; Repeat: ${it.recurrence}")
                    if (it.notes.isNotBlank()) appendLine("  - Notes: ${it.notes.replace("\n", "\n    ")}")
                }
            }
            ".ICS" -> buildList {
                add("BEGIN:VCALENDAR"); add("VERSION:2.0"); add("PRODID:-//FlowMind//Reminders//EN"); add("CALSCALE:GREGORIAN")
                reminders.forEach {
                    add("BEGIN:VTODO"); add("UID:flowmind-${it.id}-${it.createdAtMillis}@local")
                    add("DTSTAMP:${utc(it.createdAtMillis)}"); add("DUE:${utc(it.dueDateMillis)}")
                    add("SUMMARY:${escape(it.title)}"); add("DESCRIPTION:${escape(it.notes)}")
                    add("CATEGORIES:${escape(it.category)}")
                    add("PRIORITY:${when (it.priority) { "HIGH" -> 1; "LOW" -> 9; else -> 5 }}")
                    add("STATUS:${if (it.isCompleted) "COMPLETED" else "NEEDS-ACTION"}")
                    it.completedAtMillis?.let { completed -> add("COMPLETED:${utc(completed)}") }
                    when (it.recurrence) {
                        "DAILY" -> add("RRULE:FREQ=DAILY")
                        "WEEKDAYS" -> add("RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR")
                        "WEEKLY" -> add("RRULE:FREQ=WEEKLY")
                        "MONTHLY" -> add("RRULE:FREQ=MONTHLY")
                    }
                    add("END:VTODO")
                }
                add("END:VCALENDAR")
            }.joinToString("\r\n", postfix = "\r\n") { foldLine(it) }
            else -> JSONArray().apply {
                reminders.forEach { reminder -> put(JSONObject().apply {
                    put("id", reminder.id); put("title", reminder.title); put("notes", reminder.notes)
                    put("category", reminder.category); put("tag", reminder.tag); put("priority", reminder.priority)
                    put("dueDateMillis", reminder.dueDateMillis); put("dueTimeString", reminder.dueTimeString)
                    put("recurrence", reminder.recurrence); put("participant", reminder.participant); put("location", reminder.location)
                    put("isCompleted", reminder.isCompleted); put("completedAtMillis", reminder.completedAtMillis ?: JSONObject.NULL)
                    put("isArchived", reminder.isArchived); put("createdAtMillis", reminder.createdAtMillis)
                }) }
            }.toString(2)
        }
        return ReminderExportPayload(content, "FlowMind-reminders.$extension", mime)
    }
    private fun utc(millis: Long) = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(millis))
    private fun escape(value: String) = value.replace("\\", "\\\\").replace("\r\n", "\n").replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,")
    private fun foldLine(line: String): String {
        val result = StringBuilder()
        var octets = 0
        var index = 0
        while (index < line.length) {
            val codepoint = line.codePointAt(index)
            val character = String(Character.toChars(codepoint))
            val bytes = character.toByteArray(Charsets.UTF_8).size
            if (octets + bytes > 75) { result.append("\r\n "); octets = 1 }
            result.append(character); octets += bytes; index += Character.charCount(codepoint)
        }
        return result.toString()
    }
}
