@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package `in`.raahi.app.ui.screens.mechanics

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
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

    LaunchedEffect(Unit) {
        if (!permission.isGranted) {
            permission.request()
        }
    }

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
    var showFilterDialog by remember { mutableStateOf(false) }

    if (showFilterDialog) {
        CategoryFilterDialog(
            current = state.selectedCategory,
            onSelect = { viewModel.selectCategory(it) },
            onDismiss = { showFilterDialog = false },
        )
    }

    Column(Modifier.fillMaxSize()) {
        // Simplified top search & control header
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
            selectedCategory = state.selectedCategory,
            onOpenFilter = { showFilterDialog = true },
        )

        if (state.viewMode == MechanicsViewMode.LIST) {
            // Full list view option
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
            // Interactive Map View with Google Maps-style Bottom Carousel
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
                    onCameraMoved = viewModel::onCameraMoved,
                    onMapReady = onMapReady,
                )

                // Map controls overlay (Zoom + Recenter), safely placed above the bottom carousel
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = if (state.filteredItems.isEmpty()) 95.dp else 225.dp, end = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MapRoundButton(icon = Icons.Outlined.Add, onClick = onZoomIn)
                    MapRoundButton(icon = Icons.Outlined.Remove, onClick = onZoomOut)
                    RecenterFloatingButton(
                        isRecentering = state.isRecentering,
                        onClick = viewModel::recenter,
                    )
                }

                // Google Maps-style Bottom Carousel
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    MechanicsBottomCarousel(
                        items = state.filteredItems,
                        selectedItem = state.selectedItem,
                        selectedCategory = state.selectedCategory,
                        onClearCategory = { viewModel.selectCategory("All") },
                        onItemSelect = { item ->
                            if (item.source == MechanicSource.VERIFIED && item.rawMechanic != null && item.id == state.selectedItem?.id) {
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
    selectedCategory: String,
    onOpenFilter: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RaahiGlass)
            .border(1.dp, RaahiBorderSoft)
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
                // Category Filter Button with active indicator
                Box {
                    IconButton(onClick = onOpenFilter, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Filter categories",
                            tint = if (selectedCategory != "All") RaahiPrimaryTeal else RaahiTextDim,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    if (selectedCategory != "All") {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(RaahiPrimaryTeal)
                        )
                    }
                }

                // View mode toggle (Map vs List)
                IconButton(onClick = onToggleViewMode, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (viewMode == MechanicsViewMode.MAP) Icons.Outlined.List else Icons.Outlined.Map,
                        contentDescription = "Toggle view",
                        tint = RaahiPrimaryTeal,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Refresh Button
                IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp), enabled = !isRefreshing) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = RaahiPrimaryTeal)
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
                        cursorBrush = SolidColor(RaahiPrimaryTeal),
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
private fun CategoryFilterDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(RaahiBg2)
                .border(1.dp, RaahiBorderSoft, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Filter by Service",
                        color = RaahiText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RaahiDisplayFont,
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = RaahiTextDim, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                CATEGORIES.forEach { cat ->
                    val isSelected = cat == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) RaahiLightTeal else Color.Transparent)
                            .border(1.dp, if (isSelected) RaahiPrimaryTeal else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable {
                                onSelect(cat)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) RaahiPrimaryTealDark else RaahiText,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                        if (isSelected) {
                            Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = RaahiPrimaryTeal, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun MechanicsBottomCarousel(
    items: List<UnifiedMechanicItem>,
    selectedItem: UnifiedMechanicItem?,
    selectedCategory: String,
    onClearCategory: () -> Unit,
    onItemSelect: (UnifiedMechanicItem) -> Unit,
    onCall: (String) -> Unit,
    onDirections: (Double, Double) -> Unit,
) {
    if (items.isEmpty()) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            shape = RaahiCardShape,
            color = if (RaahiIsDark) Color(0xFF131D2E) else Color.White,
            border = BorderStroke(1.dp, RaahiBorderSoft),
            shadowElevation = RaahiCardElevation,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("No mechanics found nearby", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Try clearing filters or moving the map", color = RaahiTextFaint, fontSize = 11.5.sp)
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { items.size },
    )

    // Two-way sync: Swiping carousel -> select item and smoothly animate camera to marker
    LaunchedEffect(pagerState.settledPage) {
        val page = pagerState.settledPage
        if (page in items.indices && !pagerState.isScrollInProgress) {
            val currentItem = items[page]
            if (currentItem.id != selectedItem?.id) {
                onItemSelect(currentItem)
            }
        }
    }

    // Two-way sync: Map marker tapped -> scroll pager to selected item
    LaunchedEffect(selectedItem?.id) {
        val targetId = selectedItem?.id ?: return@LaunchedEffect
        val index = items.indexOfFirst { it.id == targetId }
        if (index >= 0 && index != pagerState.currentPage) {
            pagerState.animateScrollToPage(index)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        // Floating header chips: "X places nearby" and "X open"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (RaahiIsDark) Color(0xEE131D2E) else Color(0xF2FFFFFF),
                border = BorderStroke(1.dp, RaahiBorderSoft),
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${items.size} places nearby",
                        color = RaahiText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                    if (selectedCategory != "All") {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(RaahiLightTeal)
                                .clickable { onClearCategory() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(selectedCategory, color = RaahiPrimaryTealDark, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(3.dp))
                                Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = RaahiPrimaryTealDark, modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                }
            }

            val openCount = items.count { it.isAvailable }
            if (openCount > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (RaahiIsDark) Color(0xEE131D2E) else Color(0xF2FFFFFF),
                    border = BorderStroke(1.dp, RaahiGreen.copy(alpha = 0.25f)),
                    shadowElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(RaahiGreen)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("$openCount open", color = RaahiGreen, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Google Maps-style Horizontal Pager with peek on right/left edges
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(305.dp),
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val item = items[page]
            MechanicCarouselCard(
                item = item,
                isSelected = selectedItem?.id == item.id,
                onClick = { onItemSelect(item) },
                onCall = onCall,
                onDirections = onDirections,
                modifier = Modifier.width(305.dp),
            )
        }
    }
}

@Composable
private fun MechanicCarouselCard(
    item: UnifiedMechanicItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCall: (String) -> Unit,
    onDirections: (Double, Double) -> Unit,
    modifier: Modifier = Modifier.width(305.dp),
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RaahiCardShape,
        color = if (RaahiIsDark) Color(0xFF222629) else Color.White,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) RaahiPrimaryTeal else if (RaahiIsDark) Color(0xFF2E3338) else Color(0xFFE3E7E9),
        ),
        shadowElevation = if (isSelected) 4.dp else RaahiCardElevation,
    ) {
        Box {
            if (isSelected) {
                // Top accent indicator pill for the selected card
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(32.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                        .background(RaahiPrimaryTeal)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val (badgeBg, badgeTint, badgeIcon) = when (item.source) {
                        MechanicSource.VERIFIED -> Triple(RaahiLightTeal, RaahiPrimaryTealDark, Icons.Outlined.VerifiedUser)
                        MechanicSource.GOOGLE_PLACES -> Triple(RaahiLightTeal, RaahiPrimaryTeal, Icons.Outlined.Build)
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(badgeBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(badgeIcon, contentDescription = null, tint = badgeTint, modifier = Modifier.size(17.dp))
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.name,
                                color = RaahiText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (item.source == MechanicSource.VERIFIED) {
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(RaahiGreen.copy(alpha = 0.12f))
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Text("Verified", color = RaahiGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        val sub = item.specializations ?: item.subtitle ?: item.address
                        if (!sub.isNullOrBlank()) {
                            Text(
                                text = sub,
                                color = RaahiTextDim,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Rating, Distance & Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.rating != null && item.rating > 0.0) {
                        Icon(Icons.Outlined.Star, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "${"%.1f".format(item.rating)}${item.ratingCount?.let { " ($it)" } ?: ""}",
                            color = RaahiText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(" · ", color = RaahiTextFaint, fontSize = 11.sp)
                    }

                    Text(
                        text = "${"%.1f".format(item.distanceKm)} km away",
                        color = RaahiPrimaryTeal,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    if (item.isAvailable) {
                        Text(" · ", color = RaahiTextFaint, fontSize = 11.sp)
                        Text("Open", color = RaahiGreen, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Action buttons row (Call & Directions)
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        tint = RaahiPrimaryTeal,
                        onClick = { onDirections(item.lat, item.lng) },
                    )
                }
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
                MechanicCarouselCard(
                    item = item,
                    isSelected = false,
                    onClick = { onItemClick(item) },
                    onCall = onCall,
                    onDirections = onDirections,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SmallActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = tint, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MapRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (RaahiIsDark) Color(0xFF131D2E) else Color.White,
        border = BorderStroke(1.dp, RaahiBorderSoft),
        shadowElevation = 4.dp,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = RaahiText, modifier = Modifier.size(18.dp))
        }
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
            CircularProgressIndicator(color = RaahiPrimaryTeal, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
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
