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
import `in`.raahi.app.network.IntercityTripDto
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
    shape: androidx.compose.ui.graphics.Shape = RaahiShapeLarge,
    background: Color = RaahiGlass,
    borderColor: Color = RaahiBorderSoft,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier
            .shadow(elevation = 2.dp, shape = shape, spotColor = Color(0x10000000), ambientColor = Color(0x06000000))
            .background(background, shape)
            .border(1.dp, borderColor, shape)
            .then(clickMod)
    ) { content() }
}

@Composable
fun RowCard(
    modifier: Modifier = Modifier,
    urgent: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val isDark = LocalRaahiColors.current.isDark
    val bg = if (urgent) (if (isDark) Color(0xFF381A1A) else Color(0xFFFEF2F2)) else RaahiGlass
    val border = if (urgent) RaahiRed.copy(alpha = 0.35f) else RaahiBorderSoft
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RaahiShapeMedium, spotColor = Color(0x10000000), ambientColor = Color(0x06000000))
            .background(bg, RaahiShapeMedium)
            .border(1.dp, border, RaahiShapeMedium)
            .then(clickMod)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
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
                trailing, color = RaahiOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = if (onTrailingClick != null) Modifier.clickable(onClick = onTrailingClick) else Modifier,
            )
        }
    }
}

@Composable
fun RaahiChip(label: String, active: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val bg = if (active) RaahiOrange else RaahiGlass
    val border = if (active) RaahiOrange else RaahiBorder
    val textColor = if (active) Color.White else RaahiTextDim
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(
                if (active) Modifier.shadow(elevation = 4.dp, shape = RaahiShapePill, spotColor = RaahiOrange)
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

    val displayItems = remember(items) {
        if (items.size > 1) items + items + items else items
    }

    val scrollState = rememberScrollState()
    var isUserInteracting by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            isUserInteracting = true
        } else {
            delay(2000)
            isUserInteracting = false
        }
    }

    LaunchedEffect(displayItems, isUserInteracting) {
        if (isUserInteracting) return@LaunchedEffect
        while (true) {
            val max = scrollState.maxValue
            if (max <= 0) {
                delay(300)
                continue
            }
            if (scrollState.value >= max - 20) {
                scrollState.scrollTo(max / 3)
            }
            val remaining = (max - scrollState.value).coerceAtLeast(1)
            val duration = (remaining * 9).coerceIn(2500, 18000)
            scrollState.animateScrollTo(
                value = max,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = LinearEasing
                )
            )
            delay(300)
        }
    }

    val isDark = LocalRaahiColors.current.isDark

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        shape = RoundedCornerShape(10.dp),
        color = RaahiBg2,
        border = BorderStroke(1.dp, RaahiBorderSoft),
        shadowElevation = 0.5.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact LIVE indicator badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF159A68))
                )
                Text(
                    text = "LIVE",
                    color = Color(0xFF159A68),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(RaahiBorderSoft)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState, enabled = true)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                displayItems.forEachIndexed { index, item ->
                    Text(
                        text = item,
                        color = RaahiText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (index < displayItems.lastIndex) {
                        Text(
                            text = "•",
                            color = RaahiTextDim.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
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
    val navBg = if (isDark) Color(0xFF241A1B) else Color.White
    val navBorder = if (isDark) Color(0xFF483537) else Color(0xFFF2E6E6)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(64.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color(0x18E76F73),
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
    val isDark = LocalRaahiColors.current.isDark
    val activeColor = if (isDark) RaahiSecondaryTeal else RaahiDeepTeal
    val inactiveColor = if (isDark) Color(0xFF8B9CB2) else RaahiTextFaint
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
    Box(
        modifier = Modifier
            .size(42.dp)
            .scale(scale)
            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = RaahiDeepTeal)
            .background(if (active) SolidColor(RaahiDeepTeal) else RaahiBrandGradient, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = { onSelect(RaahiTab.AI_MECHANIC) }),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = "AI Mechanic",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
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
    container: Color = RaahiOrange,
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
    tint: Color = RaahiOrange,
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
            focusedBorderColor = RaahiOrange, unfocusedBorderColor = RaahiBorder,
            focusedContainerColor = RaahiGlass, unfocusedContainerColor = RaahiGlass,
            cursorColor = RaahiOrange,
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
        color = if (isOwner) Color(0xFFFCE8E8) else Color(0xFFFFF1F1),
        border = BorderStroke(1.dp, if (isOwner) Color(0xFFE76F73).copy(alpha = 0.4f) else Color(0xFFF08A8E).copy(alpha = 0.4f)),
    ) {
        Text(
            text = if (isOwner) "BY OWNER" else "BY TRAVEL AGENT",
            color = if (isOwner) Color(0xFFE76F73) else Color(0xFFC95357),
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
        )
    }
}

/** Unified surface for premium automotive cards */
@Composable
fun RaahiCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(18.dp),
    color: Color = RaahiGlass,
    border: BorderStroke? = BorderStroke(1.dp, RaahiBorderSoft),
    elevation: Dp = 1.dp,
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
                color = RaahiOrange,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
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
    val cardBg = if (isDark) Color(0xFF172033) else Color.White
    val borderCol = if (isDark) Color(0xFF24344E) else Color(0xFFE2E8F0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderCol),
        shadowElevation = 1.5.dp,
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
                        tint = RaahiOrange,
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
                    if (trip.status != "PUBLISHED") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (trip.status) {
                                "COMPLETED" -> Color(0xFFE8F8F0)
                                "IN_PROGRESS" -> Color(0xFFEAF3FF)
                                else -> RaahiSelectionBg
                            }
                        ) {
                            Text(
                                text = trip.status.replace("_", " "),
                                color = when (trip.status) {
                                    "COMPLETED" -> Color(0xFF22B573)
                                    "IN_PROGRESS" -> Color(0xFF2563EB)
                                    else -> RaahiOrange
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
                    Box(modifier = Modifier.size(7.dp).background(RaahiOrange, CircleShape))
                    Text(
                        text = trip.destAddress.ifBlank { trip.destCity },
                        fontSize = 12.sp,
                        color = RaahiTextDim,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }

            // Bottom stats & Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "₹${trip.fare.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = RaahiOrange,
                    )
                    Text(
                        text = if (trip.rideType == "OWNER") "Direct Owner (0% fee)" else (trip.paymentMode?.replace("_", " ") ?: "Agreed Fare"),
                        fontSize = 11.sp,
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
                        color = RaahiOrange.copy(alpha = 0.10f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Details",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RaahiOrange
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = RaahiOrange
                            )
                        }
                    }
                }
            }
        }
    }
}
