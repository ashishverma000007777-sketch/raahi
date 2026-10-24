package `in`.raahi.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Coherent, professional type scale and design tokens for Raahi mobility components.
 * Enforces strong visual hierarchy, consistent font sizes, balanced line heights,
 * and restrained, accessible colors without introducing new font dependencies.
 */
object RaahiTypeScale {
    // Standard Typography Tokens
    val screenTitle = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    )

    val headlineMedium = screenTitle

    val cardHeader = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    )

    val titleLarge = cardHeader

    val sectionHeader = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )

    val titleMedium = sectionHeader
    val titleSmall = sectionHeader

    val bodyLarge = TextStyle(
        fontFamily = RaahiBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    val bodyMedium = TextStyle(
        fontFamily = RaahiBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )

    val bodySmall = TextStyle(
        fontFamily = RaahiBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )

    val caption = TextStyle(
        fontFamily = RaahiBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )

    val labelUppercase = TextStyle(
        fontFamily = RaahiBodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
    )

    val button = TextStyle(
        fontFamily = RaahiBodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    val buttonMedium = TextStyle(
        fontFamily = RaahiBodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )

    val statNumber = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
    )

    // Restrained Semantic Colors for Raahi 2.0 Brand Tokens
    val TextPrimary = Color(0xFF25282D)
    val TextSecondary = Color(0xFF737D84)
    val TextMuted = Color(0xFF9AA2A7)
    val SurfaceElevated = Color(0xFFFFFFFF)
    val BackgroundSubtle = Color(0xFFF5F6F7)
    val BorderSubtle = Color(0xFFE3E7E9)
    val AccentNavy = Color(0xFF17191B)
    val AccentTeal = Color(0xFFF57C00)
    val AccentAmber = Color(0xFFF57C00)
    val AccentSecondaryTeal = Color(0xFFEF6C00)
    val AccentButton = Color(0xFFEF6C00)
    val AccentMint = Color(0xFFFFF3E0)
    val AccentLight = Color(0xFFFFF3E0)
    val ActionDestructive = Color(0xFFD94343)
    val StatusOnline = Color(0xFF238765)
    val StatusWarning = Color(0xFFD97706)
}
