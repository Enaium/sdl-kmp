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

package cn.enaium.sdl.example.vulkan

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLWindow
import cn.enaium.sdl.nativePtr
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import sdlkmpvulkan.SDL_kmp_VulkanTriangleDestroy
import sdlkmpvulkan.SDL_kmp_VulkanTriangleInit
import sdlkmpvulkan.SDL_kmp_VulkanTriangleRender

internal class NativeGpuRenderer(
    private val closeFn: () -> Unit,
    private val renderFn: (Int, Int) -> Boolean,
) : GpuRenderer {

    override fun render(width: Int, height: Int): Boolean = renderFn(width, height)

    override fun close() {
        closeFn()
    }
}

actual fun createGpuRenderer(
    window: SDLWindow,
    width: Int,
    height: Int,
): GpuRenderer = createVulkanRenderer(window)

private fun createVulkanRenderer(window: SDLWindow): GpuRenderer {
    SDL.vulkanLoadLibrary()
    val nativeWindow = window.nativePtr
        ?: throw IllegalStateException("window has no native pointer")
    val handle = VERT_SPV.usePinned { vertPinned ->
        FRAG_SPV.usePinned { fragPinned ->
            SDL_kmp_VulkanTriangleInit(
                nativeWindow,
                vertPinned.addressOf(0).reinterpret(),
                VERT_SPV.size,
                fragPinned.addressOf(0).reinterpret(),
                FRAG_SPV.size,
            )
        }
    }
    check(handle != null) { "Vulkan triangle init failed: ${SDL.error()}" }

    return NativeGpuRenderer(
        closeFn = {
            SDL_kmp_VulkanTriangleDestroy(handle)
            SDL.vulkanUnloadLibrary()
        },
        renderFn = { _, _ -> SDL_kmp_VulkanTriangleRender(handle, nativeWindow) },
    )
}
