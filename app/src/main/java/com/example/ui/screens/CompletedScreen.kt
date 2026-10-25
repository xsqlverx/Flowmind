package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ReminderItem
import com.example.data.time.ReminderTime
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ReminderViewModel

@Composable
fun CompletedScreen(viewModel: ReminderViewModel) {
    val reminders by viewModel.completedReminders.collectAsStateWithLifecycle()
    val archived by viewModel.archivedReminders.collectAsStateWithLifecycle()
    val tab by viewModel.selectedCompletedTab.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    DoneContent(reminders, tab, settings.timeProtocol, rememberNow(), viewModel::setCompletedTab, viewModel::toggleCompleted,
        viewModel::setEditingReminder, viewModel::deleteReminder, viewModel::clearCompletedOlderThan30Days,
        archived, viewModel::archiveReminder, viewModel::restoreArchivedReminder)
}

@Composable
fun DoneContent(reminders: List<ReminderItem>, filter: String = "All", timeProtocol: String = "12h", now: Long = System.currentTimeMillis(),
    onFilter: (String) -> Unit = {}, onRestore: (ReminderItem) -> Unit = {}, onEdit: (ReminderItem) -> Unit = {},
    onDelete: (ReminderItem) -> Unit = {}, onClear: () -> Unit = {}, archivedReminders: List<ReminderItem> = emptyList(),
    onArchive: (ReminderItem) -> Unit = {}, onRestoreArchived: (ReminderItem) -> Unit = {}) {
    var confirmClear by remember { mutableStateOf(false) }
    val doneToday = reminders.count { it.completedAtMillis?.let { time -> ReminderTime.sameDay(time, now) } == true }
    val sevenDaysAgo = ReminderTime.startOfDay(ReminderTime.addDays(now, -6))
    val thisWeek = reminders.count { (it.completedAtMillis ?: 0) >= sevenDaysAgo }
    val shown = if (filter == "Archived") archivedReminders.sortedByDescending { it.completedAtMillis } else reminders.filter { reminder ->
        val completed = reminder.completedAtMillis ?: 0
        when (filter) { "Today" -> ReminderTime.sameDay(completed, now); "This week", "Recent" -> completed >= sevenDaysAgo; else -> true }
    }.sortedByDescending { it.completedAtMillis }
    val groups = shown.groupBy { ReminderTime.startOfDay(it.completedAtMillis ?: it.dueDateMillis) }
    LazyColumn(Modifier.fillMaxSize().testTag("done_list"), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { ScreenHeading("Look at you go.", "A little less on your mind.") }
        item {
            Row(Modifier.fillMaxWidth().pageArrival(1).flowSurface().padding(24.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("$doneToday", fontSize = 48.sp, letterSpacing = (-2).sp, color = OnSurface, fontWeight = FontWeight.Medium)
                    Text("Completed today", fontSize = 12.sp, color = OnSurfaceVariant)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("$thisWeek", fontSize = 48.sp, letterSpacing = (-2).sp, color = Primary, fontWeight = FontWeight.Medium)
                    Text("In the last 7 days", fontSize = 12.sp, color = OnSurfaceVariant)
                }
            }
        }
        item {
            Row(Modifier.pageArrival(2).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Today", "This week", "Archived").forEach { QuietChip(it, filter == it, { onFilter(it) }) }
            }
        }
        if (shown.isEmpty()) item { EmptyState("Good things take a moment.", "Completed reminders appear here.\nYou can restore them whenever you need.", Icons.Outlined.DoneAll) }
        groups.forEach { (day, dayReminders) ->
            item(key = "done_heading_$day") { SectionHeading(ReminderTime.dayLabel(day, now), "${dayReminders.size}") }
            items(dayReminders, key = { it.id }) { reminder ->
                ReminderRow(reminder, timeProtocol, { if (reminder.isArchived) onRestoreArchived(reminder) else onRestore(reminder) }, { onEdit(reminder) },
                    onDelete = { onDelete(reminder) }, modifier = Modifier.animateItem(), onArchive = { onArchive(reminder) })
            }
        }
        if (reminders.isNotEmpty()) item {
            TextButton({ confirmClear = true }) { Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(16.dp), tint = OnSurfaceVariant)
                Spacer(Modifier.width(8.dp)); Text("Clear items older than 30 days", color = OnSurfaceVariant, fontSize = 12.sp) }
        }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("Clear older reminders?") },
        text = { Text("This permanently removes reminders completed more than 30 days ago.") },
        confirmButton = { TextButton({ confirmClear = false; onClear() }) { Text("Clear older items", color = Error) } },
        dismissButton = { TextButton({ confirmClear = false }) { Text("Keep them") } })
}
