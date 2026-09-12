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

// JNI bridge for sdl-kmp: the SDL3 GPU API. The create-info structs are
// built entirely on the C side from scalar Kotlin parameters, so no struct
// layout knowledge leaks into the Kotlin code.

#include <SDL3/SDL.h>

#include "jni_bridge.h"

SDLJNI_FUNC(jboolean) SDLJNI_NAME(gpuIsSupported)(JNIEnv *, jclass, jint formats) {
    return SDL_GPUSupportsShaderFormats(static_cast<SDL_GPUShaderFormat>(formats), nullptr)
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jint) SDLJNI_NAME(gpuGetNumDrivers)(JNIEnv *, jclass) {
    return SDL_GetNumGPUDrivers();
}

SDLJNI_FUNC(jstring) SDLJNI_NAME(gpuGetDriver)(JNIEnv *env, jclass, jint index) {
    return sdl_kmp_jni_to_string(env, SDL_GetGPUDriver(index));
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(gpuCreateDevice)(JNIEnv *, jclass, jint formats, jboolean debugMode) {
    return reinterpret_cast<jlong>(SDL_CreateGPUDevice(static_cast<SDL_GPUShaderFormat>(formats),
                                                       debugMode == JNI_TRUE, nullptr));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuDestroyDevice)(JNIEnv *, jclass, jlong device) {
    SDL_DestroyGPUDevice(reinterpret_cast<SDL_GPUDevice *>(device));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(gpuGetShaderFormats)(JNIEnv *, jclass, jlong device) {
    return static_cast<jint>(SDL_GetGPUShaderFormats(reinterpret_cast<SDL_GPUDevice *>(device)));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(gpuClaimWindow)(JNIEnv *, jclass, jlong device, jlong window) {
    return SDL_ClaimWindowForGPUDevice(reinterpret_cast<SDL_GPUDevice *>(device),
                                       reinterpret_cast<SDL_Window *>(window))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuReleaseWindow)(JNIEnv *, jclass, jlong device, jlong window) {
    SDL_ReleaseWindowFromGPUDevice(reinterpret_cast<SDL_GPUDevice *>(device),
                                   reinterpret_cast<SDL_Window *>(window));
}

SDLJNI_FUNC(jint) SDLJNI_NAME(gpuGetSwapchainTextureFormat)(JNIEnv *, jclass, jlong device,
                                                            jlong window) {
    return static_cast<jint>(SDL_GetGPUSwapchainTextureFormat(reinterpret_cast<SDL_GPUDevice *>(device),
                                                              reinterpret_cast<SDL_Window *>(window)));
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(gpuAcquireCommandBuffer)(JNIEnv *, jclass, jlong device) {
    return reinterpret_cast<jlong>(SDL_AcquireGPUCommandBuffer(reinterpret_cast<SDL_GPUDevice *>(device)));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(gpuSubmitCommandBuffer)(JNIEnv *, jclass, jlong commandBuffer) {
    return SDL_SubmitGPUCommandBuffer(reinterpret_cast<SDL_GPUCommandBuffer *>(commandBuffer))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(gpuSubmitCommandBufferAndAcquireFence)(JNIEnv *, jclass,
                                                                      jlong commandBuffer) {
    return reinterpret_cast<jlong>(SDL_SubmitGPUCommandBufferAndAcquireFence(
        reinterpret_cast<SDL_GPUCommandBuffer *>(commandBuffer)));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuCancelCommandBuffer)(JNIEnv *, jclass, jlong commandBuffer) {
    SDL_CancelGPUCommandBuffer(reinterpret_cast<SDL_GPUCommandBuffer *>(commandBuffer));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(gpuWaitForGPUIdle)(JNIEnv *, jclass, jlong device) {
    return SDL_WaitForGPUIdle(reinterpret_cast<SDL_GPUDevice *>(device)) ? JNI_TRUE : JNI_FALSE;
}

// colorTargets: parallel arrays. clearColors has 4 floats (RGBA) per target;
// clearColorEnabled flags whether to set the clear color on the target.
SDLJNI_FUNC(jlong) SDLJNI_NAME(gpuBeginRenderPass)(JNIEnv *env, jclass, jlong commandBuffer,
                                                   jlongArray textures, jintArray mipLevels,
                                                   jintArray layers, jintArray loadOps,
                                                   jintArray storeOps, jfloatArray clearColors,
                                                   jbooleanArray clearColorEnabled) {
    jsize count = env->GetArrayLength(textures);
    if (count <= 0) return 0;

    std::vector<jlong> texturesV;
    std::vector<jint> mipV;
    std::vector<jint> layerV;
    std::vector<jint> loadV;
    std::vector<jint> storeV;
    std::vector<jfloat> clearV;
    sdl_kmp_jni_read_long_array(env, textures, texturesV);
    sdl_kmp_jni_read_int_array(env, mipLevels, mipV);
    sdl_kmp_jni_read_int_array(env, layers, layerV);
    sdl_kmp_jni_read_int_array(env, loadOps, loadV);
    sdl_kmp_jni_read_int_array(env, storeOps, storeV);
    sdl_kmp_jni_read_float_array(env, clearColors, clearV);

    std::vector<bool> clearEnabled(static_cast<size_t>(count), false);
    if (clearColorEnabled != nullptr) {
        std::vector<jboolean> flags(static_cast<size_t>(count));
        env->GetBooleanArrayRegion(clearColorEnabled, 0, count, flags.data());
        for (jsize i = 0; i < count; i++) {
            clearEnabled[static_cast<size_t>(i)] = flags[static_cast<size_t>(i)] == JNI_TRUE;
        }
    }

    std::vector<SDL_GPUColorTargetInfo> targets(static_cast<size_t>(count));
    for (jsize i = 0; i < count; i++) {
        SDL_GPUColorTargetInfo &t = targets[static_cast<size_t>(i)];
        t.texture = reinterpret_cast<SDL_GPUTexture *>(texturesV[static_cast<size_t>(i)]);
        t.mip_level = static_cast<Uint32>(mipV[static_cast<size_t>(i)]);
        t.layer_or_depth_plane = static_cast<Uint32>(layerV[static_cast<size_t>(i)]);
        t.load_op = static_cast<SDL_GPULoadOp>(loadV[static_cast<size_t>(i)]);
        t.store_op = static_cast<SDL_GPUStoreOp>(storeV[static_cast<size_t>(i)]);
        if (clearEnabled[static_cast<size_t>(i)]) {
            t.clear_color.r = clearV[static_cast<size_t>(i * 4)];
            t.clear_color.g = clearV[static_cast<size_t>(i * 4 + 1)];
            t.clear_color.b = clearV[static_cast<size_t>(i * 4 + 2)];
            t.clear_color.a = clearV[static_cast<size_t>(i * 4 + 3)];
        }
    }
    return reinterpret_cast<jlong>(SDL_BeginGPURenderPass(
        reinterpret_cast<SDL_GPUCommandBuffer *>(commandBuffer), targets.data(), count, nullptr));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuEndRenderPass)(JNIEnv *, jclass, jlong renderPass) {
    SDL_EndGPURenderPass(reinterpret_cast<SDL_GPURenderPass *>(renderPass));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuBindGraphicsPipeline)(JNIEnv *, jclass, jlong renderPass,
                                                       jlong pipeline) {
    SDL_BindGPUGraphicsPipeline(reinterpret_cast<SDL_GPURenderPass *>(renderPass),
                                reinterpret_cast<SDL_GPUGraphicsPipeline *>(pipeline));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuSetViewport)(JNIEnv *, jclass, jlong renderPass, jfloat x,
                                              jfloat y, jfloat w, jfloat h, jfloat minDepth,
                                              jfloat maxDepth) {
    SDL_GPUViewport viewport{x, y, w, h, minDepth, maxDepth};
    SDL_SetGPUViewport(reinterpret_cast<SDL_GPURenderPass *>(renderPass), &viewport);
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuSetScissor)(JNIEnv *, jclass, jlong renderPass, jint x, jint y,
                                             jint w, jint h) {
    SDL_Rect rect{x, y, w, h};
    SDL_SetGPUScissor(reinterpret_cast<SDL_GPURenderPass *>(renderPass), &rect);
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuBindVertexBuffers)(JNIEnv *env, jclass, jlong renderPass,
                                                    jlongArray buffers, jintArray offsets) {
    jsize count = env->GetArrayLength(buffers);
    if (count <= 0) return;
    std::vector<jlong> buffersV;
    std::vector<jint> offsetsV;
    sdl_kmp_jni_read_long_array(env, buffers, buffersV);
    sdl_kmp_jni_read_int_array(env, offsets, offsetsV);
    std::vector<SDL_GPUBufferBinding> bindings(static_cast<size_t>(count));
    for (jsize i = 0; i < count; i++) {
        bindings[static_cast<size_t>(i)].buffer =
            reinterpret_cast<SDL_GPUBuffer *>(buffersV[static_cast<size_t>(i)]);
        bindings[static_cast<size_t>(i)].offset = static_cast<Uint32>(offsetsV[static_cast<size_t>(i)]);
    }
    SDL_BindGPUVertexBuffers(reinterpret_cast<SDL_GPURenderPass *>(renderPass), 0, bindings.data(),
                             static_cast<Uint32>(count));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuBindIndexBuffer)(JNIEnv *, jclass, jlong renderPass,
                                                  jlong buffer, jint indexElementSize) {
    SDL_GPUBufferBinding binding{};
    binding.buffer = reinterpret_cast<SDL_GPUBuffer *>(buffer);
    binding.offset = 0;
    SDL_BindGPUIndexBuffer(reinterpret_cast<SDL_GPURenderPass *>(renderPass), &binding,
                           static_cast<SDL_GPUIndexElementSize>(indexElementSize));
}

// textures and samplers are parallel arrays (either may hold 0 per entry).
SDLJNI_FUNC(void) SDLJNI_NAME(gpuBindFragmentSamplers)(JNIEnv *env, jclass, jlong renderPass,
                                                       jint slot, jlongArray textures,
                                                       jlongArray samplers) {
    jsize count = env->GetArrayLength(textures);
    if (count <= 0) return;
    std::vector<jlong> texturesV;
    std::vector<jlong> samplersV;
    sdl_kmp_jni_read_long_array(env, textures, texturesV);
    sdl_kmp_jni_read_long_array(env, samplers, samplersV);
    std::vector<SDL_GPUTextureSamplerBinding> bindings(static_cast<size_t>(count));
    for (jsize i = 0; i < count; i++) {
        bindings[static_cast<size_t>(i)].texture =
            reinterpret_cast<SDL_GPUTexture *>(texturesV[static_cast<size_t>(i)]);
        bindings[static_cast<size_t>(i)].sampler =
            reinterpret_cast<SDL_GPUSampler *>(samplersV[static_cast<size_t>(i)]);
    }
    SDL_BindGPUFragmentSamplers(reinterpret_cast<SDL_GPURenderPass *>(renderPass), slot,
                                bindings.data(), static_cast<Uint32>(count));
}

// Binds storage textures to the vertex/fragment stage (GRAPHICS_STORAGE_READ).
SDLJNI_FUNC(void) SDLJNI_NAME(gpuBindVertexStorageTextures)(JNIEnv *env, jclass, jlong renderPass,
                                                            jint slot, jlongArray textures) {
    jsize count = env->GetArrayLength(textures);
    if (count <= 0) return;
    std::vector<jlong> texturesV;
    sdl_kmp_jni_read_long_array(env, textures, texturesV);
    std::vector<SDL_GPUTexture *> ptrs(static_cast<size_t>(count));
    for (jsize i = 0; i < count; i++) {
        ptrs[static_cast<size_t>(i)] = reinterpret_cast<SDL_GPUTexture *>(texturesV[static_cast<size_t>(i)]);
    }
    SDL_BindGPUVertexStorageTextures(reinterpret_cast<SDL_GPURenderPass *>(renderPass),
                                     static_cast<Uint32>(slot), ptrs.data(),
                                     static_cast<Uint32>(count));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuBindVertexStorageBuffers)(JNIEnv *env, jclass, jlong renderPass,
                                                           jint slot, jlongArray buffers) {
    jsize count = env->GetArrayLength(buffers);
    if (count <= 0) return;
    std::vector<jlong> buffersV;
    sdl_kmp_jni_read_long_array(env, buffers, buffersV);
    std::vector<SDL_GPUBuffer *> ptrs(static_cast<size_t>(count));
    for (jsize i = 0; i < count; i++) {
        ptrs[static_cast<size_t>(i)] = reinterpret_cast<SDL_GPUBuffer *>(buffersV[static_cast<size_t>(i)]);
    }
    SDL_BindGPUVertexStorageBuffers(reinterpret_cast<SDL_GPURenderPass *>(renderPass),
                                    static_cast<Uint32>(slot), ptrs.data(),
                                    static_cast<Uint32>(count));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuBindFragmentStorageTextures)(JNIEnv *env, jclass, jlong renderPass,
                                                              jint slot, jlongArray textures) {
    jsize count = env->GetArrayLength(textures);
    if (count <= 0) return;
    std::vector<jlong> texturesV;
    sdl_kmp_jni_read_long_array(env, textures, texturesV);
    std::vector<SDL_GPUTexture *> ptrs(static_cast<size_t>(count));
    for (jsize i = 0; i < count; i++) {
        ptrs[static_cast<size_t>(i)] = reinterpret_cast<SDL_GPUTexture *>(texturesV[static_cast<size_t>(i)]);
    }
    SDL_BindGPUFragmentStorageTextures(reinterpret_cast<SDL_GPURenderPass *>(renderPass),
                                       static_cast<Uint32>(slot), ptrs.data(),
                                       static_cast<Uint32>(count));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuBindFragmentStorageBuffers)(JNIEnv *env, jclass, jlong renderPass,
                                                             jint slot, jlongArray buffers) {
    jsize count = env->GetArrayLength(buffers);
    if (count <= 0) return;
    std::vector<jlong> buffersV;
    sdl_kmp_jni_read_long_array(env, buffers, buffersV);
    std::vector<SDL_GPUBuffer *> ptrs(static_cast<size_t>(count));
    for (jsize i = 0; i < count; i++) {
        ptrs[static_cast<size_t>(i)] = reinterpret_cast<SDL_GPUBuffer *>(buffersV[static_cast<size_t>(i)]);
    }
    SDL_BindGPUFragmentStorageBuffers(reinterpret_cast<SDL_GPURenderPass *>(renderPass),
                                      static_cast<Uint32>(slot), ptrs.data(),
                                      static_cast<Uint32>(count));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuPushVertexUniformData)(JNIEnv *env, jclass, jlong commandBuffer,
                                                        jint slot, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    if (len <= 0) return;
    std::vector<jbyte> buffer(static_cast<size_t>(len));
    env->GetByteArrayRegion(data, 0, len, buffer.data());
    SDL_PushGPUVertexUniformData(reinterpret_cast<SDL_GPUCommandBuffer *>(commandBuffer), slot,
                                 buffer.data(), static_cast<Uint32>(len));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuPushFragmentUniformData)(JNIEnv *env, jclass, jlong commandBuffer,
                                                          jint slot, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    if (len <= 0) return;
    std::vector<jbyte> buffer(static_cast<size_t>(len));
    env->GetByteArrayRegion(data, 0, len, buffer.data());
    SDL_PushGPUFragmentUniformData(reinterpret_cast<SDL_GPUCommandBuffer *>(commandBuffer), slot,
                                   buffer.data(), static_cast<Uint32>(len));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuDrawPrimitives)(JNIEnv *, jclass, jlong renderPass,
                                                 jint vertexCount, jint instanceCount,
                                                 jint firstVertex, jint firstInstance) {
    SDL_DrawGPUPrimitives(reinterpret_cast<SDL_GPURenderPass *>(renderPass),
                          static_cast<Uint32>(vertexCount), static_cast<Uint32>(instanceCount),
                          static_cast<Uint32>(firstVertex), static_cast<Uint32>(firstInstance));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuDrawIndexedPrimitives)(JNIEnv *, jclass, jlong renderPass,
                                                        jint indexCount, jint instanceCount,
                                                        jint firstIndex, jint vertexOffset,
                                                        jint firstInstance) {
    SDL_DrawGPUIndexedPrimitives(reinterpret_cast<SDL_GPURenderPass *>(renderPass),
                                 static_cast<Uint32>(indexCount), static_cast<Uint32>(instanceCount),
                                 static_cast<Uint32>(firstIndex), static_cast<Sint32>(vertexOffset),
                                 static_cast<Uint32>(firstInstance));
}

// Blits a source texture region into a destination region (scaling allowed).
// Must be called outside any pass on the command buffer.
SDLJNI_FUNC(jboolean) SDLJNI_NAME(gpuBlit)(
    JNIEnv *, jclass, jlong commandBuffer,
    jlong srcTexture, jint srcMip, jint srcLayer, jint srcX, jint srcY, jint srcW, jint srcH,
    jlong dstTexture, jint dstMip, jint dstLayer, jint dstX, jint dstY, jint dstW, jint dstH,
    jint loadOp, jfloat clearR, jfloat clearG, jfloat clearB, jfloat clearA,
    jint flipMode, jint filter, jboolean cycle) {
    SDL_GPUBlitInfo info{};
    info.source.texture = reinterpret_cast<SDL_GPUTexture *>(srcTexture);
    info.source.mip_level = static_cast<Uint32>(srcMip);
    info.source.layer_or_depth_plane = static_cast<Uint32>(srcLayer);
    info.source.x = static_cast<Uint32>(srcX);
    info.source.y = static_cast<Uint32>(srcY);
    info.source.w = static_cast<Uint32>(srcW);
    info.source.h = static_cast<Uint32>(srcH);
    info.destination.texture = reinterpret_cast<SDL_GPUTexture *>(dstTexture);
    info.destination.mip_level = static_cast<Uint32>(dstMip);
    info.destination.layer_or_depth_plane = static_cast<Uint32>(dstLayer);
    info.destination.x = static_cast<Uint32>(dstX);
    info.destination.y = static_cast<Uint32>(dstY);
    info.destination.w = static_cast<Uint32>(dstW);
    info.destination.h = static_cast<Uint32>(dstH);
    info.load_op = static_cast<SDL_GPULoadOp>(loadOp);
    info.clear_color.r = clearR;
    info.clear_color.g = clearG;
    info.clear_color.b = clearB;
    info.clear_color.a = clearA;
    info.flip_mode = static_cast<SDL_FlipMode>(flipMode);
    info.filter = static_cast<SDL_GPUFilter>(filter);
    info.cycle = cycle == JNI_TRUE;
    SDL_BlitGPUTexture(reinterpret_cast<SDL_GPUCommandBuffer *>(commandBuffer), &info);
    return JNI_TRUE;
}

// Copies a texture subregion into another texture with a copy pass inside
// the given command buffer (not submitted; caller submits it later).
SDLJNI_FUNC(jboolean) SDLJNI_NAME(gpuCopyTextureToTexture)(
    JNIEnv *, jclass, jlong commandBuffer,
    jlong srcTexture, jint srcMip, jint srcLayer, jint srcX, jint srcY, jint srcZ,
    jlong dstTexture, jint dstMip, jint dstLayer, jint dstX, jint dstY, jint dstZ,
    jint w, jint h, jint d) {
    SDL_GPUCommandBuffer *cmd = reinterpret_cast<SDL_GPUCommandBuffer *>(commandBuffer);
    SDL_GPUCopyPass *pass = SDL_BeginGPUCopyPass(cmd);
    if (pass == nullptr) return JNI_FALSE;
    SDL_GPUTextureLocation src{};
    src.texture = reinterpret_cast<SDL_GPUTexture *>(srcTexture);
    src.mip_level = static_cast<Uint32>(srcMip);
    src.layer = static_cast<Uint32>(srcLayer);
    src.x = static_cast<Uint32>(srcX);
    src.y = static_cast<Uint32>(srcY);
    src.z = static_cast<Uint32>(srcZ);
    SDL_GPUTextureLocation dst{};
    dst.texture = reinterpret_cast<SDL_GPUTexture *>(dstTexture);
    dst.mip_level = static_cast<Uint32>(dstMip);
    dst.layer = static_cast<Uint32>(dstLayer);
    dst.x = static_cast<Uint32>(dstX);
    dst.y = static_cast<Uint32>(dstY);
    dst.z = static_cast<Uint32>(dstZ);
    SDL_CopyGPUTextureToTexture(pass, &src, &dst, static_cast<Uint32>(w),
                                static_cast<Uint32>(h), static_cast<Uint32>(d), false);
    SDL_EndGPUCopyPass(pass);
    return JNI_TRUE;
}

// Begins a compute pass. storageTextures/storageBuffers: parallel arrays of
// {texture, mip, layer, cycle} / {buffer, cycle} per binding.
SDLJNI_FUNC(jlong) SDLJNI_NAME(gpuBeginComputePass)(
    JNIEnv *env, jclass, jlong commandBuffer,
    jlongArray storageTextures, jintArray storageTextureMips, jintArray storageTextureLayers,
    jbooleanArray storageTextureCycles, jlongArray storageBuffers, jbooleanArray storageBufferCycles) {
    std::vector<jlong> texV;
    std::vector<jint> mipV;
    std::vector<jint> layerV;
    std::vector<jboolean> texCycleV;
    std::vector<jlong> bufV;
    std::vector<jboolean> bufCycleV;
    if (storageTextures != nullptr) {
        sdl_kmp_jni_read_long_array(env, storageTextures, texV);
        sdl_kmp_jni_read_int_array(env, storageTextureMips, mipV);
        sdl_kmp_jni_read_int_array(env, storageTextureLayers, layerV);
        jsize n = env->GetArrayLength(storageTextureCycles);
        texCycleV.resize(static_cast<size_t>(n));
        env->GetBooleanArrayRegion(storageTextureCycles, 0, n, texCycleV.data());
    }
    if (storageBuffers != nullptr) {
        sdl_kmp_jni_read_long_array(env, storageBuffers, bufV);
        jsize n = env->GetArrayLength(storageBufferCycles);
        bufCycleV.resize(static_cast<size_t>(n));
        env->GetBooleanArrayRegion(storageBufferCycles, 0, n, bufCycleV.data());
    }

    std::vector<SDL_GPUStorageTextureReadWriteBinding> texBindings(texV.size());
    for (size_t i = 0; i < texV.size(); i++) {
        texBindings[i].texture = reinterpret_cast<SDL_GPUTexture *>(texV[i]);
        texBindings[i].mip_level = static_cast<Uint32>(mipV[static_cast<size_t>(i)]);
        texBindings[i].layer = static_cast<Uint32>(layerV[static_cast<size_t>(i)]);
        texBindings[i].cycle = texCycleV[static_cast<size_t>(i)] == JNI_TRUE;
    }
    std::vector<SDL_GPUStorageBufferReadWriteBinding> bufBindings(bufV.size());
    for (size_t i = 0; i < bufV.size(); i++) {
        bufBindings[i].buffer = reinterpret_cast<SDL_GPUBuffer *>(bufV[i]);
        bufBindings[i].cycle = bufCycleV[static_cast<size_t>(i)] == JNI_TRUE;
    }

    return reinterpret_cast<jlong>(SDL_BeginGPUComputePass(
        reinterpret_cast<SDL_GPUCommandBuffer *>(commandBuffer),
        texBindings.empty() ? nullptr : texBindings.data(),
        static_cast<Uint32>(texBindings.size()),
        bufBindings.empty() ? nullptr : bufBindings.data(),
        static_cast<Uint32>(bufBindings.size())));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuEndComputePass)(JNIEnv *, jclass, jlong computePass) {
    SDL_EndGPUComputePass(reinterpret_cast<SDL_GPUComputePass *>(computePass));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuBindComputePipeline)(JNIEnv *, jclass, jlong computePass,
                                                      jlong pipeline) {
    SDL_BindGPUComputePipeline(reinterpret_cast<SDL_GPUComputePass *>(computePass),
                               reinterpret_cast<SDL_GPUComputePipeline *>(pipeline));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuBindComputeSamplers)(JNIEnv *env, jclass, jlong computePass,
                                                      jint slot, jlongArray textures,
                                                      jlongArray samplers) {
    jsize count = env->GetArrayLength(textures);
    if (count <= 0) return;
    std::vector<jlong> texturesV;
    std::vector<jlong> samplersV;
    sdl_kmp_jni_read_long_array(env, textures, texturesV);
    sdl_kmp_jni_read_long_array(env, samplers, samplersV);
    std::vector<SDL_GPUTextureSamplerBinding> bindings(static_cast<size_t>(count));
    for (jsize i = 0; i < count; i++) {
        bindings[static_cast<size_t>(i)].texture =
            reinterpret_cast<SDL_GPUTexture *>(texturesV[static_cast<size_t>(i)]);
        bindings[static_cast<size_t>(i)].sampler =
            reinterpret_cast<SDL_GPUSampler *>(samplersV[static_cast<size_t>(i)]);
    }
    SDL_BindGPUComputeSamplers(reinterpret_cast<SDL_GPUComputePass *>(computePass),
                               static_cast<Uint32>(slot), bindings.data(),
                               static_cast<Uint32>(count));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuBindComputeStorageTextures)(JNIEnv *env, jclass, jlong computePass,
                                                             jint slot, jlongArray textures) {
    jsize count = env->GetArrayLength(textures);
    if (count <= 0) return;
    std::vector<jlong> texturesV;
    sdl_kmp_jni_read_long_array(env, textures, texturesV);
    std::vector<SDL_GPUTexture *> ptrs(static_cast<size_t>(count));
    for (jsize i = 0; i < count; i++) {
        ptrs[static_cast<size_t>(i)] = reinterpret_cast<SDL_GPUTexture *>(texturesV[static_cast<size_t>(i)]);
    }
    SDL_BindGPUComputeStorageTextures(reinterpret_cast<SDL_GPUComputePass *>(computePass),
                                      static_cast<Uint32>(slot), ptrs.data(),
                                      static_cast<Uint32>(count));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuBindComputeStorageBuffers)(JNIEnv *env, jclass, jlong computePass,
                                                            jint slot, jlongArray buffers) {
    jsize count = env->GetArrayLength(buffers);
    if (count <= 0) return;
    std::vector<jlong> buffersV;
    sdl_kmp_jni_read_long_array(env, buffers, buffersV);
    std::vector<SDL_GPUBuffer *> ptrs(static_cast<size_t>(count));
    for (jsize i = 0; i < count; i++) {
        ptrs[static_cast<size_t>(i)] = reinterpret_cast<SDL_GPUBuffer *>(buffersV[static_cast<size_t>(i)]);
    }
    SDL_BindGPUComputeStorageBuffers(reinterpret_cast<SDL_GPUComputePass *>(computePass),
                                     static_cast<Uint32>(slot), ptrs.data(),
                                     static_cast<Uint32>(count));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuDispatchCompute)(JNIEnv *, jclass, jlong computePass,
                                                  jint groupCountX, jint groupCountY,
                                                  jint groupCountZ) {
    SDL_DispatchGPUCompute(reinterpret_cast<SDL_GPUComputePass *>(computePass),
                           static_cast<Uint32>(groupCountX), static_cast<Uint32>(groupCountY),
                           static_cast<Uint32>(groupCountZ));
}

// Pushes compute uniform data (SDL_PushGPUComputeUniformData lives on the
// command buffer, not the pass; pass the command buffer handle).
SDLJNI_FUNC(void) SDLJNI_NAME(gpuPushComputeUniformData)(JNIEnv *env, jclass, jlong commandBuffer,
                                                         jint slot, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    if (len <= 0) return;
    std::vector<jbyte> buffer(static_cast<size_t>(len));
    env->GetByteArrayRegion(data, 0, len, buffer.data());
    SDL_PushGPUComputeUniformData(reinterpret_cast<SDL_GPUCommandBuffer *>(commandBuffer), slot,
                                  buffer.data(), static_cast<Uint32>(len));
}

// The sdl-kmp common API uses the "number of samples" convention (1, 2, 4,
// 8), which does NOT match the SDL_GPUSampleCount enum values (0..3); map
// like the native backend's gpuSampleCountOf().
static SDL_GPUSampleCount sdl_kmp_sample_count_of(int sampleCount) {
    switch (sampleCount) {
        case 2: return SDL_GPU_SAMPLECOUNT_2;
        case 4: return SDL_GPU_SAMPLECOUNT_4;
        case 8: return SDL_GPU_SAMPLECOUNT_8;
        default: return SDL_GPU_SAMPLECOUNT_1;
    }
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(gpuCreateTexture)(JNIEnv *, jclass, jlong device, jint type,
                                                 jint format, jint usage, jint width, jint height,
                                                 jint layerCountOrDepth, jint numLevels,
                                                 jint sampleCount) {
    SDL_GPUTextureCreateInfo info{};
    info.type = static_cast<SDL_GPUTextureType>(type);
    info.format = static_cast<SDL_GPUTextureFormat>(format);
    info.usage = static_cast<SDL_GPUTextureUsageFlags>(usage);
    info.width = static_cast<Uint32>(width);
    info.height = static_cast<Uint32>(height);
    info.layer_count_or_depth = static_cast<Uint32>(layerCountOrDepth);
    info.num_levels = static_cast<Uint32>(numLevels);
    info.sample_count = sdl_kmp_sample_count_of(sampleCount);
    return reinterpret_cast<jlong>(SDL_CreateGPUTexture(reinterpret_cast<SDL_GPUDevice *>(device), &info));
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(gpuCreateBuffer)(JNIEnv *, jclass, jlong device, jint usage,
                                                jint size) {
    SDL_GPUBufferCreateInfo info{};
    info.usage = static_cast<SDL_GPUBufferUsageFlags>(usage);
    info.size = static_cast<Uint32>(size);
    return reinterpret_cast<jlong>(SDL_CreateGPUBuffer(reinterpret_cast<SDL_GPUDevice *>(device), &info));
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(gpuCreateShader)(JNIEnv *env, jclass, jlong device, jbyteArray code,
                                                jint format, jint stage, jstring entryPoint,
                                                jint numSamplers, jint numStorageTextures,
                                                jint numStorageBuffers, jint numUniformBuffers) {
    jsize len = env->GetArrayLength(code);
    if (len <= 0) return 0;
    std::vector<jbyte> buffer(static_cast<size_t>(len));
    env->GetByteArrayRegion(code, 0, len, buffer.data());
    const char *entry = entryPoint ? env->GetStringUTFChars(entryPoint, nullptr) : "";
    SDL_GPUShaderCreateInfo info{};
    info.code_size = static_cast<size_t>(len);
    info.code = reinterpret_cast<const Uint8 *>(buffer.data());
    info.entrypoint = entry;
    info.format = static_cast<SDL_GPUShaderFormat>(format);
    info.stage = static_cast<SDL_GPUShaderStage>(stage);
    info.num_samplers = static_cast<Uint32>(numSamplers);
    info.num_storage_textures = static_cast<Uint32>(numStorageTextures);
    info.num_storage_buffers = static_cast<Uint32>(numStorageBuffers);
    info.num_uniform_buffers = static_cast<Uint32>(numUniformBuffers);
    SDL_GPUShader *shader = SDL_CreateGPUShader(reinterpret_cast<SDL_GPUDevice *>(device), &info);
    if (entryPoint) env->ReleaseStringUTFChars(entryPoint, entry);
    return reinterpret_cast<jlong>(shader);
}

// vertexBufferDescriptions: flat [slot, pitch, inputRate, instanceStepRate] per buffer.
// vertexAttributes: flat [location, bufferSlot, format, offset] per attribute.
// blendStates: flat [srcColor, dstColor, colorOp, srcAlpha, dstAlpha, alphaOp, colorWriteMask]
// per color target (enable_blend is always true, matching the previous LWJGL
// implementation).
SDLJNI_FUNC(jlong) SDLJNI_NAME(gpuCreateGraphicsPipeline)(
    JNIEnv *env, jclass, jlong device, jlong vertexShader, jlong fragmentShader, jint primitiveType,
    jintArray vertexBufferDescriptions, jintArray vertexAttributes, jint fillMode, jint cullMode,
    jint frontFace, jint compareOp, jboolean enableDepthTest, jboolean enableDepthWrite,
    jintArray targetFormats, jintArray blendStates) {

    std::vector<jint> vbDesc;
    std::vector<jint> vaDesc;
    std::vector<jint> targetFmt;
    std::vector<jint> blend;
    sdl_kmp_jni_read_int_array(env, vertexBufferDescriptions, vbDesc);
    sdl_kmp_jni_read_int_array(env, vertexAttributes, vaDesc);
    sdl_kmp_jni_read_int_array(env, targetFormats, targetFmt);
    sdl_kmp_jni_read_int_array(env, blendStates, blend);

    SDL_GPUGraphicsPipelineCreateInfo info{};
    info.vertex_shader = reinterpret_cast<SDL_GPUShader *>(vertexShader);
    info.fragment_shader = reinterpret_cast<SDL_GPUShader *>(fragmentShader);
    info.primitive_type = static_cast<SDL_GPUPrimitiveType>(primitiveType);

    std::vector<SDL_GPUVertexBufferDescription> vb(static_cast<size_t>(vbDesc.size() / 4));
    for (size_t i = 0; i < vb.size(); i++) {
        vb[i].slot = static_cast<Uint32>(vbDesc[i * 4]);
        vb[i].pitch = static_cast<Uint32>(vbDesc[i * 4 + 1]);
        vb[i].input_rate = static_cast<SDL_GPUVertexInputRate>(vbDesc[i * 4 + 2]);
        vb[i].instance_step_rate = static_cast<Uint32>(vbDesc[i * 4 + 3]);
    }
    std::vector<SDL_GPUVertexAttribute> va(static_cast<size_t>(vaDesc.size() / 4));
    for (size_t i = 0; i < va.size(); i++) {
        va[i].location = static_cast<Uint32>(vaDesc[i * 4]);
        va[i].buffer_slot = static_cast<Uint32>(vaDesc[i * 4 + 1]);
        va[i].format = static_cast<SDL_GPUVertexElementFormat>(vaDesc[i * 4 + 2]);
        va[i].offset = static_cast<Uint32>(vaDesc[i * 4 + 3]);
    }
    info.vertex_input_state.vertex_buffer_descriptions = vb.empty() ? nullptr : vb.data();
    info.vertex_input_state.num_vertex_buffers = static_cast<Uint32>(vb.size());
    info.vertex_input_state.vertex_attributes = va.empty() ? nullptr : va.data();
    info.vertex_input_state.num_vertex_attributes = static_cast<Uint32>(va.size());

    info.rasterizer_state.fill_mode = static_cast<SDL_GPUFillMode>(fillMode);
    info.rasterizer_state.cull_mode = static_cast<SDL_GPUCullMode>(cullMode);
    info.rasterizer_state.front_face = static_cast<SDL_GPUFrontFace>(frontFace);

    info.depth_stencil_state.compare_op = static_cast<SDL_GPUCompareOp>(compareOp);
    info.depth_stencil_state.enable_depth_test = enableDepthTest == JNI_TRUE;
    info.depth_stencil_state.enable_depth_write = enableDepthWrite == JNI_TRUE;

    std::vector<SDL_GPUColorTargetDescription> targets(targetFmt.size());
    for (size_t i = 0; i < targets.size(); i++) {
        targets[i].format = static_cast<SDL_GPUTextureFormat>(targetFmt[i]);
        SDL_GPUColorTargetBlendState &b = targets[i].blend_state;
        b.enable_blend = true;
        b.src_color_blendfactor = static_cast<SDL_GPUBlendFactor>(blend[i * 7]);
        b.dst_color_blendfactor = static_cast<SDL_GPUBlendFactor>(blend[i * 7 + 1]);
        b.color_blend_op = static_cast<SDL_GPUBlendOp>(blend[i * 7 + 2]);
        b.src_alpha_blendfactor = static_cast<SDL_GPUBlendFactor>(blend[i * 7 + 3]);
        b.dst_alpha_blendfactor = static_cast<SDL_GPUBlendFactor>(blend[i * 7 + 4]);
        b.alpha_blend_op = static_cast<SDL_GPUBlendOp>(blend[i * 7 + 5]);
        b.enable_color_write_mask = true;
        b.color_write_mask = static_cast<Uint8>(blend[i * 7 + 6]);
    }
    info.target_info.color_target_descriptions = targets.empty() ? nullptr : targets.data();
    info.target_info.num_color_targets = static_cast<Uint32>(targets.size());

    return reinterpret_cast<jlong>(
        SDL_CreateGPUGraphicsPipeline(reinterpret_cast<SDL_GPUDevice *>(device), &info));
}

SDLJNI_FUNC(jlong) SDLJNI_NAME(gpuCreateSampler)(JNIEnv *, jclass, jlong device, jint minFilter,
                                                 jint magFilter, jint mipmapMode, jint addressModeU,
                                                 jint addressModeV, jint addressModeW,
                                                 jfloat maxAnisotropy) {
    SDL_GPUSamplerCreateInfo info{};
    info.min_filter = static_cast<SDL_GPUFilter>(minFilter);
    info.mag_filter = static_cast<SDL_GPUFilter>(magFilter);
    info.mipmap_mode = static_cast<SDL_GPUSamplerMipmapMode>(mipmapMode);
    info.address_mode_u = static_cast<SDL_GPUSamplerAddressMode>(addressModeU);
    info.address_mode_v = static_cast<SDL_GPUSamplerAddressMode>(addressModeV);
    info.address_mode_w = static_cast<SDL_GPUSamplerAddressMode>(addressModeW);
    info.max_anisotropy = maxAnisotropy;
    return reinterpret_cast<jlong>(
        SDL_CreateGPUSampler(reinterpret_cast<SDL_GPUDevice *>(device), &info));
}

// code: the compute shader bytecode. numSamplers/numReadonly*/numReadwrite*/
// numUniformBuffers/threadcount* are scalars (see SDL_GPUComputePipelineCreateInfo).
SDLJNI_FUNC(jlong) SDLJNI_NAME(gpuCreateComputePipeline)(
    JNIEnv *env, jclass, jlong device, jbyteArray code, jint format, jstring entryPoint,
    jint numSamplers, jint numReadonlyStorageTextures, jint numReadonlyStorageBuffers,
    jint numReadwriteStorageTextures, jint numReadwriteStorageBuffers, jint numUniformBuffers,
    jint threadcountX, jint threadcountY, jint threadcountZ) {
    jsize len = env->GetArrayLength(code);
    if (len <= 0) return 0;
    std::vector<jbyte> buffer(static_cast<size_t>(len));
    env->GetByteArrayRegion(code, 0, len, buffer.data());
    const char *entry = entryPoint ? env->GetStringUTFChars(entryPoint, nullptr) : "";
    SDL_GPUComputePipelineCreateInfo info{};
    info.code_size = static_cast<size_t>(len);
    info.code = reinterpret_cast<const Uint8 *>(buffer.data());
    info.entrypoint = entry;
    info.format = static_cast<SDL_GPUShaderFormat>(format);
    info.num_samplers = static_cast<Uint32>(numSamplers);
    info.num_readonly_storage_textures = static_cast<Uint32>(numReadonlyStorageTextures);
    info.num_readonly_storage_buffers = static_cast<Uint32>(numReadonlyStorageBuffers);
    info.num_readwrite_storage_textures = static_cast<Uint32>(numReadwriteStorageTextures);
    info.num_readwrite_storage_buffers = static_cast<Uint32>(numReadwriteStorageBuffers);
    info.num_uniform_buffers = static_cast<Uint32>(numUniformBuffers);
    info.threadcount_x = static_cast<Uint32>(threadcountX);
    info.threadcount_y = static_cast<Uint32>(threadcountY);
    info.threadcount_z = static_cast<Uint32>(threadcountZ);
    SDL_GPUComputePipeline *pipeline = SDL_CreateGPUComputePipeline(
        reinterpret_cast<SDL_GPUDevice *>(device), &info);
    if (entryPoint) env->ReleaseStringUTFChars(entryPoint, entry);
    return reinterpret_cast<jlong>(pipeline);
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuReleaseShader)(JNIEnv *, jclass, jlong device, jlong shader) {
    SDL_ReleaseGPUShader(reinterpret_cast<SDL_GPUDevice *>(device),
                         reinterpret_cast<SDL_GPUShader *>(shader));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuReleaseComputePipeline)(JNIEnv *, jclass, jlong device,
                                                         jlong pipeline) {
    SDL_ReleaseGPUComputePipeline(reinterpret_cast<SDL_GPUDevice *>(device),
                                  reinterpret_cast<SDL_GPUComputePipeline *>(pipeline));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuReleaseGraphicsPipeline)(JNIEnv *, jclass, jlong device,
                                                          jlong pipeline) {
    SDL_ReleaseGPUGraphicsPipeline(reinterpret_cast<SDL_GPUDevice *>(device),
                                   reinterpret_cast<SDL_GPUGraphicsPipeline *>(pipeline));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuReleaseTexture)(JNIEnv *, jclass, jlong device, jlong texture) {
    SDL_ReleaseGPUTexture(reinterpret_cast<SDL_GPUDevice *>(device),
                          reinterpret_cast<SDL_GPUTexture *>(texture));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuReleaseBuffer)(JNIEnv *, jclass, jlong device, jlong buffer) {
    SDL_ReleaseGPUBuffer(reinterpret_cast<SDL_GPUDevice *>(device),
                         reinterpret_cast<SDL_GPUBuffer *>(buffer));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuReleaseSampler)(JNIEnv *, jclass, jlong device, jlong sampler) {
    SDL_ReleaseGPUSampler(reinterpret_cast<SDL_GPUDevice *>(device),
                          reinterpret_cast<SDL_GPUSampler *>(sampler));
}

SDLJNI_FUNC(void) SDLJNI_NAME(gpuReleaseFence)(JNIEnv *, jclass, jlong device, jlong fence) {
    SDL_ReleaseGPUFence(reinterpret_cast<SDL_GPUDevice *>(device),
                        reinterpret_cast<SDL_GPUFence *>(fence));
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(gpuQueryFence)(JNIEnv *, jclass, jlong device, jlong fence) {
    return SDL_QueryGPUFence(reinterpret_cast<SDL_GPUDevice *>(device),
                             reinterpret_cast<SDL_GPUFence *>(fence))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(gpuWaitForFences)(JNIEnv *env, jclass, jlong device,
                                                    jlongArray fences) {
    jsize count = env->GetArrayLength(fences);
    if (count <= 0) return JNI_TRUE;
    std::vector<jlong> fencesV;
    sdl_kmp_jni_read_long_array(env, fences, fencesV);
    std::vector<SDL_GPUFence *> fencePtrs(static_cast<size_t>(count));
    for (jsize i = 0; i < count; i++) {
        fencePtrs[static_cast<size_t>(i)] = reinterpret_cast<SDL_GPUFence *>(fencesV[static_cast<size_t>(i)]);
    }
    return SDL_WaitForGPUFences(reinterpret_cast<SDL_GPUDevice *>(device), true, fencePtrs.data(),
                                static_cast<Uint32>(count))
               ? JNI_TRUE
               : JNI_FALSE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(gpuUploadToBuffer)(JNIEnv *env, jclass, jlong device,
                                                     jlong buffer, jbyteArray data, jint offset) {
    jsize len = env->GetArrayLength(data);
    if (len <= 0) return JNI_TRUE;
    std::vector<jbyte> bufferData(static_cast<size_t>(len));
    env->GetByteArrayRegion(data, 0, len, bufferData.data());

    SDL_GPUDevice *dev = reinterpret_cast<SDL_GPUDevice *>(device);
    SDL_GPUTransferBufferCreateInfo info{};
    info.usage = SDL_GPU_TRANSFERBUFFERUSAGE_UPLOAD;
    info.size = static_cast<Uint32>(len);
    SDL_GPUTransferBuffer *transfer = SDL_CreateGPUTransferBuffer(dev, &info);
    if (transfer == nullptr) return JNI_FALSE;

    void *mapped = SDL_MapGPUTransferBuffer(dev, transfer, false);
    if (mapped == nullptr) {
        SDL_ReleaseGPUTransferBuffer(dev, transfer);
        return JNI_FALSE;
    }
    SDL_memcpy(mapped, bufferData.data(), static_cast<size_t>(len));
    SDL_UnmapGPUTransferBuffer(dev, transfer);

    SDL_GPUCommandBuffer *cmd = SDL_AcquireGPUCommandBuffer(dev);
    if (cmd == nullptr) {
        SDL_ReleaseGPUTransferBuffer(dev, transfer);
        return JNI_FALSE;
    }
    SDL_GPUCopyPass *pass = SDL_BeginGPUCopyPass(cmd);
    SDL_GPUTransferBufferLocation src{};
    src.transfer_buffer = transfer;
    src.offset = 0;
    SDL_GPUBufferRegion region{};
    region.buffer = reinterpret_cast<SDL_GPUBuffer *>(buffer);
    region.offset = static_cast<Uint32>(offset);
    region.size = static_cast<Uint32>(len);
    SDL_UploadToGPUBuffer(pass, &src, &region, false);
    SDL_EndGPUCopyPass(pass);
    SDL_SubmitGPUCommandBuffer(cmd);
    SDL_ReleaseGPUTransferBuffer(dev, transfer);
    return JNI_TRUE;
}

SDLJNI_FUNC(jboolean) SDLJNI_NAME(gpuUploadToTexture)(JNIEnv *env, jclass, jlong device,
                                                      jlong texture, jbyteArray data,
                                                      jint bytesPerRow, jint x, jint y, jint w,
                                                      jint h) {
    jsize len = env->GetArrayLength(data);
    if (len <= 0) return JNI_TRUE;
    std::vector<jbyte> bufferData(static_cast<size_t>(len));
    env->GetByteArrayRegion(data, 0, len, bufferData.data());

    SDL_GPUDevice *dev = reinterpret_cast<SDL_GPUDevice *>(device);
    SDL_GPUTransferBufferCreateInfo info{};
    info.usage = SDL_GPU_TRANSFERBUFFERUSAGE_UPLOAD;
    info.size = static_cast<Uint32>(len);
    SDL_GPUTransferBuffer *transfer = SDL_CreateGPUTransferBuffer(dev, &info);
    if (transfer == nullptr) return JNI_FALSE;

    void *mapped = SDL_MapGPUTransferBuffer(dev, transfer, false);
    if (mapped == nullptr) {
        SDL_ReleaseGPUTransferBuffer(dev, transfer);
        return JNI_FALSE;
    }
    SDL_memcpy(mapped, bufferData.data(), static_cast<size_t>(len));
    SDL_UnmapGPUTransferBuffer(dev, transfer);

    SDL_GPUCommandBuffer *cmd = SDL_AcquireGPUCommandBuffer(dev);
    if (cmd == nullptr) {
        SDL_ReleaseGPUTransferBuffer(dev, transfer);
        return JNI_FALSE;
    }
    SDL_GPUCopyPass *pass = SDL_BeginGPUCopyPass(cmd);
    SDL_GPUTextureTransferInfo src{};
    src.transfer_buffer = transfer;
    src.offset = 0;
    src.pixels_per_row = bytesPerRow;
    SDL_GPUTextureRegion region{};
    region.texture = reinterpret_cast<SDL_GPUTexture *>(texture);
    region.mip_level = 0;
    region.layer = 0;
    region.x = static_cast<Uint32>(x);
    region.y = static_cast<Uint32>(y);
    region.z = 0;
    region.w = static_cast<Uint32>(w);
    region.h = static_cast<Uint32>(h);
    region.d = 1;
    SDL_UploadToGPUTexture(pass, &src, &region, false);
    SDL_EndGPUCopyPass(pass);
    SDL_SubmitGPUCommandBuffer(cmd);
    SDL_ReleaseGPUTransferBuffer(dev, transfer);
    return JNI_TRUE;
}

// Uploads into the buffer with a copy pass inside the GIVEN command buffer
// (the command buffer is not submitted; the caller submits it later). This
// is what SDLGPUCommandBuffer.uploadToBuffer uses.
SDLJNI_FUNC(jboolean) SDLJNI_NAME(gpuUploadToBufferInCmd)(JNIEnv *env, jclass, jlong device,
                                                          jlong commandBuffer, jlong buffer,
                                                          jbyteArray data, jint offset) {
    jsize len = env->GetArrayLength(data);
    if (len <= 0) return JNI_TRUE;
    std::vector<jbyte> bufferData(static_cast<size_t>(len));
    env->GetByteArrayRegion(data, 0, len, bufferData.data());

    SDL_GPUCommandBuffer *cmd = reinterpret_cast<SDL_GPUCommandBuffer *>(commandBuffer);
    SDL_GPUDevice *dev = reinterpret_cast<SDL_GPUDevice *>(device);
    SDL_GPUTransferBufferCreateInfo info{};
    info.usage = SDL_GPU_TRANSFERBUFFERUSAGE_UPLOAD;
    info.size = static_cast<Uint32>(len);
    SDL_GPUTransferBuffer *transfer = SDL_CreateGPUTransferBuffer(dev, &info);
    if (transfer == nullptr) return JNI_FALSE;

    void *mapped = SDL_MapGPUTransferBuffer(dev, transfer, false);
    if (mapped == nullptr) {
        SDL_ReleaseGPUTransferBuffer(dev, transfer);
        return JNI_FALSE;
    }
    SDL_memcpy(mapped, bufferData.data(), static_cast<size_t>(len));
    SDL_UnmapGPUTransferBuffer(dev, transfer);

    SDL_GPUCopyPass *pass = SDL_BeginGPUCopyPass(cmd);
    SDL_GPUTransferBufferLocation src{};
    src.transfer_buffer = transfer;
    src.offset = 0;
    SDL_GPUBufferRegion region{};
    region.buffer = reinterpret_cast<SDL_GPUBuffer *>(buffer);
    region.offset = static_cast<Uint32>(offset);
    region.size = static_cast<Uint32>(len);
    SDL_UploadToGPUBuffer(pass, &src, &region, false);
    SDL_EndGPUCopyPass(pass);
    SDL_ReleaseGPUTransferBuffer(dev, transfer);
    return JNI_TRUE;
}

// Downloads a texture region to the CPU (RGBA8), blocking on a fence.
SDLJNI_FUNC(jbyteArray) SDLJNI_NAME(gpuDownloadFromTexture)(JNIEnv *env, jclass, jlong device,
                                                            jlong texture, jint w, jint h) {
    int size = w * h * 4;
    if (size <= 0) return nullptr;
    SDL_GPUDevice *dev = reinterpret_cast<SDL_GPUDevice *>(device);
    SDL_GPUTransferBufferCreateInfo info{};
    info.usage = SDL_GPU_TRANSFERBUFFERUSAGE_DOWNLOAD;
    info.size = static_cast<Uint32>(size);
    SDL_GPUTransferBuffer *transfer = SDL_CreateGPUTransferBuffer(dev, &info);
    if (transfer == nullptr) return nullptr;

    SDL_GPUCommandBuffer *cmd = SDL_AcquireGPUCommandBuffer(dev);
    if (cmd == nullptr) {
        SDL_ReleaseGPUTransferBuffer(dev, transfer);
        return nullptr;
    }
    SDL_GPUCopyPass *pass = SDL_BeginGPUCopyPass(cmd);
    SDL_GPUTextureRegion region{};
    region.texture = reinterpret_cast<SDL_GPUTexture *>(texture);
    region.mip_level = 0;
    region.layer = 0;
    region.x = 0;
    region.y = 0;
    region.z = 0;
    region.w = static_cast<Uint32>(w);
    region.h = static_cast<Uint32>(h);
    region.d = 1;
    SDL_GPUTextureTransferInfo dst{};
    dst.transfer_buffer = transfer;
    dst.offset = 0;
    dst.pixels_per_row = w;
    SDL_DownloadFromGPUTexture(pass, &region, &dst);
    SDL_EndGPUCopyPass(pass);
    SDL_GPUFence *fence = SDL_SubmitGPUCommandBufferAndAcquireFence(cmd);
    if (fence == nullptr) {
        SDL_ReleaseGPUTransferBuffer(dev, transfer);
        return nullptr;
    }
    SDL_WaitForGPUFences(dev, true, &fence, 1);
    SDL_ReleaseGPUFence(dev, fence);

    void *mapped = SDL_MapGPUTransferBuffer(dev, transfer, false);
    jbyteArray result = nullptr;
    if (mapped != nullptr) {
        result = sdl_kmp_jni_new_byte_array(env, mapped, size);
        SDL_UnmapGPUTransferBuffer(dev, transfer);
    }
    SDL_ReleaseGPUTransferBuffer(dev, transfer);
    return result;
}

// Returns [texture, width, height] or null.
SDLJNI_FUNC(jlongArray) SDLJNI_NAME(gpuWaitAndAcquireSwapchainTexture)(JNIEnv *env, jclass,
                                                                        jlong commandBuffer,
                                                                        jlong window) {
    SDL_GPUTexture *texture = nullptr;
    Uint32 width = 0;
    Uint32 height = 0;
    if (!SDL_WaitAndAcquireGPUSwapchainTexture(reinterpret_cast<SDL_GPUCommandBuffer *>(commandBuffer),
                                               reinterpret_cast<SDL_Window *>(window), &texture,
                                               &width, &height)) {
        return nullptr;
    }
    return sdl_kmp_jni_new_long_array(
        env, {reinterpret_cast<jlong>(texture), static_cast<jlong>(width),
              static_cast<jlong>(height)});
}

// Waits until the swapchain is ready for the next frame. The frame itself is
// presented by SDL_SubmitGPUCommandBuffer (the swapchain texture is presented
// when its command buffer completes), so this is the only synchronization
// present() needs - acquiring a NEW texture here would present an empty frame.
SDLJNI_FUNC(jboolean) SDLJNI_NAME(gpuWaitForGPUSwapchain)(JNIEnv *, jclass, jlong device,
                                                         jlong window) {
    return SDL_WaitForGPUSwapchain(reinterpret_cast<SDL_GPUDevice *>(device),
                                   reinterpret_cast<SDL_Window *>(window))
               ? JNI_TRUE
               : JNI_FALSE;
}
