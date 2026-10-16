package `in`.raahi.app.ui.screens.intercity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.raahi.app.network.IntercityTripDto
import `in`.raahi.app.ui.theme.*

/**
 * Premium decision-confirmation modal for cancelling an Intercity trip.
 *
 * Implements:
 * - Clear decision hierarchy: calm neutral surface, restrained destructive action.
 * - Real trip data summary (actual pickup, destination, departure, vehicle, status, fare).
 * - Common mobility reason chips + multiline explanation field.
 * - In-flight loading indicator and duplicate request protection.
 * - Recoverable inline error messaging that preserves user inputs on failure.
 * - Safe back/dismissal behavior (only dismisses, never cancels on dismiss).
 */
@Composable
fun CancelIntercityTripModal(
    trip: IntercityTripDto,
    isCancelling: Boolean,
    errorMessage: String? = null,
    initialReason: String = "",
    onDismiss: () -> Unit,
    onConfirmCancel: (String) -> Unit,
) {
    var reasonInput by remember(initialReason) { mutableStateOf(initialReason) }
    val focusManager = LocalFocusManager.current
    val isDark = RaahiIsDark

    val commonReasons = listOf(
        "Schedule changed",
        "Customer cancelled",
        "Vehicle unavailable",
        "Other",
    )

    val surfaceBg = RaahiBg2
    val containerBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val destructiveRed = Color(0xFFDC2626)
    val warningBadgeBg = if (isDark) Color(0xFF2E2211) else Color(0xFFFEF3C7)
    val warningBadgeTint = Color(0xFFD97706)

    Dialog(
        onDismissRequest = {
            if (!isCancelling) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isCancelling,
            dismissOnClickOutside = !isCancelling,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = Color(0x33000000),
                    ),
                shape = RoundedCornerShape(20.dp),
                color = surfaceBg,
                border = BorderStroke(1.dp, RaahiBorderSoft),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    // =========================================================================
                    // 1. HEADER AREA
                    // =========================================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = warningBadgeBg,
                                modifier = Modifier.size(38.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = warningBadgeTint,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Cancel this trip?",
                                    style = RaahiTypeScale.cardHeader,
                                    color = RaahiText,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Review the trip details before confirming cancellation.",
                                    style = RaahiTypeScale.bodySmall,
                                    color = RaahiTextDim,
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            enabled = !isCancelling,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = RaahiTextDim,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // =========================================================================
                    // 2. ACTUAL TRIP SUMMARY CARD
                    // =========================================================================
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = containerBg,
                        border = BorderStroke(1.dp, RaahiBorderSoft),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Route: Pickup -> Destination
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = trip.pickupCity,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = RaahiText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "to",
                                    tint = RaahiTextDim,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = trip.destCity,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = RaahiText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )

                                Spacer(Modifier.weight(1f))

                                // Status badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = RaahiBorderSoft,
                                ) {
                                    Text(
                                        text = trip.status,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RaahiText,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    )
                                }
                            }

                            HorizontalDivider(color = RaahiBorderSoft, thickness = 0.8.dp)

                            // Departure, Vehicle, and Fare metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        tint = RaahiTextDim,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = formatTripDepartureTime(trip.departureTime),
                                        fontSize = 12.sp,
                                        color = RaahiTextDim,
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DirectionsCar,
                                        contentDescription = null,
                                        tint = RaahiTextDim,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        text = trip.vehicleCategory,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = RaahiText,
                                    )
                                }

                                Text(
                                    text = "₹${trip.fare.toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = RaahiText,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // =========================================================================
                    // 3. REASON FOR CANCELLATION
                    // =========================================================================
                    Text(
                        text = "Reason for cancellation",
                        style = RaahiTypeScale.sectionHeader,
                        color = RaahiText,
                    )

                    Spacer(Modifier.height(8.dp))

                    // Quick reason chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(commonReasons) { r ->
                            val isSelected = reasonInput.trim().equals(r, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) RaahiSelectionBg else containerBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) RaahiOrange.copy(alpha = 0.6f) else RaahiBorderSoft
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        reasonInput = r
                                        focusManager.clearFocus()
                                    }
                            ) {
                                Text(
                                    text = r,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) RaahiOrange else RaahiTextDim,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Multiline reason text field
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        placeholder = {
                            Text(
                                text = "Provide details or select a reason above...",
                                fontSize = 13.sp,
                                color = RaahiTextFaint,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp, max = 110.dp),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = containerBg,
                            unfocusedContainerColor = containerBg,
                            focusedBorderColor = RaahiBorder,
                            unfocusedBorderColor = RaahiBorderSoft,
                            focusedTextColor = RaahiText,
                            unfocusedTextColor = RaahiText,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )

                    // =========================================================================
                    // 4. RECOVERABLE ERROR BANNER
                    // =========================================================================
                    AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                        errorMessage?.let { errText ->
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = destructiveRed.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, destructiveRed.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = destructiveRed,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = errText,
                                        color = destructiveRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        lineHeight = 15.sp,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // =========================================================================
                    // 5. ACTION AREA (Keep trip / Cancel trip)
                    // =========================================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isCancelling,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, RaahiBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = RaahiText,
                            ),
                        ) {
                            Text(
                                text = "Keep trip",
                                style = RaahiTypeScale.button,
                                color = RaahiText,
                            )
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onConfirmCancel(reasonInput.trim())
                            },
                            enabled = !isCancelling,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = destructiveRed,
                                disabledContainerColor = destructiveRed.copy(alpha = 0.5f),
                            ),
                        ) {
                            if (isCancelling) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Text(
                                    text = "Cancel trip",
                                    style = RaahiTypeScale.button,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Formats an ISO departure time into a readable date-time string */
internal fun formatTripDepartureTime(isoTime: String?): String {
    if (isoTime.isNullOrBlank()) return "Scheduled departure"
    return runCatching {
        val instant = java.time.Instant.parse(isoTime)
        val zone = java.time.ZoneId.systemDefault()
        val dt = instant.atZone(zone)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM • h:mm a")
        dt.format(formatter)
    }.getOrDefault(isoTime.take(16).replace("T", " "))
}
