# Instruções para concluir o projeto (ambiente com Android SDK/NDK)

Este sandbox **não possui** Android SDK, NDK, JDK nem Gradle instalado, e o link
da ROM (TransferNow) exige execução de JavaScript e passa por desafio Cloudflare,
por isso não foi possível baixar o binário da ROM nem compilar o APK aqui.

Para concluir a entrega, siga estes passos em um computador com Android Studio /
Android SDK + NDK + JDK:

## 1. Baixar o arquivo ZIP da ROM
- Link: https://www.transfernow.net/dl/20261102MwRq75mc
- Conteúdo identificado (metadados da página): **Super Mario 64.zip**
  - Tamanho do ZIP: 6.099.886 bytes (~6,10 MB)
  - 1 arquivo interno
  - Validade: 2026-11-02 até 2026-11-09
- O download direto via `curl` retorna uma página HTML (Next.js) e um desafio
  Cloudflare; use um navegador autenticado para obter o ZIP real.

## 2. Extrair e identificar a ROM
- Extraia o ZIP localmente.
- Identifique o arquivo efetivo da ROM (nome, extensão, tamanho).
- Calcule o **SHA-256** do arquivo original:
  ```bash
  sha256sum <arquivo_rom>
  ```

## 3. Integrar a ROM ao projeto
- Copie o arquivo da ROM para:
  ```
  app/src/main/assets/rom/<nome_da_rom>.<extensao>
  ```
  Exemplo: `app/src/main/assets/rom/Super Mario 64.z64`
- Atualize `app/src/main/java/com/davimikeben/marioapk/RomInfo.java` com os dados
  reais (name, extension, sizeBytes, sha256, platform, emulatorCore).

## 4. Fornecer o núcleo de emulação nativo (NDK)
- O núcleo N64 (mupen64plus / ARES / Parallel N64) deve ser compilado como
  biblioteca nativa `.so` com o Android NDK e colocado em:
  ```
  app/src/main/jniLibs/<abi>/libn64emu.so
  ```
  ou via `externalNativeBuild` (CMake) já configurado em `app/build.gradle`.
- Abis suportadas: `armeabi-v7a`, `arm64-v8a`, `x86_64`.

## 5. Compilar o APK
```bash
cd /caminho/do/projeto
# defina o NDK em local.properties (ex: ndk.dir=/caminho/do/android-ndk)
./gradlew assembleRelease
```
O APK resultará em:
```
app/build/outputs/apk/release/app-release.apk
```

## 6. Verificar que a ROM está dentro do APK
```bash
unzip -l app/build/outputs/apk/release/app-release.apk | grep assets/rom
# deve listar assets/rom/<nome>.<ext>
```

## 7. Verificar integridade (SHA-256)
```bash
# extraia a ROM do APK
unzip -p app/build/outputs/apk/release/app-release.apk assets/rom/<nome>.<ext> > /tmp/rom_do_apk.<ext>
sha256sum /tmp/rom_do_apk.<ext>
# compare com o SHA-256 do arquivo original (deve coincidir)
```

## 8. Instalar e testar em dispositivo/emulador Android
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```
Confirme:
  - App abre e exibe o menu próprio (sem interface de emulador tradicional).
  - ROM é localizada e carregada sozinha (sem pedir ao usuário selecionar ROM).
  - Pressionar ▶️ PLAY inicia o jogo diretamente.
  - Contador de FPS aparece (apenas FPS por padrão).
  - Teclado virtual responde ao multitouch.
  - Menu flutuante abre dentro do jogo.

## 9. Entregáveis finais
  1. APK final instalável (`app-release.apk`).
  2. Projeto-fonte completo (este repositório).
  3. ROM integrada ao APK (verificada).
  4. Núcleo de emulação integrado.
  5. Teclado virtual completo.
  6. Menu personalizado.
  7. Menu flutuante.
  8. Contador de FPS.
  9. Configurações de desempenho (Mali-G52).
  10. Relatório de otimização (`RELATORIO_OTIMIZACAO.md`).
  11. Verificação de integridade da ROM (`VERIFICACAO_ROM.md`).
  12. Resultado do teste de execução (preencher após testes).
  13. Comparação tamanho original vs final.

> ⚠️ Regra absoluta: não considerar concluído se a ROM não estiver integrada e
> verificável dentro do APK, e se o jogo não iniciar de verdade.
