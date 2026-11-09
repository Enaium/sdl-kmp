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
// The Emscripten module and the JS glue are consumed as the
// @enaium/sdl-kmp npm package: this module's package.json points the
// dependency at the package staged by :sdl-kmp:assembleWasmNpm, so the build
// works without registry access (an app using the published package depends
// on "^3.4.18" instead of the file: path).
//
// The wasmJs webpack distribution requires npm, which is not always
// available, so assemble a self-contained static app directory instead:
//   build/browserApp/
//     index.html                          (entry page; import map -> package)
//     node_modules/@enaium/sdl-kmp/  (installed package: glue + SDL3 wasm)
//     browser-node-test.mjs               (headless Node runner, bare import)
//     *.mjs/*.wasm                        (the Kotlin/Wasm executable output)
// Serve it with any static file server (e.g. `python3 -m http.server`).

val sdlKmpProject = project(":sdl-kmp")
val kotlinWasmOutput = layout.buildDirectory.dir("compileSync/wasmJs/main/developmentExecutable/kotlin").get().asFile
val installedWasmPackage = layout.projectDirectory.dir("node_modules/@enaium/sdl-kmp")

val npmInstall = tasks.register<Exec>("npmInstall") {
    description = "Installs the example's npm dependencies (@enaium/sdl-kmp)."
    dependsOn(sdlKmpProject.tasks.named("assembleWasmNpm"))
    workingDir = layout.projectDirectory.asFile
    commandLine("npm", "install", "--no-audit", "--no-fund")
    inputs.file(layout.projectDirectory.file("package.json"))
    outputs.dir(layout.projectDirectory.dir("node_modules"))
}

val copyWebAssets = tasks.register<Copy>("copyWebAssets") {
    dependsOn("compileDevelopmentExecutableKotlinWasmJs")
    dependsOn(npmInstall)
    from("src/wasmJsMain/resources/web") {
        include("**")
    }
    from(layout.projectDirectory) {
        include("browser-node-test.mjs")
    }
    // npm installs file: dependencies as a symlink; copy the real package
    // contents so the static app resolves it through the import map (browser)
    // and through node_modules (Node).
    from(provider { installedWasmPackage.asFile.canonicalFile }) {
        into("node_modules/@enaium/sdl-kmp")
    }
    from(kotlinWasmOutput) {
        include("**")
    }
    into(layout.buildDirectory.dir("browserApp"))
}
