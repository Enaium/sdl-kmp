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
 * Flat, handle-based wrapper around SDL3 for Kotlin/Wasm.
 *
 * Kotlin/Wasm has no cinterop and no raw memory access, so this shim hides
 * every SDL pointer behind a 32-bit handle and exposes the whole curated
 * sdl-kmp API as simple functions taking/returning ints, floats and strings.
 * It is compiled with Emscripten together with SDL3; the JS glue layer then
 * marshals Kotlin <-> C (strings, byte arrays, output buffers).
 *
 * String convention:
 *   - return values: SDL-owned strings are returned directly (never freed);
 *     strings the caller must free (clipboard, base path, pref path,
 *     user folder, camera/sensor names) are returned through an explicit
 *     sdl_kmp_Free() contract.
 * Byte-array convention: the JS layer allocates a heap buffer, copies the
 * bytes, passes the pointer, then frees it (via sdl_kmp_Free).
 */

#include <SDL3/SDL.h>
#include <SDL3/SDL_audio.h>
#include <SDL3/SDL_camera.h>
#include <SDL3/SDL_error.h>
#include <SDL3/SDL_events.h>
#include <SDL3/SDL_filesystem.h>
#include <SDL3/SDL_gpu.h>
#include <SDL3/SDL_hints.h>
#include <SDL3/SDL_init.h>
#include <SDL3/SDL_joystick.h>
#include <SDL3/SDL_gamepad.h>
#include <SDL3/SDL_keyboard.h>
#include <SDL3/SDL_log.h>
#include <SDL3/SDL_mouse.h>
#include <SDL3/SDL_mutex.h>
#include <SDL3/SDL_pixels.h>
#include <SDL3/SDL_power.h>
#include <SDL3/SDL_process.h>
#include <SDL3/SDL_properties.h>
#include <SDL3/SDL_render.h>
#include <SDL3/SDL_sensor.h>
#include <SDL3/SDL_surface.h>
#include <SDL3/SDL_thread.h>
#include <SDL3/SDL_timer.h>
#include <SDL3/SDL_touch.h>
#include <SDL3/SDL_video.h>
#include <SDL3/SDL_vulkan.h>
#include <SDL3/SDL_iostream.h>
#include <SDL3/SDL_haptic.h>
#include <SDL3/SDL_keycode.h>

/* SDL_vulkan.h defines VkInstance/VkSurfaceKHR itself; VkPhysicalDevice is
 * only referenced in a prototype, so declare it before the include. */
typedef struct VkPhysicalDevice_T *VkPhysicalDevice;

#include <stdint.h>
#include <string.h>

/* The static SDL_Event used by sdl_kmp_PollEvent. */
static SDL_Event g_kmp_event;

/* =========================================================================
 * Core
 * ========================================================================= */

int sdl_kmp_Init(int flags) { return SDL_Init(flags); }
int sdl_kmp_InitSubSystem(int flags) { return SDL_InitSubSystem(flags); }
void sdl_kmp_QuitSubSystem(int flags) { SDL_QuitSubSystem(flags); }
int sdl_kmp_WasInit(int flags) { return SDL_WasInit(flags); }
void sdl_kmp_Quit(void) { SDL_Quit(); }
const char *sdl_kmp_GetError(void) { return SDL_GetError(); }
void sdl_kmp_ClearError(void) { SDL_ClearError(); }
int sdl_kmp_SetError(const char *msg) { return SDL_SetError("%s", msg); }

int sdl_kmp_GetVersion(void) { return SDL_GetVersion(); }
const char *sdl_kmp_GetRevision(void) { return SDL_GetRevision(); }

unsigned int sdl_kmp_GetTicks(void) { return SDL_GetTicks(); }
int sdl_kmp_PerfCounterHi(void) { return (int)(SDL_GetPerformanceCounter() >> 32); }
int sdl_kmp_PerfCounterLo(void) { return (int)(SDL_GetPerformanceCounter() & 0xFFFFFFFFu); }
int sdl_kmp_PerfFreqHi(void) { return (int)(SDL_GetPerformanceFrequency() >> 32); }
int sdl_kmp_PerfFreqLo(void) { return (int)(SDL_GetPerformanceFrequency() & 0xFFFFFFFFu); }

void sdl_kmp_Delay(int ms) { SDL_Delay(ms); }

int sdl_kmp_SetHint(const char *name, const char *value) { return SDL_SetHint(name, value); }
const char *sdl_kmp_GetHint(const char *name) { return SDL_GetHint(name); }
int sdl_kmp_GetHintBoolean(const char *name, int default_value) { return SDL_GetHintBoolean(name, default_value); }

char *sdl_kmp_GetClipboardText(void) { return SDL_GetClipboardText(); }
int sdl_kmp_SetClipboardText(const char *text) { return SDL_SetClipboardText(text); }
int sdl_kmp_HasClipboardText(void) { return SDL_HasClipboardText(); }

void sdl_kmp_Free(void *ptr) { SDL_free(ptr); }

/* =========================================================================
 * Video drivers / render drivers
 * ========================================================================= */

int sdl_kmp_GetNumVideoDrivers(void) { return SDL_GetNumVideoDrivers(); }
const char *sdl_kmp_GetVideoDriver(int index) { return SDL_GetVideoDriver(index); }
const char *sdl_kmp_GetCurrentVideoDriver(void) { return SDL_GetCurrentVideoDriver(); }
int sdl_kmp_GetNumAudioDrivers(void) { return SDL_GetNumAudioDrivers(); }
const char *sdl_kmp_GetAudioDriver(int index) { return SDL_GetAudioDriver(index); }
const char *sdl_kmp_GetCurrentAudioDriver(void) { return SDL_GetCurrentAudioDriver(); }

int sdl_kmp_GetNumRenderDrivers(void) { return SDL_GetNumRenderDrivers(); }
const char *sdl_kmp_GetRenderDriver(int index) { return SDL_GetRenderDriver(index); }

/* =========================================================================
 * Window
 * ========================================================================= */

int sdl_kmp_CreateWindow(const char *title, int w, int h, int flags_lo, int flags_hi)
{
    Uint64 flags = ((Uint64)(Uint32)flags_hi << 32) | (Uint32)flags_lo;
    SDL_Window *win = SDL_CreateWindow(title, w, h, flags);
    return (int)(intptr_t)win;
}

void sdl_kmp_DestroyWindow(int window) { SDL_DestroyWindow((SDL_Window *)(intptr_t)window); }

unsigned int sdl_kmp_GetWindowID(int window) { return SDL_GetWindowID((SDL_Window *)(intptr_t)window); }
const char *sdl_kmp_GetWindowTitle(int window) { return SDL_GetWindowTitle((SDL_Window *)(intptr_t)window); }
void sdl_kmp_SetWindowTitle(int window, const char *title) { SDL_SetWindowTitle((SDL_Window *)(intptr_t)window, title); }

void sdl_kmp_GetWindowSize(int window, int *w, int *h) { SDL_GetWindowSize((SDL_Window *)(intptr_t)window, w, h); }
void sdl_kmp_SetWindowSize(int window, int w, int h) { SDL_SetWindowSize((SDL_Window *)(intptr_t)window, w, h); }
void sdl_kmp_GetWindowPosition(int window, int *x, int *y) { SDL_GetWindowPosition((SDL_Window *)(intptr_t)window, x, y); }
void sdl_kmp_SetWindowPosition(int window, int x, int y) { SDL_SetWindowPosition((SDL_Window *)(intptr_t)window, x, y); }
void sdl_kmp_GetWindowSizeInPixels(int window, int *w, int *h) { SDL_GetWindowSizeInPixels((SDL_Window *)(intptr_t)window, w, h); }
unsigned int sdl_kmp_GetWindowFlags(int window) { return (unsigned int)SDL_GetWindowFlags((SDL_Window *)(intptr_t)window); }
int sdl_kmp_GetDisplayForWindow(int window) { return SDL_GetDisplayForWindow((SDL_Window *)(intptr_t)window); }
float sdl_kmp_GetWindowOpacity(int window) { return SDL_GetWindowOpacity((SDL_Window *)(intptr_t)window); }
int sdl_kmp_SetWindowOpacity(int window, float opacity) { return SDL_SetWindowOpacity((SDL_Window *)(intptr_t)window, opacity); }

int sdl_kmp_SetWindowFullscreen(int window, int fullscreen) { return SDL_SetWindowFullscreen((SDL_Window *)(intptr_t)window, fullscreen); }
int sdl_kmp_SetWindowBordered(int window, int bordered) { return SDL_SetWindowBordered((SDL_Window *)(intptr_t)window, bordered); }
int sdl_kmp_SetWindowResizable(int window, int resizable) { return SDL_SetWindowResizable((SDL_Window *)(intptr_t)window, resizable); }
int sdl_kmp_SetWindowAlwaysOnTop(int window, int on_top) { return SDL_SetWindowAlwaysOnTop((SDL_Window *)(intptr_t)window, on_top); }

int sdl_kmp_GetWindowMouseGrab(int window) { return SDL_GetWindowMouseGrab((SDL_Window *)(intptr_t)window); }
int sdl_kmp_SetWindowMouseGrab(int window, int grabbed) { return SDL_SetWindowMouseGrab((SDL_Window *)(intptr_t)window, grabbed); }
int sdl_kmp_GetWindowKeyboardGrab(int window) { return SDL_GetWindowKeyboardGrab((SDL_Window *)(intptr_t)window); }
int sdl_kmp_SetWindowKeyboardGrab(int window, int grabbed) { return SDL_SetWindowKeyboardGrab((SDL_Window *)(intptr_t)window, grabbed); }
int sdl_kmp_GetWindowRelativeMouseMode(int window) { return SDL_GetWindowRelativeMouseMode((SDL_Window *)(intptr_t)window); }
int sdl_kmp_SetWindowRelativeMouseMode(int window, int enabled) { return SDL_SetWindowRelativeMouseMode((SDL_Window *)(intptr_t)window, enabled); }

void sdl_kmp_GetWindowMinimumSize(int window, int *w, int *h) { SDL_GetWindowMinimumSize((SDL_Window *)(intptr_t)window, w, h); }
int sdl_kmp_SetWindowMinimumSize(int window, int w, int h) { return SDL_SetWindowMinimumSize((SDL_Window *)(intptr_t)window, w, h); }
void sdl_kmp_GetWindowMaximumSize(int window, int *w, int *h) { SDL_GetWindowMaximumSize((SDL_Window *)(intptr_t)window, w, h); }
int sdl_kmp_SetWindowMaximumSize(int window, int w, int h) { return SDL_SetWindowMaximumSize((SDL_Window *)(intptr_t)window, w, h); }
int sdl_kmp_GetWindowAspectRatio(int window, float *min, float *max) { return SDL_GetWindowAspectRatio((SDL_Window *)(intptr_t)window, min, max); }
int sdl_kmp_SetWindowAspectRatio(int window, float min, float max) { return SDL_SetWindowAspectRatio((SDL_Window *)(intptr_t)window, min, max); }

void sdl_kmp_ShowWindow(int window) { SDL_ShowWindow((SDL_Window *)(intptr_t)window); }
void sdl_kmp_HideWindow(int window) { SDL_HideWindow((SDL_Window *)(intptr_t)window); }
void sdl_kmp_RaiseWindow(int window) { SDL_RaiseWindow((SDL_Window *)(intptr_t)window); }
void sdl_kmp_MaximizeWindow(int window) { SDL_MaximizeWindow((SDL_Window *)(intptr_t)window); }
void sdl_kmp_MinimizeWindow(int window) { SDL_MinimizeWindow((SDL_Window *)(intptr_t)window); }
void sdl_kmp_RestoreWindow(int window) { SDL_RestoreWindow((SDL_Window *)(intptr_t)window); }
int sdl_kmp_FlashWindow(int window, int operation) { return SDL_FlashWindow((SDL_Window *)(intptr_t)window, (SDL_FlashOperation)operation); }

int sdl_kmp_GetWindowSurface(int window) { return (int)(intptr_t)SDL_GetWindowSurface((SDL_Window *)(intptr_t)window); }
int sdl_kmp_SetWindowIcon(int window, int icon_surface)
{
    return SDL_SetWindowIcon((SDL_Window *)(intptr_t)window, (SDL_Surface *)(intptr_t)icon_surface);
}
int sdl_kmp_GetWindowFromID(unsigned int windowID) { return (int)(intptr_t)SDL_GetWindowFromID(windowID); }

/* =========================================================================
 * Displays
 * ========================================================================= */

static SDL_DisplayID *g_displays;
static int g_display_count;

int sdl_kmp_RefreshDisplays(void)
{
    if (g_displays) { SDL_free(g_displays); g_displays = NULL; }
    g_displays = SDL_GetDisplays(&g_display_count);
    return g_display_count;
}
int sdl_kmp_GetDisplayID(int index) { return index >= 0 && index < g_display_count ? (int)g_displays[index] : 0; }
const char *sdl_kmp_GetDisplayName(int index)
{ return index >= 0 && index < g_display_count ? SDL_GetDisplayName(g_displays[index]) : NULL; }
int sdl_kmp_GetPrimaryDisplay(void) { return (int)SDL_GetPrimaryDisplay(); }

void sdl_kmp_GetDisplayBounds2(int index, int *x, int *y, int *w, int *h)
{
    SDL_Rect r;
    if (index < 0 || index >= g_display_count || SDL_GetDisplayBounds(g_displays[index], &r)) { *x = *y = *w = *h = 0; return; }
    *x = r.x; *y = r.y; *w = r.w; *h = r.h;
}

void sdl_kmp_GetDisplayUsableBounds(int index, int *x, int *y, int *w, int *h)
{
    SDL_Rect r;
    if (index < 0 || index >= g_display_count || SDL_GetDisplayUsableBounds(g_displays[index], &r)) { *x = *y = *w = *h = 0; return; }
    *x = r.x; *y = r.y; *w = r.w; *h = r.h;
}

int sdl_kmp_GetDisplayCurrentMode(int index, int *format, int *w, int *h, float *refresh, float *density)
{
    if (index < 0 || index >= g_display_count) return 0;
    const SDL_DisplayMode *m = SDL_GetCurrentDisplayMode(g_displays[index]);
    if (!m) return 0;
    *format = m->format; *w = m->w; *h = m->h; *refresh = m->refresh_rate; *density = m->pixel_density;
    return 1;
}

int sdl_kmp_GetDisplayDesktopMode(int index, int *format, int *w, int *h, float *refresh, float *density)
{
    if (index < 0 || index >= g_display_count) return 0;
    const SDL_DisplayMode *m = SDL_GetDesktopDisplayMode(g_displays[index]);
    if (!m) return 0;
    *format = m->format; *w = m->w; *h = m->h; *refresh = m->refresh_rate; *density = m->pixel_density;
    return 1;
}

/* =========================================================================
 * Renderer
 * ========================================================================= */

int sdl_kmp_CreateRenderer(int window, const char *name)
{
    SDL_Renderer *r = name ? SDL_CreateRenderer((SDL_Window *)(intptr_t)window, name)
                           : SDL_CreateRenderer((SDL_Window *)(intptr_t)window, NULL);
    return (int)(intptr_t)r;
}

const char *sdl_kmp_GetRendererName(int renderer) { return SDL_GetRendererName((SDL_Renderer *)(intptr_t)renderer); }
void sdl_kmp_DestroyRenderer(int renderer) { SDL_DestroyRenderer((SDL_Renderer *)(intptr_t)renderer); }

void sdl_kmp_GetRenderDrawColor(int renderer, int *r, int *g, int *b, int *a)
{
    Uint8 cr, cg, cb, ca;
    if (SDL_GetRenderDrawColor((SDL_Renderer *)(intptr_t)renderer, &cr, &cg, &cb, &ca)) { *r = *g = *b = *a = 0; return; }
    *r = cr; *g = cg; *b = cb; *a = ca;
}
int sdl_kmp_SetRenderDrawColor(int renderer, int r, int g, int b, int a)
{ return SDL_SetRenderDrawColor((SDL_Renderer *)(intptr_t)renderer, (Uint8)r, (Uint8)g, (Uint8)b, (Uint8)a); }

int sdl_kmp_GetRenderOutputSize(int renderer, int *w, int *h) { return SDL_GetRenderOutputSize((SDL_Renderer *)(intptr_t)renderer, w, h); }
int sdl_kmp_GetCurrentRenderOutputSize(int renderer, int *w, int *h) { return SDL_GetCurrentRenderOutputSize((SDL_Renderer *)(intptr_t)renderer, w, h); }

int sdl_kmp_GetRenderViewport(int renderer, int *x, int *y, int *w, int *h)
{
    SDL_Rect r;
    int ok = SDL_GetRenderViewport((SDL_Renderer *)(intptr_t)renderer, &r);
    if (!ok) { *x = *y = *w = *h = 0; return 0; }
    *x = r.x; *y = r.y; *w = r.w; *h = r.h;
    return 1;
}
void sdl_kmp_SetRenderViewport(int renderer, int x, int y, int w, int h)
{
    SDL_Rect r = { x, y, w, h };
    SDL_SetRenderViewport((SDL_Renderer *)(intptr_t)renderer, &r);
}
void sdl_kmp_SetRenderViewportNull(int renderer) { SDL_SetRenderViewport((SDL_Renderer *)(intptr_t)renderer, NULL); }

int sdl_kmp_GetRenderClipRect(int renderer, int *x, int *y, int *w, int *h)
{
    SDL_Rect r;
    int ok = SDL_GetRenderClipRect((SDL_Renderer *)(intptr_t)renderer, &r);
    if (!ok) { *x = *y = *w = *h = 0; return 0; }
    *x = r.x; *y = r.y; *w = r.w; *h = r.h;
    return 1;
}
void sdl_kmp_SetRenderClipRect(int renderer, int x, int y, int w, int h)
{
    SDL_Rect r = { x, y, w, h };
    SDL_SetRenderClipRect((SDL_Renderer *)(intptr_t)renderer, &r);
}
void sdl_kmp_SetRenderClipRectNull(int renderer) { SDL_SetRenderClipRect((SDL_Renderer *)(intptr_t)renderer, NULL); }

void sdl_kmp_GetRenderScale(int renderer, float *sx, float *sy) { SDL_GetRenderScale((SDL_Renderer *)(intptr_t)renderer, sx, sy); }
void sdl_kmp_SetRenderScale(int renderer, float sx, float sy) { SDL_SetRenderScale((SDL_Renderer *)(intptr_t)renderer, sx, sy); }
int sdl_kmp_GetRenderDrawBlendMode(int renderer)
{
    SDL_BlendMode mode = SDL_BLENDMODE_NONE;
    SDL_GetRenderDrawBlendMode((SDL_Renderer *)(intptr_t)renderer, &mode);
    return (int)mode;
}
int sdl_kmp_SetRenderDrawBlendMode(int renderer, int mode) { return SDL_SetRenderDrawBlendMode((SDL_Renderer *)(intptr_t)renderer, (SDL_BlendMode)mode); }

int sdl_kmp_GetRenderVSync(int renderer) { int v; if (SDL_GetRenderVSync((SDL_Renderer *)(intptr_t)renderer, &v)) return 0; return v; }
int sdl_kmp_SetRenderVSync(int renderer, int vsync) { return SDL_SetRenderVSync((SDL_Renderer *)(intptr_t)renderer, vsync); }

int sdl_kmp_GetRenderTarget(int renderer) { return (int)(intptr_t)SDL_GetRenderTarget((SDL_Renderer *)(intptr_t)renderer); }
void sdl_kmp_SetRenderTarget(int renderer, int texture) { SDL_SetRenderTarget((SDL_Renderer *)(intptr_t)renderer, (SDL_Texture *)(intptr_t)texture); }

int sdl_kmp_RenderClear(int renderer) { return SDL_RenderClear((SDL_Renderer *)(intptr_t)renderer); }
void sdl_kmp_RenderPresent(int renderer) { SDL_RenderPresent((SDL_Renderer *)(intptr_t)renderer); }

int sdl_kmp_RenderFillRect(int renderer, float x, float y, float w, float h)
{
    SDL_FRect r = { x, y, w, h };
    return SDL_RenderFillRect((SDL_Renderer *)(intptr_t)renderer, &r);
}
int sdl_kmp_RenderFillRectNull(int renderer) { return SDL_RenderFillRect((SDL_Renderer *)(intptr_t)renderer, NULL); }
int sdl_kmp_RenderRect(int renderer, float x, float y, float w, float h)
{
    SDL_FRect r = { x, y, w, h };
    return SDL_RenderRect((SDL_Renderer *)(intptr_t)renderer, &r);
}
int sdl_kmp_RenderRectNull(int renderer) { return SDL_RenderRect((SDL_Renderer *)(intptr_t)renderer, NULL); }
int sdl_kmp_RenderLine(int renderer, float x1, float y1, float x2, float y2)
{ return SDL_RenderLine((SDL_Renderer *)(intptr_t)renderer, x1, y1, x2, y2); }
int sdl_kmp_RenderPoint(int renderer, float x, float y)
{ return SDL_RenderPoint((SDL_Renderer *)(intptr_t)renderer, x, y); }

int sdl_kmp_RenderPoints(int renderer, const float *xy, int count)
{
    return SDL_RenderPoints((SDL_Renderer *)(intptr_t)renderer, (const SDL_FPoint *)xy, count);
}

int sdl_kmp_CreateTexture(int renderer, int format, int access, int w, int h)
{
    SDL_Texture *t = SDL_CreateTexture((SDL_Renderer *)(intptr_t)renderer, (SDL_PixelFormat)format, access, w, h);
    return (int)(intptr_t)t;
}
int sdl_kmp_CreateTextureFromSurface(int renderer, int surface)
{
    SDL_Texture *t = SDL_CreateTextureFromSurface((SDL_Renderer *)(intptr_t)renderer, (SDL_Surface *)(intptr_t)surface);
    return (int)(intptr_t)t;
}

int sdl_kmp_RenderTexture(int renderer, int texture, int has_src, float sx, float sy, float sw, float sh,
                          int has_dst, float dx, float dy, float dw, float dh)
{
    SDL_FRect src, dst;
    return SDL_RenderTexture((SDL_Renderer *)(intptr_t)renderer, (SDL_Texture *)(intptr_t)texture,
                             has_src ? &(SDL_FRect){ sx, sy, sw, sh } : NULL,
                             has_dst ? &(SDL_FRect){ dx, dy, dw, dh } : NULL);
}

int sdl_kmp_RenderTextureRotated(int renderer, int texture, int has_src, float sx, float sy, float sw, float sh,
                                 int has_dst, float dx, float dy, float dw, float dh,
                                 double angle, int has_center, float cx, float cy, int flip)
{
    SDL_FPoint center;
    return SDL_RenderTextureRotated((SDL_Renderer *)(intptr_t)renderer, (SDL_Texture *)(intptr_t)texture,
                                    has_src ? &(SDL_FRect){ sx, sy, sw, sh } : NULL,
                                    has_dst ? &(SDL_FRect){ dx, dy, dw, dh } : NULL,
                                    angle, has_center ? &(SDL_FPoint){ cx, cy } : NULL, (SDL_FlipMode)flip);
}

int sdl_kmp_RenderTexture9Grid(int renderer, int texture, float sx, float sy, float sw, float sh,
                               float lw, float rw, float th, float bh, float scale,
                               float dx, float dy, float dw, float dh)
{
    SDL_FRect src = { sx, sy, sw, sh }, dst = { dx, dy, dw, dh };
    return SDL_RenderTexture9Grid((SDL_Renderer *)(intptr_t)renderer, (SDL_Texture *)(intptr_t)texture,
                                  &src, lw, rw, th, bh, scale, &dst);
}

int sdl_kmp_RenderGeometry(int renderer, int texture, const float *verts, int num_verts,
                           const int *indices, int num_indices)
{
    /* verts layout: position.xy, color.rgba, tex_coord.xy  (8 floats per SDL_Vertex) */
    return SDL_RenderGeometry((SDL_Renderer *)(intptr_t)renderer, texture ? (SDL_Texture *)(intptr_t)texture : NULL,
                              (const SDL_Vertex *)verts, num_verts,
                              indices ? (const int *)indices : NULL, num_indices);
}

int sdl_kmp_RenderReadPixels(int renderer, int has_rect, int x, int y, int w, int h)
{
    SDL_Rect r = { x, y, w, h };
    SDL_Surface *s = SDL_RenderReadPixels((SDL_Renderer *)(intptr_t)renderer, has_rect ? &r : NULL);
    return (int)(intptr_t)s;
}

int sdl_kmp_SetRenderLogicalPresentation(int renderer, int w, int h, int mode)
{ return SDL_SetRenderLogicalPresentation((SDL_Renderer *)(intptr_t)renderer, w, h, (SDL_RendererLogicalPresentation)mode); }

int sdl_kmp_GetRenderLogicalPresentationRect(int renderer, float *x, float *y, float *w, float *h)
{
    SDL_FRect r;
    int ok = SDL_GetRenderLogicalPresentationRect((SDL_Renderer *)(intptr_t)renderer, &r);
    if (!ok) { *x = *y = *w = *h = 0; return 0; }
    *x = r.x; *y = r.y; *w = r.w; *h = r.h;
    return 1;
}

/* =========================================================================
 * Texture
 * ========================================================================= */

int sdl_kmp_GetTextureFormat(int texture)
{
    SDL_PropertiesID props = SDL_GetTextureProperties((SDL_Texture *)(intptr_t)texture);
    return (int)SDL_GetNumberProperty(props, SDL_PROP_TEXTURE_FORMAT_NUMBER, SDL_PIXELFORMAT_UNKNOWN);
}
int sdl_kmp_GetTextureAccess(int texture)
{
    SDL_PropertiesID props = SDL_GetTextureProperties((SDL_Texture *)(intptr_t)texture);
    return (int)SDL_GetNumberProperty(props, SDL_PROP_TEXTURE_ACCESS_NUMBER, SDL_TEXTUREACCESS_STATIC);
}
void sdl_kmp_GetTextureSize(int texture, float *w, float *h) { SDL_GetTextureSize((SDL_Texture *)(intptr_t)texture, w, h); }

void sdl_kmp_GetTextureColorMod(int texture, int *r, int *g, int *b)
{
    Uint8 cr, cg, cb;
    if (SDL_GetTextureColorMod((SDL_Texture *)(intptr_t)texture, &cr, &cg, &cb)) { *r = *g = *b = 255; return; }
    *r = cr; *g = cg; *b = cb;
}
int sdl_kmp_SetTextureColorMod(int texture, int r, int g, int b)
{ return SDL_SetTextureColorMod((SDL_Texture *)(intptr_t)texture, (Uint8)r, (Uint8)g, (Uint8)b); }
int sdl_kmp_GetTextureAlphaMod(int texture)
{
    Uint8 a = 255;
    SDL_GetTextureAlphaMod((SDL_Texture *)(intptr_t)texture, &a);
    return a;
}
int sdl_kmp_SetTextureAlphaMod(int texture, int a) { return SDL_SetTextureAlphaMod((SDL_Texture *)(intptr_t)texture, (Uint8)a); }
int sdl_kmp_GetTextureBlendMode(int texture)
{
    SDL_BlendMode mode = SDL_BLENDMODE_NONE;
    SDL_GetTextureBlendMode((SDL_Texture *)(intptr_t)texture, &mode);
    return (int)mode;
}
int sdl_kmp_SetTextureBlendMode(int texture, int mode) { return SDL_SetTextureBlendMode((SDL_Texture *)(intptr_t)texture, (SDL_BlendMode)mode); }
int sdl_kmp_GetTextureScaleMode(int texture)
{
    SDL_ScaleMode mode = SDL_SCALEMODE_NEAREST;
    SDL_GetTextureScaleMode((SDL_Texture *)(intptr_t)texture, &mode);
    return (int)mode;
}
int sdl_kmp_SetTextureScaleMode(int texture, int mode) { return SDL_SetTextureScaleMode((SDL_Texture *)(intptr_t)texture, (SDL_ScaleMode)mode); }

int sdl_kmp_UpdateTexture(int texture, int has_rect, int x, int y, int w, int h, const void *pixels, int pitch)
{
    SDL_Rect r = { x, y, w, h };
    return SDL_UpdateTexture((SDL_Texture *)(intptr_t)texture, has_rect ? &r : NULL, pixels, pitch);
}

static void *g_locked_pixels;
int sdl_kmp_LockTexture(int texture, int has_rect, int x, int y, int w, int h)
{
    SDL_Rect r = { x, y, w, h };
    void *pixels = NULL;
    int pitch = 0;
    if (SDL_LockTexture((SDL_Texture *)(intptr_t)texture, has_rect ? &r : NULL, &pixels, &pitch)) return 0;
    g_locked_pixels = pixels;
    return pitch;
}
void *sdl_kmp_LockedPixels(void) { return g_locked_pixels; }
void sdl_kmp_UnlockTexture(int texture) { SDL_UnlockTexture((SDL_Texture *)(intptr_t)texture); }
void sdl_kmp_DestroyTexture(int texture) { SDL_DestroyTexture((SDL_Texture *)(intptr_t)texture); }

/* =========================================================================
 * Pixels
 * ========================================================================= */

const char *sdl_kmp_GetPixelFormatName(int format) { return SDL_GetPixelFormatName((SDL_PixelFormat)format); }
int sdl_kmp_MapRGB(int format, int r, int g, int b)
{ return (int)SDL_MapRGB(SDL_GetPixelFormatDetails((SDL_PixelFormat)format), NULL, (Uint8)r, (Uint8)g, (Uint8)b); }
int sdl_kmp_MapRGBA(int format, int r, int g, int b, int a)
{ return (int)SDL_MapRGBA(SDL_GetPixelFormatDetails((SDL_PixelFormat)format), NULL, (Uint8)r, (Uint8)g, (Uint8)b, (Uint8)a); }
void sdl_kmp_GetRGBA(int format, unsigned int pixel, int *r, int *g, int *b, int *a)
{
    Uint8 cr, cg, cb, ca;
    SDL_GetRGBA(pixel, SDL_GetPixelFormatDetails((SDL_PixelFormat)format), NULL, &cr, &cg, &cb, &ca);
    *r = cr; *g = cg; *b = cb; *a = ca;
}

/* =========================================================================
 * Surface
 * ========================================================================= */

int sdl_kmp_CreateSurface(int w, int h, int format)
{ return (int)(intptr_t)SDL_CreateSurface(w, h, (SDL_PixelFormat)format); }
int sdl_kmp_LoadBMP(const char *path) { return (int)(intptr_t)SDL_LoadBMP(path); }
int sdl_kmp_GetSurfaceWidth(int surface) { return ((SDL_Surface *)(intptr_t)surface)->w; }
int sdl_kmp_GetSurfaceHeight(int surface) { return ((SDL_Surface *)(intptr_t)surface)->h; }
int sdl_kmp_GetSurfaceFormat(int surface) { return (int)((SDL_Surface *)(intptr_t)surface)->format; }
int sdl_kmp_GetSurfaceColorspace(int surface) { return (int)SDL_GetSurfaceColorspace((SDL_Surface *)(intptr_t)surface); }
int sdl_kmp_GetSurfacePitch(int surface) { return ((SDL_Surface *)(intptr_t)surface)->pitch; }
void *sdl_kmp_GetSurfacePixels(int surface) { return ((SDL_Surface *)(intptr_t)surface)->pixels; }
int sdl_kmp_LockSurface(int surface) { return SDL_LockSurface((SDL_Surface *)(intptr_t)surface); }
void sdl_kmp_UnlockSurface(int surface) { SDL_UnlockSurface((SDL_Surface *)(intptr_t)surface); }

int sdl_kmp_FillSurfaceRect(int surface, int has_rect, int x, int y, int w, int h, unsigned int color)
{
    SDL_Rect r = { x, y, w, h };
    return SDL_FillSurfaceRect((SDL_Surface *)(intptr_t)surface, has_rect ? &r : NULL, color);
}
int sdl_kmp_FillSurfaceRects(int surface, const int *rects, int count, unsigned int color)
{
    return SDL_FillSurfaceRects((SDL_Surface *)(intptr_t)surface, (const SDL_Rect *)rects, count, color);
}

int sdl_kmp_BlitSurface(int src, int has_src, int sx, int sy, int sw, int sh,
                        int dst, int has_dst, int dx, int dy, int dw, int dh)
{
    SDL_Rect s = { sx, sy, sw, sh }, d = { dx, dy, dw, dh };
    return SDL_BlitSurface((SDL_Surface *)(intptr_t)src, has_src ? &s : NULL,
                           (SDL_Surface *)(intptr_t)dst, has_dst ? &d : NULL);
}
int sdl_kmp_BlitSurfaceScaled(int src, int has_src, int sx, int sy, int sw, int sh,
                              int dst, int has_dst, int dx, int dy, int dw, int dh, int scale_mode)
{
    SDL_Rect s = { sx, sy, sw, sh }, d = { dx, dy, dw, dh };
    return SDL_BlitSurfaceScaled((SDL_Surface *)(intptr_t)src, has_src ? &s : NULL,
                                 (SDL_Surface *)(intptr_t)dst, has_dst ? &d : NULL, (SDL_ScaleMode)scale_mode);
}

int sdl_kmp_SaveBMP(int surface, const char *path) { return SDL_SaveBMP((SDL_Surface *)(intptr_t)surface, path); }
int sdl_kmp_ConvertSurface(int surface, int format)
{ return (int)(intptr_t)SDL_ConvertSurface((SDL_Surface *)(intptr_t)surface, (SDL_PixelFormat)format); }
void sdl_kmp_DestroySurface(int surface) { SDL_DestroySurface((SDL_Surface *)(intptr_t)surface); }

/* =========================================================================
 * Events
 * ========================================================================= */

int sdl_kmp_PollEvent(void)
{
    return SDL_PollEvent(&g_kmp_event) ? (int)g_kmp_event.type : 0;
}
int sdl_kmp_WaitEvent(void)
{
    return SDL_WaitEvent(&g_kmp_event) ? (int)g_kmp_event.type : 0;
}
void sdl_kmp_PumpEvents(void) { SDL_PumpEvents(); }

unsigned int sdl_kmp_EventTimestampLo(void) { return (unsigned int)(g_kmp_event.common.timestamp & 0xFFFFFFFFu); }
unsigned int sdl_kmp_EventTimestampHi(void) { return (unsigned int)(g_kmp_event.common.timestamp >> 32); }

int sdl_kmp_EventWindowID(void)
{
    switch (g_kmp_event.type) {
        case SDL_EVENT_WINDOW_SHOWN: case SDL_EVENT_WINDOW_HIDDEN: case SDL_EVENT_WINDOW_EXPOSED:
        case SDL_EVENT_WINDOW_MOVED: case SDL_EVENT_WINDOW_RESIZED: case SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED:
        case SDL_EVENT_WINDOW_MINIMIZED: case SDL_EVENT_WINDOW_MAXIMIZED: case SDL_EVENT_WINDOW_RESTORED:
        case SDL_EVENT_WINDOW_MOUSE_ENTER: case SDL_EVENT_WINDOW_MOUSE_LEAVE:
        case SDL_EVENT_WINDOW_FOCUS_GAINED: case SDL_EVENT_WINDOW_FOCUS_LOST:
        case SDL_EVENT_WINDOW_CLOSE_REQUESTED: case SDL_EVENT_WINDOW_HIT_TEST:
        case SDL_EVENT_WINDOW_ICCPROF_CHANGED: case SDL_EVENT_WINDOW_DISPLAY_CHANGED:
        case SDL_EVENT_WINDOW_DISPLAY_SCALE_CHANGED: case SDL_EVENT_WINDOW_SAFE_AREA_CHANGED:
        case SDL_EVENT_WINDOW_OCCLUDED: case SDL_EVENT_WINDOW_ENTER_FULLSCREEN:
        case SDL_EVENT_WINDOW_LEAVE_FULLSCREEN: case SDL_EVENT_WINDOW_DESTROYED:
        case SDL_EVENT_WINDOW_HDR_STATE_CHANGED:
            return (int)g_kmp_event.window.windowID;
        case SDL_EVENT_KEY_DOWN: case SDL_EVENT_KEY_UP:
            return (int)g_kmp_event.key.windowID;
        case SDL_EVENT_TEXT_INPUT:
            return (int)g_kmp_event.text.windowID;
        case SDL_EVENT_MOUSE_MOTION:
            return (int)g_kmp_event.motion.windowID;
        case SDL_EVENT_MOUSE_BUTTON_DOWN: case SDL_EVENT_MOUSE_BUTTON_UP:
            return (int)g_kmp_event.button.windowID;
        case SDL_EVENT_MOUSE_WHEEL:
            return (int)g_kmp_event.wheel.windowID;
        case SDL_EVENT_DROP_BEGIN: case SDL_EVENT_DROP_FILE: case SDL_EVENT_DROP_TEXT:
        case SDL_EVENT_DROP_COMPLETE: case SDL_EVENT_DROP_POSITION:
            return (int)g_kmp_event.drop.windowID;
        default:
            return 0;
    }
}
int sdl_kmp_EventData1(void) { return g_kmp_event.window.data1; }
int sdl_kmp_EventData2(void) { return g_kmp_event.window.data2; }

int sdl_kmp_EventKeyDown(void) { return g_kmp_event.key.down; }
int sdl_kmp_EventRepeat(void) { return g_kmp_event.key.repeat; }
int sdl_kmp_EventKeycode(void) { return (int)g_kmp_event.key.key; }
int sdl_kmp_EventScancode(void) { return (int)g_kmp_event.key.scancode; }
int sdl_kmp_EventMod(void) { return (int)g_kmp_event.key.mod; }
const char *sdl_kmp_EventText(void) { return g_kmp_event.text.text; }

float sdl_kmp_EventMouseX(void) { return g_kmp_event.motion.x; }
float sdl_kmp_EventMouseY(void) { return g_kmp_event.motion.y; }
float sdl_kmp_EventMouseDX(void) { return g_kmp_event.motion.xrel; }
float sdl_kmp_EventMouseDY(void) { return g_kmp_event.motion.yrel; }

int sdl_kmp_EventButton(void) { return g_kmp_event.button.button; }
int sdl_kmp_EventButtonDown(void) { return g_kmp_event.button.down; }
int sdl_kmp_EventClicks(void) { return g_kmp_event.button.clicks; }
float sdl_kmp_EventButtonX(void) { return g_kmp_event.button.x; }
float sdl_kmp_EventButtonY(void) { return g_kmp_event.button.y; }

float sdl_kmp_EventWheelX(void) { return g_kmp_event.wheel.x; }
float sdl_kmp_EventWheelY(void) { return g_kmp_event.wheel.y; }
int sdl_kmp_EventWheelDir(void) { return g_kmp_event.wheel.direction; }

int sdl_kmp_EventDisplayID(void) { return g_kmp_event.display.displayID; }

const char *sdl_kmp_EventDropData(void) { return g_kmp_event.drop.data; }

int sdl_kmp_EventDeviceID(void) { return (int)g_kmp_event.jdevice.which; }
int sdl_kmp_EventAxis(void) { return g_kmp_event.jaxis.axis; }
short sdl_kmp_EventAxisValue(void) { return g_kmp_event.jaxis.value; }
int sdl_kmp_EventBall(void) { return g_kmp_event.jball.ball; }
int sdl_kmp_EventBallDX(void) { return g_kmp_event.jball.xrel; }
int sdl_kmp_EventBallDY(void) { return g_kmp_event.jball.yrel; }
int sdl_kmp_EventHat(void) { return g_kmp_event.jhat.hat; }
int sdl_kmp_EventHatValue(void) { return g_kmp_event.jhat.value; }

int sdl_kmp_EventTouchpad(void) { return g_kmp_event.gtouchpad.touchpad; }
int sdl_kmp_EventFinger(void) { return g_kmp_event.gtouchpad.finger; }
int sdl_kmp_EventFingerDown(void) { return g_kmp_event.type == SDL_EVENT_GAMEPAD_TOUCHPAD_DOWN; }
float sdl_kmp_EventFingerX(void) { return g_kmp_event.gtouchpad.x; }
float sdl_kmp_EventFingerY(void) { return g_kmp_event.gtouchpad.y; }
float sdl_kmp_EventFingerPressure(void) { return g_kmp_event.gtouchpad.pressure; }

int sdl_kmp_EventSensorType(void) { return g_kmp_event.gsensor.sensor; }
float sdl_kmp_EventSensorData(int idx) { return idx >= 0 && idx < 3 ? g_kmp_event.gsensor.data[idx] : 0.0f; }

int sdl_kmp_EventBatteryState(void) { return g_kmp_event.jbattery.state; }
int sdl_kmp_EventBatteryPercent(void) { return g_kmp_event.jbattery.percent; }

int sdl_kmp_EventTouchIDLo(void) { return (int)(g_kmp_event.tfinger.touchID & 0xFFFFFFFFu); }
int sdl_kmp_EventTouchIDHi(void) { return (int)(g_kmp_event.tfinger.touchID >> 32); }
int sdl_kmp_EventFingerIDLo(void) { return (int)(g_kmp_event.tfinger.fingerID & 0xFFFFFFFFu); }
int sdl_kmp_EventFingerIDHi(void) { return (int)(g_kmp_event.tfinger.fingerID >> 32); }
float sdl_kmp_EventTouchX(void) { return g_kmp_event.tfinger.x; }
float sdl_kmp_EventTouchY(void) { return g_kmp_event.tfinger.y; }
float sdl_kmp_EventTouchDX(void) { return g_kmp_event.tfinger.dx; }
float sdl_kmp_EventTouchDY(void) { return g_kmp_event.tfinger.dy; }
float sdl_kmp_EventTouchPressure(void) { return g_kmp_event.tfinger.pressure; }

int sdl_kmp_EventAudioCapture(void) { return g_kmp_event.adevice.recording; }

int sdl_kmp_EventClipboardOwner(void) { return g_kmp_event.clipboard.owner; }

int sdl_kmp_EventSetEventEnabled(int type, int enabled) { SDL_SetEventEnabled((SDL_EventType)type, enabled); return 0; }
int sdl_kmp_EventEnabled(int type) { return SDL_EventEnabled((SDL_EventType)type); }
void sdl_kmp_FlushEvents(int min_type, int max_type) { SDL_FlushEvents((SDL_EventType)min_type, (SDL_EventType)max_type); }

/* =========================================================================
 * Audio
 * ========================================================================= */

static SDL_AudioDeviceID *g_audio_playback;
static int g_audio_playback_count;
static SDL_AudioDeviceID *g_audio_recording;
static int g_audio_recording_count;

int sdl_kmp_RefreshAudioDevices(void)
{
    if (g_audio_playback) { SDL_free(g_audio_playback); g_audio_playback = NULL; }
    if (g_audio_recording) { SDL_free(g_audio_recording); g_audio_recording = NULL; }
    g_audio_playback = SDL_GetAudioPlaybackDevices(&g_audio_playback_count);
    g_audio_recording = SDL_GetAudioRecordingDevices(&g_audio_recording_count);
    return g_audio_playback_count + g_audio_recording_count;
}
int sdl_kmp_GetAudioPlaybackCount(void) { return g_audio_playback_count; }
int sdl_kmp_GetAudioRecordingCount(void) { return g_audio_recording_count; }
int sdl_kmp_GetAudioPlaybackDevice(int index) { return index >= 0 && index < g_audio_playback_count ? (int)g_audio_playback[index] : 0; }
int sdl_kmp_GetAudioRecordingDevice(int index) { return index >= 0 && index < g_audio_recording_count ? (int)g_audio_recording[index] : 0; }
const char *sdl_kmp_GetAudioDeviceName(int device) { return SDL_GetAudioDeviceName((SDL_AudioDeviceID)device); }
int sdl_kmp_GetAudioDeviceFormat2(int device, int *format, int *channels, int *freq)
{
    SDL_AudioSpec spec;
    if (SDL_GetAudioDeviceFormat((SDL_AudioDeviceID)device, &spec, 0)) return 0;
    *format = spec.format; *channels = spec.channels; *freq = spec.freq;
    return 1;
}

int sdl_kmp_OpenAudioDevice(int device, int format, int channels, int freq)
{
    SDL_AudioSpec spec;
    spec.format = format; spec.channels = channels; spec.freq = freq;
    return (int)SDL_OpenAudioDevice((SDL_AudioDeviceID)device, &spec);
}
int sdl_kmp_OpenAudioDeviceStream(int device, int format, int channels, int freq)
{
    SDL_AudioSpec spec;
    spec.format = format; spec.channels = channels; spec.freq = freq;
    SDL_AudioStream *s = SDL_OpenAudioDeviceStream((SDL_AudioDeviceID)device, &spec, NULL, NULL);
    return (int)(intptr_t)s;
}
int sdl_kmp_CreateAudioStream(int sfmt, int sch, int sfreq, int dfmt, int dch, int dfreq)
{
    SDL_AudioSpec src = { sfmt, sch, sfreq }, dst = { dfmt, dch, dfreq };
    SDL_AudioStream *s = SDL_CreateAudioStream(&src, &dst);
    return (int)(intptr_t)s;
}

int sdl_kmp_AudioDevicePaused(int device) { return SDL_AudioDevicePaused((SDL_AudioDeviceID)device); }
void sdl_kmp_PauseAudioDevice(int device) { SDL_PauseAudioDevice((SDL_AudioDeviceID)device); }
void sdl_kmp_ResumeAudioDevice(int device) { SDL_ResumeAudioDevice((SDL_AudioDeviceID)device); }
int sdl_kmp_BindAudioStream(int device, int stream)
{ return SDL_BindAudioStream((SDL_AudioDeviceID)device, (SDL_AudioStream *)(intptr_t)stream); }
void sdl_kmp_UnbindAudioStream(int stream) { SDL_UnbindAudioStream((SDL_AudioStream *)(intptr_t)stream); }
void sdl_kmp_CloseAudioDevice(int device) { SDL_CloseAudioDevice((SDL_AudioDeviceID)device); }
void sdl_kmp_DestroyAudioStream(int stream) { SDL_DestroyAudioStream((SDL_AudioStream *)(intptr_t)stream); }

int sdl_kmp_PutAudioStreamData(int stream, const void *data, int len)
{ return SDL_PutAudioStreamData((SDL_AudioStream *)(intptr_t)stream, data, len); }
int sdl_kmp_GetAudioStreamData(int stream, void *buf, int len)
{ return (int)SDL_GetAudioStreamData((SDL_AudioStream *)(intptr_t)stream, buf, len); }
int sdl_kmp_GetAudioStreamAvailable(int stream) { return SDL_GetAudioStreamAvailable((SDL_AudioStream *)(intptr_t)stream); }
int sdl_kmp_GetAudioStreamQueued(int stream) { return SDL_GetAudioStreamQueued((SDL_AudioStream *)(intptr_t)stream); }

int sdl_kmp_GetAudioStreamFormat(int stream, int *sfmt, int *sch, int *sfreq, int *dfmt, int *dch, int *dfreq)
{
    SDL_AudioSpec src, dst;
    if (SDL_GetAudioStreamFormat((SDL_AudioStream *)(intptr_t)stream, &src, &dst)) return 0;
    *sfmt = src.format; *sch = src.channels; *sfreq = src.freq;
    *dfmt = dst.format; *dch = dst.channels; *dfreq = dst.freq;
    return 1;
}
int sdl_kmp_SetAudioStreamFormat(int stream, int sfmt, int sch, int sfreq, int dfmt, int dch, int dfreq)
{
    SDL_AudioSpec src = { sfmt, sch, sfreq }, dst = { dfmt, dch, dfreq };
    return SDL_SetAudioStreamFormat((SDL_AudioStream *)(intptr_t)stream, &src, &dst);
}
float sdl_kmp_GetAudioStreamGain(int stream) { return SDL_GetAudioStreamGain((SDL_AudioStream *)(intptr_t)stream); }
int sdl_kmp_SetAudioStreamGain(int stream, float gain) { return SDL_SetAudioStreamGain((SDL_AudioStream *)(intptr_t)stream, gain); }
float sdl_kmp_GetAudioStreamFrequencyRatio(int stream) { return SDL_GetAudioStreamFrequencyRatio((SDL_AudioStream *)(intptr_t)stream); }
int sdl_kmp_SetAudioStreamFrequencyRatio(int stream, float ratio) { return SDL_SetAudioStreamFrequencyRatio((SDL_AudioStream *)(intptr_t)stream, ratio); }
int sdl_kmp_GetAudioStreamDevicePaused(int stream)
{ return SDL_AudioDevicePaused(SDL_GetAudioStreamDevice((SDL_AudioStream *)(intptr_t)stream)); }
void sdl_kmp_PauseAudioStreamDevice(int stream) { SDL_PauseAudioStreamDevice((SDL_AudioStream *)(intptr_t)stream); }
void sdl_kmp_ResumeAudioStreamDevice(int stream) { SDL_ResumeAudioStreamDevice((SDL_AudioStream *)(intptr_t)stream); }
int sdl_kmp_FlushAudioStream(int stream) { return SDL_FlushAudioStream((SDL_AudioStream *)(intptr_t)stream); }
int sdl_kmp_ClearAudioStream(int stream) { return SDL_ClearAudioStream((SDL_AudioStream *)(intptr_t)stream); }

/* WAV loading into static storage */
static SDL_AudioSpec g_wav_spec;
static Uint8 *g_wav_data;
static Uint32 g_wav_len;
int sdl_kmp_LoadWAV(const char *path)
{
    if (g_wav_data) { SDL_free(g_wav_data); g_wav_data = NULL; }
    Uint8 *buf = NULL; Uint32 len = 0;
    if (!SDL_LoadWAV(path, &g_wav_spec, &buf, &len)) return 0;
    g_wav_data = buf; g_wav_len = len;
    return 1;
}
int sdl_kmp_LoadWAVFormat(void) { return g_wav_spec.format; }
int sdl_kmp_LoadWAVChannels(void) { return g_wav_spec.channels; }
int sdl_kmp_LoadWAVFreq(void) { return g_wav_spec.freq; }
int sdl_kmp_LoadWAVLen(void) { return (int)g_wav_len; }
void *sdl_kmp_LoadWAVData(void) { return g_wav_data; }
void sdl_kmp_LoadWAVFree(void) { if (g_wav_data) { SDL_free(g_wav_data); g_wav_data = NULL; g_wav_len = 0; } }

/* =========================================================================
 * Keyboard / mouse
 * ========================================================================= */

int sdl_kmp_GetNumScancodes(void) { return SDL_SCANCODE_COUNT; }
const void *sdl_kmp_GetKeyboardState(void) { return (const void *)SDL_GetKeyboardState(NULL); }
int sdl_kmp_GetModState(void) { return (int)SDL_GetModState(); }
void sdl_kmp_SetModState(int mod) { SDL_SetModState((SDL_Keymod)mod); }
int sdl_kmp_GetKeyFromScancode(int scancode) { return (int)SDL_GetKeyFromScancode((SDL_Scancode)scancode, 0, false); }
int sdl_kmp_GetScancodeFromKey(int keycode) { return (int)SDL_GetScancodeFromKey((SDL_Keycode)keycode, NULL); }
const char *sdl_kmp_GetKeyName(int keycode) { return SDL_GetKeyName((SDL_Keycode)keycode); }
const char *sdl_kmp_GetScancodeName(int scancode) { return SDL_GetScancodeName((SDL_Scancode)scancode); }

int sdl_kmp_GetMouseState(float *x, float *y, int *buttons)
{
    Uint32 b = SDL_GetMouseState(x, y);
    *buttons = (int)b;
    return 0;
}
void sdl_kmp_GetGlobalMouseState(float *x, float *y, int *buttons)
{
    Uint32 b = SDL_GetGlobalMouseState(x, y);
    *buttons = (int)b;
}
void sdl_kmp_WarpMouseInWindow(int windowID, float x, float y) { SDL_WarpMouseInWindow(SDL_GetWindowFromID(windowID), x, y); }
int sdl_kmp_CaptureMouse(int enabled) { return SDL_CaptureMouse(enabled); }
int sdl_kmp_ShowCursor(void) { return SDL_ShowCursor(); }

int sdl_kmp_TextInputActive(int windowID) { return SDL_TextInputActive(SDL_GetWindowFromID(windowID)); }
int sdl_kmp_StartTextInput(int windowID) { return SDL_StartTextInput(SDL_GetWindowFromID(windowID)); }
int sdl_kmp_StopTextInput(int windowID) { return SDL_StopTextInput(SDL_GetWindowFromID(windowID)); }

/* =========================================================================
 * Touch
 * ========================================================================= */

static SDL_TouchID *g_touch_devices;
static int g_touch_device_count;
static SDL_Finger **g_touch_fingers;
static int g_touch_finger_count;

int sdl_kmp_RefreshTouchDevices(void)
{
    int count = 0;
    if (g_touch_devices) { SDL_free(g_touch_devices); g_touch_devices = NULL; }
    g_touch_devices = SDL_GetTouchDevices(&count);
    g_touch_device_count = count;
    return count;
}
int sdl_kmp_GetTouchDevice(int index) { return index >= 0 && index < g_touch_device_count ? (int)(g_touch_devices[index] & 0xFFFFFFFFu) : 0; }
const char *sdl_kmp_GetTouchDeviceName(int touchID) { return SDL_GetTouchDeviceName((SDL_TouchID)(Uint32)touchID); }
int sdl_kmp_GetTouchDeviceType(int touchID) { return SDL_GetTouchDeviceType((SDL_TouchID)(Uint32)touchID); }

int sdl_kmp_RefreshTouchFingers(int touchID)
{
    int count = 0;
    if (g_touch_fingers) { SDL_free(g_touch_fingers); g_touch_fingers = NULL; }
    g_touch_fingers = SDL_GetTouchFingers((SDL_TouchID)(Uint32)touchID, &count);
    g_touch_finger_count = count;
    return count;
}
int sdl_kmp_GetTouchFingerCount(void) { return g_touch_finger_count; }
void sdl_kmp_GetTouchFinger(int index, unsigned int *idLo, unsigned int *idHi, float *x, float *y, float *pressure)
{
    if (index < 0 || index >= g_touch_finger_count || !g_touch_fingers[index]) {
        *idLo = *idHi = 0; *x = *y = *pressure = 0; return;
    }
    SDL_Finger *f = g_touch_fingers[index];
    *idLo = (unsigned int)(f->id & 0xFFFFFFFFu); *idHi = (unsigned int)(f->id >> 32);
    *x = f->x; *y = f->y; *pressure = f->pressure;
}

/* =========================================================================
 * Joystick / gamepad
 * ========================================================================= */

static SDL_JoystickID *g_joysticks;
static int g_joystick_count;
static SDL_JoystickID *g_gamepads;
static int g_gamepad_count;

int sdl_kmp_RefreshJoysticks(void)
{
    if (g_joysticks) { SDL_free(g_joysticks); g_joysticks = NULL; }
    g_joysticks = SDL_GetJoysticks(&g_joystick_count);
    return g_joystick_count;
}
int sdl_kmp_GetJoystickID(int index) { return index >= 0 && index < g_joystick_count ? (int)g_joysticks[index] : 0; }

int sdl_kmp_RefreshGamepads(void)
{
    if (g_gamepads) { SDL_free(g_gamepads); g_gamepads = NULL; }
    g_gamepads = SDL_GetGamepads(&g_gamepad_count);
    return g_gamepad_count;
}
int sdl_kmp_GetGamepadID(int index) { return index >= 0 && index < g_gamepad_count ? (int)g_gamepads[index] : 0; }

int sdl_kmp_OpenJoystick(int id) { return (int)(intptr_t)SDL_OpenJoystick((SDL_JoystickID)id); }
void sdl_kmp_CloseJoystick(int js) { SDL_CloseJoystick((SDL_Joystick *)(intptr_t)js); }
int sdl_kmp_GetJoystickIDFromJoystick(int js) { return (int)SDL_GetJoystickID((SDL_Joystick *)(intptr_t)js); }
const char *sdl_kmp_GetJoystickName(int js) { return SDL_GetJoystickName((SDL_Joystick *)(intptr_t)js); }
int sdl_kmp_GetJoystickType(int js) { return SDL_GetJoystickType((SDL_Joystick *)(intptr_t)js); }
int sdl_kmp_GetNumJoystickAxes(int js) { return SDL_GetNumJoystickAxes((SDL_Joystick *)(intptr_t)js); }
int sdl_kmp_GetNumJoystickBalls(int js) { return SDL_GetNumJoystickBalls((SDL_Joystick *)(intptr_t)js); }
int sdl_kmp_GetNumJoystickHats(int js) { return SDL_GetNumJoystickHats((SDL_Joystick *)(intptr_t)js); }
int sdl_kmp_GetNumJoystickButtons(int js) { return SDL_GetNumJoystickButtons((SDL_Joystick *)(intptr_t)js); }
int sdl_kmp_GetJoystickPlayerIndex(int js) { return SDL_GetJoystickPlayerIndex((SDL_Joystick *)(intptr_t)js); }
int sdl_kmp_GetJoystickFirmwareVersion(int js) { return SDL_GetJoystickFirmwareVersion((SDL_Joystick *)(intptr_t)js); }
short sdl_kmp_GetJoystickAxis(int js, int axis) { return SDL_GetJoystickAxis((SDL_Joystick *)(intptr_t)js, axis); }
int sdl_kmp_GetJoystickButton(int js, int button) { return SDL_GetJoystickButton((SDL_Joystick *)(intptr_t)js, button); }
int sdl_kmp_GetJoystickHat(int js, int hat) { return SDL_GetJoystickHat((SDL_Joystick *)(intptr_t)js, hat); }
int sdl_kmp_GetJoystickBall(int js, int ball, int *dx, int *dy) { return SDL_GetJoystickBall((SDL_Joystick *)(intptr_t)js, ball, dx, dy); }
int sdl_kmp_JoystickRumble(int js, int low, int high, int duration) { return SDL_RumbleJoystick((SDL_Joystick *)(intptr_t)js, (Uint16)low, (Uint16)high, (Uint32)duration); }

int sdl_kmp_OpenGamepad(int id) { return (int)(intptr_t)SDL_OpenGamepad((SDL_JoystickID)id); }
void sdl_kmp_CloseGamepad(int gp) { SDL_CloseGamepad((SDL_Gamepad *)(intptr_t)gp); }
int sdl_kmp_GetGamepadIDFromGamepad(int gp) { return (int)SDL_GetGamepadID((SDL_Gamepad *)(intptr_t)gp); }
const char *sdl_kmp_GetGamepadName(int gp) { return SDL_GetGamepadName((SDL_Gamepad *)(intptr_t)gp); }
int sdl_kmp_GetGamepadVendor(int gp) { return SDL_GetGamepadVendor((SDL_Gamepad *)(intptr_t)gp); }
int sdl_kmp_GetGamepadProduct(int gp) { return SDL_GetGamepadProduct((SDL_Gamepad *)(intptr_t)gp); }
const char *sdl_kmp_GetGamepadSerial(int gp) { return SDL_GetGamepadSerial((SDL_Gamepad *)(intptr_t)gp); }
int sdl_kmp_GamepadConnected(int gp) { return SDL_GamepadConnected((SDL_Gamepad *)(intptr_t)gp); }
int sdl_kmp_GetGamepadPlayerIndex(int gp) { return SDL_GetGamepadPlayerIndex((SDL_Gamepad *)(intptr_t)gp); }
int sdl_kmp_GetGamepadFirmwareVersion(int gp) { return SDL_GetGamepadFirmwareVersion((SDL_Gamepad *)(intptr_t)gp); }
int sdl_kmp_GetNumGamepadTouchpads(int gp) { return SDL_GetNumGamepadTouchpads((SDL_Gamepad *)(intptr_t)gp); }
int sdl_kmp_GetGamepadButton(int gp, int button) { return SDL_GetGamepadButton((SDL_Gamepad *)(intptr_t)gp, button); }
short sdl_kmp_GetGamepadAxis(int gp, int axis) { return SDL_GetGamepadAxis((SDL_Gamepad *)(intptr_t)gp, axis); }
int sdl_kmp_GetGamepadTouchpadFinger(int gp, int tp, int finger, int *down, float *x, float *y, float *pressure)
{
    bool d = false;
    int rc = SDL_GetGamepadTouchpadFinger((SDL_Gamepad *)(intptr_t)gp, tp, finger, &d, x, y, pressure);
    *down = d;
    return rc == 0;
}
int sdl_kmp_GamepadHasSensor(int gp, int type) { return SDL_GamepadHasSensor((SDL_Gamepad *)(intptr_t)gp, (SDL_SensorType)type); }
int sdl_kmp_GetGamepadSensorData(int gp, int type, float *out, int count)
{ return SDL_GetGamepadSensorData((SDL_Gamepad *)(intptr_t)gp, (SDL_SensorType)type, out, count) == 0; }
float sdl_kmp_GetGamepadSensorDataRate(int gp, int type) { return SDL_GetGamepadSensorDataRate((SDL_Gamepad *)(intptr_t)gp, (SDL_SensorType)type); }
int sdl_kmp_GamepadRumble(int gp, int low, int high, int duration) { return SDL_RumbleGamepad((SDL_Gamepad *)(intptr_t)gp, (Uint16)low, (Uint16)high, (Uint32)duration); }

/* =========================================================================
 * Filesystem / misc
 * ========================================================================= */

const char *sdl_kmp_GetBasePath(void) { return SDL_GetBasePath(); }
const char *sdl_kmp_GetPrefPath(const char *org, const char *app) { return SDL_GetPrefPath(org, app); }
const char *sdl_kmp_GetUserFolder(int folder) { return SDL_GetUserFolder((SDL_Folder)folder); }
int sdl_kmp_CreateDirectory(const char *path) { return SDL_CreateDirectory(path); }
int sdl_kmp_RemovePath(const char *path) { return SDL_RemovePath(path); }
int sdl_kmp_RenamePath(const char *oldp, const char *newp) { return SDL_RenamePath(oldp, newp); }

int sdl_kmp_GetPowerInfo(int *seconds, int *percent)
{
    int secs = -1, pct = -1;
    SDL_PowerState st = SDL_GetPowerInfo(&secs, &pct);
    *seconds = secs; *percent = pct;
    return (int)st;
}

int sdl_kmp_OpenURL(const char *url) { return SDL_OpenURL(url); }
int sdl_kmp_ShowSimpleMessageBox(const char *title, const char *msg) { return SDL_ShowSimpleMessageBox(SDL_MESSAGEBOX_INFORMATION, title, msg, NULL); }

int sdl_kmp_ShowMessageBox(int flags, const char *title, const char *msg,
                           const int *button_flags, const int *button_ids, const char **button_texts, int num_buttons)
{
    SDL_MessageBoxButtonData *buttons = SDL_malloc(sizeof(SDL_MessageBoxButtonData) * num_buttons);
    if (!buttons) return -1;
    for (int i = 0; i < num_buttons; i++) {
        buttons[i].flags = button_flags[i];
        buttons[i].buttonID = button_ids[i];
        buttons[i].text = button_texts[i];
    }
    SDL_MessageBoxData data;
    data.flags = flags; data.title = title; data.message = msg;
    data.numbuttons = num_buttons; data.buttons = buttons; data.window = NULL;
    int selected = -1;
    int rc = SDL_ShowMessageBox(&data, &selected);
    SDL_free(buttons);
    return rc == 0 ? selected : -1;
}

/* =========================================================================
 * Logging
 * ========================================================================= */

void sdl_kmp_Log(int priority, int category, const char *msg)
{ SDL_LogMessage(category, (SDL_LogPriority)priority, "%s", msg); }
void sdl_kmp_LogSetPriority(int category, int priority) { SDL_SetLogPriority((SDL_LogCategory)category, (SDL_LogPriority)priority); }
int sdl_kmp_LogGetPriority(int category) { return (int)SDL_GetLogPriority((SDL_LogCategory)category); }
void sdl_kmp_LogSetAllPriority(int priority) { SDL_SetLogPriorities((SDL_LogPriority)priority); }
void sdl_kmp_LogResetPriorities(void) { SDL_ResetLogPriorities(); }

/* =========================================================================
 * Threads / synchronization
 * ========================================================================= */

int sdl_kmp_GetNumLogicalCPUCores(void) { return SDL_GetNumLogicalCPUCores(); }
int sdl_kmp_GetCurrentThreadID(void) { return (int)SDL_GetCurrentThreadID(); }

int sdl_kmp_CreateMutex(void) { return (int)(intptr_t)SDL_CreateMutex(); }
void sdl_kmp_LockMutex(int m) { SDL_LockMutex((SDL_Mutex *)(intptr_t)m); }
int sdl_kmp_TryLockMutex(int m) { return SDL_TryLockMutex((SDL_Mutex *)(intptr_t)m) != 0; }
void sdl_kmp_UnlockMutex(int m) { SDL_UnlockMutex((SDL_Mutex *)(intptr_t)m); }
void sdl_kmp_DestroyMutex(int m) { SDL_DestroyMutex((SDL_Mutex *)(intptr_t)m); }

int sdl_kmp_CreateRWLock(void) { return (int)(intptr_t)SDL_CreateRWLock(); }
void sdl_kmp_LockRWLockRead(int l) { SDL_LockRWLockForReading((SDL_RWLock *)(intptr_t)l); }
int sdl_kmp_TryLockRWLockRead(int l) { return SDL_TryLockRWLockForReading((SDL_RWLock *)(intptr_t)l) != 0; }
void sdl_kmp_UnlockRWLockRead(int l) { SDL_UnlockRWLock((SDL_RWLock *)(intptr_t)l); }
void sdl_kmp_LockRWLockWrite(int l) { SDL_LockRWLockForWriting((SDL_RWLock *)(intptr_t)l); }
int sdl_kmp_TryLockRWLockWrite(int l) { return SDL_TryLockRWLockForWriting((SDL_RWLock *)(intptr_t)l) != 0; }
void sdl_kmp_UnlockRWLockWrite(int l) { SDL_UnlockRWLock((SDL_RWLock *)(intptr_t)l); }
void sdl_kmp_DestroyRWLock(int l) { SDL_DestroyRWLock((SDL_RWLock *)(intptr_t)l); }

int sdl_kmp_CreateSemaphore(int initial) { return (int)(intptr_t)SDL_CreateSemaphore(initial); }
void sdl_kmp_WaitSemaphore(int s) { SDL_WaitSemaphore((SDL_Semaphore *)(intptr_t)s); }
int sdl_kmp_TryWaitSemaphore(int s) { return SDL_TryWaitSemaphore((SDL_Semaphore *)(intptr_t)s) != 0; }
int sdl_kmp_WaitSemaphoreTimeout(int s, int ms) { return SDL_WaitSemaphoreTimeout((SDL_Semaphore *)(intptr_t)s, ms) != 0; }
void sdl_kmp_PostSemaphore(int s) { SDL_SignalSemaphore((SDL_Semaphore *)(intptr_t)s); }
int sdl_kmp_GetSemaphoreValue(int s) { return SDL_GetSemaphoreValue((SDL_Semaphore *)(intptr_t)s); }
void sdl_kmp_DestroySemaphore(int s) { SDL_DestroySemaphore((SDL_Semaphore *)(intptr_t)s); }

int sdl_kmp_CreateCondition(void) { return (int)(intptr_t)SDL_CreateCondition(); }
int sdl_kmp_WaitCondition(int cond, int mutex, int timeout_ms)
{ return SDL_WaitConditionTimeout((SDL_Condition *)(intptr_t)cond, (SDL_Mutex *)(intptr_t)mutex, timeout_ms) != 0; }
void sdl_kmp_SignalCondition(int cond) { SDL_SignalCondition((SDL_Condition *)(intptr_t)cond); }
void sdl_kmp_BroadcastCondition(int cond) { SDL_BroadcastCondition((SDL_Condition *)(intptr_t)cond); }
void sdl_kmp_DestroyCondition(int cond) { SDL_DestroyCondition((SDL_Condition *)(intptr_t)cond); }

/* Threads cannot run on single-threaded Kotlin/Wasm; expose a stub. */
int sdl_kmp_CreateThread(const char *name, void *fn, void *data) { return 0; }

/* =========================================================================
 * IO streams
 * ========================================================================= */

int sdl_kmp_IOFromFile(const char *path, const char *mode) { return (int)(intptr_t)SDL_IOFromFile(path, mode); }
int sdl_kmp_IOFromMem(void *data, int size) { return (int)(intptr_t)SDL_IOFromMem(data, size); }
int sdl_kmp_IOFromConstMem(const void *data, int size) { return (int)(intptr_t)SDL_IOFromConstMem(data, size); }
long long sdl_kmp_IORead(int io, void *buf, int size) { return (long long)SDL_ReadIO((SDL_IOStream *)(intptr_t)io, buf, size); }
long long sdl_kmp_IOWrite(int io, const void *buf, int size) { return (long long)SDL_WriteIO((SDL_IOStream *)(intptr_t)io, buf, size); }
long long sdl_kmp_IOSeek(int io, long long offset, int whence) { return (long long)SDL_SeekIO((SDL_IOStream *)(intptr_t)io, offset, (SDL_IOWhence)whence); }
long long sdl_kmp_IOTell(int io) { return (long long)SDL_TellIO((SDL_IOStream *)(intptr_t)io); }
long long sdl_kmp_IOStreamSize(int io) { return (long long)SDL_GetIOSize((SDL_IOStream *)(intptr_t)io); }
int sdl_kmp_IOFlush(int io) { return SDL_FlushIO((SDL_IOStream *)(intptr_t)io); }
void sdl_kmp_IOClose(int io) { SDL_CloseIO((SDL_IOStream *)(intptr_t)io); }

static void *g_loadfile_data;
static int g_loadfile_size;
int sdl_kmp_LoadFileToMem(const char *path)
{
    if (g_loadfile_data) { SDL_free(g_loadfile_data); g_loadfile_data = NULL; }
    size_t size = 0;
    void *data = SDL_LoadFile(path, &size);
    if (!data) return 0;
    g_loadfile_data = data; g_loadfile_size = (int)size;
    return 1;
}
int sdl_kmp_LoadFileSize(void) { return g_loadfile_size; }
void *sdl_kmp_LoadFileData(void) { return g_loadfile_data; }
void sdl_kmp_LoadFileFree(void) { if (g_loadfile_data) { SDL_free(g_loadfile_data); g_loadfile_data = NULL; g_loadfile_size = 0; } }

/* =========================================================================
 * Properties
 * ========================================================================= */

unsigned int sdl_kmp_CreateProperties(void) { return SDL_CreateProperties(); }
int sdl_kmp_SetProperty(unsigned int props, const char *name, long long value)
{ return SDL_SetNumberProperty(props, name, value); }
int sdl_kmp_SetStringProperty(unsigned int props, const char *name, const char *value)
{ return SDL_SetStringProperty(props, name, value); }
long long sdl_kmp_GetProperty(unsigned int props, const char *name, long long default_value)
{ return SDL_GetNumberProperty(props, name, default_value); }
const char *sdl_kmp_GetStringProperty(unsigned int props, const char *name)
{ return SDL_GetStringProperty(props, name, NULL); }
int sdl_kmp_HasProperty(unsigned int props, const char *name) { return SDL_HasProperty(props, name); }
int sdl_kmp_DeleteProperty(unsigned int props, const char *name) { return SDL_ClearProperty(props, name); }
int sdl_kmp_CopyProperties(unsigned int src, unsigned int dst) { return SDL_CopyProperties(src, dst); }
unsigned int sdl_kmp_GetGlobalProperties(void) { return SDL_GetGlobalProperties(); }
void sdl_kmp_DestroyProperties(unsigned int props) { SDL_DestroyProperties(props); }

/* =========================================================================
 * Camera / sensor / haptics
 * ========================================================================= */

static SDL_CameraID *g_cameras;
static int g_camera_count;
static SDL_CameraSpec **g_camera_specs;
static int g_camera_spec_count;

int sdl_kmp_RefreshCameras(void)
{
    if (g_cameras) { SDL_free(g_cameras); g_cameras = NULL; }
    g_cameras = SDL_GetCameras(&g_camera_count);
    return g_camera_count;
}
int sdl_kmp_GetCameraDevice(int index) { return index >= 0 && index < g_camera_count ? (int)g_cameras[index] : 0; }
const char *sdl_kmp_GetCameraDeviceName(int id) { return SDL_GetCameraName((SDL_CameraID)id); }
int sdl_kmp_GetCameraDevicePosition(int id) { return SDL_GetCameraPosition((SDL_CameraID)id); }
int sdl_kmp_RefreshCameraFormats(int id)
{
    int count = 0;
    if (g_camera_specs) { SDL_free(g_camera_specs); g_camera_specs = NULL; }
    g_camera_specs = SDL_GetCameraSupportedFormats((SDL_CameraID)id, &count);
    g_camera_spec_count = count;
    return count;
}
void sdl_kmp_GetCameraFormatSpec(int index, int *format, int *w, int *h, int *rate)
{
    if (index < 0 || index >= g_camera_spec_count || !g_camera_specs || !g_camera_specs[index]) {
        *format = *w = *h = *rate = 0; return;
    }
    SDL_CameraSpec *s = g_camera_specs[index];
    *format = s->format; *w = s->width; *h = s->height;
    *rate = s->framerate_denominator > 0 ? s->framerate_numerator / s->framerate_denominator : 0;
}
int sdl_kmp_OpenCamera(int id, int format, int w, int h, int rate)
{
    SDL_CameraSpec spec;
    spec.format = format; spec.width = w; spec.height = h;
    spec.framerate_numerator = rate; spec.framerate_denominator = 1;
    return (int)(intptr_t)SDL_OpenCamera((SDL_CameraID)id, &spec);
}
int sdl_kmp_GetCameraFormat(int cam, int *format, int *w, int *h, int *rate)
{
    SDL_CameraSpec spec;
    if (SDL_GetCameraFormat((SDL_Camera *)(intptr_t)cam, &spec)) return 0;
    *format = spec.format; *w = spec.width; *h = spec.height;
    *rate = spec.framerate_denominator > 0 ? spec.framerate_numerator / spec.framerate_denominator : 0;
    return 1;
}
int sdl_kmp_GetCameraPermissionState(int cam) { return SDL_GetCameraPermissionState((SDL_Camera *)(intptr_t)cam); }
int sdl_kmp_GetCameraSupportsFormat(int cam, int format, int w, int h, int rate)
{
    int count = 0;
    SDL_CameraSpec **formats = SDL_GetCameraSupportedFormats(SDL_GetCameraID((SDL_Camera *)(intptr_t)cam), &count);
    if (!formats) return 0;
    int supported = 0;
    for (int i = 0; i < count; i++) {
        SDL_CameraSpec *s = formats[i];
        int r = s->framerate_denominator > 0 ? s->framerate_numerator / s->framerate_denominator : 0;
        if (s->format == (SDL_PixelFormat)format && s->width == w && s->height == h && r == rate) { supported = 1; break; }
    }
    SDL_free(formats);
    return supported;
}
int sdl_kmp_AcquireCameraFrame(int cam)
{
    SDL_Surface *frame = SDL_AcquireCameraFrame((SDL_Camera *)(intptr_t)cam, NULL);
    return (int)(intptr_t)frame;
}
void sdl_kmp_ReleaseCameraFrame(int cam, int frame)
{ SDL_ReleaseCameraFrame((SDL_Camera *)(intptr_t)cam, (SDL_Surface *)(intptr_t)frame); }
void sdl_kmp_CloseCamera(int cam) { SDL_CloseCamera((SDL_Camera *)(intptr_t)cam); }

static SDL_SensorID *g_sensors;
static int g_sensor_count;
int sdl_kmp_RefreshSensors(void)
{
    if (g_sensors) { SDL_free(g_sensors); g_sensors = NULL; }
    g_sensors = SDL_GetSensors(&g_sensor_count);
    return g_sensor_count;
}
int sdl_kmp_GetSensorDevice(int index) { return index >= 0 && index < g_sensor_count ? (int)g_sensors[index] : 0; }
const char *sdl_kmp_GetSensorDeviceName(int id) { return SDL_GetSensorNameForID((SDL_SensorID)id); }
int sdl_kmp_GetSensorDeviceType(int id) { return SDL_GetSensorTypeForID((SDL_SensorID)id); }
int sdl_kmp_OpenSensor(int id) { return (int)(intptr_t)SDL_OpenSensor((SDL_SensorID)id); }
void sdl_kmp_CloseSensor(int s) { SDL_CloseSensor((SDL_Sensor *)(intptr_t)s); }
const char *sdl_kmp_GetSensorName(int s) { return SDL_GetSensorName((SDL_Sensor *)(intptr_t)s); }
int sdl_kmp_GetSensorType(int s) { return SDL_GetSensorType((SDL_Sensor *)(intptr_t)s); }
int sdl_kmp_GetSensorData(int s, float *out, int count) { return SDL_GetSensorData((SDL_Sensor *)(intptr_t)s, out, count) == 0; }

/* Haptics are not available on wasm; expose an empty device list. */
int sdl_kmp_GetNumHapticDevices(void) { return 0; }

/* =========================================================================
 * OpenGL
 * ========================================================================= */

int sdl_kmp_GL_LoadLibrary(const char *path) { return SDL_GL_LoadLibrary(path); }
void sdl_kmp_GL_UnloadLibrary(void) { SDL_GL_UnloadLibrary(); }
void *sdl_kmp_GL_GetProcAddress(const char *proc) { return SDL_GL_GetProcAddress(proc); }
int sdl_kmp_GL_ExtensionSupported(const char *ext) { return SDL_GL_ExtensionSupported(ext); }
void sdl_kmp_GL_ResetAttributes(void) { SDL_GL_ResetAttributes(); }
int sdl_kmp_GL_SetAttribute(int attr, int value) { return SDL_GL_SetAttribute((SDL_GLAttr)attr, value); }
int sdl_kmp_GL_GetAttribute(int attr, int *out)
{
    int value = 0;
    if (!SDL_GL_GetAttribute((SDL_GLAttr)attr, &value)) { *out = value; return 1; }
    return 0;
}
int sdl_kmp_GL_CreateContext(int window) { return (int)(intptr_t)SDL_GL_CreateContext((SDL_Window *)(intptr_t)window); }
int sdl_kmp_GL_MakeCurrent(int window, int ctx) { return SDL_GL_MakeCurrent((SDL_Window *)(intptr_t)window, (SDL_GLContext)(intptr_t)ctx); }
int sdl_kmp_GL_GetCurrentWindow(void) { return (int)(intptr_t)SDL_GL_GetCurrentWindow(); }
int sdl_kmp_GL_GetCurrentContext(void) { return (int)(intptr_t)SDL_GL_GetCurrentContext(); }
int sdl_kmp_GL_SetSwapInterval(int interval) { return SDL_GL_SetSwapInterval(interval); }
int sdl_kmp_GL_GetSwapInterval(int *out)
{
    int v = 0;
    if (!SDL_GL_GetSwapInterval(&v)) { *out = v; return 1; }
    return 0;
}
int sdl_kmp_GL_SwapWindow(int window) { return SDL_GL_SwapWindow((SDL_Window *)(intptr_t)window); }
void sdl_kmp_GL_DestroyContext(int ctx) { SDL_GL_DestroyContext((SDL_GLContext)(intptr_t)ctx); }

/* =========================================================================
 * Vulkan
 * ========================================================================= */

int sdl_kmp_Vulkan_LoadLibrary(const char *path) { return SDL_Vulkan_LoadLibrary(path); }
void sdl_kmp_Vulkan_UnloadLibrary(void) { SDL_Vulkan_UnloadLibrary(); }
void *sdl_kmp_Vulkan_GetVkGetInstanceProcAddr(void) { return (void *)SDL_Vulkan_GetVkGetInstanceProcAddr(); }

static const char *g_vulkan_extensions[32];
static int g_vulkan_extension_count;
int sdl_kmp_Vulkan_GetInstanceExtensions(void)
{
    Uint32 count = 0;
    char const * const *exts = SDL_Vulkan_GetInstanceExtensions(&count);
    if (!exts) return 0;
    if (count > 32) count = 32;
    g_vulkan_extension_count = (int)count;
    for (Uint32 i = 0; i < count; i++) g_vulkan_extensions[i] = exts[i];
    return g_vulkan_extension_count;
}
const char *sdl_kmp_Vulkan_GetInstanceExtension(int index)
{ return index >= 0 && index < g_vulkan_extension_count ? g_vulkan_extensions[index] : NULL; }

static VkSurfaceKHR g_vk_surface;
int sdl_kmp_Vulkan_CreateSurface(int window, int instance)
{
    int ok = SDL_Vulkan_CreateSurface((SDL_Window *)(intptr_t)window, (VkInstance)(uintptr_t)instance, NULL, &g_vk_surface);
    return ok ? (int)(uintptr_t)g_vk_surface : 0;
}
void sdl_kmp_Vulkan_DestroySurface(int instance, int surface)
{
    SDL_Vulkan_DestroySurface((VkInstance)(uintptr_t)instance, (VkSurfaceKHR)(uintptr_t)surface, NULL);
}
int sdl_kmp_Vulkan_GetPresentationSupport(int instance, int physical_device, int queue_family)
{
    return SDL_Vulkan_GetPresentationSupport((VkInstance)(uintptr_t)instance,
                                             (VkPhysicalDevice)(uintptr_t)physical_device, (Uint32)queue_family);
}

/* =========================================================================
 * GPU (not available on wasm with this SDL3 snapshot; report unsupported)
 * ========================================================================= */

int sdl_kmp_GPU_IsSupported(void) { return 0; }
int sdl_kmp_GPU_GetNumDrivers(void) { return 0; }
