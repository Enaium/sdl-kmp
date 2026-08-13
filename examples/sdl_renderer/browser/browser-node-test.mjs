// Node.js test harness for the wasm browser app.
//
// SDL3's Emscripten driver expects browser globals even in dummy-driver
// mode, so stub the minimal set. Run with the dummy video/audio drivers:
//
//   SDL_VIDEO_DRIVER=dummy SDL_AUDIO_DRIVER=dummy \
//     node --input-type=module browser-node-test.mjs
//
// This exercises the full Kotlin/Wasm -> JS glue -> Emscripten SDL3 pipeline
// (init, window, renderer, render loop, quit) headlessly.

globalThis.window = {
    matchMedia: () => ({ addEventListener: () => {} }),
    addEventListener: () => {},
    requestAnimationFrame: (cb) => setTimeout(cb, 16),
    screenX: 0,
    screenY: 0,
    innerWidth: 800,
    innerHeight: 600,
    devicePixelRatio: 1,
    location: { href: 'http://localhost/' },
};
// The wasmJs entry drives its loop from the global requestAnimationFrame.
globalThis.requestAnimationFrame = (cb) => setTimeout(cb, 16);
globalThis.screen = { width: 800, height: 600, availWidth: 800, availHeight: 600 };
globalThis.document = {
    createElement: (tag) => ({
        tagName: tag.toUpperCase(),
        style: {},
        width: 800,
        height: 600,
        getContext: () => null,
        addEventListener: () => {},
        setPointerCapture: () => {},
    }),
    getElementById: () => null,
    querySelector: () => null,
    addEventListener: () => {},
    body: { appendChild: () => {} },
    head: { appendChild: () => {} },
    createEvent: () => ({ initEvent: () => {} }),
    visibilityState: 'visible',
};
Object.defineProperty(globalThis, 'navigator', {
    configurable: true,
    value: {
        userAgent: 'node',
        platform: 'node',
        hardwareConcurrency: 4,
        language: 'en',
        languages: ['en'],
    },
});
globalThis.performance = { now: () => Date.now() };
globalThis.CanvasRenderingContext2D = function () {};

// AudioContext stub so SDL3's emscripten audio driver init does not throw.
globalThis.AudioContext = class {
    constructor() { this.sampleRate = 48000; }
    createBuffer() { return { getChannelData: () => new Float32Array(0) }; }
    createBufferSource() { return { connect: () => {}, start: () => {}, buffer: null }; }
    createScriptProcessor() { return { connect: () => {} }; }
    createAnalyser() { return { connect: () => {} }; }
    createGain() { return { connect: () => {}, gain: { value: 1 } }; }
    destination = {};
    resume() { return Promise.resolve(); }
};

const { initSdlKmp } = await import('./sdl_kmp_glue.js');
await initSdlKmp();

// On emscripten there is no environment to read hints from, so force the
// dummy drivers here (the demo only falls back to them after a failed init,
// which is too late in a headless environment like Node).
sdl_kmp_SetHint('SDL_VIDEO_DRIVER', 'dummy');
sdl_kmp_SetHint('SDL_AUDIO_DRIVER', 'dummy');

// Import the Kotlin/Wasm module; its runtime auto-invokes main().
await import('./sdl-kmp-examples-sdl_renderer-browser.mjs');
