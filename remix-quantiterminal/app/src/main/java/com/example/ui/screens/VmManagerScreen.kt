package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.components.CreateVmDialog
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedError
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCard
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.NavigationTab

@Composable
fun VmManagerScreen(
    viewModel: MainViewModel,
    state: MainUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header summary banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalCard),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cloud VM Infrastructure",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${state.vms.count { it.isRunning }} Active Nodes / ${state.vms.size} Provisioned",
                            fontSize = 12.sp,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = { viewModel.setCreateVmDialogVisible(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color(0xFF00382E)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_add_vm")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New VM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // VM instances list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("vm_instances_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.vms, key = { it.id }) { vm ->
                    val isConnected = state.activeVm?.id == vm.id
                    VmInstanceCard(
                        vm = vm,
                        isConnected = isConnected,
                        onConnect = {
                            viewModel.setActiveVm(vm)
                            viewModel.setTab(NavigationTab.TERMINAL)
                        },
                        onTogglePower = { viewModel.toggleVmPower(vm) },
                        onDelete = { viewModel.deleteVirtualMachine(vm) }
                    )
                }
            }
        }

        // Dialog for creating VM
        if (state.isCreatingVmDialogVisible) {
            CreateVmDialog(
                onDismiss = { viewModel.setCreateVmDialogVisible(false) },
                onCreateVm = { name, distro, region, cores, ram, disk ->
                    viewModel.createVirtualMachine(name, distro, region, cores, ram, disk)
                }
            )
        }
    }
}

@Composable
fun VmInstanceCard(
    vm: VirtualMachine,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onTogglePower: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vm_card_${vm.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFF132338) else TerminalCard
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isConnected) 1.5.dp else 1.dp,
            color = if (isConnected) CyanAccent else TerminalBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (vm.isRunning) GreenSuccess else RedError)
                    )
                    Text(
                        text = vm.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (isConnected) {
                    Surface(
                        color = CyanAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent)
                    ) {
                        Text(
                            text = "ACTIVE SESSION",
                            color = CyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // OS & Region Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${vm.distro} • ${vm.region}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = if (vm.isRunning) "Status: RUNNING" else "Status: STOPPED",
                    color = if (vm.isRunning) GreenSuccess else RedError,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hardware Specs Matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecPill(icon = Icons.Default.Speed, label = "${vm.cpuCores} vCPU", modifier = Modifier.weight(1f))
                SpecPill(icon = Icons.Default.Memory, label = "${vm.ramMb / 1024} GB RAM", modifier = Modifier.weight(1f))
                SpecPill(icon = Icons.Default.Storage, label = "${vm.diskGb} GB NVMe", modifier = Modifier.weight(1f))
                SpecPill(icon = Icons.Default.Public, label = vm.publicIp.take(12), modifier = Modifier.weight(1.2f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onConnect,
                        enabled = vm.isRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color(0xFF00382E)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_connect_${vm.id}")
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = "Terminal", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Terminal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onTogglePower,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier.testTag("btn_toggle_power_${vm.id}")
                    ) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = if (vm.isRunning) RedError else GreenSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (vm.isRunning) "Stop" else "Start", fontSize = 12.sp)
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("btn_delete_vm_${vm.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete VM",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = TerminalSurfaceElevated,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
        }
    }
}
