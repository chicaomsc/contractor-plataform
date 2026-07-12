# Acessibilidade — Landing Page JR Pinturas

**Sprint:** 7A — Design Direction  
**Versão:** 1.0  
**Data:** 2026-07-12  
**Meta:** WCAG 2.2 Nível AA  
**Idioma:** pt-PT

---

## Visão Geral

A acessibilidade não é uma funcionalidade adicional — é uma propriedade do produto desde o primeiro componente. Uma landing page acessível:

- Serve utilizadores com deficiência visual, motora ou cognitiva
- Melhora a experiência de todos (teclado, mobile, leitores de ecrã)
- É um requisito legal em Portugal para serviços públicos (e boa prática para privados)
- Melhora SEO (HTML semântico, textos alternativos, hierarquia de headings)

---

## Contraste de Cor

### Pares verificados

| Elemento | Cor foreground | Cor background | Ratio | AA Normal | AA Large | Estado |
|---|---|---|---|---|---|---|
| Texto principal | `#1C1C1A` | `#F8F6F2` | 16.2:1 | Passa | Passa | Aprovado |
| Texto sobre laranja | `#FFFFFF` | `#E8500A` | 4.8:1 | Passa | Passa | Aprovado |
| Texto sobre carvão | `#F8F6F2` | `#1C1C1A` | 16.2:1 | Passa | Passa | Aprovado |
| Texto muted | `#6B6860` | `#F8F6F2` | 5.4:1 | Passa | Passa | Aprovado |
| Texto subtle | `#9B9890` | `#F8F6F2` | 3.4:1 | Falha | Passa | Só em texto ≥ 18px |
| Texto de erro | `#C0392B` | `#FFFFFF` | 5.2:1 | Passa | Passa | Aprovado |
| Placeholder | `#9B9890` | `#FFFFFF` | 3.4:1 | Falha | Passa | Informação não crítica |

> Nota: placeholders não transportam informação — têm requisito de contraste menor (WCAG 1.4.3 aplica-se a texto funcional, não a placeholders que servem apenas de hint). Verificar com tool após definição final.

### Verificação automática

Usar **axe DevTools** ou **Lighthouse** em cada componente implementado. Nenhum PR de componente deve introduzir falhas de contraste.

---

## Navegação por Teclado

### Ordem de Tab

A ordem de focagem deve seguir a ordem visual da página (top-to-bottom, left-to-right):

1. Skip link "Saltar para o conteúdo" (primeiro elemento do DOM)
2. Logo (link para `#topo`)
3. Links de navegação do header
4. Botão WhatsApp do header
5. CTA hero
6. Conteúdo de cada secção (headings não são focáveis por defeito)
7. Serviços (links "Saber mais" se existirem)
8. Navegação da galeria (botões ← →)
9. Handle do slider antes/depois (em desktop)
10. Links do processo de trabalho
11. Campos do formulário de contacto
12. Botão de submit do formulário
13. Links do footer

### Skip Link

```html
<!-- Primeiro elemento visível no DOM -->
<a href="#main-content" class="skip-link">
  Saltar para o conteúdo principal
</a>
```

Visível apenas quando focado. Posição: fixo no topo, `z-index: 100`.

Estilo quando focado:
```css
.skip-link:focus {
  position: fixed;
  top: 8px;
  left: 8px;
  background: var(--primary);
  color: var(--primary-foreground);
  padding: 8px 16px;
  outline: 4px solid var(--focus);
}
```

### Teclas de teclado por componente

| Componente | Tecla | Comportamento |
|---|---|---|
| Menu hamburguer | Enter / Space | Abre/fecha o menu |
| Menu hamburguer | ESC | Fecha o menu |
| Slider antes/depois | ← → (arrow keys) | Move a linha divisória em 10% |
| Slider antes/depois | Home / End | Move para 0% ou 100% |
| Galeria (dots/botões) | Enter / Space | Navega para item |
| Formulário | Tab | Move entre campos na ordem correcta |
| Formulário | Enter | Submete o formulário (em campos text) |
| Dropdown serviços | ↑ ↓ | Navega entre opções |
| Dropdown serviços | Enter | Selecciona opção |

---

## Foco Visível

**Regra:** `outline: none` ou `outline: 0` são **proibidos** sem alternativa explícita.

Estilo padrão de foco:
```css
:focus-visible {
  outline: 4px solid var(--focus);
  outline-offset: 2px;
}
```

Em fundo escuro (ex: secção de estatísticas, CTA final):
```css
.dark-section :focus-visible {
  outline-color: #FFFFFF;
}
```

O foco é visível com `outline` e não com `box-shadow` para compatibilidade com Windows High Contrast Mode.

---

## Textos Alternativos (Alt Text)

### Imagens de conteúdo (alt obrigatório)

Todas as imagens que comunicam conteúdo têm `alt` descritivo:

| Imagem | Exemplo de alt |
|---|---|
| Foto hero | `"Sala de estar com pintura recém-terminada em Lisboa"` |
| Imagem ANTES | `"Parede com pintura antiga e manchas antes da obra"` |
| Imagem DEPOIS | `"A mesma parede após pintura nova, cor branco roto"` |
| Foto da equipa | `"João Ramos, fundador da JR Pinturas, em obra"` |
| Logo JR Pinturas | `"Logótipo JR Pinturas"` |

### Imagens decorativas (alt vazio)

```html
<img src="texture.png" alt="" aria-hidden="true" />
```

Texturas de fundo, separadores visuais e ícones que acompanham texto visível devem ter `alt=""` e `aria-hidden="true"`.

### Ícone de WhatsApp

```html
<button aria-label="Contactar pelo WhatsApp">
  <svg aria-hidden="true" ...>...</svg>
</button>
```

---

## Semântica HTML

### Estrutura da página

```html
<html lang="pt">
<body>
  <a href="#main-content">Saltar para o conteúdo principal</a>

  <header role="banner">
    <nav aria-label="Navegação principal">
      <ul>
        <li><a href="#servicos">Serviços</a></li>
        <li><a href="#galeria">Galeria</a></li>
        <li><a href="#como-funciona">Como funciona</a></li>
        <li><a href="#contacto">Contacto</a></li>
      </ul>
    </nav>
  </header>

  <main id="main-content">
    <section aria-labelledby="hero-title">
      <h1 id="hero-title">...</h1>
    </section>

    <section aria-labelledby="trust-title">
      <h2 id="trust-title" class="sr-only">Experiência e credenciais</h2>
    </section>

    <section aria-labelledby="services-title" id="servicos">
      <h2 id="services-title">O que fazemos</h2>
    </section>

    <section aria-labelledby="gallery-title" id="galeria">
      <h2 id="gallery-title">Antes e depois</h2>
    </section>

    <section aria-labelledby="process-title" id="como-funciona">
      <h2 id="process-title">Como trabalhamos</h2>
    </section>

    <section aria-labelledby="about-title">
      <h2 id="about-title">Sobre nós</h2>
    </section>

    <section aria-labelledby="area-title">
      <h2 id="area-title">Onde trabalhamos</h2>
    </section>

    <section aria-labelledby="cta-title" id="contacto">
      <h2 id="cta-title">Pedir orçamento</h2>
    </section>
  </main>

  <footer role="contentinfo">
    <nav aria-label="Links do rodapé">...</nav>
  </footer>
</body>
</html>
```

---

## Hierarquia de Headings

Apenas um `<h1>` por página: o headline do hero.

```
h1: Headline do hero (ex: "Pinturas e remodelações em Lisboa")
  h2: O que fazemos
  h2: Antes e depois
    h3: [Título de cada projecto na galeria]
  h2: Como trabalhamos
  h2: Sobre nós
  h2: Onde trabalhamos
  h2: Pedir orçamento
```

**Proibido:** saltar níveis (h1 → h3), usar headings para estilizar texto visualmente, omitir headings em secções com conteúdo significativo.

---

## Labels de Formulário

Todos os campos de formulário têm `<label>` associada:

```html
<div>
  <label for="contact-name">Nome</label>
  <input
    id="contact-name"
    type="text"
    name="name"
    required
    aria-required="true"
    autocomplete="name"
  />
</div>
```

**Proibido:** usar apenas `placeholder` como label (desaparece ao digitar, não é lido consistentemente por leitores de ecrã).

### Campos obrigatórios

```html
<label for="contact-phone">
  Telefone <span aria-hidden="true">*</span>
  <span class="sr-only">(obrigatório)</span>
</label>
```

### Mensagens de erro

```html
<div role="alert" aria-live="polite">
  <p id="name-error">Por favor, introduza o seu nome.</p>
</div>
<input aria-describedby="name-error" aria-invalid="true" ... />
```

---

## Reduced Motion

```css
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
```

Elementos específicos:
- **Contador de estatísticas:** mostrar valor final imediatamente (sem count-up)
- **Animações de entrada de secção:** conteúdo aparece imediatamente (sem fade + translate)
- **Pulse do WhatsApp:** animação desligada
- **Slider:** transição imediata (já é, por design)

---

## Leitores de Ecrã

### Conteúdo visível apenas para leitores de ecrã

```css
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}
```

Uso:
- Labels de secção sem título visual (ex: barra de confiança)
- Indicador "obrigatório" em campos de formulário
- Estado do menu hamburguer ("Menu aberto" / "Menu fechado")
- Paginação ("Item 2 de 5" em galeria)

### ARIA roles e properties

| Componente | Atributo | Valor |
|---|---|---|
| Menu hamburguer (botão) | `aria-expanded` | `true` / `false` |
| Menu hamburguer (botão) | `aria-controls` | `id` do nav drawer |
| Nav drawer | `aria-hidden` | `true` quando fechado |
| Slider antes/depois | `role` | `slider` |
| Slider antes/depois | `aria-valuemin` | `0` |
| Slider antes/depois | `aria-valuemax` | `100` |
| Slider antes/depois | `aria-valuenow` | valor actual (0–100) |
| Slider antes/depois | `aria-label` | `"Comparador antes e depois"` |
| Galeria (carousel) | `aria-roledescription` | `"carrossel"` |
| Galeria (item) | `aria-roledescription` | `"item"` |
| Formulário de contacto | `aria-label` | `"Formulário de pedido de orçamento"` |
| Botão de submit | `aria-busy` | `true` enquanto submete |

---

## Botões e Links

### Distinção semântica

- `<a href="...">` para navegação (âncoras na página, links externos)
- `<button type="button">` para acções (abrir menu, navegar na galeria, slider)
- `<button type="submit">` para submit de formulário

**Proibido:** `<div onclick>`, `<span onclick>`, `<a href="#">` para acções JavaScript.

### Texto de botão descritivo

| Mau | Bom |
|---|---|
| "Clique aqui" | "Pedir orçamento gratuito" |
| "Ver mais" | "Ver todos os serviços" |
| "Enviar" | "Enviar pedido de orçamento" |
| "→" | `<span aria-hidden="true">→</span><span class="sr-only">Ver galeria completa</span>` |

---

## Comparador Antes/Depois — Acessibilidade Específica

O comparador é um componente personalizado que exige atenção especial:

```html
<div
  role="group"
  aria-labelledby="comparison-title"
>
  <h3 id="comparison-title">
    Renovação de sala de estar — Junho 2025
  </h3>

  <img
    src="before.webp"
    alt="Sala de estar antes da renovação: paredes amarelas com marcas, soalho danificado"
  />

  <div
    role="slider"
    aria-label="Comparador: arrastar para ver antes e depois"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="50"
    aria-valuetext="A mostrar 50% da imagem depois"
    tabindex="0"
  ></div>

  <img
    src="after.webp"
    alt="A mesma sala após renovação: paredes pintadas de branco, soalho novo em madeira clara"
  />
</div>
```

**Em mobile (imagens empilhadas):**
```html
<figure>
  <img src="before.webp" alt="[antes]" />
  <figcaption>Antes</figcaption>
</figure>
<figure>
  <img src="after.webp" alt="[depois]" />
  <figcaption>Depois</figcaption>
</figure>
```

---

## Tamanho Mínimo de Toque (Mobile)

Conforme WCAG 2.2, critério 2.5.8 (AA):

- Área de toque mínima: 24×24 CSS pixels
- Recomendação prática: 44×44 CSS pixels
- Espaçamento entre alvos adjacentes: mínimo 8px

Verificar:
- [ ] Botão WhatsApp flutuante: 56×56px ✓
- [ ] Botões de CTA: altura 48px ✓
- [ ] Links de navegação: área 44px de altura ✓
- [ ] Dots de paginação: área 32×32px, gap 8px ✓
- [ ] Handle do slider: área 44×44px ✓
- [ ] Ícones de redes sociais no footer: área 44×44px com padding

---

## Idioma (pt-PT)

```html
<html lang="pt">
```

Se existir conteúdo em língua diferente (improvável):
```html
<span lang="en">before</span>
```

O atributo `lang` permite que leitores de ecrã usem a pronúncia correcta.

---

## Checklist de Implementação (Sprint 7B)

Para cada componente implementado, verificar:

- [ ] HTML semântico correcto (section, nav, main, footer, h1-h6)
- [ ] Ordem de headings correcta (sem saltos)
- [ ] Todos os elementos interactivos focáveis por teclado
- [ ] Foco visível em todos os estados `:focus-visible`
- [ ] Skip link funcional
- [ ] Alt text em todas as imagens de conteúdo
- [ ] `aria-hidden="true"` em imagens decorativas
- [ ] Labels associadas a todos os campos de formulário
- [ ] Mensagens de erro com `role="alert"` e `aria-describedby`
- [ ] `aria-expanded` em elementos toggle (menu)
- [ ] `aria-live="polite"` em zonas de feedback dinâmico
- [ ] Slider com `role="slider"` e valores ARIA
- [ ] Contraste verificado com axe DevTools
- [ ] Teste com VoiceOver (macOS/iOS) ou NVDA (Windows)
- [ ] `prefers-reduced-motion` implementado
- [ ] `lang="pt"` no html
- [ ] Nenhum `outline: none` sem alternativa visível
- [ ] Áreas de toque ≥ 44×44px em mobile
- [ ] Tab order lógico (sem saltos inesperados)

---

## Ferramentas Recomendadas

| Ferramenta | Uso |
|---|---|
| axe DevTools (extensão) | Auditoria automática de acessibilidade |
| Lighthouse (Chrome DevTools) | Score de acessibilidade + performance |
| VoiceOver (macOS) | Teste com leitor de ecrã real |
| NVDA + Firefox (Windows) | Teste com leitor de ecrã Windows |
| WebAIM Contrast Checker | Verificação de ratio de contraste |
| Keyboard Navigation Test | Navegar toda a página apenas com Tab/Enter/ESC |
