package `in`.raahi.app.ui.screens.mechanics

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.ui.components.*
import `in`.raahi.app.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap

private val CATEGORIES = listOf("All", "Garage", "Puncture", "Electric", "SOS / Towing")

@Composable
fun MechanicsMapScreen(
    onBack: () -> Unit,
    onMechanicClick: (userId: String) -> Unit,
    onNavigateTab: (RaahiTab) -> Unit,
    viewModel: MechanicsMapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val permission = rememberLocationPermissionState()
    var mapInstance by remember { mutableStateOf<GoogleMap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(permission.isGranted) {
        if (permission.isGranted) {
            viewModel.start()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = RaahiBg) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when {
                    !permission.isGranted -> PermissionNeededScreen(onAllow = permission.request)
                    state is MechanicsMapUiState.LocationUnavailable -> LocationUnavailableScreen(onRetry = viewModel::start)
                    state is MechanicsMapUiState.Loading -> LoadingMechanicsScreen()
                    state is MechanicsMapUiState.Error -> {
                        val errMsg = (state as MechanicsMapUiState.Error).message
                        ErrorMechanicsScreen(message = errMsg, onRetry = viewModel::retry)
                    }
                    state is MechanicsMapUiState.Loaded -> {
                        val loaded = state as MechanicsMapUiState.Loaded
                        MechanicsLoadedContent(
                            state = loaded,
                            viewModel = viewModel,
                            onBack = onBack,
                            onMapReady = { mapInstance = it },
                            onMechanicClick = onMechanicClick,
                            onZoomIn = { mapInstance?.animateCamera(CameraUpdateFactory.zoomIn()) },
                            onZoomOut = { mapInstance?.animateCamera(CameraUpdateFactory.zoomOut()) },
                            context = context,
                        )
                    }
                }
            }
            RaahiBottomNavBar(current = RaahiTab.MECHANICS, onSelect = onNavigateTab)
        }
    }
}

@Composable
private fun MechanicsLoadedContent(
    state: MechanicsMapUiState.Loaded,
    viewModel: MechanicsMapViewModel,
    onBack: () -> Unit,
    onMapReady: (GoogleMap) -> Unit,
    onMechanicClick: (String) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    context: Context,
) {
    Column(Modifier.fillMaxSize()) {
        // Top search & control header
        MechanicsHeader(
            searchQuery = state.searchQuery,
            onSearchChange = viewModel::setSearchQuery,
            viewMode = state.viewMode,
            onToggleViewMode = {
                val next = if (state.viewMode == MechanicsViewMode.MAP) MechanicsViewMode.LIST else MechanicsViewMode.MAP
                viewModel.setViewMode(next)
            },
            onBack = onBack,
            onRefresh = viewModel::retry,
            isRefreshing = state.isRefreshing,
        )

        // Category filter chips
        FilterCategoryRow(
            categories = CATEGORIES,
            selected = state.selectedCategory,
            onSelect = viewModel::selectCategory,
        )

        // Source filter chips
        SourceFilterRow(
            current = state.sourceFilter,
            onSelect = viewModel::selectSourceFilter,
            verifiedCount = state.allItems.count { it.source == MechanicSource.VERIFIED },
            googleCount = state.allItems.count { it.source == MechanicSource.GOOGLE_PLACES },
        )

        // Notice banner when Google Places API is restricted in Google Cloud
        if (state.googlePlacesNotice != null && state.sourceFilter == MechanicSourceFilter.GOOGLE_PLACES) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFEF3C7))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(
                    state.googlePlacesNotice,
                    color = Color(0xFF92400E),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        }

        if (state.viewMode == MechanicsViewMode.LIST) {
            // Pure List View
            MechanicsListView(
                items = state.filteredItems,
                onItemClick = { item ->
                    if (item.source == MechanicSource.VERIFIED && item.rawMechanic != null) {
                        onMechanicClick(item.rawMechanic.userId)
                    } else {
                        viewModel.selectItem(item)
                    }
                },
                onCall = { phone -> launchDialer(context, phone) },
                onDirections = { lat, lng -> launchDirections(context, lat, lng) },
            )
        } else {
            // Interactive Map View with Bottom Sheet
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                MechanicsMapView(
                    modifier = Modifier.fillMaxSize(),
                    userLocation = state.userLocation,
                    mechanics = state.filteredItems,
                    selectedId = state.selectedItem?.id,
                    cameraTarget = state.cameraTarget,
                    cameraZoom = state.cameraZoom,
                    onSelect = { id ->
                        val item = state.filteredItems.firstOrNull { it.id == id }
                        viewModel.selectItem(item)
                    },
                    onMapReady = onMapReady,
                )

                // Map controls overlay (Zoom + Recenter)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MapRoundButton(icon = Icons.Outlined.Add, onClick = onZoomIn)
                    MapRoundButton(icon = Icons.Outlined.Remove, onClick = onZoomOut)
                    RecenterFloatingButton(
                        isRecentering = state.isRecentering,
                        onClick = viewModel::recenter,
                    )
                }

                // Interactive Bottom Sheet with Place Cards
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    MechanicsBottomSheetCard(
                        items = state.filteredItems,
                        selectedItem = state.selectedItem,
                        onItemClick = { item ->
                            if (item.source == MechanicSource.VERIFIED && item.rawMechanic != null) {
                                onMechanicClick(item.rawMechanic.userId)
                            } else {
                                viewModel.selectItem(item)
                            }
                        },
                        onCall = { phone -> launchDialer(context, phone) },
                        onDirections = { lat, lng -> launchDirections(context, lat, lng) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MechanicsHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    viewMode: MechanicsViewMode,
    onToggleViewMode: () -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = RaahiText,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        "Mechanics & Garages",
                        color = RaahiText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RaahiDisplayFont,
                    )
                    Text(
                        "Real-time roadside assistance",
                        color = RaahiTextDim,
                        fontSize = 10.5.sp,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // View mode toggle (Map vs List)
                IconButton(onClick = onToggleViewMode, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (viewMode == MechanicsViewMode.MAP) Icons.Outlined.List else Icons.Outlined.Map,
                        contentDescription = "Toggle view",
                        tint = RaahiOrange,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp), enabled = !isRefreshing) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = RaahiOrange)
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = RaahiTextDim, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(RaahiBg)
                .border(1.dp, RaahiBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = RaahiTextDim,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Search mechanic, garage, puncture...",
                            color = RaahiTextFaint,
                            fontSize = 13.sp,
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        textStyle = TextStyle(color = RaahiText, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(RaahiOrange),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Clear",
                        tint = RaahiTextDim,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onSearchChange("") },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterCategoryRow(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories) { cat ->
            val isSelected = cat == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) RaahiOrange else Color.White)
                    .border(1.dp, if (isSelected) RaahiOrange else RaahiBorderSoft, RoundedCornerShape(20.dp))
                    .clickable { onSelect(cat) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    cat,
                    color = if (isSelected) Color.White else RaahiText,
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun SourceFilterRow(
    current: MechanicSourceFilter,
    onSelect: (MechanicSourceFilter) -> Unit,
    verifiedCount: Int,
    googleCount: Int,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(MechanicSourceFilter.entries.toTypedArray()) { filter ->
            val isSelected = filter == current
            val count = when (filter) {
                MechanicSourceFilter.ALL -> verifiedCount + googleCount
                MechanicSourceFilter.VERIFIED -> verifiedCount
                MechanicSourceFilter.GOOGLE_PLACES -> googleCount
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) RaahiText else Color.White)
                    .border(1.dp, if (isSelected) RaahiText else RaahiBorderSoft, RoundedCornerShape(8.dp))
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    "${filter.label} ($count)",
                    color = if (isSelected) Color.White else RaahiTextDim,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun MechanicsListView(
    items: List<UnifiedMechanicItem>,
    onItemClick: (UnifiedMechanicItem) -> Unit,
    onCall: (String) -> Unit,
    onDirections: (Double, Double) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyStateNotice()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.id }) { item ->
                MechanicPlaceCard(
                    item = item,
                    isSelected = false,
                    onClick = { onItemClick(item) },
                    onCall = onCall,
                    onDirections = onDirections,
                )
            }
        }
    }
}

@Composable
private fun MechanicsBottomSheetCard(
    items: List<UnifiedMechanicItem>,
    selectedItem: UnifiedMechanicItem?,
    onItemClick: (UnifiedMechanicItem) -> Unit,
    onCall: (String) -> Unit,
    onDirections: (Double, Double) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color.White)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .padding(top = 8.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(RaahiBorder)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (items.isEmpty()) "No mechanics found" else "${items.size} places nearby",
                color = RaahiText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
            )
            val openCount = items.count { it.isAvailable }
            if (openCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(RaahiGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("$openCount open", color = RaahiGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (items.isEmpty()) {
            EmptyStateNotice(modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    MechanicPlaceCard(
                        item = item,
                        isSelected = selectedItem?.id == item.id,
                        onClick = { onItemClick(item) },
                        onCall = onCall,
                        onDirections = onDirections,
                    )
                }
            }
        }
    }
}

@Composable
private fun MechanicPlaceCard(
    item: UnifiedMechanicItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCall: (String) -> Unit,
    onDirections: (Double, Double) -> Unit,
) {
    val borderColor = if (isSelected) RaahiOrange else RaahiBorderSoft
    val bgColor = if (isSelected) Color(0xFFFFF9F5) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                // Category icon badge
                val (badgeColor, iconVector) = when (item.source) {
                    MechanicSource.VERIFIED -> RaahiGreen to Icons.Outlined.VerifiedUser
                    MechanicSource.GOOGLE_PLACES -> Color(0xFF4285F4) to Icons.Outlined.Place
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(iconVector, contentDescription = null, tint = badgeColor, modifier = Modifier.size(19.dp))
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.name,
                            color = RaahiText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(6.dp))
                        // Source tag
                        when (item.source) {
                            MechanicSource.VERIFIED -> SourceBadge("Verified", RaahiGreen)
                            MechanicSource.GOOGLE_PLACES -> SourceBadge("Google Places", Color(0xFF4285F4))
                        }
                    }

                    if (!item.subtitle.isNullOrBlank()) {
                        Text(item.subtitle, color = RaahiTextDim, fontSize = 11.5.sp, maxLines = 1)
                    }
                    if (!item.address.isNullOrBlank()) {
                        Text(item.address, color = RaahiTextFaint, fontSize = 10.5.sp, maxLines = 1)
                    }

                    // Rating & distance line
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (item.rating != null) {
                            Icon(Icons.Outlined.Star, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(13.dp))
                            Text(
                                " ${"%.1f".format(item.rating)}${item.ratingCount?.let { " ($it)" } ?: ""}",
                                color = RaahiTextDim,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(" · ", color = RaahiTextFaint, fontSize = 11.sp)
                        }
                        Text(
                            "${"%.1f".format(item.distanceKm)} km away",
                            color = RaahiOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (item.specializations != null) {
                            Text(" · ${item.specializations}", color = RaahiTextDim, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                // Status indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (item.isAvailable) RaahiGreen else RaahiRed)
                )
            }

            // Action buttons row (Call & Directions)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!item.phone.isNullOrBlank()) {
                    SmallActionButton(
                        label = "Call",
                        icon = Icons.Outlined.Call,
                        tint = RaahiGreen,
                        onClick = { onCall(item.phone) },
                    )
                    Spacer(Modifier.width(8.dp))
                }
                SmallActionButton(
                    label = "Directions",
                    icon = Icons.Outlined.Navigation,
                    tint = RaahiCyan,
                    onClick = { onDirections(item.lat, item.lng) },
                )
            }
        }
    }
}

@Composable
private fun SourceBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MapRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .shadow(elevation = 4.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, RaahiBorderSoft, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = RaahiText, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RecenterFloatingButton(
    isRecentering: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .shadow(elevation = 5.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(RaahiBrandGradient)
            .clickable(onClick = onClick, enabled = !isRecentering),
        contentAlignment = Alignment.Center,
    ) {
        if (isRecentering) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Outlined.MyLocation, contentDescription = "Recenter", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun LoadingMechanicsScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = RaahiOrange, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(14.dp))
            Text(
                "Locating nearby mechanics…",
                color = RaahiText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Text(
                "Acquiring high-accuracy GPS fix",
                color = RaahiTextDim,
                fontSize = 11.5.sp,
            )
        }
    }
}

@Composable
private fun PermissionNeededScreen(onAllow: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(RaahiRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.LocationOff, contentDescription = null, tint = RaahiRed, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Location permission required",
                color = RaahiText,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Raahi needs your precise location to find mechanics and assistance around your real breakdown spot.",
                color = RaahiTextDim,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            RaahiPrimaryButton(text = "Allow Location", onClick = onAllow)
        }
    }
}

@Composable
private fun LocationUnavailableScreen(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(RaahiAmber.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.GpsOff, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "GPS fix unavailable",
                color = RaahiText,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Could not acquire your device's location. Please turn on device GPS and check location access.",
                color = RaahiTextDim,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            RaahiPrimaryButton(text = "Retry GPS", onClick = onRetry)
        }
    }
}

@Composable
private fun ErrorMechanicsScreen(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(14.dp))
            Text("Unable to load mechanics", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(message, color = RaahiTextDim, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            RaahiPrimaryButton(text = "Retry", onClick = onRetry)
        }
    }
}

@Composable
private fun EmptyStateNotice(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text("No mechanics found nearby", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Spacer(Modifier.height(4.dp))
            Text("Try clearing search filters or changing the category.", color = RaahiTextFaint, fontSize = 11.5.sp, textAlign = TextAlign.Center)
        }
    }
}

private fun launchDialer(context: Context, phone: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}"))
        context.startActivity(intent)
    }
}

private fun launchDirections(context: Context, lat: Double, lng: Double) {
    runCatching {
        val uri = Uri.parse("google.navigation:q=$lat,$lng")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng"))
            context.startActivity(fallback)
        }
    }
}
