package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurfaceElevated

@Composable
fun QuickKeysBar(
    onInsertText: (String) -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    onClear: () -> Unit,
    onExecuteQuick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        color = TerminalSurfaceElevated,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickKeyButton(label = "ESC", tag = "key_esc") { /* Handle esc */ }
            QuickKeyButton(label = "TAB", tag = "key_tab") { onInsertText("    ") }
            QuickKeyButton(label = "CTRL+C", tag = "key_ctrl_c") { onInsertText("") }
            QuickKeyButton(label = "↑ Prev", tag = "key_hist_up", highlight = true) { onHistoryUp() }
            QuickKeyButton(label = "↓ Next", tag = "key_hist_down", highlight = true) { onHistoryDown() }
            QuickKeyButton(label = "| Pipe", tag = "key_pipe") { onInsertText(" | ") }
            QuickKeyButton(label = "> Redir", tag = "key_redir") { onInsertText(" > ") }
            QuickKeyButton(label = "/", tag = "key_slash") { onInsertText("/") }
            QuickKeyButton(label = "~", tag = "key_tilde") { onInsertText("~") }
            QuickKeyButton(label = "-", tag = "key_dash") { onInsertText("-") }
            QuickKeyButton(label = "Clear", tag = "key_clear") { onClear() }
            QuickKeyButton(label = "🎯 bypass", tag = "key_bypass", isCommand = true) { onExecuteQuick("bypass") }
            QuickKeyButton(label = "💻 win-bridge", tag = "key_win_bridge", isCommand = true) { onExecuteQuick("bridge") }
            QuickKeyButton(label = "neofetch", tag = "key_neofetch", isCommand = true) { onExecuteQuick("neofetch") }
            QuickKeyButton(label = "htop", tag = "key_htop", isCommand = true) { onExecuteQuick("htop") }
            QuickKeyButton(label = "ls -la", tag = "key_ls", isCommand = true) { onExecuteQuick("ls -la") }
            QuickKeyButton(label = "git status", tag = "key_git", isCommand = true) { onExecuteQuick("git status") }
        }
    }
}

@Composable
private fun QuickKeyButton(
    label: String,
    tag: String,
    highlight: Boolean = false,
    isCommand: Boolean = false,
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        modifier = Modifier
            .height(34.dp)
            .testTag(tag),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = when {
                isCommand -> MaterialTheme.colorScheme.primaryContainer
                highlight -> Color(0xFF1E3A5F)
                else -> Color(0xFF0F172A)
            },
            contentColor = when {
                isCommand -> CyanAccent
                highlight -> Color(0xFF60A5FA)
                else -> Color(0xFFE2E8F0)
            }
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
