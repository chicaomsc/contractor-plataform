# ADR-006 — Estratégia de Renderização do Frontend

- **Status:** Aceito
- **Data:** 2026-07-12
- **Sprint:** 7B — Frontend Foundation

## Contexto

O Contractor Platform precisa servir a landing pública, a autenticação e o painel administrativo futuro. O backend Spring Boot já concentra autenticação, multi-tenancy, regras de negócio, persistência e API REST.

A Sprint 7A definiu uma landing multi-tenant orientada por dados públicos da API. A Sprint 7B consolida a fundação frontend sem implementar as secções reais da landing.

## Decisão

Usar um único frontend Next.js com App Router para:

- landing pública;
- autenticação;
- painel administrativo futuro.

O Next.js é apenas camada de apresentação, renderização e integração HTTP. O Spring Boot permanece como único backend e única fonte de regras de negócio.

O dashboard futuro terá comportamento semelhante a uma SPA tradicional, com TanStack Query para estado remoto, React Hook Form para formulários e Zod para validação de formulário. O código client-side deve ficar limitado aos boundaries necessários e continuar portável para Vite caso o dashboard seja separado futuramente.

Esta decisão é específica do Contractor Platform e não deve ser propagada ao `dwcore-starter`.

## Alternativas consideradas

1. React + Vite para todo o frontend.
2. Dois frontends separados: Next.js para landing e Vite para dashboard.
3. Full-stack Next.js com Route Handlers ou Server Actions como backend de negócio.

Vite continua válido para sistemas predominantemente autenticados, especialmente quando SSR/SEO não são factores relevantes. Dois frontends separados podem voltar a ser avaliados se landing e dashboard crescerem de forma independente. Full-stack Next.js foi rejeitado porque duplicaria responsabilidades já pertencentes ao Spring Boot.

## Consequências positivas

- Um único projeto frontend para deploy e design system.
- App Router oferece metadata, sitemap, robots e renderização pública adequadas para SEO.
- O dashboard pode usar padrões SPA sem transformar toda a aplicação em Client Components.
- A landing pública consome apenas contratos REST públicos e continua multi-tenant.

## Consequências negativas

- Exige disciplina para não criar backend-sombra dentro do Next.js.
- O dashboard precisa de boundaries explícitos para manter portabilidade futura.
- O projeto frontend passa a misturar superfícies públicas e autenticadas, exigindo organização por responsabilidade.

## Restrições

- Não implementar regras de negócio em Next.js.
- Não acessar PostgreSQL diretamente pelo frontend.
- Não persistir dados no Next.js.
- Não duplicar validações de domínio do backend.
- Não criar autenticação paralela ao Spring Security.
- Não usar Route Handlers como backend-sombra.
- Não usar Server Actions para substituir endpoints Spring Boot.
- Toda operação de negócio deve consumir a API REST do Spring Boot.

## Critérios de revisão futura

Revisar esta decisão se:

- landing pública e dashboard crescerem com ciclos de release independentes;
- o dashboard exigir separação operacional ou bundle isolado;
- a landing exigir estratégia editorial ou CMS próprios;
- Vite voltar a ser mais adequado para uma superfície predominantemente autenticada.
