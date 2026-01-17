plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
            baseName = "FeatureMainImpl"
            isStatic = true
        }
    }

    jvm("desktop")

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.main.api)
            implementation(projects.feature.dashboard.api)
            implementation(projects.feature.dashboard.impl)
            implementation(projects.feature.chats.api)
            implementation(projects.feature.chats.impl)
            implementation(projects.feature.deploy.api)
            implementation(projects.feature.deploy.impl)
            implementation(projects.feature.logs.api)
            implementation(projects.feature.logs.impl)
            implementation(projects.feature.settings.api)
            implementation(projects.feature.settings.impl)
            implementation(projects.core.ui)
            implementation(projects.core.network)

            implementation(libs.decompose)
            implementation(libs.decompose.compose)
            implementation(libs.kotlinx.coroutines.core)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
        }

        androidMain.dependencies {
            implementation(projects.core.data) // For DataStore
            implementation(libs.datastore.preferences.core)
        }

        iosMain.dependencies {
            implementation(projects.core.data) // For DataStore
            implementation(libs.datastore.preferences.core)
        }

        val desktopMain by getting {
            dependencies {
                implementation(projects.core.data) // For DataStore
                implementation(libs.datastore.preferences.core)
            }
        }

        // wasmJs: No DataStore dependency - main component not used on wasm (only auth)
    }
}

android {
    namespace = "com.chatkeep.admin.feature.main.impl"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
