import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

// Same Android sysroot helper as the sibling `example` module.
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

// Locates an installed Android NDK (same logic as the sdl-kmp module).
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
val ndkPath: String? = androidNdkPath()

// The demo C code is compiled per native target into a static library and
// embedded into the cinterop klib, mirroring how sdl-kmp embeds libSDL3.a.
fun canBuildNativeTarget(targetName: String): Boolean {
    return (hostOs.isMacOsX && targetName.startsWith("macos")) ||
        (targetName.startsWith("androidNative") && ndkPath != null)
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

        val androidMain = create("androidMain") {
            // The GL/Vulkan triangle renderers are pure C-helper callers and
            // work unchanged on Android, so reuse the nativeMain actual.
            dependsOn(nativeMain)
        }
        androidNativeArm64Main { dependsOn(androidMain) }
        androidNativeArm32Main { dependsOn(androidMain) }
        androidNativeX64Main { dependsOn(androidMain) }
        androidNativeX86Main { dependsOn(androidMain) }
    }

    // cinterop bindings for the triangle demo C helpers. The static library
    // is built by the CMake tasks registered below and embedded into the
    // produced klib, so final binaries link the demo code.
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        val targetName = this.name
        compilations.getByName("main") {
            cinterops.create("triangle") {
                defFile(project.file("src/nativeMain/cinterop/triangle.def"))
                includeDirs(
                    project.file("src/nativeMain/cinterop"),
                    rootProject.file("SDL/include"),
                )
                extraOpts(
                    "-libraryPath", layout.buildDirectory.dir("native/$targetName").get().asFile.absolutePath,
                    "-staticLibrary", "libsdl_kmp_triangle.a",
                )
            }
        }
    }

    sourceSets {
        jvm {
            mainRun {
                mainClass = "cn.enaium.sdl.example.gpu.Main_jvmKt"
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
                implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-vulkan:$lwjglVersion")
                // LWJGL only ships macOS natives for Vulkan (Linux/Windows use
                // the system Vulkan driver), while OpenGL ships all platforms.
                listOf("linux", "macos", "macos-arm64", "windows").forEach { classifier ->
                    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion") {
                        artifact { this.classifier = "natives-$classifier" }
                    }
                }
                listOf("macos", "macos-arm64").forEach { classifier ->
                    runtimeOnly("org.lwjgl:lwjgl-vulkan:$lwjglVersion") {
                        artifact { this.classifier = "natives-$classifier" }
                    }
                }
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
    val cmakeBuildDir = layout.buildDirectory.dir("cmake-triangle-$targetName").get().asFile

    val commonFlags = listOf(
        cmakeExecutable, nativeDir.absolutePath,
        "-DCMAKE_BUILD_TYPE=Release",
        "-DSDL_SOURCE_DIR=${sdlDir.absolutePath}",
        "-DSDL3_KMP_OUTPUT_DIR=${outputDir.absolutePath}",
    )

    val configureTask = tasks.register<Exec>("configureTriangle_$targetName") {
        onlyIf { canBuildNativeTarget(targetName) }
        doFirst {
            cmakeBuildDir.mkdirs()
            outputDir.mkdirs()
        }
        workingDir = cmakeBuildDir
        commandLine(commonFlags + cmakeFlags)
    }

    val buildTask = tasks.register<Exec>("buildTriangle_$targetName") {
        onlyIf { canBuildNativeTarget(targetName) }
        dependsOn(configureTask)
        workingDir = cmakeBuildDir
        commandLine(cmakeExecutable, "--build", ".", "--config", "Release")
    }

    tasks.matching {
        it.name.startsWith("cinteropTriangle") &&
            it.name.endsWith(targetName.replaceFirstChar { c -> c.uppercase() })
    }.configureEach {
        dependsOn(buildTask)
        inputs.file(outputDir.resolve("libsdl_kmp_triangle.a"))
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
}

ndkPath?.let { ndk ->
    val toolchain = "$ndk/build/cmake/android.toolchain.cmake"
    val androidFlags = { abi: String ->
        listOf(
            "-DCMAKE_TOOLCHAIN_FILE=$toolchain",
            "-DANDROID_ABI=$abi",
            "-DANDROID_PLATFORM=android-24",
            "-DANDROID_STL=c++_static",
        )
    }
    registerNativeBuildTasks("androidNativeArm64", androidFlags("arm64-v8a"))
    registerNativeBuildTasks("androidNativeArm32", androidFlags("armeabi-v7a"))
    registerNativeBuildTasks("androidNativeX64", androidFlags("x86_64"))
    registerNativeBuildTasks("androidNativeX86", androidFlags("x86"))
}
