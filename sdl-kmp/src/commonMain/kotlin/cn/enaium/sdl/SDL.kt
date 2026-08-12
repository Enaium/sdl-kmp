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

    /** The raw SDL handle address, or 0 after [close]. On native, [cn.enaium.sdl.nativePtr] converts it to the typed pointer. */
    val ptr: Long

    /** The window title. */
    var title: String

    /** The window size in screen coordinates. */
    var size: SDLPoint

    /** The window position in screen coordinates. */
    var position: SDLPoint

    /** The window size in pixels (backing scale applied). */
    val sizeInPixels: SDLPoint

    /** The window flags (see [SDLWindowFlags]). */
    val flags: ULong

    /** The display this window is on. */
    val displayId: Int

    /** The window opacity in [0, 1]. */
    var opacity: Float

    /** Whether the window is fullscreen. */
    var fullscreen: Boolean

    /** Whether the window has a border. */
    var bordered: Boolean

    /** Whether the window is resizable by the user. */
    var resizable: Boolean

    /** Whether the window stays on top of other windows. */
    var alwaysOnTop: Boolean

    /** Whether the window grabs mouse input. */
    var mouseGrab: Boolean

    /** Whether the window grabs keyboard input. */
    var keyboardGrab: Boolean

    /** Whether relative mouse mode is enabled for this window. */
    var relativeMouseMode: Boolean

    /** The window minimum size (null when not set). */
    var minimumSize: SDLPoint?

    /** The window maximum size (null when not set). */
    var maximumSize: SDLPoint?

    /** The window aspect ratio range (min, max), or null when not set. */
    var aspectRatio: SDLFloatPoint?

    fun show()
    fun hide()
    fun raise()
    fun maximize()
    fun minimize()
    fun restore()
    fun flash()

    /** The window's surface, if any (the surface is owned by the window). */
    val surface: SDLSurface?

    /** Sets the window icon from a surface. */
    fun setIcon(icon: SDLSurface): Boolean

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

    /** The raw SDL handle address, or 0 after [close]. On native, [cn.enaium.sdl.nativePtr] converts it to the typed pointer. */
    val ptr: Long

    /** The current drawing color. */
    var drawColor: SDLColor

    /** The size of the rendering target in pixels. */
    val outputSize: SDLPoint

    /** The current output size (respects the render target and viewport). */
    val currentOutputSize: SDLPoint

    /** The rendering viewport (null restores the full target). */
    var viewport: SDLRect?

    /** The clipping rectangle (null disables clipping). */
    var clipRect: SDLRect?

    /** The drawing scale. */
    var scale: SDLFloatPoint

    /** The drawing blend mode, see [SDLBlendMode]. */
    var blendMode: Int

    /** Whether VSync is enabled. */
    var vsync: Boolean

    /** The current render target texture, or null for the window. */
    var target: SDLTexture?

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

    /** Draws a point with the current [drawColor]. */
    fun drawPoint(x: Int, y: Int): Boolean

    /** Draws multiple points with the current [drawColor]. */
    fun drawPoints(points: List<SDLPoint>): Boolean

    /**
     * Creates a texture.
     * @param format a pixel format from [SDLPixelFormat]
     * @param access an access mode from [SDLTextureAccess]
     */
    fun createTexture(format: Int, access: Int, width: Int, height: Int): SDLTexture

    /** Creates a texture from [surface]. */
    fun createTextureFromSurface(surface: SDLSurface): SDLTexture

    /** Draws [texture] to the render target, optionally cropping with [src]. */
    fun renderTexture(texture: SDLTexture, src: SDLFRect? = null, dst: SDLFRect? = null): Boolean

    /** Draws [texture] rotated by [angle] degrees around [center]. */
    fun renderTextureRotated(
        texture: SDLTexture,
        src: SDLFRect? = null,
        dst: SDLFRect? = null,
        angle: Double,
        center: SDLFloatPoint? = null,
        flip: Int = SDLFlipMode.NONE,
    ): Boolean

    /** Draws a 9-grid (sliced) portion of [texture]. */
    fun renderTexture9Grid(
        texture: SDLTexture,
        src: SDLFRect,
        leftWidth: Float,
        rightWidth: Float,
        topHeight: Float,
        bottomHeight: Float,
        scale: Float,
        dst: SDLFRect,
    ): Boolean

    /** Draws [vertices] with optional [indices]; [texture] may be null. */
    fun renderGeometry(
        texture: SDLTexture?,
        vertices: List<SDLVertex>,
        indices: IntArray? = null,
    ): Boolean

    /** Reads the current render target into a new surface (null on failure). */
    fun renderReadPixels(rect: SDLRect?): SDLSurface?

    /** Sets the logical presentation size and mode (see [SDLLogicalPresentation]). */
    fun setLogicalPresentation(width: Int, height: Int, mode: Int): Boolean

    /** The logical presentation rect in renderer coordinates, or null when disabled. */
    val logicalPresentationRect: SDLFRect?

    /** Releases the underlying SDL renderer. */
    override fun close()
}

/** A texture bound to an [SDLRenderer]. */
interface SDLTexture : AutoCloseable {

    /** The pixel format, see [SDLPixelFormat]. */
    val format: Int

    /** The access mode, see [SDLTextureAccess]. */
    val access: Int

    /** The raw SDL handle address, or 0 after [close]. On native, [cn.enaium.sdl.nativePtr] converts it to the typed pointer. */
    val ptr: Long

    /** The texture size in pixels. */
    val size: SDLFloatPoint

    /** The color modulation (default 255,255,255). */
    var colorMod: SDLColor

    /** The alpha modulation (default 255). */
    var alphaMod: Int

    /** The blend mode, see [SDLBlendMode]. */
    var blendMode: Int

    /** The scale mode, see [SDLScaleMode]. */
    var scaleMode: Int

    /** Updates a rectangle (null = whole texture) of the texture with [pixels]. */
    fun update(rect: SDLRect?, pixels: ByteArray, pitch: Int): Boolean

    /** Locks the texture for write access; returns null on failure. */
    fun lock(rect: SDLRect?): SDLTextureLock?

    /** Unlocks the texture (must be called after a successful [lock]). */
    fun unlock()

    /** Releases the texture. */
    override fun close()
}

/** The locked pixel data of a [SDLTexture]. */
data class SDLTextureLock(val pixels: ByteArray, val pitch: Int)

/** Flip modes (values match SDL3's SDL_FlipMode). */
object SDLFlipMode {
    const val NONE = 0
    const val HORIZONTAL = 1
    const val VERTICAL = 2
}

/** Logical presentation modes (values match SDL3's SDL_RendererLogicalPresentation). */
object SDLLogicalPresentation {
    const val DISABLED = 0
    const val STRETCH = 1
    const val LETTERBOX = 2
    const val OVERSCAN = 3
    const val INTEGER_SCALE = 4
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

    /**
     * Polls the event queue and returns the raw `SDL_Event`, or `null` if
     * empty. The returned event owns its storage; call [SDLEventRaw.close]
     * (or `use`) once you are done reading it.
     */
    fun pollEventRaw(): SDLEventRaw?

    /**
     * Blocks until an event is available and returns the raw `SDL_Event`.
     * The returned event owns its storage; call [SDLEventRaw.close] (or
     * `use`) once you are done reading it.
     */
    fun waitEventRaw(): SDLEventRaw?

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

    // ==================== displays ====================

    /** The number of connected displays. */
    val numDisplays: Int

    /** The display at [index]. */
    fun getDisplay(index: Int): SDLDisplay

    /** The primary display. */
    fun getPrimaryDisplay(): SDLDisplay

    // ==================== renderer drivers ====================

    /** The number of compiled-in render drivers. */
    val numRenderDrivers: Int

    /** The name of the render driver at [index]. */
    fun getRenderDriver(index: Int): String?

    /** Creates a window and a renderer for it in one call. */
    fun createWindowAndRenderer(
        title: String,
        width: Int,
        height: Int,
        flags: ULong = 0u,
    ): Pair<SDLWindow, SDLRenderer>

    // ==================== pixels ====================

    /** The name of a pixel [format] (see [SDLPixelFormat]). */
    fun getPixelFormatName(format: Int): String?

    /** Maps RGB to a pixel value in [format]. */
    fun mapRGB(format: Int, r: Int, g: Int, b: Int): Int

    /** Maps RGBA to a pixel value in [format]. */
    fun mapRGBA(format: Int, r: Int, g: Int, b: Int, a: Int): Int

    /** Decodes a pixel value in [format] to RGBA. */
    fun getRGBA(format: Int, pixel: Int): SDLColor

    // ==================== surfaces ====================

    /** Creates an empty surface; [format] is from [SDLPixelFormat]. */
    fun createSurface(width: Int, height: Int, format: Int): SDLSurface

    /** Loads a BMP file into a surface. */
    fun loadBMP(path: String): SDLSurface

    // ==================== audio ====================

    /** IDs of all playback (non-capture) audio devices. */
    val audioPlaybackDevices: List<Int>

    /** IDs of all recording (capture) audio devices. */
    val audioRecordingDevices: List<Int>

    /** The name of the audio device with the given [deviceId], or null. */
    fun getAudioDeviceName(deviceId: Int): String?

    /** Opens an audio device with the given [spec]. */
    fun openAudioDevice(deviceId: Int, spec: SDLAudioSpec): SDLAudioDevice

    /** Opens a device with an attached stream; the device is started automatically. */
    fun openAudioDeviceStream(deviceId: Int, spec: SDLAudioSpec): SDLAudioStream

    /** Creates a stream converting [srcSpec] into [dstSpec]. */
    fun createAudioStream(srcSpec: SDLAudioSpec, dstSpec: SDLAudioSpec): SDLAudioStream

    /** Pauses playback on the device with [deviceId]. */
    fun pauseAudioDevice(deviceId: Int)

    /** Resumes playback on the device with [deviceId]. */
    fun resumeAudioDevice(deviceId: Int)

    /** Whether the device with [deviceId] is currently paused. */
    fun isAudioDevicePaused(deviceId: Int): Boolean

    /** Loads a WAV file, or null on failure. */
    fun loadWAV(path: String): SDLAudioData?

    // ==================== input focus ====================

    /** The window that has keyboard focus, or null. */
    val keyboardFocusWindowId: Int?

    /** The window that has mouse focus, or null. */
    val mouseFocusWindowId: Int?

    // ==================== touch ====================

    /** IDs of all touch devices. */
    val touchDevices: List<Int>

    /** The name of the touch device with [touchId], or null. */
    fun getTouchDeviceName(touchId: Int): String?

    /** The type of the touch device with [touchId] (see [SDLTouchDeviceType]). */
    fun getTouchDeviceType(touchId: Int): Int

    /** All active fingers of the touch device with [touchId]. */
    fun getTouchFingers(touchId: Int): List<SDLTouchFinger>

    // ==================== event filter / watch ====================

    /**
     * Registers [filter] as an event watch; it is called for every event
     * pushed onto the queue. Returning `false` drops the event.
     */
    fun addEventWatch(filter: (SDLEventRaw) -> Boolean): Boolean

    /** Removes a previously registered [filter] event watch. */
    fun removeEventWatch(filter: (SDLEventRaw) -> Boolean)

    /** Enables or disables events of [type]. */
    fun setEventEnabled(type: Int, enabled: Boolean)

    /** Whether events of [type] are enabled. */
    fun eventEnabled(type: Int): Boolean

    /** Removes all events in the range [minType]..[maxType] from the queue. */
    fun flushEvents(minType: Int, maxType: Int)

    /** Pushes [event] onto the queue; returns `false` on failure. */
    fun pushEvent(event: SDLEventRaw): Boolean

    // ==================== file dialogs ====================

    /** Shows an open-file dialog; [callback] receives the chosen paths (empty on cancel). */
    fun showOpenFileDialog(
        windowId: Int?,
        filters: List<SDLDialogFileFilter>,
        defaultLocation: String?,
        allowMultiple: Boolean,
        callback: (List<String>) -> Unit,
    )

    /** Shows a save-file dialog; [callback] receives the chosen path or an empty list on cancel. */
    fun showSaveFileDialog(
        windowId: Int?,
        filters: List<SDLDialogFileFilter>,
        defaultLocation: String?,
        callback: (List<String>) -> Unit,
    )

    /** Shows a folder-picker dialog; [callback] receives the chosen path or an empty list on cancel. */
    fun showFolderDialog(
        windowId: Int?,
        defaultLocation: String?,
        allowMultiple: Boolean,
        callback: (List<String>) -> Unit,
    )

    // ==================== keyboard ====================

    /** The current keyboard state, indexed by scancode. */
    val keyboardState: ByteArray

    /** The current key modifiers, see [SDLKeymod]. */
    val modState: Int

    /** Sets the current key modifiers. */
    fun setModState(modState: Int)

    /** The keycode for a [scancode]. */
    fun getKeyFromScancode(scancode: Int): Int

    /** The scancode for a [keycode]. */
    fun getScancodeFromKey(keycode: Int): Int

    /** The name of a [keycode], or null. */
    fun getKeyName(keycode: Int): String?

    /** The name of a [scancode], or null. */
    fun getScancodeName(scancode: Int): String?

    // ==================== mouse ====================

    /** The mouse position and button state (window-relative). */
    val mouseState: SDLMouseState

    /** The mouse position and button state (global). */
    val globalMouseState: SDLMouseState

    /** Warps the mouse to ([x], [y]) inside the window with [windowId]. */
    fun warpMouseInWindow(windowId: Int, x: Float, y: Float)

    /** Captures the mouse so events keep coming when the cursor leaves the window. */
    fun captureMouse(enabled: Boolean): Boolean

    /** Toggles the cursor visibility; returns true when it is shown. */
    fun showCursor(): Boolean

    // ==================== joystick / gamepad ====================

    /** IDs of all connected joysticks. */
    val joysticks: List<Int>

    /** Opens the joystick with [id]. */
    fun openJoystick(id: Int): SDLJoystick

    /** IDs of all connected gamepads. */
    val gamepads: List<Int>

    /** Opens the gamepad with [id]. */
    fun openGamepad(id: Int): SDLGamepad

    // ==================== filesystem / misc ====================

    /** The directory where the application was started, or null. */
    val basePath: String?

    /** The preferred directory for the application's files, or null. */
    fun getPrefPath(orgName: String, appName: String): String?

    /** The user's [folder] (see [SDLFolder]), or null. */
    fun getUserFolder(folder: Int): String?

    /** Creates a directory (and parents); returns `false` on failure. */
    fun createDirectory(path: String): Boolean

    /** Removes a file or directory; returns `false` on failure. */
    fun removePath(path: String): Boolean

    /** Renames a file or directory; returns `false` on failure. */
    fun renamePath(oldPath: String, newPath: String): Boolean

    /** The current battery state, see [SDLPowerState]. */
    val powerInfo: SDLPowerInfo

    /** Opens [url] in the default application; returns `false` on failure. */
    fun openURL(url: String): Boolean

    /** Whether the clipboard contains text. */
    val hasClipboardText: Boolean

    /** The boolean value of a hint, or [defaultValue]. */
    fun getHintBoolean(name: String, defaultValue: Boolean): Boolean

    /** Shows a message box with custom [buttons]; returns the id of the clicked button. */
    fun showMessageBox(
        flags: Int,
        title: String,
        message: String,
        buttons: List<SDLMessageBoxButton>,
    ): Int

    // ==================== OpenGL ====================

    /** Loads the OpenGL library; [path] is optional. */
    fun glLoadLibrary(path: String? = null): Boolean

    /** Unloads the OpenGL library. */
    fun glUnloadLibrary()

    /** The address of an OpenGL function, or 0 if not found. */
    fun glGetProcAddress(proc: String): ULong

    /** Whether the given OpenGL extension is supported. */
    fun glExtensionSupported(extension: String): Boolean

    /** Resets all OpenGL context attributes to their defaults. */
    fun glResetAttributes()

    /** Sets an OpenGL context attribute (see [SDLGLAttribute]). */
    fun glSetAttribute(attr: Int, value: Int): Boolean

    /** The value of an OpenGL context attribute, or null. */
    fun glGetAttribute(attr: Int): Int?

    /** Creates an OpenGL context for the window with [windowId]; returns 0 on failure. */
    fun glCreateContext(windowId: Int): ULong

    /** Makes [context] current for the window with [windowId]. */
    fun glMakeCurrent(windowId: Int, context: ULong): Boolean

    /** The window of the current OpenGL context, or null. */
    val glCurrentWindow: Int?

    /** The current OpenGL context, or 0. */
    val glCurrentContext: ULong

    /** Sets the swap interval (0 = immediate, 1 = vsync). */
    fun glSetSwapInterval(interval: Int): Boolean

    /** The current swap interval, or null. */
    val glSwapInterval: Int?

    /** Swaps the buffers of the window with [windowId]. */
    fun glSwapWindow(windowId: Int): Boolean

    /** Destroys an OpenGL context. */
    fun glDestroyContext(context: ULong)

    // ==================== Vulkan ====================

    /** Loads the Vulkan library; [path] is optional. */
    fun vulkanLoadLibrary(path: String? = null): Boolean

    /** Unloads the Vulkan library. */
    fun vulkanUnloadLibrary()

    /** The address of vkGetInstanceProcAddr, or 0. */
    val vulkanGetVkGetInstanceProcAddr: ULong

    /** The platform-specific Vulkan instance extensions. */
    val vulkanInstanceExtensions: List<String>

    /**
     * Creates a Vulkan surface for the window with [windowId], returning the
     * VkSurfaceKHR handle, or 0 on failure.
     */
    fun vulkanCreateSurface(windowId: Int, instance: ULong): ULong

    /** Destroys a Vulkan surface. */
    fun vulkanDestroySurface(instance: ULong, surface: ULong)

    /** Whether presentation is supported for [physicalDevice] and [queueFamilyIndex]. */
    fun vulkanGetPresentationSupport(instance: ULong, physicalDevice: ULong, queueFamilyIndex: Int): Boolean
}
