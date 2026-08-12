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
 * OpenGL / Vulkan minimal triangle demos for the example-gpu module.
 *
 * Implemented in C because the GL/Vulkan entry points are C-function-pointer
 * tables that cinterop cannot bind directly; Kotlin passes the SPIR-V bytes
 * for the Vulkan shaders. Compiled into a static library by native/CMakeLists.txt
 * and embedded into the example-gpu cinterop klib.
 */

#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <SDL3/SDL.h>
#include <SDL3/SDL_vulkan.h>

void *SDL_kmp_GLTriangleInit(void);
void *SDL_kmp_VulkanTriangleInit(SDL_Window *window,
                                 const unsigned char *vert, int vert_size,
                                 const unsigned char *frag, int frag_size);
void SDL_kmp_VulkanTriangleDestroy(void *handle);

/* =========================================================================
 * OpenGL triangle demo helper.
 *
 * Loads the GL 3.3 core / GLES 3 functions through SDL_GL_GetProcAddress
 * (cinterop cannot express these C function-pointer types), compiles a
 * triangle shader pair and draws it on every SDL_kmp_GLTriangleRender
 * call. The GL context itself is created by the caller through the sdl-kmp
 * bindings.
 * ========================================================================= */

#if defined(SDL_PLATFORM_ANDROID)
#include <SDL3/SDL_opengles2.h>
#else
#include <SDL3/SDL_opengl.h>
#endif

typedef void (*GLfn)(void);

typedef struct GLTriangleCtx {
    GLuint program;
    GLuint vao;
    int has_vao;
} GLTriangleCtx;

/* function pointers */
static void (SDLCALL *kmp_glGenBuffers)(GLsizei, GLuint *);
static void (SDLCALL *kmp_glBindBuffer)(GLenum, GLuint);
static void (SDLCALL *kmp_glBufferData)(GLenum, GLsizeiptr, const void *, GLenum);
static GLuint (SDLCALL *kmp_glCreateShader)(GLenum);
static void (SDLCALL *kmp_glShaderSource)(GLuint, GLsizei, const char **, const int *);
static void (SDLCALL *kmp_glCompileShader)(GLuint);
static void (SDLCALL *kmp_glGetShaderiv)(GLuint, GLenum, GLint *);
static void (SDLCALL *kmp_glGetShaderInfoLog)(GLuint, GLsizei, GLsizei *, char *);
static GLuint (SDLCALL *kmp_glCreateProgram)(void);
static void (SDLCALL *kmp_glAttachShader)(GLuint, GLuint);
static void (SDLCALL *kmp_glLinkProgram)(GLuint);
static void (SDLCALL *kmp_glGetProgramiv)(GLuint, GLenum, GLint *);
static void (SDLCALL *kmp_glGetProgramInfoLog)(GLuint, GLsizei, GLsizei *, char *);
static void (SDLCALL *kmp_glUseProgram)(GLuint);
static void (SDLCALL *kmp_glGenVertexArrays)(GLsizei, GLuint *);
static void (SDLCALL *kmp_glBindVertexArray)(GLuint);
static void (SDLCALL *kmp_glViewport)(GLint, GLint, GLsizei, GLsizei);
static void (SDLCALL *kmp_glClear)(GLbitfield);
static void (SDLCALL *kmp_glClearColor)(GLfloat, GLfloat, GLfloat, GLfloat);
static void (SDLCALL *kmp_glDrawArrays)(GLenum, GLint, GLsizei);
static void (SDLCALL *kmp_glDeleteProgram)(GLuint);
static void (SDLCALL *kmp_glDeleteShader)(GLuint);
static void (SDLCALL *kmp_glDeleteVertexArrays)(GLsizei, const GLuint *);

static GLfn kmp_glGet(const char *name)
{
    return (GLfn)SDL_GL_GetProcAddress(name);
}

static GLuint kmp_gl_compile(GLenum type, const char *source)
{
    GLuint shader = kmp_glCreateShader(type);
    kmp_glShaderSource(shader, 1, &source, NULL);
    kmp_glCompileShader(shader);
    GLint ok = 0;
    kmp_glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if (!ok) {
        char log[1024];
        kmp_glGetShaderInfoLog(shader, sizeof(log), NULL, log);
        SDL_SetError("shader compile failed: %s", log);
        kmp_glDeleteShader(shader);
        return 0;
    }
    return shader;
}

void *SDL_kmp_GLTriangleInit(void)
{
    kmp_glGenBuffers = (void *)kmp_glGet("glGenBuffers");
    kmp_glBindBuffer = (void *)kmp_glGet("glBindBuffer");
    kmp_glBufferData = (void *)kmp_glGet("glBufferData");
    kmp_glCreateShader = (void *)kmp_glGet("glCreateShader");
    kmp_glShaderSource = (void *)kmp_glGet("glShaderSource");
    kmp_glCompileShader = (void *)kmp_glGet("glCompileShader");
    kmp_glGetShaderiv = (void *)kmp_glGet("glGetShaderiv");
    kmp_glGetShaderInfoLog = (void *)kmp_glGet("glGetShaderInfoLog");
    kmp_glCreateProgram = (void *)kmp_glGet("glCreateProgram");
    kmp_glAttachShader = (void *)kmp_glGet("glAttachShader");
    kmp_glLinkProgram = (void *)kmp_glGet("glLinkProgram");
    kmp_glGetProgramiv = (void *)kmp_glGet("glGetProgramiv");
    kmp_glGetProgramInfoLog = (void *)kmp_glGet("glGetProgramInfoLog");
    kmp_glUseProgram = (void *)kmp_glGet("glUseProgram");
    kmp_glGenVertexArrays = (void *)kmp_glGet("glGenVertexArrays");
    kmp_glBindVertexArray = (void *)kmp_glGet("glBindVertexArray");
    kmp_glViewport = (void *)kmp_glGet("glViewport");
    kmp_glClear = (void *)kmp_glGet("glClear");
    kmp_glClearColor = (void *)kmp_glGet("glClearColor");
    kmp_glDrawArrays = (void *)kmp_glGet("glDrawArrays");
    kmp_glDeleteProgram = (void *)kmp_glGet("glDeleteProgram");
    kmp_glDeleteShader = (void *)kmp_glGet("glDeleteShader");
    kmp_glDeleteVertexArrays = (void *)kmp_glGet("glDeleteVertexArrays");

    if (!kmp_glCreateShader || !kmp_glCreateProgram || !kmp_glDrawArrays || !kmp_glClear) {
        SDL_SetError("GL3 functions not available");
        return NULL;
    }

    int profile = 0;
    SDL_GL_GetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, &profile);
    int is_es = (profile == SDL_GL_CONTEXT_PROFILE_ES);

    const char *vs_src = is_es
        ? "#version 300 es\nout vec3 vColor;void main(){vec2 p[3]=vec2[3](vec2(-0.75,-0.75),vec2(0.75,-0.75),vec2(0.0,0.75));vec3 c[3]=vec3[3](vec3(1.0,0.25,0.35),vec3(0.25,1.0,0.35),vec3(0.25,0.35,1.0));gl_Position=vec4(p[gl_VertexID],0.0,1.0);vColor=c[gl_VertexID];}\n"
        : "#version 330 core\nout vec3 vColor;void main(){vec2 p[3]=vec2[3](vec2(-0.75,-0.75),vec2(0.75,-0.75),vec2(0.0,0.75));vec3 c[3]=vec3[3](vec3(1.0,0.25,0.35),vec3(0.25,1.0,0.35),vec3(0.25,0.35,1.0));gl_Position=vec4(p[gl_VertexID],0.0,1.0);vColor=c[gl_VertexID];}\n";
    const char *fs_src = is_es
        ? "#version 300 es\nprecision mediump float;in vec3 vColor;layout(location=0)out vec4 c;void main(){c=vec4(vColor,1.0);}\n"
        : "#version 330 core\nin vec3 vColor;out vec4 c;void main(){c=vec4(vColor,1.0);}\n";

    GLuint vs = kmp_gl_compile(GL_VERTEX_SHADER, vs_src);
    GLuint fs = kmp_gl_compile(GL_FRAGMENT_SHADER, fs_src);
    if (!vs || !fs) {
        if (vs) kmp_glDeleteShader(vs);
        if (fs) kmp_glDeleteShader(fs);
        return NULL;
    }

    GLuint program = kmp_glCreateProgram();
    kmp_glAttachShader(program, vs);
    kmp_glAttachShader(program, fs);
    kmp_glLinkProgram(program);
    kmp_glDeleteShader(vs);
    kmp_glDeleteShader(fs);
    GLint ok = 0;
    kmp_glGetProgramiv(program, GL_LINK_STATUS, &ok);
    if (!ok) {
        char log[1024];
        kmp_glGetProgramInfoLog(program, sizeof(log), NULL, log);
        SDL_SetError("program link failed: %s", log);
        kmp_glDeleteProgram(program);
        return NULL;
    }

    GLTriangleCtx *ctx = (GLTriangleCtx *)SDL_calloc(1, sizeof(GLTriangleCtx));
    if (!ctx) {
        kmp_glDeleteProgram(program);
        return NULL;
    }
    ctx->program = program;
    ctx->has_vao = (kmp_glGenVertexArrays && kmp_glBindVertexArray);
    if (ctx->has_vao) {
        kmp_glGenVertexArrays(1, &ctx->vao);
        kmp_glBindVertexArray(ctx->vao);
    }
    return ctx;
}

bool SDL_kmp_GLTriangleRender(void *handle, int width, int height)
{
    GLTriangleCtx *ctx = (GLTriangleCtx *)handle;
    if (!ctx) return false;
    kmp_glViewport(0, 0, width, height);
    kmp_glClearColor(0.07f, 0.07f, 0.09f, 1.0f);
    kmp_glClear(GL_COLOR_BUFFER_BIT);
    kmp_glUseProgram(ctx->program);
    if (ctx->has_vao) kmp_glBindVertexArray(ctx->vao);
    kmp_glDrawArrays(GL_TRIANGLES, 0, 3);
    return true;
}

void SDL_kmp_GLTriangleDestroy(void *handle)
{
    GLTriangleCtx *ctx = (GLTriangleCtx *)handle;
    if (!ctx) return;
    if (kmp_glDeleteProgram) kmp_glDeleteProgram(ctx->program);
    if (ctx->has_vao && kmp_glDeleteVertexArrays) kmp_glDeleteVertexArrays(1, &ctx->vao);
    SDL_free(ctx);
}

/* =========================================================================
 * Minimal Vulkan triangle demo helper.
 *
 * Builds the whole pipeline (instance -> surface -> device -> swapchain ->
 * render pass -> graphics pipeline -> command buffer) using only function
 * pointers obtained through SDL_Vulkan_GetVkGetInstanceProcAddr, and the
 * minimal Vulkan structures declared below (no Vulkan SDK headers needed).
 * Shader SPIR-V bytes are passed in from Kotlin.
 * ========================================================================= */

/* ---- minimal Vulkan types & constants (subset used below) ---- */

typedef uint32_t VkBool32;
typedef uint32_t VkFlags;
typedef uint32_t VkSampleCountFlagBits;
typedef uint32_t VkPipelineStageFlags;
typedef uint32_t VkShaderStageFlagBits;
typedef uint32_t VkColorComponentFlags;
typedef uint32_t VkFormat;
typedef uint32_t VkColorSpaceKHR;
typedef uint32_t VkPresentModeKHR;
typedef int VkResult;
typedef int VkAttachmentLoadOp;
typedef int VkAttachmentStoreOp;
typedef int VkImageLayout;
typedef int VkPipelineBindPoint;
typedef int VkPrimitiveTopology;
typedef int VkPolygonMode;
typedef int VkFrontFace;
typedef int VkBlendFactor;
typedef int VkLogicOp;
typedef int VkCommandBufferLevel;
typedef int VkSubpassContents;
typedef uint32_t VkCullModeFlags;
typedef uint32_t VkSampleMask;
typedef struct VkViewport { float x, y, width, height; float minDepth, maxDepth; } VkViewport;
typedef struct VkExtent3D { uint32_t width, height, depth; } VkExtent3D;

/* Remaining Vulkan handles (SDL_vulkan.h only defines VkInstance,
 * VkPhysicalDevice and VkSurfaceKHR). Dispatchable handles are always
 * pointers; non-dispatchable handles are pointers on 64-bit and uint64_t
 * on 32-bit targets. */
#define KMP_VK_HANDLE(object) typedef struct object##_T *object;
#if defined(__LP64__) || defined(_WIN64) || defined(__x86_64__) || defined(__aarch64__) || (defined(__riscv) && __riscv_xlen == 64)
#define KMP_VK_NON_DISPATCHABLE(object) typedef struct object##_T *object;
#else
#define KMP_VK_NON_DISPATCHABLE(object) typedef uint64_t object;
#endif
KMP_VK_HANDLE(VkDevice)
KMP_VK_HANDLE(VkQueue)
KMP_VK_HANDLE(VkCommandBuffer)
KMP_VK_NON_DISPATCHABLE(VkSemaphore)
KMP_VK_NON_DISPATCHABLE(VkFence)
KMP_VK_NON_DISPATCHABLE(VkDeviceMemory)
KMP_VK_NON_DISPATCHABLE(VkBuffer)
KMP_VK_NON_DISPATCHABLE(VkImage)
KMP_VK_NON_DISPATCHABLE(VkEvent)
KMP_VK_NON_DISPATCHABLE(VkQueryPool)
KMP_VK_NON_DISPATCHABLE(VkBufferView)
KMP_VK_NON_DISPATCHABLE(VkImageView)
KMP_VK_NON_DISPATCHABLE(VkShaderModule)
KMP_VK_NON_DISPATCHABLE(VkPipelineCache)
KMP_VK_NON_DISPATCHABLE(VkPipeline)
KMP_VK_NON_DISPATCHABLE(VkPipelineLayout)
KMP_VK_NON_DISPATCHABLE(VkRenderPass)
KMP_VK_NON_DISPATCHABLE(VkDescriptorSetLayout)
KMP_VK_NON_DISPATCHABLE(VkSampler)
KMP_VK_NON_DISPATCHABLE(VkDescriptorPool)
KMP_VK_NON_DISPATCHABLE(VkDescriptorSet)
KMP_VK_NON_DISPATCHABLE(VkFramebuffer)
KMP_VK_NON_DISPATCHABLE(VkCommandPool)
KMP_VK_NON_DISPATCHABLE(VkSwapchainKHR)
#undef KMP_VK_HANDLE
#undef KMP_VK_NON_DISPATCHABLE

#define VK_STRUCTURE_TYPE_APPLICATION_INFO 0
#define VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO 1
#define VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO 2
#define VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO 3
#define VK_STRUCTURE_TYPE_SUBMIT_INFO 4
#define VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO 5
#define VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO 6
#define VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO 8
#define VK_STRUCTURE_TYPE_FENCE_CREATE_INFO 10
#define VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO 11
#define VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO 12
#define VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO 18
#define VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO 19
#define VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO 20
#define VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO 23
#define VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO 24
#define VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO 25
#define VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO 29
#define VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO 31
#define VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO 33
#define VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO 38
#define VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO 40
#define VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO 42
#define VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO 43
#define VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR 1000001000
#define VK_STRUCTURE_TYPE_PRESENT_INFO_KHR 1000001001

#define VK_SUCCESS 0
#define VK_PIPELINE_BIND_POINT_GRAPHICS 0
#define VK_SUBPASS_CONTENTS_INLINE 0
#define VK_COMMAND_BUFFER_LEVEL_PRIMARY 0
#define VK_IMAGE_VIEW_TYPE_2D 1
#define VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT 0x100
#define VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT 0x10
#define VK_IMAGE_LAYOUT_UNDEFINED 0
#define VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL 2
#define VK_IMAGE_LAYOUT_PRESENT_SRC_KHR 2
#define VK_ATTACHMENT_LOAD_OP_CLEAR 1
#define VK_ATTACHMENT_LOAD_OP_DONT_CARE 2
#define VK_ATTACHMENT_STORE_OP_STORE 0
#define VK_ATTACHMENT_STORE_OP_DONT_CARE 3
#define VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT 0x10
#define VK_SHARING_MODE_EXCLUSIVE 0
#define VK_SAMPLE_COUNT_1_BIT 1
#define VK_POLYGON_MODE_FILL 0
#define VK_CULL_MODE_NONE 0
#define VK_FRONT_FACE_COUNTER_CLOCKWISE 0
#define VK_COLOR_SPACE_SRGB_NONLINEAR_KHR 0
#define VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR 0x1
#define VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR 0x1
#define VK_PRESENT_MODE_FIFO_KHR 2
#define VK_SHADER_STAGE_VERTEX_BIT 0x1
#define VK_SHADER_STAGE_FRAGMENT_BIT 0x10
#define VK_QUEUE_GRAPHICS_BIT 0x1
#define VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST 0
#define VK_FORMAT_R8G8B8A8_UNORM 37
#define VK_FORMAT_B8G8R8A8_UNORM 44
#define VK_API_VERSION_1_0 0x00401000
#define VK_COLOR_COMPONENT_R_BIT 0x1
#define VK_COLOR_COMPONENT_G_BIT 0x2
#define VK_COLOR_COMPONENT_B_BIT 0x4
#define VK_COLOR_COMPONENT_A_BIT 0x8
#define VK_BLEND_FACTOR_ONE 1
#define VK_BLEND_FACTOR_ZERO 0
#define VK_LOGIC_OP_COPY 3

typedef struct VkExtent2D { uint32_t width, height; } VkExtent2D;
typedef struct VkOffset2D { int32_t x, y; } VkOffset2D;
typedef struct VkRect2D { VkOffset2D offset; VkExtent2D extent; } VkRect2D;
typedef struct VkApplicationInfo {
    uint32_t sType; const void *pNext; const char *pApplicationName;
    uint32_t applicationVersion; const char *pEngineName; uint32_t engineVersion; uint32_t apiVersion;
} VkApplicationInfo;
typedef struct VkInstanceCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; const VkApplicationInfo *pApplicationInfo;
    uint32_t enabledLayerCount; const char *const *ppEnabledLayerNames;
    uint32_t enabledExtensionCount; const char *const *ppEnabledExtensionNames;
} VkInstanceCreateInfo;
typedef struct VkDeviceQueueCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; uint32_t queueFamilyIndex;
    uint32_t queueCount; const float *pQueuePriorities;
} VkDeviceQueueCreateInfo;
typedef struct VkDeviceCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags;
    uint32_t queueCreateInfoCount; const VkDeviceQueueCreateInfo *pQueueCreateInfos;
    uint32_t enabledLayerCount; const char *const *ppEnabledLayerNames;
    uint32_t enabledExtensionCount; const char *const *ppEnabledExtensionNames;
    const void *pEnabledFeatures;
} VkDeviceCreateInfo;
typedef struct VkSwapchainCreateInfoKHR {
    uint32_t sType; const void *pNext; VkFlags flags; VkSurfaceKHR surface;
    uint32_t minImageCount; VkFormat imageFormat; VkColorSpaceKHR imageColorSpace;
    VkExtent2D imageExtent; uint32_t imageArrayLayers; VkFlags imageUsage;
    uint32_t imageSharingMode; uint32_t queueFamilyIndexCount; const uint32_t *pQueueFamilyIndices;
    VkFlags preTransform; VkFlags compositeAlpha; VkPresentModeKHR presentMode;
    VkBool32 clipped; VkSwapchainKHR oldSwapchain;
} VkSwapchainCreateInfoKHR;
typedef struct VkComponentMapping { VkFormat r, g, b, a; } VkComponentMapping;
typedef struct VkImageSubresourceRange { VkFlags aspectMask; uint32_t baseMipLevel, levelCount, baseArrayLayer, layerCount; } VkImageSubresourceRange;
typedef struct VkImageViewCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; VkImage image; int viewType;
    VkFormat format; VkComponentMapping components; VkImageSubresourceRange subresourceRange;
} VkImageViewCreateInfo;
typedef struct VkAttachmentDescription {
    VkFlags flags; VkFormat format; VkSampleCountFlagBits samples;
    VkAttachmentLoadOp loadOp; VkAttachmentStoreOp storeOp;
    VkAttachmentLoadOp stencilLoadOp; VkAttachmentStoreOp stencilStoreOp;
    VkImageLayout initialLayout; VkImageLayout finalLayout;
} VkAttachmentDescription;
typedef struct VkAttachmentReference { uint32_t attachment; VkImageLayout layout; } VkAttachmentReference;
typedef struct VkSubpassDescription {
    VkFlags flags; VkPipelineBindPoint pipelineBindPoint; uint32_t inputAttachmentCount;
    const VkAttachmentReference *pInputAttachments; uint32_t colorAttachmentCount;
    const VkAttachmentReference *pColorAttachments; const VkAttachmentReference *pResolveAttachments;
    const VkAttachmentReference *pDepthStencilAttachment; uint32_t preserveAttachmentCount;
    const uint32_t *pPreserveAttachments;
} VkSubpassDescription;
typedef struct VkRenderPassCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; uint32_t attachmentCount;
    const VkAttachmentDescription *pAttachments; uint32_t subpassCount;
    const VkSubpassDescription *pSubpasses; uint32_t dependencyCount; const void *pDependencies;
} VkRenderPassCreateInfo;
typedef struct VkShaderModuleCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; size_t codeSize; const uint32_t *pCode;
} VkShaderModuleCreateInfo;
typedef struct VkPipelineShaderStageCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; VkShaderStageFlagBits stage;
    VkShaderModule module; const char *pName; const void *pSpecializationInfo;
} VkPipelineShaderStageCreateInfo;
typedef struct VkPipelineVertexInputStateCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; uint32_t vertexBindingDescriptionCount;
    const void *pVertexBindingDescriptions; uint32_t vertexAttributeDescriptionCount; const void *pVertexAttributeDescriptions;
} VkPipelineVertexInputStateCreateInfo;
typedef struct VkPipelineInputAssemblyStateCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; VkPrimitiveTopology topology; VkBool32 primitiveRestartEnable;
} VkPipelineInputAssemblyStateCreateInfo;
typedef struct VkPipelineViewportStateCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; uint32_t viewportCount; const VkViewport *pViewports;
    uint32_t scissorCount; const VkRect2D *pScissors;
} VkPipelineViewportStateCreateInfo;
typedef struct VkPipelineRasterizationStateCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; VkBool32 depthClampEnable; VkBool32 rasterizerDiscardEnable;
    VkPolygonMode polygonMode; VkCullModeFlags cullMode; VkFrontFace frontFace; VkBool32 depthBiasEnable;
    float depthBiasConstantFactor, depthBiasClamp, depthBiasSlopeFactor; float lineWidth;
} VkPipelineRasterizationStateCreateInfo;
typedef struct VkPipelineMultisampleStateCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; VkSampleCountFlagBits rasterizationSamples;
    VkBool32 sampleShadingEnable; float minSampleShading; const VkSampleMask *pSampleMask;
    VkBool32 alphaToCoverageEnable, alphaToOneEnable;
} VkPipelineMultisampleStateCreateInfo;
typedef struct VkPipelineColorBlendAttachmentState {
    VkBool32 blendEnable; VkBlendFactor srcColorBlendFactor, dstColorBlendFactor, colorBlendOp;
    VkBlendFactor srcAlphaBlendFactor, dstAlphaBlendFactor, alphaBlendOp; VkColorComponentFlags colorWriteMask;
} VkPipelineColorBlendAttachmentState;
typedef struct VkPipelineColorBlendStateCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; VkBool32 logicOpEnable; VkLogicOp logicOp;
    uint32_t attachmentCount; const VkPipelineColorBlendAttachmentState *pAttachments;
    float blendConstants[4];
} VkPipelineColorBlendStateCreateInfo;
typedef struct VkPipelineLayoutCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; uint32_t setLayoutCount; const void *pSetLayouts;
    uint32_t pushConstantRangeCount; const void *pPushConstantRanges;
} VkPipelineLayoutCreateInfo;
typedef struct VkGraphicsPipelineCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; uint32_t stageCount; const VkPipelineShaderStageCreateInfo *pStages;
    const VkPipelineVertexInputStateCreateInfo *pVertexInputState;
    const VkPipelineInputAssemblyStateCreateInfo *pInputAssemblyState; const void *pTessellationState;
    const VkPipelineViewportStateCreateInfo *pViewportState; const VkPipelineRasterizationStateCreateInfo *pRasterizationState;
    const VkPipelineMultisampleStateCreateInfo *pMultisampleState; const void *pDepthStencilState;
    const VkPipelineColorBlendStateCreateInfo *pColorBlendState; const void *pDynamicState;
    VkPipelineLayout layout; VkRenderPass renderPass; uint32_t subpass;
    VkPipeline basePipelineHandle; int32_t basePipelineIndex;
} VkGraphicsPipelineCreateInfo;
typedef struct VkFramebufferCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; VkRenderPass renderPass;
    uint32_t attachmentCount; const VkImageView *pAttachments; uint32_t width, height, layers;
} VkFramebufferCreateInfo;
typedef struct VkCommandPoolCreateInfo {
    uint32_t sType; const void *pNext; VkFlags flags; uint32_t queueFamilyIndex;
} VkCommandPoolCreateInfo;
typedef struct VkCommandBufferAllocateInfo {
    uint32_t sType; const void *pNext; VkCommandPool commandPool; VkCommandBufferLevel level; uint32_t commandBufferCount;
} VkCommandBufferAllocateInfo;
typedef struct VkCommandBufferBeginInfo {
    uint32_t sType; const void *pNext; VkFlags flags; const void *pInheritanceInfo;
} VkCommandBufferBeginInfo;
typedef struct VkClearColorValue { float float32[4]; } VkClearColorValue;
typedef struct VkClearValue { VkClearColorValue color; } VkClearValue;
typedef struct VkRenderPassBeginInfo {
    uint32_t sType; const void *pNext; VkRenderPass renderPass; VkFramebuffer framebuffer;
    VkRect2D renderArea; uint32_t clearValueCount; const VkClearValue *pClearValues;
} VkRenderPassBeginInfo;
typedef struct VkSubmitInfo {
    uint32_t sType; const void *pNext; uint32_t waitSemaphoreCount; const VkSemaphore *pWaitSemaphores;
    const VkPipelineStageFlags *pWaitDstStageMask; uint32_t commandBufferCount; const VkCommandBuffer *pCommandBuffers;
    uint32_t signalSemaphoreCount; const VkSemaphore *pSignalSemaphores;
} VkSubmitInfo;
typedef struct VkPresentInfoKHR {
    uint32_t sType; const void *pNext; uint32_t waitSemaphoreCount; const VkSemaphore *pWaitSemaphores;
    uint32_t swapchainCount; const VkSwapchainKHR *pSwapchains; const uint32_t *pImageIndices; VkResult *pResults;
} VkPresentInfoKHR;
typedef struct VkSurfaceCapabilitiesKHR {
    uint32_t minImageCount, maxImageCount; VkExtent2D currentExtent, minImageExtent, maxImageExtent;
    uint32_t maxImageArrayLayers; VkFlags supportedTransforms, currentTransform;
    VkFlags supportedCompositeAlpha; VkFlags supportedUsageFlags;
} VkSurfaceCapabilitiesKHR;
typedef struct VkSurfaceFormatKHR { VkFormat format; VkColorSpaceKHR colorSpace; } VkSurfaceFormatKHR;
typedef struct VkQueueFamilyProperties {
    VkFlags queueFlags; uint32_t queueCount; uint32_t timestampValidBits;
    VkExtent3D minImageTransferGranularity;
} VkQueueFamilyProperties;
typedef struct VkFenceCreateInfo { uint32_t sType; const void *pNext; VkFlags flags; } VkFenceCreateInfo;
typedef struct VkSemaphoreCreateInfo { uint32_t sType; const void *pNext; VkFlags flags; } VkSemaphoreCreateInfo;

/* ---- Vulkan function pointers ---- */

typedef VkResult (SDLCALL *PFN_vkCreateInstance)(const VkInstanceCreateInfo *, const void *, VkInstance *);
typedef void (SDLCALL *PFN_vkDestroyInstance)(VkInstance, const void *);
typedef VkResult (SDLCALL *PFN_vkEnumerateInstanceExtensionProperties)(const char *, uint32_t *, void *);
typedef VkResult (SDLCALL *PFN_vkEnumeratePhysicalDevices)(VkInstance, uint32_t *, VkPhysicalDevice *);
typedef void (SDLCALL *PFN_vkGetPhysicalDeviceProperties)(VkPhysicalDevice, void *);
typedef void (SDLCALL *PFN_vkGetPhysicalDeviceQueueFamilyProperties)(VkPhysicalDevice, uint32_t *, void *);
typedef VkResult (SDLCALL *PFN_vkGetPhysicalDeviceSurfaceSupportKHR)(VkPhysicalDevice, uint32_t, VkSurfaceKHR, VkBool32 *);
typedef VkResult (SDLCALL *PFN_vkGetPhysicalDeviceSurfaceCapabilitiesKHR)(VkPhysicalDevice, VkSurfaceKHR, VkSurfaceCapabilitiesKHR *);
typedef VkResult (SDLCALL *PFN_vkGetPhysicalDeviceSurfaceFormatsKHR)(VkPhysicalDevice, VkSurfaceKHR, uint32_t *, VkSurfaceFormatKHR *);
typedef VkResult (SDLCALL *PFN_vkGetPhysicalDeviceSurfacePresentModesKHR)(VkPhysicalDevice, VkSurfaceKHR, uint32_t *, VkPresentModeKHR *);
typedef VkResult (SDLCALL *PFN_vkCreateDevice)(VkPhysicalDevice, const VkDeviceCreateInfo *, const void *, VkDevice *);
typedef void (SDLCALL *PFN_vkDestroyDevice)(VkDevice, const void *);
typedef void (SDLCALL *PFN_vkGetDeviceQueue)(VkDevice, uint32_t, uint32_t, VkQueue *);
typedef VkResult (SDLCALL *PFN_vkCreateSwapchainKHR)(VkDevice, const VkSwapchainCreateInfoKHR *, const void *, VkSwapchainKHR *);
typedef void (SDLCALL *PFN_vkDestroySwapchainKHR)(VkDevice, VkSwapchainKHR, const void *);
typedef VkResult (SDLCALL *PFN_vkGetSwapchainImagesKHR)(VkDevice, VkSwapchainKHR, uint32_t *, VkImage *);
typedef VkResult (SDLCALL *PFN_vkAcquireNextImageKHR)(VkDevice, VkSwapchainKHR, uint64_t, VkSemaphore, VkFence, uint32_t *);
typedef VkResult (SDLCALL *PFN_vkQueuePresentKHR)(VkQueue, const VkPresentInfoKHR *);
typedef VkResult (SDLCALL *PFN_vkCreateImageView)(VkDevice, const VkImageViewCreateInfo *, const void *, VkImageView *);
typedef void (SDLCALL *PFN_vkDestroyImageView)(VkDevice, VkImageView, const void *);
typedef VkResult (SDLCALL *PFN_vkCreateRenderPass)(VkDevice, const VkRenderPassCreateInfo *, const void *, VkRenderPass *);
typedef void (SDLCALL *PFN_vkDestroyRenderPass)(VkDevice, VkRenderPass, const void *);
typedef VkResult (SDLCALL *PFN_vkCreateFramebuffer)(VkDevice, const VkFramebufferCreateInfo *, const void *, VkFramebuffer *);
typedef void (SDLCALL *PFN_vkDestroyFramebuffer)(VkDevice, VkFramebuffer, const void *);
typedef VkResult (SDLCALL *PFN_vkCreateShaderModule)(VkDevice, const VkShaderModuleCreateInfo *, const void *, VkShaderModule *);
typedef void (SDLCALL *PFN_vkDestroyShaderModule)(VkDevice, VkShaderModule, const void *);
typedef VkResult (SDLCALL *PFN_vkCreatePipelineLayout)(VkDevice, const VkPipelineLayoutCreateInfo *, const void *, VkPipelineLayout *);
typedef void (SDLCALL *PFN_vkDestroyPipelineLayout)(VkDevice, VkPipelineLayout, const void *);
typedef VkResult (SDLCALL *PFN_vkCreateGraphicsPipelines)(VkDevice, VkPipelineCache, uint32_t, const VkGraphicsPipelineCreateInfo *, const void *, VkPipeline *);
typedef void (SDLCALL *PFN_vkDestroyPipeline)(VkDevice, VkPipeline, const void *);
typedef VkResult (SDLCALL *PFN_vkCreateCommandPool)(VkDevice, const VkCommandPoolCreateInfo *, const void *, VkCommandPool *);
typedef void (SDLCALL *PFN_vkDestroyCommandPool)(VkDevice, VkCommandPool, const void *);
typedef VkResult (SDLCALL *PFN_vkAllocateCommandBuffers)(VkDevice, const VkCommandBufferAllocateInfo *, VkCommandBuffer *);
typedef void (SDLCALL *PFN_vkFreeCommandBuffers)(VkDevice, VkCommandPool, uint32_t, const VkCommandBuffer *);
typedef VkResult (SDLCALL *PFN_vkBeginCommandBuffer)(VkCommandBuffer, const VkCommandBufferBeginInfo *);
typedef VkResult (SDLCALL *PFN_vkResetCommandBuffer)(VkCommandBuffer, VkFlags);
typedef VkResult (SDLCALL *PFN_vkEndCommandBuffer)(VkCommandBuffer);
typedef void (SDLCALL *PFN_vkCmdBeginRenderPass)(VkCommandBuffer, const VkRenderPassBeginInfo *, VkSubpassContents);
typedef void (SDLCALL *PFN_vkCmdEndRenderPass)(VkCommandBuffer);
typedef void (SDLCALL *PFN_vkCmdBindPipeline)(VkCommandBuffer, VkPipelineBindPoint, VkPipeline);
typedef void (SDLCALL *PFN_vkCmdSetViewport)(VkCommandBuffer, uint32_t, uint32_t, const VkViewport *);
typedef void (SDLCALL *PFN_vkCmdSetScissor)(VkCommandBuffer, uint32_t, uint32_t, const VkRect2D *);
typedef void (SDLCALL *PFN_vkCmdDraw)(VkCommandBuffer, uint32_t, uint32_t, uint32_t, uint32_t);
typedef VkResult (SDLCALL *PFN_vkCreateFence)(VkDevice, const VkFenceCreateInfo *, const void *, VkFence *);
typedef void (SDLCALL *PFN_vkDestroyFence)(VkDevice, VkFence, const void *);
typedef VkResult (SDLCALL *PFN_vkWaitForFences)(VkDevice, uint32_t, const VkFence *, VkBool32, uint64_t);
typedef VkResult (SDLCALL *PFN_vkResetFences)(VkDevice, uint32_t, const VkFence *);
typedef VkResult (SDLCALL *PFN_vkCreateSemaphore)(VkDevice, const VkSemaphoreCreateInfo *, const void *, VkSemaphore *);
typedef void (SDLCALL *PFN_vkDestroySemaphore)(VkDevice, VkSemaphore, const void *);
typedef VkResult (SDLCALL *PFN_vkQueueSubmit)(VkQueue, uint32_t, const VkSubmitInfo *, VkFence);
typedef VkResult (SDLCALL *PFN_vkDeviceWaitIdle)(VkDevice);

/* ---- context ---- */

typedef struct VulkanTriangleCtx {
    VkInstance instance;
    VkSurfaceKHR surface;
    VkPhysicalDevice physical;
    uint32_t queueFamily;
    VkDevice device;
    VkQueue queue;
    VkSwapchainKHR swapchain;
    VkFormat format;
    VkExtent2D extent;
    uint32_t imageCount;
    VkImage images[4];
    VkImageView views[4];
    VkFramebuffer framebuffers[4];
    VkRenderPass renderPass;
    VkPipelineLayout pipelineLayout;
    VkPipeline pipeline;
    VkCommandPool commandPool;
    VkCommandBuffer commandBuffer;
    VkFence fence;
    VkSemaphore imageAvailable, renderFinished;
    uint32_t currentImage;
    int width, height;
} VulkanTriangleCtx;

typedef void (*PFN_vkVoidFunction)(void);
typedef PFN_vkVoidFunction (SDLCALL *PFN_vkGetInstanceProcAddr)(VkInstance, const char *);
typedef PFN_vkVoidFunction (SDLCALL *PFN_vkGetDeviceProcAddr)(VkDevice, const char *);

static PFN_vkGetInstanceProcAddr g_vkGetInstanceProcAddr;
static PFN_vkGetDeviceProcAddr g_vkGetDeviceProcAddr;

/* function pointers (global, loaded once) */
static PFN_vkCreateInstance fnCreateInstance;
static PFN_vkDestroyInstance fnDestroyInstance;
static PFN_vkEnumerateInstanceExtensionProperties fnEnumerateInstanceExtensionProperties;
static PFN_vkEnumeratePhysicalDevices fnEnumeratePhysicalDevices;
static PFN_vkGetPhysicalDeviceProperties fnGetPhysicalDeviceProperties;
static PFN_vkGetPhysicalDeviceQueueFamilyProperties fnGetPhysicalDeviceQueueFamilyProperties;
static PFN_vkGetPhysicalDeviceSurfaceSupportKHR fnGetPhysicalDeviceSurfaceSupportKHR;
static PFN_vkGetPhysicalDeviceSurfaceCapabilitiesKHR fnGetPhysicalDeviceSurfaceCapabilitiesKHR;
static PFN_vkGetPhysicalDeviceSurfaceFormatsKHR fnGetPhysicalDeviceSurfaceFormatsKHR;
static PFN_vkGetPhysicalDeviceSurfacePresentModesKHR fnGetPhysicalDeviceSurfacePresentModesKHR;
static PFN_vkCreateDevice fnCreateDevice;
static PFN_vkDestroyDevice fnDestroyDevice;
static PFN_vkGetDeviceQueue fnGetDeviceQueue;
static PFN_vkCreateSwapchainKHR fnCreateSwapchainKHR;
static PFN_vkDestroySwapchainKHR fnDestroySwapchainKHR;
static PFN_vkGetSwapchainImagesKHR fnGetSwapchainImagesKHR;
static PFN_vkAcquireNextImageKHR fnAcquireNextImageKHR;
static PFN_vkQueuePresentKHR fnQueuePresentKHR;
static PFN_vkCreateImageView fnCreateImageView;
static PFN_vkDestroyImageView fnDestroyImageView;
static PFN_vkCreateRenderPass fnCreateRenderPass;
static PFN_vkDestroyRenderPass fnDestroyRenderPass;
static PFN_vkCreateFramebuffer fnCreateFramebuffer;
static PFN_vkDestroyFramebuffer fnDestroyFramebuffer;
static PFN_vkCreateShaderModule fnCreateShaderModule;
static PFN_vkDestroyShaderModule fnDestroyShaderModule;
static PFN_vkCreatePipelineLayout fnCreatePipelineLayout;
static PFN_vkDestroyPipelineLayout fnDestroyPipelineLayout;
static PFN_vkCreateGraphicsPipelines fnCreateGraphicsPipelines;
static PFN_vkDestroyPipeline fnDestroyPipeline;
static PFN_vkCreateCommandPool fnCreateCommandPool;
static PFN_vkDestroyCommandPool fnDestroyCommandPool;
static PFN_vkAllocateCommandBuffers fnAllocateCommandBuffers;
static PFN_vkFreeCommandBuffers fnFreeCommandBuffers;
static PFN_vkBeginCommandBuffer fnBeginCommandBuffer;
static PFN_vkResetCommandBuffer fnResetCommandBuffer;
static PFN_vkEndCommandBuffer fnEndCommandBuffer;
static PFN_vkCmdBeginRenderPass fnCmdBeginRenderPass;
static PFN_vkCmdEndRenderPass fnCmdEndRenderPass;
static PFN_vkCmdBindPipeline fnCmdBindPipeline;
static PFN_vkCmdSetViewport fnCmdSetViewport;
static PFN_vkCmdSetScissor fnCmdSetScissor;
static PFN_vkCmdDraw fnCmdDraw;
static PFN_vkCreateFence fnCreateFence;
static PFN_vkDestroyFence fnDestroyFence;
static PFN_vkWaitForFences fnWaitForFences;
static PFN_vkResetFences fnResetFences;
static PFN_vkCreateSemaphore fnCreateSemaphore;
static PFN_vkDestroySemaphore fnDestroySemaphore;
static PFN_vkQueueSubmit fnQueueSubmit;
static PFN_vkDeviceWaitIdle fnDeviceWaitIdle;

static void *SDL_kmp_vkload(const char *name)
{
    return (void *)g_vkGetInstanceProcAddr(NULL, name);
}

/* Global functions only; everything else needs a valid instance handle
 * (vkGetInstanceProcAddr returns NULL for instance/device functions when the
 * instance is NULL). */
static bool SDL_kmp_vk_load_all(void)
{
    if (!g_vkGetInstanceProcAddr) {
        g_vkGetInstanceProcAddr = (PFN_vkGetInstanceProcAddr)SDL_Vulkan_GetVkGetInstanceProcAddr();
    }
    if (!g_vkGetInstanceProcAddr) return false;
#define LOAD_FN(f, n) f = (PFN_##n)g_vkGetInstanceProcAddr(NULL, #n)
    LOAD_FN(fnCreateInstance, vkCreateInstance);
    LOAD_FN(fnEnumerateInstanceExtensionProperties, vkEnumerateInstanceExtensionProperties);
#undef LOAD_FN
    return fnCreateInstance != NULL;
}

/* Instance + device functions, resolved through the created instance. */
static bool SDL_kmp_vk_load_instance(VkInstance instance)
{
#define LOAD_FN(f, n) f = (PFN_##n)g_vkGetInstanceProcAddr(instance, #n)
    LOAD_FN(fnDestroyInstance, vkDestroyInstance); LOAD_FN(fnEnumeratePhysicalDevices, vkEnumeratePhysicalDevices);
    LOAD_FN(fnGetPhysicalDeviceProperties, vkGetPhysicalDeviceProperties); LOAD_FN(fnGetPhysicalDeviceQueueFamilyProperties, vkGetPhysicalDeviceQueueFamilyProperties);
    LOAD_FN(fnGetPhysicalDeviceSurfaceSupportKHR, vkGetPhysicalDeviceSurfaceSupportKHR); LOAD_FN(fnGetPhysicalDeviceSurfaceCapabilitiesKHR, vkGetPhysicalDeviceSurfaceCapabilitiesKHR);
    LOAD_FN(fnGetPhysicalDeviceSurfaceFormatsKHR, vkGetPhysicalDeviceSurfaceFormatsKHR); LOAD_FN(fnGetPhysicalDeviceSurfacePresentModesKHR, vkGetPhysicalDeviceSurfacePresentModesKHR);
    LOAD_FN(fnCreateDevice, vkCreateDevice); LOAD_FN(fnDestroyDevice, vkDestroyDevice); LOAD_FN(fnGetDeviceQueue, vkGetDeviceQueue);
    LOAD_FN(fnCreateSwapchainKHR, vkCreateSwapchainKHR); LOAD_FN(fnDestroySwapchainKHR, vkDestroySwapchainKHR); LOAD_FN(fnGetSwapchainImagesKHR, vkGetSwapchainImagesKHR);
    LOAD_FN(fnAcquireNextImageKHR, vkAcquireNextImageKHR); LOAD_FN(fnQueuePresentKHR, vkQueuePresentKHR);
    LOAD_FN(fnCreateImageView, vkCreateImageView); LOAD_FN(fnDestroyImageView, vkDestroyImageView);
    LOAD_FN(fnCreateRenderPass, vkCreateRenderPass); LOAD_FN(fnDestroyRenderPass, vkDestroyRenderPass);
    LOAD_FN(fnCreateFramebuffer, vkCreateFramebuffer); LOAD_FN(fnDestroyFramebuffer, vkDestroyFramebuffer);
    LOAD_FN(fnCreateShaderModule, vkCreateShaderModule); LOAD_FN(fnDestroyShaderModule, vkDestroyShaderModule);
    LOAD_FN(fnCreatePipelineLayout, vkCreatePipelineLayout); LOAD_FN(fnDestroyPipelineLayout, vkDestroyPipelineLayout);
    LOAD_FN(fnCreateGraphicsPipelines, vkCreateGraphicsPipelines); LOAD_FN(fnDestroyPipeline, vkDestroyPipeline);
    LOAD_FN(fnCreateCommandPool, vkCreateCommandPool); LOAD_FN(fnDestroyCommandPool, vkDestroyCommandPool);
    LOAD_FN(fnAllocateCommandBuffers, vkAllocateCommandBuffers); LOAD_FN(fnFreeCommandBuffers, vkFreeCommandBuffers);
    LOAD_FN(fnBeginCommandBuffer, vkBeginCommandBuffer); LOAD_FN(fnResetCommandBuffer, vkResetCommandBuffer); LOAD_FN(fnEndCommandBuffer, vkEndCommandBuffer);
    LOAD_FN(fnCmdBeginRenderPass, vkCmdBeginRenderPass); LOAD_FN(fnCmdEndRenderPass, vkCmdEndRenderPass);
    LOAD_FN(fnCmdBindPipeline, vkCmdBindPipeline); LOAD_FN(fnCmdSetViewport, vkCmdSetViewport); LOAD_FN(fnCmdSetScissor, vkCmdSetScissor); LOAD_FN(fnCmdDraw, vkCmdDraw);
    LOAD_FN(fnCreateFence, vkCreateFence); LOAD_FN(fnDestroyFence, vkDestroyFence); LOAD_FN(fnWaitForFences, vkWaitForFences); LOAD_FN(fnResetFences, vkResetFences);
    LOAD_FN(fnCreateSemaphore, vkCreateSemaphore); LOAD_FN(fnDestroySemaphore, vkDestroySemaphore);
    LOAD_FN(fnQueueSubmit, vkQueueSubmit); LOAD_FN(fnDeviceWaitIdle, vkDeviceWaitIdle);
#undef LOAD_FN
    return fnEnumeratePhysicalDevices && fnCreateDevice && fnCreateSwapchainKHR && fnQueueSubmit;
}

static bool SDL_kmp_vk_create_swapchain(VulkanTriangleCtx *ctx, SDL_Window *window, bool first)
{
    if (!first) {
        fnDeviceWaitIdle(ctx->device);
        for (uint32_t i = 0; i < ctx->imageCount; i++) {
            if (ctx->views[i]) fnDestroyImageView(ctx->device, ctx->views[i], NULL);
            if (ctx->framebuffers[i]) fnDestroyFramebuffer(ctx->device, ctx->framebuffers[i], NULL);
            ctx->views[i] = 0; ctx->framebuffers[i] = 0;
        }
        fnDestroySwapchainKHR(ctx->device, ctx->swapchain, NULL);
        ctx->swapchain = 0;
    }

    VkSurfaceCapabilitiesKHR caps;
    if (fnGetPhysicalDeviceSurfaceCapabilitiesKHR(ctx->physical, ctx->surface, &caps) != VK_SUCCESS) return false;
    uint32_t formatCount = 0;
    fnGetPhysicalDeviceSurfaceFormatsKHR(ctx->physical, ctx->surface, &formatCount, NULL);
    if (formatCount == 0) return false;
    VkSurfaceFormatKHR formats[8];
    if (formatCount > 8) formatCount = 8;
    fnGetPhysicalDeviceSurfaceFormatsKHR(ctx->physical, ctx->surface, &formatCount, formats);
    ctx->format = formats[0].format;

    int pw = 0, ph = 0;
    SDL_GetWindowSizeInPixels(window, &pw, &ph);
    ctx->extent.width = (uint32_t)pw;
    ctx->extent.height = (uint32_t)ph;
    if (ctx->extent.width == 0 || ctx->extent.height == 0) {
        ctx->extent = caps.currentExtent;
    }

    VkSwapchainCreateInfoKHR ci;
    memset(&ci, 0, sizeof(ci));
    ci.sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
    ci.surface = ctx->surface;
    ci.minImageCount = 2;
    ci.imageFormat = ctx->format;
    ci.imageColorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
    ci.imageExtent = ctx->extent;
    ci.imageArrayLayers = 1;
    ci.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
    ci.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
    ci.preTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
    ci.compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
    ci.presentMode = VK_PRESENT_MODE_FIFO_KHR;
    ci.clipped = 1;
    if (fnCreateSwapchainKHR(ctx->device, &ci, NULL, &ctx->swapchain) != VK_SUCCESS) return false;

    fnGetSwapchainImagesKHR(ctx->device, ctx->swapchain, &ctx->imageCount, NULL);
    if (ctx->imageCount > 4) ctx->imageCount = 4;
    fnGetSwapchainImagesKHR(ctx->device, ctx->swapchain, &ctx->imageCount, ctx->images);

    VkImageViewCreateInfo iv;
    memset(&iv, 0, sizeof(iv));
    iv.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
    iv.viewType = VK_IMAGE_VIEW_TYPE_2D;
    iv.format = ctx->format;
    iv.subresourceRange.aspectMask = 0x1; /* VK_IMAGE_ASPECT_COLOR_BIT */
    iv.subresourceRange.levelCount = 1;
    iv.subresourceRange.layerCount = 1;
    for (uint32_t i = 0; i < ctx->imageCount; i++) {
        iv.image = ctx->images[i];
        if (fnCreateImageView(ctx->device, &iv, NULL, &ctx->views[i]) != VK_SUCCESS) return false;

        VkFramebufferCreateInfo fb;
        memset(&fb, 0, sizeof(fb));
        fb.sType = VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
        fb.renderPass = ctx->renderPass;
        fb.attachmentCount = 1;
        fb.pAttachments = &ctx->views[i];
        fb.width = ctx->extent.width;
        fb.height = ctx->extent.height;
        fb.layers = 1;
        if (fnCreateFramebuffer(ctx->device, &fb, NULL, &ctx->framebuffers[i]) != VK_SUCCESS) return false;
    }
    return true;
}

void *SDL_kmp_VulkanTriangleInit(SDL_Window *window,
                                 const unsigned char *vert, int vert_size,
                                 const unsigned char *frag, int frag_size)
{
    if (!SDL_kmp_vk_load_all()) {
        SDL_SetError("failed to load Vulkan functions");
        return NULL;
    }

    VulkanTriangleCtx *ctx = (VulkanTriangleCtx *)SDL_calloc(1, sizeof(VulkanTriangleCtx));
    if (!ctx) return NULL;

    /* instance */
    const char *extensions[8];
    uint32_t extCount = 0;
    const char *const *exts = SDL_Vulkan_GetInstanceExtensions(&extCount);
    if (!exts || extCount == 0) {
        SDL_free(ctx);
        return NULL;
    }
    if (extCount > 8) extCount = 8;
    for (uint32_t i = 0; i < extCount; i++) extensions[i] = exts[i];

    /*
     * Portability drivers (MoltenVK on macOS) only load when the app opts
     * into VK_KHR_portability_enumeration and sets the matching instance
     * create flag; the Vulkan loader otherwise rejects them with
     * VK_ERROR_INCOMPATIBLE_DRIVER. SDL filters its reported extensions, so
     * query the loader directly.
     */
    bool enumeratePortability = false;
    if (fnEnumerateInstanceExtensionProperties) {
        typedef struct { char name[256]; uint32_t version; } VkExtensionPropertiesLocal;
        uint32_t ieCount = 0;
        if (fnEnumerateInstanceExtensionProperties(NULL, &ieCount, NULL) == VK_SUCCESS && ieCount > 0) {
            VkExtensionPropertiesLocal *ie =
                (VkExtensionPropertiesLocal *)SDL_malloc(sizeof(VkExtensionPropertiesLocal) * ieCount);
            if (ie) {
                if (fnEnumerateInstanceExtensionProperties(NULL, &ieCount, ie) == VK_SUCCESS) {
                    for (uint32_t i = 0; i < ieCount && !enumeratePortability; i++) {
                        enumeratePortability = SDL_strcmp(ie[i].name, "VK_KHR_portability_enumeration") == 0;
                    }
                }
                SDL_free(ie);
            }
        }
    }
    if (enumeratePortability && extCount < 8) {
        extensions[extCount++] = "VK_KHR_portability_enumeration";
    }

    VkApplicationInfo appInfo;
    memset(&appInfo, 0, sizeof(appInfo));
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.apiVersion = VK_API_VERSION_1_0;
    VkInstanceCreateInfo ici;
    memset(&ici, 0, sizeof(ici));
    ici.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    ici.pApplicationInfo = &appInfo;
    ici.enabledExtensionCount = extCount;
    ici.ppEnabledExtensionNames = extensions;
    if (enumeratePortability) {
        ici.flags = 0x1; /* VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR */
    }
    if (fnCreateInstance(&ici, NULL, &ctx->instance) != VK_SUCCESS) {
        SDL_free(ctx);
        SDL_SetError("vkCreateInstance failed");
        return NULL;
    }

    /* The remaining functions are resolved through the instance. */
    if (!SDL_kmp_vk_load_instance(ctx->instance)) {
        fnDestroyInstance(ctx->instance, NULL);
        SDL_free(ctx);
        SDL_SetError("failed to load Vulkan functions");
        return NULL;
    }

    /* surface */
    VkSurfaceKHR surface = 0;
    if (!SDL_Vulkan_CreateSurface(window, ctx->instance, NULL, &surface)) {
        fnDestroyInstance(ctx->instance, NULL);
        SDL_free(ctx);
        return NULL;
    }
    ctx->surface = surface;

    /* physical device + queue family */
    uint32_t devCount = 0;
    fnEnumeratePhysicalDevices(ctx->instance, &devCount, NULL);
    VkPhysicalDevice devices[4];
    if (devCount > 4) devCount = 4;
    if (devCount == 0 || fnEnumeratePhysicalDevices(ctx->instance, &devCount, devices) != VK_SUCCESS) {
        SDL_SetError("no Vulkan physical devices");
        SDL_Vulkan_DestroySurface(ctx->instance, surface, NULL);
        fnDestroyInstance(ctx->instance, NULL);
        SDL_free(ctx);
        return NULL;
    }
    ctx->physical = devices[0];
    uint32_t qCount = 0;
    fnGetPhysicalDeviceQueueFamilyProperties(ctx->physical, &qCount, NULL);
    /* must match the real VkQueueFamilyProperties layout: flags(4) + count(4)
     * + timestampValidBits(4) + VkExtent3D(12) */
    typedef struct { VkFlags flags; uint32_t queueCount; uint32_t timestampValidBits; VkExtent3D minImageTransferGranularity; } QProps;
    QProps *qprops = (QProps *)SDL_malloc(sizeof(QProps) * qCount);
    fnGetPhysicalDeviceQueueFamilyProperties(ctx->physical, &qCount, qprops);
    ctx->queueFamily = 0;
    bool found = false;
    for (uint32_t i = 0; i < qCount; i++) {
        VkBool32 supported = 0;
        fnGetPhysicalDeviceSurfaceSupportKHR(ctx->physical, i, ctx->surface, &supported);
        if ((qprops[i].flags & VK_QUEUE_GRAPHICS_BIT) && supported) {
            ctx->queueFamily = i;
            found = true;
            break;
        }
    }
    SDL_free(qprops);
    if (!found) {
        SDL_SetError("no graphics+present queue family");
        SDL_Vulkan_DestroySurface(ctx->instance, surface, NULL);
        fnDestroyInstance(ctx->instance, NULL);
        SDL_free(ctx);
        return NULL;
    }

    /* surface format (needed by the render pass, which is created before the
     * swapchain) */
    uint32_t formatCount = 0;
    fnGetPhysicalDeviceSurfaceFormatsKHR(ctx->physical, ctx->surface, &formatCount, NULL);
    if (formatCount == 0) {
        SDL_SetError("surface has no formats");
        SDL_Vulkan_DestroySurface(ctx->instance, surface, NULL);
        fnDestroyInstance(ctx->instance, NULL);
        SDL_free(ctx);
        return NULL;
    }
    VkSurfaceFormatKHR formats[8];
    if (formatCount > 8) formatCount = 8;
    fnGetPhysicalDeviceSurfaceFormatsKHR(ctx->physical, ctx->surface, &formatCount, formats);
    ctx->format = formats[0].format;

    /* device */
    const char *devExts[1] = { "VK_KHR_swapchain" };
    float priority = 1.0f;
    VkDeviceQueueCreateInfo dq;
    memset(&dq, 0, sizeof(dq));
    dq.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
    dq.queueFamilyIndex = ctx->queueFamily;
    dq.queueCount = 1;
    dq.pQueuePriorities = &priority;
    VkDeviceCreateInfo dci;
    memset(&dci, 0, sizeof(dci));
    dci.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    dci.queueCreateInfoCount = 1;
    dci.pQueueCreateInfos = &dq;
    dci.enabledExtensionCount = 1;
    dci.ppEnabledExtensionNames = devExts;
    if (fnCreateDevice(ctx->physical, &dci, NULL, &ctx->device) != VK_SUCCESS) {
        SDL_SetError("vkCreateDevice failed");
        SDL_Vulkan_DestroySurface(ctx->instance, surface, NULL);
        fnDestroyInstance(ctx->instance, NULL);
        SDL_free(ctx);
        return NULL;
    }
    fnGetDeviceQueue(ctx->device, ctx->queueFamily, 0, &ctx->queue);

    /* render pass */
    VkAttachmentDescription att;
    memset(&att, 0, sizeof(att));
    att.format = ctx->format;
    att.samples = VK_SAMPLE_COUNT_1_BIT;
    att.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
    att.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
    att.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
    att.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
    att.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    att.finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
    VkAttachmentReference colorRef;
    colorRef.attachment = 0;
    colorRef.layout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
    VkSubpassDescription sub;
    memset(&sub, 0, sizeof(sub));
    sub.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
    sub.colorAttachmentCount = 1;
    sub.pColorAttachments = &colorRef;
    VkRenderPassCreateInfo rp;
    memset(&rp, 0, sizeof(rp));
    rp.sType = VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
    rp.attachmentCount = 1;
    rp.pAttachments = &att;
    rp.subpassCount = 1;
    rp.pSubpasses = &sub;
    if (fnCreateRenderPass(ctx->device, &rp, NULL, &ctx->renderPass) != VK_SUCCESS) {
        SDL_SetError("vkCreateRenderPass failed");
        fnDestroyDevice(ctx->device, NULL);
        SDL_Vulkan_DestroySurface(ctx->instance, surface, NULL);
        fnDestroyInstance(ctx->instance, NULL);
        SDL_free(ctx);
        return NULL;
    }

    /* shader modules */
    VkShaderModuleCreateInfo sm;
    memset(&sm, 0, sizeof(sm));
    sm.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    sm.codeSize = (size_t)vert_size;
    sm.pCode = (const uint32_t *)vert;
    VkShaderModule vsm = 0, fsm = 0;
    fnCreateShaderModule(ctx->device, &sm, NULL, &vsm);
    sm.codeSize = (size_t)frag_size;
    sm.pCode = (const uint32_t *)frag;
    fnCreateShaderModule(ctx->device, &sm, NULL, &fsm);
    if (!vsm || !fsm) {
        SDL_SetError("vkCreateShaderModule failed");
        if (vsm) fnDestroyShaderModule(ctx->device, vsm, NULL);
        if (fsm) fnDestroyShaderModule(ctx->device, fsm, NULL);
        fnDestroyRenderPass(ctx->device, ctx->renderPass, NULL);
        fnDestroyDevice(ctx->device, NULL);
        SDL_Vulkan_DestroySurface(ctx->instance, surface, NULL);
        fnDestroyInstance(ctx->instance, NULL);
        SDL_free(ctx);
        return NULL;
    }

    /* pipeline layout */
    VkPipelineLayoutCreateInfo pl;
    memset(&pl, 0, sizeof(pl));
    pl.sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
    fnCreatePipelineLayout(ctx->device, &pl, NULL, &ctx->pipelineLayout);

    /* pipeline (created before swapchain; format is fixed at creation) */
    VkPipelineShaderStageCreateInfo stages[2];
    memset(stages, 0, sizeof(stages));
    stages[0].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    stages[0].stage = VK_SHADER_STAGE_VERTEX_BIT;
    stages[0].module = vsm;
    stages[0].pName = "main";
    stages[1].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    stages[1].stage = VK_SHADER_STAGE_FRAGMENT_BIT;
    stages[1].module = fsm;
    stages[1].pName = "main";

    VkPipelineVertexInputStateCreateInfo vi;
    memset(&vi, 0, sizeof(vi));
    vi.sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
    VkPipelineInputAssemblyStateCreateInfo ia;
    memset(&ia, 0, sizeof(ia));
    ia.sType = VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
    ia.topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
    VkViewport viewport = { 0, 0, 800, 600, 0, 1 };
    VkRect2D scissor = { {0, 0}, {800, 600} };
    VkPipelineViewportStateCreateInfo vs;
    memset(&vs, 0, sizeof(vs));
    vs.sType = VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
    vs.viewportCount = 1;
    vs.pViewports = &viewport;
    vs.scissorCount = 1;
    vs.pScissors = &scissor;
    VkPipelineRasterizationStateCreateInfo rs;
    memset(&rs, 0, sizeof(rs));
    rs.sType = VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
    rs.polygonMode = VK_POLYGON_MODE_FILL;
    rs.cullMode = VK_CULL_MODE_NONE;
    rs.frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE;
    rs.lineWidth = 1.0f;
    VkPipelineMultisampleStateCreateInfo ms;
    memset(&ms, 0, sizeof(ms));
    ms.sType = VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
    ms.rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;
    VkPipelineColorBlendAttachmentState ba;
    memset(&ba, 0, sizeof(ba));
    ba.colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
                        VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;
    VkPipelineColorBlendStateCreateInfo cb;
    memset(&cb, 0, sizeof(cb));
    cb.sType = VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
    cb.attachmentCount = 1;
    cb.pAttachments = &ba;

    VkGraphicsPipelineCreateInfo gp;
    memset(&gp, 0, sizeof(gp));
    gp.sType = VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
    gp.stageCount = 2;
    gp.pStages = stages;
    gp.pVertexInputState = &vi;
    gp.pInputAssemblyState = &ia;
    gp.pViewportState = &vs;
    gp.pRasterizationState = &rs;
    gp.pMultisampleState = &ms;
    gp.pColorBlendState = &cb;
    gp.layout = ctx->pipelineLayout;
    gp.renderPass = ctx->renderPass;
    fnCreateGraphicsPipelines(ctx->device, 0, 1, &gp, NULL, &ctx->pipeline);
    /* The shader modules may only be destroyed once the pipeline has been
     * created (they are read during vkCreateGraphicsPipelines). */
    fnDestroyShaderModule(ctx->device, vsm, NULL);
    fnDestroyShaderModule(ctx->device, fsm, NULL);
    if (!ctx->pipeline) {
        SDL_SetError("vkCreateGraphicsPipelines failed");
        fnDestroyPipelineLayout(ctx->device, ctx->pipelineLayout, NULL);
        fnDestroyRenderPass(ctx->device, ctx->renderPass, NULL);
        fnDestroyDevice(ctx->device, NULL);
        SDL_Vulkan_DestroySurface(ctx->instance, surface, NULL);
        fnDestroyInstance(ctx->instance, NULL);
        SDL_free(ctx);
        return NULL;
    }

    /* command pool / buffer / sync */
    VkCommandPoolCreateInfo cp;
    memset(&cp, 0, sizeof(cp));
    cp.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    cp.queueFamilyIndex = ctx->queueFamily;
    fnCreateCommandPool(ctx->device, &cp, NULL, &ctx->commandPool);
    VkCommandBufferAllocateInfo cba;
    memset(&cba, 0, sizeof(cba));
    cba.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    cba.commandPool = ctx->commandPool;
    cba.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    cba.commandBufferCount = 1;
    fnAllocateCommandBuffers(ctx->device, &cba, &ctx->commandBuffer);
    VkFenceCreateInfo fc;
    memset(&fc, 0, sizeof(fc));
    fc.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
    fc.flags = 0x1; /* VK_FENCE_CREATE_SIGNALED_BIT */
    fnCreateFence(ctx->device, &fc, NULL, &ctx->fence);
    VkSemaphoreCreateInfo sc;
    memset(&sc, 0, sizeof(sc));
    sc.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
    fnCreateSemaphore(ctx->device, &sc, NULL, &ctx->imageAvailable);
    fnCreateSemaphore(ctx->device, &sc, NULL, &ctx->renderFinished);

    if (!SDL_kmp_vk_create_swapchain(ctx, window, true)) {
        SDL_kmp_VulkanTriangleDestroy(ctx);
        return NULL;
    }
    ctx->width = (int)ctx->extent.width;
    ctx->height = (int)ctx->extent.height;
    return ctx;
}

bool SDL_kmp_VulkanTriangleRender(void *handle, SDL_Window *window)
{
    VulkanTriangleCtx *ctx = (VulkanTriangleCtx *)handle;
    if (!ctx) return false;

    int pw = 0, ph = 0;
    SDL_GetWindowSizeInPixels(window, &pw, &ph);
    if (pw != ctx->width || ph != ctx->height) {
        ctx->width = pw;
        ctx->height = ph;
        if (!SDL_kmp_vk_create_swapchain(ctx, window, false)) return false;
        ctx->width = (int)ctx->extent.width;
        ctx->height = (int)ctx->extent.height;
    }

    fnWaitForFences(ctx->device, 1, &ctx->fence, 1, UINT64_MAX);
    fnResetFences(ctx->device, 1, &ctx->fence);

    uint32_t imageIndex = 0;
    VkResult r = fnAcquireNextImageKHR(ctx->device, ctx->swapchain, UINT64_MAX, ctx->imageAvailable, 0, &imageIndex);
    if (r == 1 /* VK_ERROR_OUT_OF_DATE_KHR */) {
        if (!SDL_kmp_vk_create_swapchain(ctx, window, false)) return false;
        fnAcquireNextImageKHR(ctx->device, ctx->swapchain, UINT64_MAX, ctx->imageAvailable, 0, &imageIndex);
    }
    ctx->currentImage = imageIndex;

    VkCommandBufferBeginInfo begin;
    memset(&begin, 0, sizeof(begin));
    begin.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    fnBeginCommandBuffer(ctx->commandBuffer, &begin);

    VkClearValue clear;
    clear.color.float32[0] = 0.07f;
    clear.color.float32[1] = 0.07f;
    clear.color.float32[2] = 0.09f;
    clear.color.float32[3] = 1.0f;
    VkRenderPassBeginInfo rpBegin;
    memset(&rpBegin, 0, sizeof(rpBegin));
    rpBegin.sType = VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
    rpBegin.renderPass = ctx->renderPass;
    rpBegin.framebuffer = ctx->framebuffers[imageIndex];
    rpBegin.renderArea.offset.x = 0;
    rpBegin.renderArea.offset.y = 0;
    rpBegin.renderArea.extent = ctx->extent;
    rpBegin.clearValueCount = 1;
    rpBegin.pClearValues = &clear;
    fnCmdBeginRenderPass(ctx->commandBuffer, &rpBegin, VK_SUBPASS_CONTENTS_INLINE);
    fnCmdBindPipeline(ctx->commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, ctx->pipeline);
    VkViewport vp = { 0, 0, (float)ctx->extent.width, (float)ctx->extent.height, 0, 1 };
    VkRect2D sc = { {0, 0}, ctx->extent };
    fnCmdSetViewport(ctx->commandBuffer, 0, 1, &vp);
    fnCmdSetScissor(ctx->commandBuffer, 0, 1, &sc);
    fnCmdDraw(ctx->commandBuffer, 3, 1, 0, 0);
    fnCmdEndRenderPass(ctx->commandBuffer);
    fnEndCommandBuffer(ctx->commandBuffer);

    VkPipelineStageFlags waitStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    VkSubmitInfo si;
    memset(&si, 0, sizeof(si));
    si.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    si.waitSemaphoreCount = 1;
    si.pWaitSemaphores = &ctx->imageAvailable;
    si.pWaitDstStageMask = &waitStage;
    si.commandBufferCount = 1;
    si.pCommandBuffers = &ctx->commandBuffer;
    si.signalSemaphoreCount = 1;
    si.pSignalSemaphores = &ctx->renderFinished;
    fnQueueSubmit(ctx->queue, 1, &si, ctx->fence);

    VkPresentInfoKHR pi;
    memset(&pi, 0, sizeof(pi));
    pi.sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
    pi.waitSemaphoreCount = 1;
    pi.pWaitSemaphores = &ctx->renderFinished;
    pi.swapchainCount = 1;
    pi.pSwapchains = &ctx->swapchain;
    pi.pImageIndices = &imageIndex;
    fnQueuePresentKHR(ctx->queue, &pi);
    return true;
}

void SDL_kmp_VulkanTriangleDestroy(void *handle)
{
    VulkanTriangleCtx *ctx = (VulkanTriangleCtx *)handle;
    if (!ctx) return;
    if (ctx->device) fnDeviceWaitIdle(ctx->device);
    for (uint32_t i = 0; i < ctx->imageCount; i++) {
        if (ctx->views[i]) fnDestroyImageView(ctx->device, ctx->views[i], NULL);
        if (ctx->framebuffers[i]) fnDestroyFramebuffer(ctx->device, ctx->framebuffers[i], NULL);
    }
    if (ctx->swapchain) fnDestroySwapchainKHR(ctx->device, ctx->swapchain, NULL);
    if (ctx->pipeline) fnDestroyPipeline(ctx->device, ctx->pipeline, NULL);
    if (ctx->pipelineLayout) fnDestroyPipelineLayout(ctx->device, ctx->pipelineLayout, NULL);
    if (ctx->renderPass) fnDestroyRenderPass(ctx->device, ctx->renderPass, NULL);
    if (ctx->fence) fnDestroyFence(ctx->device, ctx->fence, NULL);
    if (ctx->imageAvailable) fnDestroySemaphore(ctx->device, ctx->imageAvailable, NULL);
    if (ctx->renderFinished) fnDestroySemaphore(ctx->device, ctx->renderFinished, NULL);
    if (ctx->commandPool) {
        fnFreeCommandBuffers(ctx->device, ctx->commandPool, 1, &ctx->commandBuffer);
        fnDestroyCommandPool(ctx->device, ctx->commandPool, NULL);
    }
    if (ctx->device) fnDestroyDevice(ctx->device, NULL);
    if (ctx->surface) SDL_Vulkan_DestroySurface(ctx->instance, ctx->surface, NULL);
    if (ctx->instance) fnDestroyInstance(ctx->instance, NULL);
    SDL_free(ctx);
}

