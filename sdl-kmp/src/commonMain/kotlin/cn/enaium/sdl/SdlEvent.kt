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

/** SDL event type values (SDL_EVENT_*). */
object SdlEventType {
    const val QUIT = 0x100
    const val WINDOW_FIRST = 0x200
    const val KEY_FIRST = 0x300
    const val KEY_DOWN = 0x300
    const val KEY_UP = 0x301
    const val TEXT_EDITING = 0x302
    const val TEXT_INPUT = 0x303
    const val MOUSE_FIRST = 0x400
    const val MOUSE_MOTION = 0x400
    const val MOUSE_BUTTON_DOWN = 0x401
    const val MOUSE_BUTTON_UP = 0x402
    const val MOUSE_WHEEL = 0x403
    const val USER_EVENT = 0x8000
}

/**
 * An SDL event, translated into a platform-independent sealed hierarchy.
 *
 * Only a curated subset of SDL3's event types is decoded; anything else is
 * surfaced as [Unknown] (its raw [SdlEventType] value is preserved).
 */
sealed class SdlEvent {

    /** Monotonic timestamp in milliseconds, from SDL_GetTicks. */
    abstract val timestamp: ULong

    /** The user requested to quit. */
    data class Quit(override val timestamp: ULong) : SdlEvent()

    /** A window state change; see [SdlWindowEventType]. */
    data class Window(
        override val timestamp: ULong,
        val windowId: Int,
        val type: Int,
        val data1: Int,
        val data2: Int,
    ) : SdlEvent()

    /** A keyboard key press or release. */
    data class Key(
        override val timestamp: ULong,
        val windowId: Int,
        val down: Boolean,
        val repeat: Boolean,
        val keycode: Int,
        val scancode: Int,
        val modifiers: Int,
    ) : SdlEvent()

    /** Text entered through the input method; see [Sdl.textInputActive]. */
    data class TextInput(
        override val timestamp: ULong,
        val windowId: Int,
        val text: String,
    ) : SdlEvent()

    /** The mouse moved. Coordinates are in window pixels. */
    data class MouseMotion(
        override val timestamp: ULong,
        val windowId: Int,
        val x: Float,
        val y: Float,
        val dx: Float,
        val dy: Float,
    ) : SdlEvent()

    /** A mouse button was pressed or released; see [SdlMouseButton]. */
    data class MouseButton(
        override val timestamp: ULong,
        val windowId: Int,
        val down: Boolean,
        val button: Int,
        val clicks: Int,
        val x: Float,
        val y: Float,
    ) : SdlEvent()

    /** The mouse wheel moved; see [SdlMouseWheelDirection]. */
    data class MouseWheel(
        override val timestamp: ULong,
        val windowId: Int,
        val x: Float,
        val y: Float,
        val direction: Int,
    ) : SdlEvent()

    /** An event type that this binding does not decode. */
    data class Unknown(override val timestamp: ULong, val type: Int) : SdlEvent()
}
