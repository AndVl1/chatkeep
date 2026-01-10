package com.chatkeep.admin.feature.deploy

import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.domain.model.Workflow

interface DeployComponent {
    val state: Value<DeployState>
    fun onRefresh()
    fun onTriggerClick(workflow: Workflow)
    fun onConfirmTrigger()
    fun onDismissDialog()

    sealed class DeployState {
        data object Loading : DeployState()
        data class Success(
            val workflows: List<Workflow>,
            val confirmDialog: ConfirmDialog? = null,
            val triggering: Boolean = false
        ) : DeployState()
        data class Error(val message: String) : DeployState()
    }

    data class ConfirmDialog(val workflow: Workflow)
}
