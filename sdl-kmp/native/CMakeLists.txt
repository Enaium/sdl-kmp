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

# Builds SDL3 as a static library and merges the cinterop helper object
# (sdl_helpers.c) into a copy of the resulting archive, so a single
# -staticLibrary flag in the cinterop definition embeds everything into the
# produced klib.
#
# The merged archive is emitted at SDL3_KMP_OUTPUT_DIR/libSDL3.a; the Gradle
# build passes -DSDL3_KMP_OUTPUT_DIR to point at build/native/<target>.
cmake_minimum_required(VERSION 3.21)
project(sdl3-kmp C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

if(NOT DEFINED SDL_SOURCE_DIR)
    set(SDL_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../SDL")
endif()
if(NOT DEFINED SDL3_KMP_OUTPUT_DIR)
    set(SDL3_KMP_OUTPUT_DIR "${CMAKE_CURRENT_BINARY_DIR}/out")
endif()

# SDL3 is consumed only through the merged static archive.
set(SDL_SHARED OFF CACHE BOOL "" FORCE)
set(SDL_STATIC ON CACHE BOOL "" FORCE)
set(SDL_TEST OFF CACHE BOOL "" FORCE)
set(SDL_EXAMPLES OFF CACHE BOOL "" FORCE)
set(SDL_TESTS OFF CACHE BOOL "" FORCE)
set(SDL_INSTALL OFF CACHE BOOL "" FORCE)
set(SDL_INSTALL_DOCS OFF CACHE BOOL "" FORCE)

add_subdirectory(${SDL_SOURCE_DIR} SDL)

add_library(sdl3-kmp-helpers OBJECT sdl_helpers.c)
target_include_directories(sdl3-kmp-helpers PRIVATE ${SDL_SOURCE_DIR}/include)

# glibc 2.38+ compatibility shims (strlcpy/strlcat/__isoc23_*) so the static
# library links against the older glibc bundled with Kotlin/Native.
if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
    target_sources(sdl3-kmp-helpers PRIVATE sdl_glibc_compat.c)
endif()

# Merge the helper object into a copy of the SDL3 archive. SDL3-static keeps
# its default output location; only the merged copy lives in SDL3_KMP_OUTPUT_DIR
# (this also avoids a circular dependency between output and input).
set(SDL3_KMP_MERGED_ARCHIVE "${SDL3_KMP_OUTPUT_DIR}/libSDL3.a")
add_custom_command(
    OUTPUT ${SDL3_KMP_MERGED_ARCHIVE}
    COMMAND ${CMAKE_COMMAND} -E make_directory ${SDL3_KMP_OUTPUT_DIR}
    COMMAND ${CMAKE_COMMAND} -E copy_if_different $<TARGET_FILE:SDL3-static> ${SDL3_KMP_MERGED_ARCHIVE}
    COMMAND ${CMAKE_AR} rcs ${SDL3_KMP_MERGED_ARCHIVE} $<TARGET_OBJECTS:sdl3-kmp-helpers>
    DEPENDS SDL3-static sdl3-kmp-helpers
    COMMENT "Merging sdl3-kmp helpers into libSDL3.a"
    COMMAND_EXPAND_LISTS
)
add_custom_target(sdl3-kmp-merged ALL DEPENDS ${SDL3_KMP_MERGED_ARCHIVE})
