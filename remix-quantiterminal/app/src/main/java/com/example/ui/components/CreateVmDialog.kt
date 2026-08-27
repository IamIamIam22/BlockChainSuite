package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCard
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVmDialog(
    onDismiss: () -> Unit,
    onCreateVm: (name: String, distro: String, region: String, cores: Int, ram: Int, disk: Int) -> Unit
) {
    var name by remember { mutableStateOf("Dev Node " + (10..99).random()) }
    var selectedDistro by remember { mutableStateOf("Ubuntu 24.04 LTS (Noble)") }
    var selectedRegion by remember { mutableStateOf("us-east-1 (N. Virginia)") }
    var cores by remember { mutableFloatStateOf(4f) }
    var ramGb by remember { mutableFloatStateOf(8f) }
    var diskGb by remember { mutableFloatStateOf(80f) }

    val distroList = listOf(
        "Ubuntu 24.04 LTS (Noble)",
        "Debian 12 Bookworm",
        "Alpine Linux 3.20 (Micro)",
        "Arch Linux Rolling",
        "Fedora 40 Cloud Server",
        "Kali Linux CyberSec"
    )

    val regionList = listOf(
        "us-east-1 (N. Virginia)",
        "us-west-2 (Oregon)",
        "eu-central-1 (Frankfurt)",
        "ap-northeast-1 (Tokyo)",
        "ap-southeast-1 (Singapore)"
    )

    var distroExpanded by remember { mutableStateOf(false) }
    var regionExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TerminalCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Provision Cloud VM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // VM Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Instance Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_vm_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = TerminalBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Distro Selector
                ExposedDropdownMenuBox(
                    expanded = distroExpanded,
                    onExpandedChange = { distroExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedDistro,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Operating System Distro") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = distroExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = TerminalBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = distroExpanded,
                        onDismissRequest = { distroExpanded = false },
                        modifier = Modifier.background(TerminalSurfaceElevated)
                    ) {
                        distroList.forEach { distro ->
                            DropdownMenuItem(
                                text = { Text(distro, color = TextPrimary) },
                                onClick = {
                                    selectedDistro = distro
                                    distroExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Region Selector
                ExposedDropdownMenuBox(
                    expanded = regionExpanded,
                    onExpandedChange = { regionExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRegion,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cloud Region") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = TerminalBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = regionExpanded,
                        onDismissRequest = { regionExpanded = false },
                        modifier = Modifier.background(TerminalSurfaceElevated)
                    ) {
                        regionList.forEach { region ->
                            DropdownMenuItem(
                                text = { Text(region, color = TextPrimary) },
                                onClick = {
                                    selectedRegion = region
                                    regionExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hardware Sliders: CPU Cores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Compute (vCPUs)", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        "${cores.toInt()} vCPU",
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = cores,
                    onValueChange = { cores = it },
                    valueRange = 1f..16f,
                    steps = 14,
                    colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                )

                // RAM Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("RAM Memory", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        "${ramGb.toInt()} GB",
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = ramGb,
                    onValueChange = { ramGb = it },
                    valueRange = 2f..64f,
                    steps = 30,
                    colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                )

                // NVMe Disk
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("NVMe SSD Storage", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        "${diskGb.toInt()} GB",
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = diskGb,
                    onValueChange = { diskGb = it },
                    valueRange = 20f..500f,
                    colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Button(
                        onClick = {
                            onCreateVm(
                                name,
                                selectedDistro,
                                selectedRegion,
                                cores.toInt(),
                                (ramGb * 1024).toInt(),
                                diskGb.toInt()
                            )
                        },
                        modifier = Modifier.testTag("btn_confirm_create_vm"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color(0xFF00382E)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Deploy",
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Launch Instance", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
