package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ReminderItem
import com.example.data.time.ReminderTime
import com.example.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderDialog(reminder: ReminderItem, onDismiss: () -> Unit, onSave: (ReminderItem) -> Unit,
    timeProtocol: String = "12h", feedback: @Composable () -> Unit = {}) {
    var title by rememberSaveable(reminder.id) { mutableStateOf(reminder.title) }
    var notes by rememberSaveable(reminder.id) { mutableStateOf(reminder.notes) }
    var category by rememberSaveable(reminder.id) { mutableStateOf(reminder.category) }
    var priority by rememberSaveable(reminder.id) { mutableStateOf(reminder.priority) }
    var recurrence by rememberSaveable(reminder.id) { mutableStateOf(reminder.recurrence) }
    var due by rememberSaveable(reminder.id) { mutableLongStateOf(reminder.dueDateMillis) }
    val context = LocalContext.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismiss, sheetState = sheet, containerColor = SurfaceContainerLow,
        contentColor = OnSurface, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Edit reminder", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth().testTag("edit_title_input"), label = { Text("What to remember") },
                isError = title.isBlank(), maxLines = 4, shape = RoundedCornerShape(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton({
                    val current = Calendar.getInstance().apply { timeInMillis = due }
                    DatePickerDialog(context, { _, year, month, day ->
                        due = Calendar.getInstance().apply { timeInMillis = due; set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, day) }.timeInMillis
                    }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show()
                }, Modifier.weight(1f).heightIn(min = 56.dp).testTag("edit_date_button"), shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(10.dp)) {
                    Icon(Icons.Outlined.CalendarToday, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(ReminderTime.format(due, "d MMM yyyy"))
                }
                OutlinedButton({
                    val current = Calendar.getInstance().apply { timeInMillis = due }
                    TimePickerDialog(context, { _, hour, minute ->
                        due = Calendar.getInstance().apply { timeInMillis = due; set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                    }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), timeProtocol == "24h").show()
                }, Modifier.weight(1f).heightIn(min = 56.dp).testTag("edit_time_button"), shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(10.dp)) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(ReminderTime.time(due, timeProtocol))
                }
            }
            OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Notes (optional)") }, maxLines = 5, shape = RoundedCornerShape(18.dp))
            OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Category") }, singleLine = true, shape = RoundedCornerShape(18.dp))
            Text("Priority", color = OnSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("LOW", "NORMAL", "HIGH").forEach { value -> QuietChip(value.lowercase().replaceFirstChar { it.uppercase() }, priority == value, { priority = value }) }
            }
            Text("Repeat", color = OnSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val values = listOf("NONE", "DAILY", "WEEKDAYS", "WEEKLY", "MONTHLY")
                val labels = listOf("Never", "Daily", "Weekdays", "Weekly", "Monthly")
                values.forEachIndexed { index, value -> QuietChip(labels[index], recurrence == value, { recurrence = value }) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onDismiss, Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(20.dp)) { Text("Cancel") }
                Button({ onSave(reminder.copy(title = title.trim(), notes = notes.trim(), category = category.trim().ifBlank { "General" },
                    priority = priority, recurrence = recurrence, dueDateMillis = due, dueTimeString = ReminderTime.label(due, timeProtocol))) },
                    Modifier.weight(1f).heightIn(min = 52.dp).testTag("save_edit_button"), enabled = title.isNotBlank(), shape = RoundedCornerShape(20.dp)) { Text("Save changes") }
            }
        }
        // Popups must belong to the sheet's window to stay above its scrim and controls.
        feedback()
    }
}
