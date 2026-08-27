package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AuthDialog
import com.example.ui.screens.CopilotScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.PortsPreviewScreen
import com.example.ui.screens.SessionCloudScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.screens.VmManagerScreen
import com.example.ui.screens.WindowsBridgeScreen
import com.example.ui.screens.WorkspaceEditorScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.MyApplicationTheme
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
import com.example.ui.viewmodel.NavigationTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CloudTermApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudTermApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.messageToast) {
        state.messageToast?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg),
        topBar = {
            CloudTermTopBar(
                state = state,
                onOpenAuth = { viewModel.setAuthDialogVisible(true) },
                onOpenCloud = { viewModel.setTab(NavigationTab.SESSION_CLOUD) }
            )
        },
        bottomBar = {
            CloudTermNavigationBar(
                currentTab = state.currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.currentTab) {
                NavigationTab.TERMINAL -> TerminalScreen(viewModel = viewModel, state = state)
                NavigationTab.COPILOT -> CopilotScreen(viewModel = viewModel, state = state)
                NavigationTab.SESSION_CLOUD -> SessionCloudScreen(viewModel = viewModel, state = state)
                NavigationTab.WINDOWS_BRIDGE -> WindowsBridgeScreen(viewModel = viewModel, state = state)
                NavigationTab.MARKETPLACE -> MarketplaceScreen(viewModel = viewModel, state = state)
                NavigationTab.VM_MANAGER -> VmManagerScreen(viewModel = viewModel, state = state)
                NavigationTab.CODE_WORKSPACE -> WorkspaceEditorScreen(viewModel = viewModel, state = state)
                NavigationTab.PORTS_PREVIEW -> PortsPreviewScreen(viewModel = viewModel, state = state)
            }
        }

        if (state.isAuthDialogVisible) {
            AuthDialog(viewModel = viewModel, state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudTermTopBar(
    state: MainUiState,
    onOpenAuth: () -> Unit,
    onOpenCloud: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFF00382E),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "CloudTerm",
                        tint = CyanAccent,
                        modifier = Modifier
                            .padding(5.dp)
                            .size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "CloudTerm VM",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cloud Stream Container",
                        color = TextMuted,
                        fontSize = 10.5.sp
                    )
                }
            }

            // Top Status Indicators: Cloud Profile & Bypass Mode
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Cloud Sync Tag / Profile
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOpenCloud() },
                    color = if (state.userProfile != null) Color(0xFF062E1F) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (state.userProfile != null) GreenSuccess else TerminalBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (state.userProfile != null) Icons.Default.CloudDone else Icons.Default.Cloud,
                            contentDescription = null,
                            tint = if (state.userProfile != null) GreenSuccess else CyanAccent,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (state.userProfile != null) "CLOUD SYNC" else "LOCAL",
                            color = if (state.userProfile != null) GreenSuccess else CyanAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Bypass Mode Tag
                Surface(
                    color = if (state.bridgeState.isBypassTargetingActive) Color(0xFF062822) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (state.bridgeState.isBypassTargetingActive) CyanAccent else TerminalBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (state.bridgeState.isBypassTargetingActive) GreenSuccess else TextMuted,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (state.bridgeState.isBypassTargetingActive) "BYPASS" else "LOCAL",
                            color = if (state.bridgeState.isBypassTargetingActive) GreenSuccess else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Cloud Online Tag
                Surface(
                    color = if (state.activeVm?.isRunning == true) Color(0xFF082F1E) else Color(0xFF3B1219),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (state.activeVm?.isRunning == true) GreenSuccess else Color(0xFFEF4444)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (state.activeVm?.isRunning == true) GreenSuccess else Color(0xFFEF4444))
                        )
                        Text(
                            text = if (state.activeVm?.isRunning == true) "ON" else "OFF",
                            color = if (state.activeVm?.isRunning == true) GreenSuccess else Color(0xFFFCA5A5),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CloudTermNavigationBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("main_bottom_nav"),
        color = TerminalSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabButton(
                title = "Terminal",
                icon = Icons.Default.Terminal,
                selected = currentTab == NavigationTab.TERMINAL,
                onClick = { onTabSelected(NavigationTab.TERMINAL) },
                tag = "nav_tab_terminal"
            )

            NavTabButton(
                title = "Copilot",
                icon = Icons.Default.AutoAwesome,
                selected = currentTab == NavigationTab.COPILOT,
                onClick = { onTabSelected(NavigationTab.COPILOT) },
                tag = "nav_tab_copilot",
                highlightColor = Color(0xFFA5B4FC)
            )

            NavTabButton(
                title = "Session",
                icon = Icons.Default.Cloud,
                selected = currentTab == NavigationTab.SESSION_CLOUD,
                onClick = { onTabSelected(NavigationTab.SESSION_CLOUD) },
                tag = "nav_tab_session_cloud"
            )

            NavTabButton(
                title = "Win Bridge",
                icon = Icons.Default.DesktopWindows,
                selected = currentTab == NavigationTab.WINDOWS_BRIDGE,
                onClick = { onTabSelected(NavigationTab.WINDOWS_BRIDGE) },
                tag = "nav_tab_windows_bridge"
            )

            NavTabButton(
                title = "Packages",
                icon = Icons.Default.ShoppingBag,
                selected = currentTab == NavigationTab.MARKETPLACE,
                onClick = { onTabSelected(NavigationTab.MARKETPLACE) },
                tag = "nav_tab_marketplace"
            )

            NavTabButton(
                title = "Nodes",
                icon = Icons.Default.Storage,
                selected = currentTab == NavigationTab.VM_MANAGER,
                onClick = { onTabSelected(NavigationTab.VM_MANAGER) },
                tag = "nav_tab_vm_manager"
            )

            NavTabButton(
                title = "Code",
                icon = Icons.Default.Code,
                selected = currentTab == NavigationTab.CODE_WORKSPACE,
                onClick = { onTabSelected(NavigationTab.CODE_WORKSPACE) },
                tag = "nav_tab_workspace"
            )

            NavTabButton(
                title = "Web",
                icon = Icons.Default.Http,
                selected = currentTab == NavigationTab.PORTS_PREVIEW,
                onClick = { onTabSelected(NavigationTab.PORTS_PREVIEW) },
                tag = "nav_tab_ports"
            )
        }
    }
}

@Composable
fun NavTabButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    highlightColor: Color = CyanAccent
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag(tag),
        color = if (selected) Color(0xFF00382E) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, highlightColor) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) highlightColor else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = if (selected) highlightColor else TextMuted,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
