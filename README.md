# MARIO-APK

Projeto Android: APK independente com ROM integrada (Super Mario 64), núcleo de
emulação interno, teclado virtual profissional, menu próprio, contador de FPS
e otimização extrema para Mali-G52.

## Branch de trabalho
`cline/rpgnnmrf` (backup de progresso). O PR deve ser aberto para `main`,
autoria **davimikeben**.

## Estrutura
- `app/src/main/assets/rom/` — destino da ROM integrada (ver `ROM_INTEGRATION.md`).
- `app/src/main/java/com/davimikeben/marioapk/` — código-fonte (Java).
- `app/jni/` — ponte JNI e CMake do núcleo de emulação N64.
- `app/build.gradle` — build Gradle (NDK, abiFilters, ProGuard, shrinkResources).

## Status (este sandbox)
- ❌ Não há Android SDK/NDK/JDK → APK não compilado aqui.
- ❌ Link TransferNow protegido por Cloudflare/JS → binário da ROM não baixado.
- ✅ Projeto-fonte completo, estrutura de assets, JNI, UI, teclado, menu flutuante,
  FPS, gerenciador de desempenho (Mali-G52) e documentação criados.

## Como concluir
Consulte `INSTRUCOES_CONCLUSAO.md` para baixar a ROM, integrar, compilar,
verificar a presença da ROM no APK (SHA-256) e testar em dispositivo Android.

> Regra absoluta: não entregar APK sem a ROM integrada e verificável.
