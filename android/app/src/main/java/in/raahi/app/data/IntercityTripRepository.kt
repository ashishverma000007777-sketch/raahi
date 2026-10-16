package `in`.raahi.app.data

import `in`.raahi.app.network.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntercityTripRepository @Inject constructor(
    private val api: RaahiApi
) {
    suspend fun createTrip(req: CreateIntercityTripRequest): IntercityTripDto =
        apiCall { api.createIntercityTrip(req) }

    suspend fun searchTrips(
        pickupCity: String? = null,
        destCity: String? = null,
        vehicleCategory: String? = null,
        fromTime: String? = null,
    ): List<IntercityTripDto> =
        apiCall { api.searchIntercityTrips(pickupCity, destCity, vehicleCategory, fromTime) }

    suspend fun getAgentTrips(): List<IntercityTripDto> =
        apiCall { api.getAgentIntercityTrips() }

    suspend fun getDriverTrips(): List<IntercityTripDto> =
        apiCall { api.getDriverIntercityTrips() }

    suspend fun getActiveTrip(): IntercityTripDto? = try {
        apiCallOrNullOn404 { api.getActiveIntercityTrip() }
    } catch (e: Exception) {
        null
    }

    suspend fun getTripDetails(id: String): IntercityTripDto =
        apiCall { api.getIntercityTripDetails(id) }

    suspend fun applyForTrip(id: String, driverNotes: String? = null): IntercityTripApplicationDto =
        apiCall { api.applyForIntercityTrip(id, ApplyTripRequest(driverNotes)) }

    suspend fun getTripApplications(id: String): List<IntercityTripApplicationDto> =
        apiCall { api.getIntercityTripApplications(id) }

    suspend fun selectDriver(id: String, driverId: String): IntercityTripDto =
        apiCall { api.selectIntercityDriver(id, driverId) }

    suspend fun arriveAtPickup(id: String): IntercityTripDto =
        apiCall { api.arriveAtIntercityPickup(id) }

    suspend fun verifyPickupOtp(id: String, otp: String): IntercityTripDto =
        apiCall { api.verifyIntercityPickupOtp(id, VerifyOtpRequest(otp)) }

    suspend fun verifyCompletionOtp(id: String, otp: String): IntercityTripDto =
        apiCall { api.verifyIntercityCompletionOtp(id, VerifyOtpRequest(otp)) }

    suspend fun cancelTrip(id: String, reason: String? = null): IntercityTripDto =
        apiCall { api.cancelIntercityTrip(id, CancelIntercityTripRequest(reason)) }

    suspend fun settlePayment(id: String): IntercityTripDto =
        apiCall { api.settleIntercityPayment(id) }

    suspend fun arriveAtDestination(id: String): IntercityTripDto =
        apiCall { api.arriveAtIntercityDestination(id) }

    suspend fun completeOwnerTrip(id: String): IntercityTripDto =
        apiCall { api.completeOwnerIntercityTrip(id) }

    suspend fun submitSettlement(id: String, settlementRef: String, notes: String? = null): IntercityTripDto =
        apiCall { api.submitIntercitySettlement(id, SubmitSettlementRequest(settlementRef, notes)) }

    suspend fun confirmSettlement(id: String): IntercityTripDto =
        apiCall { api.confirmIntercitySettlement(id) }

    suspend fun raiseDispute(id: String, reason: String, evidence: String? = null): IntercityTripDto =
        apiCall { api.raiseIntercityDispute(id, RaiseDisputeRequest(reason, evidence)) }

    suspend fun resolveDispute(id: String, resolution: String, action: String): IntercityTripDto =
        apiCall { api.resolveIntercityDispute(id, ResolveDisputeRequest(resolution, action)) }

    suspend fun getMarketplaceStatus(): MarketplaceStatusDto =
        apiCall { api.getMarketplaceStatus() }

    suspend fun updateMarketplaceStatus(online: Boolean, pickupCity: String? = null): MarketplaceStatusDto =
        apiCall { api.updateMarketplaceStatus(UpdateMarketplaceStatusRequest(online, pickupCity)) }
}
