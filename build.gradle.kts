plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.android.application) apply false
}

allprojects {
    group = "cn.enaium.sdl"
    version = "1.0.8"
}
