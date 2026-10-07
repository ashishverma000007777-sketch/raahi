package `in`.raahi.app.data

import `in`.raahi.app.network.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(private val api: RaahiApi) {
    suspend fun createTrip(req: CreateTripRequest): TripDto = apiCall { api.createTrip(req) }
    suspend fun listTrips(): List<TripDto> = apiCall { api.trips() }
    suspend fun getActiveTrip(): TripDto? = try {
        apiCall { api.activeTrip() }
    } catch (e: Exception) {
        null
    }
    suspend fun getTripDetails(id: String): TripDto = apiCall { api.tripDetails(id) }
    suspend fun startTrip(id: String, req: StartTripRequest? = null): TripDto = apiCall { api.startTrip(id, req) }
    suspend fun completeTrip(id: String, req: CompleteTripRequest? = null): TripDto = apiCall { api.completeTrip(id, req) }
    suspend fun getTripSummary(id: String): TripSummaryDto = apiCall { api.tripSummary(id) }
}
