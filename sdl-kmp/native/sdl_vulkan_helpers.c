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
 * Vulkan surface helpers, all platforms.
 *
 * SDL3's VkSurfaceKHR is a pointer to VkSurfaceKHR_T on 64-bit targets but
 * falls back to a plain uint64_t on 32-bit targets that lack the Vulkan
 * headers (e.g. Kotlin/Native's 32-bit Android sysroot). That splits the
 * cinterop binding for SDL_Vulkan_CreateSurface/DestroySurface across ABIs,
 * so these wrappers expose a stable signature (void* + uint64_t) to Kotlin.
 */

#include <stdbool.h>
#include <stdint.h>
#include <SDL3/SDL.h>
#include <SDL3/SDL_vulkan.h>

bool SDL_kmp_VulkanCreateSurface(void *window, void *instance, uint64_t *surface)
{
    VkSurfaceKHR s = 0;
    if (!SDL_Vulkan_CreateSurface((SDL_Window *)window, (VkInstance)instance, NULL, &s)) {
        return false;
    }
    *surface = (uint64_t)s;
    return true;
}

void SDL_kmp_VulkanDestroySurface(void *instance, uint64_t surface)
{
    SDL_Vulkan_DestroySurface((VkInstance)instance, (VkSurfaceKHR)surface, NULL);
}

bool SDL_kmp_VulkanGetPresentationSupport(void *instance, void *physicalDevice, uint32_t queueFamilyIndex)
{
    return SDL_Vulkan_GetPresentationSupport((VkInstance)instance, (VkPhysicalDevice)physicalDevice, queueFamilyIndex);
}
