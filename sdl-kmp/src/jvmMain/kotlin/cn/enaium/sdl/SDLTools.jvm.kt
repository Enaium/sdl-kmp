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

import org.lwjgl.sdl.SDLCamera as LwjglSDLCamera
import org.lwjgl.sdl.SDLDialog
import org.lwjgl.sdl.SDLHaptic as LwjglSDLHaptic
import org.lwjgl.sdl.SDLIOStream as LwjglSDLIOStream
import org.lwjgl.sdl.SDLLog
import org.lwjgl.sdl.SDLProperties
import org.lwjgl.sdl.SDLSensor as LwjglSDLSensor
import org.lwjgl.sdl.SDL_CameraSpec
import org.lwjgl.sdl.SDL_DialogFileFilter
import org.lwjgl.sdl.SDL_HapticEffect
import org.lwjgl.sdl.SDL_HapticLeftRight
import org.lwjgl.sdl.SDL_Surface
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.io.ByteArrayOutputStream
import java.lang.ProcessBuilder
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

// =========================================================================
// Logging
// =========================================================================

actual object SDLLog {

    actual fun log(priority: Int, category: Int, message: String) {
        SDLLog.SDL_LogMessage(category, priority, message)
    }

    actual fun debug(message: String, category: Int) = log(SDLLogPriority.DEBUG, category, message)
    actual fun info(message: String, category: Int) = log(SDLLogPriority.INFO, category, message)
    actual fun warn(message: String, category: Int) = log(SDLLogPriority.WARN, category, message)
    actual fun error(message: String, category: Int) = log(SDLLogPriority.ERROR, category, message)
    actual fun critical(message: String, category: Int) = log(SDLLogPriority.CRITICAL, category, message)

    actual fun setPriority(category: Int, priority: Int) {
        SDLLog.SDL_SetLogPriority(category, priority)
    }

    actual fun getPriority(category: Int): Int = SDLLog.SDL_GetLogPriority(category)

    actual fun setAllPriority(priority: Int) {
        SDLLog.SDL_SetLogPriorities(priority)
    }

    actual fun resetPriorities() {
        SDLLog.SDL_ResetLogPriorities()
    }

    actual fun setOutputFunction(output: SDLLogOutput?) {
        if (output == null) {
            logOutputCallbacks.clear()
            logOutputCallback = null
            SDLLog.SDL_SetLogOutputFunction(null, 0L)
        } else {
            val id = logOutputNextId.getAndIncrement()
            logOutputCallbacks[id] = output
            val cb = SDL_LogOutputFunctionAdapter(id)
            logOutputCallback = cb
            SDLLog.SDL_SetLogOutputFunction(cb, 0L)
        }
    }
}

private class SDL_LogOutputFunctionAdapter(private val id: Long) : org.lwjgl.sdl.SDL_LogOutputFunctionI {
    override fun invoke(userdata: Long, category: Int, priority: Int, message: Long) {
        val cb = logOutputCallbacks[id] ?: return
        cb.log(priority, category, MemoryUtil.memUTF8(message) ?: "")
    }
}

private val logOutputCallbacks = ConcurrentHashMap<Long, SDLLogOutput>()
private val logOutputNextId = AtomicLong(0)
private var logOutputCallback: org.lwjgl.sdl.SDL_LogOutputFunctionI? = null

// =========================================================================
// Threads (implemented with java.lang.Thread / java.util.concurrent; the
// JVM has no SDL thread bindings in LWJGL and the semantics are identical)
// =========================================================================

internal class JvmSDLThread internal constructor(
    override val ptr: Long,
    private val thread: Thread,
) : SDLThread {

    override val id: Int
        get() = thread.id.toInt()

    override val name: String?
        get() = thread.name

    override fun wait(): Int {
        thread.join()
        return 0
    }

    override fun detach() {
        // Java threads are always detached.
    }

    override fun close() = detach()
}

internal class JvmSDLMutex internal constructor(override val ptr: Long) : SDLMutex {

    private val mutex = java.util.concurrent.locks.ReentrantLock()

    override fun lock() {
        mutex.lock()
    }

    override fun tryLock(): Boolean = mutex.tryLock()

    override fun unlock() {
        mutex.unlock()
    }

    override fun close() {
        // ReentrantLock has no destroy.
    }
}

internal class JvmSDLRWLock internal constructor(override val ptr: Long) : SDLRWLock {

    private val lock = java.util.concurrent.locks.ReentrantReadWriteLock()

    override fun lockRead() = lock.readLock().lock()
    override fun tryLockRead(): Boolean = lock.readLock().tryLock()
    override fun unlockRead() = lock.readLock().unlock()
    override fun lockWrite() = lock.writeLock().lock()
    override fun tryLockWrite(): Boolean = lock.writeLock().tryLock()
    override fun unlockWrite() = lock.writeLock().unlock()

    override fun close() {
        // ReentrantReadWriteLock has no destroy.
    }
}

internal class JvmSDLSemaphore internal constructor(override val ptr: Long, initialValue: Int) : SDLSemaphore {

    private val semaphore = java.util.concurrent.Semaphore(initialValue)

    override fun acquire() {
        semaphore.acquire()
    }

    override fun waitTimeout(timeoutMs: Int): Boolean = semaphore.tryAcquire(timeoutMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)

    override fun tryWait(): Boolean = semaphore.tryAcquire()

    override fun post() {
        semaphore.release()
    }

    override val value: Int
        get() = semaphore.availablePermits()

    override fun close() {
        // Semaphore has no destroy.
    }
}

internal class JvmSDLCondition internal constructor(override val ptr: Long) : SDLCondition {

    private val lock = java.util.concurrent.locks.ReentrantLock()
    private val condition = lock.newCondition()

    override fun wait(mutex: SDLMutex) {
        lock.lock()
        try {
            condition.await()
        } finally {
            lock.unlock()
        }
    }

    override fun waitTimeout(mutex: SDLMutex, timeoutMs: Int): Boolean {
        lock.lock()
        return try {
            condition.await(timeoutMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
        } finally {
            lock.unlock()
        }
    }

    override fun signal() {
        condition.signal()
    }

    override fun broadcast() {
        condition.signalAll()
    }

    override fun close() {
        // Condition has no destroy.
    }
}

actual object SDLThreads {

    actual val numLogicalCPUCores: Int
        get() = Runtime.getRuntime().availableProcessors()

    actual fun createThread(name: String?, fn: (Int) -> Unit): SDLThread? {
        val thread = Thread { fn(0) }
        if (name != null) thread.name = name
        thread.isDaemon = false
        thread.start()
        return JvmSDLThread(0L, thread)
    }

    actual val currentThreadId: Int
        get() = Thread.currentThread().id.toInt()

    actual fun getThreadId(thread: SDLThread): Int =
        (thread as? JvmSDLThread)?.id ?: throw IllegalArgumentException("thread is not a JVM SDL thread")

    actual fun getThreadName(thread: SDLThread): String? =
        (thread as? JvmSDLThread)?.name ?: throw IllegalArgumentException("thread is not a JVM SDL thread")

    actual fun setThreadPriority(priority: Int): Boolean = try {
        Thread.currentThread().priority = when (priority) {
            SDLThreadPriority.LOW -> Thread.MIN_PRIORITY
            SDLThreadPriority.HIGH -> Thread.MAX_PRIORITY
            SDLThreadPriority.TIME_CRITICAL -> Thread.MAX_PRIORITY
            else -> Thread.NORM_PRIORITY
        }
        true
    } catch (e: Throwable) {
        false
    }

    actual fun createMutex(): SDLMutex? = JvmSDLMutex(0L)

    actual fun createRWLock(): SDLRWLock? = JvmSDLRWLock(0L)

    actual fun createSemaphore(initialValue: Int): SDLSemaphore? = JvmSDLSemaphore(0L, initialValue)

    actual fun createCondition(): SDLCondition? = JvmSDLCondition(0L)
}

// =========================================================================
// IO
// =========================================================================

internal class JvmSDLIOStream internal constructor(ptr: Long, private val owned: Boolean) : SDLIOStream {

    private var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue

    internal fun check(): Long = ptrValue.also {
        if (it == 0L) throw IllegalStateException("SDL IO stream is closed")
    }

    override fun read(size: Int): ByteArray {
        val buffer = MemoryUtil.memAlloc(size)
        try {
            val read = LwjglSDLIOStream.SDL_ReadIO(check(), buffer).toInt()
            if (read <= 0) return ByteArray(0)
            buffer.limit(read)
            val out = ByteArray(read)
            buffer.rewind()
            buffer.get(out)
            return out
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    override fun write(data: ByteArray): Int {
        val buffer = MemoryUtil.memAlloc(data.size)
        try {
            buffer.put(data).rewind()
            return LwjglSDLIOStream.SDL_WriteIO(check(), buffer).toInt()
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    override fun seek(offset: Int, whence: Int): Int =
        LwjglSDLIOStream.SDL_SeekIO(check(), offset.toLong(), whence).toInt()

    override fun tell(): Int = LwjglSDLIOStream.SDL_TellIO(check()).toInt()

    override fun size(): Int = LwjglSDLIOStream.SDL_GetIOSize(check()).toInt()

    override fun flush(): Boolean = LwjglSDLIOStream.SDL_FlushIO(check())

    override fun close() {
        val stream = ptrValue
        if (stream == 0L) return
        ptrValue = 0L
        if (owned) {
            LwjglSDLIOStream.SDL_CloseIO(stream)
        }
    }
}

actual object SDLIO {

    actual fun openFile(path: String, mode: String): SDLIOStream? =
        LwjglSDLIOStream.SDL_IOFromFile(path, mode)?.let { JvmSDLIOStream(it, owned = true) }

    actual fun fromMem(data: ByteArray): SDLIOStream? {
        val buffer = MemoryUtil.memAlloc(data.size)
        buffer.put(data).rewind()
        return LwjglSDLIOStream.SDL_IOFromMem(buffer)?.let { JvmSDLIOStream(it, owned = true) }
    }

    actual fun fromConstMem(data: ByteArray): SDLIOStream? {
        val buffer = MemoryUtil.memAlloc(data.size)
        buffer.put(data).rewind()
        return LwjglSDLIOStream.SDL_IOFromConstMem(buffer)?.let { JvmSDLIOStream(it, owned = true) }
    }

    actual fun loadFile(path: String): ByteArray? {
        val buffer = LwjglSDLIOStream.SDL_LoadFile(path) ?: return null
        try {
            val out = ByteArray(buffer.limit())
            buffer.rewind()
            buffer.get(out)
            return out
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    actual fun loadFile(stream: SDLIOStream): ByteArray? {
        val native = stream as? JvmSDLIOStream
            ?: throw IllegalArgumentException("stream is not a JVM SDL IO stream")
        val buffer = LwjglSDLIOStream.SDL_LoadFile_IO(native.check(), true) ?: return null
        native.close()
        try {
            val out = ByteArray(buffer.limit())
            buffer.rewind()
            buffer.get(out)
            return out
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }
}

// =========================================================================
// Properties
// =========================================================================

actual object SDLProperties {

    actual fun create(): ULong = SDLProperties.SDL_CreateProperties().toUInt().toULong()

    actual fun setProperty(props: ULong, name: String, value: Long): Boolean =
        SDLProperties.SDL_SetPointerProperty(props.toInt(), name, value)

    actual fun setStringProperty(props: ULong, name: String, value: String?): Boolean =
        SDLProperties.SDL_SetStringProperty(props.toInt(), name, value)

    actual fun getProperty(props: ULong, name: String, defaultValue: Long): Long =
        SDLProperties.SDL_GetPointerProperty(props.toInt(), name, defaultValue)

    actual fun getStringProperty(props: ULong, name: String): String? =
        SDLProperties.SDL_GetStringProperty(props.toInt(), name, null)

    actual fun hasProperty(props: ULong, name: String): Boolean =
        SDLProperties.SDL_HasProperty(props.toInt(), name)

    actual fun deleteProperty(props: ULong, name: String): Boolean =
        SDLProperties.SDL_ClearProperty(props.toInt(), name)

    actual fun copy(src: ULong, dst: ULong): Boolean =
        SDLProperties.SDL_CopyProperties(src.toInt(), dst.toInt())

    actual val globalProperties: ULong
        get() = SDLProperties.SDL_GetGlobalProperties().toUInt().toULong()

    actual fun destroy(props: ULong) {
        SDLProperties.SDL_DestroyProperties(props.toInt())
    }
}

// =========================================================================
// Process (java.lang.ProcessBuilder; ptr is 0)
// =========================================================================

internal class JvmSDLProcess internal constructor(private val process: java.lang.Process) : SDLProcess {

    override val ptr: Long
        get() = 0L

    override val active: Boolean
        get() = process.isAlive

    override fun wait(): Int = process.waitFor()

    override fun kill(force: Boolean): Boolean = try {
        if (force) process.destroyForcibly() else process.destroy()
        true
    } catch (e: Throwable) {
        false
    }

    override fun readOutput(): ByteArray? {
        val stream = process.inputStream
        val available = stream.available()
        if (available <= 0 && process.isAlive) return null
        val out = ByteArrayOutputStream()
        stream.copyTo(out)
        return out.toByteArray()
    }

    override fun close() {
        if (process.isAlive) process.destroy()
    }
}

actual object SDLProcesses {

    actual fun createProcess(args: List<String>): SDLProcess? = try {
        val process = ProcessBuilder(args).redirectErrorStream(false).start()
        JvmSDLProcess(process)
    } catch (e: Throwable) {
        null
    }
}

// =========================================================================
// Camera
// =========================================================================

internal class JvmSDLCamera internal constructor(ptr: Long, private val cameraId: Int) : SDLCamera {

    private var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue

    private fun check(): Long = ptrValue.also {
        if (it == 0L) throw IllegalStateException("SDL camera is closed")
    }

    override val id: Int
        get() = cameraId

    override val format: SDLCameraSpec?
        get() = SDL_CameraSpec.calloc().use { spec ->
            if (LwjglSDLCamera.SDL_GetCameraFormat(check(), spec)) {
                SDLCameraSpec(spec.format(), spec.width(), spec.height(), if (spec.framerate_denominator() > 0) spec.framerate_numerator() / spec.framerate_denominator() else 0)
            } else {
                null
            }
        }

    override val permissionState: Int
        get() = LwjglSDLCamera.SDL_GetCameraPermissionState(check())

    override fun supportsFormat(spec: SDLCameraSpec): Boolean {
        val formats = LwjglSDLCamera.SDL_GetCameraSupportedFormats(cameraId) ?: return false
        for (i in 0 until formats.limit()) {
            val ptr = formats.get(i) ?: continue
            val s = SDL_CameraSpec.create(ptr)
            if (s.format() == spec.format && s.width() == spec.width && s.height() == spec.height) return true
        }
        return false
    }

    override fun acquireFrame(): SDLSurface? {
        val surface = LwjglSDLCamera.SDL_AcquireCameraFrame(check(), null) ?: return null
        return JvmSDLSurface(surface, owned = false)
    }

    override fun releaseFrame(frame: SDLSurface) {
        val native = frame as? JvmSDLSurface
            ?: throw IllegalArgumentException("frame is not a JVM SDL surface")
        val frameSurface = native.surface ?: throw IllegalStateException("frame is closed")
        LwjglSDLCamera.SDL_ReleaseCameraFrame(check(), frameSurface)
    }

    override fun close() {
        val camera = ptrValue
        if (camera == 0L) return
        ptrValue = 0L
        LwjglSDLCamera.SDL_CloseCamera(camera)
    }
}

actual object SDLCameras {

    actual val devices: List<Int>
        get() {
            val devices = LwjglSDLCamera.SDL_GetCameras() ?: return emptyList()
            return (0 until devices.limit()).map { devices.get(it) }
        }

    actual fun getDeviceName(deviceId: Int): String? =
        LwjglSDLCamera.SDL_GetCameraName(deviceId)

    actual fun getDevicePosition(deviceId: Int): Int =
        LwjglSDLCamera.SDL_GetCameraPosition(deviceId)

    actual fun getSupportedFormats(deviceId: Int): List<SDLCameraSpec> {
        val formats = LwjglSDLCamera.SDL_GetCameraSupportedFormats(deviceId) ?: return emptyList()
        val result = mutableListOf<SDLCameraSpec>()
        for (i in 0 until formats.limit()) {
            val ptr = formats.get(i) ?: continue
            val spec = SDL_CameraSpec.create(ptr)
            result.add(SDLCameraSpec(spec.format(), spec.width(), spec.height(), if (spec.framerate_denominator() > 0) spec.framerate_numerator() / spec.framerate_denominator() else 0))
        }
        return result
    }

    actual fun open(deviceId: Int, spec: SDLCameraSpec?): SDLCamera {
        val ptr = if (spec != null) {
            SDL_CameraSpec.calloc().use { s ->
                s.format(spec.format).width(spec.width).height(spec.height).framerate_numerator(spec.framerate).framerate_denominator(1)
                LwjglSDLCamera.SDL_OpenCamera(deviceId, s)
            }
        } else {
            LwjglSDLCamera.SDL_OpenCamera(deviceId, null)
        } ?: throw IllegalStateException("SDL_OpenCamera failed: ${SDL.error()}")
        return JvmSDLCamera(ptr, deviceId)
    }
}

// =========================================================================
// Sensor
// =========================================================================

internal class JvmSDLSensor internal constructor(ptr: Long) : SDLSensor {

    private var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue

    private fun check(): Long = ptrValue.also {
        if (it == 0L) throw IllegalStateException("SDL sensor is closed")
    }

    override val id: Int
        get() = LwjglSDLSensor.SDL_GetSensorID(check())

    override val name: String?
        get() = LwjglSDLSensor.SDL_GetSensorName(check())

    override val type: Int
        get() = LwjglSDLSensor.SDL_GetSensorType(check())

    override fun data(): FloatArray {
        val buffer = MemoryUtil.memAllocFloat(3)
        return try {
            if (LwjglSDLSensor.SDL_GetSensorData(check(), buffer)) {
                FloatArray(3) { buffer.get(it) }
            } else {
                FloatArray(0)
            }
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    override fun close() {
        val sensor = ptrValue
        if (sensor == 0L) return
        ptrValue = 0L
        LwjglSDLSensor.SDL_CloseSensor(sensor)
    }
}

actual object SDLSensors {

    actual val devices: List<Int>
        get() {
            val sensors = LwjglSDLSensor.SDL_GetSensors() ?: return emptyList()
            return (0 until sensors.limit()).map { sensors.get(it) }
        }

    actual fun getDeviceName(deviceId: Int): String? =
        LwjglSDLSensor.SDL_GetSensorNameForID(deviceId)

    actual fun getDeviceType(deviceId: Int): Int =
        LwjglSDLSensor.SDL_GetSensorTypeForID(deviceId)

    actual fun open(deviceId: Int): SDLSensor {
        val ptr = LwjglSDLSensor.SDL_OpenSensor(deviceId)
            ?: throw IllegalStateException("SDL_OpenSensor failed: ${SDL.error()}")
        return JvmSDLSensor(ptr)
    }
}

// =========================================================================
// Haptic
// =========================================================================

internal class JvmSDLHaptic internal constructor(ptr: Long, private val hapticId: Int) : SDLHaptic {

    private var ptrValue: Long = ptr

    override val ptr: Long
        get() = ptrValue

    private fun check(): Long = ptrValue.also {
        if (it == 0L) throw IllegalStateException("SDL haptic is closed")
    }

    override val id: Int
        get() = hapticId

    override val name: String?
        get() = LwjglSDLHaptic.SDL_GetHapticName(check())

    override val numAxes: Int
        get() = LwjglSDLHaptic.SDL_GetNumHapticAxes(check())

    override fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean =
        SDL_HapticEffect.calloc().use { effect ->
            effect.type(LwjglSDLHaptic.SDL_HAPTIC_LEFTRIGHT.toShort())
            effect.leftright().length(durationMs).large_magnitude(lowFrequency.toShort()).small_magnitude(highFrequency.toShort())
            val id = LwjglSDLHaptic.SDL_CreateHapticEffect(check(), effect)
            if (id < 0) return false
            val ok = LwjglSDLHaptic.SDL_RunHapticEffect(check(), id, 1)
            LwjglSDLHaptic.SDL_DestroyHapticEffect(check(), id)
            ok
        }

    override fun stopEffect(effectId: Int): Boolean =
        LwjglSDLHaptic.SDL_StopHapticEffect(check(), effectId)

    override fun effectStatus(effectId: Int): Boolean =
        LwjglSDLHaptic.SDL_GetHapticEffectStatus(check(), effectId)

    override fun destroyEffect(effectId: Int): Boolean = try {
        LwjglSDLHaptic.SDL_DestroyHapticEffect(check(), effectId)
        true
    } catch (e: Throwable) {
        false
    }

    override fun close() {
        val haptic = ptrValue
        if (haptic == 0L) return
        ptrValue = 0L
        LwjglSDLHaptic.SDL_CloseHaptic(haptic)
    }
}

actual object SDLHaptics {

    actual val devices: List<Int>
        get() {
            val haptics = LwjglSDLHaptic.SDL_GetHaptics() ?: return emptyList()
            return (0 until haptics.limit()).map { haptics.get(it) }
        }

    actual fun getDeviceName(deviceId: Int): String? =
        LwjglSDLHaptic.SDL_GetHapticNameForID(deviceId)

    actual fun open(deviceId: Int): SDLHaptic {
        val ptr = LwjglSDLHaptic.SDL_OpenHaptic(deviceId)
            ?: throw IllegalStateException("SDL_OpenHaptic failed: ${SDL.error()}")
        return JvmSDLHaptic(ptr, deviceId)
    }
}
