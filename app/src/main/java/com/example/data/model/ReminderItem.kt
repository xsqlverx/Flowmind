package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val category: String = "General",
    val tag: String = "",
    val priority: String = "NORMAL", // HIGH, NORMAL, LOW
    val dueDateMillis: Long = System.currentTimeMillis(),
    val dueTimeString: String = "5:00 PM Today",
    val recurrence: String = "NONE", // NONE, DAILY, WEEKDAYS, WEEKLY, MONTHLY
    val participant: String = "",
    val location: String = "",
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val isArchived: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
