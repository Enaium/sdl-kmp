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

// =========================================================================
// GPU constants (values match SDL3's SDL_gpu.h)
// =========================================================================

/** Shader formats (values match SDL3's SDL_GPUShaderFormat). */
object SDLGPUShaderFormat {
    const val INVALID = 0
    const val PRIVATE = 1 shl 0
    const val SPIRV = 1 shl 1
    const val DXBC = 1 shl 2
    const val DXIL = 1 shl 3
    const val MSL = 1 shl 4
    const val METALLIB = 1 shl 5
}

/** Primitive types (values match SDL3's SDL_GPUPrimitiveType). */
object SDLGPUPrimitiveType {
    const val TRIANGLELIST = 0
    const val TRIANGLESTRIP = 1
    const val LINELIST = 2
    const val LINESTRIP = 3
    const val POINTLIST = 4
}

/** Load operations (values match SDL3's SDL_GPULoadOp). */
object SDLGPULoadOp {
    const val LOAD = 0
    const val CLEAR = 1
    const val DONT_CARE = 2
}

/** Store operations (values match SDL3's SDL_GPUStoreOp). */
object SDLGPUStoreOp {
    const val STORE = 0
    const val DONT_CARE = 1
    const val RESOLVE = 2
    const val RESOLVE_AND_STORE = 3
}

/** Index buffer element sizes (values match SDL3's SDL_GPUIndexElementSize). */
object SDLGPUIndexElementSize {
    const val UINT16 = 0
    const val UINT32 = 1
}

/** Texture types (values match SDL3's SDL_GPUTextureType). */
object SDLGPUTextureType {
    const val D2D = 0
    const val D2D_ARRAY = 1
    const val D3D = 2
    const val CUBE = 3
    const val CUBE_ARRAY = 4
}

/**
 * Texture formats (values match SDL3's SDL_GPUTextureFormat).
 *
 * Exposed so callers can create textures without hand-coding the raw enum
 * values (e.g. imgui-kmp's font atlas uses [R8G8B8A8_UNORM]).
 */
object SDLGPUTextureFormat {
    const val A8_UNORM = 1
    const val R8_UNORM = 2
    const val R8G8_UNORM = 3
    const val R8G8B8A8_UNORM = 4
    const val R16_UNORM = 5
    const val R16G16_UNORM = 6
    const val R16G16B16A16_UNORM = 7
    const val R10G10B10A2_UNORM = 8
    const val B5G6R5_UNORM = 9
    const val B5G5R5A1_UNORM = 10
    const val B4G4R4A4_UNORM = 11
    const val B8G8R8A8_UNORM = 12
}

/** Texture usage flags (values match SDL3's SDL_GPUTextureUsageFlags). */
object SDLGPUTextureUsage {
    const val SAMPLE = 1 shl 0
    const val COLOR_TARGET = 1 shl 1
    const val DEPTH_STENCIL_TARGET = 1 shl 2
    const val GRAPHICS_STORAGE_READ = 1 shl 3
    const val GRAPHICS_STORAGE_WRITE = 1 shl 4
    const val COMPUTE_STORAGE_READ = 1 shl 5
    const val COMPUTE_STORAGE_WRITE = 1 shl 6
}

/** Buffer usage flags (values match SDL3's SDL_GPUBufferUsageFlags). */
object SDLGPUBufferUsage {
    const val GRAPHICS_STORAGE_READ = 1 shl 0
    const val COMPUTE_STORAGE_READ = 1 shl 1
    const val COMPUTE_STORAGE_WRITE = 1 shl 2
    const val VERTEX = 1 shl 3
    const val INDEX = 1 shl 4
    const val INDIRECT = 1 shl 5
    const val GRAPHICS_STORAGE_WRITE = 1 shl 6
}

/**
 * Vertex element formats (values match SDL3's SDL_GPUVertexElementFormat).
 *
 * The values must match SDL3 exactly — SDL3 has no single-component BYTE,
 * UBYTE, BYTE_NORM or UBYTE_NORM entries (only the 2/4-component variants),
 * so the numbering here is dense and every entry maps 1:1 to the C enum.
 */
object SDLGPUVertexElementFormat {
    const val INVALID = 0
    const val INT = 1
    const val INT2 = 2
    const val INT3 = 3
    const val INT4 = 4
    const val UINT = 5
    const val UINT2 = 6
    const val UINT3 = 7
    const val UINT4 = 8
    const val FLOAT = 9
    const val FLOAT2 = 10
    const val FLOAT3 = 11
    const val FLOAT4 = 12
    const val BYTE2 = 13
    const val BYTE4 = 14
    const val UBYTE2 = 15
    const val UBYTE4 = 16
    const val BYTE2_NORM = 17
    const val BYTE4_NORM = 18
    const val UBYTE2_NORM = 19
    const val UBYTE4_NORM = 20
    const val SHORT2 = 21
    const val SHORT4 = 22
    const val USHORT2 = 23
    const val USHORT4 = 24
    const val SHORT2_NORM = 25
    const val SHORT4_NORM = 26
    const val USHORT2_NORM = 27
    const val USHORT4_NORM = 28
    const val HALF2 = 29
    const val HALF4 = 30
}

/** Vertex input rates (values match SDL3's SDL_GPUVertexInputRate). */
object SDLGPUVertexInputRate {
    const val VERTEX = 0
    const val INSTANCE = 1
}

/** Cull modes (values match SDL3's SDL_GPUCullMode). */
object SDLGPUCullMode {
    const val NONE = 0
    const val FRONT = 1
    const val BACK = 2
}

/** Fill modes (values match SDL3's SDL_GPUFillMode). */
object SDLGPUFillMode {
    const val FILL = 0
    const val LINE = 1
}

/** Front face winding (values match SDL3's SDL_GPUFrontFace). */
object SDLGPUFrontFace {
    const val COUNTER_CLOCKWISE = 0
    const val CLOCKWISE = 1
}

/** Compare operations (values match SDL3's SDL_GPUCompareOp). */
object SDLGPUCompareOp {
    const val INVALID = 0
    const val NEVER = 1
    const val LESS = 2
    const val EQUAL = 3
    const val LESS_EQUAL = 4
    const val GREATER = 5
    const val NOT_EQUAL = 6
    const val GREATER_EQUAL = 7
    const val ALWAYS = 8
}

/** Blend factors (values match SDL3's SDL_GPUBlendFactor). */
object SDLGPUBlendFactor {
    const val ZERO = 0x1
    const val ONE = 0x2
    const val SRC_COLOR = 0x3
    const val ONE_MINUS_SRC_COLOR = 0x4
    const val DST_COLOR = 0x5
    const val ONE_MINUS_DST_COLOR = 0x6
    const val SRC_ALPHA = 0x7
    const val ONE_MINUS_SRC_ALPHA = 0x8
    const val DST_ALPHA = 0x9
    const val ONE_MINUS_DST_ALPHA = 0xA
    const val SRC_ALPHA_SATURATE = 0xB
    const val CONSTANT_COLOR = 0xC
    const val ONE_MINUS_CONSTANT_COLOR = 0xD
}

/** Blend operations (values match SDL3's SDL_GPUBlendOp). */
object SDLGPUBlendOp {
    const val INVALID = 0
    const val ADD = 1
    const val SUBTRACT = 2
    const val REVERSE_SUBTRACT = 3
    const val MINIMUM = 4
    const val MAXIMUM = 5
}

/** Color write mask bits (values match SDL3's SDL_GPUColorComponentFlags). */
object SDLGPUColorComponent {
    const val R = 1 shl 0
    const val G = 1 shl 1
    const val B = 1 shl 2
    const val A = 1 shl 3
}

/** Texture filters (values match SDL3's SDL_GPUFilter). */
object SDLGPUFilter {
    const val NEAREST = 0
    const val LINEAR = 1
}

/** Sampler mipmap modes (values match SDL3's SDL_GPUSamplerMipmapMode). */
object SDLGPUSamplerMipmapMode {
    const val NEAREST = 0
    const val LINEAR = 1
}

/** Sampler address modes (values match SDL3's SDL_GPUSamplerAddressMode). */
object SDLGPUSamplerAddressMode {
    const val REPEAT = 0
    const val MIRRORED_REPEAT = 1
    const val CLAMP_TO_EDGE = 2
}

/** Swapchain compositions (values match SDL3's SDL_GPUSwapchainComposition). */
object SDLGPUSwapchainComposition {
    const val SDR = 0
    const val SDR_LINEAR = 1
    const val HDR_EXTENDED_LINEAR = 2
    const val HDR10_ST2084 = 3
}

// =========================================================================
// GPU structures
// =========================================================================

/** A blend state for one color target. */
data class SDLGPUBlendState(
    val srcColorBlendFactor: Int = SDLGPUBlendFactor.ONE,
    val dstColorBlendFactor: Int = SDLGPUBlendFactor.ZERO,
    val colorBlendOp: Int = SDLGPUBlendOp.ADD,
    val srcAlphaBlendFactor: Int = SDLGPUBlendFactor.ONE,
    val dstAlphaBlendFactor: Int = SDLGPUBlendFactor.ZERO,
    val alphaBlendOp: Int = SDLGPUBlendOp.ADD,
    val colorWriteMask: Int = SDLGPUColorComponent.R or SDLGPUColorComponent.G or
        SDLGPUColorComponent.B or SDLGPUColorComponent.A,
)

/** A color target description for a graphics pipeline. */
data class SDLGPUColorTargetDescription(
    val format: Int,
    val blendState: SDLGPUBlendState = SDLGPUBlendState(),
)

/** A vertex buffer binding description. */
data class SDLGPUVertexBufferDescription(
    val slot: Int,
    val pitch: Int,
    val inputRate: Int = SDLGPUVertexInputRate.VERTEX,
    val instanceStepRate: Int = 0,
)

/** A vertex attribute binding. */
data class SDLGPUVertexAttribute(
    val location: Int,
    val bufferSlot: Int,
    val format: Int,
    val offset: Int,
)

/** The vertex input state of a graphics pipeline. */
data class SDLGPUVertexInputState(
    val vertexBufferDescriptions: List<SDLGPUVertexBufferDescription> = emptyList(),
    val vertexAttributes: List<SDLGPUVertexAttribute> = emptyList(),
)

/** The rasterizer state of a graphics pipeline. */
data class SDLGPURasterizerState(
    val cullMode: Int = SDLGPUCullMode.NONE,
    val fillMode: Int = SDLGPUFillMode.FILL,
    val frontFace: Int = SDLGPUFrontFace.COUNTER_CLOCKWISE,
)

/** The depth-stencil state of a graphics pipeline. */
data class SDLGPUDepthStencilState(
    val enableDepthTest: Boolean = false,
    val enableDepthWrite: Boolean = false,
    val compareOp: Int = SDLGPUCompareOp.ALWAYS,
)

/** Describes a graphics pipeline to [SDLGPUDevice.createGraphicsPipeline]. */
data class SDLGPUGraphicsPipelineCreateInfo(
    val vertexShader: SDLGPUShader,
    val fragmentShader: SDLGPUShader?,
    val vertexInputState: SDLGPUVertexInputState = SDLGPUVertexInputState(),
    val primitiveType: Int = SDLGPUPrimitiveType.TRIANGLELIST,
    val rasterizerState: SDLGPURasterizerState = SDLGPURasterizerState(),
    val depthStencilState: SDLGPUDepthStencilState = SDLGPUDepthStencilState(),
    val targetDescriptions: List<SDLGPUColorTargetDescription>,
)

/** Describes a texture to [SDLGPUDevice.createTexture]. */
data class SDLGPUTextureCreateInfo(
    val type: Int = SDLGPUTextureType.D2D,
    val format: Int,
    val usage: Int,
    val width: Int,
    val height: Int,
    val layerCountOrDepth: Int = 1,
    val numLevels: Int = 1,
    val sampleCount: Int = 1,
)

/** Describes a buffer to [SDLGPUDevice.createBuffer]. */
data class SDLGPUBufferCreateInfo(
    val usage: Int,
    val size: Int,
)

/** Describes a sampler to [SDLGPUDevice.createSampler]. */
data class SDLGPUSamplerCreateInfo(
    val minFilter: Int = SDLGPUFilter.NEAREST,
    val magFilter: Int = SDLGPUFilter.NEAREST,
    val mipmapMode: Int = SDLGPUSamplerMipmapMode.NEAREST,
    val addressModeU: Int = SDLGPUSamplerAddressMode.CLAMP_TO_EDGE,
    val addressModeV: Int = SDLGPUSamplerAddressMode.CLAMP_TO_EDGE,
    val addressModeW: Int = SDLGPUSamplerAddressMode.CLAMP_TO_EDGE,
    val maxAnisotropy: Float = 0f,
)

/** A color target for a render pass. */
data class SDLGPUColorTargetInfo(
    val texture: SDLGPUTexture,
    val clearColor: SDLColor = SDLColor(0, 0, 0, 0),
    val loadOp: Int = SDLGPULoadOp.CLEAR,
    val storeOp: Int = SDLGPUStoreOp.STORE,
    val layerOrDepthPlane: Int = 0,
    val mipLevel: Int = 0,
)

/** A viewport. */
data class SDLGPUViewport(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val minDepth: Float = 0f,
    val maxDepth: Float = 1f,
)

// =========================================================================
// GPU handles
// =========================================================================

/** A graphics pipeline, see [SDLGPUDevice.createGraphicsPipeline]. */
interface SDLGPUGraphicsPipeline : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long
    override fun close()
}

/** A shader, see [SDLGPUDevice.createShader]. */
interface SDLGPUShader : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long
    override fun close()
}

/** A GPU texture. */
interface SDLGPUTexture : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long

    /** Uploads [data] into a subrectangle of the texture. */
    fun upload(data: ByteArray, bytesPerRow: Int, x: Int, y: Int, width: Int, height: Int): Boolean

    /**
     * Reads back [width]x[height] pixels starting at (0,0) into an RGBA8
     * ByteArray, or null on failure. Blocking; intended for debugging.
     */
    fun download(width: Int, height: Int): ByteArray?

    override fun close()
}

/** A GPU buffer. */
interface SDLGPUBuffer : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long
    val size: Int

    /** Replaces [size] bytes at [offset] with [data]. */
    fun setData(data: ByteArray, offset: Int = 0): Boolean

    override fun close()
}

/** A GPU sampler. */
interface SDLGPUSampler : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long
    override fun close()
}

/** A GPU fence for synchronizing command buffers. */
interface SDLGPUFence : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long
    val signaled: Boolean
    fun wait(timeoutMs: Int): Boolean
    override fun close()
}

/** An active render pass on a [SDLGPUCommandBuffer]. */
interface SDLGPURenderPass : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long

    fun bindGraphicsPipeline(pipeline: SDLGPUGraphicsPipeline)
    fun setViewport(viewport: SDLGPUViewport)
    fun setScissor(x: Int, y: Int, width: Int, height: Int)
    fun bindVertexBuffers(vararg buffers: Pair<SDLGPUBuffer, Int>)
    fun bindIndexBuffer(buffer: SDLGPUBuffer, indexSize: Int = SDLGPUIndexElementSize.UINT16)
    fun bindGraphicsSamplers(slot: Int, vararg samplers: SDLGPUSampler)
    fun bindGraphicsTextures(slot: Int, vararg textures: SDLGPUTexture)

    /**
     * Binds fragment [slot] to the given texture/sampler pairs in a single
     * `SDL_BindGPUFragmentSamplers` call. Unlike calling
     * [bindGraphicsSamplers] and [bindGraphicsTextures] separately, each
     * `SDL_GPUTextureSamplerBinding` is fully populated (both the texture
     * and the sampler), which the Vulkan backend requires.
     */
    fun bindGraphicsTextureSamplers(slot: Int, vararg bindings: Pair<SDLGPUTexture, SDLGPUSampler>)
    fun pushVertexUniformData(slot: Int, data: ByteArray)
    fun drawPrimitives(vertexCount: Int, instanceCount: Int = 1, firstVertex: Int = 0, firstInstance: Int = 0)
    fun drawIndexedPrimitives(indexCount: Int, instanceCount: Int = 1, firstIndex: Int = 0, vertexOffset: Int = 0, firstInstance: Int = 0)

    /** Ends the render pass. */
    fun end()

    override fun close()
}

/** A GPU command buffer. */
interface SDLGPUCommandBuffer : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long

    /** Begins a render pass targeting the given [colorTargets]; returns null on failure. */
    fun beginRenderPass(colorTargets: List<SDLGPUColorTargetInfo>): SDLGPURenderPass?

    fun pushVertexUniformData(slot: Int, data: ByteArray)
    fun pushFragmentUniformData(slot: Int, data: ByteArray)

    /** Ends the command buffer (use [SDLGPUDevice.submit] afterwards). */
    fun end()

    override fun close()
}

/** A swapchain texture acquired for the current frame. */
data class SDLGPUWindowTexture(
    val texture: SDLGPUTexture?,
    val srcRect: SDLRect,
)

/** A GPU device; owns all other GPU objects. */
interface SDLGPUDevice : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long

    /** The shader formats the device supports (see [SDLGPUShaderFormat]). */
    val shaderFormats: Int

    /** Binds the device to [window]; returns `false` on failure. */
    fun claimWindow(window: SDLWindow): Boolean

    /** Releases the window from the device. */
    fun releaseDrawable(window: SDLWindow)

    /** The swapchain texture format of [window], or null on failure. */
    fun getWindowFormat(window: SDLWindow): Int?

    /**
     * Acquires a texture for drawing to [window] within [commandBuffer];
     * returns null when the swapchain is unavailable (in that case the
     * command buffer must still be submitted or cancelled).
     */
    fun acquireSwapchainTexture(commandBuffer: SDLGPUCommandBuffer, window: SDLWindow): SDLGPUWindowTexture?

    /** Acquires a texture for drawing to [window]; returns null on failure. */
    fun acquireSwapchainTexture(window: SDLWindow): SDLGPUWindowTexture?

    /** Creates a texture, or null on failure. */
    fun createTexture(createInfo: SDLGPUTextureCreateInfo): SDLGPUTexture?

    /** Creates a buffer, or null on failure. */
    fun createBuffer(createInfo: SDLGPUBufferCreateInfo): SDLGPUBuffer?

    /** Creates a shader from [code]; [stage] from [SDLGPUShaderStage], [format] from [SDLGPUShaderFormat]. */
    fun createShader(
        code: ByteArray,
        format: Int,
        stage: Int,
        entryPoint: String,
        numSamplers: Int,
        numStorageTextures: Int,
        numStorageBuffers: Int,
        numUniformBuffers: Int,
    ): SDLGPUShader?

    /** Creates a graphics pipeline, or null on failure. */
    fun createGraphicsPipeline(createInfo: SDLGPUGraphicsPipelineCreateInfo): SDLGPUGraphicsPipeline?

    /** Creates a sampler, or null on failure. */
    fun createSampler(createInfo: SDLGPUSamplerCreateInfo = SDLGPUSamplerCreateInfo()): SDLGPUSampler?

    /** Begins a command buffer, or null on failure. */
    fun beginCommandBuffer(): SDLGPUCommandBuffer?

    /** Submits [commandBuffer] to the GPU; returns `false` on failure. */
    fun submit(commandBuffer: SDLGPUCommandBuffer): Boolean

    /** Submits [commandBuffer] and returns a fence signalled when it completes. */
    fun submitAndAcquireFence(commandBuffer: SDLGPUCommandBuffer): SDLGPUFence?

    /** Waits for all [fences]; returns `false` on failure. */
    fun waitForFences(fences: List<SDLGPUFence>): Boolean

    /** Presents the swapchain of [window]. */
    fun present(window: SDLWindow)

    /** Waits until all submitted work completes. */
    fun waitForIdle(): Boolean

    /** Releases all resources and destroys the device. */
    override fun close()
}

/** Shader stages (values match SDL3's SDL_GPUShaderStage). */
object SDLGPUShaderStage {
    const val VERTEX = 0
    const val FRAGMENT = 1
}

/** SDL3 GPU API. */
expect object SDLGPU {
    /** Whether the GPU API is available on this platform. */
    val isSupported: Boolean

    /**
     * Creates a GPU device. [flags] may contain SDL_GPU_DEVICE_DEBUGMODE.
     * Returns null on failure.
     */
    fun createDevice(debugMode: Boolean = false): SDLGPUDevice?

    /** The names of the available GPU drivers. */
    val drivers: List<String>
}

/** Device creation flags. */
object SDLGPUDeviceFlags {
    const val DEBUGMODE = 1 shl 1
    const val VALIDATION = 1 shl 2
}
