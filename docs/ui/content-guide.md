# Content Guide — Contractor Platform

**Sprint:** 7A — Design Direction (refinamento)  
**Versão:** 1.0  
**Data:** 2026-07-12  
**Aplicação:** Landing page de qualquer tenant

---

> Este guia define a linguagem escrita da plataforma. As regras aplicam-se ao conteúdo padrão gerado pela plataforma (labels, microcopy, mensagens de sistema). O conteúdo comercial de cada tenant (hero headline, descrição de serviços, sobre a empresa) segue os princípios de tom mas é da responsabilidade do próprio tenant.

---

## Tom de Voz

### Três palavras que definem o tom

**Directo. Concreto. Próximo.**

- **Directo:** vai ao assunto. Não existe uma segunda frase se a primeira é suficiente.
- **Concreto:** usa factos, números e localização. Nunca promessas genéricas.
- **Próximo:** fala como um profissional experiente a um cliente real, não como uma marca corporativa.

### O que este tom não é

| Não é | É |
|---|---|
| Formal e distante ("Solicite uma proposta comercial") | Humano e directo ("Pedir orçamento") |
| Vago e inspiracional ("Transformamos espaços") | Factual e concreto ("Pinturas e remodelações em Lisboa") |
| Corporativo e frio ("Somos líderes em qualidade") | Honesto e próximo ("Fazemos obras que duram") |
| Excessivamente técnico | Claro para qualquer pessoa |
| Tentativa de impressionar | Tentativa de ajudar |

---

## Princípios de Comunicação

**1. Uma ideia por frase**  
Frases longas são sintoma de pensamento pouco claro. Se uma vírgula pode tornar-se um ponto, torna-se um ponto.

**2. Activo em vez de passivo**  
"Fazemos obras com prazo garantido" — não "As obras são realizadas com prazo garantido".

**3. Substantivo antes do adjectivo**  
Definir antes de qualificar. "Pintura residencial de qualidade" — não "Serviço de qualidade para pintura residencial".

**4. Localização é credibilidade**  
Mencionar a região ou cidade concreta aumenta a confiança. "Em Lisboa" vale mais do que "em Portugal".

**5. O cliente no centro da frase**  
"O seu orçamento em 24h" — não "Emitimos orçamentos em 24h".

**6. Evitar superlativos não provados**  
"Melhor empresa de pintura" é uma promessa vazia. "4.9/5 em 80 avaliações Google" é uma prova.

**7. Microcopy conta a experiência**  
Os textos de botão, placeholder, label e erro são tão importantes quanto o headline. Não são afterthoughts.

---

## Personalidade da Empresa (para o content default da plataforma)

A plataforma serve prestadores de serviço. O conteúdo padrão (empty states, onboarding, erros) deve reflectir o perfil do utilizador:

- **Prático** — não tem tempo para textos longos
- **Orgulhoso do ofício** — valoriza referências ao trabalho manual e à qualidade
- **Céptico de tecnologia** — a plataforma deve parecer simples e fiável, não inovadora
- **Local** — opera numa região específica, com clientes reais

---

## Estilo de Escrita

### Capitalização

- Títulos de secção: primeira letra em maiúscula, resto em minúscula (não Title Case)
  - Correcto: "O que fazemos"
  - Incorrecto: "O Que Fazemos"
- Labels de secção em CAPS (via CSS, não no texto HTML): "SERVIÇOS", "GALERIA"
- Nomes de empresa: sempre como a empresa usa (respeitar identidade do tenant)

### Pontuação

- Não usar ponto final em títulos de secção, headings ou botões
- Usar ponto final em parágrafos de corpo de texto
- Evitar reticências (...) — sugerem hesitação
- Usar travessão (—) em vez de hífen para pausas em frase corrida

### Numerais

- Números abaixo de 10: por extenso em texto corrido ("oito anos", não "8 anos")
- Números acima de 10: numeral ("150 obras", não "cento e cinquenta obras")
- Excepção: sempre numeral em métricas e estatísticas de destaque ("8 anos", "4.9/5")

### Língua

- Português europeu (pt-PT)
- "Utilizador" e não "usuário"
- "Orçamento" e não "proposta" (no contexto de obras)
- "Contacto" e não "contato"
- "Obra" e não "projeto" (no contexto de remodelações)

---

## Regras para Títulos (h1, h2)

### h1 — Hero headline

- Uma frase, máximo duas linhas em mobile
- Deve conter: o que fazem + onde estão
- Pode conter: diferencial concreto (prazo, orçamento, garantia)
- Nunca: slogan vago, promessa não verificável, jargão de marketing

| Mau | Bom |
|---|---|
| "Transformamos o seu espaço" | "Pinturas e remodelações em Lisboa" |
| "Qualidade que faz a diferença" | "Orçamento em 24h, obra com prazo garantido" |
| "A sua casa merece o melhor" | "Acabamentos que duram. Em [cidade]." |

### h2 — Títulos de secção

- Descritivos, não aspiracionais
- Máximo 5 palavras
- Sem ponto final

| Mau | Bom |
|---|---|
| "Descubra os nossos serviços" | "O que fazemos" |
| "Trabalhos realizados com excelência" | "Antes e depois" |
| "Conheça a nossa equipa dedicada" | "Sobre nós" |

---

## Regras para Subtítulos (h3, lead text)

- Complementam o título sem o repetir
- Acrescentam uma segunda dimensão (detalhe, prova, contexto)
- Máximo 2 frases

**Exemplo:**
- Título: "Antes e depois"
- Subtítulo: "Veja o resultado real das obras que realizámos nos últimos meses."

**Não:**
- Subtítulo que repete o título com outras palavras
- Subtítulo mais longo que o título

---

## Regras para CTAs

### Princípios

- O CTA deve dizer o que acontece ao clicar, não o que o utilizador quer (não "Quero um orçamento" — mas "Pedir orçamento")
- Verbos no infinitivo: "Pedir", "Ver", "Enviar", "Contactar"
- Máximo 4 palavras
- Sem exclamação

### CTAs da landing (exemplos)

| Contexto | CTA primário | CTA secundário |
|---|---|---|
| Hero | "Pedir orçamento gratuito" | "Ver os nossos trabalhos" |
| Processo | "Começar agora" | — |
| CTA final | "Enviar pelo WhatsApp" | "Enviar mensagem" |
| Header | "WhatsApp" | — |

### CTAs do sistema (painel admin)

| Contexto | CTA |
|---|---|
| Guardar alterações | "Guardar" |
| Eliminar item | "Eliminar" |
| Confirmar eliminação | "Sim, eliminar" |
| Cancelar acção | "Cancelar" |
| Fazer upload | "Carregar imagem" |
| Adicionar serviço | "Adicionar serviço" |

---

## Microcopy

### Placeholders em campos de formulário

- Descrever o formato, não o label (o label já explica o campo)
- Não usar "Introduza o seu..." — demasiado formal

| Campo | Placeholder (mau) | Placeholder (bom) |
|---|---|---|
| Nome | "Introduza o seu nome completo" | "João Silva" |
| Telefone | "Número de telefone" | "912 345 678" |
| Email | "Endereço de email" | "joao@exemplo.pt" |
| Serviço | "Seleccione um serviço" | "Tipo de serviço" |
| Mensagem | "Escreva a sua mensagem" | "Descreva brevemente o que precisa" |

### Labels de formulário

- Curtas e descritivas
- Sem dois-pontos no final
- Minúscula após a primeira letra

| Mau | Bom |
|---|---|
| "Nome Completo:" | "Nome" |
| "Número de Telefone:" | "Telefone" |
| "Tipo de Serviço Pretendido:" | "Serviço" |

### Confirmações inline

- Após acção bem-sucedida, usar mensagem específica — não "Sucesso!"
- Máximo 1 frase

| Acção | Confirmação |
|---|---|
| Formulário enviado | "Mensagem enviada. Respondemos em menos de 24h." |
| Logo carregado | "Logo actualizado." |
| Serviço guardado | "Serviço guardado." |
| Imagem eliminada | "Imagem removida." |

---

## Mensagens Vazias (Empty States)

### Princípios

- Explicar o estado actual com honestidade
- Oferecer uma saída (CTA ou instrução)
- Nunca usar ícone genérico como única comunicação
- Nunca deixar campo completamente vazio

### Exemplos

**Sem serviços (landing):**
> "Estamos a preparar a nossa lista de serviços.  
> Entretanto, contacte-nos pelo WhatsApp."  
> [Botão: "WhatsApp"]

**Sem galeria (landing):**
> *(secção ocultada — não exibir empty state na landing)*

**Sem obras na galeria (admin):**
> "Ainda não tem obras na galeria.  
> Adicione o primeiro par de fotografias antes/depois."  
> [Botão: "Adicionar obra"]

**Sem serviços (admin):**
> "Ainda não criou nenhum serviço.  
> Os serviços aparecem na sua landing page."  
> [Botão: "Criar serviço"]

**Lista de clientes vazia (admin — Sprint futura):**
> "Nenhum cliente ainda.  
> Os clientes aparecem quando criar o primeiro orçamento."

---

## Mensagens de Erro

### Princípios

- Dizer o que correu mal (específico, não genérico)
- Dizer o que fazer a seguir
- Nunca culpar o utilizador
- Nunca usar jargão técnico

### Erros de formulário (validação)

| Campo | Erro genérico (mau) | Erro específico (bom) |
|---|---|---|
| Nome vazio | "Campo obrigatório" | "Introduza o seu nome" |
| Email inválido | "Email inválido" | "Verifique o formato do email (ex: nome@exemplo.pt)" |
| Telefone inválido | "Número inválido" | "Introduza um número de telefone válido" |
| Ficheiro demasiado grande | "Erro no upload" | "A imagem não pode ter mais de 5MB" |
| Tipo de ficheiro inválido | "Formato inválido" | "Carregue uma imagem JPG, PNG ou WebP" |

### Erros de sistema

| Situação | Mensagem |
|---|---|
| Falha no envio do formulário | "Não foi possível enviar a mensagem. Tente novamente ou contacte-nos pelo WhatsApp." |
| Página não encontrada (404) | "Esta página não existe ou foi removida." |
| Empresa não encontrada | "Não encontrámos esta empresa. Verifique o endereço." |
| Erro de carregamento de imagem | "Não foi possível carregar a imagem. Tente mais tarde." |
| Sessão expirada (admin) | "A sua sessão expirou. Inicie sessão novamente." |

---

## Conteúdo por Secção — Exemplos e Anti-exemplos

---

### Hero

**Tom:** Assertivo, concreto, local  
**Estrutura:** [O que fazem] em [onde]. [Diferencial concreto].

| Exemplo | Classificação |
|---|---|
| "Pinturas e remodelações em Cascais. Orçamento gratuito em 24h." | Bom |
| "Chão flutuante, ladrilhos e demolições no Grande Porto." | Bom |
| "Transformamos a sua casa com qualidade e dedicação." | Mau — genérico |
| "A sua remodelação começa aqui." | Mau — vago |
| "Somos especialistas em pintura e muito mais!" | Mau — exclamação, "e muito mais" |

**Lead text (subtítulo):**

| Exemplo | Classificação |
|---|---|
| "Realizamos obras de pintura, ladrilhos e remodelação com prazo acordado e limpeza garantida no final." | Bom |
| "A melhor empresa de pintura da região com anos de experiência." | Mau — superlativo, genérico |

---

### Serviços

**Tom:** Descritivo, técnico mas acessível  
**Estrutura:** [Nome do serviço] + [Descrição de 1–2 linhas]

| Exemplo | Classificação |
|---|---|
| "Pintura residencial — Interior e exterior de habitações, incluindo preparação de superfícies e acabamento final." | Bom |
| "Ladrilhos e mosaicos — Aplicação e substituição de pavimentos e revestimentos cerâmicos." | Bom |
| "Fazemos tudo o que a sua casa precisa!" | Mau — vago, exclamação |
| "Serviços de pintura de alta qualidade para todos os tipos de espaços" | Mau — "alta qualidade" sem substância |

---

### Processo

**Tom:** Claro, tranquilizador, sequencial  
**Estrutura:** Número + Título do passo + Explicação curta (1 frase)

| Passo | Exemplo (bom) | Exemplo (mau) |
|---|---|---|
| 1 | "Contacto — WhatsApp ou formulário, respondemos em menos de 24h." | "Entre em contacto connosco através dos nossos canais!" |
| 2 | "Visita — Deslocamo-nos ao local, sem compromisso, para avaliar o trabalho." | "A nossa equipa visita o espaço para entender as necessidades." |
| 3 | "Orçamento — Enviamos uma proposta detalhada, sem surpresas escondidas." | "Apresentamos uma solução personalizada e competitiva." |
| 4 | "Obra — Executamos dentro do prazo acordado e limpamos no final." | "Realizamos o trabalho com excelência e cuidado." |

---

### Sobre a Empresa

**Tom:** Pessoal, honesto, orgulhoso do ofício  
**Estrutura:** Fundação + missão concreta + valores observáveis

| Exemplo | Classificação |
|---|---|
| "A JR Pinturas foi fundada em [ano] por João Ramos, com o objetivo de oferecer obras de qualidade com o rigor de quem cuida do próprio espaço." | Bom |
| "Somos uma empresa com compromisso de excelência e valores sólidos." | Mau — genérico |
| "A nossa missão é transformar os seus sonhos em realidade." | Mau — clichê |
| "Ao longo de [X] anos, realizámos mais de [N] obras em [região]." | Bom — factual |

---

### CTA Final

**Tom:** Directo, sem pressão, com expectativa clara  
**Estrutura:** Pedido + Promessa de tempo de resposta

| Exemplo | Classificação |
|---|---|
| "Peça o seu orçamento gratuito. Respondemos em menos de 24 horas." | Bom |
| "Não espere mais! Contacte-nos agora!" | Mau — exclamação, urgência artificial |
| "Pronto para começar? Fale connosco." | Mau — pergunta retórica, vaga |
| "Orçamento sem compromisso. Obra com prazo garantido." | Bom — duas promessas concretas |

---

### Footer

**Tom:** Neutro, funcional  
**Conteúdo:** legal, navegação secundária — sem texto de marketing

| Exemplo | Classificação |
|---|---|
| "© 2026 JR Pinturas. Todos os direitos reservados." | Bom |
| "Feito com ❤️ pela JR Pinturas para os nossos clientes!" | Mau — emoção forçada, exclamação |
| "A melhor empresa de pintura de Lisboa, ao seu dispor." | Mau — marketing no footer |
