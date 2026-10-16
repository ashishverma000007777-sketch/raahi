package `in`.raahi.app.ui.screens.trip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.raahi.app.ui.components.RaahiBottomNavBar
import `in`.raahi.app.ui.components.RaahiTab
import `in`.raahi.app.ui.theme.*

/**
 * Dedicated Trips Hub separating commercial Intercity Marketplace
 * from Personal Highway Trip Planner with equal clarity and professional typography.
 */
@Composable
fun TripsHubScreen(
    onBack: () -> Unit = {},
    onOpenIntercityMarketplace: () -> Unit,
    onSearchIntercityTrips: (String, String) -> Unit = { _, _ -> },
    onCreateIntercityTrip: () -> Unit = {},
    onAgentIntercityDashboard: () -> Unit = {},
    onDriverIntercityTrips: () -> Unit = {},
    onPlanTrip: () -> Unit = {},
    onTripHistory: () -> Unit = {},
    onNavigateTab: (RaahiTab) -> Unit,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        bottomBar = {
            RaahiBottomNavBar(current = RaahiTab.TRIPS, onSelect = onNavigateTab)
        },
        containerColor = RaahiBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                Text(
                    text = "Trips & Travel",
                    style = RaahiTypeScale.headlineMedium,
                    color = RaahiTypeScale.TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Commercial intercity marketplace and personal highway planners.",
                    style = RaahiTypeScale.bodyMedium,
                    color = RaahiTypeScale.TextSecondary,
                )
            }

            // SECTION 1: Intercity Marketplace (Primary Hub Feature)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = RaahiTypeScale.SurfaceElevated,
                border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Badge & verified tag
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = null,
                                tint = RaahiTypeScale.AccentTeal,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Verified Corridors",
                                style = RaahiTypeScale.caption,
                                color = RaahiTypeScale.TextSecondary,
                            )
                        }
                    }

                    // Title & Description
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "City-to-city travel, made simpler.",
                            style = RaahiTypeScale.titleLarge,
                            color = RaahiTypeScale.TextPrimary,
                        )
                        Text(
                            text = "Find available seats or publish commercial routes across North India.",
                            style = RaahiTypeScale.bodyMedium,
                            color = RaahiTypeScale.TextSecondary,
                        )
                    }

                    // Route corridor visual motif
                    RouteCorridorTrack()

                    // Action buttons: Explore trips & Post a trip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onOpenIntercityMarketplace,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RaahiOrange,
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Text(
                                text = "Explore trips",
                                style = RaahiTypeScale.buttonMedium,
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        OutlinedButton(
                            onClick = onCreateIntercityTrip,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = RaahiTypeScale.TextPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = RaahiOrange,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Post a trip",
                                style = RaahiTypeScale.buttonMedium,
                            )
                        }
                    }

                    // Quick corridor shortcuts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TripsQuickChip(
                            label = "Search Routes",
                            icon = Icons.Outlined.Search,
                            onClick = { onSearchIntercityTrips("", "") },
                            modifier = Modifier.weight(1f),
                        )
                        TripsQuickChip(
                            label = "Driver Trips",
                            icon = Icons.Outlined.DirectionsCar,
                            onClick = onDriverIntercityTrips,
                            modifier = Modifier.weight(1f),
                        )
                        TripsQuickChip(
                            label = "Agency Hub",
                            icon = Icons.Outlined.VerifiedUser,
                            onClick = onAgentIntercityDashboard,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // SECTION 2: Personal Highway Trip Planner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = RaahiTypeScale.SurfaceElevated,
                border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RaahiTypeScale.AccentTeal.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color(0xFFC7E8E3)),
                        ) {
                            Text(
                                text = "HIGHWAY PLANNER",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = RaahiTypeScale.caption,
                                color = RaahiTypeScale.AccentTeal,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp,
                            )
                        }

                        Text(
                            text = "Personal Travel",
                            style = RaahiTypeScale.caption,
                            color = RaahiTypeScale.TextSecondary,
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Plan your highway journeys",
                            style = RaahiTypeScale.titleLarge,
                            color = RaahiTypeScale.TextPrimary,
                        )
                        Text(
                            text = "Smart route planning with live fuel stops, toll breakdown, and car health safety checks.",
                            style = RaahiTypeScale.bodyMedium,
                            color = RaahiTypeScale.TextSecondary,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onPlanTrip,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RaahiTypeScale.AccentTeal,
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Map,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Plan road trip",
                                style = RaahiTypeScale.buttonMedium,
                            )
                        }

                        OutlinedButton(
                            onClick = onTripHistory,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = RaahiTypeScale.TextPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = RaahiTypeScale.TextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "History",
                                style = RaahiTypeScale.buttonMedium,
                            )
                        }
                    }
                }
            }

            // Safety assurance footer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = RaahiTypeScale.BackgroundSubtle,
                border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = RaahiTypeScale.AccentTeal,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "All intercity drivers are verified with driving license and vehicle registration checks.",
                        style = RaahiTypeScale.bodySmall,
                        color = RaahiTypeScale.TextSecondary,
                        lineHeight = 16.sp,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RouteCorridorTrack(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = RaahiTypeScale.BackgroundSubtle,
        border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(RaahiTypeScale.AccentNavy),
                )
                Text(
                    text = "Departure City",
                    style = RaahiTypeScale.caption,
                    color = RaahiTypeScale.TextPrimary,
                    fontWeight = FontWeight.Medium,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(2.dp)
                        .background(Color(0xFFCBD5E1)),
                )
                Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = RaahiOrange,
                    modifier = Modifier.size(14.dp),
                )
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(2.dp)
                        .background(Color(0xFFCBD5E1)),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(RaahiTypeScale.AccentTeal),
                )
                Text(
                    text = "Arrival Hub",
                    style = RaahiTypeScale.caption,
                    color = RaahiTypeScale.TextPrimary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun TripsQuickChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = RaahiTypeScale.BackgroundSubtle,
        border = BorderStroke(1.dp, RaahiTypeScale.BorderSubtle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RaahiTypeScale.TextSecondary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = RaahiTypeScale.caption,
                color = RaahiTypeScale.TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
