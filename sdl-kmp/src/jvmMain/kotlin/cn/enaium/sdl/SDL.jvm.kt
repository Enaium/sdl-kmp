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

import org.lwjgl.sdl.SDLAudio
import org.lwjgl.sdl.SDLClipboard
import org.lwjgl.sdl.SDLFileSystem
import org.lwjgl.sdl.SDLGamepad as LwjglSDLGamepad
import org.lwjgl.sdl.SDLJoystick as LwjglSDLJoystick
import org.lwjgl.sdl.SDLMisc
import org.lwjgl.sdl.SDLMouse
import org.lwjgl.sdl.SDLPixels
import org.lwjgl.sdl.SDLPower
import org.lwjgl.sdl.SDLSurface as LwjglSDLSurface
import org.lwjgl.sdl.SDL_AudioSpec
import org.lwjgl.sdl.SDL_FPoint
import org.lwjgl.sdl.SDL_Rect
import org.lwjgl.sdl.SDL_Texture
import org.lwjgl.sdl.SDL_DisplayMode
import org.lwjgl.sdl.SDL_MessageBoxButtonData
import org.lwjgl.sdl.SDL_MessageBoxData
import org.lwjgl.sdl.SDL_Surface
import org.lwjgl.sdl.SDLError
import org.lwjgl.sdl.SDLEvents
import org.lwjgl.sdl.SDLHints
import org.lwjgl.sdl.SDLInit
import org.lwjgl.sdl.SDLKeyboard
import org.lwjgl.sdl.SDLMain
import org.lwjgl.sdl.SDLMessageBox
import org.lwjgl.sdl.SDL_FRect
import org.lwjgl.sdl.SDLRender
import org.lwjgl.sdl.SDLTimer
import org.lwjgl.sdl.SDLVersion as LwjglSDLVersion
import org.lwjgl.sdl.SDLVideo
import org.lwjgl.sdl.SDL_Event
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

// =========================================================================
// JVM (LWJGL SDL3 bindings) implementations
// =========================================================================

private fun SDL_Event.toSDLEvent(): SDLEvent {
    val type = type()
    return when (type) {
        SDLEventType.QUIT -> SDLEvent.Quit(quit().timestamp().toULong())
        in SDLEventType.WINDOW_FIRST until SDLEventType.KEY_FIRST ->
            SDLEvent.Window(
                timestamp = window().timestamp().toULong(),
                windowId = window().windowID(),
                type = type,
                data1 = window().data1(),
                data2 = window().data2(),
            )
        SDLEventType.KEY_DOWN, SDLEventType.KEY_UP ->
            SDLEvent.Key(
                timestamp = key().timestamp().toULong(),
                windowId = key().windowID(),
                down = key().down(),
                repeat = key().repeat(),
                keycode = key().key(),
                scancode = key().scancode(),
                modifiers = key().mod().toInt(),
            )
        SDLEventType.TEXT_INPUT ->
            SDLEvent.TextInput(
                timestamp = text().timestamp().toULong(),
                windowId = text().windowID(),
                text = text().text()?.let { MemoryUtil.memUTF8(it) } ?: "",
            )
        SDLEventType.MOUSE_MOTION ->
            SDLEvent.MouseMotion(
                timestamp = motion().timestamp().toULong(),
                windowId = motion().windowID(),
                x = motion().x(),
                y = motion().y(),
                dx = motion().xrel(),
                dy = motion().yrel(),
            )
        SDLEventType.MOUSE_BUTTON_DOWN, SDLEventType.MOUSE_BUTTON_UP ->
            SDLEvent.MouseButton(
                timestamp = button().timestamp().toULong(),
                windowId = button().windowID(),
                down = button().down(),
                button = button().button().toInt(),
                clicks = button().clicks().toInt(),
                x = button().x(),
                y = button().y(),
            )
        SDLEventType.MOUSE_WHEEL ->
            SDLEvent.MouseWheel(
                timestamp = wheel().timestamp().toULong(),
                windowId = wheel().windowID(),
                x = wheel().x(),
                y = wheel().y(),
                direction = wheel().direction(),
            )
        else -> SDLEvent.Unknown(timestamp = type.toULong(), type = type)
    }
}

// =========================================================================
// JVM (LWJGL) window
// =========================================================================

internal class JvmSDLWindow internal constructor(internal var ptr: Long) : SDLWindow {

    override val id: Int
        get() = SDLVideo.SDL_GetWindowID(ptr)

    override var title: String
        get() = SDLVideo.SDL_GetWindowTitle(ptr) ?: ""
        set(value) {
            SDLVideo.SDL_SetWindowTitle(ptr, value)
        }

    override var size: SDLPoint
        get() = MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            SDLVideo.SDL_GetWindowSize(ptr, w, h)
            SDLPoint(w.get(0), h.get(0))
        }
        set(value) {
            SDLVideo.SDL_SetWindowSize(ptr, value.x, value.y)
        }

    override val flags: ULong
        get() = SDLVideo.SDL_GetWindowFlags(ptr).toULong()

    override var position: SDLPoint
        get() = MemoryStack.stackPush().use { stack ->
            val x = stack.mallocInt(1)
            val y = stack.mallocInt(1)
            SDLVideo.SDL_GetWindowPosition(ptr, x, y)
            SDLPoint(x.get(0), y.get(0))
        }
        set(value) {
            SDLVideo.SDL_SetWindowPosition(ptr, value.x, value.y)
        }

    override val sizeInPixels: SDLPoint
        get() = MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            SDLVideo.SDL_GetWindowSizeInPixels(ptr, w, h)
            SDLPoint(w.get(0), h.get(0))
        }

    override val displayId: Int
        get() = SDLVideo.SDL_GetDisplayForWindow(ptr)

    override var opacity: Float
        get() = SDLVideo.SDL_GetWindowOpacity(ptr)
        set(value) {
            SDLVideo.SDL_SetWindowOpacity(ptr, value)
        }

    override var fullscreen: Boolean
        get() = (flags and SDLWindowFlags.FULLSCREEN) != 0uL
        set(value) {
            SDLVideo.SDL_SetWindowFullscreen(ptr, value)
        }

    override var bordered: Boolean
        get() = (flags and SDLWindowFlags.BORDERLESS) == 0uL
        set(value) {
            SDLVideo.SDL_SetWindowBordered(ptr, value)
        }

    override var resizable: Boolean
        get() = (flags and SDLWindowFlags.RESIZABLE) != 0uL
        set(value) {
            SDLVideo.SDL_SetWindowResizable(ptr, value)
        }

    override var alwaysOnTop: Boolean
        get() = (flags and SDLWindowFlags.ALWAYS_ON_TOP) != 0uL
        set(value) {
            SDLVideo.SDL_SetWindowAlwaysOnTop(ptr, value)
        }

    override var mouseGrab: Boolean
        get() = SDLVideo.SDL_GetWindowMouseGrab(ptr)
        set(value) {
            SDLVideo.SDL_SetWindowMouseGrab(ptr, value)
        }

    override var keyboardGrab: Boolean
        get() = SDLVideo.SDL_GetWindowKeyboardGrab(ptr)
        set(value) {
            SDLVideo.SDL_SetWindowKeyboardGrab(ptr, value)
        }

    override var relativeMouseMode: Boolean
        get() = SDLMouse.SDL_GetWindowRelativeMouseMode(ptr)
        set(value) {
            SDLMouse.SDL_SetWindowRelativeMouseMode(ptr, value)
        }

    override var minimumSize: SDLPoint?
        get() = MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            SDLVideo.SDL_GetWindowMinimumSize(ptr, w, h)
            SDLPoint(w.get(0), h.get(0))
        }
        set(value) {
            SDLVideo.SDL_SetWindowMinimumSize(ptr, value?.x ?: 0, value?.y ?: 0)
        }

    override var maximumSize: SDLPoint?
        get() = MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            SDLVideo.SDL_GetWindowMaximumSize(ptr, w, h)
            SDLPoint(w.get(0), h.get(0))
        }
        set(value) {
            SDLVideo.SDL_SetWindowMaximumSize(ptr, value?.x ?: 0, value?.y ?: 0)
        }

    override fun maximize() {
        SDLVideo.SDL_MaximizeWindow(ptr)
    }

    override fun minimize() {
        SDLVideo.SDL_MinimizeWindow(ptr)
    }

    override fun restore() {
        SDLVideo.SDL_RestoreWindow(ptr)
    }

    override fun flash() {
        SDLVideo.SDL_FlashWindow(ptr, 0)
    }

    override val surface: cn.enaium.sdl.SDLSurface?
        get() {
            val surface = SDLVideo.SDL_GetWindowSurface(ptr) ?: return null
            return JvmSDLSurface(surface, owned = false)
        }

    override fun setIcon(icon: cn.enaium.sdl.SDLSurface): Boolean {
        val jvmIcon = (icon as? JvmSDLSurface)?.surface
            ?: throw IllegalArgumentException("icon is not a JVM SDL surface")
        return SDLVideo.SDL_SetWindowIcon(ptr, jvmIcon)
    }

    override fun show() {
        SDLVideo.SDL_ShowWindow(ptr)
    }

    override fun hide() {
        SDLVideo.SDL_HideWindow(ptr)
    }

    override fun raise() {
        SDLVideo.SDL_RaiseWindow(ptr)
    }

    override fun close() {
        if (ptr == 0L) return
        SDLVideo.SDL_DestroyWindow(ptr)
        ptr = 0L
    }
}

// =========================================================================
// JVM (LWJGL) renderer
// =========================================================================

internal class JvmSDLRenderer internal constructor(internal var ptr: Long) : SDLRenderer {

    override val name: String?
        get() = SDLRender.SDL_GetRendererName(ptr)

    override var drawColor: SDLColor
        get() = MemoryStack.stackPush().use { stack ->
            val r = stack.malloc(1)
            val g = stack.malloc(1)
            val b = stack.malloc(1)
            val a = stack.malloc(1)
            SDLRender.SDL_GetRenderDrawColor(ptr, r, g, b, a)
            SDLColor(
                r.get(0).toInt() and 0xff,
                g.get(0).toInt() and 0xff,
                b.get(0).toInt() and 0xff,
                a.get(0).toInt() and 0xff,
            )
        }
        set(value) {
            SDLRender.SDL_SetRenderDrawColor(
                ptr,
                value.r.toByte(),
                value.g.toByte(),
                value.b.toByte(),
                value.a.toByte(),
            )
        }

    override val outputSize: SDLPoint
        get() = MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            SDLRender.SDL_GetRenderOutputSize(ptr, w, h)
            SDLPoint(w.get(0), h.get(0))
        }

    override val currentOutputSize: SDLPoint
        get() = MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            SDLRender.SDL_GetCurrentRenderOutputSize(ptr, w, h)
            SDLPoint(w.get(0), h.get(0))
        }

    override var viewport: SDLRect?
        get() {
            val r = SDL_Rect.calloc()
            try {
                SDLRender.SDL_GetRenderViewport(ptr, r)
                return SDLRect(r.x(), r.y(), r.w(), r.h())
            } finally {
                r.free()
            }
        }
        set(value) {
            if (value == null) {
                SDLRender.nSDL_SetRenderViewport(ptr, 0L)
            } else {
                val r = SDL_Rect.calloc()
                try {
                    r.x(value.x).y(value.y).w(value.width).h(value.height)
                    SDLRender.SDL_SetRenderViewport(ptr, r)
                } finally {
                    r.free()
                }
            }
        }

    override var clipRect: SDLRect?
        get() {
            val r = SDL_Rect.calloc()
            try {
                SDLRender.SDL_GetRenderClipRect(ptr, r)
                return SDLRect(r.x(), r.y(), r.w(), r.h())
            } finally {
                r.free()
            }
        }
        set(value) {
            if (value == null) {
                SDLRender.nSDL_SetRenderClipRect(ptr, 0L)
            } else {
                val r = SDL_Rect.calloc()
                try {
                    r.x(value.x).y(value.y).w(value.width).h(value.height)
                    SDLRender.SDL_SetRenderClipRect(ptr, r)
                } finally {
                    r.free()
                }
            }
        }

    override var scale: SDLFloatPoint
        get() = MemoryStack.stackPush().use { stack ->
            val x = stack.mallocFloat(1)
            val y = stack.mallocFloat(1)
            SDLRender.SDL_GetRenderScale(ptr, x, y)
            SDLFloatPoint(x.get(0), y.get(0))
        }
        set(value) {
            SDLRender.SDL_SetRenderScale(ptr, value.x, value.y)
        }

    override var blendMode: Int
        get() = MemoryStack.stackPush().use { stack ->
            val mode = stack.mallocInt(1)
            SDLRender.SDL_GetRenderDrawBlendMode(ptr, mode)
            mode.get(0)
        }
        set(value) {
            SDLRender.SDL_SetRenderDrawBlendMode(ptr, value)
        }

    override var vsync: Boolean
        get() = MemoryStack.stackPush().use { stack ->
            val vsync = stack.mallocInt(1)
            SDLRender.SDL_GetRenderVSync(ptr, vsync)
            vsync.get(0) != 0
        }
        set(value) {
            SDLRender.SDL_SetRenderVSync(ptr, if (value) 1 else 0)
        }

    override var target: SDLTexture?
        get() {
            val texture = SDLRender.SDL_GetRenderTarget(ptr) ?: return null
            return JvmSDLTexture(texture, this)
        }
        set(value) {
            val texture = (value as? JvmSDLTexture)?.texture
            SDLRender.SDL_SetRenderTarget(ptr, texture)
        }

    override fun clear(): Boolean = SDLRender.SDL_RenderClear(ptr)

    override fun present() {
        SDLRender.SDL_RenderPresent(ptr)
    }

    override fun fillRect(rect: SDLRect): Boolean {
        val r = SDL_FRect.calloc()
        try {
            r.x(rect.x.toFloat()).y(rect.y.toFloat()).w(rect.width.toFloat()).h(rect.height.toFloat())
            return SDLRender.SDL_RenderFillRect(ptr, r)
        } finally {
            r.free()
        }
    }

    override fun drawRect(rect: SDLRect): Boolean {
        val r = SDL_FRect.calloc()
        try {
            r.x(rect.x.toFloat()).y(rect.y.toFloat()).w(rect.width.toFloat()).h(rect.height.toFloat())
            return SDLRender.SDL_RenderRect(ptr, r)
        } finally {
            r.free()
        }
    }

    override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int): Boolean =
        SDLRender.SDL_RenderLine(ptr, x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())

    override fun drawPoint(x: Int, y: Int): Boolean =
        SDLRender.SDL_RenderPoint(ptr, x.toFloat(), y.toFloat())

    override fun drawPoints(points: List<SDLPoint>): Boolean {
        val buffer = SDL_FPoint.calloc(points.size)
        try {
            for (i in points.indices) {
                buffer.get(i).x(points[i].x.toFloat()).y(points[i].y.toFloat())
            }
            return SDLRender.SDL_RenderPoints(ptr, buffer)
        } finally {
            buffer.free()
        }
    }

    override fun createTexture(format: Int, access: Int, width: Int, height: Int): SDLTexture {
        val texture = SDLRender.SDL_CreateTexture(ptr, format, access, width, height)
            ?: throw IllegalStateException("SDL_CreateTexture failed: ${SDL.error()}")
        return JvmSDLTexture(texture, this)
    }

    override fun createTextureFromSurface(surface: SDLSurface): SDLTexture {
        val jvmSurface = (surface as? JvmSDLSurface)?.surface
            ?: throw IllegalArgumentException("surface is not a JVM SDL surface")
        val texture = SDLRender.SDL_CreateTextureFromSurface(ptr, jvmSurface)
            ?: throw IllegalStateException("SDL_CreateTextureFromSurface failed: ${SDL.error()}")
        return JvmSDLTexture(texture, this)
    }

    override fun renderTexture(texture: SDLTexture, src: SDLFRect?, dst: SDLFRect?): Boolean {
        val t = (texture as? JvmSDLTexture)?.texture
            ?: throw IllegalArgumentException("texture is not a JVM SDL texture")
        return if (src == null && dst == null) {
            SDLRender.nSDL_RenderTexture(ptr, (t as org.lwjgl.system.Struct<*>).address(), 0L, 0L)
        } else {
            val srcR = src?.let { SDL_FRect.calloc().also { r -> r.x(it.x).y(it.y).w(it.width).h(it.height) } }
            val dstR = dst?.let { SDL_FRect.calloc().also { r -> r.x(it.x).y(it.y).w(it.width).h(it.height) } }
            try {
                SDLRender.SDL_RenderTexture(ptr, t, srcR, dstR)
            } finally {
                srcR?.free()
                dstR?.free()
            }
        }
    }

    override fun renderTextureRotated(
        texture: SDLTexture,
        src: SDLFRect?,
        dst: SDLFRect?,
        angle: Double,
        center: SDLFloatPoint?,
        flip: Int,
    ): Boolean {
        val t = (texture as? JvmSDLTexture)?.texture
            ?: throw IllegalArgumentException("texture is not a JVM SDL texture")
        val srcR = src?.let { SDL_FRect.calloc().also { r -> r.x(it.x).y(it.y).w(it.width).h(it.height) } }
        val dstR = dst?.let { SDL_FRect.calloc().also { r -> r.x(it.x).y(it.y).w(it.width).h(it.height) } }
        val centerP = center?.let { SDL_FPoint.calloc().also { p -> p.x(it.x).y(it.y) } }
        return try {
            SDLRender.SDL_RenderTextureRotated(ptr, t, srcR, dstR, angle, centerP, flip)
        } finally {
            srcR?.free()
            dstR?.free()
            centerP?.free()
        }
    }

    override fun close() {
        if (ptr == 0L) return
        SDLRender.SDL_DestroyRenderer(ptr)
        ptr = 0L
    }
}

// =========================================================================
// actual implementations
// =========================================================================

actual object SDL {

    // LWJGL bundles SDL3 and loads its native library on class load; on the
    // JVM there is no SDL_main hijacking to guard against.
    actual fun setMainReady() {
        SDLMain.SDL_SetMainReady()
    }

    actual fun init(flags: Int): Boolean = SDLInit.SDL_Init(flags)

    actual fun initSubSystem(flags: Int): Boolean = SDLInit.SDL_InitSubSystem(flags)

    actual fun quitSubSystem(flags: Int) {
        SDLInit.SDL_QuitSubSystem(flags)
    }

    actual fun wasInit(flags: Int): Int = SDLInit.SDL_WasInit(flags)

    actual fun quit() {
        SDLInit.SDL_Quit()
    }

    actual fun error(): String? = SDLError.SDL_GetError()?.takeIf { it.isNotEmpty() }

    actual fun clearError() {
        SDLError.SDL_ClearError()
    }

    actual fun setError(message: String): Boolean = SDLError.SDL_SetError(message)

    actual fun version(): SDLVersion {
        val num = LwjglSDLVersion.SDL_GetVersion()
        return SDLVersion(
            major = num / 1000000,
            minor = (num / 1000) % 1000,
            micro = num % 1000,
        )
    }

    actual fun revision(): String? = LwjglSDLVersion.SDL_GetRevision()

    actual fun getTicks(): ULong = SDLTimer.SDL_GetTicks().toULong()

    actual fun performanceCounter(): ULong = SDLTimer.SDL_GetPerformanceCounter().toULong()

    actual fun performanceFrequency(): ULong = SDLTimer.SDL_GetPerformanceFrequency().toULong()

    actual fun delay(ms: Int) {
        SDLTimer.SDL_Delay(ms)
    }

    actual fun pollEvent(): SDLEvent? {
        val event = SDL_Event.calloc()
        try {
            return if (SDLEvents.SDL_PollEvent(event)) event.toSDLEvent() else null
        } finally {
            event.free()
        }
    }

    actual fun waitEvent(): SDLEvent? {
        val event = SDL_Event.calloc()
        try {
            return if (SDLEvents.SDL_WaitEvent(event)) event.toSDLEvent() else null
        } finally {
            event.free()
        }
    }

    actual fun pumpEvents() {
        SDLEvents.SDL_PumpEvents()
    }

    actual fun createWindow(title: String, width: Int, height: Int, flags: ULong): SDLWindow {
        val ptr = SDLVideo.SDL_CreateWindow(title, width, height, flags.toLong())
        check(ptr != 0L) { "SDL_CreateWindow failed: ${SDL.error()}" }
        return JvmSDLWindow(ptr)
    }

    actual fun createRenderer(window: SDLWindow, name: String?, flags: Int): SDLRenderer {
        val windowPtr = (window as? JvmSDLWindow)?.ptr ?: throw IllegalArgumentException(
            "window is not a JVM SDL window",
        )
        val ptr = if (name != null) {
            SDLRender.SDL_CreateRenderer(windowPtr, name)
        } else {
            SDLRender.nSDL_CreateRenderer(windowPtr, 0L)
        }
        check(ptr != 0L) { "SDL_CreateRenderer failed: ${SDL.error()}" }
        return JvmSDLRenderer(ptr)
    }

    actual fun setHint(name: String, value: String): Boolean = SDLHints.SDL_SetHint(name, value)

    actual fun getHint(name: String): String? = SDLHints.SDL_GetHint(name)

    actual fun getClipboardText(): String? = SDLClipboard.SDL_GetClipboardText()

    actual fun setClipboardText(text: String): Boolean = SDLClipboard.SDL_SetClipboardText(text)

    actual fun getNumVideoDrivers(): Int = SDLVideo.SDL_GetNumVideoDrivers()

    actual fun getVideoDriver(index: Int): String? = SDLVideo.SDL_GetVideoDriver(index)

    actual fun getCurrentVideoDriver(): String? = SDLVideo.SDL_GetCurrentVideoDriver()

    actual fun getNumAudioDrivers(): Int = SDLAudio.SDL_GetNumAudioDrivers()

    actual fun getAudioDriver(index: Int): String? = SDLAudio.SDL_GetAudioDriver(index)

    actual fun getCurrentAudioDriver(): String? = SDLAudio.SDL_GetCurrentAudioDriver()

    actual fun textInputActive(windowId: Int): Boolean {
        val window = SDLVideo.SDL_GetWindowFromID(windowId)
        if (window == 0L) return false
        return SDLKeyboard.SDL_TextInputActive(window)
    }

    actual fun startTextInput(windowId: Int): Boolean {
        val window = SDLVideo.SDL_GetWindowFromID(windowId)
        if (window == 0L) return false
        return SDLKeyboard.SDL_StartTextInput(window)
    }

    actual fun stopTextInput(windowId: Int): Boolean {
        val window = SDLVideo.SDL_GetWindowFromID(windowId)
        if (window == 0L) return false
        return SDLKeyboard.SDL_StopTextInput(window)
    }

    actual fun showSimpleMessageBox(title: String, message: String): Boolean =
        SDLMessageBox.SDL_ShowSimpleMessageBox(0, title, message, 0L)

    // ==================== displays ====================

    actual val numDisplays: Int
        get() = SDLVideo.SDL_GetDisplays()?.limit() ?: 0

    actual fun getDisplay(index: Int): SDLDisplay {
        val displays = SDLVideo.SDL_GetDisplays()
            ?: throw IllegalStateException("SDL_GetDisplays failed: ${SDL.error()}")
        require(index in 0 until displays.limit()) { "display index out of range: $index" }
        return JvmSDLDisplay(displays.get(index))
    }

    actual fun getPrimaryDisplay(): SDLDisplay =
        JvmSDLDisplay(SDLVideo.SDL_GetPrimaryDisplay())

    // ==================== renderer drivers ====================

    actual val numRenderDrivers: Int
        get() = SDLRender.SDL_GetNumRenderDrivers()

    actual fun getRenderDriver(index: Int): String? =
        SDLRender.SDL_GetRenderDriver(index)

    actual fun createWindowAndRenderer(
        title: String,
        width: Int,
        height: Int,
        flags: ULong,
    ): Pair<SDLWindow, SDLRenderer> = MemoryStack.stackPush().use { stack ->
        val window = stack.mallocPointer(1)
        val renderer = stack.mallocPointer(1)
        val ok = SDLRender.SDL_CreateWindowAndRenderer(title, width, height, flags.toLong(), window, renderer)
        check(ok) { "SDL_CreateWindowAndRenderer failed: ${SDL.error()}" }
        Pair(JvmSDLWindow(window.get(0)), JvmSDLRenderer(renderer.get(0)))
    }

    // ==================== pixels ====================

    actual fun getPixelFormatName(format: Int): String? =
        SDLPixels.SDL_GetPixelFormatName(format)

    actual fun mapRGB(format: Int, r: Int, g: Int, b: Int): Int {
        val details = SDLPixels.SDL_GetPixelFormatDetails(format)
            ?: throw IllegalStateException("SDL_GetPixelFormatDetails failed")
        return SDLPixels.SDL_MapRGB(details, null, r.toByte(), g.toByte(), b.toByte())
    }

    actual fun mapRGBA(format: Int, r: Int, g: Int, b: Int, a: Int): Int {
        val details = SDLPixels.SDL_GetPixelFormatDetails(format)
            ?: throw IllegalStateException("SDL_GetPixelFormatDetails failed")
        return SDLPixels.SDL_MapRGBA(details, null, r.toByte(), g.toByte(), b.toByte(), a.toByte())
    }

    actual fun getRGBA(format: Int, pixel: Int): SDLColor = MemoryStack.stackPush().use { stack ->
        val details = SDLPixels.SDL_GetPixelFormatDetails(format)
            ?: throw IllegalStateException("SDL_GetPixelFormatDetails failed")
        val r = stack.malloc(1)
        val g = stack.malloc(1)
        val b = stack.malloc(1)
        val a = stack.malloc(1)
        SDLPixels.SDL_GetRGBA(pixel, details, null, r, g, b, a)
        SDLColor(
            r.get(0).toInt() and 0xff,
            g.get(0).toInt() and 0xff,
            b.get(0).toInt() and 0xff,
            a.get(0).toInt() and 0xff,
        )
    }

    // ==================== surfaces ====================

    actual fun createSurface(width: Int, height: Int, format: Int): SDLSurface {
        val surface = LwjglSDLSurface.SDL_CreateSurface(width, height, format)
            ?: throw IllegalStateException("SDL_CreateSurface failed: ${SDL.error()}")
        return JvmSDLSurface(surface, owned = true)
    }

    actual fun loadBMP(path: String): SDLSurface {
        val surface = LwjglSDLSurface.SDL_LoadBMP(path)
            ?: throw IllegalStateException("SDL_LoadBMP failed: ${SDL.error()}")
        return JvmSDLSurface(surface, owned = true)
    }

    // ==================== audio ====================

    actual val audioPlaybackDevices: List<Int>
        get() {
            val devices = SDLAudio.SDL_GetAudioPlaybackDevices()
            return if (devices == null) emptyList() else (0 until devices.limit()).map { devices.get(it) }
        }

    actual val audioRecordingDevices: List<Int>
        get() {
            val devices = SDLAudio.SDL_GetAudioRecordingDevices()
            return if (devices == null) emptyList() else (0 until devices.limit()).map { devices.get(it) }
        }

    actual fun getAudioDeviceName(deviceId: Int): String? =
        SDLAudio.SDL_GetAudioDeviceName(deviceId)

    actual fun openAudioDevice(deviceId: Int, spec: SDLAudioSpec): SDLAudioDevice {
        val specStruct = SDL_AudioSpec.calloc()
        try {
            specStruct.format(spec.format).channels(spec.channels).freq(spec.freq)
            val id = SDLAudio.SDL_OpenAudioDevice(deviceId, specStruct)
            check(id != 0) { "SDL_OpenAudioDevice failed: ${SDL.error()}" }
            return JvmSDLAudioDevice(id, spec, recording = false)
        } finally {
            specStruct.free()
        }
    }

    actual fun openAudioDeviceStream(deviceId: Int, spec: SDLAudioSpec): SDLAudioStream {
        val specStruct = SDL_AudioSpec.calloc()
        try {
            specStruct.format(spec.format).channels(spec.channels).freq(spec.freq)
            val ptr = SDLAudio.SDL_OpenAudioDeviceStream(deviceId, specStruct, null, 0L)
                ?: throw IllegalStateException("SDL_OpenAudioDeviceStream failed: ${SDL.error()}")
            return JvmSDLAudioStream(ptr)
        } finally {
            specStruct.free()
        }
    }

    actual fun createAudioStream(srcSpec: SDLAudioSpec, dstSpec: SDLAudioSpec): SDLAudioStream {
        val srcStruct = SDL_AudioSpec.calloc()
        val dstStruct = SDL_AudioSpec.calloc()
        try {
            srcStruct.format(srcSpec.format).channels(srcSpec.channels).freq(srcSpec.freq)
            dstStruct.format(dstSpec.format).channels(dstSpec.channels).freq(dstSpec.freq)
            val ptr = SDLAudio.SDL_CreateAudioStream(srcStruct, dstStruct)
                ?: throw IllegalStateException("SDL_CreateAudioStream failed: ${SDL.error()}")
            return JvmSDLAudioStream(ptr)
        } finally {
            srcStruct.free()
            dstStruct.free()
        }
    }

    // ==================== keyboard ====================

    actual val keyboardState: ByteArray
        get() {
            val state = SDLKeyboard.SDL_GetKeyboardState()
            if (state == null) return ByteArray(0)
            val out = ByteArray(state.limit())
            state.rewind()
            state.get(out)
            return out
        }

    actual val modState: Int
        get() = SDLKeyboard.SDL_GetModState().toInt()

    actual fun setModState(modState: Int) {
        SDLKeyboard.SDL_SetModState(modState.toShort())
    }

    actual fun getKeyFromScancode(scancode: Int): Int =
        SDLKeyboard.SDL_GetKeyFromScancode(scancode, 0, false)

    actual fun getScancodeFromKey(keycode: Int): Int =
        SDLKeyboard.SDL_GetScancodeFromKey(keycode, null)

    actual fun getKeyName(keycode: Int): String? =
        SDLKeyboard.SDL_GetKeyName(keycode)

    actual fun getScancodeName(scancode: Int): String? =
        SDLKeyboard.SDL_GetScancodeName(scancode)

    // ==================== mouse ====================

    actual val mouseState: SDLMouseState
        get() = MemoryStack.stackPush().use { stack ->
            val x = stack.mallocFloat(1)
            val y = stack.mallocFloat(1)
            val buttons = SDLMouse.SDL_GetMouseState(x, y)
            SDLMouseState(x.get(0), y.get(0), buttons)
        }

    actual val globalMouseState: SDLMouseState
        get() = MemoryStack.stackPush().use { stack ->
            val x = stack.mallocFloat(1)
            val y = stack.mallocFloat(1)
            val buttons = SDLMouse.SDL_GetGlobalMouseState(x, y)
            SDLMouseState(x.get(0), y.get(0), buttons)
        }

    actual fun warpMouseInWindow(windowId: Int, x: Float, y: Float) {
        val window = SDLVideo.SDL_GetWindowFromID(windowId) ?: return
        SDLMouse.SDL_WarpMouseInWindow(window, x, y)
    }

    actual fun captureMouse(enabled: Boolean): Boolean =
        SDLMouse.SDL_CaptureMouse(enabled)

    actual fun showCursor(): Boolean = SDLMouse.SDL_ShowCursor()

    // ==================== joystick / gamepad ====================

    actual val joysticks: List<Int>
        get() {
            val ids = LwjglSDLJoystick.SDL_GetJoysticks()
            return if (ids == null) emptyList() else (0 until ids.limit()).map { ids.get(it) }
        }

    actual fun openJoystick(id: Int): SDLJoystick {
        val ptr = LwjglSDLJoystick.SDL_OpenJoystick(id)
        check(ptr != 0L) { "SDL_OpenJoystick failed: ${SDL.error()}" }
        return JvmSDLJoystick(ptr)
    }

    actual val gamepads: List<Int>
        get() {
            val ids = LwjglSDLGamepad.SDL_GetGamepads()
            return if (ids == null) emptyList() else (0 until ids.limit()).map { ids.get(it) }
        }

    actual fun openGamepad(id: Int): SDLGamepad {
        val ptr = LwjglSDLGamepad.SDL_OpenGamepad(id)
        check(ptr != 0L) { "SDL_OpenGamepad failed: ${SDL.error()}" }
        return JvmSDLGamepad(ptr)
    }

    // ==================== filesystem / misc ====================

    actual val basePath: String?
        get() = SDLFileSystem.SDL_GetBasePath()

    actual fun getPrefPath(orgName: String, appName: String): String? =
        SDLFileSystem.SDL_GetPrefPath(orgName, appName)

    actual fun getUserFolder(folder: Int): String? =
        SDLFileSystem.SDL_GetUserFolder(folder)

    actual fun createDirectory(path: String): Boolean =
        SDLFileSystem.SDL_CreateDirectory(path)

    actual fun removePath(path: String): Boolean =
        SDLFileSystem.SDL_RemovePath(path)

    actual fun renamePath(oldPath: String, newPath: String): Boolean =
        SDLFileSystem.SDL_RenamePath(oldPath, newPath)

    actual val powerInfo: SDLPowerInfo
        get() = MemoryStack.stackPush().use { stack ->
            val seconds = stack.mallocInt(1)
            val percent = stack.mallocInt(1)
            val state = SDLPower.SDL_GetPowerInfo(seconds, percent)
            SDLPowerInfo(state, percent.get(0), seconds.get(0))
        }

    actual fun openURL(url: String): Boolean =
        SDLMisc.SDL_OpenURL(url)

    actual val hasClipboardText: Boolean
        get() = SDLClipboard.SDL_HasClipboardText()

    actual fun getHintBoolean(name: String, defaultValue: Boolean): Boolean =
        SDLHints.SDL_GetHintBoolean(name, defaultValue)

    actual fun showMessageBox(
        flags: Int,
        title: String,
        message: String,
        buttons: List<SDLMessageBoxButton>,
    ): Int {
        val data = SDL_MessageBoxData.calloc()
        val buttonData = SDL_MessageBoxButtonData.calloc(buttons.size)
        val buttonId = MemoryStack.stackGet().mallocInt(1)
        try {
            data.flags(flags)
            data.title(MemoryUtil.memUTF8(title))
            data.message(MemoryUtil.memUTF8(message))
            for (i in buttons.indices) {
                val b = buttons[i]
                buttonData.get(i).flags(b.flags).buttonID(b.id).text(MemoryUtil.memUTF8(b.text))
            }
            data.buttons(buttonData)
            val ok = SDLMessageBox.SDL_ShowMessageBox(data, buttonId)
            check(ok) { "SDL_ShowMessageBox failed: ${SDL.error()}" }
            return buttonId.get(0)
        } finally {
            data.free()
            buttonData.free()
        }
    }
}

// =========================================================================
// JVM (LWJGL) display
// =========================================================================

private fun SDL_DisplayMode.toCommon(): SDLDisplayMode =
    SDLDisplayMode(
        format = format(),
        width = w(),
        height = h(),
        refreshRate = refresh_rate(),
        pixelDensity = pixel_density(),
    )

internal class JvmSDLDisplay(override val id: Int) : SDLDisplay {

    override val name: String?
        get() = SDLVideo.SDL_GetDisplayName(id)

    override val bounds: SDLRect
        get() {
            val r = SDL_Rect.calloc()
            try {
                SDLVideo.SDL_GetDisplayBounds(id, r)
                return SDLRect(r.x(), r.y(), r.w(), r.h())
            } finally {
                r.free()
            }
        }

    override val usableBounds: SDLRect
        get() {
            val r = SDL_Rect.calloc()
            try {
                SDLVideo.SDL_GetDisplayUsableBounds(id, r)
                return SDLRect(r.x(), r.y(), r.w(), r.h())
            } finally {
                r.free()
            }
        }

    override val currentMode: SDLDisplayMode
        get() = SDLVideo.SDL_GetCurrentDisplayMode(id)?.toCommon()
            ?: throw IllegalStateException("SDL_GetCurrentDisplayMode failed")

    override val desktopMode: SDLDisplayMode
        get() = SDLVideo.SDL_GetDesktopDisplayMode(id)?.toCommon()
            ?: throw IllegalStateException("SDL_GetDesktopDisplayMode failed")

    override val primary: Boolean
        get() = SDLVideo.SDL_GetPrimaryDisplay() == id
}

// =========================================================================
// JVM (LWJGL) texture
// =========================================================================

internal class JvmSDLTexture internal constructor(
    internal var texture: SDL_Texture?,
    internal val renderer: JvmSDLRenderer,
) : SDLTexture {

    private fun check(): SDL_Texture =
        texture ?: throw IllegalStateException("SDL texture is closed")

    override val format: Int
        get() = throw UnsupportedOperationException("texture format is not queryable")

    override val access: Int
        get() = throw UnsupportedOperationException("texture access is not queryable")

    override val size: SDLFloatPoint
        get() = MemoryStack.stackPush().use { stack ->
            val w = stack.mallocFloat(1)
            val h = stack.mallocFloat(1)
            SDLRender.SDL_GetTextureSize(check(), w, h)
            SDLFloatPoint(w.get(0), h.get(0))
        }

    override var colorMod: SDLColor
        get() = throw UnsupportedOperationException("texture color mod is not queryable")
        set(value) {
            SDLRender.SDL_SetTextureColorMod(check(), value.r.toByte(), value.g.toByte(), value.b.toByte())
        }

    override var alphaMod: Int
        get() = throw UnsupportedOperationException("texture alpha mod is not queryable")
        set(value) {
            SDLRender.SDL_SetTextureAlphaMod(check(), value.toByte())
        }

    override var blendMode: Int
        get() = throw UnsupportedOperationException("texture blend mode is not queryable")
        set(value) {
            SDLRender.SDL_SetTextureBlendMode(check(), value)
        }

    override var scaleMode: Int
        get() = throw UnsupportedOperationException("texture scale mode is not queryable")
        set(value) {
            SDLRender.SDL_SetTextureScaleMode(check(), value)
        }

    override fun update(rect: SDLRect?, pixels: ByteArray, pitch: Int): Boolean {
        val rectStruct = rect?.let {
            SDL_Rect.calloc().also { r -> r.x(it.x).y(it.y).w(it.width).h(it.height) }
        }
        val buffer = MemoryUtil.memAlloc(pixels.size)
        try {
            buffer.put(pixels).flip()
            return SDLRender.SDL_UpdateTexture(check(), rectStruct, buffer, pitch)
        } finally {
            rectStruct?.free()
            MemoryUtil.memFree(buffer)
        }
    }

    override fun lock(rect: SDLRect?): SDLTextureLock? = MemoryStack.stackPush().use { stack ->
        val rectStruct = rect?.let {
            SDL_Rect.calloc().also { r -> r.x(it.x).y(it.y).w(it.width).h(it.height) }
        }
        val pixelPtr = stack.mallocPointer(1)
        val pitch = stack.mallocInt(1)
        try {
            val ok = SDLRender.SDL_LockTexture(check(), rectStruct, pixelPtr, pitch)
            if (!ok) {
                null
            } else {
                val address = pixelPtr.get(0)
                val bytes = (if (rect == null) size else SDLFloatPoint(rect.width.toFloat(), rect.height.toFloat()))
                val byteCount = bytes.x.toInt() * bytes.y.toInt() * 4
                val data = ByteArray(byteCount)
                MemoryUtil.memByteBuffer(address, byteCount).get(data)
                SDLTextureLock(data, pitch.get(0))
            }
        } finally {
            rectStruct?.free()
        }
    }

    override fun unlock() {
        SDLRender.SDL_UnlockTexture(check())
    }

    override fun close() {
        val t = texture ?: return
        texture = null
        SDLRender.SDL_DestroyTexture(t)
    }
}

// =========================================================================
// JVM (LWJGL) surface
// =========================================================================

internal class JvmSDLSurface internal constructor(
    internal var surface: SDL_Surface?,
    internal val owned: Boolean,
) : cn.enaium.sdl.SDLSurface {

    internal fun check(): SDL_Surface =
        surface ?: throw IllegalStateException("SDL surface is closed")

    override val width: Int get() = check().w()
    override val height: Int get() = check().h()
    override val format: Int get() = check().format()
    override val pitch: Int get() = check().pitch()

    override val pixels: ByteArray
        get() {
            val buffer = check().pixels() ?: return ByteArray(0)
            val out = ByteArray(buffer.limit())
            buffer.rewind()
            buffer.get(out)
            return out
        }

    override fun lock(): Boolean = LwjglSDLSurface.SDL_LockSurface(check())

    override fun unlock() {
        LwjglSDLSurface.SDL_UnlockSurface(check())
    }

    override fun fillRect(rect: SDLRect?, color: SDLColor): Boolean {
        val r = rect?.let {
            SDL_Rect.calloc().also { s -> s.x(it.x).y(it.y).w(it.width).h(it.height) }
        }
        return try {
            LwjglSDLSurface.SDL_FillSurfaceRect(check(), r, SDLPixels.SDL_MapRGBA(
                SDLPixels.SDL_GetPixelFormatDetails(check().format()) ?: return false,
                null,
                color.r.toByte(),
                color.g.toByte(),
                color.b.toByte(),
                color.a.toByte(),
            ))
        } finally {
            r?.free()
        }
    }

    override fun blit(src: SDLRect?, dst: SDLSurface, dstRect: SDLRect?): Boolean {
        val jvmDst = (dst as? JvmSDLSurface)?.check()
            ?: throw IllegalArgumentException("dst is not a JVM SDL surface")
        val srcR = src?.let {
            SDL_Rect.calloc().also { s -> s.x(it.x).y(it.y).w(it.width).h(it.height) }
        }
        val dstR = dstRect?.let {
            SDL_Rect.calloc().also { s -> s.x(it.x).y(it.y).w(it.width).h(it.height) }
        }
        return try {
            LwjglSDLSurface.SDL_BlitSurface(check(), srcR, jvmDst, dstR)
        } finally {
            srcR?.free()
            dstR?.free()
        }
    }

    override fun saveBMP(path: String): Boolean = LwjglSDLSurface.SDL_SaveBMP(check(), path)

    override fun convert(format: Int): SDLSurface {
        val converted = LwjglSDLSurface.SDL_ConvertSurface(check(), format)
            ?: throw IllegalStateException("SDL_ConvertSurface failed: ${SDL.error()}")
        return JvmSDLSurface(converted, owned = true)
    }

    override fun close() {
        val s = surface ?: return
        surface = null
        if (owned) {
            LwjglSDLSurface.SDL_DestroySurface(s)
        }
    }
}

// =========================================================================
// JVM (LWJGL) audio
// =========================================================================

internal class JvmSDLAudioDevice internal constructor(
    internal val deviceId: Int,
    internal val spec: SDLAudioSpec,
    internal val recording: Boolean,
) : SDLAudioDevice {

    override val id: Int get() = deviceId

    override val format: SDLAudioSpec
        get() {
            val s = SDL_AudioSpec.calloc()
            return try {
                val frames = MemoryStack.stackGet().mallocInt(1)
                SDLAudio.SDL_GetAudioDeviceFormat(deviceId, s, frames)
                SDLAudioSpec(format = s.format(), channels = s.channels(), freq = s.freq())
            } finally {
                s.free()
            }
        }

    override val isRecording: Boolean get() = recording

    override fun bindStream(stream: SDLAudioStream): Boolean {
        val native = stream as? JvmSDLAudioStream
            ?: throw IllegalArgumentException("stream is not a JVM SDL audio stream")
        return SDLAudio.SDL_BindAudioStream(deviceId, native.ptr)
    }

    override fun unbindStream(stream: SDLAudioStream) {
        val native = stream as? JvmSDLAudioStream
            ?: throw IllegalArgumentException("stream is not a JVM SDL audio stream")
        SDLAudio.SDL_UnbindAudioStream(native.ptr)
    }

    override fun close() {
        SDLAudio.SDL_CloseAudioDevice(deviceId)
    }
}

internal class JvmSDLAudioStream internal constructor(
    internal var ptr: Long,
) : SDLAudioStream {

    override fun putData(data: ByteArray): Boolean {
        val buffer = MemoryUtil.memAlloc(data.size)
        try {
            buffer.put(data).flip()
            return SDLAudio.SDL_PutAudioStreamData(ptr, buffer)
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    override fun getData(maxLen: Int): ByteArray {
        val buffer = MemoryUtil.memAlloc(maxLen)
        try {
            val read = SDLAudio.SDL_GetAudioStreamData(ptr, buffer)
            val out = ByteArray(read)
            buffer.rewind()
            buffer.get(out)
            return out
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    override val available: Int
        get() = SDLAudio.SDL_GetAudioStreamAvailable(ptr)

    override val queued: Int
        get() = SDLAudio.SDL_GetAudioStreamQueued(ptr)

    override fun flush(): Boolean = SDLAudio.SDL_FlushAudioStream(ptr)

    override fun clear(): Boolean = SDLAudio.SDL_ClearAudioStream(ptr)

    override fun close() {
        SDLAudio.SDL_DestroyAudioStream(ptr)
        ptr = 0L
    }
}

// =========================================================================
// JVM (LWJGL) joystick / gamepad
// =========================================================================

internal class JvmSDLJoystick internal constructor(
    internal var ptr: Long,
) : SDLJoystick {

    override val id: Int get() = LwjglSDLJoystick.SDL_GetJoystickID(ptr)
    override val name: String? get() = LwjglSDLJoystick.SDL_GetJoystickName(ptr)
    override val type: Int get() = LwjglSDLJoystick.SDL_GetJoystickType(ptr)
    override val numAxes: Int get() = LwjglSDLJoystick.SDL_GetNumJoystickAxes(ptr)
    override val numBalls: Int get() = LwjglSDLJoystick.SDL_GetNumJoystickBalls(ptr)
    override val numHats: Int get() = LwjglSDLJoystick.SDL_GetNumJoystickHats(ptr)
    override val numButtons: Int get() = LwjglSDLJoystick.SDL_GetNumJoystickButtons(ptr)

    override fun axis(axis: Int): Short = LwjglSDLJoystick.SDL_GetJoystickAxis(ptr, axis)

    override fun button(button: Int): Boolean = LwjglSDLJoystick.SDL_GetJoystickButton(ptr, button)

    override fun hat(hat: Int): UByte = LwjglSDLJoystick.SDL_GetJoystickHat(ptr, hat).toUByte()

    override fun ball(ball: Int): SDLPoint? = MemoryStack.stackPush().use { stack ->
        val dx = stack.mallocInt(1)
        val dy = stack.mallocInt(1)
        if (LwjglSDLJoystick.SDL_GetJoystickBall(ptr, ball, dx, dy)) {
            SDLPoint(dx.get(0), dy.get(0))
        } else {
            null
        }
    }

    override fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean =
        LwjglSDLJoystick.SDL_RumbleJoystick(ptr, lowFrequency.toShort(), highFrequency.toShort(), durationMs)

    override fun close() {
        if (ptr != 0L) {
            LwjglSDLJoystick.SDL_CloseJoystick(ptr)
            ptr = 0L
        }
    }
}

internal class JvmSDLGamepad internal constructor(
    internal var ptr: Long,
) : SDLGamepad {

    override val id: Int get() = LwjglSDLGamepad.SDL_GetGamepadID(ptr)
    override val name: String? get() = LwjglSDLGamepad.SDL_GetGamepadName(ptr)
    override val vendor: Int get() = LwjglSDLGamepad.SDL_GetGamepadVendor(ptr).toInt()
    override val product: Int get() = LwjglSDLGamepad.SDL_GetGamepadProduct(ptr).toInt()
    override val serial: String? get() = LwjglSDLGamepad.SDL_GetGamepadSerial(ptr)
    override val connected: Boolean get() = LwjglSDLGamepad.SDL_GamepadConnected(ptr)

    override fun button(button: Int): Boolean = LwjglSDLGamepad.SDL_GetGamepadButton(ptr, button)

    override fun axis(axis: Int): Short = LwjglSDLGamepad.SDL_GetGamepadAxis(ptr, axis)

    override fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean =
        LwjglSDLGamepad.SDL_RumbleGamepad(ptr, lowFrequency.toShort(), highFrequency.toShort(), durationMs)

    override fun close() {
        if (ptr != 0L) {
            LwjglSDLGamepad.SDL_CloseGamepad(ptr)
            ptr = 0L
        }
    }
}
