package `in`.raahi.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.network.CarHealthDto
import `in`.raahi.app.network.MaintenanceItemDto
import `in`.raahi.app.network.UserDto
import `in`.raahi.app.network.VehicleDto
import `in`.raahi.app.ui.components.*
import `in`.raahi.app.ui.screens.vehicle.CarHealthUiState
import `in`.raahi.app.ui.screens.vehicle.CarHealthViewModel
import `in`.raahi.app.ui.theme.*

private enum class ProfileSubTab { CAR_HEALTH, PROFILE }

/** Profile / Car Health, restyled to the concept redesign — glass cards, the shared
 * ScoreRing component, a sliding-underline tab bar. Real data plumbing (ProfileViewModel /
 * CarHealthViewModel) is unchanged from the previous pass. */
@Composable
fun ProfileScreen(
    onBack: () -> Unit, onEditProfile: () -> Unit, onSignedOut: () -> Unit, onHelper: () -> Unit = {},
    onSetupVehicle: () -> Unit, onEditVehicle: () -> Unit, onServiceHistory: () -> Unit,
    onNavigateTab: (RaahiTab) -> Unit,
    onMyGarage: () -> Unit = onEditVehicle,
    onNotifications: () -> Unit = {},
    onPayments: () -> Unit = {},
    onEarnWithRaahi: () -> Unit = onHelper,
    viewModel: ProfileViewModel = hiltViewModel(),
    carHealthViewModel: CarHealthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val signedOut by viewModel.signedOut.collectAsState()
    val carHealthState by carHealthViewModel.state.collectAsState()
    val odometerError by carHealthViewModel.odometerUpdateError.collectAsState()
    var tab by remember { mutableStateOf(ProfileSubTab.PROFILE) }
    val scrollState = rememberScrollState()

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.load()
                carHealthViewModel.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(signedOut) { if (signedOut) onSignedOut() }

    Surface(modifier = Modifier.fillMaxSize(), color = RaahiBg) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (val s = state) {
                    is ProfileUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RaahiOrange) }
                    is ProfileUiState.Error -> Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                        OfflineStatusBanner(
                            message = s.message,
                            onRetry = viewModel::load,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        ProfileHeader(onBack, null, onEditProfile)
                        StatsRow(null)
                        SubTabBar(tab) { tab = it }
                        Spacer(Modifier.height(16.dp))
                        when (tab) {
                            ProfileSubTab.CAR_HEALTH -> CarHealthTab(
                                state = carHealthState, onSetupVehicle = onSetupVehicle, onEditVehicle = onEditVehicle,
                                onServiceHistory = onServiceHistory, onUpdateOdometer = carHealthViewModel::updateOdometer,
                                odometerError = odometerError, onClearOdometerError = carHealthViewModel::clearOdometerError,
                                onRetry = carHealthViewModel::load
                            )
                            ProfileSubTab.PROFILE -> ProfileTab(
                                user = null,
                                vehicle = (carHealthState as? CarHealthUiState.Loaded)?.vehicle,
                                onEditProfile = onEditProfile,
                                onEditVehicle = onEditVehicle,
                                onMyGarage = onMyGarage,
                                onServiceHistory = onServiceHistory,
                                onSignOut = viewModel::signOut,
                                onHelper = onHelper,
                                onEarnWithRaahi = onEarnWithRaahi,
                                onNotifications = onNotifications,
                                onPayments = onPayments,
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                    is ProfileUiState.Loaded -> Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                        if (s.errorMessage != null) {
                            OfflineStatusBanner(
                                message = s.errorMessage,
                                onRetry = viewModel::load,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        ProfileHeader(onBack, s.user, onEditProfile)
                        StatsRow(s.user)
                        SubTabBar(tab) { tab = it }
                        Spacer(Modifier.height(16.dp))
                        when (tab) {
                            ProfileSubTab.CAR_HEALTH -> CarHealthTab(
                                state = carHealthState, onSetupVehicle = onSetupVehicle, onEditVehicle = onEditVehicle,
                                onServiceHistory = onServiceHistory, onUpdateOdometer = carHealthViewModel::updateOdometer,
                                odometerError = odometerError, onClearOdometerError = carHealthViewModel::clearOdometerError,
                                onRetry = carHealthViewModel::load
                            )
                            ProfileSubTab.PROFILE -> ProfileTab(
                                user = s.user,
                                vehicle = (carHealthState as? CarHealthUiState.Loaded)?.vehicle,
                                onEditProfile = onEditProfile,
                                onEditVehicle = onEditVehicle,
                                onMyGarage = onMyGarage,
                                onServiceHistory = onServiceHistory,
                                onSignOut = viewModel::signOut,
                                onHelper = onHelper,
                                onEarnWithRaahi = onEarnWithRaahi,
                                onNotifications = onNotifications,
                                onPayments = onPayments,
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
            RaahiBottomNavBar(current = RaahiTab.PROFILE, onSelect = onNavigateTab)
        }
    }
}

@Composable
private fun ProfileHeader(onBack: () -> Unit, user: UserDto?, onEdit: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = RaahiText) }
            Text("My Profile", color = RaahiText, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont, modifier = Modifier.weight(1f))
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Edit profile", tint = RaahiTextDim, modifier = Modifier.size(19.dp)) }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp), spotColor = Color(0x0C000000))
                .clickable(onClick = onEdit),
            shape = RoundedCornerShape(18.dp),
            color = RaahiGlass,
            border = androidx.compose.foundation.BorderStroke(1.dp, RaahiBorderSoft)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Box(modifier = Modifier.size(54.dp).background(RaahiOrange, CircleShape), contentAlignment = Alignment.Center) {
                    val initial = user?.name?.trim()?.takeIf { it.isNotBlank() }?.firstOrNull()?.uppercaseChar()?.toString() ?: "R"
                    Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, fontFamily = RaahiDisplayFont)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user?.name?.trim()?.takeIf { it.isNotBlank() } ?: "Raahi User",
                            color = RaahiText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            fontFamily = RaahiDisplayFont
                        )
                        if (user?.isVerified == true) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFF388AF6),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = user?.phone?.takeIf { it.isNotBlank() } ?: "--",
                        color = RaahiTextDim,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = user?.role?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "--",
                        color = RaahiOrange,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StatsRow(user: UserDto?) {
    val totalHelpsText = user?.totalHelps?.toString() ?: "--"
    val ratingText = if (user != null && user.ratingAvg > 0.0) "%.1f".format(user.ratingAvg) else "--"
    val roleText = user?.role?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "--"
    val statusText = if (user?.isVerified == true) "Verified" else "Pending"
    val statusColor = if (user?.isVerified == true) RaahiGreen else RaahiOrange

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatItem(totalHelpsText, "Helps", RaahiGreen, Modifier.weight(1f))
        StatItem(ratingText, "Rating", RaahiAmber, Modifier.weight(1f))
        StatItem(roleText, "Role", Color(0xFF3B82F6), Modifier.weight(1f))
        StatItem(statusText, "Status", statusColor, Modifier.weight(1f))
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(RaahiGlass, RoundedCornerShape(15.dp))
            .border(1.dp, RaahiBorderSoft, RoundedCornerShape(15.dp))
            .padding(horizontal = 3.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = RaahiDisplayFont, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(label, color = RaahiTextDim, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun SubTabBar(current: ProfileSubTab, onSelect: (ProfileSubTab) -> Unit) {
    var healthPos by remember { mutableStateOf(0f to 0f) }
    var acctPos by remember { mutableStateOf(0f to 0f) }
    val target = if (current == ProfileSubTab.CAR_HEALTH) healthPos else acctPos
    val left by androidx.compose.animation.core.animateFloatAsState(target.first, label = "tabLeft")
    val width by androidx.compose.animation.core.animateFloatAsState(target.second, label = "tabWidth")

    Column {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                SubTabItem("CAR HEALTH", current == ProfileSubTab.CAR_HEALTH, Modifier.weight(1f).reportPosition { x, w -> healthPos = x to w }) { onSelect(ProfileSubTab.CAR_HEALTH) }
                SubTabItem("PROFILE", current == ProfileSubTab.PROFILE, Modifier.weight(1f).reportPosition { x, w -> acctPos = x to w }) { onSelect(ProfileSubTab.PROFILE) }
            }
            Box(
                Modifier.align(Alignment.BottomStart)
                    .offset(x = with(androidx.compose.ui.platform.LocalDensity.current) { left.toDp() })
                    .width(with(androidx.compose.ui.platform.LocalDensity.current) { width.toDp() })
                    .height(2.dp)
                    .background(RaahiBrandGradient)
            )
        }
        HorizontalDivider(color = RaahiBorderSoft, thickness = 1.dp)
    }
}

private fun Modifier.reportPosition(onPlaced: (Float, Float) -> Unit): Modifier = this.onGloballyPositioned { coords ->
    onPlaced(coords.positionInParent().x, coords.size.width.toFloat())
}

@Composable
private fun SubTabItem(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = if (active) RaahiOrange else RaahiTextDim, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, letterSpacing = 0.4.sp)
    }
}

@Composable
private fun CarHealthTab(
    state: CarHealthUiState, onSetupVehicle: () -> Unit, onEditVehicle: () -> Unit, onServiceHistory: () -> Unit,
    onUpdateOdometer: (Int, () -> Unit) -> Unit, odometerError: String?, onClearOdometerError: () -> Unit,
    onRetry: () -> Unit = {},
) {
    var showOdometerDialog by remember { mutableStateOf(false) }

    when (state) {
        is CarHealthUiState.Loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RaahiOrange) }
        is CarHealthUiState.Error -> {
            Column(Modifier.padding(vertical = 4.dp)) {
                OfflineStatusBanner(
                    message = state.message,
                    onRetry = onRetry,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                Spacer(Modifier.height(12.dp))
                Box(Modifier.padding(horizontal = 16.dp)) { SectionLabel("My Car") }
                NoVehicleCard(onSetupVehicle)
            }
        }
        is CarHealthUiState.Loaded -> {
            val vehicle = state.vehicle
            if (vehicle == null) {
                Box(Modifier.padding(horizontal = 16.dp)) { SectionLabel("My Car") }
                NoVehicleCard(onSetupVehicle)
                return
            }
            Box(Modifier.padding(horizontal = 16.dp)) { SectionLabel("My Car") }
            MyCarCard(vehicle, onEdit = onEditVehicle, onUpdateOdometer = { showOdometerDialog = true })
            Spacer(Modifier.height(16.dp))
            CarHealthScoreCard(state.health)
            Spacer(Modifier.height(22.dp))
            Box(Modifier.padding(horizontal = 16.dp)) { SectionLabel("Quick Diagnostics", trailing = "History", onTrailingClick = onServiceHistory) }
            val items = state.health?.maintenanceItems.orEmpty()
            if (items.isEmpty()) Box(Modifier.padding(horizontal = 16.dp)) { InfoCard("Add service/insurance/PUC details (Edit vehicle) to see diagnostics here.") }
            else MaintenanceGrid(items)
            Spacer(Modifier.height(22.dp))
            Box(Modifier.padding(horizontal = 16.dp)) { SectionLabel("Upcoming Maintenance") }
            if (items.isEmpty()) Box(Modifier.padding(horizontal = 16.dp)) { InfoCard("No maintenance reminders yet.") }
            else MaintenanceList(items)
        }
    }

    if (showOdometerDialog) {
        OdometerUpdateDialog(odometerError, onDismiss = { showOdometerDialog = false; onClearOdometerError() }, onSave = { km -> onUpdateOdometer(km) { showOdometerDialog = false } })
    }
}

@Composable
private fun NoVehicleCard(onSetupVehicle: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = onSetupVehicle) {
        Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(Icons.Outlined.DirectionsCar, RaahiOrange, 44.dp, RaahiShapeMedium)
            Spacer(Modifier.height(10.dp))
            Text("Set up your car", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text("Add your real vehicle details to unlock Car Health, maintenance reminders and service history", color = RaahiTextDim, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onSetupVehicle, colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange), shape = RaahiShapeMedium) { Text("Set up now", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun MyCarCard(vehicle: VehicleDto, onEdit: () -> Unit, onUpdateOdometer: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(listOfNotNull(vehicle.brand, vehicle.model, vehicle.variant).joinToString(" "), color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, fontFamily = RaahiDisplayFont)
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${vehicle.modelYear}", color = RaahiTextDim, fontSize = 11.5.sp)
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.background(RaahiCyan.copy(alpha = 0.16f), RaahiShapeSmall).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(vehicle.fuelType.lowercase().replaceFirstChar { it.uppercase() }, color = RaahiCyan, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(vehicle.registrationNumber, color = RaahiTextDim, fontSize = 11.5.sp)
                }
                IconBadge(Icons.Outlined.DirectionsCar, RaahiOrange, 52.dp, RaahiShapeMedium)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = RaahiBorderSoft)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Speed, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("${vehicle.odometerKm} km", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, fontFamily = RaahiDisplayFont)
                Spacer(Modifier.weight(1f))
                Text("Update odometer", color = RaahiOrange, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onUpdateOdometer))
            }
            Spacer(Modifier.height(10.dp))
            Text("Edit vehicle details", color = RaahiCyan, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onEdit))
        }
    }
}

@Composable
private fun CarHealthScoreCard(health: CarHealthDto?) {
    val score = health?.score
    if (score == null) { InfoCard(health?.message ?: "Complete your vehicle information to calculate your Car Health", title = "Gaadi ka Report Card"); return }
    val color = when { score >= 70 -> RaahiGreen; score >= 40 -> RaahiAmber; else -> RaahiRed }
    val grade = health.grade ?: "-"
    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), background = RaahiGlass, borderColor = RaahiBorderSoft) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(score, ringSize = 60.dp, big = true)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Gaadi ka Report Card", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, fontFamily = RaahiDisplayFont)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.background(color.copy(alpha = 0.16f), RaahiShapePill).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(if (score >= 70) "Good Condition" else if (score >= 40) "Needs Attention" else "Poor Condition", color = color, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.background(color, RaahiShapeSmall).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("Grade $grade", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (health.weeklyTrendText != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(health.weeklyTrendText, color = RaahiCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = RaahiBorderSoft)
            Spacer(Modifier.height(10.dp))

            // Sub-scores row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ReportCardMiniSubScore("Care", health.careScore?.let { "$it/100" } ?: "--", health.careGrade ?: "-", RaahiGreen)
                ReportCardMiniSubScore("Efficiency", health.efficiencyScore?.let { "$it/100" } ?: "--", health.efficiencyGrade ?: "-", RaahiCyan)
                ReportCardMiniSubScore("Safety", health.safetyScore?.let { "$it/100" } ?: "Pending", health.safetyGrade ?: "-", if (health.safetyScore != null) RaahiVioletAccent else RaahiTextDim)
            }
        }
    }
}

@Composable
private fun ReportCardMiniSubScore(label: String, score: String, grade: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = RaahiTextDim, fontSize = 10.5.sp)
        Spacer(Modifier.height(2.dp))
        Text(score, color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("Grade $grade", color = color, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MaintenanceGrid(items: List<MaintenanceItemDto>) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { item -> Box(Modifier.weight(1f)) { DiagnosticTile(item) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DiagnosticTile(item: MaintenanceItemDto) {
    val color = when (item.status) { "OVERDUE" -> RaahiRed; "DUE_SOON" -> RaahiAmber; else -> RaahiGreen }
    Column(Modifier.fillMaxWidth().background(color.copy(alpha = 0.10f), RaahiShapeMedium).padding(12.dp)) {
        Text(item.type, color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(when (item.status) { "OVERDUE" -> "Overdue"; "DUE_SOON" -> "Due soon"; else -> "OK" }, color = color, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
    }
}

@Composable
private fun MaintenanceList(items: List<MaintenanceItemDto>) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            val color = when (item.status) { "OVERDUE" -> RaahiRed; "DUE_SOON" -> RaahiAmber; else -> RaahiGreen }
            RowCard(urgent = item.status == "OVERDUE") {
                IconBadge(Icons.Outlined.Info, color, 36.dp)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.type, color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    Text(item.detail, color = if (item.status == "OVERDUE") color else RaahiTextDim, fontSize = 10.5.sp)
                }
                if (item.status == "OVERDUE") {
                    Box(Modifier.background(color.copy(alpha = 0.18f), RaahiShapePill).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("URGENT", color = color, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun OdometerUpdateDialog(error: String?, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var value by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.background(RaahiBg2, RaahiShapeLarge).border(1.dp, RaahiBorder, RaahiShapeLarge).padding(20.dp)) {
            Text("Update odometer", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, fontFamily = RaahiDisplayFont)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = value, onValueChange = { value = it.filter { c -> c.isDigit() } },
                placeholder = { Text("Current odometer (km)", color = RaahiTextDim) }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), shape = RaahiShapeSmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = RaahiText, unfocusedTextColor = RaahiText,
                    focusedContainerColor = RaahiGlassStrong, unfocusedContainerColor = RaahiGlassStrong,
                    focusedBorderColor = RaahiOrange, unfocusedBorderColor = RaahiBorder, cursorColor = RaahiOrange,
                ),
            )
            if (error != null) { Spacer(Modifier.height(8.dp)); Text(error, color = RaahiRed, fontSize = 12.sp) }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel", color = RaahiTextDim) }
                Button(onClick = { value.toIntOrNull()?.let(onSave) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange)) { Text("Save") }
            }
        }
    }
}

@Composable
private fun InfoCard(message: String, title: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = if (title != null) 16.dp else 0.dp).background(RaahiGlassStrong, RaahiShapeLarge).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            if (title != null) Text(title, color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, fontFamily = RaahiDisplayFont)
            Text(message, color = RaahiTextDim, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ProfileTab(
    user: UserDto?,
    vehicle: VehicleDto?,
    onEditProfile: () -> Unit,
    onEditVehicle: () -> Unit,
    onMyGarage: () -> Unit = onEditVehicle,
    onServiceHistory: () -> Unit,
    onSignOut: () -> Unit,
    onHelper: () -> Unit,
    onEarnWithRaahi: () -> Unit,
    onNotifications: () -> Unit,
    onPayments: () -> Unit,
) {
    val selectedTheme by ThemeManager.themeFlow.collectAsState()

    Column(Modifier.fillMaxWidth()) {
        // 1. MY VEHICLE
        Box(Modifier.padding(horizontal = 16.dp)) { SectionLabel("MY VEHICLE") }
        if (vehicle != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x0C000000))
                    .clickable(onClick = onMyGarage),
                shape = RoundedCornerShape(16.dp),
                color = RaahiGlass,
                border = androidx.compose.foundation.BorderStroke(1.dp, RaahiBorderSoft)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(RaahiOrange.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DirectionsCar,
                                contentDescription = null,
                                tint = RaahiOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            val vehicleName = "${vehicle.brand} ${vehicle.model}${if (!vehicle.variant.isNullOrBlank()) " ${vehicle.variant}" else ""}".trim()
                            val vehicleSub = "${vehicle.registrationNumber} • ${vehicle.odometerKm} km"

                            Text(
                                text = vehicleName,
                                color = RaahiText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                fontFamily = RaahiDisplayFont
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = vehicleSub,
                                color = RaahiTextDim,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(RaahiBorderSoft))
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Manage garage & vehicle records",
                            color = RaahiTextDim,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Manage",
                            tint = RaahiTextDim,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            NoVehicleCard(onSetupVehicle = onEditVehicle)
        }

        Spacer(Modifier.height(18.dp))

        // 2. ACCOUNT & GARAGE
        Box(Modifier.padding(horizontal = 16.dp)) { SectionLabel("ACCOUNT & GARAGE") }
        ProfileMenuItem(Icons.Outlined.DirectionsCar, "My Garage", onClick = onMyGarage)
        Spacer(Modifier.height(8.dp))
        ProfileMenuItem(Icons.Outlined.Person, "Edit Profile", onClick = onEditProfile)
        Spacer(Modifier.height(8.dp))
        ProfileMenuItem(Icons.Outlined.Build, "Maintenance & Service History", onClick = onServiceHistory)

        Spacer(Modifier.height(18.dp))

        // 3. EARNINGS & PARTNER
        Box(Modifier.padding(horizontal = 16.dp)) { SectionLabel("EARNINGS & PARTNER") }
        val isHelperOrMechanic = user?.role == "HELPER" || user?.role == "MECHANIC"
        ProfileMenuItem(
            Icons.Outlined.Handshake,
            if (isHelperOrMechanic) "Helper Dashboard" else "Earn with Raahi",
            onClick = if (isHelperOrMechanic) onHelper else onEarnWithRaahi,
        )

        Spacer(Modifier.height(18.dp))

        // 4. FINANCES & PLANS
        Box(Modifier.padding(horizontal = 16.dp)) { SectionLabel("FINANCES & PLANS") }
        ProfileMenuItem(Icons.Outlined.AccountBalanceWallet, "Payments & Wallet", onClick = onPayments)

        Spacer(Modifier.height(18.dp))

        // 5. APP & SUPPORT
        Box(Modifier.padding(horizontal = 16.dp)) { SectionLabel("APP & SUPPORT") }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(2.dp, RaahiShapeMedium, spotColor = Color(0x0C000000)),
            shape = RaahiShapeMedium,
            color = RaahiGlass,
            border = androidx.compose.foundation.BorderStroke(1.dp, RaahiBorderSoft)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(Icons.Outlined.Palette, RaahiOrange, 34.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    "App Theme",
                    color = RaahiText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    modifier = Modifier.weight(1f)
                )
                val isDark = RaahiIsDark
                Surface(
                    color = if (isDark) Color(0xFF131A26) else Color(0xFFEFF2F7),
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF222D3E) else Color(0xFFE2E8F0)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple("System", "System", Icons.Outlined.BrightnessAuto),
                            Triple("Dark", "Dark", Icons.Outlined.DarkMode),
                            Triple("Light", "Light", Icons.Outlined.LightMode)
                        ).forEach { (id, label, icon) ->
                            val active = selectedTheme == id
                            val bgAnim by animateColorAsState(
                                targetValue = if (active) RaahiOrange else Color.Transparent,
                                animationSpec = tween(200),
                                label = "themeBg"
                            )
                            val fgAnim by animateColorAsState(
                                targetValue = if (active) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)),
                                animationSpec = tween(200),
                                label = "themeFg"
                            )

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .then(
                                        if (active) Modifier.shadow(2.dp, RoundedCornerShape(18.dp), spotColor = RaahiOrange.copy(alpha = 0.45f))
                                        else Modifier
                                    )
                                    .background(bgAnim)
                                    .clickable { ThemeManager.setTheme(id) }
                                    .padding(horizontal = 8.dp, vertical = 5.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = fgAnim,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(3.5.dp))
                                Text(
                                    text = label,
                                    color = fgAnim,
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Log out button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(1.dp, RoundedCornerShape(14.dp), spotColor = Color(0x10000000))
                .clickable(onClick = onSignOut),
            shape = RoundedCornerShape(14.dp),
            color = if (RaahiIsDark) Color(0xFF2E1717) else Color(0xFFFEF2F2),
            border = androidx.compose.foundation.BorderStroke(1.dp, RaahiRed.copy(alpha = if (RaahiIsDark) 0.5f else 0.35f))
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = RaahiRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Log out of Raahi", color = RaahiRed, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "Raahi Driver v2.1.0 • Build 24",
            color = RaahiTextFaint,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: (() -> Unit)? = null) {
    RowCard(modifier = Modifier.padding(horizontal = 16.dp), onClick = onClick) {
        IconBadge(icon, RaahiOrange, 34.dp)
        Spacer(Modifier.width(12.dp))
        Text(title, color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = RaahiTextFaint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    RowCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        IconBadge(icon, RaahiTextDim, 34.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = RaahiTextFaint, fontSize = 10.5.sp)
            Text(value, color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
        }
    }
}
