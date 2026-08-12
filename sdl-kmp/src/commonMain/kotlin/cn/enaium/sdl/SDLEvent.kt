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
object SDLEventType {
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
    const val DISPLAY_FIRST = 0x150
    const val DISPLAY_ORIENTATION = 0x151
    const val DISPLAY_ADDED = 0x152
    const val DISPLAY_REMOVED = 0x153
    const val DISPLAY_MOVED = 0x154
    const val DISPLAY_DESKTOP_MODE_CHANGED = 0x155
    const val DISPLAY_CURRENT_MODE_CHANGED = 0x156
    const val DISPLAY_CONTENT_SCALE_CHANGED = 0x157
    const val DISPLAY_FIRST_DISPLAY = 0x158
    const val DROP_FIRST = 0x1000
    const val DROP_FILE = 0x1000
    const val DROP_TEXT = 0x1001
    const val DROP_BEGIN = 0x1002
    const val DROP_COMPLETE = 0x1003
    const val DROP_POSITION = 0x1004
    const val CLIPBOARD_UPDATE = 0x1500
    const val SENSOR_FIRST = 0x1200
    const val SENSOR_UPDATE = 0x1200
    const val JOYSTICK_FIRST = 0x600
    const val JOYSTICK_AXIS_MOTION = 0x600
    const val JOYSTICK_BALL_MOTION = 0x601
    const val JOYSTICK_HAT_MOTION = 0x602
    const val JOYSTICK_BUTTON_DOWN = 0x603
    const val JOYSTICK_BUTTON_UP = 0x604
    const val JOYSTICK_ADDED = 0x605
    const val JOYSTICK_REMOVED = 0x606
    const val JOYSTICK_BATTERY_UPDATED = 0x607
    const val GAMEPAD_FIRST = 0x650
    const val GAMEPAD_AXIS_MOTION = 0x650
    const val GAMEPAD_BUTTON_DOWN = 0x651
    const val GAMEPAD_BUTTON_UP = 0x652
    const val GAMEPAD_ADDED = 0x653
    const val GAMEPAD_REMOVED = 0x654
    const val GAMEPAD_TOUCHPAD_DOWN = 0x655
    const val GAMEPAD_TOUCHPAD_MOTION = 0x656
    const val GAMEPAD_TOUCHPAD_UP = 0x657
    const val GAMEPAD_SENSOR_UPDATE = 0x658
    const val AUDIO_DEVICE_FIRST = 0x1100
    const val AUDIO_DEVICE_ADDED = 0x1100
    const val AUDIO_DEVICE_REMOVED = 0x1101
    const val AUDIO_DEVICE_FORMAT_CHANGED = 0x1102
    const val CAMERA_DEVICE_FIRST = 0x1400
    const val CAMERA_DEVICE_ADDED = 0x1400
    const val CAMERA_DEVICE_REMOVED = 0x1401
    const val CAMERA_DEVICE_APPROVED = 0x1402
    const val CAMERA_DEVICE_DENIED = 0x1403
    const val RENDER_DEVICE_RESET = 0x2000
    const val RENDER_TARGETS_RESET = 0x2001
    const val TOUCH_FIRST = 0x700
    const val FINGER_DOWN = 0x700
    const val FINGER_UP = 0x701
    const val FINGER_MOTION = 0x702
    const val USER_EVENT = 0x8000
}

/**
 * An SDL event, translated into a platform-independent sealed hierarchy.
 *
 * Only a curated subset of SDL3's event types is decoded; anything else is
 * surfaced as [Unknown] (its raw [SDLEventType] value is preserved).
 */
sealed class SDLEvent {

    /** Monotonic timestamp in milliseconds, from SDL_GetTicks. */
    abstract val timestamp: ULong

    /** The user requested to quit. */
    data class Quit(override val timestamp: ULong) : SDLEvent()

    /** A window state change; see [SDLWindowEventType]. */
    data class Window(
        override val timestamp: ULong,
        val windowId: Int,
        val type: Int,
        val data1: Int,
        val data2: Int,
    ) : SDLEvent()

    /** A keyboard key press or release. */
    data class Key(
        override val timestamp: ULong,
        val windowId: Int,
        val down: Boolean,
        val repeat: Boolean,
        val keycode: Int,
        val scancode: Int,
        val modifiers: Int,
    ) : SDLEvent()

    /** Text entered through the input method; see [SDL.textInputActive]. */
    data class TextInput(
        override val timestamp: ULong,
        val windowId: Int,
        val text: String,
    ) : SDLEvent()

    /** The mouse moved. Coordinates are in window pixels. */
    data class MouseMotion(
        override val timestamp: ULong,
        val windowId: Int,
        val x: Float,
        val y: Float,
        val dx: Float,
        val dy: Float,
    ) : SDLEvent()

    /** A mouse button was pressed or released; see [SDLMouseButton]. */
    data class MouseButton(
        override val timestamp: ULong,
        val windowId: Int,
        val down: Boolean,
        val button: Int,
        val clicks: Int,
        val x: Float,
        val y: Float,
    ) : SDLEvent()

    /** The mouse wheel moved; see [SDLMouseWheelDirection]. */
    data class MouseWheel(
        override val timestamp: ULong,
        val windowId: Int,
        val x: Float,
        val y: Float,
        val direction: Int,
    ) : SDLEvent()

    /** A display event; see SDLEventType.DISPLAY_*. */
    data class Display(
        override val timestamp: ULong,
        val displayId: Int,
        val type: Int,
        val data1: Int,
        val data2: Int,
    ) : SDLEvent()

    /** A file or text was dropped onto the window. */
    data class Drop(
        override val timestamp: ULong,
        val windowId: Int,
        val type: Int,
        val file: String,
    ) : SDLEvent()

    /** A joystick device event. */
    data class JoyDevice(
        override val timestamp: ULong,
        val deviceId: Int,
        val type: Int,
    ) : SDLEvent()

    /** A joystick axis moved. */
    data class JoyAxis(
        override val timestamp: ULong,
        val deviceId: Int,
        val axis: Int,
        val value: Short,
    ) : SDLEvent()

    /** A joystick ball moved. */
    data class JoyBall(
        override val timestamp: ULong,
        val deviceId: Int,
        val ball: Int,
        val dx: Int,
        val dy: Int,
    ) : SDLEvent()

    /** A joystick hat moved. */
    data class JoyHat(
        override val timestamp: ULong,
        val deviceId: Int,
        val hat: Int,
        val value: UByte,
    ) : SDLEvent()

    /** A joystick button changed. */
    data class JoyButton(
        override val timestamp: ULong,
        val deviceId: Int,
        val button: Int,
        val down: Boolean,
    ) : SDLEvent()

    /** A gamepad device event. */
    data class GamepadDevice(
        override val timestamp: ULong,
        val deviceId: Int,
        val type: Int,
    ) : SDLEvent()

    /** A gamepad axis moved. */
    data class GamepadAxis(
        override val timestamp: ULong,
        val deviceId: Int,
        val axis: Int,
        val value: Short,
    ) : SDLEvent()

    /** A gamepad button changed. */
    data class GamepadButton(
        override val timestamp: ULong,
        val deviceId: Int,
        val button: Int,
        val down: Boolean,
    ) : SDLEvent()

    /** A gamepad touchpad finger changed. */
    data class GamepadTouchpad(
        override val timestamp: ULong,
        val deviceId: Int,
        val touchpad: Int,
        val finger: Int,
        val down: Boolean,
        val x: Float,
        val y: Float,
        val pressure: Float,
    ) : SDLEvent()

    /** A gamepad sensor reading. */
    data class GamepadSensor(
        override val timestamp: ULong,
        val deviceId: Int,
        val sensor: Int,
        val data: FloatArray,
    ) : SDLEvent()

    /** A joystick/gamepad battery level changed. */
    data class JoyBattery(
        override val timestamp: ULong,
        val deviceId: Int,
        val state: Int,
        val percent: Int,
    ) : SDLEvent()

    /** A sensor reading (device sensors, see [SDLSensorType]). */
    data class SensorUpdate(
        override val timestamp: ULong,
        val sensorId: ULong,
        val data: FloatArray,
    ) : SDLEvent()

    /** A camera device was added or removed. */
    data class CameraDevice(
        override val timestamp: ULong,
        val deviceId: Int,
        val type: Int,
    ) : SDLEvent()

    /** A touch finger event. */
    data class TouchFinger(
        override val timestamp: ULong,
        val touchId: ULong,
        val fingerId: ULong,
        val type: Int,
        val x: Float,
        val y: Float,
        val dx: Float,
        val dy: Float,
        val pressure: Float,
    ) : SDLEvent()

    /** An audio device event. */
    data class AudioDevice(
        override val timestamp: ULong,
        val deviceId: Int,
        val isCapture: Boolean,
        val type: Int,
    ) : SDLEvent()

    /** The clipboard content changed. */
    data class ClipboardUpdate(override val timestamp: ULong, val owner: Boolean) : SDLEvent()

    /** The render targets were reset. */
    data class RenderTargetsReset(override val timestamp: ULong) : SDLEvent()

    /** An event type that this binding does not decode. */
    data class Unknown(override val timestamp: ULong, val type: Int) : SDLEvent()
}
