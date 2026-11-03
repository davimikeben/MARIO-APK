# Relatório de Verificação do APK — SM64 Emulator (davimikeben)

## Identificação da ROM
- **Arquivo ZIP baixado**: `Super Mario 64.zip` (6.099.886 bytes / 5,82 MiB)
- **Arquivo dentro do ZIP**: `Super Mario 64 (USA).z64`
- **Extensão**: `.z64` (Nintendo 64 ROM, byte order big-endian nativo)
- **Tamanho da ROM**: 8.388.608 bytes (8 MiB exatos)
- **Plataforma**: Nintendo 64
- **Título interno (header)**: `SUPER MARIO 64` (NSME, Rev.00, USA)
- **Hash SHA-256**: `17ce077343c6133f8c9f2d6d6d9a4ab62c8cd2aa57c40aea1f490b4c8bb21d91`
- **Núcleo de emulação**: mupen64plus_next (libretro, variante GLES3)

## Resultados das verificações

### VERIFICAÇÃO 1 — Conteúdo do APK
- Status: **PASSOU ✓**
- `assets/rom/super_mario_64.z64` está presente no APK compilado (8.388.608 bytes)
- `lib/arm64-v8a/libretro_mupen64plus_next.so` presente (6.203.328 bytes)
- `lib/arm64-v8a/libsm64_emulator.so` presente (73.120 bytes — wrapper JNI)

### VERIFICAÇÃO 2 — Integridade da ROM
- Status: **PASSOU ✓**
- SHA-256 esperado: `17ce077343c6133f8c9f2d6d6d9a4ab62c8cd2aa57c40aea1f490b4c8bb21d91`
- SHA-256 obtido da ROM extraída do APK: `17ce077343c6133f8c9f2d6d6d9a4ab62c8cd2aa57c40aea1f490b4c8bb21d91`
- Hashes idênticos — ROM preservada byte a byte

### VERIFICAÇÃO 3 — Carregamento (estático)
- Status: **PENDENTE** — Requer dispositivo Android ou emulador Android para executar.
  O código está implementado em `RomLoader.ensureExtracted()` que:
  1. Copia `assets/rom/super_mario_64.z64` para `context.filesDir/rom/super_mario_64.z64`
  2. Verifica SHA-256 antes de devolver o caminho ao core
  3. Em caso de divergência, apaga e re-extrai

### VERIFICAÇÃO 4 — Execução real do core
- Status: **PENDENTE** — Requer dispositivo Android para teste runtime.
  O fluxo implementado é:
  - `GameActivity.surfaceCreated` → `EmulatorRuntime.start(corePath, surface, onReady, onError)`
  - `nativeInit` → dlopen(`libretro_mupen64plus_next.so`) → `retro_init()`
  - `nativeLoadRom` → `retro_load_game(&game_info)` com `path` apontando para a ROM extraída
  - `nativeRunFrame` em loop → `retro_run()` + `video_refresh` callback → GLRenderer

### VERIFICAÇÃO 5 — Controles
- Status: **PENDENTE** — Requer toque em tela real.
  Implementação em `VirtualKeyboardView.onTouchEvent`:
  - Multitouch real via `MotionEvent.ACTION_POINTER_DOWN/UP`
  - 12 botões virtuais (D-pad, A, B, X, Y, L, R, Start, Select + stick analógico)
  - Cada toque dispara `LibretroBridge.nativeSetInput(port=0, JOYPAD, button, value)`

### VERIFICAÇÃO 6 — Resultado final
- APK final: `download/sm64_emulator-release.apk` (20.988.830 bytes / ~20 MiB)
- APK é unsigned — assinar antes de instalar
- Tamanho original (ZIP ROM): 6.099.886 bytes
- Tamanho ROM descomprimida: 8.388.608 bytes
- Tamanho APK final: 20.988.830 bytes (inclui ROM + core libretro + wrapper JNI + UI + assets)
- Compressão eficiente mantida (ROM .z64 marcada como noCompress para preservar bytes)

## Notas
- A compilação foi feita no sandbox Linux x86_64 usando:
  - Android SDK cmdline-tools + NDK 26.1.10909125 + CMake 3.22.1
  - JDK 21 (Temurin)
  - Gradle 8.7 + AGP 8.5.2 + Kotlin 1.9.24
- Apenas ABI `arm64-v8a` foi compilada (Mali-G52 phones são todos 64-bit)
- O core libretro foi baixado do buildbot oficial: `https://buildbot.libretro.com/nightly/android/latest/arm64-v8a/mupen64plus_next_gles3_libretro_android.so.zip`
