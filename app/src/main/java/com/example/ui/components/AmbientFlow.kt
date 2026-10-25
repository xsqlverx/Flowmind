package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Primary
import kotlin.math.PI
import kotlin.math.sin

/** One clock drives the line and progress halo, and disappears when the card cannot be seen. */
@Composable
fun rememberAmbientPhase(visible: Boolean): State<Float> {
    val active = rememberMotionEnabled() && rememberAppResumed() && LocalWindowInfo.current.isWindowFocused && visible
    return if (active) {
        rememberInfiniteTransition(label = "ambient flow").animateFloat(0f, 1f,
            infiniteRepeatable(tween(8_000, easing = LinearEasing)), label = "breath phase")
    } else remember { mutableFloatStateOf(0f) }
}

/** Decorative currents, not invented activity data. State is read only while drawing. */
@Composable
fun AmbientFlow(phase: State<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(34.dp).testTag("ambient_flow").clearAndSetSemantics {}) {
        val time = phase.value * (2 * PI).toFloat()
        fun point(x: Float, echo: Boolean = false): Offset {
            val envelope = sin(PI * x).toFloat()
            val shift = if (echo) 1.6f else 0f
            val wave = sin(x * 2 * PI + time + shift).toFloat() * .28f + sin(x * 4 * PI - time).toFloat() * .08f
            return Offset(x * size.width, size.height * (.5f + envelope * wave))
        }
        fun curve(echo: Boolean = false) = Path().apply {
            moveTo(0f, size.height * .5f)
            for (step in 1..96) { val p = point(step / 96f, echo); lineTo(p.x, p.y) }
        }
        val path = curve()
        val wash = Path().apply {
            addPath(path); lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(wash, Brush.verticalGradient(listOf(Primary.copy(alpha = .065f), Color.Transparent)))
        drawLine(Primary.copy(alpha = .06f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
        drawPath(curve(echo = true), Primary.copy(alpha = .18f), style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, Primary.copy(alpha = .05f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, Primary.copy(alpha = .7f), style = Stroke(1.25.dp.toPx(), cap = StrokeCap.Round))
        val x = (phase.value + .18f) % 1f
        val marker = point(x)
        val fade = sin(PI * x).toFloat().coerceAtLeast(0f)
        drawCircle(Primary.copy(alpha = .12f * fade), 6.dp.toPx(), marker)
        drawCircle(Primary.copy(alpha = .9f * fade), 2.25.dp.toPx(), marker)
    }
}
