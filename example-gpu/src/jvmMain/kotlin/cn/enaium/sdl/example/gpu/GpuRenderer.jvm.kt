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

package cn.enaium.sdl.example.gpu

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLGLAttribute
import cn.enaium.sdl.SDLGLProfile
import cn.enaium.sdl.SDLWindow
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33
import org.lwjgl.system.FunctionProvider
import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.KHRPortabilityEnumeration
import org.lwjgl.vulkan.KHRSurface
import org.lwjgl.vulkan.KHRSwapchain
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkAttachmentDescription
import org.lwjgl.vulkan.VkAttachmentReference
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo
import org.lwjgl.vulkan.VkCommandBufferBeginInfo
import org.lwjgl.vulkan.VkCommandPoolCreateInfo
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkDeviceCreateInfo
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo
import org.lwjgl.vulkan.VkExtensionProperties
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkFenceCreateInfo
import org.lwjgl.vulkan.VkFramebufferCreateInfo
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo
import org.lwjgl.vulkan.VkImageSubresourceRange
import org.lwjgl.vulkan.VkImageViewCreateInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo
import org.lwjgl.vulkan.VkPresentInfoKHR
import org.lwjgl.vulkan.VkQueue
import org.lwjgl.vulkan.VkQueueFamilyProperties
import org.lwjgl.vulkan.VkRect2D
import org.lwjgl.vulkan.VkRenderPassBeginInfo
import org.lwjgl.vulkan.VkRenderPassCreateInfo
import org.lwjgl.vulkan.VkSemaphoreCreateInfo
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import org.lwjgl.vulkan.VkSubmitInfo
import org.lwjgl.vulkan.VkSubpassDescription
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.VkSurfaceFormatKHR
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR
import org.lwjgl.vulkan.VkViewport

actual fun createGpuRenderer(
    api: GpuApi,
    window: SDLWindow,
    width: Int,
    height: Int,
): GpuRenderer = when (api) {
    GpuApi.OPENGL -> JvmOpenGlRenderer(window)
    GpuApi.VULKAN -> JvmVulkanRenderer(window)
}

// =========================================================================
// OpenGL (LWJGL GL33; context created through the sdl-kmp GL bindings)
// =========================================================================

private class JvmOpenGlRenderer(private val window: SDLWindow) : GpuRenderer {

    private val context: ULong
    private var program = 0
    private var vao = 0

    init {
        SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MAJOR_VERSION, 3)
        SDL.glSetAttribute(SDLGLAttribute.CONTEXT_MINOR_VERSION, 3)
        SDL.glSetAttribute(SDLGLAttribute.CONTEXT_PROFILE_MASK, SDLGLProfile.CORE)
        context = SDL.glCreateContext(window.id)
        check(context != 0uL) { "SDL_GL_CreateContext failed: ${SDL.error()}" }
        check(SDL.glMakeCurrent(window.id, context)) { "SDL_GL_MakeCurrent failed: ${SDL.error()}" }
        SDL.glSetSwapInterval(1)

        GL.createCapabilities()

        val vs = compileShader(
            GL33.GL_VERTEX_SHADER,
            "#version 330 core\n" +
                "out vec3 vColor;\n" +
                "void main() {\n" +
                "  vec2 p[3] = vec2[3](vec2(-0.75, -0.75), vec2(0.75, -0.75), vec2(0.0, 0.75));\n" +
                "  vec3 c[3] = vec3[3](vec3(1.0, 0.25, 0.35), vec3(0.25, 1.0, 0.35), vec3(0.25, 0.35, 1.0));\n" +
                "  gl_Position = vec4(p[gl_VertexID], 0.0, 1.0);\n" +
                "  vColor = c[gl_VertexID];\n" +
                "}\n",
        )
        val fs = compileShader(
            GL33.GL_FRAGMENT_SHADER,
            "#version 330 core\nin vec3 vColor;\nout vec4 c;\nvoid main() { c = vec4(vColor, 1.0); }\n",
        )
        program = GL33.glCreateProgram()
        GL33.glAttachShader(program, vs)
        GL33.glAttachShader(program, fs)
        GL33.glLinkProgram(program)
        GL33.glDeleteShader(vs)
        GL33.glDeleteShader(fs)
        check(GL33.glGetProgrami(program, GL33.GL_LINK_STATUS) == GL33.GL_TRUE) {
            "GL program link failed: ${GL33.glGetProgramInfoLog(program)}"
        }
        vao = GL33.glGenVertexArrays()
        GL33.glBindVertexArray(vao)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GL33.glCreateShader(type)
        MemoryStack.stackPush().use { stack ->
            GL33.glShaderSource(shader, source)
        }
        GL33.glCompileShader(shader)
        check(GL33.glGetShaderi(shader, GL33.GL_COMPILE_STATUS) == GL33.GL_TRUE) {
            "GL shader compile failed: ${GL33.glGetShaderInfoLog(shader)}"
        }
        return shader
    }

    override fun render(width: Int, height: Int): Boolean {
        GL33.glViewport(0, 0, width, height)
        GL33.glClearColor(0.07f, 0.07f, 0.09f, 1.0f)
        GL33.glClear(GL33.GL_COLOR_BUFFER_BIT)
        GL33.glUseProgram(program)
        GL33.glBindVertexArray(vao)
        GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, 3)
        SDL.glSwapWindow(window.id)
        return true
    }

    override fun close() {
        if (program != 0) GL33.glDeleteProgram(program)
        if (vao != 0) GL33.glDeleteVertexArrays(vao)
        SDL.glMakeCurrent(window.id, 0uL)
        SDL.glDestroyContext(context)
    }
}

// =========================================================================
// Vulkan (LWJGL VK bindings; loader wired to SDL's vkGetInstanceProcAddr)
// =========================================================================

private class JvmVulkanRenderer(private val window: SDLWindow) : GpuRenderer {

    private lateinit var instance: VkInstance
    private lateinit var physical: VkPhysicalDevice
    private lateinit var device: VkDevice
    private lateinit var queue: VkQueue
    private var queueFamily = 0
    private var surface = 0L
    private var swapchain = 0L
    private var renderPass = 0L
    private var pipelineLayout = 0L
    private var pipeline = 0L
    private var commandPool = 0L
    private var commandBuffer: VkCommandBuffer? = null
    private var fence = 0L
    private var imageAvailable = 0L
    private var renderFinished = 0L
    private var swapchainFormat = 0
    private var swapchainWidth = 0
    private var swapchainHeight = 0
    private val images = ArrayList<Long>()
    private val imageViews = ArrayList<Long>()
    private val framebuffers = ArrayList<Long>()
    private var currentImage = 0

    init {
        SDL.vulkanLoadLibrary()
        val vkGetInstanceProcAddr = SDL.vulkanGetVkGetInstanceProcAddr
        check(vkGetInstanceProcAddr != 0uL) { "SDL_Vulkan_GetVkGetInstanceProcAddr failed: ${SDL.error()}" }

        // Stop LWJGL's VK static initializer from auto-creating itself from
        // the system loader; the function table must come from SDL instead.
        org.lwjgl.system.Configuration.VULKAN_EXPLICIT_INIT.set(true)
        org.lwjgl.vulkan.VK.create(
            FunctionProvider { name ->
                // LWJGL's JNI.invokePPP convention changed with its JDK 25+ FFM
                // bindings: classic JNI puts the function address first, the FFM
                // variant puts it last (checked against JNI.class bytecode).
                val procAddr = vkGetInstanceProcAddr.toLong()
                val instance = 0L
                val nameAddr = MemoryUtil.memAddress(name)
                if (Runtime.version().feature() >= 25) {
                    JNI.invokePPP(instance, nameAddr, procAddr)
                } else {
                    JNI.invokePPP(procAddr, instance, nameAddr)
                }
            },
        )

        MemoryStack.stackPush().use { stack ->
            // instance. SDL filters its reported instance extensions, so query
            // the loader directly for the portability enumeration extension:
            // on macOS the Vulkan loader (e.g. LunarG's libvulkan) only loads
            // portability drivers like MoltenVK when the app opts into
            // VK_KHR_portability_enumeration and sets its instance flag.
            val extensions = SDL.vulkanInstanceExtensions.toMutableList()
            val instanceExtCount = stack.mallocInt(1)
            VK10.vkEnumerateInstanceExtensionProperties(null as String?, instanceExtCount, null)
            val instanceExts = VkExtensionProperties.calloc(instanceExtCount.get(0), stack)
            VK10.vkEnumerateInstanceExtensionProperties(null as String?, instanceExtCount, instanceExts)
            val enumeratePortability = (0 until instanceExtCount.get(0)).any {
                instanceExts.get(it).extensionNameString() ==
                    KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME
            }
            if (enumeratePortability) extensions += KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME
            val ppExtensions = stack.mallocPointer(extensions.size)
            extensions.forEachIndexed { i, e -> ppExtensions.put(i, stack.UTF8(e)) }
            val appInfo = VkApplicationInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .apiVersion(VK10.VK_API_VERSION_1_0)
            val ici = VkInstanceCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(ppExtensions)
            if (enumeratePortability) {
                ici.flags(KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR)
            }
            val instancePtr = stack.callocPointer(1)
            val createResult = VK10.vkCreateInstance(ici, null, instancePtr)
            check(createResult == VK10.VK_SUCCESS) {
                "vkCreateInstance failed (result=$createResult, extensions=$extensions): ${SDL.error()}"
            }
            instance = VkInstance(instancePtr.get(0), ici)

            // surface
            surface = SDL.vulkanCreateSurface(window.id, instance.address().toULong()).toLong()
            check(surface != 0L) { "SDL_Vulkan_CreateSurface failed: ${SDL.error()}" }

            // physical device
            val devCount = stack.mallocInt(1)
            VK10.vkEnumeratePhysicalDevices(instance, devCount, null)
            val devices = stack.mallocPointer(devCount.get(0))
            check(VK10.vkEnumeratePhysicalDevices(instance, devCount, devices) == VK10.VK_SUCCESS) {
                "vkEnumeratePhysicalDevices failed (count=${devCount.get(0)}): ${SDL.error()}"
            }
            physical = VkPhysicalDevice(devices.get(0), instance)

            // queue family with graphics + present
            val qCount = stack.mallocInt(1)
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(physical, qCount, null)
            val props = VkQueueFamilyProperties.calloc(qCount.get(0), stack)
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(physical, qCount, props)
            val supportsPresent = stack.mallocInt(1)
            for (i in 0 until qCount.get(0)) {
                KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physical, i, surface, supportsPresent)
                if ((props.get(i).queueFlags() and VK10.VK_QUEUE_GRAPHICS_BIT) != 0 &&
                    supportsPresent.get(0) == VK10.VK_TRUE
                ) {
                    queueFamily = i
                    break
                }
            }

            // device
            val queueInfos = VkDeviceQueueCreateInfo.calloc(1, stack)
            queueInfos.get(0)
                .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .queueFamilyIndex(queueFamily)
                .pQueuePriorities(stack.floats(1.0f))
            VkDeviceQueueCreateInfo.nqueueCount(queueInfos.get(0).address(), 1)
            val deviceExtensions = stack.mallocPointer(1).put(0, stack.UTF8("VK_KHR_swapchain"))
            val dci = VkDeviceCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(queueInfos)
                .ppEnabledExtensionNames(deviceExtensions)
            VkDeviceCreateInfo.nqueueCreateInfoCount(dci.address(), 1)
            val devicePtr = stack.callocPointer(1)
            check(VK10.vkCreateDevice(physical, dci, null, devicePtr) == VK10.VK_SUCCESS)
            device = VkDevice(devicePtr.get(0), physical, dci)

            val queuePtr = stack.callocPointer(1)
            VK10.vkGetDeviceQueue(device, queueFamily, 0, queuePtr)
            queue = VkQueue(queuePtr.get(0), device)

            // render pass (format resolved from the surface)
            val fmtCount = stack.mallocInt(1)
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physical, surface, fmtCount, null)
            val formats = VkSurfaceFormatKHR.calloc(fmtCount.get(0), stack)
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physical, surface, fmtCount, formats)
            swapchainFormat = formats.get(0).format()

            val attachments = VkAttachmentDescription.calloc(1, stack)
            attachments.get(0)
                .format(swapchainFormat)
                .samples(VK10.VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
            val colorRefs = VkAttachmentReference.calloc(1, stack)
            colorRefs.get(0)
                .attachment(0)
                .layout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            val subpasses = VkSubpassDescription.calloc(1, stack)
            subpasses.get(0)
                .pipelineBindPoint(VK10.VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(1)
                .pColorAttachments(colorRefs)
            val rp = VkRenderPassCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(attachments)
                .pSubpasses(subpasses)
            val rpPtr = stack.mallocLong(1)
            check(VK10.vkCreateRenderPass(device, rp, null, rpPtr) == VK10.VK_SUCCESS)
            renderPass = rpPtr.get(0)

            // pipeline layout
            val pl = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
            val plPtr = stack.mallocLong(1)
            check(VK10.vkCreatePipelineLayout(device, pl, null, plPtr) == VK10.VK_SUCCESS)
            pipelineLayout = plPtr.get(0)

            // shader modules
            val vsModule = createShaderModule(stack, VERT_SPV)
            val fsModule = createShaderModule(stack, FRAG_SPV)
            try {
                val stages = VkPipelineShaderStageCreateInfo.calloc(2, stack)
                stages.get(0)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK10.VK_SHADER_STAGE_VERTEX_BIT)
                    .module(vsModule)
                    .pName(stack.UTF8("main"))
                stages.get(1)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(fsModule)
                    .pName(stack.UTF8("main"))

                val vi = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                val ia = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                val viewports = VkViewport.calloc(1, stack)
                viewports.get(0).x(0f).y(0f).width(800f).height(600f).minDepth(0f).maxDepth(1f)
                val scissors = VkRect2D.calloc(1, stack)
                scissors.get(0).extent { it.width(800).height(600) }
                val vs = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .pViewports(viewports)
                    .pScissors(scissors)
                val rs = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .polygonMode(VK10.VK_POLYGON_MODE_FILL)
                    .cullMode(VK10.VK_CULL_MODE_NONE)
                    .frontFace(VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE)
                    .lineWidth(1.0f)
                val ms = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .rasterizationSamples(VK10.VK_SAMPLE_COUNT_1_BIT)
                val blendAttachments = VkPipelineColorBlendAttachmentState.calloc(1, stack)
                blendAttachments.get(0).colorWriteMask(
                    VK10.VK_COLOR_COMPONENT_R_BIT or VK10.VK_COLOR_COMPONENT_G_BIT or
                        VK10.VK_COLOR_COMPONENT_B_BIT or VK10.VK_COLOR_COMPONENT_A_BIT,
                )
                val cb = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .pAttachments(blendAttachments)
                val gp = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                gp.get(0)
                    .sType(VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .pStages(stages)
                    .pVertexInputState(vi)
                    .pInputAssemblyState(ia)
                    .pViewportState(vs)
                    .pRasterizationState(rs)
                    .pMultisampleState(ms)
                    .pColorBlendState(cb)
                    .layout(pipelineLayout)
                    .renderPass(renderPass)
                val pipelinePtr = stack.mallocLong(1)
                check(
                    VK10.vkCreateGraphicsPipelines(device, VK10.VK_NULL_HANDLE, gp, null, pipelinePtr) == VK10.VK_SUCCESS,
                )
                pipeline = pipelinePtr.get(0)
            } finally {
                VK10.vkDestroyShaderModule(device, vsModule, null)
                VK10.vkDestroyShaderModule(device, fsModule, null)
            }

            // command pool + buffer + sync
            val cp = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .queueFamilyIndex(queueFamily)
            val cpPtr = stack.mallocLong(1)
            VK10.vkCreateCommandPool(device, cp, null, cpPtr)
            commandPool = cpPtr.get(0)
            val cba = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(commandPool)
                .level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1)
            val cbPtr = stack.callocPointer(1)
            check(VK10.vkAllocateCommandBuffers(device, cba, cbPtr) == VK10.VK_SUCCESS)
            commandBuffer = VkCommandBuffer(cbPtr.get(0), device)

            val fc = VkFenceCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                .flags(VK10.VK_FENCE_CREATE_SIGNALED_BIT)
            val fencePtr = stack.mallocLong(1)
            VK10.vkCreateFence(device, fc, null, fencePtr)
            fence = fencePtr.get(0)
            val sc = VkSemaphoreCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
            val semPtr = stack.mallocLong(2)
            VK10.vkCreateSemaphore(device, sc, null, semPtr)
            imageAvailable = semPtr.get(0)
            renderFinished = semPtr.get(1)
        }

        createSwapchain()
    }

    private fun createShaderModule(stack: MemoryStack, code: ByteArray): Long {
        val buf = MemoryUtil.memAlloc(code.size)
        buf.put(code).flip()
        try {
            val sm = VkShaderModuleCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(buf)
            val ptr = stack.mallocLong(1)
            val smResult = VK10.vkCreateShaderModule(device, sm, null, ptr)
            check(smResult == VK10.VK_SUCCESS) {
                "vkCreateShaderModule failed (result=$smResult, size=${code.size}): ${SDL.error()}"
            }
            return ptr.get(0)
        } finally {
            MemoryUtil.memFree(buf)
        }
    }

    private fun createSwapchain() {
        MemoryStack.stackPush().use { stack ->
            val caps = VkSurfaceCapabilitiesKHR.calloc(stack)
            KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physical, surface, caps)
            val currentExtent = caps.currentExtent()
            if (currentExtent.width() == 0xFFFFFFFF.toInt() || currentExtent.height() == 0xFFFFFFFF.toInt()) {
                swapchainWidth = window.size.x
                swapchainHeight = window.size.y
            } else {
                swapchainWidth = currentExtent.width()
                swapchainHeight = currentExtent.height()
            }
            val extent = VkExtent2D.calloc(stack).width(swapchainWidth).height(swapchainHeight)

            val ci = VkSwapchainCreateInfoKHR.calloc(stack)
                .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .surface(surface)
                .minImageCount(2)
                .imageFormat(swapchainFormat)
                .imageColorSpace(KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                .imageExtent(extent)
                .imageArrayLayers(1)
                .imageUsage(VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .imageSharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE)
                .preTransform(KHRSurface.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR)
                .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                .presentMode(KHRSurface.VK_PRESENT_MODE_FIFO_KHR)
                .clipped(true)
            val ptr = stack.mallocLong(1)
            check(KHRSwapchain.vkCreateSwapchainKHR(device, ci, null, ptr) == VK10.VK_SUCCESS)
            swapchain = ptr.get(0)

            val count = stack.mallocInt(1)
            KHRSwapchain.vkGetSwapchainImagesKHR(device, swapchain, count, null)
            images.clear()
            val imgPtr = stack.mallocLong(count.get(0))
            KHRSwapchain.vkGetSwapchainImagesKHR(device, swapchain, count, imgPtr)
            for (i in 0 until count.get(0)) images.add(imgPtr.get(i))

            imageViews.forEach { VK10.vkDestroyImageView(device, it, null) }
            framebuffers.forEach { VK10.vkDestroyFramebuffer(device, it, null) }
            imageViews.clear()
            framebuffers.clear()

            val iv = VkImageViewCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .viewType(VK10.VK_IMAGE_VIEW_TYPE_2D)
                .format(swapchainFormat)
                .subresourceRange(
                    VkImageSubresourceRange.calloc(stack)
                        .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                        .levelCount(1)
                        .layerCount(1),
                )
            for (img in images) {
                val viewPtr = stack.mallocLong(1)
                iv.image(img)
                check(VK10.vkCreateImageView(device, iv, null, viewPtr) == VK10.VK_SUCCESS)
                imageViews.add(viewPtr.get(0))
            }

            val fb = VkFramebufferCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .renderPass(renderPass)
                .width(swapchainWidth)
                .height(swapchainHeight)
                .layers(1)
            for (view in imageViews) {
                val fbPtr = stack.mallocLong(1)
                fb.attachmentCount(1).pAttachments(stack.longs(view))
                check(VK10.vkCreateFramebuffer(device, fb, null, fbPtr) == VK10.VK_SUCCESS)
                framebuffers.add(fbPtr.get(0))
            }
        }
    }

    override fun render(width: Int, height: Int): Boolean {
        if (width != swapchainWidth || height != swapchainHeight) {
            createSwapchain()
        }
        MemoryStack.stackPush().use { stack ->
            VK10.vkWaitForFences(device, fence, true, Long.MAX_VALUE)
            VK10.vkResetFences(device, fence)

            val imageIndex = stack.mallocInt(1)
            var result = KHRSwapchain.vkAcquireNextImageKHR(
                device,
                swapchain,
                Long.MAX_VALUE,
                imageAvailable,
                VK10.VK_NULL_HANDLE,
                imageIndex,
            )
            if (result == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                createSwapchain()
                result = KHRSwapchain.vkAcquireNextImageKHR(
                    device,
                    swapchain,
                    Long.MAX_VALUE,
                    imageAvailable,
                    VK10.VK_NULL_HANDLE,
                    imageIndex,
                )
            }
            if (result != VK10.VK_SUCCESS) return false
            currentImage = imageIndex.get(0)

            val cmd = commandBuffer ?: return false
            val begin = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            check(VK10.vkBeginCommandBuffer(cmd, begin) == VK10.VK_SUCCESS)

            val clearValues = VkClearValue.calloc(1, stack)
            clearValues.get(0)
                .color { c -> c.float32(0, 0.07f).float32(1, 0.07f).float32(2, 0.09f).float32(3, 1.0f) }
            val rpBegin = VkRenderPassBeginInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .renderPass(renderPass)
                .framebuffer(framebuffers[currentImage])
                .renderArea { r ->
                    r.extent { e -> e.width(swapchainWidth).height(swapchainHeight) }
                }
                .clearValueCount(1)
                .pClearValues(clearValues)
            VK10.vkCmdBeginRenderPass(cmd, rpBegin, VK10.VK_SUBPASS_CONTENTS_INLINE)
            VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline)
            val viewports = VkViewport.calloc(1, stack)
            viewports.get(0)
                .x(0f).y(0f)
                .width(swapchainWidth.toFloat())
                .height(swapchainHeight.toFloat())
                .minDepth(0f).maxDepth(1f)
            val scissors = VkRect2D.calloc(1, stack)
            scissors.get(0).extent { e -> e.width(swapchainWidth).height(swapchainHeight) }
            VK10.vkCmdSetViewport(cmd, 0, viewports)
            VK10.vkCmdSetScissor(cmd, 0, scissors)
            VK10.vkCmdDraw(cmd, 3, 1, 0, 0)
            VK10.vkCmdEndRenderPass(cmd)
            check(VK10.vkEndCommandBuffer(cmd) == VK10.VK_SUCCESS)

            val waitStage = stack.ints(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            val submit = VkSubmitInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .waitSemaphoreCount(1)
                .pWaitSemaphores(stack.longs(imageAvailable))
                .pWaitDstStageMask(waitStage)
                .pCommandBuffers(stack.pointers(cmd.address()))
                .pSignalSemaphores(stack.longs(renderFinished))
            check(VK10.vkQueueSubmit(queue, submit, fence) == VK10.VK_SUCCESS)

            val present = VkPresentInfoKHR.calloc(stack)
                .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pWaitSemaphores(stack.longs(renderFinished))
                .swapchainCount(1)
                .pSwapchains(stack.longs(swapchain))
                .pImageIndices(stack.ints(currentImage))
            KHRSwapchain.vkQueuePresentKHR(queue, present)
        }
        return true
    }

    override fun close() {
        VK10.vkDeviceWaitIdle(device)
        framebuffers.forEach { VK10.vkDestroyFramebuffer(device, it, null) }
        imageViews.forEach { VK10.vkDestroyImageView(device, it, null) }
        if (swapchain != 0L) KHRSwapchain.vkDestroySwapchainKHR(device, swapchain, null)
        if (pipeline != 0L) VK10.vkDestroyPipeline(device, pipeline, null)
        if (pipelineLayout != 0L) VK10.vkDestroyPipelineLayout(device, pipelineLayout, null)
        if (renderPass != 0L) VK10.vkDestroyRenderPass(device, renderPass, null)
        if (fence != 0L) VK10.vkDestroyFence(device, fence, null)
        if (imageAvailable != 0L) VK10.vkDestroySemaphore(device, imageAvailable, null)
        if (renderFinished != 0L) VK10.vkDestroySemaphore(device, renderFinished, null)
        if (commandPool != 0L) {
            commandBuffer?.let { VK10.vkFreeCommandBuffers(device, commandPool, it) }
            VK10.vkDestroyCommandPool(device, commandPool, null)
        }
        if (::device.isInitialized) VK10.vkDestroyDevice(device, null)
        if (surface != 0L && ::instance.isInitialized) SDL.vulkanDestroySurface(instance.address().toULong(), surface.toULong())
        if (::instance.isInitialized) VK10.vkDestroyInstance(instance, null)
        SDL.vulkanUnloadLibrary()
    }
}
