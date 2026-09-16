import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

// Kotlin/Native's own Android toolchain sysroot (api 26) ships the NDK stub
// libraries (libEGL, libGLESv2, libOpenSLES, libaaudio, ...) that SDL3's
// android drivers reference at link time. Point -L at the per-ABI directory
// so the libmain.so link resolves them without needing an extra NDK install.
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

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    // The pure-JVM Android target: commonMain compiles to dex and drives SDL
    // through the sdl-kmp JNI bridge. Unlike the androidNative targets below
    // (Kotlin/Native libmain.so with an exported SDL_main), this path needs no
    // application native code at all - the android-jvm-app submodule hosts the
    // Activity, which overrides SDLActivity.main() with the shared loop.
    android {
        namespace = "cn.enaium.sdl.example.renderer"
        compileSdk = 36
        minSdk = 24

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_21)
                }
            }
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

    // The wasmJs target exposes the common demo code to the browser submodule
    // (which provides the actual browser entry point and HTML).
    wasmJs()

    // Android native targets build libmain.so with an exported SDL_main entry
    // point; SDLActivity (from the SDL3 AAR) loads and calls it. The SDL3
    // static library is linked in from the sdl-kmp klib; the Kotlin/Native
    // android sysroot provides the NDK system libraries it references. The
    // compiler-rt builtins embedded in the sdl-kmp klib overlap with K/N's
    // bundled libgcc on some ABIs (e.g. __sync_* on armv7); allow duplicates
    // so the first (K/N's) definition wins.
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
        // Kotlin 2.4's default hierarchy template does not create nativeMain
        // automatically; declare it and attach the native targets.
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

        // Shared home of the androidNative SDL_main entry point (libmain.so).
        // Named androidNativeMain, not androidMain: the latter belongs to the
        // android (JVM) target declared above, whose sources compile to dex.
        val androidNativeMain = create("androidNativeMain") {
            dependsOn(getByName("commonMain"))
        }
        androidNativeArm64Main {
            dependsOn(androidNativeMain)
        }
        androidNativeArm32Main {
            dependsOn(androidNativeMain)
        }
        androidNativeX64Main {
            dependsOn(androidNativeMain)
        }
        androidNativeX86Main {
            dependsOn(androidNativeMain)
        }

        jvm {
            mainRun {
                mainClass = "cn.enaium.sdl.example.renderer.Main_jvmKt"
            }
        }

        commonMain {
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
