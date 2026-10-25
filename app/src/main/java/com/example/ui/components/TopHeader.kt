package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.*

@Composable
fun TopHeader(currentScreenTitle: String, onQuickActionClick: () -> Unit = {}) {
    val ambient = LocalAmbientPhase.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.size(26.dp)) {
            for (i in 0..2) {
                val x = size.width * (.2f + i * .3f)
                val breath = kotlin.math.sin((ambient?.value ?: 0f) * 2 * Math.PI + i * 1.4).toFloat() * .035f
                val inset = (if (i == 1) .12f else .28f) + breath
                drawLine(Primary, Offset(x, size.height * inset), Offset(x, size.height * (1 - inset)), 3.dp.toPx(), StrokeCap.Round)
            }
        }
        Text("flowmind", color = OnSurface, fontSize = 18.sp, letterSpacing = (-.6).sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (currentScreenTitle != "Settings") RoundAction(Icons.Outlined.Tune, "Open settings", onQuickActionClick)
    }
}
