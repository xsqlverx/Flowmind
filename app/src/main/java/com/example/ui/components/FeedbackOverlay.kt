package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.PopupPositionProvider
import com.example.ui.feedback.FeedbackEvent
import com.example.ui.feedback.FeedbackKind
import com.example.ui.theme.Error
import com.example.ui.theme.OnSurface
import com.example.ui.theme.Primary
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLow
import kotlinx.coroutines.delay

private class FeedbackReveal(private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): androidx.compose.ui.graphics.Outline = with(density) {
        val diameter = 56.dp.toPx()
        val width = diameter + (size.width - diameter) * progress
        val height = diameter + (size.height - diameter) * progress
        androidx.compose.ui.graphics.Outline.Rounded(RoundRect((size.width - width) / 2, (size.height - height) / 2,
            (size.width + width) / 2, (size.height + height) / 2, CornerRadius(28.dp.toPx())))
    }
}

internal class FeedbackTopPosition(private val topInset: Int, private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        // Use the window, not the invoking button or sheet, so every message has the same home.
        val x = ((windowSize.width - popupContentSize.width) / 2).coerceAtLeast(0)
        val y = (topInset + gap).coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
        return IntOffset(x, y)
    }
}

enum class FeedbackPhase { ICON, MESSAGE, COMPACT, EXIT }
val FeedbackPhaseKey = SemanticsPropertyKey<FeedbackPhase>("FeedbackPhase")
var SemanticsPropertyReceiver.feedbackPhase by FeedbackPhaseKey

/** One presentation surface, including errors, exports, settings, voice and reminder actions. */
@Composable
fun FeedbackOverlay(event: FeedbackEvent?, onFinished: (Long) -> Unit) {
    val resumed = rememberAppResumed()
    val motion = rememberMotionEnabled()
    val density = LocalDensity.current
    val topInset = WindowInsets.safeDrawing.getTop(density)
    val gap = with(density) { 12.dp.roundToPx() }
    val placement = remember(topInset, gap) { FeedbackTopPosition(topInset, gap) }
    // Unconsumed feedback starts again when the app returns; background time never eats a message.
    if (event != null && resumed) key(event.id) {
        Popup(popupPositionProvider = placement, properties = PopupProperties(focusable = false,
            dismissOnBackPress = false, dismissOnClickOutside = false, clippingEnabled = false)) {
            FeedbackCapsule(event, motion, topInset + gap, onFinished)
        }
    }
}

@Composable
private fun FeedbackCapsule(event: FeedbackEvent, motion: Boolean, topOffset: Int, onFinished: (Long) -> Unit) {
    val expanded = remember { Animatable(if (motion) 0f else 1f) }
    val appearance = remember { Animatable(if (motion) 0f else 1f) }
    val departure = remember { Animatable(0f) }
    var capsuleHeight by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(if (motion) FeedbackPhase.ICON else FeedbackPhase.MESSAGE) }
    val complete by rememberUpdatedState(onFinished)
    val accessibility = LocalAccessibilityManager.current
    val readTime = (1_650L + event.message.length * 28L).coerceIn(2_000L, 6_000L)
    val holdTime = accessibility?.calculateRecommendedTimeoutMillis(readTime, containsIcons = true, containsText = true, containsControls = false) ?: readTime
    LaunchedEffect(event.id, motion) {
        if (motion) {
            appearance.animateTo(1f, tween(180))
            delay(100)
            phase = FeedbackPhase.MESSAGE
            expanded.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        } else {
            appearance.snapTo(1f); expanded.snapTo(1f); departure.snapTo(0f)
            phase = FeedbackPhase.MESSAGE
        }
        delay(holdTime)
        if (motion) {
            phase = FeedbackPhase.COMPACT
            expanded.animateTo(0f, tween(240, easing = FastOutSlowInEasing))
            delay(100)
            phase = FeedbackPhase.EXIT
            departure.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        } else appearance.animateTo(0f, tween(120))
        complete(event.id)
    }
    val width = (LocalConfiguration.current.screenWidthDp - 48).coerceIn(56, 380).dp
    val reveal = FeedbackReveal(expanded.value)
    val tint = if (event.kind == FeedbackKind.ERROR) Error else Primary
    val icon = when (event.kind) {
        FeedbackKind.DELETE -> Icons.Outlined.DeleteOutline
        FeedbackKind.ARCHIVE -> Icons.Outlined.Archive
        FeedbackKind.RESTORE -> Icons.AutoMirrored.Outlined.Undo
        FeedbackKind.SNOOZE -> Icons.Outlined.Snooze
        FeedbackKind.CALENDAR -> Icons.Outlined.Event
        FeedbackKind.SUCCESS -> Icons.Outlined.Check
        FeedbackKind.ERROR -> Icons.Outlined.PriorityHigh
        FeedbackKind.INFO -> Icons.Outlined.Info
    }
    // Measure the complete text once. Clipping reveals it without reflowing it during expansion.
    Box(Modifier.width(width).onSizeChanged { capsuleHeight = it.height }.graphicsLayer {
        alpha = appearance.value * (1f - ((departure.value - .45f) / .55f).coerceIn(0f, 1f))
        scaleX = .86f + .14f * appearance.value
        scaleY = scaleX
        translationY = -(topOffset + capsuleHeight + 24.dp.toPx()) * departure.value
        shadowElevation = 20.dp.toPx()
        shape = reveal
    }.testTag("app_feedback").semantics(mergeDescendants = true) {
        liveRegion = LiveRegionMode.Polite
        contentDescription = event.message
        feedbackPhase = phase
    }, contentAlignment = Alignment.Center) {
        Row(Modifier.width(width).heightIn(min = 56.dp).graphicsLayer { alpha = 0f }.padding(start = 60.dp, end = 22.dp, top = 18.dp, bottom = 18.dp)
            .clearAndSetSemantics {}) {
            Text(event.message, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium)
        }
        Box(Modifier.matchParentSize().clip(reveal)
            .background(Brush.verticalGradient(listOf(SurfaceContainerHighest, SurfaceContainerLow)))) {
            // The rim stays quiet against the existing charcoal palette.
            Box(Modifier.matchParentSize().border(1.dp, OnSurface.copy(alpha = .14f), reveal))
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(56.dp).fillMaxHeight().graphicsLayer {
                    translationX = (width.toPx() - 56.dp.toPx()) / 2 * (1f - expanded.value)
                }, contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
                }
                Text(event.message, color = OnSurface, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(start = 4.dp, end = 22.dp, top = 18.dp, bottom = 18.dp)
                        .graphicsLayer { alpha = ((expanded.value - .35f) / .65f).coerceIn(0f, 1f) }.clearAndSetSemantics {})
            }
        }
    }
}
