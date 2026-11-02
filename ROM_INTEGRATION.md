# Integração da ROM ao APK

## Local exato
Em um projeto Android com Gradle, a ROM deve ficar em:

```
app/src/main/assets/rom/<nome_da_rom>.<extensao>
```

Exemplo: `app/src/main/assets/rom/Super Mario 64.z64`

## Como o app localiza a ROM
1. `AssetRomLoader` abre a ROM diretamente dos **assets** via `AssetManager`
   (`context.getAssets().open("assets/rom/<nome>")`).
2. Se o núcleo de emulação exigir um caminho físico no disco, a ROM é extraída
   automaticamente para `context.getFilesDir()/rom/` sem interação do usuário.
3. O SHA-256 é calculado para verificação de integridade.

## Regras obrigatórias
- A ROM deve estar dentro do APK final.
- Não depender de downloads externos.
- Não ser arquivo vazio, atalho ou ficar de fora do APK.
- O app deve conseguir localizar e carregar a ROM sozinho.

## Arquivo identificado (metadados do TransferNow)
- Nome: Super Mario 64.zip
- Tamanho do ZIP: 6.099.886 bytes (~6,10 MB)
- Arquivo interno: 1 arquivo
- Validade: 2026-11-02 a 2026-11-09

> O binário real da ROM ainda não foi obtido neste sandbox (TransferNow exige JS
> e passa por Cloudflare). Deve ser baixado e colocado em `assets/rom/` antes da
> compilação.
