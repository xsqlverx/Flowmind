package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ParsedReminderResult
import com.example.data.model.ReminderItem
import com.example.data.nlp.LlmReminderClient
import com.example.data.nlp.NlpReminderParser
import com.example.data.repository.FlowMindSettings
import com.example.data.repository.ReminderRepository
import com.example.notification.NotificationHelper
import com.example.notification.ReminderNotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReminderRepository
    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = ReminderRepository(db.reminderDao(), application)
        NotificationHelper.initNotificationChannels(application)
        ReminderNotificationScheduler.rescheduleAllActiveReminders(application)
    }

    val allReminders: StateFlow<List<ReminderItem>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeReminders: StateFlow<List<ReminderItem>> = repository.activeReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedReminders: StateFlow<List<ReminderItem>> = repository.completedReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<FlowMindSettings> = repository.settings

    // Navigation & Tabs
    private val _currentTab = MutableStateFlow("home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    // Composer State
    private val _composerText = MutableStateFlow("Remind me to submit the Q4 budget review tomorrow at 3:30pm with Sarah, high priority #finance")
    val composerText: StateFlow<String> = _composerText.asStateFlow()

    private val _parsedResult = MutableStateFlow(NlpReminderParser.parse(_composerText.value))
    val parsedResult: StateFlow<ParsedReminderResult> = _parsedResult.asStateFlow()

    private val _isVoiceListening = MutableStateFlow(false)
    val isVoiceListening: StateFlow<Boolean> = _isVoiceListening.asStateFlow()

    private val _isLlmSynthesizing = MutableStateFlow(false)
    val isLlmSynthesizing: StateFlow<Boolean> = _isLlmSynthesizing.asStateFlow()

    fun updateComposerText(newText: String) {
        _composerText.value = newText
        // The primary parser is high-speed, flexible keyword classification (catches 24/25 queries)
        _parsedResult.value = NlpReminderParser.parse(newText)
    }

    fun synthesizeWithLlm() {
        val text = _composerText.value.trim()
        if (text.isEmpty()) {
            showToast("Please enter a reminder prompt first")
            return
        }

        val provider = settings.value.llmProvider
        val apiKey = settings.value.apiKey
        val endpoint = settings.value.customEndpointUrl
        val model = settings.value.customModelName

        val isLocal = provider.lowercase(java.util.Locale.ROOT).contains("local") ||
                provider.lowercase(java.util.Locale.ROOT).contains("ollama") ||
                endpoint.contains("localhost") ||
                endpoint.contains("10.0.2.2")

        if (apiKey.isBlank() && !isLocal) {
            showToast("No API key for $provider! Configure key in Settings.")
            if (settings.value.offlineFallback) {
                val local = NlpReminderParser.parse(text)
                _parsedResult.value = local.copy(
                    parsingEngine = "Keyword Classifier (No API Key Configured)"
                )
            }
            return
        }

        _isLlmSynthesizing.value = true
        viewModelScope.launch {
            val result = LlmReminderClient.parseWithLlm(
                input = text,
                provider = provider,
                apiKey = apiKey,
                customEndpointUrl = endpoint,
                customModelName = model
            )
            if (result.isSuccess) {
                _parsedResult.value = result.getOrThrow()
                showToast("Synthesized via $provider Neural Engine ✨")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unknown error"
                if (settings.value.offlineFallback) {
                    val local = NlpReminderParser.parse(text)
                    _parsedResult.value = local.copy(
                        parsingEngine = "Keyword Classifier (Offline Fallback)"
                    )
                    showToast("Fallback to local Keywords: $err")
                } else {
                    showToast("LLM Error: $err")
                }
            }
            _isLlmSynthesizing.value = false
        }
    }

    fun toggleVoiceListening() {
        if (!_isVoiceListening.value) {
            _isVoiceListening.value = true
            val previous = _composerText.value
            _composerText.value = "Listening... Speak now."
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                _isVoiceListening.value = false
                _composerText.value = previous
            }
        } else {
            _isVoiceListening.value = false
        }
    }

    fun applyPromptTemplate(templateText: String) {
        updateComposerText(templateText)
    }

    fun parseAndSetReminder() {
        val result = _parsedResult.value
        if (result.cleanTitle.isBlank()) return

        val newReminder = ReminderItem(
            title = result.cleanTitle,
            notes = "Created via FlowMind Natural Language Synthesizer",
            category = result.category,
            tag = result.tag,
            priority = result.priority,
            dueDateMillis = result.dueDateMillis,
            dueTimeString = result.dueTimeString,
            participant = result.participant,
            recurrence = result.recurrence,
            isCompleted = false
        )

        viewModelScope.launch {
            val newId = repository.insertReminder(newReminder)
            val leadMin = settings.value.notificationLeadMinutes
            ReminderNotificationScheduler.scheduleReminder(
                getApplication(),
                newReminder.copy(id = newId),
                leadMin
            )
            showToast("Reminder Saved & Scheduled ✓")
            // Reset composer to a clean placeholder
            _composerText.value = ""
            _parsedResult.value = NlpReminderParser.parse("")
        }
    }

    // Upcoming Screen Search & Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _selectedDayIndex = MutableStateFlow(2) // Wednesday active by default
    val selectedDayIndex: StateFlow<Int> = _selectedDayIndex.asStateFlow()

    fun setSelectedDayIndex(index: Int) {
        _selectedDayIndex.value = index
    }

    private val _selectedUpcomingFilter = MutableStateFlow("All")
    val selectedUpcomingFilter: StateFlow<String> = _selectedUpcomingFilter.asStateFlow()

    fun setUpcomingFilter(filter: String) {
        _selectedUpcomingFilter.value = filter
    }

    // Completed Screen Filter
    private val _selectedCompletedTab = MutableStateFlow("Recent")
    val selectedCompletedTab: StateFlow<String> = _selectedCompletedTab.asStateFlow()

    fun setCompletedTab(tab: String) {
        _selectedCompletedTab.value = tab
    }

    // Dialog & Feedback
    private val _editingReminder = MutableStateFlow<ReminderItem?>(null)
    val editingReminder: StateFlow<ReminderItem?> = _editingReminder.asStateFlow()

    fun setEditingReminder(item: ReminderItem?) {
        _editingReminder.value = item
    }

    fun saveEditedReminder(item: ReminderItem) {
        viewModelScope.launch {
            repository.updateReminder(item)
            ReminderNotificationScheduler.scheduleReminder(
                getApplication(),
                item,
                settings.value.notificationLeadMinutes
            )
            _editingReminder.value = null
            showToast("Reminder updated")
        }
    }

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun showToast(msg: String) {
        _toastMessage.value = msg
        viewModelScope.launch {
            kotlinx.coroutines.delay(2400)
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }
    }

    // CRUD & Status Operations
    fun toggleCompleted(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.toggleCompleted(reminder)
            if (!reminder.isCompleted) {
                ReminderNotificationScheduler.cancelReminder(getApplication(), reminder.id)
                showToast("Completed reminder ✨")
            } else {
                ReminderNotificationScheduler.scheduleReminder(
                    getApplication(),
                    reminder.copy(isCompleted = false),
                    settings.value.notificationLeadMinutes
                )
                showToast("Reminder restored")
            }
        }
    }

    fun deleteReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            ReminderNotificationScheduler.cancelReminder(getApplication(), reminder.id)
            showToast("Reminder deleted")
        }
    }

    fun snoozeReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.snoozeReminder(reminder, 3600 * 1000L)
            val updated = reminder.copy(
                dueDateMillis = reminder.dueDateMillis + (3600 * 1000L),
                dueTimeString = "+1h Extended"
            )
            ReminderNotificationScheduler.scheduleReminder(
                getApplication(),
                updated,
                settings.value.notificationLeadMinutes
            )
            showToast("Snoozed for +1 hour ⏰")
        }
    }

    fun rescheduleReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            val oneDay = 24 * 3600 * 1000L
            val updated = reminder.copy(
                dueDateMillis = reminder.dueDateMillis + oneDay,
                dueTimeString = "Tomorrow · " + reminder.dueTimeString.substringAfter("·", "9:00 AM").trim()
            )
            repository.updateReminder(updated)
            ReminderNotificationScheduler.scheduleReminder(
                getApplication(),
                updated,
                settings.value.notificationLeadMinutes
            )
            showToast("Rescheduled to next slot 🗓️")
        }
    }

    fun clearCompletedOlderThan30Days() {
        viewModelScope.launch {
            val count = repository.clearCompletedOlderThan30Days()
            showToast(if (count > 0) "Cleared $count archived items" else "No items older than 30d")
        }
    }

    // Settings actions
    fun setLlmProvider(provider: String) = repository.updateLlmProvider(provider)
    fun setApiKey(key: String) = repository.updateApiKey(key)
    fun setCustomEndpointUrl(url: String) = repository.updateCustomEndpointUrl(url)
    fun setCustomModelName(model: String) = repository.updateCustomModelName(model)
    fun setOfflineFallback(enabled: Boolean) = repository.updateOfflineFallback(enabled)
    fun setFormatPipeline(format: String) = repository.updateFormatPipeline(format)
    fun setExportDestination(dest: String) = repository.updateExportDestination(dest)
    fun setSnoozeLatency(latency: String) = repository.updateSnoozeLatency(latency)
    fun setSmartQuietHours(enabled: Boolean) = repository.updateSmartQuietHours(enabled)
    fun setTimeProtocol(protocol: String) = repository.updateTimeProtocol(protocol)

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.updateNotificationsEnabled(enabled)
        if (enabled) {
            ReminderNotificationScheduler.rescheduleAllActiveReminders(getApplication())
            showToast("Notifications Enabled & Rescheduled")
        } else {
            showToast("Notifications Disabled")
        }
    }

    fun setNotificationLeadMinutes(minutes: Int) {
        repository.updateNotificationLeadMinutes(minutes)
        ReminderNotificationScheduler.rescheduleAllActiveReminders(getApplication())
        val desc = if (minutes == 0) "at exact due time" else "$minutes min before due time"
        showToast("Alerts set to $desc")
    }

    fun setNotificationSoundEnabled(enabled: Boolean) {
        repository.updateNotificationSoundEnabled(enabled)
        showToast(if (enabled) "Notification Sound & Haptics ON" else "Notification Sound Muted")
    }

    fun setNotificationHeadsUp(enabled: Boolean) {
        repository.updateNotificationHeadsUp(enabled)
        showToast(if (enabled) "High-Priority Heads-Up Banner ON" else "Standard Priority Banners")
    }

    fun sendTestNotification() {
        NotificationHelper.sendTestNotification(getApplication())
        showToast("Dispatched Test Notification 🔔")
    }

    // Latency Ping Test
    private val _gatewayLatency = MutableStateFlow("Ready to test")
    val gatewayLatency: StateFlow<String> = _gatewayLatency.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    fun testConnection() {
        _isTestingConnection.value = true
        _gatewayLatency.value = "Pinging..."
        viewModelScope.launch {
            val provider = settings.value.llmProvider
            val apiKey = settings.value.apiKey
            val endpoint = settings.value.customEndpointUrl
            val result = LlmReminderClient.pingProvider(provider, apiKey, endpoint)
            if (result.isSuccess) {
                val duration = result.getOrThrow()
                val latencyText = "${duration}ms ✓"
                _gatewayLatency.value = latencyText
                showToast("$provider Endpoint Latency: $latencyText")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unreachable"
                _gatewayLatency.value = "Error: ${err.take(30)}"
                showToast("Connection issue: $err")
            }
            _isTestingConnection.value = false
        }
    }

    // Export Reminders
    fun exportReminders(context: Context) {
        val list = allReminders.value
        val format = settings.value.formatPipeline

        val content = when (format) {
            "JSON" -> {
                val array = JSONArray()
                list.forEach { item ->
                    val obj = JSONObject()
                    obj.put("id", item.id)
                    obj.put("title", item.title)
                    obj.put("notes", item.notes)
                    obj.put("category", item.category)
                    obj.put("tag", item.tag)
                    obj.put("priority", item.priority)
                    obj.put("dueTimeString", item.dueTimeString)
                    obj.put("isCompleted", item.isCompleted)
                    array.put(obj)
                }
                array.toString(2)
            }
            "MD" -> {
                buildString {
                    appendLine("# FlowMind Reminders Export")
                    appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}")
                    appendLine()
                    list.forEach {
                        val status = if (it.isCompleted) "[x]" else "[ ]"
                        appendLine("- $status **${it.title}** (${it.category}) ${it.tag}")
                        if (it.notes.isNotEmpty()) appendLine("  > ${it.notes}")
                        appendLine("  Due: ${it.dueTimeString}")
                    }
                }
            }
            ".ICS" -> {
                buildString {
                    appendLine("BEGIN:VCALENDAR")
                    appendLine("VERSION:2.0")
                    appendLine("PRODID:-//FlowMind AI//Reminders//EN")
                    list.forEach {
                        appendLine("BEGIN:VTODO")
                        appendLine("SUMMARY:${it.title}")
                        appendLine("DESCRIPTION:${it.notes}")
                        appendLine("STATUS:${if (it.isCompleted) "COMPLETED" else "NEEDS-ACTION"}")
                        appendLine("END:VTODO")
                    }
                    appendLine("END:VCALENDAR")
                }
            }
            else -> {
                list.joinToString("\n") { "${if (it.isCompleted) "✓" else "○"} ${it.title} - ${it.dueTimeString} ${it.tag}" }
            }
        }

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "FlowMind Reminders Export ($format)")
                putExtra(Intent.EXTRA_TEXT, content)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Export Reminders via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            showToast("${list.size} Reminders Exported!")
        } catch (e: Exception) {
            showToast("Export failed: ${e.message}")
        }
    }
}
