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
val RaahiBg = Color(0xFFF5F8FC)          // Light cool blue/white page root
val RaahiBg2 = Color(0xFFFFFFFF)         // Clean white card / surface
val RaahiGlass = Color(0xFFFFFFFF)       // White card surface
val RaahiGlassStrong = Color(0xFFFFFFFF) // Elevated white card surface
val RaahiBorder = Color(0xFFE2E9F3)      // Soft subtle cool border
val RaahiBorderSoft = Color(0xFFEBF0F7)  // Soft light border

// Text — Deep Navy Typography
val RaahiText = Color(0xFF142039)        // Deep navy primary text
val RaahiTextDim = Color(0xFF596780)     // Slate navy secondary text
val RaahiTextFaint = Color(0xFF8B97AA)   // Soft muted slate text

// Accents — Automotive Coral Red
val RaahiOrange = Color(0xFFFF6B35)      // Vibrant automotive coral red
val RaahiAmber = Color(0xFFFFB547)       // Warm Amber
val RaahiPink = Color(0xFFF28BA8)        // Subtle pink/coral
val RaahiGreen = Color(0xFF22B573)       // Emerald Green (completed / healthy)
val RaahiCyan = Color(0xFF4D9EFF)        // Sky Cyan
val RaahiVioletAccent = Color(0xFF8A72F5)
val RaahiRed = Color(0xFFE5484D)         // Emergency Red (SOS)

// Coral / Selection tokens
val RaahiCoral = Color(0xFFFF6B35)
val RaahiCoralGlow = Color(0x26FF6B35)
val RaahiSelectionBg = Color(0xFFFFF0E8)
val RaahiSelectionBorder = Color(0xFFFF6B35)

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
val RaahiBrandGradient = Brush.horizontalGradient(listOf(Color(0xFFFF6B35), Color(0xFFFFA45B)))
val RaahiAiGradient = Brush.linearGradient(listOf(Color(0xFF4D9EFF), Color(0xFF8A72F5)))
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
