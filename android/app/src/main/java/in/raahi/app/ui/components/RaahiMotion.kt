package `in`.raahi.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import `in`.raahi.app.ui.theme.*

/**
 * Reusable subtle futuristic animations for Raahi.
 * Smooth, lightweight, battery-conscious.
 */

/**
 * Content enter animation: subtle fade-in + slide-up.
 */
fun Modifier.raahiContentEnter(delayMs: Int = 0): Modifier = composed {
    val alpha = remember { Animatable(0f) }
    val translateY = remember { Animatable(12f) }

    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        launch {
            alpha.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
        }
        launch {
            translateY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translateY.value
    }
}

/**
 * Subtle button press feedback animation (0.97f scale).
 */
fun Modifier.raahiPressScale(
    interactionSource: MutableInteractionSource? = null,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        scale.animateTo(
            targetValue = if (isPressed) 0.97f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
    }

    val mod = if (onClick != null) {
        Modifier.clickable(interactionSource = source, indication = null, onClick = onClick)
    } else Modifier

    this.scale(scale.value).then(mod)
}

/**
 * Reusable animated 3-dot typing indicator for AI Mechanic.
 * Cycles smoothly through:
 * ● ○ ○
 * ● ● ○
 * ● ● ●
 * ○ ● ●
 */
@Composable
fun RaahiTypingIndicator(
    modifier: Modifier = Modifier,
    dotSize: Dp = 7.dp,
    activeColor: Color = RaahiOrange,
    inactiveColor: Color = Color(0xFFCBD5E1)
) {
    val transition = rememberInfiniteTransition(label = "typingCycle")
    val step by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "step"
    )

    val currentInt = step.toInt().coerceIn(0, 3)
    val dot0Active = currentInt != 3
    val dot1Active = currentInt >= 1
    val dot2Active = currentInt >= 2

    Row(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TypingDot(active = dot0Active, size = dotSize, activeColor = activeColor, inactiveColor = inactiveColor)
        TypingDot(active = dot1Active, size = dotSize, activeColor = activeColor, inactiveColor = inactiveColor)
        TypingDot(active = dot2Active, size = dotSize, activeColor = activeColor, inactiveColor = inactiveColor)
    }
}

@Composable
private fun TypingDot(active: Boolean, size: Dp, activeColor: Color, inactiveColor: Color) {
    val scale = remember { Animatable(if (active) 1.15f else 0.85f) }
    LaunchedEffect(active) {
        scale.animateTo(
            targetValue = if (active) 1.15f else 0.85f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
    }

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale.value)
            .background(if (active) activeColor else inactiveColor, CircleShape)
    )
}

/**
 * Reusable subtle pulsing loading indicator.
 */
@Composable
fun RaahiLoadingDots(
    modifier: Modifier = Modifier,
    color: Color = RaahiOrange
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).graphicsLayer { this.alpha = alpha }.background(color, CircleShape))
        Box(Modifier.size(6.dp).graphicsLayer { this.alpha = (alpha + 0.3f).coerceAtMost(1f) }.background(color, CircleShape))
        Box(Modifier.size(6.dp).graphicsLayer { this.alpha = (alpha + 0.6f).coerceAtMost(1f) }.background(color, CircleShape))
    }
}
