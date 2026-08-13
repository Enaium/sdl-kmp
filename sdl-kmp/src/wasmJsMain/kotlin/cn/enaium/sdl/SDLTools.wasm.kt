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

import org.khronos.webgl.Float32Array
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toInt8Array
import org.khronos.webgl.toInt32Array
import org.khronos.webgl.toFloat32Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toFloatArray
import org.khronos.webgl.toIntArray
import kotlin.js.ExperimentalJsExport

// =========================================================================
// Logging
// =========================================================================

actual object SDLLog {
    private var output: SDLLogOutput? = null

    actual fun log(priority: Int, category: Int, message: String) {
        val out = output
        if (out != null) {
            out.log(priority, category, message)
        } else {
            sdl_kmp_Log(priority, category, message)
        }
    }

    actual fun debug(message: String, category: Int) = log(SDLLogPriority.DEBUG, category, message)
    actual fun info(message: String, category: Int) = log(SDLLogPriority.INFO, category, message)
    actual fun warn(message: String, category: Int) = log(SDLLogPriority.WARN, category, message)
    actual fun error(message: String, category: Int) = log(SDLLogPriority.ERROR, category, message)
    actual fun critical(message: String, category: Int) = log(SDLLogPriority.CRITICAL, category, message)

    actual fun setPriority(category: Int, priority: Int) = sdl_kmp_LogSetPriority(category, priority)
    actual fun getPriority(category: Int): Int = sdl_kmp_LogGetPriority(category)
    actual fun setAllPriority(priority: Int) = sdl_kmp_LogSetAllPriority(priority)
    actual fun resetPriorities() = sdl_kmp_LogResetPriorities()

    actual fun setOutputFunction(output: SDLLogOutput?) {
        this.output = output
    }
}

// =========================================================================
// Threads / synchronization
// =========================================================================

private class WasmMutex(handle: Int) : SDLMutex {
    private var mutex: Int = handle

    override var ptr: Long
        get() = if (mutex == 0) 0L else mutex.toLong()
        set(value) { mutex = value.toInt() }

    override fun lock() = sdl_kmp_LockMutex(mutex)
    override fun tryLock(): Boolean = sdl_kmp_TryLockMutex(mutex) != 0
    override fun unlock() = sdl_kmp_UnlockMutex(mutex)

    override fun close() {
        if (mutex != 0) { sdl_kmp_DestroyMutex(mutex); mutex = 0 }
    }
}

private class WasmRWLock(handle: Int) : SDLRWLock {
    private var lock: Int = handle

    override var ptr: Long
        get() = if (lock == 0) 0L else lock.toLong()
        set(value) { lock = value.toInt() }

    override fun lockRead() = sdl_kmp_LockRWLockRead(lock)
    override fun tryLockRead(): Boolean = sdl_kmp_TryLockRWLockRead(lock) != 0
    override fun unlockRead() = sdl_kmp_UnlockRWLockRead(lock)
    override fun lockWrite() = sdl_kmp_LockRWLockWrite(lock)
    override fun tryLockWrite(): Boolean = sdl_kmp_TryLockRWLockWrite(lock) != 0
    override fun unlockWrite() = sdl_kmp_UnlockRWLockWrite(lock)

    override fun close() {
        if (lock != 0) { sdl_kmp_DestroyRWLock(lock); lock = 0 }
    }
}

private class WasmSemaphore(handle: Int) : SDLSemaphore {
    private var semaphore: Int = handle

    override var ptr: Long
        get() = if (semaphore == 0) 0L else semaphore.toLong()
        set(value) { semaphore = value.toInt() }

    override fun acquire() = sdl_kmp_WaitSemaphore(semaphore)
    override fun waitTimeout(timeoutMs: Int): Boolean = sdl_kmp_WaitSemaphoreTimeout(semaphore, timeoutMs) != 0
    override fun tryWait(): Boolean = sdl_kmp_TryWaitSemaphore(semaphore) != 0
    override fun post() = sdl_kmp_PostSemaphore(semaphore)
    override val value: Int get() = sdl_kmp_GetSemaphoreValue(semaphore)

    override fun close() {
        if (semaphore != 0) { sdl_kmp_DestroySemaphore(semaphore); semaphore = 0 }
    }
}

private class WasmCondition(handle: Int) : SDLCondition {
    private var condition: Int = handle

    override var ptr: Long
        get() = if (condition == 0) 0L else condition.toLong()
        set(value) { condition = value.toInt() }

    override fun wait(mutex: SDLMutex) {
        sdl_kmp_WaitCondition(condition, (mutex as? WasmMutex)?.ptr?.toInt() ?: 0, -1)
    }
    override fun waitTimeout(mutex: SDLMutex, timeoutMs: Int): Boolean =
        sdl_kmp_WaitCondition(condition, (mutex as? WasmMutex)?.ptr?.toInt() ?: 0, timeoutMs) != 0

    override fun signal() = sdl_kmp_SignalCondition(condition)
    override fun broadcast() = sdl_kmp_BroadcastCondition(condition)

    override fun close() {
        if (condition != 0) { sdl_kmp_DestroyCondition(condition); condition = 0 }
    }
}

actual object SDLThreads {
    actual val numLogicalCPUCores: Int get() = sdl_kmp_GetNumLogicalCPUCores()

    actual fun createThread(name: String?, fn: (Int) -> Unit): SDLThread? = null

    actual val currentThreadId: Int get() = sdl_kmp_GetCurrentThreadID()

    actual fun getThreadId(thread: SDLThread): Int = 0

    actual fun getThreadName(thread: SDLThread): String? = null

    actual fun setThreadPriority(priority: Int): Boolean = false

    actual fun createMutex(): SDLMutex? = sdl_kmp_CreateMutex().takeIf { it != 0 }?.let { WasmMutex(it) }

    actual fun createRWLock(): SDLRWLock? = sdl_kmp_CreateRWLock().takeIf { it != 0 }?.let { WasmRWLock(it) }

    actual fun createSemaphore(initialValue: Int): SDLSemaphore? =
        sdl_kmp_CreateSemaphore(initialValue).takeIf { it != 0 }?.let { WasmSemaphore(it) }

    actual fun createCondition(): SDLCondition? = sdl_kmp_CreateCondition().takeIf { it != 0 }?.let { WasmCondition(it) }
}

// =========================================================================
// IO streams
// =========================================================================

private class WasmIOStream(handle: Int) : SDLIOStream {
    private var stream: Int = handle

    override var ptr: Long
        get() = if (stream == 0) 0L else stream.toLong()
        set(value) { stream = value.toInt() }

    override fun read(size: Int): ByteArray {
        if (size <= 0) return ByteArray(0)
        val out = Int8Array(size)
        val n = sdl_kmp_IORead(stream, out)
        return if (n <= 0) ByteArray(0) else out.subarray(0, n).toByteArray()
    }

    override fun write(data: ByteArray): Int =
        sdl_kmp_IOWrite(stream, data.toInt8Array(), data.size).toInt()

    override fun seek(offset: Int, whence: Int): Int = sdl_kmp_IOSeek(stream, offset.toDouble(), whence).toInt()

    override fun tell(): Int = sdl_kmp_IOTell(stream).toInt()

    override fun size(): Int = sdl_kmp_IOStreamSize(stream).toInt()

    override fun flush(): Boolean = sdl_kmp_IOFlush(stream) == 0

    override fun close() {
        if (stream != 0) { sdl_kmp_IOClose(stream); stream = 0 }
    }
}

actual object SDLIO {
    actual fun openFile(path: String, mode: String): SDLIOStream? =
        sdl_kmp_IOFromFile(path, mode).takeIf { it != 0 }?.let { WasmIOStream(it) }

    actual fun fromMem(data: ByteArray): SDLIOStream? =
        sdl_kmp_IOFromMem(data.toInt8Array(), data.size).takeIf { it != 0 }?.let { WasmIOStream(it) }

    actual fun fromConstMem(data: ByteArray): SDLIOStream? =
        sdl_kmp_IOFromConstMem(data.toInt8Array(), data.size).takeIf { it != 0 }?.let { WasmIOStream(it) }

    actual fun loadFile(path: String): ByteArray? {
        if (sdl_kmp_LoadFileToMem(path) == 0) return null
        val size = sdl_kmp_LoadFileSize()
        val ptr = sdl_kmp_LoadFileData()
        val data = if (size > 0 && ptr != 0) {
            sdlKmpHeapBytes(ptr, size).toByteArray()
        } else {
            ByteArray(0)
        }
        sdl_kmp_LoadFileFree()
        return data
    }

    actual fun loadFile(stream: SDLIOStream): ByteArray? {
        val s = (stream as? WasmIOStream)?.ptr?.toInt() ?: return null
        val size = sdl_kmp_IOStreamSize(s)
        if (size < 0) return null
        val n = size.toInt()
        if (n <= 0) {
            stream.close()
            return ByteArray(0)
        }
        val out = Int8Array(n)
        val read = sdl_kmp_IORead(s, out)
        stream.close()
        return if (read <= 0) ByteArray(0) else out.subarray(0, read).toByteArray()
    }
}

// =========================================================================
// Properties
// =========================================================================

actual object SDLProperties {
    actual fun create(): ULong = sdl_kmp_CreateProperties().toUInt().toULong()

    actual fun setProperty(props: ULong, name: String, value: Long): Boolean =
        sdl_kmp_SetProperty(props.toInt(), name, value.toDouble()) == 0

    actual fun setStringProperty(props: ULong, name: String, value: String?): Boolean =
        sdl_kmp_SetStringProperty(props.toInt(), name, value) == 0

    actual fun getProperty(props: ULong, name: String, defaultValue: Long): Long =
        sdl_kmp_GetProperty(props.toInt(), name, defaultValue.toDouble()).toLong()

    actual fun getStringProperty(props: ULong, name: String): String? =
        sdl_kmp_GetStringProperty(props.toInt(), name)

    actual fun hasProperty(props: ULong, name: String): Boolean =
        sdl_kmp_HasProperty(props.toInt(), name) != 0

    actual fun deleteProperty(props: ULong, name: String): Boolean =
        sdl_kmp_DeleteProperty(props.toInt(), name) != 0

    actual fun copy(src: ULong, dst: ULong): Boolean =
        sdl_kmp_CopyProperties(src.toInt(), dst.toInt()) == 0

    actual val globalProperties: ULong get() = sdl_kmp_GetGlobalProperties().toUInt().toULong()

    actual fun destroy(props: ULong) = sdl_kmp_DestroyProperties(props.toInt())
}

// =========================================================================
// Processes (not available on wasm)
// =========================================================================

actual object SDLProcesses {
    actual fun createProcess(args: List<String>): SDLProcess? = null
}

// =========================================================================
// Camera
// =========================================================================

private class WasmCamera(handle: Int) : SDLCamera {
    private var camera: Int = handle

    override var ptr: Long
        get() = if (camera == 0) 0L else camera.toLong()
        set(value) { camera = value.toInt() }

    override val id: Int get() = camera

    override val format: SDLCameraSpec?
        get() {
            sdl_kmp_GetCameraFormat(camera)
            return if (r32(4) != 0) SDLCameraSpec(r32(0), r32(1), r32(2), r32(3)) else null
        }

    override val permissionState: Int get() = sdl_kmp_GetCameraPermissionState(camera)

    override fun supportsFormat(spec: SDLCameraSpec): Boolean =
        sdl_kmp_GetCameraSupportsFormat(camera, spec.format, spec.width, spec.height, spec.framerate) != 0

    override fun acquireFrame(): SDLSurface? {
        val frame = sdl_kmp_AcquireCameraFrame(camera)
        return if (frame == 0) null else WasmSurface(frame, owned = false)
    }

    override fun releaseFrame(frame: SDLSurface) {
        val ws = (frame as? WasmSurface)?.surface ?: return
        sdl_kmp_ReleaseCameraFrame(camera, ws)
    }

    override fun close() {
        if (camera != 0) { sdl_kmp_CloseCamera(camera); camera = 0 }
    }
}

actual object SDLCameras {
    actual val devices: List<Int>
        get() {
            val count = sdl_kmp_RefreshCameras()
            return (0 until count).map { sdl_kmp_GetCameraDevice(it) }
        }

    actual fun getDeviceName(deviceId: Int): String? = sdl_kmp_GetCameraDeviceName(deviceId)

    actual fun getDevicePosition(deviceId: Int): Int = sdl_kmp_GetCameraDevicePosition(deviceId)

    actual fun getSupportedFormats(deviceId: Int): List<SDLCameraSpec> {
        val count = sdl_kmp_RefreshCameraFormats(deviceId)
        return (0 until count).map { i ->
            sdl_kmp_GetCameraFormatSpec(i)
            SDLCameraSpec(r32(0), r32(1), r32(2), r32(3))
        }
    }

    actual fun open(deviceId: Int, spec: SDLCameraSpec?): SDLCamera {
        val camera = if (spec == null) {
            sdl_kmp_OpenCamera(deviceId, 0, 0, 0, 0)
        } else {
            sdl_kmp_OpenCamera(deviceId, spec.format, spec.width, spec.height, spec.framerate)
        }
        check(camera != 0) { "SDL_OpenCamera failed: ${SDL.error()}" }
        return WasmCamera(camera)
    }
}

// =========================================================================
// Sensors
// =========================================================================

private class WasmSensor(handle: Int) : SDLSensor {
    private var sensor: Int = handle

    override var ptr: Long
        get() = if (sensor == 0) 0L else sensor.toLong()
        set(value) { sensor = value.toInt() }

    override val id: Int get() = sensor
    override val name: String? get() = sdl_kmp_GetSensorName(sensor)
    override val type: Int get() = sdl_kmp_GetSensorType(sensor)

    override fun data(): FloatArray {
        val out = Float32Array(3)
        sdl_kmp_GetSensorData(sensor, out)
        return out.toFloatArray()
    }

    override fun close() {
        if (sensor != 0) { sdl_kmp_CloseSensor(sensor); sensor = 0 }
    }
}

actual object SDLSensors {
    actual val devices: List<Int>
        get() {
            val count = sdl_kmp_RefreshSensors()
            return (0 until count).map { sdl_kmp_GetSensorDevice(it) }
        }

    actual fun getDeviceName(deviceId: Int): String? = sdl_kmp_GetSensorDeviceName(deviceId)

    actual fun getDeviceType(deviceId: Int): Int = sdl_kmp_GetSensorDeviceType(deviceId)

    actual fun open(deviceId: Int): SDLSensor {
        val sensor = sdl_kmp_OpenSensor(deviceId)
        check(sensor != 0) { "SDL_OpenSensor failed: ${SDL.error()}" }
        return WasmSensor(sensor)
    }
}

// =========================================================================
// Haptics (not available on wasm)
// =========================================================================

actual object SDLHaptics {
    actual val devices: List<Int> get() = emptyList()

    actual fun getDeviceName(deviceId: Int): String? = null

    actual fun open(deviceId: Int): SDLHaptic =
        error("haptics are not supported on wasm")
}

@OptIn(ExperimentalJsExport::class)
private fun r32(index: Int): Int = sdlKmpResultI32Get(index)
