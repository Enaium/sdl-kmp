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

import java.io.ByteArrayOutputStream

// =========================================================================
// Logging
// =========================================================================

actual object SDLLog {

    actual fun log(priority: Int, category: Int, message: String) {
        Jni.logMessage(category, priority, message)
    }

    actual fun debug(message: String, category: Int) = log(SDLLogPriority.DEBUG, category, message)
    actual fun info(message: String, category: Int) = log(SDLLogPriority.INFO, category, message)
    actual fun warn(message: String, category: Int) = log(SDLLogPriority.WARN, category, message)
    actual fun error(message: String, category: Int) = log(SDLLogPriority.ERROR, category, message)
    actual fun critical(message: String, category: Int) = log(SDLLogPriority.CRITICAL, category, message)

    actual fun setPriority(category: Int, priority: Int) {
        Jni.setLogPriority(category, priority)
    }

    actual fun getPriority(category: Int): Int = Jni.getLogPriority(category)

    actual fun setAllPriority(priority: Int) {
        Jni.setLogPriorities(priority)
    }

    actual fun resetPriorities() {
        Jni.resetLogPriorities()
    }

    actual fun setOutputFunction(output: SDLLogOutput?) {
        if (output == null) {
            logOutputCallbacks.clear()
            Jni.setLogOutputFunctionNull()
        } else {
            val id = logOutputNextId.getAndIncrement()
            logOutputCallbacks[id] = output
            Jni.setLogOutputFunction(id)
        }
    }
}

// =========================================================================
// Threads (implemented with java.lang.Thread / java.util.concurrent; the
// JVM has no SDL thread bindings and the semantics are identical)
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

    override fun read(size: Int): ByteArray = Jni.ioRead(check(), size) ?: ByteArray(0)

    override fun write(data: ByteArray): Int = Jni.ioWrite(check(), data)

    override fun seek(offset: Int, whence: Int): Int = Jni.ioSeek(check(), offset, whence)

    override fun tell(): Int = Jni.ioTell(check())

    override fun size(): Int = Jni.ioSize(check())

    override fun flush(): Boolean = Jni.ioFlush(check())

    override fun close() {
        val stream = ptrValue
        if (stream == 0L) return
        ptrValue = 0L
        if (owned) {
            Jni.ioClose(stream)
        }
    }
}

actual object SDLIO {

    actual fun openFile(path: String, mode: String): SDLIOStream? {
        val stream = Jni.ioFromFile(path, mode)
        if (stream == 0L) return null
        return JvmSDLIOStream(stream, owned = true)
    }

    actual fun fromMem(data: ByteArray): SDLIOStream? {
        val stream = Jni.ioFromMem(data)
        if (stream == 0L) return null
        return JvmSDLIOStream(stream, owned = true)
    }

    actual fun fromConstMem(data: ByteArray): SDLIOStream? {
        val stream = Jni.ioFromConstMem(data)
        if (stream == 0L) return null
        return JvmSDLIOStream(stream, owned = true)
    }

    actual fun loadFile(path: String): ByteArray? = Jni.loadFile(path)

    actual fun loadFile(stream: SDLIOStream): ByteArray? {
        val native = stream as? JvmSDLIOStream
            ?: throw IllegalArgumentException("stream is not a JVM SDL IO stream")
        return Jni.loadFileIO(native.check())
    }
}

// =========================================================================
// Properties
// =========================================================================

actual object SDLProperties {

    actual fun create(): ULong = Jni.propertiesCreate().toUInt().toULong()

    actual fun setProperty(props: ULong, name: String, value: Long): Boolean =
        Jni.setPointerProperty(props.toInt(), name, value)

    actual fun setStringProperty(props: ULong, name: String, value: String?): Boolean =
        Jni.setStringProperty(props.toInt(), name, value)

    actual fun getProperty(props: ULong, name: String, defaultValue: Long): Long =
        Jni.getPointerProperty(props.toInt(), name, defaultValue)

    actual fun getStringProperty(props: ULong, name: String): String? =
        Jni.getStringProperty(props.toInt(), name)

    actual fun hasProperty(props: ULong, name: String): Boolean =
        Jni.hasProperty(props.toInt(), name)

    actual fun deleteProperty(props: ULong, name: String): Boolean =
        Jni.clearProperty(props.toInt(), name)

    actual fun copy(src: ULong, dst: ULong): Boolean =
        Jni.copyProperties(src.toInt(), dst.toInt())

    actual val globalProperties: ULong
        get() = Jni.globalProperties().toUInt().toULong()

    actual fun destroy(props: ULong) {
        Jni.destroyProperties(props.toInt())
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
        get() = Jni.getCameraFormat(check())?.let {
            SDLCameraSpec(
                format = it[0],
                width = it[2],
                height = it[3],
                framerate = if (it[5] > 0) it[4] / it[5] else 0,
            )
        }

    override val permissionState: Int
        get() = Jni.getCameraPermissionState(check())

    override fun supportsFormat(spec: SDLCameraSpec): Boolean {
        val formats = Jni.getCameraSupportedFormats(cameraId) ?: return false
        var i = 0
        while (i < formats.size) {
            if (formats[i] == spec.format && formats[i + 2] == spec.width && formats[i + 3] == spec.height) return true
            i += 6
        }
        return false
    }

    override fun acquireFrame(): SDLSurface? {
        val surface = Jni.acquireCameraFrame(check())
        if (surface == 0L) return null
        return JvmSDLSurface(surface, owned = false)
    }

    override fun releaseFrame(frame: SDLSurface) {
        val native = frame as? JvmSDLSurface
            ?: throw IllegalArgumentException("frame is not a JVM SDL surface")
        Jni.releaseCameraFrame(check(), native.ptr)
    }

    override fun close() {
        val camera = ptrValue
        if (camera == 0L) return
        ptrValue = 0L
        Jni.closeCamera(camera)
    }
}

actual object SDLCameras {

    actual val devices: List<Int>
        get() = Jni.getCameras()?.toList() ?: emptyList()

    actual fun getDeviceName(deviceId: Int): String? =
        Jni.getCameraName(deviceId)

    actual fun getDevicePosition(deviceId: Int): Int =
        Jni.getCameraPosition(deviceId)

    actual fun getSupportedFormats(deviceId: Int): List<SDLCameraSpec> {
        val formats = Jni.getCameraSupportedFormats(deviceId) ?: return emptyList()
        val result = mutableListOf<SDLCameraSpec>()
        var i = 0
        while (i < formats.size) {
            result.add(
                SDLCameraSpec(
                    format = formats[i],
                    width = formats[i + 2],
                    height = formats[i + 3],
                    framerate = if (formats[i + 5] > 0) formats[i + 4] / formats[i + 5] else 0,
                ),
            )
            i += 6
        }
        return result
    }

    actual fun open(deviceId: Int, spec: SDLCameraSpec?): SDLCamera {
        val ptr = Jni.openCamera(
            deviceId,
            spec?.format ?: 0,
            spec?.width ?: 0,
            spec?.height ?: 0,
            spec?.framerate ?: 0,
        )
        check(ptr != 0L) { "SDL_OpenCamera failed: ${SDL.error()}" }
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
        get() = Jni.getSensorID(check())

    override val name: String?
        get() = Jni.getSensorName(check())

    override val type: Int
        get() = Jni.getSensorType(check())

    override fun data(): FloatArray = Jni.getSensorData(check()) ?: FloatArray(0)

    override fun close() {
        val sensor = ptrValue
        if (sensor == 0L) return
        ptrValue = 0L
        Jni.closeSensor(sensor)
    }
}

actual object SDLSensors {

    actual val devices: List<Int>
        get() = Jni.getSensors()?.toList() ?: emptyList()

    actual fun getDeviceName(deviceId: Int): String? =
        Jni.getSensorNameForID(deviceId)

    actual fun getDeviceType(deviceId: Int): Int =
        Jni.getSensorTypeForID(deviceId)

    actual fun open(deviceId: Int): SDLSensor {
        val ptr = Jni.openSensor(deviceId)
        check(ptr != 0L) { "SDL_OpenSensor failed: ${SDL.error()}" }
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
        get() = Jni.getHapticName(check())

    override val numAxes: Int
        get() = Jni.getNumHapticAxes(check())

    override fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean =
        Jni.hapticRumble(check(), lowFrequency, highFrequency, durationMs)

    override fun stopEffect(effectId: Int): Boolean =
        Jni.stopHapticEffect(check(), effectId)

    override fun effectStatus(effectId: Int): Boolean =
        Jni.hapticEffectStatus(check(), effectId)

    override fun destroyEffect(effectId: Int): Boolean = try {
        Jni.destroyHapticEffect(check(), effectId)
        true
    } catch (e: Throwable) {
        false
    }

    override fun close() {
        val haptic = ptrValue
        if (haptic == 0L) return
        ptrValue = 0L
        Jni.closeHaptic(haptic)
    }
}

actual object SDLHaptics {

    actual val devices: List<Int>
        get() = Jni.getHaptics()?.toList() ?: emptyList()

    actual fun getDeviceName(deviceId: Int): String? =
        Jni.getHapticNameForID(deviceId)

    actual fun open(deviceId: Int): SDLHaptic {
        val ptr = Jni.openHaptic(deviceId)
        check(ptr != 0L) { "SDL_OpenHaptic failed: ${SDL.error()}" }
        return JvmSDLHaptic(ptr, deviceId)
    }
}
