package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.roundToInt

val NavigationDragPositionKey = SemanticsPropertyKey<Float>("NavigationDragPosition")
var SemanticsPropertyReceiver.navigationDragPosition by NavigationDragPositionKey
val NavigationDraggingKey = SemanticsPropertyKey<Boolean>("NavigationDragging")
var SemanticsPropertyReceiver.navigationDragging by NavigationDraggingKey

@Composable
fun FloatingNavbar(activeTab: String, onTabSelected: (String) -> Unit, modifier: Modifier = Modifier,
    backdrop: GraphicsLayer? = null, backdropOrigin: Offset = Offset.Zero) {
    val tabs = listOf("home", "upcoming", "completed", "settings")
    val labels = listOf("Today", "Schedule", "Done", "Settings")
    val icons = listOf(Icons.Outlined.Home, Icons.Outlined.CalendarToday, Icons.Outlined.CheckCircle, Icons.Outlined.Tune)
    val selectedIndex = tabs.indexOf(activeTab).coerceAtLeast(0)
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val selectTab by rememberUpdatedState(onTabSelected)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val ambient = LocalAmbientPhase.current
    val motion = rememberMotionEnabled()
    var dragging by remember { mutableStateOf(false) }
    var target by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) { dragging = false; target = selectedIndex.toFloat() }
    val settled by animateFloatAsState(target, if (dragging || !motion) snap() else spring(dampingRatio = .76f, stiffness = 420f), label = "glass selection")
    val position = if (dragging) target else settled
    val currentPosition by rememberUpdatedState(position)
    val previewIndex = if (dragging) target.roundToInt().coerceIn(0, 3) else selectedIndex
    val lift by animateFloatAsState(if (dragging) 1f else 0f, if (motion) spring(dampingRatio = .7f, stiffness = 500f) else snap(), label = "held glass")
    val physicalPosition = if (rtl) 3f - position else position
    var barWidth by remember { mutableIntStateOf(0) }
    val insetPx = with(density) { 5.dp.toPx() }
    val cellPx = ((barWidth - insetPx * 2) / tabs.size).coerceAtLeast(0f)
    var origin by remember { mutableStateOf(Offset.Zero) }
    val frost = rememberGraphicsLayer()
    val shape = RoundedCornerShape(38.dp)
    val bubbleInteraction = remember { MutableInteractionSource() }
    Box(modifier.widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp)) {
        Box(Modifier.fillMaxWidth().height(76.dp).shadow(22.dp, shape, spotColor = Color.Black)
            .testTag("navigation_dock").semantics {
                navigationDragPosition = position
                navigationDragging = dragging
                stateDescription = if (dragging) "Release for ${labels[previewIndex]}" else labels[selectedIndex]
            }
            .clip(shape).onGloballyPositioned { origin = it.positionInRoot() - backdropOrigin; barWidth = it.size.width }
            .drawWithContent {
                // Sample the actual scrolling content, with icons drawn sharply above the frost.
                if (Build.VERSION.SDK_INT >= 31 && backdrop != null) {
                    frost.record(size = IntSize(size.width.roundToInt(), size.height.roundToInt())) {
                        translate(-origin.x, -origin.y) { drawLayer(backdrop) }
                    }
                    frost.renderEffect = BlurEffect(22.dp.toPx(), 22.dp.toPx(), TileMode.Clamp)
                    drawLayer(frost)
                }
                drawRoundRect(GlassNavbarBg, cornerRadius = CornerRadius(38.dp.toPx()))
                val inset = 5.dp.toPx()
                val cell = (size.width - inset * 2) / tabs.size
                val extraWidth = cell * .1f * lift
                val left = inset + physicalPosition * cell - extraWidth / 2
                val top = inset - 2.dp.toPx() * lift
                val pill = androidx.compose.ui.geometry.Size(cell + extraWidth, size.height - inset * 2 + 4.dp.toPx() * lift)
                drawRoundRect(Brush.verticalGradient(listOf(Color(0x75475768), Color(0x9C182A3D), Color(0x61475E77))),
                    topLeft = Offset(left, top), size = pill, cornerRadius = CornerRadius(33.dp.toPx()))
                val light = ambient?.value ?: 0f
                val glint = .5f + .28f * kotlin.math.sin(light * 2 * Math.PI).toFloat()
                drawRoundRect(Brush.radialGradient(listOf(Color(0xFFE2F1FF).copy(alpha = .045f + .14f * lift), Color.Transparent),
                    center = Offset(left + pill.width * glint, top + pill.height * .25f), radius = pill.width),
                    topLeft = Offset(left, top), size = pill, cornerRadius = CornerRadius(33.dp.toPx()))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xA6E3F3FF), Color(0x126C91B0), Color(0x827C9EBC))),
                    topLeft = Offset(left, top), size = pill, cornerRadius = CornerRadius(33.dp.toPx()), style = Stroke((1f + .5f * lift).dp.toPx()))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0x54FFFFFF), Color(0x08FFFFFF), Color(0x26FFFFFF))),
                    cornerRadius = CornerRadius(38.dp.toPx()), style = Stroke(1.dp.toPx()))
                drawContent()
            }) {
            Row(Modifier.fillMaxSize().padding(5.dp).selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    val previewed = index == previewIndex
                    val tint by animateColorAsState(if (previewed) OnSurface else OnSurfaceVariant, label = "navigation tint")
                    val emphasis by animateFloatAsState(if (previewed) 1f else 0f,
                        spring(dampingRatio = .65f, stiffness = 420f), label = "navigation icon settle")
                    Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(33.dp))
                        .selectable(selected = selected, role = Role.Tab, onClick = { onTabSelected(tab) }).testTag("tab_$tab"),
                        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icons[index], null, tint = tint, modifier = Modifier.size(22.dp).graphicsLayer {
                            scaleX = .96f + (.08f + .08f * lift) * emphasis; scaleY = scaleX
                            translationY = -(1f + 2f * lift).dp.toPx() * emphasis
                        })
                        Text(labels[index], color = tint, fontSize = 10.sp, fontWeight = if (previewed) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
            if (cellPx > 0f) {
                // This transparent handle follows the visible pill. Other tabs retain their tap targets.
                Box(Modifier.absoluteOffset { IntOffset((insetPx + physicalPosition * cellPx).roundToInt(), insetPx.roundToInt()) }
                    .width(with(density) { cellPx.toDp() }).height(66.dp).testTag("nav_bubble_drag_handle")
                    .semantics { contentDescription = "Hold and drag the selected tab" }
                    .clickable(interactionSource = bubbleInteraction, indication = null) {
                        val destination = currentPosition.roundToInt().coerceIn(0, 3)
                        if (destination != currentSelectedIndex) selectTab(tabs[destination])
                    }
                    .pointerInput(rtl, cellPx) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                target = currentPosition.coerceIn(0f, 3f)
                                dragging = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                if (dragging) {
                                    val previous = target.roundToInt()
                                    target = (target + amount.x / cellPx * (if (rtl) -1 else 1)).coerceIn(0f, 3f)
                                    if (target.roundToInt() != previous) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDragEnd = {
                                if (dragging) {
                                    val destination = target.roundToInt().coerceIn(0, 3)
                                    target = destination.toFloat()
                                    dragging = false
                                    if (destination != currentSelectedIndex) selectTab(tabs[destination])
                                }
                            },
                            onDragCancel = { target = currentSelectedIndex.toFloat(); dragging = false }
                        )
                    })
            }
        }
    }
}
