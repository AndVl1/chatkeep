package com.chatkeep.admin.feature.deploy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.chatkeep.admin.feature.deploy.Workflow
import com.chatkeep.admin.feature.deploy.WorkflowStatus
import com.chatkeep.admin.feature.deploy.DeployComponent
import kotlinx.datetime.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeployScreen(component: DeployComponent) {
    val state by component.state.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deploy") }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val currentState = state) {
                is DeployComponent.DeployState.Loading -> LoadingContent()
                is DeployComponent.DeployState.Error -> ErrorContent(
                    message = currentState.message,
                    onRetry = component::onRefresh
                )
                is DeployComponent.DeployState.Success -> DeployContent(
                    workflows = currentState.workflows,
                    triggering = currentState.triggering,
                    confirmDialog = currentState.confirmDialog,
                    onTriggerClick = component::onTriggerClick,
                    onConfirmTrigger = component::onConfirmTrigger,
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
private fun DeployContent(
    workflows: List<Workflow>,
    triggering: Boolean,
    confirmDialog: DeployComponent.ConfirmDialog?,
    onTriggerClick: (Workflow) -> Unit,
    onConfirmTrigger: () -> Unit,
    onDismissDialog: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(workflows, key = { it.id }) { workflow ->
            WorkflowItem(
                workflow = workflow,
                enabled = !triggering,
                onTriggerClick = { onTriggerClick(workflow) }
            )
        }
    }

    if (confirmDialog != null) {
        TriggerConfirmationDialog(
            workflow = confirmDialog.workflow,
            onConfirm = onConfirmTrigger,
            onDismiss = onDismissDialog
        )
    }
}

@Composable
private fun WorkflowItem(
    workflow: Workflow,
    enabled: Boolean,
    onTriggerClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workflow.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                workflow.lastRun?.let { lastRun ->
                    StatusBadge(lastRun.status)
                }
            }

            Text(
                text = workflow.filename,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            workflow.lastRun?.let { lastRun ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    lastRun.conclusion?.let { conclusion ->
                        Text(
                            text = "Conclusion: $conclusion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Last run: ${formatTimestamp(lastRun.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Triggered by: ${lastRun.triggeredBy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onTriggerClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run Workflow")
            }
        }
    }
}

@Composable
private fun StatusBadge(status: WorkflowStatus) {
    val (text, color) = when (status) {
        WorkflowStatus.QUEUED -> "Queued" to MaterialTheme.colorScheme.tertiary
        WorkflowStatus.IN_PROGRESS -> "Running" to MaterialTheme.colorScheme.primary
        WorkflowStatus.COMPLETED -> "Completed" to MaterialTheme.colorScheme.primary
        WorkflowStatus.UNKNOWN -> "Unknown" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun TriggerConfirmationDialog(
    workflow: Workflow,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trigger Workflow?") },
        text = {
            Text("Do you want to trigger \"${workflow.name}\"? This will start a new workflow run on GitHub Actions.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Trigger")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTimestamp(instant: Instant): String {
    return instant.toString().replace('T', ' ').substringBefore('.')
}
