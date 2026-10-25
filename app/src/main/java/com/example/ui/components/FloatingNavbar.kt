package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun FloatingNavbar(activeTab: String, onTabSelected: (String) -> Unit, modifier: Modifier = Modifier,
    backdrop: GraphicsLayer? = null, backdropOrigin: Offset = Offset.Zero) {
    val tabs = listOf("home", "upcoming", "completed", "settings")
    val labels = listOf("Today", "Schedule", "Done", "Settings")
    val icons = listOf(Icons.Outlined.Home, Icons.Outlined.CalendarToday, Icons.Outlined.CheckCircle, Icons.Outlined.Tune)
    val selectedIndex = tabs.indexOf(activeTab).coerceAtLeast(0)
    val position by animateFloatAsState(selectedIndex.toFloat(), spring(dampingRatio = .86f, stiffness = 480f), label = "glass selection")
    var origin by remember { mutableStateOf(Offset.Zero) }
    val frost = rememberGraphicsLayer()
    val shape = RoundedCornerShape(38.dp)
    Box(modifier.widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp)) {
        Box(Modifier.fillMaxWidth().height(76.dp).shadow(22.dp, shape, spotColor = Color.Black)
            .clip(shape).onGloballyPositioned { origin = it.positionInRoot() - backdropOrigin }
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
                val left = inset + position * cell
                val pill = androidx.compose.ui.geometry.Size(cell, size.height - inset * 2)
                drawRoundRect(Brush.verticalGradient(listOf(Color(0x75475768), Color(0x9C182A3D), Color(0x61475E77))),
                    topLeft = Offset(left, inset), size = pill, cornerRadius = CornerRadius(33.dp.toPx()))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xA6E3F3FF), Color(0x126C91B0), Color(0x827C9EBC))),
                    topLeft = Offset(left, inset), size = pill, cornerRadius = CornerRadius(33.dp.toPx()), style = Stroke(1.dp.toPx()))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0x54FFFFFF), Color(0x08FFFFFF), Color(0x26FFFFFF))),
                    cornerRadius = CornerRadius(38.dp.toPx()), style = Stroke(1.dp.toPx()))
                drawContent()
            }) {
            Row(Modifier.fillMaxSize().padding(5.dp).selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    val tint by animateColorAsState(if (selected) OnSurface else OnSurfaceVariant, label = "navigation tint")
                    Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(33.dp))
                        .selectable(selected = selected, role = Role.Tab, onClick = { onTabSelected(tab) }).testTag("tab_$tab"),
                        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icons[index], null, tint = tint, modifier = Modifier.size(22.dp))
                        Text(labels[index], color = tint, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
