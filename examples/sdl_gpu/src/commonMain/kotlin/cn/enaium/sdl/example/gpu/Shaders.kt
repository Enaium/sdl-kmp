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

/**
 * Shader sources for the GPU triangle demo.
 *
 * The vertex shader takes per-vertex position (location 0) and color
 * (location 1); the fragment shader writes the interpolated color.
 *
 * - [VERT_SPV]/[FRAG_SPV] are precompiled SPIR-V (Vulkan/Android backend),
 *   compiled from shaders/triangle.{vert,frag} with
 *   `glslangValidator --target-env vulkan1.0 -V`.
 * - [VERT_MSL]/[FRAG_MSL] are Metal Shading Language sources (macOS Metal
 *   backend), written by hand to mirror the GLSL semantics.
 */

internal val VERT_SPV = byteArrayOf(
    3.toByte(), 2.toByte(), 35.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 8.toByte(), 0.toByte(),
    31.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 2.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    71.toByte(), 76.toByte(), 83.toByte(), 76.toByte(), 46.toByte(), 115.toByte(), 116.toByte(), 100.toByte(), 46.toByte(), 52.toByte(), 53.toByte(), 48.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 14.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 15.toByte(), 0.toByte(), 9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 109.toByte(), 97.toByte(), 105.toByte(), 110.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 28.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    194.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    109.toByte(), 97.toByte(), 105.toByte(), 110.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 6.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 103.toByte(), 108.toByte(), 95.toByte(), 80.toByte(), 101.toByte(), 114.toByte(), 86.toByte(), 101.toByte(),
    114.toByte(), 116.toByte(), 101.toByte(), 120.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 6.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 103.toByte(), 108.toByte(), 95.toByte(), 80.toByte(),
    111.toByte(), 115.toByte(), 105.toByte(), 116.toByte(), 105.toByte(), 111.toByte(), 110.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 7.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 103.toByte(), 108.toByte(), 95.toByte(), 80.toByte(),
    111.toByte(), 105.toByte(), 110.toByte(), 116.toByte(), 83.toByte(), 105.toByte(), 122.toByte(), 101.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    103.toByte(), 108.toByte(), 95.toByte(), 67.toByte(), 108.toByte(), 105.toByte(), 112.toByte(), 68.toByte(), 105.toByte(), 115.toByte(), 116.toByte(), 97.toByte(),
    110.toByte(), 99.toByte(), 101.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 103.toByte(), 108.toByte(), 95.toByte(), 67.toByte(), 117.toByte(), 108.toByte(), 108.toByte(), 68.toByte(),
    105.toByte(), 115.toByte(), 116.toByte(), 97.toByte(), 110.toByte(), 99.toByte(), 101.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 3.toByte(), 0.toByte(),
    13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 112.toByte(), 111.toByte(), 115.toByte(), 105.toByte(), 116.toByte(), 105.toByte(), 111.toByte(), 110.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 28.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    118.toByte(), 67.toByte(), 111.toByte(), 108.toByte(), 111.toByte(), 114.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 99.toByte(), 111.toByte(), 108.toByte(), 111.toByte(), 114.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    71.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    72.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 72.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 72.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    72.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 28.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 19.toByte(), 0.toByte(), 2.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 33.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 22.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    32.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 21.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    8.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    43.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 8.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 28.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 10.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 30.toByte(), 0.toByte(), 6.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    10.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 10.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 21.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 14.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    32.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 43.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    14.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 15.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    23.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    43.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 20.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 128.toByte(), 63.toByte(), 32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 25.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    27.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 27.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 28.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 54.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 248.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    61.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 19.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 81.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    21.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 19.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    81.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 22.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    19.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 81.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 19.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 80.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    24.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 21.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 22.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    23.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 20.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 65.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    25.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 26.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    15.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 62.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 26.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    24.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 61.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 62.toByte(), 0.toByte(), 3.toByte(), 0.toByte(),
    28.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 253.toByte(), 0.toByte(), 1.toByte(), 0.toByte(),
    56.toByte(), 0.toByte(), 1.toByte(), 0.toByte(),
)

internal val FRAG_SPV = byteArrayOf(
    3.toByte(), 2.toByte(), 35.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 8.toByte(), 0.toByte(),
    19.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 2.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    71.toByte(), 76.toByte(), 83.toByte(), 76.toByte(), 46.toByte(), 115.toByte(), 116.toByte(), 100.toByte(), 46.toByte(), 52.toByte(), 53.toByte(), 48.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 14.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 15.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 109.toByte(), 97.toByte(), 105.toByte(), 110.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 3.toByte(), 0.toByte(),
    4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 3.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 194.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 109.toByte(), 97.toByte(), 105.toByte(), 110.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    5.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 102.toByte(), 114.toByte(), 97.toByte(), 103.toByte(),
    67.toByte(), 111.toByte(), 108.toByte(), 111.toByte(), 114.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 118.toByte(), 67.toByte(), 111.toByte(), 108.toByte(), 111.toByte(), 114.toByte(), 0.toByte(), 0.toByte(),
    71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 19.toByte(), 0.toByte(), 2.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 33.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 22.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    32.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    8.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 8.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 10.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 10.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 43.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    14.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 128.toByte(), 63.toByte(), 54.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 248.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    61.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 10.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 81.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    15.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    81.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 81.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 80.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 15.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 14.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 62.toByte(), 0.toByte(), 3.toByte(), 0.toByte(),
    9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 253.toByte(), 0.toByte(), 1.toByte(), 0.toByte(),
    56.toByte(), 0.toByte(), 1.toByte(), 0.toByte(),
)

internal val VERT_MSL = """
#include <metal_stdlib>
#include <simd/simd.h>

using namespace metal;

struct VSInput {
    float3 position [[attribute(0)]];
    float3 color [[attribute(1)]];
};

struct VSOutput {
    float3 color [[user(locn0)]];
    float4 position [[position]];
};

vertex VSOutput vs_main(VSInput in [[stage_in]]) {
    VSOutput out;
    out.color = in.color;
    out.position = float4(in.position, 1.0);
    return out;
}
"""

internal val FRAG_MSL = """
#include <metal_stdlib>
#include <simd/simd.h>

using namespace metal;

struct FSInput {
    float3 color [[user(locn0)]];
};

fragment float4 fs_main(FSInput in [[stage_in]]) {
    return float4(in.color, 1.0);
}
"""
