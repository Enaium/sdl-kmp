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

// JNI bridge for sdl-kmp: the SDL3 audio subsystem.

#include <SDL3/SDL.h>

#include "jni_bridge.h"

SDLJNI_FUNC(jintArray) SDLJNI_NAME(audioPlaybackDevices)(JNIEnv *env, jclass) {
    int count = 0;
    SDL_AudioDeviceID *devices = SDL_GetAudioPlaybackDevices(&count);
    if (devices == nullptr) return nullptr;
    std::vector<jint> out;
    out.reserve(static_cast<size_t>(count));
    for (int i = 0; i < count; i++) {
        out.push_back(static_cast<jint>(devices[i]));
    }
    SDL_free(devices);
    return sdl_kmp_jni_new_int_array(env, out);
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(audioRecordingDevices)(JNIEnv *env, jclass) {
    int count = 0;
    SDL_AudioDeviceID *devices = SDL_GetAudioRecordingDevices(&count);
    if (devices == nullptr) return nullptr;
    std::vector<jint> out;
    out.reserve(static_cast<size_t>(count));
    for (int i = 0; i < count; i++) {
        out.push_back(static_cast<jint>(devices[i]));
    }
    SDL_free(devices);
    return sdl_kmp_jni_new_int_array(env, out);
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getAudioDeviceName)(JNIEnv *env, jclass, jint deviceId) {
    return sdl_kmp_jni_to_string(env, SDL_GetAudioDeviceName(deviceId));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(openAudioDevice)(JNIEnv *, jclass, jint deviceId, jint format,
                                               jint channels, jint freq) {
    SDL_AudioSpec spec{};
    spec.format = static_cast<SDL_AudioFormat>(format);
    spec.channels = static_cast<Uint8>(channels);
    spec.freq = freq;
    return static_cast<jint>(SDL_OpenAudioDevice(deviceId, &spec));
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(openAudioDeviceStream)(JNIEnv *, jclass, jint deviceId, jint format,
                                                      jint channels, jint freq) {
    SDL_AudioSpec spec{};
    spec.format = static_cast<SDL_AudioFormat>(format);
    spec.channels = static_cast<Uint8>(channels);
    spec.freq = freq;
    return reinterpret_cast<jlong>(SDL_OpenAudioDeviceStream(deviceId, &spec, nullptr, nullptr));
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(createAudioStream)(JNIEnv *, jclass, jint srcFormat, jint srcChannels,
                                                  jint srcFreq, jint dstFormat, jint dstChannels,
                                                  jint dstFreq) {
    SDL_AudioSpec src{};
    src.format = static_cast<SDL_AudioFormat>(srcFormat);
    src.channels = static_cast<Uint8>(srcChannels);
    src.freq = srcFreq;
    SDL_AudioSpec dst{};
    dst.format = static_cast<SDL_AudioFormat>(dstFormat);
    dst.channels = static_cast<Uint8>(dstChannels);
    dst.freq = dstFreq;
    return reinterpret_cast<jlong>(SDL_CreateAudioStream(&src, &dst));
}

SDLJNI_FUNC(void) SDLJNI_NAME(pauseAudioDevice)(JNIEnv *, jclass, jint deviceId) {
    if (!SDL_AudioDevicePaused(deviceId)) {
        SDL_PauseAudioDevice(deviceId);
    }
}

SDLJNI_FUNC(void) SDLJNI_NAME(resumeAudioDevice)(JNIEnv *, jclass, jint deviceId) {
    if (SDL_AudioDevicePaused(deviceId)) {
        SDL_PauseAudioDevice(deviceId);
    }
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(audioDevicePaused)(JNIEnv *, jclass, jint deviceId) {
    return SDL_AudioDevicePaused(deviceId) ? JNI_TRUE : JNI_FALSE;
}

// outSpec is an int[3] out-param: [format, channels, freq].
SDLJNI_FUNC(jbyteArray) SDLJNI_NAME(loadWav)(JNIEnv *env, jclass, jstring path, jintArray outSpec) {
    const char *p = path ? env->GetStringUTFChars(path, nullptr) : nullptr;
    SDL_AudioSpec spec{};
    Uint8 *data = nullptr;
    Uint32 length = 0;
    bool ok = SDL_LoadWAV(p, &spec, &data, &length);
    if (path) env->ReleaseStringUTFChars(path, p);
    if (!ok || data == nullptr) return nullptr;
    jint specValues[3] = {static_cast<jint>(spec.format), spec.channels, spec.freq};
    env->SetIntArrayRegion(outSpec, 0, 3, specValues);
    jbyteArray result = sdl_kmp_jni_new_byte_array(env, data, static_cast<jsize>(length));
    SDL_free(data);
    return result;
}

// Returns [format, channels, freq, sampleFrames] or null.
SDLJNI_FUNC(jintArray) SDLJNI_NAME(getAudioDeviceFormat)(JNIEnv *env, jclass, jint deviceId) {
    SDL_AudioSpec spec{};
    int sampleFrames = 0;
    if (!SDL_GetAudioDeviceFormat(deviceId, &spec, &sampleFrames)) return nullptr;
    return sdl_kmp_jni_new_int_array(
        env, {static_cast<jint>(spec.format), spec.channels, spec.freq, sampleFrames});
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(bindAudioStream)(JNIEnv *, jclass, jint deviceId, jlong stream) {
    return SDL_BindAudioStream(deviceId, reinterpret_cast<SDL_AudioStream *>(stream)) ? JNI_TRUE
                                                                                      : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(unbindAudioStream)(JNIEnv *, jclass, jint deviceId, jlong stream) {
    SDL_UnbindAudioStream(reinterpret_cast<SDL_AudioStream *>(stream));
}

SDLJNI_FUNC(void) SDLJNI_NAME(closeAudioDevice)(JNIEnv *, jclass, jint deviceId) {
    SDL_CloseAudioDevice(deviceId);
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(putAudioStreamData)(JNIEnv *env, jclass, jlong stream,
                                                      jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    if (len <= 0) return JNI_TRUE;
    std::vector<jbyte> buffer(static_cast<size_t>(len));
    env->GetByteArrayRegion(data, 0, len, buffer.data());
    return SDL_PutAudioStreamData(reinterpret_cast<SDL_AudioStream *>(stream), buffer.data(), len)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jbyteArray) SDLJNI_NAME(getAudioStreamData)(JNIEnv *env, jclass, jlong stream,
                                                        jint maxLen) {
    if (maxLen <= 0) return nullptr;
    std::vector<jbyte> buffer(static_cast<size_t>(maxLen));
    int read = SDL_GetAudioStreamData(reinterpret_cast<SDL_AudioStream *>(stream), buffer.data(),
                                      maxLen);
    if (read <= 0) return nullptr;
    return sdl_kmp_jni_new_byte_array(env, buffer.data(), read);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getAudioStreamAvailable)(JNIEnv *, jclass, jlong stream) {
    return SDL_GetAudioStreamAvailable(reinterpret_cast<SDL_AudioStream *>(stream));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getAudioStreamQueued)(JNIEnv *, jclass, jlong stream) {
    return SDL_GetAudioStreamQueued(reinterpret_cast<SDL_AudioStream *>(stream));
}

// Returns [srcFormat, srcChannels, srcFreq, dstFormat, dstChannels, dstFreq] or null.
SDLJNI_FUNC(jintArray) SDLJNI_NAME(getAudioStreamFormat)(JNIEnv *env, jclass, jlong stream) {
    SDL_AudioSpec src{};
    SDL_AudioSpec dst{};
    if (!SDL_GetAudioStreamFormat(reinterpret_cast<SDL_AudioStream *>(stream), &src, &dst)) {
        return nullptr;
    }
    return sdl_kmp_jni_new_int_array(env, {static_cast<jint>(src.format), src.channels, src.freq,
                                           static_cast<jint>(dst.format), dst.channels, dst.freq});
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setAudioStreamFormat)(JNIEnv *, jclass, jlong stream,
                                                        jint srcFormat, jint srcChannels,
                                                        jint srcFreq, jint dstFormat,
                                                        jint dstChannels, jint dstFreq) {
    SDL_AudioSpec src{};
    src.format = static_cast<SDL_AudioFormat>(srcFormat);
    src.channels = static_cast<Uint8>(srcChannels);
    src.freq = srcFreq;
    SDL_AudioSpec dst{};
    dst.format = static_cast<SDL_AudioFormat>(dstFormat);
    dst.channels = static_cast<Uint8>(dstChannels);
    dst.freq = dstFreq;
    return SDL_SetAudioStreamFormat(reinterpret_cast<SDL_AudioStream *>(stream), &src, &dst)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jfloat) SDLJNI_NAME(getAudioStreamGain)(JNIEnv *, jclass, jlong stream) {
    return SDL_GetAudioStreamGain(reinterpret_cast<SDL_AudioStream *>(stream));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setAudioStreamGain)(JNIEnv *, jclass, jlong stream, jfloat gain) {
    return SDL_SetAudioStreamGain(reinterpret_cast<SDL_AudioStream *>(stream), gain) ? JNI_TRUE
                                                                                     : JNI_FALSE;
}

SDLJNI_FUNC(jfloat) SDLJNI_NAME(getAudioStreamFrequencyRatio)(JNIEnv *, jclass, jlong stream) {
    return SDL_GetAudioStreamFrequencyRatio(reinterpret_cast<SDL_AudioStream *>(stream));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setAudioStreamFrequencyRatio)(JNIEnv *, jclass, jlong stream,
                                                                jfloat ratio) {
    return SDL_SetAudioStreamFrequencyRatio(reinterpret_cast<SDL_AudioStream *>(stream), ratio)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(audioStreamDevicePaused)(JNIEnv *, jclass, jlong stream) {
    return SDL_AudioStreamDevicePaused(reinterpret_cast<SDL_AudioStream *>(stream)) ? JNI_TRUE
                                                                                    : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(pauseAudioStreamDevice)(JNIEnv *, jclass, jlong stream) {
    SDL_PauseAudioStreamDevice(reinterpret_cast<SDL_AudioStream *>(stream));
}

SDLJNI_FUNC(void) SDLJNI_NAME(resumeAudioStreamDevice)(JNIEnv *, jclass, jlong stream) {
    SDL_ResumeAudioStreamDevice(reinterpret_cast<SDL_AudioStream *>(stream));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(flushAudioStream)(JNIEnv *, jclass, jlong stream) {
    return SDL_FlushAudioStream(reinterpret_cast<SDL_AudioStream *>(stream)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(clearAudioStream)(JNIEnv *, jclass, jlong stream) {
    return SDL_ClearAudioStream(reinterpret_cast<SDL_AudioStream *>(stream)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(destroyAudioStream)(JNIEnv *, jclass, jlong stream) {
    SDL_DestroyAudioStream(reinterpret_cast<SDL_AudioStream *>(stream));
}
