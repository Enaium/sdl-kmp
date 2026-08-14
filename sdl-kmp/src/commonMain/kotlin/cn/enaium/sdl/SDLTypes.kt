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
    const val UNKNOWN = 0x00000000
    const val RETURN = 0x0d
    const val ESCAPE = 0x1b
    const val BACKSPACE = 0x08
    const val TAB = 0x09
    const val SPACE = 0x20
    const val DELETE = 0x7f

    // Printable characters (ASCII values, layout-independent).
    const val EXCLAIM = 0x21
    const val DBLAPOSTROPHE = 0x22
    const val HASH = 0x23
    const val DOLLAR = 0x24
    const val PERCENT = 0x25
    const val AMPERSAND = 0x26
    const val APOSTROPHE = 0x27
    const val LEFTPAREN = 0x28
    const val RIGHTPAREN = 0x29
    const val ASTERISK = 0x2a
    const val PLUS = 0x2b
    const val COMMA = 0x2c
    const val MINUS = 0x2d
    const val PERIOD = 0x2e
    const val SLASH = 0x2f
    const val KEY_0_START = 0x30
    const val KEY_0_END = 0x39
    const val COLON = 0x3a
    const val SEMICOLON = 0x3b
    const val LESS = 0x3c
    const val EQUALS = 0x3d
    const val GREATER = 0x3e
    const val QUESTION = 0x3f
    const val AT = 0x40
    const val LEFTBRACKET = 0x5b
    const val BACKSLASH = 0x5c
    const val RIGHTBRACKET = 0x5d
    const val CARET = 0x5e
    const val UNDERSCORE = 0x5f
    const val GRAVE = 0x60
    const val LEFTBRACE = 0x7b
    const val PIPE = 0x7c
    const val RIGHTBRACE = 0x7d
    const val TILDE = 0x7e

    const val A = 0x61
    const val B = 0x62
    const val C = 0x63
    const val D = 0x64
    const val E = 0x65
    const val F = 0x66
    const val G = 0x67
    const val H = 0x68
    const val I = 0x69
    const val J = 0x6a
    const val K = 0x6b
    const val L = 0x6c
    const val M = 0x6d
    const val N = 0x6e
    const val O = 0x6f
    const val P = 0x70
    const val Q = 0x71
    const val R = 0x72
    const val S = 0x73
    const val T = 0x74
    const val U = 0x75
    const val V = 0x76
    const val W = 0x77
    const val X = 0x78
    const val Y = 0x79
    const val Z = 0x7a

    // Non-printable keys (SDLK_EXTENDED_MASK | scancode).
    const val CAPSLOCK = 0x40000039
    const val F1 = 0x4000003a
    const val F2 = 0x4000003b
    const val F3 = 0x4000003c
    const val F4 = 0x4000003d
    const val F5 = 0x4000003e
    const val F6 = 0x4000003f
    const val F7 = 0x40000040
    const val F8 = 0x40000041
    const val F9 = 0x40000042
    const val F10 = 0x40000043
    const val F11 = 0x40000044
    const val F12 = 0x40000045
    const val PRINTSCREEN = 0x40000046
    const val SCROLLLOCK = 0x40000047
    const val PAUSE = 0x40000048
    const val INSERT = 0x40000049
    const val HOME = 0x4000004a
    const val PAGEUP = 0x4000004b
    const val END = 0x4000004d
    const val PAGEDOWN = 0x4000004e
    const val RIGHT = 0x4000004f
    const val LEFT = 0x40000050
    const val DOWN = 0x40000051
    const val UP = 0x40000052
    const val NUMLOCKCLEAR = 0x40000053
    const val KP_DIVIDE = 0x40000054
    const val KP_MULTIPLY = 0x40000055
    const val KP_MINUS = 0x40000056
    const val KP_PLUS = 0x40000057
    const val KP_ENTER = 0x40000058
    const val KP_1 = 0x40000059
    const val KP_2 = 0x4000005a
    const val KP_3 = 0x4000005b
    const val KP_4 = 0x4000005c
    const val KP_5 = 0x4000005d
    const val KP_6 = 0x4000005e
    const val KP_7 = 0x4000005f
    const val KP_8 = 0x40000060
    const val KP_9 = 0x40000061
    const val KP_0 = 0x40000062
    const val KP_PERIOD = 0x40000063
    const val APPLICATION = 0x40000065
    const val KP_EQUALS = 0x40000067
    const val F13 = 0x40000068
    const val F14 = 0x40000069
    const val F15 = 0x4000006a
    const val F16 = 0x4000006b
    const val F17 = 0x4000006c
    const val F18 = 0x4000006d
    const val F19 = 0x4000006e
    const val F20 = 0x4000006f
    const val F21 = 0x40000070
    const val F22 = 0x40000071
    const val F23 = 0x40000072
    const val F24 = 0x40000073
    const val LCTRL = 0x400000e0
    const val LSHIFT = 0x400000e1
    const val LALT = 0x400000e2
    const val LGUI = 0x400000e3
    const val RCTRL = 0x400000e4
    const val RSHIFT = 0x400000e5
    const val RALT = 0x400000e6
    const val RGUI = 0x400000e7
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

/** Whether the host is little-endian (used to pick SDL's *32 pixel format aliases). */
internal expect val hostIsLittleEndian: Boolean

/** Pixel formats (values match SDL3's SDL_PixelFormat). */
object SDLPixelFormat {
    const val UNKNOWN = 0
    const val RGB24 = 0x17101803
    const val BGR24 = 0x17401803
    const val ARGB8888 = 0x16362004
    const val RGBA8888 = 0x16462004
    const val ABGR8888 = 0x16762004
    const val BGRA8888 = 0x16862004
    // SDL3's *32 aliases map to a format whose in-memory byte order is R,G,B,A,
    // which depends on the host endianness (SDL_BYTEORDER):
    //   little-endian: RGBA32 = ABGR8888,  big-endian: RGBA32 = RGBA8888
    val RGBA32: Int get() = if (hostIsLittleEndian) ABGR8888 else RGBA8888
    val ARGB32: Int get() = if (hostIsLittleEndian) BGRA8888 else ARGB8888
    val BGRA32: Int get() = if (hostIsLittleEndian) ARGB8888 else BGRA8888
    val ABGR32: Int get() = if (hostIsLittleEndian) RGBA8888 else ABGR8888
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
    // SDL3 defines these aliases to the host byte order (SDL_AUDIO_S16 =
    // S16LE on little-endian, S16BE on big-endian, etc.).
    val S16: Int get() = if (hostIsLittleEndian) S16LE else S16BE
    val S32: Int get() = if (hostIsLittleEndian) S32LE else S32BE
    val F32: Int get() = if (hostIsLittleEndian) F32LE else F32BE
}

/** Audio device IDs (values match SDL3's SDL_AudioDeviceID). */
object SDLAudioDeviceID {
    const val DEFAULT_PLAYBACK: Int = -1
    const val DEFAULT_RECORDING: Int = -2
}

/** Audio device description (matches SDL3's SDL_AudioSpec fields used here). */
data class SDLAudioSpec(
    val format: Int = SDLAudioFormat.F32,
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

    /** Pauses playback on this device. */
    fun pause()

    /** Resumes playback on this device. */
    fun resume()

    /** Releases the device. */
    override fun close()
}

/** An audio stream that converts and queues audio data. */
interface SDLAudioStream : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. On native, [cn.enaium.sdl.nativePtr] converts it to the typed pointer. */
    val ptr: Long

    /** Queues [data] (bytes in the stream's input format) for playback. */
    fun putData(data: ByteArray): Boolean

    /** Reads up to `maxLen` converted bytes; returns the number read. */
    fun getData(maxLen: Int): ByteArray

    /** Number of bytes available to read. */
    val available: Int

    /** Number of bytes queued (after conversion). */
    val queued: Int

    /** The stream's input format, or null when unavailable. */
    val inputSpec: SDLAudioSpec?

    /** The stream's output format, or null when unavailable. */
    val outputSpec: SDLAudioSpec?

    /** Changes the stream's input/output format; returns `false` on failure. */
    fun setFormat(src: SDLAudioSpec, dst: SDLAudioSpec): Boolean

    /** The output gain (1.0 = unchanged); setting returns `false` on failure. */
    var gain: Float

    /** The playback speed ratio (1.0 = normal); setting returns `false` on failure. */
    var frequencyRatio: Float

    /**
     * Whether the device playback of this stream is paused. SDL_OpenAudioDeviceStream
     * starts the stream paused, so set this to `false` (or call [resume]) to hear audio.
     */
    var devicePaused: Boolean

    /** Unpauses the stream's device playback (see [devicePaused]). */
    fun resume()

    /** Pauses the stream's device playback (see [devicePaused]). */
    fun pause()

    fun flush(): Boolean
    fun clear(): Boolean

    /** Releases the stream. */
    override fun close()
}

/** A loaded WAV file. */
data class SDLAudioData(val spec: SDLAudioSpec, val data: ByteArray)

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

    /** The raw SDL handle address, or 0 after [close]. On native, [cn.enaium.sdl.nativePtr] converts it to the typed pointer. */
    val ptr: Long

    val name: String?
    val type: Int
    val numAxes: Int
    val numBalls: Int
    val numHats: Int
    val numButtons: Int
    val playerIndex: Int
    val firmwareVersion: Int

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

    /** The raw SDL handle address, or 0 after [close]. On native, [cn.enaium.sdl.nativePtr] converts it to the typed pointer. */
    val ptr: Long

    val name: String?
    val vendor: Int
    val product: Int
    val serial: String?
    val connected: Boolean
    val playerIndex: Int
    val firmwareVersion: Int
    val touchpadCount: Int

    fun button(button: Int): Boolean
    fun axis(axis: Int): Short

    /** The state of the finger on [touchpad]/[finger], or null. */
    fun touchpadFinger(touchpad: Int, finger: Int): SDLTouchpadFinger?

    /** Whether the gamepad has a sensor of [type] (see [SDLSensorType]). */
    fun hasSensor(type: Int): Boolean

    /** The current sensor readings (see [SDLSensorType]), or null on failure. */
    fun sensorData(type: Int): FloatArray?

    /** The update rate of the sensor of [type], or 0 on failure. */
    fun getSensorDataRate(type: Int): Float

    fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean

    /** Releases the gamepad. */
    override fun close()
}

/** A finger on a gamepad touchpad. */
data class SDLTouchpadFinger(
    val touchpad: Int,
    val finger: Int,
    val down: Boolean,
    val x: Float,
    val y: Float,
    val pressure: Float,
)

/** A finger on a touch device. */
data class SDLTouchFinger(
    val id: ULong,
    val x: Float,
    val y: Float,
    val pressure: Float,
    val down: Boolean,
)

/** Touch device types (values match SDL3's SDL_TouchDeviceType). */
object SDLTouchDeviceType {
    const val INVALID = -1
    const val DIRECT = 0
    const val INDIRECT_ABSOLUTE = 1
    const val INDIRECT_RELATIVE = 2
}

/** A vertex for [SDLRenderer.renderGeometry]. */
data class SDLVertex(
    val position: SDLFloatPoint,
    val color: SDLColor = SDLColor(255, 255, 255, 255),
    val texCoord: SDLFloatPoint = SDLFloatPoint(0f, 0f),
)

/** A file type filter for [SDL.showOpenFileDialog]. */
data class SDLDialogFileFilter(val name: String, val pattern: String)

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
