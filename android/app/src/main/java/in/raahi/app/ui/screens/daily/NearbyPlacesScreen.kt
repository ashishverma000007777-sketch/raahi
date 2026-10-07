package `in`.raahi.app.ui.screens.daily

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.DailyRepository
import `in`.raahi.app.data.LocationProvider
import `in`.raahi.app.network.PlaceDto
import `in`.raahi.app.network.toUserFriendlyMessage
import `in`.raahi.app.ui.components.rememberLocationPermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import `in`.raahi.app.ui.theme.*
import javax.inject.Inject

private val PLACE_TYPES = listOf(
    "dhaba" to "🍽️ Dhabas",
    "fuel" to "⛽ Fuel",
    "atm" to "💳 ATM",
    "parking" to "🅿️ Parking",
    "toilet" to "🚻 Toilet",
    "hotel" to "🏨 Hotel"
)

sealed class PlacesUiState {
    data object Loading : PlacesUiState()
    data class Loaded(val places: List<PlaceDto>) : PlacesUiState()
    data class Error(val message: String) : PlacesUiState()
}

@HiltViewModel
class NearbyPlacesViewModel @Inject constructor(
    private val repository: DailyRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {
    private val _state = MutableStateFlow<PlacesUiState>(PlacesUiState.Loading)
    val state: StateFlow<PlacesUiState> = _state.asStateFlow()
    private val _selectedType = MutableStateFlow("dhaba")
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()
    private var lastLocation: `in`.raahi.app.data.LatLng? = null

    fun selectType(type: String) {
        _selectedType.value = type
        val loc = lastLocation
        if (loc != null) load(loc)
    }

    fun start() {
        _state.value = PlacesUiState.Loading
        viewModelScope.launch {
            val loc = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
            if (loc == null) {
                _state.value = PlacesUiState.Error("Couldn't get your location — enable GPS and retry")
                return@launch
            }
            lastLocation = loc
            load(loc)
        }
    }

    private fun load(loc: `in`.raahi.app.data.LatLng) {
        _state.value = PlacesUiState.Loading
        viewModelScope.launch {
            runCatching { repository.nearbyPlaces(loc.lat, loc.lng, _selectedType.value) }
                .onSuccess { list -> _state.value = PlacesUiState.Loaded(list) }
                .onFailure { e -> _state.value = PlacesUiState.Error(e.toUserFriendlyMessage("Could not load nearby places. Check your connection.")) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyPlacesScreen(onBack: () -> Unit, viewModel: NearbyPlacesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val permission = rememberLocationPermissionState()

    LaunchedEffect(permission.isGranted) {
        if (permission.isGranted) viewModel.start()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Nearby Places",
                            color = RaahiText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RaahiDisplayFont
                        )
                        Text(
                            text = "Fuel, Food & Essential Highway Stops",
                            color = RaahiTextDim,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RaahiText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaahiBg)
            )
        },
        containerColor = RaahiBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Dedicated Editorial Illustration: Detailed highway environment with fuel, dhabas, parking (seamless blend)

            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PLACE_TYPES) { (type, label) ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { viewModel.selectType(type) },
                        label = { Text(label, fontSize = 12.5.sp, fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RaahiOrange.copy(alpha = 0.15f),
                            selectedLabelColor = RaahiOrange
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedType == type,
                            borderColor = if (selectedType == type) RaahiOrange else RaahiBorderSoft
                        ),
                        shape = RaahiShapePill
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (!permission.isGranted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x10000000))
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .border(1.dp, RaahiBorderSoft, RoundedCornerShape(20.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(RaahiOrange.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.LocationOff, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Location Permission Required", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Allow location access to discover verified fuel pumps, dhabas, and clean restrooms along your route.",
                            color = RaahiTextDim,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = permission.request,
                            colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                            shape = RaahiShapeMedium
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Enable Location", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else when (val s = state) {
                is PlacesUiState.Loading -> {
                    NearbyPlacesSkeleton()
                }
                is PlacesUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x10000000))
                                .background(Color.White, RoundedCornerShape(20.dp))
                                .border(1.dp, RaahiBorderSoft, RoundedCornerShape(20.dp))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(RaahiRed.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = RaahiRed, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Connection Issue", color = RaahiText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(s.message, color = RaahiTextDim, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = viewModel::start,
                                colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                                shape = RaahiShapeMedium
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is PlacesUiState.Loaded -> {
                    if (s.places.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x10000000))
                                    .background(Color.White, RoundedCornerShape(20.dp))
                                    .border(1.dp, RaahiBorderSoft, RoundedCornerShape(20.dp))
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(RaahiAmber.copy(alpha = 0.14f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.SearchOff, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(24.dp))
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("No Stops Found Nearby", color = RaahiText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "No places found for this category within range. Try switching to Fuel or Dhabas, or expand your search.",
                                    color = RaahiTextDim,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = viewModel::start,
                                    shape = RaahiShapeMedium
                                ) {
                                    Icon(Icons.Filled.NearMe, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Refresh Search", color = RaahiOrange, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(s.places, key = { it.id }) { place ->
                                PlaceRow(place)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyPlacesSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "places_skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(14.dp), spotColor = Color(0x0C000000))
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .border(1.dp, RaahiBorderSoft, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RaahiBorderSoft.copy(alpha = alpha))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(RaahiBorderSoft.copy(alpha = alpha))
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(RaahiBorderSoft.copy(alpha = alpha))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(width = 45.dp, height = 18.dp)
                        .clip(RaahiShapePill)
                        .background(RaahiBorderSoft.copy(alpha = alpha))
                )
            }
        }
    }
}

@Composable
private fun PlaceRow(place: PlaceDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.5.dp, RoundedCornerShape(14.dp), spotColor = Color(0x10000000))
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, RaahiBorderSoft, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(RaahiOrange.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = RaahiOrange,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = place.name,
                color = RaahiText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
        if (place.distanceKm != null) {
            Box(
                modifier = Modifier
                    .background(RaahiCyan.copy(alpha = 0.12f), RaahiShapePill)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "%.1f km".format(place.distanceKm),
                    color = RaahiCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}
