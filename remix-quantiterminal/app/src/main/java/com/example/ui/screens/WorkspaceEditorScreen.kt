package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.VirtualFile
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenTerminal
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

@Composable
fun WorkspaceEditorScreen(
    viewModel: MainViewModel,
    state: MainUiState,
    modifier: Modifier = Modifier
) {
    var isNewFileDialogOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(10.dp)
    ) {
        // Workspace Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalCard, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = state.activeEditingFile?.name ?: "Cloud Workspace",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = state.activeEditingFile?.path ?: state.currentDirectory,
                    fontSize = 11.sp,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // New file button
                IconButton(
                    onClick = { isNewFileDialogOpen = true },
                    modifier = Modifier.testTag("btn_create_file")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New File",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Save button
                IconButton(
                    onClick = { viewModel.saveActiveFile() },
                    enabled = state.activeEditingFile != null,
                    modifier = Modifier.testTag("btn_save_file")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save File",
                        tint = if (state.isEditorModified) CyanAccent else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Run in Terminal button
                Button(
                    onClick = { viewModel.runActiveFileInTerminal() },
                    enabled = state.activeEditingFile != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenTerminal,
                        contentColor = Color(0xFF003914)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_run_in_terminal")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run in VM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Files Quick Row (Tab selector for files)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            state.currentFiles.forEach { file ->
                val isSelected = state.activeEditingFile?.path == file.path
                Surface(
                    color = if (isSelected) Color(0xFF1E3A5F) else TerminalSurfaceElevated,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) CyanAccent else TerminalBorder
                    ),
                    modifier = Modifier.clickable { viewModel.openFileInEditor(file) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = if (file.isDirectory) Color(0xFFFBBF24) else CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = file.name,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Code Editor Canvas
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = TerminalSurface,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
        ) {
            if (state.activeEditingFile == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Code",
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Select a file from the list above to view and edit code.", color = TextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Line numbers gutter
                    val lineCount = (state.activeEditorContent.count { it == '\n' } + 1).coerceAtLeast(1)
                    val gutterText = (1..lineCount).joinToString("\n")
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(Color(0xFF0D131F))
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = gutterText,
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        )
                    }

                    // Main code input area
                    OutlinedTextField(
                        value = state.activeEditorContent,
                        onValueChange = { viewModel.updateEditorContent(it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("code_editor_input"),
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }

    if (isNewFileDialogOpen) {
        var newFileName by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { isNewFileDialogOpen = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalCard),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Create New File", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        placeholder = { Text("e.g. app.py, index.js, script.sh", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_filename"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = TerminalBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isNewFileDialogOpen = false }) {
                            Text("Cancel", color = TextMuted)
                        }
                        Button(
                            onClick = {
                                if (newFileName.isNotBlank()) {
                                    viewModel.createNewFile(newFileName.trim())
                                    isNewFileDialogOpen = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color(0xFF00382E)
                            ),
                            modifier = Modifier.testTag("btn_confirm_new_file")
                        ) {
                            Text("Create", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
