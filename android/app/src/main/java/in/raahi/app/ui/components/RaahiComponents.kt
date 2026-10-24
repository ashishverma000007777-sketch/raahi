package `in`.raahi.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import `in`.raahi.app.network.IntercityTripDto
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.MutatePriority
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import `in`.raahi.app.ui.theme.*

/**
 * Shared UI components implementing the new visual design system:
 * Light cream/off-white background, crisp white rounded cards with subtle drop shadows,
 * deep navy typography, and coral-red automotive accents.
 */

// ---------------------------------------------------------------------- Card surfaces ---

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RaahiCardShape,
    background: Color = RaahiGlass,
    borderColor: Color = RaahiBorderSoft,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier.then(clickMod),
        shape = shape,
        color = background,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 3.dp,
    ) {
        Column { content() }
    }
}

@Composable
fun RowCard(
    modifier: Modifier = Modifier,
    urgent: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val isDark = LocalRaahiColors.current.isDark
    val bg = if (urgent) (if (isDark) Color(0xFF381A1A) else Color(0xFFFEF2F2)) else (if (isDark) Color(0xFF1E2226) else Color(0xFFFFFFFF))
    val border = if (urgent) RaahiRed.copy(alpha = 0.35f) else (if (isDark) Color(0xFF2E353D) else Color(0xFFE2E8F0))
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier.fillMaxWidth().then(clickMod),
        shape = RaahiShapeMedium,
        color = bg,
        border = BorderStroke(1.dp, border),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun OfflineStatusBanner(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RaahiShapeMedium, spotColor = Color(0x10000000))
            .background(Color(0xFFFFFBEB), RaahiShapeMedium)
            .border(1.dp, RaahiAmber.copy(alpha = 0.35f), RaahiShapeMedium)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = RaahiAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                color = Color(0xFF92400E),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .background(RaahiAmber.copy(alpha = 0.18f), RaahiShapePill)
                        .border(1.dp, RaahiAmber.copy(alpha = 0.35f), RaahiShapePill)
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Retry",
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Retry",
                        color = Color(0xFFB45309),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, size: Dp = 32.dp, shape: androidx.compose.ui.graphics.Shape = RaahiShapeSmall) {
    Box(
        modifier = Modifier.size(size).background(tint.copy(alpha = 0.12f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun SectionLabel(text: String, trailing: String? = null, onTrailingClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = RaahiTextDim, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
        if (trailing != null) {
            Text(
                trailing, color = RaahiText, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = if (onTrailingClick != null) Modifier.clickable(onClick = onTrailingClick) else Modifier,
            )
        }
    }
}

@Composable
fun RaahiChip(label: String, active: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val bg = if (active) RaahiPrimaryTeal else RaahiGlass
    val border = if (active) RaahiPrimaryTeal else RaahiBorder
    val textColor = if (active) Color.White else RaahiTextDim
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(
                if (active) Modifier.shadow(elevation = 4.dp, shape = RaahiShapePill, spotColor = RaahiPrimaryTeal)
                else Modifier
            )
            .background(bg, RaahiShapePill)
            .border(1.dp, border, RaahiShapePill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ------------------------------------------------------------------------- Score ring ----

/** Circular Car Health score indicator matching the reference donut chart design */
@Composable
fun ScoreRing(score: Int, ringSize: Dp = 56.dp, big: Boolean = false) {
    val color = when {
        score >= 70 -> RaahiGreen
        score >= 40 -> RaahiAmber
        else -> RaahiRed
    }
    val sweep = (score.coerceIn(0, 100) / 100f) * 360f
    Box(modifier = Modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = size.minDimension * 0.13f, cap = StrokeCap.Round)
            drawArc(color = Color(0xFFF1F5F9), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(color = color, startAngle = -90f, sweepAngle = sweep, useCenter = false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$score%", color = RaahiText, fontFamily = RaahiDisplayFont,
                fontWeight = FontWeight.Bold, fontSize = if (big) 18.sp else 13.sp,
            )
            Text(
                if (score >= 70) "Good" else if (score >= 40) "Fair" else "Alert",
                color = color,
                fontSize = if (big) 9.sp else 7.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------------------------------------------------------------------------- Ticker -----

@Composable
fun Ticker(items: List<String>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return

    val scrollState = rememberScrollState()
    var isUserInteracting by remember { mutableStateOf(false) }
    var singleCycleWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val speedPxPerSec = with(density) { 36.dp.toPx() } // Constant buttery-smooth speed (~36 dp/s)

    // User drag detection: pause continuous auto-scroll during touch, resume 1.2s after user lifts finger
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            isUserInteracting = true
        } else {
            delay(1200)
            isUserInteracting = false
        }
    }

    // Continuous buttery-smooth frame-by-frame loop with zero jumps and zero pauses
    LaunchedEffect(singleCycleWidthPx, isUserInteracting) {
        if (singleCycleWidthPx <= 0 || isUserInteracting) return@LaunchedEffect

        var lastNanoTime = 0L
        var subPixelAccumulator = 0f

        while (isActive && !isUserInteracting) {
            try {
                scrollState.scroll(MutatePriority.Default) {
                    lastNanoTime = 0L
                    while (isActive && !isUserInteracting) {
                        withFrameNanos { frameTimeNanos ->
                            if (lastNanoTime != 0L) {
                                val dt = (frameTimeNanos - lastNanoTime) / 1_000_000_000f
                                val safeDt = dt.coerceIn(0f, 0.05f)
                                val deltaPx = speedPxPerSec * safeDt + subPixelAccumulator
                                val intDelta = deltaPx.toInt()
                                subPixelAccumulator = deltaPx - intDelta

                                if (intDelta > 0) {
                                    scrollBy(intDelta.toFloat())
                                }

                                // Seamless loop: when scrolled past 1 complete cycle, subtract cycle width.
                                // Because cycle 1 and cycle 2 are identical, this visual shift is completely imperceptible.
                                if (singleCycleWidthPx > 0 && scrollState.value >= singleCycleWidthPx) {
                                    scrollBy(-singleCycleWidthPx.toFloat())
                                }
                            }
                            lastNanoTime = frameTimeNanos
                        }
                    }
                }
            } catch (_: Exception) {
                // Suspended by higher priority UserInput or recomposition
            }
        }
    }

    val isDark = LocalRaahiColors.current.isDark
    val tickerBg = if (isDark) Color(0xFF1C1F23) else Color(0xFFFFFDF9)
    val tickerBorder = if (isDark) Color(0xFF382A1C) else Color(0xFFFFE0B2)
    val textColor = if (isDark) Color(0xFFF4F6F7) else Color(0xFF1E293B)
    val dividerColor = if (isDark) Color(0xFF3A2D20) else Color(0xFFFFD180).copy(alpha = 0.6f)
    val dotColor = Color(0xFFEA580C)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x18F57C00),
                ambientColor = Color(0x08000000)
            ),
        shape = RoundedCornerShape(16.dp),
        color = tickerBg,
        border = BorderStroke(1.dp, tickerBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact LIVE indicator badge with refined pill and green dot
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isDark) Color(0xFF122C1E) else Color(0xFFDCFCE7),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1B4E32) else Color(0xFF86EFAC)),
                modifier = Modifier.padding(end = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF15803D))
                    )
                    Text(
                        text = "LIVE",
                        color = Color(0xFF15803D),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Box(
                Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(dividerColor)
            )

            val spacingPx = with(density) { 14.dp.roundToPx() }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds()
                    .horizontalScroll(scrollState, enabled = true)
                    .padding(start = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cycle 1 (Measured for seamless wrap-around)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        if (coordinates.size.width > 0) {
                            singleCycleWidthPx = coordinates.size.width + spacingPx
                        }
                    }
                ) {
                    items.forEach { item ->
                        Text(
                            text = item,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "•",
                            color = dotColor,
                            fontSize = 11.sp
                        )
                    }
                }

                // Cycle 2 (Continuous continuation)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items.forEach { item ->
                        Text(
                            text = item,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "•",
                            color = dotColor,
                            fontSize = 11.sp
                        )
                    }
                }

                // Cycle 3 (Buffer for wide screens)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items.forEachIndexed { index, item ->
                        Text(
                            text = item,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (index < items.lastIndex) {
                            Text(
                                text = "•",
                                color = dotColor,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------ Bottom nav -----

enum class RaahiTab(val label: String) {
    HOME("Home"),
    SERVICES("Services"),
    AI_MECHANIC("AI"),
    GARAGE("Garage"),
    PROFILE("Profile");

    companion object {
        val ASSISTANCE: RaahiTab get() = SERVICES
        val MECHANICS: RaahiTab get() = SERVICES
        val SHOP: RaahiTab get() = GARAGE
        val TRIPS: RaahiTab get() = HOME
    }
}

/**
 * Restored floating white pill bottom navigation bar matching the original Raahi design:
 * Rounded pill container (28dp corners), 8dp elevation, 16dp horizontal spacing,
 * bouncy tab scaling, and elevated center AI button with RaahiBrandGradient.
 */
@Composable
fun RaahiBottomNavBar(
    current: RaahiTab,
    onSelect: (RaahiTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalRaahiColors.current.isDark
    val navBg = if (isDark) Color(0xFF222629) else Color.White
    val navBorder = if (isDark) Color(0xFF2E3338) else Color(0xFFE3E7E9)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(64.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color(0x18000000),
                ambientColor = Color(0x0A000000)
            )
            .background(navBg, RoundedCornerShape(28.dp))
            .border(1.dp, navBorder, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(RaahiTab.HOME, Icons.Outlined.Home, "Home", current, onSelect)
            NavItem(RaahiTab.SERVICES, Icons.Outlined.Build, "Services", current, onSelect)
            CenterNavItem(current, onSelect)
            NavItem(RaahiTab.GARAGE, Icons.Outlined.DirectionsCar, "Garage", current, onSelect)
            NavItem(RaahiTab.PROFILE, Icons.Outlined.Person, "Profile", current, onSelect)
        }
    }
}

@Composable
private fun NavItem(
    tab: RaahiTab,
    icon: ImageVector,
    label: String,
    current: RaahiTab,
    onSelect: (RaahiTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = tab == current
    val activeColor = Color(0xFFF57C00)
    val inactiveColor = Color(0xFF737D84)
    val tint = if (active) activeColor else inactiveColor
    val scale by animateFloatAsState(
        targetValue = if (active) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "navIconScale",
    )
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = { onSelect(tab) })
            .padding(vertical = 2.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier
                .size(19.dp)
                .scale(if (active) scale else 1f),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = tint,
            fontSize = 9.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun CenterNavItem(current: RaahiTab, onSelect: (RaahiTab) -> Unit) {
    val active = current == RaahiTab.AI_MECHANIC
    val scale by animateFloatAsState(
        targetValue = if (active) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "navCenterScale",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val centerBg = if (active) Color(0xFFF57C00) else Color(0xFF17191B)
    Box(
        modifier = Modifier
            .size(42.dp)
            .scale(scale)
            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0x30000000))
            .background(centerBg, CircleShape)
            .border(1.dp, if (active) Color(0xFFF57C00) else Color(0xFF2E3338), CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = { onSelect(RaahiTab.AI_MECHANIC) }),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = "AI Mechanic",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Circular red floating SOS emergency button positioned above the bottom navigation bar.
 */
@Composable
fun RaahiFloatingSosButton(
    onSos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(50.dp)
            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0x50D94343))
            .background(Color(0xFFD94343), CircleShape)
            .clickable(onClick = onSos),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = "Emergency SOS",
                tint = Color.White,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = "SOS",
                color = Color.White,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 9.sp,
            )
        }
    }
}

// ----------------------------------------------------------------------- Misc icons ------

object RaahiIcons {
    val Wrench: ImageVector get() = Icons.Filled.Build
    val ArrowRight: ImageVector get() = Icons.AutoMirrored.Outlined.ArrowForward
}

// ------------------------------------------------------------ Shared form / button kit ---
// Generic building blocks of the Raahi design system (peach + coral). Used by every screen that
// needs a primary action or a text field, including the helper flows: there is no separate helper kit.

@Composable
fun RaahiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    container: Color = RaahiPrimaryButton,
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(if (enabled) Modifier.shadow(elevation = 4.dp, shape = RoundedCornerShape(18.dp), spotColor = container.copy(alpha = 0.45f)) else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = container,
            disabledContainerColor = container.copy(alpha = 0.35f),
        ),
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp,
            )
        } else {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun RaahiOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = RaahiPrimaryButton,
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = if (enabled) 0.6f else 0.25f)),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = RaahiGlass, contentColor = tint),
    ) {
        Text(text, color = tint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun RaahiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    prefix: String? = null,
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = RaahiTextFaint, fontSize = 13.5.sp) },
        prefix = prefix?.let { { Text(it, color = RaahiText, fontWeight = FontWeight.Bold) } },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth().shadow(elevation = 1.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0x0A000000)),
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedTextColor = RaahiText, unfocusedTextColor = RaahiText,
            focusedBorderColor = RaahiPrimaryTeal, unfocusedBorderColor = RaahiBorder,
            focusedContainerColor = RaahiGlass, unfocusedContainerColor = RaahiGlass,
            cursorColor = RaahiPrimaryTeal,
        ),
    )
}

/** Standard screen title row: back arrow + bold title (+ optional trailing slot). */
@Composable
fun RaahiScreenHeader(title: String, onBack: (() -> Unit)?, subtitle: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = RaahiText)
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = RaahiText, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
            if (subtitle != null) Text(subtitle, color = RaahiTextDim, fontSize = 12.sp)
        }
        trailing()
    }
}

/** Small coloured status pill (job / application status). */
@Composable
fun RaahiStatusPill(label: String, tint: Color) {
    Text(
        label,
        color = tint, fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.background(tint.copy(alpha = 0.12f), RaahiShapePill).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Distinct badge for Owner vs Travel Agent rides */
@Composable
fun RaahiRideTypeBadge(rideType: String?, modifier: Modifier = Modifier) {
    val isOwner = rideType == null || rideType.equals("OWNER", ignoreCase = true)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF4F6F7),
        border = BorderStroke(1.dp, Color(0xFFE3E7E9)),
    ) {
        Text(
            text = if (isOwner) "By Owner" else "By Agent",
            color = RaahiText,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            letterSpacing = 0.3.sp,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
        )
    }
}

/** Unified surface for premium automotive cards */
@Composable
fun RaahiCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RaahiCardShape,
    color: Color = RaahiGlass,
    border: BorderStroke? = BorderStroke(1.dp, RaahiBorderSoft),
    elevation: Dp = RaahiCardElevation,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = color,
        border = border,
        shadowElevation = elevation,
        content = { Column(content = content) }
    )
}

/** Section header with optional action button */
@Composable
fun RaahiSectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = RaahiText,
                fontFamily = RaahiDisplayFont,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = RaahiTextDim,
                )
            }
        }
        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = RaahiText,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

private fun formatTripTime(isoTime: String?): String {
    if (isoTime.isNullOrBlank()) return "--"
    return try {
        val instant = java.time.Instant.parse(isoTime)
        val zdt = instant.atZone(java.time.ZoneId.systemDefault())
        zdt.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM • HH:mm"))
    } catch (_: Exception) {
        isoTime.take(16).replace("T", " ")
    }
}

/** Reusable premium intercity trip card */
@Composable
fun RaahiTripCard(
    trip: IntercityTripDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalRaahiColors.current.isDark
    val cardBg = if (isDark) Color(0xFF222629) else Color.White
    val borderCol = if (isDark) Color(0xFF2E3338) else Color(0xFFE2E8F0)
    val isOwner = trip.rideType == "OWNER"
    val isRoundTrip = trip.tripType == "ROUND_TRIP"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, if (isOwner) borderCol else Color(0xFFDDD6FE)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Route + Ride Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = trip.pickupCity,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = RaahiText,
                        maxLines = 1,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = RaahiCoralPrimary,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = trip.destCity,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = RaahiText,
                        maxLines = 1,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RaahiRideTypeBadge(trip.rideType)
                    if (isRoundTrip) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RaahiCoralLight,
                            border = BorderStroke(1.dp, RaahiCoralPrimary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Outlined.Sync, contentDescription = null, tint = RaahiCoralDark, modifier = Modifier.size(11.dp))
                                Text(
                                    text = "Round Trip",
                                    color = RaahiCoralDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }
                    if (trip.status != "PUBLISHED") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (trip.status) {
                                "COMPLETED" -> Color(0xFFE8F8F0)
                                "IN_PROGRESS" -> Color(0xFFEAF3FF)
                                else -> if (isDark) Color(0xFF2A2E32) else Color(0xFFEDF0F2)
                            }
                        ) {
                            Text(
                                text = trip.status.replace("_", " "),
                                color = when (trip.status) {
                                    "COMPLETED" -> Color(0xFF22B573)
                                    "IN_PROGRESS" -> Color(0xFF2563EB)
                                    else -> RaahiText
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // Route addresses snippet
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).background(Color(0xFF22B573), CircleShape))
                    Text(
                        text = trip.pickupAddress.ifBlank { trip.pickupCity },
                        fontSize = 12.sp,
                        color = RaahiTextDim,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).background(RaahiCoralPrimary, CircleShape))
                    Text(
                        text = trip.destAddress.ifBlank { trip.destCity },
                        fontSize = 12.sp,
                        color = RaahiTextDim,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }

            // Schedule & Tour Details Snippet
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color(0xFF1B1E21) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2E3338) else Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Departure: ${formatTripTime(trip.departureTime)}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = RaahiText
                            )
                        }
                        if (isRoundTrip && trip.returnTime != null) {
                            Text(
                                text = "Return: ${formatTripTime(trip.returnTime)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RaahiCoralDark
                            )
                        }
                    }

                    if (isRoundTrip && !trip.specialInstructions.isNullOrBlank()) {
                        Text(
                            text = trip.specialInstructions.lineSequence().firstOrNull() ?: trip.specialInstructions,
                            fontSize = 11.sp,
                            color = RaahiTextDim,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Driver benefit badge for OWNER rides
            if (isOwner) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF15803D), CircleShape))
                        Text(
                            "0% Commission • Driver keeps 100% fare in direct cash",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }

            // Bottom stats & Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val payoutAmount = if (isOwner) trip.fare else (trip.driverPayout ?: trip.fare)
                    Text(
                        text = "₹${payoutAmount.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = if (isOwner) Color(0xFF15803D) else (if (isDark) Color(0xFFF4F6F7) else Color(0xFF25282D)),
                    )
                    Text(
                        text = if (isOwner) "Net Cash Payout" else "Customer ₹${(trip.customerFare ?: trip.fare).toInt()} • Comm ₹${(trip.agentCommission ?: 0.0).toInt()}",
                        fontSize = 10.5.sp,
                        color = RaahiTextDim,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF1E2D44) else Color(0xFFF1F5F9),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = RaahiTextDim
                            )
                            Text(
                                text = trip.vehicleCategory,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RaahiText
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RaahiCoralPrimary,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "View Trip",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
