package `in`.raahi.app.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.ui.theme.RaahiAmber
import `in`.raahi.app.ui.theme.RaahiBg
import `in`.raahi.app.ui.theme.RaahiBorder
import `in`.raahi.app.ui.theme.RaahiBorderSoft
import `in`.raahi.app.ui.theme.RaahiBrandGradient
import `in`.raahi.app.ui.theme.RaahiDisplayFont
import `in`.raahi.app.ui.theme.RaahiGlass
import `in`.raahi.app.ui.theme.RaahiGlassStrong
import `in`.raahi.app.ui.theme.RaahiGreen
import `in`.raahi.app.ui.theme.RaahiOrange
import `in`.raahi.app.ui.theme.RaahiRed
import `in`.raahi.app.ui.theme.RaahiShapeMedium
import `in`.raahi.app.ui.theme.RaahiShapePill
import `in`.raahi.app.ui.theme.RaahiShapeSmall
import `in`.raahi.app.ui.theme.RaahiText
import `in`.raahi.app.ui.theme.RaahiTextDim
import `in`.raahi.app.ui.theme.RaahiTextFaint
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(
    verificationId: String,
    phone: String,
    onSignedIn: (isNewUser: Boolean) -> Unit,
    onBack: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    BackHandler {
        viewModel.resetToEnteringPhone()
        onBack()
    }

    var errorText by remember { mutableStateOf<String?>(null) }
    var resendSeconds by remember { mutableIntStateOf(60) }
    var resendTrigger by remember { mutableIntStateOf(0) }
    var currentVerificationId by remember { mutableStateOf(verificationId) }

    val otpState = remember {
        OtpInputState { completeCode ->
            keyboardController?.hide()
            viewModel.verifyOtp(currentVerificationId, phone, completeCode)
        }
    }

    // Blinking cursor animation for currently active empty cell
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    // Countdown timer for OTP resend
    LaunchedEffect(resendTrigger) {
        resendSeconds = 60
        while (resendSeconds > 0) {
            delay(1000)
            resendSeconds--
        }
    }

    LaunchedEffect(state) {
        when (val s = state) {
            is AuthUiState.SignedIn -> {
                errorText = null
                onSignedIn(s.isNewUser)
            }
            is AuthUiState.OtpSent -> {
                errorText = null
                currentVerificationId = s.verificationId
                resendTrigger++
            }
            is AuthUiState.Error -> {
                errorText = s.message
            }
            else -> {}
        }
    }

    // Auto-focus OTP field upon opening
    LaunchedEffect(Unit) {
        delay(200)
        focusRequester.requestFocus()
    }

    val isVerifying = state is AuthUiState.VerifyingOtp
    val isSendingResend = state is AuthUiState.SendingOtp
    val isCodeComplete = otpState.isComplete

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RaahiBg)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.resetToEnteringPhone()
                            onBack()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(RaahiGlass)
                            .border(1.dp, RaahiBorderSoft, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = RaahiText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .clip(RaahiShapePill)
                            .background(RaahiGlass)
                            .border(1.dp, RaahiBorderSoft, RaahiShapePill)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(RaahiGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Secure Verification",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = RaahiTextDim
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Security Icon Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RaahiShapeMedium)
                        .background(RaahiOrange.copy(alpha = 0.12f))
                        .border(1.dp, RaahiOrange.copy(alpha = 0.3f), RaahiShapeMedium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Security",
                        tint = RaahiOrange,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Typography
                Text(
                    text = "Verify Phone Number",
                    style = TextStyle(
                        fontFamily = RaahiDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = RaahiText,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter the 6-digit code sent to your phone",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = RaahiTextDim,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Phone Display Chip with Edit Action
                Row(
                    modifier = Modifier
                        .clip(RaahiShapePill)
                        .background(RaahiGlass)
                        .border(1.dp, RaahiBorderSoft, RaahiShapePill)
                        .clickable {
                            viewModel.resetToEnteringPhone()
                            onBack()
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPhoneMasked(phone),
                        style = TextStyle(
                            fontFamily = RaahiDisplayFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = RaahiText
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit phone",
                        tint = RaahiOrange,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.resetToEnteringPhone()
                            onBack()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wrong number? ",
                        style = TextStyle(
                            fontSize = 12.5.sp,
                            color = RaahiTextDim
                        )
                    )
                    Text(
                        text = "Change number",
                        style = TextStyle(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RaahiOrange
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real status confirmation badge: Only displays success if verificationId is present
                if (currentVerificationId.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RaahiShapePill)
                            .background(RaahiGreen.copy(alpha = 0.12f))
                            .border(1.dp, RaahiGreen.copy(alpha = 0.3f), RaahiShapePill)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircleOutline,
                            contentDescription = null,
                            tint = RaahiGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OTP sent via SMS",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = RaahiGreen
                            )
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RaahiShapePill)
                            .background(RaahiRed.copy(alpha = 0.12f))
                            .border(1.dp, RaahiRed.copy(alpha = 0.35f), RaahiShapePill)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = RaahiRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Verification service unavailable",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = RaahiRed
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Segmented 6-digit OTP Input Boxes
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 6) {
                            val digit = otpState.digits.getOrNull(i).orEmpty()
                            val isFocused = (otpState.selectedIndex == i) || (i == 5 && otpState.selectedIndex == 6 && digit.isNotEmpty())
                            val isCurrentExpected = otpState.selectedIndex == i

                            val boxBorder = when {
                                errorText != null -> RaahiRed.copy(alpha = 0.85f)
                                isFocused -> RaahiOrange
                                digit.isNotEmpty() -> RaahiBorder
                                else -> RaahiBorderSoft
                            }

                            Box(
                                modifier = Modifier
                                    .size(width = 46.dp, height = 54.dp)
                                    .clip(RaahiShapeSmall)
                                    .background(if (digit.isNotEmpty()) RaahiGlassStrong else RaahiGlass)
                                    .border(if (isFocused) 2.dp else 1.dp, boxBorder, RaahiShapeSmall)
                                    .clickable {
                                        otpState.selectCell(i)
                                        focusRequester.requestFocus()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (digit.isNotEmpty()) {
                                    Text(
                                        text = digit,
                                        style = TextStyle(
                                            fontFamily = RaahiDisplayFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            color = RaahiText,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                } else if (isCurrentExpected) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(20.dp)
                                            .background(RaahiOrange.copy(alpha = cursorAlpha))
                                    )
                                }
                            }
                        }
                    }

                    // Hidden BasicTextField driving the 6 cells with complete IME synchronization
                    BasicTextField(
                        value = otpState.textFieldValue,
                        onValueChange = { newTfv ->
                            errorText = null
                            otpState.onValueChanged(newTfv)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isCodeComplete && !isVerifying) {
                                    keyboardController?.hide()
                                    viewModel.verifyOtp(currentVerificationId, phone, otpState.code)
                                }
                            }
                        ),
                        modifier = Modifier
                            .size(1.dp)
                            .alpha(0f)
                            .focusRequester(focusRequester)
                    )
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
                                .padding(top = 16.dp)
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

                // Resend Timer Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSendingResend) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = RaahiOrange,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sending OTP...",
                            style = TextStyle(fontSize = 13.sp, color = RaahiTextDim)
                        )
                    } else if (resendSeconds > 0) {
                        Text(
                            text = "Resend code in ",
                            style = TextStyle(fontSize = 13.sp, color = RaahiTextDim)
                        )
                        Text(
                            text = "${resendSeconds}s",
                            style = TextStyle(
                                fontFamily = RaahiDisplayFont,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = RaahiOrange
                            )
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RaahiShapePill)
                                .clickable {
                                    val activity = context.findActivity()
                                    if (activity != null) {
                                        errorText = null
                                        otpState.clear()
                                        viewModel.resendOtp(phone, activity)
                                    } else {
                                        errorText = "OTP service is currently unavailable. Please try again."
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = RaahiAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Resend OTP",
                                style = TextStyle(
                                    fontFamily = RaahiDisplayFont,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = RaahiAmber
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Verify Button
                val buttonModifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RaahiShapeMedium)
                    .then(
                        if (isCodeComplete && !isVerifying) {
                            Modifier.background(RaahiBrandGradient)
                        } else {
                            Modifier.background(RaahiGlassStrong)
                        }
                    )
                    .clickable(
                        enabled = isCodeComplete && !isVerifying,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isCodeComplete) {
                            errorText = null
                            keyboardController?.hide()
                            viewModel.verifyOtp(currentVerificationId, phone, otpState.code)
                        }
                    }

                Box(
                    modifier = buttonModifier,
                    contentAlignment = Alignment.Center
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Verify & Continue",
                                style = TextStyle(
                                    fontFamily = RaahiDisplayFont,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = if (isCodeComplete) Color.White else RaahiTextFaint
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                tint = if (isCodeComplete) Color.White else RaahiTextFaint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Security assurance at footer
            Column(
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Raahi uses authentic SMS authentication for your protection.",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = RaahiTextFaint,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

/**
 * Robust helper to extract an Activity from any ContextWrapper in Compose.
 */
private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Formats a phone string for privacy display: +91 98XXX XX321 */
private fun formatPhoneMasked(phone: String): String {
    val clean = phone.filter(Char::isDigit)
    val number = if (clean.startsWith("91") && clean.length == 12) clean.drop(2) else clean
    return if (number.length == 10) {
        "+91 ${number.take(2)}XXX XX${number.takeLast(3)}"
    } else {
        phone
    }
}
