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

// =========================================================================
// Logging
// =========================================================================

/** Log priorities (values match SDL3's SDL_LogPriority). */
object SDLLogPriority {
    const val TRACE = 0
    const val VERBOSE = 1
    const val DEBUG = 2
    const val INFO = 3
    const val WARN = 4
    const val ERROR = 5
    const val CRITICAL = 6
    const val INVALID = 7
}

/** Log categories (values match SDL3's SDL_LogCategory). */
object SDLLogCategory {
    const val APPLICATION = 0
    const val ERROR = 1
    const val ASSERT = 2
    const val SYSTEM = 3
    const val AUDIO = 4
    const val VIDEO = 5
    const val RENDER = 6
    const val INPUT = 7
    const val TEST = 8
    const val GPU = 9
    const val CUSTOM = 19
}

/** A log output callback; [message] is the formatted line. */
fun interface SDLLogOutput {
    fun log(priority: Int, category: Int, message: String)
}

/** SDL3 logging. */
expect object SDLLog {
    /** Logs [message] with [priority] under [category]. */
    fun log(priority: Int, category: Int, message: String)

    /** Logs [message] at DEBUG priority under [category] (default [SDLLogCategory.APPLICATION]). */
    fun debug(message: String, category: Int = SDLLogCategory.APPLICATION)

    /** Logs [message] at INFO priority under [category]. */
    fun info(message: String, category: Int = SDLLogCategory.APPLICATION)

    /** Logs [message] at WARN priority under [category]. */
    fun warn(message: String, category: Int = SDLLogCategory.APPLICATION)

    /** Logs [message] at ERROR priority under [category]. */
    fun error(message: String, category: Int = SDLLogCategory.APPLICATION)

    /** Logs [message] at CRITICAL priority under [category]. */
    fun critical(message: String, category: Int = SDLLogCategory.APPLICATION)

    /** Sets the priority of [category]; [priority] from [SDLLogPriority]. */
    fun setPriority(category: Int, priority: Int)

    /** The priority of [category]. */
    fun getPriority(category: Int): Int

    /** Sets the priority of all categories. */
    fun setAllPriority(priority: Int)

    /** Resets all priorities to their defaults. */
    fun resetPriorities()

    /** Installs [output] as the log output function (null restores the default). */
    fun setOutputFunction(output: SDLLogOutput?)
}

// =========================================================================
// Threads / synchronization
// =========================================================================

/** Thread priorities (values match SDL3's SDL_ThreadPriority). */
object SDLThreadPriority {
    const val LOW = 0
    const val NORMAL = 1
    const val HIGH = 2
    const val TIME_CRITICAL = 3
}

/** A native thread created through [SDLThreads.createThread]. */
interface SDLThread : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. On native, [cn.enaium.sdl.nativePtr] converts it to the typed pointer. */
    val ptr: Long

    val id: Int
    val name: String?

    /** Blocks until the thread finishes and returns its exit code. */
    fun wait(): Int

    /** Releases the thread handle without waiting. */
    fun detach()

    /** Releases the thread handle; the thread keeps running detached. */
    override fun close()
}

/** An SDL mutex. */
interface SDLMutex : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long

    fun lock()
    fun tryLock(): Boolean
    fun unlock()

    /** Releases the mutex. */
    override fun close()
}

/** An SDL read-write lock. */
interface SDLRWLock : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long

    fun lockRead()
    fun tryLockRead(): Boolean
    fun unlockRead()
    fun lockWrite()
    fun tryLockWrite(): Boolean
    fun unlockWrite()

    /** Releases the lock. */
    override fun close()
}

/** An SDL counting semaphore. */
interface SDLSemaphore : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long

    /** Blocks until the semaphore has a slot. */
    fun acquire()

    /** Like [wait] but gives up after [timeoutMs] milliseconds; returns `false` on timeout. */
    fun waitTimeout(timeoutMs: Int): Boolean

    /** Non-blocking acquire; returns `false` if it would block. */
    fun tryWait(): Boolean

    /** Releases a slot. */
    fun post()

    /** The current semaphore value. */
    val value: Int

    /** Releases the semaphore. */
    override fun close()
}

/** An SDL condition variable. */
interface SDLCondition : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long

    /** Waits on [mutex] (which must be locked) until signalled. */
    fun wait(mutex: SDLMutex)

    /** Like [wait] but gives up after [timeoutMs] milliseconds. */
    fun waitTimeout(mutex: SDLMutex, timeoutMs: Int): Boolean

    /** Wakes one waiter. */
    fun signal()

    /** Wakes all waiters. */
    fun broadcast()

    /** Releases the condition variable. */
    override fun close()
}

/** SDL3 threads and synchronization primitives. */
expect object SDLThreads {
    /** Number of logical CPU cores. */
    val numLogicalCPUCores: Int

    /** Creates a thread running [fn]; returns null on failure. */
    fun createThread(name: String?, fn: (Int) -> Unit): SDLThread?

    /** The ID of the current thread. */
    val currentThreadId: Int

    /** The ID of [thread]. */
    fun getThreadId(thread: SDLThread): Int

    /** The name of [thread], or null. */
    fun getThreadName(thread: SDLThread): String?

    /** Sets the priority of the current thread (see [SDLThreadPriority]). */
    fun setThreadPriority(priority: Int): Boolean

    /** Creates a mutex, or null on failure. */
    fun createMutex(): SDLMutex?

    /** Creates a read-write lock, or null on failure. */
    fun createRWLock(): SDLRWLock?

    /** Creates a semaphore with [initialValue] slots, or null on failure. */
    fun createSemaphore(initialValue: Int): SDLSemaphore?

    /** Creates a condition variable, or null on failure. */
    fun createCondition(): SDLCondition?
}

// =========================================================================
// IO streams
// =========================================================================

/** Seek origins for [SDLIOStream.seek] (values match SDL3's SDL_IOWhence). */
object SDLIOWhence {
    const val SET = 0
    const val CUR = 1
    const val END = 2
}

/** An SDL IO stream (file, memory or other). */
interface SDLIOStream : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long

    /** Reads up to [size] bytes; returns the bytes actually read. */
    fun read(size: Int): ByteArray

    /** Writes [data]; returns the number of bytes written. */
    fun write(data: ByteArray): Int

    /** Seeks to [offset] relative to [whence] (see [SDLIOWhence]); returns the new position or -1. */
    fun seek(offset: Int, whence: Int): Int

    /** The current position, or -1 on failure. */
    fun tell(): Int

    /** The stream size in bytes, or -1 on failure. */
    fun size(): Int

    /** Flushes buffered data; returns `false` on failure. */
    fun flush(): Boolean

    /** Closes and releases the stream. */
    override fun close()
}

/** SDL3 IO and file helpers. */
expect object SDLIO {
    /** Opens [path] with [mode] ("r", "w", "a", ...), or null on failure. */
    fun openFile(path: String, mode: String): SDLIOStream?

    /** Wraps [data] in a read-write memory stream (a copy is made). */
    fun fromMem(data: ByteArray): SDLIOStream?

    /** Wraps [data] in a read-only memory stream. */
    fun fromConstMem(data: ByteArray): SDLIOStream?

    /** Reads the whole file [path] into memory, or null on failure. */
    fun loadFile(path: String): ByteArray?

    /** Reads the remaining data of [stream] into memory (the stream is closed). */
    fun loadFile(stream: SDLIOStream): ByteArray?
}

// =========================================================================
// Properties
// =========================================================================

/**
 * SDL3 properties (a key/value store, values are pointer-sized).
 *
 * The ID is the raw SDL_PropertiesID handle; it is exposed through `ptr`.
 */
expect object SDLProperties {
    /** Creates a new property store; returns 0 on failure. */
    fun create(): ULong

    /** Sets [name] to [value] in the store; returns `false` on failure. */
    fun setProperty(props: ULong, name: String, value: Long): Boolean

    /** Sets [name] to [value] (a string copy) in the store. */
    fun setStringProperty(props: ULong, name: String, value: String?): Boolean

    /** The value of [name], or [defaultValue] when absent. */
    fun getProperty(props: ULong, name: String, defaultValue: Long): Long

    /** The string value of [name], or null when absent. */
    fun getStringProperty(props: ULong, name: String): String?

    /** Whether the store contains [name]. */
    fun hasProperty(props: ULong, name: String): Boolean

    /** Removes [name]; returns `false` when absent. */
    fun deleteProperty(props: ULong, name: String): Boolean

    /** Copies all properties from [src] into [dst]; returns `false` on failure. */
    fun copy(src: ULong, dst: ULong): Boolean

    /** The global properties store. */
    val globalProperties: ULong

    /** Destroys the store. */
    fun destroy(props: ULong)
}

// =========================================================================
// Processes
// =========================================================================

/** A subprocess started through [SDLProcesses.createProcess]. */
interface SDLProcess : AutoCloseable {
    /** The raw SDL handle address, or 0 on the JVM. */
    val ptr: Long

    /** Whether the process is still running. */
    val active: Boolean

    /** Blocks until the process exits and returns its exit code. */
    fun wait(): Int

    /** Terminates the process; [force] requests an immediate kill. */
    fun kill(force: Boolean): Boolean

    /** Reads all remaining stdout; returns null when no output is pending. */
    fun readOutput(): ByteArray?

    /** Releases the process handle. */
    override fun close()
}

/** SDL3 subprocess support. */
expect object SDLProcesses {
    /** Starts [args] (argv[0] is the executable); returns null on failure. */
    fun createProcess(args: List<String>): SDLProcess?
}

// =========================================================================
// Camera
// =========================================================================

/** Camera positions (values match SDL3's SDL_CameraPosition). */
object SDLCameraPosition {
    const val UNKNOWN = 0
    const val FRONT_FACING = 1
    const val BACK_FACING = 2
}

/** A camera format description. */
data class SDLCameraSpec(
    val format: Int,
    val width: Int,
    val height: Int,
    val framerate: Int,
)

/** An opened camera. */
interface SDLCamera : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long

    val id: Int

    /** The current output format, or null on failure. */
    val format: SDLCameraSpec?

    /** The camera permission state (0 = allowed, 1 = denied). */
    val permissionState: Int

    /** Whether [spec] is supported by this camera. */
    fun supportsFormat(spec: SDLCameraSpec): Boolean

    /** Acquires the next video frame, or null if none is ready yet. The surface must be released with [releaseFrame]. */
    fun acquireFrame(): SDLSurface?

    /** Releases a frame previously returned by [acquireFrame]. */
    fun releaseFrame(frame: SDLSurface)

    /** Closes and releases the camera. */
    override fun close()
}

/** SDL3 camera support. */
expect object SDLCameras {
    /** IDs of all connected cameras. */
    val devices: List<Int>

    /** The name of the camera with [deviceId], or null. */
    fun getDeviceName(deviceId: Int): String?

    /** The position of the camera with [deviceId] (see [SDLCameraPosition]). */
    fun getDevicePosition(deviceId: Int): Int

    /** The formats supported by the camera with [deviceId]. */
    fun getSupportedFormats(deviceId: Int): List<SDLCameraSpec>

    /** Opens the camera with [deviceId]; [spec] may be null for the default format. */
    fun open(deviceId: Int, spec: SDLCameraSpec?): SDLCamera
}

// =========================================================================
// Sensors
// =========================================================================

/** Sensor types (values match SDL3's SDL_SensorType). */
object SDLSensorType {
    const val INVALID = -1
    const val UNKNOWN = 0
    const val ACCEL = 1
    const val GYRO = 2
    const val ACCEL_L = 3
    const val GYRO_L = 4
    const val ACCEL_R = 5
    const val GYRO_R = 6
}

/** An opened sensor. */
interface SDLSensor : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long

    val id: Int
    val name: String?
    val type: Int

    /** The current sensor readings. */
    fun data(): FloatArray

    /** Closes and releases the sensor. */
    override fun close()
}

/** SDL3 sensor support. */
expect object SDLSensors {
    /** IDs of all connected sensors. */
    val devices: List<Int>

    /** The name of the sensor with [deviceId], or null. */
    fun getDeviceName(deviceId: Int): String?

    /** The type of the sensor with [deviceId] (see [SDLSensorType]). */
    fun getDeviceType(deviceId: Int): Int

    /** Opens the sensor with [deviceId]. */
    fun open(deviceId: Int): SDLSensor
}

// =========================================================================
// Haptics
// =========================================================================

/** An opened haptic (force feedback) device. */
interface SDLHaptic : AutoCloseable {
    /** The raw SDL handle address, or 0 after [close]. */
    val ptr: Long

    val id: Int
    val name: String?
    val numAxes: Int

    /** Runs a rumble (left/right) effect for [durationMs]; returns `false` on failure. */
    fun rumble(lowFrequency: Int, highFrequency: Int, durationMs: Int): Boolean

    /** Stops the effect at [effectId]. */
    fun stopEffect(effectId: Int): Boolean

    /** Whether the effect at [effectId] is still running. */
    fun effectStatus(effectId: Int): Boolean

    /** Destroys the effect at [effectId]. */
    fun destroyEffect(effectId: Int): Boolean

    /** Closes and releases the device. */
    override fun close()
}

/** SDL3 haptic (force feedback) support. */
expect object SDLHaptics {
    /** IDs of all haptic devices. */
    val devices: List<Int>

    /** The name of the haptic device with [deviceId], or null. */
    fun getDeviceName(deviceId: Int): String?

    /** Opens the haptic device with [deviceId]. */
    fun open(deviceId: Int): SDLHaptic
}
