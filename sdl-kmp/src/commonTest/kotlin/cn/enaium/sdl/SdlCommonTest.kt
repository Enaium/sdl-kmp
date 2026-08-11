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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SDLCommonTest {

    @Test
    fun version() {
        val version = SDL.version()
        assertEquals(3, version.major)
        assertTrue(version.minor >= 2, "expected SDL 3.2+, got $version")
        assertNotNull(SDL.revision())
    }

    @Test
    fun initAndQuit() {
        SDL.setMainReady()
        assertTrue(SDL.init(SDLInitFlags.EVENTS), "SDL_Init failed: ${SDL.error()}")
        assertNotEquals(0, SDL.wasInit(SDLInitFlags.EVENTS) and SDLInitFlags.EVENTS)
        assertTrue(SDL.initSubSystem(SDLInitFlags.EVENTS))
        SDL.quitSubSystem(SDLInitFlags.EVENTS)
        SDL.quit()
    }

    @Test
    fun errorRoundTrip() {
        SDL.clearError()
        assertEquals(null, SDL.error())
        // SDL_SetError is variadic; LWJGL's wrapper calls it through the
        // non-variadic ABI and is not guaranteed to succeed on all platforms.
        if (SDL.setError("kotlin sdl error")) {
            assertEquals("kotlin sdl error", SDL.error())
            SDL.clearError()
            assertEquals(null, SDL.error())
        }
    }

    @Test
    fun delay() {
        // SDL3 timers work without any subsystem initialization.
        SDL.setMainReady()
        val before = SDL.getTicks()
        SDL.delay(30)
        val after = SDL.getTicks()
        assertTrue(after >= before, "ticks went backwards")
        assertTrue(after - before >= 30u, "delay(30) returned too early: ${after - before}ms")
    }

    @Test
    fun performanceCounter() {
        assertTrue(SDL.performanceFrequency() > 0u)
        val a = SDL.performanceCounter()
        val b = SDL.performanceCounter()
        assertTrue(b >= a)
    }

    @Test
    fun hints() {
        SDL.setMainReady()
        assertTrue(SDL.setHint("SDL_TEST_HINT", "value"))
        assertEquals("value", SDL.getHint("SDL_TEST_HINT"))
    }
}
