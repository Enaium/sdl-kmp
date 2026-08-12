plugins {
    alias(libs.plugins.android.application)
}

// The KMP example module builds libmain.so (with an exported SDL_main) for
// every androidNative ABI; copy those into jniLibs and depend on the link
// tasks so the APK is assembled after them.
val androidAbis = mapOf(
    "androidNativeArm64" to "arm64-v8a",
    "androidNativeArm32" to "armeabi-v7a",
    "androidNativeX64" to "x86_64",
    "androidNativeX86" to "x86",
)

abstract class PrepareJniLibsTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val abis: MapProperty<String, String>

    @TaskAction
    fun run() {
        val bin = project.layout.projectDirectory.dir("../example/build/bin").asFile
        outputDir.get().asFile.deleteRecursively()
        abis.get().forEach { (target, abi) ->
            val src = File(bin, "$target/mainDebugShared/libmain.so")
            val dstDir = File(outputDir.get().asFile, abi)
            dstDir.mkdirs()
            src.copyTo(File(dstDir, "libmain.so"), overwrite = true)
        }
    }
}

val prepareJniLibs = tasks.register<PrepareJniLibsTask>("prepareJniLibs") {
    outputDir.set(layout.buildDirectory.dir("generated/jniLibs"))
    abis.set(androidAbis)
}
prepareJniLibs.configure {
    androidAbis.keys.forEach { target ->
        dependsOn(project(":example").tasks.named("linkMainDebugShared${target.replaceFirstChar { it.uppercase() }}"))
    }
}

android {
    namespace = "cn.enaium.sdl.example"
    compileSdk = 35
    defaultConfig {
        applicationId = "cn.enaium.sdl.example"
        minSdk = 24
        targetSdk = 35
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
    // The org.libsdl.app SDLActivity sources come from the SDL submodule so
    // they match the statically linked SDL3 version.
    sourceSets["main"].java.srcDir("../SDL/android-project/app/src/main/java")
}

// Register the generated libmain.so directory with AGP's Variant API.
androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(prepareJniLibs) { it.outputDir }
    }
}

dependencies {
}
