plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    application
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "ru.andvl.chatkeep"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // HTTP client for GitHub API and OpenRouter
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.13.13")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("ru.andvl.chatkeep.changelog.MainKt")
}

tasks.shadowJar {
    archiveFileName.set("changelog-generator.jar")
    manifest {
        attributes["Main-Class"] = "ru.andvl.chatkeep.changelog.MainKt"
    }
}
