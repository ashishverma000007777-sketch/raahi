package `in`.raahi.app.ui.screens.commerce

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.HelperRepository
import `in`.raahi.app.network.HelperCommissionDto
import `in`.raahi.app.network.HelperEarningsDto
import `in`.raahi.app.network.HelperStatusDto
import `in`.raahi.app.network.LedgerEntryDto
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.NumberFormat
import java.util.Locale

private val WalletInk = Color(0xFF202A35)
private val WalletMuted = Color(0xFF77818D)
private val WalletCanvas = Color(0xFFF4F6F8)
private val WalletGreen = Color(0xFF16845B)
private val WalletRed = Color(0xFFCB4A4A)
private val WalletOrange = Color(0xFFE87932)

private fun walletMoney(value: Double?): String =
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value ?: 0.0)

data class WalletUiState(
    val loading: Boolean = true,
    val status: HelperStatusDto? = null,
    val commission: HelperCommissionDto? = null,
    val earnings: HelperEarningsDto? = null,
    val error: String? = null
)

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repository: HelperRepository
) : ViewModel() {
    var state by mutableStateOf(WalletUiState())
        private set

    fun refresh() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                val statusCall = async { repository.status() }
                val commissionCall = async { repository.commission() }
                val earningsCall = async { repository.earnings() }
                state = WalletUiState(
                    loading = false,
                    status = statusCall.await(),
                    commission = commissionCall.await(),
                    earnings = earningsCall.await()
                )
            } catch (e: Exception) {
                state = state.copy(
                    loading = false,
                    error = e.message?.takeIf { it.isNotBlank() }
                        ?: "Wallet data could not be loaded. Please retry."
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val state = viewModel.state
    var showPaymentInfo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        containerColor = WalletCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Payments & Wallet", fontWeight = FontWeight.Bold)
                        Text("Your earnings, transparently", fontSize = 12.sp, color = WalletMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh wallet")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WalletCanvas)
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = WalletOrange) }

            state.error != null && state.commission == null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.Refresh, null, tint = WalletOrange, modifier = Modifier.size(42.dp))
                Spacer(Modifier.height(12.dp))
                Text("Wallet unavailable", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(state.error, color = WalletMuted)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.refresh() }) { Text("Try again") }
            }

            else -> {
                val status = state.status
                val approved = status?.applicationStatus.equals("APPROVED", true)
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!approved) {
                        item {
                            WalletCard {
                                Icon(Icons.Outlined.Lock, null, tint = WalletOrange, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Wallet unlocks after approval", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Your helper application status: ${status?.applicationStatus ?: "Unavailable"}. Earnings and commission details appear here once your helper account is approved.",
                                    color = WalletMuted
                                )
                            }
                        }
                    } else {
                        val commission = state.commission
                        val earnings = state.earnings
                        val balance = commission?.balance ?: status?.commissionBalance ?: 0.0
                        val canAccept = status?.canAcceptJobs ?: true

                        item {
                            WalletCard(
                                modifier = Modifier.fillMaxWidth(),
                                color = WalletInk
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.AccountBalanceWallet, null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("OUTSTANDING BALANCE", color = Color.White.copy(alpha = 0.76f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(walletMoney(balance), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (balance < 0) "Commission to settle" else "No outstanding commission",
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                                Spacer(Modifier.height(18.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (canAccept) Color(0xFF285B4A) else Color(0xFF713E3E),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            if (canAccept) "● Eligible for jobs" else "● Job acceptance restricted",
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    TextButton(onClick = { showPaymentInfo = true }) {
                                        Text("Pay now", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item {
                            WalletCard {
                                Text("Commission limit", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Allowed outstanding", color = WalletMuted, fontSize = 12.sp)
                                        Text("₹50", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Commission rate", color = WalletMuted, fontSize = 12.sp)
                                        Text("${((commission?.rate ?: status?.commissionRate ?: 0.10) * 100).toInt()}%", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                val due = (commission?.due ?: kotlin.math.max(0.0, -balance)).coerceAtLeast(0.0)
                                val warning = when {
                                    due >= 50.0 -> "Limit reached: settle your dues before accepting more jobs."
                                    due >= 40.0 -> "High outstanding: you are close to the ₹50 limit."
                                    due >= 30.0 -> "Keep an eye on your outstanding commission."
                                    else -> null
                                }
                                if (warning != null) {
                                    Spacer(Modifier.height(10.dp))
                                    Surface(
                                        color = when {
                                            due >= 50.0 -> Color(0xFFFFE8E8)
                                            due >= 40.0 -> Color(0xFFFFF0E0)
                                            else -> Color(0xFFFFF7DF)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            warning,
                                            color = when {
                                                due >= 50.0 -> WalletRed
                                                due >= 40.0 -> Color(0xFF9A5419)
                                                else -> Color(0xFF856515)
                                            },
                                            modifier = Modifier.padding(12.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Only verified payments can clear your outstanding balance. Job eligibility is controlled by the server.",
                                    color = WalletMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        item {
                            Text("Earnings overview", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = WalletInk)
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                WalletMetric("Gross earnings", walletMoney(earnings?.grossEarnings), Modifier.weight(1f))
                                WalletMetric("Commission", walletMoney(earnings?.totalCommission), Modifier.weight(1f))
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                WalletMetric("Net earnings", walletMoney(earnings?.netEarnings), Modifier.weight(1f))
                                WalletMetric("Completed helps", (earnings?.completedJobs ?: 0L).toString(), Modifier.weight(1f))
                            }
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Recent activity", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = WalletInk, modifier = Modifier.weight(1f))
                                Text("${commission?.entries?.size ?: 0} entries", fontSize = 12.sp, color = WalletMuted)
                            }
                        }

                        val entries = commission?.entries.orEmpty()
                        if (entries.isEmpty()) {
                            item {
                                WalletCard {
                                    Icon(Icons.Outlined.ReceiptLong, null, tint = WalletMuted, modifier = Modifier.size(30.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("No transactions yet", fontWeight = FontWeight.SemiBold)
                                    Text("Your commission and payment entries will appear here.", color = WalletMuted, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(entries, key = { it.id ?: "${it.created_at}-${it.entry_type}-${it.amount}" }) { entry ->
                                WalletLedgerRow(entry)
                            }
                        }
                        if (state.error != null) {
                            item { Text("Some wallet details may be unavailable: ${state.error}", color = WalletRed, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }

    if (showPaymentInfo) {
        AlertDialog(
            onDismissRequest = { showPaymentInfo = false },
            title = { Text("Online payment coming soon") },
            text = {
                Text("Secure payment settlement will be enabled after the payment gateway is configured. No payment has been taken and your outstanding balance has not changed.")
            },
            confirmButton = {
                TextButton(onClick = { showPaymentInfo = false }) { Text("Got it") }
            }
        )
    }
}

@Composable
private fun WalletCard(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = color,
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun WalletMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(label, color = WalletMuted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(value, color = WalletInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WalletLedgerRow(entry: LedgerEntryDto) {
    val type = entry.entry_type.replace('_', ' ').lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    val isCommission = entry.entry_type.contains("COMMISSION", true)
    val isPayment = entry.entry_type.contains("PAYMENT", true) ||
        entry.entry_type.contains("SETTLEMENT", true)
    val isSettlement = entry.entry_type.contains("SETTLEMENT", true)
    val amountColor = when {
        isPayment || isSettlement -> WalletGreen
        isCommission -> WalletRed
        else -> WalletInk
    }
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = amountColor.copy(alpha = 0.10f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isPayment) Icons.Outlined.CheckCircle else if (isCommission) Icons.Outlined.Schedule else Icons.Outlined.ReceiptLong,
                        null,
                        tint = amountColor
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(type, fontWeight = FontWeight.SemiBold, color = WalletInk)
                entry.job_id?.let { Text("Help #${it.takeLast(8)}", color = WalletMuted, fontSize = 11.sp) }
                entry.created_at?.let { Text(it.replace('T', ' '), color = WalletMuted, fontSize = 11.sp) }
                if (entry.job_amount != null) {
                    Text("Help amount ${walletMoney(entry.job_amount)}", color = WalletMuted, fontSize = 11.sp)
                    val rate = entry.commission_rate
                    if (isCommission && rate != null) {
                        val ratePercent = if (rate <= 1.0) rate * 100.0 else rate
                        Text(
                            "Commission (${ratePercent.toInt()}%): ${walletMoney(kotlin.math.abs(entry.amount))}",
                            color = WalletRed,
                            fontSize = 11.sp
                        )
                    }
                }
                entry.payment_mode?.takeIf { it.isNotBlank() }?.let {
                    Text("Payment: $it", color = WalletMuted, fontSize = 11.sp)
                }
                entry.reference?.takeIf { it.isNotBlank() }?.let {
                    Text("Ref: $it", color = WalletMuted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                (if (entry.amount > 0) "+" else if (entry.amount < 0) "−" else "") +
                    walletMoney(kotlin.math.abs(entry.amount)),
                color = amountColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
