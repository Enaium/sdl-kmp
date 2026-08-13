# sdl-kmp

Kotlin Multiplatform bindings for [SDL3](https://github.com/libsdl-org/SDL), with a curated common API backed by two implementations:

- **JVM**: the official [LWJGL](https://www.lwjgl.org) SDL3 bindings (`org.lwjgl:lwjgl-sdl`).
- **Native (Kotlin/Native)**: the SDL3 static library from this repository's `SDL` submodule is compiled per target with CMake and **embedded into the published klib**, so consumers get a fully self-contained binary (no dynamic SDL3 dependency). This includes the Android native targets (`androidNative*`), cross-compiled with the Android NDK.

## Supported platforms

| Platform   | Targets                                             | Implementation                     |
|------------|-----------------------------------------------------|------------------------------------|
| JVM        | `jvm` (Linux/macOS/Windows)                         | LWJGL SDL3 bindings                |
| macOS      | `macosArm64`, `macosX64`                            | cinterop + embedded static SDL3    |
| Linux      | `linuxX64`                                          | cinterop + embedded static SDL3    |
| Windows    | `mingwX64`                                          | cinterop + embedded static SDL3    |
| iOS        | `iosArm64`, `iosX64`, `iosSimulatorArm64`           | cinterop + embedded static SDL3    |
| tvOS       | `tvosArm64`, `tvosSimulatorArm64`                   | cinterop + embedded static SDL3    |
| Android    | `androidNativeArm64`, `androidNativeArm32`, `androidNativeX64`, `androidNativeX86` | cinterop + embedded static SDL3 (built with the NDK) |

Not supported: JS/WASM (out of scope), watchOS (SDL3 has no watchOS support) and visionOS (Kotlin/Native has no visionOS targets yet).

## Usage

`build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.enaium.sdl:sdl-kmp:1.0.4")
        }
    }
}
```

```kotlin
import cn.enaium.sdl.*

fun main() {
    SDL.setMainReady()

    if (!SDL.init(SDLInitFlags.VIDEO)) {
        error("SDL_Init failed: ${SDL.error()}")
    }

    SDL.createWindow("hello sdl-kmp", 800, 600).use { window ->
        SDL.createRenderer(window).use { renderer ->
            var running = true
            while (running) {
                while (true) {
                    val event = SDL.pollEvent() ?: break
                    when (event) {
                        is SDLEvent.Quit -> running = false
                        is SDLEvent.Key ->
                            if (event.down && event.keycode == SDLKeycode.ESCAPE) running = false
                        else -> Unit
                    }
                }

                renderer.drawColor = SDLColor(18, 18, 24)
                renderer.clear()

                renderer.drawColor = SDLColor(255, 0, 128)
                renderer.fillRect(SDLRect(100, 100, 200, 200))

                renderer.present()
                SDL.delay(16)
            }
        }
    }

    SDL.quit()
}
```

### Platform notes

- Native: call `SDL.setMainReady()` before `SDL.init` on the main thread. It is only
  **required on Apple platforms** (macOS/iOS/tvOS); on Linux/Windows it is a harmless
  no-op that records the calling thread as the main thread and never blocks.
- **Linux headless / CI**: with no (or an unreachable) `DISPLAY`, `SDL.init(SDL_INIT_VIDEO)`
  can block while `XOpenDisplay` tries to connect. Set `SDL_VIDEO_DRIVER=dummy` (hint or
  environment variable) before init, or export `DISPLAY` correctly.
- **Kotlin version compatibility**: the published klibs are built with Kotlin 2.4.0.
  Consuming them with a different Kotlin/Native version produces an `IrLinkageError`
  ("No function found for symbol ...") at the first SDL call. Keep the consumer's
  Kotlin version in sync.
- The `SDL_VIDEO_DRIVER=dummy` hint (environment variable or `SDL.setHint`) makes SDL run headless — useful for CI and servers.
- **macOS JVM**: requires `-XstartOnFirstThread` JVM argument (so AppKit/Cocoa can initialise). The example `runJvm` task already sets this.
- On Linux the static SDL3 is built with the X11/Wayland drivers loaded dynamically (`dlopen`), so the published klib has no link-time dependency on X11.
- **Android**: building an `androidNative*` target requires an installed Android NDK (found under `$ANDROID_HOME/ndk`); the SDL3 static library is cross-compiled with its CMake toolchain. At runtime the app must be launched through `org.libsdl.app.SDLActivity` (or a subclass), which loads the shared library and calls its exported `SDL_main` (see the `examples/sdl_renderer/android` module).

### Native linking

The SDL3 static library is embedded in each target's published klib (built per target by the `sdl-kmp/native/CMakeLists.txt` wrapper). The required frameworks/system libraries are recorded in the cinterop klib as `linkerOpts` (see `sdl.def`) and are applied automatically when the consumer's final binary is linked.

## Examples

All examples live under `examples/` as standalone KMP modules; each provides
`commonMain` logic and thin platform entry points (`main()` / `SDL_main`).

- **`examples/sdl_renderer`** — "bouncing box" demo using `SDL_Renderer`
  (renderer, textures, audio, input). Runs on JVM, macOS, Linux, Windows
  (MinGW) and Android (with its `android` submodule APK).
- **`examples/sdl_vulkan`** — minimal Vulkan triangle (gradient shaders) on
  JVM, macOS, Linux and Windows. On the JVM the LWJGL function table is wired
  to SDL's `SDL_Vulkan_GetVkGetInstanceProcAddr`; on native targets a small C
  helper builds the pipeline.
- **`examples/sdl_opengl`** — minimal OpenGL 3.3 core / GLES 3 triangle on
  JVM, macOS, Linux and Windows.
- **`examples/sdl_gpu`** — triangle rendered through the SDL3 GPU API
  (cross-backend: Metal on macOS, Vulkan on Android) entirely from
  `commonMain`. Runs on JVM, macOS, Linux, Windows and Android (with its
  `android` submodule APK).

```bash
# Publish the library to the local Maven repository first (macOS builds all
# Apple targets + JVM; run on Linux for the linuxX64/mingwX64 klibs).
./gradlew :sdl-kmp:publishToMavenLocal

# JVM (pass SDL_VIDEO_DRIVER=dummy for headless mode)
./gradlew :examples:sdl_renderer:jvmRun
SDL_VIDEO_DRIVER=dummy ./gradlew :examples:sdl_renderer:jvmRun

# Native
./gradlew :examples:sdl_renderer:runDebugExecutableMacosArm64
SDL_VIDEO_DRIVER=dummy ./gradlew :examples:sdl_renderer:runDebugExecutableLinuxX64

# GPU examples (macOS: needs a display; Vulkan needs a Vulkan driver)
./gradlew :examples:sdl_vulkan:jvmRun
./gradlew :examples:sdl_opengl:jvmRun
./gradlew :examples:sdl_gpu:jvmRun
./gradlew :examples:sdl_vulkan:runDebugExecutableLinuxX64
./gradlew :examples:sdl_opengl:runDebugExecutableLinuxX64
./gradlew :examples:sdl_gpu:runDebugExecutableMacosArm64
```

### Android examples

The `sdl_renderer` and `sdl_gpu` examples each have an `android` submodule:
an Android application (AGP) that runs the same demo. The KMP module builds
`libmain.so` for every `androidNative` ABI (exporting `SDL_main` from
`androidMain`); the Android app copies those into its `jniLibs` and its
`MainActivity` extends `org.libsdl.app.SDLActivity` (loaded from the SDL
submodule so it matches the statically linked SDL3 version), which loads
`libmain.so` and calls `SDL_main`.

```bash
# Build the APKs (requires an Android NDK; install the app on a device/emulator
# with adb).
./gradlew :examples:sdl_renderer:android:assembleDebug
./gradlew :examples:sdl_gpu:android:assembleDebug
adb install -r examples/sdl_renderer/android/build/outputs/apk/debug/android-debug.apk
```

## Development

```bash
# Unit + integration tests on the host platform
./gradlew :sdl-kmp:jvmTest :sdl-kmp:macosArm64Test   # macOS
./gradlew :sdl-kmp:jvmTest :sdl-kmp:linuxX64Test     # Linux
```

Building the Linux native SDL3 library (any `linuxX64` task) requires the
Wayland, X11 and audio development packages: on Debian/Ubuntu that is
`libwayland-dev libwayland-bin libxkbcommon-dev libegl-dev libdecor-0-dev`
plus the X11 `libx*` dev packages and
`libpipewire-0.3-dev libpulse-dev libasound2-dev` (these also gate the
PipeWire/Pulse/ALSA audio drivers at build time — without them the published
klib falls back to X11 and has no audio drivers). The GitHub Actions workflows
install these automatically.

## GitHub Actions

- `.github/workflows/test.yml` — manual trigger: macOS builds all Apple klibs and runs JVM + native tests; Linux runs `linuxX64Test`, cross-compiles `mingwX64`, and runs the renderer example headless; Android installs the NDK, builds the four `androidNative` klibs and assembles the `sdl_renderer`/`sdl_gpu` APKs.
- `.github/workflows/publish.yml` — manual workflow that publishes the metadata + JVM + Apple klibs from `macos-14`, the `linuxX64`/`mingwX64` klibs from `ubuntu-latest`, and the four `androidNative` klibs from `ubuntu-latest` (with the NDK) to Maven Central.

Required secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY` (base64 GPG keyring), `SIGNING_KEY_ID`, `SIGNING_PASSWORD`.

## License

MIT. The bundled SDL3 submodule is licensed under the [zlib license](https://github.com/libsdl-org/SDL/blob/main/LICENSE.txt).
