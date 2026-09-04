/*
 * Android library that packages SDL's own android-project Java layer
 * (org.libsdl.app: SDLActivity, SDLSurface, SDL, SDLAudioManager, ...)
 * together with the per-ABI libsdl_jni shared library (SDL3 + JNI bridge,
 * built with the NDK from the same jni/ sources the desktop JVM artifacts
 * use).
 *
 * Consumers add this artifact, write an Activity extending
 * org.libsdl.app.SDLActivity and override getLibraries() to return
 * {"sdl_jni"} - no SDL Java code needs to be copied into the app.
 */
import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

group = rootProject.group
version = rootProject.version

val sdlDir = rootProject.file("SDL")

// ---------------------------------------------------------------------------
// Toolchain discovery
// ---------------------------------------------------------------------------

fun localSdkDir(): String? {
    val f = rootProject.file("local.properties")
    if (!f.isFile) return null
    return f.readLines()
        .firstOrNull { it.trimStart().startsWith("sdk.dir=") }
        ?.substringAfter('=')
        ?.trim()
}

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

fun resolveNinjaExecutable(ndk: String): String {
    val candidates = listOf(
        "$ndk/prebuilt/${hostPrebuiltDir()}/bin/ninja",
        "$ndk/build/tools/ninja",
    )
    candidates.forEach { candidate ->
        val f = File(candidate)
        if (f.isFile && f.canExecute()) return candidate
    }
    return "ninja"
}

fun hostPrebuiltDir(): String {
    val os = OperatingSystem.current()
    val arch = System.getProperty("os.arch").lowercase()
    return when {
        os.isMacOsX && arch.contains("aarch64") -> "darwin-arm64"
        os.isMacOsX -> "darwin-x86_64"
        os.isLinux && arch.contains("aarch64") -> "linux-aarch64"
        os.isLinux -> "linux-x86_64"
        os.isWindows -> "windows-x86_64"
        else -> error("Unsupported host for android-jvm build: $os/$arch")
    }
}

val cmakeExecutable: String by lazy { resolveCmakeExecutable() }
val ndkPath: String? = androidNdkPath()
val ninjaExecutable: String by lazy { ndkPath?.let { resolveNinjaExecutable(it) } ?: "ninja" }

// All four NDK ABIs (SDK 24+, matching the androidNative targets of :sdl-kmp).
val androidAbis = mapOf(
    "androidNativeArm64" to "arm64-v8a",
    "androidNativeArm32" to "armeabi-v7a",
    "androidNativeX64" to "x86_64",
    "androidNativeX86" to "x86",
)

// ---------------------------------------------------------------------------
// Android library
// ---------------------------------------------------------------------------

android {
    namespace = "cn.enaium.sdl.android"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
        consumerProguardFiles("proguard-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // SDL's own android-project Java layer is the source of this library, so
    // it always matches the SDL3 version statically linked into libsdl_jni.
    sourceSets {
        getByName("main") {
            java.srcDir("$sdlDir/android-project/app/src/main/java")
            res.srcDirs("$sdlDir/android-project/app/src/main/res")
        }
    }
    lint {
        abortOnError = false
    }
}

// ---------------------------------------------------------------------------
// Native: build libsdl_jni.so for every ABI with the NDK.
// Outputs land in build/generated/jniLibs/<abi>/libsdl_jni.so.
// ---------------------------------------------------------------------------

val jniSourceDir = rootProject.file("jni").absolutePath
val generatedJniLibs = layout.buildDirectory.dir("generated/jniLibs")

abstract class PrepareJniLibsTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val abis: MapProperty<String, String>

    @TaskAction
    fun run() {
        outputDir.get().asFile.deleteRecursively()
        abis.get().forEach { (_, abi) ->
            val src = File(project.layout.buildDirectory.get().asFile, "cmake-jni/$abi/libsdl_jni.so")
            if (!src.isFile) {
                throw GradleException("Missing native library for $abi: $src")
            }
            val dstDir = File(outputDir.get().asFile, abi)
            dstDir.mkdirs()
            src.copyTo(File(dstDir, "libsdl_jni.so"), overwrite = true)
        }
    }
}

ndkPath?.let { ndk ->
    val toolchain = "$ndk/build/cmake/android.toolchain.cmake"
    val javaHome = System.getProperty("java.home") ?: System.getenv("JAVA_HOME") ?: ""
    val jniInclude = if (javaHome.isNotEmpty()) "$javaHome/include" else ""
    val jniIncludePlatform = if (javaHome.isNotEmpty()) "$javaHome/include/darwin" else ""

    androidAbis.forEach { (_, abi) ->
        val buildDir = layout.buildDirectory.dir("cmake-jni/$abi")

        val configureTask = tasks.register<Exec>("configureJniLib_$abi") {
            doFirst {
                buildDir.get().asFile.mkdirs()
            }
            workingDir = buildDir.get().asFile
            commandLine(
                cmakeExecutable,
                "-G", "Ninja",
                "-DCMAKE_MAKE_PROGRAM=$ninjaExecutable",
                jniSourceDir,
                "-DCMAKE_BUILD_TYPE=Release",
                "-DCMAKE_TOOLCHAIN_FILE=$toolchain",
                "-DANDROID_ABI=$abi",
                "-DANDROID_PLATFORM=android-24",
                "-DANDROID_STL=c++_static",
                "-DJNI_INCLUDE_DIR=$jniInclude",
                "-DJNI_INCLUDE_DIR_PLATFORM=$jniIncludePlatform",
                "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${buildDir.get().asFile.absolutePath}",
            )
        }

        tasks.register<Exec>("buildJniLib_$abi") {
            dependsOn(configureTask)
            workingDir = buildDir.get().asFile
            commandLine(cmakeExecutable, "--build", ".", "--config", "Release")
            inputs.files(
                rootProject.file("jni/CMakeLists.txt"),
                rootProject.file("jni/jni_bridge.h"),
                rootProject.file("jni/jni_bridge.cpp"),
                rootProject.file("jni/jni_audio.cpp"),
                rootProject.file("jni/jni_input.cpp"),
                rootProject.file("jni/jni_gl.cpp"),
                rootProject.file("jni/jni_gpu.cpp"),
                rootProject.file("jni/jni_tools.cpp"),
            )
            inputs.dir(sdlDir)
            outputs.file(buildDir.map { it.file("libsdl_jni.so") })
        }
    }
}

val prepareJniLibs = tasks.register<PrepareJniLibsTask>("prepareJniLibs") {
    outputDir.set(generatedJniLibs)
    abis.set(androidAbis)
}
prepareJniLibs.configure {
    androidAbis.keys.forEach { target ->
        dependsOn("buildJniLib_${androidAbis.getValue(target)}")
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(prepareJniLibs) { it.outputDir }
    }
}

// ---------------------------------------------------------------------------
// Publishing
// ---------------------------------------------------------------------------

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    coordinates(
        groupId = rootProject.group.toString(),
        artifactId = "sdl-kmp-android-jvm",
        version = rootProject.version.toString(),
    )
    pom {
        name.set("sdl-kmp-android-jvm")
        description.set(
            "Android library packaging SDL's own android-project Java layer (SDLActivity and friends) " +
                "with the per-ABI libsdl_jni shared library (SDL3 + JNI bridge). " +
                "Extend SDLActivity and return {\"sdl_jni\"} from getLibraries() - no SDL Java code to copy.",
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
