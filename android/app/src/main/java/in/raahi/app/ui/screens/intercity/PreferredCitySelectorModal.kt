package `in`.raahi.app.ui.screens.intercity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.raahi.app.data.CityCatalogue
import `in`.raahi.app.data.CityEntry
import `in`.raahi.app.ui.theme.*

/**
 * Premium, production-grade City Selector modal for Raahi Intercity Marketplace.
 *
 * Implements:
 * - A refined travel-product visual hierarchy with calm surfaces and restrained accents.
 * - Centralized CityCatalogue filtering by canonical name, region, state, and aliases.
 * - Quick-selection popular hub chips with zero clipping.
 * - Structured regional grouping across verified intercity corridors.
 * - Clear selected states with semantic highlights and dual visual indicators.
 * - Keyboard-safe layout with scrollable list and persistent action buttons.
 * - In-flight loading state and recoverable inline error messaging.
 */
@Composable
fun PreferredCitySelectorModal(
    currentCity: String?,
    isSaving: Boolean,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSaveCity: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCity by remember(currentCity) {
        val initial = currentCity?.trim().orEmpty()
        val canonical = if (initial.isNotBlank()) CityCatalogue.canonicalName(initial) ?: initial else "All Cities"
        mutableStateOf(canonical)
    }

    val focusManager = LocalFocusManager.current
    val popularCities = remember { CityCatalogue.popularCities() }
    val groupedCities = remember(searchQuery) { CityCatalogue.grouped(searchQuery) }
    val totalMatches = remember(groupedCities) { groupedCities.values.sumOf { it.size } }

    val isDark = RaahiIsDark

    // Calm, sophisticated component tokens
    val surfaceColor = RaahiBg2
    val inputBgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val locationBadgeBg = if (isDark) Color(0xFF0C4A6E).copy(alpha = 0.4f) else Color(0xFFE0F2FE)
    val locationBadgeTint = Color(0xFF0284C7)
    val selectedRowBg = if (isDark) Color(0xFF132A22) else Color(0xFFF0FDF4)
    val selectedRowBorder = if (isDark) Color(0xFF15803D) else Color(0xFF86EFAC)
    val checkmarkGreen = Color(0xFF16A34A)

    val matchesAllCitiesOption = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        q.isEmpty() || "all cities".contains(q) || "all".contains(q) || "any".contains(q)
    }

    Dialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isSaving,
            dismissOnClickOutside = !isSaving,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 380.dp, max = 640.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(22.dp),
                        spotColor = Color(0x33000000),
                    ),
                shape = RoundedCornerShape(22.dp),
                color = surfaceColor,
                border = BorderStroke(1.dp, RaahiBorderSoft),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // =========================================================================
                    // 1. HEADER AREA
                    // =========================================================================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
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
                                color = locationBadgeBg,
                                modifier = Modifier.size(38.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = locationBadgeTint,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Choose your city",
                                    fontFamily = RaahiDisplayFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = RaahiText,
                                    lineHeight = 22.sp,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Select your preferred pickup city for intercity trip alerts.",
                                    fontFamily = RaahiBodyFont,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = RaahiTextDim,
                                    lineHeight = 16.sp,
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            enabled = !isSaving,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close city selector",
                                tint = RaahiTextDim,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // =========================================================================
                    // 2. SEARCH FIELD
                    // =========================================================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Search city or region",
                                    fontSize = 13.5.sp,
                                    color = RaahiTextFaint,
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBgColor,
                                unfocusedContainerColor = inputBgColor,
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = RaahiBorderSoft,
                                focusedTextColor = RaahiText,
                                unfocusedTextColor = RaahiText,
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = RaahiTextDim,
                                    modifier = Modifier.size(19.dp),
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = RaahiTextDim,
                                            modifier = Modifier.size(17.dp),
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search,
                                keyboardType = KeyboardType.Text,
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { focusManager.clearFocus() }
                            )
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // =========================================================================
                    // 3. POPULAR HUBS (Quick selection row)
                    // =========================================================================
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "POPULAR HUBS",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = RaahiTextFaint,
                                letterSpacing = 0.8.sp,
                            )

                            if (searchQuery.isNotBlank()) {
                                Text(
                                    text = "$totalMatches found",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RaahiTextDim,
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(popularCities, key = { "pop_${it.name}" }) { city ->
                                val isSelected = selectedCity.equals(city.name, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) selectedRowBg else inputBgColor,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) selectedRowBorder else RaahiBorderSoft,
                                    ),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedCity = city.name
                                            focusManager.clearFocus()
                                        },
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = checkmarkGreen,
                                                modifier = Modifier.size(13.dp),
                                            )
                                        }
                                        Text(
                                            text = city.name,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) RaahiText else RaahiTextDim,
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = RaahiBorderSoft, thickness = 0.8.dp)

                    // =========================================================================
                    // 4. SCROLLABLE CITY LIST
                    // =========================================================================
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (totalMatches == 0 && !matchesAllCitiesOption) {
                            // Empty Search Results State
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 28.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = RaahiBorderSoft.copy(alpha = 0.6f),
                                    modifier = Modifier.size(52.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocationOff,
                                            contentDescription = null,
                                            tint = RaahiTextDim,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    text = "No cities found matching \"$searchQuery\"",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RaahiText,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = "Only verified intercity hubs across Tricity, Punjab, Haryana, Delhi NCR, HP, Rajasthan, UP & Uttarakhand are currently supported.",
                                    fontSize = 12.sp,
                                    color = RaahiTextDim,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp,
                                )

                                Spacer(Modifier.height(12.dp))

                                TextButton(
                                    onClick = { searchQuery = "" },
                                    colors = ButtonDefaults.textButtonColors(contentColor = RaahiOrange),
                                ) {
                                    Text("Clear search query", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 12.dp),
                            ) {
                                // "All Cities" Option
                                if (matchesAllCitiesOption) {
                                    item(key = "city_all_option") {
                                        val isAllSelected = selectedCity.isBlank() ||
                                                selectedCity.equals("All", ignoreCase = true) ||
                                                selectedCity.equals("All Cities", ignoreCase = true)

                                        CityRowItem(
                                            name = "All Cities (Any Hub)",
                                            subtitle = "Receive trip alerts across all regions",
                                            isSelected = isAllSelected,
                                            matchedAlias = null,
                                            selectedBg = selectedRowBg,
                                            selectedBorder = selectedRowBorder,
                                            checkmarkColor = checkmarkGreen,
                                            onClick = {
                                                selectedCity = "All Cities"
                                                focusManager.clearFocus()
                                            }
                                        )
                                        HorizontalDivider(
                                            color = RaahiBorderSoft,
                                            thickness = 0.7.dp,
                                            modifier = Modifier.padding(start = 52.dp, end = 20.dp),
                                        )
                                    }
                                }

                                // Regional Sections
                                groupedCities.forEach { (region, citiesInRegion) ->
                                    item(key = "sec_$region") {
                                        Surface(
                                            color = surfaceColor,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp)
                                                .padding(top = 12.dp, bottom = 4.dp),
                                        ) {
                                            Text(
                                                text = region.uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RaahiTextFaint,
                                                letterSpacing = 0.8.sp,
                                            )
                                        }
                                    }

                                    items(citiesInRegion, key = { "city_${it.name}" }) { city ->
                                        val isSelected = selectedCity.equals(city.name, ignoreCase = true)
                                        val matchedAlias = if (searchQuery.isNotBlank()) {
                                            city.aliases.firstOrNull {
                                                it.contains(searchQuery.trim(), ignoreCase = true) &&
                                                        !city.name.contains(searchQuery.trim(), ignoreCase = true)
                                            }
                                        } else null

                                        CityRowItem(
                                            name = city.name,
                                            subtitle = "${city.region} • ${city.state}",
                                            isSelected = isSelected,
                                            matchedAlias = matchedAlias,
                                            selectedBg = selectedRowBg,
                                            selectedBorder = selectedRowBorder,
                                            checkmarkColor = checkmarkGreen,
                                            onClick = {
                                                selectedCity = city.name
                                                focusManager.clearFocus()
                                            }
                                        )

                                        HorizontalDivider(
                                            color = RaahiBorderSoft,
                                            thickness = 0.7.dp,
                                            modifier = Modifier.padding(start = 52.dp, end = 20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // 5. RECOVERABLE ERROR ALERT (If update failed)
                    // =========================================================================
                    AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                        errorMessage?.let { errorText ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = RaahiRed.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, RaahiRed.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = RaahiRed,
                                        modifier = Modifier.size(17.dp),
                                    )
                                    Text(
                                        text = errorText,
                                        color = RaahiRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        lineHeight = 15.sp,
                                    )
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // 6. BOTTOM ACTION AREA (Always accessible above keyboard)
                    // =========================================================================
                    HorizontalDivider(color = RaahiBorderSoft, thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, RaahiBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = RaahiText,
                            )
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                            )
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onSaveCity(selectedCity)
                            },
                            enabled = !isSaving && selectedCity.isNotBlank(),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RaahiOrange,
                                disabledContainerColor = RaahiOrange.copy(alpha = 0.5f),
                            ),
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Text(
                                    text = "Save city",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityRowItem(
    name: String,
    subtitle: String,
    isSelected: Boolean,
    matchedAlias: String?,
    selectedBg: Color,
    selectedBorder: Color,
    checkmarkColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        color = if (isSelected) selectedBg else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) BorderStroke(1.dp, selectedBorder) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFF0284C7) else RaahiTextDim.copy(alpha = 0.5f),
                    modifier = Modifier.size(19.dp),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontFamily = RaahiBodyFont,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                        color = RaahiText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(1.dp))

                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = RaahiTextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (!matchedAlias.isNullOrBlank()) {
                        Text(
                            text = "Matched alias: $matchedAlias",
                            fontSize = 11.sp,
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // Dual visual indicator: semantic row background + checkmark circle
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = checkmarkColor,
                    modifier = Modifier.size(22.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(1.dp, RaahiBorder, CircleShape)
                )
            }
        }
    }
}
