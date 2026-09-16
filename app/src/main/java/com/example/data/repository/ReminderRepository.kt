package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.ReminderDao
import com.example.data.model.ReminderItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FlowMindSettings(
    val llmProvider: String = "Groq",
    val apiKey: String = "",
    val customEndpointUrl: String = "",
    val customModelName: String = "",
    val isKeyVerified: Boolean = false,
    val offlineFallback: Boolean = true,
    val formatPipeline: String = "JSON",
    val exportDestination: String = "Local",
    val snoozeLatency: String = "1 hour",
    val smartQuietHours: Boolean = true,
    val timeProtocol: String = "12h",
    val notificationsEnabled: Boolean = true,
    val notificationLeadMinutes: Int = 0,
    val notificationSoundEnabled: Boolean = true,
    val notificationHeadsUp: Boolean = true
)

class ReminderRepository(
    private val reminderDao: ReminderDao,
    context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<FlowMindSettings> = _settings.asStateFlow()

    val allReminders: Flow<List<ReminderItem>> = reminderDao.getAllReminders()
    val activeReminders: Flow<List<ReminderItem>> = reminderDao.getActiveReminders()
    val completedReminders: Flow<List<ReminderItem>> = reminderDao.getCompletedReminders()
    val archivedReminders: Flow<List<ReminderItem>> = reminderDao.getArchivedReminders()

    suspend fun insertReminder(reminder: ReminderItem): Long = reminderDao.insertReminder(reminder)

    suspend fun updateReminder(reminder: ReminderItem) = reminderDao.updateReminder(reminder)

    suspend fun deleteReminder(reminder: ReminderItem) = reminderDao.deleteReminder(reminder)

    suspend fun deleteReminderById(id: Long) = reminderDao.deleteReminderById(id)

    suspend fun toggleCompleted(reminder: ReminderItem) {
        val updated = reminder.copy(
            isCompleted = !reminder.isCompleted,
            completedAtMillis = if (!reminder.isCompleted) System.currentTimeMillis() else null
        )
        reminderDao.updateReminder(updated)
    }

    suspend fun snoozeReminder(reminder: ReminderItem, additionalMillis: Long) {
        val newDue = reminder.dueDateMillis + additionalMillis
        val updated = reminder.copy(
            dueDateMillis = newDue,
            dueTimeString = "+1h Extended"
        )
        reminderDao.updateReminder(updated)
    }

    suspend fun getReminderById(id: Long): ReminderItem? = reminderDao.getReminderById(id)

    suspend fun getActiveRemindersList(): List<ReminderItem> = reminderDao.getActiveRemindersList()

    suspend fun clearCompletedOlderThan30Days(): Int {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 3600 * 1000)
        return reminderDao.deleteCompletedOlderThan(thirtyDaysAgo)
    }

    private fun loadSettings(): FlowMindSettings {
        val storedKey = prefs.getString("api_key", "") ?: ""
        return FlowMindSettings(
            llmProvider = prefs.getString("llm_provider", "Groq") ?: "Groq",
            apiKey = storedKey,
            customEndpointUrl = prefs.getString("custom_endpoint_url", "") ?: "",
            customModelName = prefs.getString("custom_model_name", "") ?: "",
            isKeyVerified = storedKey.isNotBlank(),
            offlineFallback = prefs.getBoolean("offline_fallback", true),
            formatPipeline = prefs.getString("format_pipeline", "JSON") ?: "JSON",
            exportDestination = prefs.getString("export_destination", "Local") ?: "Local",
            snoozeLatency = prefs.getString("snooze_latency", "1 hour") ?: "1 hour",
            smartQuietHours = prefs.getBoolean("smart_quiet_hours", true),
            timeProtocol = prefs.getString("time_protocol", "12h") ?: "12h",
            notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
            notificationLeadMinutes = prefs.getInt("notification_lead_minutes", 0),
            notificationSoundEnabled = prefs.getBoolean("notification_sound_enabled", true),
            notificationHeadsUp = prefs.getBoolean("notification_heads_up", true)
        )
    }

    fun updateLlmProvider(provider: String) {
        prefs.edit().putString("llm_provider", provider).apply()
        _settings.value = _settings.value.copy(llmProvider = provider)
    }

    fun updateApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("api_key", trimmed).apply()
        _settings.value = _settings.value.copy(apiKey = trimmed, isKeyVerified = trimmed.isNotBlank())
    }

    fun updateCustomEndpointUrl(url: String) {
        val trimmed = url.trim()
        prefs.edit().putString("custom_endpoint_url", trimmed).apply()
        _settings.value = _settings.value.copy(customEndpointUrl = trimmed)
    }

    fun updateCustomModelName(model: String) {
        val trimmed = model.trim()
        prefs.edit().putString("custom_model_name", trimmed).apply()
        _settings.value = _settings.value.copy(customModelName = trimmed)
    }

    fun updateOfflineFallback(enabled: Boolean) {
        prefs.edit().putBoolean("offline_fallback", enabled).apply()
        _settings.value = _settings.value.copy(offlineFallback = enabled)
    }

    fun updateFormatPipeline(format: String) {
        prefs.edit().putString("format_pipeline", format).apply()
        _settings.value = _settings.value.copy(formatPipeline = format)
    }

    fun updateExportDestination(destination: String) {
        prefs.edit().putString("export_destination", destination).apply()
        _settings.value = _settings.value.copy(exportDestination = destination)
    }

    fun updateSnoozeLatency(latency: String) {
        prefs.edit().putString("snooze_latency", latency).apply()
        _settings.value = _settings.value.copy(snoozeLatency = latency)
    }

    fun updateSmartQuietHours(enabled: Boolean) {
        prefs.edit().putBoolean("smart_quiet_hours", enabled).apply()
        _settings.value = _settings.value.copy(smartQuietHours = enabled)
    }

    fun updateTimeProtocol(protocol: String) {
        prefs.edit().putString("time_protocol", protocol).apply()
        _settings.value = _settings.value.copy(timeProtocol = protocol)
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _settings.value = _settings.value.copy(notificationsEnabled = enabled)
    }

    fun updateNotificationLeadMinutes(minutes: Int) {
        prefs.edit().putInt("notification_lead_minutes", minutes).apply()
        _settings.value = _settings.value.copy(notificationLeadMinutes = minutes)
    }

    fun updateNotificationSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notification_sound_enabled", enabled).apply()
        _settings.value = _settings.value.copy(notificationSoundEnabled = enabled)
    }

    fun updateNotificationHeadsUp(enabled: Boolean) {
        prefs.edit().putBoolean("notification_heads_up", enabled).apply()
        _settings.value = _settings.value.copy(notificationHeadsUp = enabled)
    }
}
