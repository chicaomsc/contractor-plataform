# Estrutura de Conteúdo — Landing Page JR Pinturas

**Sprint:** 7A — Design Direction  
**Versão:** 1.0  
**Data:** 2026-07-12  
**Status:** Estrutura aprovada para wireframing

---

## Análise da Estrutura Proposta

A estrutura original proposta era:

> Header → Hero → Barra de confiança → Serviços → Galeria → Processo → Sobre → Área geográfica → CTA → Contactos → Footer

### Problemas identificados

**1. "Sobre a empresa" muito tarde (posição 7)**
Um visitante que não conhece a JR Pinturas precisa de uma âncora de identidade antes de ser empurrado para o CTA de orçamento. Colocar "Sobre" na posição 7 significa que chegam ao contacto sem saber com quem estão a falar. Recomendo mover "Sobre" para depois do processo (posição 6), mantendo a galeria como prova antes do contexto humano.

**2. "Contactos" duplicado com Footer**
Se o CTA de orçamento já inclui número de WhatsApp e formulário, uma secção "Contactos" separada antes do footer é redundante. Em mobile especialmente, o visitante não vai ler duas secções de contacto seguidas. Unificar: um único CTA final robusto que contém todas as formas de contacto.

**3. "Área geográfica" sem chamada de atenção no momento certo**
A área de cobertura é um filtro de qualificação: quem está fora não contacta, quem está dentro tem mais intenção de compra. Deve aparecer antes do CTA final, não depois ou integrada no footer onde é ignorada.

**4. A ordem Serviços → Galeria é correcta — confirmar**
Mostrar o que fazemos antes de mostrar provas é o fluxo certo. A galeria não tem contexto se o visitante ainda não entendeu os serviços.

**5. O Processo deve vir depois da galeria, não antes**
Depois de ver os resultados (galeria), o visitante pergunta: "Como é que funciona? O que posso esperar?" O processo responde exactamente a isso.

---

## Estrutura Recomendada

| # | Secção | Mudança vs. proposta original |
|---|---|---|
| 1 | Header | Mantido |
| 2 | Hero | Mantido |
| 3 | Barra de confiança | Mantido |
| 4 | Serviços | Mantido |
| 5 | Galeria antes/depois | Mantido |
| 6 | Processo de trabalho | Mantido |
| 7 | Sobre a empresa | Mantido (mas com âncora de identidade humana, não apenas texto institucional) |
| 8 | Área geográfica | Mantido |
| 9 | CTA + Contactos | **Fundido** — um CTA final que inclui todas as formas de contacto |
| 10 | Footer | Footer simplificado: logo, links legais, redes sociais |

**Eliminado:** Secção "Contactos" separada (absorvida pelo CTA final).

---

## Tabela Detalhada de Conteúdo por Secção

### 1. Header

| Campo | Detalhe |
|---|---|
| **Objetivo** | Orientação, credibilidade imediata, acesso rápido ao WhatsApp |
| **Mensagem principal** | Logo da empresa + navegação + CTA de WhatsApp sempre visível |
| **Prova/evidência** | — |
| **CTA** | "WhatsApp" (ícone + texto, abre wa.me) |
| **Conteúdo necessário do cliente** | Logo em SVG ou PNG de alta resolução, número de WhatsApp |
| **Dependência de API** | `GET /company/me` (branding: logoUrl, primary color) |
| **Prioridade mobile** | Header colapsado em hamburguer; WhatsApp sempre visível no canto direito |

**Conteúdo esperado:**
- Logo JR Pinturas
- Navegação: Serviços / Galeria / Como Funciona / Contacto
- Botão: "WhatsApp" com ícone

---

### 2. Hero

| Campo | Detalhe |
|---|---|
| **Objetivo** | Captar atenção imediata, enunciar a proposta de valor, iniciar o fluxo para o CTA |
| **Mensagem principal** | Uma promessa concreta, com localização e diferencial. Não um slogan vago. |
| **Prova/evidência** | Fotografia real de uma obra terminada (não genérica) |
| **CTA** | "Pedir orçamento gratuito" (primário) + "Ver trabalhos" (secundário, ancora na galeria) |
| **Conteúdo necessário do cliente** | 1–2 fotografias reais de alta qualidade de obras concluídas, frase de promessa aprovada |
| **Dependência de API** | `GET /company/me` (nome, slogan se existir) |
| **Prioridade mobile** | Fotografia em altura reduzida (40vh), texto abaixo da imagem (não sobreposto) |

**Exemplo de headline (placeholder — a validar com cliente):**
> "Pinturas e remodelações em [Cidade]. Orçamento em 24h, obra com prazo garantido."

**Variantes a evitar:**
- "Transformamos o seu espaço" — genérico, não factual
- "Qualidade e confiança" — promessa sem substância

---

### 3. Barra de Confiança

| Campo | Detalhe |
|---|---|
| **Objetivo** | Activar confiança imediata com dados concretos antes de pedir atenção para os serviços |
| **Mensagem principal** | Números reais que provam experiência (anos, obras, clientes, avaliação) |
| **Prova/evidência** | Os números são a prova |
| **CTA** | — (nenhum nesta secção) |
| **Conteúdo necessário do cliente** | Anos de actividade, número de obras realizadas, número de clientes, nota média (se existir) |
| **Dependência de API** | Nenhuma — conteúdo estático ou configurável por admin |
| **Prioridade mobile** | 2 métricas em linha, 2 em baixo (2×2) |

**Placeholder de métricas:**
- `[X] anos de experiência`
- `[X]+ obras concluídas`
- `[X]+ clientes satisfeitos`
- `Avaliação [X]/5` (se tiver Google Reviews)

---

### 4. Serviços

| Campo | Detalhe |
|---|---|
| **Objetivo** | Confirmar ao visitante que a JR Pinturas faz o que ele precisa |
| **Mensagem principal** | Lista clara e honesta dos serviços, com descrição curta e diferencial |
| **Prova/evidência** | — (a prova vem na galeria) |
| **CTA** | Por serviço: "Saber mais" (opcional, ancora no CTA geral) — não obrigatório |
| **Conteúdo necessário do cliente** | Nome de cada serviço, descrição curta (2–3 linhas), fotografia opcional por serviço |
| **Dependência de API** | `GET /public/services?slug={slug}` |
| **Prioridade mobile** | Lista vertical, 1 coluna, sem imagem por serviço |

**Serviços esperados (confirmar com cliente):**
- Pintura residencial
- Pintura comercial
- Ladrilhos e mosaicos
- Demolições
- Pavimentação
- Chão flutuante
- Remodelações gerais

**Estado vazio:** "Estamos a preparar a nossa lista de serviços. Entretanto, contacte-nos pelo WhatsApp."

---

### 5. Galeria Antes/Depois

| Campo | Detalhe |
|---|---|
| **Objetivo** | Mostrar resultados reais antes de qualquer argumento textual de vendas |
| **Mensagem principal** | "Veja o antes e o depois das nossas obras" |
| **Prova/evidência** | Fotografias reais com slider de comparação |
| **CTA** | "Ver toda a galeria" (opcional, abre modal ou página separada) |
| **Conteúdo necessário do cliente** | Mínimo 4 pares de fotografias antes/depois, com mesma perspectiva |
| **Dependência de API** | `GET /public/gallery?slug={slug}` |
| **Prioridade mobile** | Slider vertical (antes em cima, depois em baixo), swipe para mudar item |

**Estado com poucas imagens:** mostrar apenas os pares disponíveis, sem espaços em branco.
**Estado sem imagens:** secção ocultada — não exibir com placeholders genéricos.

---

### 6. Processo de Trabalho

| Campo | Detalhe |
|---|---|
| **Objetivo** | Remover a fricção do "e agora o que acontece?" — o visitante já quer mas tem medo do processo |
| **Mensagem principal** | 4 passos simples e concretos: contacto → visita → proposta → obra |
| **Prova/evidência** | Transparência no processo É a prova de profissionalismo |
| **CTA** | "Começar agora" (ancora no CTA final) |
| **Conteúdo necessário do cliente** | Validar os 4 passos com o modo de trabalho real da empresa |
| **Dependência de API** | Nenhuma — conteúdo estático |
| **Prioridade mobile** | 4 passos em coluna única, numerados, sem ícones de decoro |

**Passos propostos (placeholder):**
1. "Contacto por WhatsApp ou formulário"
2. "Visita ao local sem compromisso"
3. "Orçamento detalhado em 24–48h"
4. "Obra com prazo e limpeza garantidos"

---

### 7. Sobre a Empresa

| Campo | Detalhe |
|---|---|
| **Objetivo** | Humanizar a marca, construir confiança pessoal após ver os resultados e o processo |
| **Mensagem principal** | Quem está por trás, porque se orgulham do trabalho, valores concretos (não vagos) |
| **Prova/evidência** | Fotografia real da equipa ou do fundador |
| **CTA** | — ou "Fale connosco" |
| **Conteúdo necessário do cliente** | Texto "Sobre nós" (2–4 parágrafos), fotografia real da equipa ou do responsável |
| **Dependência de API** | `GET /company/me` (nome, descrição se existir) |
| **Prioridade mobile** | Fotografia acima, texto abaixo |

**Conteúdo pendente:** texto biográfico e fotografia da equipa.

**Evitar:** texto genérico como "Somos uma empresa com compromisso de qualidade". Preferir: "A JR Pinturas foi fundada em [ano] por [nome], com o objetivo de [missão concreta]."

---

### 8. Área Geográfica

| Campo | Detalhe |
|---|---|
| **Objetivo** | Qualificar o visitante: confirmar que a empresa actua na sua zona |
| **Mensagem principal** | Lista de concelhos ou zona de actuação + "Não tem a certeza? Pergunte-nos." |
| **Prova/evidência** | — |
| **CTA** | "Perguntar disponibilidade" (WhatsApp) |
| **Conteúdo necessário do cliente** | Lista de concelhos ou distrito de actuação |
| **Dependência de API** | Nenhuma — conteúdo estático ou configurável por admin |
| **Prioridade mobile** | Lista compacta, sem mapa se pesado |

**Placeholder:** `[Área de actuação: confirmar com cliente — concelhos de Lisboa? Grande Porto? Algarve?]`

**Nota:** não incluir mapa interactivo por defeito — aumenta tempo de carregamento. Texto é suficiente.

---

### 9. CTA Final + Contactos

| Campo | Detalhe |
|---|---|
| **Objetivo** | Converter o visitante qualificado — quem chegou aqui, quer contactar |
| **Mensagem principal** | Pedido de orçamento simplificado, sem fricção |
| **Prova/evidência** | "Resposta em menos de 24h" ou similar |
| **CTA** | "Enviar mensagem pelo WhatsApp" (principal) + formulário simples (alternativo) |
| **Conteúdo necessário do cliente** | Número de WhatsApp, email de contacto, horário de atendimento |
| **Dependência de API** | `GET /company/me` (whatsapp, email, phone) |
| **Prioridade mobile** | Botão WhatsApp gigante acima do formulário |

**Campos do formulário (mínimo viável):**
- Nome
- Contacto (telefone ou email)
- Serviço de interesse (dropdown com opções de `/services`)
- Mensagem (textarea)
- Botão "Enviar" → envia por email (Sprint 7B ou 8)

**Nota:** o formulário é secundário ao WhatsApp. Em Portugal, a maioria dos clientes prefere WhatsApp. O formulário existe para quem não usa WhatsApp.

---

### 10. Footer

| Campo | Detalhe |
|---|---|
| **Objetivo** | Referência legal e navegação secundária — não um elemento de conversão |
| **Mensagem principal** | — |
| **Prova/evidência** | — |
| **CTA** | — |
| **Conteúdo necessário do cliente** | Nome legal da empresa, NIF (opcional), morada (opcional), links de redes sociais |
| **Dependência de API** | `GET /company/me` (name, taxNumber, website) |
| **Prioridade mobile** | Compacto, links em coluna única |

**Conteúdo:**
- Logo (pequeno)
- Nome legal + "Todos os direitos reservados [ano]"
- Links: Política de Privacidade / Termos de Utilização
- Redes sociais (ícones) se existirem
- "Powered by Contractor Platform" (opção de remover no admin)

---

## Conteúdo Pendente do Cliente

| # | Conteúdo | Urgência | Notas |
|---|---|---|---|
| 1 | Logo em alta resolução (SVG ou PNG 2x) | Alta | Necessária antes de qualquer implementação |
| 2 | Número de WhatsApp de negócio | Alta | Formato wa.me compatível |
| 3 | 1–2 fotografias hero (obras concluídas, alta qualidade) | Alta | Bloqueante para Sprint 7B |
| 4 | Mínimo 4 pares de fotografias antes/depois | Alta | Bloqueante para Sprint 7B |
| 5 | Frase de promessa aprovada para o hero | Média | Placeholder pode ser usado temporariamente |
| 6 | Dados de confiança: anos, nº de obras, nº de clientes | Média | Necessários antes do lançamento |
| 7 | Texto "Sobre a empresa" (2–4 parágrafos) | Média | Pode ser criado após wireframes |
| 8 | Fotografia da equipa ou do fundador | Média | Alternativa: fotografia de obra em execução |
| 9 | Lista de concelhos da área de actuação | Média | — |
| 10 | Horário de atendimento | Baixa | Para incluir no CTA/footer |
| 11 | Email de contacto | Baixa | Para formulário alternativo |
| 12 | Links de redes sociais | Baixa | Instagram especialmente útil para galeria |

---

## Dependências de API por Secção

| Secção | Endpoint | Campos utilizados |
|---|---|---|
| Header | `GET /public/company?slug={slug}` | logoUrl, primaryColor, name |
| Hero | `GET /public/company?slug={slug}` | name, tagline (se existir) |
| Serviços | `GET /public/services?slug={slug}` | name, shortDescription, icon, displayOrder |
| Galeria | `GET /public/gallery?slug={slug}` | title, beforeImageUrl, afterImageUrl, featured |
| Sobre | `GET /public/company?slug={slug}` | name, description |
| CTA Final | `GET /public/company?slug={slug}` | whatsapp, email, phone |
| Footer | `GET /public/company?slug={slug}` | name, taxNumber, website |

> Nota: os endpoints `/public/company` e `/public/branding` devem ser verificados — o backend actual expõe `/company/me` (autenticado). A Sprint 7B deve incluir o endpoint público de perfil da empresa por slug.
