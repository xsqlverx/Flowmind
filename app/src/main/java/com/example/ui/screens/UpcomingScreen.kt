package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderItem
import com.example.ui.theme.Error
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.OnTertiary
import com.example.ui.theme.Outline
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.Primary
import com.example.ui.theme.PrimaryFixedDim
import com.example.ui.theme.Secondary
import com.example.ui.theme.Surface
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.Tertiary
import com.example.ui.viewmodel.ReminderViewModel
import java.util.Locale

data class WeekDayChip(
    val dayName: String,
    val dayNumber: String,
    val dotColor: Color = Color.Transparent
)

@Composable
fun UpcomingScreen(
    viewModel: ReminderViewModel
) {
    val activeReminders by viewModel.activeReminders.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDayIndex by viewModel.selectedDayIndex.collectAsState()
    val selectedFilter by viewModel.selectedUpcomingFilter.collectAsState()

    val weekDays = remember {
        listOf(
            WeekDayChip("MON", "21", OutlineVariant),
            WeekDayChip("TUE", "22", Secondary),
            WeekDayChip("WED", "23", Tertiary),
            WeekDayChip("THU", "24", Tertiary),
            WeekDayChip("FRI", "25", Color.Transparent),
            WeekDayChip("SAT", "26", OutlineVariant)
        )
    }

    val filterOptions = remember {
        listOf("All", "Today", "Tomorrow", "This Week", "Priority")
    }

    // Filter reminders by search query and category pill
    val filteredReminders = remember(activeReminders, searchQuery, selectedFilter) {
        activeReminders.filter { item ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.notes.contains(searchQuery, ignoreCase = true) ||
                item.tag.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true)
            }

            val matchesFilter = when (selectedFilter) {
                "Today" -> item.dueTimeString.contains("Today", ignoreCase = true)
                "Tomorrow" -> item.dueTimeString.contains("Tomorrow", ignoreCase = true)
                "Priority" -> item.priority == "HIGH"
                "This Week" -> !item.dueTimeString.contains("Oct 31", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Header Section with Counter & Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Upcoming",
                        color = OnSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${activeReminders.size} Active",
                            color = Primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Flow button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceContainerLow)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Tertiary)
                            )
                            Text(
                                text = "Flow",
                                color = OnSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Quick Add button
                    IconButton(
                        onClick = {
                            viewModel.setTab("home")
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Primary)
                            .testTag("upcoming_quick_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Quick Add",
                            tint = OnPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Liquid Search Glass Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerLowest)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Outline,
                        modifier = Modifier.size(18.dp)
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                "Search reminders, tags, notes...",
                                color = Outline,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reminder_search_input")
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Horizontal Week Calendar Selector Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weekDays.forEachIndexed { index, day ->
                    val isSelected = selectedDayIndex == index
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Primary else SurfaceContainerLow)
                            .clickable { viewModel.setSelectedDayIndex(index) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = day.dayName,
                                color = if (isSelected) OnPrimary.copy(alpha = 0.85f) else Outline,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = day.dayNumber,
                                color = if (isSelected) OnPrimary else OnSurface,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) OnPrimary else day.dotColor)
                            )
                        }
                    }
                }
            }
        }

        // Filter Category Pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val countStr = when (filter) {
                        "All" -> " (${activeReminders.size})"
                        "Today" -> " (${activeReminders.count { it.dueTimeString.contains("Today") }})"
                        "Tomorrow" -> " (${activeReminders.count { it.dueTimeString.contains("Tomorrow") }})"
                        "Priority" -> ""
                        else -> ""
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) SurfaceContainerHighest else SurfaceContainerLow)
                            .border(
                                1.dp,
                                if (isSelected) Color(0x33FFFFFF) else Color.Transparent,
                                CircleShape
                            )
                            .clickable { viewModel.setUpcomingFilter(filter) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (filter == "Priority") {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Error)
                                )
                            }
                            Text(
                                text = "$filter$countStr",
                                color = if (isSelected) OnSurface else OnSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Timeline Reminder Cards List
        items(filteredReminders, key = { it.id }) { reminder ->
            UpcomingTimelineCard(
                reminder = reminder,
                onCompleteToggle = { viewModel.toggleCompleted(reminder) },
                onEditClick = { viewModel.setEditingReminder(reminder) },
                onRescheduleClick = { viewModel.rescheduleReminder(reminder) },
                onSnoozeClick = { viewModel.snoozeReminder(reminder) },
                onDeleteClick = { viewModel.deleteReminder(reminder) }
            )
        }

        // Subtle Bottom Ambient Status Badge
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Tertiary)
                        )
                        Text(
                            text = "FlowMind Sync Engine · All reminders up to date",
                            color = Outline,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
fun UpcomingTimelineCard(
    reminder: ReminderItem,
    onCompleteToggle: () -> Unit,
    onEditClick: () -> Unit,
    onRescheduleClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainer)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("reminder_card_${reminder.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Checkbox toggle + Content
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (reminder.isCompleted) Tertiary else SurfaceContainerHigh)
                            .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(8.dp))
                            .clickable { onCompleteToggle() }
                            .testTag("checkbox_toggle_${reminder.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (reminder.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = OnTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        // Badges Row (Priority + Time + Location / Recurrence)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (reminder.priority == "HIGH") {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(ErrorContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PriorityHigh,
                                            contentDescription = null,
                                            tint = Error,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "High Priority",
                                            color = Error,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            // Time Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (reminder.priority == "HIGH") Secondary else Tertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = reminder.dueTimeString,
                                    color = if (reminder.priority == "HIGH") Secondary else Tertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            if (reminder.location.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Outline,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = reminder.location,
                                        color = Outline,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            if (reminder.recurrence != "NONE") {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(SurfaceContainerLow)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Every weekday",
                                        color = Outline,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // Title
                        Text(
                            text = reminder.title,
                            color = OnSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                            textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 2
                        )

                        // Notes Description
                        if (reminder.notes.isNotEmpty()) {
                            Text(
                                text = reminder.notes,
                                color = OnSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 2.dp),
                                maxLines = 2
                            )
                        }
                    }
                }

                // Ambient dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.7f))
                )
            }

            // Footer Tags and Micro-Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (reminder.tag.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLow)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = reminder.tag,
                                color = Outline,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLow)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = reminder.category,
                            color = PrimaryFixedDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Action buttons: Snooze, Edit, Reschedule, Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Snooze +1h button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceContainerLow)
                            .clickable { onSnoozeClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("snooze_btn_${reminder.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Snooze",
                                tint = Secondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "+1h",
                                color = OnSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Edit
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerLow)
                            .testTag("edit_btn_${reminder.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit reminder",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Reschedule
                    IconButton(
                        onClick = onRescheduleClick,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerLow)
                            .testTag("reschedule_btn_${reminder.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = "Reschedule",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(ErrorContainer.copy(alpha = 0.3f))
                            .testTag("delete_btn_${reminder.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete reminder",
                            tint = Error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
