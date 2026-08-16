cmake_minimum_required(VERSION 3.16)

project(sdl3-kmp-jni C CXX)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Builds the JNI shared library (libsdl_jni) for the JVM target. SDL3 is
# compiled statically from the SDL submodule and linked into the JNI library,
# so the resulting artifact is self-contained (only the OS frameworks are
# referenced at runtime).
if(NOT DEFINED SDL_SOURCE_DIR)
    set(SDL_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../SDL")
endif()

set(SDL_SHARED OFF CACHE BOOL "" FORCE)
set(SDL_STATIC ON CACHE BOOL "" FORCE)
set(SDL_TEST OFF CACHE BOOL "" FORCE)
set(SDL_EXAMPLES OFF CACHE BOOL "" FORCE)
set(SDL_TESTS OFF CACHE BOOL "" FORCE)
set(SDL_INSTALL OFF CACHE BOOL "" FORCE)
set(SDL_INSTALL_DOCS OFF CACHE BOOL "" FORCE)

add_subdirectory(${SDL_SOURCE_DIR} SDL)

add_library(sdl_jni SHARED
    jni_bridge.cpp
    jni_audio.cpp
    jni_input.cpp
    jni_gl.cpp
    jni_gpu.cpp
    jni_tools.cpp
)
set_target_properties(sdl_jni PROPERTIES OUTPUT_NAME sdl_jni)

# SDL_main.h redefines main to SDL_main on Windows (and Unix builds link
# SDL's own SDL_main stub); this is a library, not an executable, so tell SDL
# the main() handling is the application's business (same as the cinterop
# compilerOpts in sdl.def).
target_compile_definitions(sdl_jni PRIVATE SDL_MAIN_HANDLED)

target_include_directories(sdl_jni PRIVATE
    ${SDL_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}
)

if(JNI_INCLUDE_DIR)
    target_include_directories(sdl_jni PRIVATE ${JNI_INCLUDE_DIR})
else()
    find_package(JNI REQUIRED)
    target_include_directories(sdl_jni PRIVATE ${JNI_INCLUDE_DIRS})
endif()
if(JNI_INCLUDE_DIR_PLATFORM)
    target_include_directories(sdl_jni PRIVATE ${JNI_INCLUDE_DIR_PLATFORM})
endif()

# SDL3-static is linked into the JNI shared library, so it must be compiled
# position-independent (its default on macOS; required on Linux, where the
# OpenXR/X11 dlopen stubs otherwise fail to link into a .so).
set_target_properties(SDL3-static PROPERTIES POSITION_INDEPENDENT_CODE ON)

# sdl-ttf-kmp's libsdl_ttf_jni resolves the SDL3 symbols at runtime from this
# library (a single SDL3 instance per process). SDL3 builds with hidden
# visibility on Linux, so compile it with default visibility and export the
# SDL_* symbols; macOS exports them by default and MinGW exports all global
# symbols by default (kept explicit below for the JNI DLLs).
set_target_properties(SDL3-static PROPERTIES C_VISIBILITY_PRESET default)
if(UNIX AND NOT APPLE)
    target_link_options(sdl_jni PRIVATE -Wl,--export-dynamic-symbol-regex=^SDL_)
endif()
if(WIN32)
    target_link_options(sdl_jni PRIVATE -Wl,--export-all-symbols)
endif()

# SDL3-static propagates its (LINK_ONLY) dependencies - the macOS frameworks,
# the Windows system libraries and -lm/-ldl/-lpthread on Linux - so the JNI
# shared library links everything SDL3 needs.
target_link_libraries(sdl_jni PRIVATE SDL3-static)

if(WIN32)
    set_target_properties(sdl_jni PROPERTIES PREFIX "")
endif()
