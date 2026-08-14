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

import org.lwjgl.sdl.SDLGPU
import org.lwjgl.sdl.SDLGPU.SDL_GPU_COLORCOMPONENT_A
import org.lwjgl.sdl.SDLGPU.SDL_GPU_COLORCOMPONENT_B
import org.lwjgl.sdl.SDLGPU.SDL_GPU_COLORCOMPONENT_G
import org.lwjgl.sdl.SDLGPU.SDL_GPU_COLORCOMPONENT_R
import org.lwjgl.sdl.SDL_GPUBufferBinding
import org.lwjgl.sdl.SDL_GPUBufferCreateInfo
import org.lwjgl.sdl.SDL_GPUBufferRegion
import org.lwjgl.sdl.SDL_GPUColorTargetInfo
import org.lwjgl.sdl.SDL_GPUGraphicsPipelineCreateInfo
import org.lwjgl.sdl.SDL_GPURasterizerState
import org.lwjgl.sdl.SDL_GPUSamplerCreateInfo
import org.lwjgl.sdl.SDL_GPUShaderCreateInfo
import org.lwjgl.sdl.SDL_GPUTextureCreateInfo
import org.lwjgl.sdl.SDL_GPUTextureRegion
import org.lwjgl.sdl.SDL_GPUTextureSamplerBinding
import org.lwjgl.sdl.SDL_GPUTextureTransferInfo
import org.lwjgl.sdl.SDL_GPUTransferBufferCreateInfo
import org.lwjgl.sdl.SDL_GPUTransferBufferLocation
import org.lwjgl.sdl.SDL_GPUViewport
import org.lwjgl.sdl.SDL_Rect
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

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
// JVM (LWJGL) GPU handles
// =========================================================================

internal class JvmSDLGPUShader internal constructor(ptr: Long, private val device: JvmSDLGPUDevice) : SDLGPUShader {

    private var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue

    override fun close() {
        val shader = ptrValue
        if (shader == 0L) return
        ptrValue = 0L
        SDLGPU.SDL_ReleaseGPUShader(device.check(), shader)
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
        SDLGPU.SDL_ReleaseGPUGraphicsPipeline(device.check(), pipeline)
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
        SDLGPU.SDL_ReleaseGPUTexture(device.check(), texture)
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
        SDLGPU.SDL_ReleaseGPUBuffer(device.check(), buffer)
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
        SDLGPU.SDL_ReleaseGPUSampler(device.check(), sampler)
    }
}

internal class JvmSDLGPUFence internal constructor(ptr: Long, private val device: JvmSDLGPUDevice) : SDLGPUFence {

    private var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue

    override val signaled: Boolean
        get() = ptrValue.let { it != 0L && SDLGPU.SDL_QueryGPUFence(device.check(), it) }

    override fun wait(timeoutMs: Int): Boolean = MemoryStack.stackPush().use { stack ->
        val fences = stack.mallocPointer(1)
        fences.put(0, ptrValue)
        SDLGPU.SDL_WaitForGPUFences(device.check(), true, fences)
    }

    override fun close() {
        val fence = ptrValue
        if (fence == 0L) return
        ptrValue = 0L
        SDLGPU.SDL_ReleaseGPUFence(device.check(), fence)
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
        SDLGPU.SDL_BindGPUGraphicsPipeline(check(), p)
    }

    override fun setViewport(viewport: SDLGPUViewport) {
        SDL_GPUViewport.calloc().use { v ->
            v.x(viewport.x).y(viewport.y).w(viewport.width).h(viewport.height)
                .min_depth(viewport.minDepth).max_depth(viewport.maxDepth)
            SDLGPU.SDL_SetGPUViewport(check(), v)
        }
    }

    override fun setScissor(x: Int, y: Int, width: Int, height: Int) {
        val r = SDL_Rect.calloc()
        try {
            r.x(x).y(y).w(width).h(height)
            SDLGPU.SDL_SetGPUScissor(check(), r)
        } finally {
            r.free()
        }
    }

    override fun bindVertexBuffers(vararg buffers: Pair<SDLGPUBuffer, Int>) {
        if (buffers.isEmpty()) return
        SDL_GPUBufferBinding.calloc(buffers.size).use { bindings ->
            for (i in buffers.indices) {
                val (buffer, offset) = buffers[i]
                bindings.get(i)
                    .buffer((buffer as? JvmSDLGPUBuffer)?.ptr ?: throw IllegalArgumentException("buffer is not a JVM SDL GPU buffer"))
                    .offset(offset)
            }
            SDLGPU.SDL_BindGPUVertexBuffers(check(), 0, bindings)
        }
    }

    override fun bindIndexBuffer(buffer: SDLGPUBuffer, indexSize: Int) {
        val b = buffer as? JvmSDLGPUBuffer
            ?: throw IllegalArgumentException("buffer is not a JVM SDL GPU buffer")
        SDL_GPUBufferBinding.calloc().use { binding ->
            binding.buffer(b.ptr).offset(0)
            val size = if (indexSize == SDLGPUIndexElementSize.UINT32) 4 else 2
            SDLGPU.SDL_BindGPUIndexBuffer(check(), binding, size)
        }
    }

    override fun bindGraphicsSamplers(slot: Int, vararg samplers: SDLGPUSampler) {
        if (samplers.isEmpty()) return
        SDL_GPUTextureSamplerBinding.calloc(samplers.size).use { bindings ->
            for (i in samplers.indices) {
                bindings.get(i).sampler((samplers[i] as? JvmSDLGPUSampler)?.ptr ?: throw IllegalArgumentException("sampler is not a JVM SDL GPU sampler"))
            }
            // LWJGL 3.4.x's SDL_BindGPUFragmentSamplers takes texture+sampler bindings.
            SDLGPU.SDL_BindGPUFragmentSamplers(check(), slot, bindings)
        }
    }

    override fun bindGraphicsTextures(slot: Int, vararg textures: SDLGPUTexture) {
        if (textures.isEmpty()) return
        SDL_GPUTextureSamplerBinding.calloc(textures.size).use { bindings ->
            for (i in textures.indices) {
                bindings.get(i).texture((textures[i] as? JvmSDLGPUTexture)?.ptr ?: throw IllegalArgumentException("texture is not a JVM SDL GPU texture"))
            }
            SDLGPU.SDL_BindGPUFragmentSamplers(check(), slot, bindings)
        }
    }

    override fun bindGraphicsTextureSamplers(slot: Int, vararg bindings: Pair<SDLGPUTexture, SDLGPUSampler>) {
        if (bindings.isEmpty()) return
        SDL_GPUTextureSamplerBinding.calloc(bindings.size).use { arr ->
            for (i in bindings.indices) {
                val (texture, sampler) = bindings[i]
                arr.get(i)
                    .texture((texture as? JvmSDLGPUTexture)?.ptr ?: throw IllegalArgumentException("texture is not a JVM SDL GPU texture"))
                    .sampler((sampler as? JvmSDLGPUSampler)?.ptr ?: throw IllegalArgumentException("sampler is not a JVM SDL GPU sampler"))
            }
            SDLGPU.SDL_BindGPUFragmentSamplers(check(), slot, arr)
        }
    }

    override fun pushVertexUniformData(slot: Int, data: ByteArray) {
        val buffer = MemoryUtil.memAlloc(data.size)
        try {
            buffer.put(data).rewind()
            SDLGPU.SDL_PushGPUVertexUniformData(check(), slot, buffer)
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    override fun drawPrimitives(vertexCount: Int, instanceCount: Int, firstVertex: Int, firstInstance: Int) {
        SDLGPU.SDL_DrawGPUPrimitives(check(), vertexCount, instanceCount, firstVertex, firstInstance)
    }

    override fun drawIndexedPrimitives(indexCount: Int, instanceCount: Int, firstIndex: Int, vertexOffset: Int, firstInstance: Int) {
        SDLGPU.SDL_DrawGPUIndexedPrimitives(check(), indexCount, instanceCount, firstIndex, vertexOffset, firstInstance)
    }

    override fun end() {
        val pass = ptrValue
        if (pass == 0L) return
        ptrValue = 0L
        SDLGPU.SDL_EndGPURenderPass(pass)
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
        SDL_GPUColorTargetInfo.calloc(colorTargets.size).use { targets ->
            for (i in colorTargets.indices) {
                val t = colorTargets[i]
                targets.get(i)
                    .texture((t.texture as? JvmSDLGPUTexture)?.ptr ?: throw IllegalArgumentException("color target texture is not a JVM SDL GPU texture"))
                    .mip_level(t.mipLevel)
                    .layer_or_depth_plane(t.layerOrDepthPlane)
                    .load_op(t.loadOp)
                    .store_op(t.storeOp)
                val clear = t.clearColor
                targets.get(i).clear_color { c ->
                    c.r(clear.r / 255f).g(clear.g / 255f).b(clear.b / 255f).a(clear.a / 255f)
                }
            }
            val pass = SDLGPU.SDL_BeginGPURenderPass(check(), targets, null)
            if (pass == 0L) return null
            return JvmSDLGPURenderPass(pass)
        }
    }

    override fun pushVertexUniformData(slot: Int, data: ByteArray) {
        val buffer = MemoryUtil.memAlloc(data.size)
        try {
            buffer.put(data).rewind()
            SDLGPU.SDL_PushGPUVertexUniformData(check(), slot, buffer)
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    override fun pushFragmentUniformData(slot: Int, data: ByteArray) {
        val buffer = MemoryUtil.memAlloc(data.size)
        try {
            buffer.put(data).rewind()
            SDLGPU.SDL_PushGPUFragmentUniformData(check(), slot, buffer)
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    override fun end() {
        if (ptrValue == 0L) throw IllegalStateException("SDL GPU command buffer is closed")
    }

    override fun close() {
        val cmd = ptrValue
        if (cmd == 0L) return
        ptrValue = 0L
        SDLGPU.SDL_CancelGPUCommandBuffer(cmd)
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
        get() = SDLGPU.SDL_GetGPUShaderFormats(check())

    override fun claimWindow(window: SDLWindow): Boolean {
        val w = (window as? JvmSDLWindow)?.ptr ?: throw IllegalArgumentException("window is not a JVM SDL window")
        return SDLGPU.SDL_ClaimWindowForGPUDevice(check(), w)
    }

    override fun releaseDrawable(window: SDLWindow) {
        val w = (window as? JvmSDLWindow)?.ptr ?: throw IllegalArgumentException("window is not a JVM SDL window")
        SDLGPU.SDL_ReleaseWindowFromGPUDevice(check(), w)
    }

    override fun getWindowFormat(window: SDLWindow): Int? {
        val w = (window as? JvmSDLWindow)?.ptr ?: throw IllegalArgumentException("window is not a JVM SDL window")
        val format = SDLGPU.SDL_GetGPUSwapchainTextureFormat(check(), w)
        return if (format == 0) null else format
    }

    override fun acquireSwapchainTexture(commandBuffer: SDLGPUCommandBuffer, window: SDLWindow): SDLGPUWindowTexture? = MemoryStack.stackPush().use { stack ->
        val cmd = (commandBuffer as? JvmSDLGPUCommandBuffer)?.ptrValue
            ?: throw IllegalArgumentException("command buffer is not a JVM SDL GPU command buffer")
        val w = (window as? JvmSDLWindow)?.ptr ?: throw IllegalArgumentException("window is not a JVM SDL window")
        val texture = stack.mallocPointer(1)
        val width = stack.mallocInt(1)
        val height = stack.mallocInt(1)
        if (!SDLGPU.SDL_WaitAndAcquireGPUSwapchainTexture(cmd, w, texture, width, height)) {
            return null
        }
        SDLGPUWindowTexture(
            texture = texture.get(0).takeIf { it != 0L }?.let { JvmSDLGPUTexture(it, this@JvmSDLGPUDevice, 0) },
            srcRect = SDLRect(0, 0, width.get(0), height.get(0)),
        )
    }

    override fun acquireSwapchainTexture(window: SDLWindow): SDLGPUWindowTexture? = MemoryStack.stackPush().use { stack ->
        val w = (window as? JvmSDLWindow)?.ptr ?: throw IllegalArgumentException("window is not a JVM SDL window")
        val texture = stack.mallocPointer(1)
        val width = stack.mallocInt(1)
        val height = stack.mallocInt(1)
        if (!SDLGPU.SDL_WaitAndAcquireGPUSwapchainTexture(check(), w, texture, width, height)) {
            return null
        }
        SDLGPUWindowTexture(
            texture = texture.get(0).takeIf { it != 0L }?.let { JvmSDLGPUTexture(it, this@JvmSDLGPUDevice, 0) },
            srcRect = SDLRect(0, 0, width.get(0), height.get(0)),
        )
    }

    override fun createTexture(createInfo: SDLGPUTextureCreateInfo): SDLGPUTexture? =
        SDL_GPUTextureCreateInfo.calloc().use { info ->
            info.type(createInfo.type)
                .format(createInfo.format)
                .usage(createInfo.usage)
                .width(createInfo.width)
                .height(createInfo.height)
                .layer_count_or_depth(createInfo.layerCountOrDepth)
                .num_levels(createInfo.numLevels)
                .sample_count(createInfo.sampleCount)
            val texture = SDLGPU.SDL_CreateGPUTexture(check(), info)
            if (texture == 0L) {
                null
            } else {
                JvmSDLGPUTexture(texture, this@JvmSDLGPUDevice, bytesPerPixelOf(createInfo.format))
            }
        }

    override fun createBuffer(createInfo: SDLGPUBufferCreateInfo): SDLGPUBuffer? =
        SDL_GPUBufferCreateInfo.calloc().use { info ->
            info.usage(createInfo.usage).size(createInfo.size)
            val buffer = SDLGPU.SDL_CreateGPUBuffer(check(), info)
            if (buffer == 0L) {
                null
            } else {
                JvmSDLGPUBuffer(buffer, this@JvmSDLGPUDevice, createInfo.size)
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
        // LWJGL 3.4.x's SDL_GPUShaderCreateInfo has no setters; fill the
        // struct layout manually (same field layout as SDL3's struct).
        val codeBuffer = MemoryUtil.memAlloc(code.size)
        val entryBuffer = MemoryUtil.memUTF8(entryPoint)
        val info = MemoryUtil.memAlloc(SDL_GPUShaderCreateInfo.SIZEOF)
        try {
            codeBuffer.put(code).rewind()
            info.putLong(SDL_GPUShaderCreateInfo.CODE_SIZE, code.size.toLong())
            info.putLong(SDL_GPUShaderCreateInfo.CODE, MemoryUtil.memAddress(codeBuffer))
            info.putLong(SDL_GPUShaderCreateInfo.ENTRYPOINT, MemoryUtil.memAddress(entryBuffer))
            info.putInt(SDL_GPUShaderCreateInfo.FORMAT, format)
            info.putInt(SDL_GPUShaderCreateInfo.STAGE, stage)
            info.putInt(SDL_GPUShaderCreateInfo.NUM_SAMPLERS, numSamplers)
            info.putInt(SDL_GPUShaderCreateInfo.NUM_STORAGE_TEXTURES, numStorageTextures)
            info.putInt(SDL_GPUShaderCreateInfo.NUM_STORAGE_BUFFERS, numStorageBuffers)
            info.putInt(SDL_GPUShaderCreateInfo.NUM_UNIFORM_BUFFERS, numUniformBuffers)
            info.putInt(SDL_GPUShaderCreateInfo.PROPS, 0)
            info.rewind()
            val shader = SDLGPU.nSDL_CreateGPUShader(check(), MemoryUtil.memAddress(info))
            return if (shader == 0L) {
                null
            } else {
                JvmSDLGPUShader(shader, this@JvmSDLGPUDevice)
            }
        } finally {
            MemoryUtil.memFree(info)
            MemoryUtil.memFree(entryBuffer)
            MemoryUtil.memFree(codeBuffer)
        }
    }

    override fun createGraphicsPipeline(createInfo: SDLGPUGraphicsPipelineCreateInfo): SDLGPUGraphicsPipeline? =
        SDL_GPUGraphicsPipelineCreateInfo.calloc().use { info ->
            val vertex = createInfo.vertexShader as? JvmSDLGPUShader
                ?: throw IllegalArgumentException("vertex shader is not a JVM SDL GPU shader")
            info.vertex_shader(vertex.ptr)
            createInfo.fragmentShader?.let { info.fragment_shader((it as? JvmSDLGPUShader)?.ptr ?: throw IllegalArgumentException("fragment shader is not a JVM SDL GPU shader")) }
            info.primitive_type(createInfo.primitiveType)

            // The nested arrays below are referenced by pointer from `info`;
            // they must stay alive until SDL_CreateGPUGraphicsPipeline returns,
            // so allocate them on the heap and free them afterwards.
            val vbArrays = ArrayList<org.lwjgl.sdl.SDL_GPUVertexBufferDescription.Buffer>()
            val vaArrays = ArrayList<org.lwjgl.sdl.SDL_GPUVertexAttribute.Buffer>()
            val cdArrays = ArrayList<org.lwjgl.sdl.SDL_GPUColorTargetDescription.Buffer>()
            try {
                // vertex input state
                val buffers = createInfo.vertexInputState.vertexBufferDescriptions
                val attributes = createInfo.vertexInputState.vertexAttributes
                info.vertex_input_state { vis ->
                    if (buffers.isNotEmpty()) {
                        val vb = org.lwjgl.sdl.SDL_GPUVertexBufferDescription.calloc(buffers.size)
                        vbArrays.add(vb)
                        for (i in buffers.indices) {
                            vb.get(i).slot(buffers[i].slot).pitch(buffers[i].pitch)
                                .input_rate(buffers[i].inputRate).instance_step_rate(buffers[i].instanceStepRate)
                        }
                        vis.vertex_buffer_descriptions(vb)
                        vis.num_vertex_buffers(buffers.size)
                    }
                    if (attributes.isNotEmpty()) {
                        val va = org.lwjgl.sdl.SDL_GPUVertexAttribute.calloc(attributes.size)
                        vaArrays.add(va)
                        for (i in attributes.indices) {
                            va.get(i).location(attributes[i].location).buffer_slot(attributes[i].bufferSlot)
                                .format(attributes[i].format).offset(attributes[i].offset)
                        }
                        vis.vertex_attributes(va)
                        vis.num_vertex_attributes(attributes.size)
                    }
                }

                // rasterizer state
                info.rasterizer_state { rs ->
                    rs.fill_mode(createInfo.rasterizerState.fillMode)
                        .cull_mode(createInfo.rasterizerState.cullMode)
                        .front_face(createInfo.rasterizerState.frontFace)
                }

                // depth stencil state
                info.depth_stencil_state { ds ->
                    ds.compare_op(createInfo.depthStencilState.compareOp)
                        .enable_depth_test(createInfo.depthStencilState.enableDepthTest)
                        .enable_depth_write(createInfo.depthStencilState.enableDepthWrite)
                }

                // color targets
                val targets = createInfo.targetDescriptions
                if (targets.isNotEmpty()) {
                    val cd = org.lwjgl.sdl.SDL_GPUColorTargetDescription.calloc(targets.size)
                    cdArrays.add(cd)
                    for (i in targets.indices) {
                        val t = targets[i]
                        val blend = t.blendState
                        cd.get(i).format(t.format)
                        cd.get(i).blend_state { b ->
                            // SDL3 requires enable_blend=true for the factors to
                            // take effect; without it output is opaque overwrite,
                            // which makes imgui glyphs render as thick blocks.
                            b.enable_blend(true)
                                .src_color_blendfactor(blend.srcColorBlendFactor)
                                .dst_color_blendfactor(blend.dstColorBlendFactor)
                                .color_blend_op(blend.colorBlendOp)
                                .src_alpha_blendfactor(blend.srcAlphaBlendFactor)
                                .dst_alpha_blendfactor(blend.dstAlphaBlendFactor)
                                .alpha_blend_op(blend.alphaBlendOp)
                                .color_write_mask(blend.colorWriteMask.toByte())
                        }
                    }
                    info.target_info { ti ->
                        ti.color_target_descriptions(cd)
                        ti.num_color_targets(targets.size)
                    }
                }

                val pipeline = SDLGPU.SDL_CreateGPUGraphicsPipeline(check(), info)
                if (pipeline == 0L) null else JvmSDLGPUGraphicsPipeline(pipeline, this@JvmSDLGPUDevice)
            } finally {
                cdArrays.forEach { it.free() }
                vaArrays.forEach { it.free() }
                vbArrays.forEach { it.free() }
            }
        }

    override fun createSampler(createInfo: SDLGPUSamplerCreateInfo): SDLGPUSampler? =
        SDL_GPUSamplerCreateInfo.calloc().use { info ->
            info.min_filter(createInfo.minFilter)
                .mag_filter(createInfo.magFilter)
                .mipmap_mode(createInfo.mipmapMode)
                .address_mode_u(createInfo.addressModeU)
                .address_mode_v(createInfo.addressModeV)
                .address_mode_w(createInfo.addressModeW)
                .max_anisotropy(createInfo.maxAnisotropy)
            val sampler = SDLGPU.SDL_CreateGPUSampler(check(), info)
            if (sampler == 0L) null else JvmSDLGPUSampler(sampler, this@JvmSDLGPUDevice)
        }

    override fun beginCommandBuffer(): SDLGPUCommandBuffer? {
        val cmd = SDLGPU.SDL_AcquireGPUCommandBuffer(check())
        if (cmd == 0L) return null
        return JvmSDLGPUCommandBuffer(cmd, this)
    }

    override fun submit(commandBuffer: SDLGPUCommandBuffer): Boolean {
        val native = commandBuffer as? JvmSDLGPUCommandBuffer
            ?: throw IllegalArgumentException("command buffer is not a JVM SDL GPU command buffer")
        val cmd = native.check()
        val ok = SDLGPU.SDL_SubmitGPUCommandBuffer(cmd)
        if (ok) {
            native.ptrValue = 0L
        }
        return ok
    }

    override fun submitAndAcquireFence(commandBuffer: SDLGPUCommandBuffer): SDLGPUFence? {
        val native = commandBuffer as? JvmSDLGPUCommandBuffer
            ?: throw IllegalArgumentException("command buffer is not a JVM SDL GPU command buffer")
        val cmd = native.check()
        val fence = SDLGPU.SDL_SubmitGPUCommandBufferAndAcquireFence(cmd)
        if (fence == 0L) return null
        native.ptrValue = 0L
        return JvmSDLGPUFence(fence, this)
    }

    override fun waitForFences(fences: List<SDLGPUFence>): Boolean = MemoryStack.stackPush().use { stack ->
        if (fences.isEmpty()) return true
        val buffer = stack.mallocPointer(fences.size)
        for (i in fences.indices) {
            buffer.put(i, (fences[i] as? JvmSDLGPUFence)?.ptr ?: throw IllegalArgumentException("fence is not a JVM SDL GPU fence"))
        }
        SDLGPU.SDL_WaitForGPUFences(check(), true, buffer)
    }

    override fun present(window: SDLWindow) {
        val w = (window as? JvmSDLWindow)?.ptr ?: throw IllegalArgumentException("window is not a JVM SDL window")
        // LWJGL 3.4.x has no SDL_PresentGPUDevice binding; SDL_WaitForGPUSwapchain
        // performs the equivalent present/ready synchronization.
        SDLGPU.SDL_WaitForGPUSwapchain(check(), w)
    }

    override fun waitForIdle(): Boolean = SDLGPU.SDL_WaitForGPUIdle(check())

    internal fun uploadToBuffer(buffer: JvmSDLGPUBuffer, data: ByteArray, offset: Int): Boolean {
        if (offset + data.size > buffer.size) return false
        SDL_GPUTransferBufferCreateInfo.calloc().use { info ->
            info.usage(SDLGPU.SDL_GPU_TRANSFERBUFFERUSAGE_UPLOAD).size(data.size)
            val transfer = SDLGPU.SDL_CreateGPUTransferBuffer(check(), info) ?: return false
            try {
                val mapped = SDLGPU.SDL_MapGPUTransferBuffer(check(), transfer, false, data.size.toLong()) ?: return false
                mapped.put(data).rewind()
                SDLGPU.SDL_UnmapGPUTransferBuffer(check(), transfer)

                val cmd = SDLGPU.SDL_AcquireGPUCommandBuffer(check()) ?: return false
                val pass = SDLGPU.SDL_BeginGPUCopyPass(cmd)
                SDL_GPUTransferBufferLocation.calloc().use { location ->
                    location.transfer_buffer(transfer).offset(0)
                    SDL_GPUBufferRegion.calloc().use { region ->
                        region.buffer(buffer.ptr).offset(offset).size(data.size)
                        SDLGPU.SDL_UploadToGPUBuffer(pass, location, region, false)
                    }
                }
                SDLGPU.SDL_EndGPUCopyPass(pass)
                SDLGPU.SDL_SubmitGPUCommandBuffer(cmd)
                return true
            } finally {
                SDLGPU.SDL_ReleaseGPUTransferBuffer(check(), transfer)
            }
        }
    }

    internal fun uploadToTexture(texture: JvmSDLGPUTexture, data: ByteArray, bytesPerRow: Int, x: Int, y: Int, width: Int, height: Int): Boolean {
        SDL_GPUTransferBufferCreateInfo.calloc().use { info ->
            info.usage(SDLGPU.SDL_GPU_TRANSFERBUFFERUSAGE_UPLOAD).size(data.size)
            val transfer = SDLGPU.SDL_CreateGPUTransferBuffer(check(), info) ?: return false
            try {
                val mapped = SDLGPU.SDL_MapGPUTransferBuffer(check(), transfer, false, data.size.toLong()) ?: return false
                mapped.put(data).rewind()
                SDLGPU.SDL_UnmapGPUTransferBuffer(check(), transfer)

                val cmd = SDLGPU.SDL_AcquireGPUCommandBuffer(check()) ?: return false
                val pass = SDLGPU.SDL_BeginGPUCopyPass(cmd)
                SDL_GPUTextureTransferInfo.calloc().use { src ->
                    // SDL3's pixels_per_row is in pixels, not bytes.
                    val bpp = if (texture.bytesPerPixel > 0) texture.bytesPerPixel else 1
                    src.transfer_buffer(transfer).offset(0).pixels_per_row(bytesPerRow / bpp)
                    SDL_GPUTextureRegion.calloc().use { region ->
                        region.texture(texture.ptr).mip_level(0).layer(0)
                            .x(x).y(y).z(0).w(width).h(height).d(1)
                        SDLGPU.SDL_UploadToGPUTexture(pass, src, region, false)
                    }
                }
                SDLGPU.SDL_EndGPUCopyPass(pass)
                SDLGPU.SDL_SubmitGPUCommandBuffer(cmd)
                return true
            } finally {
                SDLGPU.SDL_ReleaseGPUTransferBuffer(check(), transfer)
            }
        }
    }

    /** Copies the texture's [width]x[height] region back to the CPU (RGBA8), blocking on a fence. */
    internal fun downloadFromTexture(texture: JvmSDLGPUTexture, width: Int, height: Int): ByteArray? {
        val size = width * height * 4
        val result = ByteArray(size)
        SDL_GPUTransferBufferCreateInfo.calloc().use { info ->
            info.usage(SDLGPU.SDL_GPU_TRANSFERBUFFERUSAGE_DOWNLOAD).size(size)
            val transfer = SDLGPU.SDL_CreateGPUTransferBuffer(check(), info) ?: return null
            try {
                val cmd = SDLGPU.SDL_AcquireGPUCommandBuffer(check()) ?: return null
                val pass = SDLGPU.SDL_BeginGPUCopyPass(cmd)
                SDL_GPUTextureTransferInfo.calloc().use { dst ->
                    dst.transfer_buffer(transfer).offset(0).pixels_per_row(width)
                    SDL_GPUTextureRegion.calloc().use { region ->
                        region.texture(texture.ptr).mip_level(0).layer(0)
                            .x(0).y(0).z(0).w(width).h(height).d(1)
                        SDLGPU.SDL_DownloadFromGPUTexture(pass, region, dst)
                    }
                }
                SDLGPU.SDL_EndGPUCopyPass(pass)
                val fence = SDLGPU.SDL_SubmitGPUCommandBufferAndAcquireFence(cmd)
                if (fence == 0L) return null
                try {
                    MemoryStack.stackPush().use { stack ->
                        val fences = stack.mallocPointer(1)
                        fences.put(0, fence)
                        SDLGPU.SDL_WaitForGPUFences(check(), true, fences)
                    }
                    val mapped = SDLGPU.SDL_MapGPUTransferBuffer(check(), transfer, false, size.toLong()) ?: return null
                    try {
                        mapped.get(result)
                    } finally {
                        SDLGPU.SDL_UnmapGPUTransferBuffer(check(), transfer)
                    }
                } finally {
                    SDLGPU.SDL_ReleaseGPUFence(check(), fence)
                }
                return result
            } finally {
                SDLGPU.SDL_ReleaseGPUTransferBuffer(check(), transfer)
            }
        }
    }

    override fun close() {
        val device = ptrValue
        if (device == 0L) return
        ptrValue = 0L
        SDLGPU.SDL_DestroyGPUDevice(device)
    }
}

actual object SDLGPU {

    actual val isSupported: Boolean
        get() = SDLGPU.SDL_GPUSupportsShaderFormats(
            SDLGPUShaderFormat.SPIRV or SDLGPUShaderFormat.MSL or SDLGPUShaderFormat.DXIL or SDLGPUShaderFormat.DXBC or SDLGPUShaderFormat.METALLIB,
            null as CharSequence?,
        )

    actual fun createDevice(debugMode: Boolean): SDLGPUDevice? {
        // format_flags=0 means "any shader format" per the SDL3 docs, but some
        // bundled SDL builds reject it; pass the formats we support explicitly.
        val formats = SDLGPUShaderFormat.SPIRV or SDLGPUShaderFormat.MSL or
            SDLGPUShaderFormat.DXIL or SDLGPUShaderFormat.DXBC or SDLGPUShaderFormat.METALLIB
        val device = SDLGPU.SDL_CreateGPUDevice(formats, debugMode, null as CharSequence?)
        if (device == 0L) return null
        return JvmSDLGPUDevice(device)
    }

    actual val drivers: List<String>
        get() = (0 until SDLGPU.SDL_GetNumGPUDrivers()).mapNotNull { SDLGPU.SDL_GetGPUDriver(it) }
}
