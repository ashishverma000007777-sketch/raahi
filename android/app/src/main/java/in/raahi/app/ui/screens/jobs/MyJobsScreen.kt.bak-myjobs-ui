package `in`.raahi.app.ui.screens.jobs

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.History
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
import `in`.raahi.app.network.JobDto
import `in`.raahi.app.ui.components.OfflineStatusBanner
import `in`.raahi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyJobsScreen(
    onBack: () -> Unit,
    onOpenJob: (jobId: String) -> Unit,
    viewModel: MyJobsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Box(Modifier.fillMaxSize().background(RaahiBg)) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Jobs",
                            color = RaahiText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RaahiDisplayFont
                        )
                        Text(
                            text = "Roadside Assistance Workflow & Requests",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when (val s = state) {
                is MyJobsUiState.Loading -> {
                    MyJobsSkeleton()
                }
                is MyJobsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
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
                                Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = RaahiRed, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Connection Problem", color = RaahiText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(s.message, color = RaahiTextDim, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = viewModel::refresh,
                                colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                                shape = RaahiShapeMedium
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is MyJobsUiState.Loaded -> {
                    Column(Modifier.fillMaxSize()) {
                        if (s.errorMessage != null) {
                            OfflineStatusBanner(
                                message = s.errorMessage,
                                onRetry = viewModel::refresh,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                        if (s.jobs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x10000000))
                                        .background(Color.White, RoundedCornerShape(20.dp))
                                        .border(1.dp, RaahiBorderSoft, RoundedCornerShape(20.dp))
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(RaahiOrange.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Assignment, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(26.dp))
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    Text("No Assistance Requests Yet", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Whenever you request roadside assistance or accept a helper task, the job tracker and live ETA will appear here.",
                                        color = RaahiTextDim,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    OutlinedButton(
                                        onClick = viewModel::refresh,
                                        shape = RaahiShapeMedium
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Check Again", color = RaahiOrange, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(s.jobs, key = { it.id }) { job ->
                                    MyJobRow(job, onClick = { onOpenJob(job.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun MyJobsSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "jobs_skeleton")
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
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(14.dp), spotColor = Color(0x0C000000))
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .border(1.dp, RaahiBorderSoft, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(RaahiBorderSoft.copy(alpha = alpha))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(RaahiBorderSoft.copy(alpha = alpha))
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(RaahiBorderSoft.copy(alpha = alpha))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 22.dp)
                        .clip(RaahiShapePill)
                        .background(RaahiBorderSoft.copy(alpha = alpha))
                )
            }
        }
    }
}

@Composable
private fun MyJobRow(job: JobDto, onClick: () -> Unit) {
    val problem = PROBLEM_TYPES.firstOrNull { it.id == job.problemType }
    val statusColor = when (job.status) {
        "PENDING" -> RaahiYellow
        "MATCHED", "ARRIVED", "IN_PROGRESS" -> RaahiCyan
        "WORK_DONE" -> RaahiYellow
        "COMPLETED" -> RaahiGreen
        "CANCELLED", "EXPIRED" -> RaahiRed
        else -> RaahiTextMuted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0x10000000))
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, RaahiBorderSoft, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(RaahiOrange.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(problem?.emoji ?: "❓", fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = problem?.label ?: job.problemType,
                color = RaahiText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            )
            Text(
                text = if (job.viewerRole == "HELPER") "As helper · ₹${job.rewardAmount.toInt()}" else "Your request · ₹${job.rewardAmount.toInt()}",
                color = RaahiTextDim,
                fontSize = 11.5.sp,
            )
        }
        Box(
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = job.status.replace('_', ' '),
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
