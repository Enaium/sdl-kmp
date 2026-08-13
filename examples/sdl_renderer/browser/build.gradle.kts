import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

// Browser (wasmJs) runner for the sdl_renderer demo. SDL3 is compiled to a
// standalone Emscripten module (sdl_wasm.js, built by :sdl-kmp:linkWasmSdl)
// that this module loads before the Kotlin code runs; the wasmJs actuals in
// sdl-kmp drive it through the JS glue (sdl-kmp/wasm/sdl_kmp_glue.js).
kotlin {
    wasmJs {
        binaries.executable()
        browser()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":sdl-kmp"))
                implementation(project(":examples:sdl_renderer"))
            }
        }
    }
}

// ==================== web assets ====================
// The wasmJs webpack distribution requires npm, which is not always
// available, so assemble a self-contained static app directory instead:
//   build/browserApp/
//     index.html            (entry page)
//     sdl_kmp_glue.js       (JS bridge that instantiates the SDL3 module)
//     sdl_wasm.js/.wasm     (Emscripten-compiled SDL3, from :sdl-kmp:linkWasmSdl)
//     *.mjs/*.wasm          (the Kotlin/Wasm executable output)
// Serve it with any static file server (e.g. `python3 -m http.server`).

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
    from(layout.projectDirectory) {
        include("browser-node-test.mjs")
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
