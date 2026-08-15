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

/**
 * JNI bridge for the JVM target.
 *
 * Every `external fun` maps 1:1 to a `Java_cn_enaium_sdl_Jni_<name>`
 * function in the C sources under `jni/` (see jni_bridge.h for the naming
 * convention). All members are public (no `internal` modifier) so their JVM
 * names are not mangled by the Kotlin compiler.
 *
 * Struct-based SDL3 APIs (SDL_Rect, SDL_AudioSpec, SDL_Event fields, the GPU
 * create-info structs, ...) are marshaled on the C side; this object only
 * ever exchanges scalars, arrays and strings.
 */
internal object Jni {

    init {
        NativeLoader.load()
        initCallbackBridge()
    }

    // =========================================================================
    // Callback bridge (invoked from C through JNI)
    // =========================================================================

    @JvmStatic
    fun onEventWatch(id: Long, eventPtr: Long): Boolean {
        val filter = eventWatchCallbacks[id] ?: return true
        return try {
            filter(BorrowedJvmEventRaw(eventPtr))
        } catch (t: Throwable) {
            true
        }
    }

    @JvmStatic
    fun onDialogCallback(id: Long, files: Array<String>?) {
        dialogCallbacks.remove(id)?.invoke(files?.toList() ?: emptyList())
    }

    @JvmStatic
    fun onLogOutput(id: Long, category: Int, priority: Int, message: String) {
        logOutputCallbacks[id]?.log(priority, category, message)
    }

    external fun initCallbackBridge()

    // =========================================================================
    // Core
    // =========================================================================

    external fun setMainReady(): Boolean
    external fun init(flags: Int): Boolean
    external fun initSubSystem(flags: Int): Boolean
    external fun quitSubSystem(flags: Int)
    external fun wasInit(flags: Int): Int
    external fun quit()
    external fun getError(): String?
    external fun clearError()
    external fun setError(message: String): Boolean
    external fun getVersion(): Int
    external fun getRevision(): String?
    external fun getTicks(): Long
    external fun performanceCounter(): Long
    external fun performanceFrequency(): Long
    external fun delay(ms: Int)

    // =========================================================================
    // Window
    // =========================================================================

    external fun createWindow(title: String, width: Int, height: Int, flags: Long): Long
    external fun destroyWindow(window: Long)
    external fun getWindowID(window: Long): Int
    external fun getWindowFromID(windowId: Int): Long
    external fun getWindowTitle(window: Long): String?
    external fun setWindowTitle(window: Long, title: String)
    external fun getWindowSize(window: Long): IntArray
    external fun setWindowSize(window: Long, w: Int, h: Int)
    external fun getWindowFlags(window: Long): Long
    external fun getWindowPosition(window: Long): IntArray
    external fun setWindowPosition(window: Long, x: Int, y: Int)
    external fun getWindowSizeInPixels(window: Long): IntArray
    external fun getDisplayForWindow(window: Long): Int
    external fun getWindowOpacity(window: Long): Float
    external fun setWindowOpacity(window: Long, opacity: Float): Boolean
    external fun setWindowFullscreen(window: Long, fullscreen: Boolean): Boolean
    external fun setWindowBordered(window: Long, bordered: Boolean): Boolean
    external fun setWindowResizable(window: Long, resizable: Boolean): Boolean
    external fun setWindowAlwaysOnTop(window: Long, onTop: Boolean): Boolean
    external fun getWindowMouseGrab(window: Long): Boolean
    external fun setWindowMouseGrab(window: Long, grabbed: Boolean): Boolean
    external fun getWindowKeyboardGrab(window: Long): Boolean
    external fun setWindowKeyboardGrab(window: Long, grabbed: Boolean): Boolean
    external fun getWindowRelativeMouseMode(window: Long): Boolean
    external fun setWindowRelativeMouseMode(window: Long, enabled: Boolean): Boolean
    external fun getWindowMinimumSize(window: Long): IntArray
    external fun setWindowMinimumSize(window: Long, w: Int, h: Int)
    external fun getWindowMaximumSize(window: Long): IntArray
    external fun setWindowMaximumSize(window: Long, w: Int, h: Int)
    external fun maximizeWindow(window: Long)
    external fun minimizeWindow(window: Long)
    external fun restoreWindow(window: Long)
    external fun flashWindow(window: Long)
    external fun getWindowSurface(window: Long): Long
    external fun setWindowIcon(window: Long, icon: Long): Boolean
    external fun getWindowAspectRatio(window: Long): FloatArray?
    external fun setWindowAspectRatio(window: Long, minAspect: Float, maxAspect: Float): Boolean
    external fun showWindow(window: Long)
    external fun hideWindow(window: Long)
    external fun raiseWindow(window: Long)

    // =========================================================================
    // Renderer
    // =========================================================================

    external fun createRenderer(window: Long, name: String?): Long
    external fun createWindowAndRenderer(title: String, width: Int, height: Int, flags: Long): LongArray?
    external fun destroyRenderer(renderer: Long)
    external fun getRendererName(renderer: Long): String?
    external fun getRenderDrawColor(renderer: Long): IntArray
    external fun setRenderDrawColor(renderer: Long, r: Int, g: Int, b: Int, a: Int): Boolean
    external fun getRenderOutputSize(renderer: Long): IntArray
    external fun getCurrentRenderOutputSize(renderer: Long): IntArray
    external fun getRenderViewport(renderer: Long): IntArray
    external fun setRenderViewport(renderer: Long, rect: IntArray?): Boolean
    external fun getRenderClipRect(renderer: Long): IntArray
    external fun setRenderClipRect(renderer: Long, rect: IntArray?): Boolean
    external fun getRenderScale(renderer: Long): FloatArray
    external fun setRenderScale(renderer: Long, x: Float, y: Float): Boolean
    external fun getRenderDrawBlendMode(renderer: Long): Int
    external fun setRenderDrawBlendMode(renderer: Long, mode: Int): Boolean
    external fun getRenderVSync(renderer: Long): Int
    external fun setRenderVSync(renderer: Long, vsync: Int): Boolean
    external fun getRenderTarget(renderer: Long): Long
    external fun setRenderTarget(renderer: Long, texture: Long): Boolean
    external fun renderClear(renderer: Long): Boolean
    external fun renderPresent(renderer: Long)
    external fun renderFillRect(renderer: Long, x: Float, y: Float, w: Float, h: Float): Boolean
    external fun renderRect(renderer: Long, x: Float, y: Float, w: Float, h: Float): Boolean
    external fun renderLine(renderer: Long, x1: Float, y1: Float, x2: Float, y2: Float): Boolean
    external fun renderPoint(renderer: Long, x: Float, y: Float): Boolean
    external fun renderPoints(renderer: Long, points: FloatArray): Boolean
    external fun createTexture(renderer: Long, format: Int, access: Int, width: Int, height: Int): Long
    external fun createTextureFromSurface(renderer: Long, surface: Long): Long
    external fun renderTexture(renderer: Long, texture: Long, src: FloatArray?, dst: FloatArray?): Boolean
    external fun renderTextureRotated(
        renderer: Long,
        texture: Long,
        src: FloatArray?,
        dst: FloatArray?,
        angle: Double,
        center: FloatArray?,
        flip: Int,
    ): Boolean
    external fun renderTexture9Grid(
        renderer: Long,
        texture: Long,
        srcX: Float,
        srcY: Float,
        srcW: Float,
        srcH: Float,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        scale: Float,
        dstX: Float,
        dstY: Float,
        dstW: Float,
        dstH: Float,
    ): Boolean
    external fun renderGeometry(
        renderer: Long,
        texture: Long,
        positions: FloatArray,
        colors: FloatArray,
        texCoords: FloatArray,
        indices: IntArray?,
    ): Boolean
    external fun renderReadPixels(renderer: Long, rect: IntArray?): Long
    external fun setRenderLogicalPresentation(renderer: Long, width: Int, height: Int, mode: Int): Boolean
    external fun getRenderLogicalPresentationRect(renderer: Long): FloatArray?

    // =========================================================================
    // Texture
    // =========================================================================

    external fun getTextureSize(texture: Long): FloatArray
    external fun setTextureColorMod(texture: Long, r: Int, g: Int, b: Int): Boolean
    external fun setTextureAlphaMod(texture: Long, a: Int): Boolean
    external fun setTextureBlendMode(texture: Long, mode: Int): Boolean
    external fun setTextureScaleMode(texture: Long, mode: Int): Boolean
    external fun updateTexture(texture: Long, rect: IntArray?, pixels: ByteArray, pitch: Int): Boolean
    external fun lockTexture(texture: Long, rect: IntArray?, outPixels: ByteArray, outPitch: IntArray): Boolean
    external fun unlockTexture(texture: Long)
    external fun destroyTexture(texture: Long)

    // =========================================================================
    // Surface
    // =========================================================================

    external fun createSurface(width: Int, height: Int, format: Int): Long
    external fun loadBMP(path: String): Long
    external fun destroySurface(surface: Long)
    external fun getSurfaceColorspace(surface: Long): Int
    external fun lockSurface(surface: Long): Boolean
    external fun unlockSurface(surface: Long)
    external fun surfaceFillRect(surface: Long, rect: IntArray?, r: Int, g: Int, b: Int, a: Int): Boolean
    external fun surfaceFillRects(surface: Long, rects: IntArray, r: Int, g: Int, b: Int, a: Int): Boolean
    external fun surfaceBlit(src: Long, srcRect: IntArray?, dst: Long, dstRect: IntArray?): Boolean
    external fun surfaceBlitScaled(
        src: Long,
        srcRect: IntArray?,
        dst: Long,
        dstRect: IntArray?,
        scaleMode: Int,
    ): Boolean
    external fun surfaceSaveBMP(surface: Long, path: String): Boolean
    external fun convertSurface(surface: Long, format: Int): Long
    external fun surfaceWidth(surface: Long): Int
    external fun surfaceHeight(surface: Long): Int
    external fun surfaceFormat(surface: Long): Int
    external fun surfacePitch(surface: Long): Int
    external fun surfacePixels(surface: Long): ByteArray?

    // =========================================================================
    // Events
    // =========================================================================

    external fun eventAlloc(): Long
    external fun eventFree(event: Long)
    external fun pollEvent(event: Long): Boolean
    external fun waitEvent(event: Long): Boolean
    external fun pumpEvents()
    external fun pushEvent(event: Long): Boolean
    external fun eventType(event: Long): Int
    external fun eventTimestamp(event: Long): Long
    external fun eventWindowWindowID(event: Long): Int
    external fun eventWindowData1(event: Long): Int
    external fun eventWindowData2(event: Long): Int
    external fun eventKeyWindowID(event: Long): Int
    external fun eventKeyState(event: Long): Int
    external fun eventKeyRepeat(event: Long): Boolean
    external fun eventKeyScancode(event: Long): Int
    external fun eventKeyKeycode(event: Long): Int
    external fun eventKeyMod(event: Long): Int
    external fun eventTextWindowID(event: Long): Int
    external fun eventTextText(event: Long): String
    external fun eventMotionWindowID(event: Long): Int
    external fun eventMotionState(event: Long): Int
    external fun eventMotionX(event: Long): Float
    external fun eventMotionY(event: Long): Float
    external fun eventMotionXrel(event: Long): Float
    external fun eventMotionYrel(event: Long): Float
    external fun eventButtonWindowID(event: Long): Int
    external fun eventButtonButton(event: Long): Int
    external fun eventButtonState(event: Long): Int
    external fun eventButtonClicks(event: Long): Int
    external fun eventButtonX(event: Long): Float
    external fun eventButtonY(event: Long): Float
    external fun eventWheelWindowID(event: Long): Int
    external fun eventWheelX(event: Long): Float
    external fun eventWheelY(event: Long): Float
    external fun eventWheelDirection(event: Long): Int
    external fun addEventWatch(id: Long): Boolean
    external fun removeEventWatch(id: Long)
    external fun setEventEnabled(type: Int, enabled: Boolean)
    external fun eventEnabled(type: Int): Boolean
    external fun flushEvents(minType: Int, maxType: Int)

    // =========================================================================
    // Display
    // =========================================================================

    external fun getDisplays(): IntArray?
    external fun getPrimaryDisplay(): Int
    external fun getDisplayName(displayId: Int): String?
    external fun getDisplayBounds(displayId: Int): IntArray
    external fun getDisplayUsableBounds(displayId: Int): IntArray
    external fun getCurrentDisplayMode(displayId: Int): FloatArray?
    external fun getDesktopDisplayMode(displayId: Int): FloatArray?

    // =========================================================================
    // Pixels / clipboard / hints
    // =========================================================================

    external fun getPixelFormatName(format: Int): String?
    external fun mapRGB(format: Int, r: Int, g: Int, b: Int): Int
    external fun mapRGBA(format: Int, r: Int, g: Int, b: Int, a: Int): Int
    external fun getRGBA(format: Int, pixel: Int): IntArray?
    external fun setHint(name: String, value: String): Boolean
    external fun getHint(name: String): String?
    external fun getHintBoolean(name: String, defaultValue: Boolean): Boolean
    external fun getClipboardText(): String?
    external fun setClipboardText(text: String): Boolean
    external fun hasClipboardText(): Boolean

    // =========================================================================
    // Drivers
    // =========================================================================

    external fun getNumVideoDrivers(): Int
    external fun getVideoDriver(index: Int): String?
    external fun getCurrentVideoDriver(): String?
    external fun getNumAudioDrivers(): Int
    external fun getAudioDriver(index: Int): String?
    external fun getCurrentAudioDriver(): String?
    external fun getNumRenderDrivers(): Int
    external fun getRenderDriver(index: Int): String?

    // =========================================================================
    // Message boxes / dialogs / logging
    // =========================================================================

    external fun showSimpleMessageBox(title: String, message: String): Boolean
    external fun showMessageBox(
        flags: Int,
        title: String,
        message: String,
        buttonFlags: IntArray,
        buttonIds: IntArray,
        buttonTexts: Array<String>,
    ): Int
    external fun showOpenFileDialog(
        id: Long,
        window: Long,
        filterNames: Array<String>?,
        filterPatterns: Array<String>?,
        defaultLocation: String?,
        allowMultiple: Boolean,
    )
    external fun showSaveFileDialog(
        id: Long,
        window: Long,
        filterNames: Array<String>?,
        filterPatterns: Array<String>?,
        defaultLocation: String?,
    )
    external fun showOpenFolderDialog(
        id: Long,
        window: Long,
        defaultLocation: String?,
        allowMultiple: Boolean,
    )
    external fun logMessage(category: Int, priority: Int, message: String)
    external fun setLogPriority(category: Int, priority: Int)
    external fun getLogPriority(category: Int): Int
    external fun setLogPriorities(priority: Int)
    external fun resetLogPriorities()
    external fun setLogOutputFunction(id: Long)
    external fun setLogOutputFunctionNull()

    // =========================================================================
    // Filesystem / power / misc
    // =========================================================================

    external fun basePath(): String?
    external fun getPrefPath(orgName: String, appName: String): String?
    external fun getUserFolder(folder: Int): String?
    external fun createDirectory(path: String): Boolean
    external fun removePath(path: String): Boolean
    external fun renamePath(oldPath: String, newPath: String): Boolean
    external fun powerInfo(): IntArray
    external fun openURL(url: String): Boolean

    // =========================================================================
    // Keyboard
    // =========================================================================

    external fun keyboardState(): ByteArray?
    external fun modState(): Int
    external fun setModState(modState: Int)
    external fun getKeyFromScancode(scancode: Int): Int
    external fun getScancodeFromKey(keycode: Int): Int
    external fun getKeyName(keycode: Int): String?
    external fun getScancodeName(scancode: Int): String?
    external fun textInputActive(window: Long): Boolean
    external fun startTextInput(window: Long): Boolean
    external fun stopTextInput(window: Long): Boolean
    external fun getKeyboardFocus(): Long
    external fun getMouseFocus(): Long

    // =========================================================================
    // Mouse
    // =========================================================================

    external fun mouseState(): FloatArray
    external fun globalMouseState(): FloatArray
    external fun warpMouseInWindow(window: Long, x: Float, y: Float)
    external fun captureMouse(enabled: Boolean): Boolean
    external fun showCursor(): Boolean

    // =========================================================================
    // Touch
    // =========================================================================

    external fun getTouchDevices(): IntArray?
    external fun getTouchDeviceName(touchId: Int): String?
    external fun getTouchDeviceType(touchId: Int): Int
    external fun getTouchFingers(touchId: Int): LongArray?

    // =========================================================================
    // Joystick
    // =========================================================================

    external fun getJoysticks(): IntArray?
    external fun openJoystick(id: Int): Long
    external fun closeJoystick(joystick: Long)
    external fun joystickId(joystick: Long): Int
    external fun joystickName(joystick: Long): String?
    external fun joystickType(joystick: Long): Int
    external fun joystickNumAxes(joystick: Long): Int
    external fun joystickNumBalls(joystick: Long): Int
    external fun joystickNumHats(joystick: Long): Int
    external fun joystickNumButtons(joystick: Long): Int
    external fun joystickPlayerIndex(joystick: Long): Int
    external fun joystickFirmwareVersion(joystick: Long): Int
    external fun joystickAxis(joystick: Long, axis: Int): Short
    external fun joystickButton(joystick: Long, button: Int): Boolean
    external fun joystickHat(joystick: Long, hat: Int): Int
    external fun joystickBall(joystick: Long, ball: Int): IntArray?
    external fun joystickRumble(joystick: Long, lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean

    // =========================================================================
    // Gamepad
    // =========================================================================

    external fun getGamepads(): IntArray?
    external fun openGamepad(id: Int): Long
    external fun closeGamepad(gamepad: Long)
    external fun gamepadId(gamepad: Long): Int
    external fun gamepadName(gamepad: Long): String?
    external fun gamepadVendor(gamepad: Long): Int
    external fun gamepadProduct(gamepad: Long): Int
    external fun gamepadSerial(gamepad: Long): String?
    external fun gamepadConnected(gamepad: Long): Boolean
    external fun gamepadPlayerIndex(gamepad: Long): Int
    external fun gamepadFirmwareVersion(gamepad: Long): Int
    external fun gamepadNumTouchpads(gamepad: Long): Int
    external fun gamepadTouchpadFinger(gamepad: Long, touchpad: Int, finger: Int): FloatArray?
    external fun gamepadHasSensor(gamepad: Long, type: Int): Boolean
    external fun gamepadSensorData(gamepad: Long, type: Int): FloatArray?
    external fun gamepadSensorDataRate(gamepad: Long, type: Int): Float
    external fun gamepadButton(gamepad: Long, button: Int): Boolean
    external fun gamepadAxis(gamepad: Long, axis: Int): Short
    external fun gamepadRumble(gamepad: Long, lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean

    // =========================================================================
    // Audio
    // =========================================================================

    external fun audioPlaybackDevices(): IntArray?
    external fun audioRecordingDevices(): IntArray?
    external fun getAudioDeviceName(deviceId: Int): String?
    external fun openAudioDevice(deviceId: Int, format: Int, channels: Int, freq: Int): Int
    external fun openAudioDeviceStream(deviceId: Int, format: Int, channels: Int, freq: Int): Long
    external fun createAudioStream(
        srcFormat: Int,
        srcChannels: Int,
        srcFreq: Int,
        dstFormat: Int,
        dstChannels: Int,
        dstFreq: Int,
    ): Long
    external fun pauseAudioDevice(deviceId: Int)
    external fun resumeAudioDevice(deviceId: Int)
    external fun audioDevicePaused(deviceId: Int): Boolean
    external fun loadWav(path: String, outSpec: IntArray): ByteArray?
    external fun getAudioDeviceFormat(deviceId: Int): IntArray?
    external fun bindAudioStream(deviceId: Int, stream: Long): Boolean
    external fun unbindAudioStream(deviceId: Int, stream: Long)
    external fun closeAudioDevice(deviceId: Int)
    external fun putAudioStreamData(stream: Long, data: ByteArray): Boolean
    external fun getAudioStreamData(stream: Long, maxLen: Int): ByteArray?
    external fun getAudioStreamAvailable(stream: Long): Int
    external fun getAudioStreamQueued(stream: Long): Int
    external fun getAudioStreamFormat(stream: Long): IntArray?
    external fun setAudioStreamFormat(
        stream: Long,
        srcFormat: Int,
        srcChannels: Int,
        srcFreq: Int,
        dstFormat: Int,
        dstChannels: Int,
        dstFreq: Int,
    ): Boolean
    external fun getAudioStreamGain(stream: Long): Float
    external fun setAudioStreamGain(stream: Long, gain: Float): Boolean
    external fun getAudioStreamFrequencyRatio(stream: Long): Float
    external fun setAudioStreamFrequencyRatio(stream: Long, ratio: Float): Boolean
    external fun audioStreamDevicePaused(stream: Long): Boolean
    external fun pauseAudioStreamDevice(stream: Long)
    external fun resumeAudioStreamDevice(stream: Long)
    external fun flushAudioStream(stream: Long): Boolean
    external fun clearAudioStream(stream: Long): Boolean
    external fun destroyAudioStream(stream: Long)

    // =========================================================================
    // OpenGL
    // =========================================================================

    external fun glLoadLibrary(path: String?): Boolean
    external fun glUnloadLibrary()
    external fun glGetProcAddress(proc: String): Long
    external fun glExtensionSupported(extension: String): Boolean
    external fun glResetAttributes()
    external fun glSetAttribute(attr: Int, value: Int): Boolean
    external fun glGetAttribute(attr: Int): IntArray?
    external fun glCreateContext(window: Long): Long
    external fun glMakeCurrent(window: Long, context: Long): Boolean
    external fun glGetCurrentWindow(): Long
    external fun glGetCurrentContext(): Long
    external fun glSetSwapInterval(interval: Int): Boolean
    external fun glGetSwapInterval(): IntArray?
    external fun glSwapWindow(window: Long): Boolean
    external fun glDestroyContext(context: Long)

    // =========================================================================
    // Vulkan
    // =========================================================================

    external fun vulkanLoadLibrary(path: String?): Boolean
    external fun vulkanUnloadLibrary()
    external fun vulkanGetVkGetInstanceProcAddr(): Long
    external fun vulkanGetInstanceExtensions(): Array<String>?
    external fun vulkanCreateSurface(window: Long, instance: Long): Long
    external fun vulkanDestroySurface(instance: Long, surface: Long)
    external fun vulkanGetPresentationSupport(instance: Long, physicalDevice: Long, queueFamilyIndex: Int): Boolean

    // =========================================================================
    // GPU
    // =========================================================================

    external fun gpuIsSupported(formats: Int): Boolean
    external fun gpuGetNumDrivers(): Int
    external fun gpuGetDriver(index: Int): String?
    external fun gpuCreateDevice(formats: Int, debugMode: Boolean): Long
    external fun gpuDestroyDevice(device: Long)
    external fun gpuGetShaderFormats(device: Long): Int
    external fun gpuClaimWindow(device: Long, window: Long): Boolean
    external fun gpuReleaseWindow(device: Long, window: Long)
    external fun gpuGetSwapchainTextureFormat(device: Long, window: Long): Int
    external fun gpuAcquireCommandBuffer(device: Long): Long
    external fun gpuSubmitCommandBuffer(commandBuffer: Long): Boolean
    external fun gpuSubmitCommandBufferAndAcquireFence(commandBuffer: Long): Long
    external fun gpuCancelCommandBuffer(commandBuffer: Long)
    external fun gpuWaitForGPUIdle(device: Long): Boolean
    external fun gpuBeginRenderPass(
        commandBuffer: Long,
        textures: LongArray,
        mipLevels: IntArray,
        layers: IntArray,
        loadOps: IntArray,
        storeOps: IntArray,
        clearColors: FloatArray,
        clearColorEnabled: BooleanArray?,
    ): Long
    external fun gpuEndRenderPass(renderPass: Long)
    external fun gpuBindGraphicsPipeline(renderPass: Long, pipeline: Long)
    external fun gpuSetViewport(renderPass: Long, x: Float, y: Float, w: Float, h: Float, minDepth: Float, maxDepth: Float)
    external fun gpuSetScissor(renderPass: Long, x: Int, y: Int, w: Int, h: Int)
    external fun gpuBindVertexBuffers(renderPass: Long, buffers: LongArray, offsets: IntArray)
    external fun gpuBindIndexBuffer(renderPass: Long, buffer: Long, indexElementSize: Int)
    external fun gpuBindFragmentSamplers(renderPass: Long, slot: Int, textures: LongArray, samplers: LongArray)
    external fun gpuPushVertexUniformData(commandBuffer: Long, slot: Int, data: ByteArray)
    external fun gpuPushFragmentUniformData(commandBuffer: Long, slot: Int, data: ByteArray)
    external fun gpuDrawPrimitives(renderPass: Long, vertexCount: Int, instanceCount: Int, firstVertex: Int, firstInstance: Int)
    external fun gpuDrawIndexedPrimitives(renderPass: Long, indexCount: Int, instanceCount: Int, firstIndex: Int, vertexOffset: Int, firstInstance: Int)
    external fun gpuCreateTexture(
        device: Long,
        type: Int,
        format: Int,
        usage: Int,
        width: Int,
        height: Int,
        layerCountOrDepth: Int,
        numLevels: Int,
        sampleCount: Int,
    ): Long
    external fun gpuCreateBuffer(device: Long, usage: Int, size: Int): Long
    external fun gpuCreateShader(
        device: Long,
        code: ByteArray,
        format: Int,
        stage: Int,
        entryPoint: String,
        numSamplers: Int,
        numStorageTextures: Int,
        numStorageBuffers: Int,
        numUniformBuffers: Int,
    ): Long
    external fun gpuCreateGraphicsPipeline(
        device: Long,
        vertexShader: Long,
        fragmentShader: Long,
        primitiveType: Int,
        vertexBufferDescriptions: IntArray,
        vertexAttributes: IntArray,
        fillMode: Int,
        cullMode: Int,
        frontFace: Int,
        compareOp: Int,
        enableDepthTest: Boolean,
        enableDepthWrite: Boolean,
        targetFormats: IntArray,
        blendStates: IntArray,
    ): Long
    external fun gpuCreateSampler(
        device: Long,
        minFilter: Int,
        magFilter: Int,
        mipmapMode: Int,
        addressModeU: Int,
        addressModeV: Int,
        addressModeW: Int,
        maxAnisotropy: Float,
    ): Long
    external fun gpuReleaseShader(device: Long, shader: Long)
    external fun gpuReleaseGraphicsPipeline(device: Long, pipeline: Long)
    external fun gpuReleaseTexture(device: Long, texture: Long)
    external fun gpuReleaseBuffer(device: Long, buffer: Long)
    external fun gpuReleaseSampler(device: Long, sampler: Long)
    external fun gpuReleaseFence(device: Long, fence: Long)
    external fun gpuQueryFence(device: Long, fence: Long): Boolean
    external fun gpuWaitForFences(device: Long, fences: LongArray): Boolean
    external fun gpuUploadToBuffer(device: Long, buffer: Long, data: ByteArray, offset: Int): Boolean
    external fun gpuUploadToBufferInCmd(device: Long, commandBuffer: Long, buffer: Long, data: ByteArray, offset: Int): Boolean
    external fun gpuUploadToTexture(device: Long, texture: Long, data: ByteArray, bytesPerRow: Int, x: Int, y: Int, w: Int, h: Int): Boolean
    external fun gpuDownloadFromTexture(device: Long, texture: Long, w: Int, h: Int): ByteArray?
    external fun gpuWaitAndAcquireSwapchainTexture(commandBuffer: Long, window: Long): LongArray?
    external fun gpuWaitForGPUSwapchain(device: Long, window: Long): Boolean

    // =========================================================================
    // IO
    // =========================================================================

    external fun ioFromFile(path: String, mode: String): Long
    external fun ioFromMem(data: ByteArray): Long
    external fun ioFromConstMem(data: ByteArray): Long
    external fun ioRead(stream: Long, size: Int): ByteArray?
    external fun ioWrite(stream: Long, data: ByteArray): Int
    external fun ioSeek(stream: Long, offset: Int, whence: Int): Int
    external fun ioTell(stream: Long): Int
    external fun ioSize(stream: Long): Int
    external fun ioFlush(stream: Long): Boolean
    external fun ioClose(stream: Long)
    external fun loadFile(path: String): ByteArray?
    external fun loadFileIO(stream: Long): ByteArray?

    // =========================================================================
    // Properties
    // =========================================================================

    external fun propertiesCreate(): Int
    external fun setPointerProperty(props: Int, name: String, value: Long): Boolean
    external fun setStringProperty(props: Int, name: String, value: String?): Boolean
    external fun getPointerProperty(props: Int, name: String, defaultValue: Long): Long
    external fun getStringProperty(props: Int, name: String): String?
    external fun hasProperty(props: Int, name: String): Boolean
    external fun clearProperty(props: Int, name: String): Boolean
    external fun copyProperties(src: Int, dst: Int): Boolean
    external fun globalProperties(): Int
    external fun destroyProperties(props: Int)

    // =========================================================================
    // Camera
    // =========================================================================

    external fun getCameras(): IntArray?
    external fun getCameraName(deviceId: Int): String?
    external fun getCameraPosition(deviceId: Int): Int
    external fun getCameraSupportedFormats(deviceId: Int): IntArray?
    external fun openCamera(deviceId: Int, format: Int, width: Int, height: Int, framerate: Int): Long
    external fun getCameraFormat(camera: Long): IntArray?
    external fun getCameraPermissionState(camera: Long): Int
    external fun acquireCameraFrame(camera: Long): Long
    external fun releaseCameraFrame(camera: Long, frame: Long)
    external fun closeCamera(camera: Long)

    // =========================================================================
    // Sensor
    // =========================================================================

    external fun getSensors(): IntArray?
    external fun getSensorNameForID(deviceId: Int): String?
    external fun getSensorTypeForID(deviceId: Int): Int
    external fun openSensor(deviceId: Int): Long
    external fun getSensorID(sensor: Long): Int
    external fun getSensorName(sensor: Long): String?
    external fun getSensorType(sensor: Long): Int
    external fun getSensorData(sensor: Long): FloatArray?
    external fun closeSensor(sensor: Long)

    // =========================================================================
    // Haptic
    // =========================================================================

    external fun getHaptics(): IntArray?
    external fun getHapticNameForID(deviceId: Int): String?
    external fun openHaptic(deviceId: Int): Long
    external fun getHapticName(haptic: Long): String?
    external fun getNumHapticAxes(haptic: Long): Int
    external fun hapticRumble(haptic: Long, lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean
    external fun stopHapticEffect(haptic: Long, effectId: Int): Boolean
    external fun hapticEffectStatus(haptic: Long, effectId: Int): Boolean
    external fun destroyHapticEffect(haptic: Long, effectId: Int)
    external fun closeHaptic(haptic: Long)
}
