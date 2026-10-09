package `in`.raahi.app.ui.screens.aimechanic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.network.AiMessageDto
import `in`.raahi.app.ui.components.*
import `in`.raahi.app.ui.theme.*
import kotlinx.coroutines.launch

private val TRY_THESE = listOf(
    "Tyre puncture ho gaya",
    "Engine heat ho raha hai",
    "Battery down hai",
    "Petrol khatam",
    "AC kaam nahi kar raha"
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

    // Step 1: Initial describe screen. Once messages exist or when user sends, transition directly to chat
    val step = if (state.messages.isEmpty() && !state.sending) 1 else 3

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
                    HeaderRow(onBack)
                    if (state.error != null) {
                        OfflineStatusBanner(
                            message = if (state.error!!.contains("503") || state.error!!.contains("Gemini")) {
                                "AI Mechanic is temporarily busy. Please try again in a moment, or use Roadside Help / Nearby Mechanics."
                            } else {
                                state.error!!
                            },
                            onRetry = viewModel::start,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    if (state.loading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = RaahiOrange)
                        }
                    } else {
                        StepperRow(step)
                        when (step) {
                            1 -> DescribeStep(
                                input = input,
                                onInputChange = { input = it },
                                onAnalyze = {
                                    if (input.isNotBlank()) {
                                        viewModel.send(input)
                                        input = ""
                                    }
                                },
                                onSuggestion = { viewModel.send(it) }
                            )
                            else -> ResultsStep(
                                state = state,
                                listState = listState,
                                input = input,
                                onInputChange = { input = it },
                                onSend = {
                                    if (input.isNotBlank()) {
                                        viewModel.send(input)
                                        input = ""
                                    }
                                },
                                onRequestHelp = onRequestHelp,
                                onNearbyMechanics = onNearbyMechanics
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
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = RaahiText)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(RaahiAiGradient, RaahiShapeSmall),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepBadge(1, "Describe", step)
        StepDivider(step > 1)
        StepBadge(2, "Troubleshoot", step)
    }
}

@Composable
private fun RowScope.StepBadge(index: Int, label: String, currentStep: Int) {
    val active = (index == 1 && currentStep == 1) || (index == 2 && currentStep >= 2)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(if (active) RaahiBrandGradient else Brush2(RaahiGlassStrong), CircleShape),
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
            .background(if (active) RaahiOrange else RaahiBorderSoft)
    )
}

@Composable
private fun DescribeStep(
    input: String,
    onInputChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onSuggestion: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(RaahiGlass, RaahiShapeLarge)
                .border(1.dp, RaahiBorderSoft, RaahiShapeLarge)
                .padding(4.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                placeholder = {
                    Column {
                        Text("Describe your vehicle problem...", color = RaahiTextDim, fontSize = 13.5.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("e.g. \"Engine is overheating when AC is on\"", color = RaahiTextFaint, fontSize = 11.5.sp)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = RaahiText,
                    unfocusedTextColor = RaahiText,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = RaahiOrange,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${input.length} chars", color = RaahiTextFaint, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(RaahiGlassStrong, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Mic, contentDescription = "Voice input", tint = RaahiTextDim, modifier = Modifier.size(14.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        GradientButton(
            onClick = onAnalyze,
            enabled = input.isNotBlank(),
            gradient = RaahiBrandGradient,
            icon = Icons.Outlined.Search,
            label = "Diagnose Problem"
        )
        Spacer(Modifier.height(18.dp))
        SectionLabel("Common Roadside Issues")
        SuggestionFlow(TRY_THESE, onSuggestion)
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
            .shadow(elevation = if (enabled) 4.dp else 0.dp, shape = RaahiShapeMedium, spotColor = RaahiOrange)
            .background(if (enabled) gradient else Brush2(RaahiGlassStrong), RaahiShapeMedium)
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
private fun SuggestionFlow(items: List<String>, onClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { text ->
                    Box(
                        modifier = Modifier
                            .background(RaahiGlass, RaahiShapePill)
                            .border(1.dp, RaahiBorderSoft, RaahiShapePill)
                            .clickable { onClick(text) }
                            .padding(horizontal = 13.dp, vertical = 9.dp),
                    ) {
                        Text(text, color = RaahiTextDim, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
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
    onNearbyMechanics: () -> Unit
) {
    var feedback by remember { mutableStateOf<Boolean?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
        if (!state.sending && state.messages.any { it.role != "USER" }) {
            item { FeedbackRow(feedback) { feedback = it } }
        }
    }

    if (state.error != null) {
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
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text("Ask follow-up or symptom details…", color = RaahiTextDim, fontSize = 13.sp) },
            modifier = Modifier.weight(1f),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = RaahiText,
                unfocusedTextColor = RaahiText,
                focusedBorderColor = RaahiOrange,
                unfocusedBorderColor = RaahiBorder,
                cursorColor = RaahiOrange,
            ),
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onSend, enabled = !state.sending && input.isNotBlank()) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(if (input.isNotBlank() && !state.sending) RaahiBrandGradient else Brush2(RaahiGlassStrong), CircleShape),
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
                .background(Color.White, RaahiShapeMedium)
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
private fun FeedbackRow(picked: Boolean?, onPick: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Was this guidance helpful?", color = RaahiTextFaint, fontSize = 11.sp)
        Spacer(Modifier.width(10.dp))
        FeedbackButton(Icons.Outlined.CheckCircle, picked == true, RaahiGreen) { onPick(true) }
        Spacer(Modifier.width(8.dp))
        FeedbackButton(Icons.Outlined.WarningAmber, picked == false, RaahiRed) { onPick(false) }
    }
}

@Composable
private fun FeedbackButton(icon: androidx.compose.ui.graphics.vector.ImageVector, picked: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(if (picked) color.copy(alpha = 0.18f) else RaahiGlass, CircleShape)
            .border(1.dp, if (picked) Color.Transparent else RaahiBorderSoft, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (picked) color else RaahiTextDim, modifier = Modifier.size(14.dp))
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
                .widthIn(max = 310.dp)
                .then(
                    if (!isUser) Modifier.shadow(elevation = 2.dp, shape = RaahiShapeMedium, spotColor = Color(0x10000000)).border(1.dp, RaahiBorderSoft, RaahiShapeMedium)
                    else Modifier.shadow(elevation = 3.dp, shape = RaahiShapeMedium, spotColor = RaahiOrange)
                )
                .background(
                    when {
                        msg.unavailable -> RaahiAmber.copy(alpha = 0.08f)
                        isUser -> RaahiOrange
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
                        colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Roadside Help", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onNearbyMechanics,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RaahiOrange),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RaahiOrange),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Nearby Garages", color = RaahiOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(
                    text = msg.content,
                    color = if (isUser) Color.White else RaahiText,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}
