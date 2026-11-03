#!/usr/bin/env bash
# fetch_libretro_core.sh — baixa o core libretro mupen64plus_next pré-compilado
# do buildbot oficial do RetroArch/Libretro e o coloca em jniLibs/<abi>/.
#
# Fonte legítima: https://buildbot.libretro.com/nightly/android/latest/armeabi-v7a/mupen64plus_next_libretro_android.so.zip
#
# Uso:  ./scripts/fetch_libretro_core.sh
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JNIDIR="$PROJECT_DIR/app/src/main/jniLibs"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

declare -a ABIS=("arm64-v8a" "armeabi-v7a")
declare -a ABI_URLS=("arm64-v8a" "armeabi-v7a")
CORE_NAME="mupen64plus_next_gles3"

for i in "${!ABIS[@]}"; do
    ABI="${ABIS[$i]}"
    ABI_URL="${ABI_URLS[$i]}"
    DEST="$JNIDIR/$ABI/libretro_mupen64plus_next.so"
    if [[ -f "$DEST" && -s "$DEST" ]]; then
        echo "[skip] $DEST já existe ($(stat -c %s "$DEST") bytes)"
        continue
    fi
    mkdir -p "$JNIDIR/$ABI"
    URL="https://buildbot.libretro.com/nightly/android/latest/${ABI_URL}/${CORE_NAME}_libretro_android.so.zip"
    echo "[fetch] $URL"
    if ! curl -fL --retry 3 -o "$TMP/core_${ABI}.zip" "$URL"; then
        echo "[ERROR] Falha ao baixar core para $ABI" >&2
        exit 1
    fi
    unzip -o "$TMP/core_${ABI}.zip" -d "$TMP/${ABI}" >/dev/null
    SO="$(find "$TMP/${ABI}" -name '*.so' | head -1)"
    if [[ -z "$SO" ]]; then
        echo "[ERROR] .so não encontrado dentro do zip para $ABI" >&2
        exit 2
    fi
    cp "$SO" "$DEST"
    echo "[ok] $DEST ($(stat -c %s "$DEST") bytes)"
done

echo "Cores libretro prontos em jniLibs/."
