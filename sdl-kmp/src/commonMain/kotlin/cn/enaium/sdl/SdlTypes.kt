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

/** Compiled SDL version. */
data class SDLVersion(val major: Int, val minor: Int, val micro: Int)

/** A color with 8-bit components. */
data class SDLColor(val r: Int, val g: Int, val b: Int, val a: Int = 255)

/** An integer point/size. */
data class SDLPoint(val x: Int, val y: Int)

/** An integer rectangle. */
data class SDLRect(val x: Int, val y: Int, val width: Int, val height: Int)

/** Subsystem flags for [SDL.init] and friends (values match SDL3). */
object SDLInitFlags {
    // Note: SDL_INIT_TIMER no longer exists in SDL3; timers need no init.
    const val AUDIO = 0x00000010
    const val VIDEO = 0x00000020
    const val JOYSTICK = 0x00000200
    const val HAPTIC = 0x00001000
    const val GAMEPAD = 0x00002000
    const val EVENTS = 0x00004000
    const val SENSOR = 0x00008000
    const val CAMERA = 0x00010000
}

/** Window creation flags (values match SDL3's Uint64 flags). */
object SDLWindowFlags {
    const val FULLSCREEN: ULong = 0x0000000000000001u
    const val OPENGL: ULong = 0x0000000000000002u
    const val OCCLUDED: ULong = 0x0000000000000004u
    const val HIDDEN: ULong = 0x0000000000000008u
    const val BORDERLESS: ULong = 0x0000000000000010u
    const val RESIZABLE: ULong = 0x0000000000000020u
    const val MINIMIZED: ULong = 0x0000000000000040u
    const val MAXIMIZED: ULong = 0x0000000000000080u
    const val MOUSE_GRABBED: ULong = 0x0000000000000100u
    const val INPUT_FOCUS: ULong = 0x0000000000000200u
    const val MOUSE_FOCUS: ULong = 0x0000000000000400u
    const val EXTERNAL: ULong = 0x0000000000000800u
    const val MODAL: ULong = 0x0000000000001000u
    const val HIGH_PIXEL_DENSITY: ULong = 0x0000000000002000u
    const val MOUSE_CAPTURE: ULong = 0x0000000000004000u
    const val MOUSE_RELATIVE_MODE: ULong = 0x0000000000008000u
    const val ALWAYS_ON_TOP: ULong = 0x0000000000010000u
    const val UTILITY: ULong = 0x0000000000020000u
    const val TOOLTIP: ULong = 0x0000000000040000u
    const val POPUP_MENU: ULong = 0x0000000000080000u
    const val KEYBOARD_GRABBED: ULong = 0x0000000000100000u
    const val VULKAN: ULong = 0x0000000010000000u
    const val METAL: ULong = 0x0000000020000000u
    const val TRANSPARENT: ULong = 0x0000000040000000u
    const val NOT_FOCUSABLE: ULong = 0x0000000080000000u
}

/** Renderer creation flags (values match SDL3). */
object SDLRendererFlags {
    const val PRESENTVSYNC = 0x00000001
    const val ACCELERATED = 0x00000002
    const val GPU = 0x00000004
}

/** Mouse buttons (values match SDL3's SDL_BUTTON_*). */
object SDLMouseButton {
    const val LEFT = 1
    const val MIDDLE = 2
    const val RIGHT = 3
    const val X1 = 4
    const val X2 = 5
}

/** Mouse wheel direction (values match SDL3's SDL_MOUSEWHEEL_*). */
object SDLMouseWheelDirection {
    const val NORMAL = 0
    const val FLIPPED = 1
}

/** Common key codes (values match SDL3's SDLK_*). */
object SDLKeycode {
    const val RETURN = 0x0d
    const val ESCAPE = 0x1b
    const val BACKSPACE = 0x08
    const val TAB = 0x09
    const val SPACE = 0x20
    const val DELETE = 0x7f

    const val a = 0x61
    const val b = 0x62
    const val c = 0x63
    const val d = 0x64
    const val e = 0x65
    const val f = 0x66
    const val g = 0x67
    const val h = 0x68
    const val i = 0x69
    const val j = 0x6a
    const val k = 0x6b
    const val l = 0x6c
    const val m = 0x6d
    const val n = 0x6e
    const val o = 0x6f
    const val p = 0x70
    const val q = 0x71
    const val r = 0x72
    const val s = 0x73
    const val t = 0x74
    const val u = 0x75
    const val v = 0x76
    const val w = 0x77
    const val x = 0x78
    const val y = 0x79
    const val z = 0x7a

    const val LEFT = 0x40000050
    const val RIGHT = 0x4000004f
    const val UP = 0x40000052
    const val DOWN = 0x40000051
}

/** Window event types (values match SDL3's SDL_EVENT_WINDOW_*). */
object SDLWindowEventType {
    const val FIRST = 0x202
    const val SHOWN = 0x202
    const val HIDDEN = 0x203
    const val EXPOSED = 0x204
    const val MOVED = 0x205
    const val RESIZED = 0x206
    const val PIXEL_SIZE_CHANGED = 0x207
    const val METAL_VIEW_RESIZED = 0x208
    const val MINIMIZED = 0x209
    const val MAXIMIZED = 0x20a
    const val RESTORED = 0x20b
    const val MOUSE_ENTER = 0x20c
    const val MOUSE_LEAVE = 0x20d
    const val FOCUS_GAINED = 0x20e
    const val FOCUS_LOST = 0x20f
    const val CLOSE_REQUESTED = 0x210
    const val HIT_TEST = 0x211
    const val ICCPROF_CHANGED = 0x212
    const val DISPLAY_CHANGED = 0x213
    const val DISPLAY_SCALE_CHANGED = 0x214
    const val SAFE_AREA_CHANGED = 0x215
    const val OCCLUDED = 0x216
    const val ENTER_FULLSCREEN = 0x217
    const val LEAVE_FULLSCREEN = 0x218
    const val DESTROYED = 0x219
    const val HDR_STATE_CHANGED = 0x21a
}
