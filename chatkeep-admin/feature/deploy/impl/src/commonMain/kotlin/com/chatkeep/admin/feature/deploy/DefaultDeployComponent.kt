package com.chatkeep.admin.feature.deploy

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.chatkeep.admin.core.common.AppResult
import com.chatkeep.admin.core.common.componentScope
import com.chatkeep.admin.core.domain.model.Workflow
import com.chatkeep.admin.core.domain.usecase.GetWorkflowsUseCase
import com.chatkeep.admin.core.domain.usecase.TriggerWorkflowUseCase
import kotlinx.coroutines.launch

class DefaultDeployComponent(
    componentContext: ComponentContext,
    private val getWorkflowsUseCase: GetWorkflowsUseCase,
    private val triggerWorkflowUseCase: TriggerWorkflowUseCase
) : DeployComponent, ComponentContext by componentContext {

    private val _state = MutableValue<DeployComponent.DeployState>(DeployComponent.DeployState.Loading)
    override val state: Value<DeployComponent.DeployState> = _state

    private val scope = componentScope()

    init {
        loadData()
    }

    override fun onRefresh() {
        loadData()
    }

    override fun onTriggerClick(workflow: Workflow) {
        val currentState = _state.value
        if (currentState is DeployComponent.DeployState.Success) {
            _state.value = currentState.copy(
                confirmDialog = DeployComponent.ConfirmDialog(workflow)
            )
        }
    }

    override fun onConfirmTrigger() {
        val currentState = _state.value
        if (currentState is DeployComponent.DeployState.Success) {
            val dialog = currentState.confirmDialog
            if (dialog != null) {
                _state.value = currentState.copy(
                    confirmDialog = null,
                    triggering = true
                )
                triggerWorkflow(dialog.workflow)
            }
        }
    }

    override fun onDismissDialog() {
        val currentState = _state.value
        if (currentState is DeployComponent.DeployState.Success) {
            _state.value = currentState.copy(confirmDialog = null)
        }
    }

    private fun loadData() {
        scope.launch {
            _state.value = DeployComponent.DeployState.Loading
            when (val result = getWorkflowsUseCase()) {
                is AppResult.Success -> {
                    _state.value = DeployComponent.DeployState.Success(result.data)
                }
                is AppResult.Error -> {
                    _state.value = DeployComponent.DeployState.Error(result.message)
                }
                is AppResult.Loading -> {
                    _state.value = DeployComponent.DeployState.Loading
                }
            }
        }
    }

    private fun triggerWorkflow(workflow: Workflow) {
        scope.launch {
            when (val result = triggerWorkflowUseCase(workflow.id)) {
                is AppResult.Success -> {
                    // Reload workflows to get updated status
                    loadData()
                }
                is AppResult.Error -> {
                    val currentState = _state.value
                    if (currentState is DeployComponent.DeployState.Success) {
                        _state.value = currentState.copy(triggering = false)
                    }
                    _state.value = DeployComponent.DeployState.Error(result.message)
                }
                is AppResult.Loading -> {
                    // Keep triggering state
                }
            }
        }
    }
}
