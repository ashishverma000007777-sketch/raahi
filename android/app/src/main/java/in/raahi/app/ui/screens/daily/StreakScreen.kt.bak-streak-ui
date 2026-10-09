package `in`.raahi.app.ui.screens.daily

import java.time.LocalDate
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.DailyRepository
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.network.StreakDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import `in`.raahi.app.network.toUserFriendlyMessage
import `in`.raahi.app.ui.components.GlassCard
import `in`.raahi.app.ui.components.OfflineStatusBanner
import `in`.raahi.app.ui.components.RaahiPrimaryButton
import `in`.raahi.app.ui.components.RaahiScreenHeader
import `in`.raahi.app.ui.components.SectionLabel
import `in`.raahi.app.ui.theme.*
import javax.inject.Inject

data class StreakUiState(
    val loading: Boolean = true,
    val streak: StreakDto? = null,
    val checkingIn: Boolean = false,
    val error: String? = null,
    val reward: String? = null
)

@HiltViewModel
class StreakViewModel @Inject constructor(
    private val repository: DailyRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StreakUiState())
    val state: StateFlow<StreakUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (!authRepository.hasAuthToken()) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.streak() }
                .onSuccess { s -> _state.update { it.copy(loading = false, streak = s) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toUserFriendlyMessage("Could not load streak. Check your connection.")) } }
        }
    }

    fun checkIn() {
        _state.update { it.copy(checkingIn = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.checkin() }
                .onSuccess { s -> _state.update { it.copy(checkingIn = false, streak = s, reward = s.reward) } }
                .onFailure { e -> _state.update { it.copy(checkingIn = false, error = e.toUserFriendlyMessage("Check-in failed. Check your connection.")) } }
        }
    }
}

private data class DayStreakInfo(
    val dayLabel: String,
    val dayNumber: String,
    val isToday: Boolean,
    val isChecked: Boolean,
)

@Composable
fun StreakScreen(
    onBack: () -> Unit,
    viewModel: StreakViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "flameGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flamePulse"
    )

    val activeScale by animateFloatAsState(
        targetValue = if (state.checkingIn) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "activeFlameScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RaahiBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            RaahiScreenHeader(
                title = "Daily Streak",
                onBack = onBack,
                subtitle = "Check in daily to build your habit and earn perks"
            )

            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RaahiOrange)
                }
            } else {
                val streak = state.streak
                val canCheckin = streak?.canCheckin == true
                val currentStreakCount = streak?.currentStreak ?: 0

                val today = remember { LocalDate.now() }
                val calendarDays = remember(today, currentStreakCount, canCheckin) {
                    (6 downTo 0).map { offset ->
                        val date = today.minusDays(offset.toLong())
                        val isChecked = if (!canCheckin) {
                            offset < currentStreakCount
                        } else {
                            offset in 1..currentStreakCount
                        }
                        DayStreakInfo(
                            dayLabel = date.dayOfWeek.name.take(3),
                            dayNumber = date.dayOfMonth.toString(),
                            isToday = offset == 0,
                            isChecked = isChecked
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Streak Hero Card
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Animated flame with layered glow
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(pulseScale * activeScale),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(RaahiOrange.copy(alpha = 0.22f), Color.Transparent)
                                            ),
                                            CircleShape
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .shadow(elevation = 8.dp, shape = CircleShape, spotColor = RaahiOrange.copy(alpha = 0.35f))
                                        .background(
                                            Brush.linearGradient(listOf(Color(0xFFFF6B4A), RaahiOrange)),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = "Daily Streak Flame",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            Text(
                                text = "$currentStreakCount",
                                color = RaahiText,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = RaahiDisplayFont,
                                lineHeight = 44.sp
                            )
                            Text(
                                text = if (currentStreakCount == 1) "DAY STREAK" else "DAYS STREAK",
                                color = RaahiOrange,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )

                            Spacer(Modifier.height(20.dp))

                            HorizontalDivider(
                                color = RaahiBorderSoft,
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${streak?.longestStreak ?: 0} days",
                                        color = RaahiText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Longest Streak",
                                        color = RaahiTextDim,
                                        fontSize = 11.5.sp
                                    )
                                }
                                Box(
                                    Modifier
                                        .width(1.dp)
                                        .height(28.dp)
                                        .background(RaahiBorderSoft)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${streak?.totalCheckins ?: 0}",
                                        color = RaahiText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Total Check-ins",
                                        color = RaahiTextDim,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Calendar below the streak
                    SectionLabel(
                        text = "LAST 7 DAYS",
                        trailing = if (!canCheckin) "Checked in today ✓" else "Pending today"
                    )

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                calendarDays.forEach { day ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = day.dayLabel,
                                            color = if (day.isToday) RaahiOrange else RaahiTextDim,
                                            fontSize = 11.sp,
                                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .then(
                                                    when {
                                                        day.isChecked -> Modifier
                                                            .shadow(elevation = 2.dp, shape = CircleShape, spotColor = RaahiGreen.copy(alpha = 0.4f))
                                                            .background(RaahiGreen, CircleShape)
                                                        day.isToday && canCheckin -> Modifier
                                                            .background(RaahiOrange.copy(alpha = 0.12f), CircleShape)
                                                            .border(1.5.dp, RaahiOrange, CircleShape)
                                                        else -> Modifier
                                                            .background(Color(0xFFF1F5F9), CircleShape)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (day.isChecked) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = "Completed",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = day.dayNumber,
                                                    color = if (day.isToday) RaahiOrange else RaahiTextFaint,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (day.isToday) "Today" else "",
                                            color = RaahiOrange,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                if (!canCheckin) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = RaahiGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "You've completed your check-in for today!",
                                        color = RaahiGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = RaahiOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Check in today to keep your streak going!",
                                        color = RaahiOrange,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (state.reward == "7_day_milestone") {
                        Spacer(Modifier.height(16.dp))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            background = Color(0xFFFFFBEB),
                            borderColor = RaahiAmber.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎉", fontSize = 22.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "7-Day Milestone Reached!",
                                        color = Color(0xFF92400E),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Great dedication! Your driver tier score has received a boost.",
                                        color = Color(0xFFB45309),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }

                    if (state.error != null) {
                        Spacer(Modifier.height(14.dp))
                        OfflineStatusBanner(
                            message = state.error!!,
                            onRetry = viewModel::refresh
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    // Check-in Button
                    RaahiPrimaryButton(
                        text = if (canCheckin) "Check In Today" else "Checked In for Today ✓",
                        onClick = viewModel::checkIn,
                        enabled = canCheckin && !state.checkingIn,
                        loading = state.checkingIn,
                        container = if (canCheckin) RaahiOrange else Color(0xFF64748B)
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
