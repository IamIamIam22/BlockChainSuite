package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRole
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopilotScreen(
    viewModel: MainViewModel,
    state: MainUiState
) {
    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(state.chatMessages.size, state.isChatLoading) {
        if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.chatMessages.size - 1)
        }
    }

    val suggestedPrompts = listOf(
        "How do I set up a FastAPI Python server?",
        "Configure Redis and PostgreSQL environment variables",
        "Explain how the Windows bypass targeting works",
        "Write a Node.js Express script listening on port 3000",
        "Show Docker build and run commands"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .imePadding()
            .testTag("copilot_screen")
    ) {
        // Copilot Header Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = TerminalSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFF1E1B4B),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI Copilot",
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Gemini Terminal Copilot",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = Color(0xFF0F2E1E),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenSuccess)
                                ) {
                                    Text(
                                        text = "AI 3.5 FLASH",
                                        color = GreenSuccess,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "DevOps, bash scripting, cloud environments & debugging",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Model Selection Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Model:",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    val models = listOf(
                        "gemini-3.5-flash" to "Flash (Fast)",
                        "gemini-3.1-pro-preview" to "Pro (Deep Reasoning)",
                        "gemini-3.1-flash-lite-preview" to "Lite (Instant)"
                    )

                    models.forEach { (modelId, label) ->
                        val isSelected = state.selectedGeminiModel == modelId
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.setGeminiModel(modelId) },
                            color = if (isSelected) Color(0xFF312E81) else TerminalSurfaceElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF818CF8) else TerminalBorder
                            )
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFFE0E7FF) else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Messages List or Empty Welcome
        if (state.chatMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFF1E1B4B),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Text(
                        text = "How can Gemini assist your Cloud VM?",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Ask questions, generate bash scripts, configure environment variables, or tap a quick prompt below.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedPrompts.forEach { prompt ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.sendChatMessage(prompt) },
                                color = TerminalSurfaceElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = prompt,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.chatMessages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        onExecuteCode = { snippet -> viewModel.runCodeSnippetInTerminal(snippet) }
                    )
                }

                if (state.isChatLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CyanAccent,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Gemini is generating response...",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Quick Suggestions Horizontal Carousel (when chat has history)
        if (state.chatMessages.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(suggestedPrompts) { prompt ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.sendChatMessage(prompt) },
                        color = TerminalSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                    ) {
                        Text(
                            text = prompt,
                            color = TextMuted,
                            fontSize = 10.5.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Chat Input Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = TerminalSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.chatInput,
                    onValueChange = { viewModel.updateChatInput(it) },
                    placeholder = {
                        Text(
                            text = "Ask Gemini Copilot or type command...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copilot_chat_input"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TerminalSurfaceElevated,
                        unfocusedContainerColor = TerminalSurfaceElevated,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = TerminalBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 3,
                    singleLine = false
                )

                IconButton(
                    onClick = { viewModel.sendChatMessage() },
                    enabled = state.chatInput.isNotBlank() && !state.isChatLoading,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (state.chatInput.isNotBlank() && !state.isChatLoading) CyanAccent else TerminalSurfaceElevated)
                        .testTag("copilot_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (state.chatInput.isNotBlank() && !state.isChatLoading) Color(0xFF00382E) else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onExecuteCode: (String) -> Unit
) {
    val isUser = message.role == ChatRole.USER
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                color = Color(0xFF1E1B4B),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFA5B4FC),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = if (isUser) Color(0xFF00382E) else TerminalCard,
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (isUser) 14.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 14.dp
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUser) CyanAccent else TerminalBorder
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        color = if (isUser) Color(0xFFE6FFFA) else TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    // If message contains code blocks, render interactive action buttons
                    if (message.codeBlocks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        message.codeBlocks.forEach { snippet ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = Color(0xFF090D16),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = snippet,
                                        color = GreenSuccess,
                                        fontSize = 11.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Copy button
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable {
                                                    clipboardManager.setText(AnnotatedString(snippet))
                                                    copied = true
                                                },
                                            color = TerminalSurfaceElevated,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                    contentDescription = "Copy",
                                                    tint = if (copied) GreenSuccess else TextMuted,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = if (copied) "Copied" else "Copy",
                                                    color = if (copied) GreenSuccess else TextMuted,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Execute in Terminal Button
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { onExecuteCode(snippet) },
                                            color = Color(0xFF00382E),
                                            shape = RoundedCornerShape(4.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Run",
                                                    tint = CyanAccent,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Run in Terminal",
                                                    color = CyanAccent,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
