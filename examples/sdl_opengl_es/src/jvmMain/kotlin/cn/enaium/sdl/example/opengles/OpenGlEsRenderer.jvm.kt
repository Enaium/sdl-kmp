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
import org.lwjgl.opengles.GLES
import org.lwjgl.opengles.GLES30

actual fun createGpuRenderer(
    window: SDLWindow,
    width: Int,
    height: Int,
): GpuRenderer = JvmOpenGlEsRenderer(window)

// =========================================================================
// OpenGL ES (LWJGL GLES30; context created through the sdl-kmp GL bindings)
// =========================================================================

private class JvmOpenGlEsRenderer(private val window: SDLWindow) : GpuRenderer {

    private val context: ULong
    private var program = 0

    init {
        SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MAJOR_VERSION, 3)
        SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MINOR_VERSION, 0)
        SDL.glSetAttribute(SDLGLAttribute.CONTEXT_PROFILE_MASK, SDLGLProfile.ES)
        context = SDL.glCreateContext(window.id)
        check(context != 0uL) { "SDL_GL_CreateContext failed: ${SDL.error()}" }
        check(SDL.glMakeCurrent(window.id, context)) { "SDL_GL_MakeCurrent failed: ${SDL.error()}" }
        SDL.glSetSwapInterval(1)

        GLES.createCapabilities()

        val vs = compileShader(
            GLES30.GL_VERTEX_SHADER,
            "#version 300 es\n" +
                "out vec3 vColor;\n" +
                "void main() {\n" +
                "  vec2 p[3] = vec2[3](vec2(-0.75, -0.75), vec2(0.75, -0.75), vec2(0.0, 0.75));\n" +
                "  vec3 c[3] = vec3[3](vec3(1.0, 0.25, 0.35), vec3(0.25, 1.0, 0.35), vec3(0.25, 0.35, 1.0));\n" +
                "  gl_Position = vec4(p[gl_VertexID], 0.0, 1.0);\n" +
                "  vColor = c[gl_VertexID];\n" +
                "}\n",
        )
        val fs = compileShader(
            GLES30.GL_FRAGMENT_SHADER,
            "#version 300 es\n" +
                "precision mediump float;\n" +
                "in vec3 vColor;\n" +
                "layout(location=0) out vec4 c;\n" +
                "void main() { c = vec4(vColor, 1.0); }\n",
        )
        program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vs)
        GLES30.glAttachShader(program, fs)
        GLES30.glLinkProgram(program)
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        check(GLES30.glGetProgrami(program, GLES30.GL_LINK_STATUS) == GLES30.GL_TRUE) {
            "GLES program link failed: ${GLES30.glGetProgramInfoLog(program)}"
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        check(GLES30.glGetShaderi(shader, GLES30.GL_COMPILE_STATUS) == GLES30.GL_TRUE) {
            "GLES shader compile failed: ${GLES30.glGetShaderInfoLog(shader)}"
        }
        return shader
    }

    override fun render(width: Int, height: Int): Boolean {
        GLES30.glViewport(0, 0, width, height)
        GLES30.glClearColor(0.07f, 0.07f, 0.09f, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(program)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
        SDL.glSwapWindow(window.id)
        return true
    }

    override fun close() {
        if (program != 0) GLES30.glDeleteProgram(program)
        SDL.glMakeCurrent(window.id, 0uL)
        SDL.glDestroyContext(context)
    }
}
