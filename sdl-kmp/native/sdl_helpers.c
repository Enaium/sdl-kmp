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
 * Small C helpers for SDL3 APIs that cinterop cannot bind directly, most
 * notably variadic functions. The compiled object is appended to the SDL3
 * static archive by the wrapper CMakeLists.txt, so a single -staticLibrary
 * flag embeds everything into the cinterop klib.
 */

#include <stdbool.h>
#include <SDL3/SDL_error.h>

bool SDL_kmp_SetError(const char *message)
{
    return SDL_SetError("%s", message);
}

#include <SDL3/SDL_log.h>

void SDL_kmp_LogMessage(int category, unsigned int priority, const char *message)
{
    SDL_LogMessage(category, (SDL_LogPriority)priority, "%s", message);
}

#include <SDL3/SDL_iostream.h>
#include <SDL3/SDL_process.h>
#include <stdlib.h>
#include <string.h>

size_t SDL_kmp_ReadIO(SDL_IOStream *stream, void *buffer, int size)
{
    return SDL_ReadIO(stream, buffer, (size_t)size);
}

size_t SDL_kmp_WriteIO(SDL_IOStream *stream, const void *buffer, int size)
{
    return SDL_WriteIO(stream, buffer, (size_t)size);
}

SDL_IOStream *SDL_kmp_IOFromMem(void *mem, int size)
{
    return SDL_IOFromMem(mem, (size_t)size);
}

SDL_IOStream *SDL_kmp_IOFromConstMem(const void *mem, int size)
{
    return SDL_IOFromConstMem(mem, (size_t)size);
}

void *SDL_kmp_LoadFile(const char *path, int *size)
{
    size_t len = 0;
    void *data = SDL_LoadFile(path, &len);
    if (data && size) *size = (int)len;
    return data;
}

void *SDL_kmp_LoadFileIO(SDL_IOStream *stream, int *size)
{
    size_t len = 0;
    void *data = SDL_LoadFile_IO(stream, false, &len);
    if (data && size) *size = (int)len;
    return data;
}

void *SDL_kmp_ReadProcess(SDL_Process *process, int *size, int *exitcode)
{
    size_t len = 0;
    void *data = SDL_ReadProcess(process, &len, exitcode);
    if (data && size) *size = (int)len;
    return data;
}

#include <SDL3/SDL_gpu.h>

SDL_GPUShader *SDL_kmp_CreateGPUShader(SDL_GPUDevice *device, const void *code, int code_size,
                                       const char *entrypoint, unsigned int format, unsigned int stage,
                                       unsigned int num_samplers, unsigned int num_storage_textures,
                                       unsigned int num_storage_buffers, unsigned int num_uniform_buffers)
{
    SDL_GPUShaderCreateInfo info;
    memset(&info, 0, sizeof(info));
    info.code_size = (size_t)code_size;
    info.code = (const Uint8 *)code;
    info.entrypoint = entrypoint;
    info.format = (SDL_GPUShaderFormat)format;
    info.stage = (SDL_GPUShaderStage)stage;
    info.num_samplers = num_samplers;
    info.num_storage_textures = num_storage_textures;
    info.num_storage_buffers = num_storage_buffers;
    info.num_uniform_buffers = num_uniform_buffers;
    return SDL_CreateGPUShader(device, &info);
}
