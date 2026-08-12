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

package cn.enaium.sdl.example.gpu

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLEvent
import cn.enaium.sdl.SDLInitFlags
import cn.enaium.sdl.SDLKeycode
import cn.enaium.sdl.SDLWindowEventType
import cn.enaium.sdl.SDLWindowFlags

/** The graphics API to render with. */
enum class GpuApi {
    OPENGL,
    VULKAN,
}

/** A minimal triangle renderer; platform implementations back it. */
interface GpuRenderer : AutoCloseable {
    /** Renders one frame at [width]x[height]; returns `false` on failure. */
    fun render(width: Int, height: Int): Boolean

    override fun close()
}

/** Creates the platform renderer for [api] on [window]. */
expect fun createGpuRenderer(api: GpuApi, window: cn.enaium.sdl.SDLWindow, width: Int, height: Int): GpuRenderer

/**
 * Loads the Vulkan loader library before a Vulkan window is created.
 *
 * SDL3's macOS driver does not search `/opt/homebrew`, so when the default
 * search fails we fall back to the usual Homebrew/MacPorts install paths.
 */
private fun loadVulkanLibrary(): Boolean {
    if (SDL.vulkanLoadLibrary()) return true
    return listOf(
        "/opt/homebrew/lib/libvulkan.dylib",
        "/opt/homebrew/lib/libvulkan.1.dylib",
        "/opt/homebrew/lib/libMoltenVK.dylib",
        "/usr/local/lib/libvulkan.dylib",
    ).any { SDL.vulkanLoadLibrary(it) }
}

/**
 * Minimal OpenGL / Vulkan triangle demo shared by every platform.
 *
 * Run the JVM app with `--args=vulkan` (or edit [Main_jvmKt]) to pick the
 * API; native builds take an optional first argument instead.
 */
fun runExampleGpu(api: GpuApi) {
    SDL.setMainReady()

    if (!SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
        error("SDL_Init(VIDEO) failed: ${SDL.error()}")
    }
    println("SDL ${SDL.version()} (${SDL.revision()})")
    println("Video driver: ${SDL.getCurrentVideoDriver()}")

    val flags = when (api) {
        GpuApi.OPENGL -> SDLWindowFlags.OPENGL or SDLWindowFlags.RESIZABLE
        GpuApi.VULKAN -> SDLWindowFlags.VULKAN or SDLWindowFlags.RESIZABLE
    }

    if (api == GpuApi.VULKAN && !loadVulkanLibrary()) {
        println("Vulkan library load failed: ${SDL.error()}")
    }

    SDL.createWindow(
        title = "sdl-kmp example-gpu ($api)",
        width = 800,
        height = 600,
        flags = flags,
    ).use { window ->
        val renderer = try {
            createGpuRenderer(api, window, window.size.x, window.size.y)
        } catch (t: Throwable) {
            println("$api renderer creation failed: ${t.message}")
            println("last SDL error: ${SDL.error()}")
            return
        }
        renderer.use { r ->
            println("$api renderer created")
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
