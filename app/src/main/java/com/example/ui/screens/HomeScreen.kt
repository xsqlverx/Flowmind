package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.EntityType
import com.example.data.model.ParsedEntity
import com.example.data.model.ReminderItem
import com.example.ui.components.SwipeToDismissReminderBox
import com.example.ui.theme.Error
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OnPrimaryContainer
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.Outline
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.Primary
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryGlow
import com.example.ui.theme.Secondary
import com.example.ui.theme.SecondaryFixedDim
import com.example.ui.theme.Surface
import com.example.ui.theme.SurfaceBright
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.Tertiary
import com.example.ui.viewmodel.ReminderViewModel
import com.example.util.HapticFeedbackHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: ReminderViewModel,
    onNavigateToUpcoming: () -> Unit,
    onVoicePermissionRequest: () -> Unit = { viewModel.toggleVoiceListening() }
) {
    val context = LocalContext.current
    val activeReminders by viewModel.activeReminders.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val composerText by viewModel.composerText.collectAsState()
    val parsedResult by viewModel.parsedResult.collectAsState()
    val isVoiceListening by viewModel.isVoiceListening.collectAsState()
    val voiceAudioRms by viewModel.voiceAudioRms.collectAsState()
    val voiceStatusMessage by viewModel.voiceStatusMessage.collectAsState()
    val isLlmSynthesizing by viewModel.isLlmSynthesizing.collectAsState()

    val todayFormatted = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())

    // Mic Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Sleek Engine & Status Top Bar (Compact for 360dp, replaces the old bulky hero tile)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBBWZK0agR88fhs4Lbdy1WARI8_n4hAYBZowXIhTmN4xjgcZYAkLkuYAnpzbfalRrbABLIrVKoDLPH-Y2fFDQVQYRnk8jsJdX24armeslTbBXUVFXotdlSM5i3JcJ-O8nApExvpw0_cK5QI4FQp82539oVtZ_PytA3jjOJpvP264rNixyyuZtQO2HeRAi8XLDbsNH5or3q2XQJjFNa7Xn9mUwjJ5C-mb-Y1NkggDcyhmbHxr0371LKo",
                            contentDescription = "Smart Bot Avatar",
                            modifier = Modifier.matchParentSize()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Tertiary)
                            )
                            Text(
                                text = "${settings.llmProvider} v3.5 Neural",
                                color = OnSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = "Speech-to-Text & Real-time NLP",
                            color = OnSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Date Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = todayFormatted.uppercase(Locale.ROOT),
                        color = Primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Interactive Conversational Composer Card (360dp optimized)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceContainer.copy(alpha = 0.95f))
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Engine Indicator
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
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Synthesizer",
                                tint = if (parsedResult.needsLlmAssistance) Secondary else Primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = parsedResult.parsingEngine.uppercase(Locale.ROOT),
                                color = if (parsedResult.needsLlmAssistance) Secondary else Primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Text Input with Embedded Trailing Mic Icon
                    OutlinedTextField(
                        value = composerText,
                        onValueChange = { viewModel.updateComposerText(it) },
                        placeholder = {
                            Text(
                                "Type or speak reminder...",
                                color = Outline,
                                fontSize = 13.sp
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = onVoicePermissionRequest,
                                modifier = Modifier
                                    .size(44.dp)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(if (isVoiceListening) Primary else Color.Transparent)
                                    .testTag("embedded_voice_button")
                            ) {
                                Icon(
                                    imageVector = if (isVoiceListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isVoiceListening) "Stop listening" else "Voice input",
                                    tint = if (isVoiceListening) OnPrimary else Secondary,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .then(if (isVoiceListening) Modifier.scale(pulseScale) else Modifier)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            focusedBorderColor = PrimaryGlow,
                            unfocusedBorderColor = Color(0x26FFFFFF),
                            focusedContainerColor = SurfaceContainerLowest.copy(alpha = 0.8f),
                            unfocusedContainerColor = SurfaceContainerLowest.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("composer_input")
                    )

                    // Active Real STT Recording Feedback Strip
                    AnimatedVisibility(visible = isVoiceListening) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Primary.copy(alpha = 0.15f))
                                .border(1.dp, Primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Microphone Active",
                                    tint = OnPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = voiceStatusMessage ?: "Listening... speak now",
                                    color = Primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Real-time speech recognition active",
                                    color = OnSurfaceVariant,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Button(
                                onClick = { viewModel.stopVoiceListening() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Primary,
                                    contentColor = OnPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "Done",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Complex Syntax / 1-in-25 LLM Prompt Banner
                    AnimatedVisibility(visible = parsedResult.needsLlmAssistance) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Secondary.copy(alpha = 0.12f))
                                .border(1.dp, Secondary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Neural LLM",
                                tint = Secondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Complex syntax detected",
                                    color = Secondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Tap 'AI Synthesize' to parse multi-intent tokens.",
                                    color = OnSurfaceVariant,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }

                    // Live Extracted Entity Chips Preview
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SEMANTIC TOKENS",
                                color = OnSurfaceVariant,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Detected",
                                    tint = Tertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${parsedResult.entities.size} Tokens Parsed",
                                    color = Tertiary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Entity chips row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            parsedResult.entities.forEach { entity ->
                                EntityChipView(entity = entity)
                            }
                        }
                    }

                    // Prominent Voice Record Bar (Uncrushable, 48dp Touch Target, Toggles Listening State)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isVoiceListening) Primary.copy(alpha = 0.18f) else SurfaceContainerHigh)
                            .border(
                                1.dp,
                                if (isVoiceListening) Primary.copy(alpha = 0.5f) else Color(0x14FFFFFF),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onVoicePermissionRequest() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isVoiceListening) Primary else Secondary.copy(alpha = 0.18f))
                                    .then(if (isVoiceListening) Modifier.scale(pulseScale) else Modifier)
                                    .testTag("voice_trigger_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isVoiceListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isVoiceListening) "Stop listening" else "Record voice reminder",
                                    tint = if (isVoiceListening) OnPrimary else Secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isVoiceListening) "Listening... Tap to Stop" else "Tap to Speak Reminder",
                                    color = if (isVoiceListening) Primary else OnSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = if (isVoiceListening) "Transcribing directly to input field • Tap to finish" else "Supports dates, times, priority & tags",
                                    color = if (isVoiceListening) Primary.copy(alpha = 0.8f) else OnSurfaceVariant,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (isVoiceListening) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Primary)
                                    .clickable { viewModel.stopVoiceListening() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "STOP",
                                    color = OnPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else if (composerText.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.updateComposerText("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear input",
                                    tint = Outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Action Buttons: 50/50 Equal Weighting Row (Never crushed or misaligned on 360dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // AI Synthesize Button (50% width)
                        Button(
                            onClick = { viewModel.synthesizeWithLlm() },
                            enabled = !isLlmSynthesizing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (parsedResult.needsLlmAssistance) Secondary else SurfaceContainerHigh,
                                contentColor = if (parsedResult.needsLlmAssistance) Color.Black else OnSurface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("ai_synthesize_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Synthesize",
                                    tint = if (parsedResult.needsLlmAssistance) Color.Black else Secondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isLlmSynthesizing) "Synthesizing..." else "AI Synthesize",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Parse & Set Button (50% width)
                        Button(
                            onClick = {
                                HapticFeedbackHelper.triggerCreationHaptic(context)
                                viewModel.parseAndSetReminder()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryContainer,
                                contentColor = OnPrimaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("parse_and_set_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "Parse & Set",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Save",
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Natural Language Templates
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROMPT SHORTCUTS",
                        color = OnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "Tap to populate",
                        color = Primary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PromptTemplateChip(
                        icon = Icons.Default.Phone,
                        iconTint = Secondary,
                        label = "Call dentist Monday 9am",
                        promptText = "Call dentist on Monday at 9:00 AM",
                        onClick = { viewModel.applyPromptTemplate(it) }
                    )
                    PromptTemplateChip(
                        icon = Icons.Default.LocalFlorist,
                        iconTint = Tertiary,
                        label = "Water plants every Sunday",
                        promptText = "Water indoor plants every Sunday at 10:00 AM #home",
                        onClick = { viewModel.applyPromptTemplate(it) }
                    )
                    PromptTemplateChip(
                        icon = Icons.Default.Schedule,
                        iconTint = Primary,
                        label = "Follow up in 2 hours",
                        promptText = "Follow up with client in 2 hours regarding draft approval",
                        onClick = { viewModel.applyPromptTemplate(it) }
                    )
                }
            }
        }

        // Real-time Queue / Just Added Header
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
                        text = "Today's Active Queue",
                        color = OnSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${activeReminders.take(2).size} Due",
                            color = OnSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Text(
                    text = "View Timeline",
                    color = Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToUpcoming() }
                        .padding(4.dp),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Queue Items (show top active reminders from real Room database with swipe-to-dismiss)
        items(activeReminders.take(5), key = { it.id }) { reminder ->
            SwipeToDismissReminderBox(
                onDelete = { viewModel.deleteReminder(reminder) },
                onStartToEnd = { viewModel.toggleCompleted(reminder) },
                startToEndLabel = "Complete",
                startToEndIcon = Icons.Default.Check,
                startToEndContainerColor = Color(0xFF00513B),
                startToEndContentColor = Color(0xFF6FF7CE),
                shape = RoundedCornerShape(20.dp)
            ) {
                HomeQueueCard(
                    reminder = reminder,
                    onCompleteClick = { viewModel.toggleCompleted(reminder) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
fun EntityChipView(entity: ParsedEntity) {
    val (bg, textColor, iconTint) = when (entity.type) {
        EntityType.PRIORITY -> Triple(ErrorContainer.copy(alpha = 0.35f), Error, Error)
        EntityType.DATE_TIME -> Triple(SurfaceContainerHigh, OnSurface, Secondary)
        EntityType.CATEGORY -> Triple(SurfaceContainerHigh, OnSurface, Primary)
        EntityType.PARTICIPANT -> Triple(SurfaceContainerHigh, OnSurface, SecondaryFixedDim)
        EntityType.RECURRENCE -> Triple(SurfaceContainerHigh, OnSurface, Tertiary)
    }

    val iconVector = when (entity.type) {
        EntityType.PRIORITY -> Icons.Default.PriorityHigh
        EntityType.DATE_TIME -> Icons.Default.Schedule
        EntityType.CATEGORY -> Icons.Default.Sell
        EntityType.PARTICIPANT -> Icons.Default.Person
        EntityType.RECURRENCE -> Icons.Default.Schedule
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = entity.value,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun PromptTemplateChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    promptText: String,
    onClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(18.dp))
            .clickable { onClick(promptText) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = OnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun HomeQueueCard(
    reminder: ReminderItem,
    onCompleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceContainer)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(18.dp))
            .padding(14.dp)
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
                // Checkbox toggle button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerHighest)
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                        .clickable { onCompleteClick() }
                        .testTag("queue_complete_${reminder.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark completed",
                        tint = Color.Transparent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        color = OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
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
                                tint = SecondaryFixedDim,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = reminder.dueTimeString,
                                color = SecondaryFixedDim,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(OutlineVariant)
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainerHigh)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = reminder.category,
                                color = OnSurfaceVariant,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Status indicator dot
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(
                        if (reminder.priority == "HIGH") Error
                        else Secondary
                    )
            )
        }
    }
}
