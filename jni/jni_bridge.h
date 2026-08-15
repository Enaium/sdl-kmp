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

#ifndef SDL_KMP_JNI_BRIDGE_H
#define SDL_KMP_JNI_BRIDGE_H

#include <jni.h>
#include <stdint.h>
#include <mutex>
#include <unordered_map>
#include <vector>
#include <string>

// JNI entry-point naming macro: every external fun on the Kotlin
// `cn.enaium.sdl.Jni` object maps to Java_cn_enaium_sdl_Jni_<name>.
#define SDLJNI_FUNC(ret) extern "C" JNIEXPORT ret JNICALL
#define SDLJNI_NAME(name) Java_cn_enaium_sdl_Jni_##name

// ---------------------------------------------------------------------------
// Callback bridge (C -> Kotlin)
//
// SDL callbacks (event watch, file dialogs, log output) need to reach back
// into the JVM. The Kotlin side registers an `id` per callback; these
// functions attach the current thread if needed and dispatch to the static
// bridge methods on cn.enaium.sdl.Jni (onEventWatch / onDialogCallback /
// onLogOutput), which look the id up in the Kotlin callback maps.
// ---------------------------------------------------------------------------
void sdl_kmp_jni_init_callback_bridge(JNIEnv *env);
JNIEnv *sdl_kmp_jni_get_env();
jclass sdl_kmp_jni_get_string_class();
void sdl_kmp_jni_dispatch_dialog_callback(jlong id, const char *const *filelist);
void sdl_kmp_jni_dispatch_log_output(jlong id, int category, int priority, const char *message);

// ---------------------------------------------------------------------------
// Marshaling helpers
// ---------------------------------------------------------------------------

inline jstring sdl_kmp_jni_to_string(JNIEnv *env, const char *s) {
    return s ? env->NewStringUTF(s) : nullptr;
}

// Copies a NUL-terminated UTF-8 string into a std::string (SDL_GetError and
// friends return pointers into SDL-internal buffers that are only valid until
// the next SDL call, so the value must be copied before any further call).
inline std::string sdl_kmp_jni_copy_string(const char *s) {
    return s ? std::string(s) : std::string();
}

inline jintArray sdl_kmp_jni_new_int_array(JNIEnv *env, const std::vector<jint> &values) {
    jintArray arr = env->NewIntArray(static_cast<jsize>(values.size()));
    if (arr && !values.empty()) {
        env->SetIntArrayRegion(arr, 0, static_cast<jsize>(values.size()), values.data());
    }
    return arr;
}

inline jlongArray sdl_kmp_jni_new_long_array(JNIEnv *env, const std::vector<jlong> &values) {
    jlongArray arr = env->NewLongArray(static_cast<jsize>(values.size()));
    if (arr && !values.empty()) {
        env->SetLongArrayRegion(arr, 0, static_cast<jsize>(values.size()), values.data());
    }
    return arr;
}

inline jfloatArray sdl_kmp_jni_new_float_array(JNIEnv *env, const std::vector<jfloat> &values) {
    jfloatArray arr = env->NewFloatArray(static_cast<jsize>(values.size()));
    if (arr && !values.empty()) {
        env->SetFloatArrayRegion(arr, 0, static_cast<jsize>(values.size()), values.data());
    }
    return arr;
}

inline jbyteArray sdl_kmp_jni_new_byte_array(JNIEnv *env, const void *data, jsize len) {
    jbyteArray arr = env->NewByteArray(len);
    if (arr && data && len > 0) {
        env->SetByteArrayRegion(arr, 0, len, static_cast<const jbyte *>(data));
    }
    return arr;
}

// Reads an int[] from Kotlin into a std::vector (used for out-params).
inline bool sdl_kmp_jni_read_int_array(JNIEnv *env, jintArray arr, std::vector<jint> &out) {
    if (!arr) return false;
    jsize len = env->GetArrayLength(arr);
    out.resize(static_cast<size_t>(len));
    if (len > 0) {
        env->GetIntArrayRegion(arr, 0, len, out.data());
    }
    return true;
}

// Reads a float[] from Kotlin into a std::vector (used for out-params).
inline bool sdl_kmp_jni_read_float_array(JNIEnv *env, jfloatArray arr, std::vector<jfloat> &out) {
    if (!arr) return false;
    jsize len = env->GetArrayLength(arr);
    out.resize(static_cast<size_t>(len));
    if (len > 0) {
        env->GetFloatArrayRegion(arr, 0, len, out.data());
    }
    return true;
}

// Reads a long[] from Kotlin into a std::vector (used for out-params).
inline bool sdl_kmp_jni_read_long_array(JNIEnv *env, jlongArray arr, std::vector<jlong> &out) {
    if (!arr) return false;
    jsize len = env->GetArrayLength(arr);
    out.resize(static_cast<size_t>(len));
    if (len > 0) {
        env->GetLongArrayRegion(arr, 0, len, out.data());
    }
    return true;
}

#endif // SDL_KMP_JNI_BRIDGE_H
