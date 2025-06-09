package com.example.bikerental.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun SOSDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirmEmergency: (String) -> Unit
) {
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Emergency Icon - Smaller
                    Icon(
                        imageVector = Icons.Default.Emergency,
                        contentDescription = "Emergency",
                        modifier = Modifier.size(40.dp),
                        tint = Color.Red
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Title - Smaller font
                    Text(
                        text = "Emergency SOS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Description - More compact
                    Text(
                        text = "Select emergency type. Location will be sent to emergency services.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Emergency type selection
                    var selectedEmergency by remember { mutableStateOf("") }
                    
                    val emergencyTypes = listOf(
                        EmergencyType("Medical Emergency", Icons.Default.LocalHospital, "Injury or health issue"),
                        EmergencyType("Accident", Icons.Default.Warning, "Bike accident or collision"),
                        EmergencyType("Bike Malfunction", Icons.Default.DirectionsBike, "Mechanical failure"),
                        EmergencyType("Security Issue", Icons.Default.Security, "Theft or harassment"),
                        EmergencyType("Other Emergency", Icons.Default.Phone, "Other urgent situation")
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        emergencyTypes.forEach { emergency ->
                            EmergencyOptionCard(
                                emergencyType = emergency,
                                isSelected = selectedEmergency == emergency.type,
                                onSelect = { selectedEmergency = emergency.type }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Buttons - More compact
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cancel Button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Send SOS Button
                        Button(
                            onClick = {
                                if (selectedEmergency.isNotEmpty()) {
                                    onConfirmEmergency(selectedEmergency)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            ),
                            enabled = selectedEmergency.isNotEmpty()
                        ) {
                            Text(
                                text = "SEND SOS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Warning text - Smaller
                    Text(
                        text = "⚠️ Only use for real emergencies. False alarms may result in account suspension.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencyOptionCard(
    emergencyType: EmergencyType,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color.Red.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color.Red)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = emergencyType.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) Color.Red else MaterialTheme.colorScheme.onSurface
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = emergencyType.type,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.Red else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = emergencyType.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                modifier = Modifier.size(16.dp),
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color.Red
                )
            )
        }
    }
}

private data class EmergencyType(
    val type: String,
    val icon: ImageVector,
    val description: String
) 