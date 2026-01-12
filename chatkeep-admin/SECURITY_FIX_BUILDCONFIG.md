# Security Fix: BuildConfig for Mock Authentication Gating

## Overview

This document describes the security fix implemented to prevent hardcoded mock authentication from being accessible in production builds.

## Problem

The `DefaultAuthComponent` had hardcoded mock authentication credentials that would work in any build configuration, including production releases. This is a **CRITICAL security vulnerability**.

## Solution

Implemented a platform-specific `BuildConfig` mechanism using Kotlin Multiplatform's `expect/actual` pattern to gate mock authentication behind a debug flag.

## Implementation

### 1. BuildConfig (expect/actual)

**Location**: `core/common/src/*/kotlin/com/chatkeep/admin/core/common/BuildConfig.kt`

#### Common (expect)
```kotlin
expect object BuildConfig {
    val isDebug: Boolean
}
```

#### Android (actual)
- Uses `ApplicationInfo.FLAG_DEBUGGABLE` to detect debug builds
- Must be initialized with `BuildConfig.init(context)` before use
- Returns `false` if not initialized (safe default)

#### iOS (actual)
- Checks `NSBundle.mainBundle` for `IS_DEBUG` key
- Add to Info.plist during build:
  ```xml
  <key>IS_DEBUG</key>
  <string>1</string>
  ```

#### Desktop/JVM (actual)
- Checks system property: `chatkeep.debug`
- Checks environment variable: `CHATKEEP_DEBUG`
- Run with: `java -Dchatkeep.debug=true` or `export CHATKEEP_DEBUG=true`

#### WASM (actual)
- Checks JavaScript global: `__DEBUG__`
- Set during build configuration

### 2. DefaultAuthComponent

**Location**: `feature/auth/impl/src/commonMain/kotlin/com/chatkeep/admin/feature/auth/DefaultAuthComponent.kt`

```kotlin
private fun performLogin() {
    scope.launch {
        _state.value = AuthComponent.AuthState.Loading

        // SECURITY: Mock authentication ONLY works in debug mode
        if (!BuildConfig.isDebug) {
            _state.value = AuthComponent.AuthState.Error(
                "Telegram Login Widget integration required. " +
                "Mock authentication is disabled in production builds."
            )
            return@launch
        }

        // DEBUG ONLY: Using mock data for development
        val mockTelegramData = ...
    }
}
```

### 3. Android Initialization

**Location**: `composeApp/src/androidMain/kotlin/com/chatkeep/admin/MainActivity.kt`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize BuildConfig for debug mode detection
    BuildConfig.init(applicationContext)

    // Rest of initialization...
}
```

## Testing

### Debug Mode (Development)
1. Run debug build: `./gradlew :composeApp:installDebug`
2. Mock authentication should work

### Release Mode (Production)
1. Build release: `./gradlew :composeApp:assembleRelease`
2. Mock authentication should fail with error message
3. User sees: "Telegram Login Widget integration required..."

## Platform-Specific Debug Flags

### Android
- Debug APK: `isDebug = true`
- Release APK: `isDebug = false`

### iOS
Add to `iosApp/Configuration/Debug.xcconfig`:
```
IS_DEBUG = 1
```

And to `iosApp/Configuration/Release.xcconfig`:
```
IS_DEBUG = 0
```

### Desktop
Debug mode:
```bash
./gradlew :composeApp:run -Dchatkeep.debug=true
```

Or:
```bash
export CHATKEEP_DEBUG=true
./gradlew :composeApp:run
```

### WASM
In `webpack.config.d/debug.js`:
```javascript
config.plugins.push(
    new webpack.DefinePlugin({
        '__DEBUG__': JSON.stringify(true)
    })
);
```

## Next Steps

1. **Implement Telegram Login Widget** - Replace mock authentication with real Telegram OAuth
2. **Remove mock code** - Once widget is implemented, delete all mock authentication code
3. **Add instrumented tests** - Verify release builds reject mock login

## Files Changed

- `core/common/src/commonMain/kotlin/com/chatkeep/admin/core/common/BuildConfig.kt` (created)
- `core/common/src/androidMain/kotlin/com/chatkeep/admin/core/common/BuildConfig.android.kt` (created)
- `core/common/src/iosMain/kotlin/com/chatkeep/admin/core/common/BuildConfig.ios.kt` (created)
- `core/common/src/desktopMain/kotlin/com/chatkeep/admin/core/common/BuildConfig.desktop.kt` (created)
- `core/common/src/wasmJsMain/kotlin/com/chatkeep/admin/core/common/BuildConfig.wasmJs.kt` (created)
- `feature/auth/impl/src/commonMain/kotlin/com/chatkeep/admin/feature/auth/DefaultAuthComponent.kt` (modified)
- `composeApp/src/androidMain/kotlin/com/chatkeep/admin/MainActivity.kt` (modified)

## Security Notes

- ✅ Mock credentials ONLY work in debug builds
- ✅ Release builds show clear error message
- ✅ No System.* calls in common code
- ✅ Uses platform-specific mechanisms for detection
- ✅ Safe defaults (assumes production if detection fails)
- ⚠️ Desktop requires explicit flag (not automatic like Android)
- ⚠️ iOS requires Info.plist configuration

## Verification

```bash
# Build all targets
./gradlew :composeApp:assembleDebug      # Should allow mock login
./gradlew :composeApp:assembleRelease    # Should block mock login

# Verify core modules compile for all platforms
./gradlew :core:common:compileKotlinAndroid
./gradlew :core:common:compileKotlinIosSimulatorArm64
./gradlew :core:common:compileKotlinDesktop

# Verify auth module compiles for all platforms
./gradlew :feature:auth:impl:compileDebugKotlinAndroid
./gradlew :feature:auth:impl:compileKotlinIosSimulatorArm64
./gradlew :feature:auth:impl:compileKotlinDesktop
```
