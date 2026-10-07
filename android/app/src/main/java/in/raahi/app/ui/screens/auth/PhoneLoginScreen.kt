package `in`.raahi.app.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.ui.components.RaahiEmblem
import `in`.raahi.app.ui.theme.RaahiAmber
import `in`.raahi.app.ui.theme.RaahiBg
import `in`.raahi.app.ui.theme.RaahiBorder
import `in`.raahi.app.ui.theme.RaahiBorderSoft
import `in`.raahi.app.ui.theme.RaahiBrandGradient
import `in`.raahi.app.ui.theme.RaahiDisplayFont
import `in`.raahi.app.ui.theme.RaahiGlass
import `in`.raahi.app.ui.theme.RaahiGlassStrong
import `in`.raahi.app.ui.theme.RaahiOrange
import `in`.raahi.app.ui.theme.RaahiPink
import `in`.raahi.app.ui.theme.RaahiRed
import `in`.raahi.app.ui.theme.RaahiShapeMedium
import `in`.raahi.app.ui.theme.RaahiText
import `in`.raahi.app.ui.theme.RaahiTextDim
import `in`.raahi.app.ui.theme.RaahiTextFaint
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Handles continuous, jump-free 10-digit phone number input.
 * Supports typing, in-place editing, backspace, and pasting formatted numbers
 * without ever accepting an 11th digit.
 */
object PhoneInputHandler {
    fun processInput(current: TextFieldValue, newTfv: TextFieldValue): TextFieldValue {
        val rawInput = newTfv.text
        val digitsOnly = rawInput.filter(Char::isDigit)

        // Case 1: User is at 10 digits and tries to type one more digit (single keystroke)
        if (current.text.length == 10 && newTfv.text.length == 11 && rawInput.all { it.isDigit() }) {
            return current
        }

        // Case 2: Clean 10 or fewer digits entered / edited
        if (rawInput.all { it.isDigit() } && rawInput.length <= 10) {
            return newTfv
        }

        // Case 3: Pasted input or input with formatting/prefixes
        var cleaned = digitsOnly
        if (cleaned.length == 12 && cleaned.startsWith("91")) {
            cleaned = cleaned.substring(2)
        } else if (cleaned.length == 11 && cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1)
        }
        cleaned = cleaned.take(10)
        return TextFieldValue(text = cleaned, selection = TextRange(cleaned.length))
    }
}

@Composable
fun PhoneLoginScreen(
    onOtpSent: (verificationId: String, phone: String) -> Unit,
    onSignedIn: (isNewUser: Boolean) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    var phoneInput by remember { mutableStateOf(TextFieldValue("")) }
    val rawPhone = phoneInput.text
    var errorText by remember { mutableStateOf<String?>(null) }
    var isFocused by remember { mutableStateOf(false) }

    // Logo entrance animation
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.84f) }
    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch {
            logoScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
    }

    LaunchedEffect(state) {
        when (val s = state) {
            is AuthUiState.OtpSent -> {
                errorText = null
                onOtpSent(s.verificationId, s.phone)
            }
            is AuthUiState.SignedIn -> {
                errorText = null
                onSignedIn(s.isNewUser)
            }
            is AuthUiState.Error -> {
                errorText = s.message
            }
            else -> {}
        }
    }

    val isValid = rawPhone.length == 10 && rawPhone.firstOrNull() in '6'..'9'

    // Button press animation
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed) 0.965f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btnScale"
    )

    // Focus animation on input card
    val focusTransition by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(220),
        label = "focusAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RaahiBg)
    ) {
        // Preserved mobility-tech dark atmosphere background
        MobilityTechBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(32.dp))

                // New Raahi Brand Emblem (Heroic, seamless, subtle entrance + glow)
                RaahiEmblem(
                    size = 80.dp,
                    glow = true,
                    animated = true,
                    modifier = Modifier.graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Typography
                Text(
                    text = "Welcome to Raahi",
                    style = TextStyle(
                        fontFamily = RaahiDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = RaahiText
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your phone number to get started with Raahi",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = RaahiTextDim,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Phone Input Card with Focus Animation
                val cardBorderBrush = when {
                    errorText != null -> SolidColor(RaahiRed.copy(alpha = 0.85f))
                    isFocused -> Brush.horizontalGradient(listOf(RaahiOrange, RaahiPink))
                    else -> SolidColor(RaahiBorderSoft)
                }

                val cardBg = androidx.compose.ui.graphics.lerp(RaahiGlass, RaahiGlassStrong, focusTransition)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RaahiShapeMedium)
                        .background(cardBg)
                        .border(1.5.dp, cardBorderBrush, RaahiShapeMedium)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Country Code with Flag
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🇮🇳",
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+91",
                                style = TextStyle(
                                    fontFamily = RaahiDisplayFont,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    color = RaahiText
                                )
                            )
                        }

                        // Subtle Divider
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .width(1.dp)
                                .height(22.dp)
                                .background(RaahiBorder)
                        )

                        // Continuous 10-Digit Mobile Number Input Field
                        BasicTextField(
                            value = phoneInput,
                            onValueChange = { newTfv ->
                                errorText = null
                                phoneInput = PhoneInputHandler.processInput(phoneInput, newTfv)
                            },
                            textStyle = TextStyle(
                                fontFamily = RaahiDisplayFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp,
                                color = RaahiText,
                                letterSpacing = 1.2.sp
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (isValid && state !is AuthUiState.SendingOtp) {
                                        keyboardController?.hide()
                                        val activity = context.findActivity()
                                        if (activity != null) {
                                            viewModel.sendOtp(rawPhone, activity)
                                        }
                                    }
                                }
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(RaahiOrange),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { isFocused = it.isFocused },
                            decorationBox = { innerTextField ->
                                if (phoneInput.text.isEmpty()) {
                                    Text(
                                        text = "7009750326",
                                        style = TextStyle(
                                            fontFamily = RaahiDisplayFont,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 18.sp,
                                            color = RaahiTextFaint,
                                            letterSpacing = 1.2.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )

                        // Clear Button
                        if (phoneInput.text.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(RaahiGlassStrong)
                                    .clickable {
                                        phoneInput = TextFieldValue("")
                                        errorText = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear",
                                    tint = RaahiTextDim,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Error Message Card
                AnimatedVisibility(
                    visible = errorText != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    errorText?.let { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(RaahiRed.copy(alpha = 0.12f))
                                .border(1.dp, RaahiRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = "Error",
                                tint = RaahiRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = msg,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color = RaahiRed,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Primary CTA Button (Kept intact with real OTP dispatch for when auth gate is restored)
                val isSending = state is AuthUiState.SendingOtp
                val buttonModifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                    }
                    .clip(RaahiShapeMedium)
                    .then(
                        if (isValid && !isSending) {
                            Modifier.background(RaahiBrandGradient)
                        } else {
                            Modifier.background(RaahiGlassStrong)
                        }
                    )
                    .clickable(
                        enabled = isValid && !isSending,
                        interactionSource = buttonInteractionSource,
                        indication = null
                    ) {
                        keyboardController?.hide()
                        errorText = null
                        val activity = context.findActivity()
                        if (activity != null) {
                            viewModel.sendOtp(rawPhone, activity)
                        } else {
                            errorText = "OTP service is currently unavailable. Please try again."
                        }
                    }

                Box(
                    modifier = buttonModifier,
                    contentAlignment = Alignment.Center
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Continue",
                                style = TextStyle(
                                    fontFamily = RaahiDisplayFont,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = if (isValid) Color.White else RaahiTextFaint
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                tint = if (isValid) Color.White else RaahiTextFaint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Legal & Security Disclaimer
            Column(
                modifier = Modifier.padding(top = 32.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "By continuing, you agree to Raahi's Terms of Service & Privacy Policy",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = RaahiTextFaint,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}

/**
 * Premium dark mobility-tech background:
 * - Subtle tech grid lines
 * - Faded highway/route curves
 * - Animated traveling route pulse
 * - Waypoint beacons
 * - Soft breathing ambient glow in amber/orange + subtle pink
 */
@Composable
private fun MobilityTechBackground(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "techBgAnim")

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val routeProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "routeSweep"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Soft radial ambient glow (amber/orange + subtle pink, low opacity)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    RaahiOrange.copy(alpha = 0.085f * glowPulse),
                    RaahiPink.copy(alpha = 0.045f * glowPulse),
                    RaahiAmber.copy(alpha = 0.015f * glowPulse),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.22f),
                radius = w * 0.80f
            ),
            center = Offset(w * 0.5f, h * 0.22f),
            radius = w * 0.80f
        )

        // 2. Faded map / tech coordinate grid lines
        val gridStep = 46.dp.toPx()
        var gx = gridStep
        while (gx < w) {
            drawLine(
                color = RaahiBorderSoft.copy(alpha = 0.035f),
                start = Offset(gx, 0f),
                end = Offset(gx, h),
                strokeWidth = 1f
            )
            gx += gridStep
        }
        var gy = gridStep
        while (gy < h) {
            drawLine(
                color = RaahiBorderSoft.copy(alpha = 0.035f),
                start = Offset(0f, gy),
                end = Offset(w, gy),
                strokeWidth = 1f
            )
            gy += gridStep
        }

        // 3. Elegant road / path curves (Bézier curves tracing navigational corridors)
        val routePath1 = Path().apply {
            moveTo(-20f, h * 0.16f)
            cubicTo(
                w * 0.28f, h * 0.12f,
                w * 0.72f, h * 0.32f,
                w + 30f, h * 0.28f
            )
        }

        val routePath2 = Path().apply {
            moveTo(-20f, h * 0.75f)
            cubicTo(
                w * 0.35f, h * 0.85f,
                w * 0.65f, h * 0.68f,
                w + 30f, h * 0.78f
            )
        }

        // Draw faint base road curves
        drawPath(
            path = routePath1,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    RaahiOrange.copy(alpha = 0.12f),
                    RaahiPink.copy(alpha = 0.06f),
                    Color.Transparent
                )
            ),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )

        drawPath(
            path = routePath2,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    RaahiOrange.copy(alpha = 0.07f),
                    Color.Transparent
                )
            ),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // 4. Waypoint dots and moving route pulse
        val measure1 = PathMeasure().apply { setPath(routePath1, false) }
        val len1 = measure1.length
        if (len1 > 0f) {
            val pOrigin = measure1.getPosition(len1 * 0.22f)
            drawCircle(
                color = RaahiOrange.copy(alpha = 0.22f),
                radius = 3.5.dp.toPx(),
                center = pOrigin
            )
            drawCircle(
                color = RaahiOrange.copy(alpha = 0.08f),
                radius = 7.dp.toPx(),
                center = pOrigin
            )

            val pMid = measure1.getPosition(len1 * 0.68f)
            drawCircle(
                color = RaahiPink.copy(alpha = 0.18f),
                radius = 3.dp.toPx(),
                center = pMid
            )

            // Animated light pulse traveling smoothly along the road curve
            val headDist = len1 * routeProgress
            val tailLen = 80.dp.toPx()
            for (step in 0..10) {
                val frac = step / 10f
                val d = headDist - frac * tailLen
                if (d in 0f..len1) {
                    val pos = measure1.getPosition(d)
                    val alpha = (1f - frac) * 0.45f * sin(PI.toFloat() * routeProgress)
                    val radius = (3.2f - frac * 2.0f).dp.toPx()
                    val dotColor = if (step == 0) RaahiAmber else RaahiOrange
                    drawCircle(
                        color = dotColor.copy(alpha = alpha),
                        radius = maxOf(1f, radius),
                        center = pos,
                        blendMode = BlendMode.Plus
                    )
                }
            }
        }
    }
}

/**
 * Helper to safely extract Activity from Context/ContextWrapper in Compose.
 */
private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
