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

// JNI bridge for the sdl-kmp JVM target: core SDL3 subsystem
// (init/error/version/timers), windows, renderer, textures, surfaces, events,
// displays, pixels, clipboard, hints, message boxes, file dialogs, logging,
// filesystem, power and misc.
//
// Every exported function maps 1:1 to an `external fun` on the Kotlin
// `cn.enaium.sdl.Jni` object. See jni_bridge.h for the naming convention and
// marshaling helpers.

#include <SDL3/SDL.h>
#include <SDL3/SDL_main.h>

#include <mutex>
#include <unordered_map>

#include "jni_bridge.h"

// ===========================================================================
// Callback bridge (C -> Kotlin)
// ===========================================================================

static JavaVM *g_vm = nullptr;
static jclass g_jniClass = nullptr;
static jmethodID g_onEventWatch = nullptr;
static jmethodID g_onDialogCallback = nullptr;
static jmethodID g_onLogOutput = nullptr;

static jclass g_stringClass = nullptr;

// Note: the JavaVM is captured in sdl_kmp_jni_init_callback_bridge via
// env->GetJavaVM(). SDL's own JNI_OnLoad (from SDL_dynapi.c) handles the
// JVM's JNI_OnLoad callback, so we must not define our own copy.

jclass sdl_kmp_jni_get_string_class() {
    return g_stringClass;
}

JNIEnv *sdl_kmp_jni_get_env() {
    if (g_vm == nullptr) {
        return nullptr;
    }
    JNIEnv *env = nullptr;
    if (g_vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        g_vm->AttachCurrentThread(reinterpret_cast<void **>(&env), nullptr);
    }
    return env;
}

void sdl_kmp_jni_init_callback_bridge(JNIEnv *env) {
    if (g_jniClass != nullptr) {
        return;
    }
    if (g_vm == nullptr) {
        env->GetJavaVM(&g_vm);
    }
    jclass cls = env->FindClass("cn/enaium/sdl/Jni");
    if (cls == nullptr) {
        env->ExceptionClear();
        return;
    }
    g_jniClass = static_cast<jclass>(env->NewGlobalRef(cls));
    env->DeleteLocalRef(cls);

    g_onEventWatch = env->GetStaticMethodID(g_jniClass, "onEventWatch", "(JJ)Z");
    g_onDialogCallback = env->GetStaticMethodID(g_jniClass, "onDialogCallback", "(J[Ljava/lang/String;)V");
    g_onLogOutput = env->GetStaticMethodID(g_jniClass, "onLogOutput", "(JIILjava/lang/String;)V");

    jclass s = env->FindClass("java/lang/String");
    g_stringClass = static_cast<jclass>(env->NewGlobalRef(s));
    env->DeleteLocalRef(s);
}

static bool SDLCALL EventWatchCallback(void *userdata, SDL_Event *event) {
    JNIEnv *env = sdl_kmp_jni_get_env();
    if (env == nullptr || g_jniClass == nullptr) {
        return true;
    }
    jboolean keep = env->CallStaticBooleanMethod(
        g_jniClass, g_onEventWatch,
        static_cast<jlong>(reinterpret_cast<intptr_t>(userdata)),
        static_cast<jlong>(reinterpret_cast<intptr_t>(event)));
    return keep == JNI_TRUE;
}

void sdl_kmp_jni_dispatch_dialog_callback(jlong id, const char *const *filelist) {
    JNIEnv *env = sdl_kmp_jni_get_env();
    if (env == nullptr || g_jniClass == nullptr) {
        return;
    }
    jobjectArray files = nullptr;
    if (filelist != nullptr) {
        int count = 0;
        while (filelist[count] != nullptr) {
            count++;
        }
        files = env->NewObjectArray(count, g_stringClass, nullptr);
        for (int i = 0; i < count; i++) {
            jstring s = env->NewStringUTF(filelist[i]);
            env->SetObjectArrayElement(files, i, s);
            env->DeleteLocalRef(s);
        }
    }
    env->CallStaticVoidMethod(g_jniClass, g_onDialogCallback, id, files);
    if (files != nullptr) {
        env->DeleteLocalRef(files);
    }
}

static void SDLCALL DialogCallback(void *userdata, const char *const *filelist, int filter) {
    (void)filter;
    sdl_kmp_jni_dispatch_dialog_callback(
        static_cast<jlong>(reinterpret_cast<intptr_t>(userdata)), filelist);
}

void sdl_kmp_jni_dispatch_log_output(jlong id, int category, int priority, const char *message) {
    JNIEnv *env = sdl_kmp_jni_get_env();
    if (env == nullptr || g_jniClass == nullptr) {
        return;
    }
    jstring msg = env->NewStringUTF(message != nullptr ? message : "");
    env->CallStaticVoidMethod(g_jniClass, g_onLogOutput, id, category, priority, msg);
    env->DeleteLocalRef(msg);
}

static void SDLCALL LogOutputCallback(void *userdata, int category, SDL_LogPriority priority,
                                      const char *message) {
    sdl_kmp_jni_dispatch_log_output(
        static_cast<jlong>(reinterpret_cast<intptr_t>(userdata)), category,
        static_cast<int>(priority), message);
}

// Event-watch callbacks registered from Kotlin (keyed by the Kotlin id), so
// SDL_DelEventWatch can be called with the exact function pointer.
std::mutex g_eventWatchMutex;
std::unordered_map<jlong, SDL_EventFilter> g_eventWatchCallbacks;

// Buffers backing SDL_IOFromMem streams (SDL_CloseIO does not free the
// caller's buffer; we must).
std::mutex g_ioMemMutex;
std::unordered_map<SDL_IOStream *, void *> g_ioMemBuffers;

// ===========================================================================
// Core
// ===========================================================================

SDLJNI_FUNC(void) SDLJNI_NAME(initCallbackBridge)(JNIEnv *env, jclass) {
    sdl_kmp_jni_init_callback_bridge(env);
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setMainReady)(JNIEnv *, jclass) {
    SDL_SetMainReady();
    return JNI_TRUE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(init)(JNIEnv *, jclass, jint flags) {
    return SDL_Init(static_cast<SDL_InitFlags>(flags)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(initSubSystem)(JNIEnv *, jclass, jint flags) {
    return SDL_InitSubSystem(static_cast<SDL_InitFlags>(flags)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(quitSubSystem)(JNIEnv *, jclass, jint flags) {
    SDL_QuitSubSystem(static_cast<SDL_InitFlags>(flags));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(wasInit)(JNIEnv *, jclass, jint flags) {
    return static_cast<jint>(SDL_WasInit(static_cast<SDL_InitFlags>(flags)));
}

SDLJNI_FUNC(void) SDLJNI_NAME(quit)(JNIEnv *, jclass) {
    SDL_Quit();
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getError)(JNIEnv *env, jclass) {
    const char *err = SDL_GetError();
    return sdl_kmp_jni_to_string(env, err);
}

SDLJNI_FUNC(void) SDLJNI_NAME(clearError)(JNIEnv *, jclass) {
    SDL_ClearError();
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setError)(JNIEnv *env, jclass, jstring message) {
    const char *msg = message ? env->GetStringUTFChars(message, nullptr) : nullptr;
    bool ok = SDL_SetError("%s", msg != nullptr ? msg : "");
    if (message) env->ReleaseStringUTFChars(message, msg);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getVersion)(JNIEnv *, jclass) {
    return static_cast<jint>(SDL_GetVersion());
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getRevision)(JNIEnv *env, jclass) {
    return sdl_kmp_jni_to_string(env, SDL_GetRevision());
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(getTicks)(JNIEnv *, jclass) {
    return static_cast<jlong>(SDL_GetTicks());
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(performanceCounter)(JNIEnv *, jclass) {
    return static_cast<jlong>(SDL_GetPerformanceCounter());
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(performanceFrequency)(JNIEnv *, jclass) {
    return static_cast<jlong>(SDL_GetPerformanceFrequency());
}

SDLJNI_FUNC(void) SDLJNI_NAME(delay)(JNIEnv *, jclass, jint ms) {
    SDL_Delay(static_cast<Uint32>(ms));
}

// ===========================================================================
// Window
// ===========================================================================

SDLJNI_FUNC(jlong) SDLJNI_NAME(createWindow)(JNIEnv *env, jclass, jstring title,
                                             jint width, jint height, jlong flags) {
    const char *t = title ? env->GetStringUTFChars(title, nullptr) : nullptr;
    SDL_Window *window = SDL_CreateWindow(t, width, height, static_cast<Uint64>(flags));
    if (title) env->ReleaseStringUTFChars(title, t);
    return reinterpret_cast<jlong>(window);
}

SDLJNI_FUNC(void) SDLJNI_NAME(destroyWindow)(JNIEnv *, jclass, jlong window) {
    SDL_DestroyWindow(reinterpret_cast<SDL_Window *>(window));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getWindowID)(JNIEnv *, jclass, jlong window) {
    return static_cast<jint>(SDL_GetWindowID(reinterpret_cast<SDL_Window *>(window)));
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(getWindowFromID)(JNIEnv *, jclass, jint windowId) {
    return reinterpret_cast<jlong>(SDL_GetWindowFromID(windowId));
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getWindowTitle)(JNIEnv *env, jclass, jlong window) {
    return sdl_kmp_jni_to_string(env, SDL_GetWindowTitle(reinterpret_cast<SDL_Window *>(window)));
}

SDLJNI_FUNC(void) SDLJNI_NAME(setWindowTitle)(JNIEnv *env, jclass, jlong window, jstring title) {
    const char *t = title ? env->GetStringUTFChars(title, nullptr) : "";
    SDL_SetWindowTitle(reinterpret_cast<SDL_Window *>(window), t);
    if (title) env->ReleaseStringUTFChars(title, t);
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getWindowSize)(JNIEnv *env, jclass, jlong window) {
    int w = 0, h = 0;
    SDL_GetWindowSize(reinterpret_cast<SDL_Window *>(window), &w, &h);
    return sdl_kmp_jni_new_int_array(env, {w, h});
}

SDLJNI_FUNC(void) SDLJNI_NAME(setWindowSize)(JNIEnv *, jclass, jlong window, jint w, jint h) {
    SDL_SetWindowSize(reinterpret_cast<SDL_Window *>(window), w, h);
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(getWindowFlags)(JNIEnv *, jclass, jlong window) {
    return static_cast<jlong>(SDL_GetWindowFlags(reinterpret_cast<SDL_Window *>(window)));
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getWindowPosition)(JNIEnv *env, jclass, jlong window) {
    int x = 0, y = 0;
    SDL_GetWindowPosition(reinterpret_cast<SDL_Window *>(window), &x, &y);
    return sdl_kmp_jni_new_int_array(env, {x, y});
}

SDLJNI_FUNC(void) SDLJNI_NAME(setWindowPosition)(JNIEnv *, jclass, jlong window, jint x, jint y) {
    SDL_SetWindowPosition(reinterpret_cast<SDL_Window *>(window), x, y);
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getWindowSizeInPixels)(JNIEnv *env, jclass, jlong window) {
    int w = 0, h = 0;
    SDL_GetWindowSizeInPixels(reinterpret_cast<SDL_Window *>(window), &w, &h);
    return sdl_kmp_jni_new_int_array(env, {w, h});
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getDisplayForWindow)(JNIEnv *, jclass, jlong window) {
    return static_cast<jint>(SDL_GetDisplayForWindow(reinterpret_cast<SDL_Window *>(window)));
}

SDLJNI_FUNC(jfloat) SDLJNI_NAME(getWindowOpacity)(JNIEnv *, jclass, jlong window) {
    return SDL_GetWindowOpacity(reinterpret_cast<SDL_Window *>(window));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setWindowOpacity)(JNIEnv *, jclass, jlong window, jfloat opacity) {
    return SDL_SetWindowOpacity(reinterpret_cast<SDL_Window *>(window), opacity) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setWindowFullscreen)(JNIEnv *, jclass, jlong window, jboolean fullscreen) {
    return SDL_SetWindowFullscreen(reinterpret_cast<SDL_Window *>(window), fullscreen == JNI_TRUE)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setWindowBordered)(JNIEnv *, jclass, jlong window, jboolean bordered) {
    return SDL_SetWindowBordered(reinterpret_cast<SDL_Window *>(window), bordered == JNI_TRUE)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setWindowResizable)(JNIEnv *, jclass, jlong window, jboolean resizable) {
    return SDL_SetWindowResizable(reinterpret_cast<SDL_Window *>(window), resizable == JNI_TRUE)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setWindowAlwaysOnTop)(JNIEnv *, jclass, jlong window, jboolean onTop) {
    return SDL_SetWindowAlwaysOnTop(reinterpret_cast<SDL_Window *>(window), onTop == JNI_TRUE)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(getWindowMouseGrab)(JNIEnv *, jclass, jlong window) {
    return SDL_GetWindowMouseGrab(reinterpret_cast<SDL_Window *>(window)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setWindowMouseGrab)(JNIEnv *, jclass, jlong window, jboolean grabbed) {
    return SDL_SetWindowMouseGrab(reinterpret_cast<SDL_Window *>(window), grabbed == JNI_TRUE)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(getWindowKeyboardGrab)(JNIEnv *, jclass, jlong window) {
    return SDL_GetWindowKeyboardGrab(reinterpret_cast<SDL_Window *>(window)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setWindowKeyboardGrab)(JNIEnv *, jclass, jlong window, jboolean grabbed) {
    return SDL_SetWindowKeyboardGrab(reinterpret_cast<SDL_Window *>(window), grabbed == JNI_TRUE)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(getWindowRelativeMouseMode)(JNIEnv *, jclass, jlong window) {
    return SDL_GetWindowRelativeMouseMode(reinterpret_cast<SDL_Window *>(window)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setWindowRelativeMouseMode)(JNIEnv *, jclass, jlong window,
                                                              jboolean enabled) {
    return SDL_SetWindowRelativeMouseMode(reinterpret_cast<SDL_Window *>(window),
                                          enabled == JNI_TRUE)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getWindowMinimumSize)(JNIEnv *env, jclass, jlong window) {
    int w = 0, h = 0;
    SDL_GetWindowMinimumSize(reinterpret_cast<SDL_Window *>(window), &w, &h);
    return sdl_kmp_jni_new_int_array(env, {w, h});
}

SDLJNI_FUNC(void) SDLJNI_NAME(setWindowMinimumSize)(JNIEnv *, jclass, jlong window, jint w, jint h) {
    SDL_SetWindowMinimumSize(reinterpret_cast<SDL_Window *>(window), w, h);
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getWindowMaximumSize)(JNIEnv *env, jclass, jlong window) {
    int w = 0, h = 0;
    SDL_GetWindowMaximumSize(reinterpret_cast<SDL_Window *>(window), &w, &h);
    return sdl_kmp_jni_new_int_array(env, {w, h});
}

SDLJNI_FUNC(void) SDLJNI_NAME(setWindowMaximumSize)(JNIEnv *, jclass, jlong window, jint w, jint h) {
    SDL_SetWindowMaximumSize(reinterpret_cast<SDL_Window *>(window), w, h);
}

SDLJNI_FUNC(void) SDLJNI_NAME(maximizeWindow)(JNIEnv *, jclass, jlong window) {
    SDL_MaximizeWindow(reinterpret_cast<SDL_Window *>(window));
}

SDLJNI_FUNC(void) SDLJNI_NAME(minimizeWindow)(JNIEnv *, jclass, jlong window) {
    SDL_MinimizeWindow(reinterpret_cast<SDL_Window *>(window));
}

SDLJNI_FUNC(void) SDLJNI_NAME(restoreWindow)(JNIEnv *, jclass, jlong window) {
    SDL_RestoreWindow(reinterpret_cast<SDL_Window *>(window));
}

SDLJNI_FUNC(void) SDLJNI_NAME(flashWindow)(JNIEnv *, jclass, jlong window) {
    SDL_FlashWindow(reinterpret_cast<SDL_Window *>(window), SDL_FLASH_CANCEL);
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(getWindowSurface)(JNIEnv *, jclass, jlong window) {
    return reinterpret_cast<jlong>(SDL_GetWindowSurface(reinterpret_cast<SDL_Window *>(window)));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setWindowIcon)(JNIEnv *, jclass, jlong window, jlong icon) {
    return SDL_SetWindowIcon(reinterpret_cast<SDL_Window *>(window),
                             reinterpret_cast<SDL_Surface *>(icon))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jfloatArray) SDLJNI_NAME(getWindowAspectRatio)(JNIEnv *env, jclass, jlong window) {
    float minAspect = 0.0f, maxAspect = 0.0f;
    if (SDL_GetWindowAspectRatio(reinterpret_cast<SDL_Window *>(window), &minAspect, &maxAspect)) {
        return sdl_kmp_jni_new_float_array(env, {minAspect, maxAspect});
    }
    return nullptr;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setWindowAspectRatio)(JNIEnv *, jclass, jlong window, jfloat minAspect,
                                                        jfloat maxAspect) {
    return SDL_SetWindowAspectRatio(reinterpret_cast<SDL_Window *>(window), minAspect, maxAspect)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(showWindow)(JNIEnv *, jclass, jlong window) {
    SDL_ShowWindow(reinterpret_cast<SDL_Window *>(window));
}

SDLJNI_FUNC(void) SDLJNI_NAME(hideWindow)(JNIEnv *, jclass, jlong window) {
    SDL_HideWindow(reinterpret_cast<SDL_Window *>(window));
}

SDLJNI_FUNC(void) SDLJNI_NAME(raiseWindow)(JNIEnv *, jclass, jlong window) {
    SDL_RaiseWindow(reinterpret_cast<SDL_Window *>(window));
}

// ===========================================================================
// Renderer
// ===========================================================================

SDLJNI_FUNC(jlong) SDLJNI_NAME(createRenderer)(JNIEnv *env, jclass, jlong window, jstring name) {
    const char *n = nullptr;
    if (name != nullptr) {
        n = env->GetStringUTFChars(name, nullptr);
    }
    SDL_Renderer *renderer = SDL_CreateRenderer(reinterpret_cast<SDL_Window *>(window), n);
    if (name != nullptr) env->ReleaseStringUTFChars(name, n);
    return reinterpret_cast<jlong>(renderer);
}

SDLJNI_FUNC(jlongArray) SDLJNI_NAME(createWindowAndRenderer)(JNIEnv *env, jclass, jstring title,
                                                             jint width, jint height, jlong flags) {
    const char *t = title ? env->GetStringUTFChars(title, nullptr) : nullptr;
    SDL_Window *window = nullptr;
    SDL_Renderer *renderer = nullptr;
    bool ok = SDL_CreateWindowAndRenderer(t, width, height, static_cast<Uint64>(flags),
                                          &window, &renderer);
    if (title) env->ReleaseStringUTFChars(title, t);
    if (!ok) return nullptr;
    return sdl_kmp_jni_new_long_array(
        env, {reinterpret_cast<jlong>(window), reinterpret_cast<jlong>(renderer)});
}

SDLJNI_FUNC(void) SDLJNI_NAME(destroyRenderer)(JNIEnv *, jclass, jlong renderer) {
    SDL_DestroyRenderer(reinterpret_cast<SDL_Renderer *>(renderer));
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getRendererName)(JNIEnv *env, jclass, jlong renderer) {
    return sdl_kmp_jni_to_string(env, SDL_GetRendererName(reinterpret_cast<SDL_Renderer *>(renderer)));
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getRenderDrawColor)(JNIEnv *env, jclass, jlong renderer) {
    Uint8 r = 0, g = 0, b = 0, a = 0;
    SDL_GetRenderDrawColor(reinterpret_cast<SDL_Renderer *>(renderer), &r, &g, &b, &a);
    return sdl_kmp_jni_new_int_array(env, {r, g, b, a});
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setRenderDrawColor)(JNIEnv *, jclass, jlong renderer, jint r, jint g,
                                                      jint b, jint a) {
    return SDL_SetRenderDrawColor(reinterpret_cast<SDL_Renderer *>(renderer),
                                  static_cast<Uint8>(r), static_cast<Uint8>(g),
                                  static_cast<Uint8>(b), static_cast<Uint8>(a))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getRenderOutputSize)(JNIEnv *env, jclass, jlong renderer) {
    int w = 0, h = 0;
    SDL_GetRenderOutputSize(reinterpret_cast<SDL_Renderer *>(renderer), &w, &h);
    return sdl_kmp_jni_new_int_array(env, {w, h});
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getCurrentRenderOutputSize)(JNIEnv *env, jclass, jlong renderer) {
    int w = 0, h = 0;
    SDL_GetCurrentRenderOutputSize(reinterpret_cast<SDL_Renderer *>(renderer), &w, &h);
    return sdl_kmp_jni_new_int_array(env, {w, h});
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getRenderViewport)(JNIEnv *env, jclass, jlong renderer) {
    SDL_Rect rect{};
    SDL_GetRenderViewport(reinterpret_cast<SDL_Renderer *>(renderer), &rect);
    return sdl_kmp_jni_new_int_array(env, {rect.x, rect.y, rect.w, rect.h});
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setRenderViewport)(JNIEnv *env, jclass, jlong renderer,
                                                     jintArray rectArr) {
    std::vector<jint> v;
    if (!sdl_kmp_jni_read_int_array(env, rectArr, v)) {
        SDL_SetRenderViewport(reinterpret_cast<SDL_Renderer *>(renderer), nullptr);
    } else {
        SDL_Rect rect{v[0], v[1], v[2], v[3]};
        SDL_SetRenderViewport(reinterpret_cast<SDL_Renderer *>(renderer), &rect);
    }
    return JNI_TRUE;
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getRenderClipRect)(JNIEnv *env, jclass, jlong renderer) {
    SDL_Rect rect{};
    SDL_GetRenderClipRect(reinterpret_cast<SDL_Renderer *>(renderer), &rect);
    return sdl_kmp_jni_new_int_array(env, {rect.x, rect.y, rect.w, rect.h});
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setRenderClipRect)(JNIEnv *env, jclass, jlong renderer,
                                                     jintArray rectArr) {
    std::vector<jint> v;
    if (!sdl_kmp_jni_read_int_array(env, rectArr, v)) {
        SDL_SetRenderClipRect(reinterpret_cast<SDL_Renderer *>(renderer), nullptr);
    } else {
        SDL_Rect rect{v[0], v[1], v[2], v[3]};
        SDL_SetRenderClipRect(reinterpret_cast<SDL_Renderer *>(renderer), &rect);
    }
    return JNI_TRUE;
}

SDLJNI_FUNC(jfloatArray) SDLJNI_NAME(getRenderScale)(JNIEnv *env, jclass, jlong renderer) {
    float x = 1.0f, y = 1.0f;
    SDL_GetRenderScale(reinterpret_cast<SDL_Renderer *>(renderer), &x, &y);
    return sdl_kmp_jni_new_float_array(env, {x, y});
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setRenderScale)(JNIEnv *, jclass, jlong renderer, jfloat x, jfloat y) {
    return SDL_SetRenderScale(reinterpret_cast<SDL_Renderer *>(renderer), x, y) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getRenderDrawBlendMode)(JNIEnv *, jclass, jlong renderer) {
    SDL_BlendMode mode = SDL_BLENDMODE_NONE;
    SDL_GetRenderDrawBlendMode(reinterpret_cast<SDL_Renderer *>(renderer), &mode);
    return static_cast<jint>(mode);
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setRenderDrawBlendMode)(JNIEnv *, jclass, jlong renderer, jint mode) {
    return SDL_SetRenderDrawBlendMode(reinterpret_cast<SDL_Renderer *>(renderer),
                                      static_cast<SDL_BlendMode>(mode))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getRenderVSync)(JNIEnv *, jclass, jlong renderer) {
    int vsync = 0;
    if (SDL_GetRenderVSync(reinterpret_cast<SDL_Renderer *>(renderer), &vsync)) {
        return vsync;
    }
    return 0;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setRenderVSync)(JNIEnv *, jclass, jlong renderer, jint vsync) {
    return SDL_SetRenderVSync(reinterpret_cast<SDL_Renderer *>(renderer), vsync) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(getRenderTarget)(JNIEnv *, jclass, jlong renderer) {
    return reinterpret_cast<jlong>(SDL_GetRenderTarget(reinterpret_cast<SDL_Renderer *>(renderer)));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setRenderTarget)(JNIEnv *, jclass, jlong renderer, jlong texture) {
    return SDL_SetRenderTarget(reinterpret_cast<SDL_Renderer *>(renderer),
                               reinterpret_cast<SDL_Texture *>(texture))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(renderClear)(JNIEnv *, jclass, jlong renderer) {
    return SDL_RenderClear(reinterpret_cast<SDL_Renderer *>(renderer)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(renderPresent)(JNIEnv *, jclass, jlong renderer) {
    SDL_RenderPresent(reinterpret_cast<SDL_Renderer *>(renderer));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(renderFillRect)(JNIEnv *, jclass, jlong renderer, jfloat x, jfloat y,
                                                  jfloat w, jfloat h) {
    SDL_FRect rect{x, y, w, h};
    return SDL_RenderFillRect(reinterpret_cast<SDL_Renderer *>(renderer), &rect) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(renderRect)(JNIEnv *, jclass, jlong renderer, jfloat x, jfloat y,
                                              jfloat w, jfloat h) {
    SDL_FRect rect{x, y, w, h};
    return SDL_RenderRect(reinterpret_cast<SDL_Renderer *>(renderer), &rect) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(renderLine)(JNIEnv *, jclass, jlong renderer, jfloat x1, jfloat y1,
                                              jfloat x2, jfloat y2) {
    return SDL_RenderLine(reinterpret_cast<SDL_Renderer *>(renderer), x1, y1, x2, y2) ? JNI_TRUE
                                                                                     : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(renderPoint)(JNIEnv *, jclass, jlong renderer, jfloat x, jfloat y) {
    return SDL_RenderPoint(reinterpret_cast<SDL_Renderer *>(renderer), x, y) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(renderPoints)(JNIEnv *env, jclass, jlong renderer,
                                                jfloatArray pointsArr) {
    jsize count = env->GetArrayLength(pointsArr) / 2;
    if (count <= 0) return JNI_TRUE;
    std::vector<jfloat> pts;
    pts.resize(static_cast<size_t>(env->GetArrayLength(pointsArr)));
    env->GetFloatArrayRegion(pointsArr, 0, static_cast<jsize>(pts.size()), pts.data());
    return SDL_RenderPoints(reinterpret_cast<SDL_Renderer *>(renderer),
                            reinterpret_cast<const SDL_FPoint *>(pts.data()), count)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(createTexture)(JNIEnv *, jclass, jlong renderer, jint format,
                                              jint access, jint width, jint height) {
    SDL_Texture *texture = SDL_CreateTexture(reinterpret_cast<SDL_Renderer *>(renderer),
                                             static_cast<SDL_PixelFormat>(format),
                                             static_cast<SDL_TextureAccess>(access), width, height);
    return reinterpret_cast<jlong>(texture);
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(createTextureFromSurface)(JNIEnv *, jclass, jlong renderer,
                                                         jlong surface) {
    SDL_Texture *texture = SDL_CreateTextureFromSurface(reinterpret_cast<SDL_Renderer *>(renderer),
                                                        reinterpret_cast<SDL_Surface *>(surface));
    return reinterpret_cast<jlong>(texture);
}

// src/dst are FloatArray? (null means "the whole texture / the whole target").
SDLJNI_FUNC(jboolean) SDLJNI_NAME(renderTexture)(JNIEnv *env, jclass, jlong renderer, jlong texture,
                                                 jfloatArray srcArr, jfloatArray dstArr) {
    std::vector<jfloat> src;
    std::vector<jfloat> dst;
    sdl_kmp_jni_read_float_array(env, srcArr, src);
    sdl_kmp_jni_read_float_array(env, dstArr, dst);
    const SDL_FRect *srcRect = src.empty() ? nullptr : reinterpret_cast<const SDL_FRect *>(src.data());
    const SDL_FRect *dstRect = dst.empty() ? nullptr : reinterpret_cast<const SDL_FRect *>(dst.data());
    return SDL_RenderTexture(reinterpret_cast<SDL_Renderer *>(renderer),
                             reinterpret_cast<SDL_Texture *>(texture), srcRect, dstRect)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(renderTextureRotated)(JNIEnv *env, jclass, jlong renderer,
                                                        jlong texture, jfloatArray srcArr,
                                                        jfloatArray dstArr, jdouble angle,
                                                        jfloatArray centerArr, jint flip) {
    std::vector<jfloat> src;
    std::vector<jfloat> dst;
    std::vector<jfloat> center;
    sdl_kmp_jni_read_float_array(env, srcArr, src);
    sdl_kmp_jni_read_float_array(env, dstArr, dst);
    sdl_kmp_jni_read_float_array(env, centerArr, center);
    const SDL_FRect *srcRect = src.empty() ? nullptr : reinterpret_cast<const SDL_FRect *>(src.data());
    const SDL_FRect *dstRect = dst.empty() ? nullptr : reinterpret_cast<const SDL_FRect *>(dst.data());
    const SDL_FPoint *centerPoint = center.empty() ? nullptr : reinterpret_cast<const SDL_FPoint *>(center.data());
    return SDL_RenderTextureRotated(reinterpret_cast<SDL_Renderer *>(renderer),
                                    reinterpret_cast<SDL_Texture *>(texture), srcRect, dstRect, angle,
                                    centerPoint, static_cast<SDL_FlipMode>(flip))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(renderTexture9Grid)(JNIEnv *, jclass, jlong renderer, jlong texture,
                                                      jfloat srcX, jfloat srcY, jfloat srcW,
                                                      jfloat srcH, jfloat left, jfloat right,
                                                      jfloat top, jfloat bottom, jfloat scale,
                                                      jfloat dstX, jfloat dstY, jfloat dstW,
                                                      jfloat dstH) {
    SDL_FRect src{srcX, srcY, srcW, srcH};
    SDL_FRect dst{dstX, dstY, dstW, dstH};
    return SDL_RenderTexture9Grid(reinterpret_cast<SDL_Renderer *>(renderer),
                                  reinterpret_cast<SDL_Texture *>(texture), &src, left, right, top,
                                  bottom, scale, &dst)
               ? JNI_TRUE
               : JNI_FALSE;
}

// positions/colors/texCoords are flat float arrays (2/4/2 floats per vertex);
// indices is null for non-indexed rendering.
SDLJNI_FUNC(jboolean) SDLJNI_NAME(renderGeometry)(JNIEnv *env, jclass, jlong renderer, jlong texture,
                                                  jfloatArray positions, jfloatArray colors,
                                                  jfloatArray texCoords, jintArray indices) {
    jsize vertexCount = env->GetArrayLength(positions) / 2;
    if (vertexCount <= 0) return JNI_TRUE;
    std::vector<SDL_Vertex> vertices(static_cast<size_t>(vertexCount));
    std::vector<jfloat> pos;
    std::vector<jfloat> col;
    std::vector<jfloat> uv;
    pos.resize(static_cast<size_t>(env->GetArrayLength(positions)));
    col.resize(static_cast<size_t>(env->GetArrayLength(colors)));
    uv.resize(static_cast<size_t>(env->GetArrayLength(texCoords)));
    env->GetFloatArrayRegion(positions, 0, static_cast<jsize>(pos.size()), pos.data());
    env->GetFloatArrayRegion(colors, 0, static_cast<jsize>(col.size()), col.data());
    env->GetFloatArrayRegion(texCoords, 0, static_cast<jsize>(uv.size()), uv.data());
    for (jsize i = 0; i < vertexCount; i++) {
        vertices[static_cast<size_t>(i)].position.x = pos[static_cast<size_t>(i * 2)];
        vertices[static_cast<size_t>(i)].position.y = pos[static_cast<size_t>(i * 2 + 1)];
        vertices[static_cast<size_t>(i)].color.r = col[static_cast<size_t>(i * 4)];
        vertices[static_cast<size_t>(i)].color.g = col[static_cast<size_t>(i * 4 + 1)];
        vertices[static_cast<size_t>(i)].color.b = col[static_cast<size_t>(i * 4 + 2)];
        vertices[static_cast<size_t>(i)].color.a = col[static_cast<size_t>(i * 4 + 3)];
        vertices[static_cast<size_t>(i)].tex_coord.x = uv[static_cast<size_t>(i * 2)];
        vertices[static_cast<size_t>(i)].tex_coord.y = uv[static_cast<size_t>(i * 2 + 1)];
    }
    if (indices == nullptr) {
        return SDL_RenderGeometry(reinterpret_cast<SDL_Renderer *>(renderer),
                                  reinterpret_cast<SDL_Texture *>(texture), vertices.data(),
                                  vertexCount, nullptr, 0)
                   ? JNI_TRUE
                   : JNI_FALSE;
    }
    jsize indexCount = env->GetArrayLength(indices);
    std::vector<jint> idx(static_cast<size_t>(indexCount));
    env->GetIntArrayRegion(indices, 0, indexCount, idx.data());
    return SDL_RenderGeometry(reinterpret_cast<SDL_Renderer *>(renderer),
                              reinterpret_cast<SDL_Texture *>(texture), vertices.data(), vertexCount,
                              idx.data(), indexCount)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(renderReadPixels)(JNIEnv *env, jclass, jlong renderer,
                                                 jintArray rectArr) {
    std::vector<jint> v;
    if (!sdl_kmp_jni_read_int_array(env, rectArr, v)) {
        return reinterpret_cast<jlong>(SDL_RenderReadPixels(
            reinterpret_cast<SDL_Renderer *>(renderer), nullptr));
    }
    SDL_Rect rect{v[0], v[1], v[2], v[3]};
    return reinterpret_cast<jlong>(
        SDL_RenderReadPixels(reinterpret_cast<SDL_Renderer *>(renderer), &rect));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setRenderLogicalPresentation)(JNIEnv *, jclass, jlong renderer,
                                                                jint width, jint height, jint mode) {
    return SDL_SetRenderLogicalPresentation(reinterpret_cast<SDL_Renderer *>(renderer), width, height,
                                            static_cast<SDL_RendererLogicalPresentation>(mode))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jfloatArray) SDLJNI_NAME(getRenderLogicalPresentationRect)(JNIEnv *env, jclass,
                                                                       jlong renderer) {
    SDL_FRect rect{};
    if (SDL_GetRenderLogicalPresentationRect(reinterpret_cast<SDL_Renderer *>(renderer), &rect)) {
        return sdl_kmp_jni_new_float_array(env, {rect.x, rect.y, rect.w, rect.h});
    }
    return nullptr;
}

// ===========================================================================
// Texture
// ===========================================================================

SDLJNI_FUNC(jfloatArray) SDLJNI_NAME(getTextureSize)(JNIEnv *env, jclass, jlong texture) {
    float w = 0.0f, h = 0.0f;
    SDL_GetTextureSize(reinterpret_cast<SDL_Texture *>(texture), &w, &h);
    return sdl_kmp_jni_new_float_array(env, {w, h});
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setTextureColorMod)(JNIEnv *, jclass, jlong texture, jint r,
                                                      jint g, jint b) {
    return SDL_SetTextureColorMod(reinterpret_cast<SDL_Texture *>(texture),
                                  static_cast<Uint8>(r), static_cast<Uint8>(g),
                                  static_cast<Uint8>(b))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setTextureAlphaMod)(JNIEnv *, jclass, jlong texture, jint a) {
    return SDL_SetTextureAlphaMod(reinterpret_cast<SDL_Texture *>(texture), static_cast<Uint8>(a))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setTextureBlendMode)(JNIEnv *, jclass, jlong texture, jint mode) {
    return SDL_SetTextureBlendMode(reinterpret_cast<SDL_Texture *>(texture),
                                   static_cast<SDL_BlendMode>(mode))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setTextureScaleMode)(JNIEnv *, jclass, jlong texture, jint mode) {
    return SDL_SetTextureScaleMode(reinterpret_cast<SDL_Texture *>(texture),
                                   static_cast<SDL_ScaleMode>(mode))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getTextureColorMod)(JNIEnv *env, jclass, jlong texture) {
    Uint8 r = 0, g = 0, b = 0;
    if (!SDL_GetTextureColorMod(reinterpret_cast<SDL_Texture *>(texture), &r, &g, &b)) {
        return nullptr;
    }
    return sdl_kmp_jni_new_int_array(env, {static_cast<jint>(r), static_cast<jint>(g), static_cast<jint>(b)});
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getTextureAlphaMod)(JNIEnv *, jclass, jlong texture) {
    Uint8 a = 0;
    if (!SDL_GetTextureAlphaMod(reinterpret_cast<SDL_Texture *>(texture), &a)) {
        return -1;
    }
    return static_cast<jint>(a);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getTextureBlendMode)(JNIEnv *, jclass, jlong texture) {
    SDL_BlendMode mode;
    if (!SDL_GetTextureBlendMode(reinterpret_cast<SDL_Texture *>(texture), &mode)) {
        return -1;
    }
    return static_cast<jint>(mode);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getTextureScaleMode)(JNIEnv *, jclass, jlong texture) {
    SDL_ScaleMode mode;
    if (!SDL_GetTextureScaleMode(reinterpret_cast<SDL_Texture *>(texture), &mode)) {
        return -1;
    }
    return static_cast<jint>(mode);
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(updateTexture)(JNIEnv *env, jclass, jlong texture,
                                                 jintArray rectArr, jbyteArray pixels, jint pitch) {
    std::vector<jint> v;
    std::vector<jbyte> data;
    jsize len = env->GetArrayLength(pixels);
    data.resize(static_cast<size_t>(len));
    env->GetByteArrayRegion(pixels, 0, len, data.data());
    SDL_Rect rect{};
    SDL_Rect *rectPtr = nullptr;
    if (sdl_kmp_jni_read_int_array(env, rectArr, v)) {
        rect = SDL_Rect{v[0], v[1], v[2], v[3]};
        rectPtr = &rect;
    }
    return SDL_UpdateTexture(reinterpret_cast<SDL_Texture *>(texture), rectPtr, data.data(), pitch)
               ? JNI_TRUE
               : JNI_FALSE;
}

// Copies the locked pixel data into outPixels and the row pitch into outPitch.
SDLJNI_FUNC(jboolean) SDLJNI_NAME(lockTexture)(JNIEnv *env, jclass, jlong texture,
                                               jintArray rectArr, jbyteArray outPixels,
                                               jintArray outPitch) {
    std::vector<jint> v;
    SDL_Rect rect{};
    SDL_Rect *rectPtr = nullptr;
    if (sdl_kmp_jni_read_int_array(env, rectArr, v)) {
        rect = SDL_Rect{v[0], v[1], v[2], v[3]};
        rectPtr = &rect;
    }
    void *pixels = nullptr;
    int pitch = 0;
    if (!SDL_LockTexture(reinterpret_cast<SDL_Texture *>(texture), rectPtr, &pixels, &pitch)) {
        return JNI_FALSE;
    }
    jsize byteCount = env->GetArrayLength(outPixels);
    if (pixels != nullptr && byteCount > 0) {
        env->SetByteArrayRegion(outPixels, 0, byteCount, static_cast<const jbyte *>(pixels));
    }
    jint pitchValue = pitch;
    env->SetIntArrayRegion(outPitch, 0, 1, &pitchValue);
    SDL_UnlockTexture(reinterpret_cast<SDL_Texture *>(texture));
    return JNI_TRUE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(unlockTexture)(JNIEnv *, jclass, jlong texture) {
    SDL_UnlockTexture(reinterpret_cast<SDL_Texture *>(texture));
}

SDLJNI_FUNC(void) SDLJNI_NAME(destroyTexture)(JNIEnv *, jclass, jlong texture) {
    SDL_DestroyTexture(reinterpret_cast<SDL_Texture *>(texture));
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getTextureProperties)(JNIEnv *env, jclass, jlong texture) {
    SDL_PropertiesID props = SDL_GetTextureProperties(reinterpret_cast<SDL_Texture *>(texture));
    if (props == 0) {
        return nullptr;
    }
    jint format = static_cast<jint>(SDL_GetNumberProperty(props, SDL_PROP_TEXTURE_FORMAT_NUMBER, 0));
    jint access = static_cast<jint>(SDL_GetNumberProperty(props, SDL_PROP_TEXTURE_ACCESS_NUMBER, 0));
    return sdl_kmp_jni_new_int_array(env, {format, access});
}

// ===========================================================================
// Surface
// ===========================================================================

SDLJNI_FUNC(jlong) SDLJNI_NAME(createSurface)(JNIEnv *, jclass, jint width, jint height, jint format) {
    return reinterpret_cast<jlong>(SDL_CreateSurface(width, height, static_cast<SDL_PixelFormat>(format)));
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(loadBMP)(JNIEnv *env, jclass, jstring path) {
    const char *p = path ? env->GetStringUTFChars(path, nullptr) : nullptr;
    SDL_Surface *surface = SDL_LoadBMP(p);
    if (path) env->ReleaseStringUTFChars(path, p);
    return reinterpret_cast<jlong>(surface);
}

SDLJNI_FUNC(void) SDLJNI_NAME(destroySurface)(JNIEnv *, jclass, jlong surface) {
    SDL_DestroySurface(reinterpret_cast<SDL_Surface *>(surface));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getSurfaceColorspace)(JNIEnv *, jclass, jlong surface) {
    return static_cast<jint>(SDL_GetSurfaceColorspace(reinterpret_cast<SDL_Surface *>(surface)));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(lockSurface)(JNIEnv *, jclass, jlong surface) {
    return SDL_LockSurface(reinterpret_cast<SDL_Surface *>(surface)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(unlockSurface)(JNIEnv *, jclass, jlong surface) {
    SDL_UnlockSurface(reinterpret_cast<SDL_Surface *>(surface));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(surfaceFillRect)(JNIEnv *env, jclass, jlong surface,
                                                   jintArray rectArr, jint r, jint g, jint b,
                                                   jint a) {
    std::vector<jint> v;
    SDL_Rect rect{};
    SDL_Rect *rectPtr = nullptr;
    if (sdl_kmp_jni_read_int_array(env, rectArr, v)) {
        rect = SDL_Rect{v[0], v[1], v[2], v[3]};
        rectPtr = &rect;
    }
    Uint32 color = SDL_MapRGBA(SDL_GetPixelFormatDetails(
                                   reinterpret_cast<SDL_Surface *>(surface)->format),
                               nullptr, static_cast<Uint8>(r), static_cast<Uint8>(g),
                               static_cast<Uint8>(b), static_cast<Uint8>(a));
    return SDL_FillSurfaceRect(reinterpret_cast<SDL_Surface *>(surface), rectPtr, color) ? JNI_TRUE
                                                                                          : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(surfaceFillRects)(JNIEnv *env, jclass, jlong surface,
                                                    jintArray rectsArr, jint r, jint g, jint b,
                                                    jint a) {
    jsize count = env->GetArrayLength(rectsArr) / 4;
    if (count <= 0) return JNI_TRUE;
    std::vector<jint> rects;
    rects.resize(static_cast<size_t>(env->GetArrayLength(rectsArr)));
    env->GetIntArrayRegion(rectsArr, 0, static_cast<jsize>(rects.size()), rects.data());
    Uint32 color = SDL_MapRGBA(SDL_GetPixelFormatDetails(
                                   reinterpret_cast<SDL_Surface *>(surface)->format),
                               nullptr, static_cast<Uint8>(r), static_cast<Uint8>(g),
                               static_cast<Uint8>(b), static_cast<Uint8>(a));
    return SDL_FillSurfaceRects(reinterpret_cast<SDL_Surface *>(surface),
                                reinterpret_cast<const SDL_Rect *>(rects.data()), count, color)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(surfaceBlit)(JNIEnv *env, jclass, jlong src, jintArray srcRectArr,
                                               jlong dst, jintArray dstRectArr) {
    std::vector<jint> srcV;
    std::vector<jint> dstV;
    SDL_Rect srcRect{};
    SDL_Rect dstRect{};
    SDL_Rect *srcPtr = nullptr;
    SDL_Rect *dstPtr = nullptr;
    if (sdl_kmp_jni_read_int_array(env, srcRectArr, srcV)) {
        srcRect = SDL_Rect{srcV[0], srcV[1], srcV[2], srcV[3]};
        srcPtr = &srcRect;
    }
    if (sdl_kmp_jni_read_int_array(env, dstRectArr, dstV)) {
        dstRect = SDL_Rect{dstV[0], dstV[1], dstV[2], dstV[3]};
        dstPtr = &dstRect;
    }
    return SDL_BlitSurface(reinterpret_cast<SDL_Surface *>(src), srcPtr,
                           reinterpret_cast<SDL_Surface *>(dst), dstPtr)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(surfaceBlitScaled)(JNIEnv *env, jclass, jlong src,
                                                     jintArray srcRectArr, jlong dst,
                                                     jintArray dstRectArr, jint scaleMode) {
    std::vector<jint> srcV;
    std::vector<jint> dstV;
    SDL_Rect srcRect{};
    SDL_Rect dstRect{};
    SDL_Rect *srcPtr = nullptr;
    SDL_Rect *dstPtr = nullptr;
    if (sdl_kmp_jni_read_int_array(env, srcRectArr, srcV)) {
        srcRect = SDL_Rect{srcV[0], srcV[1], srcV[2], srcV[3]};
        srcPtr = &srcRect;
    }
    if (sdl_kmp_jni_read_int_array(env, dstRectArr, dstV)) {
        dstRect = SDL_Rect{dstV[0], dstV[1], dstV[2], dstV[3]};
        dstPtr = &dstRect;
    }
    return SDL_BlitSurfaceScaled(reinterpret_cast<SDL_Surface *>(src), srcPtr,
                                 reinterpret_cast<SDL_Surface *>(dst), dstPtr,
                                 static_cast<SDL_ScaleMode>(scaleMode))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(surfaceSaveBMP)(JNIEnv *env, jclass, jlong surface, jstring path) {
    const char *p = path ? env->GetStringUTFChars(path, nullptr) : nullptr;
    bool ok = SDL_SaveBMP(reinterpret_cast<SDL_Surface *>(surface), p);
    if (path) env->ReleaseStringUTFChars(path, p);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(convertSurface)(JNIEnv *, jclass, jlong surface, jint format) {
    return reinterpret_cast<jlong>(SDL_ConvertSurface(reinterpret_cast<SDL_Surface *>(surface),
                                                      static_cast<SDL_PixelFormat>(format)));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(surfaceWidth)(JNIEnv *, jclass, jlong surface) {
    return reinterpret_cast<SDL_Surface *>(surface)->w;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(surfaceHeight)(JNIEnv *, jclass, jlong surface) {
    return reinterpret_cast<SDL_Surface *>(surface)->h;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(surfaceFormat)(JNIEnv *, jclass, jlong surface) {
    return static_cast<jint>(reinterpret_cast<SDL_Surface *>(surface)->format);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(surfacePitch)(JNIEnv *, jclass, jlong surface) {
    return reinterpret_cast<SDL_Surface *>(surface)->pitch;
}

SDLJNI_FUNC(jbyteArray) SDLJNI_NAME(surfacePixels)(JNIEnv *env, jclass, jlong surface) {
    SDL_Surface *s = reinterpret_cast<SDL_Surface *>(surface);
    if (s == nullptr || s->pixels == nullptr) return nullptr;
    return sdl_kmp_jni_new_byte_array(env, s->pixels, s->pitch * s->h);
}

// ===========================================================================
// Events
// ===========================================================================

SDLJNI_FUNC(jlong) SDLJNI_NAME(eventAlloc)(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(SDL_calloc(1, sizeof(SDL_Event)));
}

SDLJNI_FUNC(void) SDLJNI_NAME(eventFree)(JNIEnv *, jclass, jlong event) {
    SDL_free(reinterpret_cast<void *>(event));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(pollEvent)(JNIEnv *, jclass, jlong event) {
    return SDL_PollEvent(reinterpret_cast<SDL_Event *>(event)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(waitEvent)(JNIEnv *, jclass, jlong event) {
    return SDL_WaitEvent(reinterpret_cast<SDL_Event *>(event)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(pumpEvents)(JNIEnv *, jclass) {
    SDL_PumpEvents();
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(pushEvent)(JNIEnv *, jclass, jlong event) {
    return SDL_PushEvent(reinterpret_cast<SDL_Event *>(event)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventType)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->type);
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(eventTimestamp)(JNIEnv *, jclass, jlong event) {
    return static_cast<jlong>(reinterpret_cast<SDL_Event *>(event)->common.timestamp);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventWindowWindowID)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->window.windowID);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventWindowData1)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->window.data1);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventWindowData2)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->window.data2);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventKeyWindowID)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->key.windowID);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventKeyState)(JNIEnv *, jclass, jlong event) {
    return reinterpret_cast<SDL_Event *>(event)->key.down ? 1 : 0;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(eventKeyRepeat)(JNIEnv *, jclass, jlong event) {
    return reinterpret_cast<SDL_Event *>(event)->key.repeat ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventKeyScancode)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->key.scancode);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventKeyKeycode)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->key.key);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventKeyMod)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->key.mod);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventTextWindowID)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->text.windowID);
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(eventTextText)(JNIEnv *env, jclass, jlong event) {
    return sdl_kmp_jni_to_string(env, reinterpret_cast<SDL_Event *>(event)->text.text);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventMotionWindowID)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->motion.windowID);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventMotionState)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->motion.state);
}

SDLJNI_FUNC(jfloat) SDLJNI_NAME(eventMotionX)(JNIEnv *, jclass, jlong event) {
    return reinterpret_cast<SDL_Event *>(event)->motion.x;
}

SDLJNI_FUNC(jfloat) SDLJNI_NAME(eventMotionY)(JNIEnv *, jclass, jlong event) {
    return reinterpret_cast<SDL_Event *>(event)->motion.y;
}

SDLJNI_FUNC(jfloat) SDLJNI_NAME(eventMotionXrel)(JNIEnv *, jclass, jlong event) {
    return reinterpret_cast<SDL_Event *>(event)->motion.xrel;
}

SDLJNI_FUNC(jfloat) SDLJNI_NAME(eventMotionYrel)(JNIEnv *, jclass, jlong event) {
    return reinterpret_cast<SDL_Event *>(event)->motion.yrel;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventButtonWindowID)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->button.windowID);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventButtonButton)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->button.button);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventButtonState)(JNIEnv *, jclass, jlong event) {
    return reinterpret_cast<SDL_Event *>(event)->button.down ? 1 : 0;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventButtonClicks)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->button.clicks);
}

SDLJNI_FUNC(jfloat) SDLJNI_NAME(eventButtonX)(JNIEnv *, jclass, jlong event) {
    return reinterpret_cast<SDL_Event *>(event)->button.x;
}

SDLJNI_FUNC(jfloat) SDLJNI_NAME(eventButtonY)(JNIEnv *, jclass, jlong event) {
    return reinterpret_cast<SDL_Event *>(event)->button.y;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventWheelWindowID)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->wheel.windowID);
}

SDLJNI_FUNC(jfloat) SDLJNI_NAME(eventWheelX)(JNIEnv *, jclass, jlong event) {
    return reinterpret_cast<SDL_Event *>(event)->wheel.x;
}

SDLJNI_FUNC(jfloat) SDLJNI_NAME(eventWheelY)(JNIEnv *, jclass, jlong event) {
    return reinterpret_cast<SDL_Event *>(event)->wheel.y;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(eventWheelDirection)(JNIEnv *, jclass, jlong event) {
    return static_cast<jint>(reinterpret_cast<SDL_Event *>(event)->wheel.direction);
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(addEventWatch)(JNIEnv *, jclass, jlong id) {
    SDL_EventFilter filter = EventWatchCallback;
    {
        std::lock_guard<std::mutex> lock(g_eventWatchMutex);
        g_eventWatchCallbacks[id] = filter;
    }
    if (SDL_AddEventWatch(filter, reinterpret_cast<void *>(id))) {
        return JNI_TRUE;
    }
    std::lock_guard<std::mutex> lock(g_eventWatchMutex);
    g_eventWatchCallbacks.erase(id);
    return JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(removeEventWatch)(JNIEnv *, jclass, jlong id) {
    std::lock_guard<std::mutex> lock(g_eventWatchMutex);
    auto it = g_eventWatchCallbacks.find(id);
    if (it != g_eventWatchCallbacks.end()) {
        SDL_RemoveEventWatch(it->second, reinterpret_cast<void *>(id));
        g_eventWatchCallbacks.erase(it);
    }
}

SDLJNI_FUNC(void) SDLJNI_NAME(setEventEnabled)(JNIEnv *, jclass, jint type, jboolean enabled) {
    SDL_SetEventEnabled(static_cast<Uint32>(type), enabled == JNI_TRUE);
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(eventEnabled)(JNIEnv *, jclass, jint type) {
    return SDL_EventEnabled(static_cast<Uint32>(type)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(flushEvents)(JNIEnv *, jclass, jint minType, jint maxType) {
    SDL_FlushEvents(static_cast<Uint32>(minType), static_cast<Uint32>(maxType));
}

// ===========================================================================
// Display
// ===========================================================================

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getDisplays)(JNIEnv *env, jclass) {
    int count = 0;
    SDL_DisplayID *ids = SDL_GetDisplays(&count);
    if (ids == nullptr) return nullptr;
    std::vector<jint> out;
    out.reserve(static_cast<size_t>(count));
    for (int i = 0; i < count; i++) {
        out.push_back(static_cast<jint>(ids[i]));
    }
    SDL_free(ids);
    return sdl_kmp_jni_new_int_array(env, out);
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getPrimaryDisplay)(JNIEnv *, jclass) {
    return static_cast<jint>(SDL_GetPrimaryDisplay());
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getDisplayName)(JNIEnv *env, jclass, jint displayId) {
    return sdl_kmp_jni_to_string(env, SDL_GetDisplayName(displayId));
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getDisplayBounds)(JNIEnv *env, jclass, jint displayId) {
    SDL_Rect rect{};
    SDL_GetDisplayBounds(displayId, &rect);
    return sdl_kmp_jni_new_int_array(env, {rect.x, rect.y, rect.w, rect.h});
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getDisplayUsableBounds)(JNIEnv *env, jclass, jint displayId) {
    SDL_Rect rect{};
    SDL_GetDisplayUsableBounds(displayId, &rect);
    return sdl_kmp_jni_new_int_array(env, {rect.x, rect.y, rect.w, rect.h});
}

SDLJNI_FUNC(jfloatArray) SDLJNI_NAME(getCurrentDisplayMode)(JNIEnv *env, jclass, jint displayId) {
    const SDL_DisplayMode *mode = SDL_GetCurrentDisplayMode(displayId);
    if (mode == nullptr) return nullptr;
    return sdl_kmp_jni_new_float_array(env, {static_cast<jfloat>(mode->format),
                                             static_cast<jfloat>(mode->w),
                                             static_cast<jfloat>(mode->h),
                                             static_cast<jfloat>(mode->refresh_rate),
                                             mode->pixel_density});
}

SDLJNI_FUNC(jfloatArray) SDLJNI_NAME(getDesktopDisplayMode)(JNIEnv *env, jclass, jint displayId) {
    const SDL_DisplayMode *mode = SDL_GetDesktopDisplayMode(displayId);
    if (mode == nullptr) return nullptr;
    return sdl_kmp_jni_new_float_array(env, {static_cast<jfloat>(mode->format),
                                             static_cast<jfloat>(mode->w),
                                             static_cast<jfloat>(mode->h),
                                             static_cast<jfloat>(mode->refresh_rate),
                                             mode->pixel_density});
}

// ===========================================================================
// Pixels / clipboard / hints
// ===========================================================================

SDLJNI_FUNC(jstring) SDLJNI_NAME(getPixelFormatName)(JNIEnv *env, jclass, jint format) {
    return sdl_kmp_jni_to_string(env, SDL_GetPixelFormatName(static_cast<SDL_PixelFormat>(format)));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(mapRGB)(JNIEnv *, jclass, jint format, jint r, jint g, jint b) {
    const SDL_PixelFormatDetails *details =
        SDL_GetPixelFormatDetails(static_cast<SDL_PixelFormat>(format));
    if (details == nullptr) return 0;
    return static_cast<jint>(SDL_MapRGB(details, nullptr, static_cast<Uint8>(r),
                                        static_cast<Uint8>(g), static_cast<Uint8>(b)));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(mapRGBA)(JNIEnv *, jclass, jint format, jint r, jint g, jint b,
                                       jint a) {
    const SDL_PixelFormatDetails *details =
        SDL_GetPixelFormatDetails(static_cast<SDL_PixelFormat>(format));
    if (details == nullptr) return 0;
    return static_cast<jint>(SDL_MapRGBA(details, nullptr, static_cast<Uint8>(r),
                                         static_cast<Uint8>(g), static_cast<Uint8>(b),
                                         static_cast<Uint8>(a)));
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(getRGBA)(JNIEnv *env, jclass, jint format, jint pixel) {
    const SDL_PixelFormatDetails *details =
        SDL_GetPixelFormatDetails(static_cast<SDL_PixelFormat>(format));
    if (details == nullptr) return nullptr;
    Uint8 r = 0, g = 0, b = 0, a = 0;
    SDL_GetRGBA(static_cast<Uint32>(pixel), details, nullptr, &r, &g, &b, &a);
    return sdl_kmp_jni_new_int_array(env, {r, g, b, a});
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setHint)(JNIEnv *env, jclass, jstring name, jstring value) {
    const char *n = name ? env->GetStringUTFChars(name, nullptr) : nullptr;
    const char *v = value ? env->GetStringUTFChars(value, nullptr) : nullptr;
    bool ok = SDL_SetHint(n, v);
    if (name) env->ReleaseStringUTFChars(name, n);
    if (value) env->ReleaseStringUTFChars(value, v);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getHint)(JNIEnv *env, jclass, jstring name) {
    const char *n = name ? env->GetStringUTFChars(name, nullptr) : nullptr;
    const char *value = SDL_GetHint(n);
    jstring result = sdl_kmp_jni_to_string(env, value);
    if (name) env->ReleaseStringUTFChars(name, n);
    return result;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(getHintBoolean)(JNIEnv *env, jclass, jstring name,
                                                  jboolean defaultValue) {
    const char *n = name ? env->GetStringUTFChars(name, nullptr) : nullptr;
    bool value = SDL_GetHintBoolean(n, defaultValue == JNI_TRUE);
    if (name) env->ReleaseStringUTFChars(name, n);
    return value ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getClipboardText)(JNIEnv *env, jclass) {
    char *text = SDL_GetClipboardText();
    if (text == nullptr) return nullptr;
    jstring result = env->NewStringUTF(text);
    SDL_free(text);
    return result;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(setClipboardText)(JNIEnv *env, jclass, jstring text) {
    const char *t = text ? env->GetStringUTFChars(text, nullptr) : "";
    bool ok = SDL_SetClipboardText(t);
    if (text) env->ReleaseStringUTFChars(text, t);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(hasClipboardText)(JNIEnv *, jclass) {
    return SDL_HasClipboardText() ? JNI_TRUE : JNI_FALSE;
}

// ===========================================================================
// Drivers
// ===========================================================================

SDLJNI_FUNC(jint) SDLJNI_NAME(getNumVideoDrivers)(JNIEnv *, jclass) {
    return SDL_GetNumVideoDrivers();
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getVideoDriver)(JNIEnv *env, jclass, jint index) {
    return sdl_kmp_jni_to_string(env, SDL_GetVideoDriver(index));
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getCurrentVideoDriver)(JNIEnv *env, jclass) {
    return sdl_kmp_jni_to_string(env, SDL_GetCurrentVideoDriver());
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getNumAudioDrivers)(JNIEnv *, jclass) {
    return SDL_GetNumAudioDrivers();
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getAudioDriver)(JNIEnv *env, jclass, jint index) {
    return sdl_kmp_jni_to_string(env, SDL_GetAudioDriver(index));
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getCurrentAudioDriver)(JNIEnv *env, jclass) {
    return sdl_kmp_jni_to_string(env, SDL_GetCurrentAudioDriver());
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getNumRenderDrivers)(JNIEnv *, jclass) {
    return SDL_GetNumRenderDrivers();
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getRenderDriver)(JNIEnv *env, jclass, jint index) {
    return sdl_kmp_jni_to_string(env, SDL_GetRenderDriver(index));
}

// ===========================================================================
// Message boxes
// ===========================================================================

SDLJNI_FUNC(jboolean) SDLJNI_NAME(showSimpleMessageBox)(JNIEnv *env, jclass, jstring title,
                                                        jstring message) {
    const char *t = title ? env->GetStringUTFChars(title, nullptr) : "";
    const char *m = message ? env->GetStringUTFChars(message, nullptr) : "";
    bool ok = SDL_ShowSimpleMessageBox(0, t, m, nullptr);
    if (title) env->ReleaseStringUTFChars(title, t);
    if (message) env->ReleaseStringUTFChars(message, m);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(showMessageBox)(JNIEnv *env, jclass, jint flags, jstring title,
                                              jstring message, jintArray buttonFlags,
                                              jintArray buttonIds, jobjectArray buttonTexts) {
    jsize count = env->GetArrayLength(buttonTexts);
    if (count <= 0) return -1;

    const char *t = title ? env->GetStringUTFChars(title, nullptr) : "";
    const char *m = message ? env->GetStringUTFChars(message, nullptr) : "";

    std::vector<jint> flagsV;
    std::vector<jint> idsV;
    sdl_kmp_jni_read_int_array(env, buttonFlags, flagsV);
    sdl_kmp_jni_read_int_array(env, buttonIds, idsV);

    std::vector<SDL_MessageBoxButtonData> buttons(static_cast<size_t>(count));
    std::vector<std::string> texts(static_cast<size_t>(count));
    for (jsize i = 0; i < count; i++) {
        jstring s = static_cast<jstring>(env->GetObjectArrayElement(buttonTexts, i));
        const char *cs = s ? env->GetStringUTFChars(s, nullptr) : "";
        texts[static_cast<size_t>(i)] = cs;
        if (s) {
            env->ReleaseStringUTFChars(s, cs);
            env->DeleteLocalRef(s);
        }
        buttons[static_cast<size_t>(i)].flags = static_cast<SDL_MessageBoxButtonFlags>(flagsV[static_cast<size_t>(i)]);
        buttons[static_cast<size_t>(i)].buttonID = idsV[static_cast<size_t>(i)];
        buttons[static_cast<size_t>(i)].text = texts[static_cast<size_t>(i)].c_str();
    }

    SDL_MessageBoxData data{};
    data.flags = static_cast<SDL_MessageBoxFlags>(flags);
    data.title = t;
    data.message = m;
    data.buttons = buttons.data();
    data.numbuttons = count;

    int buttonId = -1;
    bool ok = SDL_ShowMessageBox(&data, &buttonId);

    if (title) env->ReleaseStringUTFChars(title, t);
    if (message) env->ReleaseStringUTFChars(message, m);

    return ok ? buttonId : -1;
}

// ===========================================================================
// File dialogs
// ===========================================================================

static bool BuildFilterArray(JNIEnv *env, jobjectArray names, jobjectArray patterns,
                             std::vector<SDL_DialogFileFilter> &filters,
                             std::vector<std::string> &nameStorage,
                             std::vector<std::string> &patternStorage) {
    if (names == nullptr || patterns == nullptr) return false;
    jsize count = env->GetArrayLength(names);
    if (count != env->GetArrayLength(patterns)) return false;
    if (count <= 0) return false;
    nameStorage.resize(static_cast<size_t>(count));
    patternStorage.resize(static_cast<size_t>(count));
    filters.resize(static_cast<size_t>(count));
    for (jsize i = 0; i < count; i++) {
        jstring n = static_cast<jstring>(env->GetObjectArrayElement(names, i));
        jstring p = static_cast<jstring>(env->GetObjectArrayElement(patterns, i));
        const char *cn = n ? env->GetStringUTFChars(n, nullptr) : "";
        const char *cp = p ? env->GetStringUTFChars(p, nullptr) : "";
        nameStorage[static_cast<size_t>(i)] = cn;
        patternStorage[static_cast<size_t>(i)] = cp;
        if (n) env->ReleaseStringUTFChars(n, cn);
        if (p) env->ReleaseStringUTFChars(p, cp);
        if (n) env->DeleteLocalRef(n);
        if (p) env->DeleteLocalRef(p);
        filters[static_cast<size_t>(i)].name = nameStorage[static_cast<size_t>(i)].c_str();
        filters[static_cast<size_t>(i)].pattern = patternStorage[static_cast<size_t>(i)].c_str();
    }
    return true;
}

SDLJNI_FUNC(void) SDLJNI_NAME(showOpenFileDialog)(JNIEnv *env, jclass, jlong id, jlong window,
                                                  jobjectArray filterNames,
                                                  jobjectArray filterPatterns,
                                                  jstring defaultLocation, jboolean allowMultiple) {
    std::vector<SDL_DialogFileFilter> filters;
    std::vector<std::string> nameStorage;
    std::vector<std::string> patternStorage;
    BuildFilterArray(env, filterNames, filterPatterns, filters, nameStorage, patternStorage);
    const char *loc = defaultLocation ? env->GetStringUTFChars(defaultLocation, nullptr) : nullptr;
    SDL_ShowOpenFileDialog(DialogCallback, reinterpret_cast<void *>(id),
                           reinterpret_cast<SDL_Window *>(window),
                           filters.empty() ? nullptr : filters.data(),
                           static_cast<int>(filters.size()), loc, allowMultiple == JNI_TRUE);
    if (defaultLocation) env->ReleaseStringUTFChars(defaultLocation, loc);
}

SDLJNI_FUNC(void) SDLJNI_NAME(showSaveFileDialog)(JNIEnv *env, jclass, jlong id, jlong window,
                                                  jobjectArray filterNames,
                                                  jobjectArray filterPatterns,
                                                  jstring defaultLocation) {
    std::vector<SDL_DialogFileFilter> filters;
    std::vector<std::string> nameStorage;
    std::vector<std::string> patternStorage;
    BuildFilterArray(env, filterNames, filterPatterns, filters, nameStorage, patternStorage);
    const char *loc = defaultLocation ? env->GetStringUTFChars(defaultLocation, nullptr) : nullptr;
    SDL_ShowSaveFileDialog(DialogCallback, reinterpret_cast<void *>(id),
                           reinterpret_cast<SDL_Window *>(window),
                           filters.empty() ? nullptr : filters.data(),
                           static_cast<int>(filters.size()), loc);
    if (defaultLocation) env->ReleaseStringUTFChars(defaultLocation, loc);
}

SDLJNI_FUNC(void) SDLJNI_NAME(showOpenFolderDialog)(JNIEnv *env, jclass, jlong id, jlong window,
                                                    jstring defaultLocation,
                                                    jboolean allowMultiple) {
    const char *loc = defaultLocation ? env->GetStringUTFChars(defaultLocation, nullptr) : nullptr;
    SDL_ShowOpenFolderDialog(DialogCallback, reinterpret_cast<void *>(id),
                             reinterpret_cast<SDL_Window *>(window), loc,
                             allowMultiple == JNI_TRUE);
    if (defaultLocation) env->ReleaseStringUTFChars(defaultLocation, loc);
}

// ===========================================================================
// Logging
// ===========================================================================

SDLJNI_FUNC(void) SDLJNI_NAME(logMessage)(JNIEnv *env, jclass, jint category, jint priority,
                                          jstring message) {
    const char *m = message ? env->GetStringUTFChars(message, nullptr) : "";
    SDL_LogMessage(category, static_cast<SDL_LogPriority>(priority), "%s", m);
    if (message) env->ReleaseStringUTFChars(message, m);
}

SDLJNI_FUNC(void) SDLJNI_NAME(setLogPriority)(JNIEnv *, jclass, jint category, jint priority) {
    SDL_SetLogPriority(category, static_cast<SDL_LogPriority>(priority));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(getLogPriority)(JNIEnv *, jclass, jint category) {
    return static_cast<jint>(SDL_GetLogPriority(category));
}

SDLJNI_FUNC(void) SDLJNI_NAME(setLogPriorities)(JNIEnv *, jclass, jint priority) {
    SDL_SetLogPriorities(static_cast<SDL_LogPriority>(priority));
}

SDLJNI_FUNC(void) SDLJNI_NAME(resetLogPriorities)(JNIEnv *, jclass) {
    SDL_ResetLogPriorities();
}

SDLJNI_FUNC(void) SDLJNI_NAME(setLogOutputFunction)(JNIEnv *, jclass, jlong id) {
    SDL_SetLogOutputFunction(LogOutputCallback, reinterpret_cast<void *>(id));
}

SDLJNI_FUNC(void) SDLJNI_NAME(setLogOutputFunctionNull)(JNIEnv *, jclass) {
    SDL_SetLogOutputFunction(nullptr, nullptr);
}

// ===========================================================================
// Filesystem / power / misc
// ===========================================================================

SDLJNI_FUNC(jstring) SDLJNI_NAME(basePath)(JNIEnv *env, jclass) {
    const char *path = SDL_GetBasePath();
    if (path == nullptr) return nullptr;
    jstring result = env->NewStringUTF(path);
    SDL_free(const_cast<char *>(path));
    return result;
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getPrefPath)(JNIEnv *env, jclass, jstring orgName,
                                              jstring appName) {
    const char *org = orgName ? env->GetStringUTFChars(orgName, nullptr) : nullptr;
    const char *app = appName ? env->GetStringUTFChars(appName, nullptr) : nullptr;
    char *path = SDL_GetPrefPath(org, app);
    if (orgName) env->ReleaseStringUTFChars(orgName, org);
    if (appName) env->ReleaseStringUTFChars(appName, app);
    if (path == nullptr) return nullptr;
    jstring result = env->NewStringUTF(path);
    SDL_free(const_cast<char *>(path));
    return result;
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(getUserFolder)(JNIEnv *env, jclass, jint folder) {
    const char *path = SDL_GetUserFolder(static_cast<SDL_Folder>(folder));
    if (path == nullptr) return nullptr;
    jstring result = env->NewStringUTF(path);
    SDL_free(const_cast<char *>(path));
    return result;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(createDirectory)(JNIEnv *env, jclass, jstring path) {
    const char *p = path ? env->GetStringUTFChars(path, nullptr) : nullptr;
    bool ok = SDL_CreateDirectory(p);
    if (path) env->ReleaseStringUTFChars(path, p);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(removePath)(JNIEnv *env, jclass, jstring path) {
    const char *p = path ? env->GetStringUTFChars(path, nullptr) : nullptr;
    bool ok = SDL_RemovePath(p);
    if (path) env->ReleaseStringUTFChars(path, p);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(renamePath)(JNIEnv *env, jclass, jstring oldPath,
                                              jstring newPath) {
    const char *o = oldPath ? env->GetStringUTFChars(oldPath, nullptr) : nullptr;
    const char *n = newPath ? env->GetStringUTFChars(newPath, nullptr) : nullptr;
    bool ok = SDL_RenamePath(o, n);
    if (oldPath) env->ReleaseStringUTFChars(oldPath, o);
    if (newPath) env->ReleaseStringUTFChars(newPath, n);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(powerInfo)(JNIEnv *env, jclass) {
    int seconds = -1, percent = -1;
    SDL_PowerState state = SDL_GetPowerInfo(&seconds, &percent);
    return sdl_kmp_jni_new_int_array(env, {static_cast<jint>(state), percent, seconds});
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(openURL)(JNIEnv *env, jclass, jstring url) {
    const char *u = url ? env->GetStringUTFChars(url, nullptr) : nullptr;
    bool ok = SDL_OpenURL(u);
    if (url) env->ReleaseStringUTFChars(url, u);
    return ok ? JNI_TRUE : JNI_FALSE;
}
