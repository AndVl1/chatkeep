package com.chatkeep.admin.feature.home

import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable

interface HomeComponent {
    val state: Value<HomeState>

    fun onItemClick(item: HomeItem)
    fun onRefresh()
}

sealed class HomeState {
    data object Loading : HomeState()
    data class Success(val items: List<HomeItem>) : HomeState()
    data class Error(val message: String) : HomeState()
}

@Serializable
data class HomeItem(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long
)
