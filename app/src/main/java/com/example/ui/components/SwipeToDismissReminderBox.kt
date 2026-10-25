package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ErrorContainer
import com.example.util.HapticFeedbackHelper

/**
 * Reusable Swipe-To-Dismiss container allowing users to swipe list items
 * to quickly delete (swipe left) or archive / complete (swipe right) with rich visual
 * and subtle tactile haptic feedback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissReminderBox(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    onStartToEnd: (() -> Unit)? = null,
    startToEndLabel: String = "Archive",
    startToEndIcon: ImageVector = Icons.Default.Archive,
    startToEndContainerColor: Color = Color(0xFF00513B),
    startToEndContentColor: Color = Color(0xFF6FF7CE),
    endToStartLabel: String = "Delete",
    endToStartIcon: ImageVector = Icons.Default.Delete,
    endToStartContainerColor: Color = Color(0xFF410002),
    endToStartContentColor: Color = Color(0xFFFFB4AB),
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    HapticFeedbackHelper.triggerDismissHaptic(context)
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (onStartToEnd != null) {
                        HapticFeedbackHelper.triggerDismissHaptic(context)
                        onStartToEnd()
                        true
                    } else {
                        false
                    }
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = onStartToEnd != null,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection

            val bgColor by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> startToEndContainerColor
                    SwipeToDismissBoxValue.EndToStart -> endToStartContainerColor
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                label = "swipe_bg_color"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = startToEndIcon,
                                contentDescription = startToEndLabel,
                                tint = startToEndContentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = startToEndLabel,
                                color = startToEndContentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = endToStartLabel,
                                color = endToStartContentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = endToStartIcon,
                                contentDescription = endToStartLabel,
                                tint = endToStartContentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    SwipeToDismissBoxValue.Settled -> {
                        // Empty background in settled rest state
                    }
                }
            }
        },
        content = {
            content()
        }
    )
}
