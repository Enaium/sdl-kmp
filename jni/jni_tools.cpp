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

// JNI bridge for sdl-kmp: IO streams, properties, cameras, sensors and
// haptics.

#include <SDL3/SDL.h>

#include <mutex>

#include "jni_bridge.h"

// Buffers backing SDL_IOFromMem streams (SDL_CloseIO does not free the
// caller's buffer; we must). Shared with jni_bridge.cpp.
extern std::mutex g_ioMemMutex;
extern std::unordered_map<SDL_IOStream *, void *> g_ioMemBuffers;

// ===========================================================================
// IO
// ===========================================================================

SDLJNI_FUNC(jlong) SDLJNI_NAME(ioFromFile)(JNIEnv *env, jclass, jstring path, jstring mode) {
    const char *p = path ? env->GetStringUTFChars(path, nullptr) : nullptr;
    const char *m = mode ? env->GetStringUTFChars(mode, nullptr) : nullptr;
    SDL_IOStream *stream = SDL_IOFromFile(p, m);
    if (path) env->ReleaseStringUTFChars(path, p);
    if (mode) env->ReleaseStringUTFChars(mode, m);
    return reinterpret_cast<jlong>(stream);
}

// SDL_IOFromMem does not own the buffer: keep it in g_ioMemBuffers so
// ioClose/ioLoadFile can free it together with the stream.
SDLJNI_FUNC(jlong) SDLJNI_NAME(ioFromMem)(JNIEnv *env, jclass, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    void *buffer = SDL_malloc(static_cast<size_t>(len > 0 ? len : 1));
    if (len > 0) {
        env->GetByteArrayRegion(data, 0, len, static_cast<jbyte *>(buffer));
    }
    SDL_IOStream *stream = SDL_IOFromMem(buffer, len);
    if (stream == nullptr) {
        SDL_free(buffer);
        return 0;
    }
    {
        std::lock_guard<std::mutex> lock(g_ioMemMutex);
        g_ioMemBuffers[stream] = buffer;
    }
    return reinterpret_cast<jlong>(stream);
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(ioFromConstMem)(JNIEnv *env, jclass, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    void *buffer = SDL_malloc(static_cast<size_t>(len > 0 ? len : 1));
    if (len > 0) {
        env->GetByteArrayRegion(data, 0, len, static_cast<jbyte *>(buffer));
    }
    SDL_IOStream *stream = SDL_IOFromConstMem(buffer, len);
    if (stream == nullptr) {
        SDL_free(buffer);
        return 0;
    }
    {
        std::lock_guard<std::mutex> lock(g_ioMemMutex);
        g_ioMemBuffers[stream] = buffer;
    }
    return reinterpret_cast<jlong>(stream);
}

static void FreeIoMemBuffer(SDL_IOStream *stream) {
    std::lock_guard<std::mutex> lock(g_ioMemMutex);
    auto it = g_ioMemBuffers.find(stream);
    if (it != g_ioMemBuffers.end()) {
        SDL_free(it->second);
        g_ioMemBuffers.erase(it);
    }
}

SDLJNI_FUNC(jbyteArray) SDLJNI_NAME(ioRead)(JNIEnv *env, jclass, jlong stream, jint size) {
    if (size <= 0) return nullptr;
    std::vector<jbyte> buffer(static_cast<size_t>(size));
    size_t read = SDL_ReadIO(reinterpret_cast<SDL_IOStream *>(stream), buffer.data(),
                             static_cast<size_t>(size));
    if (read <= 0) return nullptr;
    return sdl_kmp_jni_new_byte_array(env, buffer.data(), static_cast<jsize>(read));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(ioWrite)(JNIEnv *env, jclass, jlong stream, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    if (len <= 0) return 0;
    std::vector<jbyte> buffer(static_cast<size_t>(len));
    env->GetByteArrayRegion(data, 0, len, buffer.data());
    size_t written = SDL_WriteIO(reinterpret_cast<SDL_IOStream *>(stream), buffer.data(),
                                 static_cast<size_t>(len));
    return static_cast<jint>(written);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(ioSeek)(JNIEnv *, jclass, jlong stream, jint offset, jint whence) {
    Sint64 result = SDL_SeekIO(reinterpret_cast<SDL_IOStream *>(stream), offset, static_cast<SDL_IOWhence>(whence));
    if (result < 0) return -1;
    return static_cast<jint>(result);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(ioTell)(JNIEnv *, jclass, jlong stream) {
    Sint64 result = SDL_TellIO(reinterpret_cast<SDL_IOStream *>(stream));
    if (result < 0) return -1;
    return static_cast<jint>(result);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(ioSize)(JNIEnv *, jclass, jlong stream) {
    Sint64 result = SDL_GetIOSize(reinterpret_cast<SDL_IOStream *>(stream));
    if (result < 0) return -1;
    return static_cast<jint>(result);
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(ioFlush)(JNIEnv *, jclass, jlong stream) {
    return SDL_FlushIO(reinterpret_cast<SDL_IOStream *>(stream)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(ioClose)(JNIEnv *, jclass, jlong stream) {
    SDL_IOStream *s = reinterpret_cast<SDL_IOStream *>(stream);
    if (s == nullptr) return;
    FreeIoMemBuffer(s);
    SDL_CloseIO(s);
}

SDLJNI_FUNC(jbyteArray) SDLJNI_NAME(loadFile)(JNIEnv *env, jclass, jstring path) {
    const char *p = path ? env->GetStringUTFChars(path, nullptr) : nullptr;
    size_t size = 0;
    void *data = SDL_LoadFile(p, &size);
    if (path) env->ReleaseStringUTFChars(path, p);
    if (data == nullptr) return nullptr;
    jbyteArray result = sdl_kmp_jni_new_byte_array(env, data, static_cast<jsize>(size));
    SDL_free(data);
    return result;
}

SDLJNI_FUNC(jbyteArray) SDLJNI_NAME(loadFileIO)(JNIEnv *env, jclass, jlong stream) {
    SDL_IOStream *s = reinterpret_cast<SDL_IOStream *>(stream);
    if (s == nullptr) return nullptr;
    size_t size = 0;
    void *data = SDL_LoadFile_IO(s, &size, true);
    FreeIoMemBuffer(s);
    if (data == nullptr) return nullptr;
    jbyteArray result = sdl_kmp_jni_new_byte_array(env, data, static_cast<jsize>(size));
    SDL_free(data);
    return result;
}

// ===========================================================================
// Properties
// ===========================================================================

SDLJNI_FUNC(jint) SDLJNI_NAME(propertiesCreate)(JNIEnv *, jclass) {
    return static_cast<jint>(SDL_CreateProperties());
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setPointerProperty)(JNIEnv *env, jclass, jint props,
                                                      jstring name, jlong value) {
    const char *n = name ? env->GetStringUTFChars(name, nullptr) : nullptr;
    bool ok = SDL_SetPointerProperty(static_cast<SDL_PropertiesID>(props), n,
                                     reinterpret_cast<void *>(value));
    if (name) env->ReleaseStringUTFChars(name, n);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setStringProperty)(JNIEnv *env, jclass, jint props,
                                                     jstring name, jstring value) {
    const char *n = name ? env->GetStringUTFChars(name, nullptr) : nullptr;
    const char *v = nullptr;
    if (value != nullptr) {
        v = env->GetStringUTFChars(value, nullptr);
    }
    bool ok = SDL_SetStringProperty(static_cast<SDL_PropertiesID>(props), n, v);
    if (name) env->ReleaseStringUTFChars(name, n);
    if (value) env->ReleaseStringUTFChars(value, v);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(getPointerProperty)(JNIEnv *env, jclass, jint props, jstring name,
                                                   jlong defaultValue) {
    const char *n = name ? env->GetStringUTFChars(name, nullptr) : nullptr;
    void *value = SDL_GetPointerProperty(static_cast<SDL_PropertiesID>(props), n,
                                         reinterpret_cast<void *>(defaultValue));
    if (name) env->ReleaseStringUTFChars(name, n);
    return reinterpret_cast<jlong>(value);
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getStringProperty)(JNIEnv *env, jclass, jint props, jstring name) {
    const char *n = name ? env->GetStringUTFChars(name, nullptr) : nullptr;
    const char *value = SDL_GetStringProperty(static_cast<SDL_PropertiesID>(props), n, nullptr);
    jstring result = sdl_kmp_jni_to_string(env, value);
    if (name) env->ReleaseStringUTFChars(name, n);
    return result;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(hasProperty)(JNIEnv *env, jclass, jint props, jstring name) {
    const char *n = name ? env->GetStringUTFChars(name, nullptr) : nullptr;
    bool ok = SDL_HasProperty(static_cast<SDL_PropertiesID>(props), n);
    if (name) env->ReleaseStringUTFChars(name, n);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(clearProperty)(JNIEnv *env, jclass, jint props, jstring name) {
    const char *n = name ? env->GetStringUTFChars(name, nullptr) : nullptr;
    bool ok = SDL_ClearProperty(static_cast<SDL_PropertiesID>(props), n);
    if (name) env->ReleaseStringUTFChars(name, n);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(copyProperties)(JNIEnv *, jclass, jint src, jint dst) {
    return SDL_CopyProperties(static_cast<SDL_PropertiesID>(src), static_cast<SDL_PropertiesID>(dst))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(globalProperties)(JNIEnv *, jclass) {
    return static_cast<jint>(SDL_GetGlobalProperties());
}

SDLJNI_FUNC(void) SDLJNI_NAME(destroyProperties)(JNIEnv *, jclass, jint props) {
    SDL_DestroyProperties(static_cast<SDL_PropertiesID>(props));
}

// ===========================================================================
// Camera
// ===========================================================================

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getCameras)(JNIEnv *env, jclass) {
    int count = 0;
    SDL_CameraID *devices = SDL_GetCameras(&count);
    if (devices == nullptr) return nullptr;
    std::vector<jint> out;
    out.reserve(static_cast<size_t>(count));
    for (int i = 0; i < count; i++) {
        out.push_back(static_cast<jint>(devices[i]));
    }
    SDL_free(devices);
    return sdl_kmp_jni_new_int_array(env, out);
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getCameraName)(JNIEnv *env, jclass, jint deviceId) {
    return sdl_kmp_jni_to_string(env, SDL_GetCameraName(deviceId));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getCameraPosition)(JNIEnv *, jclass, jint deviceId) {
    return static_cast<jint>(SDL_GetCameraPosition(deviceId));
}

// Returns a flat array [format, colorspace, width, height, fpsNum, fpsDen] per spec.
SDLJNI_FUNC(jintArray) SDLJNI_NAME(getCameraSupportedFormats)(JNIEnv *env, jclass, jint deviceId) {
    int count = 0;
    SDL_CameraSpec **formats = SDL_GetCameraSupportedFormats(deviceId, &count);
    if (formats == nullptr) return nullptr;
    std::vector<jint> out;
    out.reserve(static_cast<size_t>(count) * 6);
    for (int i = 0; i < count; i++) {
        const SDL_CameraSpec *spec = formats[i];
        if (spec == nullptr) continue;
        out.push_back(static_cast<jint>(spec->format));
        out.push_back(static_cast<jint>(spec->colorspace));
        out.push_back(spec->width);
        out.push_back(spec->height);
        out.push_back(spec->framerate_numerator);
        out.push_back(spec->framerate_denominator);
    }
    SDL_free(formats);
    return sdl_kmp_jni_new_int_array(env, out);
}

// framerate == 0 opens with a NULL spec (device default).
SDLJNI_FUNC(jlong) SDLJNI_NAME(openCamera)(JNIEnv *, jclass, jint deviceId, jint format, jint width,
                                           jint height, jint framerate) {
    if (framerate <= 0) {
        return reinterpret_cast<jlong>(SDL_OpenCamera(deviceId, nullptr));
    }
    SDL_CameraSpec spec{};
    spec.format = static_cast<SDL_PixelFormat>(format);
    spec.width = width;
    spec.height = height;
    spec.framerate_numerator = framerate;
    spec.framerate_denominator = 1;
    return reinterpret_cast<jlong>(SDL_OpenCamera(deviceId, &spec));
}

// Returns [format, colorspace, width, height, fpsNum, fpsDen] or null.
SDLJNI_FUNC(jintArray) SDLJNI_NAME(getCameraFormat)(JNIEnv *env, jclass, jlong camera) {
    SDL_CameraSpec spec{};
    if (!SDL_GetCameraFormat(reinterpret_cast<SDL_Camera *>(camera), &spec)) return nullptr;
    return sdl_kmp_jni_new_int_array(env, {static_cast<jint>(spec.format),
                                           static_cast<jint>(spec.colorspace), spec.width,
                                           spec.height, spec.framerate_numerator,
                                           spec.framerate_denominator});
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getCameraPermissionState)(JNIEnv *, jclass, jlong camera) {
    return static_cast<jint>(SDL_GetCameraPermissionState(reinterpret_cast<SDL_Camera *>(camera)));
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(acquireCameraFrame)(JNIEnv *, jclass, jlong camera) {
    return reinterpret_cast<jlong>(
        SDL_AcquireCameraFrame(reinterpret_cast<SDL_Camera *>(camera), nullptr));
}

SDLJNI_FUNC(void) SDLJNI_NAME(releaseCameraFrame)(JNIEnv *, jclass, jlong camera, jlong frame) {
    SDL_ReleaseCameraFrame(reinterpret_cast<SDL_Camera *>(camera),
                           reinterpret_cast<SDL_Surface *>(frame));
}

SDLJNI_FUNC(void) SDLJNI_NAME(closeCamera)(JNIEnv *, jclass, jlong camera) {
    SDL_CloseCamera(reinterpret_cast<SDL_Camera *>(camera));
}

// ===========================================================================
// Sensor
// ===========================================================================

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getSensors)(JNIEnv *env, jclass) {
    int count = 0;
    SDL_SensorID *devices = SDL_GetSensors(&count);
    if (devices == nullptr) return nullptr;
    std::vector<jint> out;
    out.reserve(static_cast<size_t>(count));
    for (int i = 0; i < count; i++) {
        out.push_back(static_cast<jint>(devices[i]));
    }
    SDL_free(devices);
    return sdl_kmp_jni_new_int_array(env, out);
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getSensorNameForID)(JNIEnv *env, jclass, jint deviceId) {
    return sdl_kmp_jni_to_string(env, SDL_GetSensorNameForID(deviceId));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getSensorTypeForID)(JNIEnv *, jclass, jint deviceId) {
    return static_cast<jint>(SDL_GetSensorTypeForID(deviceId));
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(openSensor)(JNIEnv *, jclass, jint deviceId) {
    return reinterpret_cast<jlong>(SDL_OpenSensor(deviceId));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getSensorID)(JNIEnv *, jclass, jlong sensor) {
    return static_cast<jint>(SDL_GetSensorID(reinterpret_cast<SDL_Sensor *>(sensor)));
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getSensorName)(JNIEnv *env, jclass, jlong sensor) {
    return sdl_kmp_jni_to_string(env, SDL_GetSensorName(reinterpret_cast<SDL_Sensor *>(sensor)));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getSensorType)(JNIEnv *, jclass, jlong sensor) {
    return static_cast<jint>(SDL_GetSensorType(reinterpret_cast<SDL_Sensor *>(sensor)));
}

SDLJNI_FUNC(jfloatArray) SDLJNI_NAME(getSensorData)(JNIEnv *env, jclass, jlong sensor) {
    float data[3] = {0.0f, 0.0f, 0.0f};
    if (SDL_GetSensorData(reinterpret_cast<SDL_Sensor *>(sensor), data, 3)) {
        return sdl_kmp_jni_new_float_array(env, {data[0], data[1], data[2]});
    }
    return nullptr;
}

SDLJNI_FUNC(void) SDLJNI_NAME(closeSensor)(JNIEnv *, jclass, jlong sensor) {
    SDL_CloseSensor(reinterpret_cast<SDL_Sensor *>(sensor));
}

// ===========================================================================
// Haptic
// ===========================================================================

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getHaptics)(JNIEnv *env, jclass) {
    int count = 0;
    SDL_HapticID *devices = SDL_GetHaptics(&count);
    if (devices == nullptr) return nullptr;
    std::vector<jint> out;
    out.reserve(static_cast<size_t>(count));
    for (int i = 0; i < count; i++) {
        out.push_back(static_cast<jint>(devices[i]));
    }
    SDL_free(devices);
    return sdl_kmp_jni_new_int_array(env, out);
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getHapticNameForID)(JNIEnv *env, jclass, jint deviceId) {
    return sdl_kmp_jni_to_string(env, SDL_GetHapticNameForID(deviceId));
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(openHaptic)(JNIEnv *, jclass, jint deviceId) {
    return reinterpret_cast<jlong>(SDL_OpenHaptic(deviceId));
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getHapticName)(JNIEnv *env, jclass, jlong haptic) {
    return sdl_kmp_jni_to_string(env, SDL_GetHapticName(reinterpret_cast<SDL_Haptic *>(haptic)));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getNumHapticAxes)(JNIEnv *, jclass, jlong haptic) {
    return SDL_GetNumHapticAxes(reinterpret_cast<SDL_Haptic *>(haptic));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(hapticRumble)(JNIEnv *, jclass, jlong haptic, jint lowFrequency,
                                                jint highFrequency, jint durationMs) {
    SDL_Haptic *h = reinterpret_cast<SDL_Haptic *>(haptic);
    SDL_HapticEffect effect{};
    SDL_zero(effect);
    effect.type = SDL_HAPTIC_LEFTRIGHT;
    effect.leftright.length = static_cast<Uint32>(durationMs);
    effect.leftright.large_magnitude = static_cast<Uint16>(lowFrequency);
    effect.leftright.small_magnitude = static_cast<Uint16>(highFrequency);
    int id = SDL_CreateHapticEffect(h, &effect);
    if (id < 0) return JNI_FALSE;
    bool ok = SDL_RunHapticEffect(h, id, 1);
    SDL_DestroyHapticEffect(h, id);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(stopHapticEffect)(JNIEnv *, jclass, jlong haptic, jint effectId) {
    return SDL_StopHapticEffect(reinterpret_cast<SDL_Haptic *>(haptic), effectId) ? JNI_TRUE
                                                                                  : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(hapticEffectStatus)(JNIEnv *, jclass, jlong haptic, jint effectId) {
    return SDL_GetHapticEffectStatus(reinterpret_cast<SDL_Haptic *>(haptic), effectId) ? JNI_TRUE
                                                                                       : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(destroyHapticEffect)(JNIEnv *, jclass, jlong haptic, jint effectId) {
    SDL_DestroyHapticEffect(reinterpret_cast<SDL_Haptic *>(haptic), effectId);
}

SDLJNI_FUNC(void) SDLJNI_NAME(closeHaptic)(JNIEnv *, jclass, jlong haptic) {
    SDL_CloseHaptic(reinterpret_cast<SDL_Haptic *>(haptic));
}
