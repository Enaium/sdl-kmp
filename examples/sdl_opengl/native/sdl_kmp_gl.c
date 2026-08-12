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

/*
 * OpenGL triangle demo for the sdl_opengl example module.
 *
 * Implemented in C because the GL entry points are a C-function-pointer
 * table that cinterop cannot bind directly. Compiled into a static library
 * by native/CMakeLists.txt and embedded into the sdl_opengl cinterop klib.
 */

#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <SDL3/SDL.h>

/* =========================================================================
 * OpenGL triangle demo helper.
 *
 * Loads the GL 3.3 core / GLES 3 functions through SDL_GL_GetProcAddress
 * (cinterop cannot express these C function-pointer types), compiles a
 * triangle shader pair and draws it on every SDL_kmp_GLTriangleRender
 * call. The GL context itself is created by the caller through the sdl-kmp
 * bindings.
 * ========================================================================= */

#if defined(SDL_PLATFORM_ANDROID)
#include <SDL3/SDL_opengles2.h>
#else
#include <SDL3/SDL_opengl.h>
#endif

typedef void (*GLfn)(void);

typedef struct GLTriangleCtx {
    GLuint program;
    GLuint vao;
    int has_vao;
} GLTriangleCtx;

/* function pointers */
static void (SDLCALL *kmp_glGenBuffers)(GLsizei, GLuint *);
static void (SDLCALL *kmp_glBindBuffer)(GLenum, GLuint);
static void (SDLCALL *kmp_glBufferData)(GLenum, GLsizeiptr, const void *, GLenum);
static GLuint (SDLCALL *kmp_glCreateShader)(GLenum);
static void (SDLCALL *kmp_glShaderSource)(GLuint, GLsizei, const char **, const int *);
static void (SDLCALL *kmp_glCompileShader)(GLuint);
static void (SDLCALL *kmp_glGetShaderiv)(GLuint, GLenum, GLint *);
static void (SDLCALL *kmp_glGetShaderInfoLog)(GLuint, GLsizei, GLsizei *, char *);
static GLuint (SDLCALL *kmp_glCreateProgram)(void);
static void (SDLCALL *kmp_glAttachShader)(GLuint, GLuint);
static void (SDLCALL *kmp_glLinkProgram)(GLuint);
static void (SDLCALL *kmp_glGetProgramiv)(GLuint, GLenum, GLint *);
static void (SDLCALL *kmp_glGetProgramInfoLog)(GLuint, GLsizei, GLsizei *, char *);
static void (SDLCALL *kmp_glUseProgram)(GLuint);
static void (SDLCALL *kmp_glGenVertexArrays)(GLsizei, GLuint *);
static void (SDLCALL *kmp_glBindVertexArray)(GLuint);
static void (SDLCALL *kmp_glViewport)(GLint, GLint, GLsizei, GLsizei);
static void (SDLCALL *kmp_glClear)(GLbitfield);
static void (SDLCALL *kmp_glClearColor)(GLfloat, GLfloat, GLfloat, GLfloat);
static void (SDLCALL *kmp_glDrawArrays)(GLenum, GLint, GLsizei);
static void (SDLCALL *kmp_glDeleteProgram)(GLuint);
static void (SDLCALL *kmp_glDeleteShader)(GLuint);
static void (SDLCALL *kmp_glDeleteVertexArrays)(GLsizei, const GLuint *);

static GLfn kmp_glGet(const char *name)
{
    return (GLfn)SDL_GL_GetProcAddress(name);
}

static GLuint kmp_gl_compile(GLenum type, const char *source)
{
    GLuint shader = kmp_glCreateShader(type);
    kmp_glShaderSource(shader, 1, &source, NULL);
    kmp_glCompileShader(shader);
    GLint ok = 0;
    kmp_glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if (!ok) {
        char log[1024];
        kmp_glGetShaderInfoLog(shader, sizeof(log), NULL, log);
        SDL_SetError("shader compile failed: %s", log);
        kmp_glDeleteShader(shader);
        return 0;
    }
    return shader;
}

void *SDL_kmp_GLTriangleInit(void)
{
    kmp_glGenBuffers = (void *)kmp_glGet("glGenBuffers");
    kmp_glBindBuffer = (void *)kmp_glGet("glBindBuffer");
    kmp_glBufferData = (void *)kmp_glGet("glBufferData");
    kmp_glCreateShader = (void *)kmp_glGet("glCreateShader");
    kmp_glShaderSource = (void *)kmp_glGet("glShaderSource");
    kmp_glCompileShader = (void *)kmp_glGet("glCompileShader");
    kmp_glGetShaderiv = (void *)kmp_glGet("glGetShaderiv");
    kmp_glGetShaderInfoLog = (void *)kmp_glGet("glGetShaderInfoLog");
    kmp_glCreateProgram = (void *)kmp_glGet("glCreateProgram");
    kmp_glAttachShader = (void *)kmp_glGet("glAttachShader");
    kmp_glLinkProgram = (void *)kmp_glGet("glLinkProgram");
    kmp_glGetProgramiv = (void *)kmp_glGet("glGetProgramiv");
    kmp_glGetProgramInfoLog = (void *)kmp_glGet("glGetProgramInfoLog");
    kmp_glUseProgram = (void *)kmp_glGet("glUseProgram");
    kmp_glGenVertexArrays = (void *)kmp_glGet("glGenVertexArrays");
    kmp_glBindVertexArray = (void *)kmp_glGet("glBindVertexArray");
    kmp_glViewport = (void *)kmp_glGet("glViewport");
    kmp_glClear = (void *)kmp_glGet("glClear");
    kmp_glClearColor = (void *)kmp_glGet("glClearColor");
    kmp_glDrawArrays = (void *)kmp_glGet("glDrawArrays");
    kmp_glDeleteProgram = (void *)kmp_glGet("glDeleteProgram");
    kmp_glDeleteShader = (void *)kmp_glGet("glDeleteShader");
    kmp_glDeleteVertexArrays = (void *)kmp_glGet("glDeleteVertexArrays");

    if (!kmp_glCreateShader || !kmp_glCreateProgram || !kmp_glDrawArrays || !kmp_glClear) {
        SDL_SetError("GL3 functions not available");
        return NULL;
    }

    int profile = 0;
    SDL_GL_GetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, &profile);
    int is_es = (profile == SDL_GL_CONTEXT_PROFILE_ES);

    const char *vs_src = is_es
        ? "#version 300 es\nout vec3 vColor;void main(){vec2 p[3]=vec2[3](vec2(-0.75,-0.75),vec2(0.75,-0.75),vec2(0.0,0.75));vec3 c[3]=vec3[3](vec3(1.0,0.25,0.35),vec3(0.25,1.0,0.35),vec3(0.25,0.35,1.0));gl_Position=vec4(p[gl_VertexID],0.0,1.0);vColor=c[gl_VertexID];}\n"
        : "#version 330 core\nout vec3 vColor;void main(){vec2 p[3]=vec2[3](vec2(-0.75,-0.75),vec2(0.75,-0.75),vec2(0.0,0.75));vec3 c[3]=vec3[3](vec3(1.0,0.25,0.35),vec3(0.25,1.0,0.35),vec3(0.25,0.35,1.0));gl_Position=vec4(p[gl_VertexID],0.0,1.0);vColor=c[gl_VertexID];}\n";
    const char *fs_src = is_es
        ? "#version 300 es\nprecision mediump float;in vec3 vColor;layout(location=0)out vec4 c;void main(){c=vec4(vColor,1.0);}\n"
        : "#version 330 core\nin vec3 vColor;out vec4 c;void main(){c=vec4(vColor,1.0);}\n";

    GLuint vs = kmp_gl_compile(GL_VERTEX_SHADER, vs_src);
    GLuint fs = kmp_gl_compile(GL_FRAGMENT_SHADER, fs_src);
    if (!vs || !fs) {
        if (vs) kmp_glDeleteShader(vs);
        if (fs) kmp_glDeleteShader(fs);
        return NULL;
    }

    GLuint program = kmp_glCreateProgram();
    kmp_glAttachShader(program, vs);
    kmp_glAttachShader(program, fs);
    kmp_glLinkProgram(program);
    kmp_glDeleteShader(vs);
    kmp_glDeleteShader(fs);
    GLint ok = 0;
    kmp_glGetProgramiv(program, GL_LINK_STATUS, &ok);
    if (!ok) {
        char log[1024];
        kmp_glGetProgramInfoLog(program, sizeof(log), NULL, log);
        SDL_SetError("program link failed: %s", log);
        kmp_glDeleteProgram(program);
        return NULL;
    }

    GLTriangleCtx *ctx = (GLTriangleCtx *)SDL_calloc(1, sizeof(GLTriangleCtx));
    if (!ctx) {
        kmp_glDeleteProgram(program);
        return NULL;
    }
    ctx->program = program;
    ctx->has_vao = (kmp_glGenVertexArrays && kmp_glBindVertexArray);
    if (ctx->has_vao) {
        kmp_glGenVertexArrays(1, &ctx->vao);
        kmp_glBindVertexArray(ctx->vao);
    }
    return ctx;
}

bool SDL_kmp_GLTriangleRender(void *handle, int width, int height)
{
    GLTriangleCtx *ctx = (GLTriangleCtx *)handle;
    if (!ctx) return false;
    kmp_glViewport(0, 0, width, height);
    kmp_glClearColor(0.07f, 0.07f, 0.09f, 1.0f);
    kmp_glClear(GL_COLOR_BUFFER_BIT);
    kmp_glUseProgram(ctx->program);
    if (ctx->has_vao) kmp_glBindVertexArray(ctx->vao);
    kmp_glDrawArrays(GL_TRIANGLES, 0, 3);
    return true;
}

void SDL_kmp_GLTriangleDestroy(void *handle)
{
    GLTriangleCtx *ctx = (GLTriangleCtx *)handle;
    if (!ctx) return;
    if (kmp_glDeleteProgram) kmp_glDeleteProgram(ctx->program);
    if (ctx->has_vao && kmp_glDeleteVertexArrays) kmp_glDeleteVertexArrays(1, &ctx->vao);
    SDL_free(ctx);
}
