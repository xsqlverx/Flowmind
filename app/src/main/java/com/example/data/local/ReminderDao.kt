package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.example.data.time.ReminderTime
import com.example.data.model.ReminderItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Transaction
    suspend fun changeCompletion(id: Long, expectedCompleted: Boolean, now: Long): CompletionChange? {
        val current = getReminderById(id) ?: return null
        if (current.isArchived || current.isCompleted != expectedCompleted) return null
        val updated = current.copy(isCompleted = !expectedCompleted, completedAtMillis = if (!expectedCompleted) now else null)
        updateReminder(updated)
        val next = if (updated.isCompleted) ReminderTime.nextOccurrence(current, now) else null
        val savedNext = next?.let { it.copy(id = insertReminder(it)) }
        return CompletionChange(updated, savedNext)
    }

    @Transaction
    suspend fun snoozeActiveReminder(id: Long, expectedDue: Long, durationMillis: Long, now: Long): ReminderItem? {
        val current = getReminderById(id) ?: return null
        if (current.isCompleted || current.isArchived || current.dueDateMillis != expectedDue) return null
        val due = now + durationMillis
        val updated = current.copy(dueDateMillis = due, dueTimeString = ReminderTime.label(due))
        updateReminder(updated)
        return updated
    }

    @Transaction
    suspend fun updateDetails(item: ReminderItem): ReminderItem? {
        val current = getReminderById(item.id) ?: return null
        // Editing an open sheet must not undo a completion from the notification shade.
        val updated = item.copy(isCompleted = current.isCompleted, completedAtMillis = current.completedAtMillis,
            isArchived = current.isArchived, createdAtMillis = current.createdAtMillis)
        updateReminder(updated)
        return updated
    }

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): ReminderItem?

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isArchived = 0 ORDER BY dueDateMillis ASC")
    suspend fun getActiveRemindersList(): List<ReminderItem>

    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, dueDateMillis ASC")
    fun getAllReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isArchived = 0 ORDER BY dueDateMillis ASC")
    fun getActiveReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 AND isArchived = 0 ORDER BY completedAtMillis DESC")
    fun getCompletedReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders WHERE isArchived = 1 ORDER BY completedAtMillis DESC")
    fun getArchivedReminders(): Flow<List<ReminderItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderItem>)

    @Update
    suspend fun updateReminder(reminder: ReminderItem)

    @Delete
    suspend fun deleteReminder(reminder: ReminderItem)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("DELETE FROM reminders WHERE isCompleted = 1 AND completedAtMillis < :cutoffMillis")
    suspend fun deleteCompletedOlderThan(cutoffMillis: Long): Int

    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM reminders WHERE isCompleted = 1")
    suspend fun getCompletedCount(): Int
}

data class CompletionChange(val updated: ReminderItem, val next: ReminderItem?)
