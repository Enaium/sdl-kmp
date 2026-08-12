import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlin.multiplatform)
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

        jvm {
            mainRun {
                mainClass = "cn.enaium.sdl.example.Main_jvmKt"
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