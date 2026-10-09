package `in`.raahi.app.ui.screens.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.raahi.app.ui.theme.*

/**
 * Compact modal dialog for selecting vehicle Brand, Model, and Variant.
 * Fits ~7–8 items cleanly, scrolls vertically for the rest, and features a small search
 * field at the top supporting 2–3 letters or full name matching.
 */
@Composable
fun VehiclePickerDialog(
    title: String,
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    allowCustom: Boolean = false,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(searchQuery, items) {
        if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 440.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp), spotColor = Color(0x22000000))
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFFEDE8E1), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header: Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        color = Color(0xFF111827),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RaahiDisplayFont
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Compact Search Field (40dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color(0xFFF7F4EE), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFE8E2D8), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color(0xFF111827),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search by name or 2–3 letters...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF64748B),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Scrollable List (~7-8 items visible)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredItems) { item ->
                        val isSelected = item.equals(selectedItem, ignoreCase = true)
                        val rowBg = if (isSelected) Color(0xFFFFF0ED) else Color.Transparent
                        val rowBorder = if (isSelected) Color(0xFFFF4B3A) else Color.Transparent
                        val textCol = if (isSelected) Color(0xFFFF4B3A) else Color(0xFF1E293B)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(rowBg)
                                .border(1.dp, rowBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelect(item)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        if (isSelected) Color(0xFFFF4B3A).copy(alpha = 0.15f) else Color(0xFFF1EBE4),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFFFF4B3A) else Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = item,
                                color = textCol,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color(0xFFFF4B3A), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Not in the catalogue: never accept free text into the vehicle database. Offer a request instead.
                    if (searchQuery.isNotBlank() && filteredItems.isEmpty() && !allowCustom) {
                        item {
                            val wanted = searchQuery.trim()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFF7ED))
                                    .border(1.dp, Color(0xFFFDBA74), RoundedCornerShape(10.dp))
                                    .clickable {
                                        runCatching {
                                            context.startActivity(
                                                android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                                    data = android.net.Uri.parse("mailto:support@raahi.in")
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Vehicle request: $title")
                                                    putExtra(android.content.Intent.EXTRA_TEXT, "Please add this vehicle to Raahi: $wanted")
                                                }
                                            )
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Can't find your vehicle? Request it",
                                    color = Color(0xFFC2410C),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
