package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderItem
import com.example.data.time.ReminderTime
import com.example.ui.theme.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
fun rememberNow(): Long {
    val owner = LocalLifecycleOwner.current
    val now by produceState(System.currentTimeMillis(), owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { value = System.currentTimeMillis(); delay(30_000) }
        }
    }
    return now
}

fun Modifier.flowSurface(radius: Int = 28) = this
    .clip(RoundedCornerShape(radius.dp))
    .background(Brush.verticalGradient(listOf(SurfaceContainer, SurfaceContainerLow)))
    .drawWithCache {
        val rim = Brush.verticalGradient(listOf(Color.White.copy(alpha = .10f), Color.White.copy(alpha = .015f)))
        onDrawWithContent {
            drawContent()
            drawRoundRect(rim, cornerRadius = CornerRadius(radius.dp.toPx()), style = Stroke(1.dp.toPx()))
        }
    }

@Composable
fun RoundAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .94f else 1f, tween(120), label = "action press")
    IconButton(onClick, interactionSource = interaction,
        modifier = modifier.size(48.dp).graphicsLayer { scaleX = scale; scaleY = scale }.background(SurfaceContainer, CircleShape)) {
        Icon(icon, label, tint = OnSurface, modifier = Modifier.size(21.dp))
    }
}

@Composable
fun ScreenHeading(title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-1.3).sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        }
        action?.invoke()
    }
}

@Composable
fun SectionHeading(title: String, detail: String? = null, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
        if (detail != null) {
            if (onClick != null) TextButton(onClick) { Text(detail, color = OnSurfaceVariant) }
            else Text(detail, color = OnSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
fun QuietChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(selected, onClick, { Text(label, fontSize = 13.sp) }, modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(18.dp), colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent, labelColor = OnSurfaceVariant,
            selectedContainerColor = OnSurface, selectedLabelColor = Surface
        ), border = BorderStroke(1.dp, if (selected) OnSurface else OutlineVariant))
}

@Composable
fun EmptyState(title: String, description: String, icon: ImageVector = Icons.Outlined.CheckCircle) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(64.dp).background(SurfaceContainerLow, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = OnSurfaceVariant, modifier = Modifier.size(27.dp))
        }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = OnSurface)
        Text(description, fontSize = 14.sp, lineHeight = 22.sp, color = OnSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun ProgressRing(progress: Float, label: String, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(550), label = "daily progress")
    Box(modifier.size(72.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            drawArc(OutlineVariant, -90f, 360f, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            if (animated > 0f) drawArc(Primary, -90f, 360f * animated, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        Text(label, color = OnSurface, fontSize = 19.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ReminderRow(reminder: ReminderItem, timeProtocol: String = "12h", onComplete: () -> Unit, onEdit: () -> Unit,
    onSnooze: () -> Unit = {}, onReschedule: () -> Unit = {}, onDelete: () -> Unit = {}, modifier: Modifier = Modifier,
    onArchive: (() -> Unit)? = null) {
    var menu by remember { mutableStateOf(false) }
    SwipeToDismissReminderBox(modifier = modifier, onDelete = onDelete,
        onStartToEnd = if (reminder.isArchived) onComplete else onArchive,
        startToEndLabel = if (reminder.isArchived) "Restore" else "Archive",
        startToEndIcon = if (reminder.isArchived) Icons.Outlined.Restore else Icons.Outlined.Archive,
        startToEndContainerColor = SurfaceContainerHigh, startToEndContentColor = OnSurface,
        endToStartContainerColor = ErrorContainer, endToStartContentColor = Error) {
    Row(Modifier.fillMaxWidth().animateContentSize().clip(RoundedCornerShape(22.dp)).background(Surface)
        .clickable(onClick = onEdit).padding(vertical = 14.dp), verticalAlignment = Alignment.Top) {
        IconButton(onComplete, modifier = Modifier.size(48.dp).testTag("complete_${reminder.id}")) {
            Icon(if (reminder.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                if (reminder.isCompleted) "Restore ${reminder.title}" else "Complete ${reminder.title}",
                tint = if (reminder.isCompleted) Tertiary else Outline, modifier = Modifier.size(25.dp))
        }
        Column(Modifier.weight(1f).padding(start = 4.dp, top = 5.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(reminder.title, fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium,
                textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null,
                color = if (reminder.isCompleted) OnSurfaceVariant else OnSurface)
            val timestamp = if (reminder.isCompleted) reminder.completedAtMillis ?: reminder.dueDateMillis else reminder.dueDateMillis
            Text(listOf(ReminderTime.label(timestamp, timeProtocol), reminder.category,
                if (reminder.priority == "HIGH") "High priority" else "").filter { it.isNotBlank() }.joinToString("  ·  "),
                fontSize = 12.sp, lineHeight = 19.sp, color = if (!reminder.isCompleted && timestamp < System.currentTimeMillis()) Error else OnSurfaceVariant)
            if (reminder.recurrence != "NONE") Text(ReminderTime.recurrenceLabel(reminder.recurrence), fontSize = 12.sp, color = OnSurfaceVariant)
        }
        Box {
            IconButton({ menu = true }, modifier = Modifier.size(48.dp).testTag("more_${reminder.id}")) {
                Icon(Icons.Outlined.MoreHoriz, "Options for ${reminder.title}", tint = OnSurfaceVariant, modifier = Modifier.size(21.dp))
            }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text("Edit reminder") }, { menu = false; onEdit() }, leadingIcon = { Icon(Icons.Outlined.Edit, null) })
                if (!reminder.isArchived && onArchive != null) DropdownMenuItem({ Text("Archive") }, { menu = false; onArchive() }, leadingIcon = { Icon(Icons.Outlined.Archive, null) })
                if (!reminder.isCompleted) {
                    DropdownMenuItem({ Text("Snooze") }, { menu = false; onSnooze() }, leadingIcon = { Icon(Icons.Outlined.Snooze, null) })
                    DropdownMenuItem({ Text("Move to tomorrow") }, { menu = false; onReschedule() }, leadingIcon = { Icon(Icons.Outlined.Event, null) })
                }
                DropdownMenuItem({ Text("Delete", color = Error) }, { menu = false; onDelete() }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = Error) })
            }
        }
    }
    }
}
