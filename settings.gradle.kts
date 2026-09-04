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
include(":sdl-kmp:wasm")

include(":examples:sdl_renderer")
include(":examples:sdl_renderer:android")
include(":examples:sdl_renderer:browser")
include(":examples:sdl_vulkan")
include(":examples:sdl_opengl")
include(":examples:sdl_opengl_es")
include(":examples:sdl_opengl_es:browser")
include(":examples:sdl_opengl_es:android")
include(":examples:sdl_gpu")
include(":examples:sdl_gpu:android")

// Per-OS/arch JNI artifacts that bundle the prebuilt libsdl_jni shared
// library as a classpath resource. NativeLoader extracts the matching one at
// runtime.
listOf(
    "linux-x86_64",
    "linux-aarch64",
    "darwin-x86_64",
    "darwin-aarch64",
    "windows-x86_64",
).forEach { classifier ->
    val name = ":jni-jvm-$classifier"
    include(name)
    project(name).projectDir = file("jni/jvm/$classifier")
}

// Android library packaging the SDL android-project Java layer
// (org.libsdl.app: SDLActivity, SDLSurface, ...) plus the per-ABI
// libsdl_jni.so, so consumers get a ready-to-use SDLActivity without
// copying any SDL Java code.
include(":android-jvm")
project(":android-jvm").projectDir = file("jni/android-jvm")
