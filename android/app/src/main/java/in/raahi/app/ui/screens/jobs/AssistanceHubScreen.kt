package `in`.raahi.app.ui.screens.jobs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.raahi.app.ui.components.RaahiBottomNavBar
import `in`.raahi.app.ui.components.RaahiTab
import `in`.raahi.app.ui.theme.*

/**
 * Dedicated Services Hub consolidating on-demand roadside assistance,
 * vehicle care & health, intercity commercial travel marketplace, and emergency SOS.
 */
@Composable
fun AssistanceHubScreen(
    onBack: () -> Unit = {},
    onRequestHelp: () -> Unit,
    onNearbyMechanics: () -> Unit,
    onMyJobs: () -> Unit,
    onSos: () -> Unit,
    onAiMechanic: () -> Unit,
    onCarHealth: () -> Unit = {},
    onFuelRates: () -> Unit = {},
    onMyGarage: () -> Unit = {},
    onExploreIntercity: () -> Unit = {},
    onPostIntercityTrip: () -> Unit = {},
    onAgencyHub: () -> Unit = {},
    onNavigateTab: (RaahiTab) -> Unit,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        bottomBar = {
            RaahiBottomNavBar(current = RaahiTab.SERVICES, onSelect = onNavigateTab)
        },
        containerColor = RaahiBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Text(
                    text = "Services & Assistance",
                    style = RaahiTypeScale.headlineMedium,
                    color = RaahiTypeScale.TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "24/7 roadside assistance, vehicle maintenance, and intercity travel.",
                    style = RaahiTypeScale.bodyMedium,
                    color = RaahiTypeScale.TextSecondary,
                )
            }

            // SECTION 1: ROADSIDE ASSISTANCE (Primary Hero Card)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RaahiTypeScale.SurfaceElevated,
                border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
                shadowElevation = 1.5.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RaahiOrange.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, RaahiOrange.copy(alpha = 0.25f)),
                        ) {
                            Text(
                                text = "ON-DEMAND ROADSIDE",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = RaahiTypeScale.caption,
                                color = RaahiOrange,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(RaahiTypeScale.StatusOnline),
                            )
                            Text(
                                text = "24/7 Dispatch",
                                style = RaahiTypeScale.caption,
                                color = RaahiTypeScale.StatusOnline,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Stranded or need roadside assistance?",
                            style = RaahiTypeScale.titleLarge,
                            color = RaahiTypeScale.TextPrimary,
                        )
                        Text(
                            text = "Connect with verified nearby mechanics for puncture, battery, towing, or engine issues.",
                            style = RaahiTypeScale.bodyMedium,
                            color = RaahiTypeScale.TextSecondary,
                        )
                    }

                    // Problem pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ProblemBadge(label = "Flat Tyre", modifier = Modifier.weight(1f))
                        ProblemBadge(label = "Jumpstart", modifier = Modifier.weight(1f))
                        ProblemBadge(label = "Towing", modifier = Modifier.weight(1f))
                        ProblemBadge(label = "Fuel Delivery", modifier = Modifier.weight(1f))
                    }

                    // Primary action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onRequestHelp,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RaahiOrange,
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SupportAgent,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Request Help",
                                style = RaahiTypeScale.buttonMedium,
                            )
                        }

                        OutlinedButton(
                            onClick = onSos,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RaahiTypeScale.ActionDestructive.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = RaahiTypeScale.ActionDestructive,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = RaahiTypeScale.ActionDestructive,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "SOS",
                                style = RaahiTypeScale.buttonMedium,
                                color = RaahiTypeScale.ActionDestructive,
                            )
                        }
                    }

                    // Secondary assistance options: Map & My Jobs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onNearbyMechanics),
                            shape = RoundedCornerShape(12.dp),
                            color = RaahiTypeScale.BackgroundSubtle,
                            border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = RaahiTypeScale.AccentNavy,
                                    modifier = Modifier.size(18.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Mechanics Map",
                                        style = RaahiTypeScale.titleSmall,
                                        color = RaahiTypeScale.TextPrimary,
                                    )
                                    Text(
                                        text = "Nearby garages & shops",
                                        style = RaahiTypeScale.caption,
                                        color = RaahiTypeScale.TextSecondary,
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onMyJobs),
                            shape = RoundedCornerShape(12.dp),
                            color = RaahiTypeScale.BackgroundSubtle,
                            border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = RaahiTypeScale.AccentTeal,
                                    modifier = Modifier.size(18.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "My Jobs",
                                        style = RaahiTypeScale.titleSmall,
                                        color = RaahiTypeScale.TextPrimary,
                                    )
                                    Text(
                                        text = "Live ETA & receipts",
                                        style = RaahiTypeScale.caption,
                                        color = RaahiTypeScale.TextSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: VEHICLE CARE
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(
                        text = "Vehicle Care",
                        style = RaahiTypeScale.titleLarge,
                        color = RaahiTypeScale.TextPrimary,
                    )
                    Text(
                        text = "Diagnostics, maintenance, fuel tracking, and garage fleet.",
                        style = RaahiTypeScale.bodySmall,
                        color = RaahiTypeScale.TextSecondary,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val isDark = RaahiIsDark
                    ServiceTile(
                        icon = Icons.Outlined.Build,
                        iconTint = RaahiDeepTeal,
                        iconBg = if (isDark) Color(0xFF132B29) else Color(0xFFE6F4F1),
                        title = "Car Health",
                        description = "Diagnostics & alerts",
                        onClick = onCarHealth,
                        modifier = Modifier.weight(1f),
                    )

                    ServiceTile(
                        icon = Icons.Outlined.LocalGasStation,
                        iconTint = RaahiWarning,
                        iconBg = if (isDark) Color(0xFF332612) else Color(0xFFFFFBEB),
                        title = "Fuel Rates",
                        description = "Daily petrol, diesel, CNG",
                        onClick = onFuelRates,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val isDark = RaahiIsDark
                    ServiceTile(
                        icon = Icons.Outlined.DirectionsCar,
                        iconTint = RaahiSecondaryTeal,
                        iconBg = if (isDark) Color(0xFF112E2B) else Color(0xFFF0FDFA),
                        title = "My Garage",
                        description = "Specs, insurance & RC",
                        onClick = onMyGarage,
                        modifier = Modifier.weight(1f),
                    )

                    ServiceTile(
                        icon = Icons.Outlined.AutoAwesome,
                        iconTint = if (isDark) Color(0xFFCBD5E1) else RaahiDarkNavy,
                        iconBg = if (isDark) Color(0xFF1C2B3F) else Color(0xFFF1F5F9),
                        title = "AI Diagnostics",
                        description = "Instant symptom advice",
                        onClick = onAiMechanic,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // SECTION 3: INTERCITY TRAVEL
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RaahiTypeScale.SurfaceElevated,
                border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
                shadowElevation = 1.5.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RaahiTypeScale.AccentNavy.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color(0xFFD0DDEB)),
                        ) {
                            Text(
                                text = "INTERCITY MARKETPLACE",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = RaahiTypeScale.caption,
                                color = RaahiTypeScale.AccentNavy,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp,
                            )
                        }

                        Text(
                            text = "Owner & Agent Rides",
                            style = RaahiTypeScale.caption,
                            color = RaahiTypeScale.TextSecondary,
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Commercial Highway Corridors",
                            style = RaahiTypeScale.titleLarge,
                            color = RaahiTypeScale.TextPrimary,
                        )
                        Text(
                            text = "Find seats on intercity routes or post your vehicle journey with fair pricing and OTP confirmation.",
                            style = RaahiTypeScale.bodyMedium,
                            color = RaahiTypeScale.TextSecondary,
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onExploreIntercity,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RaahiOrange,
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Text(
                                text = "Explore Trips",
                                style = RaahiTypeScale.buttonMedium,
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                            )
                        }

                        OutlinedButton(
                            onClick = onPostIntercityTrip,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = RaahiTypeScale.TextPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Text(
                                text = "Post a Trip",
                                style = RaahiTypeScale.buttonMedium,
                            )
                        }
                    }

                    // Agent Hub shortcut
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onAgencyHub),
                        shape = RoundedCornerShape(10.dp),
                        color = RaahiTypeScale.BackgroundSubtle,
                        border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                                ) {
                                    Text(
                                        text = "AGENT",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = RaahiTypeScale.caption,
                                        color = Color(0xFF7C3AED),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Text(
                                    text = "Agency Hub & Fleet Management",
                                    style = RaahiTypeScale.bodySmall,
                                    color = RaahiTypeScale.TextPrimary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = RaahiTypeScale.TextSecondary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ServiceTile(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = RaahiTypeScale.SurfaceElevated,
        border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = title,
                style = RaahiTypeScale.titleMedium,
                color = RaahiTypeScale.TextPrimary,
            )
            Text(
                text = description,
                style = RaahiTypeScale.bodySmall,
                color = RaahiTypeScale.TextSecondary,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun ProblemBadge(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(30.dp),
        shape = RoundedCornerShape(8.dp),
        color = RaahiTypeScale.BackgroundSubtle,
        border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = RaahiTypeScale.caption,
                color = RaahiTypeScale.TextPrimary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
