import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

fun resolveCmakeExecutable(): String {
    val exeName = if (OperatingSystem.current().isWindows) "cmake.exe" else "cmake"
    System.getenv("PATH")?.split(File.pathSeparator).orEmpty().forEach { dir ->
        val candidate = File(dir, exeName)
        if (candidate.isFile && candidate.canExecute()) return candidate.absolutePath
    }
    val extraPaths = listOf("/opt/homebrew/bin", "/usr/local/bin", "/usr/bin", "/opt/local/bin")
    extraPaths.forEach { dir ->
        val candidate = File(dir, exeName)
        if (candidate.isFile && candidate.canExecute()) return candidate.absolutePath
    }
    return exeName
}

val cmakeExecutable: String by lazy { resolveCmakeExecutable() }
val hostOs = OperatingSystem.current()

// The demo C code is compiled per native target into a static library and
// embedded into the cinterop klib, mirroring how sdl-kmp embeds libSDL3.a.
// Apple targets build on macOS; linuxX64 on Linux hosts; mingwX64 is
// cross-compiled on Linux hosts with the x86_64-w64-mingw32 toolchain.
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

// Locates an installed Android NDK (the C helper is cross-compiled for the
// androidNative targets with this toolchain).
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

// Kotlin/Native's own Android toolchain sysroot (api 26) ships the NDK stub
// libraries (libEGL, libGLESv2, ...) that the GLES demo references at link
// time. Point -L at the per-ABI directory so libmain.so links without an
// extra NDK install.
fun konanAndroidLibDir(abi: String): String? {
    val konanData = System.getenv("KONAN_DATA_DIR")
        ?: providers.gradleProperty("konan.data.dir").getOrElse("${System.getProperty("user.home")}/.konan")
    val toolchain = File(konanData, "dependencies").listFiles()
        ?.firstOrNull { it.isDirectory && it.name.matches(Regex("target-toolchain-.*-android_ndk")) }
        ?: return null
    val triple = when (abi) {
        "arm64-v8a" -> "aarch64-linux-android"
        "armeabi-v7a" -> "arm-linux-androideabi"
        "x86_64" -> "x86_64-linux-android"
        "x86" -> "i686-linux-android"
        else -> return null
    }
    return "$toolchain/sysroot/usr/lib/$triple/26"
}

fun canBuildNativeTarget(targetName: String): Boolean {
    return when {
        hostOs.isMacOsX && targetName == "macosArm64" -> true
        hostOs.isLinux && targetName == "linuxX64" -> true
        hostOs.isLinux && targetName == "mingwX64" && hasMingwCrossToolchain() -> true
        targetName.startsWith("androidNative") && androidNdkPath() != null -> true
        else -> false
    }
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    macosArm64 {
        binaries.executable()
    }

    linuxX64 {
        binaries.executable()
    }

    mingwX64 {
        binaries.executable()
    }

    // OpenGL ES is the browser-capable GL profile (WebGL2), so the module also
    // targets wasmJs; the runnable page lives in the `browser` submodule.
    wasmJs()

    // Android native targets build libmain.so with an exported SDL_main entry
    // point; SDLActivity (from the SDL3 AAR) loads and calls it. The SDL3
    // static library is linked in from the sdl-kmp klib; the Kotlin/Native
    // android sysroot provides the NDK system libraries it references.
    androidNativeArm64 {
        binaries.sharedLib("main") {
            konanAndroidLibDir("arm64-v8a")?.let { linkerOpts("-L$it") }
            linkerOpts("-Wl,--allow-multiple-definition")
        }
    }
    androidNativeArm32 {
        binaries.sharedLib("main") {
            konanAndroidLibDir("armeabi-v7a")?.let { linkerOpts("-L$it") }
            linkerOpts("-Wl,--allow-multiple-definition")
        }
    }
    androidNativeX64 {
        binaries.sharedLib("main") {
            konanAndroidLibDir("x86_64")?.let { linkerOpts("-L$it") }
            linkerOpts("-Wl,--allow-multiple-definition")
        }
    }
    androidNativeX86 {
        binaries.sharedLib("main") {
            konanAndroidLibDir("x86")?.let { linkerOpts("-L$it") }
            linkerOpts("-Wl,--allow-multiple-definition")
        }
    }

    sourceSets {
        val nativeMain = create("nativeMain") {
            dependsOn(getByName("commonMain"))
        }
        macosArm64Main {
            dependsOn(nativeMain)
        }
        linuxX64Main {
            dependsOn(nativeMain)
        }
        mingwX64Main {
            dependsOn(nativeMain)
        }

        // Reuse the native GLES renderer (cinterop + C helper) on Android.
        val androidMain = create("androidMain") {
            dependsOn(nativeMain)
        }
        androidNativeArm64Main {
            dependsOn(androidMain)
        }
        androidNativeArm32Main {
            dependsOn(androidMain)
        }
        androidNativeX64Main {
            dependsOn(androidMain)
        }
        androidNativeX86Main {
            dependsOn(androidMain)
        }
    }

    // cinterop bindings for the GLES demo C helper. The static library is
    // built by the CMake tasks registered below and embedded into the
    // produced klib, so final binaries link the demo code.
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        val targetName = this.name
        compilations.getByName("main") {
            cinterops.create("gles") {
                defFile(project.file("src/nativeMain/cinterop/gles.def"))
                includeDirs(
                    project.file("src/nativeMain/cinterop"),
                    rootProject.file("SDL/include"),
                )
                extraOpts(
                    "-libraryPath",
                    layout.buildDirectory.dir("native/$targetName").get().asFile.absolutePath,
                    "-staticLibrary",
                    "libsdl_kmp_gles.a",
                )
            }
        }
    }

    sourceSets {
        jvm {
            mainRun {
                mainClass = "cn.enaium.sdl.example.opengles.Main_jvmKt"
            }
        }

        commonMain {
            dependencies {
                implementation(project(":sdl-kmp"))
            }
        }

        jvmMain {
            dependencies {
                val lwjglVersion = libs.versions.lwjgl.get()
                implementation("org.lwjgl:lwjgl-opengles:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
                listOf("linux", "macos", "macos-arm64", "windows").forEach { classifier ->
                    runtimeOnly("org.lwjgl:lwjgl-opengles:$lwjglVersion") {
                        artifact { this.classifier = "natives-$classifier" }
                    }
                    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion") {
                        artifact { this.classifier = "natives-$classifier" }
                    }
                }
            }
        }

        wasmJsMain {
            dependsOn(getByName("commonMain"))
            dependencies {
                implementation(project(":sdl-kmp"))
            }
        }
    }
}

tasks.withType(JavaExec::class.java).configureEach {
    if (OperatingSystem.current().isMacOsX && name == "jvmRun") {
        jvmArgs("--enable-native-access=ALL-UNNAMED", "-XstartOnFirstThread")
    }
}

// ==================== Native: build the demo static library per target ====================
val nativeDir = projectDir.resolve("native")
val sdlDir = rootProject.file("SDL")

fun registerNativeBuildTasks(targetName: String, cmakeFlags: List<String> = emptyList()) {
    val outputDir = layout.buildDirectory.dir("native/$targetName").get().asFile
    val cmakeBuildDir = layout.buildDirectory.dir("cmake-opengles-$targetName").get().asFile

    val commonFlags = listOf(
        cmakeExecutable, nativeDir.absolutePath,
        "-DCMAKE_BUILD_TYPE=Release",
        "-DSDL_SOURCE_DIR=${sdlDir.absolutePath}",
        "-DSDL3_KMP_OUTPUT_DIR=${outputDir.absolutePath}",
    )

    val configureTask = tasks.register<Exec>("configureGles_$targetName") {
        onlyIf { canBuildNativeTarget(targetName) }
        doFirst {
            cmakeBuildDir.mkdirs()
            outputDir.mkdirs()
        }
        workingDir = cmakeBuildDir
        commandLine(commonFlags + cmakeFlags)
    }

    val buildTask = tasks.register<Exec>("buildGles_$targetName") {
        onlyIf { canBuildNativeTarget(targetName) }
        dependsOn(configureTask)
        workingDir = cmakeBuildDir
        commandLine(cmakeExecutable, "--build", ".", "--config", "Release")
    }

    tasks.matching {
        it.name.startsWith("cinteropGles") &&
                it.name.endsWith(targetName.replaceFirstChar { c -> c.uppercase() })
    }.configureEach {
        dependsOn(buildTask)
        inputs.file(outputDir.resolve("libsdl_kmp_gles.a"))
    }
}

if (hostOs.isMacOsX) {
    registerNativeBuildTasks(
        "macosArm64",
        listOf(
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_SYSTEM_PROCESSOR=arm64",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=12.0",
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

// The androidNative targets cross-compile the C helper with the Android NDK.
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
