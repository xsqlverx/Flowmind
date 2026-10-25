package com.example

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ReminderViewModel

class MainActivity : ComponentActivity() {
    private var reminderFromNotification by mutableLongStateOf(-1L)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        reminderFromNotification = if (savedInstanceState == null) intent.getLongExtra("reminder_id", -1L) else -1L
        setContent { FlowMindTheme {
            val model: ReminderViewModel = viewModel()
            LaunchedEffect(reminderFromNotification) {
                if (reminderFromNotification > 0) { model.openReminder(reminderFromNotification); reminderFromNotification = -1L }
            }
            FlowMindApp(model)
        } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        reminderFromNotification = intent.getLongExtra("reminder_id", -1L)
    }
}

@Composable
fun FlowMindApp(viewModel: ReminderViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val editing by viewModel.editingReminder.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val holder = rememberSaveableStateHolder()
    BackHandler(currentTab != "home" && editing == null) { viewModel.setTab("home") }
    FlowMindFrame(currentTab, viewModel::setTab) { tab ->
        holder.SaveableStateProvider(tab) {
            when (tab) {
                "upcoming" -> UpcomingScreen(viewModel)
                "completed" -> CompletedScreen(viewModel)
                "settings" -> SettingsScreen(viewModel)
                else -> HomeScreen(viewModel, { viewModel.setTab("upcoming") })
            }
        }
    }
    val edit = editing
    if (edit != null) {
        EditReminderDialog(edit, { viewModel.setEditingReminder(null) }, viewModel::saveEditedReminder, settings.timeProtocol) {
            FeedbackOverlay(feedback, viewModel::finishFeedback)
        }
    } else FeedbackOverlay(feedback, viewModel::finishFeedback)
}

@Composable
fun FlowMindFrame(activeTab: String, onTabSelected: (String) -> Unit, content: @Composable (String) -> Unit) {
    val backdrop = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Box(Modifier.fillMaxSize().background(Surface).navigationBarsPadding().imePadding()) {
        Column(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }.drawWithContent {
            backdrop.record { this@drawWithContent.drawContent() }
            drawLayer(backdrop)
        }.background(Surface).statusBarsPadding()) {
            TopHeader(if (activeTab == "settings") "Settings" else "Today", { onTabSelected("settings") })
            AnimatedContent(activeTab, modifier = Modifier.fillMaxSize(), label = "page transition", transitionSpec = {
                // Tabs switch in place; the fixed shell and moving dock carry the continuity.
                fadeIn(tween(160)) togetherWith fadeOut(tween(100))
            }) { tab -> content(tab) }
        }
        AnimatedVisibility(!keyboardVisible, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            FloatingNavbar(activeTab, onTabSelected, backdrop = backdrop, backdropOrigin = origin)
        }
    }
}
