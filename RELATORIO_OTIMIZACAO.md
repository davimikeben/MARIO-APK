# Relatório de otimização (Mali-G52 e Android)

## Alvo de otimização
GPUs ARM Mali-G52 (mídia de gama média) e dispositivos Android em geral.

## Detecção de capacidades (PerformanceManager.java)
O aplicativo detecta em tempo de execução e não presume uniformidade entre
dispositivos Mali-G52:
- Renderizador e fornecedor da GPU (`GL_RENDERER` / `GL_VENDOR`).
- Versão do OpenGL ES.
- Tamanho máximo de textura (`GL_MAX_TEXTURE_SIZE`).
- Detecção específica de Mali-G52.
- Núcleos de CPU disponíveis.
- Memória total do sistema.
- Versão do Android.

## Três modos de desempenho
| Modo | Foco | Resolução recomendada | FPS alvo |
|------|------|----------------------|----------|
| Desempenho | Fluidez máxima | 960×540 | 60 |
| Equilibrado | Qualidade x desempenho | 1280×720 | 60 |
| Qualidade | Melhor imagem | 1920×1080 | 30 |

## Técnicas aplicadas
- **Renderização eficiente:** OpenGL ES 2.0, textura RGBA direta, filtro `NEAREST`
  (fiel ao N64 e mais barato que linear), sem blending/dithering desnecessários.
- **Frame pacing:** loop controlado por tempo alvo; `Thread.sleep` no intervalo
  restante para não gastar CPU/GPU desnecessariamente.
- **Sincronização de quadros:** `RENDERMODE_CONTINUOUSLY` com pacing.
- **Ajuste de resolução:** selecionável conforme modo e capacidade do dispositivo.
- **Carregamento inteligente:** ROM lida via `AssetManager`; extraída para
  `context.getFilesDir()/rom/` somente se o núcleo exigir caminho físico, sem
  interação do usuário.
- **Redução de efeitos:** sem pós-processamento pesado; shaders mínimos.
- **Memória:** `largeHeap="true"`, `extractNativeLibs="true"`, ProGuard +
  `shrinkResources true` para remoção de recursos não usados.
- **Bibliotecas:** apenas as necessárias; `noCompress 'rom'` para manter a ROM
  legível diretamente dos assets.
- **Energético:** frame pacing evita renderizar quadros além do alvo de FPS,
  reduzindo consumo de GPU/CPU.

## Compressão máxima do APK
- `minifyEnabled true` + `shrinkResources true` (remoção de recursos não usados).
- ProGuard com ofuscação e remoção de código morto.
- `pickFirst` para evitar duplicatas de bibliotecas nativas (`libc++_shared.so`).
- Vetores de ícones (sem múltiplos PNGs).
- Assets de áudio/imagem comprimidos; `compressPngs true`.

> Nota: os tamanhos original vs final só poderão ser comparados após compilar o
> APK com a ROM real integrada (ver INSTRUCOES_CONCLUSAO.md).
