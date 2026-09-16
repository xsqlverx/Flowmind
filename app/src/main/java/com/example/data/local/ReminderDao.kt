package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ReminderItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

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
