package com.chatkeep.admin.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.chatkeep.admin.core.ui.components.ErrorContent
import com.chatkeep.admin.core.ui.components.LoadingContent
import com.chatkeep.admin.core.ui.resources.Res
import com.chatkeep.admin.core.ui.resources.home_title
import com.chatkeep.admin.feature.home.HomeComponent
import com.chatkeep.admin.feature.home.HomeState
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    component: HomeComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.home_title)) }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val currentState = state) {
                is HomeState.Loading -> LoadingContent(isLoading = true) {}
                is HomeState.Error -> ErrorContent(
                    message = currentState.message,
                    onRetry = component::onRefresh
                )
                is HomeState.Success -> HomeContent(
                    items = currentState.items,
                    onItemClick = component::onItemClick
                )
            }
        }
    }
}
