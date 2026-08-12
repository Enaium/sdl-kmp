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

add_library(sdl3-kmp-helpers OBJECT sdl_helpers.c sdl_vulkan_helpers.c)
target_include_directories(sdl3-kmp-helpers PRIVATE ${SDL_SOURCE_DIR}/include)

# glibc 2.38+ compatibility shims (strlcpy/strlcat/__isoc23_*) so the static
# library links against the older glibc bundled with Kotlin/Native.
if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
    target_sources(sdl3-kmp-helpers PRIVATE sdl_glibc_compat.c)
endif()

# The NDK's clang driver normally links the compiler-rt builtins archive
# (e.g. __aarch64_cas8_acq_rel for 64-bit atomics on arm64-v8a without LSE,
# __aeabi_* on armv7, ...) at the final link, but Kotlin/Native's linker does
# not, and its klib archive ordering is unreliable. Merge the per-ABI builtins
# INTO libSDL3.a so the app link (and dlopen) always resolves them.
if(ANDROID_NDK)
    if(ANDROID_ABI STREQUAL "arm64-v8a")
        set(SDL3_KMP_BUILTINS_ARCH aarch64)
    elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
        set(SDL3_KMP_BUILTINS_ARCH arm)
    elseif(ANDROID_ABI STREQUAL "x86_64")
        set(SDL3_KMP_BUILTINS_ARCH x86_64)
    elseif(ANDROID_ABI STREQUAL "x86")
        set(SDL3_KMP_BUILTINS_ARCH i686)
    endif()
    if(DEFINED SDL3_KMP_BUILTINS_ARCH)
        file(GLOB SDL3_KMP_BUILTINS_ARCHIVE
            "${ANDROID_NDK}/toolchains/llvm/prebuilt/*/lib/clang/*/lib/linux/libclang_rt.builtins-${SDL3_KMP_BUILTINS_ARCH}-android.a")
        list(LENGTH SDL3_KMP_BUILTINS_ARCHIVE SDL3_KMP_BUILTINS_COUNT)
        if(SDL3_KMP_BUILTINS_COUNT GREATER 0)
            list(GET SDL3_KMP_BUILTINS_ARCHIVE 0 SDL3_KMP_BUILTINS_ARCHIVE)
            execute_process(COMMAND ${CMAKE_AR} t ${SDL3_KMP_BUILTINS_ARCHIVE}
                OUTPUT_VARIABLE SDL3_KMP_BUILTINS_MEMBERS
                OUTPUT_STRIP_TRAILING_WHITESPACE)
            string(REPLACE "\n" ";" SDL3_KMP_BUILTINS_MEMBERS "${SDL3_KMP_BUILTINS_MEMBERS}")
            set(SDL3_KMP_BUILTINS_EXTRACT "${CMAKE_CURRENT_BINARY_DIR}/builtins")
            set(SDL3_KMP_BUILTINS_OBJECTS "")
            set(SDL3_KMP_BUILTINS_DIRS ${SDL3_KMP_BUILTINS_EXTRACT})
            foreach(mem IN LISTS SDL3_KMP_BUILTINS_MEMBERS)
                get_filename_component(mem_dir "${mem}" DIRECTORY)
                if(NOT mem_dir STREQUAL "" AND NOT mem_dir STREQUAL ".")
                    list(APPEND SDL3_KMP_BUILTINS_DIRS "${SDL3_KMP_BUILTINS_EXTRACT}/${mem_dir}")
                endif()
                list(APPEND SDL3_KMP_BUILTINS_OBJECTS "${SDL3_KMP_BUILTINS_EXTRACT}/${mem}")
            endforeach()
            list(REMOVE_DUPLICATES SDL3_KMP_BUILTINS_DIRS)
            message(STATUS "Merging compiler-rt builtins: ${SDL3_KMP_BUILTINS_ARCHIVE}")
        else()
            message(WARNING
                "compiler-rt builtins archive not found for ${ANDROID_ABI}; "
                "the linked library may fail at dlopen with undefined builtin symbols")
            unset(SDL3_KMP_BUILTINS_ARCHIVE)
        endif()
    endif()
endif()

# Merge the helper object into a copy of the SDL3 archive. SDL3-static keeps
# its default output location; only the merged copy lives in SDL3_KMP_OUTPUT_DIR
# (this also avoids a circular dependency between output and input).
set(SDL3_KMP_MERGED_ARCHIVE "${SDL3_KMP_OUTPUT_DIR}/libSDL3.a")
set(SDL3_KMP_MERGE_COMMANDS
    COMMAND ${CMAKE_COMMAND} -E make_directory ${SDL3_KMP_OUTPUT_DIR}
    COMMAND ${CMAKE_COMMAND} -E copy_if_different $<TARGET_FILE:SDL3-static> ${SDL3_KMP_MERGED_ARCHIVE}
    COMMAND ${CMAKE_AR} rcs ${SDL3_KMP_MERGED_ARCHIVE} $<TARGET_OBJECTS:sdl3-kmp-helpers>
)
if(DEFINED SDL3_KMP_BUILTINS_ARCHIVE)
    list(INSERT SDL3_KMP_MERGE_COMMANDS 0
        COMMAND ${CMAKE_COMMAND} -E rm -rf ${SDL3_KMP_BUILTINS_EXTRACT})
    foreach(dir IN LISTS SDL3_KMP_BUILTINS_DIRS)
        list(APPEND SDL3_KMP_MERGE_COMMANDS
            COMMAND ${CMAKE_COMMAND} -E make_directory ${dir})
    endforeach()
    list(APPEND SDL3_KMP_MERGE_COMMANDS
        COMMAND ${CMAKE_AR} x ${SDL3_KMP_BUILTINS_ARCHIVE} --output=${SDL3_KMP_BUILTINS_EXTRACT}
        COMMAND ${CMAKE_AR} rcs ${SDL3_KMP_MERGED_ARCHIVE} ${SDL3_KMP_BUILTINS_OBJECTS})
endif()
add_custom_command(
    OUTPUT ${SDL3_KMP_MERGED_ARCHIVE}
    ${SDL3_KMP_MERGE_COMMANDS}
    DEPENDS SDL3-static $<TARGET_OBJECTS:sdl3-kmp-helpers>
    COMMENT "Merging sdl3-kmp helpers into libSDL3.a"
    COMMAND_EXPAND_LISTS
)
add_custom_target(sdl3-kmp-merged ALL DEPENDS ${SDL3_KMP_MERGED_ARCHIVE})
