package `in`.raahi.app.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import `in`.raahi.app.network.UserDto
import `in`.raahi.app.network.VehicleDto
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
    onAddFuel: () -> Unit,
    onNavigateTab: (RaahiTab) -> Unit,
    onPlanTrip: () -> Unit = {},
    onOpenActiveTrip: (String) -> Unit = {},
    onTripHistory: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshMetrics(hasLocationPermission(context))
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    `in`.raahi.app.ui.components.RequestNotificationPermissionOnce()

    Surface(modifier = Modifier.fillMaxSize(), color = RaahiBg) {
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
                        metrics = metrics, onOpenNotifications = onOpenNotifications, onAddFuel = onAddFuel,
                        onProfile = { onNavigateTab(RaahiTab.PROFILE) },
                    )
                    is HomeUiState.Loaded -> HomeContent(
                        user = s.user, activeJob = s.activeJob, activeTrip = s.activeTrip, vehicle = s.vehicle, carHealth = s.carHealth,
                        offlineError = s.offlineError, onRetry = viewModel::load,
                        selectedCity = selectedCity, onSelectCity = viewModel::selectCity, onUseCurrentLocation = viewModel::useCurrentLocation,
                        onRequestHelp = onRequestHelp, onNearbyMechanics = onNearbyMechanics, onMyJobs = onMyJobs,
                        onSos = onSos, onOpenActiveJob = onOpenActiveJob, onHelperDashboard = onHelperDashboard,
                        onBecomeHelper = onBecomeHelper, onAiMechanic = onAiMechanic, onCarHealth = onCarHealth,
                        onSetupVehicle = onSetupVehicle, onOpenDaily = onOpenDaily,
                        onPlanTrip = onPlanTrip, onOpenActiveTrip = onOpenActiveTrip, onTripHistory = onTripHistory,
                        metrics = metrics, onOpenNotifications = onOpenNotifications, onAddFuel = onAddFuel,
                        onProfile = { onNavigateTab(RaahiTab.PROFILE) },
                    )
                }
            }
            RaahiBottomNavBar(current = RaahiTab.HOME, onSelect = onNavigateTab)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = RaahiOrange)
    }
}

@Composable
private fun HomeContent(
    user: UserDto?, activeJob: JobDto?, activeTrip: `in`.raahi.app.network.TripDto? = null, vehicle: VehicleDto?, carHealth: CarHealthDto?,
    offlineError: String? = null, onRetry: () -> Unit = {},
    selectedCity: String = "Chandigarh",
    onSelectCity: (String, Double, Double) -> Unit = { _, _, _ -> },
    onUseCurrentLocation: (Boolean, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onRequestHelp: () -> Unit, onNearbyMechanics: () -> Unit, onMyJobs: () -> Unit, onSos: () -> Unit,
    onOpenActiveJob: (JobDto) -> Unit, onHelperDashboard: () -> Unit, onBecomeHelper: () -> Unit,
    onAiMechanic: () -> Unit, onCarHealth: () -> Unit, onSetupVehicle: () -> Unit,
    onOpenDaily: (String) -> Unit, onProfile: () -> Unit,
    onPlanTrip: () -> Unit = {}, onOpenActiveTrip: (String) -> Unit = {}, onTripHistory: () -> Unit = {},
    metrics: HomeMetrics = HomeMetrics(), onOpenNotifications: () -> Unit = {}, onAddFuel: () -> Unit = {},
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

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp)
        ) {
            Spacer(Modifier.height(64.dp)) // Space for floating header

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

            // 1. Greeting hero
            GreetingHeroBanner(user)
            Spacer(Modifier.height(14.dp))

            // 2. Primary Action Cards: Request Help & Find Mechanic (Screen 6 reference)
            PrimaryActionCards(onRequestHelp, onNearbyMechanics)
            Spacer(Modifier.height(12.dp))

            // 3. Secondary Actions Row: My Jobs, Car Health, SOS Emergency (Screen 6 reference)
            SecondaryActionsRow(onMyJobs, onCarHealth, onSos)
            Spacer(Modifier.height(14.dp))

            // 4. AI Mechanic Card (Screen 6 reference)
            AiMechanicCard(onClick = onAiMechanic)
            Spacer(Modifier.height(14.dp))

            // 5. Real Ticker (if available)
            val tickerItems = tickerItems(vehicle, carHealth, metrics)
            if (tickerItems.isNotEmpty()) {
                Ticker(items = tickerItems, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(14.dp))
            }

            // 6. Vehicle / Car Health Hero Card
            VehicleHeroCard(vehicle, carHealth, onSetupVehicle, onCarHealth)
            Spacer(Modifier.height(14.dp))

            // 7. Plan Trip Card
            PlanTripCard(
                activeTrip = activeTrip,
                onPlanTrip = onPlanTrip,
                onOpenActiveTrip = onOpenActiveTrip
            )

            Spacer(Modifier.height(14.dp))
            if (user?.role == "HELPER" || user?.role == "MECHANIC") {
                HelperDashboardCard(onClick = onHelperDashboard)
            } else if (user?.role == "DRIVER") {
                BecomeHelperCard(onClick = onBecomeHelper)
            }

            // 8. This week
            Spacer(Modifier.height(20.dp))
            Box(Modifier.padding(horizontal = 16.dp)) { SectionLabel("This week") }
            WeekStrip(metrics = metrics, onAddFuel = onAddFuel, modifier = Modifier.padding(horizontal = 16.dp))

            // 9. More Section
            Spacer(Modifier.height(18.dp))
            Box(Modifier.padding(horizontal = 16.dp)) { SectionLabel("More") }
            DailyRow(onOpenDaily)
        }

        // Overlay header with sticky blur/solid fill
        HeaderRow(
            user = user,
            selectedCity = selectedCity,
            onLocationClick = { showLocationDialog = true },
            onProfile = onProfile,
            scrolled = scrolled,
            unread = unreadCount(metrics.summary),
            onBell = onOpenNotifications
        )
    }
}

@Composable
private fun BoxScope.HeaderRow(
    user: UserDto?,
    selectedCity: String,
    onLocationClick: () -> Unit,
    onProfile: () -> Unit,
    scrolled: Boolean,
    unread: Int,
    onBell: () -> Unit
) {
    val scrollFraction by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f, label = "headerBgFraction",
    )
    val bg = androidx.compose.ui.graphics.lerp(Color.Transparent, Color(0xF2FAF7F2), scrollFraction)
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Logo & Location Dropdown (Screen 6 reference)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RaahiEmblem(size = 28.dp, glow = false)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Raahi",
                    fontFamily = RaahiDisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = RaahiText
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onLocationClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(selectedCity, color = RaahiTextDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Change location", tint = RaahiOrange, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Notification Bell Button
        BellWithBadge(unread = unread, onClick = onBell)
        Spacer(Modifier.width(10.dp))

        // User Avatar Circle Button
        Box(
            modifier = Modifier
                .size(34.dp)
                .shadow(elevation = 2.dp, shape = CircleShape, spotColor = Color(0x10000000))
                .background(RaahiOrange, CircleShape)
                .clickable(onClick = onProfile),
            contentAlignment = Alignment.Center,
        ) {
            val initial = user?.name?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
            Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = RaahiDisplayFont)
        }
    }
}

@Composable
private fun BellWithBadge(unread: Int, onClick: () -> Unit) {
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
            .size(34.dp)
            .shadow(elevation = 2.dp, shape = CircleShape, spotColor = Color(0x10000000))
            .background(Color.White, CircleShape)
            .border(1.dp, RaahiBorderSoft, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = RaahiTextDim, modifier = Modifier.size(17.dp).rotate(angle))
        if (unread > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(7.dp)
                    .background(RaahiOrange, CircleShape)
            )
        }
    }
}

/**
* Greeting hero card matching Screen 6 in reference design:
 * "Good Morning, Ankush / Safe journeys make happier stories." with mountain road + car
 */
@Composable
private fun GreetingHeroBanner(user: UserDto?) {
    val name = user?.name?.trim()?.split(" ")?.firstOrNull() ?: ""
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(132.dp)
    ) {
        // Atmospheric sunset warm glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x35FFA77B), Color(0x12FF5A36), Color.Transparent),
                        center = Offset(800f, 160f),
                        radius = 500f
                    )
                )
        )
        // Soft gradient overlay on left for typography readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color(0xFFFFF6F0),
                        0.38f to Color(0xFFFFF6F0),
                        0.58f to Color(0xFFFFF6F0).copy(alpha = 0.50f),
                        0.78f to Color.Transparent
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Good Morning,\n$name",
                color = RaahiText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RaahiDisplayFont,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Safe journeys make\nhappier stories.",
                color = RaahiTextDim,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
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
            shape = RoundedCornerShape(18.dp),
            onClick = onRequestHelp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(0xFFFFF0ED), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SupportAgent,
                        contentDescription = null,
                        tint = RaahiOrange,
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
            shape = RoundedCornerShape(18.dp),
            onClick = onNearbyMechanics
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(0xFFF0F9FF), CircleShape),
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
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        background = if (isSos) Color(0xFFFFF0ED) else Color.White,
        borderColor = if (isSos) Color(0xFFFFCDD2) else RaahiBorderSoft,
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
        shape = RoundedCornerShape(18.dp),
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
                tint = RaahiOrange,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun BecomeHelperCard(onClick: () -> Unit) {
    RowCard(modifier = Modifier.padding(horizontal = 16.dp), onClick = onClick) {
        IconBadge(Icons.Outlined.History, RaahiGreen, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Earn with Raahi", color = RaahiGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Become a helper and earn cash helping stranded drivers nearby", color = RaahiTextDim, fontSize = 11.sp)
        }
        Text("Apply", color = RaahiGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun HelperDashboardCard(onClick: () -> Unit) {
    RowCard(modifier = Modifier.padding(horizontal = 16.dp), onClick = onClick) {
        IconBadge(Icons.Outlined.History, RaahiOrange, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Helper Dashboard", color = RaahiOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Go online, see nearby requests and your earnings", color = RaahiTextDim, fontSize = 11.sp)
        }
        Text("Open", color = RaahiOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun ActiveJobBanner(job: JobDto, onClick: () -> Unit) {
    RowCard(modifier = Modifier.padding(horizontal = 16.dp), onClick = onClick) {
        IconBadge(Icons.Outlined.History, RaahiOrange, 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(if (job.viewerRole == "HELPER") "You have an active job" else "You have an active request", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(job.problemType, color = RaahiTextDim, fontSize = 11.sp)
        }
        Text("View", color = RaahiOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun VehicleHeroCard(vehicle: VehicleDto?, carHealth: CarHealthDto?, onSetupVehicle: () -> Unit, onCarHealth: () -> Unit) {
    if (vehicle == null) {
        RowCard(modifier = Modifier.padding(horizontal = 16.dp), onClick = onSetupVehicle) {
            IconBadge(Icons.Outlined.DirectionsCar, RaahiOrange, 44.dp, RaahiShapeMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Set up your car", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("So mechanics know what they're helping with", color = RaahiTextDim, fontSize = 11.sp)
            }
            Icon(RaahiIcons.ArrowRight, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(16.dp))
        }
        return
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        onClick = onCarHealth,
    ) {
        Box(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${vehicle.brand.uppercase()} ${vehicle.model.uppercase()} · ${vehicle.registrationNumber}",
                        color = RaahiTextDim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        heroTitle(carHealth),
                        color = RaahiText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = RaahiDisplayFont,
                    )
                    Text(
                        heroSubtitle(carHealth),
                        color = RaahiTextDim, fontSize = 11.sp,
                    )
                }
                if (carHealth?.score != null) ScoreRing(carHealth.score)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                HeroStat("ODOMETER", "${vehicle.odometerKm} km", Modifier.weight(1f))
                HeroStat("FUEL", vehicle.fuelType.lowercase().replaceFirstChar { it.uppercase() }, Modifier.weight(1f))
                val (serviceText, serviceTone) = serviceStat(carHealth)
                val serviceColor = when (serviceTone) {
                    ServiceTone.NO_DATA -> RaahiTextFaint
                    ServiceTone.OK -> RaahiGreen
                    ServiceTone.DUE_SOON -> RaahiAmber
                    ServiceTone.OVERDUE -> RaahiRed
                }
                HeroStat("SERVICE", serviceText, Modifier.weight(1f), valueColor = serviceColor)
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = RaahiText) {
    Column(
        modifier
            .background(Color(0xFFF8F5EE), RaahiShapeSmall)
            .border(1.dp, RaahiBorderSoft, RaahiShapeSmall)
            .padding(horizontal = 9.dp, vertical = 8.dp)
    ) {
        Text(label, color = RaahiTextFaint, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
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
    Column(
        modifier
            .shadow(elevation = 1.dp, shape = RaahiShapeMedium, spotColor = Color(0x0C000000))
            .background(Color.White, RaahiShapeMedium)
            .border(1.dp, RaahiBorderSoft, RaahiShapeMedium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 11.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = RaahiText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
        Text(label, color = RaahiTextDim, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun PlanTripCard(
    activeTrip: `in`.raahi.app.network.TripDto?,
    onPlanTrip: () -> Unit,
    onOpenActiveTrip: (String) -> Unit,
) {
    if (activeTrip != null) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            borderColor = RaahiCyan.copy(alpha = 0.4f),
            background = Color(0xFFF0F9FF),
            onClick = { onOpenActiveTrip(activeTrip.id) }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(RaahiCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, tint = RaahiCyan, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ACTIVE TRIP", color = RaahiCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(6.dp).clip(CircleShape).background(RaahiGreen))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(activeTrip.title, color = RaahiText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = RaahiDisplayFont)
                    Text("${activeTrip.distanceKm} km · In progress", color = RaahiTextDim, fontSize = 12.sp)
                }
                Text("Resume →", color = RaahiCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            borderColor = RaahiOrange.copy(alpha = 0.25f),
            background = Color.White,
            onClick = onPlanTrip
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF0ED)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Plan a Highway Trip", color = RaahiText, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
                    Spacer(Modifier.height(2.dp))
                    Text("Fuel stops, pit stops & weather warnings", color = RaahiTextDim, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(RaahiOrange.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Plan →", color = RaahiOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(items) { (key, emoji, label) ->
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .shadow(elevation = 1.dp, shape = RaahiShapeMedium, spotColor = Color(0x0C000000))
                    .background(Color.White, RaahiShapeMedium)
                    .border(1.dp, RaahiBorderSoft, RaahiShapeMedium)
                    .clickable { onOpenDaily(key) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(emoji, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(label, color = RaahiTextDim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
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
    var gpsError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

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
        if (searchQuery.isBlank()) POPULAR_CITIES
        else POPULAR_CITIES.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.state.contains(searchQuery, ignoreCase = true)
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
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(1.dp, RaahiOrange.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
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
                            .background(RaahiOrange.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDetectingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RaahiOrange, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Navigation, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Use current location", color = RaahiText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isDetectingGps) "Acquiring GPS fix..." else (gpsError ?: "GPS auto-detect for road help & mechanics"),
                            color = if (gpsError != null) RaahiOrange else RaahiTextDim,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search city (e.g. Mohali, Zirakpur)", fontSize = 12.5.sp, color = RaahiTextFaint) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
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
                                Text(city.state, color = RaahiTextDim, fontSize = 10.5.sp)
                            }
                            if (isSelected) {
                                Text("Selected", color = RaahiOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    )
}
