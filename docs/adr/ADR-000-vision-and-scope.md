# ADR-000 — Visão e Escopo do Produto

- **Status:** Aceito
- **Data:** 2026-07-04
- **Autores:** Equipe de Arquitetura
- **Decisores:** Francisco Costa

---

## Contexto

Prestadores de serviço autônomos e pequenas empresas de serviços (pintura, construção, elétrica, jardinagem, etc.) carecem de presença digital profissional e de ferramentas acessíveis para gestão comercial básica — especialmente geração de orçamentos formais. As alternativas existentes são: planilhas manuais, ferramentas genéricas de CRM (caras e complexas) ou sistemas setoriais rígidos sem personalização visual.

O ponto de partida é um cliente beta real — uma empresa de pintura com sede em Portugal — cujas necessidades concretas validam o modelo de produto. O sistema, porém, deve ser construído de forma genérica desde o início, sem dependências a este cliente específico no código.

O codinome técnico do projeto é **contractor-platform**. O nome comercial do produto ainda não foi definido e não deve constar no código-fonte.

---

## Decisão

Construir uma plataforma SaaS multi-tenant voltada a prestadores de serviço, com os seguintes pilares:

1. **Presença digital gerenciada:** landing page dinâmica (serviços, galeria, branding) consumida via API REST, sem CMS externo.
2. **Geração de orçamentos:** fluxo completo de criação de estimativa com itens, materiais e exportação em PDF.
3. **Painel administrativo próprio:** interface web para o prestador gerir sua empresa, serviços, clientes e orçamentos.
4. **Multi-tenancy by design:** isolamento de dados por `company_id` desde o primeiro commit, suportando múltiplos prestadores na mesma instância futuramente.
5. **Domínio genérico:** entidades e regras de negócio sem acoplamento a segmentos específicos (pintura, elétrica, etc.).

---

## Escopo do MVP

| Módulo | Incluído no MVP |
|---|---|
| Autenticação (login/logout com JWT) | Sim |
| Gestão de empresa (Company, Branding, Settings) | Sim |
| Gestão de serviços | Sim |
| Galeria de imagens | Sim |
| Gestão de clientes | Sim |
| Orçamentos, itens e materiais | Sim |
| Exportação de orçamento em PDF | Sim |
| Landing page dinâmica (público) | Sim |
| Dashboard com métricas | Não |
| CRM completo | Não |
| Integração de pagamento (Stripe) | Não |
| Auto-cadastro multi-tenant (self-service) | Não |
| Integração WhatsApp API | Não |
| Agenda/calendário | Não |
| Depoimentos de clientes | Não |

---

## Alternativas Consideradas

### A1 — Produto verticalmente especializado para pintura
Criar um produto específico para empresas de pintura, com terminologia e fluxos do setor.

**Rejeitado:** Limita o potencial de mercado sem ganho técnico real na fase inicial. A genericidade não adiciona complexidade significativa se aplicada desde o início.

### A2 — Usar um CMS headless existente (Contentful, Strapi)
Delegar a gestão de conteúdo da landing a um CMS externo.

**Rejeitado:** Introduz dependência externa, custo adicional e complexidade de integração para um conjunto de conteúdo controlado e de baixa variabilidade. A API própria é suficiente e mantém o sistema coeso.

### A3 — Começar como produto single-tenant
Construir sem isolamento multi-tenant no MVP, adicionando `company_id` apenas quando necessário.

**Rejeitado:** A adição retroativa de `company_id` gera migrações custosas e risco de vazamento de dados. O custo de incluí-lo desde o início é mínimo.

---

## Consequências

**Positivas:**
- A genericidade permite expandir para outros segmentos sem refatoração de domínio.
- O isolamento por `company_id` garante a base para multi-tenancy futuro sem retrabalho.
- O cliente beta provê um caso de uso concreto que valida os módulos do MVP com dados reais.

**Negativas / Riscos:**
- A genericidade exige disciplina para não deixar vazar terminologia específica do cliente beta no código.
- O escopo do MVP é amplo para uma primeira sprint; priorização rigorosa dentro da sprint é obrigatória.
- A ausência de nome comercial pode atrasar decisões de branding no frontend.

---

## Referências

- [ADR-001 — Estilo de Arquitetura](ADR-001-architecture-style.md)
- [Modelo de Domínio](../architecture/domain-model.md)
- [Roadmap](../roadmap.md)
