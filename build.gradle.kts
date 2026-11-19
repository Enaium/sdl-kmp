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
    // <this library's version>-fixed.<pinned SDL release>: the wrapper is
    // versioned on its own, and the suffix records the exact upstream release
    // it wraps. The whole string is also valid semver, so the npm package
    // published from :sdl-kmp uses it unchanged.
    version = "1.0.14-fixed.3.4.18"
}
