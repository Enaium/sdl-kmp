import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
}

group = rootProject.group
version = rootProject.version

val sdlDir = rootProject.projectDir.resolve("SDL")

val hostOs = OperatingSystem.current()
val hostArch = System.getProperty("os.arch").lowercase()

// Apple targets build on macOS via Xcode; linuxX64 is built on Linux hosts;
// mingwX64 is cross-compiled on Linux hosts with the x86_64-w64-mingw32
// toolchain (Windows hosts default to MSVC, whose archives are incompatible
// with Kotlin/Native's MinGW linker).
fun hasMingwCrossToolchain(): Boolean {
    val name = "x86_64-w64-mingw32-gcc"
    return System.getenv("PATH")?.split(File.pathSeparator).orEmpty().any { dir ->
        val f = File(dir, name)
        f.isFile && f.canExecute()
    }
}

// Reads sdk.dir from local.properties (the standard place Gradle's Android
// plugins put the SDK path, e.g. /Users/<user>/Library/Android/sdk).
fun localSdkDir(): String? {
    val f = rootProject.file("local.properties")
    if (!f.isFile) return null
    return f.readLines()
        .firstOrNull { it.trimStart().startsWith("sdk.dir=") }
        ?.substringAfter('=')
        ?.trim()
}

// Locates an installed Android NDK, preferring the highest version under
// $ANDROID_HOME (or $ANDROID_SDK_ROOT, or local.properties' sdk.dir, or
// ~/Android/Sdk). androidNative targets cross-compile the SDL3 static library
// with this toolchain.
fun androidNdkPath(): String? {
    val home = System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: localSdkDir()
        ?: System.getProperty("user.home") + "/Android/Sdk"
    val ndkDir = File(home, "ndk")
    if (!ndkDir.isDirectory) return null
    return ndkDir.listFiles()
        ?.filter { it.isDirectory && it.name.matches(Regex("\\d+\\.\\d+\\.\\d+.*")) }
        ?.sortedBy { it.name }
        ?.lastOrNull()
        ?.absolutePath
}

fun canBuildNativeTarget(targetName: String): Boolean {
    return when {
        hostOs.isMacOsX && (
                targetName.startsWith("macos") ||
                        targetName.startsWith("ios") ||
                        targetName.startsWith("tvos")
                ) -> true

        hostOs.isLinux && targetName == "linuxX64" -> true
        hostOs.isLinux && targetName == "mingwX64" && hasMingwCrossToolchain() -> true
        targetName.startsWith("androidNative") && androidNdkPath() != null -> true
        else -> false
    }
}

fun resolveCmakeExecutable(): String {
    val exeName = if (OperatingSystem.current().isWindows) "cmake.exe" else "cmake"

    System.getenv("PATH")?.split(File.pathSeparator).orEmpty().forEach { dir ->
        val candidate = File(dir, exeName)
        if (candidate.isFile && candidate.canExecute()) return candidate.absolutePath
    }

    val extraPaths = listOf(
        "/opt/homebrew/bin",
        "/usr/local/bin",
        "/usr/bin",
        "/opt/local/bin",
    )
    extraPaths.forEach { dir ->
        val candidate = File(dir, exeName)
        if (candidate.isFile && candidate.canExecute()) return candidate.absolutePath
    }

    return exeName
}

val cmakeExecutable: String by lazy { resolveCmakeExecutable() }

// Minimum deployment targets. These match Kotlin/Native 2.4 defaults
// (macos 12.0, ios/tvos 15.0) so the SDL3 static library objects never
// exceed the final binary's minimum version.
val appleDeploymentTargets = mapOf(
    "macos" to "12.0",
    "ios" to "15.0",
    "tvos" to "15.0",
)

fun deploymentTargetFor(targetName: String): String {
    val prefix = listOf("macos", "ios", "tvos").first { targetName.startsWith(it) }
    return appleDeploymentTargets.getValue(prefix)
}

kotlin {
    // ==================== JVM ====================
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    // ==================== Native ====================
    macosArm64()
    macosX64()

    linuxX64()
    androidNativeArm64()
    androidNativeArm32()
    androidNativeX64()
    androidNativeX86()

    mingwX64()

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    tvosArm64()
    tvosSimulatorArm64()

    // ==================== cinterop for all native targets ====================
    targets.withType<KotlinNativeTarget> {
        val targetName = this.name
        val canBuild = canBuildNativeTarget(targetName)

        compilations.getByName("main") {
            cinterops {
                create("sdl") {
                    defFile(project.file("src/nativeInterop/cinterop/sdl.def"))
                    includeDirs(
                        project.file("src/nativeInterop/cinterop"),
                        rootProject.file("SDL/include"),
                    )
                    // crc32 intrinsics from the MinGW sysroot's <intrin.h>
                    // (and SDL headers) require SSE4.2 on x86_64.
                    if (targetName.endsWith("X64")) {
                        compilerOpts("-msse4.2")
                    }
                    if (canBuild) {
                        // Embed the per-target static library into the produced
                        // cinterop klib. Targets that can't be built on this host
                        // still get bindings (for klib publishing); the static
                        // library is built and embedded when building on the
                        // matching host.
                        val outputDir = layout.buildDirectory.dir("native/$targetName").get().asFile
                        extraOpts(
                            "-libraryPath", outputDir.absolutePath,
                            "-staticLibrary", "libSDL3.a",
                        )
                    }
                }
            }
            defaultSourceSet.kotlin.srcDir("src/nativeMain/kotlin")
        }
    }

    // ==================== Source sets ====================
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        jvmMain {
            dependencies {
                // SDL3 on the JVM comes from LWJGL 3.4.x (org.lwjgl:lwjgl-sdl).
                // LWJGL does not support Android, so there is no Android target.
                implementation(libs.lwjgl)
                implementation(libs.lwjgl.sdl)
                // LWJGL's public API is annotated with jspecify; make the
                // annotation classes available to the compiler.
                implementation("org.jspecify:jspecify:1.0.0")
                // VkInstance/VkPhysicalDevice handles used by SDL_Vulkan_GetPresentationSupport
                implementation("org.lwjgl:lwjgl-vulkan:${libs.versions.lwjgl.get()}")
                runtimeOnly("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}") {
                    artifact {
                        classifier = "natives-linux"
                    }
                }
                runtimeOnly("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}") {
                    artifact {
                        classifier = "natives-macos"
                    }
                }
                runtimeOnly("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}") {
                    artifact {
                        classifier = "natives-macos-arm64"
                    }
                }
                runtimeOnly("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}") {
                    artifact {
                        classifier = "natives-windows"
                    }
                }
                runtimeOnly("org.lwjgl:lwjgl-sdl:${libs.versions.lwjgl.get()}") {
                    artifact {
                        classifier = "natives-linux"
                    }
                }
                runtimeOnly("org.lwjgl:lwjgl-sdl:${libs.versions.lwjgl.get()}") {
                    artifact {
                        classifier = "natives-macos"
                    }
                }
                runtimeOnly("org.lwjgl:lwjgl-sdl:${libs.versions.lwjgl.get()}") {
                    artifact {
                        classifier = "natives-macos-arm64"
                    }
                }
                runtimeOnly("org.lwjgl:lwjgl-sdl:${libs.versions.lwjgl.get()}") {
                    artifact {
                        classifier = "natives-windows"
                    }
                }
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.junit.jupiter)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }
}

// ==================== Native: build static SDL3 library for each target ====================
val nativeDir = projectDir.resolve("native")

fun registerNativeBuildTasks(targetName: String, cmakeFlags: List<String> = emptyList()) {
    val outputDir = layout.buildDirectory.dir("native/$targetName").get().asFile
    val cmakeBuildDir = layout.buildDirectory.dir("cmake-$targetName").get().asFile

    val commonFlags = listOf(
        cmakeExecutable, nativeDir.absolutePath,
        "-DCMAKE_BUILD_TYPE=Release",
        "-DSDL_SOURCE_DIR=${sdlDir.absolutePath}",
        "-DSDL3_KMP_OUTPUT_DIR=${outputDir.absolutePath}",
    )

    val configureTask = tasks.register<Exec>("configureNative_$targetName") {
        onlyIf { canBuildNativeTarget(targetName) }
        doFirst {
            cmakeBuildDir.mkdirs()
            outputDir.mkdirs()
        }
        workingDir = cmakeBuildDir
        commandLine(commonFlags + cmakeFlags)
    }

    val buildTask = tasks.register<Exec>("buildNative_$targetName") {
        onlyIf { canBuildNativeTarget(targetName) }
        dependsOn(configureTask)
        workingDir = cmakeBuildDir
        commandLine(cmakeExecutable, "--build", ".", "--config", "Release")
    }

    tasks.matching {
        it.name.startsWith("cinteropSdl") &&
                it.name.endsWith(targetName.replaceFirstChar { c -> c.uppercase() })
    }.configureEach {
        dependsOn(buildTask)
        // The cinterop task's custom up-to-date check only watches headers and
        // the .def file; register the static library as an input so a rebuild
        // of SDL3 re-embeds it (otherwise stale archives would leak into the
        // published klib on incremental builds).
        inputs.file(outputDir.resolve("libSDL3.a"))
    }
}

if (hostOs.isMacOsX) {
    registerNativeBuildTasks(
        "macosArm64",
        listOf(
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_SYSTEM_PROCESSOR=arm64",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTargetFor("macosArm64")}",
        ),
    )
    registerNativeBuildTasks(
        "macosX64",
        listOf(
            "-DCMAKE_OSX_ARCHITECTURES=x86_64",
            "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTargetFor("macosX64")}",
        ),
    )
    registerNativeBuildTasks(
        "iosArm64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=iOS",
            "-DCMAKE_SYSTEM_PROCESSOR=arm64",
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_OSX_SYSROOT=iphoneos",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTargetFor("iosArm64")}",
        ),
    )
    registerNativeBuildTasks(
        "iosX64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=iOS",
            "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
            "-DCMAKE_OSX_ARCHITECTURES=x86_64",
            "-DCMAKE_OSX_SYSROOT=iphonesimulator",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTargetFor("iosX64")}",
        ),
    )
    registerNativeBuildTasks(
        "iosSimulatorArm64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=iOS",
            "-DCMAKE_SYSTEM_PROCESSOR=arm64",
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_OSX_SYSROOT=iphonesimulator",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTargetFor("iosSimulatorArm64")}",
        ),
    )
    registerNativeBuildTasks(
        "tvosArm64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=tvOS",
            "-DCMAKE_SYSTEM_PROCESSOR=arm64",
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_OSX_SYSROOT=appletvos",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTargetFor("tvosArm64")}",
        ),
    )
    registerNativeBuildTasks(
        "tvosSimulatorArm64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=tvOS",
            "-DCMAKE_SYSTEM_PROCESSOR=arm64",
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_OSX_SYSROOT=appletvsimulator",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTargetFor("tvosSimulatorArm64")}",
        ),
    )
} else if (hostOs.isLinux) {
    registerNativeBuildTasks("linuxX64")
    // Cross-compile the MinGW static library with the
    // x86_64-w64-mingw32 toolchain (canBuildNativeTarget gates on it).
    registerNativeBuildTasks(
        "mingwX64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=Windows",
            "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
            "-DCMAKE_C_COMPILER=x86_64-w64-mingw32-gcc",
            "-DCMAKE_CXX_COMPILER=x86_64-w64-mingw32-g++",
        ),
    )
}

// Android native targets cross-compile SDL3 with the NDK (see
// canBuildNativeTarget); the NDK can be used from any host OS.
androidNdkPath()?.let { ndk ->
    val toolchain = "$ndk/build/cmake/android.toolchain.cmake"
    val androidFlags = { abi: String, platform: String ->
        listOf(
            "-DCMAKE_TOOLCHAIN_FILE=$toolchain",
            "-DANDROID_ABI=$abi",
            "-DANDROID_PLATFORM=$platform",
            "-DANDROID_STL=c++_static",
        )
    }
    registerNativeBuildTasks("androidNativeArm64", androidFlags("arm64-v8a", "android-24"))
    registerNativeBuildTasks("androidNativeArm32", androidFlags("armeabi-v7a", "android-24"))
    registerNativeBuildTasks("androidNativeX64", androidFlags("x86_64", "android-24"))
    registerNativeBuildTasks("androidNativeX86", androidFlags("x86", "android-24"))
}

// ==================== Publishing ====================
mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "sdl-kmp",
        // null -> the plugin falls back to project.version
        version = null,
    )

    pom {
        name.set("sdl-kmp")
        description.set(
            "Kotlin Multiplatform bindings for SDL3. " +
                    "JVM uses the official LWJGL bindings; native targets embed the statically " +
                    "compiled SDL3 library into the published klib.",
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

        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/Enaium/sdl-kmp/issues")
        }
    }
}
