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
    bg = Color(0xFFF5F6F7),
    bg2 = Color(0xFFFFFFFF),
    glass = Color(0xFFFFFFFF),
    glassStrong = Color(0xFFFFF3E0),
    border = Color(0xFFE3E7E9),
    borderSoft = Color(0xFFEDF0F2),
    text = Color(0xFF25282D),
    textDim = Color(0xFF737D84),
    textFaint = Color(0xFF9AA2A7),
    selectionBg = Color(0xFFFFF3E0),
    primary = Color(0xFFF57C00),
    secondary = Color(0xFFEF6C00),
    mint = Color(0xFFFFF3E0),
    isDark = false,
)

val RaahiDarkPalette = RaahiColorPalette(
    bg = Color(0xFF17191B),
    bg2 = Color(0xFF222629),
    glass = Color(0xFF222629),
    glassStrong = Color(0xFF2A2E32),
    border = Color(0xFF2E3338),
    borderSoft = Color(0xFF222629),
    text = Color(0xFFF4F6F7),
    textDim = Color(0xFFAAB2B8),
    textFaint = Color(0xFF737D84),
    selectionBg = Color(0xFF382A1C),
    primary = Color(0xFFF57C00),
    secondary = Color(0xFFEF6C00),
    mint = Color(0xFFFFF3E0),
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

// Text — Charcoal #25282D in Light
val RaahiText: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.text
val RaahiTextDim: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.textDim
val RaahiTextFaint: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.textFaint
val RaahiIsDark: Boolean @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.isDark

// ---------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------
// Raahi Brand Tokens — Vibrant Orange Theme (#17191B, #F57C00, #EF6C00, #F5F6F7, #25282D)
// ---------------------------------------------------------------------------------------
val RaahiCharcoalHeader = Color(0xFF17191B)   // Dark charcoal header #17191B
val RaahiDarkSurface = Color(0xFF222629)      // Dark surface #222629
val RaahiLightBg = Color(0xFFF5F6F7)          // Light background #F5F6F7
val RaahiWhiteSurface = Color(0xFFFFFFFF)     // White cards #FFFFFF
val RaahiCardWhite = Color(0xFFFFFFFF)        // Card white alias
val RaahiPriceText = Color(0xFF25282D)        // Dedicated black price/fare/earning token #25282D

// Vibrant Orange Core Tokens
val RaahiPrimaryBrand = Color(0xFFF57C00)     // Primary Orange #F57C00
val RaahiPrimaryButton = Color(0xFFEF6C00)    // Button Orange #EF6C00
val RaahiLightAccent = Color(0xFFFFF3E0)      // Light Orange Background #FFF3E0
val RaahiLightOrange = Color(0xFFFFF3E0)      // Light Orange Background alias
val RaahiButtonOrange = Color(0xFFEF6C00)     // Button Orange alias
val RaahiSelectedNav = Color(0xFFF57C00)      // Selected Tab/Icon #F57C00

// Mapped Brand Tokens (Ensures full compatibility across existing screens)
val RaahiPrimaryTeal = Color(0xFFF57C00)      // Primary Orange #F57C00
val RaahiLightTeal = Color(0xFFFFF3E0)        // Light Orange Background #FFF3E0
val RaahiPrimaryText = Color(0xFF25282D)      // Primary text #25282D
val RaahiSecondaryText = Color(0xFF737D84)    // Secondary text #737D84
val RaahiBorderColor = Color(0xFFE3E7E9)      // Borders #E3E7E9
val RaahiSuccess = Color(0xFF238765)          // Success #238765 (DO NOT CHANGE)
val RaahiSosDanger = Color(0xFFD94343)        // SOS danger #D94343 (DO NOT CHANGE)
val RaahiSosRed = Color(0xFFD94343)           // SOS red #D94343 (DO NOT CHANGE)
val RaahiPrimaryTealDark = Color(0xFFEF6C00)  // Button Orange #EF6C00

// Backward compatibility tokens
val RaahiCoralPrimary = Color(0xFFF57C00)
val RaahiCoralLight = Color(0xFFFFF3E0)
val RaahiLightCoral = Color(0xFFFFF3E0)
val RaahiCoralDark = Color(0xFFEF6C00)
val RaahiCoralSecondary = Color(0xFFEF6C00)
val RaahiDarkText = Color(0xFF25282D)

// Compatibility aliases for existing references across codebase
val RaahiDeepTeal = Color(0xFFF57C00)
val RaahiTeal = Color(0xFFF57C00)
val RaahiSecondaryTeal = Color(0xFFEF6C00)
val RaahiMint = Color(0xFFFFF3E0)
val RaahiDarkNavy = Color(0xFF17191B)

// Semantic State Indicators
val RaahiGreen = Color(0xFF238765)            // Success / Online green #238765 (DO NOT CHANGE)
val RaahiWarning = Color(0xFFD97706)
val RaahiAmber = Color(0xFFD97706)
val RaahiError = Color(0xFFD94343)            // Error / SOS red #D94343 (DO NOT CHANGE)
val RaahiRed = Color(0xFFD94343)

// Brand Primary mapping
val RaahiPrimary = Color(0xFFF57C00)
val RaahiOrange = Color(0xFFF57C00)
val RaahiCoral = Color(0xFFF57C00)
val RaahiCoralGlow = Color(0x26F57C00)
val RaahiSelectionBg: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.selectionBg
val RaahiSelectionBorder = Color(0xFFF57C00)

// Supporting accents
val RaahiPink = Color(0xFFF57C00)
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
val RaahiOrangeAccent = Color(0xFFF57C00)
val RaahiOrangeDark = Color(0xFFEF6C00)
val RaahiTextPrimary: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.text
val RaahiTextSecondary: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.textDim
val RaahiTextMuted: Color @Composable @ReadOnlyComposable get() = LocalRaahiColors.current.textFaint
val RaahiYellow = RaahiAmber

// Signature Brand Gradients
val RaahiBrandGradient = Brush.horizontalGradient(listOf(Color(0xFFF57C00), Color(0xFFEF6C00)))
val RaahiTealForestGradient = Brush.linearGradient(listOf(Color(0xFFF57C00), Color(0xFFEF6C00)))
val RaahiAiGradient = Brush.linearGradient(listOf(Color(0xFFF57C00), Color(0xFFEF6C00), Color(0xFFFFF3E0)))
fun raahiHeroGradient() = Brush.linearGradient(listOf(Color(0xFF222629), Color(0xFF17191B)))

// Shape system: rounded corners 10–28dp, consistent automotive hierarchy matching reference-image.jpg.
val RaahiRadiusSmall = 10.dp
val RaahiRadiusMedium = 16.dp
val RaahiRadiusLarge = 20.dp
val RaahiRadiusSheet = 28.dp
val RaahiShapeSmall = RoundedCornerShape(RaahiRadiusSmall)
val RaahiShapeMedium = RoundedCornerShape(RaahiRadiusMedium)
val RaahiShapeLarge = RoundedCornerShape(RaahiRadiusLarge)
val RaahiCardShape = RoundedCornerShape(20.dp)
val RaahiSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val RaahiShapePill = RoundedCornerShape(50)
val RaahiCardElevation = 2.dp


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
    background = Color(0xFFF5F6F7),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFFF57C00),
    onPrimary = Color.White,
    secondary = Color(0xFFEF6C00),
    onSecondary = Color.White,
    tertiary = Color(0xFFFFF3E0),
    onTertiary = Color(0xFF25282D),
    onBackground = Color(0xFF25282D),
    onSurface = Color(0xFF25282D),
    outline = Color(0xFFE3E7E9),
    error = RaahiError,
)

private val RaahiDarkColors = darkColorScheme(
    background = Color(0xFF17191B),
    surface = Color(0xFF222629),
    primary = Color(0xFFF57C00),
    onPrimary = Color.White,
    secondary = Color(0xFFEF6C00),
    onSecondary = Color.White,
    tertiary = Color(0xFFFFF3E0),
    onTertiary = Color(0xFFF4F6F7),
    onBackground = Color(0xFFF4F6F7),
    onSurface = Color(0xFFF4F6F7),
    outline = Color(0xFF2E3338),
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
