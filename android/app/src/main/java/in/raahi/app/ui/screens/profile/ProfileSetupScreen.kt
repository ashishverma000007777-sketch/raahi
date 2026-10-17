package `in`.raahi.app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.ui.components.RaahiPrimaryButton
import `in`.raahi.app.ui.components.RaahiStatusPill
import `in`.raahi.app.ui.theme.*

@Composable
fun ProfileSetupScreen(
    onDone: () -> Unit,
    isEditing: Boolean = false,
    onBack: (() -> Unit)? = null,
    viewModel: ProfileSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val prefill by viewModel.prefill.collectAsState()
    var name by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("") }
    var vehicleReg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { if (isEditing) viewModel.loadForEdit() }
    LaunchedEffect(prefill) {
        prefill?.let { user ->
            name = user.name.orEmpty()
            vehicleType = user.vehicleType.orEmpty()
            vehicleReg = user.vehicleReg.orEmpty()
        }
    }

    LaunchedEffect(state) {
        if (state is ProfileSetupState.Saved) onDone()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RaahiBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (isEditing && onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = RaahiText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
            } else {
                Spacer(Modifier.height(16.dp))
            }

            // Floating Header Card matching reference-image.jpg
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, RaahiBorderSoft),
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    RaahiStatusPill(
                        label = if (isEditing) "PROFILE SETTINGS" else "PROFILE SETUP",
                        tint = RaahiOrange
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (isEditing) "Edit your profile" else "Let's set up your profile",
                        color = RaahiText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RaahiDisplayFont,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Mechanics and helpers will see this when they respond to your requests.",
                        color = RaahiTextDim,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Form Fields inside a sleek floating card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, RaahiBorderSoft),
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Name Box
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Name", fontSize = 13.sp) },
                        placeholder = { Text("e.g. Ramesh Kumar", color = RaahiTextFaint, fontSize = 13.5.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = RaahiOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = profileFieldColors(),
                    )

                    // 2. Vehicle Type Box
                    OutlinedTextField(
                        value = vehicleType,
                        onValueChange = { vehicleType = it },
                        label = { Text("Vehicle Model", fontSize = 13.sp) },
                        placeholder = { Text("e.g. Maruti Swift Dzire", color = RaahiTextFaint, fontSize = 13.5.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.DirectionsCar,
                                contentDescription = null,
                                tint = RaahiOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = profileFieldColors(),
                    )

                    // 3. Vehicle Registration Box
                    OutlinedTextField(
                        value = vehicleReg,
                        onValueChange = { vehicleReg = it.uppercase() },
                        label = { Text("Registration Number (Optional)", fontSize = 13.sp) },
                        placeholder = { Text("e.g. DL 01 AB 1234", color = RaahiTextFaint, fontSize = 13.5.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Badge,
                                contentDescription = null,
                                tint = RaahiOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = profileFieldColors(),
                    )

                    if (state is ProfileSetupState.Error) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RaahiRed.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, RaahiRed.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = (state as ProfileSetupState.Error).message,
                                color = RaahiRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Action Button
                    RaahiPrimaryButton(
                        text = if (isEditing) "Save Changes" else "Continue",
                        onClick = { viewModel.save(name, vehicleType, vehicleReg) },
                        enabled = state !is ProfileSetupState.Saving,
                        loading = state is ProfileSetupState.Saving,
                    )

                    if (!isEditing) {
                        TextButton(
                            onClick = onDone,
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Skip for now", color = RaahiTextDim, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun profileFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = RaahiText,
    unfocusedTextColor = RaahiText,
    focusedBorderColor = RaahiOrange,
    unfocusedBorderColor = RaahiBorderSoft,
    focusedLabelColor = RaahiOrange,
    unfocusedLabelColor = RaahiTextDim,
    focusedContainerColor = Color(0xFFF9FAFB),
    unfocusedContainerColor = Color(0xFFF9FAFB),
    cursorColor = RaahiOrange,
)
