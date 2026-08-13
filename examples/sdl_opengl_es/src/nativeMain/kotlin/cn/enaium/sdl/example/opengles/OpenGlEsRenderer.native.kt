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

package cn.enaium.sdl.example.opengles

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLGLAttribute
import cn.enaium.sdl.SDLGLProfile
import cn.enaium.sdl.SDLWindow
import sdlkmples.SDL_kmp_GLESTriangleDestroy
import sdlkmples.SDL_kmp_GLESTriangleInit
import sdlkmples.SDL_kmp_GLESTriangleRender

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
): GpuRenderer = createOpenGlEsRenderer(window, width, height)

private fun createOpenGlEsRenderer(window: SDLWindow, width: Int, height: Int): GpuRenderer {
    // Request an OpenGL ES 3.0 context (WebGL2 in the browser; EGL/GLES on
    // desktop and mobile). macOS has no OpenGL ES / EGL support, so fall
    // back to a desktop GL 3.3 core context there; the C helper picks the
    // matching GLSL version from the context profile.
    val context = createContext(window, es = true)
        ?: createContext(window, es = false)
        ?: error("SDL_GL_CreateContext failed: ${SDL.error()}")
    println("GLES context: $context")
    check(SDL.glMakeCurrent(window.id, context)) { "SDL_GL_MakeCurrent failed: ${SDL.error()}" }
    println("GLES context made current")
    SDL.glSetSwapInterval(1)

    val handle = SDL_kmp_GLESTriangleInit()
    println("GLES triangle handle: $handle")
    check(handle != null) { "GLES triangle init failed: ${SDL.error()}" }

    return NativeGpuRenderer(
        closeFn = {
            SDL_kmp_GLESTriangleDestroy(handle)
            SDL.glMakeCurrent(window.id, 0uL)
            SDL.glDestroyContext(context)
        },
        renderFn = { w, h ->
            val ok = SDL_kmp_GLESTriangleRender(handle, w, h)
            SDL.glSwapWindow(window.id)
            ok
        },
    )
}

private fun createContext(window: SDLWindow, es: Boolean): ULong? {
    SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MAJOR_VERSION, 3)
    SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MINOR_VERSION, if (es) 0 else 3)
    SDL.glSetAttribute(SDLGLAttribute.CONTEXT_PROFILE_MASK, if (es) SDLGLProfile.ES else SDLGLProfile.CORE)
    val context = SDL.glCreateContext(window.id)
    if (context == 0uL) {
        println("${if (es) "GLES" else "GL"} context creation failed: ${SDL.error()}")
    }
    return context.takeIf { it != 0uL }
}
