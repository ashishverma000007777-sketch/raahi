package `in`.raahi.app.ui.screens.jobs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.network.JobDto
import `in`.raahi.app.network.WsConnectionState
import `in`.raahi.app.ui.components.*
import `in`.raahi.app.ui.screens.jobs.JobStatusUiState.*
import `in`.raahi.app.ui.theme.*

private val STEPS = listOf("PENDING", "MATCHED", "EN_ROUTE", "ARRIVED", "IN_PROGRESS", "WORK_DONE", "COMPLETED")
private val STEP_LABELS = mapOf(
    "PENDING" to "Requested", "MATCHED" to "Helper assigned", "EN_ROUTE" to "Helper on the way",
    "ARRIVED" to "Helper arrived", "IN_PROGRESS" to "Work in progress", "WORK_DONE" to "Work completed",
    "COMPLETED" to "Job complete",
)

@Composable
fun JobStatusScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: JobStatusViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val wsState by viewModel.wsConnectionState.collectAsState(initial = WsConnectionState.CONNECTING)

    Surface(modifier = Modifier.fillMaxSize(), color = RaahiBg) {
        Column(Modifier.fillMaxSize()) {
            RaahiScreenHeader(
                title = "Help Request", onBack = onBack,
                subtitle = when (wsState) {
                    WsConnectionState.CONNECTED -> "Live"
                    WsConnectionState.CONNECTING -> "Connecting…"
                    WsConnectionState.DISCONNECTED -> "Reconnecting…"
                },
            )
            when (val s = state) {
                is Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RaahiOrange)
                }
                is Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = RaahiTextDim, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(14.dp))
                        RaahiOutlineButton("Try again", onClick = viewModel::refresh)
                    }
                }
                is Loaded -> JobStatusBody(s, viewModel, onDone)
            }
        }
    }
}

@Composable
private fun JobStatusBody(s: Loaded, vm: JobStatusViewModel, onDone: () -> Unit) {
    val job = s.job
    val isHelper = job.viewerRole == "HELPER"
    val isRequester = job.viewerRole == "REQUESTER"
    var dialog by remember { mutableStateOf<JobDialog?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 36.dp)) {
        if (job.viewerRole == null && job.status == "PENDING") {
            // Helper cancelled before arrival: the request is open again for other helpers.
            InfoCard("You are no longer assigned to this request.", RaahiAmber)
            Spacer(Modifier.height(16.dp))
            RaahiPrimaryButton("Back to Home", onClick = onDone)
            return@Column
        }

        // LIVE_HELPER_TRACKING_MAP (Top of tracking screen, Zomato/Blinkit style)
        if ((isRequester || isHelper) && job.status in setOf("PENDING", "MATCHED", "EN_ROUTE", "ARRIVED", "IN_PROGRESS")) {
            LiveJobTrackingMap(
                customerLat = job.lat,
                customerLng = job.lng,
                helperLocation = s.liveLocation,
                helperName = if (isHelper) "You" else job.helperName,
                jobStatus = job.status,
                isHelperViewer = isHelper,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            )
            Spacer(Modifier.height(14.dp))
        }

        StepperCard(job)
        Spacer(Modifier.height(14.dp))
        JobDetailsCard(job)

        val otherName = if (isHelper) job.requesterName else job.helperName
        if (otherName != null) {
            Spacer(Modifier.height(14.dp))
            OtherPartyCard(
                name = otherName,
                phone = if (isHelper) job.requesterPhone else job.helperPhone,
                rating = if (isHelper) null else job.helperRatingAvg,
                helps = if (isHelper) null else job.helperTotalHelps,
                label = if (isHelper) "CUSTOMER" else "YOUR HELPER",
            )
        }

        if (s.actionError != null) {
            Spacer(Modifier.height(12.dp))
            Text(s.actionError, color = RaahiRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(16.dp))
        when {
            isRequester -> RequesterActions(job, s.acting, vm, onDone, onDialog = { dialog = it })
            isHelper -> HelperActions(job, s.acting, vm, onDone, onDialog = { dialog = it })
        }

        // After arrival normal cancellation is gone: Report Issue / Contact Support instead.
        if ((isRequester || isHelper) && job.status in setOf("ARRIVED", "IN_PROGRESS", "WORK_DONE")) {
            Spacer(Modifier.height(10.dp))
            RaahiOutlineButton("Report Issue / Contact Support", onClick = { dialog = JobDialog.REPORT }, tint = RaahiAmber)
        }
    }

    when (dialog) {
        JobDialog.CANCEL -> CancellationReasonDialog(
            isHelper = isHelper,
            isEnRoute = job.status == "EN_ROUTE",
            onConfirm = { text, code -> vm.cancel(text, code); dialog = null },
            onDismiss = { dialog = null },
        )
        JobDialog.REPORT -> TextPromptDialog(
            title = "Report an issue", body = "Tell us what went wrong. Our support team will review it.",
            hint = "Describe the issue", confirm = "Send report", dismiss = "Close", required = true,
            onConfirm = { vm.report(it); dialog = null }, onDismiss = { dialog = null },
        )
        JobDialog.WORK_DONE -> WorkDoneDialog(
            suggested = (job.finalAmount ?: job.rewardAmount),
            onConfirm = { amount, mode -> vm.workDone(amount, mode); dialog = null },
            onDismiss = { dialog = null },
        )
        null -> {}
    }
}

private enum class JobDialog { CANCEL, REPORT, WORK_DONE }

// ------------------------------------------------------------------ role actions

@Composable
private fun RequesterActions(job: JobDto, acting: Boolean, vm: JobStatusViewModel, onDone: () -> Unit, onDialog: (JobDialog) -> Unit) {
    when (job.status) {
        "PENDING" -> {
            InfoCard("Searching for nearby helpers…", RaahiAmber)
            Spacer(Modifier.height(12.dp))
            RaahiOutlineButton("Cancel request", onClick = { onDialog(JobDialog.CANCEL) }, tint = RaahiRed)
        }
        "MATCHED" -> {
            InfoCard("Helper assigned! Preparing to travel.", RaahiAmber)
            Spacer(Modifier.height(10.dp))
            CodeCard("Show this arrival code to your helper", job.helperOtp)
            if (job.cancelAllowed == true) {
                Spacer(Modifier.height(12.dp))
                RaahiOutlineButton("Cancel request", onClick = { onDialog(JobDialog.CANCEL) }, tint = RaahiRed)
            }
        }
        "EN_ROUTE" -> {
            InfoCard("Helper is on the way to your location!", RaahiCyan)
            Spacer(Modifier.height(10.dp))
            CodeCard("Show this arrival code to your helper", job.helperOtp)
            if (job.cancelAllowed == true) {
                Spacer(Modifier.height(12.dp))
                RaahiOutlineButton("Cancel request", onClick = { onDialog(JobDialog.CANCEL) }, tint = RaahiRed)
            }
        }
        "ARRIVED" -> InfoCard("Your helper has arrived. Cancellation is no longer available — use Report Issue if something is wrong.", RaahiCyan)
        "IN_PROGRESS" -> InfoCard("Your helper is working on your vehicle.", RaahiGreen)
        "WORK_DONE" -> ConfirmCompletionCard(job, acting, onConfirm = vm::confirmCompletion)
        "COMPLETED" -> {
            if (job.rated == true) {
                InfoCard("Thanks for rating your helper.", RaahiGreen)
                Spacer(Modifier.height(12.dp))
                RaahiPrimaryButton("Back to Home", onClick = onDone)
            } else {
                RatingCard(acting, onSubmit = vm::rate)
            }
        }
        "CANCELLED", "EXPIRED" -> {
            CancellationResultCard(job = job, isHelper = false, onDone = onDone)
        }
    }
}

@Composable
private fun HelperActions(job: JobDto, acting: Boolean, vm: JobStatusViewModel, onDone: () -> Unit, onDialog: (JobDialog) -> Unit) {
    val context = LocalContext.current
    when (job.status) {
        "MATCHED" -> {
            InfoCard("Job accepted. When you depart, tap 'Start travelling' so the customer can track your route and travel protection is activated.", RaahiAmber)
            Spacer(Modifier.height(12.dp))
            RaahiPrimaryButton("Start travelling / On my way", onClick = vm::startEnRoute, loading = acting)
            Spacer(Modifier.height(10.dp))
            RaahiOutlineButton("Navigate to customer", onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${job.lat},${job.lng}?q=${job.lat},${job.lng}")))
                }
            })
            Spacer(Modifier.height(12.dp))
            ArrivalCard(acting, onArrive = vm::arrive)
            Spacer(Modifier.height(12.dp))
            RaahiOutlineButton("Cancel job", onClick = { onDialog(JobDialog.CANCEL) }, tint = RaahiRed)
        }
        "EN_ROUTE" -> {
            InfoCard("You are en route to the customer. Travel-loss protection is active.", RaahiGreen)
            Spacer(Modifier.height(12.dp))
            RaahiOutlineButton("Navigate to customer", onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${job.lat},${job.lng}?q=${job.lat},${job.lng}")))
                }
            })
            Spacer(Modifier.height(12.dp))
            ArrivalCard(acting, onArrive = vm::arrive)
            Spacer(Modifier.height(12.dp))
            RaahiOutlineButton("Cancel job", onClick = { onDialog(JobDialog.CANCEL) }, tint = RaahiRed)
        }
        "ARRIVED" -> RaahiPrimaryButton("Start work", onClick = vm::startWork, loading = acting)
        "IN_PROGRESS" -> RaahiPrimaryButton("Work completed", onClick = { onDialog(JobDialog.WORK_DONE) }, loading = acting, container = RaahiGreen)
        "WORK_DONE" -> InfoCard("Waiting for the customer to confirm completion.", RaahiAmber)
        "COMPLETED" -> {
            InfoCard(
                buildString {
                    append("Job complete.")
                    job.finalAmount?.let { append(" Customer paid you ₹${it.toInt()}${job.paymentMode?.let { m -> " ($m)" } ?: ""}.") }
                    job.commissionAmount?.takeIf { it > 0 }?.let { append(" Raahi commission ₹${"%.2f".format(it)} is due — settle it to keep accepting jobs.") }
                },
                RaahiGreen,
            )
            Spacer(Modifier.height(12.dp))
            RaahiPrimaryButton("Back to dashboard", onClick = onDone)
        }
        "CANCELLED", "EXPIRED" -> {
            CancellationResultCard(job = job, isHelper = true, onDone = onDone)
        }
    }
}

// ------------------------------------------------------------------ cards

@Composable
private fun StepperCard(job: JobDto) {
    if (job.status == "CANCELLED" || job.status == "EXPIRED") {
        InfoCard(if (job.status == "EXPIRED") "Expired" else "Cancelled", RaahiRed)
        return
    }
    val current = STEPS.indexOf(job.status).coerceAtLeast(0)
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            STEPS.forEachIndexed { i, step ->
                val done = i < current || (job.status == "COMPLETED")
                val active = i == current && job.status != "COMPLETED"
                val tint = when { done -> RaahiGreen; active -> RaahiOrange; else -> RaahiTextFaint }
                Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(tint, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        STEP_LABELS[step] ?: step,
                        color = if (done || active) RaahiText else RaahiTextFaint,
                        fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
            if (job.arrivalMethod != null && job.status != "PENDING" && job.status != "MATCHED" && job.status != "EN_ROUTE") {
                Spacer(Modifier.height(4.dp))
                Text("Arrival verified by ${job.arrivalMethod!!.replace("_", " + ")}", color = RaahiTextDim, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun JobDetailsCard(job: JobDto) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            DetailRow("Problem", PROBLEM_TYPES.firstOrNull { it.id == job.problemType }?.label ?: job.problemType)
            if (!job.problemDesc.isNullOrBlank()) DetailRow("Description", job.problemDesc)
            DetailRow("Offer", "₹${job.rewardAmount.toInt()} cash to helper")
            if (job.finalAmount != null) DetailRow("Final amount", "₹${job.finalAmount.toInt()}${job.paymentMode?.let { " · $it" } ?: ""}")
            DetailRow("Job ID", "#${job.id.take(8).uppercase()}")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = RaahiTextDim, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = RaahiText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OtherPartyCard(name: String, phone: String?, rating: Double?, helps: Int?, label: String) {
    val context = LocalContext.current
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(label, color = RaahiTextFaint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).background(RaahiSelectionBg, CircleShape), contentAlignment = Alignment.Center) {
                    Text(name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = RaahiOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (rating != null && helps != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(13.dp))
                            Text(" ${"%.1f".format(rating)} · $helps helps", color = RaahiTextDim, fontSize = 12.sp)
                        }
                    }
                    if (!phone.isNullOrBlank()) Text(phone, color = RaahiTextDim, fontSize = 11.sp)
                }
                if (!phone.isNullOrBlank()) {
                    Box(
                        Modifier.size(38.dp).background(RaahiGreen.copy(alpha = 0.14f), CircleShape)
                            .clickable { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) } },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Call, contentDescription = "Call", tint = RaahiGreen, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(text: String, tint: Color) {
    Row(
        Modifier.fillMaxWidth().background(tint.copy(alpha = 0.10f), RaahiShapeMedium).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = RaahiText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CodeCard(label: String, code: String?) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = RaahiTextDim, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Text(code ?: "------", color = RaahiOrange, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp, fontFamily = RaahiDisplayFont)
        }
    }
}

@Composable
private fun ArrivalCard(acting: Boolean, onArrive: (String?) -> Unit) {
    var otp by remember { mutableStateOf("") }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Arrived at the location?", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "We confirm arrival with your GPS position. If GPS is weak, enter the arrival code the customer shows you.",
                color = RaahiTextDim, fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            RaahiTextField(
                value = otp, onValueChange = { if (it.length <= 6) otp = it.filter(Char::isDigit) },
                placeholder = "Arrival code (optional)", keyboardType = KeyboardType.Number,
            )
            Spacer(Modifier.height(12.dp))
            RaahiPrimaryButton("I've arrived", onClick = { onArrive(otp.ifBlank { null }) }, loading = acting)
        }
    }
}

@Composable
private fun ConfirmCompletionCard(job: JobDto, acting: Boolean, onConfirm: (String) -> Unit) {
    var otp by remember { mutableStateOf("") }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Confirm the work is done", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Your helper marked the work completed" +
                    (job.finalAmount?.let { " and asked for ₹${it.toInt()}${job.paymentMode?.let { m -> " ($m)" } ?: ""}" } ?: "") +
                    ". Pay the helper directly, then enter your completion code to confirm.",
                color = RaahiTextDim, fontSize = 12.5.sp,
            )
            Spacer(Modifier.height(12.dp))
            CodeCard("Your completion code", job.completionOtp)
            Spacer(Modifier.height(12.dp))
            RaahiTextField(
                value = otp, onValueChange = { if (it.length <= 6) otp = it.filter(Char::isDigit) },
                placeholder = "Enter completion code", keyboardType = KeyboardType.Number,
            )
            Spacer(Modifier.height(12.dp))
            RaahiPrimaryButton("Confirm completion", onClick = { onConfirm(otp) }, enabled = otp.length == 6, loading = acting, container = RaahiGreen)
        }
    }
}

@Composable
private fun RatingCard(acting: Boolean, onSubmit: (Int, String?) -> Unit) {
    var stars by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Rate your helper", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { i ->
                    Icon(
                        if (i <= stars) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "$i stars",
                        tint = if (i <= stars) RaahiAmber else RaahiTextFaint,
                        modifier = Modifier.size(36.dp).clickable { stars = i },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            RaahiTextField(value = comment, onValueChange = { if (it.length <= 500) comment = it }, placeholder = "Add a comment (optional)", singleLine = false, minLines = 2)
            Spacer(Modifier.height(12.dp))
            RaahiPrimaryButton("Submit rating", onClick = { onSubmit(stars, comment.ifBlank { null }) }, enabled = stars > 0, loading = acting)
        }
    }
}

// ------------------------------------------------------------------ dialogs

@Composable
private fun TextPromptDialog(
    title: String, body: String, hint: String, confirm: String, dismiss: String, required: Boolean,
    onConfirm: (String) -> Unit, onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RaahiBg2,
        title = { Text(title, color = RaahiText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(body, color = RaahiTextDim, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                RaahiTextField(value = text, onValueChange = { if (it.length <= 500) text = it }, placeholder = hint, singleLine = false, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = !required || text.isNotBlank()) {
                Text(confirm, color = RaahiOrange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss, color = RaahiTextDim) } },
    )
}

@Composable
private fun WorkDoneDialog(suggested: Double, onConfirm: (Double?, String) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf(suggested.toInt().toString()) }
    var mode by remember { mutableStateOf("CASH") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RaahiBg2,
        title = { Text("Work completed", color = RaahiText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter what the customer pays you. Raahi never holds this money — the customer pays you directly.", color = RaahiTextDim, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                RaahiTextField(
                    value = amount, onValueChange = { if (it.length <= 6) amount = it.filter(Char::isDigit) },
                    placeholder = "Final amount", keyboardType = KeyboardType.Number, prefix = "₹ ",
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RaahiChip("Cash", active = mode == "CASH") { mode = "CASH" }
                    RaahiChip("UPI", active = mode == "UPI") { mode = "UPI" }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(amount.toDoubleOrNull(), mode) }, enabled = amount.isNotBlank()) {
                Text("Send for confirmation", color = RaahiOrange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back", color = RaahiTextDim) } },
    )
}

@Composable
private fun CancellationReasonDialog(
    isHelper: Boolean,
    isEnRoute: Boolean,
    onConfirm: (reason: String, reasonCode: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val reasons = if (isHelper) listOf(
        "VEHICLE_EMERGENCY" to "Vehicle issue / Breakdown",
        "UNSAFE_CONDITIONS" to "Unsafe conditions / Road hazard",
        "CUSTOMER_UNREACHABLE" to "Customer unreachable / No response",
        "INCORRECT_LOCATION" to "Incorrect or inaccessible location",
        "HELPER_CANCELLED" to "Unable to assist at this time",
        "OTHER" to "Other reason",
    ) else listOf(
        "CUSTOMER_CANCELLED" to "Change of plans / No longer needed",
        "CUSTOMER_CANCELLED" to "Found alternate help",
        "CUSTOMER_CANCELLED" to "Helper taking too long",
        "INCORRECT_LOCATION" to "Entered incorrect location / address",
        "VEHICLE_EMERGENCY" to "Emergency resolved",
        "OTHER" to "Other reason",
    )

    var selectedIndex by remember { mutableIntStateOf(0) }
    var explanation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RaahiBg2,
        title = {
            Text(
                if (isHelper) "Cancel Roadside Job?" else "Cancel Help Request?",
                color = RaahiText,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (isEnRoute && !isHelper) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(RaahiAmber.copy(alpha = 0.12f), RaahiShapeMedium)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Helper is already en route. Raahi's En-Route Protection Policy applies to reimburse verified travel expenses.",
                            color = RaahiAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                } else if (isHelper) {
                    Text(
                        "Vehicle emergencies and hazardous conditions are exempt from reliability penalties.",
                        color = RaahiTextDim,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                }

                Text("Select cancellation reason:", color = RaahiText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                reasons.forEachIndexed { i, (code, label) ->
                    val selected = i == selectedIndex
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                if (selected) RaahiOrange.copy(alpha = 0.14f) else Color.Transparent,
                                RaahiShapeSmall
                            )
                            .clickable { selectedIndex = i }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(14.dp)
                                .background(if (selected) RaahiOrange else RaahiTextFaint, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            label,
                            color = if (selected) RaahiOrange else RaahiText,
                            fontSize = 12.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                RaahiTextField(
                    value = explanation,
                    onValueChange = { if (it.length <= 250) explanation = it },
                    placeholder = "Additional explanation (optional)",
                    singleLine = false,
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val (code, label) = reasons[selectedIndex]
                val fullReason = if (explanation.isNotBlank()) "$label: ${explanation.trim()}" else label
                onConfirm(fullReason, code)
            }) {
                Text("Confirm Cancellation", color = RaahiRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Job", color = RaahiTextDim)
            }
        },
    )
}

@Composable
private fun CancellationResultCard(job: JobDto, isHelper: Boolean, onDone: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = RaahiRed, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (job.status == "EXPIRED") "Request Expired" else "Request Cancelled",
                    color = RaahiText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (!job.cancellationReasonCode.isNullOrBlank() || !job.cancellationStage.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                val reasonTitle = when (job.cancellationReasonCode) {
                    "VEHICLE_EMERGENCY" -> "Vehicle Issue / Emergency"
                    "UNSAFE_CONDITIONS" -> "Unsafe / Hazardous Conditions"
                    "CUSTOMER_UNREACHABLE" -> "Customer Unreachable"
                    "INCORRECT_LOCATION" -> "Incorrect Location"
                    "CUSTOMER_CANCELLED" -> "Cancelled by Customer"
                    "HELPER_CANCELLED" -> "Cancelled by Helper"
                    else -> job.cancellationReasonCode ?: "Cancelled"
                }
                DetailRow("Reason", reasonTitle)
                if (!job.cancellationStage.isNullOrBlank()) {
                    DetailRow("Stage", if (job.cancellationStage == "EN_ROUTE") "En-Route Travel" else "Pre-Dispatch")
                }
            }

            // Travel Loss Protection details
            if (job.travelLossStatus != null) {
                Spacer(Modifier.height(14.dp))
                val badgeColor = when (job.travelLossStatus) {
                    "ADMIN_APPROVED" -> RaahiGreen
                    "ELIGIBLE_PENDING_REVIEW" -> RaahiAmber
                    "REJECTED" -> RaahiRed
                    else -> RaahiTextDim
                }
                val badgeText = when (job.travelLossStatus) {
                    "ADMIN_APPROVED" -> {
                        if (job.payoutStatus == "PAID") "Reimbursement Paid: ₹${job.travelLossApproved?.toInt() ?: 0}"
                        else "Approved by Admin (₹${job.travelLossApproved?.toInt() ?: 0} approved, settlement pending)"
                    }
                    "ELIGIBLE_PENDING_REVIEW" -> "Pending Admin Review (₹${job.travelLossEstimate?.toInt() ?: 0} est.)"
                    "REJECTED" -> "Reimbursement Claim Rejected"
                    "NOT_ELIGIBLE" -> "Travel Reimbursement Ineligible"
                    else -> "Travel Protection Evaluated"
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(badgeColor.copy(alpha = 0.12f), RaahiShapeMedium)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).background(badgeColor, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(badgeText, color = badgeColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }

                if (job.travelDistanceKm != null && job.travelDistanceKm > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${job.travelDistanceKm} km verified travel distance",
                        color = RaahiTextDim,
                        fontSize = 11.5.sp,
                    )
                }

                if (!job.travelLossNotes.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (job.travelLossStatus) {
                            "REJECTED" -> "Decision: ${job.travelLossNotes}"
                            "ADMIN_APPROVED" -> "Admin Note: ${job.travelLossNotes}"
                            else -> job.travelLossNotes ?: ""
                        },
                        color = RaahiTextDim,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    if (isHelper) "Under Raahi's Roadside Assistance Cancellation Policy, travel-loss claims are reviewed by support admins. Payout settlement is processed separately upon review approval."
                    else "Under Raahi's Roadside Assistance Protection Policy, helper dispatch costs are covered by Raahi's protection fund. No late cancellation fee has been charged to your account.",
                    color = RaahiTextDim,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                )
            } else if (job.status == "CANCELLED") {
                Spacer(Modifier.height(8.dp))
                Text(
                    "This roadside assistance request was closed before travel was incurred.",
                    color = RaahiTextDim,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(16.dp))
            RaahiPrimaryButton(if (isHelper) "Back to Dashboard" else "Back to Home", onClick = onDone)
        }
    }
}
