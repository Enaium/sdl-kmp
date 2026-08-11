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

/** A floating-point point. */
data class SDLFloatPoint(val x: Float, val y: Float)

/** A floating-point rectangle. */
data class SDLFRect(val x: Float, val y: Float, val width: Float, val height: Float)

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

// =========================================================================
// Display
// =========================================================================

/** A display mode (values match SDL3's SDL_DisplayMode). */
data class SDLDisplayMode(
    val format: Int,
    val width: Int,
    val height: Int,
    val refreshRate: Float,
    val pixelDensity: Float,
)

/** A monitor/display. */
interface SDLDisplay : AutoCloseable {
    val id: Int
    val name: String?
    val bounds: SDLRect
    val usableBounds: SDLRect
    val currentMode: SDLDisplayMode
    val desktopMode: SDLDisplayMode
    val primary: Boolean
    override fun close() = Unit
}

// =========================================================================
// Renderer / texture
// =========================================================================

/** Blend modes (values match SDL3's SDL_BlendMode). */
object SDLBlendMode {
    const val NONE = 0x00000000
    const val BLEND = 0x00000001
    const val BLEND_PREMULTIPLIED = 0x00000010
    const val ADD = 0x00000002
    const val ADD_PREMULTIPLIED = 0x00000020
    const val MOD = 0x00000004
    const val MUL = 0x00000008
    const val ALPHA = 0x00000040
    const val INVALID = 0x7FFFFFFF
}

/** Texture access modes (values match SDL3's SDL_TextureAccess). */
object SDLTextureAccess {
    const val STATIC = 0
    const val STREAMING = 1
    const val TARGET = 2
}

/** Texture scale modes (values match SDL3's SDL_ScaleMode). */
object SDLScaleMode {
    const val NEAREST = 0
    const val LINEAR = 1
}

/** Pixel formats (values match SDL3's SDL_PixelFormat). */
object SDLPixelFormat {
    const val UNKNOWN = 0
    const val RGB24 = 0x17101803
    const val BGR24 = 0x17401803
    const val ARGB8888 = 0x16362004
    const val RGBA8888 = 0x16462004
    const val ABGR8888 = 0x16762004
    const val BGRA8888 = 0x16862004
    const val RGBA32 = RGBA8888
    const val ARGB32 = ARGB8888
    const val BGRA32 = BGRA8888
    const val ABGR32 = ABGR8888
}

// =========================================================================
// Audio
// =========================================================================

/** Audio sample formats (values match SDL3's SDL_AudioFormat). */
object SDLAudioFormat {
    const val U8 = 0x0008
    const val S8 = 0x8008
    const val S16LE = 0x8010
    const val S16BE = 0x9010
    const val S32LE = 0x8020
    const val S32BE = 0x9020
    const val F32LE = 0x8120
    const val F32BE = 0x9120
}

/** Audio device IDs (values match SDL3's SDL_AudioDeviceID). */
object SDLAudioDeviceID {
    const val DEFAULT_PLAYBACK = 0xFFFFFFFF
    const val DEFAULT_RECORDING = 0xFFFFFFFE
}

/** Audio device description (matches SDL3's SDL_AudioSpec fields used here). */
data class SDLAudioSpec(
    val format: Int = SDLAudioFormat.F32LE,
    val channels: Int = 2,
    val freq: Int = 48000,
)

/** An opened audio device. */
interface SDLAudioDevice : AutoCloseable {
    val id: Int
    val format: SDLAudioSpec
    val isRecording: Boolean

    /** Binds [stream] to this device; returns `false` on failure. */
    fun bindStream(stream: SDLAudioStream): Boolean

    /** Unbinds [stream] from this device. */
    fun unbindStream(stream: SDLAudioStream)

    /** Releases the device. */
    override fun close()
}

/** An audio stream that converts and queues audio data. */
interface SDLAudioStream : AutoCloseable {
    /** Queues [data] (bytes in the stream's input format) for playback. */
    fun putData(data: ByteArray): Boolean

    /** Reads up to `maxLen` converted bytes; returns the number read. */
    fun getData(maxLen: Int): ByteArray

    /** Number of bytes available to read. */
    val available: Int

    /** Number of bytes queued (after conversion). */
    val queued: Int

    fun flush(): Boolean
    fun clear(): Boolean

    /** Releases the stream. */
    override fun close()
}

// =========================================================================
// Input
// =========================================================================

/** Key modifiers (values match SDL3's SDL_Keymod). */
object SDLKeymod {
    const val NONE = 0x0000
    const val LSHIFT = 0x0001
    const val RSHIFT = 0x0002
    const val LCTRL = 0x0040
    const val RCTRL = 0x0080
    const val LALT = 0x0100
    const val RALT = 0x0200
    const val LGUI = 0x0400
    const val RGUI = 0x0800
    const val NUM = 0x1000
    const val CAPS = 0x2000
    const val MODE = 0x4000
    const val CTRL = LCTRL or RCTRL
    const val SHIFT = LSHIFT or RSHIFT
    const val ALT = LALT or RALT
    const val GUI = LGUI or RGUI
}

/** Mouse buttons (values match SDL3's SDL_BUTTON_* masks). */
object SDLMouseButtonMask {
    const val LEFT = 1 shl (1 - 1)
    const val MIDDLE = 1 shl (2 - 1)
    const val RIGHT = 1 shl (3 - 1)
    const val X1 = 1 shl (4 - 1)
    const val X2 = 1 shl (5 - 1)
}

/** An opened joystick. */
interface SDLJoystick : AutoCloseable {
    val id: Int
    val name: String?
    val type: Int
    val numAxes: Int
    val numBalls: Int
    val numHats: Int
    val numButtons: Int

    fun axis(axis: Int): Short
    fun button(button: Int): Boolean
    fun hat(hat: Int): UByte
    fun ball(ball: Int): SDLPoint?
    fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean

    /** Releases the joystick. */
    override fun close()
}

/** An opened gamepad. */
interface SDLGamepad : AutoCloseable {
    val id: Int
    val name: String?
    val vendor: Int
    val product: Int
    val serial: String?
    val connected: Boolean

    fun button(button: Int): Boolean
    fun axis(axis: Int): Short
    fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean

    /** Releases the gamepad. */
    override fun close()
}

// =========================================================================
// Misc
// =========================================================================

/** Power states (values match SDL3's SDL_PowerState). */
object SDLPowerState {
    const val ERROR = -1
    const val UNKNOWN = 0
    const val ON_BATTERY = 1
    const val NO_BATTERY = 2
    const val CHARGING = 3
    const val CHARGED = 4
}

/** Well-known user folders (values match SDL3's SDL_Folder). */
object SDLFolder {
    const val HOME = 0
    const val DESKTOP = 1
    const val DOCUMENTS = 2
    const val DOWNLOADS = 3
}

/** Message box flags (values match SDL3's SDL_MessageBoxFlags). */
object SDLMessageBoxFlags {
    const val ERROR = 0x00000010
    const val WARNING = 0x00000020
    const val INFORMATION = 0x00000040
    const val BUTTONS_LEFT_TO_RIGHT = 0x00000080
    const val BUTTONS_RIGHT_TO_LEFT = 0x00000100
}

/** Message box button flags (values match SDL3's SDL_MessageBoxButtonFlags). */
object SDLMessageBoxButtonFlags {
    const val RETURNKEY_DEFAULT = 0x00000001
    const val ESCAPEKEY_DEFAULT = 0x00000002
}

/** A message box button description. */
data class SDLMessageBoxButton(
    val id: Int,
    val text: String,
    val flags: Int = 0,
)

/** Result of [SDL.getPowerInfo]. */
data class SDLPowerInfo(val state: Int, val percent: Int, val secondsLeft: Int)

// =========================================================================
// OpenGL
// =========================================================================

/** OpenGL context attributes (values match SDL3's SDL_GLAttr). */
object SDLGLAttribute {
    const val RED_SIZE = 0
    const val GREEN_SIZE = 1
    const val BLUE_SIZE = 2
    const val ALPHA_SIZE = 3
    const val BUFFER_SIZE = 4
    const val DOUBLEBUFFER = 5
    const val DEPTH_SIZE = 6
    const val STENCIL_SIZE = 7
    const val ACCUM_RED_SIZE = 8
    const val ACCUM_GREEN_SIZE = 9
    const val ACCUM_BLUE_SIZE = 10
    const val ACCUM_ALPHA_SIZE = 11
    const val STEREO = 12
    const val MULTISAMPLEBUFFERS = 13
    const val MULTISAMPLESAMPLES = 14
    const val ACCELERATED_VISUAL = 15
    const val RETAINED_BACKING = 16
    const val CONTEXT_MAJOR_VERSION = 17
    const val CONTEXT_MINOR_VERSION = 18
    const val CONTEXT_FLAGS = 19
    const val CONTEXT_PROFILE_MASK = 20
    const val SHARE_WITH_CURRENT_CONTEXT = 21
    const val FRAMEBUFFER_SRGB_CAPABLE = 22
    const val CONTEXT_RELEASE_BEHAVIOR = 23
    const val CONTEXT_RESET_NOTIFICATION = 24
    const val CONTEXT_NO_ERROR = 25
    const val FLOATBUFFERS = 26
    const val EGL_PLATFORM = 27
}

/** OpenGL context profiles (values match SDL3's SDL_GLProfile). */
object SDLGLProfile {
    const val CORE = 0x0001
    const val COMPATIBILITY = 0x0002
    const val ES = 0x0004
}

/** OpenGL context flags (values match SDL3's SDL_GLContextFlag). */
object SDLGLContextFlag {
    const val DEBUG = 0x0001
    const val FORWARD_COMPATIBLE = 0x0002
    const val ROBUST_ACCESS = 0x0004
    const val RESET_ISOLATION = 0x0008
}

/** OpenGL context release behaviors (values match SDL3). */
object SDLGLReleaseBehavior {
    const val NONE = 0x0000
    const val FLUSH = 0x0001
}

/** OpenGL context reset notifications (values match SDL3). */
object SDLGLResetNotification {
    const val NO_NOTIFICATION = 0x0000
    const val LOSE_CONTEXT = 0x0001
}
