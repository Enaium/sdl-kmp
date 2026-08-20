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

import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** The host byte order via the JVM's native order. */
internal actual val hostIsLittleEndian: Boolean = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN

// =========================================================================
// JVM callback maps (event watch / file dialogs / log output)
//
// The C side (jni_bridge.cpp) keeps a native callback per id and dispatches
// back through the static bridge methods on Jni (onEventWatch /
// onDialogCallback / onLogOutput), which look the Kotlin callback up here.
// =========================================================================

private const val DIALOG_OPEN_FILE = 1
private const val DIALOG_SAVE_FILE = 2
private const val DIALOG_OPEN_FOLDER = 3

internal val eventWatchCallbacks = ConcurrentHashMap<Long, (SDLEventRaw) -> Boolean>()
internal val eventWatchNextId = AtomicLong(0)

internal val dialogCallbacks = ConcurrentHashMap<Long, (List<String>) -> Unit>()
internal val dialogNextId = AtomicLong(0)

internal val logOutputCallbacks = ConcurrentHashMap<Long, SDLLogOutput>()
internal val logOutputNextId = AtomicLong(0)

/** An event owned by SDL's queue; closing it does nothing. */
internal class BorrowedJvmEventRaw(private val address: Long) : SDLEventRaw {

    override val ptr: Long
        get() = address

    override fun close() {
        // The event is owned by SDL's event queue; nothing to free.
    }
}

// =========================================================================
// JVM (JNI SDL3 bindings) implementations
// =========================================================================

private fun SDLEventRaw.toSDLEvent(): SDLEvent {
    val event = this
    val type = Jni.eventType(event.ptr)
    val timestamp = Jni.eventTimestamp(event.ptr).toULong()
    return when (type) {
        SDLEventType.QUIT -> SDLEvent.Quit(timestamp)
        in SDLEventType.WINDOW_FIRST until SDLEventType.KEY_FIRST ->
            SDLEvent.Window(
                timestamp = timestamp,
                windowId = Jni.eventWindowWindowID(event.ptr),
                type = type,
                data1 = Jni.eventWindowData1(event.ptr),
                data2 = Jni.eventWindowData2(event.ptr),
            )
        SDLEventType.KEY_DOWN, SDLEventType.KEY_UP ->
            SDLEvent.Key(
                timestamp = timestamp,
                windowId = Jni.eventKeyWindowID(event.ptr),
                down = Jni.eventKeyState(event.ptr) != 0,
                repeat = Jni.eventKeyRepeat(event.ptr),
                keycode = Jni.eventKeyKeycode(event.ptr),
                scancode = Jni.eventKeyScancode(event.ptr),
                modifiers = Jni.eventKeyMod(event.ptr),
            )
        SDLEventType.TEXT_INPUT ->
            SDLEvent.TextInput(
                timestamp = timestamp,
                windowId = Jni.eventTextWindowID(event.ptr),
                text = Jni.eventTextText(event.ptr),
            )
        SDLEventType.MOUSE_MOTION ->
            SDLEvent.MouseMotion(
                timestamp = timestamp,
                windowId = Jni.eventMotionWindowID(event.ptr),
                x = Jni.eventMotionX(event.ptr),
                y = Jni.eventMotionY(event.ptr),
                dx = Jni.eventMotionXrel(event.ptr),
                dy = Jni.eventMotionYrel(event.ptr),
            )
        SDLEventType.MOUSE_BUTTON_DOWN, SDLEventType.MOUSE_BUTTON_UP ->
            SDLEvent.MouseButton(
                timestamp = timestamp,
                windowId = Jni.eventButtonWindowID(event.ptr),
                down = Jni.eventButtonState(event.ptr) != 0,
                button = Jni.eventButtonButton(event.ptr),
                clicks = Jni.eventButtonClicks(event.ptr),
                x = Jni.eventButtonX(event.ptr),
                y = Jni.eventButtonY(event.ptr),
            )
        SDLEventType.MOUSE_WHEEL ->
            SDLEvent.MouseWheel(
                timestamp = timestamp,
                windowId = Jni.eventWheelWindowID(event.ptr),
                x = Jni.eventWheelX(event.ptr),
                y = Jni.eventWheelY(event.ptr),
                direction = Jni.eventWheelDirection(event.ptr),
            )
        else -> SDLEvent.Unknown(timestamp = type.toULong(), type = type)
    }
}

// =========================================================================
// JVM (JNI) raw event
// =========================================================================

internal class JvmSDLEventRaw internal constructor(ptr: Long) : SDLEventRaw {

    private var event: Long = ptr

    override val ptr: Long
        get() = event

    override fun close() {
        val e = event
        if (e == 0L) return
        event = 0L
        Jni.eventFree(e)
    }
}

// =========================================================================
// JVM (JNI) window
// =========================================================================

internal class JvmSDLWindow internal constructor(ptr: Long) : SDLWindow {

    override var ptr: Long = ptr
        private set

    override val id: Int
        get() = Jni.getWindowID(ptr)

    override var title: String
        get() = Jni.getWindowTitle(ptr) ?: ""
        set(value) {
            Jni.setWindowTitle(ptr, value)
        }

    override var size: SDLPoint
        get() {
            val s = Jni.getWindowSize(ptr)
            return SDLPoint(s[0], s[1])
        }
        set(value) {
            Jni.setWindowSize(ptr, value.x, value.y)
        }

    override val flags: ULong
        get() = Jni.getWindowFlags(ptr).toULong()

    override var position: SDLPoint
        get() {
            val p = Jni.getWindowPosition(ptr)
            return SDLPoint(p[0], p[1])
        }
        set(value) {
            Jni.setWindowPosition(ptr, value.x, value.y)
        }

    override val sizeInPixels: SDLPoint
        get() {
            val s = Jni.getWindowSizeInPixels(ptr)
            return SDLPoint(s[0], s[1])
        }

    override val displayId: Int
        get() = Jni.getDisplayForWindow(ptr)

    override var opacity: Float
        get() = Jni.getWindowOpacity(ptr)
        set(value) {
            Jni.setWindowOpacity(ptr, value)
        }

    override var fullscreen: Boolean
        get() = (flags and SDLWindowFlags.FULLSCREEN) != 0uL
        set(value) {
            Jni.setWindowFullscreen(ptr, value)
        }

    override var bordered: Boolean
        get() = (flags and SDLWindowFlags.BORDERLESS) == 0uL
        set(value) {
            Jni.setWindowBordered(ptr, value)
        }

    override var resizable: Boolean
        get() = (flags and SDLWindowFlags.RESIZABLE) != 0uL
        set(value) {
            Jni.setWindowResizable(ptr, value)
        }

    override var alwaysOnTop: Boolean
        get() = (flags and SDLWindowFlags.ALWAYS_ON_TOP) != 0uL
        set(value) {
            Jni.setWindowAlwaysOnTop(ptr, value)
        }

    override var mouseGrab: Boolean
        get() = Jni.getWindowMouseGrab(ptr)
        set(value) {
            Jni.setWindowMouseGrab(ptr, value)
        }

    override var keyboardGrab: Boolean
        get() = Jni.getWindowKeyboardGrab(ptr)
        set(value) {
            Jni.setWindowKeyboardGrab(ptr, value)
        }

    override var relativeMouseMode: Boolean
        get() = Jni.getWindowRelativeMouseMode(ptr)
        set(value) {
            Jni.setWindowRelativeMouseMode(ptr, value)
        }

    override var minimumSize: SDLPoint?
        get() {
            val s = Jni.getWindowMinimumSize(ptr)
            return SDLPoint(s[0], s[1])
        }
        set(value) {
            Jni.setWindowMinimumSize(ptr, value?.x ?: 0, value?.y ?: 0)
        }

    override var maximumSize: SDLPoint?
        get() {
            val s = Jni.getWindowMaximumSize(ptr)
            return SDLPoint(s[0], s[1])
        }
        set(value) {
            Jni.setWindowMaximumSize(ptr, value?.x ?: 0, value?.y ?: 0)
        }

    override fun maximize() {
        Jni.maximizeWindow(ptr)
    }

    override fun minimize() {
        Jni.minimizeWindow(ptr)
    }

    override fun restore() {
        Jni.restoreWindow(ptr)
    }

    override fun flash() {
        Jni.flashWindow(ptr)
    }

    override val surface: cn.enaium.sdl.SDLSurface?
        get() {
            val surface = Jni.getWindowSurface(ptr)
            if (surface == 0L) return null
            return JvmSDLSurface(surface, owned = false)
        }

    override fun setIcon(icon: cn.enaium.sdl.SDLSurface): Boolean {
        val jvmIcon = (icon as? JvmSDLSurface)?.ptr
            ?: throw IllegalArgumentException("icon is not a JVM SDL surface")
        return Jni.setWindowIcon(ptr, jvmIcon)
    }

    override var aspectRatio: SDLFloatPoint?
        get() = Jni.getWindowAspectRatio(ptr)?.let { SDLFloatPoint(it[0], it[1]) }
        set(value) {
            Jni.setWindowAspectRatio(ptr, value?.x ?: 0f, value?.y ?: 0f)
        }

    override fun show() {
        Jni.showWindow(ptr)
    }

    override fun hide() {
        Jni.hideWindow(ptr)
    }

    override fun raise() {
        Jni.raiseWindow(ptr)
    }

    override fun close() {
        if (ptr == 0L) return
        Jni.destroyWindow(ptr)
        ptr = 0L
    }
}

// =========================================================================
// JVM (JNI) renderer
// =========================================================================

internal class JvmSDLRenderer internal constructor(ptr: Long) : SDLRenderer {

    override var ptr: Long = ptr
        private set

    override val name: String?
        get() = Jni.getRendererName(ptr)

    override var drawColor: SDLColor
        get() {
            val c = Jni.getRenderDrawColor(ptr)
            return SDLColor(c[0], c[1], c[2], c[3])
        }
        set(value) {
            Jni.setRenderDrawColor(ptr, value.r, value.g, value.b, value.a)
        }

    override val outputSize: SDLPoint
        get() {
            val s = Jni.getRenderOutputSize(ptr)
            return SDLPoint(s[0], s[1])
        }

    override val currentOutputSize: SDLPoint
        get() {
            val s = Jni.getCurrentRenderOutputSize(ptr)
            return SDLPoint(s[0], s[1])
        }

    override var viewport: SDLRect?
        get() {
            val r = Jni.getRenderViewport(ptr)
            return SDLRect(r[0], r[1], r[2], r[3])
        }
        set(value) {
            Jni.setRenderViewport(ptr, value?.let { intArrayOf(it.x, it.y, it.width, it.height) })
        }

    override var clipRect: SDLRect?
        get() {
            val r = Jni.getRenderClipRect(ptr)
            return SDLRect(r[0], r[1], r[2], r[3])
        }
        set(value) {
            Jni.setRenderClipRect(ptr, value?.let { intArrayOf(it.x, it.y, it.width, it.height) })
        }

    override var scale: SDLFloatPoint
        get() {
            val s = Jni.getRenderScale(ptr)
            return SDLFloatPoint(s[0], s[1])
        }
        set(value) {
            Jni.setRenderScale(ptr, value.x, value.y)
        }

    override var blendMode: Int
        get() = Jni.getRenderDrawBlendMode(ptr)
        set(value) {
            Jni.setRenderDrawBlendMode(ptr, value)
        }

    override var vsync: Boolean
        get() = Jni.getRenderVSync(ptr) != 0
        set(value) {
            Jni.setRenderVSync(ptr, if (value) 1 else 0)
        }

    override var target: SDLTexture?
        get() {
            val texture = Jni.getRenderTarget(ptr)
            if (texture == 0L) return null
            return JvmSDLTexture(texture, this)
        }
        set(value) {
            Jni.setRenderTarget(ptr, (value as? JvmSDLTexture)?.ptr ?: 0L)
        }

    override fun clear(): Boolean = Jni.renderClear(ptr)

    override fun present() {
        Jni.renderPresent(ptr)
    }

    override fun fillRect(rect: SDLRect): Boolean =
        Jni.renderFillRect(ptr, rect.x.toFloat(), rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat())

    override fun drawRect(rect: SDLRect): Boolean =
        Jni.renderRect(ptr, rect.x.toFloat(), rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat())

    override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int): Boolean =
        Jni.renderLine(ptr, x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())

    override fun drawPoint(x: Int, y: Int): Boolean =
        Jni.renderPoint(ptr, x.toFloat(), y.toFloat())

    override fun drawPoints(points: List<SDLPoint>): Boolean {
        val buffer = FloatArray(points.size * 2)
        for (i in points.indices) {
            buffer[i * 2] = points[i].x.toFloat()
            buffer[i * 2 + 1] = points[i].y.toFloat()
        }
        return Jni.renderPoints(ptr, buffer)
    }

    override fun createTexture(format: Int, access: Int, width: Int, height: Int): SDLTexture {
        val texture = Jni.createTexture(ptr, format, access, width, height)
        check(texture != 0L) { "SDL_CreateTexture failed: ${SDL.error()}" }
        return JvmSDLTexture(texture, this)
    }

    override fun createTextureFromSurface(surface: SDLSurface): SDLTexture {
        val jvmSurface = (surface as? JvmSDLSurface)?.ptr
            ?: throw IllegalArgumentException("surface is not a JVM SDL surface")
        val texture = Jni.createTextureFromSurface(ptr, jvmSurface)
        check(texture != 0L) { "SDL_CreateTextureFromSurface failed: ${SDL.error()}" }
        return JvmSDLTexture(texture, this)
    }

    override fun renderTexture(texture: SDLTexture, src: SDLFRect?, dst: SDLFRect?): Boolean {
        val t = (texture as? JvmSDLTexture)?.ptr
            ?: throw IllegalArgumentException("texture is not a JVM SDL texture")
        return Jni.renderTexture(
            ptr,
            t,
            src?.let { floatArrayOf(it.x, it.y, it.width, it.height) },
            dst?.let { floatArrayOf(it.x, it.y, it.width, it.height) },
        )
    }

    override fun renderTextureRotated(
        texture: SDLTexture,
        src: SDLFRect?,
        dst: SDLFRect?,
        angle: Double,
        center: SDLFloatPoint?,
        flip: Int,
    ): Boolean {
        val t = (texture as? JvmSDLTexture)?.ptr
            ?: throw IllegalArgumentException("texture is not a JVM SDL texture")
        return Jni.renderTextureRotated(
            ptr,
            t,
            src?.let { floatArrayOf(it.x, it.y, it.width, it.height) },
            dst?.let { floatArrayOf(it.x, it.y, it.width, it.height) },
            angle,
            center?.let { floatArrayOf(it.x, it.y) },
            flip,
        )
    }

    override fun renderTexture9Grid(
        texture: SDLTexture,
        src: SDLFRect,
        leftWidth: Float,
        rightWidth: Float,
        topHeight: Float,
        bottomHeight: Float,
        scale: Float,
        dst: SDLFRect,
    ): Boolean {
        val t = (texture as? JvmSDLTexture)?.ptr
            ?: throw IllegalArgumentException("texture is not a JVM SDL texture")
        return Jni.renderTexture9Grid(
            ptr,
            t,
            src.x, src.y, src.width, src.height,
            leftWidth, rightWidth, topHeight, bottomHeight, scale,
            dst.x, dst.y, dst.width, dst.height,
        )
    }

    override fun renderGeometry(texture: SDLTexture?, vertices: List<SDLVertex>, indices: IntArray?): Boolean {
        val t = (texture as? JvmSDLTexture)?.ptr ?: 0L
        val positions = FloatArray(vertices.size * 2)
        val colors = FloatArray(vertices.size * 4)
        val texCoords = FloatArray(vertices.size * 2)
        for (i in vertices.indices) {
            val v = vertices[i]
            positions[i * 2] = v.position.x
            positions[i * 2 + 1] = v.position.y
            colors[i * 4] = v.color.r / 255f
            colors[i * 4 + 1] = v.color.g / 255f
            colors[i * 4 + 2] = v.color.b / 255f
            colors[i * 4 + 3] = v.color.a / 255f
            texCoords[i * 2] = v.texCoord.x
            texCoords[i * 2 + 1] = v.texCoord.y
        }
        return Jni.renderGeometry(ptr, t, positions, colors, texCoords, indices)
    }

    override fun renderReadPixels(rect: SDLRect?): SDLSurface? {
        val surface = Jni.renderReadPixels(ptr, rect?.let { intArrayOf(it.x, it.y, it.width, it.height) })
        if (surface == 0L) return null
        return JvmSDLSurface(surface, owned = true)
    }

    override fun setLogicalPresentation(width: Int, height: Int, mode: Int): Boolean =
        Jni.setRenderLogicalPresentation(ptr, width, height, mode)

    override val logicalPresentationRect: SDLFRect?
        get() = Jni.getRenderLogicalPresentationRect(ptr)?.let { SDLFRect(it[0], it[1], it[2], it[3]) }

    override fun close() {
        if (ptr == 0L) return
        Jni.destroyRenderer(ptr)
        ptr = 0L
    }
}

// =========================================================================
// actual implementations
// =========================================================================

actual object SDL {

    actual fun setMainReady() {
        Jni.setMainReady()
    }

    actual fun init(flags: Int): Boolean = Jni.init(flags)

    actual fun initSubSystem(flags: Int): Boolean = Jni.initSubSystem(flags)

    actual fun quitSubSystem(flags: Int) {
        Jni.quitSubSystem(flags)
    }

    actual fun wasInit(flags: Int): Int = Jni.wasInit(flags)

    actual fun quit() {
        Jni.quit()
    }

    actual fun error(): String? = Jni.getError()?.takeIf { it.isNotEmpty() }

    actual fun clearError() {
        Jni.clearError()
    }

    actual fun setError(message: String): Boolean = Jni.setError(message)

    actual fun version(): SDLVersion {
        val num = Jni.getVersion()
        return SDLVersion(
            major = num / 1000000,
            minor = (num / 1000) % 1000,
            micro = num % 1000,
        )
    }

    actual fun revision(): String? = Jni.getRevision()

    actual fun getTicks(): ULong = Jni.getTicks().toULong()

    actual fun performanceCounter(): ULong = Jni.performanceCounter().toULong()

    actual fun performanceFrequency(): ULong = Jni.performanceFrequency().toULong()

    actual fun delay(ms: Int) {
        Jni.delay(ms)
    }

    actual fun pollEvent(): SDLEvent? {
        val event = Jni.eventAlloc()
        return try {
            if (Jni.pollEvent(event)) BorrowedJvmEventRaw(event).toSDLEvent() else null
        } finally {
            Jni.eventFree(event)
        }
    }

    actual fun waitEvent(): SDLEvent? {
        val event = Jni.eventAlloc()
        return try {
            if (Jni.waitEvent(event)) BorrowedJvmEventRaw(event).toSDLEvent() else null
        } finally {
            Jni.eventFree(event)
        }
    }

    actual fun pollEventRaw(): SDLEventRaw? {
        val event = Jni.eventAlloc()
        return if (Jni.pollEvent(event)) {
            JvmSDLEventRaw(event)
        } else {
            Jni.eventFree(event)
            null
        }
    }

    actual fun waitEventRaw(): SDLEventRaw? {
        val event = Jni.eventAlloc()
        return if (Jni.waitEvent(event)) {
            JvmSDLEventRaw(event)
        } else {
            Jni.eventFree(event)
            null
        }
    }

    actual fun pumpEvents() {
        Jni.pumpEvents()
    }

    actual fun createWindow(title: String, width: Int, height: Int, flags: ULong): SDLWindow {
        val ptr = Jni.createWindow(title, width, height, flags.toLong())
        check(ptr != 0L) { "SDL_CreateWindow failed: ${SDL.error()}" }
        return JvmSDLWindow(ptr)
    }

    actual fun createRenderer(window: SDLWindow, name: String?, flags: Int): SDLRenderer {
        val windowPtr = (window as? JvmSDLWindow)?.ptr ?: throw IllegalArgumentException(
            "window is not a JVM SDL window",
        )
        val ptr = Jni.createRenderer(windowPtr, name)
        check(ptr != 0L) { "SDL_CreateRenderer failed: ${SDL.error()}" }
        return JvmSDLRenderer(ptr)
    }

    actual fun setHint(name: String, value: String): Boolean = Jni.setHint(name, value)

    actual fun getHint(name: String): String? = Jni.getHint(name)

    actual fun getClipboardText(): String? = Jni.getClipboardText()

    actual fun setClipboardText(text: String): Boolean = Jni.setClipboardText(text)

    actual fun getNumVideoDrivers(): Int = Jni.getNumVideoDrivers()

    actual fun getVideoDriver(index: Int): String? = Jni.getVideoDriver(index)

    actual fun getCurrentVideoDriver(): String? = Jni.getCurrentVideoDriver()

    actual fun getNumAudioDrivers(): Int = Jni.getNumAudioDrivers()

    actual fun getAudioDriver(index: Int): String? = Jni.getAudioDriver(index)

    actual fun getCurrentAudioDriver(): String? = Jni.getCurrentAudioDriver()

    actual fun textInputActive(windowId: Int): Boolean {
        val window = Jni.getWindowFromID(windowId)
        if (window == 0L) return false
        return Jni.textInputActive(window)
    }

    actual fun startTextInput(windowId: Int): Boolean {
        val window = Jni.getWindowFromID(windowId)
        if (window == 0L) return false
        return Jni.startTextInput(window)
    }

    actual fun stopTextInput(windowId: Int): Boolean {
        val window = Jni.getWindowFromID(windowId)
        if (window == 0L) return false
        return Jni.stopTextInput(window)
    }

    actual fun showSimpleMessageBox(title: String, message: String): Boolean =
        Jni.showSimpleMessageBox(title, message)

    // ==================== displays ====================

    actual val numDisplays: Int
        get() = Jni.getDisplays()?.size ?: 0

    actual fun getDisplay(index: Int): SDLDisplay {
        val displays = Jni.getDisplays()
            ?: throw IllegalStateException("SDL_GetDisplays failed: ${SDL.error()}")
        require(index in 0 until displays.size) { "display index out of range: $index" }
        return JvmSDLDisplay(displays[index])
    }

    actual fun getPrimaryDisplay(): SDLDisplay =
        JvmSDLDisplay(Jni.getPrimaryDisplay())

    // ==================== renderer drivers ====================

    actual val numRenderDrivers: Int
        get() = Jni.getNumRenderDrivers()

    actual fun getRenderDriver(index: Int): String? =
        Jni.getRenderDriver(index)

    actual fun createWindowAndRenderer(
        title: String,
        width: Int,
        height: Int,
        flags: ULong,
    ): Pair<SDLWindow, SDLRenderer> {
        val result = Jni.createWindowAndRenderer(title, width, height, flags.toLong())
        check(result != null) { "SDL_CreateWindowAndRenderer failed: ${SDL.error()}" }
        return Pair(JvmSDLWindow(result[0]), JvmSDLRenderer(result[1]))
    }

    // ==================== pixels ====================

    actual fun getPixelFormatName(format: Int): String? =
        Jni.getPixelFormatName(format)

    actual fun mapRGB(format: Int, r: Int, g: Int, b: Int): Int =
        Jni.mapRGB(format, r, g, b)

    actual fun mapRGBA(format: Int, r: Int, g: Int, b: Int, a: Int): Int =
        Jni.mapRGBA(format, r, g, b, a)

    actual fun getRGBA(format: Int, pixel: Int): SDLColor {
        val c = Jni.getRGBA(format, pixel)
            ?: throw IllegalStateException("SDL_GetPixelFormatDetails failed")
        return SDLColor(c[0], c[1], c[2], c[3])
    }

    // ==================== surfaces ====================

    actual fun createSurface(width: Int, height: Int, format: Int): SDLSurface {
        val surface = Jni.createSurface(width, height, format)
        check(surface != 0L) { "SDL_CreateSurface failed: ${SDL.error()}" }
        return JvmSDLSurface(surface, owned = true)
    }

    actual fun loadBMP(path: String): SDLSurface {
        val surface = Jni.loadBMP(path)
        check(surface != 0L) { "SDL_LoadBMP failed: ${SDL.error()}" }
        return JvmSDLSurface(surface, owned = true)
    }

    // ==================== audio ====================

    actual val audioPlaybackDevices: List<Int>
        get() = Jni.audioPlaybackDevices()?.toList() ?: emptyList()

    actual val audioRecordingDevices: List<Int>
        get() = Jni.audioRecordingDevices()?.toList() ?: emptyList()

    actual fun getAudioDeviceName(deviceId: Int): String? =
        Jni.getAudioDeviceName(deviceId)

    actual fun openAudioDevice(deviceId: Int, spec: SDLAudioSpec): SDLAudioDevice {
        val id = Jni.openAudioDevice(deviceId, spec.format, spec.channels, spec.freq)
        check(id != 0) { "SDL_OpenAudioDevice failed: ${SDL.error()}" }
        return JvmSDLAudioDevice(id, spec, recording = false)
    }

    actual fun openAudioDeviceStream(deviceId: Int, spec: SDLAudioSpec): SDLAudioStream {
        val ptr = Jni.openAudioDeviceStream(deviceId, spec.format, spec.channels, spec.freq)
        check(ptr != 0L) { "SDL_OpenAudioDeviceStream failed: ${SDL.error()}" }
        return JvmSDLAudioStream(ptr)
    }

    actual fun createAudioStream(srcSpec: SDLAudioSpec, dstSpec: SDLAudioSpec): SDLAudioStream {
        val ptr = Jni.createAudioStream(
            srcSpec.format, srcSpec.channels, srcSpec.freq,
            dstSpec.format, dstSpec.channels, dstSpec.freq,
        )
        check(ptr != 0L) { "SDL_CreateAudioStream failed: ${SDL.error()}" }
        return JvmSDLAudioStream(ptr)
    }

    actual fun pauseAudioDevice(deviceId: Int) {
        Jni.pauseAudioDevice(deviceId)
    }

    actual fun resumeAudioDevice(deviceId: Int) {
        Jni.resumeAudioDevice(deviceId)
    }

    actual fun isAudioDevicePaused(deviceId: Int): Boolean =
        Jni.audioDevicePaused(deviceId)

    actual fun loadWAV(path: String): SDLAudioData? {
        val outSpec = IntArray(3)
        val data = Jni.loadWav(path, outSpec) ?: return null
        return SDLAudioData(
            spec = SDLAudioSpec(format = outSpec[0], channels = outSpec[1], freq = outSpec[2]),
            data = data,
        )
    }

    // ==================== input focus ====================

    actual val keyboardFocusWindowId: Int?
        get() = Jni.getKeyboardFocus().takeIf { it != 0L }?.let { Jni.getWindowID(it) }

    actual val mouseFocusWindowId: Int?
        get() = Jni.getMouseFocus().takeIf { it != 0L }?.let { Jni.getWindowID(it) }

    // ==================== touch ====================

    actual val touchDevices: List<Int>
        get() = Jni.getTouchDevices()?.toList() ?: emptyList()

    actual fun getTouchDeviceName(touchId: Int): String? =
        Jni.getTouchDeviceName(touchId)

    actual fun getTouchDeviceType(touchId: Int): Int =
        Jni.getTouchDeviceType(touchId)

    actual fun getTouchFingers(touchId: Int): List<SDLTouchFinger> {
        val data = Jni.getTouchFingers(touchId) ?: return emptyList()
        val result = mutableListOf<SDLTouchFinger>()
        var i = 0
        while (i < data.size) {
            result.add(
                SDLTouchFinger(
                    id = data[i].toULong(),
                    x = Float.fromBits(data[i + 1].toInt()),
                    y = Float.fromBits(data[i + 2].toInt()),
                    pressure = Float.fromBits(data[i + 3].toInt()),
                    down = true,
                ),
            )
            i += 4
        }
        return result
    }

    // ==================== event watch / filter ====================

    actual fun addEventWatch(filter: (SDLEventRaw) -> Boolean): Boolean {
        val id = eventWatchNextId.getAndIncrement()
        eventWatchCallbacks[id] = filter
        return if (Jni.addEventWatch(id)) {
            true
        } else {
            eventWatchCallbacks.remove(id)
            false
        }
    }

    actual fun removeEventWatch(filter: (SDLEventRaw) -> Boolean) {
        val id = eventWatchCallbacks.entries.firstOrNull { it.value === filter }?.key ?: return
        eventWatchCallbacks.remove(id)
        Jni.removeEventWatch(id)
    }

    actual fun setEventEnabled(type: Int, enabled: Boolean) {
        Jni.setEventEnabled(type, enabled)
    }

    actual fun eventEnabled(type: Int): Boolean =
        Jni.eventEnabled(type)

    actual fun flushEvents(minType: Int, maxType: Int) {
        Jni.flushEvents(minType, maxType)
    }

    actual fun pushEvent(event: SDLEventRaw): Boolean {
        val raw = event as? JvmSDLEventRaw
            ?: throw IllegalArgumentException("event is not a JVM SDL event")
        return Jni.pushEvent(raw.ptr)
    }

    // ==================== file dialogs ====================

    actual fun showOpenFileDialog(
        windowId: Int?,
        filters: List<SDLDialogFileFilter>,
        defaultLocation: String?,
        allowMultiple: Boolean,
        callback: (List<String>) -> Unit,
    ) = showJvmFileDialog(
        dialogType = DIALOG_OPEN_FILE,
        windowId = windowId,
        filters = filters,
        defaultLocation = defaultLocation,
        allowMultiple = allowMultiple,
        callback = callback,
    )

    actual fun showSaveFileDialog(
        windowId: Int?,
        filters: List<SDLDialogFileFilter>,
        defaultLocation: String?,
        callback: (List<String>) -> Unit,
    ) = showJvmFileDialog(
        dialogType = DIALOG_SAVE_FILE,
        windowId = windowId,
        filters = filters,
        defaultLocation = defaultLocation,
        allowMultiple = false,
        callback = callback,
    )

    actual fun showFolderDialog(
        windowId: Int?,
        defaultLocation: String?,
        allowMultiple: Boolean,
        callback: (List<String>) -> Unit,
    ) = showJvmFileDialog(
        dialogType = DIALOG_OPEN_FOLDER,
        windowId = windowId,
        filters = emptyList(),
        defaultLocation = defaultLocation,
        allowMultiple = allowMultiple,
        callback = callback,
    )

    private fun showJvmFileDialog(
        dialogType: Int,
        windowId: Int?,
        filters: List<SDLDialogFileFilter>,
        defaultLocation: String?,
        allowMultiple: Boolean,
        callback: (List<String>) -> Unit,
    ) {
        val id = dialogNextId.getAndIncrement()
        dialogCallbacks[id] = callback
        val window = windowId?.let { Jni.getWindowFromID(it) } ?: 0L
        val names = filters.map { it.name }.toTypedArray()
        val patterns = filters.map { it.pattern }.toTypedArray()
        try {
            when (dialogType) {
                DIALOG_OPEN_FILE -> Jni.showOpenFileDialog(id, window, names, patterns, defaultLocation, allowMultiple)
                DIALOG_SAVE_FILE -> Jni.showSaveFileDialog(id, window, names, patterns, defaultLocation)
                else -> Jni.showOpenFolderDialog(id, window, defaultLocation, allowMultiple)
            }
        } catch (t: Throwable) {
            dialogCallbacks.remove(id)
            throw t
        }
    }

    // ==================== keyboard ====================

    actual val keyboardState: ByteArray
        get() = Jni.keyboardState() ?: ByteArray(0)

    actual val modState: Int
        get() = Jni.modState()

    actual fun setModState(modState: Int) {
        Jni.setModState(modState)
    }

    actual fun getKeyFromScancode(scancode: Int): Int =
        Jni.getKeyFromScancode(scancode)

    actual fun getScancodeFromKey(keycode: Int): Int =
        Jni.getScancodeFromKey(keycode)

    actual fun getKeyName(keycode: Int): String? =
        Jni.getKeyName(keycode)

    actual fun getScancodeName(scancode: Int): String? =
        Jni.getScancodeName(scancode)

    // ==================== mouse ====================

    actual val mouseState: SDLMouseState
        get() {
            val s = Jni.mouseState()
            return SDLMouseState(s[0], s[1], s[2].toInt())
        }

    actual val globalMouseState: SDLMouseState
        get() {
            val s = Jni.globalMouseState()
            return SDLMouseState(s[0], s[1], s[2].toInt())
        }

    actual fun warpMouseInWindow(windowId: Int, x: Float, y: Float) {
        val window = Jni.getWindowFromID(windowId) ?: return
        Jni.warpMouseInWindow(window, x, y)
    }

    actual fun captureMouse(enabled: Boolean): Boolean =
        Jni.captureMouse(enabled)

    actual fun showCursor(): Boolean = Jni.showCursor()

    // ==================== joystick / gamepad ====================

    actual val joysticks: List<Int>
        get() = Jni.getJoysticks()?.toList() ?: emptyList()

    actual fun openJoystick(id: Int): SDLJoystick {
        val ptr = Jni.openJoystick(id)
        check(ptr != 0L) { "SDL_OpenJoystick failed: ${SDL.error()}" }
        return JvmSDLJoystick(ptr)
    }

    actual val gamepads: List<Int>
        get() = Jni.getGamepads()?.toList() ?: emptyList()

    actual fun openGamepad(id: Int): SDLGamepad {
        val ptr = Jni.openGamepad(id)
        check(ptr != 0L) { "SDL_OpenGamepad failed: ${SDL.error()}" }
        return JvmSDLGamepad(ptr)
    }

    // ==================== filesystem / misc ====================

    actual val basePath: String?
        get() = Jni.basePath()

    actual fun getPrefPath(orgName: String, appName: String): String? =
        Jni.getPrefPath(orgName, appName)

    actual fun getUserFolder(folder: Int): String? =
        Jni.getUserFolder(folder)

    actual fun createDirectory(path: String): Boolean =
        Jni.createDirectory(path)

    actual fun removePath(path: String): Boolean =
        Jni.removePath(path)

    actual fun renamePath(oldPath: String, newPath: String): Boolean =
        Jni.renamePath(oldPath, newPath)

    actual val powerInfo: SDLPowerInfo
        get() {
            val info = Jni.powerInfo()
            return SDLPowerInfo(info[0], info[1], info[2])
        }

    actual fun openURL(url: String): Boolean =
        Jni.openURL(url)

    actual val hasClipboardText: Boolean
        get() = Jni.hasClipboardText()

    actual fun getHintBoolean(name: String, defaultValue: Boolean): Boolean =
        Jni.getHintBoolean(name, defaultValue)

    actual fun showMessageBox(
        flags: Int,
        title: String,
        message: String,
        buttons: List<SDLMessageBoxButton>,
    ): Int {
        val buttonFlags = IntArray(buttons.size) { buttons[it].flags }
        val buttonIds = IntArray(buttons.size) { buttons[it].id }
        val buttonTexts = buttons.map { it.text }.toTypedArray()
        val clicked = Jni.showMessageBox(flags, title, message, buttonFlags, buttonIds, buttonTexts)
        check(clicked >= 0) { "SDL_ShowMessageBox failed: ${SDL.error()}" }
        return clicked
    }

    // ==================== OpenGL ====================

    actual fun glLoadLibrary(path: String?): Boolean =
        Jni.glLoadLibrary(path)

    actual fun glUnloadLibrary() {
        Jni.glUnloadLibrary()
    }

    actual fun glGetProcAddress(proc: String): ULong =
        Jni.glGetProcAddress(proc).toULong()

    actual fun glExtensionSupported(extension: String): Boolean =
        Jni.glExtensionSupported(extension)

    actual fun glResetAttributes() {
        Jni.glResetAttributes()
    }

    actual fun glSetAttribute(attr: Int, value: Int): Boolean =
        Jni.glSetAttribute(attr, value)

    actual fun glGetAttribute(attr: Int): Int? = Jni.glGetAttribute(attr)?.get(0)

    actual fun glCreateContext(windowId: Int): ULong {
        val window = Jni.getWindowFromID(windowId) ?: return 0uL
        return Jni.glCreateContext(window).toULong()
    }

    actual fun glMakeCurrent(windowId: Int, context: ULong): Boolean {
        val window = Jni.getWindowFromID(windowId) ?: return false
        return Jni.glMakeCurrent(window, context.toLong())
    }

    actual val glCurrentWindow: Int?
        get() = Jni.glGetCurrentWindow().takeIf { it != 0L }?.let { Jni.getWindowID(it) }

    actual val glCurrentContext: ULong
        get() = Jni.glGetCurrentContext().toULong()

    actual fun glSetSwapInterval(interval: Int): Boolean =
        Jni.glSetSwapInterval(interval)

    actual val glSwapInterval: Int?
        get() = Jni.glGetSwapInterval()?.get(0)

    actual fun glSwapWindow(windowId: Int): Boolean {
        val window = Jni.getWindowFromID(windowId) ?: return false
        return Jni.glSwapWindow(window)
    }

    actual fun glDestroyContext(context: ULong) {
        if (context != 0uL) {
            Jni.glDestroyContext(context.toLong())
        }
    }

    // ==================== Vulkan ====================

    actual fun vulkanLoadLibrary(path: String?): Boolean =
        Jni.vulkanLoadLibrary(path)

    actual fun vulkanUnloadLibrary() {
        Jni.vulkanUnloadLibrary()
    }

    actual val vulkanGetVkGetInstanceProcAddr: ULong
        get() = Jni.vulkanGetVkGetInstanceProcAddr().toULong()

    actual val vulkanInstanceExtensions: List<String>
        get() = Jni.vulkanGetInstanceExtensions()?.toList() ?: emptyList()

    actual fun vulkanCreateSurface(windowId: Int, instance: ULong): ULong {
        val window = Jni.getWindowFromID(windowId) ?: return 0uL
        return Jni.vulkanCreateSurface(window, instance.toLong()).toULong()
    }

    actual fun vulkanDestroySurface(instance: ULong, surface: ULong) {
        if (surface != 0uL) {
            Jni.vulkanDestroySurface(instance.toLong(), surface.toLong())
        }
    }

    actual fun vulkanGetPresentationSupport(instance: ULong, physicalDevice: ULong, queueFamilyIndex: Int): Boolean =
        Jni.vulkanGetPresentationSupport(instance.toLong(), physicalDevice.toLong(), queueFamilyIndex)
}

// =========================================================================
// JVM (JNI) display
// =========================================================================

internal class JvmSDLDisplay(override val id: Int) : SDLDisplay {

    override val name: String?
        get() = Jni.getDisplayName(id)

    override val bounds: SDLRect
        get() {
            val r = Jni.getDisplayBounds(id)
            return SDLRect(r[0], r[1], r[2], r[3])
        }

    override val usableBounds: SDLRect
        get() {
            val r = Jni.getDisplayUsableBounds(id)
            return SDLRect(r[0], r[1], r[2], r[3])
        }

    override val currentMode: SDLDisplayMode
        get() = Jni.getCurrentDisplayMode(id)?.toCommon()
            ?: throw IllegalStateException("SDL_GetCurrentDisplayMode failed")

    override val desktopMode: SDLDisplayMode
        get() = Jni.getDesktopDisplayMode(id)?.toCommon()
            ?: throw IllegalStateException("SDL_GetDesktopDisplayMode failed")

    override val primary: Boolean
        get() = Jni.getPrimaryDisplay() == id
}

private fun FloatArray.toCommon(): SDLDisplayMode =
    SDLDisplayMode(
        format = this[0].toInt(),
        width = this[1].toInt(),
        height = this[2].toInt(),
        refreshRate = this[3],
        pixelDensity = this[4],
    )

// =========================================================================
// JVM (JNI) texture
// =========================================================================

internal class JvmSDLTexture internal constructor(
    ptr: Long,
    internal val renderer: JvmSDLRenderer,
) : SDLTexture {

    internal var texture: Long = ptr

    override val ptr: Long
        get() = texture

    private fun check(): Long =
        texture.also { if (it == 0L) throw IllegalStateException("SDL texture is closed") }

    override val format: Int
        get() {
            val p = Jni.getTextureProperties(check())
            return p?.get(0) ?: 0
        }

    override val access: Int
        get() {
            val p = Jni.getTextureProperties(check())
            return p?.get(1) ?: 0
        }

    override val size: SDLFloatPoint
        get() {
            val s = Jni.getTextureSize(check())
            return SDLFloatPoint(s[0], s[1])
        }

    override var colorMod: SDLColor
        get() {
            val c = Jni.getTextureColorMod(check()) ?: return SDLColor(255, 255, 255)
            return SDLColor(c[0], c[1], c[2])
        }
        set(value) {
            Jni.setTextureColorMod(check(), value.r, value.g, value.b)
        }

    override var alphaMod: Int
        get() = Jni.getTextureAlphaMod(check()).takeIf { it >= 0 } ?: 255
        set(value) {
            Jni.setTextureAlphaMod(check(), value)
        }

    override var blendMode: Int
        get() = Jni.getTextureBlendMode(check()).takeIf { it >= 0 } ?: SDLBlendMode.BLEND
        set(value) {
            Jni.setTextureBlendMode(check(), value)
        }

    override var scaleMode: Int
        get() = Jni.getTextureScaleMode(check()).takeIf { it >= 0 } ?: SDLScaleMode.LINEAR
        set(value) {
            Jni.setTextureScaleMode(check(), value)
        }

    override fun update(rect: SDLRect?, pixels: ByteArray, pitch: Int): Boolean {
        return Jni.updateTexture(
            check(),
            rect?.let { intArrayOf(it.x, it.y, it.width, it.height) },
            pixels,
            pitch,
        )
    }

    override fun lock(rect: SDLRect?): SDLTextureLock? {
        val rectArr = rect?.let { intArrayOf(it.x, it.y, it.width, it.height) }
        val bytes = (if (rect == null) size else SDLFloatPoint(rect.width.toFloat(), rect.height.toFloat()))
        val byteCount = bytes.x.toInt() * bytes.y.toInt() * 4
        val pixels = ByteArray(byteCount)
        val pitch = IntArray(1)
        if (!Jni.lockTexture(check(), rectArr, pixels, pitch)) return null
        return SDLTextureLock(pixels, pitch[0])
    }

    override fun unlock() {
        Jni.unlockTexture(check())
    }

    override fun close() {
        val t = texture
        if (t == 0L) return
        texture = 0L
        Jni.destroyTexture(t)
    }
}

// =========================================================================
// JVM (JNI) surface
// =========================================================================

internal class JvmSDLSurface internal constructor(
    ptr: Long,
    internal val owned: Boolean,
) : cn.enaium.sdl.SDLSurface {

    internal var surface: Long = ptr

    override val ptr: Long
        get() = surface

    internal fun check(): Long =
        surface.also { if (it == 0L) throw IllegalStateException("SDL surface is closed") }

    override val width: Int get() = Jni.surfaceWidth(check())
    override val height: Int get() = Jni.surfaceHeight(check())
    override val format: Int get() = Jni.surfaceFormat(check())
    override val colorspace: Int get() = Jni.getSurfaceColorspace(check())
    override val pitch: Int get() = Jni.surfacePitch(check())

    override val pixels: ByteArray
        get() = Jni.surfacePixels(check()) ?: ByteArray(0)

    override fun lock(): Boolean = Jni.lockSurface(check())

    override fun unlock() {
        Jni.unlockSurface(check())
    }

    override fun fillRect(rect: SDLRect?, color: SDLColor): Boolean {
        return Jni.surfaceFillRect(
            check(),
            rect?.let { intArrayOf(it.x, it.y, it.width, it.height) },
            color.r, color.g, color.b, color.a,
        )
    }

    override fun fillRects(rects: List<SDLRect>, color: SDLColor): Boolean {
        if (rects.isEmpty()) return true
        val arr = IntArray(rects.size * 4)
        for (i in rects.indices) {
            arr[i * 4] = rects[i].x
            arr[i * 4 + 1] = rects[i].y
            arr[i * 4 + 2] = rects[i].width
            arr[i * 4 + 3] = rects[i].height
        }
        return Jni.surfaceFillRects(check(), arr, color.r, color.g, color.b, color.a)
    }

    override fun blit(src: SDLRect?, dst: SDLSurface, dstRect: SDLRect?): Boolean {
        val jvmDst = (dst as? JvmSDLSurface)?.check()
            ?: throw IllegalArgumentException("dst is not a JVM SDL surface")
        return Jni.surfaceBlit(
            check(),
            src?.let { intArrayOf(it.x, it.y, it.width, it.height) },
            jvmDst,
            dstRect?.let { intArrayOf(it.x, it.y, it.width, it.height) },
        )
    }

    override fun blitScaled(src: SDLRect?, dst: SDLSurface, dstRect: SDLRect?, scaleMode: Int): Boolean {
        val jvmDst = (dst as? JvmSDLSurface)?.check()
            ?: throw IllegalArgumentException("dst is not a JVM SDL surface")
        return Jni.surfaceBlitScaled(
            check(),
            src?.let { intArrayOf(it.x, it.y, it.width, it.height) },
            jvmDst,
            dstRect?.let { intArrayOf(it.x, it.y, it.width, it.height) },
            scaleMode,
        )
    }

    override fun saveBMP(path: String): Boolean = Jni.surfaceSaveBMP(check(), path)

    override fun convert(format: Int): SDLSurface {
        val converted = Jni.convertSurface(check(), format)
        check(converted != 0L) { "SDL_ConvertSurface failed: ${SDL.error()}" }
        return JvmSDLSurface(converted, owned = true)
    }

    override fun close() {
        val s = surface
        if (s == 0L) return
        surface = 0L
        if (owned) {
            Jni.destroySurface(s)
        }
    }
}

// =========================================================================
// JVM (JNI) audio
// =========================================================================

internal class JvmSDLAudioDevice internal constructor(
    internal val deviceId: Int,
    internal val spec: SDLAudioSpec,
    internal val recording: Boolean,
) : SDLAudioDevice {

    override val id: Int get() = deviceId

    override val format: SDLAudioSpec
        get() {
            val f = Jni.getAudioDeviceFormat(deviceId)
                ?: throw IllegalStateException("SDL_GetAudioDeviceFormat failed: ${SDL.error()}")
            return SDLAudioSpec(format = f[0], channels = f[1], freq = f[2])
        }

    override val isRecording: Boolean get() = recording

    override fun bindStream(stream: SDLAudioStream): Boolean {
        val native = stream as? JvmSDLAudioStream
            ?: throw IllegalArgumentException("stream is not a JVM SDL audio stream")
        return Jni.bindAudioStream(deviceId, native.ptr)
    }

    override fun unbindStream(stream: SDLAudioStream) {
        val native = stream as? JvmSDLAudioStream
            ?: throw IllegalArgumentException("stream is not a JVM SDL audio stream")
        Jni.unbindAudioStream(deviceId, native.ptr)
    }

    override fun pause() {
        Jni.pauseAudioDevice(deviceId)
    }

    override fun resume() {
        Jni.resumeAudioDevice(deviceId)
    }

    override fun close() {
        Jni.closeAudioDevice(deviceId)
    }
}

internal class JvmSDLAudioStream internal constructor(
    ptr: Long,
) : SDLAudioStream {

    override var ptr: Long = ptr
        private set

    override fun putData(data: ByteArray): Boolean = Jni.putAudioStreamData(ptr, data)

    override fun getData(maxLen: Int): ByteArray = Jni.getAudioStreamData(ptr, maxLen) ?: ByteArray(0)

    override val available: Int
        get() = Jni.getAudioStreamAvailable(ptr)

    override val queued: Int
        get() = Jni.getAudioStreamQueued(ptr)

    override val inputSpec: SDLAudioSpec?
        get() = Jni.getAudioStreamFormat(ptr)?.let {
            SDLAudioSpec(format = it[0], channels = it[1], freq = it[2])
        }

    override val outputSpec: SDLAudioSpec?
        get() = Jni.getAudioStreamFormat(ptr)?.let {
            SDLAudioSpec(format = it[3], channels = it[4], freq = it[5])
        }

    override fun setFormat(src: SDLAudioSpec, dst: SDLAudioSpec): Boolean =
        Jni.setAudioStreamFormat(ptr, src.format, src.channels, src.freq, dst.format, dst.channels, dst.freq)

    override var gain: Float
        get() = Jni.getAudioStreamGain(ptr)
        set(value) {
            Jni.setAudioStreamGain(ptr, value)
        }

    override var frequencyRatio: Float
        get() = Jni.getAudioStreamFrequencyRatio(ptr)
        set(value) {
            Jni.setAudioStreamFrequencyRatio(ptr, value)
        }

    override var devicePaused: Boolean
        get() = Jni.audioStreamDevicePaused(ptr)
        set(value) {
            if (value) {
                Jni.pauseAudioStreamDevice(ptr)
            } else {
                Jni.resumeAudioStreamDevice(ptr)
            }
        }

    override fun resume() {
        devicePaused = false
    }

    override fun pause() {
        devicePaused = true
    }

    override fun flush(): Boolean = Jni.flushAudioStream(ptr)

    override fun clear(): Boolean = Jni.clearAudioStream(ptr)

    override fun close() {
        Jni.destroyAudioStream(ptr)
        ptr = 0L
    }
}

// =========================================================================
// JVM (JNI) joystick / gamepad
// =========================================================================

internal class JvmSDLJoystick internal constructor(
    ptr: Long,
) : SDLJoystick {

    override var ptr: Long = ptr
        private set

    override val id: Int get() = Jni.joystickId(ptr)
    override val name: String? get() = Jni.joystickName(ptr)
    override val type: Int get() = Jni.joystickType(ptr)
    override val numAxes: Int get() = Jni.joystickNumAxes(ptr)
    override val numBalls: Int get() = Jni.joystickNumBalls(ptr)
    override val numHats: Int get() = Jni.joystickNumHats(ptr)
    override val numButtons: Int get() = Jni.joystickNumButtons(ptr)
    override val playerIndex: Int get() = Jni.joystickPlayerIndex(ptr)
    override val firmwareVersion: Int get() = Jni.joystickFirmwareVersion(ptr)

    override fun axis(axis: Int): Short = Jni.joystickAxis(ptr, axis)

    override fun button(button: Int): Boolean = Jni.joystickButton(ptr, button)

    override fun hat(hat: Int): UByte = Jni.joystickHat(ptr, hat).toUByte()

    override fun ball(ball: Int): SDLPoint? = Jni.joystickBall(ptr, ball)?.let { SDLPoint(it[0], it[1]) }

    override fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean =
        Jni.joystickRumble(ptr, lowFrequency, highFrequency, durationMs)

    override fun close() {
        if (ptr != 0L) {
            Jni.closeJoystick(ptr)
            ptr = 0L
        }
    }
}

internal class JvmSDLGamepad internal constructor(
    ptr: Long,
) : SDLGamepad {

    override var ptr: Long = ptr
        private set

    override val id: Int get() = Jni.gamepadId(ptr)
    override val name: String? get() = Jni.gamepadName(ptr)
    override val vendor: Int get() = Jni.gamepadVendor(ptr)
    override val product: Int get() = Jni.gamepadProduct(ptr)
    override val serial: String? get() = Jni.gamepadSerial(ptr)
    override val connected: Boolean get() = Jni.gamepadConnected(ptr)
    override val playerIndex: Int get() = Jni.gamepadPlayerIndex(ptr)
    override val firmwareVersion: Int get() = Jni.gamepadFirmwareVersion(ptr)
    override val touchpadCount: Int get() = Jni.gamepadNumTouchpads(ptr)

    override fun touchpadFinger(touchpad: Int, finger: Int): SDLTouchpadFinger? =
        Jni.gamepadTouchpadFinger(ptr, touchpad, finger)?.let {
            SDLTouchpadFinger(
                touchpad = touchpad,
                finger = finger,
                down = it[0] != 0f,
                x = it[1],
                y = it[2],
                pressure = it[3],
            )
        }

    override fun hasSensor(type: Int): Boolean = Jni.gamepadHasSensor(ptr, type)

    override fun sensorData(type: Int): FloatArray? = Jni.gamepadSensorData(ptr, type)

    override fun getSensorDataRate(type: Int): Float = Jni.gamepadSensorDataRate(ptr, type)

    override fun button(button: Int): Boolean = Jni.gamepadButton(ptr, button)

    override fun axis(axis: Int): Short = Jni.gamepadAxis(ptr, axis)

    override fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean =
        Jni.gamepadRumble(ptr, lowFrequency, highFrequency, durationMs)

    override fun close() {
        if (ptr != 0L) {
            Jni.closeGamepad(ptr)
            ptr = 0L
        }
    }
}
