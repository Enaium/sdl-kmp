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

package cn.enaium.sdl

/**
 * A native SDL window.
 *
 * The window is owned by the underlying SDL3 instance; call [close] (or
 * [SDL.quit]) to release it. On native platforms the SDL main thread
 * requirement applies (create and use windows from the thread that called
 * [SDL.setMainReady]).
 */
interface SDLWindow : AutoCloseable {

    /** The SDL window ID, unique across the process. */
    val id: Int

    /** The window title. */
    var title: String

    /** The window size in screen coordinates. */
    var size: SDLPoint

    /** The window flags (see [SDLWindowFlags]). */
    val flags: ULong

    fun show()
    fun hide()
    fun raise()

    /** The window was closed; releases the underlying SDL window. */
    override fun close()
}

/**
 * An SDL2D renderer bound to an [SDLWindow].
 *
 * All drawing happens in window coordinates; call [present] after issuing
 * draw commands. Destroy the renderer before its window.
 */
interface SDLRenderer : AutoCloseable {

    /** The name of the rendering driver in use. */
    val name: String?

    /** The current drawing color. */
    var drawColor: SDLColor

    /** The size of the rendering target in pixels. */
    val outputSize: SDLPoint

    /** Clears the rendering target with the current [drawColor]. */
    fun clear(): Boolean

    /** Presents the rendered frame to the window. */
    fun present()

    /** Fills [rect] with the current [drawColor]. */
    fun fillRect(rect: SDLRect): Boolean

    /** Draws an outline of [rect] with the current [drawColor]. */
    fun drawRect(rect: SDLRect): Boolean

    /** Draws a line between two points with the current [drawColor]. */
    fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int): Boolean

    /** Releases the underlying SDL renderer. */
    override fun close()
}

/**
 * Kotlin Multiplatform bindings for SDL3.
 *
 * This is a curated, common subset of the SDL3 API:
 *
 *  - core: [init], [quit], [error], [version], hints, clipboard
 *  - video: [createWindow], driver enumeration
 *  - renderer: 2D drawing ([createRenderer])
 *  - events: [pollEvent], [waitEvent], [pumpEvents]
 *  - timer: [getTicks], [delay], high-resolution performance counters
 *  - audio: driver/device enumeration
 *  - misc: [showSimpleMessageBox]
 *
 * On the JVM the bindings delegate to the SDL3 bindings bundled with LWJGL;
 * on native platforms they delegate to the SDL3 static library embedded in
 * the published klib (see the sdl.def cinterop file).
 */
expect object SDL {

    /**
     * Marks the calling thread as the SDL main thread. On macOS/iOS this
     * must be called before [init] with video, otherwise SDL reports that
     * the application did not initialize properly. No-op on the JVM.
     */
    fun setMainReady()

    /**
     * Initializes the given subsystems. Returns `false` on failure, in
     * which case [error] describes the problem.
     */
    fun init(flags: Int): Boolean

    /** Initializes a subsystem that was not initialized yet. */
    fun initSubSystem(flags: Int): Boolean

    /** Shuts down initialized subsystems without uninitializing SDL itself. */
    fun quitSubSystem(flags: Int)

    /** Returns the currently initialized subsystems. */
    fun wasInit(flags: Int): Int

    /** Shuts down all initialized subsystems and SDL itself. */
    fun quit()

    /** The last SDL error message, or `null` if there is none. */
    fun error(): String?

    /** Clears the last SDL error message. */
    fun clearError()

    /** Sets a custom SDL error message; returns `false` on failure. */
    fun setError(message: String): Boolean

    /** The version of the underlying SDL library. */
    fun version(): SDLVersion

    /** The revision of the underlying SDL library, e.g. `SDL-3.2.22-...`. */
    fun revision(): String?

    /** Milliseconds elapsed since SDL initialization. */
    fun getTicks(): ULong

    /** High-resolution performance counter; divide by [performanceFrequency]. */
    fun performanceCounter(): ULong

    /** Frequency of the high-resolution performance counter. */
    fun performanceFrequency(): ULong

    /** Blocks the calling thread for at least `ms` milliseconds. */
    fun delay(ms: Int)

    /**
     * Polls the event queue. Returns the next pending event, or `null` if
     * the queue is empty. Call this frequently from the SDL main thread.
     */
    fun pollEvent(): SDLEvent?

    /** Blocks until an event is available and returns it. */
    fun waitEvent(): SDLEvent?

    /** Pumps the platform event loop without blocking. */
    fun pumpEvents()

    /**
     * Creates a window and returns it, or throws an exception describing
     * the SDL error. [SDLWindowFlags] describes [flags].
     */
    fun createWindow(title: String, width: Int, height: Int, flags: ULong = 0u): SDLWindow

    /**
     * Creates a renderer for [window] and returns it, or throws an
     * exception describing the SDL error. [SDLRendererFlags] describes
     * [flags].
     */
    fun createRenderer(window: SDLWindow, name: String? = null, flags: Int = 0): SDLRenderer

    /** Sets a hint (e.g. `SDL_HINT_VIDEO_DRIVER`); returns `false` on failure. */
    fun setHint(name: String, value: String): Boolean

    /** The current value of a hint, or `null`. */
    fun getHint(name: String): String?

    /** The current clipboard text, or `null`. */
    fun getClipboardText(): String?

    /** Sets the clipboard text; returns `false` on failure. */
    fun setClipboardText(text: String): Boolean

    /** The number of compiled-in video drivers. */
    fun getNumVideoDrivers(): Int

    /** The name of the video driver at [index]. */
    fun getVideoDriver(index: Int): String?

    /** The name of the active video driver, or `null`. */
    fun getCurrentVideoDriver(): String?

    /** The number of compiled-in audio drivers. */
    fun getNumAudioDrivers(): Int

    /** The name of the audio driver at [index]. */
    fun getAudioDriver(index: Int): String?

    /** The name of the active audio driver, or `null`. */
    fun getCurrentAudioDriver(): String?

    /** Whether text input is active for the window with the given ID. */
    fun textInputActive(windowId: Int): Boolean

    /** Starts text input for the given window ID; returns `false` on failure. */
    fun startTextInput(windowId: Int): Boolean

    /** Stops text input; returns `false` on failure. */
    fun stopTextInput(windowId: Int): Boolean

    /** Shows a simple modal message box; returns `false` on failure. */
    fun showSimpleMessageBox(title: String, message: String): Boolean
}
