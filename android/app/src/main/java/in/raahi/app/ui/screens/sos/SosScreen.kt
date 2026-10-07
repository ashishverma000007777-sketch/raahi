package `in`.raahi.app.ui.screens.sos

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.data.EmergencyContact
import `in`.raahi.app.ui.components.*
import `in`.raahi.app.ui.theme.*

@Composable
fun SosScreen(
    onBack: () -> Unit,
    onEmergencyContacts: () -> Unit,
    viewModel: SosViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    var showConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE11D48),
                        Color(0xFFDC2626),
                        Color(0xFF991B1B)
                    )
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header: Screen 15 reference
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "SOS",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RaahiDisplayFont
                    )
                    Text(
                        "Emergency Assistance",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.5.sp
                    )
                }
                TextButton(onClick = onEmergencyContacts) {
                    Icon(Icons.Filled.ContactPhone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Contacts", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (state.activeSos != null) {
                ActiveSosBody(
                    notifiedCount = state.justNotifiedCount,
                    onResolve = viewModel::resolve,
                    onAlertContacts = {
                        alertContactsBySms(context, contacts, state.activeSos!!.lat, state.activeSos!!.lng)
                    },
                    hasContacts = contacts.isNotEmpty(),
                )
            } else {
                IdleSosBody(
                    triggering = state.triggering,
                    error = state.error,
                    onSosTap = { showConfirm = true },
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Send SOS alert?", color = RaahiText, fontWeight = FontWeight.Bold) },
            text = { Text("Your location will be broadcast to nearby verified helpers and mechanics immediately.", color = RaahiTextDim) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        viewModel.trigger()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiRed)
                ) { Text("Send SOS", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel", color = RaahiTextDim) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun IdleSosBody(triggering: Boolean, error: String?, onSosTap: () -> Unit) {
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

        Spacer(Modifier.height(1.dp))

        // Center: Glowing Beacon Rings + Button (Screen 15 reference)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clickable(enabled = !triggering, onClick = onSosTap),
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
                // Center pulsing circle
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
                text = "Tap to alert nearby helpers\nand emergency contacts",
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            )

            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Text(error, color = Color(0xFFFFD1D1), fontSize = 12.5.sp, textAlign = TextAlign.Center)
            }
        }

        // Bottom CTA: Slide / Send SOS Button (Screen 15 reference)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp), spotColor = Color(0x33000000))
                .background(Color.White, RoundedCornerShape(28.dp))
                .clickable(enabled = !triggering, onClick = onSosTap)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFFE11D48), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Slide to Send SOS",
                color = Color(0xFFE11D48),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RaahiDisplayFont,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(42.dp))
        }
    }
}

@Composable
private fun ActiveSosBody(notifiedCount: Int?, onResolve: () -> Unit, onAlertContacts: () -> Unit, hasContacts: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(90.dp).background(Color.White.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
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
        if (hasContacts) {
            Button(
                onClick = onAlertContacts,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
            ) { Text("Also SMS My Emergency Contacts", color = Color.White, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(12.dp))
        }
        Button(
            onClick = onResolve,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        ) { Text("I'm Safe — Resolve SOS", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 15.sp) }
    }
}

private fun alertContactsBySms(context: android.content.Context, contacts: List<EmergencyContact>, lat: Double, lng: Double) {
    val locationUrl = "https://maps.google.com/?q=$lat,$lng"
    val message = "SOS: I need emergency help. My location: $locationUrl"
    contacts.forEach { contact ->
        val uri = Uri.parse("sms:${contact.phone}?body=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        runCatching { context.startActivity(intent) }
    }
}
