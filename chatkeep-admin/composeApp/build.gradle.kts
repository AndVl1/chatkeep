import java.util.Base64

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
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
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
            export(projects.feature.chatdetails.api)
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
            api(projects.feature.chatdetails.api)
            implementation(projects.feature.chatdetails.impl)
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

            // Feature modules as API for iOS framework export
            api(projects.feature.auth.api)
            api(projects.feature.dashboard.api)
            api(projects.feature.chats.api)
            api(projects.feature.chatdetails.api)
            api(projects.feature.deploy.api)
            api(projects.feature.logs.api)
            api(projects.feature.settings.api)
            api(projects.feature.main.api)
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

    // Generate versionCode from git commit count
    val gitCommitCount = providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
    }.standardOutput.asText.get().trim().toIntOrNull() ?: 1

    defaultConfig {
        applicationId = "com.chatkeep.admin"
        minSdk = 24
        targetSdk = 35
        versionCode = gitCommitCount
        versionName = "1.0.0"
    }

    // Configure keystore from environment variables for release signing
    // Supports both ANDROID_KEYSTORE_PATH (file path) and ANDROID_KEYSTORE_BASE_64 (base64 encoded)
    val releaseKeystoreConfigured = run {
        val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
        val keystoreBase64 = System.getenv("ANDROID_KEYSTORE_BASE_64")
        val keystorePass = System.getenv("ANDROID_KEYSTORE_PASS")
        val keyAlias = System.getenv("ANDROID_KEYSTORE_ALIAS")
        val keyPass = System.getenv("ANDROID_KEYSTORE_KEY_PASS")

        keystorePass != null && keyAlias != null && keyPass != null &&
            (keystorePath != null || keystoreBase64 != null)
    }

    signingConfigs {
        if (releaseKeystoreConfigured) {
            create("release") {
                val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
                val keystoreBase64 = System.getenv("ANDROID_KEYSTORE_BASE_64")
                val keystorePass = System.getenv("ANDROID_KEYSTORE_PASS")!!
                val keyAliasEnv = System.getenv("ANDROID_KEYSTORE_ALIAS")!!
                val keyPassEnv = System.getenv("ANDROID_KEYSTORE_KEY_PASS")!!

                // Prefer file path, fallback to base64 decoding
                storeFile = if (keystorePath != null) {
                    File(keystorePath)
                } else {
                    val keystoreFile = File.createTempFile("keystore", ".jks")
                    keystoreFile.deleteOnExit()
                    keystoreFile.writeBytes(Base64.getDecoder().decode(keystoreBase64))
                    keystoreFile
                }
                storePassword = keystorePass
                this.keyAlias = keyAliasEnv
                keyPassword = keyPassEnv
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("production") {
            dimension = "environment"
            versionNameSuffix = "-prod.${gitCommitCount}"
        }

        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test.${gitCommitCount}"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Use release signing if configured, otherwise fall back to debug signing
            signingConfig = if (releaseKeystoreConfigured) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            // Debug uses default signing
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
