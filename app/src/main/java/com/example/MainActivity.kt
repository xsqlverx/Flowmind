package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.EditReminderDialog
import com.example.ui.components.FloatingNavbar
import com.example.ui.components.TopHeader
import com.example.ui.screens.CompletedScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UpcomingScreen
import com.example.ui.theme.FlowMindTheme
import com.example.ui.theme.OnSurface
import com.example.ui.theme.Surface
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.Tertiary
import com.example.ui.viewmodel.ReminderViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlowMindTheme {
                val viewModel: ReminderViewModel = viewModel()
                FlowMindApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FlowMindApp(viewModel: ReminderViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val editingReminder by viewModel.editingReminder.collectAsState()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* handled */ }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val screenTitle = when (currentTab) {
        "upcoming" -> "Upcoming"
        "completed" -> "Completed"
        "settings" -> "Settings"
        else -> "Home"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Surface,
        topBar = {
            TopHeader(
                currentScreenTitle = screenTitle,
                onQuickActionClick = {
                    viewModel.showToast("Neural Engine: Active Telemetry")
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Content Tabs
            when (currentTab) {
                "upcoming" -> UpcomingScreen(viewModel = viewModel)
                "completed" -> CompletedScreen(viewModel = viewModel)
                "settings" -> SettingsScreen(viewModel = viewModel)
                else -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToUpcoming = { viewModel.setTab("upcoming") }
                )
            }

            // Floating Toast Notification
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { 40 }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp)
            ) {
                toastMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(SurfaceContainerHighest)
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("app_toast")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Tertiary)
                            )
                            Text(
                                text = msg,
                                color = OnSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Floating Liquid Glass Navbar
            FloatingNavbar(
                activeTab = currentTab,
                onTabSelected = { viewModel.setTab(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Edit Dialog
            editingReminder?.let { reminder ->
                EditReminderDialog(
                    reminder = reminder,
                    onDismiss = { viewModel.setEditingReminder(null) },
                    onSave = { updated -> viewModel.saveEditedReminder(updated) }
                )
            }
        }
    }
}
