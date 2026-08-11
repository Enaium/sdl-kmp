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

class SDLExtendedApiNativeTest {

    @Test
    fun texturesAndWindowState() {
        SDL.setMainReady()
        assertTrue(SDL.setHint("SDL_VIDEO_DRIVER", "dummy"))
        assertTrue(SDL.init(SDLInitFlags.VIDEO))

        SDL.createWindow("extended native", 320, 240).use { window ->
            // window position round-trip
            window.position = SDLPoint(40, 50)
            assertEquals(SDLPoint(40, 50), window.position)
            assertEquals(SDLPoint(320, 240), window.sizeInPixels)

            SDL.createRenderer(window).use { renderer ->
                // renderer state
                assertEquals(SDLPoint(320, 240), renderer.currentOutputSize)
                renderer.scale = SDLFloatPoint(1f, 1f)
                renderer.vsync = false
                renderer.blendMode = SDLBlendMode.NONE

                // texture create / update / render
                val texture = renderer.createTexture(
                    format = SDLPixelFormat.RGBA8888,
                    access = SDLTextureAccess.STREAMING,
                    width = 16,
                    height = 16,
                )
                val pixels = ByteArray(16 * 16 * 4) { 128.toByte() }
                assertTrue(texture.update(null, pixels, 16 * 4))
                assertTrue(renderer.renderTexture(texture, dst = SDLFRect(0f, 0f, 16f, 16f)))
                assertTrue(
                    renderer.renderTextureRotated(
                        texture = texture,
                        dst = SDLFRect(50f, 50f, 16f, 16f),
                        angle = 45.0,
                        center = SDLFloatPoint(8f, 8f),
                    ),
                )
                renderer.present()
                texture.close()
            }
        }

        SDL.quit()
    }

    @Test
    fun pixelsAndSurfaces() {
        SDL.setMainReady()
        val red = SDL.mapRGBA(SDLPixelFormat.RGBA8888, 255, 0, 0, 255)
        assertEquals(0xFF0000FF.toInt(), red)
        assertEquals(
            SDLColor(255, 0, 0, 255),
            SDL.getRGBA(SDLPixelFormat.RGBA8888, red),
        )
        assertTrue(SDL.getPixelFormatName(SDLPixelFormat.RGBA8888)!!.contains("RGBA8888"))

        SDL.createSurface(8, 8, SDLPixelFormat.RGBA8888).use { surface ->
            assertEquals(SDLPoint(8, 8), SDLPoint(surface.width, surface.height))
            assertTrue(surface.fillRect(null, SDLColor(255, 0, 0, 255)))
        }
    }

    @Test
    fun miscApi() {
        SDL.setMainReady()
        assertTrue(SDL.init(SDLInitFlags.EVENTS))
        assertTrue(SDL.getHintBoolean("SDL_HINT_VIDEO_DRIVER", false) == false)
        SDL.setHint("SDL_TEST_HINT2", "1")
        assertTrue(SDL.getHintBoolean("SDL_TEST_HINT2", false))
        assertEquals(0, SDL.modState and SDLKeymod.NONE)
        assertTrue(SDL.getKeyName(SDLKeycode.ESCAPE) != null)
        assertTrue(SDL.getScancodeName(41) != null) // SDL_SCANCODE_ESCAPE
        SDL.quit()
    }
}

class SDLGLApiTest {

    @Test
    fun glAttributes() {
        SDL.setMainReady()
        assertTrue(SDL.init(SDLInitFlags.VIDEO))
        // GL attributes require a real OpenGL-capable video driver; the
        // dummy driver used on CI does not support them.
        if (SDL.getCurrentVideoDriver() != "dummy") {
            assertTrue(SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MAJOR_VERSION, 3))
            assertEquals(3, SDL.glGetAttribute(SDLGLAttribute.CONTEXT_MAJOR_VERSION))
            assertTrue(SDL.glSetAttribute(SDLGLAttribute.CONTEXT_PROFILE_MASK, SDLGLProfile.CORE))
            assertEquals(SDLGLProfile.CORE, SDL.glGetAttribute(SDLGLAttribute.CONTEXT_PROFILE_MASK))
            SDL.glResetAttributes()
        }
        SDL.quit()
    }
}
