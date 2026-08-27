package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SoftwareItem
import com.example.data.model.ToolType
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCard
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.MainViewModel

@Composable
fun MarketplaceScreen(
    viewModel: MainViewModel,
    state: MainUiState,
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "AI / LLM", "NPM", "Python", "Ruby", "Go", "Rust", "Docker", "Databases", "System")

    val filteredList = state.softwareList.filter { item ->
        val matchesCategory = when (state.selectedCategory) {
            "All" -> true
            "AI / LLM" -> item.toolType == ToolType.AI_LLM
            "NPM" -> item.toolType == ToolType.NPM
            "Python" -> item.toolType == ToolType.PYTHON
            "Ruby" -> item.toolType == ToolType.RUBY
            "Go" -> item.toolType == ToolType.GO
            "Rust" -> item.toolType == ToolType.RUST
            "Docker" -> item.toolType == ToolType.DOCKER
            "Databases" -> item.category.contains("Database") || item.toolType == ToolType.DEV_OPS
            "System" -> item.toolType == ToolType.SYSTEM
            else -> true
        }

        val matchesSearch = if (state.searchQuery.isEmpty()) true else {
            item.name.contains(state.searchQuery, ignoreCase = true) ||
                    item.description.contains(state.searchQuery, ignoreCase = true) ||
                    item.category.contains(state.searchQuery, ignoreCase = true)
        }

        matchesCategory && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(12.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search 2026 software market (npm, pip, ruby, go, AI)...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanAccent) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("marketplace_search_bar"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = TerminalBorder,
                focusedContainerColor = TerminalCard,
                unfocusedContainerColor = TerminalCard,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val selected = state.selectedCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setSelectedCategory(category) },
                    label = {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanAccent,
                        selectedLabelColor = Color(0xFF00382E),
                        containerColor = TerminalSurfaceElevated,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("filter_chip_$category")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Software Items List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .testTag("software_catalog_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredList, key = { it.id }) { item ->
                SoftwareCard(
                    item = item,
                    onInstall = { viewModel.installSoftware(item) },
                    onRunSample = {
                        viewModel.setTab(com.example.ui.viewmodel.NavigationTab.TERMINAL)
                        viewModel.executeCommand(item.launchCommand)
                    }
                )
            }
        }
    }
}

@Composable
fun SoftwareCard(
    item: SoftwareItem,
    onInstall: () -> Unit,
    onRunSample: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val badgeColor = when (item.toolType) {
        ToolType.NPM -> Color(0xFFCB3837)
        ToolType.PYTHON -> Color(0xFF3776AB)
        ToolType.RUBY -> Color(0xFFCC342D)
        ToolType.GO -> Color(0xFF00ADD8)
        ToolType.RUST -> Color(0xFFDEA584)
        ToolType.DOCKER -> Color(0xFF2496ED)
        ToolType.AI_LLM -> PurpleAccent
        ToolType.DEV_OPS -> AmberWarning
        ToolType.SYSTEM -> GreenSuccess
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("software_card_${item.id}"),
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
                    // Ecosystem Tag
                    Surface(
                        color = badgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = item.toolType.name,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = item.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Stars",
                        tint = AmberWarning,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = item.stars,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.description,
                color = TextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Command preview snippet box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = TerminalSurfaceElevated,
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$ ${item.commandInstall}",
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyanAccent
                    )
                }
            }

            // Expandable code recipe
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text("Sample Usage in Cloud VM:", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF070A10),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.sampleCode,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions: Install to VM & View Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "Hide Code" else "View Recipe",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onInstall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color(0xFF00382E)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_install_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Install",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Install to VM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
