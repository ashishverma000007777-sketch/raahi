package `in`.raahi.app.ui.screens.daily

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.DailyRepository
import `in`.raahi.app.network.TipDto
import `in`.raahi.app.network.toUserFriendlyMessage
import `in`.raahi.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyTipViewModel @Inject constructor(private val repository: DailyRepository) : ViewModel() {
    private val _tip = MutableStateFlow<TipDto?>(null)
    val tip: StateFlow<TipDto?> = _tip.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadTip()
    }

    fun loadTip() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching { repository.todaysTip() }
                .onSuccess {
                    _tip.value = it
                    _isLoading.value = false
                }
                .onFailure {
                    _error.value = it.toUserFriendlyMessage("Could not load today's tip. Check your connection.")
                    _isLoading.value = false
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTipsScreen(onBack: () -> Unit, viewModel: DailyTipViewModel = hiltViewModel()) {
    val tip by viewModel.tip.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily Tip",
                            color = RaahiText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = RaahiDisplayFont
                        )
                        Text(
                            text = "Automotive Maintenance & Longevity",
                            color = RaahiTextDim,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RaahiText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaahiBg)
            )
        },
        containerColor = RaahiBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            when {
                isLoading -> {
                    DailyTipLoadingSkeleton()
                }
                error != null -> {
                    DailyTipErrorCard(
                        errorMessage = error!!,
                        onRetry = viewModel::loadTip
                    )
                }
                tip != null -> {
                    DailyTipDetailCard(tip = tip!!)
                }
                else -> {
                    DailyTipEmptyCard(onRefresh = viewModel::loadTip)
                }
            }
        }
    }
}

@Composable
private fun DailyTipDetailCard(tip: TipDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x10000000))
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, RaahiBorderSoft, RoundedCornerShape(20.dp))
            .padding(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .background(RaahiOrange.copy(alpha = 0.12f), RaahiShapePill)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "DAILY CARE",
                    color = RaahiOrange,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(RaahiAmber.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = RaahiAmber,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = tip.title,
            color = RaahiText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = tip.body,
            color = RaahiTextDim,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RaahiSelectionBg, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Build,
                contentDescription = null,
                tint = RaahiOrange,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Pro Tip: Consistent preventive checks extend vehicle health and minimize costly repairs.",
                color = RaahiOrangeDark,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DailyTipLoadingSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x10000000))
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, RaahiBorderSoft, RoundedCornerShape(20.dp))
            .padding(22.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 20.dp)
                .clip(RaahiShapePill)
                .background(RaahiBorderSoft.copy(alpha = alpha))
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(RaahiBorderSoft.copy(alpha = alpha))
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(RaahiBorderSoft.copy(alpha = alpha))
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(RaahiBorderSoft.copy(alpha = alpha))
        )
    }
}

@Composable
private fun DailyTipErrorCard(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x10000000))
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, RaahiBorderSoft, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(RaahiRed.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = RaahiRed,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Unable to load today's tip",
            color = RaahiText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = errorMessage,
            color = RaahiTextDim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
            shape = RaahiShapeMedium
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Try Again", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DailyTipEmptyCard(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x10000000))
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, RaahiBorderSoft, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No Tip Available Yet",
            color = RaahiText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Today's maintenance tip is currently being curated. Check back in a few moments.",
            color = RaahiTextDim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = onRefresh,
            shape = RaahiShapeMedium
        ) {
            Text("Refresh", color = RaahiOrange, fontWeight = FontWeight.SemiBold)
        }
    }
}
