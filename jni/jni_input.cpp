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

// JNI bridge for sdl-kmp: keyboard, mouse, touch, joystick and gamepad.

#include <SDL3/SDL.h>

#include "jni_bridge.h"

// ===========================================================================
// Keyboard
// ===========================================================================

SDLJNI_FUNC(jbyteArray) SDLJNI_NAME(keyboardState)(JNIEnv *env, jclass) {
    int numkeys = 0;
    const bool *state = SDL_GetKeyboardState(&numkeys);
    if (state == nullptr || numkeys <= 0) return nullptr;
    return sdl_kmp_jni_new_byte_array(env, state, numkeys);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(modState)(JNIEnv *, jclass) {
    return static_cast<jint>(SDL_GetModState());
}

SDLJNI_FUNC(void) SDLJNI_NAME(setModState)(JNIEnv *, jclass, jint modState) {
    SDL_SetModState(static_cast<SDL_Keymod>(modState));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getKeyFromScancode)(JNIEnv *, jclass, jint scancode) {
    return static_cast<jint>(SDL_GetKeyFromScancode(static_cast<SDL_Scancode>(scancode), 0, false));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getScancodeFromKey)(JNIEnv *, jclass, jint keycode) {
    return static_cast<jint>(SDL_GetScancodeFromKey(static_cast<SDL_Keycode>(keycode), nullptr));
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getKeyName)(JNIEnv *env, jclass, jint keycode) {
    return sdl_kmp_jni_to_string(env, SDL_GetKeyName(static_cast<SDL_Keycode>(keycode)));
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getScancodeName)(JNIEnv *env, jclass, jint scancode) {
    return sdl_kmp_jni_to_string(env, SDL_GetScancodeName(static_cast<SDL_Scancode>(scancode)));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(textInputActive)(JNIEnv *, jclass, jlong window) {
    return SDL_TextInputActive(reinterpret_cast<SDL_Window *>(window)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(startTextInput)(JNIEnv *, jclass, jlong window) {
    return SDL_StartTextInput(reinterpret_cast<SDL_Window *>(window)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(stopTextInput)(JNIEnv *, jclass, jlong window) {
    return SDL_StopTextInput(reinterpret_cast<SDL_Window *>(window)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(getKeyboardFocus)(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(SDL_GetKeyboardFocus());
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(getMouseFocus)(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(SDL_GetMouseFocus());
}

// ===========================================================================
// Mouse
// ===========================================================================

SDLJNI_FUNC(jfloatArray) SDLJNI_NAME(mouseState)(JNIEnv *env, jclass) {
    float x = 0.0f, y = 0.0f;
    Uint32 buttons = SDL_GetMouseState(&x, &y);
    return sdl_kmp_jni_new_float_array(env, {x, y, static_cast<jfloat>(buttons)});
}

SDLJNI_FUNC(jfloatArray) SDLJNI_NAME(globalMouseState)(JNIEnv *env, jclass) {
    float x = 0.0f, y = 0.0f;
    Uint32 buttons = SDL_GetGlobalMouseState(&x, &y);
    return sdl_kmp_jni_new_float_array(env, {x, y, static_cast<jfloat>(buttons)});
}

SDLJNI_FUNC(void) SDLJNI_NAME(warpMouseInWindow)(JNIEnv *, jclass, jlong window, jfloat x, jfloat y) {
    SDL_WarpMouseInWindow(reinterpret_cast<SDL_Window *>(window), x, y);
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(captureMouse)(JNIEnv *, jclass, jboolean enabled) {
    return SDL_CaptureMouse(enabled == JNI_TRUE) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(showCursor)(JNIEnv *, jclass) {
    return SDL_ShowCursor() ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(hideCursor)(JNIEnv *, jclass) {
    return SDL_HideCursor() ? JNI_TRUE : JNI_FALSE;
}

// ===========================================================================
// Touch
// ===========================================================================

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getTouchDevices)(JNIEnv *env, jclass) {
    int count = 0;
    SDL_TouchID *devices = SDL_GetTouchDevices(&count);
    if (devices == nullptr) return nullptr;
    std::vector<jint> out;
    out.reserve(static_cast<size_t>(count));
    for (int i = 0; i < count; i++) {
        out.push_back(static_cast<jint>(devices[i]));
    }
    SDL_free(devices);
    return sdl_kmp_jni_new_int_array(env, out);
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getTouchDeviceName)(JNIEnv *env, jclass, jint touchId) {
    return sdl_kmp_jni_to_string(env, SDL_GetTouchDeviceName(static_cast<SDL_TouchID>(touchId)));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getTouchDeviceType)(JNIEnv *, jclass, jint touchId) {
    return static_cast<jint>(SDL_GetTouchDeviceType(static_cast<SDL_TouchID>(touchId)));
}

// Returns a flat array [id, floatBits(x), floatBits(y), floatBits(pressure)] per finger.
SDLJNI_FUNC(jlongArray) SDLJNI_NAME(getTouchFingers)(JNIEnv *env, jclass, jint touchId) {
    int count = 0;
    SDL_Finger **fingers = SDL_GetTouchFingers(static_cast<SDL_TouchID>(touchId), &count);
    if (fingers == nullptr) return nullptr;
    std::vector<jlong> out;
    out.reserve(static_cast<size_t>(count) * 4);
    for (int i = 0; i < count; i++) {
        const SDL_Finger *finger = fingers[i];
        if (finger == nullptr) continue;
        out.push_back(static_cast<jlong>(finger->id));
        out.push_back(static_cast<jlong>(*reinterpret_cast<const Uint32 *>(&finger->x)));
        out.push_back(static_cast<jlong>(*reinterpret_cast<const Uint32 *>(&finger->y)));
        out.push_back(static_cast<jlong>(*reinterpret_cast<const Uint32 *>(&finger->pressure)));
    }
    SDL_free(fingers);
    return sdl_kmp_jni_new_long_array(env, out);
}

// ===========================================================================
// Joystick
// ===========================================================================

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getJoysticks)(JNIEnv *env, jclass) {
    int count = 0;
    SDL_JoystickID *ids = SDL_GetJoysticks(&count);
    if (ids == nullptr) return nullptr;
    std::vector<jint> out;
    out.reserve(static_cast<size_t>(count));
    for (int i = 0; i < count; i++) {
        out.push_back(static_cast<jint>(ids[i]));
    }
    SDL_free(ids);
    return sdl_kmp_jni_new_int_array(env, out);
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(openJoystick)(JNIEnv *, jclass, jint id) {
    return reinterpret_cast<jlong>(SDL_OpenJoystick(static_cast<SDL_JoystickID>(id)));
}

SDLJNI_FUNC(void) SDLJNI_NAME(closeJoystick)(JNIEnv *, jclass, jlong joystick) {
    SDL_CloseJoystick(reinterpret_cast<SDL_Joystick *>(joystick));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(joystickId)(JNIEnv *, jclass, jlong joystick) {
    return static_cast<jint>(SDL_GetJoystickID(reinterpret_cast<SDL_Joystick *>(joystick)));
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(joystickName)(JNIEnv *env, jclass, jlong joystick) {
    return sdl_kmp_jni_to_string(env, SDL_GetJoystickName(reinterpret_cast<SDL_Joystick *>(joystick)));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(joystickType)(JNIEnv *, jclass, jlong joystick) {
    return static_cast<jint>(SDL_GetJoystickType(reinterpret_cast<SDL_Joystick *>(joystick)));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(joystickNumAxes)(JNIEnv *, jclass, jlong joystick) {
    return SDL_GetNumJoystickAxes(reinterpret_cast<SDL_Joystick *>(joystick));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(joystickNumBalls)(JNIEnv *, jclass, jlong joystick) {
    return SDL_GetNumJoystickBalls(reinterpret_cast<SDL_Joystick *>(joystick));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(joystickNumHats)(JNIEnv *, jclass, jlong joystick) {
    return SDL_GetNumJoystickHats(reinterpret_cast<SDL_Joystick *>(joystick));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(joystickNumButtons)(JNIEnv *, jclass, jlong joystick) {
    return SDL_GetNumJoystickButtons(reinterpret_cast<SDL_Joystick *>(joystick));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(joystickPlayerIndex)(JNIEnv *, jclass, jlong joystick) {
    return SDL_GetJoystickPlayerIndex(reinterpret_cast<SDL_Joystick *>(joystick));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(joystickFirmwareVersion)(JNIEnv *, jclass, jlong joystick) {
    return static_cast<jint>(SDL_GetJoystickFirmwareVersion(reinterpret_cast<SDL_Joystick *>(joystick)));
}

SDLJNI_FUNC(jshort) SDLJNI_NAME(joystickAxis)(JNIEnv *, jclass, jlong joystick, jint axis) {
    return static_cast<jshort>(SDL_GetJoystickAxis(reinterpret_cast<SDL_Joystick *>(joystick), axis));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(joystickButton)(JNIEnv *, jclass, jlong joystick, jint button) {
    return SDL_GetJoystickButton(reinterpret_cast<SDL_Joystick *>(joystick), button) ? JNI_TRUE
                                                                                     : JNI_FALSE;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(joystickHat)(JNIEnv *, jclass, jlong joystick, jint hat) {
    return static_cast<jint>(SDL_GetJoystickHat(reinterpret_cast<SDL_Joystick *>(joystick), hat));
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(joystickBall)(JNIEnv *env, jclass, jlong joystick, jint ball) {
    int dx = 0, dy = 0;
    if (SDL_GetJoystickBall(reinterpret_cast<SDL_Joystick *>(joystick), ball, &dx, &dy)) {
        return sdl_kmp_jni_new_int_array(env, {dx, dy});
    }
    return nullptr;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(joystickRumble)(JNIEnv *, jclass, jlong joystick, jint lowFrequency,
                                                  jint highFrequency, jint durationMs) {
    return SDL_RumbleJoystick(reinterpret_cast<SDL_Joystick *>(joystick),
                              static_cast<Uint16>(lowFrequency), static_cast<Uint16>(highFrequency),
                              static_cast<Uint32>(durationMs))
               ? JNI_TRUE
               : JNI_FALSE;
}

// ===========================================================================
// Gamepad
// ===========================================================================

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getGamepads)(JNIEnv *env, jclass) {
    int count = 0;
    SDL_JoystickID *ids = SDL_GetGamepads(&count);
    if (ids == nullptr) return nullptr;
    std::vector<jint> out;
    out.reserve(static_cast<size_t>(count));
    for (int i = 0; i < count; i++) {
        out.push_back(static_cast<jint>(ids[i]));
    }
    SDL_free(ids);
    return sdl_kmp_jni_new_int_array(env, out);
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(openGamepad)(JNIEnv *, jclass, jint id) {
    return reinterpret_cast<jlong>(SDL_OpenGamepad(static_cast<SDL_JoystickID>(id)));
}

SDLJNI_FUNC(void) SDLJNI_NAME(closeGamepad)(JNIEnv *, jclass, jlong gamepad) {
    SDL_CloseGamepad(reinterpret_cast<SDL_Gamepad *>(gamepad));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(gamepadId)(JNIEnv *, jclass, jlong gamepad) {
    return static_cast<jint>(SDL_GetGamepadID(reinterpret_cast<SDL_Gamepad *>(gamepad)));
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(gamepadName)(JNIEnv *env, jclass, jlong gamepad) {
    return sdl_kmp_jni_to_string(env, SDL_GetGamepadName(reinterpret_cast<SDL_Gamepad *>(gamepad)));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(gamepadVendor)(JNIEnv *, jclass, jlong gamepad) {
    return static_cast<jint>(SDL_GetGamepadVendor(reinterpret_cast<SDL_Gamepad *>(gamepad)));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(gamepadProduct)(JNIEnv *, jclass, jlong gamepad) {
    return static_cast<jint>(SDL_GetGamepadProduct(reinterpret_cast<SDL_Gamepad *>(gamepad)));
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(gamepadSerial)(JNIEnv *env, jclass, jlong gamepad) {
    return sdl_kmp_jni_to_string(env, SDL_GetGamepadSerial(reinterpret_cast<SDL_Gamepad *>(gamepad)));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(gamepadConnected)(JNIEnv *, jclass, jlong gamepad) {
    return SDL_GamepadConnected(reinterpret_cast<SDL_Gamepad *>(gamepad)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(gamepadPlayerIndex)(JNIEnv *, jclass, jlong gamepad) {
    return SDL_GetGamepadPlayerIndex(reinterpret_cast<SDL_Gamepad *>(gamepad));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(gamepadFirmwareVersion)(JNIEnv *, jclass, jlong gamepad) {
    return static_cast<jint>(SDL_GetGamepadFirmwareVersion(reinterpret_cast<SDL_Gamepad *>(gamepad)));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(gamepadNumTouchpads)(JNIEnv *, jclass, jlong gamepad) {
    return SDL_GetNumGamepadTouchpads(reinterpret_cast<SDL_Gamepad *>(gamepad));
}

// Returns [down, x, y, pressure].
SDLJNI_FUNC(jfloatArray) SDLJNI_NAME(gamepadTouchpadFinger)(JNIEnv *env, jclass, jlong gamepad,
                                                            jint touchpad, jint finger) {
    bool down = false;
    float x = 0.0f, y = 0.0f, pressure = 0.0f;
    if (SDL_GetGamepadTouchpadFinger(reinterpret_cast<SDL_Gamepad *>(gamepad), touchpad, finger,
                                     &down, &x, &y, &pressure)) {
        return sdl_kmp_jni_new_float_array(env, {down ? 1.0f : 0.0f, x, y, pressure});
    }
    return nullptr;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(gamepadHasSensor)(JNIEnv *, jclass, jlong gamepad, jint type) {
    return SDL_GamepadHasSensor(reinterpret_cast<SDL_Gamepad *>(gamepad),
                                static_cast<SDL_SensorType>(type))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jfloatArray) SDLJNI_NAME(gamepadSensorData)(JNIEnv *env, jclass, jlong gamepad,
                                                        jint type) {
    float data[3] = {0.0f, 0.0f, 0.0f};
    if (SDL_GetGamepadSensorData(reinterpret_cast<SDL_Gamepad *>(gamepad),
                                 static_cast<SDL_SensorType>(type), data, 3)) {
        return sdl_kmp_jni_new_float_array(env, {data[0], data[1], data[2]});
    }
    return nullptr;
}

SDLJNI_FUNC(jfloat) SDLJNI_NAME(gamepadSensorDataRate)(JNIEnv *, jclass, jlong gamepad, jint type) {
    return SDL_GetGamepadSensorDataRate(reinterpret_cast<SDL_Gamepad *>(gamepad),
                                        static_cast<SDL_SensorType>(type));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(gamepadButton)(JNIEnv *, jclass, jlong gamepad, jint button) {
    return SDL_GetGamepadButton(reinterpret_cast<SDL_Gamepad *>(gamepad), static_cast<SDL_GamepadButton>(button)) ? JNI_TRUE
                                                                                  : JNI_FALSE;
}

SDLJNI_FUNC(jshort) SDLJNI_NAME(gamepadAxis)(JNIEnv *, jclass, jlong gamepad, jint axis) {
    return static_cast<jshort>(SDL_GetGamepadAxis(reinterpret_cast<SDL_Gamepad *>(gamepad), static_cast<SDL_GamepadAxis>(axis)));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(gamepadRumble)(JNIEnv *, jclass, jlong gamepad, jint lowFrequency,
                                                 jint highFrequency, jint durationMs) {
    return SDL_RumbleGamepad(reinterpret_cast<SDL_Gamepad *>(gamepad),
                             static_cast<Uint16>(lowFrequency), static_cast<Uint16>(highFrequency),
                             static_cast<Uint32>(durationMs))
               ? JNI_TRUE
               : JNI_FALSE;
}
