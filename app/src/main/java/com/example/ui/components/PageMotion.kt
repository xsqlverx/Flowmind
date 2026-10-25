package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Primary
import com.example.ui.theme.PrimaryGlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val LocalAmbientPhase = staticCompositionLocalOf<State<Float>?> { null }
private val LocalPageArrival = staticCompositionLocalOf<State<Float>> { mutableFloatStateOf(1f) }

/** One entrance clock per page: scrolling and ordinary recompositions never restart the entrance. */
@Composable
fun PageScene(tab: String, content: @Composable () -> Unit) {
    val motion = rememberMotionEnabled()
    val arrival = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(tab, motion) {
        if (motion) arrival.animateTo(1f, tween(660, easing = LinearEasing))
        else arrival.snapTo(1f)
    }
    CompositionLocalProvider(LocalPageArrival provides arrival.asState()) {
        Box(Modifier.fillMaxSize().testTag("page_$tab")) { content() }
    }
}

/** A short stagger gives each section its own weight instead of sliding the entire screen. */
@Composable
fun Modifier.pageArrival(order: Int = 0): Modifier {
    val arrival = LocalPageArrival.current
    return graphicsLayer {
        val time = arrival.value * 660f
        val fraction = ((time - order.coerceIn(0, 5) * 52f) / 380f).coerceIn(0f, 1f)
        val settle = FastOutSlowInEasing.transform(fraction)
        alpha = (fraction * 1.7f).coerceIn(0f, 1f)
        translationY = (1f - settle) * (26 + order.coerceIn(0, 5) * 3).dp.toPx()
        scaleX = .975f + .025f * settle
        scaleY = scaleX
        transformOrigin = TransformOrigin(.5f, 0f)
    }
}

/** Low-contrast, moving illumination behind every tab. No particles or invented data. */
@Composable
fun LivingBackdrop(phase: State<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier.testTag("living_backdrop")) {
        val time = phase.value * (2 * PI).toFloat()
        val first = Offset(size.width * (.23f + .19f * sin(time)), size.height * (.16f + .07f * cos(time)))
        val second = Offset(size.width * (.8f + .12f * cos(time)), size.height * (.66f + .14f * sin(time)))
        drawRect(Brush.radialGradient(listOf(Primary.copy(alpha = .085f), Color.Transparent), first, size.width * .92f))
        drawRect(Brush.radialGradient(listOf(PrimaryGlow.copy(alpha = .075f), Color.Transparent), second, size.width * .85f))
    }
}
