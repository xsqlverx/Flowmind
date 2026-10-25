package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun UpcomingScreen(viewModel: ReminderViewModel) {
    val reminders by viewModel.activeReminders.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filter by viewModel.selectedUpcomingFilter.collectAsStateWithLifecycle()
    val day by viewModel.selectedDayMillis.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    ScheduleContent(reminders, query, filter, day, settings.timeProtocol, rememberNow(), viewModel::setSearchQuery,
        viewModel::setUpcomingFilter, viewModel::setSelectedDay, { viewModel.setTab("home") }, viewModel::setEditingReminder,
        viewModel::toggleCompleted, viewModel::snoozeReminder, viewModel::rescheduleReminder, viewModel::deleteReminder, viewModel::archiveReminder)
}

@Composable
fun ScheduleContent(reminders: List<ReminderItem>, query: String = "", filter: String = "All", selectedDay: Long? = null,
    timeProtocol: String = "12h", now: Long = System.currentTimeMillis(), onQuery: (String) -> Unit = {},
    onFilter: (String) -> Unit = {}, onDay: (Long?) -> Unit = {}, onAdd: () -> Unit = {}, onEdit: (ReminderItem) -> Unit = {},
    onComplete: (ReminderItem) -> Unit = {}, onSnooze: (ReminderItem) -> Unit = {}, onReschedule: (ReminderItem) -> Unit = {}, onDelete: (ReminderItem) -> Unit = {},
    onArchive: (ReminderItem) -> Unit = {}) {
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }
    val today = ReminderTime.startOfDay(now)
    val days = (0..6).map { ReminderTime.addDays(today, weekOffset * 7 + it) }
    val filtered = ReminderTime.filter(reminders, query, filter, selectedDay, now)
    val groups = filtered.groupBy { ReminderTime.startOfDay(it.dueDateMillis) }
    LazyColumn(Modifier.fillMaxSize().testTag("schedule_list"), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { ScreenHeading("A little ahead.", "${reminders.size} ${if (reminders.size == 1) "reminder" else "reminders"} in your schedule") {
            RoundAction(Icons.Outlined.Add, "Create a reminder", onAdd)
        } }
        item {
            Column(Modifier.fillMaxWidth().pageArrival(1).flowSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(ReminderTime.format(days.first(), "MMMM yyyy"), color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    IconButton({ weekOffset-- }, Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous week", tint = OnSurfaceVariant) }
                    IconButton({ weekOffset++ }, Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next week", tint = OnSurfaceVariant) }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    days.forEach { day ->
                        val selected = selectedDay?.let { ReminderTime.sameDay(day, it) } == true
                        val isToday = ReminderTime.sameDay(day, now)
                        Column(Modifier.width(48.dp).clip(RoundedCornerShape(22.dp))
                            .background(if (selected) OnSurface else if (isToday) SurfaceContainerHighest else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { onDay(if (selected) null else day) }.padding(vertical = 12.dp).testTag("day_$day"),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(ReminderTime.format(day, "EEE").take(2), fontSize = 11.sp, color = if (selected) Surface else OnSurfaceVariant)
                            Text(ReminderTime.format(day, "d"), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = if (selected) Surface else OnSurface)
                            Box(Modifier.size(4.dp).background(if (reminders.any { ReminderTime.sameDay(it.dueDateMillis, day) }) {
                                if (selected) Surface else Primary
                            } else androidx.compose.ui.graphics.Color.Transparent, CircleShape))
                        }
                    }
                }
            }
        }
        item {
            OutlinedTextField(query, onQuery, Modifier.fillMaxWidth().pageArrival(2).testTag("search_input"), placeholder = { Text("Find a reminder") },
                leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(20.dp)) }, singleLine = true, shape = RoundedCornerShape(22.dp),
                trailingIcon = if (query.isNotEmpty()) { { IconButton({ onQuery("") }) { Icon(Icons.Outlined.Close, "Clear search") } } } else null,
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = OutlineVariant, focusedBorderColor = Primary))
        }
        item {
            Row(Modifier.pageArrival(3).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Today", "Tomorrow", "This week", "Priority").forEach { label ->
                    QuietChip(label, selectedDay == null && filter == label, { onDay(null); onFilter(label) })
                }
            }
        }
        if (selectedDay != null) item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ReminderTime.format(selectedDay, "EEEE, d MMMM"), color = Primary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                TextButton({ onDay(null) }) { Text("All dates", color = OnSurfaceVariant) }
            }
        }
        if (filtered.isEmpty()) item {
            EmptyState("Nothing here yet.", if (query.isNotBlank()) "Try another search or clear your filters." else "Enjoy the space, or add something\nyou'd like to remember.", Icons.Outlined.EventAvailable)
        }
        groups.forEach { (day, dayReminders) ->
            item(key = "heading_$day") { SectionHeading(ReminderTime.dayLabel(day, now), "${dayReminders.size}") }
            items(dayReminders, key = { it.id }) { reminder ->
                ReminderRow(reminder, timeProtocol, { onComplete(reminder) }, { onEdit(reminder) }, { onSnooze(reminder) },
                    { onReschedule(reminder) }, { onDelete(reminder) }, Modifier.animateItem(), { onArchive(reminder) })
            }
        }
    }
}
