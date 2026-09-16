package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderItem
import com.example.ui.theme.Error
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.Outline
import com.example.ui.theme.Primary
import com.example.ui.theme.Secondary
import com.example.ui.theme.SecondaryFixedDim
import com.example.ui.theme.Surface
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.Tertiary
import com.example.ui.theme.TertiaryContainer
import com.example.ui.viewmodel.ReminderViewModel

@Composable
fun CompletedScreen(
    viewModel: ReminderViewModel
) {
    val completedReminders by viewModel.completedReminders.collectAsState()
    val selectedTab by viewModel.selectedCompletedTab.collectAsState()

    // Group items for display
    val todayReminders = remember(completedReminders) {
        completedReminders.filter { it.dueTimeString.contains("PM") || it.dueTimeString.contains("AM") || it.dueTimeString.contains("Today") }
    }
    val yesterdayReminders = remember(completedReminders) {
        completedReminders.filter { !todayReminders.contains(it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Title and Overview Banner Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                // Glow Halos
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .align(Alignment.TopEnd)
                        .blur(45.dp)
                        .background(Primary.copy(alpha = 0.12f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .align(Alignment.BottomStart)
                        .blur(35.dp)
                        .background(Tertiary.copy(alpha = 0.10f), CircleShape)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Tertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "ACTIVITY JOURNAL",
                                    color = Primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Completed",
                                    color = OnSurface,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.4).sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Synced badge
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceContainerHigh)
                                .border(0.5.dp, Tertiary.copy(alpha = 0.3f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Tertiary)
                                )
                                Text(
                                    text = "${completedReminders.size} Resolved",
                                    color = Tertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Quick Metrics Grid (3 columns)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Metric 1: Month
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceContainer.copy(alpha = 0.7f))
                                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Month",
                                        color = OnSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = "${completedReminders.size + 38}",
                                    color = OnSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "+14% vs last",
                                    color = Tertiary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Metric 2: On-Time
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceContainer.copy(alpha = 0.7f))
                                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "On-Time",
                                        color = OnSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Secondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = "96%",
                                    color = OnSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Optimal cadence",
                                    color = SecondaryFixedDim,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Metric 3: AI Streak
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceContainer.copy(alpha = 0.7f))
                                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AI Streak",
                                        color = OnSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Tertiary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = "5 Days",
                                    color = OnSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Personal Best",
                                    color = Tertiary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // Filter Segmented Tabs
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(SurfaceContainerLow)
                    .border(1.dp, Color(0x14FFFFFF), CircleShape)
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Recent", "By Category", "Archived").forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) Primary else Color.Transparent)
                                .clickable { viewModel.setCompletedTab(tab) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (tab == "Recent") "All Resolved" else tab,
                                color = if (isSelected) OnPrimary else OnSurfaceVariant,
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

        // Group 1: Earlier Today
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EARLIER TODAY",
                    color = OnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "${todayReminders.size} items",
                    color = Outline,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        items(todayReminders, key = { it.id }) { reminder ->
            CompletedItemCard(
                reminder = reminder,
                onRestore = { viewModel.toggleCompleted(reminder) },
                onDelete = { viewModel.deleteReminder(reminder) }
            )
        }

        // Group 2: Yesterday
        if (yesterdayReminders.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YESTERDAY",
                        color = OnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "${yesterdayReminders.size} items",
                        color = Outline,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            items(yesterdayReminders, key = { it.id }) { reminder ->
                CompletedItemCard(
                    reminder = reminder,
                    onRestore = { viewModel.toggleCompleted(reminder) },
                    onDelete = { viewModel.deleteReminder(reminder) }
                )
            }
        }

        // Quick Footer Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 0.5.dp,
                        color = Color(0x1AFFFFFF),
                        shape = RoundedCornerShape(0.dp)
                    )
                    .padding(top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Tertiary)
                    )
                    Text(
                        text = "Encrypted local SQLite store",
                        color = Outline,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text(
                    text = "Clear older than 30d",
                    color = Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { viewModel.clearCompletedOlderThan30Days() }
                        .padding(4.dp)
                        .testTag("clear_older_30d_button"),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
fun CompletedItemCard(
    reminder: ReminderItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceContainer.copy(alpha = 0.85f))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(18.dp))
            .padding(14.dp)
            .testTag("completed_item_${reminder.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Completed Checkmark Glyph
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(TertiaryContainer.copy(alpha = 0.25f))
                        .border(1.dp, Tertiary.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Resolved",
                        tint = Tertiary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        color = OnSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.LineThrough,
                        maxLines = 1,
                        softWrap = false
                    )

                    Row(
                        modifier = Modifier.padding(top = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Outline,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = reminder.dueTimeString,
                                color = Outline,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (reminder.tag.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainerHighest)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = reminder.tag,
                                    color = SecondaryFixedDim,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Restore and Delete action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onRestore,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .testTag("restore_btn_${reminder.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restore reminder",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .testTag("delete_completed_btn_${reminder.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Permanently delete",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
