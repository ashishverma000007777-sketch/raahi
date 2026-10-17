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
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp),
                                                shape = RoundedCornerShape(20.dp),
                                                color = CardSurface,
                                                border = BorderStroke(1.dp, CardBorder),
                                                shadowElevation = 2.dp
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                                ) {
                                                    // 1. Vehicle Identity & Default Badge Header
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        ) {
                                                            Text(
                                                                text = "${currentVehicle.brand} ${currentVehicle.model}",
                                                                fontSize = 20.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = TextPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )

                                                            // Compact "✓ DEFAULT" Status Badge (Non-clickable)
                                                            if (isDefault) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = Color(0xFFE8F5E9),
                                                                    border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                                                                ) {
                                                                    Row(
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Check,
                                                                            contentDescription = null,
                                                                            tint = Color(0xFF2E7D32),
                                                                            modifier = Modifier.size(11.dp)
                                                                        )
                                                                        Text(
                                                                            text = "DEFAULT",
                                                                            fontSize = 9.5.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = Color(0xFF1B5E20),
                                                                            letterSpacing = 0.4.sp
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        // Set Default Action Button for non-default vehicle
                                                        if (!isDefault) {
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = Color(0xFFF1F3F5),
                                                                border = BorderStroke(1.dp, Color(0xFFDFE2E6)),
                                                                modifier = Modifier.clickable {
                                                                    viewModel.setDefault(currentVehicle.id)
                                                                }
                                                            ) {
                                                                Text(
                                                                    text = "Set as default",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = TextPrimary,
                                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // Subtitle: Year · Fuel Type · Variant
                                                    val subtitle = buildString {
                                                        if (currentVehicle.modelYear > 0) append(currentVehicle.modelYear)
                                                        if (isNotEmpty()) append(" · ")
                                                        append(currentVehicle.fuelType.uppercase())
                                                        if (!currentVehicle.variant.isNullOrBlank()) {
                                                            append(" · ")
                                                            append(currentVehicle.variant)
                                                        }
                                                    }
                                                    Text(
                                                        text = subtitle,
                                                        fontSize = 12.5.sp,
                                                        color = TextSecondary,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    // 2. Automotive Graphic & Aligned Registration Plate
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        PremiumVehicleGraphic(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(112.dp)
                                                        )

                                                        IndianLicensePlate(
                                                            registrationNumber = currentVehicle.registrationNumber,
                                                            modifier = Modifier.widthIn(min = 180.dp, max = 220.dp)
                                                        )
                                                    }

                                                    // 3. Balanced 2x2 Telemetry Grid
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            AutomotiveTelemetryTile(
                                                                label = "ODOMETER",
                                                                value = if (currentVehicle.odometerKm > 0) "%,d".format(currentVehicle.odometerKm) else "0",
                                                                unit = "KM",
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            AutomotiveTelemetryTile(
                                                                label = "FUEL TYPE",
                                                                value = currentVehicle.fuelType.replaceFirstChar { it.uppercase() },
                                                                unit = "",
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            AutomotiveTelemetryTile(
                                                                label = "LAST SERVICE",
                                                                value = currentVehicle.lastServiceDate?.takeIf { it.isNotBlank() } ?: "Not recorded",
                                                                unit = "",
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            AutomotiveTelemetryTile(
                                                                label = "INSURANCE",
                                                                value = currentVehicle.insuranceExpiry?.takeIf { it.isNotBlank() } ?: "Not recorded",
                                                                unit = "",
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                    }

                                                    // 4. Proportional Action Buttons (Consistent Height 42dp & Weight)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Edit Button
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = Color(0xFFF4F5F7),
                                                            border = BorderStroke(1.dp, Color(0xFFE2E5E8)),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(42.dp)
                                                                .clickable { onEditVehicle(currentVehicle.id) }
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxSize(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.Edit,
                                                                    contentDescription = "Edit",
                                                                    tint = TextPrimary,
                                                                    modifier = Modifier.size(15.dp)
                                                                )
                                                                Spacer(Modifier.width(4.dp))
                                                                Text(
                                                                    text = "Edit",
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = TextPrimary
                                                                )
                                                            }
                                                        }

                                                        // Remove Button
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = Color(0xFFFFF3F3),
                                                            border = BorderStroke(1.dp, Color(0xFFFFD6D6)),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(42.dp)
                                                                .clickable { vehicleToRemove = currentVehicle }
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxSize(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.DeleteOutline,
                                                                    contentDescription = "Remove",
                                                                    tint = DangerRed,
                                                                    modifier = Modifier.size(15.dp)
                                                                )
                                                                Spacer(Modifier.width(4.dp))
                                                                Text(
                                                                    text = "Remove",
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = DangerRed
                                                                )
                                                            }
                                                        }

                                                        // Primary Action: Health & History
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = CharcoalHeader,
                                                            modifier = Modifier
                                                                .weight(1.6f)
                                                                .height(42.dp)
                                                                .clickable { onViewCarHealth(currentVehicle.id) }
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .padding(horizontal = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.Center
                                                            ) {
                                                                Text(
                                                                    text = "Health & History",
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White
                                                                )
                                                                Spacer(Modifier.width(4.dp))
                                                                Icon(
                                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                                    contentDescription = null,
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
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
// Telemetry Metric Tile
// ---------------------------------------------------------------------------------------
@Composable
private fun AutomotiveTelemetryTile(
    label: String,
    value: String,
    unit: String = "",
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = TelemetryBg,
        border = BorderStroke(1.dp, TelemetryBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.6.sp,
                maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = value,
                    fontSize = if (value == "Not recorded" || value.length > 10) 13.sp else 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (value == "Not recorded") TextSecondary else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (unit.isNotBlank()) {
                    Text(
                        text = unit,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Multi-Vehicle Selector Card
// ---------------------------------------------------------------------------------------
@Composable
private fun VehicleSelectorCard(
    vehicle: VehicleDto,
    isSelected: Boolean,
    isDefault: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(160.dp)
            .height(86.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) CharcoalHeader else Color.White,
        border = BorderStroke(
            1.dp,
            if (isSelected) CharcoalHeader else Color(0xFFE2E5E8)
        ),
        shadowElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${vehicle.brand} ${vehicle.model}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (vehicle.modelYear > 0) "${vehicle.modelYear} · ${vehicle.fuelType.uppercase()}" else vehicle.fuelType.uppercase(),
                        fontSize = 10.sp,
                        color = if (isSelected) Color(0xFF9EA3AE) else TextSecondary,
                        maxLines = 1,
                    )
                }
                if (isDefault) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) Color(0x33238765) else Color(0xFFE8F5E9),
                        border = BorderStroke(0.5.dp, if (isSelected) Color(0xFF238765) else Color(0xFFC8E6C9))
                    ) {
                        Text(
                            text = "DEFAULT",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color(0xFF4ADE80) else Color(0xFF1B5E20),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = vehicle.registrationNumber.uppercase(),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFFDFE2E6) else Color(0xFF475569),
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------------------
// Indian High-Security Registration Plate (HSRP)
// ---------------------------------------------------------------------------------------
@Composable
private fun IndianLicensePlate(
    registrationNumber: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(7.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(32.dp)
                .padding(end = 8.dp),
        ) {
            // Blue left strip for "IND"
            Surface(
                modifier = Modifier
                    .width(30.dp)
                    .fillMaxHeight(),
                color = Color(0xFF003399),
                shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00BFFF)),
                    )
                    Text(
                        text = "IND",
                        color = Color.White,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            // Plate Registration Number
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = registrationNumber.uppercase().ifBlank { "NOT REGISTERED" },
                    color = Color(0xFF0F172A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Bespoke Automotive Perspective Graphic (Matches Front-View Stance in Reference)
// ---------------------------------------------------------------------------------------
@Composable
private fun PremiumVehicleGraphic(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Soft Ambient Ground Shadow
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x38000000),
                        Color(0x15000000),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.50f, h * 0.90f),
                    radius = w * 0.46f
                ),
                topLeft = Offset(w * 0.04f, h * 0.83f),
                size = Size(w * 0.92f, h * 0.14f)
            )

            // 2. Wide Low-Profile Front Tires
            drawRoundRect(
                color = Color(0xFF1E2126),
                topLeft = Offset(w * 0.12f, h * 0.65f),
                size = Size(w * 0.12f, h * 0.24f),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF1E2126),
                topLeft = Offset(w * 0.76f, h * 0.65f),
                size = Size(w * 0.12f, h * 0.24f),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // 3. Aerodynamic Coupe Roof & Windshield
            val cabinPath = Path().apply {
                moveTo(w * 0.28f, h * 0.44f)
                cubicTo(w * 0.32f, h * 0.18f, w * 0.40f, h * 0.14f, w * 0.50f, h * 0.14f)
                cubicTo(w * 0.60f, h * 0.14f, w * 0.68f, h * 0.18f, w * 0.72f, h * 0.44f)
                close()
            }
            drawPath(
                path = cabinPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1C2026), Color(0xFF2C323D))
                )
            )

            // Windshield Tint Sheen
            val glassReflection = Path().apply {
                moveTo(w * 0.34f, h * 0.42f)
                lineTo(w * 0.42f, h * 0.19f)
                lineTo(w * 0.48f, h * 0.19f)
                lineTo(w * 0.40f, h * 0.42f)
                close()
            }
            drawPath(path = glassReflection, color = Color(0x24FFFFFF))

            // 4. Side Mirrors
            drawRoundRect(
                color = Color(0xFF8E95A0),
                topLeft = Offset(w * 0.14f, h * 0.42f),
                size = Size(w * 0.08f, h * 0.045f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF8E95A0),
                topLeft = Offset(w * 0.78f, h * 0.42f),
                size = Size(w * 0.08f, h * 0.045f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // 5. Main Metallic Vehicle Body Shell
            val bodyPath = Path().apply {
                moveTo(w * 0.10f, h * 0.78f)
                cubicTo(w * 0.12f, h * 0.52f, w * 0.18f, h * 0.46f, w * 0.30f, h * 0.44f)
                lineTo(w * 0.70f, h * 0.44f)
                cubicTo(w * 0.82f, h * 0.46f, w * 0.88f, h * 0.52f, w * 0.90f, h * 0.78f)
                cubicTo(w * 0.78f, h * 0.84f, w * 0.22f, h * 0.84f, w * 0.10f, h * 0.78f)
                close()
            }
            drawPath(
                path = bodyPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8ECEF),
                        Color(0xFFD0D7DE),
                        Color(0xFFB4BAC3),
                        Color(0xFF9BA2AD)
                    )
                )
            )

            // Subtle Hood Sculpt Lines
            drawLine(
                color = Color(0x35000000),
                start = Offset(w * 0.33f, h * 0.44f),
                end = Offset(w * 0.36f, h * 0.60f),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color(0x35000000),
                start = Offset(w * 0.67f, h * 0.44f),
                end = Offset(w * 0.64f, h * 0.60f),
                strokeWidth = 1.5.dp.toPx()
            )

            // 6. Sculpted Front Grille
            drawRoundRect(
                color = Color(0xFF1B1D22),
                topLeft = Offset(w * 0.32f, h * 0.58f),
                size = Size(w * 0.36f, h * 0.16f),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Grille Louvers
            for (i in 1..3) {
                val yLine = h * 0.58f + (h * 0.16f / 4f) * i
                drawLine(
                    color = Color(0xFF2E333D),
                    start = Offset(w * 0.34f, yLine),
                    end = Offset(w * 0.66f, yLine),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Emblem on Grille
            drawCircle(
                color = BrandOrange,
                radius = 3.5.dp.toPx(),
                center = Offset(w * 0.50f, h * 0.66f)
            )

            // 7. High-Tech Projector LED Headlights with Glowing Signature
            // Left Headlight
            val leftLightPath = Path().apply {
                moveTo(w * 0.16f, h * 0.58f)
                lineTo(w * 0.30f, h * 0.60f)
                lineTo(w * 0.28f, h * 0.65f)
                lineTo(w * 0.15f, h * 0.63f)
                close()
            }
            drawPath(
                path = leftLightPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFFE0F2FE))
                )
            )
            drawPath(
                path = leftLightPath,
                color = Color(0xFF0284C7),
                style = Stroke(width = 1.dp.toPx())
            )

            // Right Headlight
            val rightLightPath = Path().apply {
                moveTo(w * 0.84f, h * 0.58f)
                lineTo(w * 0.70f, h * 0.60f)
                lineTo(w * 0.72f, h * 0.65f)
                lineTo(w * 0.85f, h * 0.63f)
                close()
            }
            drawPath(
                path = rightLightPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFE0F2FE), Color(0xFF38BDF8))
                )
            )
            drawPath(
                path = rightLightPath,
                color = Color(0xFF0284C7),
                style = Stroke(width = 1.dp.toPx())
            )

            // 8. Lower Air Dam & Splitter
            drawRoundRect(
                color = Color(0xFF14161A),
                topLeft = Offset(w * 0.24f, h * 0.77f),
                size = Size(w * 0.52f, h * 0.05f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
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
