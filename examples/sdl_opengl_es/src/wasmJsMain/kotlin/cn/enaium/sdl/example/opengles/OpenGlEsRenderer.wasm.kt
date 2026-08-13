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

package cn.enaium.sdl.example.opengles

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLGLAttribute
import cn.enaium.sdl.SDLGLProfile
import cn.enaium.sdl.SDLWindow

// GLES triangle helper compiled into the SDL3 wasm module (see
// sdl-kmp/wasm/sdl_wasm_shim.c); exposed as JS globals by sdl_kmp_glue.js.
internal external fun sdl_kmp_GLES_TriangleInit(): Int
internal external fun sdl_kmp_GLES_TriangleRender(program: Int, width: Int, height: Int): Int
internal external fun sdl_kmp_GLES_TriangleDestroy(program: Int)

private class WasmGpuRenderer(
    private val window: SDLWindow,
    private val context: ULong,
    private val program: Int,
) : GpuRenderer {

    override fun render(width: Int, height: Int): Boolean {
        sdl_kmp_GLES_TriangleRender(program, width, height)
        SDL.glSwapWindow(window.id)
        return true
    }

    override fun close() {
        sdl_kmp_GLES_TriangleDestroy(program)
        SDL.glMakeCurrent(window.id, 0uL)
        SDL.glDestroyContext(context)
    }
}

actual fun createGpuRenderer(
    window: SDLWindow,
    width: Int,
    height: Int,
): GpuRenderer {
    // GLES 3.0 context -> WebGL2 in the browser.
    SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MAJOR_VERSION, 3)
    SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MINOR_VERSION, 0)
    SDL.glSetAttribute(SDLGLAttribute.CONTEXT_PROFILE_MASK, SDLGLProfile.ES)

    val context = SDL.glCreateContext(window.id)
    check(context != 0uL) { "SDL_GL_CreateContext failed: ${SDL.error()}" }
    check(SDL.glMakeCurrent(window.id, context)) { "SDL_GL_MakeCurrent failed: ${SDL.error()}" }
    SDL.glSetSwapInterval(1)

    val program = sdl_kmp_GLES_TriangleInit()
    check(program != 0) { "GLES triangle init failed: ${SDL.error()}" }
    return WasmGpuRenderer(window, context, program)
}
