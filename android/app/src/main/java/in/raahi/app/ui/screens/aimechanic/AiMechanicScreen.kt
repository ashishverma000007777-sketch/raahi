package `in`.raahi.app.ui.screens.aimechanic

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.network.AiMessageDto
import `in`.raahi.app.network.VehicleDto
import `in`.raahi.app.ui.components.*
import `in`.raahi.app.ui.theme.*
import kotlinx.coroutines.launch

private data class IssueCategory(
    val title: String,
    val issues: List<String>
)

private val ISSUE_CATEGORIES = listOf(
    IssueCategory(
        "Battery / Starting",
        listOf(
            "Car won't start / clicking sound",
            "Battery dead / jump start needed",
            "Slow engine crank & dim headlights"
        )
    ),
    IssueCategory(
        "Engine Overheating",
        listOf(
            "Temperature gauge in red zone",
            "Steam / coolant boiling under hood",
            "Overheating when stuck in traffic"
        )
    ),
    IssueCategory(
        "Tyre Problems",
        listOf(
            "Puncture / tyre pressure completely flat",
            "Tyre pressure warning light (TPMS)",
            "Wheel wobble / vibration at speed"
        )
    ),
    IssueCategory(
        "Fuel / CNG",
        listOf(
            "Ran out of petrol / diesel",
            "Strong CNG or petrol smell",
            "Engine sputtering / fuel delivery jerk"
        )
    ),
    IssueCategory(
        "AC Problems",
        listOf(
            "AC blowing warm / hot air",
            "Foul or burning smell from AC vents",
            "Loud rattling noise when AC turned on"
        )
    ),
    IssueCategory(
        "Warning Lights",
        listOf(
            "Check Engine light illuminated",
            "Oil pressure warning light (red)",
            "Brake system warning indicator on"
        )
    )
)

@Composable
fun AiMechanicScreen(
    onBack: () -> Unit,
    onNavigateTab: (RaahiTab) -> Unit,
    onRequestHelp: () -> Unit = { onNavigateTab(RaahiTab.HOME) },
    onNearbyMechanics: () -> Unit = { onNavigateTab(RaahiTab.MECHANICS) },
    viewModel: AiMechanicViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Step 1: Initial describe screen. Once in chat mode or when messages exist or sending, show results
    val step = if (!state.inChatMode && state.messages.isEmpty() && !state.sending) 1 else 2

    BackHandler(enabled = step == 2 && state.messages.isEmpty()) {
        viewModel.resetToDescribe()
    }

    LaunchedEffect(state.messages.size, state.sending) {
        if (state.messages.isNotEmpty() || state.sending) {
            val targetIdx = (state.messages.size + (if (state.sending) 1 else 0) - 1).coerceAtLeast(0)
            scope.launch { listState.animateScrollToItem(targetIdx) }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = RaahiBg) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f)) {
                    HeaderRow(onBack = {
                        if (step == 2 && state.messages.isEmpty()) {
                            viewModel.resetToDescribe()
                        } else {
                            onBack()
                        }
                    })
                    if (state.error != null) {
                        val isSendFailure = state.lastFailedMessage != null
                        OfflineStatusBanner(
                            message = if (state.error!!.contains("503") || state.error!!.contains("Gemini")) {
                                "AI Mechanic is temporarily busy. Please try again in a moment, or use Roadside Help / Nearby Mechanics."
                            } else {
                                state.error!!
                            },
                            onRetry = if (isSendFailure) viewModel::retry else viewModel::start,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        if (isSendFailure && !state.lastFailedMessage.isNullOrBlank() && input.isBlank()) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 2.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    "Restore question to edit",
                                    color = RaahiPrimaryTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable {
                                            input = state.lastFailedMessage!!
                                            viewModel.clearFailedMessage()
                                            viewModel.resetToDescribe()
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                    if (state.loading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = RaahiPrimaryTeal)
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    "Connecting to AI Mechanic…",
                                    color = RaahiTextDim,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        StepperRow(step)
                        when (step) {
                            1 -> DescribeStep(
                                vehicle = state.activeVehicle,
                                input = input,
                                sending = state.sending,
                                onInputChange = { input = it },
                                onAnalyze = {
                                    if (input.isNotBlank() && !state.sending) {
                                        viewModel.send(input)
                                        input = ""
                                    }
                                }
                            )
                            else -> ResultsStep(
                                state = state,
                                listState = listState,
                                input = input,
                                onInputChange = { input = it },
                                onSend = {
                                    if (input.isNotBlank() && !state.sending) {
                                        viewModel.send(input)
                                        input = ""
                                    }
                                },
                                onRequestHelp = onRequestHelp,
                                onNearbyMechanics = onNearbyMechanics,
                                onRetry = viewModel::retry,
                                onRestoreQuestion = {
                                    if (!state.lastFailedMessage.isNullOrBlank()) {
                                        input = state.lastFailedMessage!!
                                        viewModel.clearFailedMessage()
                                    }
                                    viewModel.resetToDescribe()
                                }
                            )
                        }
                    }
                }
                RaahiBottomNavBar(current = RaahiTab.AI_MECHANIC, onSelect = onNavigateTab)
            }
        }
    }
}

@Composable
private fun HeaderRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = RaahiText)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(RaahiLightTeal, RaahiShapeSmall),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = RaahiPrimaryTeal, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text("AI Mechanic", color = RaahiText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = RaahiDisplayFont)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(RaahiGreen, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text("Online · English & Hindi", color = RaahiGreen, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StepperRow(step: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepBadge(1, "Describe", step >= 1)
        StepDivider(step > 1)
        StepBadge(2, "Diagnosis & Steps", step >= 2)
    }
}

@Composable
private fun RowScope.StepBadge(index: Int, label: String, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(if (active) Brush2(RaahiPrimaryTeal) else Brush2(RaahiGlassStrong), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$index",
                color = if (active) Color.White else RaahiTextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RaahiDisplayFont
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (active) RaahiText else RaahiTextDim,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}

private fun Brush2(color: Color) = androidx.compose.ui.graphics.SolidColor(color)

@Composable
private fun RowScope.StepDivider(active: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp)
            .height(1.dp)
            .background(if (active) RaahiPrimaryTeal else RaahiBorderSoft)
    )
}

@Composable
private fun VehicleContextCard(vehicle: VehicleDto) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RaahiGlass, RaahiShapeMedium)
            .border(1.dp, RaahiPrimaryTeal.copy(alpha = 0.35f), RaahiShapeMedium)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(RaahiLightTeal, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = RaahiPrimaryTeal,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${vehicle.brand} ${vehicle.model}${if (!vehicle.variant.isNullOrBlank()) " ${vehicle.variant}" else ""}".trim(),
                        color = RaahiText,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RaahiDisplayFont
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(RaahiGreen.copy(alpha = 0.14f), RaahiShapePill)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "CONTEXT LINKED",
                            color = RaahiGreen,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                val details = listOfNotNull(
                    vehicle.fuelType.ifBlank { null },
                    vehicle.modelYear.takeIf { it > 0 }?.toString(),
                    vehicle.registrationNumber.ifBlank { null },
                    if (vehicle.odometerKm > 0) "${vehicle.odometerKm} km" else null
                )
                Text(
                    details.joinToString(" · "),
                    color = RaahiTextDim,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun DescribeStep(
    vehicle: VehicleDto?,
    input: String,
    sending: Boolean = false,
    onInputChange: (String) -> Unit,
    onAnalyze: () -> Unit
) {
    val context = LocalContext.current
    val speechAvailable = remember {
        try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Exception) {
            false
        }
    }
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                val updated = if (input.isBlank()) spoken else "$input $spoken"
                onInputChange(updated)
            }
        }
    }

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        if (vehicle != null) {
            VehicleContextCard(vehicle)
            Spacer(Modifier.height(12.dp))
        }

        // Problem Description Input Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(RaahiGlass, RaahiShapeLarge)
                .border(1.dp, RaahiBorder, RaahiShapeLarge)
                .padding(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 2000) onInputChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                placeholder = {
                    Column {
                        Text(
                            "Describe your vehicle problem…",
                            color = RaahiTextDim,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "e.g. \"Temperature gauge rising into red zone and coolant smell near radiator\"",
                            color = RaahiTextFaint,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = RaahiText,
                    unfocusedTextColor = RaahiText,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = RaahiPrimaryTeal,
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${input.length}/2000",
                    color = RaahiTextFaint,
                    fontSize = 10.5.sp,
                    modifier = Modifier.weight(1f)
                )

                if (input.isNotBlank()) {
                    IconButton(
                        onClick = { onInputChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Clear input",
                            tint = RaahiTextDim,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }

                if (speechAvailable) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(RaahiLightTeal, CircleShape)
                            .clickable {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe symptoms or sounds…")
                                }
                                speechLauncher.launch(intent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Mic,
                            contentDescription = "Voice input",
                            tint = RaahiPrimaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        GradientButton(
            onClick = onAnalyze,
            enabled = input.isNotBlank() && !sending,
            gradient = RaahiBrandGradient,
            icon = Icons.Outlined.Search,
            label = if (sending) "Diagnosing…" else "Diagnose Problem"
        )

        Spacer(Modifier.height(20.dp))

        SectionLabel("Quick Issue Categories")
        Spacer(Modifier.height(8.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allCategories = listOf("All") + ISSUE_CATEGORIES.map { it.title }
            allCategories.forEachIndexed { index, catTitle ->
                val active = selectedCategoryIndex == index
                Box(
                    modifier = Modifier
                        .background(if (active) RaahiLightTeal else RaahiGlass, RaahiShapePill)
                        .border(1.dp, if (active) RaahiPrimaryTeal else RaahiBorderSoft, RaahiShapePill)
                        .clickable { selectedCategoryIndex = index }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        catTitle,
                        color = if (active) RaahiPrimaryTealDark else RaahiTextDim,
                        fontSize = 11.5.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Categorized Issue Chips
        val activeIssues = if (selectedCategoryIndex == 0) {
            ISSUE_CATEGORIES.flatMap { cat -> cat.issues.map { cat.title to it } }
        } else {
            val cat = ISSUE_CATEGORIES[selectedCategoryIndex - 1]
            cat.issues.map { cat.title to it }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activeIssues.forEach { (catTitle, issueText) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RaahiGlass, RaahiShapeMedium)
                        .border(1.dp, RaahiBorderSoft, RaahiShapeMedium)
                        .clickable {
                            onInputChange(if (input.isBlank()) issueText else "$input, $issueText")
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                issueText,
                                color = RaahiText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                catTitle,
                                color = RaahiTextFaint,
                                fontSize = 10.5.sp
                            )
                        }
                        Text(
                            "+ Add",
                            color = RaahiPrimaryTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(RaahiAmber.copy(alpha = 0.08f), RaahiShapeMedium)
                .border(1.dp, RaahiAmber.copy(alpha = 0.25f), RaahiShapeMedium)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = RaahiAmber,
                    modifier = Modifier.size(16.dp).padding(top = 1.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Safety Note: For smoke, fire, fluid leaks, or brake issues, safely pull over immediately and do not drive. Request roadside assistance.",
                    color = RaahiTextDim,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GradientButton(
    onClick: () -> Unit,
    enabled: Boolean,
    gradient: androidx.compose.ui.graphics.Brush,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(elevation = if (enabled) 2.dp else 0.dp, shape = RaahiShapeMedium, spotColor = Color(0x10000000))
            .background(if (enabled) Brush2(RaahiPrimaryTeal) else Brush2(RaahiGlassStrong), RaahiShapeMedium)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun ColumnScope.ResultsStep(
    state: AiChatUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onRequestHelp: () -> Unit,
    onNearbyMechanics: () -> Unit,
    onRetry: () -> Unit = {},
    onRestoreQuestion: () -> Unit = {},
) {
    val context = LocalContext.current
    val speechAvailable = remember {
        try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Exception) {
            false
        }
    }
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onInputChange(if (input.isBlank()) spoken else "$input $spoken")
            }
        }
    }

    if (state.activeVehicle != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(RaahiGlass, RaahiShapePill)
                .border(1.dp, RaahiBorderSoft, RaahiShapePill)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = RaahiPrimaryTeal,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Vehicle: ${state.activeVehicle.brand} ${state.activeVehicle.model} · Active Context",
                    color = RaahiTextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (state.messages.isEmpty() && !state.sending) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 4.dp)
                        .background(RaahiGlass, RaahiShapeMedium)
                        .border(1.dp, RaahiBorder, RaahiShapeMedium)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(RaahiLightTeal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = RaahiPrimaryTeal,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Diagnosis Incomplete",
                            color = RaahiText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RaahiDisplayFont
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            state.error ?: "We could not reach the diagnosis server. Please retry or adjust your description.",
                            color = RaahiTextDim,
                            fontSize = 12.5.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 17.sp
                        )
                        if (!state.lastFailedMessage.isNullOrBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(RaahiGlassStrong, RaahiShapeSmall)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    "“${state.lastFailedMessage}”",
                                    color = RaahiTextFaint,
                                    fontSize = 11.5.sp,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onRestoreQuestion,
                                modifier = Modifier.weight(1f),
                                shape = RaahiShapeMedium,
                                border = androidx.compose.foundation.BorderStroke(1.dp, RaahiBorder)
                            ) {
                                Text("Edit Question", color = RaahiPrimaryTeal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.weight(1f),
                                shape = RaahiShapeMedium,
                                colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryTeal)
                            ) {
                                Text("Retry", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        items(state.messages, key = { it.id }) { msg ->
            ChatBubble(
                msg = msg,
                onRequestHelp = onRequestHelp,
                onNearbyMechanics = onNearbyMechanics
            )
        }
        if (state.sending) {
            item {
                AssistantTypingBubble()
            }
        }
    }

    if (state.error != null && state.messages.isNotEmpty()) {
        Text(
            state.error,
            color = RaahiRed,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text("Ask follow-up or symptom details…", color = RaahiTextDim, fontSize = 13.sp) },
            modifier = Modifier.weight(1f),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
            trailingIcon = if (speechAvailable) {
                {
                    IconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask follow-up…")
                            }
                            speechLauncher.launch(intent)
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Mic,
                            contentDescription = "Voice input",
                            tint = RaahiPrimaryTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = RaahiText,
                unfocusedTextColor = RaahiText,
                focusedBorderColor = RaahiPrimaryTeal,
                unfocusedBorderColor = RaahiBorder,
                cursorColor = RaahiPrimaryTeal,
            ),
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onSend, enabled = !state.sending && input.isNotBlank()) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(if (input.isNotBlank() && !state.sending) Brush2(RaahiPrimaryTeal) else Brush2(RaahiGlassStrong), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Send",
                    tint = if (input.isNotBlank() && !state.sending) Color.White else RaahiTextFaint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AssistantTypingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Row(
            modifier = Modifier
                .shadow(elevation = 2.dp, shape = RaahiShapeMedium, spotColor = Color(0x10000000))
                .background(RaahiGlass, RaahiShapeMedium)
                .border(1.dp, RaahiBorderSoft, RaahiShapeMedium)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(RaahiAiGradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            RaahiTypingIndicator()
        }
    }
}

@Composable
private fun ChatBubble(
    msg: AiMessageDto,
    onRequestHelp: () -> Unit,
    onNearbyMechanics: () -> Unit
) {
    val isUser = msg.role == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 330.dp)
                .then(
                    if (!isUser) Modifier.shadow(elevation = 2.dp, shape = RaahiShapeMedium, spotColor = Color(0x10000000)).border(1.dp, RaahiBorderSoft, RaahiShapeMedium)
                    else Modifier.shadow(elevation = 2.dp, shape = RaahiShapeMedium, spotColor = Color(0x15000000))
                )
                .background(
                    when {
                        msg.unavailable -> RaahiAmber.copy(alpha = 0.08f)
                        isUser -> Color(0xFF222629)
                        else -> Color.White
                    },
                    RaahiShapeMedium,
                )
                .padding(14.dp)
        ) {
            if (msg.unavailable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI Mechanic Busy", color = RaahiAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                val displayMsg = if (msg.content.contains("503") || msg.content.contains("Gemini") || msg.content.contains("Could not reach")) {
                    "AI Mechanic is temporarily busy. Please try again in a moment, or use Roadside Help / Nearby Mechanics."
                } else {
                    msg.content
                }
                Text(
                    text = displayMsg,
                    color = RaahiText,
                    fontSize = 13.sp,
                    lineHeight = 18.5.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRequestHelp,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryTeal),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Roadside Help", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onNearbyMechanics,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RaahiPrimaryTeal),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RaahiPrimaryTeal),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Nearby Garages", color = RaahiPrimaryTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (isUser) {
                Text(
                    text = msg.content,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            } else {
                // Assistant Diagnosis Response
                val isCriticalSafety = msg.content.contains("brake", ignoreCase = true) ||
                    msg.content.contains("smoke", ignoreCase = true) ||
                    msg.content.contains("fire", ignoreCase = true) ||
                    msg.content.contains("coolant leak", ignoreCase = true) ||
                    msg.content.contains("fuel leak", ignoreCase = true) ||
                    msg.content.contains("overheat", ignoreCase = true) ||
                    msg.content.contains("do not drive", ignoreCase = true) ||
                    msg.content.contains("stop driving", ignoreCase = true) ||
                    msg.content.contains("pull over", ignoreCase = true)

                val urgency = when {
                    isCriticalSafety -> "HIGH URGENCY" to RaahiRed
                    msg.content.contains("battery", ignoreCase = true) ||
                    msg.content.contains("puncture", ignoreCase = true) ||
                    msg.content.contains("flat", ignoreCase = true) -> "MODERATE" to RaahiAmber
                    else -> "ADVISORY" to RaahiGreen
                }

                // Safety Warning Banner
                if (isCriticalSafety) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RaahiRed.copy(alpha = 0.10f), RaahiShapeSmall)
                            .border(1.dp, RaahiRed.copy(alpha = 0.35f), RaahiShapeSmall)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = RaahiRed,
                                modifier = Modifier.size(15.dp).padding(top = 1.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Safety Priority: Safely pull over immediately if experiencing brake loss, engine smoke, or fluid leaks.",
                                color = RaahiRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Urgency indicator pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(urgency.second.copy(alpha = 0.12f), RaahiShapePill)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            urgency.first,
                            color = urgency.second,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Parsed diagnosis content
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    msg.content.lines().forEach { rawLine ->
                        val line = rawLine.trim()
                        if (line.isNotEmpty()) {
                            val cleaned = line
                                .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                                .replace(Regex("^#{1,3}\\s*"), "")
                            val heading = line.startsWith("### ") ||
                                line.startsWith("## ") ||
                                line.startsWith("# ") ||
                                (line.startsWith("**") && line.endsWith("**") && line.length < 70)
                            val bullet = line.startsWith("* ") ||
                                line.startsWith("- ") ||
                                line.startsWith("• ")
                            val numbered = Regex("^\\d+[.)]\\s+").containsMatchIn(line)
                            Text(
                                text = when {
                                    bullet -> "•  " + cleaned.removePrefix("* ").removePrefix("- ").removePrefix("• ")
                                    else -> cleaned
                                },
                                color = RaahiText,
                                fontSize = if (heading) 14.5.sp else 13.sp,
                                fontWeight = if (heading) FontWeight.Bold else FontWeight.Normal,
                                lineHeight = if (heading) 20.sp else 19.sp,
                                modifier = if (numbered || bullet) Modifier.padding(start = 3.dp) else Modifier
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Honest AI Disclaimer
                Text(
                    "⚠️ AI Roadside Advisory: Guidance is preliminary and cannot replace physical inspection by a certified mechanic.",
                    color = RaahiTextFaint,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )

                Spacer(Modifier.height(12.dp))

                // Contextual Roadside Help Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRequestHelp,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryTeal),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Roadside Help", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onNearbyMechanics,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RaahiPrimaryTeal),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RaahiPrimaryTeal),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Nearby Garages", color = RaahiPrimaryTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
