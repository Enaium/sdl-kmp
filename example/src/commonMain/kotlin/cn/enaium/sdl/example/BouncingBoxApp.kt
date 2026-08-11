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

package cn.enaium.sdl.example

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLColor
import cn.enaium.sdl.SDLEvent
import cn.enaium.sdl.SDLInitFlags
import cn.enaium.sdl.SDLKeycode
import cn.enaium.sdl.SDLPoint
import cn.enaium.sdl.SDLRect
import cn.enaium.sdl.SDLWindowEventType
import cn.enaium.sdl.SDLWindowFlags

/**
 * A small "bouncing box" demo shared by every platform: window, renderer,
 * event loop, timer and input handling all live in commonMain.
 *
 * Platform entry points only provide `main()` (see jvmMain/nativeMain).
 */
fun runExample() {
    SDL.setMainReady()

    // The LWJGL-bundled SDL3 fails to initialize the Cocoa video driver on
    // macOS; fall back to the dummy driver and run headless in that case
    // (also covers CI/docker without a display server).
    var headless = false
    if (!SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
        println("video init failed (${SDL.error()}); falling back to the dummy driver")
        check(SDL.setHint("SDL_VIDEO_DRIVER", "dummy")) { "SDL_SetHint failed: ${SDL.error()}" }
        check(SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
            "SDL_Init failed: ${SDL.error()}"
        }
        headless = true
    }

    println("SDL ${SDL.version()} (${SDL.revision()})")
    println("Video driver: ${SDL.getCurrentVideoDriver()}")
    println("Audio driver: ${SDL.getCurrentAudioDriver()}")

    // SDL_VIDEO_DRIVER=dummy makes the demo run headless (CI, docker, ...);
    // in that case the loop exits after a fixed number of frames.
    headless = headless || SDL.getCurrentVideoDriver() == "dummy"
    val maxFrames = if (headless) 300 else Int.MAX_VALUE

    SDL.createWindow(
        title = "sdl-kmp example",
        width = 800,
        height = 600,
        flags = SDLWindowFlags.RESIZABLE,
    ).use { window ->
        SDL.createRenderer(window).use { renderer ->
            val box = BouncingBox { window.size }

            var running = true
            var frames = 0
            val start = SDL.getTicks()

            while (running) {
                // ---- events ----
                while (true) {
                    val event = SDL.pollEvent() ?: break
                    when (event) {
                        is SDLEvent.Quit -> running = false
                        is SDLEvent.Window ->
                            if (event.type == SDLWindowEventType.CLOSE_REQUESTED) {
                                running = false
                            }
                        is SDLEvent.Key ->
                            if (event.down && event.keycode == SDLKeycode.ESCAPE) {
                                running = false
                            }
                        is SDLEvent.MouseButton -> println(
                            "mouse ${if (event.down) "down" else "up"} at ${event.x.toInt()},${event.y.toInt()}",
                        )
                        else -> Unit
                    }
                }

                // ---- update ----
                box.update()

                // ---- render ----
                renderer.drawColor = SDLColor(18, 18, 24)
                renderer.clear()

                renderer.drawColor = SDLColor(255, 0, 128)
                renderer.fillRect(box.rect)

                renderer.drawColor = SDLColor(0, 200, 255)
                renderer.drawLine(0, 0, window.size.x, window.size.y)
                renderer.drawLine(window.size.x, 0, 0, window.size.y)

                renderer.drawColor = SDLColor(128, 128, 128)
                renderer.drawRect(SDLRect(0, 0, window.size.x - 1, window.size.y - 1))

                renderer.present()

                frames++
                if (frames % 120 == 0) {
                    val elapsedMs = (SDL.getTicks() - start).toFloat() / 1000f
                    println("fps: ${(frames / elapsedMs).toInt()}")
                }

                // ---- frame pacing (~60 FPS) ----
                SDL.delay(16)

                if (frames >= maxFrames) {
                    running = false
                }
            }

            println("ran $frames frames")
        }
    }

    SDL.quit()
    if (headless) {
        println("headless run finished")
    }
}

/** A box that bounces off the window edges. */
class BouncingBox(private val bounds: () -> SDLPoint) {

    private val size = 48
    private var x = (bounds().x - size) / 2
    private var y = (bounds().y - size) / 2
    private var vx = 3
    private var vy = 2

    val rect: SDLRect
        get() = SDLRect(x, y, size, size)

    fun update() {
        val (w, h) = bounds()
        x += vx
        y += vy
        if (x < 0) {
            x = 0
            vx = -vx
        } else if (x + size > w) {
            x = w - size
            vx = -vx
        }
        if (y < 0) {
            y = 0
            vy = -vy
        } else if (y + size > h) {
            y = h - size
            vy = -vy
        }
    }
}
