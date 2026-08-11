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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SDLWindowNativeTest {

    @Test
    fun windowAndRenderer() {
        SDL.setMainReady()
        // The dummy driver works headless (CI without a display server).
        assertTrue(SDL.setHint("SDL_VIDEO_DRIVER", "dummy"))
        assertTrue(SDL.init(SDLInitFlags.VIDEO), "SDL_Init(VIDEO) failed: ${SDL.error()}")
        assertEquals("dummy", SDL.getCurrentVideoDriver())

        SDL.createWindow("native test", 320, 240).use { window ->
            assertEquals("native test", window.title)
            assertEquals(SDLPoint(320, 240), window.size)

            SDL.createRenderer(window).use { renderer ->
                assertNotNull(renderer.name)
                assertEquals(SDLPoint(320, 240), renderer.outputSize)

                renderer.drawColor = SDLColor(255, 0, 0, 255)
                assertEquals(SDLColor(255, 0, 0, 255), renderer.drawColor)

                assertTrue(renderer.clear())
                assertTrue(renderer.fillRect(SDLRect(10, 10, 50, 50)))
                assertTrue(renderer.drawRect(SDLRect(20, 20, 30, 30)))
                assertTrue(renderer.drawLine(0, 0, 100, 100))
                renderer.present()
            }
        }

        SDL.quit()
    }

    @Test
    fun eventLoopDummyDriver() {
        SDL.setMainReady()
        assertTrue(SDL.setHint("SDL_VIDEO_DRIVER", "dummy"))
        assertTrue(SDL.init(SDLInitFlags.VIDEO))
        SDL.createWindow("event test", 160, 120).use { window ->
            val deadline = SDL.getTicks() + 200u
            while (SDL.getTicks() < deadline) {
                val event = SDL.pollEvent()
                if (event is SDLEvent.Unknown) {
                    // no-op; unknown event types must not crash the poll loop
                }
                SDL.delay(5)
            }
        }
        SDL.quit()
    }
}
