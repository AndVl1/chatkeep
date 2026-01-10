# ChatKeep Admin

A Kotlin Multiplatform (KMP) application built with Compose Multiplatform, supporting Android, iOS, Desktop (JVM), and Web (WASM).

## Project Structure

```
chatkeep-admin/
├── core/                    # Core modules
│   ├── common/             # Common utilities, Result types, extensions
│   ├── data/               # DataStore for preferences
│   ├── database/           # Room database (Android/iOS/JVM)
│   ├── network/            # Ktor HTTP client
│   └── ui/                 # Theme, components, resources
│
├── feature/                # Feature modules
│   └── home/
│       ├── api/           # Public interfaces and models
│       └── impl/          # Implementation and UI
│
├── composeApp/            # Platform entry points
│   ├── androidMain/       # Android MainActivity
│   ├── iosMain/           # iOS MainViewController
│   ├── desktopMain/       # Desktop main()
│   └── wasmJsMain/        # Web entry point
│
└── iosApp/               # Xcode project for iOS
```

## Technology Stack

- **Language**: Kotlin 2.1.0
- **UI**: Compose Multiplatform 1.7.3
- **Navigation**: Decompose 3.5.0
- **HTTP Client**: Ktor 3.1.1
- **Database**: Room 2.8.4 (Android/iOS/Desktop only)
- **Preferences**: DataStore 1.2.0
- **Serialization**: kotlinx.serialization 1.7.3

## Build & Run

### Prerequisites

- JDK 17 or higher
- Android Studio (for Android)
- Xcode 15+ (for iOS, macOS only)

### Android

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug
```

Or open the project in Android Studio and run.

### iOS

1. Build the framework:
```bash
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

2. Open `iosApp/iosApp.xcodeproj` in Xcode
3. Run on simulator or device

### Desktop

```bash
./gradlew :composeApp:run
```

Or package:
```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```

### Web (WASM)

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Open browser at http://localhost:8080

## Module Dependencies

```
composeApp
├── feature:home:impl
│   ├── feature:home:api
│   ├── core:ui
│   └── core:network
├── core:ui
│   └── core:common
├── core:network
│   └── core:common
└── core:database
    └── core:common
```

## Architecture

### Component Pattern (Decompose)

Each feature follows the component-based architecture:

1. **API module** - Public interface and models
2. **Impl module** - Implementation and Compose UI

Components are lifecycle-aware and manage their own state.

### State Management

- State is exposed via `Value<State>` from Decompose
- UI subscribes to state changes with `subscribeAsState()`
- Unidirectional data flow

### Navigation

- Stack-based navigation with `ChildStack`
- Type-safe configurations with `@Serializable`
- Automatic back handling

## Configuration

### Package Name
`com.chatkeep.admin`

### Android
- Min SDK: 24 (Android 7.0)
- Target SDK: 35 (Android 15)

### iOS
- Deployment Target: iOS 15.0+

### Desktop
- JVM Target: 17

## License

This project is a standalone KMP template.
