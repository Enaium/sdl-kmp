import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
        getByName("macosArm64Main").dependsOn(nativeMain)
        getByName("linuxX64Main").dependsOn(nativeMain)

        getByName("commonMain") {
            dependencies {
                // Consume the artifact published to the local Maven repository
                // (run `./gradlew :sdl-kmp:publishToMavenLocal` first).
                implementation("cn.enaium.sdl:sdl-kmp:1.0.0")
            }
        }
    }
}

// The `application` plugin is incompatible with Kotlin Multiplatform, so the
// JVM run task is registered manually.
tasks.register<JavaExec>("runJvm") {
    group = "application"
    description = "Runs the example on the JVM."
    mainClass.set(providers.gradleProperty("example.mainClass").orElse("cn.enaium.sdl.example.Main_jvmKt"))
    val compilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    classpath = files(compilation.output.allOutputs, compilation.runtimeDependencyFiles)
    // JDK 22+ requires explicit native access for JNI-based libraries like LWJGL.
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
