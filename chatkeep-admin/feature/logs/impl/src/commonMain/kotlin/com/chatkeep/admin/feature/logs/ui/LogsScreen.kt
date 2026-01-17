package com.chatkeep.admin.feature.logs.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.chatkeep.admin.feature.logs.LogEntry
import com.chatkeep.admin.feature.logs.LogsData
import com.chatkeep.admin.feature.logs.LogsComponent
import kotlinx.datetime.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(component: LogsComponent) {
    val state by component.state.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logs") },
                actions = {
                    IconButton(onClick = component::onRefresh) {
                        Text("⟳")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val currentState = state) {
                is LogsComponent.LogsState.Loading -> LoadingContent()
                is LogsComponent.LogsState.Error -> ErrorContent(
                    message = currentState.message,
                    onRetry = component::onRefresh
                )
                is LogsComponent.LogsState.Success -> LogsContent(
                    logs = currentState.logs
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
private fun LogsContent(logs: LogsData) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Logs from ${logs.fromTime} to ${logs.toTime} (${logs.totalCount} entries)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(12.dp)
            ) {
                logs.entries.forEach { entry ->
                    LogEntryItem(entry)
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(entry: LogEntry) {
    val levelColor = when (entry.level.uppercase()) {
        "ERROR" -> Color.Red
        "WARN" -> Color(0xFFFFA500)
        "INFO" -> Color.Blue
        "DEBUG" -> Color.Gray
        else -> Color.Unspecified
    }

    val annotatedText = buildAnnotatedString {
        // Timestamp
        withStyle(style = SpanStyle(color = Color.Gray)) {
            append("${entry.timestamp} ")
        }
        // Level
        withStyle(style = SpanStyle(color = levelColor)) {
            append("[${entry.level}] ")
        }
        // Logger
        withStyle(style = SpanStyle(color = Color.DarkGray)) {
            append("${entry.logger}: ")
        }
        // Message
        append(entry.message)
    }

    Text(
        text = annotatedText,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
