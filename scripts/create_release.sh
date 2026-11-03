#!/usr/bin/env bash
# create_release.sh — Cria Release v1.0.0 no GitHub e faz upload do APK assinado.
# Usa GitHub REST API diretamente (sem gh CLI).
set -euo pipefail

APK_SIGNED="/tmp/sm64_emulator-release-signed.apk"
APK_UNSIGNED="/home/z/my-project/download/sm64_emulator-release.apk"
TAG="v1.0.0"
REPO="davimikeben/MARIO-APK"

if [[ -z "${GH_TOKEN:-}" ]]; then
  echo "[ERROR] GH_TOKEN env var vazia — exporte GH_TOKEN antes de rodar." >&2
  exit 1
fi
AUTH="-H \"Authorization: token $GH_TOKEN\" -H \"Accept: application/vnd.github+json\""

if [[ ! -f "$APK_SIGNED" ]]; then
  echo "[ERROR] APK assinado não encontrado em $APK_SIGNED" >&2
  exit 1
fi

# Calcula hashes e tamanho
APK_SIZE=$(stat -c %s "$APK_SIGNED")
APK_SHA=$(sha256sum "$APK_SIGNED" | cut -d' ' -f1)
APK_UNSIGNED_SIZE=$(stat -c %s "$APK_UNSIGNED")
ROM_SIZE=8388608

echo "=== Criando Release $TAG em $REPO ==="

BODY=$(cat <<JSON
{
  "tag_name": "$TAG",
  "target_commitish": "main",
  "name": "SM64 Emulator v1.0.0 — APK com ROM integrada (davimikeben)",
  "body": "## APK Android independente — Super Mario 64 (Nintendo 64)\n\n**Um único APK instalável com:**\n- ROM \`Super Mario 64 (USA).z64\` integrada (8 MiB)\n- Core libretro \`mupen64plus_next_gles3\` embutido (6,2 MiB)\n- Wrapper JNI nativo \`libsm64_emulator.so\` (OpenGL ES 3.0 + OpenSLES)\n- Teclado virtual completo (multitouch): D-pad, stick analógico, A/B/X/Y, L/R, Start, C-buttons\n- Menu principal próprio (PLAY / Teclado / Desempenho / Gráficos / Áudio / Tela / Monitoramento / Salvar / Restaurar)\n- Menu flutuante in-game (mover/redimensionar botões, transparência, gráficos, monitoramento, reiniciar)\n- Contador de FPS discreto + opcional CPU/RAM/Temperatura\n- 3 modos de desempenho: Performance / Equilibrado / Qualidade\n- Detecção e otimização automática para Mali-G52\n\n## Verificações realizadas\n\n| # | Verificação | Resultado |\n|---|---|---|\n| 1 | Conteúdo do APK (ROM em \`assets/rom/super_mario_64.z64\`) | ✓ PASSOU |\n| 2 | Integridade SHA-256 da ROM (extraída do APK == original) | ✓ PASSOU |\n| 3 | Carregamento runtime da ROM | Pendente (requer dispositivo) |\n| 4 | Execução do core libretro | Pendente (requer dispositivo) |\n| 5 | Controles virtuais (multitouch) | Pendente (requer dispositivo) |\n\n## Tamanhos\n\n| Item | Bytes | Tamanho |\n|---|---|---|\n| ZIP original (ROM comprimida) | 6.099.886 | 5,82 MiB |\n| ROM descomprimida (.z64) | 8.388.608 | 8 MiB |\n| APK final (signed) | $APK_SIZE | $(echo "scale=2; $APK_SIZE/1024/1024" | bc) MiB |\n| APK final (unsigned) | $APK_UNSIGNED_SIZE | $(echo "scale=2; $APK_UNSIGNED_SIZE/1024/1024" | bc) MiB |\n\n## Hash SHA-256 do APK assinado\n\`\`\`\n$APK_SHA\n\`\`\`\n\n## Hash SHA-256 da ROM (preservada no APK)\n\`\`\`\n17ce077343c6133f8c9f2d6d6d9a4ab62c8cd2aa57c40aea1f490b4c8bb21d91\n\`\`\`\n\n## Instalação\n\n1. Baixe \`sm64_emulator-release.apk\` abaixo\n2. No Android: Configurações → Segurança → permitir Fontes desconhecidas\n3. Instale o APK\n4. Abra o aplicativo — o menu aparece, configure controles e pressione **PLAY**\n\nAPK assinado com keystore debug (CN=Android Debug, O=Android, C=US).",
  "draft": false,
  "prerelease": false
}
JSON
)

RESPONSE=$(curl -sSL -X POST \
  -H "Authorization: token $GH_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -d "$BODY" \
  "https://api.github.com/repos/$REPO/releases")

echo "$RESPONSE" | head -50

# Extrai ID e upload_url da resposta
RELEASE_ID=$(echo "$RESPONSE" | grep -oE '"id":[[:space:]]*[0-9]+' | head -1 | grep -oE '[0-9]+')
UPLOAD_URL_TEMPLATE=$(echo "$RESPONSE" | grep -oE '"upload_url":[[:space:]]*"[^"]+"' | head -1 | sed -E 's/.*"upload_url":[[:space:]]*"([^"]+)".*/\1/')
UPLOAD_URL=$(echo "$UPLOAD_URL_TEMPLATE" | sed 's/{?name,label}//')

echo
echo "=== Release criada ==="
echo "Release ID: $RELEASE_ID"
echo "Upload URL: $UPLOAD_URL"
echo
echo "=== Enviando APK como asset ==="

curl -sSL -X POST \
  -H "Authorization: token $GH_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary @"$APK_SIGNED" \
  "${UPLOAD_URL}?name=sm64_emulator-release.apk&label=APK%20asssigned%20%28arm64-v8a%29" \
  | head -30

echo
echo "=== Enviando SHA256 do APK ==="
echo "$APK_SHA  sm64_emulator-release.apk" > /tmp/sm64_emulator-release.apk.sha256
curl -sSL -X POST \
  -H "Authorization: token $GH_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "Content-Type: text/plain" \
  --data-binary @/tmp/sm64_emulator-release.apk.sha256 \
  "${UPLOAD_URL}?name=sm64_emulator-release.apk.sha256&label=SHA-256%20checksum" \
  | head -20

unset GH_TOKEN
echo
echo "=== DONE ==="