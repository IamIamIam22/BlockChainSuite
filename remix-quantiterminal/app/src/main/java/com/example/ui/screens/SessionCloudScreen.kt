package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CloudSyncStatus
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCard
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YellowWarning
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionCloudScreen(
    viewModel: MainViewModel,
    state: MainUiState
) {
    val context = LocalContext.current
    var showAddEnvDialog by remember { mutableStateOf(false) }
    var newEnvKey by remember { mutableStateOf("") }
    var newEnvValue by remember { mutableStateOf("") }
    var envSearchQuery by remember { mutableStateOf("") }

    val activeEnvs = viewModel.terminalEngine.getEnvVars()
    val filteredEnvs = if (envSearchQuery.isBlank()) {
        activeEnvs.toList()
    } else {
        activeEnvs.filter { (k, v) ->
            k.contains(envSearchQuery, ignoreCase = true) || v.contains(envSearchQuery, ignoreCase = true)
        }.toList()
    }

    val lastSavedFormatted = remember(state.sessionState.lastUpdated) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
        sdf.format(Date(state.sessionState.lastUpdated))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(14.dp)
            .testTag("session_cloud_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        item {
            Column {
                Text(
                    text = "Session Persistence & Cloud Sync",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Working directory and environment variables automatically resume on launch.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Firebase Cloud Sync & Auth Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = TerminalCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = if (state.userProfile != null) Color(0xFF062E1F) else Color(0xFF1E293B),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (state.userProfile != null) GreenSuccess else TerminalBorder
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (state.userProfile != null) Icons.Default.CloudDone else Icons.Default.Cloud,
                                        contentDescription = null,
                                        tint = if (state.userProfile != null) GreenSuccess else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = if (state.userProfile != null) state.userProfile.displayName else "Local SQLite Mode",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (state.userProfile != null) state.userProfile.email else "Sign in to enable Firebase Firestore sync",
                                    color = TextMuted,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        // Auth Action Button
                        if (state.userProfile == null) {
                            Button(
                                onClick = { viewModel.setAuthDialogVisible(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF00382E)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("auth_signin_button")
                            ) {
                                Icon(imageVector = Icons.Default.Login, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Connect Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.signOut() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                                modifier = Modifier.testTag("auth_signout_button")
                            ) {
                                Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Disconnect", fontSize = 10.5.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sync Status Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (state.sessionState.cloudSyncStatus) {
                                            CloudSyncStatus.CLOUD_SYNCED -> GreenSuccess
                                            CloudSyncStatus.SYNCING -> YellowWarning
                                            CloudSyncStatus.LOCAL_SAVED -> CyanAccent
                                            else -> Color(0xFF94A3B8)
                                        }
                                    )
                            )
                            Text(
                                text = when (state.sessionState.cloudSyncStatus) {
                                    CloudSyncStatus.CLOUD_SYNCED -> "Firestore Cloud Synced"
                                    CloudSyncStatus.SYNCING -> "Syncing to Cloud..."
                                    CloudSyncStatus.LOCAL_SAVED -> "Local Room DB (Offline Active)"
                                    else -> "Sync Ready"
                                },
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "Last Saved: $lastSavedFormatted",
                            color = TextMuted,
                            fontSize = 10.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Force Sync Trigger
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.triggerManualCloudSync() },
                        color = TerminalSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Force Immediate Cloud & Room Snapshot",
                                color = CyanAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Active Working Directory Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = TerminalCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Saved Working Directory",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = Color(0xFF00382E),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent)
                        ) {
                            Text(
                                text = "AUTO-RESUME",
                                color = CyanAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF090D16),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Text(
                            text = state.currentDirectory,
                            color = GreenSuccess,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // Environment Variables Section Header & Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Environment Variables (${activeEnvs.size})",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { showAddEnvDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF00382E)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("add_env_var_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add Variable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = envSearchQuery,
                    onValueChange = { envSearchQuery = it },
                    placeholder = { Text("Filter variables (e.g. PATH, NODE_ENV, PORT)...", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TerminalSurfaceElevated,
                        unfocusedContainerColor = TerminalSurfaceElevated,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = TerminalBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }
        }

        // Environment Variables List
        items(filteredEnvs, key = { it.first }) { (key, value) ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = TerminalCard,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = key,
                            color = CyanAccent,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = value,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2
                        )
                    }

                    IconButton(
                        onClick = { viewModel.removeCustomEnvironmentVariable(key) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Add Environment Variable Dialog
    if (showAddEnvDialog) {
        AlertDialog(
            onDismissRequest = { showAddEnvDialog = false },
            title = {
                Text(
                    text = "Add Environment Variable",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This will be persisted across app restarts and synchronized with Firebase Firestore.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = newEnvKey,
                        onValueChange = { newEnvKey = it.uppercase() },
                        label = { Text("Variable Name (e.g. DATABASE_URL)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = TerminalBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = newEnvValue,
                        onValueChange = { newEnvValue = it },
                        label = { Text("Value") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = TerminalBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEnvKey.isNotBlank()) {
                            viewModel.setCustomEnvironmentVariable(newEnvKey, newEnvValue)
                            newEnvKey = ""
                            newEnvValue = ""
                            showAddEnvDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF00382E))
                ) {
                    Text("Save & Persist", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEnvDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = TerminalSurfaceElevated
        )
    }
}
