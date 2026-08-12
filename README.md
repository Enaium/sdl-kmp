# sdl-kmp

Kotlin Multiplatform bindings for [SDL3](https://github.com/libsdl-org/SDL), with a curated common API backed by two implementations:

- **JVM**: the official [LWJGL](https://www.lwjgl.org) SDL3 bindings (`org.lwjgl:lwjgl-sdl`). LWJGL does not support Android, so there is no Android target.
- **Native (Kotlin/Native)**: the SDL3 static library from this repository's `SDL` submodule is compiled per target with CMake and **embedded into the published klib**, so consumers get a fully self-contained binary (no dynamic SDL3 dependency).

## Supported platforms

| Platform   | Targets                                        | Implementation                     |
|------------|------------------------------------------------|------------------------------------|
| JVM        | `jvm` (Linux/macOS/Windows)                    | LWJGL SDL3 bindings                |
| macOS      | `macosArm64`, `macosX64`                       | cinterop + embedded static SDL3    |
| Linux      | `linuxX64`                                     | cinterop + embedded static SDL3    |
| Windows    | `mingwX64`                                     | cinterop + embedded static SDL3    |
| iOS        | `iosArm64`, `iosX64`, `iosSimulatorArm64`      | cinterop + embedded static SDL3    |
| tvOS       | `tvosArm64`, `tvosSimulatorArm64`              | cinterop + embedded static SDL3    |

Not supported: Android (LWJGL does not support it), JS/WASM (out of scope), watchOS (SDL3 has no watchOS support) and visionOS (Kotlin/Native has no visionOS targets yet).

## Usage

`build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.enaium.sdl:sdl-kmp:1.0.2")
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

### Native linking

The SDL3 static library is embedded in each target's published klib (built per target by the `sdl-kmp/native/CMakeLists.txt` wrapper). The required frameworks/system libraries are recorded in the cinterop klib as `linkerOpts` (see `sdl.def`) and are applied automatically when the consumer's final binary is linked.

## Example

The `example` module is a small "bouncing box" demo. All logic lives in `commonMain`; platform entry points only provide `main()`.

```bash
# Publish the library to the local Maven repository first (macOS builds all
# Apple targets + JVM; run on Linux for the linuxX64/mingwX64 klibs).
./gradlew :sdl-kmp:publishToMavenLocal

# JVM (pass SDL_VIDEO_DRIVER=dummy for headless mode)
./gradlew :example:runJvm
SDL_VIDEO_DRIVER=dummy ./gradlew :example:runJvm

# Native
./gradlew :example:runDebugExecutableMacosArm64
SDL_VIDEO_DRIVER=dummy ./gradlew :example:runDebugExecutableLinuxX64
```

## Development

```bash
# Unit + integration tests on the host platform
./gradlew :sdl-kmp:jvmTest :sdl-kmp:macosArm64Test   # macOS
./gradlew :sdl-kmp:jvmTest :sdl-kmp:linuxX64Test     # Linux (needs X11 dev headers)
```

## GitHub Actions

- `.github/workflows/test.yml` — runs on push/PR: macOS builds all Apple klibs and runs JVM + native tests; Linux runs `linuxX64Test`, cross-compiles `mingwX64`, and runs the example headless on both runners.
- `.github/workflows/publish.yml` — manual workflow that publishes the metadata + JVM + Apple klibs from `macos-14` and the `linuxX64`/`mingwX64` klibs from `ubuntu-latest` to Maven Central.

Required secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY` (base64 GPG keyring), `SIGNING_KEY_ID`, `SIGNING_PASSWORD`.

## License

MIT. The bundled SDL3 submodule is licensed under the [zlib license](https://github.com/libsdl-org/SDL/blob/main/LICENSE.txt).
