package `in`.raahi.app.ui.screens.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.data.VehicleCatalogue
import `in`.raahi.app.network.UpsertVehicleRequest
import `in`.raahi.app.ui.theme.*

private val FUEL_TYPES = listOf("PETROL", "DIESEL", "CNG", "ELECTRIC", "HYBRID", "LPG")
private val DATE_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")

@Composable
fun VehicleSetupScreen(
    onDone: () -> Unit,
    onBack: (() -> Unit)? = null,
    isEditing: Boolean = false,
    viewModel: VehicleSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val prefill by viewModel.prefill.collectAsState()

    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var variant by remember { mutableStateOf("") }
    var modelYear by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("PETROL") }
    var registrationNumber by remember { mutableStateOf("") }
    var odometerKm by remember { mutableStateOf("") }

    var showBrandPicker by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }
    var showVariantPicker by remember { mutableStateOf(false) }

    var showOptional by remember { mutableStateOf(false) }
    var lastServiceDate by remember { mutableStateOf("") }
    var lastServiceOdometerKm by remember { mutableStateOf("") }
    var insuranceExpiry by remember { mutableStateOf("") }
    var pucExpiry by remember { mutableStateOf("") }
    var tyreReplacedDate by remember { mutableStateOf("") }
    var tyreReplacedOdometerKm by remember { mutableStateOf("") }
    var batteryReplacedDate by remember { mutableStateOf("") }
    var batteryReplacedOdometerKm by remember { mutableStateOf("") }

    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { if (isEditing) viewModel.loadForEdit() }
    LaunchedEffect(prefill) {
        prefill?.let { v ->
            brand = v.brand; model = v.model; variant = v.variant.orEmpty()
            modelYear = v.modelYear.toString(); fuelType = v.fuelType
            registrationNumber = v.registrationNumber; odometerKm = v.odometerKm.toString()
            lastServiceDate = v.lastServiceDate.orEmpty()
            lastServiceOdometerKm = v.lastServiceOdometerKm?.toString().orEmpty()
            insuranceExpiry = v.insuranceExpiry.orEmpty()
            pucExpiry = v.pucExpiry.orEmpty()
            tyreReplacedDate = v.tyreReplacedDate.orEmpty()
            tyreReplacedOdometerKm = v.tyreReplacedOdometerKm?.toString().orEmpty()
            batteryReplacedDate = v.batteryReplacedDate.orEmpty()
            batteryReplacedOdometerKm = v.batteryReplacedOdometerKm?.toString().orEmpty()
            if (lastServiceDate.isNotBlank() || insuranceExpiry.isNotBlank() || pucExpiry.isNotBlank() ||
                tyreReplacedDate.isNotBlank() || batteryReplacedDate.isNotBlank()
            ) showOptional = true
        }
    }
    LaunchedEffect(state) { if (state is VehicleSetupState.Saved) onDone() }

    fun validOptionalDate(s: String) = s.isBlank() || DATE_REGEX.matches(s)

    fun trySave() {
        validationError = null
        val year = modelYear.toIntOrNull()
        val odo = odometerKm.toIntOrNull()
        when {
            brand.isBlank() -> validationError = "Vehicle brand is required"
            model.isBlank() -> validationError = "Vehicle model is required"
            year == null || year < 1980 || year > 2100 -> validationError = "Enter a valid model year (e.g. 2022)"
            registrationNumber.isBlank() -> validationError = "Registration number is required"
            odo == null || odo < 0 -> validationError = "Enter a valid odometer reading"
            !validOptionalDate(lastServiceDate) -> validationError = "Last service date must be YYYY-MM-DD"
            !validOptionalDate(insuranceExpiry) -> validationError = "Insurance expiry must be YYYY-MM-DD"
            !validOptionalDate(pucExpiry) -> validationError = "PUC expiry must be YYYY-MM-DD"
            !validOptionalDate(tyreReplacedDate) -> validationError = "Tyre replaced date must be YYYY-MM-DD"
            !validOptionalDate(batteryReplacedDate) -> validationError = "Battery replaced date must be YYYY-MM-DD"
            else -> {
                viewModel.save(
                    UpsertVehicleRequest(
                        brand = brand.trim(), model = model.trim(), variant = variant.trim().ifBlank { null },
                        modelYear = year, fuelType = fuelType, registrationNumber = registrationNumber.trim(),
                        odometerKm = odo,
                        lastServiceDate = lastServiceDate.ifBlank { null },
                        lastServiceOdometerKm = lastServiceOdometerKm.toIntOrNull(),
                        insuranceExpiry = insuranceExpiry.ifBlank { null },
                        pucExpiry = pucExpiry.ifBlank { null },
                        tyreReplacedDate = tyreReplacedDate.ifBlank { null },
                        tyreReplacedOdometerKm = tyreReplacedOdometerKm.toIntOrNull(),
                        batteryReplacedDate = batteryReplacedDate.ifBlank { null },
                        batteryReplacedOdometerKm = batteryReplacedOdometerKm.toIntOrNull(),
                    )
                )
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = RaahiBg) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RaahiText)
                    }
                }
            }

            Column(Modifier.padding(horizontal = 20.dp)) {
                // Header Texts matching reference
                Text(
                    text = if (isEditing) "Edit your car" else "Set up your car",
                    color = RaahiText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RaahiDisplayFont
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Real details from your RC and last service — this is what your Car Health is calculated from.",
                    color = RaahiTextDim,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(16.dp))


                Spacer(Modifier.height(20.dp))

                // 1. Brand Selector (Tap Brand -> Compact dialog)
                FieldLabel("Vehicle brand *")
                SelectableField(
                    value = brand,
                    placeholder = "e.g. Hyundai",
                    icon = Icons.Outlined.DirectionsCar,
                    onClick = { showBrandPicker = true }
                )

                Spacer(Modifier.height(14.dp))

                // 2. Model Selector (Brand selection filters Models)
                FieldLabel("Model *")
                SelectableField(
                    value = model,
                    placeholder = if (brand.isNotBlank()) "e.g. Grand i10 Nios" else "Select brand first",
                    icon = Icons.Outlined.DirectionsCar,
                    enabled = brand.isNotBlank(),
                    onClick = {
                        if (brand.isNotBlank()) showModelPicker = true
                        else showBrandPicker = true
                    }
                )

                Spacer(Modifier.height(14.dp))

                // 3. Variant Selector (Model selection filters Variants)
                FieldLabel("Variant")
                SelectableField(
                    value = variant,
                    placeholder = if (model.isNotBlank()) "e.g. Sportz (optional)" else "Select model first",
                    icon = Icons.Outlined.Badge,
                    enabled = model.isNotBlank(),
                    onClick = {
                        if (model.isNotBlank()) showVariantPicker = true
                        else if (brand.isNotBlank()) showModelPicker = true
                        else showBrandPicker = true
                    }
                )

                Spacer(Modifier.height(14.dp))

                // 4. Model Year
                FieldLabel("Model year *")
                RaahiLightTextField(
                    value = modelYear,
                    onValueChange = { modelYear = it.filter { c -> c.isDigit() }.take(4) },
                    placeholder = "e.g. 2022",
                    icon = Icons.Outlined.CalendarToday,
                    keyboardType = KeyboardType.Number
                )

                Spacer(Modifier.height(16.dp))

                // 5. Fuel Type Selector Chips
                FieldLabel("Fuel type *")
                FuelTypeSelector(selected = fuelType) { fuelType = it }

                Spacer(Modifier.height(16.dp))

                // 6. Registration Number
                FieldLabel("Registration number *")
                RaahiLightTextField(
                    value = registrationNumber,
                    onValueChange = { registrationNumber = it.uppercase() },
                    placeholder = "e.g. PB65AB1234",
                    icon = Icons.Outlined.DirectionsCar
                )

                Spacer(Modifier.height(14.dp))

                // 7. Current Odometer
                FieldLabel("Current odometer (km) *")
                RaahiLightTextField(
                    value = odometerKm,
                    onValueChange = { odometerKm = it.filter { c -> c.isDigit() } },
                    placeholder = "e.g. 48320",
                    icon = Icons.Outlined.Speed,
                    keyboardType = KeyboardType.Number
                )

                Spacer(Modifier.height(18.dp))

                // Optional details accordion
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, RaahiBorder, RoundedCornerShape(12.dp))
                        .clickable { showOptional = !showOptional }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (showOptional) "Hide optional details" else "+ Add optional details (service, insurance, PUC...)",
                        color = RaahiOrange,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (showOptional) {
                    Spacer(Modifier.height(14.dp))
                    FieldLabel("Last service date")
                    RaahiLightTextField(lastServiceDate, { lastServiceDate = it }, placeholder = "YYYY-MM-DD")
                    Spacer(Modifier.height(14.dp))
                    FieldLabel("Odometer at last service (km)")
                    RaahiLightTextField(lastServiceOdometerKm, { lastServiceOdometerKm = it.filter { c -> c.isDigit() } }, placeholder = "e.g. 43000", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(14.dp))
                    FieldLabel("Insurance expiry")
                    RaahiLightTextField(insuranceExpiry, { insuranceExpiry = it }, placeholder = "YYYY-MM-DD")
                    Spacer(Modifier.height(14.dp))
                    FieldLabel("PUC expiry")
                    RaahiLightTextField(pucExpiry, { pucExpiry = it }, placeholder = "YYYY-MM-DD")
                    Spacer(Modifier.height(14.dp))
                    FieldLabel("Tyres last replaced — date")
                    RaahiLightTextField(tyreReplacedDate, { tyreReplacedDate = it }, placeholder = "YYYY-MM-DD")
                    Spacer(Modifier.height(14.dp))
                    FieldLabel("Tyres last replaced — odometer (km)")
                    RaahiLightTextField(tyreReplacedOdometerKm, { tyreReplacedOdometerKm = it.filter { c -> c.isDigit() } }, placeholder = "e.g. 30000", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(14.dp))
                    FieldLabel("Battery last replaced — date")
                    RaahiLightTextField(batteryReplacedDate, { batteryReplacedDate = it }, placeholder = "YYYY-MM-DD")
                    Spacer(Modifier.height(14.dp))
                    FieldLabel("Battery last replaced — odometer (km)")
                    RaahiLightTextField(batteryReplacedOdometerKm, { batteryReplacedOdometerKm = it.filter { c -> c.isDigit() } }, placeholder = "e.g. 30000", keyboardType = KeyboardType.Number)
                }

                val errorText = validationError ?: (state as? VehicleSetupState.Error)?.message
                if (errorText != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(errorText, color = RaahiRed, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(Modifier.height(24.dp))

                // Primary CTA button: "Save & Continue" with vibrant coral styling & glow
                Button(
                    onClick = { trySave() },
                    enabled = state !is VehicleSetupState.Saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), spotColor = RaahiOrange),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                ) {
                    if (state is VehicleSetupState.Saving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (isEditing) "Save Changes" else "Save & Continue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }

                if (!isEditing) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                        Text("Skip for now", color = RaahiTextDim, fontSize = 13.sp)
                    }
                }
            }
        }

        // Compact Brand Picker Dialog
        if (showBrandPicker) {
            val brandsList = VehicleCatalogue.BRANDS.map { it.name }
            VehiclePickerDialog(
                title = "Select Vehicle Brand",
                items = brandsList,
                selectedItem = brand,
                onSelect = { selectedBrand ->
                    brand = selectedBrand
                    model = ""
                    variant = ""
                    // Automatically prompt for model
                    showModelPicker = true
                },
                onDismiss = { showBrandPicker = false }
            )
        }

        // Compact Model Picker Dialog (filtered by selected brand)
        if (showModelPicker) {
            val modelsList = VehicleCatalogue.findModels(brand).map { it.name }
            VehiclePickerDialog(
                title = "Select $brand Model",
                items = modelsList,
                selectedItem = model,
                onSelect = { selectedModel ->
                    model = selectedModel
                    variant = ""
                    // If default fuel type is available, pre-select it
                    val modelObj = VehicleCatalogue.findModels(brand).firstOrNull { it.name.equals(selectedModel, ignoreCase = true) }
                    if (modelObj != null) {
                        fuelType = modelObj.defaultFuelType
                        if (modelObj.variants.isNotEmpty()) {
                            showVariantPicker = true
                        }
                    }
                },
                onDismiss = { showModelPicker = false }
            )
        }

        // Compact Variant Picker Dialog (filtered by selected model)
        if (showVariantPicker) {
            val variantsList = VehicleCatalogue.findVariants(brand, model)
            VehiclePickerDialog(
                title = "Select $model Variant",
                items = variantsList,
                selectedItem = variant,
                onSelect = { selectedVariant ->
                    variant = selectedVariant
                },
                onDismiss = { showVariantPicker = false }
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = RaahiTextDim,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SelectableField(
    value: String,
    placeholder: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(elevation = 1.dp, shape = RaahiShapeSmall, spotColor = Color(0x0C000000))
            .background(if (enabled) Color.White else Color(0xFFF8F5EE), RaahiShapeSmall)
            .border(
                1.dp,
                if (value.isNotBlank()) RaahiOrange else RaahiBorder,
                RaahiShapeSmall
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (value.isNotBlank()) RaahiOrange else RaahiTextFaint,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value.ifBlank { placeholder },
            color = if (value.isNotBlank()) RaahiText else RaahiTextFaint,
            fontSize = 14.sp,
            fontWeight = if (value.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Dropdown",
            tint = RaahiTextFaint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun RaahiLightTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RaahiShapeSmall, spotColor = Color(0x0C000000)),
        placeholder = { Text(placeholder, color = RaahiTextFaint, fontSize = 14.sp) },
        leadingIcon = if (icon != null) {
            { Icon(icon, contentDescription = null, tint = RaahiTextFaint, modifier = Modifier.size(19.dp)) }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RaahiShapeSmall,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = RaahiText,
            unfocusedTextColor = RaahiText,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = RaahiOrange,
            unfocusedBorderColor = RaahiBorder,
            cursorColor = RaahiOrange,
        ),
    )
}

@Composable
private fun FuelTypeSelector(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FUEL_TYPES.forEach { type ->
            val active = type == selected
            Box(
                modifier = Modifier
                    .then(
                        if (active) Modifier.shadow(elevation = 4.dp, shape = RaahiShapePill, spotColor = RaahiOrange)
                        else Modifier
                    )
                    .background(if (active) RaahiOrange else Color.White, RaahiShapePill)
                    .border(1.dp, if (active) RaahiOrange else RaahiBorder, RaahiShapePill)
                    .clickable { onSelect(type) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = type,
                    color = if (active) Color.White else RaahiTextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
