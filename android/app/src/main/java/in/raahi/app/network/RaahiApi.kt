package `in`.raahi.app.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

// Response envelope — matches ApiResponse.java on the backend exactly.
data class ApiEnvelope<T>(val success: Boolean, val data: T?, val error: ApiErrorBody?)
data class ApiErrorBody(val code: String, val message: String)

data class VerifyRequest(val idToken: String, val requestedRole: String? = null)
data class VerifyResponse(val token: String, val isNewUser: Boolean, val user: UserDto)
data class UserDto(
    val id: String, val name: String?, val phone: String?, val role: String,
    val vehicleType: String?, val vehicleReg: String?, val isVerified: Boolean,
    val ratingAvg: Double, val totalHelps: Int
)

data class UpdateProfileRequest(
    val name: String? = null, val vehicleType: String? = null,
    val vehicleReg: String? = null, val language: String? = null
)
data class FcmTokenRequest(val token: String)

data class CreateJobRequest(
    val problemType: String, val problemDesc: String?,
    val lat: Double, val lng: Double,
    val highwayName: String? = null, val rewardAmount: Double = 0.0
)
data class VerifyOtpRequest(val otp: String)
data class JobDto(
    val id: String, val status: String, val problemType: String, val problemDesc: String?,
    val lat: Double, val lng: Double, val rewardAmount: Double, val helperOtp: String?,
    val requesterName: String?, val requesterPhone: String?,
    val helperName: String?, val helperPhone: String?,
    val helperRatingAvg: Double?, val helperTotalHelps: Int?, val helperVerified: Boolean?,
    // "REQUESTER" or "HELPER" — which side of this job the signed-in user is on, computed
    // server-side (see API_CONTRACT.md) rather than guessed client-side by matching names.
    val viewerRole: String?,
    val cancelAllowed: Boolean? = null,
    val arrivalMethod: String? = null,
    val arrivedAt: String? = null,
    val finalAmount: Double? = null,
    val paymentMode: String? = null,
    val rated: Boolean? = null,
    val completionOtp: String? = null,
    val commissionAmount: Double? = null,
    val distanceKm: Double? = null,
    val createdAt: String? = null,
)

data class ArriveRequest(val lat: Double?, val lng: Double?, val otp: String?)
data class WorkDoneRequest(val finalAmount: Double?, val paymentMode: String)
data class RateRequest(val stars: Int, val comment: String?)
data class CancelRequest(val reason: String?)
data class ReportRequest(val message: String, val kind: String = "ISSUE")
data class AvailabilityRequest(val online: Boolean, val lat: Double?, val lng: Double?)

data class HelperStatusDto(
    val applicationStatus: String, // NONE / PENDING / APPROVED / REJECTED / SUSPENDED
    val rejectionReason: String?, val suspensionReason: String?,
    val accountActive: Boolean, val blockedUntil: String?, val blockReason: String?,
    val online: Boolean, val commissionBalance: Double, val commissionDue: Boolean,
    val commissionRate: Double, val canAcceptJobs: Boolean,
    val ratingAvg: Double, val totalHelps: Int,
    val name: String?, val phone: String?, val services: String?, val serviceRadiusKm: Int?,
)
data class LedgerEntryDto(
    val id: String?, val job_id: String?, val entry_type: String, val amount: Double,
    val job_amount: Double?, val payment_mode: String?, val reference: String?, val created_at: String?,
)
data class HelperCommissionDto(val balance: Double, val due: Double, val rate: Double, val entries: List<LedgerEntryDto>)
data class HelperEarningsDto(
    val completedJobs: Long, val grossEarnings: Double, val totalCommission: Double,
    val netEarnings: Double, val commissionBalance: Double,
)
data class HelperJobHistoryDto(
    val jobId: String, val status: String, val problemType: String?, val amount: Double?,
    val paymentMode: String?, val rated: Boolean?, val createdAt: String?, val completedAt: String?,
)
data class HelperRatingDto(val job_id: String?, val stars: Int, val comment: String?, val created_at: String?)

data class MechanicDto(
    val userId: String, val name: String?, val phone: String?, val shopName: String?,
    val specializations: String?, val isAvailable: Boolean,
    val lat: Double?, val lng: Double?, val distanceKm: Double, val ratingAvg: Double,
)

data class TriggerSosRequest(val lat: Double, val lng: Double)
data class SosDto(
    val id: String, val status: String, val lat: Double, val lng: Double,
    val createdAt: String, val requesterName: String?, val distanceKm: Double?,
)
data class TriggerSosResponse(val sos: SosDto, val nearbyMechanicsNotified: Int)

data class DocSummaryDto(val id: String, val aiStatus: String, val aiNote: String?)
data class HelperApplicationDto(
    val id: String, val status: String, val email: String?, val rejectionReason: String?,
    val submittedAt: String?, val reviewedAt: String?,
    val aadhaarFront: DocSummaryDto?, val aadhaarBack: DocSummaryDto?, val selfie: DocSummaryDto?,
    val applicantName: String?, val applicantPhone: String?,
    val suspensionReason: String? = null,
    val experienceYears: Int? = null, val serviceArea: String? = null, val services: String? = null,
    val equipment: String? = null, val vehicleType: String? = null, val vehicleBrand: String? = null,
    val vehicleModel: String? = null, val vehicleVariant: String? = null, val vehicleReg: String? = null,
    val serviceRadiusKm: Int? = null, val payoutUpi: String? = null, val payoutHolder: String? = null,
    val payoutBankAcctMasked: String? = null, val payoutIfsc: String? = null,
)
data class RejectRequest(val reason: String)

data class SendAiMessageRequest(val content: String)
data class AiSessionDto(val id: String, val title: String?, val createdAt: String, val lastMessageAt: String)
data class AiMessageDto(val id: String, val role: String, val content: String, val createdAt: String, val unavailable: Boolean)

data class StreakDto(val currentStreak: Int, val longestStreak: Int, val totalCheckins: Int, val lastCheckin: String?, val canCheckin: Boolean, val reward: String?)
data class CreateAlertRequest(val type: String, val message: String, val location: String? = null, val lat: Double? = null, val lng: Double? = null)
data class AlertDto(
    val id: String, val type: String, val message: String, val location: String?,
    val lat: Double?, val lng: Double?, val upvotes: Int, val downvotes: Int,
    val createdAt: String, val postedBy: String?, val myVote: String?,
)
data class VoteRequest(val vote: String)
data class TipDto(val title: String, val body: String)
data class PlaceDto(val id: String, val name: String, val lat: Double, val lng: Double, val distanceKm: Double?)

data class FuelRateDto(val state: String, val petrol: Double, val diesel: Double, val cng: Double?, val updatedAt: String, val source: String? = null)
data class ShopProductDto(
    val id: String, val name: String, val category: String, val price: Double, val discountPrice: Double?,
    val brand: String?, val rating: Double?, val reviewCount: Int?, val description: String?,
    val affiliateUrl: String, val imageUrl: String,
)
data class SubscriptionPlanDto(val tier: String, val name: String, val priceMonthly: Double, val priceYearly: Double, val features: List<String>)
data class SubscriptionStatusDto(val tier: String, val status: String, val expiresAt: String?, val isActive: Boolean)

// Vehicle & Car Health — see docs/API_CONTRACT.md "Vehicle & Car Health" for the full
// contract. Every field here is either something the user typed in during "Set up your car"
// or a value the backend computed deterministically from those fields (CarHealthDto) —
// nothing here is ever populated with placeholder/sample data on the Android side.
data class VehicleDto(
    val id: String, val brand: String, val model: String, val variant: String?,
    val modelYear: Int, val fuelType: String, val registrationNumber: String,
    val odometerKm: Int, val odometerUpdatedAt: String?,
    val lastServiceDate: String?, val lastServiceOdometerKm: Int?,
    val insuranceExpiry: String?, val pucExpiry: String?,
    val tyreReplacedDate: String?, val tyreReplacedOdometerKm: Int?,
    val batteryReplacedDate: String?, val batteryReplacedOdometerKm: Int?,
    val createdAt: String?, val updatedAt: String?,
)
data class UpsertVehicleRequest(
    val brand: String, val model: String, val variant: String?, val modelYear: Int,
    val fuelType: String, val registrationNumber: String, val odometerKm: Int,
    val lastServiceDate: String? = null, val lastServiceOdometerKm: Int? = null,
    val insuranceExpiry: String? = null, val pucExpiry: String? = null,
    val tyreReplacedDate: String? = null, val tyreReplacedOdometerKm: Int? = null,
    val batteryReplacedDate: String? = null, val batteryReplacedOdometerKm: Int? = null,
)
data class UpdateOdometerRequest(val odometerKm: Int)
data class MaintenanceItemDto(val type: String, val status: String, val detail: String, val dueAtKm: Int?, val dueAtDate: String?)
data class WeeklyReportDto(
    val title: String? = null,
    val distanceDrivenKm: Int? = null,
    val fuelSpent: Double? = null,
    val litresFilled: Double? = null,
    val tripsCompleted: Int? = null,
    val highlights: List<String> = emptyList(),
)

data class YearEndReportDto(
    val year: Int = 2026,
    val totalTrips: Int? = null,
    val totalDistanceKm: Double? = null,
    val totalFuelSpent: Double? = null,
    val totalFuelLitres: Double? = null,
    val maintenanceCount: Int? = null,
    val highlights: List<String> = emptyList(),
)

data class CarHealthDto(
    val score: Int?,
    val message: String?,
    val factorsConsidered: Int,
    val factorsTotal: Int,
    val maintenanceItems: List<MaintenanceItemDto> = emptyList(),
    val grade: String? = null,
    val weeklyTrend: Int? = null,
    val weeklyTrendText: String? = null,
    val careScore: Int? = null,
    val careGrade: String? = null,
    val careSummary: String? = null,
    val careFactorsConsidered: Int = 0,
    val careFactorsTotal: Int = 5,
    val efficiencyScore: Int? = null,
    val efficiencyGrade: String? = null,
    val efficiencyMileageKmPerLitre: Double? = null,
    val efficiencyTrendText: String? = null,
    val efficiencySummary: String? = null,
    val safetyScore: Int? = null,
    val safetyGrade: String? = null,
    val safetyStatusText: String? = null,
    val safetySummary: String? = null,
    val weeklyReport: WeeklyReportDto? = null,
    val yearEndReport: YearEndReportDto? = null,
)
data class ServiceRecordDto(
    val id: String, val serviceDate: String, val odometerKm: Int, val serviceType: String,
    val notes: String?, val cost: Double?, val workshopName: String?, val partsReplaced: String?,
    val createdAt: String?,
)
data class CreateServiceRecordRequest(
    val serviceDate: String, val odometerKm: Int, val serviceType: String,
    val notes: String? = null, val cost: Double? = null,
    val workshopName: String? = null, val partsReplaced: String? = null,
)

// Paisa Bachao / Savings DTOs
data class StreakBadgeDto(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val badgeText: String,
    val isAchieved: Boolean,
    val currentCount: Int,
    val targetCount: Int,
    val progressText: String,
)

data class StateFuelRateDto(
    val state: String,
    val petrol: Double?,
    val diesel: Double?,
    val cng: Double?,
)

data class FuelPriceRadarDto(
    val homeState: String?,
    val homeStatePetrol: Double?,
    val homeStateDiesel: Double?,
    val cheapestNearbyState: String?,
    val cheapestStatePetrol: Double?,
    val priceDifferencePerLitre: Double?,
    val radarAdvice: String?,
    val stateRates: List<StateFuelRateDto> = emptyList(),
)

data class CommunityBenchmarkDto(
    val available: Boolean,
    val vehicleModel: String?,
    val fuelType: String?,
    val userAvgMileage: Double?,
    val communityAvgMileage: Double?,
    val differenceKmPerLitre: Double?,
    val comparisonSummary: String?,
    val note: String?,
)

data class SavingsSummaryDto(
    val currentMonthFuelSpend: Double? = null,
    val previousMonthFuelSpend: Double? = null,
    val fuelSpendSavings: Double? = null,
    val fuelSpendSavingsMessage: String? = null,
    val hasMoMComparison: Boolean = false,
    val currentMonthLitres: Double? = null,
    val currentMonthDistanceKm: Double? = null,
    val currentMonthMileageKmPerLitre: Double? = null,
    val previousMonthMileageKmPerLitre: Double? = null,
    val mileageTrendKmPerLitre: Double? = null,
    val mileageTrendText: String? = null,
    val totalMaintenanceSpend: Double? = null,
    val currentYearMaintenanceSpend: Double? = null,
    val maintenanceRecordsCount: Int = 0,
    val streaks: List<StreakBadgeDto> = emptyList(),
    val fuelPriceRadar: FuelPriceRadarDto? = null,
    val communityBenchmark: CommunityBenchmarkDto? = null,
)

// Home screen data — every field is derived server-side from persisted rows (see HomeDtos.java).
data class WeeklyKmDto(val km: Int?, val since: String?, val fullWindow: Boolean, val unavailableReason: String?)
data class HomeSummaryDto(val weeklyKm: WeeklyKmDto?, val helpsGivenThisWeek: Int, val unreadNotifications: Int)
data class FuelSummaryDto(val periodDays: Int, val fillUps: Int, val totalSpent: Double, val totalLitres: Double?, val lastFilledAt: String?, val hasAnyLogs: Boolean)
data class AiStatusDto(val state: String, val available: Boolean, val provider: String?, val message: String?)
data class CreateFuelLogRequest(
    val totalCost: Double? = null, val litres: Double? = null, val pricePerLitre: Double? = null,
    val odometerKm: Int? = null, val fuelType: String? = null, val filledAt: String? = null,
)
data class FuelLogDto(val id: String, val filledAt: String, val litres: Double?, val pricePerLitre: Double?, val totalCost: Double, val odometerKm: Int?, val fuelType: String?)
data class NotificationDto(val id: String, val title: String, val body: String, val createdAt: String, val read: Boolean)

// Trip Mode DTOs
data class CreateTripRequest(
    val startLocationName: String,
    val startLat: Double,
    val startLng: Double,
    val destLocationName: String,
    val destLat: Double,
    val destLng: Double,
    val title: String? = null,
    val fuelType: String? = null,
    val tankFull: Boolean? = null,
    val passengers: Int? = null,
    val preferredRoute: String? = null,
    val currentOdometer: Int? = null,
)

data class StartTripRequest(val startOdometerKm: Int? = null)

data class CompleteTripRequest(
    val endOdometerKm: Int? = null,
    val actualFuelCost: Double? = null,
    val actualLitres: Double? = null,
    val stopsVisitedCount: Int? = null,
)

data class TripStopDto(
    val id: String,
    val stopType: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val distanceKm: Double?,
    val pricePerLitre: Double?,
    val priceDiffPerLitre: Double?,
    val rating: Double?,
    val amenities: String?,
    val stopOrder: Int,
    val isVisited: Boolean,
)

data class TripDto(
    val id: String,
    val title: String,
    val startLocationName: String,
    val startLat: Double,
    val startLng: Double,
    val destLocationName: String,
    val destLat: Double,
    val destLng: Double,
    val distanceKm: Double,
    val durationMinutes: Int,
    val durationFormatted: String,
    val fuelType: String?,
    val tankFull: Boolean,
    val passengers: Int,
    val preferredRoute: String?,
    val estimatedFuelLitres: Double?,
    val estimatedFuelCost: Double?,
    val estimatedTolls: Double?,
    val tollStatus: String,
    val weatherCondition: String?,
    val weatherWarning: String?,
    val weatherStatus: String,
    val status: String,
    val startOdometerKm: Int?,
    val endOdometerKm: Int?,
    val actualFuelCost: Double?,
    val actualLitres: Double?,
    val stopsVisitedCount: Int,
    val startedAt: String?,
    val completedAt: String?,
    val createdAt: String,
    val prominentFuelStops: List<TripStopDto>?,
    val pitStops: List<TripStopDto>?,
    val mechanicsAlongRoute: List<TripStopDto>?,
    val allStops: List<TripStopDto>?,
)

data class TripStoryCard(
    val title: String,
    val subtitle: String,
    val distance: String,
    val fuelCost: String,
    val stopsCount: String,
    val duration: String,
    val shareableText: String,
)

data class TripSummaryDto(
    val tripId: String,
    val title: String,
    val date: String,
    val totalDistanceKm: Double,
    val durationFormatted: String,
    val fuelLoggedLitres: Double?,
    val fuelCost: Double?,
    val tollsFormatted: String,
    val stopsCount: Int,
    val stopsVisitedCount: Int,
    val routeDescription: String,
    val drivingScoreMessage: String,
    val storyCard: TripStoryCard?,
)

interface RaahiApi {

    @POST("auth/verify")
    suspend fun verify(@Body req: VerifyRequest): ApiEnvelope<VerifyResponse>

    @GET("auth/me")
    suspend fun me(): ApiEnvelope<UserDto>

    @PUT("auth/profile")
    suspend fun updateProfile(@Body req: UpdateProfileRequest): ApiEnvelope<UserDto>

    @PUT("auth/fcm-token")
    suspend fun updateFcmToken(@Body req: FcmTokenRequest): ApiEnvelope<Map<String, Boolean>>

    @POST("auth/logout")
    suspend fun logout(): ApiEnvelope<Map<String, Boolean>>

    @POST("jobs")
    suspend fun createJob(@Body req: CreateJobRequest): ApiEnvelope<JobDto>

    @GET("jobs")
    suspend fun availableJobs(@Query("lat") lat: Double? = null, @Query("lng") lng: Double? = null): ApiEnvelope<List<JobDto>>

    @GET("jobs/mine")
    suspend fun myJobs(): ApiEnvelope<List<JobDto>>

    @GET("jobs/{id}")
    suspend fun getJob(@Path("id") id: String): ApiEnvelope<JobDto>

    @POST("jobs/{id}/accept")
    suspend fun acceptJob(@Path("id") id: String): ApiEnvelope<JobDto>

    @POST("jobs/{id}/arrive")
    suspend fun arriveJob(@Path("id") id: String, @Body req: ArriveRequest): ApiEnvelope<JobDto>

    @POST("jobs/{id}/start")
    suspend fun startJob(@Path("id") id: String): ApiEnvelope<JobDto>

    @POST("jobs/{id}/work-done")
    suspend fun workDone(@Path("id") id: String, @Body req: WorkDoneRequest): ApiEnvelope<JobDto>

    @POST("jobs/{id}/confirm-completion")
    suspend fun confirmCompletion(@Path("id") id: String, @Body req: VerifyOtpRequest): ApiEnvelope<JobDto>

    @POST("jobs/{id}/rate")
    suspend fun rateJob(@Path("id") id: String, @Body req: RateRequest): ApiEnvelope<Map<String, Boolean>>

    @POST("jobs/{id}/cancel")
    suspend fun cancelJob(@Path("id") id: String, @Body req: CancelRequest): ApiEnvelope<Map<String, Any>>

    @POST("jobs/{id}/report")
    suspend fun reportJob(@Path("id") id: String, @Body req: ReportRequest): ApiEnvelope<Map<String, Boolean>>

    @GET("mechanics/nearby")
    suspend fun nearbyMechanics(
        @Query("lat") lat: Double, @Query("lng") lng: Double,
        @Query("radius") radius: Double? = null,
    ): ApiEnvelope<List<MechanicDto>>

    @GET("mechanics/{userId}")
    suspend fun getMechanic(@Path("userId") userId: String): ApiEnvelope<MechanicDto>

    @POST("sos")
    suspend fun triggerSos(@Body req: TriggerSosRequest): ApiEnvelope<TriggerSosResponse>

    @GET("sos/active")
    suspend fun activeSosNearby(
        @Query("lat") lat: Double, @Query("lng") lng: Double,
        @Query("radius") radius: Double? = null,
    ): ApiEnvelope<List<SosDto>>

    @GET("sos/mine")
    suspend fun mySosEvents(): ApiEnvelope<List<SosDto>>

    @POST("sos/{id}/resolve")
    suspend fun resolveSos(@Path("id") id: String): ApiEnvelope<Map<String, Boolean>>

    @Multipart
    @POST("helper/apply")
    suspend fun applyAsHelper(@Part parts: List<MultipartBody.Part>): ApiEnvelope<HelperApplicationDto>


    @PUT("helper/availability")
    suspend fun setHelperAvailability(@Body req: AvailabilityRequest): ApiEnvelope<Map<String, Boolean>>

    @GET("helper/commission")
    suspend fun helperCommission(): ApiEnvelope<HelperCommissionDto>

    @GET("helper/earnings")
    suspend fun helperEarnings(): ApiEnvelope<HelperEarningsDto>

    @GET("helper/ratings")
    suspend fun helperRatings(): ApiEnvelope<List<HelperRatingDto>>

    @GET("helper/jobs/history")
    suspend fun helperJobHistory(): ApiEnvelope<List<HelperJobHistoryDto>>

    @GET("helper/application/me")
    suspend fun myHelperApplication(): ApiEnvelope<HelperApplicationDto>

    @POST("ai/sessions")
    suspend fun createAiSession(): ApiEnvelope<AiSessionDto>

    @GET("ai/sessions")
    suspend fun myAiSessions(): ApiEnvelope<List<AiSessionDto>>

    @GET("ai/sessions/{id}/messages")
    suspend fun aiSessionMessages(@Path("id") id: String): ApiEnvelope<List<AiMessageDto>>

    @POST("ai/sessions/{id}/messages")
    suspend fun sendAiMessage(@Path("id") id: String, @Body req: SendAiMessageRequest): ApiEnvelope<AiMessageDto>

    @GET("daily/streak")
    suspend fun streak(): ApiEnvelope<StreakDto>

    @POST("daily/streak/checkin")
    suspend fun checkin(): ApiEnvelope<StreakDto>

    @GET("daily/alerts")
    suspend fun alerts(@Query("type") type: String? = null): ApiEnvelope<List<AlertDto>>

    @POST("daily/alerts")
    suspend fun createAlert(@Body req: CreateAlertRequest): ApiEnvelope<AlertDto>

    @POST("daily/alerts/{id}/vote")
    suspend fun voteAlert(@Path("id") id: String, @Body req: VoteRequest): ApiEnvelope<AlertDto>

    @GET("daily/tips")
    suspend fun todaysTip(): ApiEnvelope<TipDto>

    @GET("daily/places")
    suspend fun nearbyPlaces(@Query("lat") lat: Double, @Query("lng") lng: Double, @Query("type") type: String): ApiEnvelope<List<PlaceDto>>

    @GET("fuel-rates")
    suspend fun fuelRates(): ApiEnvelope<List<FuelRateDto>>

    @GET("shop/products")
    suspend fun shopProducts(@Query("category") category: String? = null): ApiEnvelope<List<ShopProductDto>>

    @GET("subscriptions/plans")
    suspend fun subscriptionPlans(): ApiEnvelope<List<SubscriptionPlanDto>>

    @GET("subscriptions/me")
    suspend fun mySubscription(): ApiEnvelope<SubscriptionStatusDto>

    @POST("subscriptions/subscribe")
    suspend fun subscribe(@Query("tier") tier: String): ApiEnvelope<Any>

    @GET("vehicles/me")
    suspend fun myVehicle(): ApiEnvelope<VehicleDto>

    @PUT("vehicles/me")
    suspend fun upsertVehicle(@Body req: UpsertVehicleRequest): ApiEnvelope<VehicleDto>

    @PUT("vehicles/me/odometer")
    suspend fun updateOdometer(@Body req: UpdateOdometerRequest): ApiEnvelope<VehicleDto>

    @GET("vehicles/me/health")
    suspend fun carHealth(): ApiEnvelope<CarHealthDto>

    @GET("vehicles/me/service-records")
    suspend fun serviceRecords(): ApiEnvelope<List<ServiceRecordDto>>

    @POST("vehicles/me/service-records")
    suspend fun addServiceRecord(@Body req: CreateServiceRecordRequest): ApiEnvelope<ServiceRecordDto>

    @GET("home/summary")
    suspend fun homeSummary(): ApiEnvelope<HomeSummaryDto>

    @GET("fuel-log/summary")
    suspend fun fuelSummary(): ApiEnvelope<FuelSummaryDto>

    @POST("fuel-log")
    suspend fun addFuelLog(@Body req: CreateFuelLogRequest): ApiEnvelope<FuelLogDto>

    @GET("fuel-log")
    suspend fun fuelLogs(): ApiEnvelope<List<FuelLogDto>>

    @GET("ai/status")
    suspend fun aiStatus(): ApiEnvelope<AiStatusDto>

    @GET("notifications")
    suspend fun notifications(): ApiEnvelope<List<NotificationDto>>

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(): ApiEnvelope<Map<String, Int>>

    // Trip Mode endpoints
    @POST("trips")
    suspend fun createTrip(@Body req: CreateTripRequest): ApiEnvelope<TripDto>

    @GET("trips")
    suspend fun trips(): ApiEnvelope<List<TripDto>>

    @GET("trips/active")
    suspend fun activeTrip(): ApiEnvelope<TripDto>

    @GET("trips/{id}")
    suspend fun tripDetails(@Path("id") id: String): ApiEnvelope<TripDto>

    @POST("trips/{id}/start")
    suspend fun startTrip(@Path("id") id: String, @Body req: StartTripRequest? = null): ApiEnvelope<TripDto>

    @POST("trips/{id}/complete")
    suspend fun completeTrip(@Path("id") id: String, @Body req: CompleteTripRequest? = null): ApiEnvelope<TripDto>

    @GET("trips/{id}/summary")
    suspend fun tripSummary(@Path("id") id: String): ApiEnvelope<TripSummaryDto>

    // Paisa Bachao / Savings endpoint
    @GET("savings")
    suspend fun getSavings(): ApiEnvelope<SavingsSummaryDto>
}
