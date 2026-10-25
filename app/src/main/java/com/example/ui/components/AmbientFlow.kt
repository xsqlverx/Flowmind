package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Primary
import kotlin.math.PI
import kotlin.math.sin

/** A decorative breath, not a chart of invented activity. Animated state is read only while drawing. */
@Composable
fun AmbientFlow(visible: Boolean, modifier: Modifier = Modifier) {
    val active = rememberMotionEnabled() && rememberAppResumed() && LocalWindowInfo.current.isWindowFocused && visible
    val phase: State<Float> = if (active) {
        rememberInfiniteTransition(label = "ambient flow").animateFloat(0f, 1f,
            infiniteRepeatable(tween(12_000, easing = LinearEasing)), label = "breath phase")
    } else remember { mutableFloatStateOf(0f) }
    Canvas(modifier.fillMaxWidth().height(26.dp).testTag("ambient_flow").clearAndSetSemantics {}) {
        val time = phase.value * (2 * PI).toFloat()
        fun point(x: Float): Offset {
            val envelope = sin(PI * x).toFloat()
            val wave = sin(x * 2 * PI + time).toFloat() * .24f + sin(x * 4 * PI - time).toFloat() * .07f
            return Offset(x * size.width, size.height * (.5f + envelope * wave))
        }
        val path = Path().apply {
            moveTo(0f, size.height / 2)
            for (step in 1..96) { val p = point(step / 96f); lineTo(p.x, p.y) }
        }
        drawLine(Primary.copy(alpha = .06f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
        drawPath(path, Primary.copy(alpha = .04f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, Primary.copy(alpha = .48f), style = Stroke(1.1.dp.toPx(), cap = StrokeCap.Round))
        val marker = point(.72f)
        drawCircle(Primary.copy(alpha = .08f), 5.dp.toPx(), marker)
        drawCircle(Primary.copy(alpha = .8f), 2.dp.toPx(), marker)
    }
}
