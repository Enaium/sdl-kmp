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

// JNI bridge for sdl-kmp: OpenGL and Vulkan helpers.

#include <SDL3/SDL.h>
#include <SDL3/SDL_vulkan.h>

#include "jni_bridge.h"

// ===========================================================================
// OpenGL
// ===========================================================================

SDLJNI_FUNC(jboolean) SDLJNI_NAME(glLoadLibrary)(JNIEnv *env, jclass, jstring path) {
    const char *p = nullptr;
    if (path != nullptr) {
        p = env->GetStringUTFChars(path, nullptr);
    }
    bool ok = SDL_GL_LoadLibrary(p);
    if (path != nullptr) env->ReleaseStringUTFChars(path, p);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(glUnloadLibrary)(JNIEnv *, jclass) {
    SDL_GL_UnloadLibrary();
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(glGetProcAddress)(JNIEnv *env, jclass, jstring proc) {
    const char *p = proc ? env->GetStringUTFChars(proc, nullptr) : nullptr;
    void *fn = reinterpret_cast<void *>(SDL_GL_GetProcAddress(p));
    if (proc) env->ReleaseStringUTFChars(proc, p);
    return reinterpret_cast<jlong>(fn);
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(glExtensionSupported)(JNIEnv *env, jclass, jstring extension) {
    const char *e = extension ? env->GetStringUTFChars(extension, nullptr) : nullptr;
    bool ok = SDL_GL_ExtensionSupported(e);
    if (extension) env->ReleaseStringUTFChars(extension, e);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(glResetAttributes)(JNIEnv *, jclass) {
    SDL_GL_ResetAttributes();
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(glSetAttribute)(JNIEnv *, jclass, jint attr, jint value) {
    return SDL_GL_SetAttribute(static_cast<SDL_GLAttr>(attr), value) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(glGetAttribute)(JNIEnv *env, jclass, jint attr) {
    int value = 0;
    if (SDL_GL_GetAttribute(static_cast<SDL_GLAttr>(attr), &value)) {
        return sdl_kmp_jni_new_int_array(env, {value});
    }
    return nullptr;
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(glCreateContext)(JNIEnv *, jclass, jlong window) {
    return reinterpret_cast<jlong>(SDL_GL_CreateContext(reinterpret_cast<SDL_Window *>(window)));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(glMakeCurrent)(JNIEnv *, jclass, jlong window, jlong context) {
    return SDL_GL_MakeCurrent(reinterpret_cast<SDL_Window *>(window),
                              reinterpret_cast<SDL_GLContext>(context))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(glGetCurrentWindow)(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(SDL_GL_GetCurrentWindow());
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(glGetCurrentContext)(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(SDL_GL_GetCurrentContext());
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(glSetSwapInterval)(JNIEnv *, jclass, jint interval) {
    return SDL_GL_SetSwapInterval(interval) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(jintArray) SDLJNI_NAME(glGetSwapInterval)(JNIEnv *env, jclass) {
    int interval = 0;
    if (SDL_GL_GetSwapInterval(&interval)) {
        return sdl_kmp_jni_new_int_array(env, {interval});
    }
    return nullptr;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(glSwapWindow)(JNIEnv *, jclass, jlong window) {
    return SDL_GL_SwapWindow(reinterpret_cast<SDL_Window *>(window)) ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(glDestroyContext)(JNIEnv *, jclass, jlong context) {
    SDL_GL_DestroyContext(reinterpret_cast<SDL_GLContext>(context));
}

// ===========================================================================
// Vulkan
// ===========================================================================

SDLJNI_FUNC(jboolean) SDLJNI_NAME(vulkanLoadLibrary)(JNIEnv *env, jclass, jstring path) {
    const char *p = nullptr;
    if (path != nullptr) {
        p = env->GetStringUTFChars(path, nullptr);
    }
    bool ok = SDL_Vulkan_LoadLibrary(p);
    if (path != nullptr) env->ReleaseStringUTFChars(path, p);
    return ok ? JNI_TRUE : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(vulkanUnloadLibrary)(JNIEnv *, jclass) {
    SDL_Vulkan_UnloadLibrary();
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(vulkanGetVkGetInstanceProcAddr)(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(SDL_Vulkan_GetVkGetInstanceProcAddr());
}

SDLJNI_FUNC(jobjectArray) SDLJNI_NAME(vulkanGetInstanceExtensions)(JNIEnv *env, jclass) {
    Uint32 count = 0;
    char const *const *names = SDL_Vulkan_GetInstanceExtensions(&count);
    if (names == nullptr || count <= 0) return nullptr;
    jobjectArray result =
        env->NewObjectArray(static_cast<jsize>(count), sdl_kmp_jni_get_string_class(), nullptr);
    for (Uint32 i = 0; i < count; i++) {
        jstring s = env->NewStringUTF(names[i]);
        env->SetObjectArrayElement(result, static_cast<jsize>(i), s);
        env->DeleteLocalRef(s);
    }
    return result;
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(vulkanCreateSurface)(JNIEnv *, jclass, jlong window, jlong instance) {
    VkSurfaceKHR surface = nullptr;
    if (SDL_Vulkan_CreateSurface(reinterpret_cast<SDL_Window *>(window),
                                 reinterpret_cast<VkInstance>(instance), nullptr, &surface)) {
        return reinterpret_cast<jlong>(surface);
    }
    return 0;
}

SDLJNI_FUNC(void) SDLJNI_NAME(vulkanDestroySurface)(JNIEnv *, jclass, jlong instance, jlong surface) {
    if (surface != 0) {
        SDL_Vulkan_DestroySurface(reinterpret_cast<VkInstance>(instance),
                                  reinterpret_cast<VkSurfaceKHR>(surface), nullptr);
    }
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(vulkanGetPresentationSupport)(JNIEnv *, jclass, jlong instance,
                                                                jlong physicalDevice,
                                                                jint queueFamilyIndex) {
    return SDL_Vulkan_GetPresentationSupport(reinterpret_cast<VkInstance>(instance),
                                             reinterpret_cast<VkPhysicalDevice>(physicalDevice),
                                             static_cast<Uint32>(queueFamilyIndex))
               ? JNI_TRUE
               : JNI_FALSE;
}
