package `in`.raahi.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.raahi.app.network.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepository @Inject constructor(
    private val api: RaahiApi,
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences("raahi_vehicles_store", Context.MODE_PRIVATE)
    private val gson = Gson()

    /** null means the user hasn't completed "Set up your car" yet — a real, expected state
     * (backend 404 VEHICLE_NOT_FOUND), not an error. */
    suspend fun myVehicle(): VehicleDto? = apiCallOrNullOn404 { api.myVehicle() }

    suspend fun upsertVehicle(req: UpsertVehicleRequest): VehicleDto = apiCall { api.upsertVehicle(req) }

    suspend fun updateOdometer(odometerKm: Int): VehicleDto =
        apiCall { api.updateOdometer(UpdateOdometerRequest(odometerKm)) }

    suspend fun carHealth(): CarHealthDto = apiCall { api.carHealth() }

    suspend fun serviceRecords(): List<ServiceRecordDto> = apiCall { api.serviceRecords() }

    suspend fun addServiceRecord(req: CreateServiceRecordRequest): ServiceRecordDto =
        apiCall { api.addServiceRecord(req) }

    // Multi-vehicle store methods:

    fun getSavedVehicles(): List<VehicleDto> {
        val raw = prefs.getString("saved_vehicles", null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<VehicleDto>>() {}.type
            gson.fromJson<List<VehicleDto>>(raw, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private fun saveVehiclesToPrefs(list: List<VehicleDto>) {
        prefs.edit().putString("saved_vehicles", gson.toJson(list)).apply()
    }

    fun getActiveVehicleId(): String? = prefs.getString("active_vehicle_id", null)

    private fun setActiveVehicleId(id: String?) {
        prefs.edit().putString("active_vehicle_id", id).apply()
    }

    suspend fun allVehicles(): List<VehicleDto> {
        val localList = getSavedVehicles().toMutableList()
        val backendVehicle = runCatching { myVehicle() }.getOrNull()

        if (backendVehicle != null) {
            val existingIdx = localList.indexOfFirst {
                it.id == backendVehicle.id ||
                it.registrationNumber.equals(backendVehicle.registrationNumber, ignoreCase = true)
            }
            if (existingIdx >= 0) {
                localList[existingIdx] = backendVehicle
            } else {
                localList.add(0, backendVehicle)
            }
            val activeId = getActiveVehicleId()
            if (activeId == null || localList.none { it.id == activeId }) {
                setActiveVehicleId(backendVehicle.id)
            }
            saveVehiclesToPrefs(localList)
        }
        return localList
    }

    fun getVehicleById(id: String): VehicleDto? = getSavedVehicles().firstOrNull { it.id == id }

    suspend fun saveVehicle(req: UpsertVehicleRequest, existingId: String? = null): VehicleDto {
        val localList = getSavedVehicles().toMutableList()
        val activeId = getActiveVehicleId()
        val isUpdatingActive = existingId != null && existingId == activeId
        val isFirstVehicle = localList.isEmpty()

        if (isFirstVehicle || isUpdatingActive || existingId == null) {
            val backendVehicle = upsertVehicle(req)
            val idx = localList.indexOfFirst {
                it.id == backendVehicle.id ||
                it.id == existingId ||
                it.registrationNumber.equals(req.registrationNumber, ignoreCase = true)
            }
            if (idx >= 0) {
                localList[idx] = backendVehicle
            } else {
                localList.add(0, backendVehicle)
            }
            setActiveVehicleId(backendVehicle.id)
            saveVehiclesToPrefs(localList)
            return backendVehicle
        } else {
            val targetId = existingId ?: UUID.randomUUID().toString()
            val vehicle = VehicleDto(
                id = targetId,
                brand = req.brand,
                model = req.model,
                variant = req.variant,
                modelYear = req.modelYear,
                fuelType = req.fuelType,
                registrationNumber = req.registrationNumber,
                odometerKm = req.odometerKm,
                odometerUpdatedAt = null,
                lastServiceDate = req.lastServiceDate,
                lastServiceOdometerKm = req.lastServiceOdometerKm,
                insuranceExpiry = req.insuranceExpiry,
                pucExpiry = req.pucExpiry,
                tyreReplacedDate = req.tyreReplacedDate,
                tyreReplacedOdometerKm = req.tyreReplacedOdometerKm,
                batteryReplacedDate = req.batteryReplacedDate,
                batteryReplacedOdometerKm = req.batteryReplacedOdometerKm,
                createdAt = null,
                updatedAt = null,
            )
            val idx = localList.indexOfFirst { it.id == targetId }
            if (idx >= 0) {
                localList[idx] = vehicle
            } else {
                localList.add(vehicle)
            }
            saveVehiclesToPrefs(localList)
            return vehicle
        }
    }

    suspend fun removeVehicle(vehicleId: String) {
        val localList = getSavedVehicles().toMutableList()
        localList.removeAll { it.id == vehicleId }
        saveVehiclesToPrefs(localList)

        val activeId = getActiveVehicleId()
        if (activeId == vehicleId) {
            val newActive = localList.firstOrNull()
            if (newActive != null) {
                setActiveVehicleId(newActive.id)
                runCatching { upsertVehicle(newActive.toUpsertRequest()) }
            } else {
                setActiveVehicleId(null)
            }
        }
    }

    suspend fun setActiveVehicle(vehicleId: String) {
        val vehicle = getVehicleById(vehicleId) ?: return
        setActiveVehicleId(vehicle.id)
        runCatching { upsertVehicle(vehicle.toUpsertRequest()) }
    }
}

fun VehicleDto.toUpsertRequest(): UpsertVehicleRequest = UpsertVehicleRequest(
    brand = brand,
    model = model,
    variant = variant,
    modelYear = modelYear,
    fuelType = fuelType,
    registrationNumber = registrationNumber,
    odometerKm = odometerKm,
    lastServiceDate = lastServiceDate,
    lastServiceOdometerKm = lastServiceOdometerKm,
    insuranceExpiry = insuranceExpiry,
    pucExpiry = pucExpiry,
    tyreReplacedDate = tyreReplacedDate,
    tyreReplacedOdometerKm = tyreReplacedOdometerKm,
    batteryReplacedDate = batteryReplacedDate,
    batteryReplacedOdometerKm = batteryReplacedOdometerKm,
)
