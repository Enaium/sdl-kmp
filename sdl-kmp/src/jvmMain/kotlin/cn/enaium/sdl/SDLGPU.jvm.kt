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
 * Bytes per texel for a [SDLGPUTextureFormat] value (0 for compressed/unknown).
 *
 * Stored on [JvmSDLGPUTexture] so [upload] can convert SDL3's
 * `pixels_per_row` (which counts *pixels*, not bytes) from the caller's
 * byte-based row stride.
 */
private fun bytesPerPixelOf(format: Int): Int = when (format) {
    SDLGPUTextureFormat.A8_UNORM, SDLGPUTextureFormat.R8_UNORM -> 1
    SDLGPUTextureFormat.R8G8_UNORM -> 2
    SDLGPUTextureFormat.R8G8B8A8_UNORM, SDLGPUTextureFormat.B8G8R8A8_UNORM,
    SDLGPUTextureFormat.B4G4R4A4_UNORM, SDLGPUTextureFormat.B5G5R5A1_UNORM,
    SDLGPUTextureFormat.R10G10B10A2_UNORM -> 4
    SDLGPUTextureFormat.R16_UNORM -> 2
    SDLGPUTextureFormat.R16G16_UNORM -> 4
    SDLGPUTextureFormat.R16G16B16A16_UNORM -> 8
    else -> 0
}

// =========================================================================
// JVM (JNI) GPU handles
// =========================================================================

internal class JvmSDLGPUShader internal constructor(ptr: Long, private val device: JvmSDLGPUDevice) : SDLGPUShader {

    private var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue

    override fun close() {
        val shader = ptrValue
        if (shader == 0L) return
        ptrValue = 0L
        Jni.gpuReleaseShader(device.check(), shader)
    }
}

internal class JvmSDLGPUGraphicsPipeline internal constructor(ptr: Long, private val device: JvmSDLGPUDevice) : SDLGPUGraphicsPipeline {

    private var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue

    override fun close() {
        val pipeline = ptrValue
        if (pipeline == 0L) return
        ptrValue = 0L
        Jni.gpuReleaseGraphicsPipeline(device.check(), pipeline)
    }
}

internal class JvmSDLGPUTexture internal constructor(
    ptr: Long,
    private val device: JvmSDLGPUDevice,
    internal val bytesPerPixel: Int,
) : SDLGPUTexture {

    private var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue

    override fun upload(data: ByteArray, bytesPerRow: Int, x: Int, y: Int, width: Int, height: Int): Boolean =
        device.uploadToTexture(this, data, bytesPerRow, x, y, width, height)

    override fun download(width: Int, height: Int): ByteArray? =
        device.downloadFromTexture(this, width, height)

    override fun close() {
        val texture = ptrValue
        if (texture == 0L) return
        ptrValue = 0L
        Jni.gpuReleaseTexture(device.check(), texture)
    }
}

internal class JvmSDLGPUBuffer internal constructor(
    ptr: Long,
    private val device: JvmSDLGPUDevice,
    private val bufferSize: Int,
) : SDLGPUBuffer {

    private var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue

    override val size: Int
        get() = bufferSize

    override fun setData(data: ByteArray, offset: Int): Boolean =
        device.uploadToBuffer(this, data, offset)

    override fun close() {
        val buffer = ptrValue
        if (buffer == 0L) return
        ptrValue = 0L
        Jni.gpuReleaseBuffer(device.check(), buffer)
    }
}

internal class JvmSDLGPUSampler internal constructor(ptr: Long, private val device: JvmSDLGPUDevice) : SDLGPUSampler {

    private var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue

    override fun close() {
        val sampler = ptrValue
        if (sampler == 0L) return
        ptrValue = 0L
        Jni.gpuReleaseSampler(device.check(), sampler)
    }
}

internal class JvmSDLGPUFence internal constructor(ptr: Long, private val device: JvmSDLGPUDevice) : SDLGPUFence {

    private var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue

    override val signaled: Boolean
        get() = ptrValue.let { it != 0L && Jni.gpuQueryFence(device.check(), it) }

    override fun wait(timeoutMs: Int): Boolean =
        Jni.gpuWaitForFences(device.check(), longArrayOf(ptrValue))

    override fun close() {
        val fence = ptrValue
        if (fence == 0L) return
        ptrValue = 0L
        Jni.gpuReleaseFence(device.check(), fence)
    }
}

internal class JvmSDLGPURenderPass internal constructor(ptr: Long) : SDLGPURenderPass {

    private var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue

    private fun check(): Long = ptrValue.also {
        if (it == 0L) throw IllegalStateException("SDL GPU render pass is closed")
    }

    override fun bindGraphicsPipeline(pipeline: SDLGPUGraphicsPipeline) {
        val p = (pipeline as? JvmSDLGPUGraphicsPipeline)?.ptr
            ?: throw IllegalArgumentException("pipeline is not a JVM SDL GPU pipeline")
        Jni.gpuBindGraphicsPipeline(check(), p)
    }

    override fun setViewport(viewport: SDLGPUViewport) {
        Jni.gpuSetViewport(check(), viewport.x, viewport.y, viewport.width, viewport.height, viewport.minDepth, viewport.maxDepth)
    }

    override fun setScissor(x: Int, y: Int, width: Int, height: Int) {
        Jni.gpuSetScissor(check(), x, y, width, height)
    }

    override fun bindVertexBuffers(vararg buffers: Pair<SDLGPUBuffer, Int>) {
        if (buffers.isEmpty()) return
        val ptrs = LongArray(buffers.size) { i ->
            (buffers[i].first as? JvmSDLGPUBuffer)?.ptr
                ?: throw IllegalArgumentException("buffer is not a JVM SDL GPU buffer")
        }
        val offsets = IntArray(buffers.size) { buffers[it].second }
        Jni.gpuBindVertexBuffers(check(), ptrs, offsets)
    }

    override fun bindIndexBuffer(buffer: SDLGPUBuffer, indexSize: Int) {
        val b = buffer as? JvmSDLGPUBuffer
            ?: throw IllegalArgumentException("buffer is not a JVM SDL GPU buffer")
        Jni.gpuBindIndexBuffer(check(), b.ptr, indexSize)
    }

    override fun bindGraphicsSamplers(slot: Int, vararg samplers: SDLGPUSampler) {
        if (samplers.isEmpty()) return
        val ptrs = LongArray(samplers.size) { i ->
            (samplers[i] as? JvmSDLGPUSampler)?.ptr
                ?: throw IllegalArgumentException("sampler is not a JVM SDL GPU sampler")
        }
        Jni.gpuBindFragmentSamplers(check(), slot, LongArray(ptrs.size), ptrs)
    }

    override fun bindGraphicsTextures(slot: Int, vararg textures: SDLGPUTexture) {
        if (textures.isEmpty()) return
        val ptrs = LongArray(textures.size) { i ->
            (textures[i] as? JvmSDLGPUTexture)?.ptr
                ?: throw IllegalArgumentException("texture is not a JVM SDL GPU texture")
        }
        Jni.gpuBindFragmentSamplers(check(), slot, ptrs, LongArray(ptrs.size))
    }

    override fun bindGraphicsTextureSamplers(slot: Int, vararg bindings: Pair<SDLGPUTexture, SDLGPUSampler>) {
        if (bindings.isEmpty()) return
        val textures = LongArray(bindings.size) { i ->
            (bindings[i].first as? JvmSDLGPUTexture)?.ptr
                ?: throw IllegalArgumentException("texture is not a JVM SDL GPU texture")
        }
        val samplers = LongArray(bindings.size) { i ->
            (bindings[i].second as? JvmSDLGPUSampler)?.ptr
                ?: throw IllegalArgumentException("sampler is not a JVM SDL GPU sampler")
        }
        Jni.gpuBindFragmentSamplers(check(), slot, textures, samplers)
    }

    override fun pushVertexUniformData(slot: Int, data: ByteArray) {
        Jni.gpuPushVertexUniformData(check(), slot, data)
    }

    override fun drawPrimitives(vertexCount: Int, instanceCount: Int, firstVertex: Int, firstInstance: Int) {
        Jni.gpuDrawPrimitives(check(), vertexCount, instanceCount, firstVertex, firstInstance)
    }

    override fun drawIndexedPrimitives(indexCount: Int, instanceCount: Int, firstIndex: Int, vertexOffset: Int, firstInstance: Int) {
        Jni.gpuDrawIndexedPrimitives(check(), indexCount, instanceCount, firstIndex, vertexOffset, firstInstance)
    }

    override fun end() {
        val pass = ptrValue
        if (pass == 0L) return
        ptrValue = 0L
        Jni.gpuEndRenderPass(pass)
    }

    override fun close() = end()
}

internal class JvmSDLGPUCommandBuffer internal constructor(ptr: Long, private val device: JvmSDLGPUDevice) : SDLGPUCommandBuffer {

    internal var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue
    internal fun check(): Long = ptrValue.also {
        if (it == 0L) throw IllegalStateException("SDL GPU command buffer is closed")
    }

    override fun beginRenderPass(colorTargets: List<SDLGPUColorTargetInfo>): SDLGPURenderPass? {
        if (colorTargets.isEmpty()) return null
        val textures = LongArray(colorTargets.size) { i ->
            (colorTargets[i].texture as? JvmSDLGPUTexture)?.ptr
                ?: throw IllegalArgumentException("color target texture is not a JVM SDL GPU texture")
        }
        val mipLevels = IntArray(colorTargets.size) { colorTargets[it].mipLevel }
        val layers = IntArray(colorTargets.size) { colorTargets[it].layerOrDepthPlane }
        val loadOps = IntArray(colorTargets.size) { colorTargets[it].loadOp }
        val storeOps = IntArray(colorTargets.size) { colorTargets[it].storeOp }
        val clearColors = FloatArray(colorTargets.size * 4)
        val clearEnabled = BooleanArray(colorTargets.size)
        for (i in colorTargets.indices) {
            val clear = colorTargets[i].clearColor
            clearColors[i * 4] = clear.r / 255f
            clearColors[i * 4 + 1] = clear.g / 255f
            clearColors[i * 4 + 2] = clear.b / 255f
            clearColors[i * 4 + 3] = clear.a / 255f
            clearEnabled[i] = true
        }
        val pass = Jni.gpuBeginRenderPass(check(), textures, mipLevels, layers, loadOps, storeOps, clearColors, clearEnabled)
        if (pass == 0L) return null
        return JvmSDLGPURenderPass(pass)
    }

    override fun pushVertexUniformData(slot: Int, data: ByteArray) {
        Jni.gpuPushVertexUniformData(check(), slot, data)
    }

    override fun pushFragmentUniformData(slot: Int, data: ByteArray) {
        Jni.gpuPushFragmentUniformData(check(), slot, data)
    }

    override fun uploadToBuffer(buffer: SDLGPUBuffer, data: ByteArray, offset: Int): Boolean {
        val b = (buffer as? JvmSDLGPUBuffer)?.ptr
            ?: throw IllegalArgumentException("buffer is not a JVM SDL GPU buffer")
        if (offset + data.size > buffer.size) return false
        return Jni.gpuUploadToBufferInCmd(device.check(), check(), b, data, offset)
    }

    override fun end() {
        if (ptrValue == 0L) throw IllegalStateException("SDL GPU command buffer is closed")
    }

    override fun close() {
        val cmd = ptrValue
        if (cmd == 0L) return
        ptrValue = 0L
        Jni.gpuCancelCommandBuffer(cmd)
    }
}

internal class JvmSDLGPUDevice internal constructor(ptr: Long) : SDLGPUDevice {

    private var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue

    internal fun check(): Long = ptrValue.also {
        if (it == 0L) throw IllegalStateException("SDL GPU device is closed")
    }

    override val shaderFormats: Int
        get() = Jni.gpuGetShaderFormats(check())

    override fun claimWindow(window: SDLWindow): Boolean {
        val w = (window as? JvmSDLWindow)?.ptr ?: throw IllegalArgumentException("window is not a JVM SDL window")
        return Jni.gpuClaimWindow(check(), w)
    }

    override fun releaseDrawable(window: SDLWindow) {
        val w = (window as? JvmSDLWindow)?.ptr ?: throw IllegalArgumentException("window is not a JVM SDL window")
        Jni.gpuReleaseWindow(check(), w)
    }

    override fun getWindowFormat(window: SDLWindow): Int? {
        val w = (window as? JvmSDLWindow)?.ptr ?: throw IllegalArgumentException("window is not a JVM SDL window")
        val format = Jni.gpuGetSwapchainTextureFormat(check(), w)
        return if (format == 0) null else format
    }

    override fun acquireSwapchainTexture(commandBuffer: SDLGPUCommandBuffer, window: SDLWindow): SDLGPUWindowTexture? {
        val cmd = (commandBuffer as? JvmSDLGPUCommandBuffer)?.ptrValue
            ?: throw IllegalArgumentException("command buffer is not a JVM SDL GPU command buffer")
        val w = (window as? JvmSDLWindow)?.ptr ?: throw IllegalArgumentException("window is not a JVM SDL window")
        val result = Jni.gpuWaitAndAcquireSwapchainTexture(cmd, w) ?: return null
        return SDLGPUWindowTexture(
            texture = result[0].takeIf { it != 0L }?.let { JvmSDLGPUTexture(it, this@JvmSDLGPUDevice, 0) },
            srcRect = SDLRect(0, 0, result[1].toInt(), result[2].toInt()),
        )
    }

    override fun acquireSwapchainTexture(window: SDLWindow): SDLGPUWindowTexture? {
        val w = (window as? JvmSDLWindow)?.ptr ?: throw IllegalArgumentException("window is not a JVM SDL window")
        val cmd = Jni.gpuAcquireCommandBuffer(check())
        if (cmd == 0L) return null
        val result = Jni.gpuWaitAndAcquireSwapchainTexture(cmd, w)
        // Note: the texture is only usable through the command buffer it was
        // acquired on; like the native backend, the buffer is left unsubmitted
        // (submitting it here would present an empty frame).
        if (result == null) return null
        return SDLGPUWindowTexture(
            texture = result[0].takeIf { it != 0L }?.let { JvmSDLGPUTexture(it, this@JvmSDLGPUDevice, 0) },
            srcRect = SDLRect(0, 0, result[1].toInt(), result[2].toInt()),
        )
    }

    override fun createTexture(createInfo: SDLGPUTextureCreateInfo): SDLGPUTexture? {
        val texture = Jni.gpuCreateTexture(
            check(),
            createInfo.type,
            createInfo.format,
            createInfo.usage,
            createInfo.width,
            createInfo.height,
            createInfo.layerCountOrDepth,
            createInfo.numLevels,
            createInfo.sampleCount,
        )
        if (texture == 0L) {
            return null
        } else {
            return JvmSDLGPUTexture(texture, this@JvmSDLGPUDevice, bytesPerPixelOf(createInfo.format))
        }
    }

    override fun createBuffer(createInfo: SDLGPUBufferCreateInfo): SDLGPUBuffer? {
        val buffer = Jni.gpuCreateBuffer(check(), createInfo.usage, createInfo.size)
        if (buffer == 0L) {
            return null
        } else {
            return JvmSDLGPUBuffer(buffer, this@JvmSDLGPUDevice, createInfo.size)
        }
    }

    override fun createShader(
        code: ByteArray,
        format: Int,
        stage: Int,
        entryPoint: String,
        numSamplers: Int,
        numStorageTextures: Int,
        numStorageBuffers: Int,
        numUniformBuffers: Int,
    ): SDLGPUShader? {
        val shader = Jni.gpuCreateShader(
            check(),
            code,
            format,
            stage,
            entryPoint,
            numSamplers,
            numStorageTextures,
            numStorageBuffers,
            numUniformBuffers,
        )
        return if (shader == 0L) null else JvmSDLGPUShader(shader, this@JvmSDLGPUDevice)
    }

    override fun createGraphicsPipeline(createInfo: SDLGPUGraphicsPipelineCreateInfo): SDLGPUGraphicsPipeline? {
        val vertex = createInfo.vertexShader as? JvmSDLGPUShader
            ?: throw IllegalArgumentException("vertex shader is not a JVM SDL GPU shader")
        val fragment = createInfo.fragmentShader as? JvmSDLGPUShader
            ?: throw IllegalArgumentException("fragment shader is not a JVM SDL GPU shader")

        val buffers = createInfo.vertexInputState.vertexBufferDescriptions
        val attributes = createInfo.vertexInputState.vertexAttributes
        val vbDesc = IntArray(buffers.size * 4)
        for (i in buffers.indices) {
            vbDesc[i * 4] = buffers[i].slot
            vbDesc[i * 4 + 1] = buffers[i].pitch
            vbDesc[i * 4 + 2] = buffers[i].inputRate
            vbDesc[i * 4 + 3] = buffers[i].instanceStepRate
        }
        val vaDesc = IntArray(attributes.size * 4)
        for (i in attributes.indices) {
            vaDesc[i * 4] = attributes[i].location
            vaDesc[i * 4 + 1] = attributes[i].bufferSlot
            vaDesc[i * 4 + 2] = attributes[i].format
            vaDesc[i * 4 + 3] = attributes[i].offset
        }

        val targets = createInfo.targetDescriptions
        val targetFormats = IntArray(targets.size) { targets[it].format }
        val blendStates = IntArray(targets.size * 7)
        for (i in targets.indices) {
            val blend = targets[i].blendState
            blendStates[i * 7] = blend.srcColorBlendFactor
            blendStates[i * 7 + 1] = blend.dstColorBlendFactor
            blendStates[i * 7 + 2] = blend.colorBlendOp
            blendStates[i * 7 + 3] = blend.srcAlphaBlendFactor
            blendStates[i * 7 + 4] = blend.dstAlphaBlendFactor
            blendStates[i * 7 + 5] = blend.alphaBlendOp
            blendStates[i * 7 + 6] = blend.colorWriteMask
        }

        val rs = createInfo.rasterizerState
        val ds = createInfo.depthStencilState
        val pipeline = Jni.gpuCreateGraphicsPipeline(
            check(),
            vertex.ptr,
            fragment.ptr,
            createInfo.primitiveType,
            vbDesc,
            vaDesc,
            rs.fillMode,
            rs.cullMode,
            rs.frontFace,
            ds.compareOp,
            ds.enableDepthTest,
            ds.enableDepthWrite,
            targetFormats,
            blendStates,
        )
        if (pipeline == 0L) return null
        return JvmSDLGPUGraphicsPipeline(pipeline, this@JvmSDLGPUDevice)
    }

    override fun createSampler(createInfo: SDLGPUSamplerCreateInfo): SDLGPUSampler? {
        val sampler = Jni.gpuCreateSampler(
            check(),
            createInfo.minFilter,
            createInfo.magFilter,
            createInfo.mipmapMode,
            createInfo.addressModeU,
            createInfo.addressModeV,
            createInfo.addressModeW,
            createInfo.maxAnisotropy,
        )
        if (sampler == 0L) return null
        return JvmSDLGPUSampler(sampler, this@JvmSDLGPUDevice)
    }

    override fun beginCommandBuffer(): SDLGPUCommandBuffer? {
        val cmd = Jni.gpuAcquireCommandBuffer(check())
        if (cmd == 0L) return null
        return JvmSDLGPUCommandBuffer(cmd, this)
    }

    override fun submit(commandBuffer: SDLGPUCommandBuffer): Boolean {
        val native = commandBuffer as? JvmSDLGPUCommandBuffer
            ?: throw IllegalArgumentException("command buffer is not a JVM SDL GPU command buffer")
        val cmd = native.check()
        val ok = Jni.gpuSubmitCommandBuffer(cmd)
        if (ok) {
            native.ptrValue = 0L
        }
        return ok
    }

    override fun submitAndAcquireFence(commandBuffer: SDLGPUCommandBuffer): SDLGPUFence? {
        val native = commandBuffer as? JvmSDLGPUCommandBuffer
            ?: throw IllegalArgumentException("command buffer is not a JVM SDL GPU command buffer")
        val cmd = native.check()
        val fence = Jni.gpuSubmitCommandBufferAndAcquireFence(cmd)
        if (fence == 0L) return null
        native.ptrValue = 0L
        return JvmSDLGPUFence(fence, this)
    }

    override fun waitForFences(fences: List<SDLGPUFence>): Boolean {
        if (fences.isEmpty()) return true
        val ptrs = LongArray(fences.size) { i ->
            (fences[i] as? JvmSDLGPUFence)?.ptr
                ?: throw IllegalArgumentException("fence is not a JVM SDL GPU fence")
        }
        return Jni.gpuWaitForFences(check(), ptrs)
    }

    override fun present(window: SDLWindow) {
        val w = (window as? JvmSDLWindow)?.ptr ?: throw IllegalArgumentException("window is not a JVM SDL window")
        // The frame is presented by SDL_SubmitGPUCommandBuffer (the swapchain
        // texture is presented when its command buffer completes); this only
        // synchronizes with the swapchain for the next frame.
        Jni.gpuWaitForGPUSwapchain(check(), w)
    }

    override fun waitForIdle(): Boolean = Jni.gpuWaitForGPUIdle(check())

    internal fun uploadToBuffer(buffer: JvmSDLGPUBuffer, data: ByteArray, offset: Int): Boolean {
        if (offset + data.size > buffer.size) return false
        return Jni.gpuUploadToBuffer(check(), buffer.ptr, data, offset)
    }

    internal fun uploadToTexture(texture: JvmSDLGPUTexture, data: ByteArray, bytesPerRow: Int, x: Int, y: Int, width: Int, height: Int): Boolean {
        // SDL3's pixels_per_row counts *pixels*, not bytes.
        val bpp = if (texture.bytesPerPixel > 0) texture.bytesPerPixel else 1
        return Jni.gpuUploadToTexture(check(), texture.ptr, data, bytesPerRow / bpp, x, y, width, height)
    }

    /** Copies the texture's [width]x[height] region back to the CPU (RGBA8), blocking on a fence. */
    internal fun downloadFromTexture(texture: JvmSDLGPUTexture, width: Int, height: Int): ByteArray? =
        Jni.gpuDownloadFromTexture(check(), texture.ptr, width, height)

    override fun close() {
        val device = ptrValue
        if (device == 0L) return
        ptrValue = 0L
        Jni.gpuDestroyDevice(device)
    }
}

actual object SDLGPU {

    actual val isSupported: Boolean
        get() = Jni.gpuIsSupported(
            SDLGPUShaderFormat.SPIRV or SDLGPUShaderFormat.MSL or SDLGPUShaderFormat.DXIL or SDLGPUShaderFormat.DXBC or SDLGPUShaderFormat.METALLIB,
        )

    actual fun createDevice(debugMode: Boolean): SDLGPUDevice? {
        // format_flags=0 means "any shader format" per the SDL3 docs, but some
        // bundled SDL builds reject it; pass the formats we support explicitly.
        val formats = SDLGPUShaderFormat.SPIRV or SDLGPUShaderFormat.MSL or
            SDLGPUShaderFormat.DXIL or SDLGPUShaderFormat.DXBC or SDLGPUShaderFormat.METALLIB
        val device = Jni.gpuCreateDevice(formats, debugMode)
        if (device == 0L) return null
        return JvmSDLGPUDevice(device)
    }

    actual val drivers: List<String>
        get() = (0 until Jni.gpuGetNumDrivers()).mapNotNull { Jni.gpuGetDriver(it) }
}
