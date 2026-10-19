package `in`.raahi.app.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.LocationOff

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import `in`.raahi.app.R
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import `in`.raahi.app.network.AiStatusDto
import `in`.raahi.app.network.CarHealthDto
import `in`.raahi.app.network.JobDto
import `in`.raahi.app.network.HelperStatusDto
import `in`.raahi.app.network.UserDto
import `in`.raahi.app.network.VehicleDto
import `in`.raahi.app.network.IntercityTripDto
import `in`.raahi.app.data.CityCatalogue
import `in`.raahi.app.data.LocationResolver
import `in`.raahi.app.ui.components.*
import `in`.raahi.app.ui.theme.*

@Composable
fun HomeScreen(
    onRequestHelp: () -> Unit,
    onNearbyMechanics: () -> Unit,
    onMyJobs: () -> Unit,
    onSos: () -> Unit,
    onOpenActiveJob: (JobDto) -> Unit,
    onHelperDashboard: () -> Unit,
    onBecomeHelper: () -> Unit,
    onAiMechanic: () -> Unit,
    onCarHealth: () -> Unit,
    onSetupVehicle: () -> Unit,
    onOpenDaily: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenWallet: () -> Unit = {},
    onAddFuel: () -> Unit,
    onNavigateTab: (RaahiTab) -> Unit,
    onPlanTrip: () -> Unit = {},
    onOpenActiveTrip: (String) -> Unit = {},
    onTripHistory: () -> Unit = {},
    onSearchIntercityTrips: (String, String) -> Unit = { _, _ -> },
    onCreateIntercityTrip: () -> Unit = {},
    onAgentIntercityDashboard: () -> Unit = {},
    onDriverIntercityTrips: () -> Unit = {},
    onOpenActiveIntercityTrip: (String) -> Unit = {},
    onOpenIntercityMarketplace: () -> Unit = {},
    onFuelRates: () -> Unit = { onOpenDaily("fuel") },
    onShop: () -> Unit = { onOpenDaily("shop") },
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val helperStatus by viewModel.helperStatus.collectAsState()
    val helperBusy by viewModel.helperBusy.collectAsState()
    val helperError by viewModel.helperError.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val incomingRequest by viewModel.incomingRequest.collectAsState()

    val context = LocalContext.current
    var showDrivingConfirmation by remember { mutableStateOf(false) }
    var hasLocationPerm by remember { mutableStateOf(hasLocationPermission(context)) }
    var isDrivingConfirmed by remember {
        mutableStateOf(
            `in`.raahi.app.data.DrivingTrackingService.isConfirmedDriving &&
                context.getSharedPreferences("raahi_driving_tracking", Context.MODE_PRIVATE)
                    .getBoolean("is_driving_confirmed", false)
        )
    }

    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPerm = granted
        if (granted) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, `in`.raahi.app.data.DrivingTrackingService::class.java)
                    .setAction(`in`.raahi.app.data.DrivingTrackingService.ACTION_START)
            )
        }
    }

    fun syncDrivingTrackingState() {
        hasLocationPerm = hasLocationPermission(context)
        val prefs = context.getSharedPreferences("raahi_driving_tracking", Context.MODE_PRIVATE)
        isDrivingConfirmed = `in`.raahi.app.data.DrivingTrackingService.isConfirmedDriving &&
            prefs.getBoolean("is_driving_confirmed", false)
    }

    LaunchedEffect(Unit) {
        syncDrivingTrackingState()
    }

    // A driving-alert notification can launch or bring Raahi back to the foreground.
    // Consume the intent extra once so rotation/recomposition does not reopen the dialog.
    LaunchedEffect(context) {
        val activity = context as? android.app.Activity
        if (activity?.intent?.getBooleanExtra("show_driving_confirmation", false) == true) {
            showDrivingConfirmation = true
            activity.intent.removeExtra("show_driving_confirmation")
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    `in`.raahi.app.data.DrivingTrackingService.EVENT_DRIVING_DETECTED -> {
                        showDrivingConfirmation = true
                        syncDrivingTrackingState()
                    }
                    `in`.raahi.app.data.DrivingTrackingService.EVENT_STATUS_CHANGED,
                    `in`.raahi.app.data.DrivingTrackingService.EVENT_DISTANCE_UPDATED -> {
                        syncDrivingTrackingState()
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(`in`.raahi.app.data.DrivingTrackingService.EVENT_DRIVING_DETECTED)
            addAction(`in`.raahi.app.data.DrivingTrackingService.EVENT_STATUS_CHANGED)
            addAction(`in`.raahi.app.data.DrivingTrackingService.EVENT_DISTANCE_UPDATED)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    if (showDrivingConfirmation) {
        val isDark = RaahiIsDark
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showDrivingConfirmation = false
                context.startService(
                    Intent(context, `in`.raahi.app.data.DrivingTrackingService::class.java)
                        .setAction(`in`.raahi.app.data.DrivingTrackingService.ACTION_DECLINE)
                )
            }
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = RaahiBg2,
                border = BorderStroke(1.dp, RaahiBorderSoft),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(if (isDark) Color(0xFF222629) else Color(0xFFEDF0F2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DirectionsCar,
                            contentDescription = null,
                            tint = RaahiText,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Are you driving?",
                        color = RaahiText,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RaahiDisplayFont
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Raahi detected possible driving activity. Confirming starts accurate GPS trip tracking and updates your odometer automatically.",
                        color = RaahiTextDim,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            showDrivingConfirmation = false
                            context.startService(
                                Intent(context, `in`.raahi.app.data.DrivingTrackingService::class.java)
                                    .setAction(`in`.raahi.app.data.DrivingTrackingService.ACTION_CONFIRM)
                            )
                            isDrivingConfirmed = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryButton),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Yes, I am driving",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            showDrivingConfirmation = false
                            context.startService(
                                Intent(context, `in`.raahi.app.data.DrivingTrackingService::class.java)
                                    .setAction(`in`.raahi.app.data.DrivingTrackingService.ACTION_DECLINE)
                            )
                        }
                    ) {
                        Text(
                            text = "No, not now",
                            color = RaahiTextDim,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                syncDrivingTrackingState()
                viewModel.load()
                viewModel.refreshMetrics(hasLocationPermission(context))
                viewModel.refreshHelperStatus()
                if (hasLocationPermission(context)) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, `in`.raahi.app.data.DrivingTrackingService::class.java)
                            .setAction(`in`.raahi.app.data.DrivingTrackingService.ACTION_START)
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    `in`.raahi.app.ui.components.RequestNotificationPermissionOnce()


    Surface(modifier = Modifier.fillMaxSize(), color = RaahiBg) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (val s = state) {
                        is HomeUiState.Loading -> LoadingState()
                        is HomeUiState.Error -> HomeContent(
                            user = null, activeJob = null, activeTrip = null, vehicle = null, carHealth = null,
                            offlineError = s.message, onRetry = viewModel::load,
                            selectedCity = selectedCity, onSelectCity = viewModel::selectCity, onUseCurrentLocation = viewModel::useCurrentLocation,
                            onRequestHelp = onRequestHelp, onNearbyMechanics = onNearbyMechanics, onMyJobs = onMyJobs,
                            onSos = onSos, onOpenActiveJob = onOpenActiveJob, onHelperDashboard = onHelperDashboard,
                            onBecomeHelper = onBecomeHelper, onAiMechanic = onAiMechanic, onCarHealth = onCarHealth,
                            onSetupVehicle = onSetupVehicle, onOpenDaily = onOpenDaily,
                            onPlanTrip = onPlanTrip, onOpenActiveTrip = onOpenActiveTrip, onTripHistory = onTripHistory,
                            metrics = metrics, onOpenNotifications = onOpenNotifications, onOpenWallet = onOpenWallet, onAddFuel = onAddFuel,
                            onFuelRates = onFuelRates, onShop = onShop,
                            helperStatus = helperStatus, helperBusy = helperBusy, helperError = helperError,
                            onToggleHelperOnline = { online -> viewModel.setHelperOnline(online, hasLocationPermission(context)) },
                            onRefreshHelperStatus = viewModel::refreshHelperStatus,
                            onProfile = { onNavigateTab(RaahiTab.PROFILE) },
                            hasLocationPermission = hasLocationPerm,
                            isDrivingConfirmed = isDrivingConfirmed,
                            onStartDrive = {
                                context.startService(
                                    Intent(context, `in`.raahi.app.data.DrivingTrackingService::class.java)
                                        .setAction(`in`.raahi.app.data.DrivingTrackingService.ACTION_CONFIRM)
                                )
                                isDrivingConfirmed = true
                            },
                            onStopDrive = {
                                context.startService(
                                    Intent(context, `in`.raahi.app.data.DrivingTrackingService::class.java)
                                        .setAction(`in`.raahi.app.data.DrivingTrackingService.ACTION_STOP)
                                )
                                isDrivingConfirmed = false
                            },
                            onRequestLocationPermission = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                        )
                        is HomeUiState.Loaded -> HomeContent(
                            user = s.user, activeJob = s.activeJob, activeTrip = s.activeTrip,
                            activeIntercityTrip = s.activeIntercityTrip,
                            vehicle = s.vehicle, carHealth = s.carHealth,
                            offlineError = s.offlineError, onRetry = viewModel::load,
                            selectedCity = selectedCity, onSelectCity = viewModel::selectCity, onUseCurrentLocation = viewModel::useCurrentLocation,
                            onSearchIntercityTrips = onSearchIntercityTrips,
                            onCreateIntercityTrip = onCreateIntercityTrip,
                            onAgentIntercityDashboard = onAgentIntercityDashboard,
                            onDriverIntercityTrips = onDriverIntercityTrips,
                            onOpenActiveIntercityTrip = onOpenActiveIntercityTrip,
                            onOpenIntercityMarketplace = onOpenIntercityMarketplace,
                            onRequestHelp = onRequestHelp, onNearbyMechanics = onNearbyMechanics, onMyJobs = onMyJobs,
                            onSos = onSos, onOpenActiveJob = onOpenActiveJob, onHelperDashboard = onHelperDashboard,
                            onBecomeHelper = onBecomeHelper, onAiMechanic = onAiMechanic, onCarHealth = onCarHealth,
                            onSetupVehicle = onSetupVehicle, onOpenDaily = onOpenDaily,
                            onPlanTrip = onPlanTrip, onOpenActiveTrip = onOpenActiveTrip, onTripHistory = onTripHistory,
                            metrics = metrics, onOpenNotifications = onOpenNotifications, onOpenWallet = onOpenWallet, onAddFuel = onAddFuel,
                            onFuelRates = onFuelRates, onShop = onShop,
                            helperStatus = helperStatus, helperBusy = helperBusy, helperError = helperError,
                            onToggleHelperOnline = { online -> viewModel.setHelperOnline(online, hasLocationPermission(context)) },
                            onRefreshHelperStatus = viewModel::refreshHelperStatus,
                            onProfile = { onNavigateTab(RaahiTab.PROFILE) },
                            hasLocationPermission = hasLocationPerm,
                            isDrivingConfirmed = isDrivingConfirmed,
                            onStartDrive = {
                                context.startService(
                                    Intent(context, `in`.raahi.app.data.DrivingTrackingService::class.java)
                                        .setAction(`in`.raahi.app.data.DrivingTrackingService.ACTION_CONFIRM)
                                )
                                isDrivingConfirmed = true
                            },
                            onStopDrive = {
                                context.startService(
                                    Intent(context, `in`.raahi.app.data.DrivingTrackingService::class.java)
                                        .setAction(`in`.raahi.app.data.DrivingTrackingService.ACTION_STOP)
                                )
                                isDrivingConfirmed = false
                            },
                            onRequestLocationPermission = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                        )

                    }
                }
                RaahiBottomNavBar(current = RaahiTab.HOME, onSelect = onNavigateTab)
            }
            RaahiFloatingSosButton(
                onSos = onSos,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 86.dp)
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = RaahiPrimaryButton)
    }
}


@Composable
private fun RoadsideServicesSection(
    onRequestHelp: () -> Unit,
    onFindMechanic: () -> Unit,
    onMyJobs: () -> Unit,
    onSos: () -> Unit,
    onFuelRates: () -> Unit,
    onShop: () -> Unit,
) {
    val isDark = RaahiIsDark
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Roadside & Assistance",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = RaahiText,
                fontFamily = RaahiDisplayFont,
            )
            Text(
                text = "24/7 Active",
                fontSize = 11.5.sp,
                color = RaahiSuccess,
                fontWeight = FontWeight.Bold,
            )
        }

        // Two Dominant Primary Action Cards (Side-by-Side matching Mockup Screen 6)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Request Help
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onRequestHelp),
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) Color(0xFF26191B) else Color(0xFFFFF9F8),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF4A2528) else Color(0xFFFFD5CC)),
                shadowElevation = 3.5.dp,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF3D1E22) else Color(0xFFFFECE8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SupportAgent,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Request Help",
                        color = RaahiText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RaahiDisplayFont
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Roadside dispatch",
                        color = if (isDark) Color(0xFFFDA4AF) else Color(0xFFBE123C),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            // Card 2: Find Mechanic
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onFindMechanic),
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) Color(0xFF162332) else Color(0xFFF0F9FF),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF213B59) else Color(0xFFBAE6FD)),
                shadowElevation = 3.5.dp,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF1D3752) else Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = RaahiIcons.Wrench,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Find Mechanic",
                        color = RaahiText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RaahiDisplayFont
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Garages near you",
                        color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        // Secondary 4-Action Utility Row with Elevated Cards & Tinted Micro-surfaces
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF1E2226) else Color(0xFFFFFFFF),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF2E353D) else Color(0xFFE2E8F0)),
            shadowElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickUtilityItem(
                    title = "SOS",
                    icon = Icons.Outlined.Warning,
                    tint = Color(0xFFDC2626),
                    bg = if (isDark) Color(0xFF381A1C) else Color(0xFFFEE2E2),
                    onClick = onSos
                )
                QuickUtilityItem(
                    title = "My Jobs",
                    icon = Icons.Outlined.History,
                    tint = Color(0xFF2563EB),
                    bg = if (isDark) Color(0xFF14273E) else Color(0xFFEFF6FF),
                    onClick = onMyJobs
                )
                QuickUtilityItem(
                    title = "Fuel Rates",
                    icon = Icons.Outlined.LocalGasStation,
                    tint = Color(0xFFD97706),
                    bg = if (isDark) Color(0xFF352611) else Color(0xFFFFFBEB),
                    onClick = onFuelRates
                )
                QuickUtilityItem(
                    title = "Shop",
                    icon = Icons.Outlined.ShoppingBag,
                    tint = Color(0xFF7C3AED),
                    bg = if (isDark) Color(0xFF291C3F) else Color(0xFFF5F3FF),
                    onClick = onShop
                )
            }
        }
    }
}

@Composable
private fun RowScope.QuickUtilityItem(
    title: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(bg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = when (title) {
                "SOS" -> Color(0xFFDC2626)
                "My Jobs" -> Color(0xFF1D4ED8)
                "Fuel Rates" -> Color(0xFFB45309)
                "Shop" -> Color(0xFF6D28D9)
                else -> RaahiText
            },
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeContent(
    user: UserDto?, activeJob: JobDto?, activeTrip: `in`.raahi.app.network.TripDto? = null,
    activeIntercityTrip: `in`.raahi.app.network.IntercityTripDto? = null,
    vehicle: VehicleDto?, carHealth: CarHealthDto?,
    offlineError: String? = null, onRetry: () -> Unit = {},
    selectedCity: String = "Chandigarh",
    onSelectCity: (String, Double, Double) -> Unit = { _, _, _ -> },
    onUseCurrentLocation: (Boolean, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onSearchIntercityTrips: (String, String) -> Unit = { _, _ -> },
    onCreateIntercityTrip: () -> Unit = {},
    onAgentIntercityDashboard: () -> Unit = {},
    onDriverIntercityTrips: () -> Unit = {},
    onOpenActiveIntercityTrip: (String) -> Unit = {},
    onOpenIntercityMarketplace: () -> Unit = {},
    onRequestHelp: () -> Unit, onNearbyMechanics: () -> Unit, onMyJobs: () -> Unit, onSos: () -> Unit,
    onOpenActiveJob: (JobDto) -> Unit, onHelperDashboard: () -> Unit, onBecomeHelper: () -> Unit,
    onAiMechanic: () -> Unit, onCarHealth: () -> Unit, onSetupVehicle: () -> Unit,
    onOpenDaily: (String) -> Unit, onProfile: () -> Unit,
    onPlanTrip: () -> Unit = {}, onOpenActiveTrip: (String) -> Unit = {}, onTripHistory: () -> Unit = {},
    metrics: HomeMetrics = HomeMetrics(), onOpenNotifications: () -> Unit = {}, onOpenWallet: () -> Unit = {}, onAddFuel: () -> Unit = {},
    onFuelRates: () -> Unit = { onOpenDaily("fuel") },
    onShop: () -> Unit = { onOpenDaily("shop") },
    helperStatus: HelperStatusDto? = null, helperBusy: Boolean = false, helperError: String? = null,
    onToggleHelperOnline: (Boolean) -> Unit = {}, onRefreshHelperStatus: () -> Unit = {},
    hasLocationPermission: Boolean = true,
    isDrivingConfirmed: Boolean = false,
    onStartDrive: () -> Unit = {},
    onStopDrive: () -> Unit = {},
    onRequestLocationPermission: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 20 } }
    var showLocationDialog by remember { mutableStateOf(false) }

    if (showLocationDialog) {
        LocationSelectionDialog(
            currentCity = selectedCity,
            onDismiss = { showLocationDialog = false },
            onSelectCity = onSelectCity,
            onUseCurrentLocation = onUseCurrentLocation,
        )
    }

    Column(Modifier.fillMaxSize()) {
        // Top Header Block - seamlessly blended with background (no solid black block)
        Surface(
            color = RaahiBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                // Top Header Row
                HeaderRow(
                    user = user,
                    selectedCity = selectedCity,
                    onLocationClick = { showLocationDialog = true },
                    onProfile = onProfile,
                    unread = unreadCount(metrics.summary),
                    onBell = onOpenNotifications,
                    onWallet = onOpenWallet,
                )

                // 1. Live Updates Ticker as a proper floating-style container
                val tickerItems = tickerItems(vehicle, carHealth, metrics)
                if (tickerItems.isNotEmpty()) {
                    Ticker(
                        items = tickerItems,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Scrollable Body Content
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(bottom = 136.dp)
        ) {
            Spacer(Modifier.height(14.dp))

            // 2. Automotive Greeting Section
            AutomotiveHeroSection(user)
            Spacer(Modifier.height(14.dp))

            // 3. Role-Based Marketplace Dashboard (Owner, Travel Agent, or Driver)
            RoleBasedMarketplaceDashboard(
                user = user,
                vehicle = vehicle,
                helperStatus = helperStatus,
                activeIntercityTrip = activeIntercityTrip,
                onCreateTrip = onCreateIntercityTrip,
                onAgentDashboard = onAgentIntercityDashboard,
                onDriverTrips = onDriverIntercityTrips,
                onOpenMarketplace = onOpenIntercityMarketplace,
                onOpenActiveTrip = onOpenActiveIntercityTrip,
                onTripHistory = onTripHistory,
            )
            Spacer(Modifier.height(14.dp))

            // 4. Roadside & Assistance (2 dominant cards: Request Help & Find Mechanic, plus 4 utility chips)
            RoadsideServicesSection(
                onRequestHelp = onRequestHelp,
                onFindMechanic = onNearbyMechanics,
                onMyJobs = onMyJobs,
                onSos = onSos,
                onFuelRates = onFuelRates,
                onShop = onShop,
            )
            Spacer(Modifier.height(14.dp))

            // 5. Dynamic Drive Tracking Card (Refined Auto Drive Detection status surface)
            DriveTrackingCard(
                hasLocationPermission = hasLocationPermission,
                isDrivingConfirmed = isDrivingConfirmed,
                onStopDrive = onStopDrive,
                onRequestLocationPermission = onRequestLocationPermission,
            )
            Spacer(Modifier.height(14.dp))

            // Contextual alerts & banners
            if (offlineError != null) {
                OfflineStatusBanner(
                    message = offlineError,
                    onRetry = onRetry,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            if (activeJob != null) {
                ActiveJobBanner(activeJob, onClick = { onOpenActiveJob(activeJob) })
                Spacer(Modifier.height(10.dp))
            }

            if (activeIntercityTrip != null) {
                ActiveIntercityTripBanner(
                    trip = activeIntercityTrip,
                    onClick = { onOpenActiveIntercityTrip(activeIntercityTrip.id) },
                )
                Spacer(Modifier.height(12.dp))
            }

            // 6. Compact Vehicle Summary (License plate badge + inline spec bar + garage action)
            VehicleHeroCard(vehicle, carHealth, onSetupVehicle, onCarHealth)
            Spacer(Modifier.height(14.dp))

            // 8. Plan Trip Card
            PlanTripCard(
                activeTrip = activeTrip,
                onPlanTrip = onPlanTrip,
                onOpenActiveTrip = onOpenActiveTrip
            )

            Spacer(Modifier.height(14.dp))
            if (user?.role == "HELPER" || user?.role == "MECHANIC") {
                HelperApplicationStatusCard(
                    status = helperStatus, busy = helperBusy, error = helperError,
                    onToggle = onToggleHelperOnline, onRetry = onRefreshHelperStatus,
                    onOpenDashboard = onHelperDashboard, onApplyOrReview = onBecomeHelper,
                )
            }

            // 9. This week
            Spacer(Modifier.height(18.dp))
            Box(Modifier.padding(horizontal = 18.dp)) { SectionLabel("This week") }
            WeekStrip(metrics = metrics, onAddFuel = onAddFuel, modifier = Modifier.padding(horizontal = 18.dp))

            // 10. More Section
            Spacer(Modifier.height(18.dp))
            Box(Modifier.padding(horizontal = 18.dp)) { SectionLabel("More") }
            DailyRow(onOpenDaily)
        }
    }
}

@Composable
private fun HeaderRow(
    user: UserDto?,
    selectedCity: String,
    onLocationClick: () -> Unit,
    onProfile: () -> Unit,
    unread: Int,
    onBell: () -> Unit,
    onWallet: () -> Unit,
) {
    val isDark = LocalRaahiColors.current.isDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Brand & Location Dropdown
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RaahiEmblem(
                size = 30.dp,
                glow = false,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Raahi",
                    fontFamily = RaahiDisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 20.sp,
                    color = RaahiText
                )
                Spacer(Modifier.height(1.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onLocationClick)
                        .background(if (isDark) Color(0xFF2B2117) else Color(0xFFFFF3E0))
                        .border(1.dp, if (isDark) Color(0xFF4A3420) else Color(0xFFFFD180), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        tint = Color(0xFFF57C00),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        selectedCity,
                        color = RaahiText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Change location",
                        tint = Color(0xFFEF6C00),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Right Actions (Wallet, Notifications, Profile)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Payments & Wallet compact icon button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape, spotColor = Color(0x182563EB))
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF14243B) else Color(0xFFEFF6FF), CircleShape)
                    .border(1.dp, if (isDark) Color(0xFF1E3A60) else Color(0xFFBFDBFE), CircleShape)
                    .clickable(onClick = onWallet),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AccountBalanceWallet,
                    contentDescription = "Payments & Wallet",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Notification Bell Button
            BellWithBadge(unread = unread, onClick = onBell)

            // User Avatar Circle Button with Gradient & Soft Shadow
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(elevation = 2.5.dp, shape = CircleShape, spotColor = Color(0x28F57C00))
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFF57C00), Color(0xFFEF6C00))), CircleShape)
                    .border(1.5.dp, Color(0xFFFFE0B2), CircleShape)
                    .clickable(onClick = onProfile),
                contentAlignment = Alignment.Center,
            ) {
                val initial = user?.name?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
                Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = RaahiDisplayFont)
            }
        }
    }
}

@Composable
private fun BellWithBadge(unread: Int, onClick: () -> Unit) {
    val isDark = LocalRaahiColors.current.isDark
    val btnBg = if (isDark) Color(0xFF2F2014) else Color(0xFFFFF7ED)
    val btnBorder = if (isDark) Color(0xFF4E3420) else Color(0xFFFFEDD5)
    val btnTint = Color(0xFFEA580C)

    val transition = rememberInfiniteTransition(label = "ring")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4500
                0f at 0
                -15f at 60
                13f at 120
                -9f at 180
                7f at 240
                0f at 320
                0f at 4500
            },
            RepeatMode.Restart,
        ),
        label = "ringAngle",
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(elevation = 2.dp, shape = CircleShape, spotColor = Color(0x18EA580C))
            .clip(CircleShape)
            .background(btnBg, CircleShape)
            .border(1.dp, btnBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = btnTint, modifier = Modifier.size(18.dp).rotate(angle))
        if (unread > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(7.dp)
                    .background(Color(0xFFDC2626), CircleShape)
            )
        }
    }
}

/**
 * Dynamic Drive Tracking Card:
 * - When permission missing: "Drive Tracking Paused", "Location access needed to detect drives automatically.", [Enable] button
 * - When driving confirmed: "Drive in Progress", "Recording trip distance · Odometer sync active", [Stop Drive] button
 * - When monitoring active: "Auto Drive Detection", "Monitoring for your next drive", compact [Monitoring] status indicator
 */
@Composable
private fun DriveTrackingCard(
    hasLocationPermission: Boolean,
    isDrivingConfirmed: Boolean,
    onStopDrive: () -> Unit,
    onRequestLocationPermission: () -> Unit,
) {
    val isDark = RaahiIsDark
    val (cardBg, cardBorder, spotTint) = when {
        !hasLocationPermission -> Triple(
            if (isDark) Color(0xFF2C2114) else Color(0xFFFFFBEB),
            if (isDark) Color(0xFF4A371F) else Color(0xFFFDE68A),
            Color(0x18D97706)
        )
        isDrivingConfirmed -> Triple(
            if (isDark) Color(0xFF142B1E) else Color(0xFFF0FDF4),
            if (isDark) Color(0xFF1F5238) else Color(0xFF86EFAC),
            Color(0x2015803D)
        )
        else -> Triple(
            if (isDark) Color(0xFF16251E) else Color(0xFFF8FCFA),
            if (isDark) Color(0xFF1F4232) else Color(0xFFBBF7D0),
            Color(0x1415803D)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 3.dp, shape = RaahiCardShape, spotColor = spotTint)
            .background(cardBg, RaahiCardShape)
            .border(1.dp, cardBorder, RaahiCardShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (iconVector, iconTint, iconBg, title, subtitle) = when {
            !hasLocationPermission -> Quint(
                Icons.Outlined.LocationOff,
                Color(0xFFD97706),
                if (isDark) Color(0xFF3B2A16) else Color(0xFFFEF3C7),
                "Drive Tracking Paused",
                "Location access needed to detect drives automatically."
            )
            isDrivingConfirmed -> Quint(
                Icons.Filled.Navigation,
                Color(0xFF15803D),
                if (isDark) Color(0xFF1A3B28) else Color(0xFFDCFCE7),
                "Drive in Progress",
                "Recording trip distance · Odometer sync active"
            )
            else -> Quint(
                Icons.Outlined.DirectionsCar,
                Color(0xFF15803D),
                if (isDark) Color(0xFF1A3828) else Color(0xFFDCFCE7),
                "Auto Drive Detection",
                "Monitoring for your next drive"
            )
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = RaahiText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RaahiDisplayFont
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = RaahiTextDim,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        Spacer(Modifier.width(10.dp))

        when {
            !hasLocationPermission -> {
                Button(
                    onClick = onRequestLocationPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiWarning),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Enable", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            isDrivingConfirmed -> {
                Button(
                    onClick = onStopDrive,
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiError),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Stop Drive", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            else -> {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isDark) Color(0xFF142E20) else Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1F5238) else Color(0xFF86EFAC)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF15803D))
                        )
                        Text(
                            text = "Monitoring",
                            color = Color(0xFF15803D),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private data class Quint<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

/**
 * Compact, refined greeting header matching premium mobility product standards:
 * Minimal vertical space, calm hierarchy, and clear typography.
 */
@Composable
private fun AutomotiveHeroSection(user: UserDto?) {
    val firstName = user?.name?.trim()?.takeIf { it.isNotBlank() }?.split("\\s+".toRegex())?.firstOrNull()?.takeIf { it.isNotBlank() }
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good evening"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (firstName != null) "$greeting, $firstName" else greeting,
            color = RaahiText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = RaahiDisplayFont,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Safe journeys make happier stories.",
            color = RaahiTextDim,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Primary 2 cards matching Screen 6: Request Help & Find Mechanic
 */
@Composable
private fun PrimaryActionCards(
    onRequestHelp: () -> Unit,
    onNearbyMechanics: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Request Help card
        GlassCard(
            modifier = Modifier.weight(1f),
            shape = RaahiCardShape,
            onClick = onRequestHelp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isDark = RaahiIsDark
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(if (isDark) Color(0xFF222629) else Color(0xFFEDF0F2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SupportAgent,
                        contentDescription = null,
                        tint = RaahiText,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Request Help",
                    color = RaahiText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RaahiDisplayFont
                )
                Text(
                    text = "Get roadside assistance",
                    color = RaahiTextDim,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }

        // Find Mechanic card
        GlassCard(
            modifier = Modifier.weight(1f),
            shape = RaahiCardShape,
            onClick = onNearbyMechanics
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isDark = RaahiIsDark
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(if (isDark) Color(0xFF14273E) else Color(0xFFF0F9FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = RaahiIcons.Wrench,
                        contentDescription = null,
                        tint = RaahiCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Find Mechanic",
                    color = RaahiText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RaahiDisplayFont
                )
                Text(
                    text = "Near you",
                    color = RaahiTextDim,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Secondary 3 actions matching Screen 6: My Jobs, Car Health, SOS Emergency
 */
@Composable
private fun SecondaryActionsRow(
    onMyJobs: () -> Unit,
    onCarHealth: () -> Unit,
    onSos: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SecondaryActionTile(
            title = "My Jobs",
            icon = Icons.Outlined.History,
            iconTint = RaahiAmber,
            modifier = Modifier.weight(1f),
            onClick = onMyJobs
        )
        SecondaryActionTile(
            title = "Car Health",
            icon = Icons.Outlined.DirectionsCar,
            iconTint = RaahiGreen,
            modifier = Modifier.weight(1f),
            onClick = onCarHealth
        )
        SecondaryActionTile(
            title = "SOS Emergency",
            icon = Icons.Outlined.Warning,
            iconTint = RaahiRed,
            isSos = true,
            modifier = Modifier.weight(1f),
            onClick = onSos
        )
    }
}

@Composable
private fun SecondaryActionTile(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    isSos: Boolean = false,
    onClick: () -> Unit,
) {
    val isDark = RaahiIsDark
    val bg = if (isSos) (if (isDark) Color(0xFF2E1719) else Color(0xFFFFF0ED)) else RaahiGlass
    val border = if (isSos) (if (isDark) Color(0xFF5A2429) else Color(0xFFFFCDD2)) else RaahiBorderSoft
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        background = bg,
        borderColor = border,
        onClick = onClick
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                color = if (isSos) RaahiRed else RaahiText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                fontFamily = RaahiDisplayFont
            )
        }
    }
}

/**
 * AI Mechanic Card matching Screen 6
 */
@Composable
private fun AiMechanicCard(onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RaahiCardShape,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8))), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "AI Mechanic",
                    color = RaahiText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RaahiDisplayFont
                )
                Text(
                    text = "Ask anything about your car",
                    color = RaahiTextDim,
                    fontSize = 11.5.sp
                )
            }
            Icon(
                imageVector = RaahiIcons.ArrowRight,
                contentDescription = null,
                tint = RaahiTextDim,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun HelperApplicationStatusCard(
    status: HelperStatusDto?, busy: Boolean, error: String?,
    onToggle: (Boolean) -> Unit, onRetry: () -> Unit,
    onOpenDashboard: () -> Unit, onApplyOrReview: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Raahi Helper", color = RaahiText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            when {
                status == null -> {
                    Text(error ?: "Loading application status…", color = RaahiTextDim, fontSize = 12.5.sp)
                    TextButton(onClick = onRetry) { Text("Retry", color = RaahiText) }
                }
                status.applicationStatus == "APPROVED" -> {
                    Text(if (status.online) "You're online and can receive nearby requests."
                        else "You're offline. Go online to receive nearby requests.",
                        color = RaahiTextDim, fontSize = 12.5.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (status.online) "Online" else "Offline",
                            color = if (status.online) RaahiGreen else RaahiTextDim,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (busy) CircularProgressIndicator(color = RaahiPrimaryButton,
                            modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Switch(checked = status.online, onCheckedChange = onToggle,
                            enabled = status.accountActive && status.blockedUntil == null,
                            colors = SwitchDefaults.colors(checkedTrackColor = RaahiPrimaryButton,
                                checkedThumbColor = Color.White))
                    }
                    TextButton(onClick = onOpenDashboard) { Text("Open Helper Dashboard", color = RaahiText) }
                }
                else -> {
                    val info = when (status.applicationStatus) {
                        "PENDING" -> Triple("Application under review", "Your documents are waiting for approval.", "View status")
                        "REJECTED" -> Triple("Application not approved", status.rejectionReason ?: "Review your application.", "Review application")
                        "SUSPENDED" -> Triple("Helper account suspended", status.suspensionReason ?: "Contact support to review your account.", "View details")
                        else -> Triple("Become a Raahi helper", "Apply to receive nearby roadside requests.", "Apply now")
                    }
                    Text(info.first, color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(info.second, color = RaahiTextDim, fontSize = 12.5.sp)
                    TextButton(onClick = onApplyOrReview) { Text(info.third, color = RaahiText) }
                }
            }
            if (!error.isNullOrBlank() && status != null) {
                Text(error, color = RaahiRed, fontSize = 12.sp)
                TextButton(onClick = onRetry) { Text("Retry", color = RaahiText) }
            }
        }
    }
}

@Composable
private fun BecomeHelperCard(onClick: () -> Unit) {
    RowCard(modifier = Modifier.padding(horizontal = 16.dp), onClick = onClick) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF0FDF4), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.History, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Earn with Raahi", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text("Become a helper and earn cash helping stranded drivers nearby", color = RaahiTextDim, fontSize = 11.sp)
        }
        Text("Apply →", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun HelperDashboardCard(onClick: () -> Unit) {
    RowCard(modifier = Modifier.padding(horizontal = 16.dp), onClick = onClick) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.SupportAgent, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Helper Dashboard", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text("Go online, see nearby requests and your earnings", color = RaahiTextDim, fontSize = 11.sp)
        }
        Text("Open →", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun ActiveJobBanner(job: JobDto, onClick: () -> Unit) {
    val isDark = RaahiIsDark
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF2C2114) else Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF4A371F) else Color(0xFFFDE68A)),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(if (isDark) Color(0xFF3B2A16) else Color(0xFFFEF3C7), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.SupportAgent, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (job.viewerRole == "HELPER") "You have an active job" else "You have an active request", color = Color(0xFFB45309), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(job.problemType, color = RaahiText, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
            }
            Text("View →", color = Color(0xFFD97706), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ActiveIntercityTripBanner(
    trip: `in`.raahi.app.network.IntercityTripDto,
    onClick: () -> Unit,
) {
    val isDark = RaahiIsDark
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF14243B) else Color(0xFFEFF6FF),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1F3D66) else Color(0xFFBFDBFE)),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(if (isDark) Color(0xFF1D3557) else Color(0xFFDBEAFE), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ACTIVE INTERCITY TRIP", color = Color(0xFF2563EB), fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp, letterSpacing = 0.5.sp)
                    Surface(
                        color = if (isDark) Color(0xFF1D3557) else Color(0xFFDBEAFE),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(trip.status.replace("_", " "), color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("${trip.pickupCity} → ${trip.destCity} (₹${trip.fare.toInt()})", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Text("Tap for live route & OTP verification", color = RaahiTextDim, fontSize = 11.sp)
            }
            Text("Track →", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RoleBasedMarketplaceDashboard(
    user: UserDto?,
    vehicle: VehicleDto?,
    helperStatus: HelperStatusDto?,
    activeIntercityTrip: IntercityTripDto?,
    onCreateTrip: () -> Unit,
    onAgentDashboard: () -> Unit,
    onDriverTrips: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenActiveTrip: (String) -> Unit,
    onTripHistory: () -> Unit,
) {
    if (user == null) {
        return
    }
    val role = user.role.uppercase()
    val isTravelAgent = role == "TRAVEL_AGENT"
    val isOwner = role in listOf("VEHICLE_OWNER", "OWNER")

    when {
        isTravelAgent -> TravelAgentDashboardCard(
            helperStatus = helperStatus,
            activeIntercityTrip = activeIntercityTrip,
            onCreateTrip = onCreateTrip,
            onAgentDashboard = onAgentDashboard,
            onOpenMarketplace = onOpenMarketplace,
            onOpenActiveTrip = onOpenActiveTrip,
            onTripHistory = onTripHistory,
        )
        isOwner -> OwnerDashboardCard(
            activeIntercityTrip = activeIntercityTrip,
            onCreateTrip = onCreateTrip,
            onMyTrips = onAgentDashboard,
            onOpenActiveTrip = onOpenActiveTrip,
            onTripHistory = onTripHistory,
        )
        else -> DriverDashboardCard(
            activeIntercityTrip = activeIntercityTrip,
            onDriverTrips = onDriverTrips,
            onOpenMarketplace = onOpenMarketplace,
            onOpenActiveTrip = onOpenActiveTrip,
            onTripHistory = onTripHistory,
        )
    }
}

@Composable
private fun OwnerDashboardCard(
    activeIntercityTrip: IntercityTripDto?,
    onCreateTrip: () -> Unit,
    onMyTrips: () -> Unit,
    onOpenActiveTrip: (String) -> Unit,
    onTripHistory: () -> Unit,
) {
    val isDark = RaahiIsDark
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Card: Dark Charcoal Header Container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RaahiCardShape,
            color = Color(0xFF17191B),
            border = BorderStroke(1.dp, Color(0xFFF57C00).copy(alpha = 0.35f)),
            shadowElevation = 3.5.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF222629),
                        border = BorderStroke(1.dp, Color(0xFF2E3338))
                    ) {
                        Text(
                            text = "VEHICLE OWNER",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E7EA),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF142E20),
                        border = BorderStroke(1.dp, Color(0xFF1E4B33))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(Modifier.size(6.dp).background(Color(0xFF22C55E), CircleShape))
                            Text(
                                text = "0% Platform Fee",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80)
                            )
                        }
                    }
                }

                // Title & Subtitle
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Your Journey, Verified Drivers",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = RaahiDisplayFont
                    )
                    Text(
                        text = "Post your personal vehicle journey, review interested drivers, and assign for departure.",
                        fontSize = 12.sp,
                        color = Color(0xFFA0AAB0)
                    )
                }

                // Dominant Action: Post a Trip as Owner
                Button(
                    onClick = onCreateTrip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .shadow(2.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RaahiPrimaryButton,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Post a Trip as Owner",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // Two Primary Role Actions: My Trips (with applicant info inside) & Completed Trips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onMyTrips),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF222629) else Color(0xFFFFFDFB),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF3E3124) else Color(0xFFFFE0B2)),
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (isDark) Color(0xFF332214) else Color(0xFFFFF3E0), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("My Trips", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = RaahiText)
                        Text("Posted routes & drivers", fontSize = 11.sp, color = if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C), fontWeight = FontWeight.Medium)
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onTripHistory),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF222629) else Color(0xFFF8FAFD),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1E334D) else Color(0xFFBFDBFE)),
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (isDark) Color(0xFF16253C) else Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Completed Trips", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = RaahiText)
                        Text("History & summaries", fontSize = 11.sp, color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Active Trip Banner inside card (if available)
        if (activeIntercityTrip != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenActiveTrip(activeIntercityTrip.id) },
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF14243B) else Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1F3D66) else Color(0xFFBFDBFE)),
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ACTIVE TRIP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        Text("${activeIntercityTrip.pickupCity} → ${activeIntercityTrip.destCity}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RaahiText)
                    }
                    Text("Track & OTP →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                }
            }
        }
    }
}

@Composable
private fun TravelAgentDashboardCard(
    helperStatus: HelperStatusDto?,
    activeIntercityTrip: IntercityTripDto?,
    onCreateTrip: () -> Unit,
    onAgentDashboard: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenActiveTrip: (String) -> Unit,
    onTripHistory: () -> Unit,
) {
    val isDark = RaahiIsDark
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Card: Dark Charcoal Header Container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RaahiCardShape,
            color = Color(0xFF17191B),
            border = BorderStroke(1.dp, Color(0xFFF57C00).copy(alpha = 0.35f)),
            shadowElevation = 3.5.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Role Badge + Commercial Hub
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF222629),
                        border = BorderStroke(1.dp, Color(0xFF2E3338))
                    ) {
                        Text(
                            text = "TRAVEL AGENT",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E7EA),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF132238),
                        border = BorderStroke(1.dp, Color(0xFF1E3A60))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(Modifier.size(6.dp).background(Color(0xFF3B82F6), CircleShape))
                            Text(
                                text = "Commercial Hub",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF60A5FA)
                            )
                        }
                    }
                }

                // Title & Subtitle
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Manage Trips Grow Your Fleet",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = RaahiDisplayFont
                    )
                    Text(
                        text = "Publish corridor trips, review driver applications, and manage commission settlements.",
                        fontSize = 12.sp,
                        color = Color(0xFFA0AAB0)
                    )
                }

                // Dominant Actions: Create Trip & Agency Hub
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCreateTrip,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(46.dp)
                            .shadow(2.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RaahiPrimaryButton,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Create Trip",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onAgentDashboard,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E3338)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF222629),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Agency Hub",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 2x2 Metric Cards (Screen 3 Mockup) with Distinct Automotive Tonal Identity & Depth
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAgentDashboard),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF222629) else Color(0xFFFFFDF9),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF3E3124) else Color(0xFFFFE0B2)),
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isDark) Color(0xFF332214) else Color(0xFFFFF3E0), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.BusinessCenter, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("My Trips", fontSize = 11.5.sp, color = RaahiTextDim)
                        Text("12 Active", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RaahiText)
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAgentDashboard),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF222629) else Color(0xFFF8FAFD),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1E334D) else Color(0xFFBFDBFE)),
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isDark) Color(0xFF16253C) else Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Driver Interests", fontSize = 11.5.sp, color = RaahiTextDim)
                        Text("7 Pending", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RaahiText)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAgentDashboard),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF222629) else Color(0xFFF8FCFA),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1B3D2B) else Color(0xFFBBF7D0)),
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isDark) Color(0xFF132B1E) else Color(0xFFF0FDF4), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Assignments", fontSize = 11.5.sp, color = RaahiTextDim)
                        Text("4 Ongoing", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RaahiText)
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAgentDashboard),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF222629) else Color(0xFFFAF5FF),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF32214A) else Color(0xFFDDD6FE)),
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isDark) Color(0xFF25173E) else Color(0xFFF5F3FF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Total Earnings", fontSize = 11.5.sp, color = RaahiTextDim)
                        Text("₹48.7K", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RaahiPriceText)
                    }
                }
            }
        }

        // Commission & Settlement Quick Bar
        val balance = helperStatus?.commissionBalance ?: 0.0
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAgentDashboard),
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF16251E) else Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1F4E34) else Color(0xFFBBF7D0)),
            shadowElevation = 2.5.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("COMMISSION SETTLEMENT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RaahiTextDim)
                    Text(
                        text = if (balance != 0.0) "Balance: ₹${if (balance % 1.0 == 0.0) balance.toLong().toString() else "%.2f".format(balance)}" else "Settlement up to date",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (balance < 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                    )
                }
                Text("Settlement →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
            }
        }

        // Active Trip Banner inside card (if available)
        if (activeIntercityTrip != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenActiveTrip(activeIntercityTrip.id) },
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF14243B) else Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1F3D66) else Color(0xFFBFDBFE)),
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ACTIVE TRIP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        Text("${activeIntercityTrip.pickupCity} → ${activeIntercityTrip.destCity}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RaahiText)
                    }
                    Text("Track & OTP →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                }
            }
        }

        // Active Trip Banner inside card (if available)
        if (activeIntercityTrip != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenActiveTrip(activeIntercityTrip.id) },
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF222629) else Color.White,
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2E3338) else Color(0xFFE3E7E9))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ACTIVE TRIP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RaahiTextDim)
                        Text("${activeIntercityTrip.pickupCity} → ${activeIntercityTrip.destCity}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RaahiText)
                    }
                    Text("Track & Verify →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RaahiText)
                }
            }
        }

        // Footer Links
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Marketplace Overview",
                modifier = Modifier.clickable(onClick = onOpenMarketplace).padding(vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = RaahiText
            )
            Text(
                text = "Trip History",
                modifier = Modifier.clickable(onClick = onTripHistory).padding(vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = RaahiTextDim
            )
        }
    }
}

@Composable
private fun DriverDashboardCard(
    activeIntercityTrip: IntercityTripDto?,
    onDriverTrips: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenActiveTrip: (String) -> Unit,
    onTripHistory: () -> Unit,
) {
    val isDark = RaahiIsDark
    var selectedFilterChip by remember { mutableStateOf("All") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Card: Dark Charcoal Header Container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RaahiCardShape,
            color = Color(0xFF17191B),
            border = BorderStroke(1.dp, Color(0xFFF57C00).copy(alpha = 0.35f)),
            shadowElevation = 3.5.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Role Badge + Opportunities
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF222629),
                        border = BorderStroke(1.dp, Color(0xFF2E3338))
                    ) {
                        Text(
                            text = "VERIFIED DRIVER",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E7EA),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF142E20),
                        border = BorderStroke(1.dp, Color(0xFF1E4B33))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(Modifier.size(6.dp).background(Color(0xFF22C55E), CircleShape))
                            Text(
                                text = "Paid Routes Open",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80)
                            )
                        }
                    }
                }

                // Title & Subtitle
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "More Trips Bigger Opportunities",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = RaahiDisplayFont
                    )
                    Text(
                        text = "Drive. Earn. Grow with Raahi. Browse verified intercity trips and earn on high-demand corridors.",
                        fontSize = 12.sp,
                        color = Color(0xFFA0AAB0)
                    )
                }

                // Dominant Action: Find Available Trips
                Button(
                    onClick = onOpenMarketplace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .shadow(2.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RaahiPrimaryButton,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Find Available Trips",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Filter Chips Row (Screen 1 Mockup)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filterChips = listOf("All", "Nearby", "Today", "Tomorrow")
            filterChips.forEach { chip ->
                val selected = selectedFilterChip == chip
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFilterChip = chip },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) RaahiPrimaryButton else if (isDark) Color(0xFF222629) else Color.White,
                    border = BorderStroke(1.dp, if (selected) RaahiPrimaryButton else if (isDark) Color(0xFF2E3338) else Color(0xFFE2E8F0)),
                    shadowElevation = if (selected) 2.dp else 1.dp
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chip,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else RaahiText
                        )
                    }
                }
            }
        }

        // Secondary Actions: My Interests & Assigned Trips with Distinctive Tinting & Borders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDriverTrips,
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1E3A60) else Color(0xFFBFDBFE)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isDark) Color(0xFF14243B) else Color(0xFFEFF6FF),
                    contentColor = Color(0xFF2563EB)
                ),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF2563EB))
                Spacer(Modifier.width(5.dp))
                Text("My Interests", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, color = Color(0xFF1D4ED8))
            }

            OutlinedButton(
                onClick = onDriverTrips,
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1B3D2B) else Color(0xFFBBF7D0)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isDark) Color(0xFF132B1E) else Color(0xFFF0FDF4),
                    contentColor = Color(0xFF16A34A)
                ),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(Icons.Outlined.DirectionsCar, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF16A34A))
                Spacer(Modifier.width(5.dp))
                Text("Assigned Trips", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, color = Color(0xFF15803D))
            }
        }

        // Active / Assigned Trip Banner (if available)
        if (activeIntercityTrip != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenActiveTrip(activeIntercityTrip.id) },
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF14243B) else Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1F3D66) else Color(0xFFBFDBFE)),
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ASSIGNED ROUTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        Text("${activeIntercityTrip.pickupCity} → ${activeIntercityTrip.destCity} (₹${activeIntercityTrip.fare.toInt()})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                    }
                    Text("Start Drive →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                }
            }
        }

        // Available Trip Preview Card (matching Screen 1 Mockup)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenMarketplace),
            shape = RaahiCardShape,
            color = if (isDark) Color(0xFF1F2429) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF2E3842) else Color(0xFFE2E8F0)),
            shadowElevation = 3.5.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row: Route + Poster Type Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text("Chandigarh", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RaahiText)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(15.dp))
                        Text("Delhi NCR", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RaahiText)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF332214) else Color(0xFFFFF3E0),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF4A321E) else Color(0xFFFFD180))
                    ) {
                        Text(
                            text = "By Owner",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEA580C)
                        )
                    }
                }

                // Sub-info Row: Date & Time + Vehicle Spec
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(14.dp))
                        Text("Today · 4:00 PM departure", fontSize = 12.sp, color = RaahiTextDim)
                    }
                    Text("Sedan • AC", fontSize = 12.sp, color = RaahiTextDim, fontWeight = FontWeight.Medium)
                }

                // Dedicated Fare & Action Section: Fare has its own dedicated space with clear boundaries
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF132A1D) else Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1E4E33) else Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DRIVER EARNING",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "₹4,500",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = Color(0xFF15803D)
                            )
                        }

                        Button(
                            onClick = onOpenMarketplace,
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RaahiPrimaryButton,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text("Express Interest", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Driver Completed Rides Action
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTripHistory),
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF222629) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF2E3338) else Color(0xFFE3E7E9)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "View Completed Rides & Summaries",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RaahiText,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HomeVehicleFeatureChip(text: String) {
    val isDark = RaahiIsDark
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isDark) Color(0xFF1E2225) else Color(0xFFF4F6F8),
        border = BorderStroke(0.5.dp, if (isDark) Color(0xFF2E3338) else Color(0xFFE2E6EA))
    ) {
        Text(
            text = "✓ $text",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = RaahiTextDim,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun VehicleHeroCard(
    vehicle: VehicleDto?,
    carHealth: CarHealthDto?,
    onSetupVehicle: () -> Unit,
    onCarHealth: () -> Unit
) {
    val isDark = RaahiIsDark
    if (vehicle == null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(onClick = onSetupVehicle),
            shape = RaahiCardShape,
            color = if (isDark) Color(0xFF1E2226) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF2E3640) else Color(0xFFE2E8F0)),
            shadowElevation = 3.5.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF332214) else Color(0xFFFFF3E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.DirectionsCar,
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MY VEHICLE",
                                color = Color(0xFFEA580C),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Add your vehicle",
                                color = RaahiText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RaahiDisplayFont
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF332214) else Color(0xFFFFF3E0),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF4A3420) else Color(0xFFFFD180))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(13.dp))
                            Text("Add", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                        }
                    }
                }

                Text(
                    text = "Unlock car health telemetry, automated odometer tracking and service reminders.",
                    color = RaahiTextDim,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )

                // Feature pills with colorful tinting
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HomeVehicleFeatureChip("Health Telemetry")
                    HomeVehicleFeatureChip("Odometer Sync")
                    HomeVehicleFeatureChip("Reminders")
                }
            }
        }
        return
    }

    // VEHICLE REGISTERED: Clean, compact automotive composition using unified VehicleComponents
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RaahiCardShape,
        color = if (isDark) Color(0xFF1E2226) else Color.White,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF2E3640) else Color(0xFFE2E8F0)),
        shadowElevation = 3.5.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Vehicle name + License Plate Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "MY VEHICLE",
                        color = Color(0xFFEA580C),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${vehicle.brand} ${vehicle.model}${if (!vehicle.variant.isNullOrBlank()) " ${vehicle.variant}" else ""}".trim(),
                        color = RaahiText,
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RaahiDisplayFont
                    )
                }

                // Indian License Plate Badge
                VehicleNumberPlate(
                    registrationNumber = vehicle.registrationNumber,
                    style = LicensePlateStyle.Compact
                )
            }

            // Automotive Front Vehicle Illustration
            VehicleIllustration(
                size = VehicleIllustrationSize.Standard,
                vehicleBrand = vehicle.brand,
                vehicleModel = vehicle.model,
                contentDescription = "${vehicle.brand} ${vehicle.model} Front View",
                modifier = Modifier.fillMaxWidth()
            )

            // Compact Specification Bar with Distinct Visual Telemetry Cards
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF161A1E) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF272F38) else Color(0xFFE2E8F0)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Spec 1: Odometer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(if (isDark) Color(0xFF14243B) else Color(0xFFEFF6FF), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Column {
                            Text("ODOMETER", fontSize = 8.5.sp, color = RaahiTextDim, fontWeight = FontWeight.Bold)
                            Text(if (vehicle.odometerKm > 0) "%,d km".format(vehicle.odometerKm) else "0 km", fontSize = 11.5.sp, color = RaahiText, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(Modifier.width(1.dp).height(24.dp).background(if (isDark) Color(0xFF2E3338) else Color(0xFFE2E8F0)))

                    // Spec 2: Fuel Type
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(if (isDark) Color(0xFF352611) else Color(0xFFFFFBEB), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalGasStation,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Column {
                            Text("FUEL", fontSize = 8.5.sp, color = RaahiTextDim, fontWeight = FontWeight.Bold)
                            Text(vehicle.fuelType.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.5.sp, color = RaahiText, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(Modifier.width(1.dp).height(24.dp).background(if (isDark) Color(0xFF2E3338) else Color(0xFFE2E8F0)))

                    // Spec 3: Car Health Score
                    val (serviceText, serviceTone) = serviceStat(carHealth)
                    val serviceColor = when (serviceTone) {
                        ServiceTone.NO_DATA -> RaahiSecondaryText
                        ServiceTone.OK -> Color(0xFF15803D)
                        ServiceTone.DUE_SOON -> RaahiAmber
                        ServiceTone.OVERDUE -> RaahiRed
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable(onClick = onCarHealth)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(if (isDark) Color(0xFF132B1E) else Color(0xFFF0FDF4), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Favorite,
                                contentDescription = null,
                                tint = serviceColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Column {
                            Text("HEALTH", fontSize = 8.5.sp, color = RaahiTextDim, fontWeight = FontWeight.Bold)
                            Text(if (carHealth?.score != null) "${carHealth.score}/100" else serviceText, fontSize = 11.5.sp, color = serviceColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Bottom Action Links: "My Garage & Records"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSetupVehicle)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Manage vehicle records & garage",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEA580C),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFFEA580C),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable

private fun WeekStrip(metrics: HomeMetrics, onAddFuel: () -> Unit, modifier: Modifier = Modifier) {
    val km = weeklyKmStat(metrics.summary)
    val helps = helpsGivenStat(metrics.summary)
    val fuel = fuelSpendStat(metrics.fuel)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WeekStat(Icons.Outlined.DirectionsCar, km.value, km.label, Modifier.weight(1f))
        WeekStat(Icons.Outlined.SupportAgent, helps.value, helps.label, Modifier.weight(1f))
        WeekStat(Icons.Outlined.WaterDrop, fuel.value, fuel.label, Modifier.weight(1f), onClick = onAddFuel)
    }
}

@Composable
private fun WeekStat(icon: ImageVector, value: String, label: String, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val isDark = RaahiIsDark
    val (iconBg, iconTint) = when {
        label.contains("km", ignoreCase = true) -> (if (isDark) Color(0xFF14243B) else Color(0xFFEFF6FF)) to Color(0xFF2563EB)
        label.contains("help", ignoreCase = true) -> (if (isDark) Color(0xFF132B1E) else Color(0xFFF0FDF4)) to Color(0xFF16A34A)
        else -> (if (isDark) Color(0xFF352611) else Color(0xFFFFFBEB)) to Color(0xFFD97706)
    }

    Column(
        modifier
            .shadow(elevation = 2.5.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0x10000000))
            .background(if (isDark) Color(0xFF1E2226) else Color(0xFFFFFFFF), RoundedCornerShape(14.dp))
            .border(1.dp, if (isDark) Color(0xFF2E3842) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(iconBg, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            color = RaahiText,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = RaahiDisplayFont,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            label,
            color = RaahiTextDim,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlanTripCard(
    activeTrip: `in`.raahi.app.network.TripDto?,
    onPlanTrip: () -> Unit,
    onOpenActiveTrip: (String) -> Unit,
) {
    val isDark = RaahiIsDark
    if (activeTrip != null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onOpenActiveTrip(activeTrip.id) },
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF13243A) else Color(0xFFF0F9FF),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E3D66) else Color(0xFF93C5FD)),
            shadowElevation = 3.5.dp,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1D3557) else Color(0xFFDBEAFE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ACTIVE HIGHWAY TRIP", color = Color(0xFF2563EB), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF15803D)))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(activeTrip.title, color = RaahiText, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
                    Text("${activeTrip.distanceKm} km · In progress", color = RaahiTextDim, fontSize = 12.sp)
                }
                Text("Resume →", color = Color(0xFF2563EB), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(onClick = onPlanTrip),
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF24201A) else Color(0xFFFFFDF9),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF45331E) else Color(0xFFFFD180)),
            shadowElevation = 3.5.dp,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF382514) else Color(0xFFFFF3E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = Color(0xFFEF6C00), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Plan a Highway Trip", color = RaahiText, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
                    Spacer(Modifier.height(2.dp))
                    Text("Fuel stops, pit stops & weather warnings", color = RaahiTextDim, fontSize = 12.sp)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RaahiPrimaryButton,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        "Plan →",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyRow(onOpenDaily: (String) -> Unit) {
    val items = listOf(
        Triple("paisa", "\uD83D\uDCB0", "Paisa Bachao"),
        Triple("trips", "\uD83D\uDEE3\uFE0F", "Trips"),
        Triple("report", "\uD83D\uDCCB", "Report Card"),
        Triple("streak", "\uD83D\uDD25", "Streak"), Triple("alerts", "\u26A0\uFE0F", "Alerts"),
        Triple("places", "\uD83D\uDCCD", "Places"), Triple("tips", "\uD83D\uDCA1", "Tips"),
        Triple("fuel", "\u26FD", "Fuel"), Triple("shop", "\uD83D\uDED2", "Shop"), Triple("plans", "\u2B50", "Plans"),
    )
    val isDark = RaahiIsDark
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(items) { (key, emoji, label) ->
            Column(
                modifier = Modifier
                    .width(78.dp)
                    .shadow(elevation = 2.5.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0x10000000))
                    .background(if (isDark) Color(0xFF1E2226) else Color(0xFFFFFFFF), RoundedCornerShape(14.dp))
                    .border(1.dp, if (isDark) Color(0xFF2E3842) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    .clickable { onOpenDaily(key) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(if (isDark) Color(0xFF2A313A) else Color(0xFFF1F5F9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 17.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text(label, color = RaahiText, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    }
}


private fun hasLocationPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

data class CityLocation(val name: String, val state: String, val lat: Double, val lng: Double)

val POPULAR_CITIES = listOf(
    CityLocation("Chandigarh", "Chandigarh", 30.7333, 76.7794),
    CityLocation("Mohali", "Punjab", 30.7046, 76.7179),
    CityLocation("Panchkula", "Haryana", 30.6942, 76.8606),
    CityLocation("Zirakpur", "Punjab", 30.6425, 76.8173),
    CityLocation("Delhi NCR", "Delhi", 28.6139, 77.2090),
    CityLocation("Gurgaon", "Haryana", 28.4595, 77.0266),
    CityLocation("Noida", "Uttar Pradesh", 28.5355, 77.3910),
    CityLocation("Ludhiana", "Punjab", 30.9010, 75.8573),
    CityLocation("Amritsar", "Punjab", 31.6340, 74.8723),
    CityLocation("Shimla", "Himachal Pradesh", 31.1048, 77.1734),
    CityLocation("Manali", "Himachal Pradesh", 32.2432, 77.1892),
    CityLocation("Jaipur", "Rajasthan", 26.9124, 75.7873),
    CityLocation("Mumbai", "Maharashtra", 19.0760, 72.8777),
    CityLocation("Bengaluru", "Karnataka", 12.9716, 77.5946),
    CityLocation("Pune", "Maharashtra", 18.5204, 73.8567),
)

@Composable
fun LocationSelectionDialog(
    currentCity: String,
    onDismiss: () -> Unit,
    onSelectCity: (String, Double, Double) -> Unit,
    onUseCurrentLocation: (Boolean, (Boolean, String?) -> Unit) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var isDetectingGps by remember { mutableStateOf(false) }
    var isResolvingCustomLocation by remember { mutableStateOf(false) }
    var gpsError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = RaahiIsDark

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isDetectingGps = true
            gpsError = null
            onUseCurrentLocation(true) { success, name ->
                isDetectingGps = false
                if (success) {
                    onDismiss()
                } else {
                    gpsError = name ?: "Could not detect location"
                }
            }
        } else {
            gpsError = "Location permission denied"
        }
    }

    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            CityCatalogue.popularCities()
        } else {
            CityCatalogue.filter(searchQuery)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        containerColor = RaahiBg,
        shape = RoundedCornerShape(24.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Location",
                        fontFamily = RaahiDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = RaahiText
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = RaahiTextDim)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Use current location button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(14.dp), spotColor = Color(0x10000000))
                        .background(RaahiGlass, RoundedCornerShape(14.dp))
                        .border(1.dp, if (isDark) Color(0xFF2E3338) else Color(0xFFE3E7E9), RoundedCornerShape(14.dp))
                        .clickable {
                            val hasPermission = hasLocationPermission(context)
                            if (hasPermission) {
                                isDetectingGps = true
                                gpsError = null
                                onUseCurrentLocation(true) { success, name ->
                                    isDetectingGps = false
                                    if (success) onDismiss() else gpsError = name ?: "Could not detect location"
                                }
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(if (isDark) Color(0xFF222629) else Color(0xFFEDF0F2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDetectingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RaahiPrimaryButton, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Navigation, contentDescription = null, tint = RaahiText, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Use current location", color = RaahiText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isDetectingGps) "Acquiring GPS fix..." else (gpsError ?: "GPS auto-detect for road help & mechanics"),
                            color = if (gpsError != null) RaahiRed else RaahiTextDim,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search city (e.g. Abohar, Chandigarh, Jaipur)", fontSize = 12.5.sp, color = RaahiTextFaint) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RaahiGlass,
                        unfocusedContainerColor = RaahiGlass,
                        focusedBorderColor = RaahiOrange,
                        unfocusedBorderColor = RaahiBorder,
                        focusedTextColor = RaahiText,
                        unfocusedTextColor = RaahiText
                    )
                )

                Spacer(Modifier.height(12.dp))

                // City List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    filteredCities.forEach { city ->
                        val isSelected = currentCity.equals(city.name, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) RaahiOrange.copy(alpha = 0.10f) else Color.Transparent)
                                .clickable {
                                    onSelectCity(city.name, city.lat, city.lng)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Place,
                                contentDescription = null,
                                tint = if (isSelected) RaahiOrange else RaahiTextFaint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(city.name, color = if (isSelected) RaahiOrange else RaahiText, fontSize = 13.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                Text("${city.region} • ${city.state}", color = RaahiTextDim, fontSize = 10.5.sp)
                            }
                            if (isSelected) {
                                Text("Selected", color = RaahiOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (filteredCities.isEmpty() && searchQuery.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(RaahiOrange.copy(alpha = 0.08f))
                                .clickable(enabled = !isResolvingCustomLocation) {
                                    coroutineScope.launch {
                                        isResolvingCustomLocation = true
                                        val resolved = LocationResolver.resolveCoordinates(context, searchQuery)
                                        isResolvingCustomLocation = false
                                        val lat = resolved?.first ?: 30.1453
                                        val lng = resolved?.second ?: 74.1993
                                        onSelectCity(searchQuery.trim(), lat, lng)
                                        onDismiss()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isResolvingCustomLocation) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RaahiOrange, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Place, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Use \"$searchQuery\"", color = RaahiOrange, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                Text("Resolve coordinates for village / town", color = RaahiTextDim, fontSize = 10.5.sp)
                            }
                        }
                    }
                }
            }
        }
    )
}
