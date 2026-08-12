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
 * Raw SDL object handles.
 *
 * Every wrapper in this binding exposes the raw SDL3 handle it holds through
 * a `ptr` property containing the handle's memory address (`0` once the
 * object is closed). This lets other native libraries (e.g. an imgui binding)
 * interoperate with SDL directly:
 *
 *  - on the JVM the value is already the address LWJGL uses for the handle;
 *  - on native targets cast it back to the typed cinterop pointer, e.g.
 *    `ptr.toULong().toCPointer<SDL_Window>()` (see the `nativePtr`
 *    extension properties in nativeMain for a ready-made conversion).
 */

/**
 * A raw `SDL_Event`, as returned by [SDL.pollEventRaw] / [SDL.waitEventRaw].
 *
 * It owns the underlying event storage; call [close] (or use `use`) when the
 * event is no longer needed to release it.
 */
interface SDLEventRaw : AutoCloseable {
    /** Address of the raw `SDL_Event`, or 0 after [close]. */
    val ptr: Long

    /** Releases the raw event storage. */
    override fun close()
}
