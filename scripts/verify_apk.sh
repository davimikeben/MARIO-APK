#!/usr/bin/env bash
# verify_apk.sh — Verifica a ROM dentro do APK compilado.
#
# VERIFICAÇÃO 1: conteúdo do APK (ROM está em assets/rom/?)
# VERIFICAÇÃO 2: integridade (extrai ROM do APK e compara SHA-256)
set -euo pipefail

APK="${1:-}"
if [[ -z "$APK" ]]; then
    APK="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/download/sm64_emulator-release.apk"
fi
if [[ ! -f "$APK" ]]; then
    echo "[ERROR] APK não encontrado: $APK" >&2
    exit 1
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "=== APK: $APK ($(stat -c %s "$APK") bytes) ==="
echo
echo "=== VERIFICAÇÃO 1 — Conteúdo do APK ==="
unzip -l "$APK" | grep -E "assets/rom/|libretro_mupen64plus|libsm64_emulator" || {
    echo "[FAIL] ROM ou libs não encontradas no APK" >&2
    exit 2
}
echo

echo "=== VERIFICAÇÃO 2 — Integridade da ROM ==="
unzip -j "$APK" "assets/rom/super_mario_64.z64" -d "$TMP" >/dev/null
EXTRACTED="$TMP/super_mario_64.z64"
EXTRACTED_SIZE=$(stat -c %s "$EXTRACTED")
EXTRACTED_HASH=$(sha256sum "$EXTRACTED" | cut -d' ' -f1)
EXPECTED_HASH="17ce077343c6133f8c9f2d6d6d9a4ab62c8cd2aa57c40aea1f490b4c8bb21d91"

echo "Arquivo no APK: assets/rom/super_mario_64.z64"
echo "Tamanho empacotado: $EXTRACTED_SIZE bytes"
echo "SHA-256 esperado:   $EXPECTED_HASH"
echo "SHA-256 obtido:     $EXTRACTED_HASH"
if [[ "$EXTRACTED_HASH" == "$EXPECTED_HASH" ]]; then
    echo "[PASS] ✓ ROM íntegra — hashes idênticos"
else
    echo "[FAIL] ✗ Hashes divergentes" >&2
    exit 3
fi

echo
echo "=== Resumo de tamanho ==="
ORIGINAL_ZIP="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/work/Super Mario 64.zip"
if [[ -f "$ORIGINAL_ZIP" ]]; then
    ZIP_SIZE=$(stat -c %s "$ORIGINAL_ZIP")
    ROM_SIZE=8388608
    APK_SIZE=$(stat -c %s "$APK")
    echo "ZIP original (ROM comprimida):       $ZIP_SIZE bytes ($((ZIP_SIZE/1024/1024)) MiB)"
    echo "ROM descomprimida (.z64):            $ROM_SIZE bytes ($((ROM_SIZE/1024/1024)) MiB)"
    echo "APK final (incluindo core+ROM+UI):   $APK_SIZE bytes ($((APK_SIZE/1024/1024)) MiB)"
fi
