plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

// ==================== Submodule patches ====================
// The vendored SDL submodule stays pinned to an upstream commit; fixes that
// must not live in the submodule history are kept under patches/ and applied
// here, before any task that configures or compiles the SDL sources, so both
// CI and local builds work from a clean checkout.
val applySubmodulePatches = tasks.register("applySubmodulePatches") {
    group = "build"
    description = "Applies patches/ to the vendored SDL submodule (idempotent)."
    doLast {
        fun applyPatch(patchFile: java.io.File, targetDir: java.io.File) {
            if (!patchFile.isFile) {
                throw GradleException("Patch file not found: ${patchFile.absolutePath}")
            }
            fun gitApply(reverse: Boolean): Pair<Int, String> {
                val cmd = mutableListOf("git", "apply")
                if (reverse) cmd.add("--reverse")
                cmd.add("--check")
                cmd.add(patchFile.absolutePath)
                val proc = ProcessBuilder(cmd)
                    .directory(targetDir)
                    .redirectErrorStream(true)
                    .start()
                val out = proc.inputStream.bufferedReader().readText()
                return proc.waitFor() to out
            }
            // Already applied -> reverse check succeeds; leave it alone.
            val (reverseExit, _) = gitApply(reverse = true)
            if (reverseExit == 0) {
                logger.info("Patch ${patchFile.name} already applied to ${targetDir.name}; skipping.")
                return
            }
            val proc = ProcessBuilder("git", "apply", patchFile.absolutePath)
                .directory(targetDir)
                .redirectErrorStream(true)
                .start()
            val out = proc.inputStream.bufferedReader().readText()
            if (proc.waitFor() != 0) {
                throw GradleException(
                    "Failed to apply ${patchFile.name} to ${targetDir.absolutePath}: $out",
                )
            }
            logger.lifecycle("Applied ${patchFile.name} to ${targetDir.name}.")
        }

        applyPatch(
            rootProject.file("patches/SDL.patch"),
            rootProject.file("SDL"),
        )
    }
}

// Any task that configures or compiles the vendored SDL sources (native
// static libs, per-OS JNI, Android JNI, wasm) needs the patches applied
// first.
allprojects {
    tasks.configureEach {
        if (
            name.startsWith("buildNative_") ||
            name.startsWith("configureNative_") ||
            name.startsWith("buildJniLib_") ||
            name.startsWith("configureJniLib_") ||
            name == "buildJniLibrary" ||
            name == "configureJniLibrary" ||
            name == "configureWasmSdl" ||
            name == "buildWasmSdl" ||
            name == "linkWasmSdl"
        ) {
            dependsOn(rootProject.tasks.named("applySubmodulePatches"))
        }
    }
}

allprojects {
    group = "cn.enaium.sdl"
    version = "1.0.10"
}
