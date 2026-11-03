# SM64 Emulator — davimikeben build

APK Android independente que roda **Super Mario 64 (N64)** com a ROM integrada, núcleo de
emulação libretro (mupen64plus_next GLES3) embutido, teclado virtual completo, menu próprio
e otimização para Mali-G52.

## ✅ Verificação do APK final

| Verificação | Resultado |
|---|---|
| Conteúdo do APK | ✓ `assets/rom/super_mario_64.z64` presente (8.388.608 bytes) |
| Integridade (SHA-256) | ✓ `17ce077343c6133f8c9f2d6d6d9a4ab62c8cd2aa57c40aea1f490b4c8bb21d91` — idêntico ao original |
| Bibliotecas nativas | ✓ `lib/arm64-v8a/libretro_mupen64plus_next.so` (6.203.328 bytes) + `lib/arm64-v8a/libsm64_emulator.so` |
| APK final | 20.988.830 bytes (~20 MiB) |

## Estrutura do projeto

```
app/
├── src/main/
│   ├── assets/rom/
│   │   ├── super_mario_64.z64              ← ROM integrada (8 MiB)
│   │   └── super_mario_64.z64.sha256       ← hash para verificação runtime
│   ├── cpp/                                ← Wrapper JNI (libsm64_emulator.so)
│   │   ├── sm64_jni.cpp
│   │   ├── libretro_dlopen.{h,cpp}         ← dlopen/dlsym do core libretro
│   │   ├── gl_renderer.{h,cpp}             ← OpenGL ES 3.0 otimizado Mali-G52
│   │   ├── audio_sink.{h,cpp}              ← OpenSLES de baixa latência
│   │   ├── perf_pacer.{h,cpp}              ← Frame pacing + FPS
│   │   └── CMakeLists.txt
│   ├── java/com/davimikeben/sm64emulator/
│   │   ├── core/                           ← LibretroBridge + EmulatorRuntime
│   │   ├── ui/                             ← MainActivity, GameActivity, menus
│   │   ├── input/                          ← VirtualKeyboardView (multitouch)
│   │   ├── overlay/                        ← FpsOverlayView
│   │   ├── perf/                           ← PerfManager (Mali-G52 detection)
│   │   ├── rom/                            ← RomLoader (assets→filesDir)
│   │   └── util/                           ← SettingsManager, JsonHelper
│   ├── jniLibs/arm64-v8a/libretro_mupen64plus_next.so   ← Core libretro pré-compilado
│   └── res/                                ← Layouts, themes, drawables
├── build.gradle.kts
└── proguard-rules.pro

scripts/
├── setup_android_sdk.sh        ← Baixa Android SDK + NDK + CMake
├── fetch_libretro_core.sh      ← Baixa core mupen64plus_next_gles3 do buildbot libretro
├── build_apk.sh                ← Wrapper do gradle assembleRelease
└── verify_apk.sh               ← Verifica ROM dentro do APK (contente + SHA-256)
```

## Como compilar

```bash
bash scripts/setup_android_sdk.sh
bash scripts/fetch_libretro_core.sh
bash scripts/build_apk.sh
bash scripts/verify_apk.sh
```

O APK final fica em `download/sm64_emulator-release.apk`.

## Recursos implementados

- **PLAY** direto do menu → carrega ROM automaticamente
- **Teclado virtual completo**: D-pad (câmera), stick analógico (movimento), A (pulo), B (atacar), C-buttons, L (Z-trigger), R, Start — todos com multitouch e opacidade ajustável
- **Menu flutuante** durante o jogo (mover/redimensionar botões, transparência, gráficos, monitoramento, reiniciar, salvar, voltar)
- **Contador de FPS discreto** + opcionalmente CPU/RAM/Temp
- **3 modos de desempenho**: Performance / Equilibrado / Qualidade
- **Detecção de Mali-G52** e configuração dinâmica
- **Renderização OpenGL ES 3.0** com EGL + OpenSLES de baixa latência
- **Salvamento/restauração** completo de configurações em JSON
- **Frame pacing** otimizado por modo

## Notas

- O core libretro (`libretro_mupen64plus_next.so`) é obtido do buildbot oficial do
  Libretro (https://buildbot.libretro.com) — versão GLES3, compatível com Mali-G52.
- A ROM é extraída dos assets para `context.filesDir/rom/` na primeira execução,
  com verificação SHA-256 antes de ser carregada.
- APK é unsigned — para instalação em dispositivo, deve ser assinado (debug ou release).
