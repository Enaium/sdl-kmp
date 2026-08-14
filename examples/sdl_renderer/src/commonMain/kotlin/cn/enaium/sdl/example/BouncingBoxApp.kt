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
import cn.enaium.sdl.SDLAudioData
import cn.enaium.sdl.SDLAudioDeviceID
import cn.enaium.sdl.SDLAudioFormat
import cn.enaium.sdl.SDLAudioSpec
import cn.enaium.sdl.SDLBlendMode
import cn.enaium.sdl.SDLColor
import cn.enaium.sdl.SDLFloatPoint
import cn.enaium.sdl.SDLFRect
import cn.enaium.sdl.SDLInitFlags
import cn.enaium.sdl.SDLKeycode
import cn.enaium.sdl.SDLPixelFormat
import cn.enaium.sdl.SDLPoint
import cn.enaium.sdl.SDLRect
import cn.enaium.sdl.SDLScaleMode
import cn.enaium.sdl.SDLTextureAccess
import cn.enaium.sdl.SDLVertex
import cn.enaium.sdl.SDLAudioStream
import cn.enaium.sdl.SDLEvent
import cn.enaium.sdl.SDLRenderer
import cn.enaium.sdl.SDLSurface
import cn.enaium.sdl.SDLTexture
import cn.enaium.sdl.SDLWindow
import cn.enaium.sdl.SDLWindowEventType
import cn.enaium.sdl.SDLWindowFlags

/**
 * The bouncing-box demo as a frame state machine, shared by every platform.
 *
 * [frame] runs one frame and returns `false` when the demo should stop; the
 * platform entry points decide how to drive it:
 *
 *  - native / JVM: a blocking `while (demo.frame()) SDL.delay(16)` loop;
 *  - wasm: `requestAnimationFrame`, since a busy loop would freeze the browser.
 *
 * Setup and teardown are shared too: [createBouncingBoxDemo] creates the
 * window/renderer/textures/audio, and [BouncingBoxDemo.close] releases them.
 */
class BouncingBoxDemo(
    private val window: SDLWindow,
    private val renderer: SDLRenderer,
    private val texture: SDLTexture,
    private val surfaceTexture: SDLTexture,
    private val surface: SDLSurface,
    private val bounceStream: SDLAudioStream?,
    private val blip: ByteArray,
    private val maxFrames: Int,
) {
    private val box = BouncingBox { window.size }
    private var angle = 0.0
    private var paused = false
    private var clipEnabled = false
    private var logicalEnabled = false
    // Mouse-following square position (window coordinates).
    private var mouseX = window.size.x / 2f
    private var mouseY = window.size.y / 2f
    private var frames = 0
    private var running = true
    private val start = SDL.getTicks()

    /** The number of frames rendered so far. */
    fun frameCount(): Int = frames

    /** Runs one frame; returns `false` when the demo should stop. */
    fun frame(): Boolean {
        if (!running) return false

        // ---- events ----
        while (true) {
            val event = SDL.pollEvent() ?: break
            when (event) {
                is SDLEvent.Quit -> running = false
                is SDLEvent.Window ->
                    if (event.type == SDLWindowEventType.CLOSE_REQUESTED) {
                        running = false
                    }
                is SDLEvent.Key -> when {
                    !event.down -> Unit
                    event.keycode == SDLKeycode.ESCAPE -> running = false
                    event.keycode == SDLKeycode.SPACE -> {
                        paused = !paused
                        if (bounceStream != null) {
                            if (paused) {
                                SDL.pauseAudioDevice(SDLAudioDeviceID.DEFAULT_PLAYBACK)
                            } else {
                                SDL.resumeAudioDevice(SDLAudioDeviceID.DEFAULT_PLAYBACK)
                            }
                            println("audio ${if (paused) "paused" else "resumed"}")
                        }
                    }
                    event.keycode == SDLKeycode.UP -> bounceStream?.let { it.gain = (it.gain + 0.1f).coerceAtMost(2f); println("gain=${it.gain}") }
                    event.keycode == SDLKeycode.DOWN -> bounceStream?.let { it.gain = (it.gain - 0.1f).coerceAtLeast(0f); println("gain=${it.gain}") }
                    event.keycode == SDLKeycode.RIGHT -> bounceStream?.let { it.frequencyRatio = (it.frequencyRatio + 0.1f).coerceAtMost(2f); println("ratio=${it.frequencyRatio}") }
                    event.keycode == SDLKeycode.LEFT -> bounceStream?.let { it.frequencyRatio = (it.frequencyRatio - 0.1f).coerceAtLeast(0.5f); println("ratio=${it.frequencyRatio}") }
                    event.keycode == SDLKeycode.S -> {
                        val shot = renderer.renderReadPixels(null)
                        if (shot != null) {
                            val ok = shot.saveBMP("screenshot.bmp")
                            println("screenshot saved: $ok")
                            shot.close()
                        } else {
                            println("screenshot failed: ${SDL.error()}")
                        }
                        clipEnabled = !clipEnabled
                    }
                    event.keycode == SDLKeycode.L -> {
                        logicalEnabled = !logicalEnabled
                        if (logicalEnabled) {
                            renderer.setLogicalPresentation(800, 600, 2) // LETTERBOX
                        } else {
                            renderer.setLogicalPresentation(0, 0, 0) // DISABLED
                        }
                        println("logical presentation ${if (logicalEnabled) "enabled (800x600 letterbox)" else "disabled"}")
                    }
                    event.keycode == SDLKeycode.R -> {
                        renderer.viewport = null
                        renderer.clipRect = null
                        renderer.scale = SDLFloatPoint(1f, 1f)
                        clipEnabled = false
                        println("view/clip/scale reset")
                    }
                }
                is SDLEvent.AudioDevice -> println(
                    "audio device event: id=${event.deviceId} capture=${event.isCapture} type=0x${event.type.toString(16)}",
                )
                is SDLEvent.MouseButton -> println(
                    "mouse ${if (event.down) "down" else "up"} at ${event.x.toInt()},${event.y.toInt()}",
                )
                is SDLEvent.MouseMotion -> {
                    // Track the cursor for the mouse-following square.
                    mouseX = event.x
                    mouseY = event.y
                }
                else -> Unit
            }
        }

        // ---- update ----
        if (box.update() && bounceStream != null && !paused) {
            // Bounced: queue a short blip. Clear any stale queued data
            // first so fast bounces restart the sound cleanly.
            bounceStream.clear()
            bounceStream.putData(blip)
        }
        angle = (angle + 1.0) % 360.0

        // ---- render ----
        renderer.drawColor = SDLColor(18, 18, 24)
        renderer.clear()

        // clip rect / viewport / scale demo
        if (clipEnabled) {
            renderer.clipRect = SDLRect(40, 40, window.size.x - 80, window.size.y - 80)
        } else {
            renderer.clipRect = null
        }

        renderer.drawColor = SDLColor(255, 0, 128)
        renderer.fillRect(box.rect)

        // ---- square that follows the mouse ----
        val mouseRect = SDLRect(
            (mouseX - 15).toInt(),
            (mouseY - 15).toInt(),
            30,
            30,
        )
        renderer.drawColor = SDLColor(80, 255, 120)
        renderer.fillRect(mouseRect)
        renderer.drawColor = SDLColor(240, 240, 240)
        renderer.drawRect(mouseRect)

        // ---- textured spinning square ----
        texture.update(null, gradientPixels(64, 64), 64 * 4)
        renderer.renderTextureRotated(
            texture = texture,
            dst = SDLFRect(200f, 200f, 200f, 200f),
            angle = angle,
            center = SDLFloatPoint(100f, 100f),
        )

        // ---- 9-grid (sliced) texture ----
        renderer.renderTexture9Grid(
            texture = texture,
            src = SDLFRect(8f, 8f, 48f, 48f),
            leftWidth = 16f,
            rightWidth = 16f,
            topHeight = 16f,
            bottomHeight = 16f,
            scale = 1f,
            dst = SDLFRect(560f, 20f, 200f, 120f),
        )

        // ---- surface-backed texture (blit demo) ----
        renderer.blendMode = SDLBlendMode.BLEND
        renderer.renderTexture(
            texture = surfaceTexture,
            dst = SDLFRect(20f, 460f, 160f, 120f),
        )
        renderer.blendMode = SDLBlendMode.NONE

        // ---- renderGeometry: a gradient triangle ----
        renderer.renderGeometry(
            texture = texture,
            vertices = listOf(
                SDLVertex(SDLFloatPoint(120f, 420f), SDLColor(255, 0, 0)),
                SDLVertex(SDLFloatPoint(220f, 420f), SDLColor(0, 255, 0)),
                SDLVertex(SDLFloatPoint(170f, 360f), SDLColor(0, 0, 255)),
            ),
        )

        // ---- viewport + scale demo (HUD corner) ----
        renderer.viewport = SDLRect(window.size.x - 140, 20, 120, 90)
        renderer.scale = SDLFloatPoint(0.5f, 0.5f)
        renderer.drawColor = SDLColor(255, 255, 255)
        renderer.fillRect(SDLRect(0, 0, 200, 140))
        renderer.drawColor = SDLColor(0, 0, 0)
        renderer.drawRect(SDLRect(0, 0, 199, 139))
        renderer.viewport = null
        renderer.scale = SDLFloatPoint(1f, 1f)

        renderer.drawColor = SDLColor(0, 200, 255)
        renderer.drawLine(0, 0, window.size.x, window.size.y)
        renderer.drawLine(window.size.x, 0, 0, window.size.y)

        renderer.drawColor = SDLColor(128, 128, 128)
        renderer.drawRect(SDLRect(0, 0, window.size.x - 1, window.size.y - 1))

        // ---- logical presentation (L key toggles; default off so
        // fullscreen devices like phones keep their native resolution) ----
        if (logicalEnabled && frames % 120 == 0) {
            val logicalRect = renderer.logicalPresentationRect
            if (logicalRect != null) {
                println("logical presentation rect: $logicalRect")
            }
        }

        renderer.present()

        frames++
        if (frames % 120 == 0) {
            val elapsedMs = (SDL.getTicks() - start).toFloat() / 1000f
            println("fps: ${(frames / elapsedMs).toInt()}")
        }

        if (frames >= maxFrames) {
            running = false
        }
        return running
    }

    /** Releases all demo resources, including the window and renderer. */
    fun close() {
        texture.close()
        surfaceTexture.close()
        surface.close()
        bounceStream?.close()
        renderer.close()
        window.close()
    }
}

/**
 * Sets up the demo: SDL init, window, renderer, textures and audio. Returns
 * null when window or renderer creation fails.
 */
fun createBouncingBoxDemo(maxFrames: Int): BouncingBoxDemo? {
    // ---------- audio: enumerate devices ----------
    val playbackDevices = SDL.audioPlaybackDevices
    if (playbackDevices.isNotEmpty()) {
        println("Audio playback devices: ${playbackDevices.map { it to SDL.getAudioDeviceName(it) }}")
    } else {
        println("Audio playback devices: none")
    }

    val window = try {
        SDL.createWindow(
            title = "sdl-kmp example",
            width = 800,
            height = 600,
            flags = SDLWindowFlags.RESIZABLE,
        )
    } catch (t: Throwable) {
        println("window creation failed: ${t.message}")
        return null
    }

    val renderer = try {
        SDL.createRenderer(window)
    } catch (t: Throwable) {
        println("renderer creation failed: ${t.message}")
        window.close()
        return null
    }

    val texture = renderer.createTexture(
        format = SDLPixelFormat.RGBA8888,
        access = SDLTextureAccess.STREAMING,
        width = 64,
        height = 64,
    )

    // A second texture from a software surface (createSurface + blit).
    val surface = SDL.createSurface(160, 120, SDLPixelFormat.RGBA8888)
    surface.fillRects(
        listOf(
            SDLRect(0, 0, 160, 60),
            SDLRect(0, 120 - 30, 160, 30),
        ),
        SDLColor(40, 60, 120),
    )
    surface.fillRect(SDLRect(60, 30, 40, 60), SDLColor(220, 180, 60))
    val surfaceTexture = renderer.createTextureFromSurface(surface)
    println("Surface: ${surface.width}x${surface.height} fmt=0x${surface.format.toString(16)} colorspace=${surface.colorspace}")

    // ---------- audio: open a device stream; the bouncing box plays a
    // short "blip" whenever it bounces off an edge ----------
    val bounceSpec = SDLAudioSpec(
        format = SDLAudioFormat.F32LE,
        channels = 2,
        freq = 48000,
    )
    val bounceStream = try {
        SDL.openAudioDeviceStream(
            deviceId = SDLAudioDeviceID.DEFAULT_PLAYBACK,
            spec = bounceSpec,
        ).also { it ->
            it.gain = 0.5f
            // SDL_OpenAudioDeviceStream opens the device in a PAUSED
            // state; without this no audio is heard (all platforms).
            it.devicePaused = false
            SDL.resumeAudioDevice(SDLAudioDeviceID.DEFAULT_PLAYBACK)
            println(
                "Audio stream: input=${it.inputSpec} output=${it.outputSpec} " +
                    "gain=${it.gain} streamPaused=${it.devicePaused} " +
                    "devicePaused=${SDL.isAudioDevicePaused(SDLAudioDeviceID.DEFAULT_PLAYBACK)}",
            )
        }
    } catch (e: Throwable) {
        println("audio unavailable: ${e.message}")
        null
    }

    // A short blip (~90ms, exponential decay) played on each bounce.
    val blip = generateBounceBlip(freq = 880f, sampleRate = 48000, durationMs = 90, volume = 0.6f)

    return BouncingBoxDemo(window, renderer, texture, surfaceTexture, surface, bounceStream, blip, maxFrames)
}

/**
 * Blocking runner used by native / JVM: drives [BouncingBoxDemo.frame] with
 * `SDL.delay` for frame pacing.
 */
fun runExample() {
    SDL.setMainReady()

    // Try video init with the platform's best driver. On headless CI or
    // servers the dummy driver is needed; on a desktop the native driver
    // (cocoa/x11) should work. If the initial attempt fails and the
    // dummy driver succeeds we assume headless — otherwise report the error.
    // The dummy fallback skips AUDIO: headless runners often have no audio
    // device at all, and the demo degrades gracefully without it.
    var headless = false
    if (!SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS or SDLInitFlags.AUDIO)) {
        SDL.setHint("SDL_VIDEO_DRIVER", "dummy")
        if (SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
            println("video init fell back to the dummy driver — running headless (audio skipped)")
            headless = true
        } else {
            error("SDL_Init(VIDEO) failed: ${SDL.error()}\nMake sure a display is available, or set SDL_VIDEO_DRIVER=dummy to run headless.")
        }
    }

    println("SDL ${SDL.version()} (${SDL.revision()})")
    println("Video driver: ${SDL.getCurrentVideoDriver()}")
    println("Audio driver: ${SDL.getCurrentAudioDriver()}")

    // SDL_VIDEO_DRIVER=dummy makes the demo run headless (CI, docker, ...);
    // in that case the loop exits after a fixed number of frames.
    headless = headless || SDL.getCurrentVideoDriver() == "dummy"
    val maxFrames = if (headless) 300 else Int.MAX_VALUE

    val demo = createBouncingBoxDemo(maxFrames) ?: return

    while (demo.frame()) {
        // ---- frame pacing (~60 FPS) ----
        SDL.delay(16)
    }

    println("ran ${demo.frameCount()} frames")
    demo.close()
    SDL.quit()
    if (headless) {
        println("headless run finished")
    }
}

/** Generates a 64x64 RGBA gradient. */
fun gradientPixels(width: Int, height: Int): ByteArray {
    val pixels = ByteArray(width * height * 4)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val i = (y * width + x) * 4
            pixels[i] = (x * 255 / width).toByte()
            pixels[i + 1] = (y * 255 / height).toByte()
            pixels[i + 2] = 255.toByte()
            pixels[i + 3] = 255.toByte()
        }
    }
    return pixels
}

/**
 * Generates a short decaying "blip" in stereo F32LE for bounce effects.
 */
fun generateBounceBlip(freq: Float, sampleRate: Int, durationMs: Int, volume: Float): ByteArray {
    val samples = sampleRate * durationMs / 1000
    val out = ByteArray(samples * 2 * 4) // 2 channels * 4 bytes (F32)
    for (i in 0 until samples) {
        val t = i.toFloat() / sampleRate
        val decay = kotlin.math.exp(-6f * i / samples) // exponential decay
        val v = kotlin.math.sin(2.0 * kotlin.math.PI * freq * t).toFloat() * volume * decay
        val bits = v.toBits()
        for (c in 0 until 2) {
            val o = (i * 2 + c) * 4
            out[o] = (bits ushr 0).toByte()
            out[o + 1] = (bits ushr 8).toByte()
            out[o + 2] = (bits ushr 16).toByte()
            out[o + 3] = (bits ushr 24).toByte()
        }
    }
    return out
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

    /** Returns `true` when the box bounced off an edge this update. */
    fun update(): Boolean {
        val (w, h) = bounds()
        var bounced = false
        x += vx
        y += vy
        if (x < 0) {
            x = 0
            vx = -vx
            bounced = true
        } else if (x + size > w) {
            x = w - size
            vx = -vx
            bounced = true
        }
        if (y < 0) {
            y = 0
            vy = -vy
            bounced = true
        } else if (y + size > h) {
            y = h - size
            vy = -vy
            bounced = true
        }
        return bounced
    }
}
