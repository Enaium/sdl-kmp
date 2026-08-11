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
            ?: throw IllegalStateException("SDL_CreateWindow failed: ${error()}")
        return NativeSDLWindow(ptr)
    }

    actual fun createRenderer(window: SDLWindow, name: String?, flags: Int): SDLRenderer {
        val windowPtr = (window as? NativeSDLWindow)?.check()
            ?: throw IllegalArgumentException("window is not a native SDL window")
        // SDL3 dropped the renderer flags parameter; use SDL_CreateRendererWithProperties
        // if flags are needed.
        val ptr = SDL_CreateRenderer(windowPtr, name)
            ?: throw IllegalStateException("SDL_CreateRenderer failed: ${error()}")
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
}
