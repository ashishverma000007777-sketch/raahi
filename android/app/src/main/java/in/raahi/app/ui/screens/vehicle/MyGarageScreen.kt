package `in`.raahi.app.ui.screens.vehicle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.network.VehicleDto
import `in`.raahi.app.ui.components.*
import `in`.raahi.app.ui.theme.*

// ---------------------------------------------------------------------------------------
// Reference-Derived OEM Automotive Design Palette
// ---------------------------------------------------------------------------------------
private val CharcoalHeader = Color(0xFF16181D)
private val HeaderPill = Color(0xFF24272E)
private val HeaderPillBorder = Color(0xFF32363E)
private val ScreenCanvasBg = Color(0xFFF5F6FA)
private val CardSurface = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFE4E7EB)
private val TextPrimary = Color(0xFF16181D)
private val TextSecondary = Color(0xFF707780)
private val TextMuted = Color(0xFF8A929A)
private val TelemetryBg = Color(0xFFF6F7F9)
private val TelemetryBorder = Color(0xFFEBECEF)
private val BrandOrange = Color(0xFFF57C00)
private val DangerRed = Color(0xFFD94343)
private val SuccessGreen = Color(0xFF238765)

@Composable
fun MyGarageScreen(
    onBack: () -> Unit,
    onAddVehicle: () -> Unit,
    onEditVehicle: (String) -> Unit,
    onViewCarHealth: (String) -> Unit,
    onNavigateTab: ((RaahiTab) -> Unit)? = null,
    onSosClick: (() -> Unit)? = null,
    viewModel: MyGarageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var vehicleToRemove by remember { mutableStateOf<VehicleDto?>(null) }
    var selectedVehicleId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val vehicleCount = (state as? MyGarageState.Success)?.vehicles?.size ?: 0

    // Synchronize selected vehicle
    val successState = state as? MyGarageState.Success
    val currentVehicle = remember(successState, selectedVehicleId) {
        successState?.let { s ->
            s.vehicles.firstOrNull { it.id == selectedVehicleId }
                ?: s.vehicles.firstOrNull { it.id == s.activeVehicleId }
                ?: s.vehicles.firstOrNull()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CharcoalHeader
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ---------------------------------------------------------------------------
                // 1. Unified Charcoal Automotive Header
                // ---------------------------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Left: Back button + Title & Vehicle Count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "My Garage",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.2.sp
                                )
                                if (vehicleCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0x25FFFFFF),
                                        border = BorderStroke(1.dp, Color(0x30FFFFFF))
                                    ) {
                                        Text(
                                            text = "$vehicleCount",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (vehicleCount == 1) "1 vehicle saved" else "$vehicleCount vehicles saved",
                                fontSize = 11.5.sp,
                                color = Color(0xFF9EA3AE),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    // Right: Sleek dark squircle "+ Add" action button
                    IconButton(
                        onClick = onAddVehicle,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HeaderPill)
                            .border(1.dp, HeaderPillBorder, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add vehicle",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // ---------------------------------------------------------------------------
                // 2. Cohesive Automotive Sheet Surface
                // ---------------------------------------------------------------------------
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = ScreenCanvasBg,
                    shadowElevation = 4.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (val s = state) {
                            is MyGarageState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = BrandOrange)
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
                                            tint = DangerRed,
                                            modifier = Modifier.size(44.dp),
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = s.message,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.5.sp,
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Button(
                                            onClick = { viewModel.load() },
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(44.dp)
                                        ) {
                                            Text("Try Again", color = Color.White, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                            is MyGarageState.Success -> {
                                if (s.vehicles.isEmpty()) {
                                    EmptyGarageView(onAddVehicle = onAddVehicle)
                                } else if (currentVehicle != null) {
                                    val isDefault = currentVehicle.id == s.activeVehicleId

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(
                                            top = 6.dp,
                                            bottom = if (onNavigateTab != null) 136.dp else 36.dp
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // A. Subtle Top Accent Drag Bar
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 6.dp, bottom = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 36.dp, height = 4.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(Color(0xFFD4D8DE))
                                                )
                                            }
                                        }

                                        // B. Multi-Vehicle Horizontal Selector (When > 1 vehicle)
                                        if (s.vehicles.size > 1) {
                                            item {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 16.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "SAVED VEHICLES",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextMuted,
                                                            letterSpacing = 0.8.sp
                                                        )
                                                        Text(
                                                            text = "${s.vehicles.size} vehicles",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = TextSecondary
                                                        )
                                                    }

                                                    LazyRow(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        itemsIndexed(s.vehicles, key = { _, v -> v.id }) { _, v ->
                                                            val isSelected = v.id == currentVehicle.id
                                                            VehicleSelectorCard(
                                                                vehicle = v,
                                                                isSelected = isSelected,
                                                                isDefault = v.id == s.activeVehicleId,
                                                                onClick = { selectedVehicleId = v.id }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // C. Primary Elevated Vehicle Showcase Card
                                        item {
                                            VehicleShowcaseCard(
                                                vehicle = currentVehicle,
                                                isDefault = isDefault,
                                                onEdit = { onEditVehicle(currentVehicle.id) },
                                                onRemove = { vehicleToRemove = currentVehicle },
                                                onHealthAndHistory = { onViewCarHealth(currentVehicle.id) },
                                                onSetDefault = { viewModel.setDefault(currentVehicle.id) },
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }

                                        // D. Add Another Vehicle Action
                                        item {
                                            OutlinedButton(
                                                onClick = onAddVehicle,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 2.dp)
                                                    .height(44.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, Color(0xFFD6DAE0)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = Color.White.copy(alpha = 0.8f),
                                                    contentColor = TextPrimary
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = BrandOrange
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = "Add Another Vehicle",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Navigation Bar
                if (onNavigateTab != null) {
                    RaahiBottomNavBar(current = RaahiTab.GARAGE, onSelect = onNavigateTab)
                }
            }

            // Floating SOS Button
            if (onSosClick != null) {
                RaahiFloatingSosButton(
                    onSos = onSosClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = if (onNavigateTab != null) 86.dp else 24.dp)
                )
            }
        }
    }

    // Confirmation dialog for vehicle removal
    vehicleToRemove?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { vehicleToRemove = null },
            containerColor = CardSurface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Remove Vehicle?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove ${vehicle.brand} ${vehicle.model} (${vehicle.registrationNumber}) from your garage?",
                    color = TextSecondary,
                    fontSize = 13.5.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = vehicle.id
                        vehicleToRemove = null
                        viewModel.removeVehicle(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Remove", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { vehicleToRemove = null },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            },
        )
    }
}



// ---------------------------------------------------------------------------------------
// Empty Garage View
// ---------------------------------------------------------------------------------------
@Composable
private fun EmptyGarageView(onAddVehicle: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF3E0))
                    .border(1.dp, Color(0xFFFFE0B2), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Your Garage is Empty",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Add your car or motorcycle to track odometer, car health, and service history.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onAddVehicle,
                colors = ButtonDefaults.buttonColors(containerColor = CharcoalHeader),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(46.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Add Your First Vehicle",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
