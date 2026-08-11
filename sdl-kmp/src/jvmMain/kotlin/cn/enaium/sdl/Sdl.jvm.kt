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
        check(ptr != 0L) { "SDL_CreateWindow failed: ${error()}" }
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
        check(ptr != 0L) { "SDL_CreateRenderer failed: ${error()}" }
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
}
