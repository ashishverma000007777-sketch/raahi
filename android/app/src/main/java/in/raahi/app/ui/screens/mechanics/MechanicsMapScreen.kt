package `in`.raahi.app.ui.screens.mechanics

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.network.MechanicDto
import `in`.raahi.app.network.OsmMechanicShopDto
import `in`.raahi.app.ui.components.*
import `in`.raahi.app.ui.theme.*
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap

/**
 * Premium Google Maps-quality Mechanic Help & Discovery Map experience.
 * Features:
 * - Full-screen interactive MapLibre map with OpenStreetMap raster tiles
 * - Floating search header & category filter chips
 * - Bidirectional marker <-> list selection with animated highlight rings
 * - Smooth draggable bottom sheet with collapsed peek and expanded states
 * - Dedicated floating zoom (+/-) and GPS re-center controls
 */
@Composable
fun MechanicsMapScreen(
    onBack: () -> Unit,
    onMechanicClick: (userId: String) -> Unit,
    onNavigateTab: (RaahiTab) -> Unit,
    viewModel: MechanicsMapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val permission = rememberLocationPermissionState()
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }

    var selectedMechanic by remember { mutableStateOf<MechanicDto?>(null) }
    var selectedShop by remember { mutableStateOf<OsmMechanicShopDto?>(null) }
    var isSheetExpanded by remember { mutableStateOf(false) }

    fun selectMechanic(m: MechanicDto) {
        selectedMechanic = m
        selectedShop = null
        if (m.lat != null && m.lng != null) {
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(org.maplibre.android.geometry.LatLng(m.lat, m.lng), 15.2))
        }
    }

    fun selectShop(s: OsmMechanicShopDto) {
        selectedShop = s
        selectedMechanic = null
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(org.maplibre.android.geometry.LatLng(s.lat, s.lng), 15.2))
    }

    LaunchedEffect(permission.isGranted) {
        if (permission.isGranted) viewModel.start()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = RaahiBg) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                val loaded = state as? MechanicsMapUiState.Loaded
                val errorState = state as? MechanicsMapUiState.Error
                val defaultLoc = `in`.raahi.app.data.LatLng(28.6139, 77.2090)
                val currentLoc = loaded?.userLocation ?: defaultLoc

                val filteredMechanics = remember(loaded?.mechanics, selectedFilter) {
                    applyFilter(loaded?.mechanics.orEmpty(), selectedFilter)
                }
                val shops = loaded?.shops.orEmpty()

                // 1. Full-screen Background Map Canvas
                MechanicsMapView(
                    modifier = Modifier.fillMaxSize(),
                    userLocation = currentLoc,
                    mechanics = filteredMechanics,
                    shops = shops,
                    selectedMechanicId = selectedMechanic?.userId,
                    selectedShopId = selectedShop?.id,
                    onMechanicClick = { selectMechanic(it) },
                    onShopClick = { selectShop(it) },
                    onMapReady = { map = it },
                )

                // 2. Floating Top Header & Filter Chips
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                ) {
                    FloatingSearchHeader(
                        onBack = onBack,
                        onRefresh = viewModel::retry,
                        availableCount = loaded?.mechanics?.count { it.isAvailable } ?: 0,
                        totalCount = loaded?.mechanics?.size ?: 0,
                        errorMessage = loaded?.errorMessage ?: errorState?.message
                    )

                    val categories = remember(loaded?.mechanics) { deriveCategories(loaded?.mechanics.orEmpty()) }
                    if (categories.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        FloatingFilterChipRow(categories, selectedFilter) { selectedFilter = it }
                    }
                }

                // 3. Floating Map Controls (Zoom in, Zoom out, Recenter)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = if (selectedMechanic != null || selectedShop != null) 250.dp else 140.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Recenter FAB
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(4.dp, CircleShape, spotColor = Color(0x33000000))
                            .background(Color.White, CircleShape)
                            .border(1.dp, RaahiBorderSoft, CircleShape)
                            .clickable {
                                map?.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        org.maplibre.android.geometry.LatLng(currentLoc.lat, currentLoc.lng),
                                        14.2
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.MyLocation, contentDescription = "Recenter", tint = RaahiOrange, modifier = Modifier.size(20.dp))
                    }

                    // Zoom Controls
                    Column(
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color(0x22000000))
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, RaahiBorderSoft, RoundedCornerShape(12.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clickable { map?.animateCamera(CameraUpdateFactory.zoomIn()) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Zoom In", tint = RaahiText, modifier = Modifier.size(18.dp))
                        }
                        HorizontalDivider(color = RaahiBorderSoft, modifier = Modifier.width(38.dp))
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clickable { map?.animateCamera(CameraUpdateFactory.zoomOut()) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = "Zoom Out", tint = RaahiText, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // 4. Selected Mechanic Preview Card (Floating above bottom sheet)
                if (selectedMechanic != null || selectedShop != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 110.dp)
                    ) {
                        if (selectedMechanic != null) {
                            val m = selectedMechanic!!
                            SelectedMechanicPreviewCard(
                                mechanic = m,
                                onCall = { openPhoneDialer(context, m.phone) },
                                onDirections = {
                                    if (m.lat != null && m.lng != null) {
                                        openDirections(context, m.lat, m.lng, m.shopName ?: m.name ?: "Mechanic")
                                    }
                                },
                                onViewProfile = { onMechanicClick(m.userId) },
                                onDismiss = { selectedMechanic = null }
                            )
                        } else if (selectedShop != null) {
                            val shop = selectedShop!!
                            SelectedShopPreviewCard(
                                shop = shop,
                                onCall = { openPhoneDialer(context, shop.phone) },
                                onDirections = { openDirections(context, shop.lat, shop.lng, shop.name) },
                                onDismiss = { selectedShop = null }
                            )
                        }
                    }
                }

                // 5. Draggable Bottom Sheet with Nearby Mechanic Results
                when {
                    !permission.isGranted -> PermissionNeededCard(
                        onRequest = permission.request,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    state is MechanicsMapUiState.LocationUnavailable -> LocationUnavailableCard(
                        onRetry = viewModel::start,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    state is MechanicsMapUiState.Loading -> Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = RaahiOrange)
                    }
                    else -> DraggableMechanicsBottomSheet(
                        mechanics = filteredMechanics,
                        shops = shops,
                        selectedMechanicId = selectedMechanic?.userId,
                        selectedShopId = selectedShop?.id,
                        errorMessage = loaded?.errorMessage ?: errorState?.message,
                        isExpanded = isSheetExpanded,
                        onToggleExpand = { isSheetExpanded = !isSheetExpanded },
                        onDragExpand = { isSheetExpanded = it },
                        onRetry = viewModel::retry,
                        onMechanicClick = { selectMechanic(it) },
                        onShopClick = { selectShop(it) },
                        onCallPhone = { openPhoneDialer(context, it) },
                        onGetDirections = { lat, lng, name -> openDirections(context, lat, lng, name) },
                        onViewMechanicDetail = { onMechanicClick(it) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
            RaahiBottomNavBar(current = RaahiTab.MECHANICS, onSelect = onNavigateTab)
        }
    }
}

// ------------------------------------------------------------------ Header & Controls

@Composable
private fun FloatingSearchHeader(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    availableCount: Int,
    totalCount: Int,
    errorMessage: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color(0x26000000)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, RaahiBorderSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = RaahiText)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Find a mechanic nearby",
                    color = RaahiText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RaahiDisplayFont
                )
                if (errorMessage != null) {
                    Text("Offline mode · Tap to retry", color = RaahiAmber, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                } else if (totalCount > 0) {
                    Text("$availableCount open · $totalCount verified nearby", color = RaahiTextDim, fontSize = 11.sp)
                } else {
                    Text("Searching nearby road network…", color = RaahiTextDim, fontSize = 11.sp)
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = RaahiTextDim)
            }
        }
    }
}

@Composable
private fun FloatingFilterChipRow(categories: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { c ->
            val active = c == selected
            Box(
                modifier = Modifier
                    .shadow(3.dp, RoundedCornerShape(16.dp), spotColor = Color(0x18000000))
                    .background(if (active) RaahiOrange else Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, if (active) RaahiOrange else RaahiBorderSoft, RoundedCornerShape(16.dp))
                    .clickable { onSelect(c) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    c,
                    color = if (active) Color.White else RaahiText,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

private fun deriveCategories(mechanics: List<MechanicDto>): List<String> {
    val tokens = mechanics.flatMap { it.specializations?.split(",", "·", "/") ?: emptyList() }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(6)
    return listOf("All") + tokens
}

private fun applyFilter(mechanics: List<MechanicDto>, filter: String): List<MechanicDto> =
    if (filter == "All") mechanics else mechanics.filter { it.specializations?.contains(filter, ignoreCase = true) == true }

// ------------------------------------------------------------------ Selected Preview Cards

@Composable
private fun SelectedMechanicPreviewCard(
    mechanic: MechanicDto,
    onCall: () -> Unit,
    onDirections: () -> Unit,
    onViewProfile: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = Color(0x33000000)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, RaahiOrange)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    if (mechanic.specializations?.contains("SOS", ignoreCase = true) == true) Icons.Outlined.SupportAgent else RaahiIcons.Wrench,
                    if (mechanic.isAvailable) RaahiGreen else RaahiOrange,
                    40.dp,
                    CircleShape
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            mechanic.shopName ?: mechanic.name ?: "Verified Mechanic",
                            color = RaahiText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Outlined.Verified, contentDescription = "Verified", tint = RaahiCyan, modifier = Modifier.size(15.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Star, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(12.dp))
                        Text(" ${"%.1f".format(mechanic.ratingAvg)}", color = RaahiTextDim, fontSize = 11.5.sp)
                        if (mechanic.distanceKm >= 0) {
                            Text(" · ${"%.1f".format(mechanic.distanceKm)} km away", color = RaahiOrange, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = RaahiTextDim, modifier = Modifier.size(16.dp))
                }
            }

            if (!mechanic.specializations.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(mechanic.specializations, color = RaahiTextDim, fontSize = 11.5.sp, maxLines = 1)
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!mechanic.phone.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onCall,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RaahiGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RaahiGreen.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Call", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                OutlinedButton(
                    onClick = onDirections,
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RaahiOrange),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RaahiOrange.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Outlined.Directions, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Directions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onViewProfile,
                    modifier = Modifier.weight(1.2f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange)
                ) {
                    Text("Details", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun SelectedShopPreviewCard(
    shop: OsmMechanicShopDto,
    onCall: () -> Unit,
    onDirections: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = Color(0x33000000)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, RaahiAmber)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(RaahiIcons.Wrench, RaahiAmber, 40.dp, CircleShape)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(shop.name, color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, maxLines = 1)
                    Text("${"%.1f".format(shop.distanceKm)} km away · OSM repair shop", color = RaahiTextDim, fontSize = 11.5.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = RaahiTextDim, modifier = Modifier.size(16.dp))
                }
            }
            if (!shop.address.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(shop.address, color = RaahiTextDim, fontSize = 11.5.sp, maxLines = 2)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!shop.phone.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onCall,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RaahiGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RaahiGreen.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Call", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Button(
                    onClick = onDirections,
                    modifier = Modifier.weight(1.2f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange)
                ) {
                    Icon(Icons.Outlined.Directions, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Directions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Draggable Bottom Sheet

@Composable
private fun DraggableMechanicsBottomSheet(
    mechanics: List<MechanicDto>,
    shops: List<OsmMechanicShopDto>,
    selectedMechanicId: String?,
    selectedShopId: String?,
    errorMessage: String?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDragExpand: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onMechanicClick: (MechanicDto) -> Unit,
    onShopClick: (OsmMechanicShopDto) -> Unit,
    onCallPhone: (String?) -> Unit,
    onGetDirections: (Double, Double, String) -> Unit,
    onViewMechanicDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), spotColor = Color(0x33000000))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -15) onDragExpand(true)
                    else if (dragAmount > 15) onDragExpand(false)
                }
            },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            // Drag Handle & Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(Modifier.width(38.dp).height(4.dp).background(RaahiBorder, RaahiShapePill))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val title = when {
                    errorMessage != null -> "Nearby Mechanics"
                    mechanics.isEmpty() && shops.isEmpty() -> "No mechanics found nearby"
                    mechanics.isEmpty() -> "${shops.size} repair shops nearby"
                    else -> "${mechanics.size} mechanics nearby"
                }
                Text(
                    title,
                    color = RaahiText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    modifier = Modifier.weight(1f)
                )

                val openCount = mechanics.count { it.isAvailable }
                if (mechanics.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(RaahiGreen.copy(alpha = 0.12f), RaahiShapePill)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("$openCount open", color = RaahiGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                }

                IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Toggle List",
                        tint = RaahiTextDim
                    )
                }
            }

            // Expanded List View
            if (isExpanded) {
                when {
                    errorMessage != null -> UnavailableMechanicsState(errorMessage, onRetry = onRetry, modifier = Modifier.height(150.dp))
                    mechanics.isEmpty() && shops.isEmpty() -> EmptyMechanicsState(Modifier.height(130.dp))
                    else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (mechanics.isNotEmpty()) {
                            item {
                                Text("Verified Raahi mechanics", color = RaahiTextDim, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                            }
                            items(mechanics, key = { "verified-${it.userId}" }) { m ->
                                val isSelected = m.userId == selectedMechanicId
                                MechanicRow(
                                    m = m,
                                    isSelected = isSelected,
                                    onClick = { onMechanicClick(m) },
                                    onCall = { onCallPhone(m.phone) },
                                    onDirections = {
                                        if (m.lat != null && m.lng != null) {
                                            onGetDirections(m.lat, m.lng, m.shopName ?: m.name ?: "Mechanic")
                                        }
                                    },
                                    onViewDetail = { onViewMechanicDetail(m.userId) }
                                )
                            }
                        }

                        if (shops.isNotEmpty()) {
                            item {
                                Text("Nearby repair shops · OSM", color = RaahiAmber, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
                            }
                            items(shops, key = { "osm-${it.id}" }) { shop ->
                                val isSelected = shop.id == selectedShopId
                                RowCard(
                                    onClick = { onShopClick(shop) },
                                    modifier = if (isSelected) Modifier.border(1.5.dp, RaahiAmber, RoundedCornerShape(12.dp)) else Modifier
                                ) {
                                    IconBadge(RaahiIcons.Wrench, RaahiAmber, 36.dp, CircleShape)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(shop.name, color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        if (!shop.address.isNullOrBlank()) {
                                            Text(shop.address, color = RaahiTextDim, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                    Text("${"%.1f".format(shop.distanceKm)} km", color = RaahiTextDim, fontSize = 11.sp)
                                    IconButton(onClick = { onGetDirections(shop.lat, shop.lng, shop.name) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Outlined.Directions, contentDescription = "Directions", tint = RaahiOrange, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MechanicRow(
    m: MechanicDto,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onCall: () -> Unit = {},
    onDirections: () -> Unit = {},
    onViewDetail: () -> Unit = {}
) {
    val isSos = m.specializations?.contains("SOS", ignoreCase = true) == true
    Column {
        RowCard(
            onClick = onClick,
            modifier = if (isSelected) Modifier.border(1.5.dp, RaahiOrange, RoundedCornerShape(12.dp)) else Modifier
        ) {
            IconBadge(
                if (isSos) Icons.Outlined.SupportAgent else RaahiIcons.Wrench,
                if (m.isAvailable) RaahiGreen else RaahiRed,
                38.dp,
                CircleShape
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.shopName ?: m.name ?: "Mechanic", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Outlined.Verified, contentDescription = "Verified", tint = RaahiCyan, modifier = Modifier.size(13.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Star, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(11.dp))
                    Text(" ${"%.1f".format(m.ratingAvg)}", color = RaahiTextDim, fontSize = 11.sp)
                    if (!m.specializations.isNullOrBlank()) {
                        Text(" · ${m.specializations}", color = RaahiTextDim, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (m.distanceKm >= 0) {
                    Text("${"%.1f".format(m.distanceKm)} km", color = RaahiOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = RaahiDisplayFont)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(if (m.isAvailable) RaahiGreen else RaahiRed, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (m.isAvailable) "Open" else "Closed",
                        color = if (m.isAvailable) RaahiGreen else RaahiRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        if (isSelected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!m.phone.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onCall,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RaahiGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RaahiGreen.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Call", fontSize = 11.sp)
                    }
                }
                OutlinedButton(
                    onClick = onDirections,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RaahiOrange),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RaahiOrange.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Outlined.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Directions", fontSize = 11.sp)
                }
                Button(
                    onClick = onViewDetail,
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange)
                ) {
                    Text("Details", fontSize = 11.sp)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ States

@Composable
private fun UnavailableMechanicsState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text("Mechanics currently unavailable", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(3.dp))
            Text(message, color = RaahiTextDim, fontSize = 11.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Retry", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptyMechanicsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(6.dp))
        Text("No verified mechanics nearby yet", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text("Try expanding your search area or clear category filters.", color = RaahiTextDim, fontSize = 11.sp)
    }
}

@Composable
private fun PermissionNeededCard(onRequest: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.LocationOff, contentDescription = null, tint = RaahiRed, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(10.dp))
            Text("Location permission required", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text("Allow location access to discover nearby mechanics on the map.", color = RaahiTextDim, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            RaahiPrimaryButton("Allow location", onClick = onRequest)
        }
    }
}

@Composable
private fun LocationUnavailableCard(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.LocationOff, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(10.dp))
            Text("GPS Location unavailable", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text("Could not get your GPS position. Check if device location is turned on.", color = RaahiTextDim, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            RaahiPrimaryButton("Retry GPS", onClick = onRetry)
        }
    }
}

// ------------------------------------------------------------------ External Intent Helpers

private fun openPhoneDialer(context: Context, phone: String?) {
    if (phone.isNullOrBlank()) return
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}"))
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun openDirections(context: Context, lat: Double, lng: Double, label: String) {
    val encodedLabel = Uri.encode(label)
    val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
        } catch (_: Exception) {
            val osmUri = Uri.parse("https://www.openstreetmap.org/?mlat=$lat&mlon=$lng#map=17/$lat/$lng")
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, osmUri))
            } catch (_: Exception) {}
        }
    }
}
