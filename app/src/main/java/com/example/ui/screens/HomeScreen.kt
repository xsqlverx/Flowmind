package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ParsedReminderResult
import com.example.data.model.ReminderItem
import com.example.data.time.ReminderTime
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ReminderViewModel
import com.example.voice.ReminderVoiceInput
import com.example.notification.NotificationHelper

@Composable
fun HomeScreen(viewModel: ReminderViewModel, onNavigateToUpcoming: () -> Unit) {
    val reminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val text by viewModel.composerText.collectAsStateWithLifecycle()
    val parsed by viewModel.parsedResult.collectAsStateWithLifecycle()
    val refining by viewModel.isLlmSynthesizing.collectAsStateWithLifecycle()
    val saving by viewModel.isSaving.collectAsStateWithLifecycle()
    val editing by viewModel.editingReminder.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val owner = LocalLifecycleOwner.current
    var alertsAvailable by remember { mutableStateOf(NotificationHelper.canPost(context)) }
    fun exactAlarmsAllowed() = Build.VERSION.SDK_INT < 31 || (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    var exactAllowed by remember { mutableStateOf(exactAlarmsAllowed()) }
    val voice = remember(context, viewModel) {
        ReminderVoiceInput(context, { transcript ->
            viewModel.updateComposerText(listOf(viewModel.composerText.value.trim(), transcript).filter { it.isNotBlank() }.joinToString(" "))
        }, viewModel::showFeedback)
    }
    DisposableEffect(owner, voice) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) voice.cancel()
            if (event == Lifecycle.Event.ON_RESUME) {
                alertsAvailable = NotificationHelper.canPost(context)
                exactAllowed = exactAlarmsAllowed()
                viewModel.refreshNotificationSchedule()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); voice.release() }
    }
    LaunchedEffect(editing?.id) { if (editing != null) voice.cancel() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voice.start() else viewModel.showFeedback("Microphone access wasn't granted. You can enable it in Android app settings.")
    }
    LaunchedEffect(settings.notificationHeadsUp) { alertsAvailable = NotificationHelper.canPost(context) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        alertsAvailable = NotificationHelper.canPost(context)
        if (granted) viewModel.refreshNotificationSchedule()
        else viewModel.showFeedback("Allow notifications in settings to receive reminder alerts.")
    }
    HomeContent(reminders, text, parsed, settings.timeProtocol, refining, saving, voice.isListening, voice.isProcessing,
        voice.partialTranscript, voice.level, rememberNow(), viewModel::updateComposerText,
        onVoice = {
            keyboard?.hide()
            if (voice.isActive) voice.cancel()
            else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) voice.start()
            else permission.launch(Manifest.permission.RECORD_AUDIO)
        }, onRefine = viewModel::synthesizeWithLlm, onAdd = {
            keyboard?.hide()
            viewModel.parseAndSetReminder()
            val prefs = context.getSharedPreferences("flowmind_settings_prefs", Context.MODE_PRIVATE)
            if (Build.VERSION.SDK_INT >= 33 && settings.notificationsEnabled && !NotificationHelper.hasRuntimePermission(context) &&
                !prefs.getBoolean("notification_permission_requested", false)) {
                prefs.edit().putBoolean("notification_permission_requested", true).apply()
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onSchedule = onNavigateToUpcoming, onEdit = viewModel::setEditingReminder, onComplete = viewModel::toggleCompleted,
        onSnooze = viewModel::snoozeReminder, onReschedule = viewModel::rescheduleReminder, onDelete = viewModel::deleteReminder,
        notificationWarning = when {
            !settings.notificationsEnabled -> "Reminder alerts are paused."
            !alertsAvailable -> "Notifications are off. Enable them to receive alerts."
            !exactAllowed -> "Alerts may be delayed. Allow precise alarm timing."
            else -> null
        }, onNotificationSettings = { viewModel.setTab("settings") }, onArchive = viewModel::archiveReminder)
}

@Composable
fun HomeContent(
    reminders: List<ReminderItem>, text: String, parsed: ParsedReminderResult, timeProtocol: String = "12h",
    refining: Boolean = false, saving: Boolean = false, listening: Boolean = false, processingVoice: Boolean = false,
    partialTranscript: String = "", voiceLevel: Float = 0f, now: Long = System.currentTimeMillis(),
    onText: (String) -> Unit = {}, onVoice: () -> Unit = {}, onRefine: () -> Unit = {}, onAdd: () -> Unit = {},
    onSchedule: () -> Unit = {}, onEdit: (ReminderItem) -> Unit = {}, onComplete: (ReminderItem) -> Unit = {},
    onSnooze: (ReminderItem) -> Unit = {}, onReschedule: (ReminderItem) -> Unit = {}, onDelete: (ReminderItem) -> Unit = {},
    notificationWarning: String? = null, onNotificationSettings: () -> Unit = {}, onArchive: (ReminderItem) -> Unit = {}
) {
    val active = reminders.filter { !it.isCompleted && !it.isArchived }.sortedBy { it.dueDateMillis }
    val today = active.filter { ReminderTime.sameDay(it.dueDateMillis, now) }
    val doneToday = reminders.count { it.isCompleted && it.completedAtMillis?.let { time -> ReminderTime.sameDay(time, now) } == true }
    val next = active.firstOrNull()
    val total = today.size + doneToday
    val scroll = rememberLazyListState()
    val nextVisible by remember { derivedStateOf { scroll.layoutInfo.visibleItemsInfo.any { it.key == "next_up" } } }
    LazyColumn(Modifier.fillMaxSize().testTag("home_list"), state = scroll, contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            ScreenHeading("Your day,\nuncluttered.", ReminderTime.format(now, "EEEE, d MMMM"))
        }
        if (notificationWarning != null) item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.NotificationsNone, null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                Text(notificationWarning, color = OnSurfaceVariant, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
                TextButton(onNotificationSettings) { Text("Set up", fontSize = 12.sp) }
            }
        }
        item(key = "next_up") {
            val ambientPhase = rememberAmbientPhase(nextVisible)
            Column(Modifier.fillMaxWidth().flowSurface().clickable(onClick = onSchedule).padding(22.dp)) {
                Row(Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (next != null) "NEXT UP" else "A LITTLE HEADSPACE", color = OnSurfaceVariant, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Medium)
                        Text(next?.title ?: "Nothing on your mind?\nKeep it that way.", fontSize = 20.sp, lineHeight = 28.sp,
                            color = OnSurface, fontWeight = FontWeight.Medium)
                        Text(next?.let { ReminderTime.label(it.dueDateMillis, timeProtocol) } ?: "Add a reminder when you need one.", color = Primary, fontSize = 12.sp, lineHeight = 19.sp)
                    }
                    ProgressRing(if (total == 0) 0f else doneToday.toFloat() / total, if (total == 0) "—" else "$doneToday/$total",
                        ambientPhase = ambientPhase)
                }
                Spacer(Modifier.height(14.dp))
                AmbientFlow(ambientPhase)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().flowSurface(28).animateContentSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("OFF YOUR MIND", color = OnSurfaceVariant, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.NorthEast, null, tint = OnSurfaceVariant, modifier = Modifier.size(15.dp))
                }
                BasicTextField(text, onText, Modifier.fillMaxWidth().heightIn(min = 85.dp).testTag("composer_input"),
                    textStyle = TextStyle(color = OnSurface, fontSize = 21.sp, lineHeight = 30.sp, letterSpacing = (-.3).sp),
                    cursorBrush = SolidColor(Primary), maxLines = 8, decorationBox = { input ->
                        Box { if (text.isEmpty()) Text("What do you need\nto remember?", color = Slate500, fontSize = 23.sp, lineHeight = 31.sp, letterSpacing = (-.5).sp); input() }
                    })
                AnimatedVisibility(text.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(parsed.cleanTitle, fontSize = 14.sp, color = OnSurface)
                        Text(ReminderTime.label(parsed.dueDateMillis, timeProtocol) + "  ·  " + parsed.category,
                            color = Primary, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.testTag("parsed_preview"))
                        if (parsed.recurrence != "NONE") Text(ReminderTime.recurrenceLabel(parsed.recurrence), color = OnSurfaceVariant, fontSize = 12.sp)
                    }
                }
                AnimatedVisibility(listening || processingVoice) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VoiceMeter(voiceLevel)
                        Text(partialTranscript.ifBlank { if (processingVoice) "Finishing your transcript…" else "Listening. Tap the mic to cancel." },
                            color = Primary, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f).testTag("voice_status"))
                    }
                }
                HorizontalDivider(color = OutlineVariant.copy(alpha = .6f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundAction(if (listening || processingVoice) Icons.Outlined.Stop else Icons.Outlined.MicNone,
                        if (listening || processingVoice) "Cancel voice input" else "Dictate a reminder", onVoice, Modifier.testTag("voice_button"))
                    TextButton(onRefine, enabled = text.isNotBlank() && !refining && !saving && !listening && !processingVoice,
                        contentPadding = PaddingValues(horizontal = 10.dp)) {
                        if (refining) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 1.5.dp, color = Primary)
                        else Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(16.dp), tint = OnSurfaceVariant)
                        Spacer(Modifier.width(6.dp)); Text("Refine", fontSize = 12.sp, color = OnSurfaceVariant)
                    }
                    Spacer(Modifier.weight(1f))
                    FilledIconButton(onAdd, enabled = text.isNotBlank() && parsed.cleanTitle.isNotBlank() && !saving && !refining && !listening && !processingVoice,
                        modifier = Modifier.size(48.dp).testTag("add_reminder_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = OnSurface, contentColor = Surface,
                            disabledContainerColor = SurfaceContainerHighest, disabledContentColor = Slate500)) {
                        if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Save reminder", Modifier.size(22.dp))
                    }
                }
            }
        }
        item { SectionHeading("On your radar", if (active.isEmpty()) null else "See all", onSchedule) }
        if (active.isEmpty()) item { EmptyState("Room to breathe.", "Your reminders will live here.\nType one above, or use your voice.", Icons.Outlined.WbSunny) }
        else items(active.take(3), key = { it.id }) { reminder ->
            ReminderRow(reminder, timeProtocol, { onComplete(reminder) }, { onEdit(reminder) },
                { onSnooze(reminder) }, { onReschedule(reminder) }, { onDelete(reminder) }, Modifier.animateItem(), { onArchive(reminder) })
        }
        if (active.isNotEmpty()) item {
            Text("$doneToday completed today · ${today.size} still to go", color = OnSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun VoiceMeter(level: Float) {
    val height by animateFloatAsState(level, tween(100), label = "voice level")
    Canvas(Modifier.size(28.dp, 24.dp)) {
        for (i in 0..4) {
            val amplitude = .22f + height * (if (i == 2) .78f else if (i % 2 == 0) .4f else .62f)
            val x = size.width * (i + .5f) / 5
            drawLine(Primary, Offset(x, size.height * (1 - amplitude) / 2), Offset(x, size.height * (1 + amplitude) / 2), 2.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}
