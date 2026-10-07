package `in`.raahi.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.raahi.app.R
import `in`.raahi.app.ui.theme.RaahiDisplayFont
import `in`.raahi.app.ui.theme.RaahiOrange
import `in`.raahi.app.ui.theme.RaahiPink
import `in`.raahi.app.ui.theme.RaahiText

/**
 * Redesigned Raahi Mobility Brand Emblem:
 * An aerodynamic highway interchange symbol:
 * - Dual-lane perspective highway sweeping upwards
 * - Forward navigation trajectory heading into the future
 * - Ambient warm glow and optional subtle shimmer
 * - Standardized brand mark identical across all app touchpoints
 */
@Composable
fun RaahiEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    glow: Boolean = false,
    animated: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "raahiEmblemAnim")
    val pulseGlow by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "emblemPulse"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Soft warm ambient glow behind the mark when enabled
        if (glow) {
            Canvas(modifier = Modifier.size(size * 1.35f)) {
                val w = this.size.width
                val h = this.size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            RaahiOrange.copy(alpha = 0.32f * pulseGlow),
                            RaahiPink.copy(alpha = 0.12f * pulseGlow),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.50f, h * 0.50f),
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = Offset(w * 0.50f, h * 0.50f),
                    blendMode = BlendMode.Plus
                )
            }
        }

        // Master Raahi brand mark
        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo),
            contentDescription = "Raahi",
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (animated) {
                        Modifier.graphicsLayer {
                            scaleX = 0.98f + 0.02f * pulseGlow
                            scaleY = 0.98f + 0.02f * pulseGlow
                        }
                    } else {
                        Modifier
                    }
                )
        )
    }
}

/**
 * Clean, modern Raahi wordmark with matching typography and accent.
 */
@Composable
fun RaahiWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    color: Color = RaahiText,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Raahi",
            fontFamily = RaahiDisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Standard brand header row component combining the emblem and wordmark.
 */
@Composable
fun RaahiBrandHeader(
    modifier: Modifier = Modifier,
    emblemSize: Dp = 28.dp,
    wordmarkSize: TextUnit = 18.sp,
    showWordmark: Boolean = true,
    animated: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RaahiEmblem(
            size = emblemSize,
            glow = true,
            animated = animated
        )
        if (showWordmark) {
            Spacer(modifier = Modifier.width(9.dp))
            RaahiWordmark(fontSize = wordmarkSize)
        }
    }
}
