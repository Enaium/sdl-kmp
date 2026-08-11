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

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cn.enaium.sdl

import kotlinx.cinterop.*
import sdl3.*
import cnames.structs.SDL_Window
import cnames.structs.SDL_Renderer
import cnames.structs.SDL_AudioStream
import cnames.structs.SDL_Joystick
import cnames.structs.SDL_Gamepad
import cnames.structs.SDL_GLContextState
import cnames.structs.VkInstance_T
import cnames.structs.VkPhysicalDevice_T
import cnames.structs.VkSurfaceKHR_T


private fun CPointer<ByteVar>.toKStringOrNull(): String? = toKString()

// =========================================================================
// Native (cinterop) window
// =========================================================================

internal class NativeSDLWindow internal constructor(internal var ptr: CPointer<SDL_Window>?) : SDLWindow {

    internal fun check(): CPointer<SDL_Window> =
        ptr ?: throw IllegalStateException("SDL window is closed")

    override val id: Int
        get() = SDL_GetWindowID(check()).toInt()

    override var title: String
        get() = SDL_GetWindowTitle(check())?.toKString() ?: ""
        set(value) {
            SDL_SetWindowTitle(check(), value)
        }

    override var size: SDLPoint
        get() = memScoped {
            val w = alloc<IntVar>()
            val h = alloc<IntVar>()
            SDL_GetWindowSize(check(), w.ptr, h.ptr)
            SDLPoint(w.value, h.value)
        }
        set(value) {
            SDL_SetWindowSize(check(), value.x, value.y)
        }

    override val flags: ULong
        get() = SDL_GetWindowFlags(check())

    override var position: SDLPoint
        get() = memScoped {
            val x = alloc<IntVar>()
            val y = alloc<IntVar>()
            SDL_GetWindowPosition(check(), x.ptr, y.ptr)
            SDLPoint(x.value, y.value)
        }
        set(value) {
            SDL_SetWindowPosition(check(), value.x, value.y)
        }

    override val sizeInPixels: SDLPoint
        get() = memScoped {
            val w = alloc<IntVar>()
            val h = alloc<IntVar>()
            SDL_GetWindowSizeInPixels(check(), w.ptr, h.ptr)
            SDLPoint(w.value, h.value)
        }

    override val displayId: Int
        get() = SDL_GetDisplayForWindow(check()).toInt()

    override var opacity: Float
        get() = SDL_GetWindowOpacity(check())
        set(value) {
            SDL_SetWindowOpacity(check(), value)
        }

    override var fullscreen: Boolean
        get() = (flags and SDLWindowFlags.FULLSCREEN) != 0uL
        set(value) {
            SDL_SetWindowFullscreen(check(), value)
        }

    override var bordered: Boolean
        get() = (flags and SDLWindowFlags.BORDERLESS) == 0uL
        set(value) {
            SDL_SetWindowBordered(check(), value)
        }

    override var resizable: Boolean
        get() = (flags and SDLWindowFlags.RESIZABLE) != 0uL
        set(value) {
            SDL_SetWindowResizable(check(), value)
        }

    override var alwaysOnTop: Boolean
        get() = (flags and SDLWindowFlags.ALWAYS_ON_TOP) != 0uL
        set(value) {
            SDL_SetWindowAlwaysOnTop(check(), value)
        }

    override var mouseGrab: Boolean
        get() = SDL_GetWindowMouseGrab(check())
        set(value) {
            SDL_SetWindowMouseGrab(check(), value)
        }

    override var keyboardGrab: Boolean
        get() = SDL_GetWindowKeyboardGrab(check())
        set(value) {
            SDL_SetWindowKeyboardGrab(check(), value)
        }

    override var relativeMouseMode: Boolean
        get() = SDL_GetWindowRelativeMouseMode(check())
        set(value) {
            SDL_SetWindowRelativeMouseMode(check(), value)
        }

    override var minimumSize: SDLPoint?
        get() = memScoped {
            val w = alloc<IntVar>()
            val h = alloc<IntVar>()
            SDL_GetWindowMinimumSize(check(), w.ptr, h.ptr)
            SDLPoint(w.value, h.value)
        }
        set(value) {
            if (value == null) {
                SDL_SetWindowMinimumSize(check(), 0, 0)
            } else {
                SDL_SetWindowMinimumSize(check(), value.x, value.y)
            }
        }

    override var maximumSize: SDLPoint?
        get() = memScoped {
            val w = alloc<IntVar>()
            val h = alloc<IntVar>()
            SDL_GetWindowMaximumSize(check(), w.ptr, h.ptr)
            SDLPoint(w.value, h.value)
        }
        set(value) {
            if (value == null) {
                SDL_SetWindowMaximumSize(check(), 0, 0)
            } else {
                SDL_SetWindowMaximumSize(check(), value.x, value.y)
            }
        }

    override fun maximize() {
        SDL_MaximizeWindow(check())
    }

    override fun minimize() {
        SDL_MinimizeWindow(check())
    }

    override fun restore() {
        SDL_RestoreWindow(check())
    }

    override fun flash() {
        SDL_FlashWindow(check(), SDL_FlashOperation.SDL_FLASH_CANCEL)
    }

    override val surface: SDLSurface?
        get() {
            val ptr = SDL_GetWindowSurface(check()) ?: return null
            return NativeSDLSurface(ptr, owned = false)
        }

    override fun setIcon(icon: SDLSurface): Boolean {
        val iconPtr = (icon as? NativeSDLSurface)?.check()
            ?: throw IllegalArgumentException("icon is not a native SDL surface")
        return SDL_SetWindowIcon(check(), iconPtr)
    }

    override fun show() {
        SDL_ShowWindow(check())
    }

    override fun hide() {
        SDL_HideWindow(check())
    }

    override fun raise() {
        SDL_RaiseWindow(check())
    }

    override fun close() {
        val window = ptr ?: return
        ptr = null
        SDL_DestroyWindow(window)
    }
}

// =========================================================================
// Native (cinterop) renderer
// =========================================================================

internal class NativeSDLRenderer internal constructor(internal var ptr: CPointer<SDL_Renderer>?) : SDLRenderer {

    internal fun check(): CPointer<SDL_Renderer> =
        ptr ?: throw IllegalStateException("SDL renderer is closed")

    override val name: String?
        get() = SDL_GetRendererName(check())?.toKString()

    override var drawColor: SDLColor
        get() = memScoped {
            val r = alloc<UByteVar>()
            val g = alloc<UByteVar>()
            val b = alloc<UByteVar>()
            val a = alloc<UByteVar>()
            SDL_GetRenderDrawColor(check(), r.ptr, g.ptr, b.ptr, a.ptr)
            SDLColor(r.value.toInt(), g.value.toInt(), b.value.toInt(), a.value.toInt())
        }
        set(value) {
            SDL_SetRenderDrawColor(
                check(),
                value.r.toUByte(),
                value.g.toUByte(),
                value.b.toUByte(),
                value.a.toUByte(),
            )
        }

    override val outputSize: SDLPoint
        get() = memScoped {
            val w = alloc<IntVar>()
            val h = alloc<IntVar>()
            SDL_GetRenderOutputSize(check(), w.ptr, h.ptr)
            SDLPoint(w.value, h.value)
        }

    override val currentOutputSize: SDLPoint
        get() = memScoped {
            val w = alloc<IntVar>()
            val h = alloc<IntVar>()
            SDL_GetCurrentRenderOutputSize(check(), w.ptr, h.ptr)
            SDLPoint(w.value, h.value)
        }

    override var viewport: SDLRect?
        get() = memScoped {
            val r = alloc<SDL_Rect>()
            SDL_GetRenderViewport(check(), r.ptr)
            SDLRect(r.x, r.y, r.w, r.h)
        }
        set(value) {
            if (value == null) {
                SDL_SetRenderViewport(check(), null)
            } else {
                memScoped {
                    val r = alloc<SDL_Rect>()
                    r.x = value.x
                    r.y = value.y
                    r.w = value.width
                    r.h = value.height
                    SDL_SetRenderViewport(check(), r.ptr)
                }
            }
        }

    override var clipRect: SDLRect?
        get() = memScoped {
            val r = alloc<SDL_Rect>()
            SDL_GetRenderClipRect(check(), r.ptr)
            SDLRect(r.x, r.y, r.w, r.h)
        }
        set(value) {
            if (value == null) {
                SDL_SetRenderClipRect(check(), null)
            } else {
                memScoped {
                    val r = alloc<SDL_Rect>()
                    r.x = value.x
                    r.y = value.y
                    r.w = value.width
                    r.h = value.height
                    SDL_SetRenderClipRect(check(), r.ptr)
                }
            }
        }

    override var scale: SDLFloatPoint
        get() = memScoped {
            val x = alloc<FloatVar>()
            val y = alloc<FloatVar>()
            SDL_GetRenderScale(check(), x.ptr, y.ptr)
            SDLFloatPoint(x.value, y.value)
        }
        set(value) {
            SDL_SetRenderScale(check(), value.x, value.y)
        }

    override var blendMode: Int
        get() = memScoped {
            val mode = alloc<SDL_BlendModeVar>()
            SDL_GetRenderDrawBlendMode(check(), mode.ptr)
            mode.value.toInt()
        }
        set(value) {
            SDL_SetRenderDrawBlendMode(check(), value.toUInt())
        }

    override var vsync: Boolean
        get() = memScoped {
            val vsync = alloc<IntVar>()
            SDL_GetRenderVSync(check(), vsync.ptr)
            vsync.value != 0
        }
        set(value) {
            SDL_SetRenderVSync(check(), if (value) 1 else 0)
        }

    override var target: SDLTexture?
        get() {
            val ptr = SDL_GetRenderTarget(check()) ?: return null
            return NativeSDLTexture(ptr, this)
        }
        set(value) {
            val ptr = (value as? NativeSDLTexture)?.ptr
            SDL_SetRenderTarget(check(), ptr)
        }

    override fun clear(): Boolean = SDL_RenderClear(check())

    override fun present() {
        SDL_RenderPresent(check())
    }

    override fun fillRect(rect: SDLRect): Boolean = memScoped {
        val r = alloc<SDL_FRect>()
        r.x = rect.x.toFloat()
        r.y = rect.y.toFloat()
        r.w = rect.width.toFloat()
        r.h = rect.height.toFloat()
        SDL_RenderFillRect(check(), r.ptr)
    }

    override fun drawRect(rect: SDLRect): Boolean = memScoped {
        val r = alloc<SDL_FRect>()
        r.x = rect.x.toFloat()
        r.y = rect.y.toFloat()
        r.w = rect.width.toFloat()
        r.h = rect.height.toFloat()
        SDL_RenderRect(check(), r.ptr)
    }

    override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int): Boolean =
        SDL_RenderLine(check(), x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())

    override fun drawPoint(x: Int, y: Int): Boolean =
        SDL_RenderPoint(check(), x.toFloat(), y.toFloat())

    override fun drawPoints(points: List<SDLPoint>): Boolean = memScoped {
        val arr = allocArray<SDL_FPoint>(points.size)
        for (i in points.indices) {
            arr[i].x = points[i].x.toFloat()
            arr[i].y = points[i].y.toFloat()
        }
        SDL_RenderPoints(check(), arr, points.size)
    }

    override fun createTexture(format: Int, access: Int, width: Int, height: Int): SDLTexture {
        val ptr = SDL_CreateTexture(check(), format.toUInt(), textureAccessOf(access), width, height)
            ?: throw IllegalStateException("SDL_CreateTexture failed: ${SDL.error()}")
        return NativeSDLTexture(ptr, this)
    }

    override fun createTextureFromSurface(surface: SDLSurface): SDLTexture {
        val surfacePtr = (surface as? NativeSDLSurface)?.check()
            ?: throw IllegalArgumentException("surface is not a native SDL surface")
        val ptr = SDL_CreateTextureFromSurface(check(), surfacePtr)
            ?: throw IllegalStateException("SDL_CreateTextureFromSurface failed: ${SDL.error()}")
        return NativeSDLTexture(ptr, this)
    }

    override fun renderTexture(texture: SDLTexture, src: SDLFRect?, dst: SDLFRect?): Boolean =
        memScoped {
            val texturePtr = (texture as? NativeSDLTexture)?.ptr
                ?: throw IllegalArgumentException("texture is not a native SDL texture")
            val srcPtr = src?.let {
                val r = alloc<SDL_FRect>()
                r.x = it.x
                r.y = it.y
                r.w = it.width
                r.h = it.height
                r.ptr
            }
            val dstPtr = dst?.let {
                val r = alloc<SDL_FRect>()
                r.x = it.x
                r.y = it.y
                r.w = it.width
                r.h = it.height
                r.ptr
            }
            SDL_RenderTexture(check(), texturePtr, srcPtr, dstPtr)
        }

    override fun renderTextureRotated(
        texture: SDLTexture,
        src: SDLFRect?,
        dst: SDLFRect?,
        angle: Double,
        center: SDLFloatPoint?,
        flip: Int,
    ): Boolean = memScoped {
        val texturePtr = (texture as? NativeSDLTexture)?.ptr
            ?: throw IllegalArgumentException("texture is not a native SDL texture")
        val srcPtr = src?.let {
            val r = alloc<SDL_FRect>()
            r.x = it.x
            r.y = it.y
            r.w = it.width
            r.h = it.height
            r.ptr
        }
        val dstPtr = dst?.let {
            val r = alloc<SDL_FRect>()
            r.x = it.x
            r.y = it.y
            r.w = it.width
            r.h = it.height
            r.ptr
        }
        val centerPtr = center?.let {
            val p = alloc<SDL_FPoint>()
            p.x = it.x
            p.y = it.y
            p.ptr
        }
        SDL_RenderTextureRotated(check(), texturePtr, srcPtr, dstPtr, angle, centerPtr, flip.toUInt())
    }

    override fun close() {
        val renderer = ptr ?: return
        ptr = null
        SDL_DestroyRenderer(renderer)
    }
}

// =========================================================================
// Event translation
// =========================================================================

private fun SDL_Event.toSDLEvent(): SDLEvent {
    val type = this.type.toInt()
    return when (type) {
        SDLEventType.QUIT -> SDLEvent.Quit(quit.timestamp)
        in SDLEventType.WINDOW_FIRST until SDLEventType.KEY_FIRST ->
            SDLEvent.Window(
                timestamp = window.timestamp,
                windowId = window.windowID.toInt(),
                type = type,
                data1 = window.data1,
                data2 = window.data2,
            )
        SDLEventType.KEY_DOWN, SDLEventType.KEY_UP ->
            SDLEvent.Key(
                timestamp = key.timestamp,
                windowId = key.windowID.toInt(),
                down = key.down,
                repeat = key.repeat,
                keycode = key.key.toInt(),
                scancode = key.scancode.toInt(),
                modifiers = key.mod.toInt(),
            )
        SDLEventType.TEXT_INPUT ->
            SDLEvent.TextInput(
                timestamp = text.timestamp,
                windowId = text.windowID.toInt(),
                text = text.text?.toKString() ?: "",
            )
        SDLEventType.MOUSE_MOTION ->
            SDLEvent.MouseMotion(
                timestamp = motion.timestamp,
                windowId = motion.windowID.toInt(),
                x = motion.x,
                y = motion.y,
                dx = motion.xrel,
                dy = motion.yrel,
            )
        SDLEventType.MOUSE_BUTTON_DOWN, SDLEventType.MOUSE_BUTTON_UP ->
            SDLEvent.MouseButton(
                timestamp = button.timestamp,
                windowId = button.windowID.toInt(),
                down = button.down,
                button = button.button.toInt(),
                clicks = button.clicks.toInt(),
                x = button.x,
                y = button.y,
            )
        SDLEventType.MOUSE_WHEEL ->
            SDLEvent.MouseWheel(
                timestamp = wheel.timestamp,
                windowId = wheel.windowID.toInt(),
                x = wheel.x,
                y = wheel.y,
                direction = wheel.direction.value.toInt(),
            )
        else -> SDLEvent.Unknown(timestamp = type.toULong(), type = type)
    }
}

// =========================================================================
// Enum mappings (cinterop generates enums; the common API uses ints)
// =========================================================================

private fun <T : kotlinx.cinterop.CEnum> enumValueOf(
    entries: List<T>,
    value: Int,
    defaultValue: () -> T,
): T {
    // UInt does not implement Number on Kotlin/Native, so match per type.
    val target = value.toLong()
    entries.forEach {
        val matches = when (val v = it.value) {
            is UInt -> v.toLong() == target
            is Int -> v.toLong() == target
            is UByte -> v.toLong() == target
            is UShort -> v.toLong() == target
            is Long -> v == target
            else -> false
        }
        if (matches) return it
    }
    return defaultValue()
}

private fun textureAccessOf(value: Int): SDL_TextureAccess = enumValueOf(
    SDL_TextureAccess.entries,
    value,
) { SDL_TextureAccess.SDL_TEXTUREACCESS_STATIC }

private fun folderOf(value: Int): SDL_Folder = enumValueOf(
    SDL_Folder.entries,
    value,
) { SDL_Folder.SDL_FOLDER_HOME }

private fun glAttrOf(value: Int): SDL_GLAttr = enumValueOf(
    SDL_GLAttr.entries,
    value,
) { SDL_GLAttr.SDL_GL_RED_SIZE }

// =========================================================================
// Native (cinterop) display
// =========================================================================

internal class NativeSDLDisplay(override val id: Int) : SDLDisplay {

    override val name: String?
        get() = SDL_GetDisplayName(id.toUInt())?.toKString()

    override val bounds: SDLRect
        get() = memScoped {
            val r = alloc<SDL_Rect>()
            SDL_GetDisplayBounds(id.toUInt(), r.ptr)
            SDLRect(r.x, r.y, r.w, r.h)
        }

    override val usableBounds: SDLRect
        get() = memScoped {
            val r = alloc<SDL_Rect>()
            SDL_GetDisplayUsableBounds(id.toUInt(), r.ptr)
            SDLRect(r.x, r.y, r.w, r.h)
        }

    override val currentMode: SDLDisplayMode
        get() {
            val mode = SDL_GetCurrentDisplayMode(id.toUInt())
                ?: throw IllegalStateException("SDL_GetCurrentDisplayMode failed: ${SDL.error()}")
            return SDLDisplayMode(
                format = mode.pointed.format.toInt(),
                width = mode.pointed.w,
                height = mode.pointed.h,
                refreshRate = mode.pointed.refresh_rate,
                pixelDensity = mode.pointed.pixel_density,
            )
        }

    override val desktopMode: SDLDisplayMode
        get() {
            val mode = SDL_GetDesktopDisplayMode(id.toUInt())
                ?: throw IllegalStateException("SDL_GetDesktopDisplayMode failed: ${SDL.error()}")
            return SDLDisplayMode(
                format = mode.pointed.format.toInt(),
                width = mode.pointed.w,
                height = mode.pointed.h,
                refreshRate = mode.pointed.refresh_rate,
                pixelDensity = mode.pointed.pixel_density,
            )
        }

    override val primary: Boolean
        get() = SDL_GetPrimaryDisplay().toInt() == id
}

// =========================================================================
// Native (cinterop) texture
// =========================================================================

internal class NativeSDLTexture internal constructor(
    internal var ptr: CPointer<SDL_Texture>?,
    internal val renderer: NativeSDLRenderer,
) : SDLTexture {

    private fun check(): CPointer<SDL_Texture> =
        ptr ?: throw IllegalStateException("SDL texture is closed")

    override val format: Int
        get() = throw UnsupportedOperationException("texture format is not queryable")

    override val access: Int
        get() = throw UnsupportedOperationException("texture access is not queryable")

    override val size: SDLFloatPoint
        get() = memScoped {
            val w = alloc<FloatVar>()
            val h = alloc<FloatVar>()
            SDL_GetTextureSize(check(), w.ptr, h.ptr)
            SDLFloatPoint(w.value, h.value)
        }

    override var colorMod: SDLColor
        get() = throw UnsupportedOperationException("texture color mod is not queryable")
        set(value) {
            SDL_SetTextureColorMod(
                check(),
                value.r.toUByte(),
                value.g.toUByte(),
                value.b.toUByte(),
            )
        }

    override var alphaMod: Int
        get() = throw UnsupportedOperationException("texture alpha mod is not queryable")
        set(value) {
            SDL_SetTextureAlphaMod(check(), value.toUByte())
        }

    override var blendMode: Int
        get() = throw UnsupportedOperationException("texture blend mode is not queryable")
        set(value) {
            SDL_SetTextureBlendMode(check(), value.toUInt())
        }

    override var scaleMode: Int
        get() = throw UnsupportedOperationException("texture scale mode is not queryable")
        set(value) {
            SDL_SetTextureScaleMode(check(), value)
        }

    override fun update(rect: SDLRect?, pixels: ByteArray, pitch: Int): Boolean = memScoped {
        val rectPtr = rect?.let {
            val r = alloc<SDL_Rect>()
            r.x = it.x
            r.y = it.y
            r.w = it.width
            r.h = it.height
            r.ptr
        }
        val pixelPtr = pixels.usePinned { it.addressOf(0) }
        SDL_UpdateTexture(check(), rectPtr, pixelPtr, pitch)
    }

    override fun lock(rect: SDLRect?): SDLTextureLock? = memScoped {
        val rectPtr = rect?.let {
            val r = alloc<SDL_Rect>()
            r.x = it.x
            r.y = it.y
            r.w = it.width
            r.h = it.height
            r.ptr
        }
        val pixelPtr = alloc<COpaquePointerVar>()
        val pitch = alloc<IntVar>()
        val ok = SDL_LockTexture(check(), rectPtr, pixelPtr.ptr, pitch.ptr)
        if (!ok) {
            null
        } else {
            val size = (if (rect == null) size else SDLFloatPoint(rect.width.toFloat(), rect.height.toFloat()))
            val bytes = size.x.toInt() * size.y.toInt() * 4
            val data = ByteArray(bytes)
            val source = pixelPtr.value ?: return@memScoped null
            copyBytes(data, 0, source, bytes)
            SDLTextureLock(data, pitch.value)
        }
    }

    override fun unlock() {
        SDL_UnlockTexture(check())
    }

    override fun close() {
        val texture = ptr ?: return
        ptr = null
        SDL_DestroyTexture(texture)
    }
}

// =========================================================================
// Native (cinterop) surface
// =========================================================================

internal class NativeSDLSurface internal constructor(
    internal var ptr: CPointer<SDL_Surface>?,
    internal val owned: Boolean,
) : SDLSurface {

    internal fun check(): CPointer<SDL_Surface> =
        ptr ?: throw IllegalStateException("SDL surface is closed")

    override val width: Int
        get() = check().pointed.w

    override val height: Int
        get() = check().pointed.h

    override val format: Int
        get() = check().pointed.format.toInt()

    override val pitch: Int
        get() = check().pointed.pitch

    override val pixels: ByteArray
        get() {
            val bytes = pitch * height
            val out = ByteArray(bytes)
            copyBytes(out, 0, check().pointed.pixels, bytes)
            return out
        }

    override fun lock(): Boolean = SDL_LockSurface(check())

    override fun unlock() {
        SDL_UnlockSurface(check())
    }

    override fun fillRect(rect: SDLRect?, color: SDLColor): Boolean = memScoped {
        val rectPtr = rect?.let {
            val r = alloc<SDL_Rect>()
            r.x = it.x
            r.y = it.y
            r.w = it.width
            r.h = it.height
            r.ptr
        }
        SDL_FillSurfaceRect(check(), rectPtr, color.toUInt())
    }

    override fun blit(src: SDLRect?, dst: SDLSurface, dstRect: SDLRect?): Boolean = memScoped {
        val srcPtr = src?.let {
            val r = alloc<SDL_Rect>()
            r.x = it.x
            r.y = it.y
            r.w = it.width
            r.h = it.height
            r.ptr
        }
        val dstPtr = dstRect?.let {
            val r = alloc<SDL_Rect>()
            r.x = it.x
            r.y = it.y
            r.w = it.width
            r.h = it.height
            r.ptr
        }
        SDL_BlitSurface(check(), srcPtr, (dst as? NativeSDLSurface)?.check(), dstPtr)
    }

    override fun saveBMP(path: String): Boolean = SDL_SaveBMP(check(), path)

    override fun convert(format: Int): SDLSurface {
        val ptr = SDL_ConvertSurface(check(), format.toUInt())
            ?: throw IllegalStateException("SDL_ConvertSurface failed: ${SDL.error()}")
        return NativeSDLSurface(ptr, owned = true)
    }

    override fun close() {
        val surface = ptr ?: return
        ptr = null
        if (owned) {
            SDL_DestroySurface(surface)
        }
    }
}

private fun copyBytes(dst: ByteArray, dstOffset: Int, src: CPointer<out CPointed>?, length: Int) {
    if (src == null) return
    val bytes = src.reinterpret<ByteVar>()
    for (i in 0 until length) {
        dst[dstOffset + i] = bytes[i]
    }
}

private fun SDLColor.toUInt(): UInt =
    (r.toUInt() shl 24) or (g.toUInt() shl 16) or (b.toUInt() shl 8) or a.toUInt()

// =========================================================================
// Native (cinterop) audio
// =========================================================================

internal class NativeSDLAudioDevice internal constructor(
    internal val deviceId: Int,
    internal val spec: SDLAudioSpec,
    internal val recording: Boolean,
) : SDLAudioDevice {

    override val id: Int get() = deviceId

    override val format: SDLAudioSpec
        get() = memScoped {
            val specPtr = alloc<SDL_AudioSpec>()
            val frames = alloc<IntVar>()
            SDL_GetAudioDeviceFormat(deviceId.toUInt(), specPtr.ptr, frames.ptr)
            SDLAudioSpec(
                format = specPtr.format.toInt(),
                channels = specPtr.channels,
                freq = specPtr.freq,
            )
        }

    override val isRecording: Boolean get() = recording

    override fun bindStream(stream: SDLAudioStream): Boolean {
        val native = stream as? NativeSDLAudioStream
            ?: throw IllegalArgumentException("stream is not a native SDL audio stream")
        return SDL_BindAudioStream(deviceId.toUInt(), native.ptr)
    }

    override fun unbindStream(stream: SDLAudioStream) {
        val native = stream as? NativeSDLAudioStream
            ?: throw IllegalArgumentException("stream is not a native SDL audio stream")
        SDL_UnbindAudioStream(native.ptr)
    }

    override fun close() {
        SDL_CloseAudioDevice(deviceId.toUInt())
    }
}

internal class NativeSDLAudioStream internal constructor(
    internal var ptr: CPointer<cnames.structs.SDL_AudioStream>?,
) : SDLAudioStream {

    private fun check(): CPointer<cnames.structs.SDL_AudioStream> =
        ptr ?: throw IllegalStateException("SDL audio stream is closed")

    override fun putData(data: ByteArray): Boolean = memScoped {
        val ok = data.usePinned { pinned ->
            SDL_PutAudioStreamData(check(), pinned.addressOf(0), data.size)
        }
        ok
    }

    override fun getData(maxLen: Int): ByteArray {
        val out = ByteArray(maxLen)
        val read = out.usePinned { pinned ->
            SDL_GetAudioStreamData(check(), pinned.addressOf(0), maxLen)
        }
        return if (read < maxLen) out.copyOf(read) else out
    }

    override val available: Int
        get() = SDL_GetAudioStreamAvailable(check())

    override val queued: Int
        get() = SDL_GetAudioStreamQueued(check())

    override fun flush(): Boolean = SDL_FlushAudioStream(check())

    override fun clear(): Boolean = SDL_ClearAudioStream(check())

    override fun close() {
        val stream = ptr ?: return
        ptr = null
        SDL_DestroyAudioStream(stream)
    }
}

// =========================================================================
// Native (cinterop) joystick / gamepad
// =========================================================================

internal class NativeSDLJoystick internal constructor(
    internal var ptr: CPointer<cnames.structs.SDL_Joystick>?,
) : SDLJoystick {

    private fun check(): CPointer<cnames.structs.SDL_Joystick> =
        ptr ?: throw IllegalStateException("SDL joystick is closed")

    override val id: Int
        get() = SDL_GetJoystickID(check()).toInt()

    override val name: String?
        get() = SDL_GetJoystickName(check())?.toKString()

    override val type: Int
        get() = SDL_GetJoystickType(check()).value.toInt()

    override val numAxes: Int get() = SDL_GetNumJoystickAxes(check())
    override val numBalls: Int get() = SDL_GetNumJoystickBalls(check())
    override val numHats: Int get() = SDL_GetNumJoystickHats(check())
    override val numButtons: Int get() = SDL_GetNumJoystickButtons(check())

    override fun axis(axis: Int): Short = SDL_GetJoystickAxis(check(), axis)

    override fun button(button: Int): Boolean = SDL_GetJoystickButton(check(), button)

    override fun hat(hat: Int): UByte = SDL_GetJoystickHat(check(), hat)

    override fun ball(ball: Int): SDLPoint? = memScoped {
        val dx = alloc<IntVar>()
        val dy = alloc<IntVar>()
        if (SDL_GetJoystickBall(check(), ball, dx.ptr, dy.ptr)) {
            SDLPoint(dx.value, dy.value)
        } else {
            null
        }
    }

    override fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean =
        SDL_RumbleJoystick(check(), lowFrequency.toUShort(), highFrequency.toUShort(), durationMs.toUInt())

    override fun close() {
        val joystick = ptr ?: return
        ptr = null
        SDL_CloseJoystick(joystick)
    }
}

internal class NativeSDLGamepad internal constructor(
    internal var ptr: CPointer<cnames.structs.SDL_Gamepad>?,
) : SDLGamepad {

    private fun check(): CPointer<cnames.structs.SDL_Gamepad> =
        ptr ?: throw IllegalStateException("SDL gamepad is closed")

    override val id: Int
        get() = SDL_GetGamepadID(check()).toInt()

    override val name: String?
        get() = SDL_GetGamepadName(check())?.toKString()

    override val vendor: Int
        get() = SDL_GetGamepadVendor(check()).toInt()

    override val product: Int
        get() = SDL_GetGamepadProduct(check()).toInt()

    override val serial: String?
        get() = SDL_GetGamepadSerial(check())?.toKString()

    override val connected: Boolean
        get() = SDL_GamepadConnected(check())

    override fun button(button: Int): Boolean =
        SDL_GetGamepadButton(check(), button)

    override fun axis(axis: Int): Short =
        SDL_GetGamepadAxis(check(), axis)

    override fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean =
        SDL_RumbleGamepad(check(), lowFrequency.toUShort(), highFrequency.toUShort(), durationMs.toUInt())

    override fun close() {
        val gamepad = ptr ?: return
        ptr = null
        SDL_CloseGamepad(gamepad)
    }
}

// =========================================================================
// actual implementations
// =========================================================================

actual object SDL {

    actual fun setMainReady() {
        SDL_SetMainReady()
    }

    actual fun init(flags: Int): Boolean = SDL_Init(flags.toUInt())

    actual fun initSubSystem(flags: Int): Boolean = SDL_InitSubSystem(flags.toUInt())

    actual fun quitSubSystem(flags: Int) {
        SDL_QuitSubSystem(flags.toUInt())
    }

    actual fun wasInit(flags: Int): Int = SDL_WasInit(flags.toUInt()).toInt()

    actual fun quit() {
        SDL_Quit()
    }

    actual fun error(): String? = SDL_GetError()?.toKString()?.takeIf { it.isNotEmpty() }

    actual fun clearError() {
        SDL_ClearError()
    }

    actual fun setError(message: String): Boolean = SDL_kmp_SetError(message)

    actual fun version(): SDLVersion {
        val num = SDL_GetVersion()
        return SDLVersion(
            major = num / 1000000,
            minor = (num / 1000) % 1000,
            micro = num % 1000,
        )
    }

    actual fun revision(): String? = SDL_GetRevision()?.toKString()

    actual fun getTicks(): ULong = SDL_GetTicks()

    actual fun performanceCounter(): ULong = SDL_GetPerformanceCounter()

    actual fun performanceFrequency(): ULong = SDL_GetPerformanceFrequency()

    actual fun delay(ms: Int) {
        SDL_Delay(ms.toUInt())
    }

    actual fun pollEvent(): SDLEvent? {
        val event = nativeHeap.alloc<SDL_Event>()
        try {
            return if (SDL_PollEvent(event.ptr)) {
                event.toSDLEvent()
            } else {
                null
            }
        } finally {
            nativeHeap.free(event)
        }
    }

    actual fun waitEvent(): SDLEvent? {
        val event = nativeHeap.alloc<SDL_Event>()
        try {
            return if (SDL_WaitEvent(event.ptr)) {
                event.toSDLEvent()
            } else {
                null
            }
        } finally {
            nativeHeap.free(event)
        }
    }

    actual fun pumpEvents() {
        SDL_PumpEvents()
    }

    actual fun createWindow(title: String, width: Int, height: Int, flags: ULong): SDLWindow {
        val ptr = SDL_CreateWindow(title, width, height, flags)
            ?: throw IllegalStateException("SDL_CreateWindow failed: ${SDL.error()}")
        return NativeSDLWindow(ptr)
    }

    actual fun createRenderer(window: SDLWindow, name: String?, flags: Int): SDLRenderer {
        val windowPtr = (window as? NativeSDLWindow)?.check()
            ?: throw IllegalArgumentException("window is not a native SDL window")
        // SDL3 dropped the renderer flags parameter; use SDL_CreateRendererWithProperties
        // if flags are needed.
        val ptr = SDL_CreateRenderer(windowPtr, name)
            ?: throw IllegalStateException("SDL_CreateRenderer failed: ${SDL.error()}")
        return NativeSDLRenderer(ptr)
    }

    actual fun setHint(name: String, value: String): Boolean = SDL_SetHint(name, value)

    actual fun getHint(name: String): String? = SDL_GetHint(name)?.toKString()

    actual fun getClipboardText(): String? = SDL_GetClipboardText()?.toKString()

    actual fun setClipboardText(text: String): Boolean = SDL_SetClipboardText(text)

    actual fun getNumVideoDrivers(): Int = SDL_GetNumVideoDrivers()

    actual fun getVideoDriver(index: Int): String? = SDL_GetVideoDriver(index)?.toKString()

    actual fun getCurrentVideoDriver(): String? = SDL_GetCurrentVideoDriver()?.toKString()

    actual fun getNumAudioDrivers(): Int = SDL_GetNumAudioDrivers()

    actual fun getAudioDriver(index: Int): String? = SDL_GetAudioDriver(index)?.toKString()

    actual fun getCurrentAudioDriver(): String? = SDL_GetCurrentAudioDriver()?.toKString()

    actual fun textInputActive(windowId: Int): Boolean {
        val window = SDL_GetWindowFromID(windowId.toUInt()) ?: return false
        return SDL_TextInputActive(window)
    }

    actual fun startTextInput(windowId: Int): Boolean {
        val window = SDL_GetWindowFromID(windowId.toUInt()) ?: return false
        return SDL_StartTextInput(window)
    }

    actual fun stopTextInput(windowId: Int): Boolean {
        val window = SDL_GetWindowFromID(windowId.toUInt()) ?: return false
        return SDL_StopTextInput(window)
    }

    actual fun showSimpleMessageBox(title: String, message: String): Boolean =
        SDL_ShowSimpleMessageBox(0u, title, message, null)

    // ==================== displays ====================

    actual val numDisplays: Int
        get() = memScoped {
            val count = alloc<IntVar>()
            val displays = SDL_GetDisplays(count.ptr)
            val n = count.value
            if (displays != null) {
                nativeHeap.free(displays)
            }
            n
        }

    actual fun getDisplay(index: Int): SDLDisplay {
        val count = nativeHeap.alloc<IntVar>()
        try {
            val displays = SDL_GetDisplays(count.ptr)
                ?: throw IllegalStateException("SDL_GetDisplays failed: ${SDL.error()}")
            try {
                require(index in 0 until count.value) { "display index out of range: $index" }
                return NativeSDLDisplay(displays[index].toInt())
            } finally {
                nativeHeap.free(displays)
            }
        } finally {
            nativeHeap.free(count)
        }
    }

    actual fun getPrimaryDisplay(): SDLDisplay =
        NativeSDLDisplay(SDL_GetPrimaryDisplay().toInt())

    // ==================== renderer drivers ====================

    actual val numRenderDrivers: Int
        get() = SDL_GetNumRenderDrivers()

    actual fun getRenderDriver(index: Int): String? =
        SDL_GetRenderDriver(index)?.toKString()

    actual fun createWindowAndRenderer(
        title: String,
        width: Int,
        height: Int,
        flags: ULong,
    ): Pair<SDLWindow, SDLRenderer> {
        val window = nativeHeap.alloc<CPointerVar<SDL_Window>>()
        val renderer = nativeHeap.alloc<CPointerVar<SDL_Renderer>>()
        try {
            val ok = SDL_CreateWindowAndRenderer(title, width, height, flags, window.ptr, renderer.ptr)
            check(ok) { "SDL_CreateWindowAndRenderer failed: ${SDL.error()}" }
            return Pair(NativeSDLWindow(window.value), NativeSDLRenderer(renderer.value))
        } finally {
            nativeHeap.free(window)
            nativeHeap.free(renderer)
        }
    }

    // ==================== pixels ====================

    actual fun getPixelFormatName(format: Int): String? =
        SDL_GetPixelFormatName(format.toUInt())?.toKString()

    actual fun mapRGB(format: Int, r: Int, g: Int, b: Int): Int {
        val details = SDL_GetPixelFormatDetails(format.toUInt())
            ?: throw IllegalStateException("SDL_GetPixelFormatDetails failed")
        return SDL_MapRGB(details, null, r.toUByte(), g.toUByte(), b.toUByte()).toInt()
    }

    actual fun mapRGBA(format: Int, r: Int, g: Int, b: Int, a: Int): Int {
        val details = SDL_GetPixelFormatDetails(format.toUInt())
            ?: throw IllegalStateException("SDL_GetPixelFormatDetails failed")
        return SDL_MapRGBA(details, null, r.toUByte(), g.toUByte(), b.toUByte(), a.toUByte()).toInt()
    }

    actual fun getRGBA(format: Int, pixel: Int): SDLColor = memScoped {
        val details = SDL_GetPixelFormatDetails(format.toUInt())
            ?: throw IllegalStateException("SDL_GetPixelFormatDetails failed")
        val r = alloc<UByteVar>()
        val g = alloc<UByteVar>()
        val b = alloc<UByteVar>()
        val a = alloc<UByteVar>()
        SDL_GetRGBA(pixel.toUInt(), details, null, r.ptr, g.ptr, b.ptr, a.ptr)
        SDLColor(r.value.toInt(), g.value.toInt(), b.value.toInt(), a.value.toInt())
    }

    // ==================== surfaces ====================

    actual fun createSurface(width: Int, height: Int, format: Int): SDLSurface {
        val ptr = SDL_CreateSurface(width, height, format.toUInt())
            ?: throw IllegalStateException("SDL_CreateSurface failed: ${SDL.error()}")
        return NativeSDLSurface(ptr, owned = true)
    }

    actual fun loadBMP(path: String): SDLSurface {
        val ptr = SDL_LoadBMP(path)
            ?: throw IllegalStateException("SDL_LoadBMP failed: ${SDL.error()}")
        return NativeSDLSurface(ptr, owned = true)
    }

    // ==================== audio ====================

    actual val audioPlaybackDevices: List<Int>
        get() = memScoped {
            val count = alloc<IntVar>()
            val devices = SDL_GetAudioPlaybackDevices(count.ptr)
            if (devices == null) {
                emptyList()
            } else {
                val list = ArrayList<Int>(count.value)
                for (i in 0 until count.value) {
                    list.add(devices[i].toInt())
                }
                list
            }
        }

    actual val audioRecordingDevices: List<Int>
        get() = memScoped {
            val count = alloc<IntVar>()
            val devices = SDL_GetAudioRecordingDevices(count.ptr)
            if (devices == null) {
                emptyList()
            } else {
                val list = ArrayList<Int>(count.value)
                for (i in 0 until count.value) {
                    list.add(devices[i].toInt())
                }
                list
            }
        }

    actual fun getAudioDeviceName(deviceId: Int): String? =
        SDL_GetAudioDeviceName(deviceId.toUInt())?.toKString()

    actual fun openAudioDevice(deviceId: Int, spec: SDLAudioSpec): SDLAudioDevice {
        val specPtr = nativeHeap.alloc<SDL_AudioSpec>()
        try {
            specPtr.format = spec.format.toUInt()
            specPtr.channels = spec.channels
            specPtr.freq = spec.freq
            val id = SDL_OpenAudioDevice(deviceId.toUInt(), specPtr.ptr)
            check(id != 0u) { "SDL_OpenAudioDevice failed: ${SDL.error()}" }
            return NativeSDLAudioDevice(id.toInt(), spec, recording = false)
        } finally {
            nativeHeap.free(specPtr)
        }
    }

    actual fun openAudioDeviceStream(deviceId: Int, spec: SDLAudioSpec): SDLAudioStream {
        val specPtr = nativeHeap.alloc<SDL_AudioSpec>()
        try {
            specPtr.format = spec.format.toUInt()
            specPtr.channels = spec.channels
            specPtr.freq = spec.freq
            val ptr = SDL_OpenAudioDeviceStream(deviceId.toUInt(), specPtr.ptr, null, null)
                ?: throw IllegalStateException("SDL_OpenAudioDeviceStream failed: ${SDL.error()}")
            return NativeSDLAudioStream(ptr)
        } finally {
            nativeHeap.free(specPtr)
        }
    }

    actual fun createAudioStream(srcSpec: SDLAudioSpec, dstSpec: SDLAudioSpec): SDLAudioStream {
        val srcPtr = nativeHeap.alloc<SDL_AudioSpec>()
        val dstPtr = nativeHeap.alloc<SDL_AudioSpec>()
        try {
            srcPtr.format = srcSpec.format.toUInt()
            srcPtr.channels = srcSpec.channels
            srcPtr.freq = srcSpec.freq
            dstPtr.format = dstSpec.format.toUInt()
            dstPtr.channels = dstSpec.channels
            dstPtr.freq = dstSpec.freq
            val ptr = SDL_CreateAudioStream(srcPtr.ptr, dstPtr.ptr)
                ?: throw IllegalStateException("SDL_CreateAudioStream failed: ${SDL.error()}")
            return NativeSDLAudioStream(ptr)
        } finally {
            nativeHeap.free(srcPtr)
            nativeHeap.free(dstPtr)
        }
    }

    // ==================== keyboard ====================

    actual val keyboardState: ByteArray
        get() = memScoped {
            val numKeys = alloc<IntVar>()
            val state = SDL_GetKeyboardState(numKeys.ptr)
            val n = numKeys.value
            val out = ByteArray(n)
            if (state != null) {
                for (i in 0 until n) {
                    out[i] = if (state[i].value) 1 else 0
                }
            }
            out
        }

    actual val modState: Int
        get() = SDL_GetModState().toInt()

    actual fun setModState(modState: Int) {
        SDL_SetModState(modState.toUShort())
    }

    actual fun getKeyFromScancode(scancode: Int): Int =
        SDL_GetKeyFromScancode(scancode.toUInt(), 0.toUShort(), false).toInt()

    actual fun getScancodeFromKey(keycode: Int): Int =
        SDL_GetScancodeFromKey(keycode.toUInt(), null).toInt()

    actual fun getKeyName(keycode: Int): String? =
        SDL_GetKeyName(keycode.toUInt())?.toKString()

    actual fun getScancodeName(scancode: Int): String? =
        SDL_GetScancodeName(scancode.toUInt())?.toKString()

    // ==================== mouse ====================

    actual val mouseState: SDLMouseState
        get() = memScoped {
            val x = alloc<FloatVar>()
            val y = alloc<FloatVar>()
            val buttons = SDL_GetMouseState(x.ptr, y.ptr)
            SDLMouseState(x.value, y.value, buttons.toInt())
        }

    actual val globalMouseState: SDLMouseState
        get() = memScoped {
            val x = alloc<FloatVar>()
            val y = alloc<FloatVar>()
            val buttons = SDL_GetGlobalMouseState(x.ptr, y.ptr)
            SDLMouseState(x.value, y.value, buttons.toInt())
        }

    actual fun warpMouseInWindow(windowId: Int, x: Float, y: Float) {
        val window = SDL_GetWindowFromID(windowId.toUInt()) ?: return
        SDL_WarpMouseInWindow(window, x, y)
    }

    actual fun captureMouse(enabled: Boolean): Boolean =
        SDL_CaptureMouse(enabled)

    actual fun showCursor(): Boolean = SDL_ShowCursor()

    // ==================== joystick / gamepad ====================

    actual val joysticks: List<Int>
        get() = memScoped {
            val count = alloc<IntVar>()
            val ids = SDL_GetJoysticks(count.ptr)
            if (ids == null) {
                emptyList()
            } else {
                val list = ArrayList<Int>(count.value)
                for (i in 0 until count.value) {
                    list.add(ids[i].toInt())
                }
                list
            }
        }

    actual fun openJoystick(id: Int): SDLJoystick {
        val ptr = SDL_OpenJoystick(id.toUInt())
            ?: throw IllegalStateException("SDL_OpenJoystick failed: ${SDL.error()}")
        return NativeSDLJoystick(ptr)
    }

    actual val gamepads: List<Int>
        get() = memScoped {
            val count = alloc<IntVar>()
            val ids = SDL_GetGamepads(count.ptr)
            if (ids == null) {
                emptyList()
            } else {
                val list = ArrayList<Int>(count.value)
                for (i in 0 until count.value) {
                    list.add(ids[i].toInt())
                }
                list
            }
        }

    actual fun openGamepad(id: Int): SDLGamepad {
        val ptr = SDL_OpenGamepad(id.toUInt())
            ?: throw IllegalStateException("SDL_OpenGamepad failed: ${SDL.error()}")
        return NativeSDLGamepad(ptr)
    }

    // ==================== filesystem / misc ====================

    actual val basePath: String?
        get() {
            val ptr = SDL_GetBasePath() ?: return null
            val s = ptr.toKString()
            SDL_free(ptr)
            return s
        }

    actual fun getPrefPath(orgName: String, appName: String): String? {
        val ptr = SDL_GetPrefPath(orgName, appName) ?: return null
        val s = ptr.toKString()
        SDL_free(ptr)
        return s
    }

    actual fun getUserFolder(folder: Int): String? {
        val ptr = SDL_GetUserFolder(folderOf(folder)) ?: return null
        val s = ptr.toKString()
        SDL_free(ptr)
        return s
    }

    actual fun createDirectory(path: String): Boolean = SDL_CreateDirectory(path)

    actual fun removePath(path: String): Boolean = SDL_RemovePath(path)

    actual fun renamePath(oldPath: String, newPath: String): Boolean =
        SDL_RenamePath(oldPath, newPath)

    actual val powerInfo: SDLPowerInfo
        get() = memScoped {
            val seconds = alloc<IntVar>()
            val percent = alloc<IntVar>()
            val state = SDL_GetPowerInfo(seconds.ptr, percent.ptr)
            SDLPowerInfo(state, percent.value, seconds.value)
        }

    actual fun openURL(url: String): Boolean = SDL_OpenURL(url)

    actual val hasClipboardText: Boolean
        get() = SDL_HasClipboardText()

    actual fun getHintBoolean(name: String, defaultValue: Boolean): Boolean =
        SDL_GetHintBoolean(name, defaultValue)

    actual fun showMessageBox(
        flags: Int,
        title: String,
        message: String,
        buttons: List<SDLMessageBoxButton>,
    ): Int {
        val data = nativeHeap.alloc<SDL_MessageBoxData>()
        val buttonData = nativeHeap.allocArray<SDL_MessageBoxButtonData>(buttons.size)
        val buttonTexts = ArrayList<CPointer<ByteVar>>(buttons.size)
        val titlePtr = nativeHeap.allocArray<ByteVar>(title.length + 1)
        val messagePtr = nativeHeap.allocArray<ByteVar>(message.length + 1)
        val buttonId = nativeHeap.alloc<IntVar>()
        try {
            for (i in title.indices) titlePtr[i] = title[i].code.toByte()
            titlePtr[title.length] = 0
            for (i in message.indices) messagePtr[i] = message[i].code.toByte()
            messagePtr[message.length] = 0

            for (i in buttons.indices) {
                val button = buttons[i]
                val textPtr = nativeHeap.allocArray<ByteVar>(button.text.length + 1)
                for (j in button.text.indices) textPtr[j] = button.text[j].code.toByte()
                textPtr[button.text.length] = 0
                buttonTexts.add(textPtr)
                buttonData[i].flags = button.flags.toUInt()
                buttonData[i].buttonID = button.id
                buttonData[i].text = textPtr
            }

            data.flags = flags.toUInt()
            data.window = null
            data.title = titlePtr
            data.message = messagePtr
            data.numbuttons = buttons.size
            data.buttons = buttonData
            data.colorScheme = null

            val ok = SDL_ShowMessageBox(data.ptr, buttonId.ptr)
            check(ok) { "SDL_ShowMessageBox failed: ${SDL.error()}" }
            return buttonId.value
        } finally {
            buttonTexts.forEach { nativeHeap.free(it) }
            nativeHeap.free(titlePtr)
            nativeHeap.free(messagePtr)
            nativeHeap.free(buttonData)
            nativeHeap.free(data)
            nativeHeap.free(buttonId)
        }
    }

    // ==================== OpenGL ====================

    actual fun glLoadLibrary(path: String?): Boolean = SDL_GL_LoadLibrary(path)

    actual fun glUnloadLibrary() {
        SDL_GL_UnloadLibrary()
    }

    actual fun glGetProcAddress(proc: String): ULong {
        val fn = SDL_GL_GetProcAddress(proc) ?: return 0uL
        return fn.reinterpret<ULongVar>().pointed.value
    }

    actual fun glExtensionSupported(extension: String): Boolean =
        SDL_GL_ExtensionSupported(extension)

    actual fun glResetAttributes() {
        SDL_GL_ResetAttributes()
    }

    actual fun glSetAttribute(attr: Int, value: Int): Boolean =
        SDL_GL_SetAttribute(glAttrOf(attr), value)

    actual fun glGetAttribute(attr: Int): Int? = memScoped {
        val value = alloc<IntVar>()
        if (SDL_GL_GetAttribute(glAttrOf(attr), value.ptr)) {
            value.value
        } else {
            null
        }
    }

    actual fun glCreateContext(windowId: Int): ULong {
        val window = SDL_GetWindowFromID(windowId.toUInt()) ?: return 0uL
        val ctx = SDL_GL_CreateContext(window) ?: return 0uL
        return ctx.reinterpret<ULongVar>().pointed.value
    }

    actual fun glMakeCurrent(windowId: Int, context: ULong): Boolean {
        val window = SDL_GetWindowFromID(windowId.toUInt()) ?: return false
        val ctx = if (context == 0uL) null else context.toLong().toCPointer<SDL_GLContextState>()
        return SDL_GL_MakeCurrent(window, ctx)
    }

    actual val glCurrentWindow: Int?
        get() = SDL_GL_GetCurrentWindow()?.let { SDL_GetWindowID(it).toInt() }

    actual val glCurrentContext: ULong
        get() {
            val ctx = SDL_GL_GetCurrentContext() ?: return 0uL
            return ctx.reinterpret<ULongVar>().pointed.value
        }

    actual fun glSetSwapInterval(interval: Int): Boolean =
        SDL_GL_SetSwapInterval(interval)

    actual val glSwapInterval: Int?
        get() = memScoped {
            val interval = alloc<IntVar>()
            if (SDL_GL_GetSwapInterval(interval.ptr)) {
                interval.value
            } else {
                null
            }
        }

    actual fun glSwapWindow(windowId: Int): Boolean {
        val window = SDL_GetWindowFromID(windowId.toUInt()) ?: return false
        return SDL_GL_SwapWindow(window)
    }

    actual fun glDestroyContext(context: ULong) {
        if (context != 0uL) {
            SDL_GL_DestroyContext(context.toLong().toCPointer<SDL_GLContextState>())
        }
    }

    // ==================== Vulkan ====================

    actual fun vulkanLoadLibrary(path: String?): Boolean = SDL_Vulkan_LoadLibrary(path)

    actual fun vulkanUnloadLibrary() {
        SDL_Vulkan_UnloadLibrary()
    }

    actual val vulkanGetVkGetInstanceProcAddr: ULong
        get() {
            val fn = SDL_Vulkan_GetVkGetInstanceProcAddr() ?: return 0uL
            return fn.reinterpret<ULongVar>().pointed.value
        }

    actual val vulkanInstanceExtensions: List<String>
        get() = memScoped {
            val count = alloc<UIntVar>()
            val names = SDL_Vulkan_GetInstanceExtensions(count.ptr)
            if (names == null) {
                emptyList()
            } else {
                val list = ArrayList<String>(count.value.toInt())
                for (i in 0 until count.value.toInt()) {
                    names[i]?.toKString()?.let { list.add(it) }
                }
                list
            }
        }

    actual fun vulkanCreateSurface(windowId: Int, instance: ULong): ULong {
        val window = SDL_GetWindowFromID(windowId.toUInt()) ?: return 0uL
        val surface = nativeHeap.alloc<VkSurfaceKHRVar>()
        try {
            val instancePtr = if (instance == 0uL) null else instance.toLong().toCPointer<VkInstance_T>()
            val ok = SDL_Vulkan_CreateSurface(window, instancePtr, null, surface.ptr)
            return if (ok) {
                val vkSurface = surface.value ?: return@vulkanCreateSurface 0uL
                vkSurface.reinterpret<ULongVar>().pointed.value
            } else {
                0uL
            }
        } finally {
            nativeHeap.free(surface)
        }
    }

    actual fun vulkanDestroySurface(instance: ULong, surface: ULong) {
        if (surface != 0uL) {
            val instancePtr = if (instance == 0uL) null else instance.toLong().toCPointer<VkInstance_T>()
            SDL_Vulkan_DestroySurface(instancePtr, surface.toLong().toCPointer<VkSurfaceKHR_T>(), null)
        }
    }

    actual fun vulkanGetPresentationSupport(instance: ULong, physicalDevice: ULong, queueFamilyIndex: Int): Boolean {
        val instancePtr = if (instance == 0uL) null else instance.toLong().toCPointer<VkInstance_T>()
        val devicePtr = if (physicalDevice == 0uL) null else physicalDevice.toLong().toCPointer<VkPhysicalDevice_T>()
        return SDL_Vulkan_GetPresentationSupport(instancePtr, devicePtr, queueFamilyIndex.toUInt())
    }
}
