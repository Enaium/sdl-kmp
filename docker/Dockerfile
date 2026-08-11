# Test toolchain for sdl-kmp: used to verify the linuxX64 (and cross-compiled
# mingwX64) builds locally, mirroring the GitHub Actions ubuntu-latest job.
FROM ubuntu:24.04

RUN apt-get update -qq \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
        openjdk-21-jdk-headless \
        cmake \
        make \
        gcc \
        g++ \
        pkg-config \
        # X11 development headers (SDL3 builds its X11 video driver; the
        # library is dlopen'd at runtime, so only headers are needed).
        libx11-dev \
        libxext-dev \
        libxcursor-dev \
        libxi-dev \
        libxfixes-dev \
        libxrandr-dev \
        libxrender-dev \
        libxss-dev \
        libxtst-dev \
        # MinGW cross toolchain for the mingwX64 static library.
        gcc-mingw-w64-x86-64 \
        g++-mingw-w64-x86-64 \
        git \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH
