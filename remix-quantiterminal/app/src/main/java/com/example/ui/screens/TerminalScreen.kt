package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LineType
import com.example.data.model.TerminalLine
import com.example.ui.components.HardwareMetricsBar
import com.example.ui.components.QuickKeysBar
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedError
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalOutputWhite
import com.example.ui.theme.TerminalPathBlue
import com.example.ui.theme.TerminalPromptGreen
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    viewModel: MainViewModel,
    state: MainUiState,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Auto scroll to bottom on new output
    LaunchedEffect(state.terminalLines.size) {
        if (state.terminalLines.isNotEmpty()) {
            listState.animateScrollToItem(state.terminalLines.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(8.dp)
    ) {
        // Hardware specs bar
        HardwareMetricsBar(
            activeVm = state.activeVm,
            allVms = state.vms,
            onSelectVm = { viewModel.setActiveVm(it) },
            onTogglePower = { viewModel.toggleVmPower(it) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Session & Cloud Status Mini Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.setTab(com.example.ui.viewmodel.NavigationTab.SESSION_CLOUD) },
            color = TerminalSurfaceElevated,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Terminal,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "CWD: ${state.currentDirectory}",
                        color = GreenSuccess,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${viewModel.terminalEngine.getEnvVars().size} Env Vars",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Surface(
                        color = if (state.userProfile != null) Color(0xFF062E1F) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (state.userProfile != null) GreenSuccess else TerminalBorder
                        )
                    ) {
                        Text(
                            text = if (state.userProfile != null) "☁️ CLOUD SYNCED" else "💾 ROOM SAVED",
                            color = if (state.userProfile != null) GreenSuccess else CyanAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick suggestions and Windows Bridge Bypass pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Presets:", color = TextMuted, fontSize = 10.sp)
                listOf("session", "env", "export", "bypass", "neofetch").forEach { cmd ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = TerminalSurfaceElevated,
                        modifier = Modifier.clickable { viewModel.executeCommand(cmd) }
                    ) {
                        Text(
                            text = cmd,
                            fontSize = 10.sp,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1B4B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                modifier = Modifier.clickable { viewModel.setTab(com.example.ui.viewmodel.NavigationTab.COPILOT) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Terminal,
                        contentDescription = "AI Copilot",
                        tint = Color(0xFFA5B4FC),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "Gemini Copilot",
                        color = Color(0xFFE0E7FF),
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Terminal Output Area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = TerminalSurface,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
        ) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .testTag("terminal_output_list"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.terminalLines, key = { it.id }) { line ->
                        TerminalLineItem(line = line)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Mobile Quick Keys Accessory Bar
        QuickKeysBar(
            onInsertText = { text ->
                viewModel.updateCommandInput(state.currentCommandInput + text)
            },
            onHistoryUp = { viewModel.historyUp() },
            onHistoryDown = { viewModel.historyDown() },
            onClear = { viewModel.executeCommand("clear") },
            onExecuteQuick = { cmd -> viewModel.executeCommand(cmd) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Command Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalSurfaceElevated, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "❯",
                color = CyanAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            OutlinedTextField(
                value = state.currentCommandInput,
                onValueChange = { viewModel.updateCommandInput(it) },
                placeholder = {
                    Text(
                        text = "Enter Linux command (e.g. npm, pip, go, curl, apt)...",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_input_field"),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.executeCommand() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            if (state.isExecuting) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp),
                    color = CyanAccent,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = { viewModel.executeCommand() },
                    modifier = Modifier.testTag("btn_send_command")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Execute Command",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TerminalLineItem(line: TerminalLine) {
    val textColor = when (line.type) {
        LineType.INPUT -> TerminalPromptGreen
        LineType.OUTPUT -> TerminalOutputWhite
        LineType.SYSTEM_INFO -> Color(0xFF60A5FA)
        LineType.SUCCESS -> GreenSuccess
        LineType.WARNING -> Color(0xFFFBBF24)
        LineType.ERROR -> RedError
        LineType.CODE -> Color(0xFFE2E8F0)
        LineType.ACCENT -> CyanAccent
    }

    val isInput = line.type == LineType.INPUT

    if (isInput) {
        // Parse prompt and command
        Text(
            text = line.text,
            color = TerminalPromptGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 16.sp
        )
    } else {
        Text(
            text = line.text,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
