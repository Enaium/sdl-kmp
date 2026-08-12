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

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cn.enaium.sdl

import cnames.structs.SDL_AudioStream
import cnames.structs.SDL_Gamepad
import cnames.structs.SDL_Joystick
import cnames.structs.SDL_Renderer
import cnames.structs.SDL_Window
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.toCPointer
import sdl3.SDL_Event
import sdl3.SDL_Surface
import sdl3.SDL_Texture

private fun <T : CPointed> Long.typedPointer(): CPointer<T>? =
    if (this == 0L) null else toCPointer<T>()

/** The typed cinterop `SDL_Window` pointer of this window, or null once closed. */
val SDLWindow.nativePtr: CPointer<SDL_Window>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_Renderer` pointer of this renderer, or null once closed. */
val SDLRenderer.nativePtr: CPointer<SDL_Renderer>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_Texture` pointer of this texture, or null once closed. */
val SDLTexture.nativePtr: CPointer<SDL_Texture>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_Surface` pointer of this surface, or null once closed. */
val SDLSurface.nativePtr: CPointer<SDL_Surface>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_AudioStream` pointer of this stream, or null once closed. */
val SDLAudioStream.nativePtr: CPointer<SDL_AudioStream>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_Joystick` pointer of this joystick, or null once closed. */
val SDLJoystick.nativePtr: CPointer<SDL_Joystick>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_Gamepad` pointer of this gamepad, or null once closed. */
val SDLGamepad.nativePtr: CPointer<SDL_Gamepad>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_Event` pointer of this raw event, or null once closed. */
val SDLEventRaw.nativePtr: CPointer<SDL_Event>?
    get() = ptr.typedPointer()
