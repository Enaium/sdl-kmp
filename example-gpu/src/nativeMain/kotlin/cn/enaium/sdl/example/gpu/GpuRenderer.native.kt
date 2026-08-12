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

package cn.enaium.sdl.example.gpu

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLGLAttribute
import cn.enaium.sdl.SDLGLProfile
import cn.enaium.sdl.SDLWindow
import cn.enaium.sdl.nativePtr
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import sdlkmptriangle.SDL_kmp_GLTriangleDestroy
import sdlkmptriangle.SDL_kmp_GLTriangleInit
import sdlkmptriangle.SDL_kmp_GLTriangleRender
import sdlkmptriangle.SDL_kmp_VulkanTriangleDestroy
import sdlkmptriangle.SDL_kmp_VulkanTriangleInit
import sdlkmptriangle.SDL_kmp_VulkanTriangleRender

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
    api: GpuApi,
    window: SDLWindow,
    width: Int,
    height: Int,
): GpuRenderer = when (api) {
    GpuApi.OPENGL -> createOpenGlRenderer(window, width, height)
    GpuApi.VULKAN -> createVulkanRenderer(window)
}

private fun createOpenGlRenderer(window: SDLWindow, width: Int, height: Int): GpuRenderer {
    // Request a desktop GL 3.3 core context (GLES 3 on Android; the C helper
    // picks the matching GLSL version from the context profile).
    SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MAJOR_VERSION, 3)
    SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MINOR_VERSION, 3)
    SDL.glSetAttribute(SDLGLAttribute.CONTEXT_PROFILE_MASK, SDLGLProfile.CORE)

    val context = SDL.glCreateContext(window.id)
    println("GL context: $context")
    check(context != 0uL) { "SDL_GL_CreateContext failed: ${SDL.error()}" }
    check(SDL.glMakeCurrent(window.id, context)) { "SDL_GL_MakeCurrent failed: ${SDL.error()}" }
    println("GL context made current")
    SDL.glSetSwapInterval(1)

    val handle = SDL_kmp_GLTriangleInit()
    println("GL triangle handle: $handle")
    check(handle != null) { "GL triangle init failed: ${SDL.error()}" }

    return NativeGpuRenderer(
        closeFn = {
            SDL_kmp_GLTriangleDestroy(handle)
            SDL.glMakeCurrent(window.id, 0uL)
            SDL.glDestroyContext(context)
        },
        renderFn = { w, h ->
            val ok = SDL_kmp_GLTriangleRender(handle, w, h)
            SDL.glSwapWindow(window.id)
            ok
        },
    )
}

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
