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

/**
 * A software pixel buffer.
 *
 * The surface owns its pixels; call [close] to release it. [lock] must be
 * called before reading or writing [pixels], and [unlock] afterwards.
 */
interface SDLSurface : AutoCloseable {

    /** The surface width in pixels. */
    val width: Int

    /** The raw SDL handle address, or 0 after [close]. On native, [cn.enaium.sdl.nativePtr] converts it to the typed pointer. */
    val ptr: Long

    /** The surface height in pixels. */
    val height: Int

    /** The pixel format, see [SDLPixelFormat]. */
    val format: Int

    /** The surface colorspace, or 0 (SDL_COLORSPACE_UNKNOWN). */
    val colorspace: Int

    /** The number of bytes per row. */
    val pitch: Int

    /** The pixel data (only valid between [lock] and [unlock]). */
    val pixels: ByteArray

    /** Locks the surface for direct pixel access; returns `false` on failure. */
    fun lock(): Boolean

    /** Unlocks the surface. */
    fun unlock()

    /** Fills [rect] (null = whole surface) with [color]. */
    fun fillRect(rect: SDLRect?, color: SDLColor): Boolean

    /** Fills all [rects] with [color]; returns `false` on failure. */
    fun fillRects(rects: List<SDLRect>, color: SDLColor): Boolean

    /** Blits [src] (null = whole surface) of this surface onto [dst]. */
    fun blit(src: SDLRect?, dst: SDLSurface, dstRect: SDLRect?): Boolean

    /** Blits and scales [src] (null = whole surface) onto [dstRect]. */
    fun blitScaled(
        src: SDLRect?,
        dst: SDLSurface,
        dstRect: SDLRect?,
        scaleMode: Int = SDLScaleMode.NEAREST,
    ): Boolean

    /** Saves the surface as a BMP file; returns `false` on failure. */
    fun saveBMP(path: String): Boolean

    /** Converts the surface to [format], returning a new surface. */
    fun convert(format: Int): SDLSurface

    /** Releases the surface. */
    override fun close()
}

/** The mouse position and button state. */
data class SDLMouseState(val x: Float, val y: Float, val buttons: Int)
