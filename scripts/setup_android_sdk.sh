#!/usr/bin/env bash
# setup_android_sdk.sh — baixa Android command-line tools + NDK + cmake no sandbox.
# Idempotente: re-executa sem rebaixar o que já existe.
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
NDK_VERSION="${NDK_VERSION:-27.2.12479018}"
CMAKE_VERSION="${CMAKE_VERSION:-3.22.1}"

mkdir -p "$ANDROID_HOME/cmdline-tools"
cd "$ANDROID_HOME/cmdline-tools"

if [[ ! -d "latest" ]]; then
    echo "[fetch] cmdline-tools"
    curl -fL -o cmdtools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    unzip -q cmdtools.zip
    mv cmdline-tools latest
    rm cmdtools.zip
fi

export ANDROID_HOME
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0" \
           "ndk;${NDK_VERSION}" "cmake;${CMAKE_VERSION}"

echo "ANDROID_HOME=$ANDROID_HOME"
echo "NDK: $ANDROID_HOME/ndk/${NDK_VERSION}"
echo "CMake: $ANDROID_HOME/cmake/${CMAKE_VERSION}"
