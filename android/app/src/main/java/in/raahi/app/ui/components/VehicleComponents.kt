package `in`.raahi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.raahi.app.R
import `in`.raahi.app.network.VehicleDto
import `in`.raahi.app.ui.theme.*

// ---------------------------------------------------------------------------------------
// Unified Automotive Design System Tokens (Light #F5F6F7 Canvas, White Cards #FFFFFF)
// ---------------------------------------------------------------------------------------
private val VehicleCardBgLight = Color(0xFFFFFFFF)
private val VehicleCardBgDark = Color(0xFF222629)
private val VehicleBorderLight = Color(0xFFE2E5E8)
private val VehicleBorderDark = Color(0xFF2E3338)

private val VehicleTelemetryBgLight = Color(0xFFF6F7F9)
private val VehicleTelemetryBgDark = Color(0xFF1B1E22)
private val VehicleTelemetryBorderLight = Color(0xFFEBECEF)
private val VehicleTelemetryBorderDark = Color(0xFF2C3036)

private val VehicleTextPrimaryLight = Color(0xFF25282D)
private val VehicleTextPrimaryDark = Color(0xFFF4F6F7)
private val VehicleTextSecondaryLight = Color(0xFF707780)
private val VehicleTextSecondaryDark = Color(0xFFAAB2B8)
private val VehicleTextMuted = Color(0xFF8A929A)

private val VehicleBrandOrange = Color(0xFFF57C00)
private val VehicleButtonOrange = Color(0xFFEF6C00)
private val VehicleSuccessGreen = Color(0xFF238765)
private val VehicleHsrpBlue = Color(0xFF003399)

// ---------------------------------------------------------------------------------------
// 1. Vehicle Illustration Component
// ---------------------------------------------------------------------------------------
enum class VehicleIllustrationSize(val height: Dp) {
    Hero(138.dp),
    Standard(96.dp),
    Compact(60.dp),
    Thumbnail(42.dp)
}

/**
 * Professional, modern automotive illustration of the Hyundai Creta front view.
 * Features realistic SUV proportions, front grille, split LED signature, bumper skid plate,
 * wheels/tires, and transparent ground shadow.
 */
@Composable
fun VehicleIllustration(
    modifier: Modifier = Modifier,
    size: VehicleIllustrationSize = VehicleIllustrationSize.Hero,
    vehicleBrand: String? = null,
    vehicleModel: String? = null,
    contentDescription: String? = null,
) {
    val description = contentDescription
        ?: listOfNotNull(vehicleBrand, vehicleModel, "Front View").joinToString(" ").ifBlank { "Vehicle Illustration" }

    // When brand/model is Creta or general car, we render the authentic front-view Creta illustration
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(size.height),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hyundai_creta_front),
            contentDescription = description,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
    }
}

// ---------------------------------------------------------------------------------------
// 2. Indian High-Security Registration Plate (HSRP)
// ---------------------------------------------------------------------------------------
enum class LicensePlateStyle {
    Standard, // Full embossed plate with blue IND block and hologram dot
    Compact   // Compact badge style for list rows & headers
}

/**
 * Standard Indian High-Security Registration Plate (HSRP) component.
 * Displays blue "IND" badge with hologram circle and authentic bold monospace plate text.
 */
@Composable
fun VehicleNumberPlate(
    registrationNumber: String,
    modifier: Modifier = Modifier,
    style: LicensePlateStyle = LicensePlateStyle.Standard,
) {
    val plateNumber = registrationNumber.trim().uppercase().ifBlank { "NOT REGISTERED" }

    when (style) {
        LicensePlateStyle.Standard -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(7.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                shadowElevation = 0.5.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(32.dp)
                        .padding(end = 10.dp),
                ) {
                    // Left Blue IND section
                    Surface(
                        modifier = Modifier
                            .width(32.dp)
                            .fillMaxHeight(),
                        color = VehicleHsrpBlue,
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
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "IND",
                                color = Color.White,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }

                    // Monospace Plate Text
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = plateNumber,
                            color = Color(0xFF0F172A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        LicensePlateStyle.Compact -> {
            val isDark = RaahiIsDark
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(6.dp),
                color = if (isDark) Color(0xFF222629) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2E3338) else Color(0xFFCBD5E1)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "IND",
                        color = VehicleHsrpBlue,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = plateNumber,
                        color = if (isDark) VehicleTextPrimaryDark else VehicleTextPrimaryLight,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// 3. Vehicle Status Badge
// ---------------------------------------------------------------------------------------
@Composable
fun VehicleStatusBadge(
    modifier: Modifier = Modifier,
    label: String = "DEFAULT",
    isDefault: Boolean = true,
    isDarkContext: Boolean = false,
) {
    if (isDarkContext) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(4.dp),
            color = Color(0x33238765),
            border = BorderStroke(0.5.dp, Color(0xFF238765))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isDefault) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(9.dp)
                    )
                }
                Text(
                    text = label,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4ADE80),
                    letterSpacing = 0.3.sp
                )
            }
        }
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFE8F5E9),
            border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (isDefault) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(11.dp)
                    )
                }
                Text(
                    text = label,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// 4. Vehicle Specification Row / Telemetry Tile
// ---------------------------------------------------------------------------------------
@Composable
fun VehicleSpecificationTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String = "",
    icon: ImageVector? = null,
    isDark: Boolean = RaahiIsDark,
) {
    val bg = if (isDark) VehicleTelemetryBgDark else VehicleTelemetryBgLight
    val border = if (isDark) VehicleTelemetryBorderDark else VehicleTelemetryBorderLight
    val textPrimary = if (isDark) VehicleTextPrimaryDark else VehicleTextPrimaryLight
    val textSecondary = if (isDark) VehicleTextSecondaryDark else VehicleTextSecondaryLight

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = VehicleTextMuted,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = VehicleTextMuted,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = value,
                    fontSize = if (value == "Not recorded" || value.length > 10) 13.sp else 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (value == "Not recorded") textSecondary else textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (unit.isNotBlank()) {
                    Text(
                        text = unit,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * 2x2 Telemetry Grid used in Showcase vehicle cards.
 */
@Composable
fun VehicleSpecificationGrid(
    odometerKm: Int,
    fuelType: String,
    lastServiceDate: String?,
    insuranceExpiry: String?,
    modifier: Modifier = Modifier,
    isDark: Boolean = RaahiIsDark,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VehicleSpecificationTile(
                label = "ODOMETER",
                value = if (odometerKm > 0) "%,d".format(odometerKm) else "0",
                unit = "KM",
                icon = Icons.Outlined.Speed,
                modifier = Modifier.weight(1f),
                isDark = isDark
            )
            VehicleSpecificationTile(
                label = "FUEL TYPE",
                value = fuelType.replaceFirstChar { it.uppercase() },
                unit = "",
                icon = Icons.Outlined.LocalGasStation,
                modifier = Modifier.weight(1f),
                isDark = isDark
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VehicleSpecificationTile(
                label = "LAST SERVICE",
                value = lastServiceDate?.takeIf { it.isNotBlank() } ?: "Not recorded",
                unit = "",
                icon = Icons.Outlined.Build,
                modifier = Modifier.weight(1f),
                isDark = isDark
            )
            VehicleSpecificationTile(
                label = "INSURANCE",
                value = insuranceExpiry?.takeIf { it.isNotBlank() } ?: "Not recorded",
                unit = "",
                icon = Icons.Outlined.VerifiedUser,
                modifier = Modifier.weight(1f),
                isDark = isDark
            )
        }
    }
}

// ---------------------------------------------------------------------------------------
// 5. Vehicle Showcase Card (Primary Garage Card Standard)
// ---------------------------------------------------------------------------------------
@Composable
fun VehicleShowcaseCard(
    vehicle: VehicleDto,
    isDefault: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onHealthAndHistory: () -> Unit,
    onSetDefault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = VehicleCardBgLight,
        border = BorderStroke(1.dp, VehicleBorderLight),
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
                        text = "${vehicle.brand} ${vehicle.model}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = VehicleTextPrimaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isDefault) {
                        VehicleStatusBadge(isDefault = true)
                    }
                }

                if (!isDefault) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F3F5),
                        border = BorderStroke(1.dp, Color(0xFFDFE2E6)),
                        modifier = Modifier.clickable(onClick = onSetDefault)
                    ) {
                        Text(
                            text = "Set as default",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VehicleTextPrimaryLight,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Subtitle: Year · Fuel Type · Variant
            val subtitle = buildString {
                if (vehicle.modelYear > 0) append(vehicle.modelYear)
                if (isNotEmpty()) append(" · ")
                append(vehicle.fuelType.uppercase())
                if (!vehicle.variant.isNullOrBlank()) {
                    append(" · ")
                    append(vehicle.variant)
                }
            }
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = VehicleTextSecondaryLight,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 2. Front Vehicle Illustration & Aligned HSRP Plate
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VehicleIllustration(
                    size = VehicleIllustrationSize.Hero,
                    vehicleBrand = vehicle.brand,
                    vehicleModel = vehicle.model,
                    contentDescription = "${vehicle.brand} ${vehicle.model} Front View"
                )

                VehicleNumberPlate(
                    registrationNumber = vehicle.registrationNumber,
                    modifier = Modifier.widthIn(min = 180.dp, max = 220.dp),
                    style = LicensePlateStyle.Standard
                )
            }

            // 3. Balanced 2x2 Telemetry Grid
            VehicleSpecificationGrid(
                odometerKm = vehicle.odometerKm,
                fuelType = vehicle.fuelType,
                lastServiceDate = vehicle.lastServiceDate,
                insuranceExpiry = vehicle.insuranceExpiry,
                isDark = false
            )

            // 4. Action Buttons (Consistent 42dp Height)
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
                        .clickable(onClick = onEdit)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = VehicleTextPrimaryLight,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Edit",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VehicleTextPrimaryLight
                        )
                    }
                }

                // Remove Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF5F5),
                    border = BorderStroke(1.dp, Color(0xFFFFDADA)),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clickable(onClick = onRemove)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Remove",
                            tint = Color(0xFFD94343),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Remove",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD94343)
                        )
                    }
                }

                // Health & History Primary Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF16181D),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(42.dp)
                        .clickable(onClick = onHealthAndHistory)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Health & History",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// 6. Multi-Vehicle Horizontal Selector Card
// ---------------------------------------------------------------------------------------
@Composable
fun VehicleSelectorCard(
    vehicle: VehicleDto,
    isSelected: Boolean,
    isDefault: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(160.dp)
            .height(88.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFF16181D) else Color.White,
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF16181D) else Color(0xFFE2E5E8)
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
                        color = if (isSelected) Color.White else VehicleTextPrimaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (vehicle.modelYear > 0) "${vehicle.modelYear} · ${vehicle.fuelType.uppercase()}" else vehicle.fuelType.uppercase(),
                        fontSize = 10.sp,
                        color = if (isSelected) Color(0xFF9EA3AE) else VehicleTextSecondaryLight,
                        maxLines = 1,
                    )
                }
                if (isDefault) {
                    VehicleStatusBadge(
                        label = "DEFAULT",
                        isDefault = true,
                        isDarkContext = isSelected
                    )
                }
            }

            Text(
                text = vehicle.registrationNumber.uppercase().ifBlank { "NOT REGISTERED" },
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
// 7. Compact Vehicle Card (For Home, Profile, Trip Plan, Diagnostics)
// ---------------------------------------------------------------------------------------
@Composable
fun CompactVehicleCard(
    vehicle: VehicleDto,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    badgeText: String? = "MY VEHICLE",
    showIllustration: Boolean = true,
    illustrationSize: VehicleIllustrationSize = VehicleIllustrationSize.Standard,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    additionalContent: @Composable (ColumnScope.() -> Unit)? = null,
) {
    val isDark = RaahiIsDark
    val bg = if (isDark) VehicleCardBgDark else VehicleCardBgLight
    val border = if (isDark) VehicleBorderDark else VehicleBorderLight
    val textPrimary = if (isDark) VehicleTextPrimaryDark else VehicleTextPrimaryLight
    val textSecondary = if (isDark) VehicleTextSecondaryDark else VehicleTextSecondaryLight
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(clickMod),
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Badge / Title + Registration Plate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    if (!badgeText.isNullOrBlank()) {
                        Text(
                            text = badgeText,
                            color = VehicleTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text = listOfNotNull(
                            vehicle.brand,
                            vehicle.model,
                            vehicle.variant.takeIf { !it.isNullOrBlank() }
                        ).joinToString(" "),
                        color = textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RaahiDisplayFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                VehicleNumberPlate(
                    registrationNumber = vehicle.registrationNumber,
                    style = LicensePlateStyle.Compact
                )
            }

            // Optional Illustration
            if (showIllustration) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VehicleIllustration(
                        size = illustrationSize,
                        vehicleBrand = vehicle.brand,
                        vehicleModel = vehicle.model,
                        contentDescription = "${vehicle.brand} ${vehicle.model}"
                    )
                }
            }

            // Additional custom content (e.g. spec bar, action links)
            if (additionalContent != null) {
                additionalContent()
            }

            // Optional footer action link
            if (!actionText.isNullOrBlank()) {
                val actionClickMod = if (onActionClick != null) Modifier.clickable(onClick = onActionClick) else Modifier
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(actionClickMod)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = actionText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

