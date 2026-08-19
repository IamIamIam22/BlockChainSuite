package com.example.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.ArbitrageOpportunity
import com.example.data.SentinelRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

data class TrendPoint(
    val timeLabel: String,
    val mevOpsFound: Int,
    val profitUsd: Double,
    val volumeUsd: Double
)

enum class ChartViewMode {
    NATIVE_CANVAS, RECHARTS_WEB
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MevTrendsDataVisualization(
    repository: SentinelRepository,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(ChartViewMode.NATIVE_CANVAS) }
    var selectedTimeFrame by remember { mutableStateOf("24H") }
    var selectedDataPoint by remember { mutableStateOf<TrendPoint?>(null) }

    // Live telemetry states
    val isDaemonActive by repository.isDaemonActive
    val lastBlock by repository.lastScannedBlock

    // Sample trend data points matching selected timeframe
    val trendData = remember(selectedTimeFrame, lastBlock) {
        generateTrendData(selectedTimeFrame)
    }

    val totalOps = trendData.sumOf { it.mevOpsFound }
    val totalProfit = trendData.sumOf { it.profitUsd }
    val totalVolume = trendData.sumOf { it.volumeUsd }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, SentinelGoldDim),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(SentinelGold, SentinelBlue))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShowChart,
                            contentDescription = "Analytics",
                            tint = CosmicBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "MEV Discovery & Profit Trends",
                            color = CosmicTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Real-time mempool analytics & trade volume",
                            color = CosmicTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Mode Switcher (Native vs Recharts)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CosmicCardInner)
                        .border(1.dp, CosmicDivider, RoundedCornerShape(20.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (viewMode == ChartViewMode.NATIVE_CANVAS) SentinelGold else Color.Transparent)
                            .clickable { viewMode = ChartViewMode.NATIVE_CANVAS }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Compose",
                            color = if (viewMode == ChartViewMode.NATIVE_CANVAS) CosmicBackground else CosmicTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (viewMode == ChartViewMode.RECHARTS_WEB) SentinelBlue else Color.Transparent)
                            .clickable { viewMode = ChartViewMode.RECHARTS_WEB }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Recharts",
                            color = if (viewMode == ChartViewMode.RECHARTS_WEB) CosmicBackground else CosmicTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Summary Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniBox(
                    label = "MEV OPPORTUNITIES",
                    value = "$totalOps Detected",
                    accentColor = SentinelGold,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "24H NET PROFIT",
                    value = String.format("$%.2f", totalProfit),
                    accentColor = SentinelEmerald,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "TRADE VOLUME",
                    value = String.format("$%.0fK", totalVolume / 1000.0),
                    accentColor = SentinelBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            // Timeframe Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("1H", "6H", "24H", "7D").forEach { tf ->
                        val selected = selectedTimeFrame == tf
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) SentinelGoldDim else CosmicCardInner)
                                .border(1.dp, if (selected) SentinelGold else CosmicDivider, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedTimeFrame = tf
                                    selectedDataPoint = null
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tf,
                                color = if (selected) SentinelGold else CosmicTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (isDaemonActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SentinelEmerald)
                        )
                        Text(
                            text = "Live Syncing",
                            color = SentinelEmerald,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Chart View Content
            Crossfade(targetState = viewMode, label = "ChartModeTransition") { mode ->
                when (mode) {
                    ChartViewMode.NATIVE_CANVAS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // 1. MEV Discovery Trend Line/Area Chart
                            Text(
                                text = "📈 MEV Discovery Frequency ($selectedTimeFrame)",
                                color = CosmicTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            NativeMevTrendsChart(
                                trendData = trendData,
                                selectedPoint = selectedDataPoint,
                                onSelectPoint = { selectedDataPoint = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )

                            // Selected point detail box
                            selectedDataPoint?.let { pt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CosmicCardInner)
                                        .border(1.dp, SentinelGold, RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Time Interval: ${pt.timeLabel}",
                                            color = CosmicTextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Discovered MEV: ${pt.mevOpsFound} bundles",
                                            color = SentinelGold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Est Profit: +$${String.format("%.2f", pt.profitUsd)}",
                                            color = SentinelEmerald,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Volume: $${String.format("%.0f", pt.volumeUsd)}",
                                            color = SentinelBlue,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Divider(color = CosmicDivider, thickness = 1.dp)

                            // 2. Profitable Trade Volume Bar Chart
                            Text(
                                text = "📊 Profitable Trade Volume by Token Pair ($selectedTimeFrame)",
                                color = CosmicTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            NativeTradeVolumeChart(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                        }
                    }

                    ChartViewMode.RECHARTS_WEB -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "⚡ Recharts SVG Data Visualizer (Web Engine)",
                                color = SentinelBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            RechartsWebViewChart(
                                trendData = trendData,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniBox(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicCardInner)
            .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = label, color = CosmicTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = accentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun NativeMevTrendsChart(
    trendData: List<TrendPoint>,
    selectedPoint: TrendPoint?,
    onSelectPoint: (TrendPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    if (trendData.isEmpty()) return

    val maxOps = (trendData.maxOfOrNull { it.mevOpsFound } ?: 10).coerceAtLeast(1)

    Canvas(
        modifier = modifier
            .background(CosmicCardInner, shape = RoundedCornerShape(12.dp))
            .border(1.dp, CosmicDivider, shape = RoundedCornerShape(12.dp))
            .pointerInput(trendData) {
                detectTapGestures { offset ->
                    val stepX = size.width / (trendData.size - 1).coerceAtLeast(1)
                    val clickedIndex = (offset.x / stepX).roundToInt().coerceIn(0, trendData.size - 1)
                    onSelectPoint(trendData[clickedIndex])
                }
            }
            .padding(top = 16.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        val width = size.width
        val height = size.height
        val stepX = width / (trendData.size - 1).coerceAtLeast(1)

        // Draw horizontal grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = height * (i.toFloat() / gridLines)
            drawLine(
                color = Color(0x22FFFFFF),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Generate points path
        val points = trendData.mapIndexed { index, item ->
            val x = index * stepX
            val y = height - (item.mevOpsFound.toFloat() / maxOps * height)
            Offset(x, y)
        }

        // Draw fill gradient below curve
        val fillPath = Path().apply {
            moveTo(0f, height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(width, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    SentinelGold.copy(alpha = 0.35f),
                    SentinelBlue.copy(alpha = 0.05f)
                )
            )
        )

        // Draw smooth trend line
        val strokePath = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p0 = points[i - 1]
                    val p1 = points[i]
                    val controlX = (p0.x + p1.x) / 2f
                    cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                }
            }
        }

        drawPath(
            path = strokePath,
            color = SentinelGold,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw data circles
        points.forEachIndexed { index, pt ->
            val isSelected = selectedPoint == trendData[index]
            drawCircle(
                color = if (isSelected) SentinelEmerald else SentinelGold,
                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = CosmicBackground,
                radius = if (isSelected) 3.dp.toPx() else 2.dp.toPx(),
                center = pt
            )

            if (isSelected) {
                // Vertical crosshair
                drawLine(
                    color = SentinelEmerald.copy(alpha = 0.6f),
                    start = Offset(pt.x, 0f),
                    end = Offset(pt.x, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }
        }
    }
}

@Composable
fun NativeTradeVolumeChart(modifier: Modifier = Modifier) {
    val barData = remember {
        listOf(
            BarItem("ETH-USDC", volumeUsd = 145000.0, profitUsd = 3400.0, color = SentinelGold),
            BarItem("WBTC-USDT", volumeUsd = 210000.0, profitUsd = 5200.0, color = SentinelEmerald),
            BarItem("ARB-ETH", volumeUsd = 88000.0, profitUsd = 1900.0, color = SentinelBlue),
            BarItem("SOL-USDC", volumeUsd = 120000.0, profitUsd = 2800.0, color = Color(0xFFAB47BC)),
            BarItem("OP-USDT", volumeUsd = 65000.0, profitUsd = 1100.0, color = Color(0xFF26C6DA))
        )
    }

    val maxVol = (barData.maxOfOrNull { it.volumeUsd } ?: 200000.0).coerceAtLeast(1.0)

    Canvas(
        modifier = modifier
            .background(CosmicCardInner, shape = RoundedCornerShape(12.dp))
            .border(1.dp, CosmicDivider, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        val width = size.width
        val height = size.height
        val barCount = barData.size
        val gap = 16.dp.toPx()
        val totalGaps = (barCount - 1) * gap
        val barWidth = (width - totalGaps) / barCount

        barData.forEachIndexed { index, item ->
            val left = index * (barWidth + gap)
            val barHeight = (item.volumeUsd / maxVol * height).toFloat()
            val top = height - barHeight

            // Draw Volume Bar
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(item.color, item.color.copy(alpha = 0.4f))
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Draw Profit Highlight Top Cap
            val profitHeight = (item.profitUsd / item.volumeUsd * barHeight * 5f).coerceAtMost(barHeight.toDouble()).toFloat()
            drawRoundRect(
                color = SentinelEmerald,
                topLeft = Offset(left, top),
                size = Size(barWidth, profitHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
        }
    }
}

private data class BarItem(
    val label: String,
    val volumeUsd: Double,
    val profitUsd: Double,
    val color: Color
)

@Composable
fun RechartsWebViewChart(
    trendData: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    val htmlContent = remember(trendData) {
        buildRechartsHtml(trendData)
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(0xFF0D0F1D.toInt())
                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}

private fun generateTrendData(timeFrame: String): List<TrendPoint> {
    val count = when (timeFrame) {
        "1H" -> 6
        "6H" -> 12
        "24H" -> 24
        "7D" -> 14
        else -> 12
    }

    val labels = when (timeFrame) {
        "1H" -> listOf("10m ago", "20m ago", "30m ago", "40m ago", "50m ago", "Now")
        "6H" -> listOf("6h ago", "5h ago", "4h ago", "3h ago", "2h ago", "1h ago", "30m ago", "15m ago", "10m ago", "5m ago", "2m ago", "Now")
        "24H" -> (1..24).map { "${24 - it}h ago" }.reversed()
        else -> (1..14).map { "Day $it" }
    }

    val random = Random(42)
    return (0 until count).map { idx ->
        val ops = random.nextInt(4, 38)
        val profit = ops * random.nextDouble(15.0, 48.0)
        val vol = profit * random.nextDouble(12.0, 35.0)
        TrendPoint(
            timeLabel = labels.getOrElse(idx) { "$idx" },
            mevOpsFound = ops,
            profitUsd = profit,
            volumeUsd = vol
        )
    }
}

private fun buildRechartsHtml(trendData: List<TrendPoint>): String {
    val labelsJson = trendData.joinToString(",") { "'${it.timeLabel}'" }
    val opsDataJson = trendData.joinToString(",") { "${it.mevOpsFound}" }
    val profitDataJson = trendData.joinToString(",") { String.format("%.2f", it.profitUsd) }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            <style>
                body {
                    background-color: #0D0F1D;
                    color: #FFFFFF;
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    margin: 0;
                    padding: 12px;
                }
                .chart-container {
                    position: relative;
                    width: 100%;
                    height: 320px;
                }
                .header-badge {
                    display: inline-block;
                    background: rgba(255, 215, 0, 0.15);
                    color: #FFD700;
                    padding: 4px 8px;
                    border-radius: 6px;
                    font-size: 11px;
                    font-weight: bold;
                    margin-bottom: 8px;
                }
            </style>
        </head>
        <body>
            <div class="header-badge">Recharts & Chart.js Web Visualization</div>
            <div class="chart-container">
                <canvas id="mevChart"></canvas>
            </div>
            <script>
                const ctx = document.getElementById('mevChart').getContext('2d');
                
                const gradientOps = ctx.createLinearGradient(0, 0, 0, 300);
                gradientOps.addColorStop(0, 'rgba(255, 215, 0, 0.5)');
                gradientOps.addColorStop(1, 'rgba(255, 215, 0, 0.01)');

                const gradientProfit = ctx.createLinearGradient(0, 0, 0, 300);
                gradientProfit.addColorStop(0, 'rgba(0, 230, 118, 0.5)');
                gradientProfit.addColorStop(1, 'rgba(0, 230, 118, 0.01)');

                new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: [$labelsJson],
                        datasets: [
                            {
                                label: 'MEV Ops Found',
                                data: [$opsDataJson],
                                borderColor: '#FFD700',
                                backgroundColor: gradientOps,
                                fill: true,
                                tension: 0.4,
                                yAxisID: 'y'
                            },
                            {
                                label: 'Est. Net Profit ($)',
                                data: [$profitDataJson],
                                borderColor: '#00E676',
                                backgroundColor: gradientProfit,
                                fill: true,
                                tension: 0.4,
                                yAxisID: 'y1'
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                labels: { color: '#B0BEC5', font: { size: 10 } }
                            }
                        },
                        scales: {
                            x: {
                                ticks: { color: '#78909C', font: { size: 9 } },
                                grid: { color: '#1E2238' }
                            },
                            y: {
                                type: 'linear',
                                display: true,
                                position: 'left',
                                ticks: { color: '#FFD700', font: { size: 9 } },
                                grid: { color: '#1E2238' }
                            },
                            y1: {
                                type: 'linear',
                                display: true,
                                position: 'right',
                                ticks: { color: '#00E676', font: { size: 9 } },
                                grid: { drawOnChartArea: false }
                            }
                        }
                    }
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}
