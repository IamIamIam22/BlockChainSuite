package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VirtualMachine
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedError
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCard
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HardwareMetricsBar(
    activeVm: VirtualMachine?,
    allVms: List<VirtualMachine>,
    onSelectVm: (VirtualMachine) -> Unit,
    onTogglePower: (VirtualMachine) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TerminalCard,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Header: VM Selector & Power Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TerminalSurfaceElevated)
                            .clickable { isDropdownExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("vm_selector_chip"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (activeVm?.isRunning == true) GreenSuccess else RedError)
                        )
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Cloud VM",
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = activeVm?.name ?: "No Active VM",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select VM",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.background(TerminalSurfaceElevated)
                    ) {
                        allVms.forEach { vm ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (vm.isRunning) GreenSuccess else RedError)
                                        )
                                        Column {
                                            Text(text = vm.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                                            Text(
                                                text = "${vm.distro} • ${vm.region}",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectVm(vm)
                                    isDropdownExpanded = false
                                },
                                modifier = Modifier.testTag("dropdown_vm_${vm.id}")
                            )
                        }
                    }
                }

                // Power button
                if (activeVm != null) {
                    IconButton(
                        onClick = { onTogglePower(activeVm) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_vm_power")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = if (activeVm.isRunning) "Stop VM" else "Start VM",
                            tint = if (activeVm.isRunning) GreenSuccess else RedError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Real-time telemetry specs
            if (activeVm != null && activeVm.isRunning) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // CPU gauge
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "CPU",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "CPU (${activeVm.cpuCores} vCPU)", fontSize = 11.sp, color = TextSecondary)
                            }
                            Text(
                                text = "${activeVm.cpuUsagePercent.toInt()}%",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { activeVm.cpuUsagePercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = CyanAccent,
                            trackColor = Color(0xFF1E293B)
                        )
                    }

                    // RAM gauge
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = "RAM",
                                    tint = GreenSuccess,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "RAM (${activeVm.ramMb / 1024} GB)", fontSize = 11.sp, color = TextSecondary)
                            }
                            Text(
                                text = "${(activeVm.ramUsageMb / 1024f).toString().take(3)}G",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { activeVm.ramUsageMb.toFloat() / activeVm.ramMb },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = GreenSuccess,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }

                // IP & Region info pill
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "IP: ${activeVm.ipAddress} (Public: ${activeVm.publicIp})",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = activeVm.region.split(" ").first(),
                        fontSize = 10.sp,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
