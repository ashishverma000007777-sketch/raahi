package `in`.raahi.app.ui.screens.sos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.data.EmergencyContact
import `in`.raahi.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SosScreen(
    onBack: () -> Unit,
    onEmergencyContacts: () -> Unit,
    viewModel: SosViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF4EE),
                        Color(0xFFFFE9E2),
                        Color(0xFFFFF7F2)
                    )
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color(0xFF292524))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "SOS",
                        color = Color(0xFF292524),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RaahiDisplayFont
                    )
                    Text(
                        "Emergency Assistance",
                        color = Color(0xFF78716C),
                        fontSize = 11.5.sp
                    )
                }
                TextButton(onClick = onEmergencyContacts) {
                    Icon(Icons.Filled.ContactPhone, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Contacts", color = Color(0xFFE11D48), fontWeight = FontWeight.SemiBold)
                }
            }

            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (state.activeSos != null) {
                ActiveSosBody(
                    notifiedCount = state.justNotifiedCount,
                    contacts = contacts,
                    lat = state.activeSos!!.lat,
                    lng = state.activeSos!!.lng,
                    onResolve = viewModel::resolve,
                )
            } else {
                IdleSosBody(
                    triggering = state.triggering,
                    error = state.error,
                    onSosSlide = { viewModel.trigger() },
                )
            }
        }
    }
}

@Composable
private fun IdleSosBody(
    triggering: Boolean,
    error: String?,
    onSosSlide: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "beaconPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(10.dp))

        // Center: Glowing Beacon Rings (Visual indicator, not triggered by accidental tap)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Outer ring 1
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .scale(pulseScale)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                )
                // Outer ring 2
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(pulseScale)
                        .background(Color.White.copy(alpha = 0.16f), CircleShape)
                )
                // Center circle
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .shadow(elevation = 14.dp, shape = CircleShape, spotColor = Color(0x66000000))
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (triggering) {
                        CircularProgressIndicator(color = RaahiRed, strokeWidth = 3.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "SOS",
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Slide the slider below to alert\nnearby helpers and emergency contacts",
                color = Color(0xFF57534E),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            )

            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Text(error, color = Color(0xFFB91C1C), fontSize = 12.5.sp, textAlign = TextAlign.Center)
            }
        }

        // Bottom CTA: Real interactive Slide-to-Send gesture (prevents accidental tap)
        SosSlider(
            triggering = triggering,
            onSlideComplete = onSosSlide,
        )
    }
}

@Composable
fun SosSlider(
    triggering: Boolean,
    onSlideComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val thumbSizeDp = 46.dp
    val paddingDp = 6.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    val paddingPx = with(density) { paddingDp.toPx() }

    val maxOffset = (trackWidthPx - thumbSizePx - paddingPx * 2).coerceAtLeast(0f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Color(0x33000000))
            .background(Color.White, CircleShape)
            .padding(horizontal = paddingDp)
            .onSizeChanged { size ->
                trackWidthPx = size.width.toFloat()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Track text with fade on drag
        val textAlpha = if (maxOffset > 0) (1f - (dragOffset.value / maxOffset)).coerceIn(0f, 1f) else 1f
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (triggering) "Sending SOS..." else "Slide to Send SOS",
                color = Color(0xFFE11D48).copy(alpha = textAlpha),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RaahiDisplayFont,
                textAlign = TextAlign.Center
            )
        }

        // Draggable Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .size(thumbSizeDp)
                .shadow(elevation = 4.dp, shape = CircleShape, spotColor = Color(0x40000000))
                .background(Color(0xFFE11D48), CircleShape)
                .pointerInput(triggering, maxOffset) {
                    if (triggering || maxOffset <= 0f) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (dragOffset.value >= maxOffset * 0.75f) {
                                    dragOffset.animateTo(maxOffset, tween(150))
                                    onSlideComplete()
                                    dragOffset.animateTo(0f, tween(300))
                                } else {
                                    dragOffset.animateTo(0f, tween(200))
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                dragOffset.animateTo(0f, tween(200))
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val newOffset = (dragOffset.value + dragAmount).coerceIn(0f, maxOffset)
                                dragOffset.snapTo(newOffset)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (triggering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = "Slide to Send SOS",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveSosBody(
    notifiedCount: Int?,
    contacts: List<EmergencyContact>,
    lat: Double,
    lng: Double,
    onResolve: () -> Unit,
) {
    val context = LocalContext.current
    var smsSent by remember { mutableStateOf(false) }
    var isSendingSms by remember { mutableStateOf(false) }

    fun doSendDirect() {
        isSendingSms = true
        val success = sendDirectSms(context, contacts, lat, lng)
        isSendingSms = false
        if (success) {
            smsSent = true
            Toast.makeText(
                context,
                "SOS SMS sent to ${contacts.size} emergency contacts",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // Graceful fallback if direct SMS failed
            openSmsComposerFallback(context, contacts, lat, lng)
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            doSendDirect()
        } else {
            Toast.makeText(
                context,
                "SMS permission not granted. Opening SMS app...",
                Toast.LENGTH_SHORT
            ).show()
            openSmsComposerFallback(context, contacts, lat, lng)
        }
    }

    val onSmsClick: () -> Unit = {
        if (!smsSent && !isSendingSms) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                doSendDirect()
            } else {
                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(90.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("SOS Alert Active", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
        Spacer(Modifier.height(6.dp))
        Text(
            if (notifiedCount != null) "$notifiedCount nearby helpers notified" else "Your emergency location has been broadcast",
            color = Color.White.copy(alpha = 0.9f), fontSize = 13.5.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        if (contacts.isNotEmpty()) {
            Button(
                onClick = onSmsClick,
                enabled = !smsSent && !isSendingSms,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (smsSent) Color(0xFF10B981) else Color.White.copy(alpha = 0.25f),
                    disabledContainerColor = if (smsSent) Color(0xFF10B981) else Color.White.copy(alpha = 0.2f)
                ),
            ) {
                if (isSendingSms) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Sending SMS...", color = Color.White, fontWeight = FontWeight.Bold)
                } else if (smsSent) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SMS Sent to Emergency Contacts", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Text("Also SMS My Emergency Contacts", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = onResolve,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        ) {
            Text("I'm Safe — Resolve SOS", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

private fun sendDirectSms(
    context: Context,
    contacts: List<EmergencyContact>,
    lat: Double,
    lng: Double
): Boolean {
    val locationUrl = "https://maps.google.com/?q=$lat,$lng"
    val message = "SOS: I need emergency help. My location: $locationUrl"
    val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(SmsManager::class.java)
    } else {
        @Suppress("DEPRECATION")
        SmsManager.getDefault()
    }

    var sentCount = 0
    contacts.forEach { contact ->
        if (contact.phone.isNotBlank()) {
            try {
                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(contact.phone, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(contact.phone, null, message, null, null)
                }
                sentCount++
            } catch (e: Exception) {
                android.util.Log.e("SosScreen", "Failed to send SMS to ${contact.phone}", e)
            }
        }
    }
    return sentCount > 0
}

private fun openSmsComposerFallback(
    context: Context,
    contacts: List<EmergencyContact>,
    lat: Double,
    lng: Double
) {
    val locationUrl = "https://maps.google.com/?q=$lat,$lng"
    val message = "SOS: I need emergency help. My location: $locationUrl"
    contacts.forEach { contact ->
        val uri = Uri.parse("sms:${contact.phone}?body=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        runCatching { context.startActivity(intent) }
    }
}
