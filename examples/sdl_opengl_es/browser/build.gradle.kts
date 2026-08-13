import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

// Browser (wasmJs) runner for the sdl_opengl_es demo (GLES 3.0 -> WebGL2).
// SDL3 is compiled to a standalone Emscripten module (sdl_wasm.js, built by
// :sdl-kmp:linkWasmSdl) that this module loads before the Kotlin code runs.
kotlin {
    wasmJs {
        binaries.executable()
        browser()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":sdl-kmp"))
                implementation(project(":examples:sdl_opengl_es"))
            }
        }
    }
}

// ==================== web assets ====================
// Assemble a self-contained static app directory (no npm/webpack required):
//   build/browserApp/  index.html + sdl_kmp_glue.js + sdl_wasm.js/.wasm + the
//   Kotlin/Wasm executable output. Serve it with any static file server.

val sdlKmpProject = project(":sdl-kmp")
val sdlKmpLinkTask = sdlKmpProject.tasks.named("linkWasmSdl")
val wasmSdlOutput = sdlKmpProject.layout.buildDirectory.dir("wasmSdl").get().asFile
val kotlinWasmOutput = layout.buildDirectory.dir("compileSync/wasmJs/main/developmentExecutable/kotlin").get().asFile

val copyWebAssets = tasks.register<Copy>("copyWebAssets") {
    dependsOn(sdlKmpLinkTask)
    dependsOn("compileDevelopmentExecutableKotlinWasmJs")
    from("src/wasmJsMain/resources/web") {
        include("**")
    }
    from(sdlKmpProject.layout.projectDirectory.dir("wasm")) {
        include("sdl_kmp_glue.js")
    }
    from(wasmSdlOutput) {
        include("sdl_wasm.js", "sdl_wasm.wasm")
    }
    from(kotlinWasmOutput) {
        include("**")
    }
    into(layout.buildDirectory.dir("browserApp"))
}
