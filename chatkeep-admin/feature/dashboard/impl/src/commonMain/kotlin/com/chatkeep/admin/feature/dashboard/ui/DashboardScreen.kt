package com.chatkeep.admin.feature.dashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.chatkeep.admin.feature.dashboard.DashboardInfo
import com.chatkeep.admin.feature.dashboard.Trend
import com.chatkeep.admin.feature.dashboard.DashboardComponent
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(component: DashboardComponent) {
    val state by component.state.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val currentState = state) {
                is DashboardComponent.DashboardState.Loading -> LoadingContent()
                is DashboardComponent.DashboardState.Error -> ErrorContent(
                    message = currentState.message,
                    onRetry = component::onRefresh
                )
                is DashboardComponent.DashboardState.Success -> DashboardContent(
                    dashboard = currentState.dashboard,
                    isRestarting = currentState.isRestarting,
                    showRestartDialog = currentState.showRestartDialog,
                    onRefresh = component::onRefresh,
                    onRestartClick = component::onRestartClick,
                    onConfirmRestart = component::onConfirmRestart,
                    onDismissDialog = component::onDismissDialog
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun DashboardContent(
    dashboard: DashboardInfo,
    isRestarting: Boolean,
    showRestartDialog: Boolean,
    onRefresh: () -> Unit,
    onRestartClick: () -> Unit,
    onConfirmRestart: () -> Unit,
    onDismissDialog: () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Service Status Card
            ServiceStatusCard(
                running = dashboard.serviceStatus.running,
                uptime = dashboard.serviceStatus.uptime
            )

            // Deploy Info Card
            DeployInfoCard(
                commitSha = dashboard.deployInfo.commitSha,
                deployedAt = dashboard.deployInfo.deployedAt,
                imageVersion = dashboard.deployInfo.imageVersion
            )

            // Quick Stats Card
            QuickStatsCard(
                totalChats = dashboard.quickStats.totalChats,
                messagesToday = dashboard.quickStats.messagesToday,
                messagesYesterday = dashboard.quickStats.messagesYesterday,
                trend = dashboard.quickStats.trend
            )

            // Restart Button
            Button(
                onClick = onRestartClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRestarting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isRestarting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restarting...")
                } else {
                    Text("Restart Bot")
                }
            }
        }
    }

    if (showRestartDialog) {
        RestartConfirmationDialog(
            onConfirm = onConfirmRestart,
            onDismiss = onDismissDialog
        )
    }
}

@Composable
private fun ServiceStatusCard(running: Boolean, uptime: Long) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Service Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                ) {
                    Text(
                        text = if (running) "Running" else "Stopped",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (running) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
                    )
                }

                Text(
                    text = "Uptime: ${formatUptime(uptime)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DeployInfoCard(commitSha: String?, deployedAt: Instant?, imageVersion: String?) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Deploy Info",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            InfoRow("Commit", commitSha?.take(8) ?: "N/A")
            InfoRow("Deployed", deployedAt?.let { formatTimestamp(it) } ?: "N/A")
            InfoRow("Image", imageVersion ?: "N/A")
        }
    }
}

@Composable
private fun QuickStatsCard(
    totalChats: Int,
    messagesToday: Int,
    messagesYesterday: Int,
    trend: Trend
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Quick Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            InfoRow("Total Chats", totalChats.toString())

            // Messages Today with clear value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Messages Today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = messagesToday.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            // Trend comparison with yesterday
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "vs Yesterday",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = messagesYesterday.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    TrendIndicator(trend)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TrendIndicator(trend: Trend) {
    val (text, color) = when (trend) {
        Trend.UP -> "↑" to MaterialTheme.colorScheme.primary
        Trend.DOWN -> "↓" to MaterialTheme.colorScheme.error
        Trend.SAME -> "→" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = color
    )
}

@Composable
private fun RestartConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restart Bot?") },
        text = { Text("Are you sure you want to restart the bot? This will temporarily interrupt service.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Restart")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatUptime(seconds: Long): String {
    val duration = seconds.seconds
    val days = duration.inWholeDays
    val hours = duration.inWholeHours % 24
    val minutes = (duration.inWholeMinutes % 60)

    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

private fun formatTimestamp(instant: Instant): String {
    // Simplified formatting - in production use proper date formatter
    return instant.toString().substringBefore('T')
}
