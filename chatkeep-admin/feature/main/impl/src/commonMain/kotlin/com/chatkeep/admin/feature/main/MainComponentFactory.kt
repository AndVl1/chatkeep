package com.chatkeep.admin.feature.main

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.arkivanov.decompose.ComponentContext
import com.chatkeep.admin.core.network.AdminApiService

/**
 * Factory function to create a MainComponent.
 * This is the public API for creating main components from outside the impl module.
 */
fun createMainComponent(
    componentContext: ComponentContext,
    apiService: AdminApiService,
    dataStore: DataStore<Preferences>,
    onLogout: () -> Unit
): MainComponent {
    return DefaultMainComponent(
        componentContext = componentContext,
        apiService = apiService,
        dataStore = dataStore,
        onLogout = onLogout
    )
}
