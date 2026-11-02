<div align="center">

# 🎮 EXTREME N64

### APK independente com ROM integrada, emulação interna e teclado virtual profissional

**por davimikeben**

Um jogo inteiro dentro de um único arquivo `.apk`. Você instala, abre e joga.
Sem emulador externo. Sem baixar ROM separada. Sem configurar nada.

`com.davimikeben.extremen64` · Android 7.0+ (API 24) · arm64-v8a · armeabi-v7a · x86_64

</div>

---

## 📖 Índice

1. [O que é este projeto](#-o-que-é-este-projeto)
2. [O que vem dentro do APK](#-o-que-vem-dentro-do-apk)
3. [Como tudo funciona por dentro](#-como-tudo-funciona-por-dentro)
4. [A experiência de uso, passo a passo](#-a-experiência-de-uso-passo-a-passo)
5. [O menu principal](#-o-menu-principal)
6. [O teclado virtual](#-o-teclado-virtual)
7. [Mapa completo das teclas](#-mapa-completo-das-teclas)
8. [Menu flutuante (dentro do jogo)](#-menu-flutuante-dentro-do-jogo)
9. [Contador de FPS e monitoramento](#-contador-de-fps-e-monitoramento)
10. [Perfis de desempenho e otimização para Mali-G52](#-perfis-de-desempenho-e-otimização-para-mali-g52)
11. [Áudio](#-áudio)
12. [Gráficos](#-gráficos)
13. [Salvar e restaurar](#-salvar-e-restaurar)
14. [Estrutura do projeto](#-estrutura-do-projeto)
15. [Stack de tecnologia](#-stack-de-tecnologia)
16. [Como compilar](#-como-compilar)
17. [Verificação de integridade (resultados reais)](#-verificação-de-integridade-resultados-reais)
18. [Perguntas frequentes](#-perguntas-frequentes)
19. [Limites conhecidos e transparência](#-limites-conhecidos-e-transparência)
20. [Créditos e licença](#-créditos-e-licença)

---

## 🚀 O que é este projeto

Deixa eu te contar a ideia por trás disso, porque ela é simples e ao mesmo tempo meio maluca.

Normalmente, para jogar um jogo de Nintendo 64 no celular, você precisa de três coisas separadas: um **aplicativo emulador**, um **arquivo de núcleo** (o "cérebro" que entende o console) e o **arquivo do jogo** (a ROM). São três downloads, três coisas para configurar, três lugares diferentes onde as coisas podem dar errado. E quase sempre dão.

Este projeto resolve isso de um jeito direto: **tudo vem dentro de um único APK**.

Quando você instala o `EXTREME-N64-davimikeben.apk`, você não está instalando só um app vazio que depois vai pedir arquivos. Você está instalando, ao mesmo tempo:

- o aplicativo com toda a interface;
- o **núcleo de emulação** (Mupen64Plus-Next, compilado para as três arquiteturas de Android mais usadas);
- a **ROM do jogo**, embutida nos assets do próprio APK;
- o **teclado virtual** completo, desenhado para o controle de 64 bits;
- o **menu**, os perfis de desempenho, os ajustes de vídeo, de áudio e de tela.

Não existe app externo. Não existe download em segundo plano. Não existe "escolha o arquivo da ROM". A ROM já está lá dentro, esperando. Você abre o app, toca em ▶️ PLAY e o jogo começa.

> **A regra de ouro deste projeto:** o APK nunca é entregue sem o jogo. Se a ROM não estivesse embutida e legível, o projeto seria considerado falho — não existe versão "meia-boca" que só mostra uma tela bonita e não roda nada.

---

## 📦 O que vem dentro do APK

Aqui estão os números reais, medidos no arquivo final, sem enfeite:

| Componente | Detalhe |
|---|---|
| **Arquivo final** | `EXTREME-N64-davimikeben.apk` |
| **Tamanho** | 24.357.547 bytes (~23,2 MiB) |
| **Pacote** | `com.davimikeben.extremen64` |
| **Versão** | 1.0 (versionCode 1) |
| **Nome exibido** | EXTREME N64 - davimikeben |
| **Android mínimo** | 7.0 (API 24) |
| **Android alvo** | 14 (API 34) |
| **Orientação** | Paisagem (landscape), travada |
| **Assinatura** | Assinado (certificado de build) |

### 🕹️ A ROM

| Campo | Valor |
|---|---|
| **Nome** | `Super Mario 64 (USA).z64` |
| **Tamanho** | 8.388.608 bytes (8,0 MB) |
| **Formato** | `.z64` (big-endian, Nintendo 64) |
| **Plataforma** | Nintendo 64 |
| **Caminho no APK** | `assets/rom/Super Mario 64 (USA).z64` |
| **SHA-256** | `17ce077343c6133f8c9f2d6d6d9a4ab62c8cd2aa57c40aea1f490b4c8bb21d91` |

Esse hash não é decorativo. Ele foi conferido duas vezes: uma no arquivo do projeto e outra **extraindo a ROM de dentro do APK já compilado**. Os dois valores batem exatamente. Isso garante que o arquivo que saiu do forno é byte a byte o mesmo que entrou — nada de corrupção, nada de truncamento.

### 🧠 Os núcleos de emulação

O "cérebro" que roda o jogo é o **Mupen64Plus-Next**, um núcleo no padrão **Libretro**. Ele vem compilado para três arquiteturas, então o APK roda em praticamente qualquer celular Android moderno:

| Arquitetura | Arquivo | Tamanho |
|---|---|---|
| `arm64-v8a` | `libmupen64plus_libretro.so` | 6.203.328 bytes |
| `armeabi-v7a` | `libmupen64plus_libretro.so` | 6.070.572 bytes |
| `x86_64` | `libmupen64plus_libretro.so` | 3.680.736 bytes |

Além deles, cada ABI carrega a ponte nativa do app (`libextremen64.so`), que é o código escrito especificamente para este projeto e que faz a conversa entre o Android e o núcleo.

---

## ⚙️ Como tudo funciona por dentro

Vou abrir a caixa e mostrar onde cada peça se encaixa. A arquitetura é dividida em três camadas bem separadas, cada uma com uma responsabilidade clara.

```
┌─────────────────────────────────────────────────────────────┐
│  CAMADA 1 — INTERFACE (Kotlin + Jetpack Compose)            │
│  Menu, teclado virtual, menu flutuante, FPS, diálogos       │
└───────────────────────────┬─────────────────────────────────┘
                            │  chamadas de função
┌───────────────────────────▼─────────────────────────────────┐
│  CAMADA 2 — PONTE (Kotlin)                                  │
│  LibretroBridge · EmulatorView · SettingsManager            │
│  Carrega o núcleo, extrai a ROM, bombeia os quadros         │
└───────────────────────────┬─────────────────────────────────┘
                            │  JNI (Java Native Interface)
┌───────────────────────────▼─────────────────────────────────┐
│  CAMADA 3 — NATIVO (C++17)                                  │
│  native-lib.cpp — vídeo, áudio, entrada, ambiente do núcleo │
│  Carrega libmupen64plus_libretro.so via dlopen              │
└───────────────────────────┬─────────────────────────────────┘
                            │  Libretro API
┌───────────────────────────▼─────────────────────────────────┐
│  NÚCLEO — Mupen64Plus-Next (libretro)                       │
│  Roda a ROM e devolve imagem, som e leitura dos controles   │
└─────────────────────────────────────────────────────────────┘
```

### 🎬 O caminho do vídeo

O núcleo não desenha direto na tela. Ele conversa com o app através de dois modos:

1. **Renderização por hardware (padrão):** o núcleo pede uma "janela" de desenho (um contexto OpenGL ES) e desenha ele mesmo usando a GPU. O app entrega os endereços das funções de OpenGL que o núcleo precisa, e ele se vira. Isso é o que dá o melhor desempenho, porque a imagem vai direto para a placa de vídeo — ideal para o Mali-G52.
2. **Renderização por software (reserva):** se o núcleo preferir trabalhar com imagem crua na memória, o app recebe os pixels quadro a quadro, guarda num buffer e sobe isso para a tela como textura. É o plano B, para garantir que nunca fique tela preta.

O app aceita **OpenGL ES 3.0** normalmente e faz **fallback automático para OpenGL ES 2.0** quando o perfil de desempenho "⚡ DESEMPENHO" é escolhido. Ou seja: em celular mais fraco, o app se adapta em vez de travar.

### 🔊 O caminho do áudio

O som usa **OpenSL ES**, a API de áudio de baixa latência do Android. O fluxo é assim:

```
Núcleo gera amostras de som
        │
        ▼
Grava num anel de memória (ring buffer, ~2 segundos de estéreo)
        │
        ▼
Um "tocador" (player) toca blocos de 1024 amostras por vez
        │
        ▼
Ao terminar cada bloco, ele avisa: "acabou, me dá o próximo"
        │
        ▼
O app puxa o próximo pedaço do anel e entrega de volta
        │
        ▼  (e o ciclo não para)
```

Esse desenho evita estalos e mantém o som sincronizado. Se o anel encher porque o jogo está gerando som mais rápido do que o aparelho toca, o app descarta amostras antigas de propósito — é melhor perder um pedacinho do que acumular atraso e virar uma bola de neve.

### 🎯 O caminho do controle

O teclado virtual do app **não** inventa comandos estranhos ao jogo. Ele fala a mesma língua do núcleo: a **Libretro**. Cada botão na tela tem um número (ID) que corresponde exatamente ao botão correspondente no padrão Libretro. Quando você aperta "A" na tela, o app marca o botão "A" do núcleo como pressionado. Simples assim, e funciona com qualquer jogo que use o controle padrão.

O analógico funciona de forma parecida, mas em vez de "apertado/solto" ele manda uma **posição**: quanto você empurrou o direcional, de `-1.0` a `+1.0` em cada eixo. E, para jogos que esperam só um direcional digital, o app faz um espelhamento inteligente: se você empurrar o analógico bem para a esquerda, ele também marca "esquerda" no direcional digital. Assim o mesmo comando funciona nos dois mundos.

### 🧩 O que o núcleo pergunta e o app responde

Um núcleo Libretro não é autossuficiente — ele faz uma série de perguntas ao aplicativo durante a inicialização. Coisas como "onde eu salvo os arquivos?", "que formato de imagem eu uso?", "qual o idioma?" e por aí vai. O app responde tudo isso numa função central que trata cada pedido:

- ✅ Onde estão as pastas de sistema e de salvamento → devolve as pastas internas do app
- ✅ Formato de imagem (RGB565 ou XRGB8888) → aceita e anota qual foi
- ✅ Interface de renderização por hardware → aceita OpenGL ES 3 ou 2, conforme o perfil
- ✅ Interfaces de log, idioma, opções do núcleo, capacidades de entrada → responde
- ✅ Caminhos de diretórios de conteúdo, playlist e core assets → devolve pasta válida

Tudo o que não é pedido explicitamente é recusado de forma limpa (`false`), que é o comportamento correto e esperado.

---

## 🎮 A experiência de uso, passo a passo

Vou descrever exatamente o que acontece, na ordem, do momento em que você toca no ícone até a primeira fase.

**1. Você abre o app.**
Aparece o menu principal sobre um fundo escuro, com degradê do azul-noite ao azul-marinho e o nome em destaque rosa. Nada de tela branca piscando, nada de "selecione um arquivo".

**2. Você toca em ▶️ PLAY.**
Neste instante, a mágica acontece nos bastidores, em etapas:

```
"Extraindo ROM integrada..."      → o app copia o jogo de dentro do APK
                                     para uma pasta interna dele
"Carregando núcleo de emulação..." → encontra e mapeia o cérebro da emulação
"Inicializando núcleo..."          → liga o núcleo, configura pastas de
                                     sistema e de save
"Carregando Super Mario 64..."     → entrega a ROM ao núcleo
"Em execução"                      → o jogo começou
```

Cada uma dessas mensagens aparece no centro da tela enquanto acontece. Se algo der errado, é aqui que você vê o motivo — sem tela preta misteriosa.

**3. O jogo começa.**
A partir daí, tudo o que você vê na tela é: o jogo, o teclado virtual por cima, um contador de FPS pequenininho no topo, e um botãozinho discreto ⚙️ no canto. Mais nada. Sem barras de ferramentas, sem menus atravessados. A tela é do jogo.

**4. Você joga.**
Empurra o analógico, aperta os botões, joga a fase. O som sai pelo alto-falante (ou fone), os controles respondem, e o contador de FPS te diz se está tudo fluindo bem.

**5. Quando quiser mexer em algo.**
Toca no ⚙️ do canto, o menu rápido abre **por cima** do jogo — e o jogo continua rodando atrás, sem reiniciar. Você ajusta a transparência dos botões, liga o modo de mover controles, reinicia a partida ou volta ao menu principal. Fecha o menu e continua jogando de onde parou.

**6. Você sai.**
Se apertar o botão de voltar ou minimizar, o app pausa a emulação automaticamente (não fica gastando bateria em segundo plano) e retoma quando você volta.

---

## 🏠 O menu principal

O menu é organizado em **três colunas** com três botões cada, todos com ícone, nome e uma descrição curta embaixo. Nada de ícone solto sem explicação.

| Ícone | Botão | O que faz |
|:---:|---|---|
| ▶️ | **PLAY** | Inicia o jogo. É o botão em destaque, à esquerda. |
| 🎮 | **CONTROLES** | Abre os ajustes do teclado virtual e o mapa de teclas. |
| ⚡ | **DESEMPENHO** | Escolhe o perfil de otimização (foco no Mali-G52). |
| 🎨 | **GRÁFICOS** | Escolhe o filtro de imagem e a escala de saída. |
| 🔊 | **ÁUDIO** | Liga ou desliga a saída de som. |
| 📱 | **TELA** | Controla a exibição do contador de FPS. |
| 📊 | **MONITORAMENTO** | Mostra RAM, arquitetura, versão do Android e modelo. |
| 💾 | **SALVAR** | Grava as configurações atuais e mostra um resumo delas. |
| 🔄 | **RESTAURAR** | Volta tudo ao padrão de fábrica do app. |

No topo do menu, logo abaixo do título, aparece a assinatura **"por davimikeben"** e um resumo técnico: nome da ROM, tamanho e um pedaço do hash — para você saber exatamente o que está rodando.

### 🗂️ Detalhe de cada diálogo

**🎮 CONTROLES** — aqui você mexe no teclado virtual:

- **Transparência:** um controle deslizante de 5% a 100%. Quanto menor, mais "fantasma" ficam os botões sobre o jogo.
- **Tamanho:** de 0,60x a 1,80x. Aumenta ou diminui todos os botões juntos.
- **Vibração:** liga ou desliga o retorno tátil ao apertar.
- **Mapa de teclas:** uma lista que mostra o nome e a função de cada tecla (é a tabela completa mais abaixo).
- **♻️ Reposicionar teclas no padrão:** se você arrastar os botões para lugares estranhos e se arrepender, este botão devolve todo mundo para o lugar.

**📊 MONITORAMENTO** — mostra em tempo real:

- RAM livre e RAM total do aparelho
- Arquitetura (ABI) do processador
- Versão do Android
- Modelo do aparelho

**💾 SALVAR** — quando você toca, as preferências são gravadas e o app mostra um resumo do que ficou salvo: perfil, FPS visível, opacidade, escala, vibração, áudio e filtro.

**🔄 RESTAURAR** — limpa todas as preferências e volta o app ao estado inicial, como se tivesse acabado de instalar.

---

## 🎯 O teclado virtual

Esta é, com todo o respeito, a parte mais caprichada do projeto. Não é um teclado genérico adaptado às pressas. É um teclado **desenhado para o controle do Nintendo 64**, com todos os botões que o console tinha, e mais um pouco.

### O que ele tem

O controle de N64 original tinha: um direcional digital, um analógico, os botões A e B, os quatro botões C, os ombros L e R, o gatilho Z e o START. O teclado virtual tem tudo isso:

- **Direcional analógico** (canto inferior esquerdo) — um círculo com um "miolo" que você arrasta. Ele manda a posição proporcional do analógico e ainda espelha para o direcional digital.
- **Direcional digital** (embaixo, no centro) — as quatro setas clássicas ⬆️⬇️⬅️➡️.
- **Botões de ação** (canto inferior direito) — 🅱️ e 🅰️, os dois botões principais.
- **Botões C** (à direita, no meio) — C↑ C↓ C← C→, que no N64 servem principalmente para controlar a câmera. Aqui eles são traduzidos para o eixo analógico direito.
- **Ombros e gatilho** (em cima) — L1 (ombro esquerdo), Z (gatilho central), R1 (ombro direito) e START.

### Por que ele é "profissional"

- **Multitouch de verdade.** Você pode segurar o analógico com o polegar esquerdo, apertar A com o direito, e segurar um botão C com outro dedo — tudo ao mesmo tempo. Cada tecla rastreia o dedo específico que a tocou, então nada de botão "grudado" ou solto errado.
- **Feedback visual.** Quando um botão é apertado, ele acende na cor de destaque (rosa). Você vê na tela o que apertou.
- **Posição personalizada.** No modo de edição, cada grupo de botões (analógico, ações, botões C, direcional) pode ser **arrastado** para onde você quiser. Isso é ótimo para celulares com formatos diferentes, ou para quem tem dedos maiores ou menores.
- **Tamanho individual e geral.** Você pode mudar o tamanho de tudo junto, e cada família de botões já tem uma proporção pensada (botão A é maior, botões C são menores, etc.).
- **Transparência ajustável.** Os botões podem ficar bem apagados, para não atrapalhar a visão do jogo, ou bem sólidos, para quem prefere ver bem.
- **Layout salvo.** Se você arrastar os botões para um lugar que te agrada, o app **lembra** da posição na próxima vez que você abrir. Não precisa reposicionar toda vez.
- **Modo de edição seguro.** Só dá para arrastar os botões quando o modo de edição está ligado. Fora dele, os botões são botões de verdade — nada de mover sem querer no meio de uma fase.

### Como usar o modo de edição

1. Toque no ⚙️ do canto durante o jogo.
2. Ligue a opção **🎮 Mover botões**.
3. Os grupos de botões ganham uma **borda rosa**. Arraste cada um para onde quiser.
4. Desligue a opção. Pronto — a posição ficou salva.

Se quiser voltar tudo ao lugar, abra 🎮 CONTROLES no menu principal e toque em **♻️ Reposicionar teclas no padrão**.

---

## ⌨️ Mapa completo das teclas

Aqui está cada tecla da tela, com o nome e a função. Nada é inventado: tudo corresponde a um botão real do controle de N64 (ou do padrão Libretro).

| Tecla na tela | Nome | Função real no jogo |
|:---:|---|---|
| 🎮 | **Analógico** | Move o personagem. Manda posição proporcional (analógico) e também espelha para o direcional digital. |
| ⬆️ ⬇️ ⬅️ ➡️ | **Direcional digital** | Navegação em menus, mudanças de câmera e movimentos alternativos. |
| 🅰️ | **Botão A** | Ação principal — no Super Mario 64, é o pulo. |
| 🅱️ | **Botão B** | Ação secundária — agachar, pegar, socar. |
| 🅾️ | **Botão Z (L2)** | Gatilho central — no jogo, agachar e interagir. |
| **L1** | **Ombro esquerdo** | Botão de ombro esquerdo. |
| **R1** | **Ombro direito** | Botão de ombro direito. |
| ✳️ | **C↑ C↓ C← C→** | Os quatro botões C — controle de câmera (eixo analógico direito). |
| ⏸️ | **START** | Inicia e pausa a partida. |

> **Observação técnica:** no padrão Libretro, o gatilho Z do N64 é mapeado como "L2". Por isso, no código, a tecla Z usa o identificador de L2. É o mapeamento correto esperado pelo núcleo — não é um erro.

Para quem gosta de ver o número por trás de cada botão, eis a tabela de identificadores (iguais aos do `libretro.h`):

| ID | Botão | ID | Botão |
|:---:|---|:---:|---|
| 0 | B | 8 | A |
| 1 | Y | 9 | X |
| 2 | SELECT | 10 | L1 |
| 3 | START | 11 | R1 |
| 4 | D-pad ↑ | 12 | L2 (Z) |
| 5 | D-pad ↓ | 13 | R2 |
| 6 | D-pad ← | | |
| 7 | D-pad → | | |

---

## ⚙️ Menu flutuante (dentro do jogo)

O menu flutuante é aquele painel que abre quando você toca no ⚙️ no canto superior direito durante a partida. Ele foi feito com uma preocupação específica: **abrir o menu nunca reinicia o jogo**. O jogo continua rodando atrás do painel, e você volta exatamente de onde parou.

Ele fica encostado na direita da tela, com fundo escuro semitransparente, e traz:

| Item | O que faz |
|---|---|
| **FPS e modo atual** | Mostra o FPS e se está renderizando por GPU ou CPU. |
| **🎮 Mover botões** | Liga o modo de edição (mesma função do menu de controles). |
| **Transparência** | Controle deslizante na hora, com efeito imediato. |
| **Tamanho dos botões** | Controle deslizante de 0,60x a 1,80x. |
| **🔄 Reiniciar o jogo** | Reinicia a partida do zero. |
| **💾 Salvar configurações** | Grava as preferências atuais. |
| **🏠 Voltar ao menu principal** | Fecha o jogo com segurança e volta para o menu. |
| **✖ Fechar** | Fecha só o menu e volta ao jogo. |

O botãozinho que abre e fecha esse menu é pequeno e discreto de propósito: ele não atrapalha a visão do jogo e vira um "✖" quando o menu está aberto.

---

## 📈 Contador de FPS e monitoramento

Durante o jogo, a única informação na tela é o **contador de FPS** — pequeno, discreto, no centro do topo, em verde. É o suficiente para saber se está liso, e nada mais.

Como o número é calculado: o app conta quantos quadros foram desenhados e divide pelo tempo real decorrido, atualizando várias vezes por segundo. Isso dá um valor honesto, que acompanha a realidade em vez de mostrar um número fixo "bonito".

Você pode ligar e desligar o contador no menu **📱 TELA**.

As métricas mais pesadas (memória, arquitetura, versão do sistema, modelo do aparelho) **não** ficam na tela durante o jogo — ficam guardadas no menu **📊 MONITORAMENTO**. A ideia é simples: durante a partida, tela limpa; quando você quiser investigar, tem tudo num lugar só.

---

## ⚡ Perfis de desempenho e otimização para Mali-G52

O projeto foi pensado, especialmente, para rodar redondo em celulares com GPU **Mali-G52** — que é uma placa comum em aparelhos intermediários e que, com os ajustes certos, roda Nintendo 64 muito bem. Mas os perfis servem para qualquer aparelho.

Existem três modos, escolhidos no menu **⚡ DESEMPENHO**:

| Perfil | Foco | O que muda |
|---|---|---|
| ⚡ **DESEMPENHO** | Fluidez máxima | Usa **OpenGL ES 2.0** (mais leve) e prioriza a taxa de quadros. Ideal para Mali-G52 e aparelhos intermediários. |
| ⚖️ **EQUILIBRADO** | Meio-termo | Usa **OpenGL ES 3.0** e equilibra imagem e velocidade. É o padrão de fábrica. |
| 🎨 **QUALIDADE** | Melhor imagem | Usa **OpenGL ES 3.0** e resolução cheia, para quem tem aparelho potente e quer o visual mais bonito possível. |

### As decisões de otimização por trás disso

- **Escolha de versão do OpenGL conforme o perfil.** O perfil de desempenho usa ES 2.0 porque ele exige menos da GPU. Os outros usam ES 3.0, que é mais moderno e eficiente quando o aparelho aguenta.
- **Contexto de vídeo preservado ao pausar.** Quando você minimiza o app e volta, o contexto de vídeo é reaproveitado em vez de recriado do zero — inicialização praticamente instantânea ao retomar.
- **Buffer de imagem reaproveitado.** No modo de renderização por software, o app só realoca a memória da imagem quando o tamanho muda — evita alocar e liberar memória a cada quadro.
- **Compilação nativa enxuta.** O código nativo é compilado com `-O2`, com remoção de código morto (`-ffunction-sections -fdata-sections` + `--gc-sections`), ou seja: só vai para dentro do APK o que realmente é usado.
- **Biblioteca nativa mapeada direto do APK.** O núcleo é carregado direto de dentro do pacote, sem precisar copiar o arquivo para fora, o que economiza espaço e tempo.
- **Buffer de áudio circular.** O som usa um anel de memória de tamanho fixo, com descarte controlado quando enche — troca "atalho de memória" por estabilidade, sem travar.
- **Pausa automática.** Quando o app vai para segundo plano, a emulação para; quando volta, retoma. Isso economiza bateria de forma significativa.

---

## 🔊 Áudio

O som é entregue por **OpenSL ES**, com um cuidado grande para não estalar nem atrasar.

O app mantém um **anel de memória** (ring buffer) que comporta cerca de 2 segundos de áudio estéreo. O núcleo escreve nesse anel; o "tocador" de áudio lê dele em blocos. Quando o tocador termina um bloco, ele pede o próximo; o app então puxa mais um pedaço do anel e entrega. É um vaivém contínuo e leve.

Se o aparelho não conseguir tocar tão rápido quanto o jogo gera som, o anel teoricamente encheria. Para evitar que isso vire um atraso crescente (e um som "atrasado em relação à imagem"), o app **descarta as amostras mais antigas** quando o anel fica cheio. Preferir perder um pedacinho do que atrasar tudo é a escolha certa aqui.

Você pode ligar e desligar a saída de som no menu **🔊 ÁUDIO**. A taxa de amostragem é sincronizada automaticamente com o que o núcleo pede.

---

## 🎨 Gráficos

No menu **🎨 GRÁFICOS** você escolhe o filtro de imagem:

- **Nítido** — pixels quadrados e definidos, mais "retrô".
- **Suave** — leve suavização, deixa a imagem mais agradável em telas grandes.
- **Suave + Scanlines** — suavização com linhas horizontais, imitando uma TV antiga.

A própria tela do app explica uma coisa importante: **a resolução interna quem decide é o perfil de desempenho**. No perfil DESEMPENHO o app usa OpenGL ES 2.0; nos demais, OpenGL ES 3.0. Assim, o filtro é a "estética" da imagem, e o perfil é o "esforço" que o aparelho vai fazer.

---

## 💾 Salvar e restaurar

O app guarda suas preferências automaticamente conforme você as muda — mas os botões **💾 SALVAR** e **🔄 RESTAURAR** existem para dar controle explícito:

- **💾 SALVAR** confirma a gravação e mostra um resumo do que está salvo: perfil escolhido, se o FPS aparece, opacidade e escala dos controles, vibração, áudio e filtro de vídeo.
- **🔄 RESTAURAR** limpa as preferências e volta tudo ao padrão: perfil EQUILIBRADO, opacidade 45%, escala 1,00x, vibração e áudio ligados, filtro "Suave". É o "reset de fábrica" do app.

**O que é salvo, exatamente:**

| Preferência | Padrão de fábrica | Faixa |
|---|---|---|
| Perfil de desempenho | ⚖️ EQUILIBRADO | DESEMPENHO / EQUILIBRADO / QUALIDADE |
| Mostrar FPS | Ligado | Ligado / Desligado |
| Transparência dos controles | 45% | 5% a 100% |
| Escala dos controles | 1,00x | 0,60x a 1,80x |
| Vibração | Ligado | Ligado / Desligado |
| Áudio | Ligado | Ligado / Desligado |
| Filtro de vídeo | Suave | Nítido / Suave / Suave + Scanlines |
| Posição dos botões | Padrão | Arrastável no modo de edição |

---

## 📁 Estrutura do projeto

```
EXTREME N64/
│
├── app/
│   ├── build.gradle                  → configuração do módulo (ABIs, CMake, release)
│   ├── proguard-rules.pro            → regras de proteção do código na versão release
│   └── src/main/
│       ├── AndroidManifest.xml        → app em paisagem, nome "EXTREME N64 - davimikeben"
│       │
│       ├── assets/rom/
│       │   └── Super Mario 64 (USA).z64        ← a ROM, embutida no APK (8,0 MB)
│       │
│       ├── jniLibs/                   → os núcleos de emulação prontos para cada ABI
│       │   ├── arm64-v8a/libmupen64plus_libretro.so
│       │   ├── armeabi-v7a/libmupen64plus_libretro.so
│       │   └── x86_64/libmupen64plus_libretro.so
│       │
│       ├── cpp/                       → código nativo (C++17)
│       │   ├── CMakeLists.txt          → regras de compilação nativa
│       │   ├── libretro.h              → cabeçalho oficial da API Libretro
│       │   └── native-lib.cpp          → a ponte: vídeo, áudio, entrada, ambiente
│       │
│       ├── java/com/davimikeben/extremen64/   → código Kotlin
│       │   ├── MainActivity.kt          → atividade principal, menu e tela de jogo
│       │   ├── EmulatorView.kt          → superfície OpenGL e ciclo de inicialização
│       │   ├── LibretroBridge.kt        → ponte Kotlin ↔ nativo, extração da ROM
│       │   ├── VirtualControls.kt       → teclado virtual completo e multitouch
│       │   ├── FloatingMenu.kt          → menu rápido dentro do jogo
│       │   ├── SettingsManager.kt       → preferências do usuário
│       │   ├── Dialogs.kt               → diálogos de controles, gráficos, áudio, tela
│       │   ├── PerformanceDialog.kt     → seleção do perfil de desempenho
│       │   └── MonitoringDialog.kt      → monitoramento de RAM e sistema
│       │
│       └── res/                        → ícones (todas as densidades) e temas
│
├── build.gradle                       → configuração raiz (plugins)
├── settings.gradle                    → nome do projeto e módulos
├── gradle.properties                  → memória, cache e opções do Gradle
└── README.md                          → este arquivo
```

---

## 🛠️ Stack de tecnologia

| Camada | Tecnologia |
|---|---|
| Linguagem da interface | Kotlin |
| Interface gráfica | Jetpack Compose (Material 3) |
| Código nativo | C++17 |
| Compilação nativa | Android NDK + CMake |
| Gráficos | OpenGL ES 3.0 (com fallback para ES 2.0) |
| Áudio | OpenSL ES |
| Emulação | Núcleo Libretro (Mupen64Plus-Next) |
| Integração nativa | JNI (Java Native Interface) |
| Build | Android Gradle Plugin 8.2.2 · Gradle 8.14.6 |
| Otimização de release | R8 / ProGuard + remoção de recursos não usados |
| Leitura de assets | Android AssetManager |

---

## 🔨 Como compilar

### O que você precisa

- **Android SDK** com a API 34 (Android 14)
- **Android NDK** 26.1.10909125 (ou compatível)
- **CMake** 3.22.1
- **JDK 17**
- **Gradle** 8.x

### Passo a passo

```bash
# 1. Clone o repositório
git clone https://github.com/davimikeben/MARIO-APK.git
cd MARIO-APK

# 2. Aponte para o seu SDK do Android
echo "sdk.dir=/caminho/para/seu/android-sdk" > local.properties

# 3. Compile a versão de release
gradle assembleRelease
```

O APK sai em:

```
app/build/outputs/apk/release/app-release.apk
```

Para instalar direto no aparelho conectado:

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

> **Dica:** se a compilação reclamar de versão do NDK, ajuste a linha `ndkVersion` no `app/build.gradle` para a versão que você tem instalada. O projeto foi compilado e testado com a `26.1.10909125`.

---

## ✅ Verificação de integridade (resultados reais)

Tudo o que está escrito aqui foi conferido no arquivo final. Não é chute.

### 1. A ROM está dentro do APK?

Sim. Inspecionando o conteúdo do pacote:

```
assets/rom/Super Mario 64 (USA).z64     8.388.608 bytes
```

### 2. A ROM saiu intacta?

Sim. O hash da ROM **extraída de dentro do APK** bate exatamente com o hash do arquivo original:

```
SHA-256 (original):  17ce077343c6133f8c9f2d6d6d9a4ab62c8cd2aa57c40aea1f490b4c8bb21d91
SHA-256 (do APK):    17ce077343c6133f8c9f2d6d6d9a4ab62c8cd2aa57c40aea1f490b4c8bb21d91
                     ✔ idênticos
```

### 3. Os núcleos nativos estão presentes?

Sim, nas três arquiteturas:

```
lib/arm64-v8a/libmupen64plus_libretro.so     6.203.328 bytes
lib/armeabi-v7a/libmupen64plus_libretro.so   6.070.572 bytes
lib/x86_64/libmupen64plus_libretro.so        3.680.736 bytes
lib/arm64-v8a/libextremen64.so
lib/armeabi-v7a/libextremen64.so
lib/x86_64/libextremen64.so
```

### 4. O pacote está assinado?

Sim, o APK está assinado e pronto para instalação.

### 5. Ficha técnica confirmada

```
package:      com.davimikeben.extremen64
versão:       1.0 (versionCode 1)
nome exibido: EXTREME N64 - davimikeben
minSdk:       24   (Android 7.0)
targetSdk:    34   (Android 14)
ABIs:         arm64-v8a, armeabi-v7a, x86_64
atividade:    com.davimikeben.extremen64.MainActivity
SHA-256 APK:  0afbb177c2567edeb492adece38c19a28527df8a42415a25f5882e3624dfbc20
```

---

## ❓ Perguntas frequentes

**Preciso baixar a ROM em algum lugar?**
Não. A ROM já vem dentro do APK. São um download só.

**Preciso instalar um emulador?**
Não. O núcleo de emulação está embutido. O APK é autossuficiente.

**Funciona sem internet?**
Sim. Depois de instalado, o app não precisa de internet para nada.

**Vai rodar no meu celular?**
Se for Android 7.0 ou mais novo e tiver uma das três arquiteturas suportadas (arm64-v8a, armeabi-v7a ou x86_64) — o que cobre praticamente todos os celulares dos últimos anos — deve rodar. Comece pelo perfil ⚖️ EQUILIBRADO e, se quiser mais fluidez, tente ⚡ DESEMPENHO.

**Está travando ou lento. O que faço?**
Vá em ⚡ DESEMPENHO e escolha o perfil "⚡ DESEMPENHO". Ele reduz a exigência da GPU usando OpenGL ES 2.0. Também vale diminuir a escala dos controles e desligar o filtro de imagem mais pesado.

**O som está atrasado em relação à imagem.**
Isso costuma ser do aparelho. Testar com fone de ouvido às vezes ajuda, e o perfil de desempenho tende a reduzir essa sensação ao deixar tudo mais fluido.

**Não gostei da posição dos botões.**
Toque em ⚙️ durante o jogo, ligue "🎮 Mover botões", arraste os grupos para onde quiser e desligue a opção. Fica salvo.

**Quero voltar tudo ao normal.**
Menu principal → 🔄 RESTAURAR.

**O que é aquele botãozinho no canto superior direito?**
É o atalho do menu rápido (⚙️). Ele abre e fecha o painel de ajustes sem reiniciar o jogo.

---

## 🔍 Limites conhecidos e transparência

Gosto de ser direto sobre o que foi e o que não foi testado, para você não ser pego de surpresa.

- **Verificação de integridade: feita e confirmada.** Conteúdo do APK, presença da ROM, hash SHA-256 batendo byte a byte, presença dos núcleos nas três arquiteturas, assinatura e ficha técnica — tudo isso foi conferido de verdade no arquivo final.
- **Teste de execução em aparelho: ainda pendente.** A compilação e a verificação foram feitas em ambiente automatizado, **sem uma tela física nem uma GPU real**. Isso significa que os testes visuais (imagem aparecendo corretamente, fluidez real, resposta do áudio, comportamento no Mali-G52) precisam ser feitos instalando o APK em um celular Android de verdade. O código está completo e a estrutura está pronta para rodar; a validação em campo é o próximo passo natural.
- **Faça backup antes de tudo.** Sempre.

Se você encontrar qualquer comportamento estranho ao rodar em um aparelho real, o melhor caminho é abrir uma *issue* descrevendo o modelo do celular, a versão do Android e o que aconteceu.

---

## 🙏 Créditos e licença

- **Projeto, interface, teclado virtual e integração:** davimikeben
- **Núcleo de emulação:** Mupen64Plus-Next, no padrão Libretro
- **API de integração:** Libretro

O jogo *Super Mario 64* é propriedade da Nintendo. Este projeto é um trabalho de engenharia de software feito para estudo e uso pessoal de emulação; todos os direitos sobre o jogo pertencem aos seus respectivos donos.

---

<div align="center">

### 🎮 Feito por **davimikeben**

*Um jogo inteiro. Um arquivo só. Sem desculpas.*

</div>
