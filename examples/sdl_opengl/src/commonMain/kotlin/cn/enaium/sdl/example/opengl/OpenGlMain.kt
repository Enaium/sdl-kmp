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

package cn.enaium.sdl.example.opengl

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLEvent
import cn.enaium.sdl.SDLInitFlags
import cn.enaium.sdl.SDLKeycode
import cn.enaium.sdl.SDLWindowEventType
import cn.enaium.sdl.SDLWindowFlags

/**
 * Minimal OpenGL triangle demo shared by every platform.
 *
 * Platform entry points only provide `main()` (see jvmMain/nativeMain).
 */
fun runExample() {
    SDL.setMainReady()

    if (!SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
        error("SDL_Init(VIDEO) failed: ${SDL.error()}")
    }
    println("SDL ${SDL.version()} (${SDL.revision()})")
    println("Video driver: ${SDL.getCurrentVideoDriver()}")

    SDL.createWindow(
        title = "sdl-kmp example-opengl",
        width = 800,
        height = 600,
        flags = SDLWindowFlags.OPENGL or SDLWindowFlags.RESIZABLE,
    ).use { window ->
        val renderer = try {
            createGpuRenderer(window, window.size.x, window.size.y)
        } catch (t: Throwable) {
            println("OpenGL renderer creation failed: ${t.message}")
            println("last SDL error: ${SDL.error()}")
            return
        }
        renderer.use { r ->
            println("OpenGL renderer created")
            var running = true
            var frames = 0
            val start = SDL.getTicks()

            while (running) {
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

                if (!r.render(window.size.x, window.size.y)) {
                    println("render failed: ${SDL.error()}")
                    running = false
                }

                frames++
                if (frames % 120 == 0) {
                    val elapsedMs = (SDL.getTicks() - start).toFloat() / 1000f
                    println("fps: ${(frames / elapsedMs).toInt()}")
                }
                SDL.delay(16)
            }
            println("ran $frames frames")
        }
    }

    SDL.quit()
}
