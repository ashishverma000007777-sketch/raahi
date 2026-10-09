package `in`.raahi.app.ui.screens.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.ui.components.rememberLocationPermissionState
import `in`.raahi.app.ui.theme.*

@Composable
fun RequestHelpScreen(
    onBack: () -> Unit,
    onCreated: (jobId: String) -> Unit,
    viewModel: RequestHelpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val permission = rememberLocationPermissionState()

    LaunchedEffect(permission.isGranted) {
        if (permission.isGranted && state.location is LocationFixState.Idle) {
            viewModel.fetchLocation()
        }
    }
    LaunchedEffect(state.submit) {
        val submit = state.submit
        if (submit is SubmitState.Success) onCreated(submit.jobId)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = RaahiBg) {
        val scrollState = rememberScrollState()
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                TopHeader(onBack)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 36.dp)
                ) {

                Spacer(Modifier.height(14.dp))

                LocationCard(
                    state.location,
                    hasPermission = permission.isGranted,
                    onRequestPermission = permission.request,
                    onRetry = viewModel::fetchLocation,
                )

                Spacer(Modifier.height(18.dp))

                SectionTitle("Select issue type")
                Spacer(Modifier.height(8.dp))
                ProblemGrid(state.selectedProblem, onSelect = viewModel::selectProblem)

                Spacer(Modifier.height(18.dp))

                SectionTitle("Describe the issue (optional)")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::setDescription,
                    placeholder = { Text("e.g. Rear tyre puncture, on NH-44...", color = RaahiTextFaint, fontSize = 13.5.sp) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RaahiShapeSmall, spotColor = Color(0x0C000000)),
                    shape = RaahiShapeSmall,
                    colors = requestFieldColors(),
                )

                Spacer(Modifier.height(18.dp))

                SectionTitle("Your offer (cash)")
                Spacer(Modifier.height(8.dp))
                PriceSection(state.price, onPriceChange = viewModel::setPrice)

                Spacer(Modifier.height(14.dp))

                CashNote()

                if (state.submit is SubmitState.Error) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        (state.submit as SubmitState.Error).message,
                        color = RaahiRed, fontSize = 12.5.sp, fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.height(22.dp))

                // Primary CTA button: "Find Nearby Helpers" with coral red background and coral glow
                Button(
                    onClick = viewModel::submit,
                    enabled = state.submit !is SubmitState.Submitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(18.dp), spotColor = RaahiOrange.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                ) {
                    if (state.submit is SubmitState.Submitting) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Find Nearby Helpers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun TopHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = RaahiText)
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text("Request Help", color = RaahiText, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
            Text("Tell us what's wrong. We'll find the nearest helpers for you.", color = RaahiTextDim, fontSize = 11.5.sp, maxLines = 1)
        }
    }
}

@Composable
private fun LocationCard(
    location: LocationFixState,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit,
) {
    val ready = location is LocationFixState.Ready
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp), spotColor = Color(0x10000000))
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, RaahiBorderSoft, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background((if (ready) RaahiGreen else RaahiOrange).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (ready) Icons.Filled.LocationOn else Icons.Filled.LocationOff,
                contentDescription = null, tint = if (ready) RaahiGreen else RaahiOrange, modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val title = when {
                !hasPermission -> "Location permission needed"
                location is LocationFixState.Fetching -> "Detecting location..."
                ready -> "Location detected"
                else -> "Location unavailable"
            }
            Text(title, color = if (ready) RaahiGreen else RaahiText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            val loc = (location as? LocationFixState.Ready)?.location
            Text(
                if (loc != null) "Lat ${"%.4f".format(loc.lat)}, Lng ${"%.4f".format(loc.lng)}"
                else "Helpers need this to reach your breakdown location",
                color = RaahiTextDim, fontSize = 11.sp, maxLines = 1,
            )
        }
        when {
            !hasPermission -> RetryChip("Allow", onRequestPermission)
            location is LocationFixState.Fetching -> CircularProgressIndicator(modifier = Modifier.size(18.dp), color = RaahiOrange, strokeWidth = 2.dp)
            ready -> Box(Modifier.size(24.dp).background(RaahiGreen, CircleShape), contentAlignment = Alignment.Center) {
                Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            else -> RetryChip("Retry", onRetry)
        }
    }
}

@Composable
private fun RetryChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(RaahiOrange.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = RaahiOrange, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = RaahiText,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = RaahiDisplayFont
    )
}

@Composable
private fun ProblemGrid(selected: String?, onSelect: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        items(PROBLEM_TYPES) { p ->
            val isSelected = p.id == selected
            Column(
                modifier = Modifier
                    .then(
                        if (isSelected) Modifier.shadow(elevation = 5.dp, shape = RoundedCornerShape(18.dp), spotColor = RaahiOrange)
                        else Modifier.shadow(elevation = 1.dp, shape = RoundedCornerShape(18.dp), spotColor = Color(0x0C000000))
                    )
                    .background(
                        if (isSelected) Color(0xFFFFF0ED) else Color.White,
                        RoundedCornerShape(18.dp)
                    )
                    .border(
                        1.dp,
                        if (isSelected) RaahiOrange else RaahiBorderSoft,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelect(p.id) }
                    .padding(vertical = 16.dp, horizontal = 4.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(p.emoji, fontSize = 24.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = p.label,
                    color = if (isSelected) RaahiOrange else RaahiText,
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PriceSection(price: String, onPriceChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        // Price Input Display Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0x10000000))
                .background(Color.White, RoundedCornerShape(14.dp))
                .border(1.dp, RaahiBorderSoft, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("₹", color = RaahiOrange, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
            Spacer(Modifier.width(10.dp))
            BasicTextFieldPrice(price, onPriceChange, Modifier.weight(1f))
            Text("cash", color = RaahiTextDim, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(10.dp))

        // Horizontally scrollable amount chips: ₹50, ₹100, ₹150, ₹200, ₹300, ₹500
        // ₹500 never wraps vertically or elongates
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QUICK_PRICES.forEach { amount ->
                val selected = price == amount.toString()
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .widthIn(min = 60.dp)
                        .then(
                            if (selected) Modifier.shadow(elevation = 5.dp, shape = RoundedCornerShape(20.dp), spotColor = RaahiOrange)
                            else Modifier.shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp), spotColor = Color(0x0C000000))
                        )
                        .background(
                            if (selected) RaahiOrange else Color.White,
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            if (selected) RaahiOrange else RaahiBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onPriceChange(amount.toString()) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "₹$amount",
                        color = if (selected) Color.White else RaahiText,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@Composable
private fun BasicTextFieldPrice(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text("100", color = RaahiTextFaint, fontSize = 24.sp, fontWeight = FontWeight.Bold) },
        textStyle = androidx.compose.ui.text.TextStyle(
            color = RaahiText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = RaahiDisplayFont
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = RaahiOrange,
        ),
        modifier = modifier,
    )
}

@Composable
private fun CashNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(12.dp), spotColor = Color(0x0A000000))
            .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("💵", fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            "Payment goes directly to the helper in cash. Raahi takes 0% commission.",
            color = Color(0xFF166534), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun requestFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = RaahiText,
    unfocusedTextColor = RaahiText,
    focusedBorderColor = RaahiOrange,
    unfocusedBorderColor = RaahiBorder,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    cursorColor = RaahiOrange,
)
