package `in`.raahi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
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
// Raahi Design Tokens — Exact reference mockup styling with full Dark & Light support
// ---------------------------------------------------------------------------------------

data class RaahiColorPalette(
    val bg: Color,
    val bg2: Color,
    val glass: Color,
    val glassStrong: Color,
    val border: Color,
    val borderSoft: Color,
    val text: Color,
    val textDim: Color,
    val textFaint: Color,
    val selectionBg: Color,
    val isDark: Boolean,
)

val RaahiLightPalette = RaahiColorPalette(
    bg = Color(0xFFF6F8FC),
    bg2 = Color(0xFFFFFFFF),
    glass = Color(0xFFFFFFFF),
    glassStrong = Color(0xFFF8FAFD),
    border = Color(0xFFE2E8F0),
    borderSoft = Color(0xFFEDF2F7),
    text = Color(0xFF0F172A),
    textDim = Color(0xFF475569),
    textFaint = Color(0xFF94A3B8),
    selectionBg = Color(0xFFFFF0E8),
    isDark = false,
)

val RaahiDarkPalette = RaahiColorPalette(
    bg = Color(0xFF0B1120),
    bg2 = Color(0xFF151F32),
    glass = Color(0xFF182338),
    glassStrong = Color(0xFF1E2C45),
    border = Color(0xFF283854),
    borderSoft = Color(0xFF202D45),
    text = Color(0xFFF8FAFC),
    textDim = Color(0xFF94A3B8),
    textFaint = Color(0xFF64748B),
    selectionBg = Color(0xFF332018),
    isDark = true,
)

val LocalRaahiColors = staticCompositionLocalOf { RaahiLightPalette }

// Backgrounds
val RaahiBg: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.bg
val RaahiBg2: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.bg2
val RaahiGlass: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.glass
val RaahiGlassStrong: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.glassStrong
val RaahiBorder: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.border
val RaahiBorderSoft: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.borderSoft

// Text — Deep Navy in Light, Crisp Off-White in Dark
val RaahiText: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.text
val RaahiTextDim: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.textDim
val RaahiTextFaint: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.textFaint
val RaahiIsDark: Boolean @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.isDark


// Accents — Automotive Coral Red (consistent across themes)
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
val RaahiSelectionBg: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.selectionBg
val RaahiSelectionBorder = Color(0xFFFF6B35)

// Backward-compat aliases
val RaahiNavyBackground: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.bg
val RaahiCardBg: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.glass
val RaahiSurfaceHigh: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.glassStrong
val RaahiSecondaryCard: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.bg2
val RaahiCardBorder: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.border
val RaahiOrangeAccent = RaahiOrange
val RaahiOrangeDark = Color(0xFFDC2626)
val RaahiTextPrimary: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.text
val RaahiTextSecondary: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.textDim
val RaahiTextMuted: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.textFaint
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
    ),
    displayMedium = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = RaahiDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = RaahiGlobalFont,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
    ),
)

private val RaahiLightColors = lightColorScheme(
    background = Color(0xFFF5F8FC),
    surface = Color.White,
    primary = RaahiOrange,
    onPrimary = Color.White,
    onBackground = Color(0xFF142039),
    onSurface = Color(0xFF142039),
    outline = Color(0xFFE2E9F3),
    error = RaahiRed,
)

private val RaahiDarkColors = darkColorScheme(
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    primary = RaahiOrange,
    onPrimary = Color.White,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    outline = Color(0xFF334155),
    error = RaahiRed,
)

@Composable
fun RaahiTheme(content: @Composable () -> Unit) {
    val themePreference by ThemeManager.themeFlow.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themePreference) {
        ThemeManager.THEME_DARK -> true
        ThemeManager.THEME_LIGHT -> false
        else -> isSystemDark
    }

    val palette = if (isDark) RaahiDarkPalette else RaahiLightPalette
    val colorScheme = if (isDark) RaahiDarkColors else RaahiLightColors

    CompositionLocalProvider(
        LocalRaahiColors provides palette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RaahiTypography,
            content = content
        )
    }
}
