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
fun canBuildNativeTarget(targetName: String): Boolean {
    return hostOs.isMacOsX && targetName.startsWith("macos")
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

    sourceSets {
        val nativeMain = create("nativeMain") {
            dependsOn(getByName("commonMain"))
        }
        macosArm64Main {
            dependsOn(nativeMain)
        }
    }

    // cinterop bindings for the GL demo C helper. The static library is
    // built by the CMake tasks registered below and embedded into the
    // produced klib, so final binaries link the demo code.
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        val targetName = this.name
        compilations.getByName("main") {
            cinterops.create("gl") {
                defFile(project.file("src/nativeMain/cinterop/gl.def"))
                includeDirs(
                    project.file("src/nativeMain/cinterop"),
                    rootProject.file("SDL/include"),
                )
                extraOpts(
                    "-libraryPath", layout.buildDirectory.dir("native/$targetName").get().asFile.absolutePath,
                    "-staticLibrary", "libsdl_kmp_gl.a",
                )
            }
        }
    }

    sourceSets {
        jvm {
            mainRun {
                mainClass = "cn.enaium.sdl.example.opengl.Main_jvmKt"
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
                listOf("linux", "macos", "macos-arm64", "windows").forEach { classifier ->
                    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion") {
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
    val cmakeBuildDir = layout.buildDirectory.dir("cmake-opengl-$targetName").get().asFile

    val commonFlags = listOf(
        cmakeExecutable, nativeDir.absolutePath,
        "-DCMAKE_BUILD_TYPE=Release",
        "-DSDL_SOURCE_DIR=${sdlDir.absolutePath}",
        "-DSDL3_KMP_OUTPUT_DIR=${outputDir.absolutePath}",
    )

    val configureTask = tasks.register<Exec>("configureGl_$targetName") {
        onlyIf { canBuildNativeTarget(targetName) }
        doFirst {
            cmakeBuildDir.mkdirs()
            outputDir.mkdirs()
        }
        workingDir = cmakeBuildDir
        commandLine(commonFlags + cmakeFlags)
    }

    val buildTask = tasks.register<Exec>("buildGl_$targetName") {
        onlyIf { canBuildNativeTarget(targetName) }
        dependsOn(configureTask)
        workingDir = cmakeBuildDir
        commandLine(cmakeExecutable, "--build", ".", "--config", "Release")
    }

    tasks.matching {
        it.name.startsWith("cinteropGl") &&
            it.name.endsWith(targetName.replaceFirstChar { c -> c.uppercase() })
    }.configureEach {
        dependsOn(buildTask)
        inputs.file(outputDir.resolve("libsdl_kmp_gl.a"))
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

