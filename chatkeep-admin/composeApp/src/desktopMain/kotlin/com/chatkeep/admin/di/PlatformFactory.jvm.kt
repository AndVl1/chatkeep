package com.chatkeep.admin.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.chatkeep.admin.core.common.DesktopPlatformContext
import com.chatkeep.admin.core.common.createDataStorePath
import com.chatkeep.admin.core.data.local.DataStoreTokenStorage
import com.chatkeep.admin.core.common.TokenStorage
import okio.Path.Companion.toPath

actual fun createPlatformDataStore(context: Any): Any {
    val platformContext = DesktopPlatformContext()
    val path = createDataStorePath(platformContext)
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { "$path/chatkeep_admin.preferences_pb".toPath() }
    )
}

actual fun createPlatformTokenStorage(dataStore: Any): TokenStorage {
    @Suppress("UNCHECKED_CAST")
    return DataStoreTokenStorage(dataStore as DataStore<Preferences>)
}

actual fun getApiBaseUrl(): String {
    return System.getenv("API_BASE_URL") ?: "https://admin.chatmoderatorbot.ru"
}
