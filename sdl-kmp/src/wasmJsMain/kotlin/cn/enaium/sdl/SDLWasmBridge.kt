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

import org.khronos.webgl.Float32Array
import org.khronos.webgl.Int32Array
import org.khronos.webgl.Int8Array
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsArray
import kotlin.js.JsString

/*
 * Declarations for the JS glue (sdl_kmp_glue.js) that wraps the
 * Emscripten-compiled SDL3 module. The glue is loaded and initialized by the
 * host page BEFORE the Kotlin module runs, so these globals always exist.
 *
 * Result-buffer convention: functions that have C out-parameters write their
 * results into the global sdlKmpResultI32 / sdlKmpResultF32 arrays (fixed
 * slots), which the Kotlin caller reads after the call.
 */

@OptIn(ExperimentalJsExport::class)
internal external val sdlKmpResultI32: Int32Array

@OptIn(ExperimentalJsExport::class)
internal external val sdlKmpResultF32: Float32Array

/** Live read-only view over the Emscripten heap starting at [ptr]. */
internal external fun sdlKmpHeapBytes(ptr: Int, len: Int): Int8Array

/** Copies [bytes] into the Emscripten heap starting at [ptr]. */
internal external fun sdlKmpSetHeapBytes(ptr: Int, bytes: Int8Array)

/** Reads a single element of the global int result buffer. */
internal external fun sdlKmpResultI32Get(index: Int): Int

/** Reads a single element of the global float result buffer. */
internal external fun sdlKmpResultF32Get(index: Int): Float

// =========================================================================
// Core
// =========================================================================

internal external fun sdl_kmp_Init(flags: Int): Int
internal external fun sdl_kmp_InitSubSystem(flags: Int): Int
internal external fun sdl_kmp_QuitSubSystem(flags: Int)
internal external fun sdl_kmp_WasInit(flags: Int): Int
internal external fun sdl_kmp_Quit()
internal external fun sdl_kmp_GetError(): String?
internal external fun sdl_kmp_ClearError()
internal external fun sdl_kmp_SetError(message: String): Int
internal external fun sdl_kmp_GetVersion(): Int
internal external fun sdl_kmp_GetRevision(): String?
internal external fun sdl_kmp_GetTicks(): Int
internal external fun sdl_kmp_PerfCounterHi(): Int
internal external fun sdl_kmp_PerfCounterLo(): Int
internal external fun sdl_kmp_PerfFreqHi(): Int
internal external fun sdl_kmp_PerfFreqLo(): Int
internal external fun sdl_kmp_Delay(ms: Int)
internal external fun sdl_kmp_SetHint(name: String, value: String): Int
internal external fun sdl_kmp_GetHint(name: String): String?
internal external fun sdl_kmp_GetHintBoolean(name: String, defaultValue: Int): Int
internal external fun sdl_kmp_GetClipboardText(): String?
internal external fun sdl_kmp_SetClipboardText(text: String): Int
internal external fun sdl_kmp_HasClipboardText(): Int

// =========================================================================
// Drivers
// =========================================================================

internal external fun sdl_kmp_GetNumVideoDrivers(): Int
internal external fun sdl_kmp_GetVideoDriver(index: Int): String?
internal external fun sdl_kmp_GetCurrentVideoDriver(): String?
internal external fun sdl_kmp_GetNumAudioDrivers(): Int
internal external fun sdl_kmp_GetAudioDriver(index: Int): String?
internal external fun sdl_kmp_GetCurrentAudioDriver(): String?
internal external fun sdl_kmp_GetNumRenderDrivers(): Int
internal external fun sdl_kmp_GetRenderDriver(index: Int): String?

// =========================================================================
// Window
// =========================================================================

internal external fun sdl_kmp_CreateWindow(title: String, width: Int, height: Int, flagsLo: Int, flagsHi: Int): Int
internal external fun sdl_kmp_DestroyWindow(window: Int)
internal external fun sdl_kmp_GetWindowID(window: Int): Int
internal external fun sdl_kmp_GetWindowTitle(window: Int): String?
internal external fun sdl_kmp_SetWindowTitle(window: Int, title: String)
internal external fun sdl_kmp_GetWindowSize(window: Int)
internal external fun sdl_kmp_SetWindowSize(window: Int, width: Int, height: Int)
internal external fun sdl_kmp_GetWindowPosition(window: Int)
internal external fun sdl_kmp_SetWindowPosition(window: Int, x: Int, y: Int)
internal external fun sdl_kmp_GetWindowSizeInPixels(window: Int)
internal external fun sdl_kmp_GetWindowFlags(window: Int): Int
internal external fun sdl_kmp_GetDisplayForWindow(window: Int): Int
internal external fun sdl_kmp_GetWindowOpacity(window: Int): Double
internal external fun sdl_kmp_SetWindowOpacity(window: Int, opacity: Double): Int
internal external fun sdl_kmp_SetWindowFullscreen(window: Int, fullscreen: Int): Int
internal external fun sdl_kmp_SetWindowBordered(window: Int, bordered: Int): Int
internal external fun sdl_kmp_SetWindowResizable(window: Int, resizable: Int): Int
internal external fun sdl_kmp_SetWindowAlwaysOnTop(window: Int, onTop: Int): Int
internal external fun sdl_kmp_GetWindowMouseGrab(window: Int): Int
internal external fun sdl_kmp_SetWindowMouseGrab(window: Int, grabbed: Int): Int
internal external fun sdl_kmp_GetWindowKeyboardGrab(window: Int): Int
internal external fun sdl_kmp_SetWindowKeyboardGrab(window: Int, grabbed: Int): Int
internal external fun sdl_kmp_GetWindowRelativeMouseMode(window: Int): Int
internal external fun sdl_kmp_SetWindowRelativeMouseMode(window: Int, enabled: Int): Int
internal external fun sdl_kmp_GetWindowMinimumSize(window: Int)
internal external fun sdl_kmp_SetWindowMinimumSize(window: Int, width: Int, height: Int): Int
internal external fun sdl_kmp_GetWindowMaximumSize(window: Int)
internal external fun sdl_kmp_SetWindowMaximumSize(window: Int, width: Int, height: Int): Int
internal external fun sdl_kmp_GetWindowAspectRatio(window: Int)
internal external fun sdl_kmp_SetWindowAspectRatio(window: Int, min: Double, max: Double): Int
internal external fun sdl_kmp_ShowWindow(window: Int)
internal external fun sdl_kmp_HideWindow(window: Int)
internal external fun sdl_kmp_RaiseWindow(window: Int)
internal external fun sdl_kmp_MaximizeWindow(window: Int)
internal external fun sdl_kmp_MinimizeWindow(window: Int)
internal external fun sdl_kmp_RestoreWindow(window: Int)
internal external fun sdl_kmp_FlashWindow(window: Int, operation: Int): Int
internal external fun sdl_kmp_GetWindowSurface(window: Int): Int
internal external fun sdl_kmp_SetWindowIcon(window: Int, surface: Int): Int
internal external fun sdl_kmp_GetWindowFromID(windowID: Int): Int

// =========================================================================
// Displays
// =========================================================================

internal external fun sdl_kmp_RefreshDisplays(): Int
internal external fun sdl_kmp_GetDisplayID(index: Int): Int
internal external fun sdl_kmp_GetDisplayName(index: Int): String?
internal external fun sdl_kmp_GetPrimaryDisplay(): Int
internal external fun sdl_kmp_GetDisplayBounds2(index: Int)
internal external fun sdl_kmp_GetDisplayUsableBounds(index: Int)
internal external fun sdl_kmp_GetDisplayCurrentMode(index: Int)
internal external fun sdl_kmp_GetDisplayDesktopMode(index: Int)

// =========================================================================
// Renderer
// =========================================================================

internal external fun sdl_kmp_CreateRenderer(window: Int, name: String?): Int
internal external fun sdl_kmp_GetRendererName(renderer: Int): String?
internal external fun sdl_kmp_DestroyRenderer(renderer: Int)
internal external fun sdl_kmp_GetRenderDrawColor(renderer: Int)
internal external fun sdl_kmp_SetRenderDrawColor(renderer: Int, r: Int, g: Int, b: Int, a: Int): Int
internal external fun sdl_kmp_GetRenderOutputSize(renderer: Int)
internal external fun sdl_kmp_GetCurrentRenderOutputSize(renderer: Int)
internal external fun sdl_kmp_GetRenderViewport(renderer: Int)
internal external fun sdl_kmp_SetRenderViewport(renderer: Int, x: Int, y: Int, width: Int, height: Int)
internal external fun sdl_kmp_SetRenderViewportNull(renderer: Int)
internal external fun sdl_kmp_GetRenderClipRect(renderer: Int)
internal external fun sdl_kmp_SetRenderClipRect(renderer: Int, x: Int, y: Int, width: Int, height: Int)
internal external fun sdl_kmp_SetRenderClipRectNull(renderer: Int)
internal external fun sdl_kmp_GetRenderScale(renderer: Int)
internal external fun sdl_kmp_SetRenderScale(renderer: Int, sx: Double, sy: Double)
internal external fun sdl_kmp_GetRenderDrawBlendMode(renderer: Int): Int
internal external fun sdl_kmp_SetRenderDrawBlendMode(renderer: Int, mode: Int): Int
internal external fun sdl_kmp_GetRenderVSync(renderer: Int): Int
internal external fun sdl_kmp_SetRenderVSync(renderer: Int, vsync: Int): Int
internal external fun sdl_kmp_GetRenderTarget(renderer: Int): Int
internal external fun sdl_kmp_SetRenderTarget(renderer: Int, texture: Int)
internal external fun sdl_kmp_RenderClear(renderer: Int): Int
internal external fun sdl_kmp_RenderPresent(renderer: Int)
internal external fun sdl_kmp_RenderFillRect(renderer: Int, x: Double, y: Double, width: Double, height: Double): Int
internal external fun sdl_kmp_RenderFillRectNull(renderer: Int): Int
internal external fun sdl_kmp_RenderRect(renderer: Int, x: Double, y: Double, width: Double, height: Double): Int
internal external fun sdl_kmp_RenderRectNull(renderer: Int): Int
internal external fun sdl_kmp_RenderLine(renderer: Int, x1: Double, y1: Double, x2: Double, y2: Double): Int
internal external fun sdl_kmp_RenderPoint(renderer: Int, x: Double, y: Double): Int
internal external fun sdl_kmp_RenderPoints(renderer: Int, xy: Float32Array, count: Int): Int
internal external fun sdl_kmp_CreateTexture(renderer: Int, format: Int, access: Int, width: Int, height: Int): Int
internal external fun sdl_kmp_CreateTextureFromSurface(renderer: Int, surface: Int): Int
internal external fun sdl_kmp_RenderTexture(
    renderer: Int, texture: Int, hasSrc: Int, sx: Double, sy: Double, sw: Double, sh: Double,
    hasDst: Int, dx: Double, dy: Double, dw: Double, dh: Double,
): Int
internal external fun sdl_kmp_RenderTextureRotated(
    renderer: Int, texture: Int, hasSrc: Int, sx: Double, sy: Double, sw: Double, sh: Double,
    hasDst: Int, dx: Double, dy: Double, dw: Double, dh: Double,
    angle: Double, hasCenter: Int, cx: Double, cy: Double, flip: Int,
): Int
internal external fun sdl_kmp_RenderTexture9Grid(
    renderer: Int, texture: Int, sx: Double, sy: Double, sw: Double, sh: Double,
    lw: Double, rw: Double, th: Double, bh: Double, scale: Double,
    dx: Double, dy: Double, dw: Double, dh: Double,
): Int
internal external fun sdl_kmp_RenderGeometry(
    renderer: Int, texture: Int, verts: Float32Array?, numVerts: Int, indices: Int32Array?, numIndices: Int,
): Int
internal external fun sdl_kmp_RenderReadPixels(renderer: Int, hasRect: Int, x: Int, y: Int, width: Int, height: Int): Int
internal external fun sdl_kmp_SetRenderLogicalPresentation(renderer: Int, width: Int, height: Int, mode: Int): Int
internal external fun sdl_kmp_GetRenderLogicalPresentationRect(renderer: Int)

// =========================================================================
// Texture
// =========================================================================

internal external fun sdl_kmp_GetTextureFormat(texture: Int): Int
internal external fun sdl_kmp_GetTextureAccess(texture: Int): Int
internal external fun sdl_kmp_GetTextureSize(texture: Int)
internal external fun sdl_kmp_GetTextureColorMod(texture: Int)
internal external fun sdl_kmp_SetTextureColorMod(texture: Int, r: Int, g: Int, b: Int): Int
internal external fun sdl_kmp_GetTextureAlphaMod(texture: Int): Int
internal external fun sdl_kmp_SetTextureAlphaMod(texture: Int, a: Int): Int
internal external fun sdl_kmp_GetTextureBlendMode(texture: Int): Int
internal external fun sdl_kmp_SetTextureBlendMode(texture: Int, mode: Int): Int
internal external fun sdl_kmp_GetTextureScaleMode(texture: Int): Int
internal external fun sdl_kmp_SetTextureScaleMode(texture: Int, mode: Int): Int
internal external fun sdl_kmp_UpdateTexture(texture: Int, hasRect: Int, x: Int, y: Int, width: Int, height: Int, pixels: Int8Array, pitch: Int): Int
internal external fun sdl_kmp_LockTexture(texture: Int, hasRect: Int, x: Int, y: Int, width: Int, height: Int): Int
internal external fun sdl_kmp_LockedPixelsPtr(): Int
internal external fun sdl_kmp_UnlockTexture(texture: Int)
internal external fun sdl_kmp_DestroyTexture(texture: Int)

// =========================================================================
// Pixels / surface
// =========================================================================

internal external fun sdl_kmp_GetPixelFormatName(format: Int): String?
internal external fun sdl_kmp_MapRGB(format: Int, r: Int, g: Int, b: Int): Int
internal external fun sdl_kmp_MapRGBA(format: Int, r: Int, g: Int, b: Int, a: Int): Int
internal external fun sdl_kmp_GetRGBA(format: Int, pixel: Int)
internal external fun sdl_kmp_CreateSurface(width: Int, height: Int, format: Int): Int
internal external fun sdl_kmp_LoadBMP(path: String): Int
internal external fun sdl_kmp_GetSurfaceWidth(surface: Int): Int
internal external fun sdl_kmp_GetSurfaceHeight(surface: Int): Int
internal external fun sdl_kmp_GetSurfaceFormat(surface: Int): Int
internal external fun sdl_kmp_GetSurfaceColorspace(surface: Int): Int
internal external fun sdl_kmp_GetSurfacePitch(surface: Int): Int
internal external fun sdl_kmp_GetSurfacePixels(surface: Int): Int
internal external fun sdl_kmp_LockSurface(surface: Int): Int
internal external fun sdl_kmp_UnlockSurface(surface: Int)
internal external fun sdl_kmp_FillSurfaceRect(surface: Int, hasRect: Int, x: Int, y: Int, width: Int, height: Int, color: Int): Int
internal external fun sdl_kmp_FillSurfaceRects(surface: Int, rects: Int32Array, count: Int, color: Int): Int
internal external fun sdl_kmp_BlitSurface(src: Int, hasSrc: Int, sx: Int, sy: Int, sw: Int, sh: Int, dst: Int, hasDst: Int, dx: Int, dy: Int, dw: Int, dh: Int): Int
internal external fun sdl_kmp_BlitSurfaceScaled(src: Int, hasSrc: Int, sx: Int, sy: Int, sw: Int, sh: Int, dst: Int, hasDst: Int, dx: Int, dy: Int, dw: Int, dh: Int, scaleMode: Int): Int
internal external fun sdl_kmp_SaveBMP(surface: Int, path: String): Int
internal external fun sdl_kmp_ConvertSurface(surface: Int, format: Int): Int
internal external fun sdl_kmp_DestroySurface(surface: Int)

// =========================================================================
// Events
// =========================================================================

internal external fun sdl_kmp_PollEvent(): Int
internal external fun sdl_kmp_WaitEvent(): Int
internal external fun sdl_kmp_PumpEvents()
internal external fun sdl_kmp_EventTimestampLo(): Int
internal external fun sdl_kmp_EventTimestampHi(): Int
internal external fun sdl_kmp_EventWindowID(): Int
internal external fun sdl_kmp_EventData1(): Int
internal external fun sdl_kmp_EventData2(): Int
internal external fun sdl_kmp_EventKeyDown(): Int
internal external fun sdl_kmp_EventRepeat(): Int
internal external fun sdl_kmp_EventKeycode(): Int
internal external fun sdl_kmp_EventScancode(): Int
internal external fun sdl_kmp_EventMod(): Int
internal external fun sdl_kmp_EventText(): String?
internal external fun sdl_kmp_EventMouseX(): Double
internal external fun sdl_kmp_EventMouseY(): Double
internal external fun sdl_kmp_EventMouseDX(): Double
internal external fun sdl_kmp_EventMouseDY(): Double
internal external fun sdl_kmp_EventButton(): Int
internal external fun sdl_kmp_EventButtonDown(): Int
internal external fun sdl_kmp_EventClicks(): Int
internal external fun sdl_kmp_EventButtonX(): Double
internal external fun sdl_kmp_EventButtonY(): Double
internal external fun sdl_kmp_EventWheelX(): Double
internal external fun sdl_kmp_EventWheelY(): Double
internal external fun sdl_kmp_EventWheelDir(): Int
internal external fun sdl_kmp_EventDisplayID(): Int
internal external fun sdl_kmp_EventDropData(): String?
internal external fun sdl_kmp_EventDeviceID(): Int
internal external fun sdl_kmp_EventAxis(): Int
internal external fun sdl_kmp_EventAxisValue(): Int
internal external fun sdl_kmp_EventBall(): Int
internal external fun sdl_kmp_EventBallDX(): Int
internal external fun sdl_kmp_EventBallDY(): Int
internal external fun sdl_kmp_EventHat(): Int
internal external fun sdl_kmp_EventHatValue(): Int
internal external fun sdl_kmp_EventTouchpad(): Int
internal external fun sdl_kmp_EventFinger(): Int
internal external fun sdl_kmp_EventFingerDown(): Int
internal external fun sdl_kmp_EventFingerX(): Double
internal external fun sdl_kmp_EventFingerY(): Double
internal external fun sdl_kmp_EventFingerPressure(): Double
internal external fun sdl_kmp_EventSensorType(): Int
internal external fun sdl_kmp_EventSensorData(index: Int): Double
internal external fun sdl_kmp_EventBatteryState(): Int
internal external fun sdl_kmp_EventBatteryPercent(): Int
internal external fun sdl_kmp_EventTouchIDLo(): Int
internal external fun sdl_kmp_EventTouchIDHi(): Int
internal external fun sdl_kmp_EventFingerIDLo(): Int
internal external fun sdl_kmp_EventFingerIDHi(): Int
internal external fun sdl_kmp_EventTouchX(): Double
internal external fun sdl_kmp_EventTouchY(): Double
internal external fun sdl_kmp_EventTouchDX(): Double
internal external fun sdl_kmp_EventTouchDY(): Double
internal external fun sdl_kmp_EventTouchPressure(): Double
internal external fun sdl_kmp_EventAudioCapture(): Int
internal external fun sdl_kmp_EventClipboardOwner(): Int
internal external fun sdl_kmp_EventSetEventEnabled(type: Int, enabled: Int): Int
internal external fun sdl_kmp_EventEnabled(type: Int): Int
internal external fun sdl_kmp_FlushEvents(minType: Int, maxType: Int)

// =========================================================================
// Audio
// =========================================================================

internal external fun sdl_kmp_RefreshAudioDevices(): Int
internal external fun sdl_kmp_GetAudioPlaybackCount(): Int
internal external fun sdl_kmp_GetAudioRecordingCount(): Int
internal external fun sdl_kmp_GetAudioPlaybackDevice(index: Int): Int
internal external fun sdl_kmp_GetAudioRecordingDevice(index: Int): Int
internal external fun sdl_kmp_GetAudioDeviceName(device: Int): String?
internal external fun sdl_kmp_GetAudioDeviceFormat2(device: Int)
internal external fun sdl_kmp_OpenAudioDevice(device: Int, format: Int, channels: Int, freq: Int): Int
internal external fun sdl_kmp_OpenAudioDeviceStream(device: Int, format: Int, channels: Int, freq: Int): Int
internal external fun sdl_kmp_CreateAudioStream(sfmt: Int, sch: Int, sfreq: Int, dfmt: Int, dch: Int, dfreq: Int): Int
internal external fun sdl_kmp_AudioDevicePaused(device: Int): Int
internal external fun sdl_kmp_PauseAudioDevice(device: Int)
internal external fun sdl_kmp_ResumeAudioDevice(device: Int)
internal external fun sdl_kmp_BindAudioStream(device: Int, stream: Int): Int
internal external fun sdl_kmp_UnbindAudioStream(stream: Int)
internal external fun sdl_kmp_CloseAudioDevice(device: Int)
internal external fun sdl_kmp_DestroyAudioStream(stream: Int)
internal external fun sdl_kmp_PutAudioStreamData(stream: Int, data: Int8Array, len: Int): Int
internal external fun sdl_kmp_GetAudioStreamData(stream: Int, out: Int8Array): Int
internal external fun sdl_kmp_GetAudioStreamAvailable(stream: Int): Int
internal external fun sdl_kmp_GetAudioStreamQueued(stream: Int): Int
internal external fun sdl_kmp_GetAudioStreamFormat(stream: Int)
internal external fun sdl_kmp_SetAudioStreamFormat(stream: Int, sfmt: Int, sch: Int, sfreq: Int, dfmt: Int, dch: Int, dfreq: Int): Int
internal external fun sdl_kmp_GetAudioStreamGain(stream: Int): Double
internal external fun sdl_kmp_SetAudioStreamGain(stream: Int, gain: Double): Int
internal external fun sdl_kmp_GetAudioStreamFrequencyRatio(stream: Int): Double
internal external fun sdl_kmp_SetAudioStreamFrequencyRatio(stream: Int, ratio: Double): Int
internal external fun sdl_kmp_GetAudioStreamDevicePaused(stream: Int): Int
internal external fun sdl_kmp_PauseAudioStreamDevice(stream: Int)
internal external fun sdl_kmp_ResumeAudioStreamDevice(stream: Int)
internal external fun sdl_kmp_FlushAudioStream(stream: Int): Int
internal external fun sdl_kmp_ClearAudioStream(stream: Int): Int
internal external fun sdl_kmp_LoadWAV(path: String): Int
internal external fun sdl_kmp_LoadWAVFormat(): Int
internal external fun sdl_kmp_LoadWAVChannels(): Int
internal external fun sdl_kmp_LoadWAVFreq(): Int
internal external fun sdl_kmp_LoadWAVLen(): Int
internal external fun sdl_kmp_LoadWAVData(): Int
internal external fun sdl_kmp_LoadWAVFree()

// =========================================================================
// Keyboard / mouse / text input
// =========================================================================

internal external fun sdl_kmp_GetNumScancodes(): Int
internal external fun sdl_kmp_GetKeyboardState()
internal external fun sdl_kmp_GetModState(): Int
internal external fun sdl_kmp_SetModState(mod: Int)
internal external fun sdl_kmp_GetKeyFromScancode(scancode: Int): Int
internal external fun sdl_kmp_GetScancodeFromKey(keycode: Int): Int
internal external fun sdl_kmp_GetKeyName(keycode: Int): String?
internal external fun sdl_kmp_GetScancodeName(scancode: Int): String?
internal external fun sdl_kmp_GetMouseState()
internal external fun sdl_kmp_GetGlobalMouseState()
internal external fun sdl_kmp_WarpMouseInWindow(windowID: Int, x: Double, y: Double)
internal external fun sdl_kmp_CaptureMouse(enabled: Int): Int
internal external fun sdl_kmp_ShowCursor(): Int
internal external fun sdl_kmp_TextInputActive(windowID: Int): Int
internal external fun sdl_kmp_StartTextInput(windowID: Int): Int
internal external fun sdl_kmp_StopTextInput(windowID: Int): Int

// =========================================================================
// Touch
// =========================================================================

internal external fun sdl_kmp_RefreshTouchDevices(): Int
internal external fun sdl_kmp_GetTouchDevice(index: Int): Int
internal external fun sdl_kmp_GetTouchDeviceName(touchID: Int): String?
internal external fun sdl_kmp_GetTouchDeviceType(touchID: Int): Int
internal external fun sdl_kmp_RefreshTouchFingers(touchID: Int): Int
internal external fun sdl_kmp_GetTouchFingerCount(): Int
internal external fun sdl_kmp_GetTouchFinger(index: Int)

// =========================================================================
// Joystick / gamepad
// =========================================================================

internal external fun sdl_kmp_RefreshJoysticks(): Int
internal external fun sdl_kmp_GetJoystickID(index: Int): Int
internal external fun sdl_kmp_RefreshGamepads(): Int
internal external fun sdl_kmp_GetGamepadID(index: Int): Int
internal external fun sdl_kmp_OpenJoystick(id: Int): Int
internal external fun sdl_kmp_CloseJoystick(js: Int)
internal external fun sdl_kmp_GetJoystickIDFromJoystick(js: Int): Int
internal external fun sdl_kmp_GetJoystickName(js: Int): String?
internal external fun sdl_kmp_GetJoystickType(js: Int): Int
internal external fun sdl_kmp_GetNumJoystickAxes(js: Int): Int
internal external fun sdl_kmp_GetNumJoystickBalls(js: Int): Int
internal external fun sdl_kmp_GetNumJoystickHats(js: Int): Int
internal external fun sdl_kmp_GetNumJoystickButtons(js: Int): Int
internal external fun sdl_kmp_GetJoystickPlayerIndex(js: Int): Int
internal external fun sdl_kmp_GetJoystickFirmwareVersion(js: Int): Int
internal external fun sdl_kmp_GetJoystickAxis(js: Int, axis: Int): Int
internal external fun sdl_kmp_GetJoystickButton(js: Int, button: Int): Int
internal external fun sdl_kmp_GetJoystickHat(js: Int, hat: Int): Int
internal external fun sdl_kmp_GetJoystickBall(js: Int, ball: Int)
internal external fun sdl_kmp_JoystickRumble(js: Int, low: Int, high: Int, duration: Int): Int
internal external fun sdl_kmp_OpenGamepad(id: Int): Int
internal external fun sdl_kmp_CloseGamepad(gp: Int)
internal external fun sdl_kmp_GetGamepadIDFromGamepad(gp: Int): Int
internal external fun sdl_kmp_GetGamepadName(gp: Int): String?
internal external fun sdl_kmp_GetGamepadVendor(gp: Int): Int
internal external fun sdl_kmp_GetGamepadProduct(gp: Int): Int
internal external fun sdl_kmp_GetGamepadSerial(gp: Int): String?
internal external fun sdl_kmp_GamepadConnected(gp: Int): Int
internal external fun sdl_kmp_GetGamepadPlayerIndex(gp: Int): Int
internal external fun sdl_kmp_GetGamepadFirmwareVersion(gp: Int): Int
internal external fun sdl_kmp_GetNumGamepadTouchpads(gp: Int): Int
internal external fun sdl_kmp_GetGamepadButton(gp: Int, button: Int): Int
internal external fun sdl_kmp_GetGamepadAxis(gp: Int, axis: Int): Int
internal external fun sdl_kmp_GetGamepadTouchpadFinger(gp: Int, tp: Int, finger: Int)
internal external fun sdl_kmp_GamepadHasSensor(gp: Int, type: Int): Int
internal external fun sdl_kmp_GetGamepadSensorData(gp: Int, type: Int)
internal external fun sdl_kmp_GetGamepadSensorDataRate(gp: Int, type: Int): Double
internal external fun sdl_kmp_GamepadRumble(gp: Int, low: Int, high: Int, duration: Int): Int

// =========================================================================
// Filesystem / misc
// =========================================================================

internal external fun sdl_kmp_GetBasePath(): String?
internal external fun sdl_kmp_GetPrefPath(org: String, app: String): String?
internal external fun sdl_kmp_GetUserFolder(folder: Int): String?
internal external fun sdl_kmp_CreateDirectory(path: String): Int
internal external fun sdl_kmp_RemovePath(path: String): Int
internal external fun sdl_kmp_RenamePath(oldPath: String, newPath: String): Int
internal external fun sdl_kmp_GetPowerInfo()
internal external fun sdl_kmp_OpenURL(url: String): Int
internal external fun sdl_kmp_ShowSimpleMessageBox(title: String, message: String): Int
internal external fun sdl_kmp_ShowMessageBox(
    flags: Int, title: String, message: String,
    buttonFlags: Int32Array, buttonIds: Int32Array, buttonTexts: JsArray<JsString>, count: Int,
): Int

// =========================================================================
// Logging
// =========================================================================

internal external fun sdl_kmp_Log(priority: Int, category: Int, message: String)
internal external fun sdl_kmp_LogSetPriority(category: Int, priority: Int)
internal external fun sdl_kmp_LogGetPriority(category: Int): Int
internal external fun sdl_kmp_LogSetAllPriority(priority: Int)
internal external fun sdl_kmp_LogResetPriorities()

// =========================================================================
// Threads / synchronization
// =========================================================================

internal external fun sdl_kmp_GetNumLogicalCPUCores(): Int
internal external fun sdl_kmp_GetCurrentThreadID(): Int
internal external fun sdl_kmp_CreateMutex(): Int
internal external fun sdl_kmp_LockMutex(m: Int)
internal external fun sdl_kmp_TryLockMutex(m: Int): Int
internal external fun sdl_kmp_UnlockMutex(m: Int)
internal external fun sdl_kmp_DestroyMutex(m: Int)
internal external fun sdl_kmp_CreateRWLock(): Int
internal external fun sdl_kmp_LockRWLockRead(l: Int)
internal external fun sdl_kmp_TryLockRWLockRead(l: Int): Int
internal external fun sdl_kmp_UnlockRWLockRead(l: Int)
internal external fun sdl_kmp_LockRWLockWrite(l: Int)
internal external fun sdl_kmp_TryLockRWLockWrite(l: Int): Int
internal external fun sdl_kmp_UnlockRWLockWrite(l: Int)
internal external fun sdl_kmp_DestroyRWLock(l: Int)
internal external fun sdl_kmp_CreateSemaphore(initial: Int): Int
internal external fun sdl_kmp_WaitSemaphore(s: Int)
internal external fun sdl_kmp_TryWaitSemaphore(s: Int): Int
internal external fun sdl_kmp_WaitSemaphoreTimeout(s: Int, ms: Int): Int
internal external fun sdl_kmp_PostSemaphore(s: Int)
internal external fun sdl_kmp_GetSemaphoreValue(s: Int): Int
internal external fun sdl_kmp_DestroySemaphore(s: Int)
internal external fun sdl_kmp_CreateCondition(): Int
internal external fun sdl_kmp_WaitCondition(cond: Int, mutex: Int, timeoutMs: Int): Int
internal external fun sdl_kmp_SignalCondition(cond: Int)
internal external fun sdl_kmp_BroadcastCondition(cond: Int)
internal external fun sdl_kmp_DestroyCondition(cond: Int)
internal external fun sdl_kmp_CreateThread(name: String?, fn: Int, data: Int): Int

// =========================================================================
// IO streams
// =========================================================================

internal external fun sdl_kmp_IOFromFile(path: String, mode: String): Int
internal external fun sdl_kmp_IOFromMem(data: Int8Array, size: Int): Int
internal external fun sdl_kmp_IOFromConstMem(data: Int8Array, size: Int): Int
internal external fun sdl_kmp_IORead(io: Int, out: Int8Array): Int
internal external fun sdl_kmp_IOWrite(io: Int, data: Int8Array, size: Int): Int
internal external fun sdl_kmp_IOSeek(io: Int, offset: Double, whence: Int): Double
internal external fun sdl_kmp_IOTell(io: Int): Double
internal external fun sdl_kmp_IOStreamSize(io: Int): Double
internal external fun sdl_kmp_IOFlush(io: Int): Int
internal external fun sdl_kmp_IOClose(io: Int)
internal external fun sdl_kmp_LoadFileToMem(path: String): Int
internal external fun sdl_kmp_LoadFileSize(): Int
internal external fun sdl_kmp_LoadFileData(): Int
internal external fun sdl_kmp_LoadFileFree()

// =========================================================================
// Properties
// =========================================================================

internal external fun sdl_kmp_CreateProperties(): Int
internal external fun sdl_kmp_SetProperty(props: Int, name: String, value: Double): Int
internal external fun sdl_kmp_SetStringProperty(props: Int, name: String, value: String?): Int
internal external fun sdl_kmp_GetProperty(props: Int, name: String, defaultValue: Double): Double
internal external fun sdl_kmp_GetStringProperty(props: Int, name: String): String?
internal external fun sdl_kmp_HasProperty(props: Int, name: String): Int
internal external fun sdl_kmp_DeleteProperty(props: Int, name: String): Int
internal external fun sdl_kmp_CopyProperties(src: Int, dst: Int): Int
internal external fun sdl_kmp_GetGlobalProperties(): Int
internal external fun sdl_kmp_DestroyProperties(props: Int)

// =========================================================================
// Camera / sensor / haptics
// =========================================================================

internal external fun sdl_kmp_RefreshCameras(): Int
internal external fun sdl_kmp_GetCameraDevice(index: Int): Int
internal external fun sdl_kmp_GetCameraDeviceName(id: Int): String?
internal external fun sdl_kmp_GetCameraDevicePosition(id: Int): Int
internal external fun sdl_kmp_RefreshCameraFormats(id: Int): Int
internal external fun sdl_kmp_GetCameraFormatSpec(index: Int)
internal external fun sdl_kmp_OpenCamera(id: Int, format: Int, width: Int, height: Int, rate: Int): Int
internal external fun sdl_kmp_GetCameraFormat(camera: Int)
internal external fun sdl_kmp_GetCameraPermissionState(camera: Int): Int
internal external fun sdl_kmp_GetCameraSupportsFormat(camera: Int, format: Int, width: Int, height: Int, rate: Int): Int
internal external fun sdl_kmp_AcquireCameraFrame(camera: Int): Int
internal external fun sdl_kmp_ReleaseCameraFrame(camera: Int, frame: Int)
internal external fun sdl_kmp_CloseCamera(camera: Int)
internal external fun sdl_kmp_RefreshSensors(): Int
internal external fun sdl_kmp_GetSensorDevice(index: Int): Int
internal external fun sdl_kmp_GetSensorDeviceName(id: Int): String?
internal external fun sdl_kmp_GetSensorDeviceType(id: Int): Int
internal external fun sdl_kmp_OpenSensor(id: Int): Int
internal external fun sdl_kmp_CloseSensor(sensor: Int)
internal external fun sdl_kmp_GetSensorName(sensor: Int): String?
internal external fun sdl_kmp_GetSensorType(sensor: Int): Int
internal external fun sdl_kmp_GetSensorData(sensor: Int, out: Float32Array): Int
internal external fun sdl_kmp_GetNumHapticDevices(): Int

// =========================================================================
// OpenGL
// =========================================================================

internal external fun sdl_kmp_GL_LoadLibrary(path: String?): Int
internal external fun sdl_kmp_GL_UnloadLibrary()
internal external fun sdl_kmp_GL_GetProcAddress(proc: String): Int
internal external fun sdl_kmp_GL_ExtensionSupported(ext: String): Int
internal external fun sdl_kmp_GL_ResetAttributes()
internal external fun sdl_kmp_GL_SetAttribute(attr: Int, value: Int): Int
internal external fun sdl_kmp_GL_GetAttribute(attr: Int)
internal external fun sdl_kmp_GL_CreateContext(window: Int): Int
internal external fun sdl_kmp_GL_MakeCurrent(window: Int, ctx: Int): Int
internal external fun sdl_kmp_GL_GetCurrentWindow(): Int
internal external fun sdl_kmp_GL_GetCurrentContext(): Int
internal external fun sdl_kmp_GL_SetSwapInterval(interval: Int): Int
internal external fun sdl_kmp_GL_GetSwapInterval()
internal external fun sdl_kmp_GL_SwapWindow(window: Int): Int
internal external fun sdl_kmp_GL_DestroyContext(ctx: Int)

// =========================================================================
// Vulkan
// =========================================================================

internal external fun sdl_kmp_Vulkan_LoadLibrary(path: String?): Int
internal external fun sdl_kmp_Vulkan_UnloadLibrary()
internal external fun sdl_kmp_Vulkan_GetVkGetInstanceProcAddr(): Int
internal external fun sdl_kmp_Vulkan_GetInstanceExtensions(): Int
internal external fun sdl_kmp_Vulkan_GetInstanceExtension(index: Int): String?
internal external fun sdl_kmp_Vulkan_CreateSurface(window: Int, instance: Int): Int
internal external fun sdl_kmp_Vulkan_DestroySurface(instance: Int, surface: Int)
internal external fun sdl_kmp_Vulkan_GetPresentationSupport(instance: Int, physicalDevice: Int, queueFamily: Int): Int

// =========================================================================
// GPU (unsupported on wasm)
// =========================================================================

internal external fun sdl_kmp_GPU_IsSupported(): Int
internal external fun sdl_kmp_GPU_GetNumDrivers(): Int
