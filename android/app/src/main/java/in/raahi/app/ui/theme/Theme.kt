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
    val primary: Color,
    val secondary: Color,
    val mint: Color,
    val isDark: Boolean,
)

val RaahiLightPalette = RaahiColorPalette(
    bg = Color(0xFFFFF9F8),
    bg2 = Color(0xFFFFFFFF),
    glass = Color(0xFFFFFFFF),
    glassStrong = Color(0xFFFDF2F2),
    border = Color(0xFFF2E6E6),
    borderSoft = Color(0xFFF9EFEF),
    text = Color(0xFF202124),
    textDim = Color(0xFF5F6368),
    textFaint = Color(0xFF80868B),
    selectionBg = Color(0xFFFCE8E8),
    primary = Color(0xFFE76F73),
    secondary = Color(0xFFF08A8E),
    mint = Color(0xFFFCE8E8),
    isDark = false,
)

val RaahiDarkPalette = RaahiColorPalette(
    bg = Color(0xFF1C1314),
    bg2 = Color(0xFF241A1B),
    glass = Color(0xFF2D2021),
    glassStrong = Color(0xFF38292A),
    border = Color(0xFF483537),
    borderSoft = Color(0xFF3E2C2E),
    text = Color(0xFFF8FAFC),
    textDim = Color(0xFFD1C2C3),
    textFaint = Color(0xFFA8989A),
    selectionBg = Color(0xFF422123),
    primary = Color(0xFFE76F73),
    secondary = Color(0xFFF08A8E),
    mint = Color(0xFFFCE8E8),
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

// Text — Charcoal #202124 in Light
val RaahiText: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.text
val RaahiTextDim: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.textDim
val RaahiTextFaint: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.textFaint
val RaahiIsDark: Boolean @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.isDark

// ---------------------------------------------------------------------------------------
// Raahi Brand Tokens — Light Coral / Red Theme (#E76F73, #FCE8E8, #FFF9F8, #202124)
// ---------------------------------------------------------------------------------------
val RaahiCoralPrimary = Color(0xFFE76F73)      // Primary Light Coral/Red #E76F73
val RaahiCoralLight = Color(0xFFFCE8E8)        // Light Coral #FCE8E8
val RaahiLightCoral = Color(0xFFFCE8E8)        // Light Coral #FCE8E8
val RaahiCoralDark = Color(0xFFC95357)         // Deeper Coral
val RaahiCoralSecondary = Color(0xFFF08A8E)    // Secondary Coral #F08A8E
val RaahiLightBg = Color(0xFFFFF9F8)           // Background #FFF9F8
val RaahiWhiteSurface = Color(0xFFFFFFFF)      // Cards #FFFFFF
val RaahiDarkText = Color(0xFF202124)          // Text #202124
val RaahiSecondaryText = Color(0xFF5F6368)     // Secondary Text #5F6368

// Compatibility aliases for existing references across codebase
val RaahiDeepTeal = Color(0xFFE76F73)
val RaahiSecondaryTeal = Color(0xFFF08A8E)
val RaahiMint = Color(0xFFFCE8E8)
val RaahiDarkNavy = Color(0xFF202124)
val RaahiDarkSurface = Color(0xFF241A1B)

// Semantic State Indicators — GREEN ONLY FOR SEMANTIC STATES (Success / Verified / Online)
val RaahiSuccess = Color(0xFF16A34A)           // Semantic Success #16A34A (Verified, Healthy, Online)
val RaahiGreen = Color(0xFF16A34A)             // Semantic Green #16A34A
val RaahiWarning = Color(0xFFD97706)           // Semantic Warning #D97706 (Amber)
val RaahiAmber = Color(0xFFD97706)             // Warning Amber
val RaahiError = Color(0xFFDC2626)             // Semantic Error #DC2626 (Emergency SOS, Debit)
val RaahiRed = Color(0xFFDC2626)               // Red (Emergency, Debit, Cancel)

// Brand Primary mapping
val RaahiPrimary = Color(0xFFE76F73)
val RaahiOrange = Color(0xFFE76F73)
val RaahiCoral = Color(0xFFE76F73)
val RaahiCoralGlow = Color(0x26E76F73)
val RaahiSelectionBg: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.selectionBg
val RaahiSelectionBorder = Color(0xFFE76F73)

// Supporting accents
val RaahiPink = Color(0xFFF08A8E)
val RaahiCyan = Color(0xFF0EA5E9)
val RaahiBlue = Color(0xFF0284C7)
val RaahiVioletAccent = Color(0xFF7C3AED)
val RaahiWarningOrange = RaahiAmber

// Backward-compat aliases
val RaahiNavyBackground: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.bg
val RaahiCardBg: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.glass
val RaahiSurfaceHigh: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.glassStrong
val RaahiSecondaryCard: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.bg2
val RaahiCardBorder: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.border
val RaahiOrangeAccent = Color(0xFFE76F73)
val RaahiOrangeDark = Color(0xFFC95357)
val RaahiTextPrimary: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.text
val RaahiTextSecondary: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.textDim
val RaahiTextMuted: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.textFaint
val RaahiYellow = RaahiAmber

// Signature Brand Gradients
val RaahiBrandGradient = Brush.horizontalGradient(listOf(Color(0xFFE76F73), Color(0xFFF08A8E)))
val RaahiTealForestGradient = Brush.linearGradient(listOf(Color(0xFFE76F73), Color(0xFFC95357)))
val RaahiAiGradient = Brush.linearGradient(listOf(Color(0xFFE76F73), Color(0xFFF08A8E), Color(0xFFFCE8E8)))
fun raahiHeroGradient() = Brush.linearGradient(listOf(Color(0xFFFCE8E8), Color(0xFFFFF9F8)))

// Shape system: rounded corners 10–22dp, consistent hierarchy.
val RaahiRadiusSmall = 10.dp
val RaahiRadiusMedium = 16.dp
val RaahiRadiusLarge = 20.dp
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
    background = Color(0xFFFFF9F8),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFFE76F73),
    onPrimary = Color.White,
    secondary = Color(0xFFF08A8E),
    onSecondary = Color.White,
    tertiary = Color(0xFFFCE8E8),
    onTertiary = Color(0xFF202124),
    onBackground = Color(0xFF202124),
    onSurface = Color(0xFF202124),
    outline = Color(0xFFF2E6E6),
    error = RaahiError,
)

private val RaahiDarkColors = darkColorScheme(
    background = Color(0xFF1C1314),
    surface = Color(0xFF241A1B),
    primary = Color(0xFFE76F73),
    onPrimary = Color.White,
    secondary = Color(0xFFF08A8E),
    onSecondary = Color.White,
    tertiary = Color(0xFFFCE8E8),
    onTertiary = Color(0xFFF8FAFC),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    outline = Color(0xFF483537),
    error = RaahiError,
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
