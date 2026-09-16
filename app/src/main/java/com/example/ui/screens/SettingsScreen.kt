package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import com.example.data.nlp.LlmReminderClient
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Error
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnSecondaryContainer
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.Outline
import com.example.ui.theme.Primary
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryGlow
import com.example.ui.theme.Secondary
import com.example.ui.theme.SecondaryContainer
import com.example.ui.theme.Surface
import com.example.ui.theme.SurfaceBright
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.SurfaceVariant
import com.example.ui.theme.Tertiary
import com.example.ui.theme.TertiaryContainer
import com.example.ui.viewmodel.ReminderViewModel

data class LlmProviderOption(
    val id: String,
    val name: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun SettingsScreen(
    viewModel: ReminderViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val allReminders by viewModel.allReminders.collectAsState()
    val gatewayLatency by viewModel.gatewayLatency.collectAsState()
    val isTesting by viewModel.isTestingConnection.collectAsState()

    var apiKeyInput by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var customModelInput by remember(settings.customModelName) { mutableStateOf(settings.customModelName) }
    var customEndpointInput by remember(settings.customEndpointUrl) { mutableStateOf(settings.customEndpointUrl) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            viewModel.showToast("Notification permission granted ✓")
        } else {
            viewModel.showToast("Notification permission denied")
        }
    }

    val providers = remember {
        listOf(
            LlmProviderOption("NVIDIA", "NVIDIA NIM", "meta/llama-3.3-70b", Icons.Default.Memory),
            LlmProviderOption("OpenRouter", "OpenRouter", "200+ Models (Llama, Claude)", Icons.Default.Hub),
            LlmProviderOption("Groq", "Groq LPU", "Llama 3.3 • 800 t/s", Icons.Default.Bolt),
            LlmProviderOption("Google Gemini", "Gemini", "2.5 Flash Multi-Modal", Icons.Default.AutoAwesome),
            LlmProviderOption("OpenAI", "OpenAI", "GPT-4o Omnimodal", Icons.Default.Psychology),
            LlmProviderOption("Anthropic", "Anthropic", "Claude 3.5 Haiku", Icons.Default.Chat),
            LlmProviderOption("Custom", "Custom / Local", "Ollama, vLLM, or URL", Icons.Default.Lan)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Header Title Section
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
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
                            text = "CORE INTELLIGENCE MATRIX",
                            color = Tertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Text(
                    text = "Settings & AI Engine",
                    color = OnSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "Manage your intelligence providers, API credentials, and data portability.",
                    color = OnSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }

        // SECTION 1: LLM Provider & API Key
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "LLM Provider",
                            color = OnSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "v3.5 Neural",
                            color = Secondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Provider Selection Grid (NVIDIA, OpenRouter, Groq, Gemini, OpenAI, Anthropic, Custom)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProviderTile(
                            provider = providers[0],
                            isSelected = settings.llmProvider == providers[0].id,
                            onClick = { viewModel.setLlmProvider(providers[0].id) },
                            modifier = Modifier.weight(1f)
                        )
                        ProviderTile(
                            provider = providers[1],
                            isSelected = settings.llmProvider == providers[1].id,
                            onClick = { viewModel.setLlmProvider(providers[1].id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProviderTile(
                            provider = providers[2],
                            isSelected = settings.llmProvider == providers[2].id,
                            onClick = { viewModel.setLlmProvider(providers[2].id) },
                            modifier = Modifier.weight(1f)
                        )
                        ProviderTile(
                            provider = providers[3],
                            isSelected = settings.llmProvider == providers[3].id,
                            onClick = { viewModel.setLlmProvider(providers[3].id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProviderTile(
                            provider = providers[4],
                            isSelected = settings.llmProvider == providers[4].id,
                            onClick = { viewModel.setLlmProvider(providers[4].id) },
                            modifier = Modifier.weight(1f)
                        )
                        ProviderTile(
                            provider = providers[5],
                            isSelected = settings.llmProvider == providers[5].id,
                            onClick = { viewModel.setLlmProvider(providers[5].id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProviderTile(
                            provider = providers[6],
                            isSelected = settings.llmProvider == providers[6].id,
                            onClick = { viewModel.setLlmProvider(providers[6].id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // API Key & Model Configuration Liquid Glass Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Header with status indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${settings.llmProvider} Config",
                                    color = OnSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            val isKeyConfigured = apiKeyInput.isNotBlank() || settings.llmProvider == "Custom"
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (isKeyConfigured) TertiaryContainer.copy(alpha = 0.25f)
                                        else Color(0x25FFB74D)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isKeyConfigured) Tertiary else Color(0xFFFFB74D))
                                    )
                                    Text(
                                        text = if (isKeyConfigured) "Key Saved & Ready" else "No Key (Offline Mode)",
                                        color = if (isKeyConfigured) Tertiary else Color(0xFFFFB74D),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // Provider Guidance Note
                        val providerGuidance = when (settings.llmProvider.lowercase(java.util.Locale.ROOT)) {
                            "nvidia", "nvidia nim" -> "Accepts any NVIDIA NIM Cloud key (nvapi-...). High throughput 70B parameter models."
                            "openrouter" -> "Accepts any OpenRouter key (sk-or-v1-...). Access 200+ models with unified routing."
                            "groq" -> "Accepts any Groq API key (gsk_...). Ultra-fast sub-second parsing."
                            "google gemini", "gemini" -> "Accepts any Google AI Studio key (AIzaSy...). Multi-modal reasoning & 1M context."
                            "openai" -> "Accepts any OpenAI key (sk-...). Industry standard GPT-4o models."
                            "anthropic" -> "Accepts any Anthropic key (sk-ant-...). Claude 3.5 family."
                            else -> "Configure any custom OpenAI-compatible API base URL, reverse proxy, or local Ollama instance."
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainer)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = providerGuidance,
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // API Key Input Field
                        val keyPlaceholder = when (settings.llmProvider.lowercase(java.util.Locale.ROOT)) {
                            "nvidia", "nvidia nim" -> "nvapi-..."
                            "openrouter" -> "sk-or-v1-..."
                            "groq" -> "gsk_..."
                            "google gemini", "gemini" -> "AIzaSy..."
                            "openai" -> "sk-..."
                            "anthropic" -> "sk-ant-..."
                            else -> "sk-... (or blank for Ollama)"
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "API Key",
                                color = OnSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceContainerHighest)
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = OnSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    OutlinedTextField(
                                        value = apiKeyInput,
                                        onValueChange = { apiKeyInput = it },
                                        placeholder = {
                                            Text(
                                                text = keyPlaceholder,
                                                color = OnSurfaceVariant.copy(alpha = 0.5f),
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        },
                                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = OnSurface,
                                            unfocusedTextColor = OnSurface,
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        ),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("api_key_input")
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // Paste button
                                    IconButton(
                                        onClick = {
                                            try {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = clipboard.primaryClip
                                                if (clip != null && clip.itemCount > 0) {
                                                    val pasted = clip.getItemAt(0).text?.toString()?.trim().orEmpty()
                                                    if (pasted.isNotEmpty()) {
                                                        apiKeyInput = pasted
                                                        viewModel.showToast("Pasted API key from clipboard")
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                viewModel.showToast("Could not access clipboard")
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste Key",
                                            tint = Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Visibility toggle
                                    IconButton(
                                        onClick = { isKeyVisible = !isKeyVisible },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Key Visibility",
                                            tint = OnSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Clear button if not empty
                                    if (apiKeyInput.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                apiKeyInput = ""
                                                viewModel.showToast("API Key cleared")
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear Key",
                                                tint = OnSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Custom Model ID Field
                        val defaultModelName = LlmReminderClient.resolveDefaultModel(settings.llmProvider, "")
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Model Identifier",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Default: $defaultModelName",
                                    color = Primary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceContainerHighest)
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                OutlinedTextField(
                                    value = customModelInput,
                                    onValueChange = { customModelInput = it },
                                    placeholder = {
                                        Text(
                                            text = defaultModelName,
                                            color = OnSurfaceVariant.copy(alpha = 0.5f),
                                            fontSize = 12.sp,
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
                                        .testTag("custom_model_input")
                                )
                            }
                        }

                        // Custom Endpoint URL Field (for Custom/Local, or overriding any provider URL)
                        val defaultEndpointUrl = LlmReminderClient.resolveDefaultEndpoint(settings.llmProvider, "")
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "API Endpoint / Proxy URL",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (customEndpointInput.isNotBlank()) {
                                    Text(
                                        text = "Custom Active",
                                        color = Secondary,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceContainerHighest)
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                OutlinedTextField(
                                    value = customEndpointInput,
                                    onValueChange = { customEndpointInput = it },
                                    placeholder = {
                                        Text(
                                            text = defaultEndpointUrl,
                                            color = OnSurfaceVariant.copy(alpha = 0.5f),
                                            fontSize = 12.sp,
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
                                        .testTag("custom_endpoint_input")
                                )
                            }
                        }

                        // Diagnostics Latency Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Tertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Gateway Latency:",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = gatewayLatency,
                                color = Tertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Action Buttons: Test Connection & Save Configuration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.setApiKey(apiKeyInput.trim())
                                    viewModel.setCustomEndpointUrl(customEndpointInput.trim())
                                    viewModel.setCustomModelName(customModelInput.trim())
                                    viewModel.testConnection()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SurfaceContainerHigh,
                                    contentColor = OnSurface
                                ),
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_connection_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NetworkPing,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isTesting) "Pinging..." else "Test Latency",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.setApiKey(apiKeyInput.trim())
                                    viewModel.setCustomEndpointUrl(customEndpointInput.trim())
                                    viewModel.setCustomModelName(customModelInput.trim())
                                    viewModel.showToast("${settings.llmProvider} configuration saved ✓")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Primary,
                                    contentColor = OnPrimary
                                ),
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_key_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Save Settings",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // Offline NLP Fallback Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceContainer)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Offline NLP Fallback",
                                    color = OnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Keyword & Regex parser activates when connection drops",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = settings.offlineFallback,
                                onCheckedChange = { viewModel.setOfflineFallback(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SecondaryContainer
                                ),
                                modifier = Modifier.testTag("fallback_switch")
                            )
                        }
                    }
                }
            }
        }

        // SECTION 2: Data Portability
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Data Portability",
                        color = OnSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "${allReminders.size} Active Tasks",
                        color = OnSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Format Pipeline
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "FORMAT PIPELINE",
                                color = OnSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                                    .background(SurfaceContainerHighest)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("JSON", "MD", "Apple", ".ICS").forEach { fmt ->
                                    val isSelected = settings.formatPipeline == fmt
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(CircleShape)
                                            .background(if (isSelected) SecondaryContainer else Color.Transparent)
                                            .clickable { viewModel.setFormatPipeline(fmt) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = fmt,
                                            color = if (isSelected) OnSecondaryContainer else OnSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // Target Destination
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "TARGET DESTINATION",
                                color = OnSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                maxLines = 1,
                                softWrap = false
                            )

                            // Destination 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceContainerHigh)
                                    .clickable { viewModel.setExportDestination("Local") }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Secondary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderSpecial,
                                            contentDescription = null,
                                            tint = Secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Local Files / iCloud Drive",
                                            color = OnSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "Encrypted App Folder • /FlowMind/Backups",
                                            color = OnSurfaceVariant,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                RadioButton(
                                    selected = settings.exportDestination == "Local",
                                    onClick = { viewModel.setExportDestination("Local") },
                                    colors = RadioButtonDefaults.colors(selectedColor = Primary)
                                )
                            }

                            // Destination 2
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceContainerHigh)
                                    .clickable { viewModel.setExportDestination("Dropbox") }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Dropbox Remote",
                                            color = OnSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "Connected as alex@operator.io",
                                            color = OnSurfaceVariant,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                RadioButton(
                                    selected = settings.exportDestination == "Dropbox",
                                    onClick = { viewModel.setExportDestination("Dropbox") },
                                    colors = RadioButtonDefaults.colors(selectedColor = Primary)
                                )
                            }
                        }

                        // Export Button
                        Button(
                            onClick = { viewModel.exportReminders(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecondaryContainer,
                                contentColor = OnSecondaryContainer
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("export_reminders_button")
                        ) {
                            Text(
                                text = "Export ${allReminders.size} Reminders Now",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // SECTION: Push & System Notifications
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Push & System Notifications",
                            color = OnSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (settings.notificationsEnabled) Color(0x1F7CDBFE) else Color(0x1AFFFFFF))
                            .border(
                                1.dp,
                                if (settings.notificationsEnabled) Color(0x407CDBFE) else Color(0x1FFFFFFF),
                                CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (settings.notificationsEnabled) "● Active Delivery" else "○ Paused",
                            color = if (settings.notificationsEnabled) Secondary else OnSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Master Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable System Alerts",
                                    color = OnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Triggers exact device alarms and lockscreen notifications when due",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = settings.notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Secondary
                                ),
                                modifier = Modifier.testTag("notifications_master_switch")
                            )
                        }

                        // Notification Permission Warning (Android 13+)
                        if (!hasNotificationPermission) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x1AFBBF24))
                                    .border(1.dp, Color(0x40FBBF24), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationImportant,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Permission Required",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "Grant notification access to receive reminder alerts",
                                            color = OnSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFBBF24),
                                            contentColor = Color.Black
                                        ),
                                        shape = CircleShape,
                                        modifier = Modifier.testTag("grant_notification_permission_button")
                                    ) {
                                        Text(
                                            text = "Allow",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // Lead Time / Advance Notice Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "ALERT TIMING / ADVANCE NOTICE",
                                color = OnSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                                    .background(SurfaceContainerHighest)
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(
                                    0 to "At due time",
                                    5 to "5m before",
                                    15 to "15m before",
                                    30 to "30m before"
                                ).forEach { (min, label) ->
                                    val isSelected = settings.notificationLeadMinutes == min
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(CircleShape)
                                            .background(if (isSelected) SecondaryContainer else Color.Transparent)
                                            .clickable { viewModel.setNotificationLeadMinutes(min) }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) OnSecondaryContainer else OnSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // Sound & Vibration Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceContainerHigh)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Secondary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = Secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sound & Vibration",
                                        color = OnSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "Dual-pulse haptics & alarm ringtone",
                                        color = OnSurfaceVariant,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            Switch(
                                checked = settings.notificationSoundEnabled,
                                onCheckedChange = { viewModel.setNotificationSoundEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Secondary
                                ),
                                modifier = Modifier.testTag("sound_vibration_switch")
                            )
                        }

                        // Heads-Up Floating Banner Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceContainerHigh)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "High-Priority Heads-Up Banner",
                                        color = OnSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "Display prominent floating banner over apps",
                                        color = OnSurfaceVariant,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            Switch(
                                checked = settings.notificationHeadsUp,
                                onCheckedChange = { viewModel.setNotificationHeadsUp(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Primary
                                ),
                                modifier = Modifier.testTag("heads_up_banner_switch")
                            )
                        }

                        // Test Notification Action Button
                        Button(
                            onClick = { viewModel.sendTestNotification() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceContainerHighest,
                                contentColor = OnSurface
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .border(1.dp, Color(0x20FFFFFF), CircleShape)
                                .testTag("send_test_notification_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Send Test Notification",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // SECTION 3: Engine Tuning
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Engine Tuning",
                        color = OnSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Snooze latency
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Default Snooze Latency",
                                    color = OnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Smart Predictive",
                                    color = Primary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("15m", "1 hour", "Next Day").forEach { latency ->
                                    val isSelected = settings.snoozeLatency == latency
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Primary else SurfaceContainerHighest)
                                            .clickable { viewModel.setSnoozeLatency(latency) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = latency,
                                            color = if (isSelected) OnPrimary else OnSurface,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // Quiet hours
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Smart Quiet Hours",
                                    color = OnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Silently queue non-urgent reminders 22:00 – 07:00",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = settings.smartQuietHours,
                                onCheckedChange = { viewModel.setSmartQuietHours(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Primary
                                )
                            )
                        }

                        // Time Protocol
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Time Protocol",
                                    color = OnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Natural language interpreter parser",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SurfaceContainerHighest)
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf("12h", "24h Military").forEach { fmt ->
                                    val isSelected = (settings.timeProtocol == "12h" && fmt == "12h") ||
                                                     (settings.timeProtocol == "24h" && fmt == "24h Military")
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) SurfaceBright else Color.Transparent)
                                            .clickable { viewModel.setTimeProtocol(if (fmt == "12h") "12h" else "24h") }
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = fmt,
                                            color = OnSurface,
                                            fontSize = 11.sp,
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
            }
        }

        // SECTION 4: App Diagnostics & Status
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "FlowMind Engine v2.4.0",
                                color = OnSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = "All Systems Operational",
                            color = Tertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "SQLite Core • 2.4 MB",
                            color = OnSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(text = "•", color = OnSurfaceVariant, fontSize = 11.sp)
                        Text(
                            text = "E2E AES-256 Encrypted",
                            color = OnSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(text = "•", color = OnSurfaceVariant, fontSize = 11.sp)
                        Text(
                            text = "Neural Embeddings Ready",
                            color = OnSurfaceVariant,
                            fontSize = 11.sp,
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
fun ProviderTile(
    provider: LlmProviderOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) SurfaceContainerHigh else SurfaceContainerLow)
            .border(
                1.dp,
                if (isSelected) PrimaryGlow.copy(alpha = 0.5f) else Color(0x14FFFFFF),
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
            .testTag("provider_tile_${provider.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Primary.copy(alpha = 0.2f) else SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = provider.icon,
                        contentDescription = null,
                        tint = if (isSelected) Primary else OnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = OnPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Text(
                text = provider.name,
                color = OnSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = provider.subtitle,
                color = OnSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
