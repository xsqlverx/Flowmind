package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.export.ReminderExport
import com.example.data.repository.FlowMindSettings
import com.example.notification.NotificationHelper
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ReminderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(viewModel: ReminderViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val reminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val latency by viewModel.gatewayLatency.collectAsStateWithLifecycle()
    val testing by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    fun hasPermission() = NotificationHelper.canPost(context)
    fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, if (settings.notificationHeadsUp) NotificationHelper.CHANNEL_URGENT_ID else NotificationHelper.CHANNEL_STANDARD_ID)
        } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        context.startActivity(intent)
    }
    fun hasExactPermission() = Build.VERSION.SDK_INT < 31 || (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    var notificationsGranted by remember { mutableStateOf(hasPermission()) }
    var exactGranted by remember { mutableStateOf(hasExactPermission()) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) {
            notificationsGranted = hasPermission(); exactGranted = hasExactPermission(); viewModel.refreshNotificationSchedule()
        } }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsGranted = hasPermission()
        viewModel.setNotificationsEnabled(granted)
        if (!granted) viewModel.showToast("Notifications are blocked. You can allow them in Android app settings.")
    }
    LaunchedEffect(settings.notificationHeadsUp) { notificationsGranted = hasPermission() }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) scope.launch {
            val payload = ReminderExport.create(reminders, settings.formatPipeline)
            val result = withContext(Dispatchers.IO) { runCatching {
                val stream = context.contentResolver.openOutputStream(uri) ?: error("Couldn't open the selected file")
                stream.bufferedWriter(Charsets.UTF_8).use { it.write(payload.content) }
            } }
            viewModel.showToast(if (result.isSuccess) "Saved ${reminders.size} reminders" else "Couldn't save the export. Please try another location.")
        }
    }
    SettingsContent(settings, reminders.size, latency, testing, notificationsGranted, exactGranted,
        onNotifications = { enabled ->
            if (enabled && !NotificationHelper.hasRuntimePermission(context) && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else if (enabled && !hasPermission()) openNotificationSettings()
            else viewModel.setNotificationsEnabled(enabled)
        }, onExactPermission = {
            if (Build.VERSION.SDK_INT >= 31) runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) }
                .onFailure { viewModel.showToast("Open Android settings → Special app access → Alarms & reminders.") }
        }, onAppSettings = { openNotificationSettings() },
        onTest = {
            if (!notificationsGranted) viewModel.showToast("Allow notifications first, then send a test.") else viewModel.sendTestNotification()
        }, onLead = viewModel::setNotificationLeadMinutes, onSound = viewModel::setNotificationSoundEnabled,
        onHeadsUp = viewModel::setNotificationHeadsUp, onQuiet = viewModel::setSmartQuietHours,
        onSnooze = viewModel::setSnoozeLatency, onTime = viewModel::setTimeProtocol,
        onProvider = viewModel::setLlmProvider, onSaveAi = { key, model, endpoint ->
            viewModel.setApiKey(key); viewModel.setCustomModelName(model); viewModel.setCustomEndpointUrl(endpoint); viewModel.showToast("AI settings saved")
        }, onFallback = viewModel::setOfflineFallback, onTestConnection = viewModel::testConnection,
        onFormat = viewModel::setFormatPipeline, onDestination = viewModel::setExportDestination,
        onExport = {
            if (settings.exportDestination == "Local") saveFile.launch(ReminderExport.create(reminders, settings.formatPipeline).fileName)
            else viewModel.exportReminders(context)
        })
}

@Composable
fun SettingsContent(settings: FlowMindSettings, count: Int = 0, latency: String = "Not tested", testing: Boolean = false,
    notificationsGranted: Boolean = true, exactGranted: Boolean = true, onNotifications: (Boolean) -> Unit = {},
    onExactPermission: () -> Unit = {}, onAppSettings: () -> Unit = {}, onTest: () -> Unit = {}, onLead: (Int) -> Unit = {},
    onSound: (Boolean) -> Unit = {}, onHeadsUp: (Boolean) -> Unit = {}, onQuiet: (Boolean) -> Unit = {},
    onSnooze: (String) -> Unit = {}, onTime: (String) -> Unit = {}, onProvider: (String) -> Unit = {},
    onSaveAi: (String, String, String) -> Unit = { _, _, _ -> }, onFallback: (Boolean) -> Unit = {}, onTestConnection: () -> Unit = {},
    onFormat: (String) -> Unit = {}, onDestination: (String) -> Unit = {}, onExport: () -> Unit = {}) {
    var aiExpanded by rememberSaveable { mutableStateOf(false) }
    var key by remember(settings.llmProvider, settings.apiKey) { mutableStateOf(settings.apiKey) }
    var model by remember(settings.llmProvider, settings.customModelName) { mutableStateOf(settings.customModelName) }
    var endpoint by remember(settings.llmProvider, settings.customEndpointUrl) { mutableStateOf(settings.customEndpointUrl) }
    var showKey by remember { mutableStateOf(false) }
    val dirty = key.trim() != settings.apiKey || model.trim() != settings.customModelName || endpoint.trim() != settings.customEndpointUrl
    val validEndpoint = endpoint.isBlank() || endpoint.trim().startsWith("https://")
    LazyColumn(Modifier.fillMaxSize().testTag("settings_list"), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)) {
        item { ScreenHeading("Make it yours.", "Just the things that matter.") }
        item {
            SettingsGroup("Reminders") {
                SettingSwitch("Notifications", if (notificationsGranted) "A nudge when it's time." else "Blocked in Android settings.", settings.notificationsEnabled && notificationsGranted, onNotifications)
                if (!notificationsGranted) TextButton(onAppSettings) { Text("Open Android settings") }
                SettingsDivider()
                Text("Remind me", fontSize = 14.sp, color = OnSurface, modifier = Modifier.padding(top = 8.dp))
                ChoiceRow(listOf("At the time", "5 min early", "15 min early", "30 min early"),
                    listOf(0, 5, 15, 30).indexOf(settings.notificationLeadMinutes)) { onLead(listOf(0, 5, 15, 30)[it]) }
                SettingsDivider()
                SettingSwitch("Sound & vibration", "Use your phone's notification preferences.", settings.notificationSoundEnabled, onSound)
                SettingSwitch("Heads-up alerts", "Show an alert over other apps.", settings.notificationHeadsUp, onHeadsUp)
                SettingSwitch("Quiet hours", "Silent alerts from 10 pm to 7 am.", settings.smartQuietHours, onQuiet)
                SettingsDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Precise timing", fontSize = 14.sp, color = OnSurface)
                        Text(if (exactGranted) "Alarms & reminders allowed" else "Android may delay your alerts", fontSize = 12.sp, color = OnSurfaceVariant)
                    }
                    TextButton(if (exactGranted) onTest else onExactPermission) { Text(if (exactGranted) "Send test" else "Allow") }
                }
            }
        }
        item {
            SettingsGroup("Your rhythm") {
                Text("Snooze for", fontSize = 14.sp, color = OnSurface)
                ChoiceRow(listOf("15 min", "30 min", "1 hour", "2 hours"), listOf("15 min", "30 min", "1 hour", "2 hours").indexOf(settings.snoozeLatency)) {
                    onSnooze(listOf("15 min", "30 min", "1 hour", "2 hours")[it])
                }
                SettingsDivider()
                Text("Time format", fontSize = 14.sp, color = OnSurface, modifier = Modifier.padding(top = 8.dp))
                ChoiceRow(listOf("12 hour", "24 hour"), if (settings.timeProtocol == "24h") 1 else 0) { onTime(if (it == 0) "12h" else "24h") }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().flowSurface().animateContentSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth().clickable { aiExpanded = !aiExpanded }.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("A little help from AI", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = OnSurface)
                        Text("Optional. Everyday reminders work offline.", fontSize = 12.sp, lineHeight = 19.sp, color = OnSurfaceVariant)
                    }
                    Icon(if (aiExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, "Expand AI settings", tint = OnSurfaceVariant)
                }
                AnimatedVisibility(aiExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Refine sends only your draft to the provider you choose. Voice input uses your phone's speech service and may need a connection.", fontSize = 12.sp, lineHeight = 20.sp, color = OnSurfaceVariant)
                        val providers = listOf("Groq", "NVIDIA", "OpenRouter", "Google Gemini", "OpenAI", "Anthropic", "Custom")
                        ChoiceRow(providers, providers.indexOf(settings.llmProvider)) { onProvider(providers[it]) }
                        OutlinedTextField(key, { key = it }, Modifier.fillMaxWidth().testTag("api_key_input"), label = { Text("API key") }, singleLine = true,
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = { IconButton({ showKey = !showKey }) { Icon(if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (showKey) "Hide API key" else "Show API key") } },
                            shape = RoundedCornerShape(16.dp))
                        OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("Model (optional)") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                        if (settings.llmProvider in listOf("Custom", "Local Ollama")) {
                            OutlinedTextField(endpoint, { endpoint = it }, Modifier.fillMaxWidth(), label = { Text("Endpoint URL") },
                                placeholder = { Text("https://…/v1/chat/completions") }, singleLine = true, isError = !validEndpoint,
                                supportingText = { Text(if (validEndpoint) "Use an HTTPS endpoint, including for local servers." else "Include https://") }, shape = RoundedCornerShape(16.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button({ onSaveAi(key, model, endpoint) }, enabled = validEndpoint && dirty, shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f)) { Text("Save") }
                            OutlinedButton(onTestConnection, enabled = !testing && !dirty, shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f)) { Text(if (testing) "Testing…" else "Test") }
                        }
                        Text(latency, color = OnSurfaceVariant, fontSize = 12.sp)
                        SettingSwitch("Local fallback", "Keep the local preview if refinement fails.", settings.offlineFallback, onFallback)
                    }
                }
            }
        }
        item {
            SettingsGroup("Keep a copy") {
                Text("$count reminders, ready to take with you.", color = OnSurfaceVariant, fontSize = 13.sp)
                ChoiceRow(listOf("JSON", "Markdown", "Calendar"), listOf("JSON", "MD", ".ICS").indexOf(settings.formatPipeline)) { onFormat(listOf("JSON", "MD", ".ICS")[it]) }
                ChoiceRow(listOf("Save a file", "Share / Dropbox"), if (settings.exportDestination == "Local") 0 else 1) { onDestination(if (it == 0) "Local" else "Dropbox") }
                OutlinedButton(onExport, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("export_button"), shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Outlined.FileDownload, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Export reminders")
                }
            }
        }
        item { Text("Made for a quieter mind.\nFlowMind · 1.0", color = Slate500, fontSize = 12.sp, lineHeight = 21.sp, modifier = Modifier.padding(vertical = 8.dp)) }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().flowSurface().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
        content()
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontSize = 14.sp, color = OnSurface)
            Text(subtitle, fontSize = 12.sp, lineHeight = 18.sp, color = OnSurfaceVariant)
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedThumbColor = Surface, checkedTrackColor = OnSurface,
            uncheckedThumbColor = OnSurfaceVariant, uncheckedTrackColor = SurfaceContainerHigh, uncheckedBorderColor = OutlineVariant))
    }
}

@Composable
private fun ChoiceRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label -> QuietChip(label, selected == index, { onSelect(index) }) }
    }
}

@Composable
private fun SettingsDivider() { HorizontalDivider(color = OutlineVariant.copy(alpha = .6f)) }
