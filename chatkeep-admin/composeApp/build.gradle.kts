plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            export(projects.feature.auth.api)
            export(projects.feature.dashboard.api)
            export(projects.feature.chats.api)
            export(projects.feature.deploy.api)
            export(projects.feature.logs.api)
            export(projects.feature.settings.api)
            export(projects.feature.main.api)
        }
    }

    jvm("desktop")

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            // Core modules
            implementation(projects.core.common)
            implementation(projects.core.ui)
            implementation(projects.core.network)

            // Ktor client for AppFactory
            implementation(libs.ktor.client.core)

            // Feature modules (api() for iOS framework export)
            api(projects.feature.auth.api)
            implementation(projects.feature.auth.impl)
            api(projects.feature.dashboard.api)
            implementation(projects.feature.dashboard.impl)
            api(projects.feature.chats.api)
            implementation(projects.feature.chats.impl)
            api(projects.feature.deploy.api)
            implementation(projects.feature.deploy.impl)
            api(projects.feature.logs.api)
            implementation(projects.feature.logs.impl)
            api(projects.feature.settings.api)
            implementation(projects.feature.settings.impl)
            api(projects.feature.main.api)
            implementation(projects.feature.main.impl)

            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)

            // Decompose
            implementation(libs.decompose)
            implementation(libs.decompose.compose)
            implementation(libs.essenty.lifecycle)

            // Serialization
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.cio)

            // Core modules with platform-specific dependencies
            implementation(projects.core.data)

            // DataStore preferences delegate for Android
            implementation("androidx.datastore:datastore-preferences:1.1.1")
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.cio)

                // Core modules with platform-specific dependencies
                implementation(projects.core.data)

                // DataStore for Desktop
                implementation(libs.datastore.preferences.core)
            }
        }

        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
                // Note: core:data not included for WASM (DataStore not supported)
            }
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)

            // Core modules with platform-specific dependencies
            implementation(projects.core.data)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.chatkeep.admin.MainKt"
    }
}

android {
    namespace = "com.chatkeep.admin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chatkeep.admin"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
