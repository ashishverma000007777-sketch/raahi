package `in`.raahi.app.ui.screens.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.network.VehicleDto
import `in`.raahi.app.ui.components.*
import `in`.raahi.app.ui.theme.*

@Composable
fun MyGarageScreen(
    onBack: () -> Unit,
    onAddVehicle: () -> Unit,
    onEditVehicle: (String) -> Unit,
    onViewCarHealth: (String) -> Unit,
    viewModel: MyGarageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var vehicleToRemove by remember { mutableStateOf<VehicleDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = RaahiBg) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(RaahiGlass)
                            .border(1.dp, RaahiBorderSoft, CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = RaahiText,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "My Garage",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RaahiDisplayFont,
                            color = RaahiText,
                        )
                        Text(
                            text = "Manage your vehicles & records",
                            fontSize = 11.5.sp,
                            color = RaahiTextDim,
                        )
                    }
                }

                // Add Vehicle button in header
                Row(
                    modifier = Modifier
                        .clip(RaahiShapePill)
                        .background(RaahiOrange.copy(alpha = 0.12f))
                        .border(1.dp, RaahiOrange.copy(alpha = 0.35f), RaahiShapePill)
                        .clickable(onClick = onAddVehicle)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = RaahiOrange,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Add",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = RaahiOrange,
                    )
                }
            }

            when (val s = state) {
                is MyGarageState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RaahiOrange)
                    }
                }
                is MyGarageState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = RaahiRed,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = s.message,
                                color = RaahiTextDim,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                            )
                            Spacer(Modifier.height(16.dp))
                            RaahiOutlineButton(text = "Try Again", onClick = { viewModel.load() })
                        }
                    }
                }
                is MyGarageState.Success -> {
                    if (s.vehicles.isEmpty()) {
                        EmptyGarageView(onAddVehicle = onAddVehicle)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                        ) {
                            items(s.vehicles, key = { it.id }) { vehicle ->
                                val isDefault = vehicle.id == s.activeVehicleId
                                VehicleCard(
                                    vehicle = vehicle,
                                    isDefault = isDefault,
                                    onSetDefault = { viewModel.setDefault(vehicle.id) },
                                    onEdit = { onEditVehicle(vehicle.id) },
                                    onRemove = { vehicleToRemove = vehicle },
                                    onViewHealth = { onViewCarHealth(vehicle.id) },
                                )
                            }

                            item {
                                Spacer(Modifier.height(4.dp))
                                RaahiOutlineButton(
                                    text = "+ Add Another Vehicle",
                                    onClick = onAddVehicle,
                                    tint = RaahiOrange,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for vehicle removal
    vehicleToRemove?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { vehicleToRemove = null },
            containerColor = RaahiBg2,
            title = {
                Text(
                    text = "Remove vehicle?",
                    color = RaahiText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove ${vehicle.brand} ${vehicle.model} (${vehicle.registrationNumber}) from your garage?",
                    color = RaahiTextDim,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = vehicle.id
                        vehicleToRemove = null
                        viewModel.removeVehicle(id)
                    }
                ) {
                    Text("Remove", color = RaahiRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToRemove = null }) {
                    Text("Cancel", color = RaahiTextDim)
                }
            },
        )
    }
}

@Composable
private fun VehicleCard(
    vehicle: VehicleDto,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onViewHealth: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Brand & Model, Default badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(RaahiOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DirectionsCar,
                            contentDescription = null,
                            tint = RaahiOrange,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${vehicle.brand} ${vehicle.model}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = RaahiText,
                            fontFamily = RaahiDisplayFont,
                        )
                        val variantText = buildString {
                            if (!vehicle.variant.isNullOrBlank()) append(vehicle.variant)
                            if (vehicle.modelYear > 0) {
                                if (isNotEmpty()) append(" · ")
                                append(vehicle.modelYear)
                            }
                        }
                        if (variantText.isNotBlank()) {
                            Text(
                                text = variantText,
                                fontSize = 12.sp,
                                color = RaahiTextDim,
                            )
                        }
                    }
                }

                if (isDefault) {
                    Row(
                        modifier = Modifier
                            .clip(RaahiShapePill)
                            .background(RaahiGreen.copy(alpha = 0.14f))
                            .border(1.dp, RaahiGreen.copy(alpha = 0.35f), RaahiShapePill)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = RaahiGreen,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "DEFAULT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RaahiGreen,
                            letterSpacing = 0.5.sp,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RaahiShapePill)
                            .background(RaahiGlass)
                            .border(1.dp, RaahiBorderSoft, RaahiShapePill)
                            .clickable(onClick = onSetDefault)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Set Default",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RaahiOrange,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Indian License Plate Badge
            IndianLicensePlate(registrationNumber = vehicle.registrationNumber)

            Spacer(Modifier.height(14.dp))

            // 2x2 Specs Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SpecBox(
                    icon = Icons.Outlined.Speed,
                    label = "Odometer",
                    value = "${"%,d".format(vehicle.odometerKm)} km",
                    modifier = Modifier.weight(1f),
                )
                SpecBox(
                    icon = Icons.Outlined.LocalGasStation,
                    label = "Fuel Type",
                    value = vehicle.fuelType.replaceFirstChar { it.uppercase() },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SpecBox(
                    icon = Icons.Outlined.Build,
                    label = "Last Service",
                    value = vehicle.lastServiceDate ?: "Not recorded",
                    modifier = Modifier.weight(1f),
                )
                SpecBox(
                    icon = Icons.Outlined.VerifiedUser,
                    label = "Insurance",
                    value = vehicle.insuranceExpiry?.let { "Valid: $it" } ?: "Not recorded",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RaahiBorderSoft)
            )

            Spacer(Modifier.height(12.dp))

            // Actions: Edit Details, Remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        tint = RaahiTextDim,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Edit Details",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RaahiText,
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onRemove)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove",
                        tint = RaahiRed,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Remove",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RaahiRed,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Full width: View Car Health & Service History
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RaahiOrange.copy(alpha = 0.10f))
                    .border(1.dp, RaahiOrange.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onViewHealth)
                    .padding(vertical = 12.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.HealthAndSafety,
                    contentDescription = null,
                    tint = RaahiOrange,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "View Car Health & Service History",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RaahiOrange,
                )
            }
        }
    }
}

@Composable
private fun IndianLicensePlate(
    registrationNumber: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Blue left strip for "IND"
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp)
                    .background(Color(0xFF003399), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00BFFF)),
                    )
                    Text(
                        text = "IND",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Text(
                text = registrationNumber.uppercase().ifBlank { "NOT REGISTERED" },
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                letterSpacing = 2.5.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun SpecBox(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(RaahiBg2)
            .border(1.dp, RaahiBorderSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RaahiOrange,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 10.5.sp,
                    color = RaahiTextDim,
                )
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RaahiText,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun EmptyGarageView(onAddVehicle: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(RaahiOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = RaahiOrange,
                    modifier = Modifier.size(42.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "No vehicles saved yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = RaahiText,
                fontFamily = RaahiDisplayFont,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Add your car to track service history, car health, and get faster roadside assistance.",
                fontSize = 13.sp,
                color = RaahiTextDim,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(24.dp))
            RaahiPrimaryButton(
                text = "+ Add Your Vehicle",
                onClick = onAddVehicle,
                modifier = Modifier.fillMaxWidth(0.8f),
            )
        }
    }
}
