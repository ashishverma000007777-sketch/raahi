package `in`.raahi.app.ui.screens.splash

import android.app.Activity
import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import `in`.raahi.app.R
import `in`.raahi.app.ui.theme.RaahiBg
import `in`.raahi.app.ui.theme.RaahiDisplayFont
import `in`.raahi.app.ui.theme.RaahiOrange
import `in`.raahi.app.ui.theme.RaahiPink
import `in`.raahi.app.ui.theme.RaahiText
import `in`.raahi.app.ui.theme.RaahiTextDim
import `in`.raahi.app.ui.theme.RaahiBorderSoft
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

private const val TOTAL_SPLASH_MS = 2000

@Composable
fun RaahiSplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val reduceMotion = remember { animationsDisabled(context) }
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val finish by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            delay(TOTAL_SPLASH_MS.toLong())
        } else {
            progress.animateTo(1f, tween(TOTAL_SPLASH_MS, easing = LinearEasing))
        }
        finish()
    }

    SplashSystemBars()

    val p = progress.value
    val easeFast = remember(p) { FastOutSlowInEasing.transform(p.coerceIn(0f, 1f)) }

    val logoAlpha = if (reduceMotion) 1f else (easeFast / 0.35f).coerceIn(0f, 1f)
    val logoScale = if (reduceMotion) 1f else 0.92f + 0.08f * (easeFast / 0.35f).coerceIn(0f, 1f)

    val wordmarkProg = if (reduceMotion) 1f else ((p - 0.20f) / 0.35f).coerceIn(0f, 1f)
    val wordmarkAlpha = FastOutSlowInEasing.transform(wordmarkProg)

    val tagProg = if (reduceMotion) 1f else ((p - 0.35f) / 0.35f).coerceIn(0f, 1f)
    val tagAlpha = FastOutSlowInEasing.transform(tagProg)

    val lineProgress = if (reduceMotion) 1f else ((p - 0.15f) / 0.80f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF9F5),
                        Color(0xFFFBF4ED),
                        Color(0xFFF7EFE6)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Main Screen 1 Composition
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 36.dp, horizontal = 22.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            // Upper Center: Logo + Wordmark + Tagline (matching Screen 1)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                // Raahi Brand Icon
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .graphicsLayer {
                            alpha = logoAlpha
                            scaleX = logoScale
                            scaleY = logoScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(136.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    RaahiOrange.copy(alpha = 0.25f * logoAlpha),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.width * 0.50f
                            ),
                            radius = size.width * 0.50f
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.ic_splash_logo),
                        contentDescription = "Raahi Logo",
                        modifier = Modifier.size(96.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Wordmark: Raahi
                Text(
                    text = "Raahi",
                    style = TextStyle(
                        fontFamily = RaahiDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        color = RaahiText,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.graphicsLayer {
                        alpha = wordmarkAlpha
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle: Always On Your Road
                Text(
                    text = "Always On Your Road",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp,
                        color = RaahiTextDim,
                        letterSpacing = 0.05.em
                    ),
                    modifier = Modifier.graphicsLayer {
                        alpha = tagAlpha
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Animated Accent Progress Line
                SplashSignalLine(
                    progress = lineProgress,
                    modifier = Modifier.graphicsLayer {
                        alpha = if (reduceMotion) 1f else ((p - 0.15f) / 0.25f).coerceIn(0f, 1f)
                    }
                )
            }

            // Lower Section: 3 Value Pillars and Bottom Cinematic Highway Illustration matching splash screen.png
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 3 Value Pillars: Roadside Help, Trusted Mechanics, Happier Journeys
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SplashPillarItem(
                        icon = Icons.Filled.HealthAndSafety,
                        title = "Roadside Help"
                    )
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(RaahiBorderSoft)
                    )
                    SplashPillarItem(
                        icon = Icons.Filled.Build,
                        title = "Trusted\nMechanics"
                    )
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(RaahiBorderSoft)
                    )
                    SplashPillarItem(
                        icon = Icons.Filled.LocationOn,
                        title = "Happier\nJourneys"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

            }
        }
    }
}

@Composable
private fun SplashPillarItem(icon: ImageVector, title: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(RaahiOrange.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RaahiOrange,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp,
                color = RaahiTextDim,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        )
    }
}


@Composable
private fun SplashSignalLine(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val barWidth = 140.dp
    val barHeight = 3.dp
    val activeFraction = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .width(barWidth)
            .height(barHeight)
            .clip(RoundedCornerShape(barHeight))
            .background(Color(0xFFEDE3D8))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fillW = size.width * activeFraction
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        RaahiOrange,
                        RaahiPink
                    ),
                    startX = 0f,
                    endX = size.width
                ),
                topLeft = Offset.Zero,
                size = Size(fillW, size.height),
                cornerRadius = CornerRadius(size.height, size.height)
            )
        }
    }
}

@Composable
private fun SplashSystemBars() {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val insets = WindowCompat.getInsetsController(window, view)
        insets.isAppearanceLightStatusBars = true
        insets.isAppearanceLightNavigationBars = true
    }
}

private fun animationsDisabled(context: Context): Boolean {
    return runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)
}
