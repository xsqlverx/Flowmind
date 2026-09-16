package com.example.data.model

data class ParsedEntity(
    val type: EntityType,
    val value: String,
    val iconName: String
)

enum class EntityType {
    DATE_TIME,
    CATEGORY,
    PRIORITY,
    PARTICIPANT,
    RECURRENCE
}

data class ParsedReminderResult(
    val cleanTitle: String,
    val rawInput: String,
    val entities: List<ParsedEntity>,
    val category: String,
    val tag: String,
    val priority: String,
    val dueTimeString: String,
    val dueDateMillis: Long,
    val participant: String,
    val recurrence: String,
    val parsingEngine: String = "Keyword Classifier (Local)",
    val needsLlmAssistance: Boolean = false,
    val notes: String = ""
)
