package `in`.raahi.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.raahi.app.R

// ---------------------------------------------------------------------------------------
// Raahi Design Tokens — Exact reference mockup styling (cream / navy / coral automotive)
// ---------------------------------------------------------------------------------------

// Backgrounds
val RaahiBg = Color(0xFFFAF7F2)          // Light warm cream / off-white (page root)
val RaahiBg2 = Color(0xFFFFFFFF)         // Clean white card / surface
val RaahiGlass = Color(0xFFFFFFFF)       // White card surface
val RaahiGlassStrong = Color(0xFFFFFFFF) // Elevated white card surface
val RaahiBorder = Color(0xFFEDE8E1)      // Soft subtle warm border
val RaahiBorderSoft = Color(0xFFF3EFEA)  // Soft light border

// Text — Deep Navy Typography
val RaahiText = Color(0xFF111827)        // Deep navy primary text
val RaahiTextDim = Color(0xFF475569)     // Slate navy secondary text
val RaahiTextFaint = Color(0xFF94A3B8)   // Soft muted slate text

// Accents — Automotive Coral Red
val RaahiOrange = Color(0xFFFF4B3A)      // Vibrant automotive coral red
val RaahiAmber = Color(0xFFF59E0B)       // Warm Amber
val RaahiPink = Color(0xFFFF3366)        // Subtle pink/coral
val RaahiGreen = Color(0xFF10B981)       // Emerald Green (completed / healthy)
val RaahiCyan = Color(0xFF0EA5E9)        // Sky Cyan
val RaahiVioletAccent = Color(0xFF7C5CFF)
val RaahiRed = Color(0xFFEF4444)         // Emergency Red (SOS)

// Coral / Selection tokens
val RaahiCoral = Color(0xFFFF4B3A)
val RaahiCoralGlow = Color(0x33FF4B3A)
val RaahiSelectionBg = Color(0xFFFFF0ED)
val RaahiSelectionBorder = Color(0xFFFF4B3A)

// Backward-compat aliases
val RaahiNavyBackground = RaahiBg
val RaahiCardBg = Color(0xFFFFFFFF)
val RaahiSurfaceHigh = Color(0xFFF8F5EE)
val RaahiSecondaryCard = Color(0xFFFCFAF7)
val RaahiCardBorder = RaahiBorder
val RaahiOrangeAccent = RaahiOrange
val RaahiOrangeDark = Color(0xFFDC2626)
val RaahiTextPrimary = RaahiText
val RaahiTextSecondary = RaahiTextDim
val RaahiTextMuted = RaahiTextFaint
val RaahiYellow = RaahiAmber
val RaahiWarningOrange = RaahiAmber

// Signature gradients
val RaahiBrandGradient = Brush.horizontalGradient(listOf(Color(0xFFFF5242), Color(0xFFFF3366)))
val RaahiAiGradient = Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF7C5CFF)))
fun raahiHeroGradient() = Brush.linearGradient(
    listOf(Color(0xFFFFF6F0), Color(0xFFFFFFFF))
)

// Shape system: rounded corners 14–22dp, pill for chips/badges.
val RaahiRadiusSmall = 12.dp
val RaahiRadiusMedium = 16.dp
val RaahiRadiusLarge = 22.dp
val RaahiShapeSmall = RoundedCornerShape(RaahiRadiusSmall)
val RaahiShapeMedium = RoundedCornerShape(RaahiRadiusMedium)
val RaahiShapeLarge = RoundedCornerShape(RaahiRadiusLarge)
val RaahiShapePill = RoundedCornerShape(50)

// --- Typography: Unified Noto Sans with complete English + Hindi/Devanagari glyphs ---
private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private fun googleFontFamily(name: String): FontFamily {
    val font = GoogleFont(name)
    return FontFamily(
        Font(googleFont = font, fontProvider = fontProvider, weight = FontWeight.Normal),
        Font(googleFont = font, fontProvider = fontProvider, weight = FontWeight.Medium),
        Font(googleFont = font, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = font, fontProvider = fontProvider, weight = FontWeight.Bold),
    )
}

/** Global Noto Sans font — unified across English and Devanagari */
val RaahiGlobalFont: FontFamily = runCatching { googleFontFamily("Noto Sans") }
    .getOrDefault(FontFamily.Default)

val RaahiDisplayFont: FontFamily = RaahiGlobalFont
val RaahiBodyFont: FontFamily = RaahiGlobalFont

val RaahiTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        color = RaahiText,
    ),
    displayMedium = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        color = RaahiText,
    ),
    displaySmall = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = RaahiText,
    ),
    headlineLarge = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = RaahiText,
    ),
    headlineMedium = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = RaahiText,
    ),
    headlineSmall = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = RaahiText,
    ),
    titleLarge = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = RaahiText,
    ),
    titleMedium = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = RaahiText,
    ),
    titleSmall = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = RaahiText,
    ),
    bodyLarge = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = RaahiText,
    ),
    bodyMedium = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        color = RaahiTextDim,
    ),
    bodySmall = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = RaahiTextFaint,
    ),
    labelLarge = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = RaahiText,
    ),
    labelMedium = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = RaahiTextDim,
    ),
    labelSmall = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        color = RaahiTextFaint,
    ),
)

private val RaahiLightColors = lightColorScheme(
    background = RaahiBg,
    surface = Color.White,
    primary = RaahiOrange,
    onPrimary = Color.White,
    onBackground = RaahiText,
    onSurface = RaahiText,
    outline = RaahiBorder,
    error = RaahiRed,
)

@Composable
fun RaahiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RaahiLightColors,
        typography = RaahiTypography,
        content = content
    )
}
