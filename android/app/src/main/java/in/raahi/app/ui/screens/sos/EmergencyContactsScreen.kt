package `in`.raahi.app.ui.screens.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.data.EmergencyContact
import `in`.raahi.app.ui.theme.*

@Composable
fun EmergencyContactsScreen(
    onBack: () -> Unit,
    viewModel: EmergencyContactsViewModel = hiltViewModel(),
) {
    val contacts by viewModel.contacts.collectAsState()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Scaffold(
        containerColor = RaahiLightBg,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = RaahiCharcoalHeader,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .background(RaahiDarkSurface, CircleShape)
                            .border(1.dp, Color(0xFF2E3438), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "Emergency Contacts",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RaahiDisplayFont,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "Stored only on this device. Used to send an SOS text if you tap \"alert my contacts\".",
                color = RaahiSecondaryText,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                lineHeight = 18.sp,
            )

            // Add contact card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RaahiCardShape,
                color = RaahiWhiteSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, RaahiBorderColor),
                shadowElevation = RaahiCardElevation,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Contact Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = contactFieldColors(),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = contactFieldColors(),
                        )
                        Button(
                            onClick = {
                                if (name.isNotBlank() && phone.isNotBlank()) {
                                    viewModel.add(name.trim(), phone.trim())
                                    name = ""
                                    phone = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RaahiPrimaryTeal,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier.height(52.dp),
                        ) {
                            Text("Add", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No emergency contacts yet",
                        color = RaahiSecondaryText,
                        fontSize = 14.sp,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(contacts, key = { it.phone }) { contact ->
                        ContactRow(contact, onRemove = { viewModel.remove(contact.phone) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: EmergencyContact, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RaahiShapeMedium,
        color = RaahiWhiteSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, RaahiBorderColor),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(RaahiLightTeal, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = RaahiPrimaryTeal,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    color = RaahiPrimaryText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(
                    text = contact.phone,
                    color = RaahiSecondaryText,
                    fontSize = 12.5.sp,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove",
                    tint = RaahiSosDanger,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun contactFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = RaahiPrimaryText,
    unfocusedTextColor = RaahiPrimaryText,
    focusedBorderColor = RaahiPrimaryTeal,
    unfocusedBorderColor = RaahiBorderColor,
    focusedContainerColor = RaahiWhiteSurface,
    unfocusedContainerColor = RaahiWhiteSurface,
    cursorColor = RaahiPrimaryTeal,
    focusedLabelColor = RaahiPrimaryTeal,
    unfocusedLabelColor = RaahiSecondaryText,
)

