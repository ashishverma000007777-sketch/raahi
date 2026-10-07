package `in`.raahi.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
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

// --- Typography -----------------------------------------------------------------------
private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private fun googleFontFamily(name: String): FontFamily {
    val font = GoogleFont(name)
    return FontFamily(
        Font(googleFont = font, fontProvider = fontProvider, weight = FontWeight.Medium),
        Font(googleFont = font, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = font, fontProvider = fontProvider, weight = FontWeight.Bold),
    )
}

/** Headlines, numbers, step badges — Space Grotesk. */
val RaahiDisplayFont: FontFamily = runCatching { googleFontFamily("Space Grotesk") }
    .getOrDefault(FontFamily.Default)

/** Body text everywhere else — Plus Jakarta Sans. */
val RaahiBodyFont: FontFamily = runCatching { googleFontFamily("Plus Jakarta Sans") }
    .getOrDefault(FontFamily.Default)

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
        typography = MaterialTheme.typography,
        content = content
    )
}
