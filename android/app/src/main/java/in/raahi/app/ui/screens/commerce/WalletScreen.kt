package `in`.raahi.app.ui.screens.commerce

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import `in`.raahi.app.ui.components.RaahiCard
import `in`.raahi.app.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Locale

/**
 * Standardized currency formatting adhering to automotive product standards:
 * - Negative amounts: -₹250
 * - Zero amounts: ₹0
 * - Positive delta: +₹500
 * - Positive balance: ₹500
 */
fun formatWalletCurrency(value: Double?, isDelta: Boolean = false): String {
    val amount = value ?: 0.0
    val isZero = kotlin.math.abs(amount) < 0.0001
    val isInt = kotlin.math.abs(amount % 1.0) < 0.0001
    val numStr = if (isInt) {
        "%,d".format(Locale("en", "IN"), kotlin.math.abs(amount).toLong())
    } else {
        String.format(Locale("en", "IN"), "%,.2f", kotlin.math.abs(amount))
    }

    if (isZero) return "₹0"

    return if (isDelta) {
        if (amount > 0) "+₹$numStr" else "-₹$numStr"
    } else {
        if (amount < 0) "-₹$numStr" else "₹$numStr"
    }
}

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
                val status = repository.status()
                val approved = status.applicationStatus.equals("APPROVED", ignoreCase = true)
                if (approved) {
                    val commissionCall = async { repository.commission() }
                    val earningsCall = async { repository.earnings() }
                    state = WalletUiState(
                        loading = false,
                        status = status,
                        commission = commissionCall.await(),
                        earnings = earningsCall.await()
                    )
                } else {
                    state = WalletUiState(
                        loading = false,
                        status = status,
                        commission = null,
                        earnings = null
                    )
                }
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
        containerColor = RaahiBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Payments & Wallet",
                            fontWeight = FontWeight.Bold,
                            color = RaahiText,
                            fontSize = 18.sp,
                            fontFamily = RaahiDisplayFont,
                        )
                        Text(
                            text = "Commission settlement & earnings history",
                            fontSize = 11.5.sp,
                            color = RaahiTextDim,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = RaahiText,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh",
                            tint = RaahiText,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RaahiBg,
                    titleContentColor = RaahiText,
                )
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RaahiOrange)
            }

            state.error != null && state.commission == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    tint = RaahiOrange,
                    modifier = Modifier.size(42.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Wallet unavailable",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = RaahiText,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = state.error,
                    color = RaahiTextDim,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.refresh() },
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Try Again")
                }
            }

            else -> {
                val status = state.status
                val approved = status?.applicationStatus.equals("APPROVED", ignoreCase = true)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!approved) {
                        item {
                            RaahiCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = RaahiBg2,
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(RaahiOrange.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = null,
                                            tint = RaahiOrange,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = "Wallet unlocks after verification",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RaahiText,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Your helper application status: ${status?.applicationStatus ?: "Pending"}. Full commission and ledger tracking will activate once verified by Raahi dispatch.",
                                        color = RaahiTextDim,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                    )
                                }
                            }
                        }
                    } else {
                        val commission = state.commission
                        val earnings = state.earnings
                        val balance = commission?.balance ?: status?.commissionBalance ?: 0.0
                        val canAccept = status?.canAcceptJobs ?: true

                        // HERO: Balance Card
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = RaahiDarkSurface,
                                border = BorderStroke(1.dp, Color(0xFF1E2D44)),
                                shadowElevation = 2.dp,
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "OUTSTANDING BALANCE",
                                                color = Color.White.copy(alpha = 0.75f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.6.sp,
                                            )
                                        }

                                        Surface(
                                            color = if (canAccept) Color(0xFF14532D) else Color(0xFF5A2A2A),
                                            shape = RoundedCornerShape(50),
                                            border = BorderStroke(1.dp, if (canAccept) RaahiSuccess else RaahiError),
                                        ) {
                                            Text(
                                                text = if (canAccept) "● Eligible for jobs" else "● Dues restricted",
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = formatWalletCurrency(balance, isDelta = false),
                                            color = Color.White,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = RaahiDisplayFont,
                                        )
                                        Text(
                                            text = when {
                                                balance < 0 -> "Commission due for completed services"
                                                balance == 0.0 -> "All dues clear (₹0 outstanding)"
                                                else -> "Available balance in your favor"
                                            },
                                            color = Color.White.copy(alpha = 0.70f),
                                            fontSize = 12.5.sp,
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { showPaymentInfo = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = RaahiDeepTeal),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text("Settle Dues", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Commission Rules & Outstanding Limit
                        item {
                            RaahiCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = RaahiBg2,
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Commission Terms",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RaahiText,
                                        fontFamily = RaahiDisplayFont,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Allowed Outstanding", color = RaahiTextDim, fontSize = 11.5.sp)
                                            Text("₹50", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RaahiText)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Commission Rate", color = RaahiTextDim, fontSize = 11.5.sp)
                                            val ratePct = ((commission?.rate ?: status?.commissionRate ?: 0.10) * 100).toInt()
                                            Text("$ratePct%", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RaahiText)
                                        }
                                    }

                                    val due = (commission?.due ?: kotlin.math.max(0.0, -balance)).coerceAtLeast(0.0)
                                    val warning = when {
                                        due >= 50.0 -> "Limit reached: Settle your outstanding ₹${due.toInt()} dues to continue accepting jobs."
                                        due >= 40.0 -> "Close to limit: You have ₹${due.toInt()} outstanding against ₹50 limit."
                                        else -> null
                                    }
                                    if (warning != null) {
                                        Surface(
                                            color = if (due >= 50.0) Color(0xFFFFECEC) else Color(0xFFFFF7ED),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, if (due >= 50.0) Color(0xFFFCA5A5) else Color(0xFFFDBA74)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = warning,
                                                color = if (due >= 50.0) Color(0xFFDC2626) else Color(0xFFC2410C),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(10.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Earnings Overview Section
                        item {
                            Text(
                                text = "Earnings Overview",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = RaahiText,
                                fontFamily = RaahiDisplayFont,
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetricCard("Gross Earnings", formatWalletCurrency(earnings?.grossEarnings), Modifier.weight(1f))
                                MetricCard("Total Commission", formatWalletCurrency(earnings?.totalCommission), Modifier.weight(1f))
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetricCard("Net Earnings", formatWalletCurrency(earnings?.netEarnings), Modifier.weight(1f), isNet = true)
                                MetricCard("Completed Helps", "${earnings?.completedJobs ?: 0}", Modifier.weight(1f))
                            }
                        }

                        // Recent Activity Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Transactions",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RaahiText,
                                    fontFamily = RaahiDisplayFont,
                                )
                                Text(
                                    text = "${commission?.entries?.size ?: 0} entries",
                                    fontSize = 12.sp,
                                    color = RaahiTextDim,
                                )
                            }
                        }

                        val entries = commission?.entries.orEmpty()
                        if (entries.isEmpty()) {
                            item {
                                RaahiCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = RaahiBg2,
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ReceiptLong,
                                            contentDescription = null,
                                            tint = RaahiTextDim,
                                            modifier = Modifier.size(36.dp),
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "No transactions yet",
                                            fontWeight = FontWeight.Bold,
                                            color = RaahiText,
                                            fontSize = 14.sp
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "Your commission debits and payment credits will appear here.",
                                            color = RaahiTextDim,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(entries, key = { it.id ?: "${it.created_at}-${it.entry_type}-${it.amount}" }) { entry ->
                                WalletLedgerRow(entry)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPaymentInfo) {
        AlertDialog(
            onDismissRequest = { showPaymentInfo = false },
            containerColor = RaahiBg2,
            title = {
                Text(
                    text = "Online Settlement",
                    color = RaahiText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Instant UPI and net-banking settlement gateway will be active in the next release. Currently, cash and manual settlement with Raahi dispatch are recorded in your ledger.",
                    color = RaahiTextDim,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = { showPaymentInfo = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Got It")
                }
            }
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isNet: Boolean = false,
) {
    Surface(
        modifier = modifier,
        color = RaahiBg2,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, RaahiBorderSoft),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                color = RaahiTextDim,
                fontSize = 11.5.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                color = if (isNet) Color(0xFF22B573) else RaahiText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun WalletLedgerRow(entry: LedgerEntryDto) {
    val isCommission = entry.entry_type.contains("COMMISSION", true)
    val isPayment = entry.entry_type.contains("PAYMENT", true) || entry.entry_type.contains("SETTLEMENT", true)

    val isDebit = entry.amount < 0 || (isCommission && entry.amount != 0.0)
    val isCredit = entry.amount > 0 || (isPayment && entry.amount != 0.0)
    val isZero = kotlin.math.abs(entry.amount) < 0.0001

    val badgeText = when {
        isDebit -> "DEBIT"
        isCredit -> "CREDIT"
        else -> "SETTLED"
    }

    val badgeColor = when {
        isDebit -> Color(0xFFE5484D)
        isCredit -> Color(0xFF22B573)
        else -> RaahiTextDim
    }

    val amountColor = when {
        isDebit -> Color(0xFFE5484D)
        isCredit -> Color(0xFF22B573)
        else -> RaahiText
    }

    val title = when {
        isCommission -> "Commission Fee"
        isPayment -> "Settlement Payment"
        else -> entry.entry_type.replace('_', ' ').lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    val formattedAmount = when {
        isZero -> "₹0"
        isDebit -> "-₹${"%,d".format(Locale("en", "IN"), kotlin.math.abs(entry.amount).toLong())}"
        else -> "+₹${"%,d".format(Locale("en", "IN"), kotlin.math.abs(entry.amount).toLong())}"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RaahiBg2,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, RaahiBorderSoft),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isCredit) Icons.Outlined.CheckCircle else if (isCommission) Icons.Outlined.Schedule else Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Transaction Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = RaahiText
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                val subDetails = buildList {
                    entry.job_id?.takeIf { it.isNotBlank() }?.let { add("Job #${it.takeLast(6)}") }
                    entry.created_at?.takeIf { it.isNotBlank() }?.let { add(it.take(16).replace('T', ' ')) }
                }.joinToString(" · ")

                if (subDetails.isNotBlank()) {
                    Text(
                        text = subDetails,
                        color = RaahiTextDim,
                        fontSize = 11.5.sp
                    )
                }

                if (entry.job_amount != null) {
                    val rate = entry.commission_rate
                    val rateStr = if (rate != null) " (${((if (rate <= 1.0) rate * 100.0 else rate)).toInt()}%)" else ""
                    Text(
                        text = "Job ₹${entry.job_amount.toInt()}$rateStr",
                        color = RaahiTextDim,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Amount with explicit +/- sign
            Text(
                text = formattedAmount,
                color = amountColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
    }
}
