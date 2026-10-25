package com.example.ui.feedback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FeedbackKind { INFO, SUCCESS, DELETE, ARCHIVE, RESTORE, SNOOZE, CALENDAR, ERROR }

data class FeedbackEvent(val id: Long, val message: String, val kind: FeedbackKind)

/** Events have identities: two identical confirmations still get two complete presentations. */
class FeedbackQueue {
    private var nextId = 0L
    private val pending = ArrayDeque<FeedbackEvent>()
    private val _current = MutableStateFlow<FeedbackEvent?>(null)
    val current = _current.asStateFlow()

    @Synchronized fun enqueue(message: String, kind: FeedbackKind = kindFor(message)) {
        if (message.isBlank()) return
        pending.addLast(FeedbackEvent(++nextId, message, kind))
        _current.value = pending.first()
    }

    @Synchronized fun finish(id: Long) {
        if (pending.firstOrNull()?.id != id) return
        pending.removeFirst()
        _current.value = pending.firstOrNull()
    }

    companion object {
        fun kindFor(message: String): FeedbackKind {
            val text = message.lowercase()
            return when {
                listOf("couldn't", "failed", "blocked", "allow ", "access", "unavailable", "busy", "timed out", "didn't catch", "no speech").any { it in text } -> FeedbackKind.ERROR
                "deleted" in text || text.startsWith("cleared ") -> FeedbackKind.DELETE
                "archived" in text -> FeedbackKind.ARCHIVE
                "restored" in text -> FeedbackKind.RESTORE
                "snoozed" in text -> FeedbackKind.SNOOZE
                "tomorrow" in text -> FeedbackKind.CALENDAR
                listOf("saved", "done", "refined", "test posted").any { it in text } -> FeedbackKind.SUCCESS
                else -> FeedbackKind.INFO
            }
        }
    }
}
