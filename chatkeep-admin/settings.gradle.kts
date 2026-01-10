pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "chatkeep-admin"

// Core modules
include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:network")
include(":core:ui")

// Feature modules
include(":feature:home:api")
include(":feature:home:impl")
include(":feature:auth:api")
include(":feature:auth:impl")
include(":feature:dashboard:api")
include(":feature:dashboard:impl")
include(":feature:chats:api")
include(":feature:chats:impl")
include(":feature:deploy:api")
include(":feature:deploy:impl")
include(":feature:logs:api")
include(":feature:logs:impl")
include(":feature:settings:api")
include(":feature:settings:impl")
include(":feature:main:api")
include(":feature:main:impl")

// App entry points
include(":composeApp")
