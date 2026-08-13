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
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33
import org.lwjgl.opengles.GLES
import org.lwjgl.opengles.GLES30

actual fun createGpuRenderer(
    window: SDLWindow,
    width: Int,
    height: Int,
): GpuRenderer = JvmOpenGlEsRenderer(window)

// =========================================================================
// OpenGL ES (LWJGL GLES30; context created through the sdl-kmp GL bindings)
//
// macOS has no OpenGL ES / EGL support, so there the context falls back to
// desktop GL 3.3 core (LWJGL GL33) with matching GLSL sources.
// =========================================================================

private class JvmOpenGlEsRenderer(private val window: SDLWindow) : GpuRenderer {

    private val context: ULong
    private val gl: GLFns
    private var program = 0
    private var vao = 0

    init {
        // Try an OpenGL ES 3.0 context first, then a desktop GL 3.3 core one.
        var contextValue = createContext(window, es = true)
        val isEs = contextValue != 0uL
        if (contextValue == 0uL) {
            contextValue = createContext(window, es = false)
            check(contextValue != 0uL) { "SDL_GL_CreateContext failed: ${SDL.error()}" }
        }
        context = contextValue
        check(SDL.glMakeCurrent(window.id, context)) { "SDL_GL_MakeCurrent failed: ${SDL.error()}" }
        SDL.glSetSwapInterval(1)

        gl = if (isEs) {
            GLES.createCapabilities()
            GLESFns
        } else {
            GL.createCapabilities()
            GL33Fns
        }

        val version = if (isEs) 300 else 330
        val vs = compileShader(
            gl.createShader(GLES30.GL_VERTEX_SHADER),
            "#version $version ${if (isEs) "es" else "core"}\n" +
                "out vec3 vColor;\n" +
                "void main() {\n" +
                "  vec2 p[3] = vec2[3](vec2(-0.75, -0.75), vec2(0.75, -0.75), vec2(0.0, 0.75));\n" +
                "  vec3 c[3] = vec3[3](vec3(1.0, 0.25, 0.35), vec3(0.25, 1.0, 0.35), vec3(0.25, 0.35, 1.0));\n" +
                "  gl_Position = vec4(p[gl_VertexID], 0.0, 1.0);\n" +
                "  vColor = c[gl_VertexID];\n" +
                "}\n",
        )
        val fs = compileShader(
            gl.createShader(GLES30.GL_FRAGMENT_SHADER),
            "#version $version ${if (isEs) "es" else "core"}\n" +
                (if (isEs) "precision mediump float;\n" else "") +
                "in vec3 vColor;\n" +
                (if (isEs) "layout(location=0) out vec4 c;\n" else "out vec4 c;\n") +
                "void main() { c = vec4(vColor, 1.0); }\n",
        )
        program = gl.createProgram()
        gl.attachShader(program, vs)
        gl.attachShader(program, fs)
        gl.linkProgram(program)
        gl.deleteShader(vs)
        gl.deleteShader(fs)
        check(gl.getProgrami(program, GLES30.GL_LINK_STATUS) == 1) {
            "GL program link failed: ${gl.getProgramInfoLog(program)}"
        }
        // Desktop core profiles reject draws without a bound VAO; ES 3.0
        // supports VAOs too, so bind one on every path.
        vao = gl.genVertexArrays()
        gl.bindVertexArray(vao)
    }

    private fun compileShader(shader: Int, source: String): Int {
        gl.shaderSource(shader, source)
        gl.compileShader(shader)
        check(gl.getShaderi(shader, GLES30.GL_COMPILE_STATUS) == 1) {
            "GL shader compile failed: ${gl.getShaderInfoLog(shader)}"
        }
        return shader
    }

    override fun render(width: Int, height: Int): Boolean {
        gl.viewport(0, 0, width, height)
        gl.clearColor(0.07f, 0.07f, 0.09f, 1.0f)
        gl.clear(GLES30.GL_COLOR_BUFFER_BIT)
        gl.useProgram(program)
        gl.drawArrays(GLES30.GL_TRIANGLES, 0, 3)
        SDL.glSwapWindow(window.id)
        return true
    }

    override fun close() {
        if (program != 0) gl.deleteProgram(program)
        if (vao != 0) gl.deleteVertexArrays(vao)
        SDL.glMakeCurrent(window.id, 0uL)
        SDL.glDestroyContext(context)
    }
}

private fun createContext(window: SDLWindow, es: Boolean): ULong {
    SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MAJOR_VERSION, 3)
    SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MINOR_VERSION, if (es) 0 else 3)
    SDL.glSetAttribute(SDLGLAttribute.CONTEXT_PROFILE_MASK, if (es) SDLGLProfile.ES else SDLGLProfile.CORE)
    val context = SDL.glCreateContext(window.id)
    if (context == 0uL) {
        println("${if (es) "GLES" else "GL"} context creation failed: ${SDL.error()}")
    }
    return context
}

/** GL surface shared by the GLES30 and GL33 paths. */
private interface GLFns {
    fun createShader(type: Int): Int
    fun shaderSource(shader: Int, source: String)
    fun compileShader(shader: Int)
    fun getShaderi(shader: Int, pname: Int): Int
    fun getShaderInfoLog(shader: Int): String
    fun createProgram(): Int
    fun attachShader(program: Int, shader: Int)
    fun linkProgram(program: Int)
    fun getProgrami(program: Int, pname: Int): Int
    fun getProgramInfoLog(program: Int): String
    fun deleteShader(shader: Int)
    fun genVertexArrays(): Int
    fun bindVertexArray(vao: Int)
    fun deleteVertexArrays(vao: Int)
    fun useProgram(program: Int)
    fun viewport(x: Int, y: Int, width: Int, height: Int)
    fun clearColor(r: Float, g: Float, b: Float, a: Float)
    fun clear(mask: Int)
    fun drawArrays(mode: Int, first: Int, count: Int)
    fun deleteProgram(program: Int)
}

private object GLESFns : GLFns {
    override fun createShader(type: Int) = GLES30.glCreateShader(type)
    override fun shaderSource(shader: Int, source: String) = GLES30.glShaderSource(shader, source)
    override fun compileShader(shader: Int) = GLES30.glCompileShader(shader)
    override fun getShaderi(shader: Int, pname: Int) = GLES30.glGetShaderi(shader, pname)
    override fun getShaderInfoLog(shader: Int) = GLES30.glGetShaderInfoLog(shader)
    override fun createProgram() = GLES30.glCreateProgram()
    override fun attachShader(program: Int, shader: Int) = GLES30.glAttachShader(program, shader)
    override fun linkProgram(program: Int) = GLES30.glLinkProgram(program)
    override fun getProgrami(program: Int, pname: Int) = GLES30.glGetProgrami(program, pname)
    override fun getProgramInfoLog(program: Int) = GLES30.glGetProgramInfoLog(program)
    override fun deleteShader(shader: Int) = GLES30.glDeleteShader(shader)
    override fun genVertexArrays() = GLES30.glGenVertexArrays()
    override fun bindVertexArray(vao: Int) = GLES30.glBindVertexArray(vao)
    override fun deleteVertexArrays(vao: Int) = GLES30.glDeleteVertexArrays(vao)
    override fun useProgram(program: Int) = GLES30.glUseProgram(program)
    override fun viewport(x: Int, y: Int, width: Int, height: Int) = GLES30.glViewport(x, y, width, height)
    override fun clearColor(r: Float, g: Float, b: Float, a: Float) = GLES30.glClearColor(r, g, b, a)
    override fun clear(mask: Int) = GLES30.glClear(mask)
    override fun drawArrays(mode: Int, first: Int, count: Int) = GLES30.glDrawArrays(mode, first, count)
    override fun deleteProgram(program: Int) = GLES30.glDeleteProgram(program)
}

private object GL33Fns : GLFns {
    override fun createShader(type: Int) = GL33.glCreateShader(type)
    override fun shaderSource(shader: Int, source: String) = GL33.glShaderSource(shader, source)
    override fun compileShader(shader: Int) = GL33.glCompileShader(shader)
    override fun getShaderi(shader: Int, pname: Int) = GL33.glGetShaderi(shader, pname)
    override fun getShaderInfoLog(shader: Int) = GL33.glGetShaderInfoLog(shader)
    override fun createProgram() = GL33.glCreateProgram()
    override fun attachShader(program: Int, shader: Int) = GL33.glAttachShader(program, shader)
    override fun linkProgram(program: Int) = GL33.glLinkProgram(program)
    override fun getProgrami(program: Int, pname: Int) = GL33.glGetProgrami(program, pname)
    override fun getProgramInfoLog(program: Int) = GL33.glGetProgramInfoLog(program)
    override fun deleteShader(shader: Int) = GL33.glDeleteShader(shader)
    override fun genVertexArrays() = GL33.glGenVertexArrays()
    override fun bindVertexArray(vao: Int) = GL33.glBindVertexArray(vao)
    override fun deleteVertexArrays(vao: Int) = GL33.glDeleteVertexArrays(vao)
    override fun useProgram(program: Int) = GL33.glUseProgram(program)
    override fun viewport(x: Int, y: Int, width: Int, height: Int) = GL33.glViewport(x, y, width, height)
    override fun clearColor(r: Float, g: Float, b: Float, a: Float) = GL33.glClearColor(r, g, b, a)
    override fun clear(mask: Int) = GL33.glClear(mask)
    override fun drawArrays(mode: Int, first: Int, count: Int) = GL33.glDrawArrays(mode, first, count)
    override fun deleteProgram(program: Int) = GL33.glDeleteProgram(program)
}
