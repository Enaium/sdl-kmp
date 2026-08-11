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

class SdlWindowJvmTest {

    @Test
    fun windowAndRenderer() {
        Sdl.setMainReady()
        // The dummy driver works headless (CI without a display server).
        assertTrue(Sdl.setHint("SDL_VIDEO_DRIVER", "dummy"))
        assertTrue(Sdl.init(SdlInitFlags.VIDEO), "SDL_Init(VIDEO) failed: ${Sdl.error()}")
        assertEquals("dummy", Sdl.getCurrentVideoDriver())

        Sdl.createWindow("jvm test", 320, 240).use { window ->
            assertEquals("jvm test", window.title)
            assertEquals(SdlPoint(320, 240), window.size)

            Sdl.createRenderer(window).use { renderer ->
                assertNotNull(renderer.name)
                assertEquals(SdlPoint(320, 240), renderer.outputSize)

                renderer.drawColor = SdlColor(255, 0, 0, 255)
                assertEquals(SdlColor(255, 0, 0, 255), renderer.drawColor)

                assertTrue(renderer.clear())
                assertTrue(renderer.fillRect(SdlRect(10, 10, 50, 50)))
                assertTrue(renderer.drawRect(SdlRect(20, 20, 30, 30)))
                assertTrue(renderer.drawLine(0, 0, 100, 100))
                renderer.present()
            }
        }

        Sdl.quit()
    }

    @Test
    fun eventLoopDummyDriver() {
        Sdl.setMainReady()
        assertTrue(Sdl.setHint("SDL_VIDEO_DRIVER", "dummy"))
        assertTrue(Sdl.init(SdlInitFlags.VIDEO))
        Sdl.createWindow("event test", 160, 120).use { window ->
            val deadline = Sdl.getTicks() + 200u
            while (Sdl.getTicks() < deadline) {
                Sdl.pollEvent()
                Sdl.delay(5)
            }
        }
        Sdl.quit()
    }
}
