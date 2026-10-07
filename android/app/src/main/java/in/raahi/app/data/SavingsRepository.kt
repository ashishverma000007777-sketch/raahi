package `in`.raahi.app.data

import `in`.raahi.app.network.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingsRepository @Inject constructor(private val api: RaahiApi) {
    suspend fun getSavings(): SavingsSummaryDto = apiCall { api.getSavings() }
}
