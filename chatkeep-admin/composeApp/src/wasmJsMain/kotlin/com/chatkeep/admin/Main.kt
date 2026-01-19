package com.chatkeep.admin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.chatkeep.admin.core.common.DeepLinkData
import com.chatkeep.admin.di.AppFactory
import com.chatkeep.admin.di.createPlatformDataStore
import com.chatkeep.admin.di.createPlatformHttpClient
import com.chatkeep.admin.di.createPlatformTokenStorage

// External JS functions for window message handling
@JsFun("(callback) => { window.addEventListener('message', (event) => { callback(event.data); }); }")
private external fun addMessageListener(callback: (JsAny) -> Unit)

@JsFun("(data) => data && data.type === 'chatkeep-auth'")
private external fun isAuthMessage(data: JsAny): Boolean

@JsFun("(data) => BigInt(data.data.id)")
private external fun getAuthId(data: JsAny): JsBigInt

@JsFun("(data) => data.data.first_name")
private external fun getAuthFirstName(data: JsAny): JsString

@JsFun("(data) => data.data.last_name || null")
private external fun getAuthLastName(data: JsAny): JsString?

@JsFun("(data) => data.data.username || null")
private external fun getAuthUsername(data: JsAny): JsString?

@JsFun("(data) => data.data.photo_url || null")
private external fun getAuthPhotoUrl(data: JsAny): JsString?

@JsFun("(data) => BigInt(data.data.auth_date)")
private external fun getAuthDate(data: JsAny): JsBigInt

@JsFun("(data) => data.data.hash")
private external fun getAuthHash(data: JsAny): JsString

@JsFun("(data) => data.data.state")
private external fun getAuthState(data: JsAny): JsString

// URL parameter checking
@JsFun("() => { const params = new URLSearchParams(window.location.search); return params.has('id') && params.has('hash'); }")
private external fun hasAuthParamsInUrl(): Boolean

@JsFun("() => { const params = new URLSearchParams(window.location.search); return params.get('id') || ''; }")
private external fun getUrlParamId(): JsString

@JsFun("() => { const params = new URLSearchParams(window.location.search); return params.get('first_name') || ''; }")
private external fun getUrlParamFirstName(): JsString

@JsFun("() => { const params = new URLSearchParams(window.location.search); return params.get('last_name') || null; }")
private external fun getUrlParamLastName(): JsString?

@JsFun("() => { const params = new URLSearchParams(window.location.search); return params.get('username') || null; }")
private external fun getUrlParamUsername(): JsString?

@JsFun("() => { const params = new URLSearchParams(window.location.search); return params.get('photo_url') || null; }")
private external fun getUrlParamPhotoUrl(): JsString?

@JsFun("() => { const params = new URLSearchParams(window.location.search); return params.get('auth_date') || ''; }")
private external fun getUrlParamAuthDate(): JsString

@JsFun("() => { const params = new URLSearchParams(window.location.search); return params.get('hash') || ''; }")
private external fun getUrlParamHash(): JsString

@JsFun("() => { const params = new URLSearchParams(window.location.search); return params.get('state') || ''; }")
private external fun getUrlParamState(): JsString

@JsFun("() => { window.history.replaceState({}, '', window.location.pathname); }")
private external fun clearUrlParams()

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val lifecycle = LifecycleRegistry()

    // Create platform dependencies
    val dataStore = createPlatformDataStore(Unit)
    // WASM doesn't support DataStore, so always use default
    val baseUrl = "https://admin.chatmoderatorbot.ru"
    val httpClient = createPlatformHttpClient(baseUrl)
    val tokenStorage = createPlatformTokenStorage(dataStore)

    // Create app factory
    val appFactory = AppFactory(
        httpClient = httpClient,
        baseUrl = baseUrl,
        tokenStorage = tokenStorage,
        dataStore = dataStore
    )

    // Create root component
    val rootComponent = appFactory.createRootComponent(
        componentContext = DefaultComponentContext(lifecycle)
    )

    // Check URL parameters for auth callback (redirect flow)
    if (hasAuthParamsInUrl()) {
        println("[Auth] Found auth data in URL, processing...")

        val id = getUrlParamId().toString().toLongOrNull()
        val authDate = getUrlParamAuthDate().toString().toLongOrNull()

        if (id != null && authDate != null) {
            val deepLinkData = DeepLinkData(
                id = id,
                firstName = getUrlParamFirstName().toString(),
                lastName = getUrlParamLastName()?.toString(),
                username = getUrlParamUsername()?.toString(),
                photoUrl = getUrlParamPhotoUrl()?.toString(),
                authDate = authDate,
                hash = getUrlParamHash().toString(),
                state = getUrlParamState().toString()
            )
            rootComponent.handleDeepLink(deepLinkData)

            // Clean URL after processing
            clearUrlParams()
        }
    }

    // Listen for auth callback messages from login popup
    addMessageListener { data ->
        if (isAuthMessage(data)) {
            val deepLinkData = DeepLinkData(
                id = getAuthId(data).toLong(),
                firstName = getAuthFirstName(data).toString(),
                lastName = getAuthLastName(data)?.toString(),
                username = getAuthUsername(data)?.toString(),
                photoUrl = getAuthPhotoUrl(data)?.toString(),
                authDate = getAuthDate(data).toLong(),
                hash = getAuthHash(data).toString(),
                state = getAuthState(data).toString()
            )
            rootComponent.handleDeepLink(deepLinkData)
        }
    }

    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        App(
            rootComponent = rootComponent,
            settingsRepository = appFactory.settingsRepository
        )
    }
}

