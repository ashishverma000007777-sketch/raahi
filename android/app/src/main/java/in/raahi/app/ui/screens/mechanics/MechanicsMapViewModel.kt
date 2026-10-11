package `in`.raahi.app.ui.screens.mechanics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.GooglePlaceResult
import `in`.raahi.app.data.GooglePlacesProvider
import `in`.raahi.app.data.GooglePlacesStatus
import `in`.raahi.app.data.LatLng
import `in`.raahi.app.data.LocationProvider
import `in`.raahi.app.data.MechanicsRepository
import `in`.raahi.app.network.MechanicDto
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
    GOOGLE_PLACES("Google Places"),
}

enum class MechanicSource { VERIFIED, GOOGLE_PLACES }

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
    val rawGooglePlace: GooglePlaceResult? = null,
)

sealed class MechanicsMapUiState {
    data object Loading : MechanicsMapUiState()
    data object LocationUnavailable : MechanicsMapUiState()
    data class Error(val message: String) : MechanicsMapUiState()
    data class Loaded(
        val userLocation: LatLng,
        val mechanics: List<MechanicDto>,
        val errorMessage: String? = null,
        val isRefreshing: Boolean = false,
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
}

@HiltViewModel
class MechanicsMapViewModel @Inject constructor(
    private val mechanicsRepository: MechanicsRepository,
    private val locationProvider: LocationProvider,
    private val googlePlacesProvider: GooglePlacesProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<MechanicsMapUiState>(MechanicsMapUiState.Loading)
    val state: StateFlow<MechanicsMapUiState> = _state.asStateFlow()

    init {
        acquireLocationAndLoad()
    }

    fun start() = acquireLocationAndLoad()
    fun retry() = refresh()
    fun selectCategory(category: String) = setCategory(category)
    fun selectSourceFilter(filter: MechanicSourceFilter) = setSourceFilter(filter)

    fun acquireLocationAndLoad() {
        viewModelScope.launch {
            _state.value = MechanicsMapUiState.Loading

            // 1. First try cached fast GPS
            val cached = locationProvider.getLastKnownGpsLocation()
            if (cached != null && (cached.lat != 0.0 || cached.lng != 0.0)) {
                loadNearby(cached)
                return@launch
            }

            // 2. Request fresh real hardware GPS fix
            val fresh = locationProvider.getRealGpsLocation(freshTimeoutMs = 7000L)
            if (fresh != null && (fresh.lat != 0.0 || fresh.lng != 0.0)) {
                loadNearby(fresh)
                return@launch
            }

            // 3. Fallback to standard location provider
            val fallback = locationProvider.getCurrentLocation()
            if (fallback != null && (fallback.lat != 0.0 || fallback.lng != 0.0)) {
                loadNearby(fallback)
            } else {
                _state.value = MechanicsMapUiState.LocationUnavailable
            }
        }
    }

    fun recenter() {
        val current = _state.value as? MechanicsMapUiState.Loaded ?: return
        viewModelScope.launch {
            _state.value = current.copy(isRecentering = true)
            val fresh = locationProvider.getRealGpsLocation(freshTimeoutMs = 5000L)
                ?: locationProvider.getLastKnownGpsLocation()
                ?: current.userLocation

            val distMoved = haversineDistanceKm(current.userLocation.lat, current.userLocation.lng, fresh.lat, fresh.lng)
            if (distMoved > 0.15) {
                // User moved noticeably; refresh nearby mechanics around fresh coordinates
                loadNearby(fresh, isRecentering = true)
            } else {
                _state.value = current.copy(
                    isRecentering = false,
                    userLocation = fresh,
                    cameraTarget = fresh,
                    cameraZoom = 14.5,
                )
            }
        }
    }

    fun onCameraMoved() {
        val current = _state.value as? MechanicsMapUiState.Loaded ?: return
        if (current.cameraTarget != null || current.cameraZoom != null) {
            _state.value = current.copy(cameraTarget = null, cameraZoom = null)
        }
    }

    fun setViewMode(mode: MechanicsViewMode) {
        val current = _state.value as? MechanicsMapUiState.Loaded ?: return
        _state.value = current.copy(viewMode = mode)
    }

    fun setSearchQuery(query: String) {
        val current = _state.value as? MechanicsMapUiState.Loaded ?: return
        val filtered = filterItems(current.allItems, query, current.selectedCategory, current.sourceFilter)
        _state.value = current.copy(
            searchQuery = query,
            filteredItems = filtered,
            selectedItem = if (current.selectedItem != null && filtered.any { it.id == current.selectedItem.id }) current.selectedItem else null
        )
    }

    fun setCategory(category: String) {
        val current = _state.value as? MechanicsMapUiState.Loaded ?: return
        val filtered = filterItems(current.allItems, current.searchQuery, category, current.sourceFilter)
        _state.value = current.copy(
            selectedCategory = category,
            filteredItems = filtered,
            selectedItem = if (current.selectedItem != null && filtered.any { it.id == current.selectedItem.id }) current.selectedItem else null
        )
    }

    fun setSourceFilter(filter: MechanicSourceFilter) {
        val current = _state.value as? MechanicsMapUiState.Loaded ?: return
        val filtered = filterItems(current.allItems, current.searchQuery, current.selectedCategory, filter)
        _state.value = current.copy(
            sourceFilter = filter,
            filteredItems = filtered,
            selectedItem = if (current.selectedItem != null && filtered.any { it.id == current.selectedItem.id }) current.selectedItem else null
        )
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
            // Concurrent fetch: Verified Raahi mechanics and Google Places
            val mechanicsDeferred = async { runCatching { mechanicsRepository.nearby(location.lat, location.lng) } }
            val placesDeferred = async { runCatching { googlePlacesProvider.searchNearbyAutoRepair(location.lat, location.lng) } }

            val mechanicsResult = mechanicsDeferred.await()
            val placesResult = placesDeferred.await()

            val mechanics = mechanicsResult.getOrDefault(emptyList())

            var googlePlaces = emptyList<GooglePlaceResult>()
            var googleNotice: String? = null
            when (val pStatus = placesResult.getOrNull()) {
                is GooglePlacesStatus.Success -> googlePlaces = pStatus.places
                is GooglePlacesStatus.Unavailable -> googleNotice = pStatus.reason
                else -> {}
            }

            val error = when {
                mechanicsResult.isFailure ->
                    mechanicsResult.exceptionOrNull()?.toUserFriendlyMessage(
                        "Verified Raahi mechanics unavailable. Please check your connection."
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

            // 2. Genuine Google Places results (if available)
            for (gp in googlePlaces) {
                unifiedList.add(
                    UnifiedMechanicItem(
                        id = "google-${gp.id}",
                        name = gp.name,
                        subtitle = "Google Places",
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

            // Sort by distance
            unifiedList.sortBy { it.distanceKm }

            val currentQuery = current?.searchQuery.orEmpty()
            val currentCategory = current?.selectedCategory ?: "All"
            val currentSourceFilter = current?.sourceFilter ?: MechanicSourceFilter.ALL
            val filtered = filterItems(unifiedList, currentQuery, currentCategory, currentSourceFilter)

            _state.value = MechanicsMapUiState.Loaded(
                userLocation = location,
                mechanics = mechanics,
                errorMessage = error,
                isRefreshing = false,
                googlePlaces = googlePlaces,
                googlePlacesNotice = googleNotice,
                allItems = unifiedList,
                filteredItems = filtered,
                selectedItem = current?.selectedItem?.let { sel -> unifiedList.firstOrNull { it.id == sel.id } },
                searchQuery = currentQuery,
                selectedCategory = currentCategory,
                sourceFilter = currentSourceFilter,
                viewMode = current?.viewMode ?: MechanicsViewMode.MAP,
                isRecentering = false,
                cameraTarget = if (isRecentering || current == null) location else current.cameraTarget,
                cameraZoom = if (isRecentering || current == null) 14.5 else current.cameraZoom,
            )
        }
    }

    private fun filterItems(
        items: List<UnifiedMechanicItem>,
        query: String,
        category: String,
        sourceFilter: MechanicSourceFilter,
    ): List<UnifiedMechanicItem> {
        val q = query.trim().lowercase()
        return items.filter { item ->
            val matchesSource = when (sourceFilter) {
                MechanicSourceFilter.ALL -> true
                MechanicSourceFilter.VERIFIED -> item.source == MechanicSource.VERIFIED
                MechanicSourceFilter.GOOGLE_PLACES -> item.source == MechanicSource.GOOGLE_PLACES
            }
            if (!matchesSource) return@filter false

            val matchesQuery = if (q.isEmpty()) true else {
                item.name.lowercase().contains(q) ||
                    (item.address?.lowercase()?.contains(q) == true) ||
                    (item.specializations?.lowercase()?.contains(q) == true) ||
                    (item.subtitle?.lowercase()?.contains(q) == true)
            }
            if (!matchesQuery) return@filter false

            val matchesCategory = when (category) {
                "All" -> true
                "Puncture" -> item.specializations?.contains("puncture", ignoreCase = true) == true ||
                    item.name.contains("puncture", ignoreCase = true)
                "Garage" -> item.specializations?.contains("garage", ignoreCase = true) == true ||
                    item.specializations?.contains("engine", ignoreCase = true) == true ||
                    item.name.contains("garage", ignoreCase = true) ||
                    item.name.contains("motor", ignoreCase = true) ||
                    item.name.contains("service", ignoreCase = true)
                "Electrical" -> item.specializations?.contains("electric", ignoreCase = true) == true ||
                    item.specializations?.contains("battery", ignoreCase = true) == true ||
                    item.name.contains("battery", ignoreCase = true) ||
                    item.name.contains("electric", ignoreCase = true)
                "Towing" -> item.specializations?.contains("tow", ignoreCase = true) == true ||
                    item.name.contains("tow", ignoreCase = true) ||
                    item.name.contains("crane", ignoreCase = true)
                else -> true
            }
            matchesCategory
        }
    }

    fun refresh() {
        val loc = (_state.value as? MechanicsMapUiState.Loaded)?.userLocation
        if (loc != null) {
            loadNearby(loc)
        } else {
            acquireLocationAndLoad()
        }
    }

    private fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c * 10.0).roundToInt() / 10.0
    }
}
