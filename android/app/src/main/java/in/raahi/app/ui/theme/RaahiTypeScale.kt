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
    val TextPrimary = Color(0xFF202124)
    val TextSecondary = Color(0xFF5F6368)
    val TextMuted = Color(0xFF80868B)
    val SurfaceElevated = Color(0xFFFFFFFF)
    val BackgroundSubtle = Color(0xFFFFF9F8)
    val BorderSubtle = Color(0xFFF2E6E6)
    val AccentNavy = Color(0xFF202124)
    val AccentTeal = Color(0xFFE76F73)
    val AccentSecondaryTeal = Color(0xFFF08A8E)
    val AccentMint = Color(0xFFFCE8E8)
    val ActionDestructive = Color(0xFFDC2626)
    val StatusOnline = Color(0xFF16A34A)
    val StatusWarning = Color(0xFFD97706)
}
