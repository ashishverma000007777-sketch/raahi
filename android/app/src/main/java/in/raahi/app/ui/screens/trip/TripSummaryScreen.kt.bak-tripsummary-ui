package `in`.raahi.app.ui.screens.trip

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.network.TripSummaryDto
import `in`.raahi.app.ui.components.GlassCard
import `in`.raahi.app.ui.components.SectionLabel
import `in`.raahi.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSummaryScreen(
    onDone: () -> Unit,
    viewModel: TripSummaryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Summary", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, fontFamily = RaahiDisplayFont) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = RaahiText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaahiBg)
            )
        },
        containerColor = RaahiBg
    ) { padding ->
        when (val s = state) {
            is TripSummaryUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RaahiOrange)
                }
            }
            is TripSummaryUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(s.message, color = RaahiRed, fontSize = 14.sp)
                }
            }
            is TripSummaryUiState.Success -> {
                val sum = s.summary
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Header celebration
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RaahiShapeMedium)
                            .background(RaahiGreen.copy(alpha = 0.12f))
                            .border(1.dp, RaahiGreen.copy(alpha = 0.3f), RaahiShapeMedium)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(RaahiGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = RaahiGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Trip Completed 🎉", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Stats recorded into your vehicle history", color = RaahiTextDim, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // G. AUTO TRIP STORY CARD (Shareable design)
                    SectionLabel("Auto Trip Story")
                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RaahiShapeLarge)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFFF6F0), Color(0xFFFDE8DF), Color(0xFFF8CFBF))
                                )
                            )
                            .border(1.dp, RaahiOrange.copy(alpha = 0.35f), RaahiShapeLarge)
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(RaahiBrandGradient)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("RAAHI ROAD TRIP", color = RaahiVioletAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                                }
                                Text(sum.date, color = RaahiTextFaint, fontSize = 11.sp)
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                sum.title,
                                color = RaahiText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RaahiDisplayFont
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(sum.routeDescription, color = RaahiTextDim, fontSize = 13.sp)

                            Spacer(Modifier.height(18.dp))

                            // 4 Key metrics pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StoryPill(
                                    label = "Distance",
                                    value = "${sum.totalDistanceKm.toInt()} km",
                                    tint = RaahiCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                StoryPill(
                                    label = "Duration",
                                    value = sum.durationFormatted,
                                    tint = RaahiOrange,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val fuelText = if (sum.fuelCost != null) "₹${sum.fuelCost.toInt()}" else "–"
                                StoryPill(
                                    label = "Fuel",
                                    value = fuelText,
                                    tint = RaahiAmber,
                                    modifier = Modifier.weight(1f)
                                )
                                StoryPill(
                                    label = "Stops",
                                    value = "${sum.stopsCount} stops",
                                    tint = RaahiGreen,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Share button
                    val story = sum.storyCard
                    if (story != null) {
                        Button(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, story.shareableText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Trip Story")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RaahiShapeMedium,
                            colors = ButtonDefaults.buttonColors(containerColor = RaahiVioletAccent.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RaahiVioletAccent.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = RaahiVioletAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Share Trip Story Card", color = RaahiVioletAccent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // F. Detailed Summary Metrics
                    SectionLabel("Recorded Trip Details")
                    Spacer(Modifier.height(8.dp))

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SummaryDetailRow("Date", sum.date)
                            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RaahiBorderSoft)
                            SummaryDetailRow("Total Distance", "${sum.totalDistanceKm} km")
                            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RaahiBorderSoft)
                            SummaryDetailRow("Driving Duration", sum.durationFormatted)
                            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RaahiBorderSoft)
                            SummaryDetailRow("Fuel Logged", if (sum.fuelLoggedLitres != null) "${sum.fuelLoggedLitres} L" else "None recorded")
                            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RaahiBorderSoft)
                            SummaryDetailRow("Fuel Spending", if (sum.fuelCost != null) "₹${String.format(Locale.ROOT, "%.0f", sum.fuelCost)}" else "None recorded")
                            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = RaahiBorderSoft)
                            SummaryDetailRow("Tolls", sum.tollsFormatted)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Driving Behavior Honesty Note (Strict Rule per spec)
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = RaahiBorderSoft,
                        background = RaahiGlass
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                sum.drivingScoreMessage,
                                color = RaahiTextDim,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Back to Home CTA
                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RaahiShapeMedium,
                        colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange)
                    ) {
                        Text("Back to Home", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StoryPill(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Column {
            Text(label, color = RaahiTextFaint, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text(value, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
        }
    }
}

@Composable
private fun SummaryDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = RaahiTextDim, fontSize = 13.sp)
        Text(value, color = RaahiText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
