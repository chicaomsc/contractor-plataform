# Direção Visual — Contractor Platform / JR Pinturas

**Sprint:** 7A — Design Direction  
**Versão:** 1.0  
**Data:** 2026-07-12  
**Status:** Direção principal recomendada

---

## Conceito Visual

O produto não é uma startup. É uma empresa artesanal com história, orgulho no ofício e clientes reais em Portugal. A identidade visual deve comunicar isso antes mesmo que uma palavra seja lida.

A linguagem gráfica parte de dois princípios opostos que, em tensão, produzem algo genuíno: **a marca do trabalho manual** (imperfeição, textura, material) e **a disciplina do profissional organizado** (limpeza, precisão, resultado). É essa dualidade que distingue um bom artífice de um genérico "prestador de serviços".

Referências conceptuais (não visuais):
- Portefólio de arquitecto português — editorial, sem adorno
- Brochura de construtora familiar europeia — séria, com fotografias reais
- Catálogo de tinta profissional — sistema cromático preciso, comunicação técnica

---

## Personalidade da Marca

| Traço | Expressão visual |
|---|---|
| Confiável | Paleta restrita, hierarquia clara, sem excessos |
| Profissional | Tipografia firme, alinhamento recto, espaço respeitado |
| Artesanal | Textura subtil, assimetria deliberada, fotografia real |
| Local | Proximidade humana, linguagem directa, referência geográfica |
| Transformador | Comparação antes/depois como elemento visual central |

---

## Princípios de Composição

1. **Assimetria intencional** — blocos de conteúdo não se espelham. O layout não é simétrico nem dividido em grelhas de cartões iguais.
2. **Fotografia como estrutura** — as imagens não decoram; elas constroem a arquitectura da página.
3. **Espaço negativo activo** — não há medo de deixar zonas sem elementos. O respiro é parte da linguagem.
4. **Contraste como hierarquia** — nenhum elemento compete em tamanho ou cor com o que o antecede na hierarquia de leitura.
5. **Detalhes físicos discretos** — linhas, bordas, grão. Nunca sombras flutuantes ou efeitos digitais óbvios.
6. **Zero decoração sem função** — cada elemento responde a uma pergunta de conversão.

---

## Uso de Fotografia

**O que usar:**
- Fotografias reais das obras executadas pela JR Pinturas
- Perspectiva próxima que revele textura e acabamento
- Luz natural, diurna, quente — evitar flash
- Contexto habitacional ou comercial reconhecível (não estudio)
- Trabalhadores em acção (opcional, se autorizado)

**O que evitar:**
- Stock photography genérica (paredes brancas perfeitas sem história)
- Pessoas sorridentes de fato com tinta na mão (encenação óbvia)
- Imagens sobresaturadas ou com filtro excessivo
- Perspectiva "drone" sem contexto humano

**Tratamento de cor nas imagens:**
- Ligeira redução de saturação global para harmonizar com a paleta escura
- Conservar o calor natural da luz portuguesa
- Sem filtros Instagram ou efeitos de "câmara vintage"

---

## Tratamento das Imagens Antes/Depois

O comparador é o elemento de conversão mais potente da página. Deve ser tratado como peça editorial, não como funcionalidade técnica.

**Princípios:**
- As duas imagens devem ter a mesma perspectiva, distância e iluminação (brief para o cliente)
- A divisão pode ser vertical (sliding) ou estática lado a lado, dependendo do contexto
- Em mobile: empilhadas verticalmente com label clara ("Antes" / "Depois")
- No slider interactivo: a linha divisória é fina, sem controlos excessivamente grandes
- Não adicionar texto sobre a fotografia — a imagem fala por si

**Estados sem fotografia:**
- Placeholder com silhueta de divisória e texto "Fotografia em breve" — nunca caixa vazia ou ícone genérico
- A grelha mantém-se mesmo sem conteúdo (evitar layout partido)

---

## Elementos Gráficos Inspirados no Ofício

Detalhes visuais que referenciam o trabalho sem ser óbvios:

| Elemento | Referência física | Uso recomendado |
|---|---|---|
| Linha horizontal fina (~1px) | Fita de pintura | Separador de secções |
| Textura de granulado subtil | Parede texturada / betão à vista | Background de secções alternadas |
| Ângulo de 45° em detalhe decorativo | Pincel / espátula | Detalhe em citações ou badges |
| Rectângulo com borda esquerda (~3px) laranja | Marcador / régua | Highlights de conteúdo, breadcrumbs |
| Mancha de tinta estilizada (SVG minimalista) | Respingo ou rolo | Ícone de marca, não decoração de página |

**O que não fazer:** fita adesiva decorativa em excesso, ícones de balde de tinta em toda a secção, clip-art de pincel.

---

## Referências de Materiais e Texturas

- **Betão à vista leve:** cinza frio, grão médio — background de alternância em desktop
- **Gesso rugoso:** off-white quente, ligeiramente irregular — hero background sem fotografia
- **Tinta fosca:** acabamento mate, sem brilho — influência na ausência de sombras e gradientes
- **Madeira clara em detalhe:** eventual uso em bordas de fotografia ou quotes

Implementação: CSS `noise` SVG inline ou imagem PNG de ruído em `opacity: 0.03–0.06`. Nunca pesado ou visível como efeito.

---

## Direcção de Ícones

- Sistema: **Phosphor Icons** (peso "Regular" e "Bold" — não "Fill" como padrão)
- Tamanho: 20px em contexto de texto, 24px em contexto de lista, 32px isolado
- Nunca usar ícone como elemento principal de uma secção — complementa texto
- Serviços identificados por nome e descrição curta, não por ícone decorativo
- Ícones de acção (WhatsApp, telefone, email) com cor de acento ou laranja

---

## Direcção de Animações

**Filosofia:** movimento só existe quando guia a atenção ou confirma uma acção.

| Elemento | Comportamento | Duração | Easing |
|---|---|---|---|
| Entrada de secções | Fade-in + translate Y 12px | 320ms | ease-out |
| Slider antes/depois | Drag contínuo sem transição de CSS | — | — |
| Hover em botão primário | Background escurece 8% | 180ms | ease |
| Hover em link | Underline slide from left | 200ms | ease-in-out |
| CTA flutuante WhatsApp | Pulse subtil (scale 1.05) | 2s loop | ease-in-out |
| Contador de confiança | Count-up quando entra no viewport | 800ms | ease-out |

**Reduced motion:** todas as animações são desligadas quando `prefers-reduced-motion: reduce`. O conteúdo não desaparece — simplesmente aparece sem transição.

---

## Exemplos do Que Fazer

- Hero com fotografia real a ocupar 55–65% da largura em desktop, texto editorial à esquerda
- Secção de galeria com grelha irregular (não 3×2 idênticos) — uma imagem maior em destaque
- Tipografia em caixa alta para labels de secção ("O QUE FAZEMOS", "ANTES E DEPOIS")
- Linha horizontal laranja de 3px antes de título de hero
- Barra de estatísticas em fundo carvão com números grandes em branco
- CTA "Pedir Orçamento" em laranja sólido, sem sombra, sem gradiente, texto branco

## Exemplos do Que Evitar

- Botões com `border-radius: 9999px` (pill shape)
- Gradientes de fundo em secções de marketing
- Animações em parallax agressivo em cada scroll
- Seis cartões iguais com ícone + título + descrição na secção de serviços
- Background com círculos desfocados de cor
- Tipografia fina demais (weight abaixo de 400 em tamanho abaixo de 16px)

---

## Por que Esta Linguagem Evita Aparência Gerada por IA

O problema de sites gerados ou inspirados em IA é a média estatística: hero centralizado, gradiente roxo/azul, grid de 6 cards, CTA em pill button. São escolhas que minimizam risco criativo ao máximo da probabilidade, produzindo sites que se parecem com todos os outros.

A linguagem proposta aqui faz escolhas de custo criativo alto:
1. **Assimetria editorial** — exige decisão de composição caso a caso
2. **Fotografia como estrutura** — sem foto real, o layout não existe (não há fallback decorativo)
3. **Paleta restrita e anti-tendência** — carvão + laranja + branco quente não é a paleta padrão de 2025
4. **Ausência de sombras e rounded corners** — vai contra o padrão Tailwind/shadcn por defeito
5. **Texto directo e localizado** — impossível de gerar genericamente sem conhecer o cliente

---

## Três Direcções Visuais

---

### Direcção A — "Matéria Bruta"

**Conceito:** O peso e a textura dos materiais de construção como linguagem visual. Fundo escuro dominante. Fotografias em contraste alto. Tipografia de impacto.

**Paleta base:** `#111110` (betão escuro), `#E85C0A` (laranja intenso), `#F5F2EC` (reboco claro)

**Tipografia:** Display em condensed ultra-bold. Body em sans-serif regular.

**Vantagens:**
- Diferenciação máxima do mercado (nenhum concorrente faz isto)
- Impacto imediato e memorável
- Funciona muito bem com fotografia de qualidade

**Riscos:**
- Pode intimidar públicos mais conservadores (clientes mais velhos)
- Exige fotografias de alta qualidade — sem elas o layout colapsa
- Legibilidade a verificar cuidadosamente em texto longo

**Adequação à JR Pinturas:** Adequada se a empresa quiser posicionar-se como premium e diferenciar-se claramente dos concorrentes locais. Requer buy-in do cliente sobre o "peso" visual.

**Aplicação no hero:** Fotografia a ocupar 100% da largura em desktop com overlay de carvão a 40%. Texto hero sobre a imagem em branco, título condensed muito grande.

**Aplicação na galeria:** Grid masonry com imagens em preto e branco (antes) e a cores (depois) — o contraste de cor substitui a divisória.

**Aplicação nos serviços:** Lista vertical com linha separadora em laranja. Sem cards. Sem ícones.

---

### Direcção B — "Obra em Ordem" ← RECOMENDADA

**Conceito:** A organização e precisão do profissional qualificado como promessa visual. Fundo branco quente com secções alternadas em cinza-betão. Layout editorial com fotografia lateral.

**Paleta base:** `#F8F6F2` (branco quente), `#1C1C1A` (carvão), `#E8500A` (laranja JR), `#E8E5DF` (betão claro)

**Tipografia:** Headlines em sans-serif semi-condensed bold (firmeza sem agressividade). Body em sans-serif regular com bom espaçamento de linha.

**Vantagens:**
- Equilíbrio entre profissionalismo e acessibilidade
- Funciona com fotografias boas e mediocres
- Muito legível em mobile
- Conversão expectável mais alta (não afasta públicos conservadores)
- Facilidade de manutenção pelo cliente sem quebrar o design

**Riscos:**
- Pode parecer "limpo demais" sem fotografias reais
- Exige disciplina editorial para não escorregar para template genérico

**Adequação à JR Pinturas:** Alta. Posiciona a empresa como profissional e organizada — exactamente as qualidades que os clientes procuram num prestador de obras.

**Aplicação no hero:** Layout dividido: coluna de texto à esquerda (55%) com título grande, subtítulo, CTA; fotografia à direita (45%) com enquadramento assimétrico e detalhe em laranja.

**Aplicação na galeria:** Grid de 2 colunas em mobile / 3 colunas em desktop, com um item em destaque a ocupar 2 colunas. Slider antes/depois nos itens com imagens pares.

**Aplicação nos serviços:** Lista em 2 colunas com linha divisória sutil, nome do serviço em bold, descrição curta abaixo. Sem cards com sombra. Hover revela a cor de acento na linha esquerda.

---

### Direcção C — "Luz Natural"

**Conceito:** A luz natural portuguesa como atmosfera. Paleta quente, terracotta, creme. Visual mais próximo, humanizado, familiar.

**Paleta base:** `#FAF5EE` (creme), `#3D2B1F` (castanho escuro), `#E07840` (laranja terracotta), `#C4A882` (areia)

**Tipografia:** Serifada elegante para títulos (humanismo, artesanato). Sans-serif para corpo.

**Vantagens:**
- Sente-se imediatamente local e genuíno
- Evoca calor e confiança familiar
- Forte diferenciação visual em relação a concorrentes com identidade genérica

**Riscos:**
- Pode parecer demasiado "rústico" ou "regional" — limita percepção de sofisticação
- A tipografia serifada aumenta complexidade de implementação
- Risco de envelhecer mais rapidamente

**Adequação à JR Pinturas:** Adequada se o posicionamento for de proximidade e confiança familiar. Menos adequada se o objectivo for competir com empresas de maior dimensão.

**Aplicação no hero:** Fotografia a quase toda a largura com texto sobreposto em fundo creme translúcido. Título em serifada grande.

**Aplicação na galeria:** Grid de fotografias com moldura fina em castanho, label "Antes / Depois" em tipografia serifada.

**Aplicação nos serviços:** Lista com ícones simples à esquerda, texto à direita. Fundo em creme com separadores em areia.

---

## Direcção Recomendada: B — "Obra em Ordem"

**Justificativa:**

1. É a que melhor equilibra diferenciação e conversão para o público-alvo da JR Pinturas.
2. A paleta carvão + laranja + branco quente é coerente com a identidade de marca existente e não exige redesign da logo.
3. Funciona bem com fotografias de qualidade variável — o cliente pode começar com o que tem.
4. O layout editorial com fotografia lateral é muito mais distinto do que qualquer template SaaS.
5. É a mais escalável para outros clientes do Contractor Platform com identidades diferentes.
6. Tem o maior potencial de conversão: profissional sem ser frio, local sem ser amador.

**Condições para validação:**
- A logo da JR Pinturas deve ser testada sobre o fundo `#F8F6F2` e sobre `#1C1C1A` antes de aprovar a paleta final.
- Os valores hexadecimais propostos estão sujeitos a ajuste depois da validação visual com a marca real.
- Duas fotografias reais de obras devem estar disponíveis antes de iniciar a Sprint 7B (implementação do hero).

---

## A Landing Pertence ao Contractor Platform, Não à JR Pinturas

Esta secção documenta uma decisão arquitectural fundamental que deve guiar **toda** a implementação do frontend.

### JR Pinturas é o primeiro tenant. Não é o único.

A landing page que estamos a desenhar não é "o site da JR Pinturas". É a landing page do Contractor Platform, instanciada com os dados da JR Pinturas. Amanhã pode ser instanciada com os dados de outra empresa de pintura em Braga, ou de uma empresa de ladrilhos no Algarve.

### O que isso significa na prática

**1. Nenhum componente conhece a JR Pinturas**

Um componente não pode ter o nome de uma empresa, uma cor hardcoded, ou um texto específico de um cliente. Todos os dados que variam por tenant chegam da API pública:

| Dado | Fonte |
|---|---|
| Nome da empresa | `GET /public/company?slug={slug}` |
| Logo | `company.logoUrl` |
| Cor primária | `company.primaryColor` (sobrescreve `--primary`) |
| Serviços | `GET /public/services?slug={slug}` |
| Galeria | `GET /public/gallery?slug={slug}` |
| WhatsApp | `company.whatsapp` |
| Descrição | `company.description` |

**2. O branding é carregado em runtime**

O design system define os tokens com valores por defeito. Quando a página carrega, os tokens de marca (`--primary`, `--background`, `--foreground`) são sobrescritos com os valores do tenant:

```css
/* Tokens por defeito (design system) */
:root {
  --primary: #E8500A;
  --background: #F8F6F2;
}

/* Sobrescritos pelo SiteLayout com dados do tenant */
[data-tenant="jr-pinturas"] {
  --primary: [valor da API];
  --background: [valor da API se configurado];
}
```

**3. A direcção visual "Obra em Ordem" é o sistema por defeito**

A direcção B ("Obra em Ordem") define os valores de fallback quando o tenant não configurou branding personalizado. Um tenant que configure apenas a cor primária herda tudo o resto do sistema por defeito.

**4. O slug é o identificador do tenant**

A rota `/{slug}` é o ponto de entrada de qualquer landing. O slug resolve o tenant. Todos os fetches subsequentes usam esse slug. Não existe nenhuma lógica de routing baseada em nome de empresa.

### Implicações de design

- A direcção visual escolhida deve funcionar com **diferentes** paletas de cor primária — não apenas o laranja da JR Pinturas
- Testar o layout com uma cor primária azul, verde e vermelha antes de considerar o sistema robusto
- Fotografias de hero e galeria são do tenant — o design não pode depender de fotografias com características específicas (orientação, cor dominante)
- O logótipo é variável — o header deve funcionar com logos horizontais, verticais e quadradas
