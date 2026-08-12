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

import cnames.structs.SDL_Camera
import cnames.structs.SDL_Condition
import cnames.structs.SDL_Haptic
import cnames.structs.SDL_IOStream
import cnames.structs.SDL_Mutex
import cnames.structs.SDL_Process
import cnames.structs.SDL_RWLock
import cnames.structs.SDL_Semaphore
import cnames.structs.SDL_Sensor
import cnames.structs.SDL_Thread
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicLong
import kotlin.concurrent.AtomicReference
import kotlinx.cinterop.*
import sdl3.*

// =========================================================================
// Logging
// =========================================================================

actual object SDLLog {

    actual fun log(priority: Int, category: Int, message: String) {
        SDL_kmp_LogMessage(category, logPriorityOf(priority).value.toUInt(), message)
    }

    actual fun debug(message: String, category: Int) = log(SDLLogPriority.DEBUG, category, message)
    actual fun info(message: String, category: Int) = log(SDLLogPriority.INFO, category, message)
    actual fun warn(message: String, category: Int) = log(SDLLogPriority.WARN, category, message)
    actual fun error(message: String, category: Int) = log(SDLLogPriority.ERROR, category, message)
    actual fun critical(message: String, category: Int) = log(SDLLogPriority.CRITICAL, category, message)

    actual fun setPriority(category: Int, priority: Int) {
        SDL_SetLogPriority(category, logPriorityOf(priority))
    }

    actual fun getPriority(category: Int): Int = SDL_GetLogPriority(category).value.toInt()

    actual fun setAllPriority(priority: Int) {
        SDL_SetLogPriorities(logPriorityOf(priority))
    }

    actual fun resetPriorities() {
        SDL_ResetLogPriorities()
    }

    actual fun setOutputFunction(output: SDLLogOutput?) {
        if (output == null) {
            logOutputCallbacks.clear()
            SDL_SetLogOutputFunction(null, null)
        } else {
            val id = logOutputNextId.getAndIncrement()
            logOutputCallbacks[id] = output
            val holder = nativeHeap.alloc<LongVar>().also { it.value = id }
            logOutputHolders[id] = holder.ptr
            SDL_SetLogOutputFunction(staticCFunction(::nativeLogOutput), holder.ptr)
        }
    }
}

private val logOutputCallbacks = kotlin.collections.mutableMapOf<Long, SDLLogOutput>()
private val logOutputHolders = kotlin.collections.mutableMapOf<Long, CPointer<LongVar>>()
private val logOutputNextId = AtomicLong(0)

private fun nativeLogOutput(userdata: COpaquePointer?, category: Int, priority: SDL_LogPriority, message: CPointer<ByteVar>?) {
    val id = userdata?.reinterpret<LongVar>()?.pointed?.value ?: return
    val cb = logOutputCallbacks[id] ?: return
    cb.log(priority.value.toInt(), category, message?.toKString() ?: "")
}

private fun logPriorityOf(value: Int): SDL_LogPriority = when (value) {
    SDLLogPriority.TRACE -> SDL_LogPriority.SDL_LOG_PRIORITY_TRACE
    SDLLogPriority.VERBOSE -> SDL_LogPriority.SDL_LOG_PRIORITY_VERBOSE
    SDLLogPriority.DEBUG -> SDL_LogPriority.SDL_LOG_PRIORITY_DEBUG
    SDLLogPriority.INFO -> SDL_LogPriority.SDL_LOG_PRIORITY_INFO
    SDLLogPriority.WARN -> SDL_LogPriority.SDL_LOG_PRIORITY_WARN
    SDLLogPriority.ERROR -> SDL_LogPriority.SDL_LOG_PRIORITY_ERROR
    SDLLogPriority.CRITICAL -> SDL_LogPriority.SDL_LOG_PRIORITY_CRITICAL
    else -> SDL_LogPriority.SDL_LOG_PRIORITY_INVALID
}

// =========================================================================
// Threads
// =========================================================================

private val threadCallbacks = kotlin.collections.mutableMapOf<Long, (Int) -> Unit>()
private val threadHolders = kotlin.collections.mutableMapOf<Long, CPointer<LongVar>>()
private val threadNextId = AtomicLong(0)

private fun nativeThreadFn(userdata: COpaquePointer?): Int {
    val id = userdata?.reinterpret<LongVar>()?.pointed?.value ?: return 1
    val fn = threadCallbacks[id]
    if (fn == null) {
        threadCallbacks.remove(id)
        return 1
    }
    val result = try {
        fn(id.toInt())
        0
    } catch (t: Throwable) {
        1
    }
    threadCallbacks.remove(id)
    return result
}

internal class NativeSDLThread internal constructor(raw: CPointer<SDL_Thread>?) : SDLThread {

    internal var raw: CPointer<SDL_Thread>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    private fun check(): CPointer<SDL_Thread> =
        raw ?: throw IllegalStateException("SDL thread is closed")

    override val id: Int
        get() = SDL_GetThreadID(check()).toInt()

    override val name: String?
        get() = SDL_GetThreadName(check())?.toKString()

    override fun wait(): Int {
        val thread = check()
        return memScoped {
            val status = alloc<IntVar>()
            SDL_WaitThread(thread, status.ptr)
            raw = null
            status.value
        }
    }

    override fun detach() {
        val thread = raw ?: return
        raw = null
        SDL_DetachThread(thread)
    }

    override fun close() = detach()
}

internal class NativeSDLMutex internal constructor(raw: CPointer<SDL_Mutex>?) : SDLMutex {

    internal var raw: CPointer<SDL_Mutex>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    internal fun check(): CPointer<SDL_Mutex> =
        raw ?: throw IllegalStateException("SDL mutex is closed")

    override fun lock() {
        SDL_LockMutex(check())
    }

    override fun tryLock(): Boolean = SDL_TryLockMutex(check())

    override fun unlock() {
        SDL_UnlockMutex(check())
    }

    override fun close() {
        val mutex = raw ?: return
        raw = null
        SDL_DestroyMutex(mutex)
    }
}

internal class NativeSDLRWLock internal constructor(raw: CPointer<SDL_RWLock>?) : SDLRWLock {

    internal var raw: CPointer<SDL_RWLock>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    private fun check(): CPointer<SDL_RWLock> =
        raw ?: throw IllegalStateException("SDL RW lock is closed")

    override fun lockRead() {
        SDL_LockRWLockForReading(check())
    }

    override fun tryLockRead(): Boolean = SDL_TryLockRWLockForReading(check())

    override fun unlockRead() {
        SDL_UnlockRWLock(check())
    }

    override fun lockWrite() {
        SDL_LockRWLockForWriting(check())
    }

    override fun tryLockWrite(): Boolean = SDL_TryLockRWLockForWriting(check())

    override fun unlockWrite() {
        SDL_UnlockRWLock(check())
    }

    override fun close() {
        val lock = raw ?: return
        raw = null
        SDL_DestroyRWLock(lock)
    }
}

internal class NativeSDLSemaphore internal constructor(raw: CPointer<SDL_Semaphore>?) : SDLSemaphore {

    internal var raw: CPointer<SDL_Semaphore>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    private fun check(): CPointer<SDL_Semaphore> =
        raw ?: throw IllegalStateException("SDL semaphore is closed")

    override fun acquire() {
        SDL_WaitSemaphore(check())
    }

    override fun waitTimeout(timeoutMs: Int): Boolean = SDL_WaitSemaphoreTimeout(check(), timeoutMs)

    override fun tryWait(): Boolean = SDL_TryWaitSemaphore(check())

    override fun post() {
        SDL_SignalSemaphore(check())
    }

    override val value: Int
        get() = SDL_GetSemaphoreValue(check()).toInt()

    override fun close() {
        val semaphore = raw ?: return
        raw = null
        SDL_DestroySemaphore(semaphore)
    }
}

internal class NativeSDLCondition internal constructor(raw: CPointer<SDL_Condition>?) : SDLCondition {

    internal var raw: CPointer<SDL_Condition>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    private fun check(): CPointer<SDL_Condition> =
        raw ?: throw IllegalStateException("SDL condition is closed")

    override fun wait(mutex: SDLMutex) {
        val m = (mutex as? NativeSDLMutex)?.check()
            ?: throw IllegalArgumentException("mutex is not a native SDL mutex")
        SDL_WaitCondition(check(), m)
    }

    override fun waitTimeout(mutex: SDLMutex, timeoutMs: Int): Boolean {
        val m = (mutex as? NativeSDLMutex)?.check()
            ?: throw IllegalArgumentException("mutex is not a native SDL mutex")
        return SDL_WaitConditionTimeout(check(), m, timeoutMs)
    }

    override fun signal() {
        SDL_SignalCondition(check())
    }

    override fun broadcast() {
        SDL_BroadcastCondition(check())
    }

    override fun close() {
        val condition = raw ?: return
        raw = null
        SDL_DestroyCondition(condition)
    }
}

actual object SDLThreads {

    actual val numLogicalCPUCores: Int
        get() = SDL_GetNumLogicalCPUCores()

    actual fun createThread(name: String?, fn: (Int) -> Unit): SDLThread? {
        val id = threadNextId.getAndIncrement()
        threadCallbacks[id] = fn
        val holder = nativeHeap.alloc<LongVar>().also { it.value = id }
        threadHolders[id] = holder.ptr
        val ptr = sdl3.SDL_CreateThreadRuntime(staticCFunction(::nativeThreadFn), name, holder.ptr, null, null)
        if (ptr == null) {
            threadCallbacks.remove(id)
            threadHolders.remove(id)
            nativeHeap.free(holder)
        }
        return ptr?.let { NativeSDLThread(it) }
    }

    actual val currentThreadId: Int
        get() = SDL_GetCurrentThreadID().toInt()

    actual fun getThreadId(thread: SDLThread): Int =
        (thread as? NativeSDLThread)?.id ?: throw IllegalArgumentException("thread is not a native SDL thread")

    actual fun getThreadName(thread: SDLThread): String? =
        (thread as? NativeSDLThread)?.name ?: throw IllegalArgumentException("thread is not a native SDL thread")

    actual fun setThreadPriority(priority: Int): Boolean =
        SDL_SetCurrentThreadPriority(threadPriorityOf(priority))

    actual fun createMutex(): SDLMutex? = SDL_CreateMutex()?.let { NativeSDLMutex(it) }

    actual fun createRWLock(): SDLRWLock? = SDL_CreateRWLock()?.let { NativeSDLRWLock(it) }

    actual fun createSemaphore(initialValue: Int): SDLSemaphore? =
        SDL_CreateSemaphore(initialValue.toUInt())?.let { NativeSDLSemaphore(it) }

    actual fun createCondition(): SDLCondition? = SDL_CreateCondition()?.let { NativeSDLCondition(it) }
}

private fun threadPriorityOf(value: Int): SDL_ThreadPriority = when (value) {
    SDLThreadPriority.LOW -> SDL_ThreadPriority.SDL_THREAD_PRIORITY_LOW
    SDLThreadPriority.HIGH -> SDL_ThreadPriority.SDL_THREAD_PRIORITY_HIGH
    SDLThreadPriority.TIME_CRITICAL -> SDL_ThreadPriority.SDL_THREAD_PRIORITY_TIME_CRITICAL
    else -> SDL_ThreadPriority.SDL_THREAD_PRIORITY_NORMAL
}

// =========================================================================
// IO
// =========================================================================

internal class NativeSDLIOStream internal constructor(raw: CPointer<SDL_IOStream>?, private val owned: Boolean) : SDLIOStream {

    internal var raw: CPointer<SDL_IOStream>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    internal fun check(): CPointer<SDL_IOStream> =
        raw ?: throw IllegalStateException("SDL IO stream is closed")

    override fun read(size: Int): ByteArray = memScoped {
        val buf = allocArray<ByteVar>(size)
        val read = SDL_kmp_ReadIO(check(), buf, size).toInt()
        if (read <= 0) ByteArray(0) else buf.readBytes(read)
    }

    override fun write(data: ByteArray): Int = memScoped {
        val ok = data.usePinned { pinned ->
            SDL_kmp_WriteIO(check(), pinned.addressOf(0), data.size)
        }
        ok.toInt()
    }

    override fun seek(offset: Int, whence: Int): Int =
        SDL_SeekIO(check(), offset.toLong(), ioWhenceOf(whence)).toInt()

    override fun tell(): Int = SDL_TellIO(check()).toInt()

    override fun size(): Int = SDL_GetIOSize(check()).toInt()

    override fun flush(): Boolean = SDL_FlushIO(check())

    override fun close() {
        val stream = raw ?: return
        raw = null
        if (owned) {
            SDL_CloseIO(stream)
        }
    }
}



private fun ioWhenceOf(value: Int): SDL_IOWhence = when (value) {
    SDLIOWhence.CUR -> SDL_IOWhence.SDL_IO_SEEK_CUR
    SDLIOWhence.END -> SDL_IOWhence.SDL_IO_SEEK_END
    else -> SDL_IOWhence.SDL_IO_SEEK_SET
}

actual object SDLIO {

    actual fun openFile(path: String, mode: String): SDLIOStream? =
        SDL_IOFromFile(path, mode)?.let { NativeSDLIOStream(it, owned = true) }

    actual fun fromMem(data: ByteArray): SDLIOStream? = memScoped {
        val ptr = allocArray<ByteVar>(data.size)
        for (i in data.indices) ptr[i] = data[i].toByte()
        SDL_kmp_IOFromMem(ptr, data.size)?.let { NativeSDLIOStream(it, owned = true) }
    }

    actual fun fromConstMem(data: ByteArray): SDLIOStream? = memScoped {
        val ptr = allocArray<ByteVar>(data.size)
        for (i in data.indices) ptr[i] = data[i].toByte()
        SDL_kmp_IOFromConstMem(ptr, data.size)?.let { NativeSDLIOStream(it, owned = true) }
    }

    actual fun loadFile(path: String): ByteArray? = memScoped {
        val size = alloc<IntVar>()
        val data = SDL_kmp_LoadFile(path, size.ptr) ?: return null
        try {
            data.reinterpret<ByteVar>().readBytes(size.value)
        } finally {
            SDL_free(data)
        }
    }

    actual fun loadFile(stream: SDLIOStream): ByteArray? {
        val native = stream as? NativeSDLIOStream
            ?: throw IllegalArgumentException("stream is not a native SDL IO stream")
        return memScoped {
            val size = alloc<IntVar>()
            val data = SDL_kmp_LoadFileIO(native.check(), size.ptr) ?: return null
            native.raw = null
            try {
                data.reinterpret<ByteVar>().readBytes(size.value)
            } finally {
                SDL_free(data)
            }
        }
    }
}

// =========================================================================
// Properties
// =========================================================================

actual object SDLProperties {

    actual fun create(): ULong = SDL_CreateProperties().toULong()

    actual fun setProperty(props: ULong, name: String, value: Long): Boolean =
        SDL_SetPointerProperty(props.toUInt(), name, value.toCPointer<ByteVar>())

    actual fun setStringProperty(props: ULong, name: String, value: String?): Boolean =
        SDL_SetStringProperty(props.toUInt(), name, value)

    actual fun getProperty(props: ULong, name: String, defaultValue: Long): Long {
        val p = SDL_GetPointerProperty(props.toUInt(), name, defaultValue.toCPointer<ByteVar>())
        return if (p == null) defaultValue else p.rawValue.toLong()
    }

    actual fun getStringProperty(props: ULong, name: String): String? =
        SDL_GetStringProperty(props.toUInt(), name, null)?.toKString()

    actual fun hasProperty(props: ULong, name: String): Boolean = SDL_HasProperty(props.toUInt(), name)

    actual fun deleteProperty(props: ULong, name: String): Boolean = SDL_ClearProperty(props.toUInt(), name)

    actual fun copy(src: ULong, dst: ULong): Boolean = SDL_CopyProperties(src.toUInt(), dst.toUInt())

    actual val globalProperties: ULong
        get() = SDL_GetGlobalProperties().toULong()

    actual fun destroy(props: ULong) {
        SDL_DestroyProperties(props.toUInt())
    }
}

// =========================================================================
// Process
// =========================================================================

internal class NativeSDLProcess internal constructor(raw: CPointer<SDL_Process>?) : SDLProcess {

    internal var raw: CPointer<SDL_Process>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    private fun check(): CPointer<SDL_Process> =
        raw ?: throw IllegalStateException("SDL process is closed")

    override val active: Boolean
        get() = memScoped {
            val exit = alloc<IntVar>()
            !SDL_WaitProcess(check(), false, exit.ptr)
        }

    override fun wait(): Int = memScoped {
        val exit = alloc<IntVar>()
        SDL_WaitProcess(check(), true, exit.ptr)
        exit.value
    }

    override fun kill(force: Boolean): Boolean = SDL_KillProcess(check(), force)

    override fun readOutput(): ByteArray? = memScoped {
        val size = alloc<IntVar>()
        val exit = alloc<IntVar>()
        val data = SDL_kmp_ReadProcess(check(), size.ptr, exit.ptr) ?: return null
        try {
            data.reinterpret<ByteVar>().readBytes(size.value)
        } finally {
            SDL_free(data)
        }
    }

    override fun close() {
        val process = raw ?: return
        raw = null
        SDL_DestroyProcess(process)
    }
}

actual object SDLProcesses {

    actual fun createProcess(args: List<String>): SDLProcess? = memScoped {
        val argv = allocArray<CPointerVar<ByteVar>>(args.size + 1)
        for (i in args.indices) {
            argv[i] = args[i].cstr.ptr
        }
        argv[args.size] = null
        SDL_CreateProcess(argv, true)?.let { NativeSDLProcess(it) }
    }
}

// =========================================================================
// Camera
// =========================================================================

internal class NativeSDLCamera internal constructor(raw: CPointer<SDL_Camera>?, internal val cameraId: Int) : SDLCamera {

    internal var raw: CPointer<SDL_Camera>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    private fun check(): CPointer<SDL_Camera> =
        raw ?: throw IllegalStateException("SDL camera is closed")

    override val id: Int
        get() = SDL_GetCameraID(check()).toInt()

    override val format: SDLCameraSpec?
        get() = memScoped {
            val spec = alloc<SDL_CameraSpec>()
            if (SDL_GetCameraFormat(check(), spec.ptr)) {
                SDLCameraSpec(spec.format.toInt(), spec.width, spec.height, if (spec.framerate_denominator > 0) spec.framerate_numerator / spec.framerate_denominator else 0)
            } else {
                null
            }
        }

    override val permissionState: Int
        get() = SDL_GetCameraPermissionState(check())

    override fun supportsFormat(spec: SDLCameraSpec): Boolean =
        SDLCameras.getSupportedFormats(id).any { it.format == spec.format && it.width == spec.width && it.height == spec.height }

    override fun acquireFrame(): SDLSurface? {
        val surface = SDL_AcquireCameraFrame(check(), null) ?: return null
        return NativeSDLSurface(surface, owned = false)
    }

    override fun releaseFrame(frame: SDLSurface) {
        val native = frame as? NativeSDLSurface
            ?: throw IllegalArgumentException("frame is not a native SDL surface")
        native.raw?.let { SDL_ReleaseCameraFrame(check(), it) }
    }

    override fun close() {
        val camera = raw ?: return
        raw = null
        SDL_CloseCamera(camera)
    }
}

actual object SDLCameras {

    actual val devices: List<Int>
        get() = memScoped {
            val count = alloc<IntVar>()
            val ids = SDL_GetCameras(count.ptr) ?: return emptyList()
            try {
                (0 until count.value).map { ids[it].toInt() }
            } finally {
                SDL_free(ids)
            }
        }

    actual fun getDeviceName(deviceId: Int): String? = SDL_GetCameraName(deviceId.toUInt())?.toKString()

    actual fun getDevicePosition(deviceId: Int): Int =
        SDL_GetCameraPosition(deviceId.toUInt()).value.toInt()

    actual fun getSupportedFormats(deviceId: Int): List<SDLCameraSpec> = memScoped {
        val count = alloc<IntVar>()
        val formats = SDL_GetCameraSupportedFormats(deviceId.toUInt(), count.ptr) ?: return emptyList()
        try {
            (0 until count.value).mapNotNull { i ->
                val f = formats[i] ?: return@mapNotNull null
                val cs = f.pointed
                SDLCameraSpec(cs.format.toInt(), cs.width, cs.height, if (cs.framerate_denominator > 0) cs.framerate_numerator / cs.framerate_denominator else 0)
            }
        } finally {
            SDL_free(formats)
        }
    }

    actual fun open(deviceId: Int, spec: SDLCameraSpec?): SDLCamera {
        val native = memScoped {
            spec?.let {
                val s = alloc<SDL_CameraSpec>()
                s.format = it.format.toUInt()
                s.width = it.width
                s.height = it.height
                s.framerate_numerator = it.framerate
                s.framerate_denominator = 1
                s.ptr
            }
        }
        val ptr = SDL_OpenCamera(deviceId.toUInt(), native)
            ?: throw IllegalStateException("SDL_OpenCamera failed: ${SDL.error()}")
        return NativeSDLCamera(ptr, deviceId)
    }
}

// =========================================================================
// Sensor
// =========================================================================

internal class NativeSDLSensor internal constructor(raw: CPointer<SDL_Sensor>?) : SDLSensor {

    internal var raw: CPointer<SDL_Sensor>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    private fun check(): CPointer<SDL_Sensor> =
        raw ?: throw IllegalStateException("SDL sensor is closed")

    override val id: Int
        get() = SDL_GetSensorID(check()).toInt()

    override val name: String?
        get() = SDL_GetSensorNameForID(SDL_GetSensorID(check()))?.toKString()

    override val type: Int
        get() = SDL_GetSensorType(check())

    override fun data(): FloatArray {
        val data = FloatArray(3)
        val ok = memScoped {
            val f = allocArray<FloatVar>(3)
            val result = SDL_GetSensorData(check(), f, 3)
            if (result) {
                for (i in 0 until 3) data[i] = f[i]
            }
            result
        }
        return if (ok) data else FloatArray(0)
    }

    override fun close() {
        val sensor = raw ?: return
        raw = null
        SDL_CloseSensor(sensor)
    }
}

actual object SDLSensors {

    actual val devices: List<Int>
        get() = memScoped {
            val count = alloc<IntVar>()
            val ids = SDL_GetSensors(count.ptr) ?: return emptyList()
            try {
                (0 until count.value).map { ids[it].toLong().toInt() }
            } finally {
                SDL_free(ids)
            }
        }

    actual fun getDeviceName(deviceId: Int): String? =
        SDL_GetSensorNameForID(deviceId.toUInt())?.toKString()

    actual fun getDeviceType(deviceId: Int): Int =
        SDL_GetSensorTypeForID(deviceId.toUInt())

    actual fun open(deviceId: Int): SDLSensor {
        val ptr = SDL_OpenSensor(deviceId.toUInt())
            ?: throw IllegalStateException("SDL_OpenSensor failed: ${SDL.error()}")
        return NativeSDLSensor(ptr)
    }
}

// =========================================================================
// Haptic
// =========================================================================

internal class NativeSDLHaptic internal constructor(raw: CPointer<SDL_Haptic>?, internal val hapticId: Int) : SDLHaptic {

    internal var raw: CPointer<SDL_Haptic>? = raw

    override val ptr: Long
        get() = raw?.rawValue?.toLong() ?: 0L

    private fun check(): CPointer<SDL_Haptic> =
        raw ?: throw IllegalStateException("SDL haptic is closed")

    override val id: Int
        get() = SDL_GetHapticID(check()).toInt()

    override val name: String?
        get() = SDL_GetHapticName(check())?.toKString()

    override val numAxes: Int
        get() = SDL_GetNumHapticAxes(check())

    override fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean = memScoped {
        val effect = alloc<SDL_HapticEffect>()
        effect.type = SDL_HAPTIC_LEFTRIGHT.toUShort()
        effect.leftright.length = durationMs.toUInt()
        effect.leftright.large_magnitude = lowFrequency.toUShort()
        effect.leftright.small_magnitude = highFrequency.toUShort()
        val id = SDL_CreateHapticEffect(check(), effect.ptr)
        if (id == 0) return false
        val ok = SDL_RunHapticEffect(check(), id, 1u)
        SDL_DestroyHapticEffect(check(), id)
        ok
    }

    override fun stopEffect(effectId: Int): Boolean =
        SDL_StopHapticEffect(check(), effectId)

    override fun effectStatus(effectId: Int): Boolean =
        SDL_GetHapticEffectStatus(check(), effectId)

    override fun destroyEffect(effectId: Int): Boolean {
        SDL_DestroyHapticEffect(check(), effectId)
        return true
    }

    override fun close() {
        val haptic = raw ?: return
        raw = null
        SDL_CloseHaptic(haptic)
    }
}

actual object SDLHaptics {

    actual val devices: List<Int>
        get() = memScoped {
            val count = alloc<IntVar>()
            val ids = SDL_GetHaptics(count.ptr) ?: return emptyList()
            try {
                (0 until count.value).map { ids[it].toInt() }
            } finally {
                SDL_free(ids)
            }
        }

    actual fun getDeviceName(deviceId: Int): String? =
        SDL_GetHapticNameForID(deviceId.toUInt())?.toKString()

    actual fun open(deviceId: Int): SDLHaptic {
        val ptr = SDL_OpenHaptic(deviceId.toUInt())
            ?: throw IllegalStateException("SDL_OpenHaptic failed: ${SDL.error()}")
        return NativeSDLHaptic(ptr, deviceId)
    }
}
