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

package cn.enaium.sdl.example.opengl

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLGLAttribute
import cn.enaium.sdl.SDLGLProfile
import cn.enaium.sdl.SDLWindow
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33
import org.lwjgl.system.MemoryStack

actual fun createGpuRenderer(
    window: SDLWindow,
    width: Int,
    height: Int,
): GpuRenderer = JvmOpenGlRenderer(window)

// =========================================================================
// OpenGL (LWJGL GL33; context created through the sdl-kmp GL bindings)
// =========================================================================

private class JvmOpenGlRenderer(private val window: SDLWindow) : GpuRenderer {

    private val context: ULong
    private var program = 0
    private var vao = 0

    init {
        SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MAJOR_VERSION, 3)
        SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MINOR_VERSION, 3)
        SDL.glSetAttribute(SDLGLAttribute.CONTEXT_PROFILE_MASK, SDLGLProfile.CORE)
        context = SDL.glCreateContext(window.id)
        check(context != 0uL) { "SDL_GL_CreateContext failed: ${SDL.error()}" }
        check(SDL.glMakeCurrent(window.id, context)) { "SDL_GL_MakeCurrent failed: ${SDL.error()}" }
        SDL.glSetSwapInterval(1)

        GL.createCapabilities()

        val vs = compileShader(
            GL33.GL_VERTEX_SHADER,
            "#version 330 core\n" +
                "out vec3 vColor;\n" +
                "void main() {\n" +
                "  vec2 p[3] = vec2[3](vec2(-0.75, -0.75), vec2(0.75, -0.75), vec2(0.0, 0.75));\n" +
                "  vec3 c[3] = vec3[3](vec3(1.0, 0.25, 0.35), vec3(0.25, 1.0, 0.35), vec3(0.25, 0.35, 1.0));\n" +
                "  gl_Position = vec4(p[gl_VertexID], 0.0, 1.0);\n" +
                "  vColor = c[gl_VertexID];\n" +
                "}\n",
        )
        val fs = compileShader(
            GL33.GL_FRAGMENT_SHADER,
            "#version 330 core\nin vec3 vColor;\nout vec4 c;\nvoid main() { c = vec4(vColor, 1.0); }\n",
        )
        program = GL33.glCreateProgram()
        GL33.glAttachShader(program, vs)
        GL33.glAttachShader(program, fs)
        GL33.glLinkProgram(program)
        GL33.glDeleteShader(vs)
        GL33.glDeleteShader(fs)
        check(GL33.glGetProgrami(program, GL33.GL_LINK_STATUS) == GL33.GL_TRUE) {
            "GL program link failed: ${GL33.glGetProgramInfoLog(program)}"
        }
        vao = GL33.glGenVertexArrays()
        GL33.glBindVertexArray(vao)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GL33.glCreateShader(type)
        MemoryStack.stackPush().use { stack ->
            GL33.glShaderSource(shader, source)
        }
        GL33.glCompileShader(shader)
        check(GL33.glGetShaderi(shader, GL33.GL_COMPILE_STATUS) == GL33.GL_TRUE) {
            "GL shader compile failed: ${GL33.glGetShaderInfoLog(shader)}"
        }
        return shader
    }

    override fun render(width: Int, height: Int): Boolean {
        GL33.glViewport(0, 0, width, height)
        GL33.glClearColor(0.07f, 0.07f, 0.09f, 1.0f)
        GL33.glClear(GL33.GL_COLOR_BUFFER_BIT)
        GL33.glUseProgram(program)
        GL33.glBindVertexArray(vao)
        GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, 3)
        SDL.glSwapWindow(window.id)
        return true
    }

    override fun close() {
        if (program != 0) GL33.glDeleteProgram(program)
        if (vao != 0) GL33.glDeleteVertexArrays(vao)
        SDL.glMakeCurrent(window.id, 0uL)
        SDL.glDestroyContext(context)
    }
}
