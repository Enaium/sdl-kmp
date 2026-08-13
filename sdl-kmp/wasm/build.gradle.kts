/*
 * Copyright (c) 2026 Enaium
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/*
 * Standalone distribution of the Emscripten-compiled SDL3 module used by the
 * sdl-kmp wasmJs target.
 *
 * Kotlin/Wasm has no cinterop and does not merge library resources into the
 * browser output, so the compiled SDL3 (sdl_wasm.js + sdl_wasm.wasm) and the
 * JS glue (sdl_kmp_glue.js) cannot ride inside the wasmJs klib. Instead they
 * are published here as a plain jar (cn.enaium.sdl:sdl-kmp-wasm-assets); web
 * apps unpack it into their static root so the host page can `import` the
 * glue before the Kotlin module runs.
 *
 * The jar is built from :sdl-kmp:linkWasmSdl, which compiles SDL3 with
 * Emscripten (set the EMSDK env var or pass -Pwasm.emsdk=<emsdk root>).
 */

plugins {
    java
    alias(libs.plugins.maven.publish)
}

val sdlKmpProject = project(":sdl-kmp")
val wasmGlue = layout.projectDirectory.file("sdl_kmp_glue.js")
val wasmSdlOutput = sdlKmpProject.layout.buildDirectory.dir("wasmSdl")

tasks.jar {
    archiveBaseName.set("sdl-kmp-wasm-assets")
    dependsOn(sdlKmpProject.tasks.named("linkWasmSdl"))
    from(wasmGlue)
    from(wasmSdlOutput) {
        include("sdl_wasm.js", "sdl_wasm.wasm")
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "sdl-kmp-wasm-assets",
        // null -> the plugin falls back to project.version
        version = null,
    )

    pom {
        name.set("sdl-kmp wasm assets")
        description.set(
            "Emscripten-compiled SDL3 module (sdl_wasm.js, sdl_wasm.wasm) and the JS " +
                    "glue (sdl_kmp_glue.js) for the sdl-kmp wasmJs target. Unpack the jar " +
                    "into your web root next to the Kotlin/Wasm output and load " +
                    "sdl_kmp_glue.js from the host page before the Kotlin module runs.",
        )
        url.set("https://github.com/Enaium/sdl-kmp")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("Zlib")
                url.set("https://opensource.org/license/zlib")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("Enaium")
            }
        }

        scm {
            url.set("https://github.com/Enaium/sdl-kmp")
            connection.set("scm:git:git@github.com:Enaium/sdl-kmp.git")
            developerConnection.set("scm:git:git@github.com:Enaium/sdl-kmp.git")
        }
    }
}
