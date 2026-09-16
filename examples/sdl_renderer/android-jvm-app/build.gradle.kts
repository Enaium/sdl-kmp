plugins {
    alias(libs.plugins.android.application)
}

// The pure-JVM (ART) entry into the shared example. MainActivity extends
// org.libsdl.app.SDLActivity, loads "sdl_jni" (the SDL3 + JNI bridge library
// the AAR ships) and runs the commonMain demo from the activity's main() on
// SDL's dedicated thread - no native code, no Kotlin/Native, no vendored SDL
// sources. SDL3, the JNI bridge, SDL's Android Java layer and the per-ABI
// libsdl_jni.so all arrive through :sdl-kmp's android variant
// (sdl-kmp-android + sdl-kmp-android-jvm).
android {
    namespace = "cn.enaium.sdl.example.renderer.jvm"
    compileSdk = 36
    defaultConfig {
        applicationId = "cn.enaium.sdl.example.renderer.jvm"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":sdl-kmp"))
    implementation(project(":examples:sdl_renderer"))
}
