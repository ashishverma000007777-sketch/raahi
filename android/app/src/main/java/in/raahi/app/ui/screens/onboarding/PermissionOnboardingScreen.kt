package `in`.raahi.app.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import `in`.raahi.app.ui.components.GlassCard
import `in`.raahi.app.ui.components.RaahiPrimaryButton
import `in`.raahi.app.ui.theme.*

@Composable
fun PermissionOnboardingScreen(
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotification by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    // Refresh permissions on resume when returning from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasLocation = results[Manifest.permission.ACCESS_FINE_LOCATION] == true || results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotification = granted
    }

    fun completeOnboarding() {
        context.getSharedPreferences("raahi_onboarding", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("permissions_onboarded", true)
            .apply()
        onDone()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = RaahiBg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(RaahiOrange.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = RaahiOrange,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Permissions Setup",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RaahiDisplayFont,
                color = RaahiText,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Enable these permissions so Raahi can accurately locate you, alert helpers, and protect your drives.",
                fontSize = 13.sp,
                color = RaahiTextDim,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )

            Spacer(Modifier.height(28.dp))

            // Permission item 1: Location
            PermissionCard(
                icon = Icons.Outlined.MyLocation,
                title = "Location Permission",
                description = "Required to share your breakdown spot with nearby mechanics and helpers.",
                isGranted = hasLocation,
                onRequest = {
                    locationLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                },
            )

            Spacer(Modifier.height(14.dp))

            // Permission item 2: Notifications
            PermissionCard(
                icon = Icons.Outlined.Notifications,
                title = "Push Notifications",
                description = "Get notified when a helper accepts your request or arrives at your vehicle.",
                isGranted = hasNotification,
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )



            Spacer(Modifier.weight(1f, fill = false))
            Spacer(Modifier.height(36.dp))

            RaahiPrimaryButton(
                text = "Continue",
                onClick = { completeOnboarding() },
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "You can update these anytime in Android System Settings",
                fontSize = 11.5.sp,
                color = RaahiTextDim,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isGranted) RaahiGreen.copy(alpha = 0.12f) else RaahiOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) RaahiGreen else RaahiOrange,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RaahiText,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = RaahiTextDim,
                    lineHeight = 15.sp,
                )
            }

            Spacer(Modifier.width(10.dp))

            if (isGranted) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(RaahiGreen.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Granted",
                        tint = RaahiGreen,
                        modifier = Modifier.size(16.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RaahiShapePill)
                        .background(RaahiOrange)
                        .clickable(onClick = onRequest)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Allow",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
