package `in`.raahi.app.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.raahi.app.network.ApiEnvelope
import `in`.raahi.app.network.AvailabilityRequest
import `in`.raahi.app.network.HelperApplicationDto
import `in`.raahi.app.network.HelperCommissionDto
import `in`.raahi.app.network.HelperEarningsDto
import `in`.raahi.app.network.HelperJobHistoryDto
import `in`.raahi.app.network.HelperRatingDto
import `in`.raahi.app.network.HelperStatusDto
import `in`.raahi.app.network.RaahiApi
import `in`.raahi.app.network.apiCall
import `in`.raahi.app.network.apiCallOrNullOn404
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the user typed/picked in the helper onboarding wizard. */
data class HelperApplicationForm(
    val experienceYears: String = "",
    val serviceArea: String = "",
    val services: Set<String> = emptySet(),
    val equipment: Set<String> = emptySet(),
    val aadhaarFront: Uri? = null,
    val aadhaarBack: Uri? = null,
    val selfie: Uri? = null,
    val vehicleType: String = "",
    val vehicleBrand: String = "",
    val vehicleModel: String = "",
    val vehicleVariant: String = "",
    val vehicleReg: String = "",
    val radiusKm: String = "15",
    val payoutUpi: String = "",
    val payoutHolder: String = "",
    val payoutBankAcct: String = "",
    val payoutIfsc: String = "",
)

@Singleton
class HelperRepository @Inject constructor(
    private val api: RaahiApi,
    @ApplicationContext private val context: Context,
) {
    /** Same Raahi account: name + verified phone come from the signed-in user, nothing is re-entered. */
    suspend fun apply(form: HelperApplicationForm, email: String? = null): HelperApplicationDto {
        val parts = mutableListOf<MultipartBody.Part>()
        fun text(name: String, value: String?) {
            if (!value.isNullOrBlank()) parts += MultipartBody.Part.createFormData(name, value.trim())
        }
        text("email", email)
        text("experienceYears", form.experienceYears)
        text("serviceArea", form.serviceArea)
        text("services", form.services.joinToString(","))
        text("equipment", form.equipment.joinToString(","))
        text("vehicleType", form.vehicleType)
        text("vehicleBrand", form.vehicleBrand)
        text("vehicleModel", form.vehicleModel)
        text("vehicleVariant", form.vehicleVariant)
        text("vehicleReg", form.vehicleReg)
        text("serviceRadiusKm", form.radiusKm)
        text("payoutUpi", form.payoutUpi)
        text("payoutHolder", form.payoutHolder)
        text("payoutBankAcct", form.payoutBankAcct)
        text("payoutIfsc", form.payoutIfsc)
        parts += uriToPart("aadhaarFront", requireNotNull(form.aadhaarFront))
        parts += uriToPart("aadhaarBack", requireNotNull(form.aadhaarBack))
        parts += uriToPart("selfie", requireNotNull(form.selfie))
        return apiCall { api.applyAsHelper(parts) }
    }

    suspend fun myApplication(): HelperApplicationDto? {
        return try {
            apiCallOrNullOn404 { api.myHelperApplication() }
        } catch (e: Exception) {
            val isNotFound = (e as? `in`.raahi.app.network.NetworkError.ClientError)?.statusCode == 404 ||
                (e.cause as? HttpException)?.code() == 404
            if (isNotFound) null else throw e
        }
    }

    suspend fun status(): HelperStatusDto = apiCall { api.helperStatus() }

    suspend fun setOnline(online: Boolean, lat: Double?, lng: Double?) {
        apiCall { api.setHelperAvailability(AvailabilityRequest(online, lat, lng)) }
    }

    suspend fun commission(): HelperCommissionDto = apiCall { api.helperCommission() }
    suspend fun earnings(): HelperEarningsDto = apiCall { api.helperEarnings() }
    suspend fun ratings(): List<HelperRatingDto> = apiCall { api.helperRatings() }
    suspend fun history(): List<HelperJobHistoryDto> = apiCall { api.helperJobHistory() }

    // Copies the picked content:// image into the cache dir so OkHttp can stream it with a known length.
    private fun uriToPart(fieldName: String, uri: Uri): MultipartBody.Part {
        val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val cacheFile = File.createTempFile(fieldName, ".img", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            cacheFile.outputStream().use { output -> input.copyTo(output) }
        }
        val body = cacheFile.asRequestBody(contentType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(fieldName, "$fieldName.jpg", body)
    }
}
