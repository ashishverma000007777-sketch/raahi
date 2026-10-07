package `in`.raahi.app.data

import `in`.raahi.app.network.ArriveRequest
import `in`.raahi.app.network.CancelRequest
import `in`.raahi.app.network.CreateJobRequest
import `in`.raahi.app.network.JobDto
import `in`.raahi.app.network.RaahiApi
import `in`.raahi.app.network.RateRequest
import `in`.raahi.app.network.ReportRequest
import `in`.raahi.app.network.VerifyOtpRequest
import `in`.raahi.app.network.WorkDoneRequest
import `in`.raahi.app.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobsRepository @Inject constructor(private val api: RaahiApi) {

    suspend fun myJobs(): List<JobDto> = apiCall { api.myJobs() }

    /** Open requests for an approved helper, nearest first when a location is given. */
    suspend fun availableJobs(lat: Double? = null, lng: Double? = null): List<JobDto> =
        apiCall { api.availableJobs(lat, lng) }

    suspend fun getJob(id: String): JobDto = apiCall { api.getJob(id) }

    suspend fun createJob(
        problemType: String, problemDesc: String?, lat: Double, lng: Double,
        highwayName: String? = null, rewardAmount: Double = 0.0
    ): JobDto = apiCall { api.createJob(CreateJobRequest(problemType, problemDesc, lat, lng, highwayName, rewardAmount)) }

    suspend fun acceptJob(id: String): JobDto = apiCall { api.acceptJob(id) }

    /** Arrival proof: GPS fix and/or the arrival OTP the customer shows. */
    suspend fun arrive(id: String, lat: Double?, lng: Double?, otp: String?): JobDto =
        apiCall { api.arriveJob(id, ArriveRequest(lat, lng, otp?.takeIf { it.isNotBlank() })) }

    suspend fun startWork(id: String): JobDto = apiCall { api.startJob(id) }

    suspend fun workDone(id: String, finalAmount: Double?, paymentMode: String): JobDto =
        apiCall { api.workDone(id, WorkDoneRequest(finalAmount, paymentMode)) }

    suspend fun confirmCompletion(id: String, otp: String): JobDto =
        apiCall { api.confirmCompletion(id, VerifyOtpRequest(otp)) }

    suspend fun rate(id: String, stars: Int, comment: String?) {
        apiCall { api.rateJob(id, RateRequest(stars, comment?.takeIf { it.isNotBlank() })) }
    }

    suspend fun cancelJob(id: String, reason: String? = null) {
        apiCall { api.cancelJob(id, CancelRequest(reason)) }
    }

    suspend fun report(id: String, message: String) {
        apiCall { api.reportJob(id, ReportRequest(message)) }
    }
}
