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

package cn.enaium.sdl.example.opengles

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLEvent
import cn.enaium.sdl.SDLInitFlags
import cn.enaium.sdl.SDLKeycode
import cn.enaium.sdl.SDLWindowEventType
import cn.enaium.sdl.SDLWindowFlags
import kotlin.js.ExperimentalJsExport

/**
 * Browser (wasmJs) entry point.
 *
 * Browsers cannot repaint a busy main thread, so the render loop is driven
 * from `requestAnimationFrame` (one frame per paint). The host page
 * initializes the Emscripten SDL3 module (sdl_kmp_glue.js) before this module
 * is imported.
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() {
    SDL.setMainReady()
    SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)

    val window = SDL.createWindow(
        title = "sdl-kmp example-opengles",
        width = 800,
        height = 600,
        flags = SDLWindowFlags.OPENGL or SDLWindowFlags.RESIZABLE,
    )
    val renderer = createGpuRenderer(window, 800, 600)
    var running = true

    fun frame() {
        if (!running) {
            renderer.close()
            window.close()
            SDL.quit()
            return
        }

        while (true) {
            val event = SDL.pollEvent() ?: break
            when (event) {
                is SDLEvent.Quit -> running = false
                is SDLEvent.Window ->
                    if (event.type == SDLWindowEventType.CLOSE_REQUESTED) running = false
                is SDLEvent.Key ->
                    if (event.down && event.keycode == SDLKeycode.ESCAPE) running = false
                else -> Unit
            }
        }
        if (!running) {
            requestAnimationFrame { frame() }
            return
        }

        renderer.render(window.size.x, window.size.y)
        requestAnimationFrame { frame() }
    }

    requestAnimationFrame { frame() }
}

private external fun requestAnimationFrame(callback: () -> Unit): Int
