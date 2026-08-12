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

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cn.enaium.sdl

import cnames.structs.SDL_GPUCommandBuffer
import cnames.structs.SDL_GPUBuffer
import cnames.structs.SDL_GPUDevice
import cnames.structs.SDL_GPUFence
import cnames.structs.SDL_GPUGraphicsPipeline
import cnames.structs.SDL_GPURenderPass
import cnames.structs.SDL_GPUSampler
import cnames.structs.SDL_GPUShader
import cnames.structs.SDL_GPUTexture
import cnames.structs.SDL_GPUTransferBuffer
import cnames.structs.SDL_Window
import kotlinx.cinterop.*
import sdl3.*

private fun textureTypeOf(value: Int): SDL_GPUTextureType = when (value) {
    SDLGPUTextureType.D2D_ARRAY -> SDL_GPUTextureType.SDL_GPU_TEXTURETYPE_2D_ARRAY
    SDLGPUTextureType.D3D -> SDL_GPUTextureType.SDL_GPU_TEXTURETYPE_3D
    SDLGPUTextureType.CUBE -> SDL_GPUTextureType.SDL_GPU_TEXTURETYPE_CUBE
    SDLGPUTextureType.CUBE_ARRAY -> SDL_GPUTextureType.SDL_GPU_TEXTURETYPE_CUBE_ARRAY
    else -> SDL_GPUTextureType.SDL_GPU_TEXTURETYPE_2D
}

private fun loadOpOf(value: Int): SDL_GPULoadOp = when (value) {
    SDLGPULoadOp.LOAD -> SDL_GPULoadOp.SDL_GPU_LOADOP_LOAD
    SDLGPULoadOp.DONT_CARE -> SDL_GPULoadOp.SDL_GPU_LOADOP_DONT_CARE
    else -> SDL_GPULoadOp.SDL_GPU_LOADOP_CLEAR
}

private fun storeOpOf(value: Int): SDL_GPUStoreOp = when (value) {
    SDLGPUStoreOp.DONT_CARE -> SDL_GPUStoreOp.SDL_GPU_STOREOP_DONT_CARE
    SDLGPUStoreOp.RESOLVE -> SDL_GPUStoreOp.SDL_GPU_STOREOP_RESOLVE
    SDLGPUStoreOp.RESOLVE_AND_STORE -> SDL_GPUStoreOp.SDL_GPU_STOREOP_RESOLVE_AND_STORE
    else -> SDL_GPUStoreOp.SDL_GPU_STOREOP_STORE
}

private fun vertexInputRateOf(value: Int): SDL_GPUVertexInputRate = when (value) {
    SDLGPUVertexInputRate.INSTANCE -> SDL_GPUVertexInputRate.SDL_GPU_VERTEXINPUTRATE_INSTANCE
    else -> SDL_GPUVertexInputRate.SDL_GPU_VERTEXINPUTRATE_VERTEX
}

private fun compareOpOf(value: Int): SDL_GPUCompareOp = when (value) {
    SDLGPUCompareOp.NEVER -> SDL_GPUCompareOp.SDL_GPU_COMPAREOP_NEVER
    SDLGPUCompareOp.LESS -> SDL_GPUCompareOp.SDL_GPU_COMPAREOP_LESS
    SDLGPUCompareOp.EQUAL -> SDL_GPUCompareOp.SDL_GPU_COMPAREOP_EQUAL
    SDLGPUCompareOp.LESS_EQUAL -> SDL_GPUCompareOp.SDL_GPU_COMPAREOP_LESS_OR_EQUAL
    SDLGPUCompareOp.GREATER -> SDL_GPUCompareOp.SDL_GPU_COMPAREOP_GREATER
    SDLGPUCompareOp.NOT_EQUAL -> SDL_GPUCompareOp.SDL_GPU_COMPAREOP_NOT_EQUAL
    SDLGPUCompareOp.GREATER_EQUAL -> SDL_GPUCompareOp.SDL_GPU_COMPAREOP_GREATER_OR_EQUAL
    SDLGPUCompareOp.ALWAYS -> SDL_GPUCompareOp.SDL_GPU_COMPAREOP_ALWAYS
    else -> SDL_GPUCompareOp.SDL_GPU_COMPAREOP_INVALID
}

private fun blendFactorOf(value: Int): SDL_GPUBlendFactor = when (value) {
    SDLGPUBlendFactor.ONE -> SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_ONE
    SDLGPUBlendFactor.SRC_COLOR -> SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_SRC_COLOR
    SDLGPUBlendFactor.ONE_MINUS_SRC_COLOR -> SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_ONE_MINUS_SRC_COLOR
    SDLGPUBlendFactor.DST_COLOR -> SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_DST_COLOR
    SDLGPUBlendFactor.ONE_MINUS_DST_COLOR -> SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_ONE_MINUS_DST_COLOR
    SDLGPUBlendFactor.SRC_ALPHA -> SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_SRC_ALPHA
    SDLGPUBlendFactor.ONE_MINUS_SRC_ALPHA -> SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_ONE_MINUS_SRC_ALPHA
    SDLGPUBlendFactor.DST_ALPHA -> SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_DST_ALPHA
    SDLGPUBlendFactor.ONE_MINUS_DST_ALPHA -> SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_ONE_MINUS_DST_ALPHA
    SDLGPUBlendFactor.SRC_ALPHA_SATURATE -> SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_SRC_ALPHA_SATURATE
    SDLGPUBlendFactor.CONSTANT_COLOR -> SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_CONSTANT_COLOR
    SDLGPUBlendFactor.ONE_MINUS_CONSTANT_COLOR -> SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_ONE_MINUS_CONSTANT_COLOR
    else -> SDL_GPUBlendFactor.SDL_GPU_BLENDFACTOR_ZERO
}

private fun blendOpOf(value: Int): SDL_GPUBlendOp = when (value) {
    SDLGPUBlendOp.SUBTRACT -> SDL_GPUBlendOp.SDL_GPU_BLENDOP_SUBTRACT
    SDLGPUBlendOp.REVERSE_SUBTRACT -> SDL_GPUBlendOp.SDL_GPU_BLENDOP_REVERSE_SUBTRACT
    SDLGPUBlendOp.MINIMUM -> SDL_GPUBlendOp.SDL_GPU_BLENDOP_MIN
    SDLGPUBlendOp.MAXIMUM -> SDL_GPUBlendOp.SDL_GPU_BLENDOP_MAX
    else -> SDL_GPUBlendOp.SDL_GPU_BLENDOP_ADD
}

private fun filterOf(value: Int): SDL_GPUFilter = when (value) {
    SDLGPUFilter.LINEAR -> SDL_GPUFilter.SDL_GPU_FILTER_LINEAR
    else -> SDL_GPUFilter.SDL_GPU_FILTER_NEAREST
}

private fun mipmapModeOf(value: Int): SDL_GPUSamplerMipmapMode = when (value) {
    SDLGPUSamplerMipmapMode.LINEAR -> SDL_GPUSamplerMipmapMode.SDL_GPU_SAMPLERMIPMAPMODE_LINEAR
    else -> SDL_GPUSamplerMipmapMode.SDL_GPU_SAMPLERMIPMAPMODE_NEAREST
}

private fun addressModeOf(value: Int): SDL_GPUSamplerAddressMode = when (value) {
    SDLGPUSamplerAddressMode.REPEAT -> SDL_GPUSamplerAddressMode.SDL_GPU_SAMPLERADDRESSMODE_REPEAT
    SDLGPUSamplerAddressMode.MIRRORED_REPEAT -> SDL_GPUSamplerAddressMode.SDL_GPU_SAMPLERADDRESSMODE_MIRRORED_REPEAT
    else -> SDL_GPUSamplerAddressMode.SDL_GPU_SAMPLERADDRESSMODE_CLAMP_TO_EDGE
}

private fun shaderStageOf(value: Int): SDL_GPUShaderStage = when (value) {
    SDLGPUShaderStage.FRAGMENT -> SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_FRAGMENT
    else -> SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_VERTEX
}

// =========================================================================
// Native GPU handles
// =========================================================================

internal class NativeSDLGPUShader internal constructor(raw: CPointer<SDL_GPUShader>?, private val device: NativeSDLGPUDevice) : SDLGPUShader {

    internal var raw: CPointer<SDL_GPUShader>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    override fun close() {
        val shader = raw ?: return
        raw = null
        SDL_ReleaseGPUShader(device.check(), shader)
    }
}

internal class NativeSDLGPUGraphicsPipeline internal constructor(raw: CPointer<SDL_GPUGraphicsPipeline>?, private val device: NativeSDLGPUDevice) : SDLGPUGraphicsPipeline {

    internal var raw: CPointer<SDL_GPUGraphicsPipeline>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    override fun close() {
        val pipeline = raw ?: return
        raw = null
        SDL_ReleaseGPUGraphicsPipeline(device.check(), pipeline)
    }
}

internal class NativeSDLGPUTexture internal constructor(raw: CPointer<SDL_GPUTexture>?, private val device: NativeSDLGPUDevice) : SDLGPUTexture {

    internal var raw: CPointer<SDL_GPUTexture>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    override fun upload(data: ByteArray, bytesPerRow: Int, x: Int, y: Int, width: Int, height: Int): Boolean {
        val texture = checkTexture() ?: return false
        return device.uploadToTexture(this, data, bytesPerRow, x, y, width, height)
    }

    private fun checkTexture(): CPointer<SDL_GPUTexture>? = raw

    override fun close() {
        val texture = raw ?: return
        raw = null
        SDL_ReleaseGPUTexture(device.check(), texture)
    }
}

internal class NativeSDLGPUBuffer internal constructor(
    raw: CPointer<SDL_GPUBuffer>?,
    private val device: NativeSDLGPUDevice,
    private val bufferSize: Int,
) : SDLGPUBuffer {

    internal var raw: CPointer<SDL_GPUBuffer>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    override val size: Int
        get() = bufferSize

    override fun setData(data: ByteArray, offset: Int): Boolean {
        val buffer = raw ?: return false
        if (offset + data.size > bufferSize) return false
        return device.uploadToBuffer(this, data, offset)
    }

    override fun close() {
        val buffer = raw ?: return
        raw = null
        SDL_ReleaseGPUBuffer(device.check(), buffer)
    }
}

internal class NativeSDLGPUSampler internal constructor(raw: CPointer<SDL_GPUSampler>?, private val device: NativeSDLGPUDevice) : SDLGPUSampler {

    internal var raw: CPointer<SDL_GPUSampler>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    override fun close() {
        val sampler = raw ?: return
        raw = null
        SDL_ReleaseGPUSampler(device.check(), sampler)
    }
}

internal class NativeSDLGPUFence internal constructor(raw: CPointer<SDL_GPUFence>?, private val device: NativeSDLGPUDevice) : SDLGPUFence {

    internal var raw: CPointer<SDL_GPUFence>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    override val signaled: Boolean
        get() = raw?.let { SDL_QueryGPUFence(device.check(), it) } ?: false

    override fun wait(timeoutMs: Int): Boolean {
        val fence = raw ?: return true
        return device.waitForFences(listOf(this))
    }

    override fun close() {
        val fence = raw ?: return
        raw = null
        SDL_ReleaseGPUFence(device.check(), fence)
    }
}

internal class NativeSDLGPURenderPass internal constructor(
    raw: CPointer<SDL_GPURenderPass>?,
    private val device: NativeSDLGPUDevice,
    private val commandBuffer: CPointer<SDL_GPUCommandBuffer>?,
) : SDLGPURenderPass {

    internal var raw: CPointer<SDL_GPURenderPass>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    private fun check(): CPointer<SDL_GPURenderPass> =
        raw ?: throw IllegalStateException("SDL GPU render pass is closed")

    override fun bindGraphicsPipeline(pipeline: SDLGPUGraphicsPipeline) {
        val p = (pipeline as? NativeSDLGPUGraphicsPipeline)?.raw
            ?: throw IllegalArgumentException("pipeline is not a native SDL GPU pipeline")
        SDL_BindGPUGraphicsPipeline(check(), p)
    }

    override fun setViewport(viewport: SDLGPUViewport) {
        memScoped {
            val v = alloc<SDL_GPUViewport>()
            v.x = viewport.x
            v.y = viewport.y
            v.w = viewport.width
            v.h = viewport.height
            v.min_depth = viewport.minDepth
            v.max_depth = viewport.maxDepth
            SDL_SetGPUViewport(check(), v.ptr)
        }
    }

    override fun setScissor(x: Int, y: Int, width: Int, height: Int) {
        memScoped {
            val r = alloc<SDL_Rect>()
            r.x = x
            r.y = y
            r.w = width
            r.h = height
            SDL_SetGPUScissor(check(), r.ptr)
        }
    }

    override fun bindVertexBuffers(vararg buffers: Pair<SDLGPUBuffer, Int>) = memScoped {
        if (buffers.isEmpty()) return
        val bindings = allocArray<SDL_GPUBufferBinding>(buffers.size)
        for (i in buffers.indices) {
            val (buffer, offset) = buffers[i]
            bindings[i].buffer = (buffer as? NativeSDLGPUBuffer)?.raw
                ?: throw IllegalArgumentException("buffer is not a native SDL GPU buffer")
            bindings[i].offset = offset.toUInt()
        }
        SDL_BindGPUVertexBuffers(check(), 0u, bindings, buffers.size.toUInt())
    }

    override fun bindIndexBuffer(buffer: SDLGPUBuffer, indexSize: Int) {
        val b = (buffer as? NativeSDLGPUBuffer)?.raw
            ?: throw IllegalArgumentException("buffer is not a native SDL GPU buffer")
        memScoped {
            val binding = alloc<SDL_GPUBufferBinding>()
            binding.buffer = b
            binding.offset = 0u
            val indexSizeEnum = if (indexSize == SDLGPUIndexElementSize.UINT32) {
                SDL_GPUIndexElementSize.SDL_GPU_INDEXELEMENTSIZE_32BIT
            } else {
                SDL_GPUIndexElementSize.SDL_GPU_INDEXELEMENTSIZE_16BIT
            }
            SDL_BindGPUIndexBuffer(check(), binding.ptr, indexSizeEnum)
        }
    }

    override fun bindGraphicsSamplers(slot: Int, vararg samplers: SDLGPUSampler) = memScoped {
        if (samplers.isEmpty()) return
        val arr = allocArray<SDL_GPUTextureSamplerBinding>(samplers.size)
        for (i in samplers.indices) {
            arr[i].sampler = (samplers[i] as? NativeSDLGPUSampler)?.raw
                ?: throw IllegalArgumentException("sampler is not a native SDL GPU sampler")
        }
        SDL_BindGPUFragmentSamplers(check(), slot.toUInt(), arr, samplers.size.toUInt())
    }

    override fun bindGraphicsTextures(slot: Int, vararg textures: SDLGPUTexture) = memScoped {
        if (textures.isEmpty()) return
        val arr = allocArray<SDL_GPUTextureSamplerBinding>(textures.size)
        for (i in textures.indices) {
            arr[i].texture = (textures[i] as? NativeSDLGPUTexture)?.raw
                ?: throw IllegalArgumentException("texture is not a native SDL GPU texture")
            arr[i].sampler = null
        }
        SDL_BindGPUFragmentSamplers(check(), slot.toUInt(), arr, textures.size.toUInt())
    }

    override fun pushVertexUniformData(slot: Int, data: ByteArray) {
        val cmd = commandBuffer ?: throw IllegalStateException("no command buffer for render pass")
        data.usePinned { pinned ->
            SDL_PushGPUVertexUniformData(cmd, slot.toUInt(), pinned.addressOf(0), data.size.toUInt())
        }
    }

    override fun drawPrimitives(vertexCount: Int, instanceCount: Int, firstVertex: Int, firstInstance: Int) {
        SDL_DrawGPUPrimitives(check(), vertexCount.toUInt(), instanceCount.toUInt(), firstVertex.toUInt(), firstInstance.toUInt())
    }

    override fun drawIndexedPrimitives(indexCount: Int, instanceCount: Int, firstIndex: Int, vertexOffset: Int, firstInstance: Int) {
        SDL_DrawGPUIndexedPrimitives(check(), indexCount.toUInt(), instanceCount.toUInt(), firstIndex.toUInt(), vertexOffset.toInt(), firstInstance.toUInt())
    }

    override fun end() {
        val pass = raw ?: return
        raw = null
        SDL_EndGPURenderPass(pass)
    }

    override fun close() = end()
}

internal class NativeSDLGPUCommandBuffer internal constructor(
    raw: CPointer<SDL_GPUCommandBuffer>?,
    private val device: NativeSDLGPUDevice,
) : SDLGPUCommandBuffer {

    internal var raw: CPointer<SDL_GPUCommandBuffer>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    private fun check(): CPointer<SDL_GPUCommandBuffer> =
        raw ?: throw IllegalStateException("SDL GPU command buffer is closed")

    override fun beginRenderPass(colorTargets: List<SDLGPUColorTargetInfo>): SDLGPURenderPass? = memScoped {
        if (colorTargets.isEmpty()) return null
        val targets = allocArray<SDL_GPUColorTargetInfo>(colorTargets.size)
        for (i in colorTargets.indices) {
            val t = colorTargets[i]
            targets[i].texture = (t.texture as? NativeSDLGPUTexture)?.raw
                ?: throw IllegalArgumentException("color target texture is not a native SDL GPU texture")
            targets[i].mip_level = t.mipLevel.toUInt()
            targets[i].layer_or_depth_plane = t.layerOrDepthPlane.toUInt()
            targets[i].clear_color.r = t.clearColor.r / 255f
            targets[i].clear_color.g = t.clearColor.g / 255f
            targets[i].clear_color.b = t.clearColor.b / 255f
            targets[i].clear_color.a = t.clearColor.a / 255f
            targets[i].load_op = loadOpOf(t.loadOp)
            targets[i].store_op = storeOpOf(t.storeOp)
        }
        val pass = SDL_BeginGPURenderPass(check(), targets, colorTargets.size.toUInt(), null)
        pass?.let { NativeSDLGPURenderPass(it, device, raw) }
    }

    override fun pushVertexUniformData(slot: Int, data: ByteArray) {
        data.usePinned { pinned ->
            SDL_PushGPUVertexUniformData(check(), slot.toUInt(), pinned.addressOf(0), data.size.toUInt())
        }
    }

    override fun pushFragmentUniformData(slot: Int, data: ByteArray) {
        data.usePinned { pinned ->
            SDL_PushGPUFragmentUniformData(check(), slot.toUInt(), pinned.addressOf(0), data.size.toUInt())
        }
    }

    override fun end() {
        // The command buffer is submitted through SDLGPUDevice.submit; mark
        // it as no longer usable here so accidental double use fails fast.
        if (raw == null) throw IllegalStateException("SDL GPU command buffer is closed")
    }

    override fun close() {
        // Without submit the buffer must be cancelled to avoid a leak.
        val cmd = raw ?: return
        raw = null
        SDL_CancelGPUCommandBuffer(cmd)
    }
}

internal class NativeSDLGPUDevice internal constructor(raw: CPointer<SDL_GPUDevice>?) : SDLGPUDevice {

    internal var raw: CPointer<SDL_GPUDevice>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    internal fun check(): CPointer<SDL_GPUDevice> =
        raw ?: throw IllegalStateException("SDL GPU device is closed")

    override val shaderFormats: Int
        get() = SDL_GetGPUShaderFormats(check()).toInt()

    override fun claimWindow(window: SDLWindow): Boolean {
        val native = (window as? NativeSDLWindow)?.check()
            ?: throw IllegalArgumentException("window is not a native SDL window")
        return SDL_ClaimWindowForGPUDevice(check(), native)
    }

    override fun releaseDrawable(window: SDLWindow) {
        val native = (window as? NativeSDLWindow)?.check()
            ?: throw IllegalArgumentException("window is not a native SDL window")
        SDL_ReleaseWindowFromGPUDevice(check(), native)
    }

    override fun getWindowFormat(window: SDLWindow): Int? {
        val native = (window as? NativeSDLWindow)?.check()
            ?: throw IllegalArgumentException("window is not a native SDL window")
        val format = SDL_GetGPUSwapchainTextureFormat(check(), native)
        return if (format.value == 0u) null else format.value.toInt()
    }

    override fun acquireSwapchainTexture(window: SDLWindow): SDLGPUWindowTexture? = memScoped {
        val native = (window as? NativeSDLWindow)?.check()
            ?: throw IllegalArgumentException("window is not a native SDL window")
        val cmd = SDL_AcquireGPUCommandBuffer(check()) ?: return null
        val texture = alloc<CPointerVar<SDL_GPUTexture>>()
        val width = alloc<UIntVar>()
        val height = alloc<UIntVar>()
        if (!SDL_WaitAndAcquireGPUSwapchainTexture(cmd, native, texture.ptr, width.ptr, height.ptr)) {
            SDL_CancelGPUCommandBuffer(cmd)
            return null
        }
        val tex = texture.value
        SDLGPUWindowTexture(
            texture = tex?.let { NativeSDLGPUTexture(it, this@NativeSDLGPUDevice) },
            srcRect = SDLRect(0, 0, width.value.toInt(), height.value.toInt()),
        )
    }

    override fun createTexture(createInfo: SDLGPUTextureCreateInfo): SDLGPUTexture? = memScoped {
        val info = alloc<SDL_GPUTextureCreateInfo>()
        info.type = textureTypeOf(createInfo.type)
        info.format = gpuTextureFormatOf(createInfo.format)
        info.usage = createInfo.usage.toUInt()
        info.width = createInfo.width.toUInt()
        info.height = createInfo.height.toUInt()
        info.layer_count_or_depth = createInfo.layerCountOrDepth.toUInt()
        info.num_levels = createInfo.numLevels.toUInt()
        info.sample_count = gpuSampleCountOf(createInfo.sampleCount)
        val texture = SDL_CreateGPUTexture(check(), info.ptr) ?: return null
        NativeSDLGPUTexture(texture, this@NativeSDLGPUDevice)
    }

    override fun createBuffer(createInfo: SDLGPUBufferCreateInfo): SDLGPUBuffer? = memScoped {
        val info = alloc<SDL_GPUBufferCreateInfo>()
        info.usage = createInfo.usage.toUInt()
        info.size = createInfo.size.toUInt()
        val buffer = SDL_CreateGPUBuffer(check(), info.ptr) ?: return null
        NativeSDLGPUBuffer(buffer, this@NativeSDLGPUDevice, createInfo.size)
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
    ): SDLGPUShader? = memScoped {
        val codePtr = gpuShaderCode(code)
        val shader = SDL_kmp_CreateGPUShader(
            check(),
            codePtr,
            code.size,
            entryPoint,
            format.toUInt(),
            shaderStageOf(stage).value.toUInt(),
            numSamplers.toUInt(),
            numStorageTextures.toUInt(),
            numStorageBuffers.toUInt(),
            numUniformBuffers.toUInt(),
        ) ?: return null
        NativeSDLGPUShader(shader, this@NativeSDLGPUDevice)
    }

    override fun createGraphicsPipeline(createInfo: SDLGPUGraphicsPipelineCreateInfo): SDLGPUGraphicsPipeline? = memScoped {
        val info = alloc<SDL_GPUGraphicsPipelineCreateInfo>()
        info.vertex_shader = (createInfo.vertexShader as? NativeSDLGPUShader)?.raw
            ?: throw IllegalArgumentException("vertex shader is not a native SDL GPU shader")
        info.fragment_shader = (createInfo.fragmentShader as? NativeSDLGPUShader)?.raw
        info.primitive_type = when (createInfo.primitiveType) {
            SDLGPUPrimitiveType.TRIANGLESTRIP -> SDL_GPUPrimitiveType.SDL_GPU_PRIMITIVETYPE_TRIANGLESTRIP
            SDLGPUPrimitiveType.LINELIST -> SDL_GPUPrimitiveType.SDL_GPU_PRIMITIVETYPE_LINELIST
            SDLGPUPrimitiveType.LINESTRIP -> SDL_GPUPrimitiveType.SDL_GPU_PRIMITIVETYPE_LINESTRIP
            SDLGPUPrimitiveType.POINTLIST -> SDL_GPUPrimitiveType.SDL_GPU_PRIMITIVETYPE_POINTLIST
            else -> SDL_GPUPrimitiveType.SDL_GPU_PRIMITIVETYPE_TRIANGLELIST
        }

        // vertex input state
        val buffers = allocArray<SDL_GPUVertexBufferDescription>(createInfo.vertexInputState.vertexBufferDescriptions.size)
        for (i in createInfo.vertexInputState.vertexBufferDescriptions.indices) {
            val b = createInfo.vertexInputState.vertexBufferDescriptions[i]
            buffers[i].slot = b.slot.toUInt()
            buffers[i].pitch = b.pitch.toUInt()
            buffers[i].input_rate = vertexInputRateOf(b.inputRate)
            buffers[i].instance_step_rate = b.instanceStepRate.toUInt()
        }
        val attributes = allocArray<SDL_GPUVertexAttribute>(createInfo.vertexInputState.vertexAttributes.size)
        for (i in createInfo.vertexInputState.vertexAttributes.indices) {
            val a = createInfo.vertexInputState.vertexAttributes[i]
            attributes[i].location = a.location.toUInt()
            attributes[i].buffer_slot = a.bufferSlot.toUInt()
            attributes[i].format = gpuVertexElementFormatOf(a.format)
            attributes[i].offset = a.offset.toUInt()
        }
        info.vertex_input_state.vertex_buffer_descriptions = buffers
        info.vertex_input_state.num_vertex_buffers = createInfo.vertexInputState.vertexBufferDescriptions.size.toUInt()
        info.vertex_input_state.vertex_attributes = attributes
        info.vertex_input_state.num_vertex_attributes = createInfo.vertexInputState.vertexAttributes.size.toUInt()

        // rasterizer state
        info.rasterizer_state.fill_mode = if (createInfo.rasterizerState.fillMode == SDLGPUFillMode.LINE) {
            SDL_GPUFillMode.SDL_GPU_FILLMODE_LINE
        } else {
            SDL_GPUFillMode.SDL_GPU_FILLMODE_FILL
        }
        info.rasterizer_state.cull_mode = when (createInfo.rasterizerState.cullMode) {
            SDLGPUCullMode.FRONT -> SDL_GPUCullMode.SDL_GPU_CULLMODE_FRONT
            SDLGPUCullMode.BACK -> SDL_GPUCullMode.SDL_GPU_CULLMODE_BACK
            else -> SDL_GPUCullMode.SDL_GPU_CULLMODE_NONE
        }
        info.rasterizer_state.front_face = if (createInfo.rasterizerState.frontFace == SDLGPUFrontFace.CLOCKWISE) {
            SDL_GPUFrontFace.SDL_GPU_FRONTFACE_CLOCKWISE
        } else {
            SDL_GPUFrontFace.SDL_GPU_FRONTFACE_COUNTER_CLOCKWISE
        }

        // depth stencil state
        info.depth_stencil_state.compare_op = compareOpOf(createInfo.depthStencilState.compareOp)
        info.depth_stencil_state.enable_depth_test = createInfo.depthStencilState.enableDepthTest
        info.depth_stencil_state.enable_depth_write = createInfo.depthStencilState.enableDepthWrite

        // color targets
        val targets = allocArray<SDL_GPUColorTargetDescription>(createInfo.targetDescriptions.size)
        for (i in createInfo.targetDescriptions.indices) {
            val t = createInfo.targetDescriptions[i]
            val blend = t.blendState
            targets[i].format = gpuTextureFormatOf(t.format)
            targets[i].blend_state.src_color_blendfactor = blendFactorOf(blend.srcColorBlendFactor)
            targets[i].blend_state.dst_color_blendfactor = blendFactorOf(blend.dstColorBlendFactor)
            targets[i].blend_state.color_blend_op = blendOpOf(blend.colorBlendOp)
            targets[i].blend_state.src_alpha_blendfactor = blendFactorOf(blend.srcAlphaBlendFactor)
            targets[i].blend_state.dst_alpha_blendfactor = blendFactorOf(blend.dstAlphaBlendFactor)
            targets[i].blend_state.alpha_blend_op = blendOpOf(blend.alphaBlendOp)
            targets[i].blend_state.color_write_mask = blend.colorWriteMask.toUByte()
            targets[i].blend_state.enable_blend = true
            targets[i].blend_state.enable_color_write_mask = true
        }
        info.target_info.color_target_descriptions = targets
        info.target_info.num_color_targets = createInfo.targetDescriptions.size.toUInt()

        val pipeline = SDL_CreateGPUGraphicsPipeline(check(), info.ptr) ?: return null
        NativeSDLGPUGraphicsPipeline(pipeline, this@NativeSDLGPUDevice)
    }

    override fun createSampler(createInfo: SDLGPUSamplerCreateInfo): SDLGPUSampler? = memScoped {
        val info = alloc<SDL_GPUSamplerCreateInfo>()
        info.min_filter = filterOf(createInfo.minFilter)
        info.mag_filter = filterOf(createInfo.magFilter)
        info.mipmap_mode = mipmapModeOf(createInfo.mipmapMode)
        info.address_mode_u = addressModeOf(createInfo.addressModeU)
        info.address_mode_v = addressModeOf(createInfo.addressModeV)
        info.address_mode_w = addressModeOf(createInfo.addressModeW)
        info.max_anisotropy = createInfo.maxAnisotropy
        val sampler = SDL_CreateGPUSampler(check(), info.ptr) ?: return null
        NativeSDLGPUSampler(sampler, this@NativeSDLGPUDevice)
    }

    override fun beginCommandBuffer(): SDLGPUCommandBuffer? {
        val cmd = SDL_AcquireGPUCommandBuffer(check()) ?: return null
        return NativeSDLGPUCommandBuffer(cmd, this)
    }

    override fun submit(commandBuffer: SDLGPUCommandBuffer): Boolean {
        val native = commandBuffer as? NativeSDLGPUCommandBuffer
            ?: throw IllegalArgumentException("command buffer is not a native SDL GPU command buffer")
        val cmd = native.raw ?: throw IllegalStateException("SDL GPU command buffer is closed")
        native.raw = null
        return SDL_SubmitGPUCommandBuffer(cmd)
    }

    override fun submitAndAcquireFence(commandBuffer: SDLGPUCommandBuffer): SDLGPUFence? {
        val native = commandBuffer as? NativeSDLGPUCommandBuffer
            ?: throw IllegalArgumentException("command buffer is not a native SDL GPU command buffer")
        val cmd = native.raw ?: throw IllegalStateException("SDL GPU command buffer is closed")
        native.raw = null
        val fence = SDL_SubmitGPUCommandBufferAndAcquireFence(cmd) ?: return null
        return NativeSDLGPUFence(fence, this)
    }

    override fun waitForFences(fences: List<SDLGPUFence>): Boolean = memScoped {
        if (fences.isEmpty()) return true
        val arr = allocArray<CPointerVar<SDL_GPUFence>>(fences.size)
        for (i in fences.indices) {
            arr[i] = (fences[i] as? NativeSDLGPUFence)?.raw
                ?: throw IllegalArgumentException("fence is not a native SDL GPU fence")
        }
        SDL_WaitForGPUFences(check(), true, arr, fences.size.toUInt())
    }

    override fun present(window: SDLWindow) {
        val native = (window as? NativeSDLWindow)?.check()
            ?: throw IllegalArgumentException("window is not a native SDL window")
        SDL_WaitForGPUSwapchain(check(), native)
    }

    override fun waitForIdle(): Boolean = SDL_WaitForGPUIdle(check())

    internal fun uploadToBuffer(buffer: NativeSDLGPUBuffer, data: ByteArray, offset: Int): Boolean = memScoped {
        val b = buffer.raw ?: return false
        val transfer = alloc<SDL_GPUTransferBufferCreateInfo>()
        transfer.usage = SDL_GPUTransferBufferUsage.SDL_GPU_TRANSFERBUFFERUSAGE_UPLOAD
        transfer.size = data.size.toUInt()
        val tbuffer = SDL_CreateGPUTransferBuffer(check(), transfer.ptr) ?: return false
        try {
            val mapped = SDL_MapGPUTransferBuffer(check(), tbuffer, false) ?: return false
            data.usePinned { pinned ->
                val src = pinned.addressOf(0).reinterpret<ByteVar>()
                val dst = mapped.reinterpret<ByteVar>()
                for (i in 0 until data.size) dst[i] = src[i]
            }
            SDL_UnmapGPUTransferBuffer(check(), tbuffer)

            val cmd = SDL_AcquireGPUCommandBuffer(check()) ?: return false
            val pass = SDL_BeginGPUCopyPass(cmd)
            val src = alloc<SDL_GPUTransferBufferLocation>()
            src.transfer_buffer = tbuffer
            src.offset = 0u
            val region = alloc<SDL_GPUBufferRegion>()
            region.offset = offset.toUInt()
            region.size = data.size.toUInt()
            SDL_UploadToGPUBuffer(pass, src.ptr, region.ptr, false)
            SDL_EndGPUCopyPass(pass)
            SDL_SubmitGPUCommandBuffer(cmd)
            true
        } finally {
            SDL_ReleaseGPUTransferBuffer(check(), tbuffer)
        }
    }

    internal fun uploadToTexture(texture: NativeSDLGPUTexture, data: ByteArray, bytesPerRow: Int, x: Int, y: Int, width: Int, height: Int): Boolean = memScoped {
        val tex = texture.raw ?: return false
        val transfer = alloc<SDL_GPUTransferBufferCreateInfo>()
        transfer.usage = SDL_GPUTransferBufferUsage.SDL_GPU_TRANSFERBUFFERUSAGE_UPLOAD
        transfer.size = data.size.toUInt()
        val buffer = SDL_CreateGPUTransferBuffer(check(), transfer.ptr) ?: return false
        try {
            val mapped = SDL_MapGPUTransferBuffer(check(), buffer, false) ?: return false
            data.usePinned { pinned ->
                val src = pinned.addressOf(0).reinterpret<ByteVar>()
                val dst = mapped.reinterpret<ByteVar>()
                for (i in 0 until data.size) dst[i] = src[i]
            }
            SDL_UnmapGPUTransferBuffer(check(), buffer)

            val cmd = SDL_AcquireGPUCommandBuffer(check()) ?: return false
            val pass = SDL_BeginGPUCopyPass(cmd)
            val src = alloc<SDL_GPUTextureTransferInfo>()
            src.transfer_buffer = buffer
            src.offset = 0u
            src.pixels_per_row = bytesPerRow.toUInt()
            val region = alloc<SDL_GPUTextureRegion>()
            region.texture = tex
            region.mip_level = 0u
            region.layer = 0u
            region.x = x.toUInt()
            region.y = y.toUInt()
            region.z = 0u
            region.w = width.toUInt()
            region.h = height.toUInt()
            region.d = 1u
            SDL_UploadToGPUTexture(pass, src.ptr, region.ptr, false)
            SDL_EndGPUCopyPass(pass)
            SDL_SubmitGPUCommandBuffer(cmd)
            true
        } finally {
            SDL_ReleaseGPUTransferBuffer(check(), buffer)
        }
    }

    override fun close() {
        val device = raw ?: return
        raw = null
        SDL_DestroyGPUDevice(device)
    }
}

private fun gpuShaderCode(code: ByteArray): CPointer<UByteVar>? = memScoped {
    val arr = allocArray<UByteVar>(code.size)
    for (i in code.indices) arr[i] = code[i].toUByte()
    arr
}

private fun gpuTextureFormatOf(value: Int): SDL_GPUTextureFormat =
    SDL_GPUTextureFormat.entries.firstOrNull { it.value.toInt() == value } ?: SDL_GPUTextureFormat.SDL_GPU_TEXTUREFORMAT_R8G8B8A8_UNORM

private fun gpuSampleCountOf(value: Int): SDL_GPUSampleCount = when (value) {
    2 -> SDL_GPUSampleCount.SDL_GPU_SAMPLECOUNT_2
    4 -> SDL_GPUSampleCount.SDL_GPU_SAMPLECOUNT_4
    8 -> SDL_GPUSampleCount.SDL_GPU_SAMPLECOUNT_8
    else -> SDL_GPUSampleCount.SDL_GPU_SAMPLECOUNT_1
}

private fun gpuVertexElementFormatOf(value: Int): SDL_GPUVertexElementFormat =
    SDL_GPUVertexElementFormat.entries.firstOrNull { it.value.toInt() == value } ?: SDL_GPUVertexElementFormat.SDL_GPU_VERTEXELEMENTFORMAT_FLOAT

actual object SDLGPU {

    actual val isSupported: Boolean
        get() = SDL_GPUSupportsShaderFormats(
            SDLGPUShaderFormat.SPIRV.toUInt() or SDLGPUShaderFormat.MSL.toUInt() or
                SDLGPUShaderFormat.DXIL.toUInt() or SDLGPUShaderFormat.DXBC.toUInt() or
                SDLGPUShaderFormat.METALLIB.toUInt(),
            null,
        )

    actual fun createDevice(debugMode: Boolean): SDLGPUDevice? {
        val device = SDL_CreateGPUDevice(0u, debugMode, null as String?) ?: return null
        return NativeSDLGPUDevice(device)
    }

    actual val drivers: List<String>
        get() = (0 until SDL_GetNumGPUDrivers()).mapNotNull { SDL_GetGPUDriver(it)?.toKString() }
}
