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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.NetworkExecutor
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
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun PortsPreviewScreen(
    viewModel: MainViewModel,
    state: MainUiState,
    modifier: Modifier = Modifier
) {
    var urlTarget by remember { mutableStateOf("https://httpbin.org/get") }
    var responseBody by remember { mutableStateOf("Ready to send HTTP requests to cloud services or web endpoints.") }
    var isLoading by remember { mutableStateOf(false) }
    var statusCode by remember { mutableStateOf("200 OK") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(12.dp)
    ) {
        // Forwarded Ports Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TerminalCard),
            shape = RoundedCornerShape(12.dp),
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
                        Icon(Icons.Default.Sensors, contentDescription = "Ports", tint = GreenSuccess, modifier = Modifier.size(18.dp))
                        Text("Active Cloud Ports & Web Services", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text("Auto-Forwarded", color = CyanAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PortChip(port = 3000, service = "Node.js / Express", modifier = Modifier.weight(1f)) {
                        urlTarget = "https://httpbin.org/json"
                    }
                    PortChip(port = 8000, service = "FastAPI / Python", modifier = Modifier.weight(1f)) {
                        urlTarget = "https://httpbin.org/get"
                    }
                    PortChip(port = 8080, service = "Go / Caddy Web", modifier = Modifier.weight(1f)) {
                        urlTarget = "https://httpbin.org/uuid"
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Web Inspector & HTTP Test Client
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = TerminalSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFF0F3E2E),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "GET",
                            color = GreenSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }

                    OutlinedTextField(
                        value = urlTarget,
                        onValueChange = { urlTarget = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("http_url_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = TerminalBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                responseBody = NetworkExecutor.executeCurl(urlTarget)
                                isLoading = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF00382E)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_send_http_request")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF00382E))
                        } else {
                            Text("Send", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Response body display
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = Color(0xFF080C14),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = responseBody,
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
}

@Composable
private fun PortChip(
    port: Int,
    service: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        color = TerminalSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(GreenSuccess, RoundedCornerShape(3.dp))
                )
                Text(
                    text = ":$port",
                    color = CyanAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(text = service, color = TextMuted, fontSize = 10.sp, maxLines = 1)
        }
    }
}
