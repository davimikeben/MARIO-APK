# Verificação de integridade da ROM no APK

## Status neste sandbox
- ✅ Identificado o pacote no TransferNow: **Super Mario 64.zip** (6.099.886 bytes).
- ❌ Binário da ROM **não baixado** (TransferNow exige JS + desafio Cloudflare).
- ❌ APK **não compilado** (não há Android SDK/NDK/JDK neste ambiente).
- ❌ Testes em dispositivo **não realizados** (não há dispositivo/emulador).

Portanto, os resultados abaixo estão **pendentes** e devem ser preenchidos após a
conclusão em ambiente com Android SDK/NDK e um dispositivo Android.

## Verificações obrigatórias (a realizar)

| # | Verificação | Como realizar | Resultado |
|---|-------------|---------------|-----------|
| 1 | Conteúdo do APK | `unzip -l app-release.apk \| grep assets/rom` | Pendente |
| 2 | Integridade SHA-256 | Extrair ROM do APK e comparar `sha256sum` com o original | Pendente |
| 3 | Carregamento | Instalar APK e confirmar que o app localiza a ROM integrada | Pendente |
| 4 | Execução real | Iniciar núcleo e confirmar que o jogo roda | Pendente |
| 5 | Controles | Testar botões virtuais, multitouch, comunicação com o núcleo | Pendente |
| 6 | Resultado | Relatório consolidado | Pendente |

## Dados esperados (a preencher com os valores reais)

- Nome da ROM integrada: ____________________
- Extensão: ____________________
- Caminho interno no APK: `assets/rom/<nome>.<ext>`
- Tamanho original (bytes): ____________________
- Tamanho empacotado (bytes): ____________________
- Hash SHA-256 original: ____________________
- Hash SHA-256 extraído do APK: ____________________
- Integridade (coincide?): ____________________
- Teste de inicialização: ____________________
- Teste dos controles: ___________________>

> A regra do projeto proíbe considerar a entrega concluída sem que a presença da
> ROM dentro do APK e seu carregamento pelo aplicativo sejam efetivamente
> verificados.
