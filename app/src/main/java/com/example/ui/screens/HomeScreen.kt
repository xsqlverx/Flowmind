package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sell
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.EntityType
import com.example.data.model.ParsedEntity
import com.example.data.model.ReminderItem
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: ReminderViewModel,
    onNavigateToUpcoming: () -> Unit
) {
    val activeReminders by viewModel.activeReminders.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val composerText by viewModel.composerText.collectAsState()
    val parsedResult by viewModel.parsedResult.collectAsState()
    val isVoiceListening by viewModel.isVoiceListening.collectAsState()
    val isLlmSynthesizing by viewModel.isLlmSynthesizing.collectAsState()

    val todayFormatted = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Ambient Light Halo Hero Banner Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                // Background ambient blurs
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .align(Alignment.TopEnd)
                        .blur(50.dp)
                        .background(Primary.copy(alpha = 0.12f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.BottomStart)
                        .blur(40.dp)
                        .background(Secondary.copy(alpha = 0.10f), CircleShape)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Meta & AI Health Strip
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
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
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
                                Text(
                                    text = "Active Engine",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Tertiary)
                                    )
                                    Text(
                                        text = "${settings.llmProvider} v3.5 Neural",
                                        color = OnSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // Date Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = todayFormatted.uppercase(Locale.ROOT),
                                color = Primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Motivational Prompt Intro
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "What's on your mind?",
                            color = OnSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "Type or speak naturally. FlowMind parses times, recurring cadence, people, and urgency in real-time.",
                            color = OnSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Interactive Conversational Composer Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainer.copy(alpha = 0.95f))
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Engine Indicator & Routing Ratio Pill
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
                                letterSpacing = 0.6.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (parsedResult.needsLlmAssistance) Secondary.copy(alpha = 0.2f) else SurfaceContainerHigh)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (parsedResult.needsLlmAssistance) "1/25 Complex LLM" else "24/25 Keyword Fast-Path",
                                color = if (parsedResult.needsLlmAssistance) Secondary else OnSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Text Input
                    OutlinedTextField(
                        value = composerText,
                        onValueChange = { viewModel.updateComposerText(it) },
                        placeholder = {
                            Text(
                                "e.g. Schedule gym tomorrow at 7am every week...",
                                color = Outline,
                                fontSize = 14.sp
                            )
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
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("composer_input")
                    )

                    // Complex Syntax / 1-in-25 LLM Prompt Banner
                    AnimatedVisibility(visible = parsedResult.needsLlmAssistance) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Secondary.copy(alpha = 0.12f))
                                .border(1.dp, Secondary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Neural LLM",
                                tint = Secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Convoluted syntax detected (1/25 edge-case)",
                                    color = Secondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Tap 'AI Synthesize' to extract multi-intent semantics with ${settings.llmProvider}.",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // Live Extracted Entity Chips Preview
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
                                text = "SEMANTIC TOKENS",
                                color = OnSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
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
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${parsedResult.entities.size} Tokens Parsed",
                                    color = Tertiary,
                                    fontSize = 11.sp,
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

                    // Composer Action Bar with Voice, AI Synthesize & Parse Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleVoiceListening() },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isVoiceListening) Primary else SurfaceContainerHigh)
                                    .testTag("voice_trigger_button")
                            ) {
                                Icon(
                                    imageVector = if (isVoiceListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Record voice reminder",
                                    tint = if (isVoiceListening) OnPrimary else Secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = if (isVoiceListening) "Listening..." else "Voice / Type",
                                color = OnSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Dedicated AI Synthesize Button for 1/25 complex cases or manual trigger
                            Button(
                                onClick = { viewModel.synthesizeWithLlm() },
                                enabled = !isLlmSynthesizing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (parsedResult.needsLlmAssistance) Secondary else SurfaceContainerHigh,
                                    contentColor = if (parsedResult.needsLlmAssistance) Color.Black else OnSurface
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .height(42.dp)
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
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Primary Instant Parse & Save Button (Fast 24/25 cases)
                            Button(
                                onClick = { viewModel.parseAndSetReminder() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryContainer,
                                    contentColor = OnPrimaryContainer
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .height(42.dp)
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

        // Queue Items (show top active reminders from real Room database)
        items(activeReminders.take(3), key = { it.id }) { reminder ->
            HomeQueueCard(
                reminder = reminder,
                onCompleteClick = { viewModel.toggleCompleted(reminder) }
            )
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
