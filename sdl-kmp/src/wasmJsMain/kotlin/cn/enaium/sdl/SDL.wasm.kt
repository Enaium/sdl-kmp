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

@file:OptIn(kotlin.js.ExperimentalJsExport::class, kotlin.ExperimentalStdlibApi::class)

package cn.enaium.sdl

import org.khronos.webgl.Float32Array
import org.khronos.webgl.Int32Array
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toInt8Array
import org.khronos.webgl.toInt32Array
import org.khronos.webgl.toFloat32Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toFloatArray
import org.khronos.webgl.toIntArray
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsArray
import kotlin.js.JsString
import kotlin.js.toJsArray
import kotlin.js.toJsString

private fun r32(index: Int): Int = sdlKmpResultI32Get(index)

private fun rf32(index: Int): Float = sdlKmpResultF32Get(index)

private fun combine64(lo: Int, hi: Int): ULong = (hi.toULong() shl 32) or lo.toUInt().toULong()

// =========================================================================
// Window
// =========================================================================

private class WasmWindow(handle: Int) : SDLWindow {
    private var window: Int = handle

    override var ptr: Long
        get() = if (window == 0) 0L else window.toLong()
        set(value) { window = value.toInt() }

    override val id: Int get() = sdl_kmp_GetWindowID(window)
    override var title: String
        get() = sdl_kmp_GetWindowTitle(window) ?: ""
        set(value) = sdl_kmp_SetWindowTitle(window, value)

    override var size: SDLPoint
        get() {
            sdl_kmp_GetWindowSize(window)
            return SDLPoint(r32(0), r32(1))
        }
        set(value) = sdl_kmp_SetWindowSize(window, value.x, value.y)

    override var position: SDLPoint
        get() {
            sdl_kmp_GetWindowPosition(window)
            return SDLPoint(r32(0), r32(1))
        }
        set(value) = sdl_kmp_SetWindowPosition(window, value.x, value.y)

    override val sizeInPixels: SDLPoint
        get() {
            sdl_kmp_GetWindowSizeInPixels(window)
            return SDLPoint(r32(0), r32(1))
        }

    override val flags: ULong get() = sdl_kmp_GetWindowFlags(window).toUInt().toULong()
    override val displayId: Int get() = sdl_kmp_GetDisplayForWindow(window)

    override var opacity: Float
        get() = sdl_kmp_GetWindowOpacity(window).toFloat()
        set(value) { sdl_kmp_SetWindowOpacity(window, value.toDouble()) }

    override var fullscreen: Boolean
        get() = (flags and SDLWindowFlags.FULLSCREEN) != 0uL
        set(value) { sdl_kmp_SetWindowFullscreen(window, if (value) 1 else 0) }

    override var bordered: Boolean
        get() = (flags and SDLWindowFlags.BORDERLESS) == 0uL
        set(value) { sdl_kmp_SetWindowBordered(window, if (value) 1 else 0) }

    override var resizable: Boolean
        get() = (flags and SDLWindowFlags.RESIZABLE) != 0uL
        set(value) { sdl_kmp_SetWindowResizable(window, if (value) 1 else 0) }

    override var alwaysOnTop: Boolean
        get() = (flags and SDLWindowFlags.ALWAYS_ON_TOP) != 0uL
        set(value) { sdl_kmp_SetWindowAlwaysOnTop(window, if (value) 1 else 0) }

    override var mouseGrab: Boolean
        get() = sdl_kmp_GetWindowMouseGrab(window) != 0
        set(value) { sdl_kmp_SetWindowMouseGrab(window, if (value) 1 else 0) }

    override var keyboardGrab: Boolean
        get() = sdl_kmp_GetWindowKeyboardGrab(window) != 0
        set(value) { sdl_kmp_SetWindowKeyboardGrab(window, if (value) 1 else 0) }

    override var relativeMouseMode: Boolean
        get() = sdl_kmp_GetWindowRelativeMouseMode(window) != 0
        set(value) { sdl_kmp_SetWindowRelativeMouseMode(window, if (value) 1 else 0) }

    override var minimumSize: SDLPoint?
        get() {
            sdl_kmp_GetWindowMinimumSize(window)
            return SDLPoint(r32(0), r32(1))
        }
        set(value) { sdl_kmp_SetWindowMinimumSize(window, value?.x ?: 0, value?.y ?: 0) }

    override var maximumSize: SDLPoint?
        get() {
            sdl_kmp_GetWindowMaximumSize(window)
            return SDLPoint(r32(0), r32(1))
        }
        set(value) { sdl_kmp_SetWindowMaximumSize(window, value?.x ?: 0, value?.y ?: 0) }

    override var aspectRatio: SDLFloatPoint?
        get() {
            sdl_kmp_GetWindowAspectRatio(window)
            return if (r32(2) != 0) SDLFloatPoint(rf32(0), rf32(1)) else null
        }
        set(value) { sdl_kmp_SetWindowAspectRatio(window, value?.x?.toDouble() ?: 0.0, value?.y?.toDouble() ?: 0.0) }

    override fun show() = sdl_kmp_ShowWindow(window)
    override fun hide() = sdl_kmp_HideWindow(window)
    override fun raise() = sdl_kmp_RaiseWindow(window)
    override fun maximize() = sdl_kmp_MaximizeWindow(window)
    override fun minimize() = sdl_kmp_MinimizeWindow(window)
    override fun restore() = sdl_kmp_RestoreWindow(window)
    override fun flash() { sdl_kmp_FlashWindow(window, 0) }

    override val surface: SDLSurface?
        get() {
            val s = sdl_kmp_GetWindowSurface(window)
            return if (s == 0) null else WasmSurface(s, owned = false)
        }

    override fun setIcon(icon: SDLSurface): Boolean {
        val ws = (icon as? WasmSurface)?.surface ?: return false
        return sdl_kmp_SetWindowIcon(window, ws) != 0
    }

    override fun close() {
        if (window != 0) {
            sdl_kmp_DestroyWindow(window)
            window = 0
        }
    }
}

// =========================================================================
// Renderer
// =========================================================================

private class WasmRenderer(handle: Int) : SDLRenderer {
    private var renderer: Int = handle

    override var ptr: Long
        get() = if (renderer == 0) 0L else renderer.toLong()
        set(value) { renderer = value.toInt() }

    override val name: String? get() = sdl_kmp_GetRendererName(renderer)

    override var drawColor: SDLColor
        get() {
            sdl_kmp_GetRenderDrawColor(renderer)
            return SDLColor(r32(0), r32(1), r32(2), r32(3))
        }
        set(value) { sdl_kmp_SetRenderDrawColor(renderer, value.r, value.g, value.b, value.a) }

    override val outputSize: SDLPoint
        get() {
            sdl_kmp_GetRenderOutputSize(renderer)
            return SDLPoint(r32(0), r32(1))
        }

    override val currentOutputSize: SDLPoint
        get() {
            sdl_kmp_GetCurrentRenderOutputSize(renderer)
            return SDLPoint(r32(0), r32(1))
        }

    override var viewport: SDLRect?
        get() {
            sdl_kmp_GetRenderViewport(renderer)
            return if (r32(4) != 0) SDLRect(r32(0), r32(1), r32(2), r32(3)) else null
        }
        set(value) {
            if (value == null) sdl_kmp_SetRenderViewportNull(renderer)
            else sdl_kmp_SetRenderViewport(renderer, value.x, value.y, value.width, value.height)
        }

    override var clipRect: SDLRect?
        get() {
            sdl_kmp_GetRenderClipRect(renderer)
            return if (r32(4) != 0) SDLRect(r32(0), r32(1), r32(2), r32(3)) else null
        }
        set(value) {
            if (value == null) sdl_kmp_SetRenderClipRectNull(renderer)
            else sdl_kmp_SetRenderClipRect(renderer, value.x, value.y, value.width, value.height)
        }

    override var scale: SDLFloatPoint
        get() {
            sdl_kmp_GetRenderScale(renderer)
            return SDLFloatPoint(rf32(0), rf32(1))
        }
        set(value) = sdl_kmp_SetRenderScale(renderer, value.x.toDouble(), value.y.toDouble())

    override var blendMode: Int
        get() = sdl_kmp_GetRenderDrawBlendMode(renderer)
        set(value) { sdl_kmp_SetRenderDrawBlendMode(renderer, value) }

    override var vsync: Boolean
        get() = sdl_kmp_GetRenderVSync(renderer) != 0
        set(value) { sdl_kmp_SetRenderVSync(renderer, if (value) 1 else 0) }

    override var target: SDLTexture?
        get() {
            val t = sdl_kmp_GetRenderTarget(renderer)
            return if (t == 0) null else WasmTexture(t, this)
        }
        set(value) { sdl_kmp_SetRenderTarget(renderer, (value as? WasmTexture)?.texture ?: 0) }

    override fun clear(): Boolean = sdl_kmp_RenderClear(renderer) != 0
    override fun present() = sdl_kmp_RenderPresent(renderer)

    override fun fillRect(rect: SDLRect): Boolean =
        sdl_kmp_RenderFillRect(renderer, rect.x.toDouble(), rect.y.toDouble(), rect.width.toDouble(), rect.height.toDouble()) != 0

    override fun drawRect(rect: SDLRect): Boolean =
        sdl_kmp_RenderRect(renderer, rect.x.toDouble(), rect.y.toDouble(), rect.width.toDouble(), rect.height.toDouble()) != 0

    override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int): Boolean =
        sdl_kmp_RenderLine(renderer, x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble()) != 0

    override fun drawPoint(x: Int, y: Int): Boolean =
        sdl_kmp_RenderPoint(renderer, x.toDouble(), y.toDouble()) != 0

    override fun drawPoints(points: List<SDLPoint>): Boolean {
        val arr = FloatArray(points.size * 2)
        points.forEachIndexed { i, p ->
            arr[i * 2] = p.x.toFloat(); arr[i * 2 + 1] = p.y.toFloat()
        }
        return sdl_kmp_RenderPoints(renderer, arr.toFloat32Array(), points.size) != 0
    }

    override fun createTexture(format: Int, access: Int, width: Int, height: Int): SDLTexture {
        val t = sdl_kmp_CreateTexture(renderer, format, access, width, height)
        check(t != 0) { "SDL_CreateTexture failed: ${SDL.error()}" }
        return WasmTexture(t, this)
    }

    override fun createTextureFromSurface(surface: SDLSurface): SDLTexture {
        val ws = (surface as? WasmSurface)?.surface ?: throw IllegalArgumentException("surface is not a wasm SDL surface")
        val t = sdl_kmp_CreateTextureFromSurface(renderer, ws)
        check(t != 0) { "SDL_CreateTextureFromSurface failed: ${SDL.error()}" }
        return WasmTexture(t, this)
    }

    override fun renderTexture(texture: SDLTexture, src: SDLFRect?, dst: SDLFRect?): Boolean {
        val t = (texture as? WasmTexture)?.texture ?: return false
        val hasSrc = if (src != null) 1 else 0
        val hasDst = if (dst != null) 1 else 0
        return sdl_kmp_RenderTexture(
            renderer, t,
            hasSrc, src?.x?.toDouble() ?: 0.0, src?.y?.toDouble() ?: 0.0, src?.width?.toDouble() ?: 0.0, src?.height?.toDouble() ?: 0.0,
            hasDst, dst?.x?.toDouble() ?: 0.0, dst?.y?.toDouble() ?: 0.0, dst?.width?.toDouble() ?: 0.0, dst?.height?.toDouble() ?: 0.0,
        ) != 0
    }

    override fun renderTextureRotated(
        texture: SDLTexture,
        src: SDLFRect?,
        dst: SDLFRect?,
        angle: Double,
        center: SDLFloatPoint?,
        flip: Int,
    ): Boolean {
        val t = (texture as? WasmTexture)?.texture ?: return false
        val hasSrc = if (src != null) 1 else 0
        val hasDst = if (dst != null) 1 else 0
        val hasCenter = if (center != null) 1 else 0
        return sdl_kmp_RenderTextureRotated(
            renderer, t,
            hasSrc, src?.x?.toDouble() ?: 0.0, src?.y?.toDouble() ?: 0.0, src?.width?.toDouble() ?: 0.0, src?.height?.toDouble() ?: 0.0,
            hasDst, dst?.x?.toDouble() ?: 0.0, dst?.y?.toDouble() ?: 0.0, dst?.width?.toDouble() ?: 0.0, dst?.height?.toDouble() ?: 0.0,
            angle, hasCenter, center?.x?.toDouble() ?: 0.0, center?.y?.toDouble() ?: 0.0, flip,
        ) != 0
    }

    override fun renderTexture9Grid(
        texture: SDLTexture,
        src: SDLFRect,
        leftWidth: Float,
        rightWidth: Float,
        topHeight: Float,
        bottomHeight: Float,
        scale: Float,
        dst: SDLFRect,
    ): Boolean {
        val t = (texture as? WasmTexture)?.texture ?: return false
        return sdl_kmp_RenderTexture9Grid(
            renderer, t,
            src.x.toDouble(), src.y.toDouble(), src.width.toDouble(), src.height.toDouble(),
            leftWidth.toDouble(), rightWidth.toDouble(), topHeight.toDouble(), bottomHeight.toDouble(), scale.toDouble(),
            dst.x.toDouble(), dst.y.toDouble(), dst.width.toDouble(), dst.height.toDouble(),
        ) != 0
    }

    override fun renderGeometry(texture: SDLTexture?, vertices: List<SDLVertex>, indices: IntArray?): Boolean {
        val t = (texture as? WasmTexture)?.texture ?: 0
        val verts = FloatArray(vertices.size * 8)
        vertices.forEachIndexed { i, v ->
            verts[i * 8] = v.position.x
            verts[i * 8 + 1] = v.position.y
            verts[i * 8 + 2] = v.color.r / 255f
            verts[i * 8 + 3] = v.color.g / 255f
            verts[i * 8 + 4] = v.color.b / 255f
            verts[i * 8 + 5] = v.color.a / 255f
            verts[i * 8 + 6] = v.texCoord.x
            verts[i * 8 + 7] = v.texCoord.y
        }
        return sdl_kmp_RenderGeometry(
            renderer, t,
            verts.toFloat32Array(), vertices.size,
            indices?.toInt32Array(), indices?.size ?: 0,
        ) != 0
    }

    override fun renderReadPixels(rect: SDLRect?): SDLSurface? {
        val s = if (rect == null) {
            sdl_kmp_RenderReadPixels(renderer, 0, 0, 0, 0, 0)
        } else {
            sdl_kmp_RenderReadPixels(renderer, 1, rect.x, rect.y, rect.width, rect.height)
        }
        return if (s == 0) null else WasmSurface(s, owned = true)
    }

    override fun setLogicalPresentation(width: Int, height: Int, mode: Int): Boolean =
        sdl_kmp_SetRenderLogicalPresentation(renderer, width, height, mode) != 0

    override val logicalPresentationRect: SDLFRect?
        get() {
            sdl_kmp_GetRenderLogicalPresentationRect(renderer)
            return if (r32(4) != 0) SDLFRect(rf32(0), rf32(1), rf32(2), rf32(3)) else null
        }

    override fun close() {
        if (renderer != 0) {
            sdl_kmp_DestroyRenderer(renderer)
            renderer = 0
        }
    }
}

// =========================================================================
// Texture
// =========================================================================

private class WasmTexture(handle: Int, private val renderer: WasmRenderer) : SDLTexture {
    internal var texture: Int = handle

    override var ptr: Long
        get() = if (texture == 0) 0L else texture.toLong()
        set(value) { texture = value.toInt() }

    override val format: Int get() = sdl_kmp_GetTextureFormat(texture)
    override val access: Int get() = sdl_kmp_GetTextureAccess(texture)

    override val size: SDLFloatPoint
        get() {
            sdl_kmp_GetTextureSize(texture)
            return SDLFloatPoint(rf32(0), rf32(1))
        }

    override var colorMod: SDLColor
        get() {
            sdl_kmp_GetTextureColorMod(texture)
            return SDLColor(r32(0), r32(1), r32(2))
        }
        set(value) { sdl_kmp_SetTextureColorMod(texture, value.r, value.g, value.b) }

    override var alphaMod: Int
        get() = sdl_kmp_GetTextureAlphaMod(texture)
        set(value) { sdl_kmp_SetTextureAlphaMod(texture, value) }

    override var blendMode: Int
        get() = sdl_kmp_GetTextureBlendMode(texture)
        set(value) { sdl_kmp_SetTextureBlendMode(texture, value) }

    override var scaleMode: Int
        get() = sdl_kmp_GetTextureScaleMode(texture)
        set(value) { sdl_kmp_SetTextureScaleMode(texture, value) }

    override fun update(rect: SDLRect?, pixels: ByteArray, pitch: Int): Boolean {
        val hasRect = if (rect != null) 1 else 0
        return sdl_kmp_UpdateTexture(
            texture, hasRect,
            rect?.x ?: 0, rect?.y ?: 0, rect?.width ?: 0, rect?.height ?: 0,
            pixels.toInt8Array(), pitch,
        ) != 0
    }

    override fun lock(rect: SDLRect?): SDLTextureLock? {
        val hasRect = if (rect != null) 1 else 0
        val pitch = sdl_kmp_LockTexture(texture, hasRect, rect?.x ?: 0, rect?.y ?: 0, rect?.width ?: 0, rect?.height ?: 0)
        if (pitch == 0) return null
        val size = (rect?.height ?: this.size.y.toInt()) * pitch
        val ptr = sdl_kmp_LockedPixelsPtr()
        val bytes = ByteArray(size.coerceAtLeast(0))
        if (ptr != 0) {
            val arr = sdlKmpHeapBytes(ptr, size.coerceAtLeast(0)).toByteArray()
            for (i in bytes.indices) bytes[i] = arr[i]
        }
        return SDLTextureLock(bytes, pitch)
    }

    override fun unlock() = sdl_kmp_UnlockTexture(texture)

    override fun close() {
        if (texture != 0) {
            sdl_kmp_DestroyTexture(texture)
            texture = 0
        }
    }
}

// =========================================================================
// Display
// =========================================================================

private class WasmDisplay(private val index: Int) : SDLDisplay {
    override val id: Int get() = sdl_kmp_GetDisplayID(index)
    override val name: String? get() = sdl_kmp_GetDisplayName(index)
    override val bounds: SDLRect
        get() {
            sdl_kmp_GetDisplayBounds2(index)
            return SDLRect(r32(0), r32(1), r32(2), r32(3))
        }
    override val usableBounds: SDLRect
        get() {
            sdl_kmp_GetDisplayUsableBounds(index)
            return SDLRect(r32(0), r32(1), r32(2), r32(3))
        }
    override val currentMode: SDLDisplayMode
        get() {
            sdl_kmp_GetDisplayCurrentMode(index)
            return SDLDisplayMode(r32(0), r32(1), r32(2), rf32(0), rf32(1))
        }
    override val desktopMode: SDLDisplayMode
        get() {
            sdl_kmp_GetDisplayDesktopMode(index)
            return SDLDisplayMode(r32(0), r32(1), r32(2), rf32(0), rf32(1))
        }
    override val primary: Boolean get() = sdl_kmp_GetPrimaryDisplay() == id
}

// =========================================================================
// Audio
// =========================================================================

private class WasmAudioDevice(handle: Int, override val isRecording: Boolean) : SDLAudioDevice {
    private var device: Int = handle

    override val id: Int get() = device
    override val format: SDLAudioSpec
        get() {
            sdl_kmp_GetAudioDeviceFormat2(device)
            return SDLAudioSpec(format = r32(0), channels = r32(1), freq = r32(2))
        }

    override fun bindStream(stream: SDLAudioStream): Boolean {
        val ws = (stream as? WasmAudioStream)?.stream ?: return false
        return sdl_kmp_BindAudioStream(device, ws) != 0
    }

    override fun unbindStream(stream: SDLAudioStream) {
        val ws = (stream as? WasmAudioStream)?.stream ?: return
        sdl_kmp_UnbindAudioStream(ws)
    }

    override fun pause() = sdl_kmp_PauseAudioDevice(device)
    override fun resume() = sdl_kmp_ResumeAudioDevice(device)

    override fun close() {
        if (device != 0) {
            sdl_kmp_CloseAudioDevice(device)
            device = 0
        }
    }
}

private class WasmAudioStream(handle: Int) : SDLAudioStream {
    internal var stream: Int = handle

    override var ptr: Long
        get() = if (stream == 0) 0L else stream.toLong()
        set(value) { stream = value.toInt() }

    override fun putData(data: ByteArray): Boolean =
        sdl_kmp_PutAudioStreamData(stream, data.toInt8Array(), data.size) != 0

    override fun getData(maxLen: Int): ByteArray {
        if (maxLen <= 0) return ByteArray(0)
        val out = Int8Array(maxLen)
        val n = sdl_kmp_GetAudioStreamData(stream, out)
        return if (n <= 0) ByteArray(0) else out.subarray(0, n).toByteArray()
    }

    override val available: Int get() = sdl_kmp_GetAudioStreamAvailable(stream)
    override val queued: Int get() = sdl_kmp_GetAudioStreamQueued(stream)

    override val inputSpec: SDLAudioSpec?
        get() {
            sdl_kmp_GetAudioStreamFormat(stream)
            return if (r32(6) != 0) SDLAudioSpec(format = r32(0), channels = r32(1), freq = r32(2)) else null
        }

    override val outputSpec: SDLAudioSpec?
        get() {
            sdl_kmp_GetAudioStreamFormat(stream)
            return if (r32(6) != 0) SDLAudioSpec(format = r32(3), channels = r32(4), freq = r32(5)) else null
        }

    override fun setFormat(src: SDLAudioSpec, dst: SDLAudioSpec): Boolean =
        sdl_kmp_SetAudioStreamFormat(stream, src.format, src.channels, src.freq, dst.format, dst.channels, dst.freq) != 0

    override var gain: Float
        get() = sdl_kmp_GetAudioStreamGain(stream).toFloat()
        set(value) { sdl_kmp_SetAudioStreamGain(stream, value.toDouble()) }

    override var frequencyRatio: Float
        get() = sdl_kmp_GetAudioStreamFrequencyRatio(stream).toFloat()
        set(value) { sdl_kmp_SetAudioStreamFrequencyRatio(stream, value.toDouble()) }

    override var devicePaused: Boolean
        get() = sdl_kmp_GetAudioStreamDevicePaused(stream) != 0
        set(value) {
            if (value) sdl_kmp_PauseAudioStreamDevice(stream) else sdl_kmp_ResumeAudioStreamDevice(stream)
        }

    override fun resume() = sdl_kmp_ResumeAudioStreamDevice(stream)
    override fun pause() = sdl_kmp_PauseAudioStreamDevice(stream)
    override fun flush(): Boolean = sdl_kmp_FlushAudioStream(stream) != 0
    override fun clear(): Boolean = sdl_kmp_ClearAudioStream(stream) != 0

    override fun close() {
        if (stream != 0) {
            sdl_kmp_DestroyAudioStream(stream)
            stream = 0
        }
    }
}

// =========================================================================
// Joystick / gamepad
// =========================================================================

private class WasmJoystick(handle: Int) : SDLJoystick {
    private var joystick: Int = handle

    override var ptr: Long
        get() = if (joystick == 0) 0L else joystick.toLong()
        set(value) { joystick = value.toInt() }

    override val id: Int get() = sdl_kmp_GetJoystickIDFromJoystick(joystick)
    override val name: String? get() = sdl_kmp_GetJoystickName(joystick)
    override val type: Int get() = sdl_kmp_GetJoystickType(joystick)
    override val numAxes: Int get() = sdl_kmp_GetNumJoystickAxes(joystick)
    override val numBalls: Int get() = sdl_kmp_GetNumJoystickBalls(joystick)
    override val numHats: Int get() = sdl_kmp_GetNumJoystickHats(joystick)
    override val numButtons: Int get() = sdl_kmp_GetNumJoystickButtons(joystick)
    override val playerIndex: Int get() = sdl_kmp_GetJoystickPlayerIndex(joystick)
    override val firmwareVersion: Int get() = sdl_kmp_GetJoystickFirmwareVersion(joystick)

    override fun axis(axis: Int): Short = sdl_kmp_GetJoystickAxis(joystick, axis).toShort()
    override fun button(button: Int): Boolean = sdl_kmp_GetJoystickButton(joystick, button) != 0
    override fun hat(hat: Int): UByte = sdl_kmp_GetJoystickHat(joystick, hat).toUByte()

    override fun ball(ball: Int): SDLPoint? {
        sdl_kmp_GetJoystickBall(joystick, ball)
        return if (r32(2) != 0) SDLPoint(r32(0), r32(1)) else null
    }

    override fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean =
        sdl_kmp_JoystickRumble(joystick, lowFrequency, highFrequency, durationMs) != 0

    override fun close() {
        if (joystick != 0) {
            sdl_kmp_CloseJoystick(joystick)
            joystick = 0
        }
    }
}

private class WasmGamepad(handle: Int) : SDLGamepad {
    private var gamepad: Int = handle

    override var ptr: Long
        get() = if (gamepad == 0) 0L else gamepad.toLong()
        set(value) { gamepad = value.toInt() }

    override val id: Int get() = sdl_kmp_GetGamepadIDFromGamepad(gamepad)
    override val name: String? get() = sdl_kmp_GetGamepadName(gamepad)
    override val vendor: Int get() = sdl_kmp_GetGamepadVendor(gamepad)
    override val product: Int get() = sdl_kmp_GetGamepadProduct(gamepad)
    override val serial: String? get() = sdl_kmp_GetGamepadSerial(gamepad)
    override val connected: Boolean get() = sdl_kmp_GamepadConnected(gamepad) != 0
    override val playerIndex: Int get() = sdl_kmp_GetGamepadPlayerIndex(gamepad)
    override val firmwareVersion: Int get() = sdl_kmp_GetGamepadFirmwareVersion(gamepad)
    override val touchpadCount: Int get() = sdl_kmp_GetNumGamepadTouchpads(gamepad)

    override fun button(button: Int): Boolean = sdl_kmp_GetGamepadButton(gamepad, button) != 0
    override fun axis(axis: Int): Short = sdl_kmp_GetGamepadAxis(gamepad, axis).toShort()

    override fun touchpadFinger(touchpad: Int, finger: Int): SDLTouchpadFinger? {
        sdl_kmp_GetGamepadTouchpadFinger(gamepad, touchpad, finger)
        return if (r32(4) != 0) SDLTouchpadFinger(touchpad, finger, r32(0) != 0, rf32(0), rf32(1), rf32(2)) else null
    }

    override fun hasSensor(type: Int): Boolean = sdl_kmp_GamepadHasSensor(gamepad, type) != 0

    override fun sensorData(type: Int): FloatArray? {
        sdl_kmp_GetGamepadSensorData(gamepad, type)
        return if (r32(3) != 0) floatArrayOf(rf32(0), rf32(1), rf32(2)) else null
    }

    override fun getSensorDataRate(type: Int): Float = sdl_kmp_GetGamepadSensorDataRate(gamepad, type).toFloat()

    override fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean =
        sdl_kmp_GamepadRumble(gamepad, lowFrequency, highFrequency, durationMs) != 0

    override fun close() {
        if (gamepad != 0) {
            sdl_kmp_CloseGamepad(gamepad)
            gamepad = 0
        }
    }
}

// =========================================================================
// Event decoding
// =========================================================================

@OptIn(ExperimentalJsExport::class)
private fun eventTimestamp(): ULong = combine64(sdl_kmp_EventTimestampLo(), sdl_kmp_EventTimestampHi())

private fun decodeEvent(type: Int): SDLEvent {
    return when (type) {
        SDLEventType.QUIT -> SDLEvent.Quit(eventTimestamp())

        in SDLEventType.WINDOW_FIRST until SDLEventType.KEY_FIRST -> SDLEvent.Window(
            timestamp = eventTimestamp(),
            windowId = sdl_kmp_EventWindowID(),
            type = type,
            data1 = sdl_kmp_EventData1(),
            data2 = sdl_kmp_EventData2(),
        )

        SDLEventType.KEY_DOWN, SDLEventType.KEY_UP -> SDLEvent.Key(
            timestamp = eventTimestamp(),
            windowId = sdl_kmp_EventWindowID(),
            down = sdl_kmp_EventKeyDown() != 0,
            repeat = sdl_kmp_EventRepeat() != 0,
            keycode = sdl_kmp_EventKeycode(),
            scancode = sdl_kmp_EventScancode(),
            modifiers = sdl_kmp_EventMod(),
        )

        SDLEventType.TEXT_INPUT -> SDLEvent.TextInput(
            timestamp = eventTimestamp(),
            windowId = sdl_kmp_EventWindowID(),
            text = sdl_kmp_EventText() ?: "",
        )

        SDLEventType.MOUSE_MOTION -> SDLEvent.MouseMotion(
            timestamp = eventTimestamp(),
            windowId = sdl_kmp_EventWindowID(),
            x = sdl_kmp_EventMouseX().toFloat(),
            y = sdl_kmp_EventMouseY().toFloat(),
            dx = sdl_kmp_EventMouseDX().toFloat(),
            dy = sdl_kmp_EventMouseDY().toFloat(),
        )

        SDLEventType.MOUSE_BUTTON_DOWN, SDLEventType.MOUSE_BUTTON_UP -> SDLEvent.MouseButton(
            timestamp = eventTimestamp(),
            windowId = sdl_kmp_EventWindowID(),
            down = sdl_kmp_EventButtonDown() != 0,
            button = sdl_kmp_EventButton(),
            clicks = sdl_kmp_EventClicks(),
            x = sdl_kmp_EventButtonX().toFloat(),
            y = sdl_kmp_EventButtonY().toFloat(),
        )

        SDLEventType.MOUSE_WHEEL -> SDLEvent.MouseWheel(
            timestamp = eventTimestamp(),
            windowId = sdl_kmp_EventWindowID(),
            x = sdl_kmp_EventWheelX().toFloat(),
            y = sdl_kmp_EventWheelY().toFloat(),
            direction = sdl_kmp_EventWheelDir(),
        )

        in SDLEventType.DISPLAY_FIRST until SDLEventType.KEY_FIRST -> SDLEvent.Display(
            timestamp = eventTimestamp(),
            displayId = sdl_kmp_EventDisplayID(),
            type = type,
            data1 = sdl_kmp_EventData1(),
            data2 = sdl_kmp_EventData2(),
        )

        SDLEventType.DROP_FILE, SDLEventType.DROP_TEXT -> SDLEvent.Drop(
            timestamp = eventTimestamp(),
            windowId = sdl_kmp_EventWindowID(),
            type = type,
            file = sdl_kmp_EventDropData() ?: "",
        )

        SDLEventType.JOYSTICK_ADDED, SDLEventType.JOYSTICK_REMOVED -> SDLEvent.JoyDevice(
            timestamp = eventTimestamp(), deviceId = sdl_kmp_EventDeviceID(), type = type,
        )

        SDLEventType.JOYSTICK_AXIS_MOTION -> SDLEvent.JoyAxis(
            timestamp = eventTimestamp(), deviceId = sdl_kmp_EventDeviceID(),
            axis = sdl_kmp_EventAxis(), value = sdl_kmp_EventAxisValue().toShort(),
        )

        SDLEventType.JOYSTICK_BALL_MOTION -> SDLEvent.JoyBall(
            timestamp = eventTimestamp(), deviceId = sdl_kmp_EventDeviceID(),
            ball = sdl_kmp_EventBall(), dx = sdl_kmp_EventBallDX(), dy = sdl_kmp_EventBallDY(),
        )

        SDLEventType.JOYSTICK_HAT_MOTION -> SDLEvent.JoyHat(
            timestamp = eventTimestamp(), deviceId = sdl_kmp_EventDeviceID(),
            hat = sdl_kmp_EventHat(), value = sdl_kmp_EventHatValue().toUByte(),
        )

        SDLEventType.JOYSTICK_BUTTON_DOWN, SDLEventType.JOYSTICK_BUTTON_UP -> SDLEvent.JoyButton(
            timestamp = eventTimestamp(), deviceId = sdl_kmp_EventDeviceID(),
            button = sdl_kmp_EventButton(), down = sdl_kmp_EventButtonDown() != 0,
        )

        SDLEventType.JOYSTICK_BATTERY_UPDATED -> SDLEvent.JoyBattery(
            timestamp = eventTimestamp(), deviceId = sdl_kmp_EventDeviceID(),
            state = sdl_kmp_EventBatteryState(), percent = sdl_kmp_EventBatteryPercent(),
        )

        SDLEventType.GAMEPAD_ADDED, SDLEventType.GAMEPAD_REMOVED -> SDLEvent.GamepadDevice(
            timestamp = eventTimestamp(), deviceId = sdl_kmp_EventDeviceID(), type = type,
        )

        SDLEventType.GAMEPAD_AXIS_MOTION -> SDLEvent.GamepadAxis(
            timestamp = eventTimestamp(), deviceId = sdl_kmp_EventDeviceID(),
            axis = sdl_kmp_EventAxis(), value = sdl_kmp_EventAxisValue().toShort(),
        )

        SDLEventType.GAMEPAD_BUTTON_DOWN, SDLEventType.GAMEPAD_BUTTON_UP -> SDLEvent.GamepadButton(
            timestamp = eventTimestamp(), deviceId = sdl_kmp_EventDeviceID(),
            button = sdl_kmp_EventButton(), down = sdl_kmp_EventButtonDown() != 0,
        )

        in SDLEventType.GAMEPAD_TOUCHPAD_DOWN..SDLEventType.GAMEPAD_TOUCHPAD_UP -> SDLEvent.GamepadTouchpad(
            timestamp = eventTimestamp(),
            deviceId = sdl_kmp_EventDeviceID(),
            touchpad = sdl_kmp_EventTouchpad(),
            finger = sdl_kmp_EventFinger(),
            down = sdl_kmp_EventFingerDown() != 0,
            x = sdl_kmp_EventFingerX().toFloat(),
            y = sdl_kmp_EventFingerY().toFloat(),
            pressure = sdl_kmp_EventFingerPressure().toFloat(),
        )

        SDLEventType.GAMEPAD_SENSOR_UPDATE -> SDLEvent.GamepadSensor(
            timestamp = eventTimestamp(),
            deviceId = sdl_kmp_EventDeviceID(),
            sensor = sdl_kmp_EventSensorType(),
            data = floatArrayOf(
                sdl_kmp_EventSensorData(0).toFloat(),
                sdl_kmp_EventSensorData(1).toFloat(),
                sdl_kmp_EventSensorData(2).toFloat(),
            ),
        )

        SDLEventType.SENSOR_UPDATE -> SDLEvent.SensorUpdate(
            timestamp = eventTimestamp(),
            sensorId = sdl_kmp_EventDeviceID().toUInt().toULong(),
            data = floatArrayOf(
                sdl_kmp_EventSensorData(0).toFloat(),
                sdl_kmp_EventSensorData(1).toFloat(),
                sdl_kmp_EventSensorData(2).toFloat(),
            ),
        )

        SDLEventType.CAMERA_DEVICE_ADDED, SDLEventType.CAMERA_DEVICE_REMOVED,
        SDLEventType.CAMERA_DEVICE_APPROVED, SDLEventType.CAMERA_DEVICE_DENIED,
        -> SDLEvent.CameraDevice(
            timestamp = eventTimestamp(), deviceId = sdl_kmp_EventDeviceID(), type = type,
        )

        in SDLEventType.TOUCH_FIRST until SDLEventType.DROP_FIRST -> SDLEvent.TouchFinger(
            timestamp = eventTimestamp(),
            touchId = combine64(sdl_kmp_EventTouchIDLo(), sdl_kmp_EventTouchIDHi()),
            fingerId = combine64(sdl_kmp_EventFingerIDLo(), sdl_kmp_EventFingerIDHi()),
            type = type,
            x = sdl_kmp_EventTouchX().toFloat(),
            y = sdl_kmp_EventTouchY().toFloat(),
            dx = sdl_kmp_EventTouchDX().toFloat(),
            dy = sdl_kmp_EventTouchDY().toFloat(),
            pressure = sdl_kmp_EventTouchPressure().toFloat(),
        )

        SDLEventType.AUDIO_DEVICE_ADDED, SDLEventType.AUDIO_DEVICE_REMOVED,
        SDLEventType.AUDIO_DEVICE_FORMAT_CHANGED,
        -> SDLEvent.AudioDevice(
            timestamp = eventTimestamp(),
            deviceId = sdl_kmp_EventDeviceID(),
            isCapture = sdl_kmp_EventAudioCapture() != 0,
            type = type,
        )

        SDLEventType.CLIPBOARD_UPDATE -> SDLEvent.ClipboardUpdate(
            timestamp = eventTimestamp(), owner = sdl_kmp_EventClipboardOwner() != 0,
        )

        SDLEventType.RENDER_TARGETS_RESET -> SDLEvent.RenderTargetsReset(eventTimestamp())

        else -> SDLEvent.Unknown(timestamp = eventTimestamp(), type = type)
    }
}

// =========================================================================
// Raw event
// =========================================================================

private class WasmEventRaw(handle: Int, private val decoded: SDLEvent) : SDLEventRaw {
    private var address: Long = handle.toLong()
    override val ptr: Long
        get() = address

    @OptIn(ExperimentalJsExport::class)
    val event: SDLEvent get() = decoded

    override fun close() {
        address = 0L
    }
}

// =========================================================================
// SDL actual
// =========================================================================

actual object SDL {

    actual fun setMainReady() {
        // SDL3 on Emscripten does not require SDL_SetMainReady.
    }

    actual fun init(flags: Int): Boolean = sdl_kmp_Init(flags) != 0

    actual fun initSubSystem(flags: Int): Boolean = sdl_kmp_InitSubSystem(flags) != 0

    actual fun quitSubSystem(flags: Int) = sdl_kmp_QuitSubSystem(flags)

    actual fun wasInit(flags: Int): Int = sdl_kmp_WasInit(flags)

    actual fun quit() = sdl_kmp_Quit()

    actual fun error(): String? = sdl_kmp_GetError()?.takeIf { it.isNotEmpty() }

    actual fun clearError() = sdl_kmp_ClearError()

    actual fun setError(message: String): Boolean = sdl_kmp_SetError(message) != 0

    actual fun version(): SDLVersion {
        val v = sdl_kmp_GetVersion()
        return SDLVersion(major = v / 1000000, minor = (v / 1000) % 1000, micro = v % 1000)
    }

    actual fun revision(): String? = sdl_kmp_GetRevision()

    actual fun getTicks(): ULong = sdl_kmp_GetTicks().toUInt().toULong()

    actual fun performanceCounter(): ULong = combine64(sdl_kmp_PerfCounterLo(), sdl_kmp_PerfCounterHi())

    actual fun performanceFrequency(): ULong = combine64(sdl_kmp_PerfFreqLo(), sdl_kmp_PerfFreqHi())

    actual fun delay(ms: Int) = sdl_kmp_Delay(ms)

    actual fun pollEvent(): SDLEvent? {
        val type = sdl_kmp_PollEvent()
        if (type == 0) return null
        return decodeEvent(type)
    }

    actual fun waitEvent(): SDLEvent? {
        val type = sdl_kmp_WaitEvent()
        if (type == 0) return null
        return decodeEvent(type)
    }

    actual fun pollEventRaw(): SDLEventRaw? {
        val type = sdl_kmp_PollEvent()
        if (type == 0) return null
        return WasmEventRaw(0, decodeEvent(type))
    }

    actual fun waitEventRaw(): SDLEventRaw? {
        val type = sdl_kmp_WaitEvent()
        if (type == 0) return null
        return WasmEventRaw(0, decodeEvent(type))
    }

    actual fun pumpEvents() = sdl_kmp_PumpEvents()

    actual fun createWindow(title: String, width: Int, height: Int, flags: ULong): SDLWindow {
        val window = sdl_kmp_CreateWindow(title, width, height, flags.toInt(), (flags shr 32).toInt())
        check(window != 0) { "SDL_CreateWindow failed: ${error()}" }
        return WasmWindow(window)
    }

    actual fun createRenderer(window: SDLWindow, name: String?, flags: Int): SDLRenderer {
        val w = (window as? WasmWindow)?.ptr?.toInt() ?: throw IllegalArgumentException("window is not a wasm SDL window")
        val renderer = sdl_kmp_CreateRenderer(w, name)
        check(renderer != 0) { "SDL_CreateRenderer failed: ${error()}" }
        return WasmRenderer(renderer)
    }

    actual fun setHint(name: String, value: String): Boolean = sdl_kmp_SetHint(name, value) != 0

    actual fun getHint(name: String): String? = sdl_kmp_GetHint(name)

    actual fun getClipboardText(): String? = sdl_kmp_GetClipboardText()

    actual fun setClipboardText(text: String): Boolean = sdl_kmp_SetClipboardText(text) != 0

    actual fun getNumVideoDrivers(): Int = sdl_kmp_GetNumVideoDrivers()

    actual fun getVideoDriver(index: Int): String? = sdl_kmp_GetVideoDriver(index)

    actual fun getCurrentVideoDriver(): String? = sdl_kmp_GetCurrentVideoDriver()

    actual fun getNumAudioDrivers(): Int = sdl_kmp_GetNumAudioDrivers()

    actual fun getAudioDriver(index: Int): String? = sdl_kmp_GetAudioDriver(index)

    actual fun getCurrentAudioDriver(): String? = sdl_kmp_GetCurrentAudioDriver()

    actual fun textInputActive(windowId: Int): Boolean = sdl_kmp_TextInputActive(windowId) != 0

    actual fun startTextInput(windowId: Int): Boolean = sdl_kmp_StartTextInput(windowId) != 0

    actual fun stopTextInput(windowId: Int): Boolean = sdl_kmp_StopTextInput(windowId) != 0

    actual fun showSimpleMessageBox(title: String, message: String): Boolean =
        sdl_kmp_ShowSimpleMessageBox(title, message) != 0

    // ==================== displays ====================

    actual val numDisplays: Int
        get() = sdl_kmp_RefreshDisplays()

    actual fun getDisplay(index: Int): SDLDisplay {
        require(index in 0 until numDisplays) { "display index out of range: $index" }
        return WasmDisplay(index)
    }

    actual fun getPrimaryDisplay(): SDLDisplay = WasmDisplay(0)

    // ==================== renderer drivers ====================

    actual val numRenderDrivers: Int get() = sdl_kmp_GetNumRenderDrivers()

    actual fun getRenderDriver(index: Int): String? = sdl_kmp_GetRenderDriver(index)

    actual fun createWindowAndRenderer(title: String, width: Int, height: Int, flags: ULong): Pair<SDLWindow, SDLRenderer> {
        val window = createWindow(title, width, height, flags)
        val renderer = createRenderer(window)
        return Pair(window, renderer)
    }

    // ==================== pixels ====================

    actual fun getPixelFormatName(format: Int): String? = sdl_kmp_GetPixelFormatName(format)

    actual fun mapRGB(format: Int, r: Int, g: Int, b: Int): Int = sdl_kmp_MapRGB(format, r, g, b)

    actual fun mapRGBA(format: Int, r: Int, g: Int, b: Int, a: Int): Int = sdl_kmp_MapRGBA(format, r, g, b, a)

    actual fun getRGBA(format: Int, pixel: Int): SDLColor {
        sdl_kmp_GetRGBA(format, pixel)
        return SDLColor(r32(0), r32(1), r32(2), r32(3))
    }

    // ==================== surfaces ====================

    actual fun createSurface(width: Int, height: Int, format: Int): SDLSurface {
        val surface = sdl_kmp_CreateSurface(width, height, format)
        check(surface != 0) { "SDL_CreateSurface failed: ${error()}" }
        return WasmSurface(surface, owned = true)
    }

    actual fun loadBMP(path: String): SDLSurface {
        val surface = sdl_kmp_LoadBMP(path)
        check(surface != 0) { "SDL_LoadBMP failed: ${error()}" }
        return WasmSurface(surface, owned = true)
    }

    // ==================== audio ====================

    actual val audioPlaybackDevices: List<Int>
        get() {
            sdl_kmp_RefreshAudioDevices()
            val count = sdl_kmp_GetAudioPlaybackCount()
            return (0 until count).map { sdl_kmp_GetAudioPlaybackDevice(it) }
        }

    actual val audioRecordingDevices: List<Int>
        get() {
            sdl_kmp_RefreshAudioDevices()
            val count = sdl_kmp_GetAudioRecordingCount()
            return (0 until count).map { sdl_kmp_GetAudioRecordingDevice(it) }
        }

    actual fun getAudioDeviceName(deviceId: Int): String? = sdl_kmp_GetAudioDeviceName(deviceId)

    actual fun openAudioDevice(deviceId: Int, spec: SDLAudioSpec): SDLAudioDevice {
        val device = sdl_kmp_OpenAudioDevice(deviceId, spec.format, spec.channels, spec.freq)
        check(device != 0) { "SDL_OpenAudioDevice failed: ${error()}" }
        return WasmAudioDevice(device, isRecording = deviceId < 0 && deviceId == SDLAudioDeviceID.DEFAULT_RECORDING)
    }

    actual fun openAudioDeviceStream(deviceId: Int, spec: SDLAudioSpec): SDLAudioStream {
        val stream = sdl_kmp_OpenAudioDeviceStream(deviceId, spec.format, spec.channels, spec.freq)
        check(stream != 0) { "SDL_OpenAudioDeviceStream failed: ${error()}" }
        return WasmAudioStream(stream)
    }

    actual fun createAudioStream(srcSpec: SDLAudioSpec, dstSpec: SDLAudioSpec): SDLAudioStream {
        val stream = sdl_kmp_CreateAudioStream(srcSpec.format, srcSpec.channels, srcSpec.freq, dstSpec.format, dstSpec.channels, dstSpec.freq)
        check(stream != 0) { "SDL_CreateAudioStream failed: ${error()}" }
        return WasmAudioStream(stream)
    }

    actual fun pauseAudioDevice(deviceId: Int) = sdl_kmp_PauseAudioDevice(deviceId)

    actual fun resumeAudioDevice(deviceId: Int) = sdl_kmp_ResumeAudioDevice(deviceId)

    actual fun isAudioDevicePaused(deviceId: Int): Boolean = sdl_kmp_AudioDevicePaused(deviceId) != 0

    actual fun loadWAV(path: String): SDLAudioData? {
        if (sdl_kmp_LoadWAV(path) == 0) return null
        val len = sdl_kmp_LoadWAVLen()
        val ptr = sdl_kmp_LoadWAVData()
        val view = if (len > 0 && ptr != 0) sdlKmpHeapBytes(ptr, len).toByteArray() else null
        val data = if (view == null) ByteArray(0) else view
        sdl_kmp_LoadWAVFree()
        return SDLAudioData(
            spec = SDLAudioSpec(format = sdl_kmp_LoadWAVFormat(), channels = sdl_kmp_LoadWAVChannels(), freq = sdl_kmp_LoadWAVFreq()),
            data = data,
        )
    }

    // ==================== input focus ====================

    actual val keyboardFocusWindowId: Int? get() = null
    actual val mouseFocusWindowId: Int? get() = null

    // ==================== touch ====================

    actual val touchDevices: List<Int>
        get() {
            val count = sdl_kmp_RefreshTouchDevices()
            return (0 until count).map { sdl_kmp_GetTouchDevice(it) }
        }

    actual fun getTouchDeviceName(touchId: Int): String? = sdl_kmp_GetTouchDeviceName(touchId)

    actual fun getTouchDeviceType(touchId: Int): Int = sdl_kmp_GetTouchDeviceType(touchId)

    actual fun getTouchFingers(touchId: Int): List<SDLTouchFinger> {
        val count = sdl_kmp_RefreshTouchFingers(touchId)
        return (0 until count).map { i ->
            sdl_kmp_GetTouchFinger(i)
            SDLTouchFinger(
                id = combine64(r32(0), r32(1)),
                x = rf32(0),
                y = rf32(1),
                pressure = rf32(2),
                down = true,
            )
        }
    }

    // ==================== event filter / watch ====================

    private val eventWatches = mutableMapOf<Long, (SDLEventRaw) -> Boolean>()
    private var watchNextId = 0L

    actual fun addEventWatch(filter: (SDLEventRaw) -> Boolean): Boolean {
        val id = watchNextId++
        eventWatches[id] = filter
        return true
    }

    actual fun removeEventWatch(filter: (SDLEventRaw) -> Boolean) {
        val id = eventWatches.entries.firstOrNull { it.value === filter }?.key ?: return
        eventWatches.remove(id)
    }

    actual fun setEventEnabled(type: Int, enabled: Boolean) {
        sdl_kmp_EventSetEventEnabled(type, if (enabled) 1 else 0)
    }

    actual fun eventEnabled(type: Int): Boolean = sdl_kmp_EventEnabled(type) != 0

    actual fun flushEvents(minType: Int, maxType: Int) = sdl_kmp_FlushEvents(minType, maxType)

    actual fun pushEvent(event: SDLEventRaw): Boolean = false

    // ==================== file dialogs ====================

    actual fun showOpenFileDialog(
        windowId: Int?,
        filters: List<SDLDialogFileFilter>,
        defaultLocation: String?,
        allowMultiple: Boolean,
        callback: (List<String>) -> Unit,
    ) {
        showJvmLikeDialog(callback)
    }

    actual fun showSaveFileDialog(
        windowId: Int?,
        filters: List<SDLDialogFileFilter>,
        defaultLocation: String?,
        callback: (List<String>) -> Unit,
    ) {
        showJvmLikeDialog(callback)
    }

    actual fun showFolderDialog(
        windowId: Int?,
        defaultLocation: String?,
        allowMultiple: Boolean,
        callback: (List<String>) -> Unit,
    ) {
        showJvmLikeDialog(callback)
    }

    private fun showJvmLikeDialog(callback: (List<String>) -> Unit) {
        // SDL3's portal file dialogs are not wired on wasm; invoke the
        // callback with an empty selection (cancel).
        callback(emptyList())
    }

    // ==================== keyboard ====================

    actual val keyboardState: ByteArray
        get() {
            sdl_kmp_GetKeyboardState()
            val count = sdl_kmp_GetNumScancodes()
            val ptr = r32(0)
            val out = ByteArray(count)
            if (ptr != 0) {
                val arr = sdlKmpHeapBytes(ptr, count).toByteArray()
                for (i in 0 until count) out[i] = arr[i]
            }
            return out
        }

    actual val modState: Int get() = sdl_kmp_GetModState()

    actual fun setModState(modState: Int) = sdl_kmp_SetModState(modState)

    actual fun getKeyFromScancode(scancode: Int): Int = sdl_kmp_GetKeyFromScancode(scancode)

    actual fun getScancodeFromKey(keycode: Int): Int = sdl_kmp_GetScancodeFromKey(keycode)

    actual fun getKeyName(keycode: Int): String? = sdl_kmp_GetKeyName(keycode)

    actual fun getScancodeName(scancode: Int): String? = sdl_kmp_GetScancodeName(scancode)

    // ==================== mouse ====================

    actual val mouseState: SDLMouseState
        get() {
            sdl_kmp_GetMouseState()
            return SDLMouseState(rf32(0), rf32(1), r32(2))
        }

    actual val globalMouseState: SDLMouseState
        get() {
            sdl_kmp_GetGlobalMouseState()
            return SDLMouseState(rf32(0), rf32(1), r32(2))
        }

    actual fun warpMouseInWindow(windowId: Int, x: Float, y: Float) = sdl_kmp_WarpMouseInWindow(windowId, x.toDouble(), y.toDouble())

    actual fun captureMouse(enabled: Boolean): Boolean = sdl_kmp_CaptureMouse(if (enabled) 1 else 0) != 0

    actual fun showCursor(): Boolean = sdl_kmp_ShowCursor() != 0

    // ==================== joystick / gamepad ====================

    actual val joysticks: List<Int>
        get() {
            val count = sdl_kmp_RefreshJoysticks()
            return (0 until count).map { sdl_kmp_GetJoystickID(it) }
        }

    actual fun openJoystick(id: Int): SDLJoystick {
        val js = sdl_kmp_OpenJoystick(id)
        check(js != 0) { "SDL_OpenJoystick failed: ${error()}" }
        return WasmJoystick(js)
    }

    actual val gamepads: List<Int>
        get() {
            val count = sdl_kmp_RefreshGamepads()
            return (0 until count).map { sdl_kmp_GetGamepadID(it) }
        }

    actual fun openGamepad(id: Int): SDLGamepad {
        val gp = sdl_kmp_OpenGamepad(id)
        check(gp != 0) { "SDL_OpenGamepad failed: ${error()}" }
        return WasmGamepad(gp)
    }

    // ==================== filesystem / misc ====================

    actual val basePath: String? get() = sdl_kmp_GetBasePath()

    actual fun getPrefPath(orgName: String, appName: String): String? = sdl_kmp_GetPrefPath(orgName, appName)

    actual fun getUserFolder(folder: Int): String? = sdl_kmp_GetUserFolder(folder)

    actual fun createDirectory(path: String): Boolean = sdl_kmp_CreateDirectory(path) != 0

    actual fun removePath(path: String): Boolean = sdl_kmp_RemovePath(path) != 0

    actual fun renamePath(oldPath: String, newPath: String): Boolean = sdl_kmp_RenamePath(oldPath, newPath) != 0

    actual val powerInfo: SDLPowerInfo
        get() {
            sdl_kmp_GetPowerInfo()
            return SDLPowerInfo(state = r32(0), percent = r32(2), secondsLeft = r32(1))
        }

    actual fun openURL(url: String): Boolean = sdl_kmp_OpenURL(url) != 0

    actual val hasClipboardText: Boolean get() = sdl_kmp_HasClipboardText() != 0

    actual fun getHintBoolean(name: String, defaultValue: Boolean): Boolean = sdl_kmp_GetHintBoolean(name, if (defaultValue) 1 else 0) != 0

    actual fun showMessageBox(
        flags: Int,
        title: String,
        message: String,
        buttons: List<SDLMessageBoxButton>,
    ): Int {
        if (buttons.isEmpty()) return sdl_kmp_ShowSimpleMessageBox(title, message).let { -1 }
        val bflags = buttons.map { it.flags }.toIntArray().toInt32Array()
        val bids = buttons.map { it.id }.toIntArray().toInt32Array()
        val texts: JsArray<JsString> = buttons.map { it.text.toJsString() }.toJsArray()
        return sdl_kmp_ShowMessageBox(flags, title, message, bflags, bids, texts, buttons.size)
    }

    // ==================== OpenGL ====================

    actual fun glLoadLibrary(path: String?): Boolean = sdl_kmp_GL_LoadLibrary(path) != 0

    actual fun glUnloadLibrary() = sdl_kmp_GL_UnloadLibrary()

    actual fun glGetProcAddress(proc: String): ULong = sdl_kmp_GL_GetProcAddress(proc).toUInt().toULong()

    actual fun glExtensionSupported(extension: String): Boolean = sdl_kmp_GL_ExtensionSupported(extension) != 0

    actual fun glResetAttributes() = sdl_kmp_GL_ResetAttributes()

    actual fun glSetAttribute(attr: Int, value: Int): Boolean = sdl_kmp_GL_SetAttribute(attr, value) != 0

    actual fun glGetAttribute(attr: Int): Int? {
        sdl_kmp_GL_GetAttribute(attr)
        return if (r32(1) != 0) r32(0) else null
    }

    actual fun glCreateContext(windowId: Int): ULong = sdl_kmp_GL_CreateContext(windowId).toUInt().toULong()

    actual fun glMakeCurrent(windowId: Int, context: ULong): Boolean =
        sdl_kmp_GL_MakeCurrent(windowId, context.toInt()) != 0

    actual val glCurrentWindow: Int?
        get() = sdl_kmp_GL_GetCurrentWindow().takeIf { it != 0 }

    actual val glCurrentContext: ULong
        get() = sdl_kmp_GL_GetCurrentContext().toUInt().toULong()

    actual fun glSetSwapInterval(interval: Int): Boolean = sdl_kmp_GL_SetSwapInterval(interval) != 0

    actual val glSwapInterval: Int?
        get() {
            sdl_kmp_GL_GetSwapInterval()
            return if (r32(1) != 0) r32(0) else null
        }

    actual fun glSwapWindow(windowId: Int): Boolean = sdl_kmp_GL_SwapWindow(windowId) != 0

    actual fun glDestroyContext(context: ULong) {
        if (context != 0uL) sdl_kmp_GL_DestroyContext(context.toInt())
    }

    // ==================== Vulkan ====================

    actual fun vulkanLoadLibrary(path: String?): Boolean = sdl_kmp_Vulkan_LoadLibrary(path) != 0

    actual fun vulkanUnloadLibrary() = sdl_kmp_Vulkan_UnloadLibrary()

    actual val vulkanGetVkGetInstanceProcAddr: ULong
        get() = sdl_kmp_Vulkan_GetVkGetInstanceProcAddr().toUInt().toULong()

    actual val vulkanInstanceExtensions: List<String>
        get() {
            val count = sdl_kmp_Vulkan_GetInstanceExtensions()
            return (0 until count).mapNotNull { sdl_kmp_Vulkan_GetInstanceExtension(it) }
        }

    actual fun vulkanCreateSurface(windowId: Int, instance: ULong): ULong {
        val window = sdl_kmp_GetWindowFromID(windowId)
        if (window == 0) return 0uL
        return sdl_kmp_Vulkan_CreateSurface(window, instance.toInt()).toUInt().toULong()
    }

    actual fun vulkanDestroySurface(instance: ULong, surface: ULong) {
        if (surface != 0uL) sdl_kmp_Vulkan_DestroySurface(instance.toInt(), surface.toInt())
    }

    actual fun vulkanGetPresentationSupport(instance: ULong, physicalDevice: ULong, queueFamilyIndex: Int): Boolean =
        sdl_kmp_Vulkan_GetPresentationSupport(instance.toInt(), physicalDevice.toInt(), queueFamilyIndex) != 0
}
