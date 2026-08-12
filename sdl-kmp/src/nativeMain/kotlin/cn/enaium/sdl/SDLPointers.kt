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
import cnames.structs.SDL_Camera
import cnames.structs.SDL_Condition
import cnames.structs.SDL_Gamepad
import cnames.structs.SDL_GPUCommandBuffer
import cnames.structs.SDL_GPUBuffer
import cnames.structs.SDL_GPUDevice
import cnames.structs.SDL_GPUFence
import cnames.structs.SDL_GPUGraphicsPipeline
import cnames.structs.SDL_GPURenderPass
import cnames.structs.SDL_GPUSampler
import cnames.structs.SDL_GPUShader
import cnames.structs.SDL_GPUTexture
import cnames.structs.SDL_Haptic
import cnames.structs.SDL_IOStream
import cnames.structs.SDL_Joystick
import cnames.structs.SDL_Mutex
import cnames.structs.SDL_Process
import cnames.structs.SDL_RWLock
import cnames.structs.SDL_Renderer
import cnames.structs.SDL_Semaphore
import cnames.structs.SDL_Sensor
import cnames.structs.SDL_Thread
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

/** The typed cinterop `SDL_Thread` pointer of this thread, or null once closed. */
val SDLThread.nativePtr: CPointer<SDL_Thread>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_Mutex` pointer of this mutex, or null once closed. */
val SDLMutex.nativePtr: CPointer<SDL_Mutex>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_RWLock` pointer of this lock, or null once closed. */
val SDLRWLock.nativePtr: CPointer<SDL_RWLock>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_Semaphore` pointer of this semaphore, or null once closed. */
val SDLSemaphore.nativePtr: CPointer<SDL_Semaphore>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_Condition` pointer of this condition, or null once closed. */
val SDLCondition.nativePtr: CPointer<SDL_Condition>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_IOStream` pointer of this stream, or null once closed. */
val SDLIOStream.nativePtr: CPointer<SDL_IOStream>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_Process` pointer of this process, or null once closed. */
val SDLProcess.nativePtr: CPointer<SDL_Process>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_Camera` pointer of this camera, or null once closed. */
val SDLCamera.nativePtr: CPointer<SDL_Camera>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_Sensor` pointer of this sensor, or null once closed. */
val SDLSensor.nativePtr: CPointer<SDL_Sensor>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_Haptic` pointer of this haptic device, or null once closed. */
val SDLHaptic.nativePtr: CPointer<SDL_Haptic>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_GPUDevice` pointer of this device, or null once closed. */
val SDLGPUDevice.nativePtr: CPointer<SDL_GPUDevice>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_GPUTexture` pointer of this texture, or null once closed. */
val SDLGPUTexture.nativePtr: CPointer<SDL_GPUTexture>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_GPUBuffer` pointer of this buffer, or null once closed. */
val SDLGPUBuffer.nativePtr: CPointer<SDL_GPUBuffer>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_GPUShader` pointer of this shader, or null once closed. */
val SDLGPUShader.nativePtr: CPointer<SDL_GPUShader>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_GPUGraphicsPipeline` pointer of this pipeline, or null once closed. */
val SDLGPUGraphicsPipeline.nativePtr: CPointer<SDL_GPUGraphicsPipeline>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_GPUSampler` pointer of this sampler, or null once closed. */
val SDLGPUSampler.nativePtr: CPointer<SDL_GPUSampler>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_GPUCommandBuffer` pointer of this command buffer, or null once closed. */
val SDLGPUCommandBuffer.nativePtr: CPointer<SDL_GPUCommandBuffer>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_GPURenderPass` pointer of this render pass, or null once closed. */
val SDLGPURenderPass.nativePtr: CPointer<SDL_GPURenderPass>?
    get() = ptr.typedPointer()

/** The typed cinterop `SDL_GPUFence` pointer of this fence, or null once closed. */
val SDLGPUFence.nativePtr: CPointer<SDL_GPUFence>?
    get() = ptr.typedPointer()
