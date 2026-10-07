package `in`.raahi.app.ui.screens.helper

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.data.HelperApplicationForm
import `in`.raahi.app.data.VehicleCatalogue
import `in`.raahi.app.ui.components.*
import `in`.raahi.app.ui.screens.jobs.PROBLEM_TYPES
import `in`.raahi.app.ui.screens.vehicle.VehiclePickerDialog
import `in`.raahi.app.ui.theme.*

private val EQUIPMENT = listOf("Jack", "Jump starter", "Tow rope", "Toolkit", "Air compressor", "Fuel can", "OBD scanner", "Tow truck")
private val VEHICLE_TYPES = listOf("Bike" to "BIKE", "Car" to "CAR", "Van" to "VAN", "Tow truck" to "TOW_TRUCK")

/** Earn with Raahi: shows application status, or the onboarding wizard for new/rejected applicants. */
@Composable
fun HelperApplicationScreen(
    onBack: () -> Unit,
    onOpenDashboard: () -> Unit,
    viewModel: HelperApplicationViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsState()
    BackHandler(enabled = s.applying && s.step != WizardStep.BASICS) { viewModel.back() }

    Surface(Modifier.fillMaxSize(), color = RaahiBg) {
        Column(Modifier.fillMaxSize()) {
            RaahiScreenHeader(
                title = if (s.applying) "Become a Helper" else "Earn with Raahi",
                subtitle = if (s.applying) "Step ${s.step.ordinal + 1} of ${WizardStep.entries.size} · ${s.step.title}" else null,
                onBack = { if (!viewModel.back()) onBack() },
            )
            when {
                s.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RaahiOrange) }
                s.applying -> Wizard(s, viewModel)
                s.status != null -> StatusView(s, viewModel, onOpenDashboard)
                else -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.error ?: "Something went wrong", color = RaahiTextDim, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(14.dp))
                        RaahiOutlineButton("Try again", onClick = viewModel::refresh)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ status states

@Composable
private fun StatusView(s: HelperApplicationUiState, vm: HelperApplicationViewModel, onOpenDashboard: () -> Unit) {
    val st = s.status!!
    val (tint, title, body) = when (st.applicationStatus) {
        "PENDING" -> Triple(RaahiAmber, "Application under review", "We are verifying your documents. This usually takes a short while — we will notify you as soon as it is done.")
        "APPROVED" -> Triple(RaahiGreen, "You're an approved helper", "Go online from your dashboard to start receiving nearby requests.")
        "REJECTED" -> Triple(RaahiRed, "Application not approved", st.rejectionReason?.let { "Reason: $it" } ?: "Please check your details and apply again.")
        "SUSPENDED" -> Triple(RaahiRed, "Helper account suspended", st.suspensionReason?.let { "Reason: $it. Contact Raahi support to review your account." } ?: "Contact Raahi support to review your account.")
        else -> Triple(RaahiTextDim, "No application yet", "")
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                RaahiStatusPill(st.applicationStatus, tint)
                Spacer(Modifier.height(12.dp))
                Text(title, color = RaahiText, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
                Spacer(Modifier.height(6.dp))
                Text(body, color = RaahiTextDim, fontSize = 13.5.sp)
            }
        }
        if (st.applicationStatus == "APPROVED" && st.commissionDue) {
            Spacer(Modifier.height(12.dp))
            Text("Commission due: ₹${"%.2f".format(-st.commissionBalance)}. Settle it to accept new jobs.", color = RaahiRed, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(20.dp))
        when (st.applicationStatus) {
            "APPROVED" -> RaahiPrimaryButton("Open Helper Dashboard", onClick = onOpenDashboard)
            "REJECTED" -> RaahiPrimaryButton("Apply again", onClick = vm::startApplication)
            "PENDING" -> RaahiOutlineButton("Refresh status", onClick = vm::refresh)
            else -> {}
        }
        s.error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = RaahiRed, fontSize = 13.sp) }
    }
}

// ------------------------------------------------------------------ wizard

@Composable
private fun Wizard(s: HelperApplicationUiState, vm: HelperApplicationViewModel) {
    val steps = WizardStep.entries
    Column(Modifier.fillMaxSize()) {
        // progress dots
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            steps.forEach {
                Box(Modifier.weight(1f).height(4.dp).background(if (it.ordinal <= s.step.ordinal) RaahiOrange else RaahiBorder, CircleShape))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            when (s.step) {
                WizardStep.BASICS -> BasicsStep(s.form, vm)
                WizardStep.SERVICES -> ChipStep("What can you help with?", PROBLEM_TYPES.map { it.label to it.id.uppercase() }, s.form.services) { sel ->
                    vm.update { it.copy(services = sel) }
                }
                WizardStep.EQUIPMENT -> ChipStep("Equipment you carry (optional)", EQUIPMENT.map { it to it }, s.form.equipment) { sel ->
                    vm.update { it.copy(equipment = sel) }
                }
                WizardStep.KYC -> KycStep(s.form, vm)
                WizardStep.VEHICLE -> VehicleStep(s.form, vm)
                WizardStep.PAYOUT -> PayoutStep(s.form, vm)
                WizardStep.REVIEW -> ReviewStep(s.form)
            }
            s.error?.let { Spacer(Modifier.height(12.dp)); Text(it, color = RaahiRed, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
        }
        Column(Modifier.padding(16.dp)) {
            if (s.step == WizardStep.REVIEW) {
                RaahiPrimaryButton("Submit application", onClick = vm::submit, loading = s.submitting)
            } else {
                RaahiPrimaryButton("Continue", onClick = vm::next)
            }
        }
    }
}

@Composable
private fun StepTitle(text: String, hint: String? = null) {
    Text(text, color = RaahiText, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
    if (hint != null) { Spacer(Modifier.height(4.dp)); Text(hint, color = RaahiTextDim, fontSize = 12.5.sp) }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = RaahiTextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp, top = 12.dp))
}

@Composable
private fun BasicsStep(f: HelperApplicationForm, vm: HelperApplicationViewModel) {
    StepTitle("Tell us about you", "Your name and verified phone number come from your Raahi account — nothing to re-enter.")
    FieldLabel("Years of experience")
    RaahiTextField(f.experienceYears, { v -> vm.update { it.copy(experienceYears = v.filter(Char::isDigit).take(2)) } }, "e.g. 5", keyboardType = KeyboardType.Number)
    FieldLabel("Service area")
    RaahiTextField(f.serviceArea, { v -> vm.update { it.copy(serviceArea = v.take(120)) } }, "e.g. Mohali, Chandigarh, Zirakpur")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipStep(title: String, options: List<Pair<String, String>>, selected: Set<String>, onChange: (Set<String>) -> Unit) {
    StepTitle(title, "Tap to select. You can select more than one.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (label, id) ->
            RaahiChip(label, active = id in selected) { onChange(if (id in selected) selected - id else selected + id) }
        }
    }
}

@Composable
private fun KycStep(f: HelperApplicationForm, vm: HelperApplicationViewModel) {
    StepTitle("Verify your identity", "Documents are encrypted and only used for Raahi's verification.")
    UploadRow("Aadhaar — front", f.aadhaarFront) { uri -> vm.update { it.copy(aadhaarFront = uri) } }
    Spacer(Modifier.height(10.dp))
    UploadRow("Aadhaar — back", f.aadhaarBack) { uri -> vm.update { it.copy(aadhaarBack = uri) } }
    Spacer(Modifier.height(10.dp))
    UploadRow("Selfie", f.selfie) { uri -> vm.update { it.copy(selfie = uri) } }
}

@Composable
private fun UploadRow(label: String, uri: Uri?, onPicked: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { picked -> if (picked != null) onPicked(picked) }
    GlassCard(Modifier.fillMaxWidth().clickable { launcher.launch("image/*") }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(if (uri != null) RaahiGreen else RaahiTextFaint, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(if (uri != null) "Uploaded · tap to replace" else "Tap to choose a photo", color = RaahiTextDim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun VehicleStep(f: HelperApplicationForm, vm: HelperApplicationViewModel) {
    var picker by remember { mutableStateOf<String?>(null) }
    StepTitle("Your service vehicle", "The vehicle you use to reach customers.")
    FieldLabel("Type")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VEHICLE_TYPES.forEach { (label, id) -> RaahiChip(label, active = f.vehicleType == id) { vm.update { it.copy(vehicleType = id) } } }
    }
    FieldLabel("Brand")
    PickerField(f.vehicleBrand, "Select brand") { picker = "brand" }
    FieldLabel("Model")
    PickerField(f.vehicleModel, "Select model", enabled = f.vehicleBrand.isNotBlank()) { picker = "model" }
    FieldLabel("Variant (optional)")
    PickerField(f.vehicleVariant, "Select variant", enabled = f.vehicleModel.isNotBlank()) { picker = "variant" }
    FieldLabel("Registration number")
    RaahiTextField(f.vehicleReg, { v -> vm.update { it.copy(vehicleReg = v.uppercase().filter { c -> c.isLetterOrDigit() }.take(12)) } }, "e.g. PB65AB1234")
    FieldLabel("Service radius (km)")
    RaahiTextField(f.radiusKm, { v -> vm.update { it.copy(radiusKm = v.filter(Char::isDigit).take(3)) } }, "15", keyboardType = KeyboardType.Number)

    when (picker) {
        "brand" -> VehiclePickerDialog("Select Vehicle Brand", VehicleCatalogue.BRANDS.map { it.name }, f.vehicleBrand,
            onSelect = { b -> vm.update { it.copy(vehicleBrand = b, vehicleModel = "", vehicleVariant = "") }; picker = "model" },
            onDismiss = { picker = null })
        "model" -> VehiclePickerDialog("Select ${f.vehicleBrand} Model", VehicleCatalogue.findModels(f.vehicleBrand).map { it.name }, f.vehicleModel,
            onSelect = { m -> vm.update { it.copy(vehicleModel = m, vehicleVariant = "") }; picker = null },
            onDismiss = { picker = null })
        "variant" -> VehiclePickerDialog("Select ${f.vehicleModel} Variant", VehicleCatalogue.findVariants(f.vehicleBrand, f.vehicleModel), f.vehicleVariant,
            onSelect = { v -> vm.update { it.copy(vehicleVariant = v) }; picker = null },
            onDismiss = { picker = null })
    }
}

@Composable
private fun PickerField(value: String, placeholder: String, enabled: Boolean = true, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().clickable(enabled = enabled) { onClick() }) {
        Text(
            value.ifBlank { placeholder },
            color = if (value.isBlank()) RaahiTextFaint else RaahiText,
            fontSize = 14.sp, modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun PayoutStep(f: HelperApplicationForm, vm: HelperApplicationViewModel) {
    StepTitle("Payout details", "Raahi does not hold your earnings — customers pay you directly. These details are used for refunds and settlements.")
    FieldLabel("UPI ID")
    RaahiTextField(f.payoutUpi, { v -> vm.update { it.copy(payoutUpi = v.trim().take(80)) } }, "name@bank")
    FieldLabel("Or bank account")
    RaahiTextField(f.payoutHolder, { v -> vm.update { it.copy(payoutHolder = v.take(100)) } }, "Account holder name")
    Spacer(Modifier.height(8.dp))
    RaahiTextField(f.payoutBankAcct, { v -> vm.update { it.copy(payoutBankAcct = v.filter(Char::isDigit).take(18)) } }, "Account number", keyboardType = KeyboardType.Number)
    Spacer(Modifier.height(8.dp))
    RaahiTextField(f.payoutIfsc, { v -> vm.update { it.copy(payoutIfsc = v.uppercase().filter { c -> c.isLetterOrDigit() }.take(11)) } }, "IFSC code")
}

@Composable
private fun ReviewStep(f: HelperApplicationForm) {
    StepTitle("Review your application", "Check everything before you submit. You can go back to edit any step.")
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            ReviewRow("Experience", "${f.experienceYears} years")
            ReviewRow("Service area", f.serviceArea)
            ReviewRow("Services", PROBLEM_TYPES.filter { it.id.uppercase() in f.services }.joinToString { it.label })
            ReviewRow("Equipment", f.equipment.joinToString().ifBlank { "—" })
            ReviewRow("Documents", "Aadhaar front, back and selfie uploaded")
            ReviewRow("Vehicle", listOf(f.vehicleBrand, f.vehicleModel, f.vehicleVariant).filter { it.isNotBlank() }.joinToString(" ") + " · ${f.vehicleReg}")
            ReviewRow("Radius", "${f.radiusKm} km")
            ReviewRow("Payout", if (f.payoutUpi.isNotBlank()) f.payoutUpi else "Bank ••••${f.payoutBankAcct.takeLast(4)}")
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = RaahiTextDim, fontSize = 12.sp, modifier = Modifier.width(96.dp))
        Text(value, color = RaahiText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}
