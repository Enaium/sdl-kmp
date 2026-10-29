plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kmp.library) apply false
}

// ==================== Project coordinates ====================
allprojects {
    group = "cn.enaium.sdl"
    // Tracks the pinned SDL release (3.4.18); the trailing component is this
    // library's own revision.
    version = "3.4.18.1"
}
