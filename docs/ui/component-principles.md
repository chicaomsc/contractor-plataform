# Princípios de Componentes — Contractor Platform

**Sprint:** 7A — Design Direction (refinamento)  
**Versão:** 1.0  
**Data:** 2026-07-12  
**Aplicação:** Todo o frontend — landing e painel admin

---

> Estes princípios são obrigatórios. Não são sugestões. Qualquer implementação que os viole deve ser justificada explicitamente antes de ser aceite.

---

## Os Princípios

---

### 1. Layout vence decoração

O que define a identidade visual é a composição dos elementos no espaço, não os efeitos aplicados sobre eles. Um layout assimétrico e bem proporcionado comunica mais do que qualquer `box-shadow` ou `gradient`.

**Na prática:**
- Trabalhar primeiro a estrutura (onde fica o quê, em que proporção)
- Adicionar cor, tipografia e texturas apenas depois
- Se remover todos os efeitos visuais e o layout ainda funcionar, o layout está certo

---

### 2. Fotografia vence ilustração

Uma fotografia real de uma obra tem mais impacto de conversão do que qualquer ilustração, ícone decorativo ou imagem de stock. A autenticidade é o diferencial.

**Na prática:**
- Nunca substituir fotografia real por ilustração (mesmo que temporariamente em produção)
- Placeholders devem ser fundos de cor, não ilustrações genéricas
- Ícones existem para apoiar texto, nunca para substituir conteúdo visual
- Se não há fotografia, usar fundo de textura + tipografia. Nunca clip-art

---

### 3. Espaçamento vence borda

Elementos que respiram não precisam de bordas para se separar. A tentação de adicionar `border` a tudo é sinal de que o espaçamento está insuficiente.

**Na prática:**
- Antes de adicionar uma borda, aumentar o `margin` ou `padding`
- Bordas são reservadas para elementos que precisam de delimitação explícita (inputs, cards de dados, tabelas)
- Separadores de secção: linha de 1px, não `border-radius` em painel colorido

---

### 4. Hierarquia vence cor

A estrutura de leitura é estabelecida pelo tamanho, peso e posição dos elementos — não pela cor. Se remover a cor e a hierarquia colapsar, o design não tem hierarquia real.

**Na prática:**
- O `h1` deve ser imediatamente reconhecível sem leitura — só pelo tamanho
- A cor de acento (laranja) existe para **uma** acção por contexto — não para múltiplos destaques simultâneos
- Texto muted existe para hierarquia secundária — não para estética

---

### 5. Branco é componente

O espaço em branco (ou espaço negativo) não é a ausência de conteúdo. É um componente activo que cria respiração, foco e elegância.

**Na prática:**
- Não tentar preencher todos os espaços "vazios"
- `padding` generoso em secções é uma decisão de design, não um erro
- Em dúvida entre adicionar um elemento e manter o espaço, manter o espaço

---

### 6. Cada secção tem um ponto focal

O utilizador entra numa secção e o olho deve saber imediatamente onde ir. Se existirem vários elementos a competir em peso visual, nenhum deles ganha.

**Na prática:**
- Hero: ponto focal é a fotografia (desktop) ou o headline (mobile)
- Estatísticas: ponto focal são os números
- Galeria: ponto focal é a comparação antes/depois
- Processo: ponto focal é a numeração
- Nunca mais de um elemento de destaque por secção

---

### 7. Evitar cards por defeito

O card (rectângulo com fundo, borda e sombra) é o recurso mais sobreutilizado no web design. A maioria das vezes é desnecessário.

**Na prática:**
- Serviços: lista com separador de linha, não grid de cards
- Processo: lista numerada, não cards com ícone
- Estatísticas: números directamente sobre o fundo, sem cards
- Usar card apenas quando o conteúdo é genuinamente autónomo e comparável (ex: planos de preços, itens de catálogo no admin)

---

### 8. Evitar grids simétricos quando não acrescentam valor

Três colunas iguais com o mesmo conteúdo é a solução preguiçosa. Criar assimetria deliberada produz mais interesse visual e dirige melhor a atenção.

**Na prática:**
- Galeria: um item featured maior, restantes menores
- Layout de secção: 55/45 em vez de 50/50
- Processo: linha com numeração que escala (o número 1 domina, o 4 é menor)
- Evitar `grid-template-columns: repeat(3, 1fr)` em marketing sections

---

### 9. O conteúdo deve respirar

Nunca comprimir conteúdo para caber mais numa viewport. Mais conteúdo visível não é melhor experiência — é mais ruído.

**Na prática:**
- Secções têm `padding-block` mínimo de 64px (mobile) e 96px (desktop)
- Parágrafos têm `line-height` mínimo de 1.7
- Listas têm `gap` ou `margin-bottom` de pelo menos 12px por item
- O scroll é gratuito — deixar o utilizador rolar

---

### 10. As imagens contam a história

Se as imagens forem removidas e o conteúdo ainda fizer sentido, as imagens eram decorativas. Se sem as imagens a história desaparece, as imagens estão a fazer o trabalho certo.

**Na prática:**
- Imagem de hero: deve mostrar uma obra real, com contexto reconhecível
- Galeria antes/depois: conta a história de uma transformação concreta
- Fotografia da equipa: humaniza a marca — não substituir por ícone ou placeholder
- Não usar imagens de stock — detectável, reduz credibilidade

---

### 11. Componentes são genéricos, conteúdo é específico

Nenhum componente deve conhecer os dados de um tenant específico. O componente define a estrutura; a API fornece o conteúdo.

**Na prática:**
- `HeroSection` aceita `headline`, `imageUrl`, `cta` — não sabe que é a JR Pinturas
- `ServiceList` aceita `services: ServiceItem[]` — não tem nomes de serviços hardcoded
- Nunca: `if (company.name === "JR Pinturas") { ... }` num componente

---

### 12. Acessibilidade é arquitetura, não acabamento

Acessibilidade não se "adiciona" no final. Está na estrutura semântica, na hierarquia de headings, nas áreas de toque, no contraste. Implementar após o facto é sempre mais caro.

**Na prática:**
- HTML semântico é não-negociável (`section`, `nav`, `main`, `h1`–`h6`, `button`, `a`)
- Foco visível nunca é removido sem alternativa
- Imagens têm `alt` desde o primeiro commit
- Formulários têm `label` desde o primeiro commit

---

## Checklist por Pull Request

Antes de submeter qualquer PR com componente novo:

- [ ] O layout funciona sem cor ou efeitos visuais?
- [ ] Existe apenas um ponto focal por secção?
- [ ] O espaçamento é suficiente para dispensar bordas desnecessárias?
- [ ] O componente não conhece dados de nenhum tenant específico?
- [ ] HTML semântico correcto?
- [ ] Foco visível implementado?
- [ ] Imagens têm alt text?
- [ ] Funciona em mobile sem layout partido?
- [ ] Respeita `prefers-reduced-motion`?
