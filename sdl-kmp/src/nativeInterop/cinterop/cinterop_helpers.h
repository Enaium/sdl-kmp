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

/*
 * Additional declarations for cinterop. Keep this header self-contained:
 * it is compiled by cinterop against the SDKs, so no SDL3 build config is
 * available here.
 */

#ifndef CINTEROP_HELPERS_H
#define CINTEROP_HELPERS_H

#include <stdbool.h>
#include <stdint.h>

/*
 * Wrapper around SDL_SetError() for the non-variadic case (cinterop cannot
 * bind variadic functions). Defined in sdl_helpers.c, which is merged into
 * libSDL3.a by the wrapper CMake build.
 */
bool SDL_kmp_SetError(const char *message);

/*
 * Vulkan surface helpers with a stable cross-platform signature. SDL3's
 * VkSurfaceKHR is a uint64_t on 32-bit targets without Vulkan headers but a
 * pointer on 64-bit targets, which would otherwise split the cinterop
 * binding. Defined in sdl_vulkan_helpers.c (also merged into libSDL3.a).
 */
bool SDL_kmp_VulkanCreateSurface(void *window, void *instance, uint64_t *surface);
void SDL_kmp_VulkanDestroySurface(void *instance, uint64_t surface);
bool SDL_kmp_VulkanGetPresentationSupport(void *instance, void *physicalDevice, uint32_t queueFamilyIndex);

#endif /* CINTEROP_HELPERS_H */

/*
 * Wrapper around SDL_LogMessage() for the non-variadic case. Defined in
 * sdl_helpers.c (merged into libSDL3.a).
 */
void SDL_kmp_LogMessage(int category, unsigned int priority, const char *message);

/*
 * size_t-neutral wrappers so the binding compiles identically on 32-bit and
 * 64-bit targets (Kotlin/Native maps size_t to different Kotlin types).
 * Defined in sdl_helpers.c (merged into libSDL3.a).
 */
#include <stddef.h>
struct SDL_IOStream;
struct SDL_Process;
size_t SDL_kmp_ReadIO(struct SDL_IOStream *stream, void *buffer, int size);
size_t SDL_kmp_WriteIO(struct SDL_IOStream *stream, const void *buffer, int size);
struct SDL_IOStream *SDL_kmp_IOFromMem(void *mem, int size);
struct SDL_IOStream *SDL_kmp_IOFromConstMem(const void *mem, int size);
void *SDL_kmp_LoadFile(const char *path, int *size);
void *SDL_kmp_LoadFileIO(struct SDL_IOStream *stream, int *size);
void *SDL_kmp_ReadProcess(struct SDL_Process *process, int *size, int *exitcode);

struct SDL_GPUDevice;
struct SDL_GPUShader;
struct SDL_GPUShader *SDL_kmp_CreateGPUShader(struct SDL_GPUDevice *device, const void *code, int code_size,
                                              const char *entrypoint, unsigned int format, unsigned int stage,
                                              unsigned int num_samplers, unsigned int num_storage_textures,
                                              unsigned int num_storage_buffers, unsigned int num_uniform_buffers);
