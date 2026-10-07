package `in`.raahi.app.ui.screens.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.network.HelperJobHistoryDto
import `in`.raahi.app.network.JobDto
import `in`.raahi.app.ui.components.*
import `in`.raahi.app.ui.theme.*
import kotlinx.coroutines.delay

/** Helper Dashboard: online/offline, requests, active job, history, earnings/commission, rating, profile. */
@Composable
fun HelperDashboardScreen(
    onBack: () -> Unit,
    onJobAccepted: (String) -> Unit,
    onOpenJob: (String) -> Unit,
    onApplyOrReview: () -> Unit,
    viewModel: HelperDashboardViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsState()

    LaunchedEffect(s.acceptedJobId) {
        s.acceptedJobId?.let { id -> viewModel.consumeAccepted(); onJobAccepted(id) }
    }
    // New requests arrive while the helper is online: refresh the list every 15 s.
    LaunchedEffect(s.tab, s.status?.online) {
        while (s.tab == DashTab.REQUESTS && s.status?.online == true) {
            delay(15_000)
            viewModel.refreshTab()
        }
    }

    Surface(Modifier.fillMaxSize(), color = RaahiBg) {
        Column(Modifier.fillMaxSize()) {
            RaahiScreenHeader(title = "Helper Dashboard", subtitle = "Earn with Raahi", onBack = onBack)
            val st = s.status
            when {
                s.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RaahiOrange) }
                st == null -> ErrorBlock(s.error ?: "Could not load your helper status", viewModel::load)
                st.applicationStatus != "APPROVED" -> NotApprovedBlock(st.applicationStatus, onApplyOrReview)
                else -> Column(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        OnlineCard(online = st.online, canGoOnline = st.accountActive && st.blockedUntil == null, busy = s.togglingOnline, onToggle = viewModel::setOnline)
                        if (st.blockedUntil != null) {
                            Spacer(Modifier.height(10.dp))
                            Text("Your account is blocked until ${st.blockedUntil.take(10)}${st.blockReason?.let { " — $it" } ?: ""}.", color = RaahiRed, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        }
                        if (st.commissionDue) {
                            Spacer(Modifier.height(10.dp))
                            CommissionDueBanner(due = -st.commissionBalance, rate = st.commissionRate)
                        }
                        s.error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = RaahiRed, fontSize = 12.5.sp, fontWeight = FontWeight.Medium) }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashTab.entries.forEach { t -> RaahiChip(t.label, active = s.tab == t) { viewModel.selectTab(t) } }
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                        if (s.tabLoading) {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RaahiOrange, modifier = Modifier.size(22.dp), strokeWidth = 2.dp) }
                        }
                        when (s.tab) {
                            DashTab.REQUESTS -> RequestsTab(s, st.online, st.canAcceptJobs, viewModel)
                            DashTab.ACTIVE -> ActiveTab(s.active, onOpenJob)
                            DashTab.HISTORY -> HistoryTab(s.history)
                            DashTab.EARNINGS -> EarningsTab(s)
                            DashTab.PROFILE -> ProfileTab(s, onApplyOrReview)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = RaahiTextDim, fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            RaahiOutlineButton("Try again", onClick = onRetry)
        }
    }
}

@Composable
private fun NotApprovedBlock(status: String, onApplyOrReview: () -> Unit) {
    val (title, body, cta) = when (status) {
        "PENDING" -> Triple("Application under review", "You'll be able to go online once Raahi approves your documents.", "View application status")
        "REJECTED" -> Triple("Application not approved", "Check the reason and apply again.", "View details")
        "SUSPENDED" -> Triple("Helper account suspended", "Contact Raahi support to review your account.", "View details")
        else -> Triple("Become a Raahi helper", "Earn by helping drivers on the road. Complete a short onboarding to get started.", "Start application")
    }
    Column(Modifier.padding(16.dp)) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(title, color = RaahiText, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
                Spacer(Modifier.height(6.dp))
                Text(body, color = RaahiTextDim, fontSize = 13.5.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        RaahiPrimaryButton(cta, onClick = onApplyOrReview)
    }
}

@Composable
private fun OnlineCard(online: Boolean, canGoOnline: Boolean, busy: Boolean, onToggle: (Boolean) -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (online) "You're online" else "You're offline", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(if (online) "Nearby customers can request your help" else "Go online to receive nearby requests", color = RaahiTextDim, fontSize = 12.sp)
            }
            if (busy) {
                CircularProgressIndicator(color = RaahiOrange, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Switch(
                    checked = online, onCheckedChange = onToggle, enabled = canGoOnline,
                    colors = SwitchDefaults.colors(checkedTrackColor = RaahiOrange, checkedThumbColor = Color.White),
                )
            }
        }
    }
}

@Composable
private fun CommissionDueBanner(due: Double, rate: Double) {
    Column(Modifier.fillMaxWidth().background(RaahiRed.copy(alpha = 0.10f), RaahiShapeMedium).padding(14.dp)) {
        Text("Commission due ₹${"%.2f".format(due)}", color = RaahiRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text("Raahi's ${(rate * 100).toInt()}% commission on completed jobs must be settled before you can accept new jobs. Contact Raahi support after paying to have it recorded.", color = RaahiText, fontSize = 12.sp)
    }
}

// ------------------------------------------------------------------ tabs

@Composable
private fun RequestsTab(s: HelperDashUiState, online: Boolean, canAccept: Boolean, vm: HelperDashboardViewModel) {
    val visible = s.requests.filter { it.id !in s.dismissed }
    if (!online) { EmptyNote("Go online to see nearby requests."); return }
    if (visible.isEmpty()) { EmptyNote("No nearby requests right now. New requests appear here automatically."); return }
    visible.forEach { job ->
        GlassCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(PROBLEM_TYPES.firstOrNull { it.id == job.problemType }?.label ?: job.problemType, color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            listOfNotNull(job.distanceKm?.let { "${"%.1f".format(it)} km away" }, job.requesterName).joinToString(" · "),
                            color = RaahiTextDim, fontSize = 12.sp,
                        )
                    }
                    Text("₹${job.rewardAmount.toInt()}", color = RaahiOrange, fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = RaahiDisplayFont)
                }
                if (!job.problemDesc.isNullOrBlank()) { Spacer(Modifier.height(8.dp)); Text(job.problemDesc, color = RaahiTextDim, fontSize = 12.5.sp) }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { RaahiOutlineButton("Decline", onClick = { vm.decline(job.id) }, tint = RaahiTextDim) }
                    Box(Modifier.weight(1f)) {
                        RaahiPrimaryButton("Accept", onClick = { vm.accept(job.id) }, enabled = canAccept, loading = s.acceptingId == job.id)
                    }
                }
                if (!canAccept) {
                    Spacer(Modifier.height(6.dp))
                    Text("You can't accept jobs right now (commission due or account restriction).", color = RaahiRed, fontSize = 11.5.sp)
                }
            }
        }
    }
}

@Composable
private fun ActiveTab(jobs: List<JobDto>, onOpen: (String) -> Unit) {
    if (jobs.isEmpty()) { EmptyNote("No active job. Accept a request to get started."); return }
    jobs.forEach { job ->
        GlassCard(Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onOpen(job.id) }) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(PROBLEM_TYPES.firstOrNull { it.id == job.problemType }?.label ?: job.problemType, color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(job.requesterName, color = RaahiTextDim, fontSize = 12.sp)
                }
                RaahiStatusPill(job.status.replace("_", " "), RaahiOrange)
            }
        }
    }
}

@Composable
private fun HistoryTab(items: List<HelperJobHistoryDto>) {
    if (items.isEmpty()) { EmptyNote("Completed and cancelled jobs will show up here."); return }
    items.forEach { h ->
        GlassCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(PROBLEM_TYPES.firstOrNull { it.id == h.problemType }?.label ?: (h.problemType ?: "Job"), color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(listOfNotNull((h.completedAt ?: h.createdAt)?.take(10), h.paymentMode).joinToString(" · "), color = RaahiTextDim, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹${(h.amount ?: 0.0).toInt()}", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    RaahiStatusPill(h.status, if (h.status == "COMPLETED") RaahiGreen else RaahiRed)
                }
            }
        }
    }
}

@Composable
private fun EarningsTab(s: HelperDashUiState) {
    val e = s.earnings
    val c = s.commission
    if (e == null || c == null) { EmptyNote("Earnings will appear after your first completed job."); return }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("NET EARNINGS", color = RaahiTextFaint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            Text("₹${"%.2f".format(e.netEarnings)}", color = RaahiText, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
            Spacer(Modifier.height(10.dp))
            StatRow("Completed jobs", e.completedJobs.toString())
            StatRow("Customers paid you", "₹${"%.2f".format(e.grossEarnings)}")
            StatRow("Raahi commission (${(c.rate * 100).toInt()}%)", "₹${"%.2f".format(e.totalCommission)}")
            StatRow("Commission balance", if (c.balance < 0) "-₹${"%.2f".format(-c.balance)}" else "₹0.00", valueColor = if (c.balance < 0) RaahiRed else RaahiGreen)
        }
    }
    Spacer(Modifier.height(10.dp))
    Text("Raahi does not hold customer money. You collect the job amount directly; the commission above is what you owe Raahi.", color = RaahiTextDim, fontSize = 11.5.sp)
    Spacer(Modifier.height(14.dp))
    Text("COMMISSION LEDGER", color = RaahiTextFaint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    Spacer(Modifier.height(8.dp))
    if (c.entries.isEmpty()) EmptyNote("No ledger entries yet.")
    c.entries.forEach { en ->
        GlassCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (en.entry_type == "COMMISSION") "Commission" else "Settlement", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(listOfNotNull(en.job_amount?.let { "on ₹${it.toInt()}" }, en.payment_mode, en.reference).joinToString(" · "), color = RaahiTextDim, fontSize = 11.5.sp)
                }
                Text(
                    (if (en.amount < 0) "-" else "+") + "₹${"%.2f".format(kotlin.math.abs(en.amount))}",
                    color = if (en.amount < 0) RaahiRed else RaahiGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun ProfileTab(s: HelperDashUiState, onApplyOrReview: () -> Unit) {
    val st = s.status ?: return
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(st.name ?: "Helper", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = RaahiDisplayFont)
            Text(st.phone ?: "", color = RaahiTextDim, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(18.dp))
                Text(" ${"%.1f".format(st.ratingAvg)} · ${st.totalHelps} jobs completed", color = RaahiText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            StatRow("Services", st.services?.split(",")?.joinToString { id -> PROBLEM_TYPES.firstOrNull { it.id.equals(id, true) }?.label ?: id } ?: "—")
            StatRow("Service radius", st.serviceRadiusKm?.let { "$it km" } ?: "—")
            StatRow("Commission rate", "${(st.commissionRate * 100).toInt()}%")
        }
    }
    Spacer(Modifier.height(12.dp))
    RaahiOutlineButton("View application", onClick = onApplyOrReview)
    Spacer(Modifier.height(16.dp))
    Text("RECENT RATINGS", color = RaahiTextFaint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    Spacer(Modifier.height(8.dp))
    if (s.ratings.isEmpty()) EmptyNote("Ratings from customers will appear here.")
    s.ratings.forEach { r ->
        GlassCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("★".repeat(r.stars) + "☆".repeat(5 - r.stars), color = RaahiAmber, fontSize = 15.sp)
                if (!r.comment.isNullOrBlank()) Text(r.comment, color = RaahiText, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color = RaahiText) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = RaahiTextDim, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(text, color = RaahiTextDim, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp))
}
