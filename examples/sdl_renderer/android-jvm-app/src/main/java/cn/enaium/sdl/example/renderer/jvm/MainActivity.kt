package cn.enaium.sdl.example.renderer.jvm

import android.util.Log
import cn.enaium.sdl.SDL
import cn.enaium.sdl.example.renderer.runExample
import org.libsdl.app.SDLActivity

/**
 * Launcher activity for the pure-JVM Android path.
 *
 * The stock Android integration points SDL at a native library and calls its
 * exported `SDL_main`; a Kotlin/JVM application has none, so it overrides
 * [main] instead - SDL's documented application entry point, which runs on
 * SDL's dedicated thread between its own `nativeInitMainThread` and
 * `nativeCleanupMainThread` handles - and loads the one shared object that has
 * to be there: `sdl_jni`, the SDL3 + JNI bridge library bundled by the
 * `sdl-kmp-android-jvm` artifact (SDL's Android Java layer and the per-ABI
 * `.so` files come from the same artifact).
 *
 * The shared commonMain demo drives the window/renderer/audio loop through the
 * sdl-kmp JNI bridge, so nothing here is compiled with Kotlin/Native or the
 * NDK.
 */
class MainActivity : SDLActivity() {

    override fun getLibraries(): Array<String> = arrayOf("sdl_jni")

    override fun main() {
        try {
            runExample()
        } catch (t: Throwable) {
            Log.e(TAG, "sdl-kmp example failed", t)
        } finally {
            SDL.quit()
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
