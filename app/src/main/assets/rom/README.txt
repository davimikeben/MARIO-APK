================================================================================
  PASTA DE RECURSOS INTERNOS — ROM INTEGRADA AO APK
================================================================================

ATENÇÃO: Esta pasta deve conter o arquivo real da ROM do jogo (exemplo:
Super Mario 64.z64), extraído do ZIP fornecido pelo usuário.

O arquivo deve ficar exatamente em:
  app/src/main/assets/rom/<nome_da_rom>.<extensao>

Regras obrigatórias:
  - A ROM deve fazer parte do APK final (empacotada via Gradle/assets).
  - Não depender de downloads externos.
  - Não ser um arquivo vazio, atalho ou deixada de fora.
  - O app deve localizá-la via AssetManager e, se necessário, extraí-la
    automaticamente para context.getFilesDir()/rom/.

Passos para integrar:
  1. Coloque o arquivo real da ROM nesta pasta com nome e extensão reais.
  2. Atualize RomInfo.java (nome, extensão, tamanho, SHA-256, platform, core).
  3. Recompile: ./gradlew assembleRelease
  4. Verifique no APK: unzip -l app/build/outputs/apk/release/app-release.apk
     deve listar assets/rom/<nome>.<ext>
  5. Compare o SHA-256 do arquivo dentro do APK com o original.

ARQUIVO IDENTIFICADO (metadados do TransferNow, não o binário):
  - Nome: Super Mario 64.zip
  - Tamanho do ZIP: 6.099.886 bytes (~6.10 MB)
  - Arquivo interno esperado: Super Mario 64.zip (1 arquivo)
  - Validade: 2026-11-02 até 2026-11-09
  - Link fonte: https://www.transfernow.net/dl/20261102MwRq75mc

OBSERVAÇÃO IMPORTANTE: Este sandbox não possui Android SDK/NDK/JDK, portanto
o APK não pôde ser compilado aqui nem o binário da ROM pôde ser baixado
automaticamente (o TransferNow exige execução de JS e passa por desafio
Cloudflare). O binário da ROM e a compilação do APK devem ser concluídos em
um ambiente com Android SDK/NDK instalado (ver INSTRUCOES_CONCLUSAO.md).

================================================================================
