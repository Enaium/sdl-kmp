# Copyright (c) 2026 Enaium
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

# Builds the example-gpu triangle demos as a static library that the Gradle
# build embeds into the example-gpu cinterop klib via -staticLibrary.
# SDL3 symbols are not linked here; they come from the sdl-kmp klib at the
# final application link. The output archive is emitted at
# SDL3_KMP_OUTPUT_DIR/libsdl_kmp_triangle.a (the Gradle build passes
# -DSDL3_KMP_OUTPUT_DIR to point at build/native/<target>).
cmake_minimum_required(VERSION 3.21)
project(sdl-kmp-example-gpu C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

if(NOT DEFINED SDL_SOURCE_DIR)
    set(SDL_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../SDL")
endif()
if(NOT DEFINED SDL3_KMP_OUTPUT_DIR)
    set(SDL3_KMP_OUTPUT_DIR "${CMAKE_CURRENT_BINARY_DIR}/out")
endif()

add_library(sdl-kmp-triangle STATIC sdl_kmp_triangle.c)
target_include_directories(sdl-kmp-triangle PRIVATE
    ${SDL_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../src/nativeMain/cinterop)
set_target_properties(sdl-kmp-triangle PROPERTIES OUTPUT_NAME sdl_kmp_triangle)
set_target_properties(sdl-kmp-triangle PROPERTIES ARCHIVE_OUTPUT_DIRECTORY ${SDL3_KMP_OUTPUT_DIR})
