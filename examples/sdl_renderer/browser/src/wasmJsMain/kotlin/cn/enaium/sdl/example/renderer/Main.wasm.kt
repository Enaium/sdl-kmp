/*
 * Copyright (c) 2026 Enaium
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cn.enaium.sdl.example.renderer

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLInitFlags
import kotlin.js.ExperimentalJsExport

/**
 * Browser (wasmJs) entry point.
 *
 * Browsers cannot repaint a busy main thread, so the shared [BouncingBoxDemo]
 * frame state machine (used by native/JVM through a blocking [runExample]
 * loop) is driven here from `requestAnimationFrame` instead — one frame per
 * browser paint, which is the cooperative model Kotlin/Wasm needs.
 *
 * The host page initializes the Emscripten SDL3 module (sdl_kmp_glue.js)
 * before this module is imported, so the SDL wasmJs actuals' JS globals are
 * already available when [main] runs.
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() {
    SDL.setMainReady()
    SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS or SDLInitFlags.AUDIO)

    // Headless (Node) runs have no quit event; stop after a fixed frame count.
    val headless = SDL.getCurrentVideoDriver() == "dummy"
    val maxFrames = if (headless) 300 else Int.MAX_VALUE

    val demo = createBouncingBoxDemo(maxFrames) ?: return

    fun frame() {
        if (demo.frame()) {
            requestAnimationFrame { frame() }
        } else {
            println("ran ${demo.frameCount()} frames")
            demo.close()
            SDL.quit()
        }
    }

    requestAnimationFrame { frame() }
}

private external fun requestAnimationFrame(callback: () -> Unit): Int
