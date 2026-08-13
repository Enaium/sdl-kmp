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

package cn.enaium.sdl

import org.khronos.webgl.Int32Array
import org.khronos.webgl.toInt32Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toInt8Array
import kotlin.js.ExperimentalJsExport

/** SDL_Surface backed by the Emscripten SDL3 module. */
internal class WasmSurface(handle: Int, private val owned: Boolean) : SDLSurface {
    internal var surface: Int = handle

    override var ptr: Long
        get() = if (surface == 0) 0L else surface.toLong()
        set(value) { surface = value.toInt() }

    override val width: Int get() = sdl_kmp_GetSurfaceWidth(surface)
    override val height: Int get() = sdl_kmp_GetSurfaceHeight(surface)
    override val format: Int get() = sdl_kmp_GetSurfaceFormat(surface)
    override val colorspace: Int get() = sdl_kmp_GetSurfaceColorspace(surface)
    override val pitch: Int get() = sdl_kmp_GetSurfacePitch(surface)

    private var lockedPixels: ByteArray? = null

    override val pixels: ByteArray
        get() = lockedPixels ?: throw IllegalStateException("surface is not locked")

    override fun lock(): Boolean {
        if (sdl_kmp_LockSurface(surface) == 0) return false
        val size = pitch * height
        val ptr = sdl_kmp_GetSurfacePixels(surface)
        lockedPixels = if (size > 0 && ptr != 0) {
            sdlKmpHeapBytes(ptr, size).toByteArray()
        } else {
            ByteArray(0)
        }
        return true
    }

    override fun unlock() {
        // The pixels array may have been mutated in place; copy it back.
        val data = lockedPixels
        if (data != null && data.isNotEmpty()) {
            val ptr = sdl_kmp_GetSurfacePixels(surface)
            if (ptr != 0) sdlKmpSetHeapBytes(ptr, data.toInt8Array())
        }
        sdl_kmp_UnlockSurface(surface)
        lockedPixels = null
    }

    override fun fillRect(rect: SDLRect?, color: SDLColor): Boolean {
        val px = SDL.mapRGBA(format, color.r, color.g, color.b, color.a)
        return if (rect == null) {
            sdl_kmp_FillSurfaceRect(surface, 0, 0, 0, 0, 0, px) != 0
        } else {
            sdl_kmp_FillSurfaceRect(surface, 1, rect.x, rect.y, rect.width, rect.height, px) != 0
        }
    }

    override fun fillRects(rects: List<SDLRect>, color: SDLColor): Boolean {
        if (rects.isEmpty()) return true
        val px = SDL.mapRGBA(format, color.r, color.g, color.b, color.a)
        val arr = IntArray(rects.size * 4).also { a ->
            rects.forEachIndexed { i, r ->
                a[i * 4] = r.x; a[i * 4 + 1] = r.y; a[i * 4 + 2] = r.width; a[i * 4 + 3] = r.height
            }
        }
        return sdl_kmp_FillSurfaceRects(surface, arr.toInt32Array(), rects.size, px) != 0
    }

    override fun blit(src: SDLRect?, dst: SDLSurface, dstRect: SDLRect?): Boolean {
        val d = (dst as? WasmSurface)?.surface ?: return false
        return sdl_kmp_BlitSurface(
            surface, if (src != null) 1 else 0, src?.x ?: 0, src?.y ?: 0, src?.width ?: 0, src?.height ?: 0,
            d, if (dstRect != null) 1 else 0, dstRect?.x ?: 0, dstRect?.y ?: 0, dstRect?.width ?: 0, dstRect?.height ?: 0,
        ) != 0
    }

    override fun blitScaled(src: SDLRect?, dst: SDLSurface, dstRect: SDLRect?, scaleMode: Int): Boolean {
        val d = (dst as? WasmSurface)?.surface ?: return false
        return sdl_kmp_BlitSurfaceScaled(
            surface, if (src != null) 1 else 0, src?.x ?: 0, src?.y ?: 0, src?.width ?: 0, src?.height ?: 0,
            d, if (dstRect != null) 1 else 0, dstRect?.x ?: 0, dstRect?.y ?: 0, dstRect?.width ?: 0, dstRect?.height ?: 0,
            scaleMode,
        ) != 0
    }

    override fun saveBMP(path: String): Boolean = sdl_kmp_SaveBMP(surface, path) != 0

    override fun convert(format: Int): SDLSurface {
        val s = sdl_kmp_ConvertSurface(surface, format)
        check(s != 0) { "SDL_ConvertSurface failed: ${SDL.error()}" }
        return WasmSurface(s, owned = true)
    }

    override fun close() {
        if (surface != 0) {
            if (owned) sdl_kmp_DestroySurface(surface)
            surface = 0
        }
    }
}
