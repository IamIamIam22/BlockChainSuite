package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.RemoteRequestLog
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedError
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCard
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WindowsBridgeScreen(
    viewModel: MainViewModel,
    state: MainUiState,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var selectedSnippetTab by remember { mutableIntStateOf(0) }
    val bridgeState = state.bridgeState
    val scrollState = rememberScrollState()

    fun copyToClipboard(text: String, label: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(12.dp)
            .verticalScroll(scrollState)
    ) {
        // ==========================================
        // 1. BYPASS TARGETING HERO BANNER
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_bypass_targeting"),
            colors = CardDefaults.cardColors(
                containerColor = if (bridgeState.isBypassTargetingActive) Color(0xFF062822) else Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (bridgeState.isBypassTargetingActive) CyanAccent else TerminalBorder
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
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
                            shape = RoundedCornerShape(8.dp),
                            color = if (bridgeState.isBypassTargetingActive) Color(0xFF00382E) else Color(0xFF334155),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (bridgeState.isBypassTargetingActive) GreenSuccess else TextMuted)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Bypass Targeting",
                                tint = if (bridgeState.isBypassTargetingActive) GreenSuccess else TextMuted,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Bypass Targeting Mode",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = if (bridgeState.isBypassTargetingActive) GreenSuccess.copy(alpha = 0.2f) else Color(0xFF475569),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (bridgeState.isBypassTargetingActive) "ACTIVE" else "OFF",
                                        color = if (bridgeState.isBypassTargetingActive) GreenSuccess else Color(0xFFCBD5E1),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Run tasks entirely inside secure browser cloud stream",
                                color = if (bridgeState.isBypassTargetingActive) CyanAccent else TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = bridgeState.isBypassTargetingActive,
                        onCheckedChange = { viewModel.toggleBypassTargeting() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00382E),
                            checkedTrackColor = CyanAccent,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("switch_bypass_targeting")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = TerminalBorder.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "✓ Remote commands from Windows bypass targeting limitations\n✓ 100% of execution runs in this cloud container sandbox\n✓ Zero impact or changes made to your actual computer OS",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // 2. WINDOWS REMOTE BRIDGE SERVER STATUS
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_bridge_status"),
            colors = CardDefaults.cardColors(containerColor = TerminalSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.DesktopWindows, contentDescription = "Windows Bridge", tint = CyanAccent, modifier = Modifier.size(20.dp))
                        Text("Windows Bridge Gateway Daemon", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (bridgeState.isRunning) GreenSuccess else RedError)
                        )
                        Text(
                            text = if (bridgeState.isRunning) "DAEMON LISTENING" else "DAEMON STOPPED",
                            color = if (bridgeState.isRunning) GreenSuccess else RedError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BridgeMetricChip(
                        label = "ENDPOINT",
                        value = "http://localhost:${bridgeState.port}",
                        icon = Icons.Default.Lan,
                        modifier = Modifier.weight(1f)
                    )
                    BridgeMetricChip(
                        label = "ACTIVE VM NODE",
                        value = state.activeVm?.name ?: "CloudTerm-1",
                        icon = Icons.Default.Cloud,
                        modifier = Modifier.weight(1f)
                    )
                    BridgeMetricChip(
                        label = "REQUESTS",
                        value = "${bridgeState.totalRequestsReceived} processed",
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Control buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.toggleBridgeServer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bridgeState.isRunning) Color(0xFF334155) else CyanAccent,
                            contentColor = if (bridgeState.isRunning) Color.White else Color(0xFF00382E)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_toggle_bridge_daemon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (bridgeState.isRunning) "Restart Bridge Daemon" else "Start Bridge Daemon", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            copyToClipboard("http://localhost:${bridgeState.port}/api/exec", "Bridge API URL")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerminalSurfaceElevated,
                            contentColor = CyanAccent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("btn_copy_endpoint")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Endpoint", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // 3. WINDOWS INTERACTION SCRIPTS & CONFIGS
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_windows_snippets"),
            colors = CardDefaults.cardColors(containerColor = TerminalCard),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = "Scripts", tint = CyanAccent, modifier = Modifier.size(20.dp))
                        Text("Windows Computer Connector Scripts", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs for different Windows methods
                TabRow(
                    selectedTabIndex = selectedSnippetTab,
                    containerColor = TerminalSurfaceElevated,
                    contentColor = CyanAccent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedSnippetTab]),
                            color = CyanAccent
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedSnippetTab == 0,
                        onClick = { selectedSnippetTab = 0 },
                        text = { Text("PowerShell", fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                    )
                    Tab(
                        selected = selectedSnippetTab == 1,
                        onClick = { selectedSnippetTab = 1 },
                        text = { Text("CMD / Batch", fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                    )
                    Tab(
                        selected = selectedSnippetTab == 2,
                        onClick = { selectedSnippetTab = 2 },
                        text = { Text("Win Terminal", fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                    )
                    Tab(
                        selected = selectedSnippetTab == 3,
                        onClick = { selectedSnippetTab = 3 },
                        text = { Text("REST / cURL", fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedSnippetTab) {
                    0 -> PowerShellSnippetView(port = bridgeState.port, vmName = state.activeVm?.name ?: "CloudTerm-1", onCopy = { copyToClipboard(it, "PowerShell Script") })
                    1 -> CmdSnippetView(port = bridgeState.port, onCopy = { copyToClipboard(it, "CMD Batch Script") })
                    2 -> WindowsTerminalProfileSnippetView(port = bridgeState.port, onCopy = { copyToClipboard(it, "Windows Terminal Profile") })
                    3 -> CurlSnippetView(port = bridgeState.port, onCopy = { copyToClipboard(it, "cURL Snippet") })
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // 4. INTERACTIVE WINDOWS BRIDGE TESTER / REPL
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_windows_tester"),
            colors = CardDefaults.cardColors(containerColor = TerminalSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = "Tester", tint = GreenSuccess, modifier = Modifier.size(18.dp))
                        Text("Live Windows Terminal Bridge Tester", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Surface(
                        color = Color(0xFF0F3E2E),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "BYPASS ACTIVE",
                            color = GreenSuccess,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Test running commands through the Windows Remote Bridge right now to verify targeting bypass and instant cloud stream execution:",
                    color = TextMuted,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // Quick preset buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("neofetch", "python3 main.py", "npm list", "go version", "cat README.md", "ps aux").forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TerminalSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                            modifier = Modifier.clickable {
                                viewModel.updateWindowsTestInput(preset)
                                viewModel.executeWindowsTestCommand()
                            }
                        ) {
                            Text(
                                text = preset,
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Simulated Windows Command Prompt Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF080C14), RoundedCornerShape(8.dp))
                        .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PS C:\\Users\\WinDev>",
                        color = Color(0xFF60A5FA),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    OutlinedTextField(
                        value = state.windowsTestInput,
                        onValueChange = { viewModel.updateWindowsTestInput(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("windows_test_input"),
                        singleLine = true,
                        placeholder = { Text("e.g. npm start, python3 main.py", color = TextMuted, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.executeWindowsTestCommand() })
                    )

                    if (state.isWindowsExecuting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CyanAccent)
                    } else {
                        IconButton(
                            onClick = { viewModel.executeWindowsTestCommand() },
                            modifier = Modifier.testTag("btn_run_windows_test")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Execute via Bridge",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (state.windowsTestOutput.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        color = Color(0xFF030712),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "☁️ [Cloud Stream Output via Windows Bridge]:",
                                    color = GreenSuccess,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bypass Applied ✓",
                                    color = CyanAccent,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.windowsTestOutput,
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // 5. LIVE BRIDGE AUDIT LOGS
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_bridge_logs"),
            colors = CardDefaults.cardColors(containerColor = TerminalCard),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = "Logs", tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Text("Live Remote Bridge Traffic Stream", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Text(
                        text = "${bridgeState.requestLogs.size} logs",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (bridgeState.requestLogs.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        color = TerminalSurface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "No incoming remote requests yet. Run a command above or execute the PowerShell script on your Windows computer to see live traffic!",
                            color = TextMuted,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bridgeState.requestLogs.take(8).forEach { log ->
                            BridgeLogItem(log = log)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun BridgeMetricChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = TerminalSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(12.dp))
                Text(label, color = TextMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, maxLines = 1)
        }
    }
}

@Composable
private fun PowerShellSnippetView(port: Int, vmName: String, onCopy: (String) -> Unit) {
    val script = """
# Run in Windows PowerShell / Windows Terminal
${'$'}uri = "http://localhost:$port/api/exec"
Write-Host "Connected to CloudTerm VM ($vmName) [Bypass Targeting Mode]" -ForegroundColor Cyan

do {
    ${'$'}cmd = Read-Host "win-cloudterm:~$"
    if ([string]::IsNullOrWhiteSpace(${'$'}cmd)) { continue }
    if (${'$'}cmd -eq 'exit' -or ${'$'}cmd -eq 'quit') { break }
    
    ${'$'}payload = @{ cmd = ${'$'}cmd; bypassTargeting = ${'$'}true } | ConvertTo-Json
    try {
        ${'$'}resp = Invoke-RestMethod -Uri ${'$'}uri -Method Post -Body ${'$'}payload -ContentType "application/json"
        Write-Host ${'$'}resp.output
    } catch {
        Write-Host "Bridge error: ${'$'}(${'$'}_.Exception.Message)" -ForegroundColor Red
    }
} while (${'$'}true)
    """.trimIndent()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Interactive PowerShell REPL Loop", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Button(
                onClick = { onCopy(script) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF00382E)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Script", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        CodeBox(code = script)
    }
}

@Composable
private fun CmdSnippetView(port: Int, onCopy: (String) -> Unit) {
    val script = """
:: Run in Windows Command Prompt (cmd.exe)
:: One-liner test:
curl -X POST http://localhost:$port/api/exec -H "Content-Type: application/json" -d "{\"cmd\":\"python3 main.py\",\"bypassTargeting\":true}"

:: Batch script loop (save as cloudterm.bat):
@echo off
echo [CloudTerm Windows Bridge - Cloud Stream Execution]
:loop
set /p USERCMD="cloudterm> "
if "%USERCMD%"=="exit" goto end
curl -s -X POST http://localhost:$port/api/exec -H "Content-Type: application/json" -d "{\"cmd\":\"%USERCMD%\"}"
echo.
goto loop
:end
    """.trimIndent()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Windows Command Prompt (cmd.exe / batch)", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Button(
                onClick = { onCopy(script) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF00382E)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy CMD Script", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        CodeBox(code = script)
    }
}

@Composable
private fun WindowsTerminalProfileSnippetView(port: Int, onCopy: (String) -> Unit) {
    val profileJson = """
// Add to settings.json -> profiles.list in Windows Terminal
{
  "name": "CloudTerm Cloud VM",
  "commandline": "powershell.exe -NoExit -Command \"do { ${'$'}c = Read-Host 'cloudterm:~$'; if (${'$'}c -eq 'exit') { break }; (Invoke-RestMethod -Uri 'http://localhost:$port/api/exec' -Method Post -Body (@{cmd=${'$'}c}|ConvertTo-Json) -ContentType 'application/json').output } while (${'$'}true)\"",
  "icon": "ms-appx:///ProfileIcons/{0caa0dad-35be-5f56-a8ff-afceeeaa6101}.png",
  "colorScheme": "One Half Dark",
  "tabTitle": "CloudTerm VM (Bypass Mode)"
}
    """.trimIndent()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Windows Terminal settings.json Profile", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Button(
                onClick = { onCopy(profileJson) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF00382E)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Profile JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        CodeBox(code = profileJson)
    }
}

@Composable
private fun CurlSnippetView(port: Int, onCopy: (String) -> Unit) {
    val curlSnippet = """
# HTTP POST Execution Endpoint
curl -X POST http://localhost:$port/api/exec \
  -H "Content-Type: application/json" \
  -d '{
    "cmd": "neofetch",
    "bypassTargeting": true,
    "source": "Windows Developer Script"
  }'

# Python Remote Automation Client:
import requests
resp = requests.post("http://localhost:$port/api/exec", json={"cmd": "python3 main.py", "bypassTargeting": True})
print(resp.json()["output"])
    """.trimIndent()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("REST API / Python / cURL Automation", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Button(
                onClick = { onCopy(curlSnippet) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF00382E)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy API Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        CodeBox(code = curlSnippet)
    }
}

@Composable
private fun CodeBox(code: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF030712),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
    ) {
        Text(
            text = code,
            color = Color(0xFFE2E8F0),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 15.sp,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
private fun BridgeLogItem(log: RemoteRequestLog) {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeStr = sdf.format(Date(log.timestamp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = TerminalSurfaceElevated,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = if (log.statusCode == 200) Color(0xFF0F3E2E) else Color(0xFF3B1219),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${log.statusCode} OK",
                        color = if (log.statusCode == 200) GreenSuccess else Color(0xFFF87171),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(log.clientSource, color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                        if (log.bypassTargetingApplied) {
                            Text("• Bypass ✓", color = GreenSuccess, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Text(
                        text = "> ${log.command}",
                        color = TextPrimary,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${log.latencyMs}ms", color = Color(0xFFFBBF24), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(timeStr, color = TextMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
