pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "sdl-kmp"

include(":sdl-kmp")

include(":examples:sdl_renderer")
include(":examples:sdl_renderer:android")
include(":examples:sdl_vulkan")
include(":examples:sdl_opengl")
include(":examples:sdl_gpu")
include(":examples:sdl_gpu:android")
