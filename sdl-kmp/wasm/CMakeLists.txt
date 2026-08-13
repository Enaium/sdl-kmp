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

# Builds SDL3 as a static library for the Emscripten (wasmJs) target. The
# sdl_wasm_shim.c wrapper is compiled and linked together with libSDL3.a by
# the Gradle linkWasmSdl task (emcc), producing the sdl_wasm.js module that
# Kotlin/Wasm drives through its JS glue.
cmake_minimum_required(VERSION 3.21)
project(sdl3-kmp-wasm C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

if(NOT DEFINED SDL_SOURCE_DIR)
    set(SDL_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../SDL")
endif()
if(NOT DEFINED SDL3_KMP_OUTPUT_DIR)
    set(SDL3_KMP_OUTPUT_DIR "${CMAKE_CURRENT_BINARY_DIR}/out")
endif()

set(SDL_SHARED OFF CACHE BOOL "" FORCE)
set(SDL_STATIC ON CACHE BOOL "" FORCE)
set(SDL_TEST OFF CACHE BOOL "" FORCE)
set(SDL_EXAMPLES OFF CACHE BOOL "" FORCE)
set(SDL_TESTS OFF CACHE BOOL "" FORCE)
set(SDL_INSTALL OFF CACHE BOOL "" FORCE)
set(SDL_INSTALL_DOCS OFF CACHE BOOL "" FORCE)

add_subdirectory(${SDL_SOURCE_DIR} SDL)

# Emit a copy of the SDL3 archive into the requested output directory.
add_custom_command(
    OUTPUT ${SDL3_KMP_OUTPUT_DIR}/libSDL3.a
    COMMAND ${CMAKE_COMMAND} -E make_directory ${SDL3_KMP_OUTPUT_DIR}
    COMMAND ${CMAKE_COMMAND} -E copy_if_different $<TARGET_FILE:SDL3-static> ${SDL3_KMP_OUTPUT_DIR}/libSDL3.a
    DEPENDS SDL3-static
    COMMENT "Copying SDL3 static library for wasm"
)
add_custom_target(sdl3-kmp-wasm-merged ALL DEPENDS ${SDL3_KMP_OUTPUT_DIR}/libSDL3.a)
