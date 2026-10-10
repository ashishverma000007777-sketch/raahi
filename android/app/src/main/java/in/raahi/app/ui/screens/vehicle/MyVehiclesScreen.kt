package `in`.raahi.app.ui.screens.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
fun MyVehiclesScreen(
    onBack: () -> Unit,
    onAddVehicle: () -> Unit,
    onEditVehicle: () -> Unit,
    onViewHealth: () -> Unit = {},
    viewModel: MyVehiclesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = RaahiBg) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = RaahiText)
                }
                Text(
                    "My Garage",
                    color = RaahiText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RaahiDisplayFont,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = viewModel::loadVehicle) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = RaahiTextDim)
                }
            }

            when (val s = state) {
                is MyVehiclesUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RaahiOrange)
                    }
                }
                is MyVehiclesUiState.Error -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = RaahiRed, modifier = Modifier.size(44.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(s.message, color = RaahiTextDim, textAlign = TextAlign.Center, fontSize = 13.5.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = viewModel::loadVehicle,
                                colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is MyVehiclesUiState.Success -> {
                    val vehicle = s.vehicle
                    if (vehicle == null) {
                        NoVehiclesView(onAddVehicle = onAddVehicle)
                    } else {
                        VehicleDetailsView(
                            vehicle = vehicle,
                            onEdit = onEditVehicle,
                            onDelete = { showDeleteConfirmDialog = true },
                            onViewHealth = onViewHealth
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirmDialog = false },
            title = {
                Text("Remove Vehicle?", color = RaahiText, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to remove your vehicle? This will delete your maintenance logs and health stats from your profile.",
                    color = RaahiTextDim,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVehicle {
                            showDeleteConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiRed),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Remove")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel", color = RaahiText)
                }
            },
            containerColor = RaahiBg2,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun NoVehiclesView(onAddVehicle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(RaahiOrange.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "No vehicle in your garage",
            color = RaahiText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = RaahiDisplayFont
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Add your car to track Car Health score, service intervals, fuel logs, and get roadside assistance without entering details repeatedly.",
            color = RaahiTextDim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAddVehicle,
            colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Your Vehicle", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun VehicleDetailsView(
    vehicle: VehicleDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewHealth: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PRIMARY VEHICLE", color = RaahiTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Box(
                modifier = Modifier
                    .background(RaahiGreen.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, RaahiGreen.copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(RaahiGreen, CircleShape))
                    Spacer(Modifier.width(5.dp))
                    Text("ACTIVE / DEFAULT", color = RaahiGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Vehicle Main Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = Color(0x22000000)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = RaahiBg2),
            border = androidx.compose.foundation.BorderStroke(1.dp, RaahiBorderSoft)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(RaahiOrange.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${vehicle.brand} ${vehicle.model}",
                            color = RaahiText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RaahiDisplayFont
                        )
                        if (!vehicle.variant.isNullOrBlank() || vehicle.modelYear != null) {
                            Text(
                                listOfNotNull(vehicle.variant, vehicle.modelYear?.toString()).joinToString(" · "),
                                color = RaahiTextDim,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // License Plate Simulation (Indian standard plate styling)
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFBFBFB), RoundedCornerShape(6.dp))
                        .border(1.5.dp, Color(0xFF222222), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(8.dp).background(Color(0xFF003399), CircleShape))
                            Text("IND", color = Color(0xFF003399), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            vehicle.registrationNumber,
                            color = Color(0xFF111111),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = RaahiBorderSoft, thickness = 0.8.dp)
                Spacer(Modifier.height(14.dp))

                // Specifications Grid
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        SpecItem(label = "Odometer", value = "${vehicle.odometerKm} km", icon = Icons.Outlined.Speed, modifier = Modifier.weight(1f))
                        SpecItem(label = "Fuel Type", value = vehicle.fuelType ?: "—", icon = Icons.Outlined.LocalGasStation, modifier = Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth()) {
                        SpecItem(
                            label = "Last Service",
                            value = vehicle.lastServiceDate ?: "Not logged",
                            icon = Icons.Outlined.Build,
                            modifier = Modifier.weight(1f)
                        )
                        SpecItem(
                            label = "Insurance Expiry",
                            value = vehicle.insuranceExpiry ?: "Not set",
                            icon = Icons.Outlined.Security,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (vehicle.pucExpiry != null) {
                        Row(Modifier.fillMaxWidth()) {
                            SpecItem(
                                label = "PUC Expiry",
                                value = vehicle.pucExpiry,
                                icon = Icons.Outlined.VerifiedUser,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RaahiText),
                border = androidx.compose.foundation.BorderStroke(1.dp, RaahiBorder)
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit Details", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(0.9f).height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RaahiRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, RaahiRed.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = RaahiRed)
                Spacer(Modifier.width(6.dp))
                Text("Remove", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = RaahiRed)
            }
        }

        // View Car Health Button
        Button(
            onClick = onViewHealth,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange)
        ) {
            Icon(Icons.Outlined.HealthAndSafety, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("View Car Health & Service History", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        }
    }
}

@Composable
private fun SpecItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = RaahiCyan, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = RaahiTextDim, fontSize = 10.sp)
            Text(value, color = RaahiText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
