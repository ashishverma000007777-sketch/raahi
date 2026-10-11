package `in`.raahi.app.ui.screens.mechanics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.GooglePlaceResult
import `in`.raahi.app.data.GooglePlacesProvider
import `in`.raahi.app.data.GooglePlacesStatus
import `in`.raahi.app.data.LatLng
import `in`.raahi.app.data.LocationProvider
import `in`.raahi.app.data.MechanicsRepository
import `in`.raahi.app.network.MechanicDto
import `in`.raahi.app.network.OsmMechanicShopDto
import `in`.raahi.app.network.toUserFriendlyMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

enum class MechanicsViewMode { MAP, LIST }

enum class MechanicSourceFilter(val label: String) {
    ALL("All Sources"),
    VERIFIED("Verified Raahi"),
    COMMUNITY_OSM("Community (OSM)"),
    GOOGLE_PLACES("Google Places"),
}

enum class MechanicSource { VERIFIED, OSM_COMMUNITY, GOOGLE_PLACES }

data class UnifiedMechanicItem(
    val id: String,
    val name: String,
    val subtitle: String?,
    val address: String?,
    val phone: String?,
    val lat: Double,
    val lng: Double,
    val distanceKm: Double,
    val rating: Double?,
    val ratingCount: Int? = null,
    val isAvailable: Boolean,
    val specializations: String?,
    val source: MechanicSource,
    val rawMechanic: MechanicDto? = null,
    val rawOsmShop: OsmMechanicShopDto? = null,
    val rawGooglePlace: GooglePlaceResult? = null,
)

sealed class MechanicsMapUiState {
    data object Loading : MechanicsMapUiState()
    data object LocationUnavailable : MechanicsMapUiState()
    data class Loaded(
        val userLocation: LatLng,
        val mechanics: List<MechanicDto>,
        val errorMessage: String? = null,
        val isRefreshing: Boolean = false,
        val shops: List<OsmMechanicShopDto> = emptyList(),
        val googlePlaces: List<GooglePlaceResult> = emptyList(),
        val googlePlacesNotice: String? = null,
        val allItems: List<UnifiedMechanicItem> = emptyList(),
        val filteredItems: List<UnifiedMechanicItem> = emptyList(),
        val selectedItem: UnifiedMechanicItem? = null,
        val searchQuery: String = "",
        val selectedCategory: String = "All",
        val sourceFilter: MechanicSourceFilter = MechanicSourceFilter.ALL,
        val viewMode: MechanicsViewMode = MechanicsViewMode.MAP,
        val isRecentering: Boolean = false,
        val cameraTarget: LatLng? = null,
        val cameraZoom: Double? = null,
    ) : MechanicsMapUiState()
    data class Error(val message: String) : MechanicsMapUiState()
}

@HiltViewModel
class MechanicsMapViewModel @Inject constructor(
    private val locationProvider: LocationProvider,
    private val mechanicsRepository: MechanicsRepository,
    private val authRepository: AuthRepository,
    private val googlePlacesProvider: GooglePlacesProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<MechanicsMapUiState>(MechanicsMapUiState.Loading)
    val state: StateFlow<MechanicsMapUiState> = _state.asStateFlow()

    private var lastLocation: LatLng? = null

    /**
     * Initializes or restarts location resolution.
     * Always acquires the real device GPS position; never uses hardcoded Delhi coordinates.
     */
    fun start() {
        if (!authRepository.hasAuthToken()) return
        val current = _state.value as? MechanicsMapUiState.Loaded
        if (current == null) {
            _state.value = MechanicsMapUiState.Loading
        }
        viewModelScope.launch {
            val location = runCatching { locationProvider.getRealGpsLocation() }.getOrNull()
            if (location == null) {
                if (lastLocation == null) {
                    _state.value = MechanicsMapUiState.LocationUnavailable
                    return@launch
                }
            } else {
                lastLocation = location
            }

            val targetLocation = lastLocation ?: location
            if (targetLocation == null) {
                _state.value = MechanicsMapUiState.LocationUnavailable
                return@launch
            }

            loadNearby(targetLocation, isRecentering = true)
        }
    }

    fun retry() {
        start()
    }

    /**
     * Re-queries fresh GPS fix and centers the map camera directly over the user.
     * Updates nearby mechanics if user has moved.
     */
    fun recenter() {
        val current = _state.value as? MechanicsMapUiState.Loaded
        viewModelScope.launch {
            if (current != null) {
                _state.value = current.copy(isRecentering = true)
            }
            val fresh = runCatching { locationProvider.getRealGpsLocation(freshTimeoutMs = 5000L) }.getOrNull()
            val loc = fresh ?: lastLocation ?: current?.userLocation
            if (loc != null) {
                lastLocation = loc
                if (current != null) {
                    val distMoved = haversineDistanceKm(current.userLocation.lat, current.userLocation.lng, loc.lat, loc.lng)
                    if (distMoved > 0.15) {
                        // User moved noticeably; refresh search at new coordinates
                        loadNearby(loc, isRecentering = false)
                    } else {
                        // Center camera to user's confirmed position
                        _state.value = current.copy(
                            userLocation = loc,
                            isRecentering = false,
                            cameraTarget = loc,
                            cameraZoom = 14.5,
                        )
                    }
                } else {
                    loadNearby(loc, isRecentering = false)
                }
            } else {
                if (current != null) {
                    _state.value = current.copy(isRecentering = false)
                } else {
                    _state.value = MechanicsMapUiState.LocationUnavailable
                }
            }
        }
    }

    fun onCameraMoved() {
        val current = _state.value as? MechanicsMapUiState.Loaded ?: return
        if (current.cameraTarget != null || current.cameraZoom != null) {
            _state.value = current.copy(cameraTarget = null, cameraZoom = null)
        }
    }

    fun setSearchQuery(query: String) {
        val current = _state.value as? MechanicsMapUiState.Loaded ?: return
        val filtered = filterItems(current.allItems, query, current.selectedCategory, current.sourceFilter)
        _state.value = current.copy(searchQuery = query, filteredItems = filtered)
    }

    fun selectCategory(category: String) {
        val current = _state.value as? MechanicsMapUiState.Loaded ?: return
        val filtered = filterItems(current.allItems, current.searchQuery, category, current.sourceFilter)
        _state.value = current.copy(selectedCategory = category, filteredItems = filtered)
    }

    fun selectSourceFilter(source: MechanicSourceFilter) {
        val current = _state.value as? MechanicsMapUiState.Loaded ?: return
        val filtered = filterItems(current.allItems, current.searchQuery, current.selectedCategory, source)
        _state.value = current.copy(sourceFilter = source, filteredItems = filtered)
    }

    fun setViewMode(mode: MechanicsViewMode) {
        val current = _state.value as? MechanicsMapUiState.Loaded ?: return
        _state.value = current.copy(viewMode = mode)
    }

    fun selectItem(item: UnifiedMechanicItem?) {
        val current = _state.value as? MechanicsMapUiState.Loaded ?: return
        _state.value = current.copy(
            selectedItem = item,
            cameraTarget = item?.let { LatLng(it.lat, it.lng) },
            cameraZoom = if (item != null) 15.0 else null,
        )
    }

    private fun loadNearby(location: LatLng, isRecentering: Boolean = false) {
        val current = _state.value as? MechanicsMapUiState.Loaded
        if (current != null) {
            _state.value = current.copy(isRefreshing = true, isRecentering = isRecentering)
        } else {
            _state.value = MechanicsMapUiState.Loading
        }

        viewModelScope.launch {
            // Concurrent fetch: Verified Raahi mechanics, OSM Community leads, Google Places
            val mechanicsDeferred = async { runCatching { mechanicsRepository.nearby(location.lat, location.lng) } }
            val shopsDeferred = async { runCatching { mechanicsRepository.nearbyShops(location.lat, location.lng) } }
            val placesDeferred = async { runCatching { googlePlacesProvider.searchNearbyAutoRepair(location.lat, location.lng) } }

            val mechanicsResult = mechanicsDeferred.await()
            val shopsResult = shopsDeferred.await()
            val placesResult = placesDeferred.await()

            val mechanics = mechanicsResult.getOrDefault(emptyList())
            val shops = shopsResult.getOrDefault(emptyList())

            var googlePlaces = emptyList<GooglePlaceResult>()
            var googleNotice: String? = null
            when (val pStatus = placesResult.getOrNull()) {
                is GooglePlacesStatus.Success -> googlePlaces = pStatus.places
                is GooglePlacesStatus.Unavailable -> googleNotice = pStatus.reason
                else -> {}
            }

            val error = when {
                mechanicsResult.isFailure && shopsResult.isFailure ->
                    mechanicsResult.exceptionOrNull()?.toUserFriendlyMessage(
                        "Nearby mechanics and repair shops unavailable. Please check your connection."
                    )
                shopsResult.isFailure ->
                    shopsResult.exceptionOrNull()?.toUserFriendlyMessage(
                        "Imported community repair shops could not load. Please retry."
                    )
                mechanicsResult.isFailure ->
                    mechanicsResult.exceptionOrNull()?.toUserFriendlyMessage(
                        "Verified Raahi mechanics unavailable. Community repair shops may still be shown."
                    )
                else -> null
            }

            // Build unified list with clear source tags
            val unifiedList = mutableListOf<UnifiedMechanicItem>()

            // 1. Verified Raahi Mechanics
            for (m in mechanics) {
                val mLat = m.lat ?: continue
                val mLng = m.lng ?: continue
                val dist = if (m.distanceKm >= 0) m.distanceKm else haversineDistanceKm(location.lat, location.lng, mLat, mLng)
                unifiedList.add(
                    UnifiedMechanicItem(
                        id = "verified-${m.userId}",
                        name = m.shopName ?: m.name ?: "Verified Mechanic",
                        subtitle = m.name?.takeIf { it != m.shopName },
                        address = null,
                        phone = m.phone,
                        lat = mLat,
                        lng = mLng,
                        distanceKm = dist,
                        rating = m.ratingAvg.takeIf { it > 0 },
                        isAvailable = m.isAvailable,
                        specializations = m.specializations,
                        source = MechanicSource.VERIFIED,
                        rawMechanic = m,
                    )
                )
            }

            // 2. OSM Community repair shops
            for (s in shops) {
                if (!s.lat.isFinite() || !s.lng.isFinite()) continue
                val dist = if (s.distanceKm >= 0) s.distanceKm else haversineDistanceKm(location.lat, location.lng, s.lat, s.lng)
                unifiedList.add(
                    UnifiedMechanicItem(
                        id = "osm-${s.id}",
                        name = s.name,
                        subtitle = "Community Record (OSM)",
                        address = s.address,
                        phone = s.phone,
                        lat = s.lat,
                        lng = s.lng,
                        distanceKm = dist,
                        rating = null,
                        isAvailable = true,
                        specializations = "Repair / Garage",
                        source = MechanicSource.OSM_COMMUNITY,
                        rawOsmShop = s,
                    )
                )
            }

            // 3. Genuine Google Places results (if available)
            for (gp in googlePlaces) {
                unifiedList.add(
                    UnifiedMechanicItem(
                        id = "google-${gp.id}",
                        name = gp.name,
                        subtitle = "Google Places Verified",
                        address = gp.address,
                        phone = null,
                        lat = gp.lat,
                        lng = gp.lng,
                        distanceKm = gp.distanceKm,
                        rating = gp.rating,
                        ratingCount = gp.userRatingCount,
                        isAvailable = gp.isOpenNow ?: true,
                        specializations = "Auto Service",
                        source = MechanicSource.GOOGLE_PLACES,
                        rawGooglePlace = gp,
                    )
                )
            }

            unifiedList.sortBy { it.distanceKm }

            val prevQuery = current?.searchQuery.orEmpty()
            val prevCategory = current?.selectedCategory ?: "All"
            val prevSourceFilter = current?.sourceFilter ?: MechanicSourceFilter.ALL
            val prevViewMode = current?.viewMode ?: MechanicsViewMode.MAP

            val filtered = filterItems(unifiedList, prevQuery, prevCategory, prevSourceFilter)

            _state.value = MechanicsMapUiState.Loaded(
                userLocation = location,
                mechanics = mechanics,
                shops = shops,
                googlePlaces = googlePlaces,
                googlePlacesNotice = googleNotice,
                allItems = unifiedList,
                filteredItems = filtered,
                selectedItem = null,
                searchQuery = prevQuery,
                selectedCategory = prevCategory,
                sourceFilter = prevSourceFilter,
                viewMode = prevViewMode,
                errorMessage = error,
                isRefreshing = false,
                isRecentering = false,
                cameraTarget = if (isRecentering || current == null) location else null,
                cameraZoom = if (isRecentering || current == null) 14.0 else null,
            )
        }
    }

    private fun filterItems(
        items: List<UnifiedMechanicItem>,
        query: String,
        category: String,
        source: MechanicSourceFilter,
    ): List<UnifiedMechanicItem> {
        return items.filter { item ->
            // Source filter
            val matchesSource = when (source) {
                MechanicSourceFilter.ALL -> true
                MechanicSourceFilter.VERIFIED -> item.source == MechanicSource.VERIFIED
                MechanicSourceFilter.COMMUNITY_OSM -> item.source == MechanicSource.OSM_COMMUNITY
                MechanicSourceFilter.GOOGLE_PLACES -> item.source == MechanicSource.GOOGLE_PLACES
            }
            if (!matchesSource) return@filter false

            // Category filter
            val matchesCategory = when (category) {
                "All" -> true
                "SOS / Towing" -> item.specializations?.contains("SOS", ignoreCase = true) == true ||
                    item.specializations?.contains("tow", ignoreCase = true) == true
                "Puncture" -> item.specializations?.contains("puncture", ignoreCase = true) == true ||
                    item.name.contains("puncture", ignoreCase = true)
                "Electric" -> item.specializations?.contains("electric", ignoreCase = true) == true ||
                    item.specializations?.contains("battery", ignoreCase = true) == true
                "Garage" -> item.specializations?.contains("garage", ignoreCase = true) == true ||
                    item.name.contains("garage", ignoreCase = true) || item.name.contains("motors", ignoreCase = true)
                else -> item.specializations?.contains(category, ignoreCase = true) == true
            }
            if (!matchesCategory) return@filter false

            // Search query
            if (query.isBlank()) return@filter true
            val q = query.trim().lowercase()
            item.name.lowercase().contains(q) ||
                (item.address?.lowercase()?.contains(q) == true) ||
                (item.specializations?.lowercase()?.contains(q) == true) ||
                (item.phone?.contains(q) == true)
        }
    }

    private fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
