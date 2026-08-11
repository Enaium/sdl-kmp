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

import cn.enaium.sdl.Sdl
import cn.enaium.sdl.SdlColor
import cn.enaium.sdl.SdlEvent
import cn.enaium.sdl.SdlInitFlags
import cn.enaium.sdl.SdlKeycode
import cn.enaium.sdl.SdlPoint
import cn.enaium.sdl.SdlRect
import cn.enaium.sdl.SdlWindowEventType
import cn.enaium.sdl.SdlWindowFlags

/**
 * A small "bouncing box" demo shared by every platform: window, renderer,
 * event loop, timer and input handling all live in commonMain.
 *
 * Platform entry points only provide `main()` (see jvmMain/nativeMain).
 */
fun runExample() {
    Sdl.setMainReady()

    // The LWJGL-bundled SDL3 fails to initialize the Cocoa video driver on
    // macOS; fall back to the dummy driver and run headless in that case
    // (also covers CI/docker without a display server).
    var headless = false
    if (!Sdl.init(SdlInitFlags.VIDEO or SdlInitFlags.EVENTS)) {
        println("video init failed (${Sdl.error()}); falling back to the dummy driver")
        check(Sdl.setHint("SDL_VIDEO_DRIVER", "dummy")) { "SDL_SetHint failed: ${Sdl.error()}" }
        check(Sdl.init(SdlInitFlags.VIDEO or SdlInitFlags.EVENTS)) {
            "SDL_Init failed: ${Sdl.error()}"
        }
        headless = true
    }

    println("SDL ${Sdl.version()} (${Sdl.revision()})")
    println("Video driver: ${Sdl.getCurrentVideoDriver()}")
    println("Audio driver: ${Sdl.getCurrentAudioDriver()}")

    // SDL_VIDEO_DRIVER=dummy makes the demo run headless (CI, docker, ...);
    // in that case the loop exits after a fixed number of frames.
    headless = headless || Sdl.getCurrentVideoDriver() == "dummy"
    val maxFrames = if (headless) 300 else Int.MAX_VALUE

    Sdl.createWindow(
        title = "sdl-kmp example",
        width = 800,
        height = 600,
        flags = SdlWindowFlags.RESIZABLE,
    ).use { window ->
        Sdl.createRenderer(window).use { renderer ->
            val box = BouncingBox { window.size }

            var running = true
            var frames = 0
            val start = Sdl.getTicks()

            while (running) {
                // ---- events ----
                while (true) {
                    val event = Sdl.pollEvent() ?: break
                    when (event) {
                        is SdlEvent.Quit -> running = false
                        is SdlEvent.Window ->
                            if (event.type == SdlWindowEventType.CLOSE_REQUESTED) {
                                running = false
                            }
                        is SdlEvent.Key ->
                            if (event.down && event.keycode == SdlKeycode.ESCAPE) {
                                running = false
                            }
                        is SdlEvent.MouseButton -> println(
                            "mouse ${if (event.down) "down" else "up"} at ${event.x.toInt()},${event.y.toInt()}",
                        )
                        else -> Unit
                    }
                }

                // ---- update ----
                box.update()

                // ---- render ----
                renderer.drawColor = SdlColor(18, 18, 24)
                renderer.clear()

                renderer.drawColor = SdlColor(255, 0, 128)
                renderer.fillRect(box.rect)

                renderer.drawColor = SdlColor(0, 200, 255)
                renderer.drawLine(0, 0, window.size.x, window.size.y)
                renderer.drawLine(window.size.x, 0, 0, window.size.y)

                renderer.drawColor = SdlColor(128, 128, 128)
                renderer.drawRect(SdlRect(0, 0, window.size.x - 1, window.size.y - 1))

                renderer.present()

                frames++
                if (frames % 120 == 0) {
                    val elapsedMs = (Sdl.getTicks() - start).toFloat() / 1000f
                    println("fps: ${(frames / elapsedMs).toInt()}")
                }

                // ---- frame pacing (~60 FPS) ----
                Sdl.delay(16)

                if (frames >= maxFrames) {
                    running = false
                }
            }

            println("ran $frames frames")
        }
    }

    Sdl.quit()
    if (headless) {
        println("headless run finished")
    }
}

/** A box that bounces off the window edges. */
class BouncingBox(private val bounds: () -> SdlPoint) {

    private val size = 48
    private var x = (bounds().x - size) / 2
    private var y = (bounds().y - size) / 2
    private var vx = 3
    private var vy = 2

    val rect: SdlRect
        get() = SdlRect(x, y, size, size)

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
