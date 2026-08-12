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

package cn.enaium.sdl.example.gpu

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLColor
import cn.enaium.sdl.SDLEvent
import cn.enaium.sdl.SDLGPU
import cn.enaium.sdl.SDLGPUBufferCreateInfo
import cn.enaium.sdl.SDLGPUBufferUsage
import cn.enaium.sdl.SDLGPUColorTargetDescription
import cn.enaium.sdl.SDLGPUColorTargetInfo
import cn.enaium.sdl.SDLGPUGraphicsPipelineCreateInfo
import cn.enaium.sdl.SDLGPUPrimitiveType
import cn.enaium.sdl.SDLGPUShaderFormat
import cn.enaium.sdl.SDLGPUShaderStage
import cn.enaium.sdl.SDLGPUVertexAttribute
import cn.enaium.sdl.SDLGPUVertexBufferDescription
import cn.enaium.sdl.SDLGPUVertexElementFormat
import cn.enaium.sdl.SDLGPUVertexInputRate
import cn.enaium.sdl.SDLGPUVertexInputState
import cn.enaium.sdl.SDLGPUViewport
import cn.enaium.sdl.SDLInitFlags
import cn.enaium.sdl.SDLKeycode
import cn.enaium.sdl.SDLPixelFormat
import cn.enaium.sdl.SDLWindowEventType
import cn.enaium.sdl.SDLWindowFlags

/** A triangle vertex: position (x, y, z) followed by color (r, g, b). */
private const val VERTEX_STRIDE = 24 // 6 floats

/** Positions use the SDL_GPU NDC convention (y points down, like Vulkan). */
private fun floatArrayToBytes(f: FloatArray): ByteArray {
    val out = ByteArray(f.size * 4)
    for (i in f.indices) {
        val bits = f[i].toBits()
        out[i * 4] = (bits ushr 0).toByte()
        out[i * 4 + 1] = (bits ushr 8).toByte()
        out[i * 4 + 2] = (bits ushr 16).toByte()
        out[i * 4 + 3] = (bits ushr 24).toByte()
    }
    return out
}

/**
 * Minimal SDL_GPU triangle demo shared by every platform.
 *
 * Uses the SDL3 GPU API (SDLGPU) entirely from commonMain: creates a device,
 * builds shaders (SPIR-V on Vulkan/Android, MSL on Metal/macOS), a graphics
 * pipeline, uploads a small vertex buffer and draws a gradient triangle.
 *
 * Platform entry points only provide `main()` (see jvmMain/nativeMain).
 */
fun runExample() {
    SDL.setMainReady()

    if (!SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
        error("SDL_Init(VIDEO) failed: ${SDL.error()}")
    }
    println("SDL ${SDL.version()} (${SDL.revision()})")
    println("Video driver: ${SDL.getCurrentVideoDriver()}")

    val device = SDLGPU.createDevice() ?: error("SDL_CreateGPUDevice failed: ${SDL.error()}")
        println("GPU drivers: ${SDLGPU.drivers}")
    println("GPU shader formats: 0x${device.shaderFormats.toString(16)}")

    SDL.createWindow(
        title = "sdl-kmp example-gpu",
        width = 800,
        height = 600,
        flags = SDLWindowFlags.RESIZABLE,
    ).use { window ->
        device.use {
            check(device.claimWindow(window)) { "SDL_ClaimWindowForGPUDevice failed: ${SDL.error()}" }
            val windowFormat = device.getWindowFormat(window)
            println("swapchain format: $windowFormat")

            // Pick the shader format supported by this device (macOS -> MSL,
            // Android/Vulkan -> SPIR-V).
            val (vertShader, fragShader) = if ((device.shaderFormats and SDLGPUShaderFormat.MSL) != 0) {
                device.createShader(VERT_MSL.encodeToByteArray(), SDLGPUShaderFormat.MSL, SDLGPUShaderStage.VERTEX, "vs_main", 0, 0, 0, 0) to
                    device.createShader(FRAG_MSL.encodeToByteArray(), SDLGPUShaderFormat.MSL, SDLGPUShaderStage.FRAGMENT, "fs_main", 0, 0, 0, 0)
            } else {
                check((device.shaderFormats and SDLGPUShaderFormat.SPIRV) != 0) {
                    "device supports neither MSL nor SPIR-V (formats=0x${device.shaderFormats.toString(16)})"
                }
                device.createShader(VERT_SPV, SDLGPUShaderFormat.SPIRV, SDLGPUShaderStage.VERTEX, "main", 0, 0, 0, 0) to
                    device.createShader(FRAG_SPV, SDLGPUShaderFormat.SPIRV, SDLGPUShaderStage.FRAGMENT, "main", 0, 0, 0, 0)
            }
            check(vertShader != null && fragShader != null) { "shader creation failed: ${SDL.error()}" }

            val pipeline = device.createGraphicsPipeline(
                SDLGPUGraphicsPipelineCreateInfo(
                    vertexShader = vertShader!!,
                    fragmentShader = fragShader!!,
                    vertexInputState = SDLGPUVertexInputState(
                        vertexBufferDescriptions = listOf(
                            SDLGPUVertexBufferDescription(
                                slot = 0,
                                pitch = VERTEX_STRIDE,
                                inputRate = SDLGPUVertexInputRate.VERTEX,
                            ),
                        ),
                        vertexAttributes = listOf(
                            SDLGPUVertexAttribute(location = 0, bufferSlot = 0, format = SDLGPUVertexElementFormat.FLOAT3, offset = 0),
                            SDLGPUVertexAttribute(location = 1, bufferSlot = 0, format = SDLGPUVertexElementFormat.FLOAT3, offset = 12),
                        ),
                    ),
                    primitiveType = SDLGPUPrimitiveType.TRIANGLELIST,
                    targetDescriptions = listOf(
                        SDLGPUColorTargetDescription(format = windowFormat ?: SDLPixelFormat.RGBA8888),
                    ),
                ),
            )
            check(pipeline != null) { "pipeline creation failed: ${SDL.error()}" }

            // A centered triangle with per-vertex colors (gradient fill).
            val vertexData = floatArrayToBytes(
                floatArrayOf(
                    // position          color
                    -0.75f, -0.75f, 0f, 1f, 0.25f, 0.35f,
                    0.75f, -0.75f, 0f, 0.25f, 1f, 0.35f,
                    0f, 0.75f, 0f, 0.25f, 0.35f, 1f,
                ),
            )
            val vertexBuffer = device.createBuffer(
                SDLGPUBufferCreateInfo(usage = SDLGPUBufferUsage.VERTEX, size = vertexData.size),
            )
            check(vertexBuffer != null) { "vertex buffer creation failed: ${SDL.error()}" }
            check(vertexBuffer.setData(vertexData)) { "vertex buffer upload failed: ${SDL.error()}" }

            var running = true
            var frames = 0
            val start = SDL.getTicks()

            while (running) {
                while (true) {
                    val event = SDL.pollEvent() ?: break
                    when (event) {
                        is SDLEvent.Quit -> running = false
                        is SDLEvent.Window ->
                            if (event.type == SDLWindowEventType.CLOSE_REQUESTED) running = false
                        is SDLEvent.Key ->
                            if (event.down && event.keycode == SDLKeycode.ESCAPE) running = false
                        else -> Unit
                    }
                }

                // ---- frame: acquire a command buffer first, then the
                // swapchain texture inside it (matching SDL's testgpu pattern;
                // the swapchain texture must be acquired per command buffer) ----
                val cmd = device.beginCommandBuffer() ?: break
                val windowTexture = device.acquireSwapchainTexture(cmd, window)
                val targetTexture = windowTexture?.texture
                // viewport in swapchain texture pixels
                val vw = windowTexture?.srcRect?.width ?: window.size.x
                val vh = windowTexture?.srcRect?.height ?: window.size.y
                if (targetTexture != null) {
                    val pass = cmd.beginRenderPass(
                        colorTargets = listOf(
                            SDLGPUColorTargetInfo(
                                texture = targetTexture,
                                clearColor = SDLColor(18, 18, 24, 255),
                            ),
                        ),
                    )
                    if (pass != null) {
                        pass.bindGraphicsPipeline(pipeline)
                        pass.setViewport(
                            SDLGPUViewport(
                                x = 0f,
                                y = 0f,
                                width = vw.toFloat(),
                                height = vh.toFloat(),
                            ),
                        )
                        pass.setScissor(0, 0, vw, vh)
                        pass.bindVertexBuffers(vertexBuffer to 0)
                        pass.drawPrimitives(vertexCount = 3)
                        pass.end()
                        pass.close()
                    }
                    cmd.end()
                }
                device.submit(cmd)
                cmd.close()
                device.present(window)

                frames++
                if (frames % 120 == 0) {
                    val elapsedMs = (SDL.getTicks() - start).toFloat() / 1000f
                    println("fps: ${(frames / elapsedMs).toInt()}")
                }
                SDL.delay(16)
            }
            println("ran $frames frames")

            vertexBuffer.close()
            pipeline.close()
            vertShader.close()
            fragShader.close()
            device.releaseDrawable(window)
        }
    }

    SDL.quit()
}
