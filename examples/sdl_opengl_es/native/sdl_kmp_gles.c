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
 * OpenGL ES gradient triangle demo for the sdl_opengl_es module.
 *
 * The GLES context is created by the caller through the sdl-kmp bindings
 * (SDL_GL_CreateContext with the ES profile); this only loads the GLES entry
 * points through SDL_GL_GetProcAddress, compiles a #version 300 es triangle
 * shader pair and draws it. Minimal GL types are defined here so no GLES
 * development headers are required to build.
 */

#include <stdbool.h>
#include <stdint.h>
#include <string.h>
#include <SDL3/SDL.h>

/* Minimal GLES 2/3 types (avoid depending on GLES dev headers). */
typedef uint32_t GLenum;
typedef uint32_t GLuint;
typedef int32_t GLint;
typedef int32_t GLsizei;
typedef uint32_t GLbitfield;
typedef float GLfloat;

/* GL constants used below (values from <GLES2/gl2.h>). */
#define GL_COMPILE_STATUS 0x8B81
#define GL_VERTEX_SHADER 0x8B31
#define GL_FRAGMENT_SHADER 0x8B30
#define GL_LINK_STATUS 0x8B82
#define GL_COLOR_BUFFER_BIT 0x00004000
#define GL_TRIANGLES 0x0004

typedef void (*GLESfn)(void);

typedef struct GLESTriangleCtx {
    GLuint program;
} GLESTriangleCtx;

/* function pointers */
static GLuint (SDLCALL *kmp_glesCreateShader)(GLenum);
static void (SDLCALL *kmp_glesShaderSource)(GLuint, GLsizei, const char **, const int *);
static void (SDLCALL *kmp_glesCompileShader)(GLuint);
static void (SDLCALL *kmp_glesGetShaderiv)(GLuint, GLenum, GLint *);
static void (SDLCALL *kmp_glesGetShaderInfoLog)(GLuint, GLsizei, GLsizei *, char *);
static GLuint (SDLCALL *kmp_glesCreateProgram)(void);
static void (SDLCALL *kmp_glesAttachShader)(GLuint, GLuint);
static void (SDLCALL *kmp_glesLinkProgram)(GLuint);
static void (SDLCALL *kmp_glesGetProgramiv)(GLuint, GLenum, GLint *);
static void (SDLCALL *kmp_glesGetProgramInfoLog)(GLuint, GLsizei, GLsizei *, char *);
static void (SDLCALL *kmp_glesUseProgram)(GLuint);
static void (SDLCALL *kmp_glesViewport)(GLint, GLint, GLsizei, GLsizei);
static void (SDLCALL *kmp_glesClear)(GLbitfield);
static void (SDLCALL *kmp_glesClearColor)(GLfloat, GLfloat, GLfloat, GLfloat);
static void (SDLCALL *kmp_glesDrawArrays)(GLenum, GLint, GLsizei);
static void (SDLCALL *kmp_glesDeleteProgram)(GLuint);
static void (SDLCALL *kmp_glesDeleteShader)(GLuint);

static GLESfn kmp_glesGet(const char *name)
{
    return (GLESfn)SDL_GL_GetProcAddress(name);
}

static GLuint kmp_gles_compile(GLenum type, const char *source)
{
    GLuint shader = kmp_glesCreateShader(type);
    kmp_glesShaderSource(shader, 1, &source, NULL);
    kmp_glesCompileShader(shader);
    GLint ok = 0;
    kmp_glesGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if (!ok) {
        char log[1024];
        kmp_glesGetShaderInfoLog(shader, sizeof(log), NULL, log);
        SDL_SetError("GLES shader compile failed: %s", log);
        kmp_glesDeleteShader(shader);
        return 0;
    }
    return shader;
}

void *SDL_kmp_GLESTriangleInit(void)
{
    kmp_glesCreateShader = (void *)kmp_glesGet("glCreateShader");
    kmp_glesShaderSource = (void *)kmp_glesGet("glShaderSource");
    kmp_glesCompileShader = (void *)kmp_glesGet("glCompileShader");
    kmp_glesGetShaderiv = (void *)kmp_glesGet("glGetShaderiv");
    kmp_glesGetShaderInfoLog = (void *)kmp_glesGet("glGetShaderInfoLog");
    kmp_glesCreateProgram = (void *)kmp_glesGet("glCreateProgram");
    kmp_glesAttachShader = (void *)kmp_glesGet("glAttachShader");
    kmp_glesLinkProgram = (void *)kmp_glesGet("glLinkProgram");
    kmp_glesGetProgramiv = (void *)kmp_glesGet("glGetProgramiv");
    kmp_glesGetProgramInfoLog = (void *)kmp_glesGet("glGetProgramInfoLog");
    kmp_glesUseProgram = (void *)kmp_glesGet("glUseProgram");
    kmp_glesViewport = (void *)kmp_glesGet("glViewport");
    kmp_glesClear = (void *)kmp_glesGet("glClear");
    kmp_glesClearColor = (void *)kmp_glesGet("glClearColor");
    kmp_glesDrawArrays = (void *)kmp_glesGet("glDrawArrays");
    kmp_glesDeleteProgram = (void *)kmp_glesGet("glDeleteProgram");
    kmp_glesDeleteShader = (void *)kmp_glesGet("glDeleteShader");

    if (!kmp_glesCreateShader || !kmp_glesCreateProgram || !kmp_glesDrawArrays || !kmp_glesClear) {
        SDL_SetError("GLES functions not available");
        return NULL;
    }

    const char *vs_src =
        "#version 300 es\n"
        "out vec3 vColor;\n"
        "void main() {\n"
        "  vec2 p[3] = vec2[3](vec2(-0.75, -0.75), vec2(0.75, -0.75), vec2(0.0, 0.75));\n"
        "  vec3 c[3] = vec3[3](vec3(1.0, 0.25, 0.35), vec3(0.25, 1.0, 0.35), vec3(0.25, 0.35, 1.0));\n"
        "  gl_Position = vec4(p[gl_VertexID], 0.0, 1.0);\n"
        "  vColor = c[gl_VertexID];\n"
        "}\n";
    const char *fs_src =
        "#version 300 es\n"
        "precision mediump float;\n"
        "in vec3 vColor;\n"
        "layout(location=0) out vec4 c;\n"
        "void main() { c = vec4(vColor, 1.0); }\n";

    GLuint vs = kmp_gles_compile(GL_VERTEX_SHADER, vs_src);
    GLuint fs = kmp_gles_compile(GL_FRAGMENT_SHADER, fs_src);
    if (!vs || !fs) {
        if (vs) kmp_glesDeleteShader(vs);
        if (fs) kmp_glesDeleteShader(fs);
        return NULL;
    }

    GLuint program = kmp_glesCreateProgram();
    kmp_glesAttachShader(program, vs);
    kmp_glesAttachShader(program, fs);
    kmp_glesLinkProgram(program);
    kmp_glesDeleteShader(vs);
    kmp_glesDeleteShader(fs);
    GLint ok = 0;
    kmp_glesGetProgramiv(program, GL_LINK_STATUS, &ok);
    if (!ok) {
        char log[1024];
        kmp_glesGetProgramInfoLog(program, sizeof(log), NULL, log);
        SDL_SetError("GLES program link failed: %s", log);
        kmp_glesDeleteProgram(program);
        return NULL;
    }

    GLESTriangleCtx *ctx = (GLESTriangleCtx *)SDL_calloc(1, sizeof(GLESTriangleCtx));
    if (!ctx) {
        kmp_glesDeleteProgram(program);
        return NULL;
    }
    ctx->program = program;
    return ctx;
}

bool SDL_kmp_GLESTriangleRender(void *handle, int width, int height)
{
    GLESTriangleCtx *ctx = (GLESTriangleCtx *)handle;
    if (!ctx) return false;
    kmp_glesViewport(0, 0, width, height);
    kmp_glesClearColor(0.07f, 0.07f, 0.09f, 1.0f);
    kmp_glesClear(GL_COLOR_BUFFER_BIT);
    kmp_glesUseProgram(ctx->program);
    kmp_glesDrawArrays(GL_TRIANGLES, 0, 3);
    return true;
}

void SDL_kmp_GLESTriangleDestroy(void *handle)
{
    GLESTriangleCtx *ctx = (GLESTriangleCtx *)handle;
    if (!ctx) return;
    if (kmp_glesDeleteProgram) kmp_glesDeleteProgram(ctx->program);
    SDL_free(ctx);
}
