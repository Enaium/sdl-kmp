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
 * Glue between the Emscripten-compiled SDL3 module (sdl_wasm.js) and
 * Kotlin/Wasm. Instantiates the module and exposes every sdl_kmp_* shim
 * function as a global so Kotlin's external declarations can call them.
 *
 * Marshaling conventions (see sdl_wasm_shim.c):
 *   - scalars are passed straight through (numbers);
 *   - strings are converted with UTF8ToString/stringToUTF8;
 *   - functions with out-parameters write their results into the global
 *     sdlKmpResultI32 / sdlKmpResultF32 typed arrays (fixed slots), which
 *     Kotlin reads after the call;
 *   - byte arrays are copied into a heap buffer for the call and freed after;
 *   - pointers returned by SDL that the caller must free are converted and
 *     freed here (through sdl_kmp_Free).
 */

export async function initSdlKmp(options = {}) {
    const mod = await import('./sdl_wasm.js');
    const loadSdl = typeof mod.default === 'function' ? mod.default : mod;
    const Module = await loadSdl({
        canvas: options.canvas,
    });

    const C = Module.cwrap;
    const UTF8 = Module.UTF8ToString;
    const malloc = Module._malloc;
    const free = Module._free;

    const allocStr = (s) => {
        if (s === null || s === undefined) return 0;
        const bytes = Module.lengthBytesUTF8(s) + 1;
        const p = malloc(bytes);
        Module.stringToUTF8(s, p, bytes);
        return p;
    };
    const freeStr = (p) => { if (p) free(p); };
    const readCString = (p) => (p ? UTF8(p) : null);

    /* Result scratch buffers (fixed slots). */
    const R32 = new Int32Array(16);
    const RF32 = new Float32Array(16);
    globalThis.sdlKmpResultI32 = R32;
    globalThis.sdlKmpResultF32 = RF32;

    const setRet = (f) => { globalThis[f] = Module['_' + f]; };
    const setCwrap = (name, ret, args) => { globalThis[name] = C(name, ret, args); };

    /* Live view over a region of the Emscripten heap (valid until a heap grow). */
    globalThis.sdlKmpHeapBytes = (ptr, len) => Module.HEAPU8.subarray(ptr, ptr + len);

    /* Write bytes into the Emscripten heap at ptr. */
    globalThis.sdlKmpSetHeapBytes = (ptr, bytes) => {
        Module.HEAPU8.set(bytes, ptr);
    };

    /* Scalar accessors for the result scratch buffers (typed arrays have no
     * element setters in Kotlin/Wasm). */
    globalThis.sdlKmpResultI32Get = (i) => R32[i];
    globalThis.sdlKmpResultF32Get = (i) => RF32[i];

    /* ------------------------------------------------------------------
     * Core
     * ------------------------------------------------------------------ */
    setRet('sdl_kmp_Init');
    setRet('sdl_kmp_InitSubSystem');
    setRet('sdl_kmp_QuitSubSystem');
    setRet('sdl_kmp_WasInit');
    setRet('sdl_kmp_Quit');
    globalThis.sdl_kmp_GetError = () => readCString(Module._sdl_kmp_GetError());
    setRet('sdl_kmp_ClearError');
    globalThis.sdl_kmp_SetError = (m) => { const p = allocStr(m); const r = Module._sdl_kmp_SetError(p); freeStr(p); return r; };
    setRet('sdl_kmp_GetVersion');
    globalThis.sdl_kmp_GetRevision = () => readCString(Module._sdl_kmp_GetRevision());
    setRet('sdl_kmp_GetTicks');
    setRet('sdl_kmp_PerfCounterHi');
    setRet('sdl_kmp_PerfCounterLo');
    setRet('sdl_kmp_PerfFreqHi');
    setRet('sdl_kmp_PerfFreqLo');
    setRet('sdl_kmp_Delay');
    globalThis.sdl_kmp_SetHint = (n, v) => { const p = allocStr(n), q = allocStr(v); const r = Module._sdl_kmp_SetHint(p, q); freeStr(p); freeStr(q); return r; };
    globalThis.sdl_kmp_GetHint = (n) => { const p = allocStr(n); const r = readCString(Module._sdl_kmp_GetHint(p)); freeStr(p); return r; };
    globalThis.sdl_kmp_GetHintBoolean = (n, d) => { const p = allocStr(n); const r = Module._sdl_kmp_GetHintBoolean(p, d); freeStr(p); return r; };
    globalThis.sdl_kmp_GetClipboardText = () => { const p = Module._sdl_kmp_GetClipboardText(); const r = readCString(p); Module._sdl_kmp_Free(p); return r; };
    globalThis.sdl_kmp_SetClipboardText = (t) => { const p = allocStr(t); const r = Module._sdl_kmp_SetClipboardText(p); freeStr(p); return r; };
    setRet('sdl_kmp_HasClipboardText');
    setRet('sdl_kmp_Free');

    /* ------------------------------------------------------------------
     * Drivers
     * ------------------------------------------------------------------ */
    setRet('sdl_kmp_GetNumVideoDrivers');
    globalThis.sdl_kmp_GetVideoDriver = (i) => readCString(Module._sdl_kmp_GetVideoDriver(i));
    globalThis.sdl_kmp_GetCurrentVideoDriver = () => readCString(Module._sdl_kmp_GetCurrentVideoDriver());
    setRet('sdl_kmp_GetNumAudioDrivers');
    globalThis.sdl_kmp_GetAudioDriver = (i) => readCString(Module._sdl_kmp_GetAudioDriver(i));
    globalThis.sdl_kmp_GetCurrentAudioDriver = () => readCString(Module._sdl_kmp_GetCurrentAudioDriver());
    setRet('sdl_kmp_GetNumRenderDrivers');
    globalThis.sdl_kmp_GetRenderDriver = (i) => readCString(Module._sdl_kmp_GetRenderDriver(i));

    /* ------------------------------------------------------------------
     * Window
     * ------------------------------------------------------------------ */
    globalThis.sdl_kmp_CreateWindow = (t, w, h, lo, hi) => { const p = allocStr(t); const r = Module._sdl_kmp_CreateWindow(p, w, h, lo, hi); freeStr(p); return r; };
    setRet('sdl_kmp_DestroyWindow');
    setRet('sdl_kmp_GetWindowID');
    globalThis.sdl_kmp_GetWindowTitle = (w) => readCString(Module._sdl_kmp_GetWindowTitle(w));
    globalThis.sdl_kmp_SetWindowTitle = (w, t) => { const p = allocStr(t); Module._sdl_kmp_SetWindowTitle(w, p); freeStr(p); };
    globalThis.sdl_kmp_GetWindowSize = (w) => {
        const b = malloc(8);
        Module._sdl_kmp_GetWindowSize(w, b, b + 4);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        free(b);
    };
    setRet('sdl_kmp_SetWindowSize');
    globalThis.sdl_kmp_GetWindowPosition = (w) => {
        const b = malloc(8);
        Module._sdl_kmp_GetWindowPosition(w, b, b + 4);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        free(b);
    };
    setRet('sdl_kmp_SetWindowPosition');
    globalThis.sdl_kmp_GetWindowSizeInPixels = (w) => {
        const b = malloc(8);
        Module._sdl_kmp_GetWindowSizeInPixels(w, b, b + 4);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        free(b);
    };
    setRet('sdl_kmp_GetWindowFlags');
    setRet('sdl_kmp_GetDisplayForWindow');
    setRet('sdl_kmp_GetWindowOpacity');
    setRet('sdl_kmp_SetWindowOpacity');
    setRet('sdl_kmp_SetWindowFullscreen');
    setRet('sdl_kmp_SetWindowBordered');
    setRet('sdl_kmp_SetWindowResizable');
    setRet('sdl_kmp_SetWindowAlwaysOnTop');
    setRet('sdl_kmp_GetWindowMouseGrab');
    setRet('sdl_kmp_SetWindowMouseGrab');
    setRet('sdl_kmp_GetWindowKeyboardGrab');
    setRet('sdl_kmp_SetWindowKeyboardGrab');
    setRet('sdl_kmp_GetWindowRelativeMouseMode');
    setRet('sdl_kmp_SetWindowRelativeMouseMode');
    globalThis.sdl_kmp_GetWindowMinimumSize = (w) => {
        const b = malloc(8);
        Module._sdl_kmp_GetWindowMinimumSize(w, b, b + 4);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        free(b);
    };
    setRet('sdl_kmp_SetWindowMinimumSize');
    globalThis.sdl_kmp_GetWindowMaximumSize = (w) => {
        const b = malloc(8);
        Module._sdl_kmp_GetWindowMaximumSize(w, b, b + 4);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        free(b);
    };
    setRet('sdl_kmp_SetWindowMaximumSize');
    globalThis.sdl_kmp_GetWindowAspectRatio = (w) => {
        const b = malloc(8);
        const ok = Module._sdl_kmp_GetWindowAspectRatio(w, b, b + 4);
        RF32[0] = Module.HEAPF32[b >> 2]; RF32[1] = Module.HEAPF32[(b + 4) >> 2];
        free(b);
        R32[2] = ok;
    };
    setRet('sdl_kmp_SetWindowAspectRatio');
    setRet('sdl_kmp_ShowWindow');
    setRet('sdl_kmp_HideWindow');
    setRet('sdl_kmp_RaiseWindow');
    setRet('sdl_kmp_MaximizeWindow');
    setRet('sdl_kmp_MinimizeWindow');
    setRet('sdl_kmp_RestoreWindow');
    setRet('sdl_kmp_FlashWindow');
    setRet('sdl_kmp_GetWindowSurface');
    setRet('sdl_kmp_SetWindowIcon');
    setRet('sdl_kmp_GetWindowFromID');

    /* ------------------------------------------------------------------
     * Displays
     * ------------------------------------------------------------------ */
    setRet('sdl_kmp_RefreshDisplays');
    setRet('sdl_kmp_GetDisplayID');
    globalThis.sdl_kmp_GetDisplayName = (i) => readCString(Module._sdl_kmp_GetDisplayName(i));
    setRet('sdl_kmp_GetPrimaryDisplay');
    globalThis.sdl_kmp_GetDisplayBounds2 = (i) => {
        const b = malloc(16);
        Module._sdl_kmp_GetDisplayBounds2(i, b, b + 4, b + 8, b + 12);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        R32[2] = Module.HEAP32[(b + 8) >> 2]; R32[3] = Module.HEAP32[(b + 12) >> 2];
        free(b);
    };
    globalThis.sdl_kmp_GetDisplayUsableBounds = (i) => {
        const b = malloc(16);
        Module._sdl_kmp_GetDisplayUsableBounds(i, b, b + 4, b + 8, b + 12);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        R32[2] = Module.HEAP32[(b + 8) >> 2]; R32[3] = Module.HEAP32[(b + 12) >> 2];
        free(b);
    };
    globalThis.sdl_kmp_GetDisplayCurrentMode = (i) => {
        const b = malloc(24);
        const ok = Module._sdl_kmp_GetDisplayCurrentMode(i, b, b + 4, b + 8, b + 12, b + 16);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        R32[2] = Module.HEAP32[(b + 8) >> 2]; RF32[0] = Module.HEAPF32[(b + 12) >> 2];
        RF32[1] = Module.HEAPF32[(b + 16) >> 2]; R32[3] = ok;
        free(b);
    };
    globalThis.sdl_kmp_GetDisplayDesktopMode = (i) => {
        const b = malloc(24);
        const ok = Module._sdl_kmp_GetDisplayDesktopMode(i, b, b + 4, b + 8, b + 12, b + 16);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        R32[2] = Module.HEAP32[(b + 8) >> 2]; RF32[0] = Module.HEAPF32[(b + 12) >> 2];
        RF32[1] = Module.HEAPF32[(b + 16) >> 2]; R32[3] = ok;
        free(b);
    };

    /* ------------------------------------------------------------------
     * Renderer
     * ------------------------------------------------------------------ */
    globalThis.sdl_kmp_CreateRenderer = (w, name) => {
        const p = name === null ? 0 : allocStr(name);
        const r = Module._sdl_kmp_CreateRenderer(w, p);
        freeStr(p);
        return r;
    };
    globalThis.sdl_kmp_GetRendererName = (r) => readCString(Module._sdl_kmp_GetRendererName(r));
    setRet('sdl_kmp_DestroyRenderer');
    globalThis.sdl_kmp_GetRenderDrawColor = (r) => {
        const b = malloc(4);
        Module._sdl_kmp_GetRenderDrawColor(r, b, b + 1, b + 2, b + 3);
        R32[0] = Module.HEAPU8[b]; R32[1] = Module.HEAPU8[b + 1]; R32[2] = Module.HEAPU8[b + 2]; R32[3] = Module.HEAPU8[b + 3];
        free(b);
    };
    setRet('sdl_kmp_SetRenderDrawColor');
    globalThis.sdl_kmp_GetRenderOutputSize = (r) => {
        const b = malloc(8);
        Module._sdl_kmp_GetRenderOutputSize(r, b, b + 4);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        free(b);
    };
    globalThis.sdl_kmp_GetCurrentRenderOutputSize = (r) => {
        const b = malloc(8);
        Module._sdl_kmp_GetCurrentRenderOutputSize(r, b, b + 4);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        free(b);
    };
    globalThis.sdl_kmp_GetRenderViewport = (r) => {
        const b = malloc(16);
        R32[4] = Module._sdl_kmp_GetRenderViewport(r, b, b + 4, b + 8, b + 12);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        R32[2] = Module.HEAP32[(b + 8) >> 2]; R32[3] = Module.HEAP32[(b + 12) >> 2];
        free(b);
    };
    setRet('sdl_kmp_SetRenderViewport');
    setRet('sdl_kmp_SetRenderViewportNull');
    globalThis.sdl_kmp_GetRenderClipRect = (r) => {
        const b = malloc(16);
        R32[4] = Module._sdl_kmp_GetRenderClipRect(r, b, b + 4, b + 8, b + 12);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        R32[2] = Module.HEAP32[(b + 8) >> 2]; R32[3] = Module.HEAP32[(b + 12) >> 2];
        free(b);
    };
    setRet('sdl_kmp_SetRenderClipRect');
    setRet('sdl_kmp_SetRenderClipRectNull');
    globalThis.sdl_kmp_GetRenderScale = (r) => {
        const b = malloc(8);
        Module._sdl_kmp_GetRenderScale(r, b, b + 4);
        RF32[0] = Module.HEAPF32[b >> 2]; RF32[1] = Module.HEAPF32[(b + 4) >> 2];
        free(b);
    };
    setRet('sdl_kmp_SetRenderScale');
    setRet('sdl_kmp_GetRenderDrawBlendMode');
    setRet('sdl_kmp_SetRenderDrawBlendMode');
    setRet('sdl_kmp_GetRenderVSync');
    setRet('sdl_kmp_SetRenderVSync');
    setRet('sdl_kmp_GetRenderTarget');
    setRet('sdl_kmp_SetRenderTarget');
    setRet('sdl_kmp_RenderClear');
    setRet('sdl_kmp_RenderPresent');
    setRet('sdl_kmp_RenderFillRect');
    setRet('sdl_kmp_RenderFillRectNull');
    setRet('sdl_kmp_RenderRect');
    setRet('sdl_kmp_RenderRectNull');
    setRet('sdl_kmp_RenderLine');
    setRet('sdl_kmp_RenderPoint');
    setRet('sdl_kmp_CreateTexture');
    setRet('sdl_kmp_CreateTextureFromSurface');
    setRet('sdl_kmp_RenderTexture');
    setRet('sdl_kmp_RenderTextureRotated');
    setRet('sdl_kmp_RenderTexture9Grid');
    globalThis.sdl_kmp_RenderPoints = (r, arr, count) => {
        const b = malloc(count * 8);
        for (let i = 0; i < count * 2; i++) Module.HEAPF32[(b >> 2) + i] = arr[i];
        const rc = Module._sdl_kmp_RenderPoints(r, b, count);
        free(b);
        return rc;
    };
    globalThis.sdl_kmp_RenderGeometry = (r, t, verts, nv, indices, ni) => {
        let vb = 0, ib = 0;
        if (verts) { vb = malloc(nv * 32); Module.HEAPF32.set(verts, vb >> 2); }
        if (indices) { ib = malloc(ni * 4); Module.HEAP32.set(indices, ib >> 2); }
        const rc = Module._sdl_kmp_RenderGeometry(r, t, vb, nv, ib, ni);
        if (vb) free(vb);
        if (ib) free(ib);
        return rc;
    };
    setRet('sdl_kmp_RenderReadPixels');
    setRet('sdl_kmp_SetRenderLogicalPresentation');
    globalThis.sdl_kmp_GetRenderLogicalPresentationRect = (r) => {
        const b = malloc(16);
        R32[4] = Module._sdl_kmp_GetRenderLogicalPresentationRect(r, b, b + 4, b + 8, b + 12);
        RF32[0] = Module.HEAPF32[b >> 2]; RF32[1] = Module.HEAPF32[(b + 4) >> 2];
        RF32[2] = Module.HEAPF32[(b + 8) >> 2]; RF32[3] = Module.HEAPF32[(b + 12) >> 2];
        free(b);
    };

    /* ------------------------------------------------------------------
     * Texture
     * ------------------------------------------------------------------ */
    setRet('sdl_kmp_GetTextureFormat');
    setRet('sdl_kmp_GetTextureAccess');
    globalThis.sdl_kmp_GetTextureSize = (t) => {
        const b = malloc(8);
        Module._sdl_kmp_GetTextureSize(t, b, b + 4);
        RF32[0] = Module.HEAPF32[b >> 2]; RF32[1] = Module.HEAPF32[(b + 4) >> 2];
        free(b);
    };
    globalThis.sdl_kmp_GetTextureColorMod = (t) => {
        const b = malloc(3);
        Module._sdl_kmp_GetTextureColorMod(t, b, b + 1, b + 2);
        R32[0] = Module.HEAPU8[b]; R32[1] = Module.HEAPU8[b + 1]; R32[2] = Module.HEAPU8[b + 2];
        free(b);
    };
    setRet('sdl_kmp_SetTextureColorMod');
    setRet('sdl_kmp_GetTextureAlphaMod');
    setRet('sdl_kmp_SetTextureAlphaMod');
    setRet('sdl_kmp_GetTextureBlendMode');
    setRet('sdl_kmp_SetTextureBlendMode');
    setRet('sdl_kmp_GetTextureScaleMode');
    setRet('sdl_kmp_SetTextureScaleMode');
    globalThis.sdl_kmp_UpdateTexture = (t, hr, x, y, w, h, pixels, pitch) => {
        let b = 0;
        if (pixels) { b = malloc(pixels.byteLength); Module.HEAPU8.set(pixels, b); }
        const rc = Module._sdl_kmp_UpdateTexture(t, hr, x, y, w, h, b, pitch);
        if (b) free(b);
        return rc;
    };
    globalThis.sdl_kmp_LockTexture = (t, hr, x, y, w, h) => {
        const pitch = Module._sdl_kmp_LockTexture(t, hr, x, y, w, h);
        R32[0] = pitch;
        return pitch;
    };
    globalThis.sdl_kmp_LockedPixelsPtr = () => Module._sdl_kmp_LockedPixels();
    setRet('sdl_kmp_UnlockTexture');
    setRet('sdl_kmp_DestroyTexture');

    /* ------------------------------------------------------------------
     * Pixels / surface
     * ------------------------------------------------------------------ */
    globalThis.sdl_kmp_GetPixelFormatName = (f) => readCString(Module._sdl_kmp_GetPixelFormatName(f));
    setRet('sdl_kmp_MapRGB');
    setRet('sdl_kmp_MapRGBA');
    globalThis.sdl_kmp_GetRGBA = (f, px) => {
        const b = malloc(4);
        Module._sdl_kmp_GetRGBA(f, px, b, b + 1, b + 2, b + 3);
        R32[0] = Module.HEAPU8[b]; R32[1] = Module.HEAPU8[b + 1]; R32[2] = Module.HEAPU8[b + 2]; R32[3] = Module.HEAPU8[b + 3];
        free(b);
    };
    setRet('sdl_kmp_CreateSurface');
    globalThis.sdl_kmp_LoadBMP = (p) => { const c = allocStr(p); const r = Module._sdl_kmp_LoadBMP(c); freeStr(c); return r; };
    setRet('sdl_kmp_GetSurfaceWidth');
    setRet('sdl_kmp_GetSurfaceHeight');
    setRet('sdl_kmp_GetSurfaceFormat');
    setRet('sdl_kmp_GetSurfaceColorspace');
    setRet('sdl_kmp_GetSurfacePitch');
    setRet('sdl_kmp_GetSurfacePixels');
    setRet('sdl_kmp_LockSurface');
    setRet('sdl_kmp_UnlockSurface');
    setRet('sdl_kmp_FillSurfaceRect');
    globalThis.sdl_kmp_FillSurfaceRects = (s, rects, count, color) => {
        const b = malloc(count * 16);
        Module.HEAP32.set(rects, b >> 2);
        const rc = Module._sdl_kmp_FillSurfaceRects(s, b, count, color);
        free(b);
        return rc;
    };
    setRet('sdl_kmp_BlitSurface');
    setRet('sdl_kmp_BlitSurfaceScaled');
    globalThis.sdl_kmp_SaveBMP = (s, p) => { const c = allocStr(p); const r = Module._sdl_kmp_SaveBMP(s, c); freeStr(c); return r; };
    setRet('sdl_kmp_ConvertSurface');
    setRet('sdl_kmp_DestroySurface');

    /* ------------------------------------------------------------------
     * Events
     * ------------------------------------------------------------------ */
    setRet('sdl_kmp_PollEvent');
    setRet('sdl_kmp_WaitEvent');
    setRet('sdl_kmp_PumpEvents');
    setRet('sdl_kmp_EventTimestampLo');
    setRet('sdl_kmp_EventTimestampHi');
    setRet('sdl_kmp_EventWindowID');
    setRet('sdl_kmp_EventData1');
    setRet('sdl_kmp_EventData2');
    setRet('sdl_kmp_EventKeyDown');
    setRet('sdl_kmp_EventRepeat');
    setRet('sdl_kmp_EventKeycode');
    setRet('sdl_kmp_EventScancode');
    setRet('sdl_kmp_EventMod');
    globalThis.sdl_kmp_EventText = () => readCString(Module._sdl_kmp_EventText());
    setRet('sdl_kmp_EventMouseX');
    setRet('sdl_kmp_EventMouseY');
    setRet('sdl_kmp_EventMouseDX');
    setRet('sdl_kmp_EventMouseDY');
    setRet('sdl_kmp_EventButton');
    setRet('sdl_kmp_EventButtonDown');
    setRet('sdl_kmp_EventClicks');
    setRet('sdl_kmp_EventButtonX');
    setRet('sdl_kmp_EventButtonY');
    setRet('sdl_kmp_EventWheelX');
    setRet('sdl_kmp_EventWheelY');
    setRet('sdl_kmp_EventWheelDir');
    setRet('sdl_kmp_EventDisplayID');
    globalThis.sdl_kmp_EventDropData = () => readCString(Module._sdl_kmp_EventDropData());
    setRet('sdl_kmp_EventDeviceID');
    setRet('sdl_kmp_EventAxis');
    setRet('sdl_kmp_EventAxisValue');
    setRet('sdl_kmp_EventBall');
    setRet('sdl_kmp_EventBallDX');
    setRet('sdl_kmp_EventBallDY');
    setRet('sdl_kmp_EventHat');
    setRet('sdl_kmp_EventHatValue');
    setRet('sdl_kmp_EventTouchpad');
    setRet('sdl_kmp_EventFinger');
    setRet('sdl_kmp_EventFingerDown');
    setRet('sdl_kmp_EventFingerX');
    setRet('sdl_kmp_EventFingerY');
    setRet('sdl_kmp_EventFingerPressure');
    setRet('sdl_kmp_EventSensorType');
    setRet('sdl_kmp_EventSensorData');
    setRet('sdl_kmp_EventBatteryState');
    setRet('sdl_kmp_EventBatteryPercent');
    setRet('sdl_kmp_EventTouchIDLo');
    setRet('sdl_kmp_EventTouchIDHi');
    setRet('sdl_kmp_EventFingerIDLo');
    setRet('sdl_kmp_EventFingerIDHi');
    setRet('sdl_kmp_EventTouchX');
    setRet('sdl_kmp_EventTouchY');
    setRet('sdl_kmp_EventTouchDX');
    setRet('sdl_kmp_EventTouchDY');
    setRet('sdl_kmp_EventTouchPressure');
    setRet('sdl_kmp_EventAudioCapture');
    setRet('sdl_kmp_EventClipboardOwner');
    setRet('sdl_kmp_EventSetEventEnabled');
    setRet('sdl_kmp_EventEnabled');
    setRet('sdl_kmp_FlushEvents');

    /* ------------------------------------------------------------------
     * Audio
     * ------------------------------------------------------------------ */
    setRet('sdl_kmp_RefreshAudioDevices');
    setRet('sdl_kmp_GetAudioPlaybackCount');
    setRet('sdl_kmp_GetAudioRecordingCount');
    setRet('sdl_kmp_GetAudioPlaybackDevice');
    setRet('sdl_kmp_GetAudioRecordingDevice');
    globalThis.sdl_kmp_GetAudioDeviceName = (d) => readCString(Module._sdl_kmp_GetAudioDeviceName(d));
    globalThis.sdl_kmp_GetAudioDeviceFormat2 = (d) => {
        const b = malloc(12);
        R32[3] = Module._sdl_kmp_GetAudioDeviceFormat2(d, b, b + 4, b + 8);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2]; R32[2] = Module.HEAP32[(b + 8) >> 2];
        free(b);
    };
    setRet('sdl_kmp_OpenAudioDevice');
    setRet('sdl_kmp_OpenAudioDeviceStream');
    setRet('sdl_kmp_CreateAudioStream');
    setRet('sdl_kmp_AudioDevicePaused');
    setRet('sdl_kmp_PauseAudioDevice');
    setRet('sdl_kmp_ResumeAudioDevice');
    setRet('sdl_kmp_BindAudioStream');
    setRet('sdl_kmp_UnbindAudioStream');
    setRet('sdl_kmp_CloseAudioDevice');
    setRet('sdl_kmp_DestroyAudioStream');
    globalThis.sdl_kmp_PutAudioStreamData = (s, data, len) => {
        const b = malloc(len);
        Module.HEAPU8.set(data, b);
        const rc = Module._sdl_kmp_PutAudioStreamData(s, b, len);
        free(b);
        return rc;
    };
    globalThis.sdl_kmp_GetAudioStreamData = (s, out) => {
        const b = malloc(out.byteLength);
        const n = Module._sdl_kmp_GetAudioStreamData(s, b, out.byteLength);
        out.set(Module.HEAPU8.subarray(b, b + n));
        free(b);
        return n;
    };
    setRet('sdl_kmp_GetAudioStreamAvailable');
    setRet('sdl_kmp_GetAudioStreamQueued');
    globalThis.sdl_kmp_GetAudioStreamFormat = (s) => {
        const b = malloc(24);
        R32[6] = Module._sdl_kmp_GetAudioStreamFormat(s, b, b + 4, b + 8, b + 12, b + 16, b + 20);
        for (let i = 0; i < 6; i++) R32[i] = Module.HEAP32[(b >> 2) + i];
        free(b);
    };
    setRet('sdl_kmp_SetAudioStreamFormat');
    setRet('sdl_kmp_GetAudioStreamGain');
    setRet('sdl_kmp_SetAudioStreamGain');
    setRet('sdl_kmp_GetAudioStreamFrequencyRatio');
    setRet('sdl_kmp_SetAudioStreamFrequencyRatio');
    setRet('sdl_kmp_GetAudioStreamDevicePaused');
    setRet('sdl_kmp_PauseAudioStreamDevice');
    setRet('sdl_kmp_ResumeAudioStreamDevice');
    setRet('sdl_kmp_FlushAudioStream');
    setRet('sdl_kmp_ClearAudioStream');
    globalThis.sdl_kmp_LoadWAV = (p) => { const c = allocStr(p); const r = Module._sdl_kmp_LoadWAV(c); freeStr(c); return r; };
    setRet('sdl_kmp_LoadWAVFormat');
    setRet('sdl_kmp_LoadWAVChannels');
    setRet('sdl_kmp_LoadWAVFreq');
    setRet('sdl_kmp_LoadWAVLen');
    setRet('sdl_kmp_LoadWAVData');
    setRet('sdl_kmp_LoadWAVFree');

    /* ------------------------------------------------------------------
     * Keyboard / mouse
     * ------------------------------------------------------------------ */
    setRet('sdl_kmp_GetNumScancodes');
    globalThis.sdl_kmp_GetKeyboardState = () => {
        const p = Module._sdl_kmp_GetKeyboardState();
        R32[0] = p;
    };
    setRet('sdl_kmp_GetModState');
    setRet('sdl_kmp_SetModState');
    setRet('sdl_kmp_GetKeyFromScancode');
    setRet('sdl_kmp_GetScancodeFromKey');
    globalThis.sdl_kmp_GetKeyName = (k) => readCString(Module._sdl_kmp_GetKeyName(k));
    globalThis.sdl_kmp_GetScancodeName = (k) => readCString(Module._sdl_kmp_GetScancodeName(k));
    globalThis.sdl_kmp_GetMouseState = () => {
        const b = malloc(12);
        Module._sdl_kmp_GetMouseState(b, b + 4, b + 8);
        RF32[0] = Module.HEAPF32[b >> 2]; RF32[1] = Module.HEAPF32[(b + 4) >> 2];
        R32[2] = Module.HEAP32[(b + 8) >> 2];
        free(b);
    };
    globalThis.sdl_kmp_GetGlobalMouseState = () => {
        const b = malloc(12);
        Module._sdl_kmp_GetGlobalMouseState(b, b + 4, b + 8);
        RF32[0] = Module.HEAPF32[b >> 2]; RF32[1] = Module.HEAPF32[(b + 4) >> 2];
        R32[2] = Module.HEAP32[(b + 8) >> 2];
        free(b);
    };
    setRet('sdl_kmp_WarpMouseInWindow');
    setRet('sdl_kmp_CaptureMouse');
    setRet('sdl_kmp_ShowCursor');
    setRet('sdl_kmp_TextInputActive');
    setRet('sdl_kmp_StartTextInput');
    setRet('sdl_kmp_StopTextInput');

    /* ------------------------------------------------------------------
     * Touch
     * ------------------------------------------------------------------ */
    setRet('sdl_kmp_RefreshTouchDevices');
    setRet('sdl_kmp_GetTouchDevice');
    globalThis.sdl_kmp_GetTouchDeviceName = (t) => readCString(Module._sdl_kmp_GetTouchDeviceName(t));
    setRet('sdl_kmp_GetTouchDeviceType');
    setRet('sdl_kmp_RefreshTouchFingers');
    setRet('sdl_kmp_GetTouchFingerCount');
    globalThis.sdl_kmp_GetTouchFinger = (i) => {
        const b = malloc(24);
        Module._sdl_kmp_GetTouchFinger(i, b, b + 4, b + 8, b + 12, b + 16);
        R32[0] = Module.HEAPU32[b >> 2]; R32[1] = Module.HEAPU32[(b + 4) >> 2];
        RF32[0] = Module.HEAPF32[(b + 8) >> 2]; RF32[1] = Module.HEAPF32[(b + 12) >> 2];
        RF32[2] = Module.HEAPF32[(b + 16) >> 2];
        free(b);
    };

    /* ------------------------------------------------------------------
     * Joystick / gamepad
     * ------------------------------------------------------------------ */
    setRet('sdl_kmp_RefreshJoysticks');
    setRet('sdl_kmp_GetJoystickID');
    setRet('sdl_kmp_RefreshGamepads');
    setRet('sdl_kmp_GetGamepadID');
    setRet('sdl_kmp_OpenJoystick');
    setRet('sdl_kmp_CloseJoystick');
    setRet('sdl_kmp_GetJoystickIDFromJoystick');
    globalThis.sdl_kmp_GetJoystickName = (j) => readCString(Module._sdl_kmp_GetJoystickName(j));
    setRet('sdl_kmp_GetJoystickType');
    setRet('sdl_kmp_GetNumJoystickAxes');
    setRet('sdl_kmp_GetNumJoystickBalls');
    setRet('sdl_kmp_GetNumJoystickHats');
    setRet('sdl_kmp_GetNumJoystickButtons');
    setRet('sdl_kmp_GetJoystickPlayerIndex');
    setRet('sdl_kmp_GetJoystickFirmwareVersion');
    setRet('sdl_kmp_GetJoystickAxis');
    setRet('sdl_kmp_GetJoystickButton');
    setRet('sdl_kmp_GetJoystickHat');
    globalThis.sdl_kmp_GetJoystickBall = (j, ball) => {
        const b = malloc(8);
        R32[2] = Module._sdl_kmp_GetJoystickBall(j, ball, b, b + 4);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        free(b);
    };
    setRet('sdl_kmp_JoystickRumble');
    setRet('sdl_kmp_OpenGamepad');
    setRet('sdl_kmp_CloseGamepad');
    setRet('sdl_kmp_GetGamepadIDFromGamepad');
    globalThis.sdl_kmp_GetGamepadName = (g) => readCString(Module._sdl_kmp_GetGamepadName(g));
    setRet('sdl_kmp_GetGamepadVendor');
    setRet('sdl_kmp_GetGamepadProduct');
    globalThis.sdl_kmp_GetGamepadSerial = (g) => { const p = Module._sdl_kmp_GetGamepadSerial(g); const r = readCString(p); Module._sdl_kmp_Free(p); return r; };
    setRet('sdl_kmp_GamepadConnected');
    setRet('sdl_kmp_GetGamepadPlayerIndex');
    setRet('sdl_kmp_GetGamepadFirmwareVersion');
    setRet('sdl_kmp_GetNumGamepadTouchpads');
    setRet('sdl_kmp_GetGamepadButton');
    setRet('sdl_kmp_GetGamepadAxis');
    globalThis.sdl_kmp_GetGamepadTouchpadFinger = (g, tp, f) => {
        const b = malloc(20);
        R32[4] = Module._sdl_kmp_GetGamepadTouchpadFinger(g, tp, f, b, b + 4, b + 8, b + 12);
        R32[0] = Module.HEAP32[b >> 2];
        RF32[0] = Module.HEAPF32[(b + 4) >> 2]; RF32[1] = Module.HEAPF32[(b + 8) >> 2]; RF32[2] = Module.HEAPF32[(b + 12) >> 2];
        free(b);
    };
    setRet('sdl_kmp_GamepadHasSensor');
    globalThis.sdl_kmp_GetGamepadSensorData = (g, type) => {
        const b = malloc(12);
        R32[3] = Module._sdl_kmp_GetGamepadSensorData(g, type, b, 3);
        RF32[0] = Module.HEAPF32[b >> 2]; RF32[1] = Module.HEAPF32[(b + 4) >> 2]; RF32[2] = Module.HEAPF32[(b + 8) >> 2];
        free(b);
    };
    setRet('sdl_kmp_GetGamepadSensorDataRate');
    setRet('sdl_kmp_GamepadRumble');

    /* ------------------------------------------------------------------
     * Filesystem / misc
     * ------------------------------------------------------------------ */
    globalThis.sdl_kmp_GetBasePath = () => { const p = Module._sdl_kmp_GetBasePath(); const r = readCString(p); Module._sdl_kmp_Free(p); return r; };
    globalThis.sdl_kmp_GetPrefPath = (o, a) => { const p = allocStr(o), q = allocStr(a); const rp = Module._sdl_kmp_GetPrefPath(p, q); freeStr(p); freeStr(q); const r = readCString(rp); Module._sdl_kmp_Free(rp); return r; };
    globalThis.sdl_kmp_GetUserFolder = (f) => { const p = Module._sdl_kmp_GetUserFolder(f); const r = readCString(p); Module._sdl_kmp_Free(p); return r; };
    globalThis.sdl_kmp_CreateDirectory = (p) => { const c = allocStr(p); const r = Module._sdl_kmp_CreateDirectory(c); freeStr(c); return r; };
    globalThis.sdl_kmp_RemovePath = (p) => { const c = allocStr(p); const r = Module._sdl_kmp_RemovePath(c); freeStr(c); return r; };
    globalThis.sdl_kmp_RenamePath = (a, b) => { const p = allocStr(a), q = allocStr(b); const r = Module._sdl_kmp_RenamePath(p, q); freeStr(p); freeStr(q); return r; };
    globalThis.sdl_kmp_GetPowerInfo = () => {
        const b = malloc(8);
        const st = Module._sdl_kmp_GetPowerInfo(b, b + 4);
        R32[0] = st; R32[1] = Module.HEAP32[b >> 2]; R32[2] = Module.HEAP32[(b + 4) >> 2];
        free(b);
    };
    globalThis.sdl_kmp_OpenURL = (u) => { const c = allocStr(u); const r = Module._sdl_kmp_OpenURL(c); freeStr(c); return r; };
    globalThis.sdl_kmp_ShowSimpleMessageBox = (t, m) => { const p = allocStr(t), q = allocStr(m); const r = Module._sdl_kmp_ShowSimpleMessageBox(p, q); freeStr(p); freeStr(q); return r; };
    globalThis.sdl_kmp_ShowMessageBox = (flags, title, msg, bflags, bids, btexts, count) => {
        const t = allocStr(title), m = allocStr(msg);
        const fbuf = malloc(count * 4), ibuf = malloc(count * 4);
        Module.HEAP32.set(bflags, fbuf >> 2); Module.HEAP32.set(bids, ibuf >> 2);
        const tbuf = malloc(count * 4);
        for (let i = 0; i < count; i++) Module.HEAPU32[(tbuf >> 2) + i] = allocStr(btexts[i]);
        const r = Module._sdl_kmp_ShowMessageBox(flags, t, m, fbuf, ibuf, tbuf, count);
        for (let i = 0; i < count; i++) freeStr(Module.HEAPU32[(tbuf >> 2) + i]);
        freeStr(t); freeStr(m); free(fbuf); free(ibuf); free(tbuf);
        return r;
    };

    /* ------------------------------------------------------------------
     * Logging
     * ------------------------------------------------------------------ */
    globalThis.sdl_kmp_Log = (pr, cat, msg) => { const c = allocStr(msg); Module._sdl_kmp_Log(pr, cat, c); freeStr(c); };
    setRet('sdl_kmp_LogSetPriority');
    setRet('sdl_kmp_LogGetPriority');
    setRet('sdl_kmp_LogSetAllPriority');
    setRet('sdl_kmp_LogResetPriorities');

    /* ------------------------------------------------------------------
     * Threads / synchronization
     * ------------------------------------------------------------------ */
    setRet('sdl_kmp_GetNumLogicalCPUCores');
    setRet('sdl_kmp_GetCurrentThreadID');
    setRet('sdl_kmp_CreateMutex');
    setRet('sdl_kmp_LockMutex');
    setRet('sdl_kmp_TryLockMutex');
    setRet('sdl_kmp_UnlockMutex');
    setRet('sdl_kmp_DestroyMutex');
    setRet('sdl_kmp_CreateRWLock');
    setRet('sdl_kmp_LockRWLockRead');
    setRet('sdl_kmp_TryLockRWLockRead');
    setRet('sdl_kmp_UnlockRWLockRead');
    setRet('sdl_kmp_LockRWLockWrite');
    setRet('sdl_kmp_TryLockRWLockWrite');
    setRet('sdl_kmp_UnlockRWLockWrite');
    setRet('sdl_kmp_DestroyRWLock');
    setRet('sdl_kmp_CreateSemaphore');
    setRet('sdl_kmp_WaitSemaphore');
    setRet('sdl_kmp_TryWaitSemaphore');
    setRet('sdl_kmp_WaitSemaphoreTimeout');
    setRet('sdl_kmp_PostSemaphore');
    setRet('sdl_kmp_GetSemaphoreValue');
    setRet('sdl_kmp_DestroySemaphore');
    setRet('sdl_kmp_CreateCondition');
    setRet('sdl_kmp_WaitCondition');
    setRet('sdl_kmp_SignalCondition');
    setRet('sdl_kmp_BroadcastCondition');
    setRet('sdl_kmp_DestroyCondition');
    setRet('sdl_kmp_CreateThread');

    /* ------------------------------------------------------------------
     * IO streams
     * ------------------------------------------------------------------ */
    globalThis.sdl_kmp_IOFromFile = (p, m) => { const a = allocStr(p), b = allocStr(m); const r = Module._sdl_kmp_IOFromFile(a, b); freeStr(a); freeStr(b); return r; };
    globalThis.sdl_kmp_IOFromMem = (data, size) => {
        const b = malloc(size || 1);
        if (data) Module.HEAPU8.set(data, b);
        const r = Module._sdl_kmp_IOFromMem(b, size);
        return r;
    };
    globalThis.sdl_kmp_IOFromConstMem = (data, size) => {
        const b = malloc(size || 1);
        if (data) Module.HEAPU8.set(data, b);
        const r = Module._sdl_kmp_IOFromConstMem(b, size);
        free(b);
        return r;
    };
    globalThis.sdl_kmp_IORead = (io, out) => {
        const b = malloc(out.byteLength || 1);
        const n = Module._sdl_kmp_IORead(io, b, out.byteLength);
        if (n > 0) out.set(Module.HEAPU8.subarray(b, b + n));
        free(b);
        return n;
    };
    globalThis.sdl_kmp_IOWrite = (io, data, size) => {
        const b = malloc(size);
        Module.HEAPU8.set(data, b);
        const n = Module._sdl_kmp_IOWrite(io, b, size);
        free(b);
        return n;
    };
    setRet('sdl_kmp_IOSeek');
    setRet('sdl_kmp_IOTell');
    setRet('sdl_kmp_IOStreamSize');
    setRet('sdl_kmp_IOFlush');
    setRet('sdl_kmp_IOClose');
    globalThis.sdl_kmp_LoadFileToMem = (p) => { const c = allocStr(p); const r = Module._sdl_kmp_LoadFileToMem(c); freeStr(c); return r; };
    setRet('sdl_kmp_LoadFileSize');
    setRet('sdl_kmp_LoadFileData');
    setRet('sdl_kmp_LoadFileFree');

    /* ------------------------------------------------------------------
     * Properties
     * ------------------------------------------------------------------ */
    setRet('sdl_kmp_CreateProperties');
    setRet('sdl_kmp_SetProperty');
    globalThis.sdl_kmp_SetStringProperty = (p, n, v) => { const a = allocStr(n), b = v === null ? 0 : allocStr(v); const r = Module._sdl_kmp_SetStringProperty(p, a, b); freeStr(a); freeStr(b); return r; };
    setRet('sdl_kmp_GetProperty');
    globalThis.sdl_kmp_GetStringProperty = (p, n) => { const a = allocStr(n); const r = readCString(Module._sdl_kmp_GetStringProperty(p, a)); freeStr(a); return r; };
    globalThis.sdl_kmp_HasProperty = (p, n) => { const a = allocStr(n); const r = Module._sdl_kmp_HasProperty(p, a); freeStr(a); return r; };
    globalThis.sdl_kmp_DeleteProperty = (p, n) => { const a = allocStr(n); const r = Module._sdl_kmp_DeleteProperty(p, a); freeStr(a); return r; };
    setRet('sdl_kmp_CopyProperties');
    setRet('sdl_kmp_GetGlobalProperties');
    setRet('sdl_kmp_DestroyProperties');

    /* ------------------------------------------------------------------
     * Camera / sensor
     * ------------------------------------------------------------------ */
    setRet('sdl_kmp_RefreshCameras');
    setRet('sdl_kmp_GetCameraDevice');
    globalThis.sdl_kmp_GetCameraDeviceName = (id) => { const p = Module._sdl_kmp_GetCameraDeviceName(id); const r = readCString(p); Module._sdl_kmp_Free(p); return r; };
    setRet('sdl_kmp_GetCameraDevicePosition');
    setRet('sdl_kmp_RefreshCameraFormats');
    globalThis.sdl_kmp_GetCameraFormatSpec = (i) => {
        const b = malloc(16);
        Module._sdl_kmp_GetCameraFormatSpec(i, b, b + 4, b + 8, b + 12);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        R32[2] = Module.HEAP32[(b + 8) >> 2]; R32[3] = Module.HEAP32[(b + 12) >> 2];
        free(b);
    };
    setRet('sdl_kmp_OpenCamera');
    globalThis.sdl_kmp_GetCameraFormat = (c) => {
        const b = malloc(16);
        R32[4] = Module._sdl_kmp_GetCameraFormat(c, b, b + 4, b + 8, b + 12);
        R32[0] = Module.HEAP32[b >> 2]; R32[1] = Module.HEAP32[(b + 4) >> 2];
        R32[2] = Module.HEAP32[(b + 8) >> 2]; R32[3] = Module.HEAP32[(b + 12) >> 2];
        free(b);
    };
    setRet('sdl_kmp_GetCameraPermissionState');
    setRet('sdl_kmp_GetCameraSupportsFormat');
    setRet('sdl_kmp_AcquireCameraFrame');
    setRet('sdl_kmp_ReleaseCameraFrame');
    setRet('sdl_kmp_CloseCamera');
    setRet('sdl_kmp_RefreshSensors');
    setRet('sdl_kmp_GetSensorDevice');
    globalThis.sdl_kmp_GetSensorDeviceName = (id) => { const p = Module._sdl_kmp_GetSensorDeviceName(id); const r = readCString(p); Module._sdl_kmp_Free(p); return r; };
    setRet('sdl_kmp_GetSensorDeviceType');
    setRet('sdl_kmp_OpenSensor');
    setRet('sdl_kmp_CloseSensor');
    globalThis.sdl_kmp_GetSensorName = (s) => readCString(Module._sdl_kmp_GetSensorName(s));
    setRet('sdl_kmp_GetSensorType');
    globalThis.sdl_kmp_GetSensorData = (s, out) => {
        const b = malloc(out.byteLength || 1);
        const rc = Module._sdl_kmp_GetSensorData(s, b, out.byteLength >> 2);
        out.set(Module.HEAPF32.subarray(b >> 2, (b >> 2) + (out.byteLength >> 2)));
        free(b);
        return rc;
    };
    setRet('sdl_kmp_GetNumHapticDevices');

    /* ------------------------------------------------------------------
     * OpenGL
     * ------------------------------------------------------------------ */
    globalThis.sdl_kmp_GL_LoadLibrary = (p) => { const c = p === null ? 0 : allocStr(p); const r = Module._sdl_kmp_GL_LoadLibrary(c); freeStr(c); return r; };
    setRet('sdl_kmp_GL_UnloadLibrary');
    globalThis.sdl_kmp_GL_GetProcAddress = (p) => { const c = allocStr(p); const r = Module._sdl_kmp_GL_GetProcAddress(c); freeStr(c); return r; };
    globalThis.sdl_kmp_GL_ExtensionSupported = (e) => { const c = allocStr(e); const r = Module._sdl_kmp_GL_ExtensionSupported(c); freeStr(c); return r; };
    setRet('sdl_kmp_GL_ResetAttributes');
    setRet('sdl_kmp_GL_SetAttribute');
    globalThis.sdl_kmp_GL_GetAttribute = (a) => {
        const b = malloc(4);
        R32[1] = Module._sdl_kmp_GL_GetAttribute(a, b);
        R32[0] = Module.HEAP32[b >> 2];
        free(b);
    };
    setRet('sdl_kmp_GL_CreateContext');
    setRet('sdl_kmp_GL_MakeCurrent');
    setRet('sdl_kmp_GL_GetCurrentWindow');
    setRet('sdl_kmp_GL_GetCurrentContext');
    setRet('sdl_kmp_GL_SetSwapInterval');
    globalThis.sdl_kmp_GL_GetSwapInterval = () => {
        const b = malloc(4);
        R32[1] = Module._sdl_kmp_GL_GetSwapInterval(b);
        R32[0] = Module.HEAP32[b >> 2];
        free(b);
    };
    setRet('sdl_kmp_GL_SwapWindow');
    setRet('sdl_kmp_GL_DestroyContext');

    /* ------------------------------------------------------------------
     * Vulkan
     * ------------------------------------------------------------------ */
    globalThis.sdl_kmp_Vulkan_LoadLibrary = (p) => { const c = p === null ? 0 : allocStr(p); const r = Module._sdl_kmp_Vulkan_LoadLibrary(c); freeStr(c); return r; };
    setRet('sdl_kmp_Vulkan_UnloadLibrary');
    setRet('sdl_kmp_Vulkan_GetVkGetInstanceProcAddr');
    setRet('sdl_kmp_Vulkan_GetInstanceExtensions');
    globalThis.sdl_kmp_Vulkan_GetInstanceExtension = (i) => readCString(Module._sdl_kmp_Vulkan_GetInstanceExtension(i));
    setRet('sdl_kmp_Vulkan_CreateSurface');
    setRet('sdl_kmp_Vulkan_DestroySurface');
    setRet('sdl_kmp_Vulkan_GetPresentationSupport');

    /* ------------------------------------------------------------------
     * GPU (unsupported on wasm)
     * ------------------------------------------------------------------ */
    setRet('sdl_kmp_GPU_IsSupported');
    setRet('sdl_kmp_GPU_GetNumDrivers');

    return Module;
}
