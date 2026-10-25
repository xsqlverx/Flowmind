package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.data.export.ReminderExport
import com.example.data.local.AppDatabase
import com.example.data.model.ParsedReminderResult
import com.example.data.model.ReminderItem
import com.example.data.nlp.LlmReminderClient
import com.example.data.nlp.NlpReminderParser
import com.example.data.repository.ReminderRepository
import com.example.data.time.ReminderTime
import com.example.notification.NotificationHelper
import com.example.notification.ReminderNotificationScheduler
import com.example.notification.ReminderScheduleResult
import com.example.notification.NotificationPostResult
import com.example.notification.NotificationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.util.HapticFeedbackHelper
import com.example.ui.feedback.FeedbackQueue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderViewModel(application: Application, private val savedState: SavedStateHandle = SavedStateHandle()) : AndroidViewModel(application) {
    private val feedbackQueue = FeedbackQueue()
    val feedback = feedbackQueue.current
    fun finishFeedback(id: Long) = feedbackQueue.finish(id)

    private val repository = ReminderRepository(AppDatabase.getDatabase(application).reminderDao(), application)
    init { NotificationHelper.initNotificationChannels(application); refreshNotificationSchedule() }
    val allReminders = repository.allReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeReminders = repository.activeReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val completedReminders = repository.completedReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedReminders = repository.archivedReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = repository.settings

    val currentTab = savedState.getStateFlow("tab", "home")
    fun setTab(tab: String) { if (tab in listOf("home", "upcoming", "completed", "settings")) savedState["tab"] = tab }
    val composerText = savedState.getStateFlow("draft", "")
    private val _parsedResult = MutableStateFlow(NlpReminderParser.parse(composerText.value))
    val parsedResult = _parsedResult.asStateFlow()
    private val _isLlmSynthesizing = MutableStateFlow(false)
    val isLlmSynthesizing = _isLlmSynthesizing.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()
    private var draftRevision = 0L
    fun updateComposerText(text: String) {
        draftRevision++
        savedState["draft"] = text
        _parsedResult.value = NlpReminderParser.parse(text)
    }
    fun applyPromptTemplate(text: String) = updateComposerText(text)

    fun synthesizeWithLlm() {
        if (_isLlmSynthesizing.value) return
        val text = composerText.value.trim()
        if (text.isBlank()) { showFeedback("Write a reminder first."); return }
        val configuration = settings.value
        val local = LlmReminderClient.isLocalEndpoint(configuration.llmProvider, configuration.customEndpointUrl)
        if (configuration.apiKey.isBlank() && !local) { showFeedback("Add your ${configuration.llmProvider} API key in settings to refine this draft."); return }
        val revision = draftRevision
        _isLlmSynthesizing.value = true
        viewModelScope.launch {
            try {
                val result = LlmReminderClient.parseWithLlm(text, configuration.llmProvider, configuration.apiKey, configuration.customEndpointUrl, configuration.customModelName)
                // A response belongs only to the draft and provider that requested it.
                if (revision != draftRevision || configuration != settings.value) return@launch
                if (result.isSuccess) {
                    _parsedResult.value = result.getOrThrow()
                    showFeedback("Preview refined. Check the details before saving.")
                } else {
                    if (configuration.offlineFallback) _parsedResult.value = NlpReminderParser.parse(text)
                    showFeedback("Couldn't refine this draft. ${if (configuration.offlineFallback) "Kept your local preview." else "Your draft is unchanged."}")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally { _isLlmSynthesizing.value = false }
        }
    }

    fun parseAndSetReminder() {
        if (_isSaving.value || _isLlmSynthesizing.value) return
        val text = composerText.value.trim()
        // Reparse relative dates on save; an AI preview is preserved only for the current draft.
        val preview = _parsedResult.value
        val result = if (preview.parsingEngine.contains("Keyword Classifier")) NlpReminderParser.parse(text) else preview
        if (text.isBlank() || result.cleanTitle.isBlank()) { showFeedback("Give your reminder a title first."); return }
        val revision = draftRevision
        val reminder = ReminderItem(title = result.cleanTitle, notes = result.notes, category = result.category, tag = result.tag,
            priority = result.priority, dueDateMillis = result.dueDateMillis, dueTimeString = ReminderTime.label(result.dueDateMillis, settings.value.timeProtocol),
            participant = result.participant, recurrence = result.recurrence)
        _isSaving.value = true
        viewModelScope.launch {
            try {
                val id = repository.insertReminder(reminder)
                val schedule = ReminderNotificationScheduler.scheduleReminder(getApplication(), reminder.copy(id = id), settings.value.notificationLeadMinutes)
                if (revision == draftRevision) updateComposerText("")
                HapticFeedbackHelper.triggerCreationHaptic(getApplication())
                showFeedback(when {
                    schedule == ReminderScheduleResult.FAILED -> "Reminder saved, but its alert couldn't be scheduled. Retry from settings."
                    !NotificationHelper.canPost(getApplication()) -> "Reminder saved. Allow notifications to receive alerts."
                    schedule == ReminderScheduleResult.APPROXIMATE -> "Reminder saved. Allow precise alarms for on-time alerts."
                    else -> "Reminder saved"
                })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                showFeedback("Couldn't save the reminder. Your draft is still here.")
            } finally { _isSaving.value = false }
        }
    }

    val searchQuery = savedState.getStateFlow("search", "")
    fun setSearchQuery(query: String) { savedState["search"] = query }
    val selectedDayMillis = savedState.getStateFlow<Long?>("day", null)
    fun setSelectedDay(day: Long?) { savedState["day"] = day }
    val selectedUpcomingFilter = savedState.getStateFlow("schedule_filter", "All")
    fun setUpcomingFilter(filter: String) { savedState["schedule_filter"] = filter }
    val selectedCompletedTab = savedState.getStateFlow("done_filter", "All")
    fun setCompletedTab(tab: String) { savedState["done_filter"] = tab }
    private val _editingReminder = MutableStateFlow<ReminderItem?>(null)
    val editingReminder = _editingReminder.asStateFlow()
    fun setEditingReminder(reminder: ReminderItem?) { _editingReminder.value = reminder }
    fun openReminder(id: Long) { viewModelScope.launch {
        repository.getReminderById(id)?.let {
            setTab(if (it.isCompleted) "completed" else "upcoming")
            setEditingReminder(it)
        }
    } }
    private val pendingIds = mutableSetOf<Long>()
    private fun updateReminder(item: ReminderItem, operation: suspend () -> Unit) {
        if (!pendingIds.add(item.id)) return
        viewModelScope.launch {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                showFeedback("Couldn't update this reminder. Please try again.")
            } finally { pendingIds.remove(item.id) }
        }
    }
    fun saveEditedReminder(item: ReminderItem) {
        if (item.title.isBlank()) { showFeedback("A reminder needs a title."); return }
        updateReminder(item) {
            val updated = repository.updateDetails(item) ?: return@updateReminder
            ReminderNotificationScheduler.scheduleReminder(getApplication(), updated, settings.value.notificationLeadMinutes)
            _editingReminder.value = null
            HapticFeedbackHelper.triggerCreationHaptic(getApplication())
            showFeedback("Changes saved")
        }
    }
    fun toggleCompleted(item: ReminderItem) = updateReminder(item) {
        val change = repository.toggleCompleted(item) ?: return@updateReminder
        if (change.updated.isCompleted) {
            HapticFeedbackHelper.triggerCompletionHaptic(getApplication())
            ReminderNotificationScheduler.cancelReminder(getApplication(), item.id)
            NotificationHelper.dismissNotification(getApplication(), item.id.toInt())
            change.next?.let { ReminderNotificationScheduler.scheduleReminder(getApplication(), it, settings.value.notificationLeadMinutes) }
            showFeedback(if (change.next == null) "Consider it done." else "Done. The next repeat is scheduled.")
        } else {
            withContext(Dispatchers.IO) { NotificationState(getApplication()).clear(item.id) }
            ReminderNotificationScheduler.scheduleReminder(getApplication(), change.updated, settings.value.notificationLeadMinutes)
            showFeedback("Reminder restored")
        }
    }
    fun deleteReminder(item: ReminderItem) = updateReminder(item) {
        repository.deleteReminder(item)
        HapticFeedbackHelper.triggerDismissHaptic(getApplication())
        withContext(Dispatchers.IO) { NotificationState(getApplication()).clear(item.id) }
        ReminderNotificationScheduler.cancelReminder(getApplication(), item.id)
        NotificationHelper.dismissNotification(getApplication(), item.id.toInt())
        showFeedback("Reminder deleted")
    }
    fun archiveReminder(item: ReminderItem) = updateReminder(item) {
        repository.archiveReminder(item) ?: return@updateReminder
        ReminderNotificationScheduler.cancelReminder(getApplication(), item.id)
        NotificationHelper.dismissNotification(getApplication(), item.id.toInt())
        HapticFeedbackHelper.triggerDismissHaptic(getApplication())
        showFeedback("Reminder archived")
    }
    fun restoreArchivedReminder(item: ReminderItem) = updateReminder(item) {
        val updated = repository.restoreArchivedReminder(item) ?: return@updateReminder
        withContext(Dispatchers.IO) { NotificationState(getApplication()).clear(item.id) }
        ReminderNotificationScheduler.scheduleReminder(getApplication(), updated, settings.value.notificationLeadMinutes)
        HapticFeedbackHelper.triggerCreationHaptic(getApplication())
        showFeedback("Reminder restored")
    }
    fun snoozeReminder(item: ReminderItem) = updateReminder(item) {
        val updated = repository.snoozeReminder(item, ReminderTime.snoozeMillis(settings.value.snoozeLatency)) ?: return@updateReminder
        withContext(Dispatchers.IO) { NotificationState(getApplication()).markSnoozed(item.id, updated.dueDateMillis) }
        ReminderNotificationScheduler.scheduleReminder(getApplication(), updated, 0)
        NotificationHelper.dismissNotification(getApplication(), item.id.toInt())
        showFeedback("Snoozed for ${settings.value.snoozeLatency}")
    }
    fun rescheduleReminder(item: ReminderItem) = updateReminder(item) {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val due = Calendar.getInstance().apply {
            timeInMillis = item.dueDateMillis
            set(Calendar.YEAR, tomorrow.get(Calendar.YEAR)); set(Calendar.MONTH, tomorrow.get(Calendar.MONTH)); set(Calendar.DAY_OF_MONTH, tomorrow.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
        val updated = repository.updateDetails(item.copy(dueDateMillis = due, dueTimeString = ReminderTime.label(due, settings.value.timeProtocol))) ?: return@updateReminder
        ReminderNotificationScheduler.scheduleReminder(getApplication(), updated, settings.value.notificationLeadMinutes)
        showFeedback("Moved to tomorrow")
    }
    fun clearCompletedOlderThan30Days() { viewModelScope.launch {
        val count = repository.clearCompletedOlderThan30Days()
        showFeedback(if (count == 0) "No older reminders to clear" else "Cleared $count older reminders")
    } }

    fun showFeedback(message: String) {
        feedbackQueue.enqueue(message)
    }
    fun setLlmProvider(provider: String) { repository.updateLlmProvider(provider); _gatewayLatency.value = "Not tested" }
    fun setApiKey(key: String) { repository.updateApiKey(key); _gatewayLatency.value = "Not tested" }
    fun setCustomEndpointUrl(url: String) { repository.updateCustomEndpointUrl(url); _gatewayLatency.value = "Not tested" }
    fun setCustomModelName(model: String) = repository.updateCustomModelName(model)
    fun setOfflineFallback(enabled: Boolean) = repository.updateOfflineFallback(enabled)
    fun setFormatPipeline(format: String) = repository.updateFormatPipeline(format)
    fun setExportDestination(destination: String) = repository.updateExportDestination(destination)
    fun setSnoozeLatency(latency: String) = repository.updateSnoozeLatency(latency)
    fun setSmartQuietHours(enabled: Boolean) = repository.updateSmartQuietHours(enabled)
    fun setTimeProtocol(protocol: String) = repository.updateTimeProtocol(protocol)
    fun refreshNotificationSchedule() { viewModelScope.launch {
        try { ReminderNotificationScheduler.rescheduleAllActiveReminders(getApplication()) }
        catch (error: Exception) { Log.e("FlowMindAlarms", "Couldn't refresh reminder alarms", error); showFeedback("Couldn't refresh reminder alerts. Please try again.") }
    } }
    fun setNotificationsEnabled(enabled: Boolean) { repository.updateNotificationsEnabled(enabled); refreshNotificationSchedule() }
    fun setNotificationLeadMinutes(minutes: Int) { repository.updateNotificationLeadMinutes(minutes); refreshNotificationSchedule() }
    fun setNotificationSoundEnabled(enabled: Boolean) = repository.updateNotificationSoundEnabled(enabled)
    fun setNotificationHeadsUp(enabled: Boolean) = repository.updateNotificationHeadsUp(enabled)
    fun sendTestNotification() {
        showFeedback(when (NotificationHelper.sendTestNotification(getApplication())) {
            NotificationPostResult.POSTED -> "Test posted. Check your notification shade."
            NotificationPostResult.BLOCKED -> "Notifications are blocked. Allow them in Android settings."
            else -> "Couldn't post a test notification. Check Android notification settings."
        })
    }

    private val _gatewayLatency = MutableStateFlow("Not tested")
    val gatewayLatency = _gatewayLatency.asStateFlow()
    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection = _isTestingConnection.asStateFlow()
    fun testConnection() {
        if (_isTestingConnection.value) return
        val configuration = settings.value
        _isTestingConnection.value = true
        _gatewayLatency.value = "Checking endpoint…"
        viewModelScope.launch {
            try {
                val result = LlmReminderClient.pingProvider(configuration.llmProvider, configuration.apiKey, configuration.customEndpointUrl)
                if (configuration == settings.value) _gatewayLatency.value = result.fold({ "Endpoint responded in ${it} ms" }, { "Connection failed: ${it.message.orEmpty().take(100)}" })
            } finally { _isTestingConnection.value = false }
        }
    }
    fun exportReminders(context: Context) {
        val payload = ReminderExport.create(allReminders.value, settings.value.formatPipeline)
        runCatching {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = payload.mimeType; putExtra(Intent.EXTRA_SUBJECT, payload.fileName); putExtra(Intent.EXTRA_TEXT, payload.content)
            }, "Share reminders").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { showFeedback("Couldn't open sharing. Try saving a file instead.") }
    }
}
