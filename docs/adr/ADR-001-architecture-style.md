# ADR-001 — Estilo de Arquitetura

- **Status:** Aceito
- **Data:** 2026-07-04
- **Autores:** Equipe de Arquitetura
- **Decisores:** Francisco Costa

---

## Contexto

O sistema precisa de uma arquitetura que equilibre:

- **Velocidade de entrega inicial:** equipe pequena, MVP com prazo definido.
- **Organização e manutenibilidade:** evitar o crescimento caótico de um "big ball of mud".
- **Preparação para evolução:** módulos que possam ser extraídos futuramente se necessário, sem que isso seja uma exigência do MVP.
- **Simplicidade operacional:** infraestrutura mínima para deploy, monitoramento e debugging.

O projeto é uma aplicação web com backend REST e frontend SPA. A carga esperada no lançamento é baixa (cliente beta único), com escala gradual prevista.

---

## Decisão

Adotar **monólito modular** como estilo de arquitetura para o backend, combinado com **SPA desacoplada** no frontend.

### Backend — Monólito Modular

O backend é uma única aplicação Spring Boot organizada em módulos coesos por domínio. Cada módulo possui:

- **Pacote próprio** sob `io.chicaodw.platform.<módulo>`
- **Fronteira explícita:** classes internas do módulo são package-private; apenas interfaces/DTOs de contrato são públicos
- **Responsabilidade única:** cada módulo gerencia seu próprio domínio sem acessar diretamente repositórios de outros módulos
- **Sem dependências cíclicas** entre módulos

Módulos previstos no MVP:

| Módulo | Pacote | Responsabilidade |
|---|---|---|
| `auth` | `io.chicaodw.platform.auth` | Autenticação, JWT, Spring Security |
| `company` | `io.chicaodw.platform.company` | Company, Branding, Settings |
| `catalog` | `io.chicaodw.platform.catalog` | Service, GalleryItem |
| `customer` | `io.chicaodw.platform.customer` | Customer |
| `estimate` | `io.chicaodw.platform.estimate` | Estimate, EstimateItem, Material |
| `public` | `io.chicaodw.platform.public` | Endpoints públicos da landing page |
| `shared` | `io.chicaodw.platform.shared` | Utilitários, exceções, DTOs comuns |

### Frontend — SPA Desacoplada

Aplicação React + Vite + TypeScript, hospedada independentemente (Vercel), comunicando-se exclusivamente via API REST. Dividida em duas áreas:

- **Landing pública:** rotas acessíveis sem autenticação, consumindo a API pública.
- **Painel administrativo:** rotas protegidas por autenticação JWT.

### Comunicação

- Backend expõe API REST JSON versionada sob `/api/v1/`
- Frontend consome a API via HTTP (sem SSE, WebSocket ou GraphQL no MVP)
- Autenticação via JWT no header `Authorization: Bearer <token>`

### Infraestrutura

| Componente | Tecnologia | Plataforma |
|---|---|---|
| Backend | Java 25 + Spring Boot | Railway |
| Frontend | React + Vite + TypeScript | Vercel |
| Banco de dados | PostgreSQL | Railway (managed) |
| Storage de imagens | Supabase Storage | Supabase |

---

## Alternativas Consideradas

### A1 — Microsserviços desde o início
Dividir o sistema em serviços independentes (auth-service, catalog-service, estimate-service, etc.).

**Rejeitado:** Complexidade operacional desproporcional ao tamanho da equipe e da carga esperada. Overhead de deploy, service discovery, comunicação inter-serviço e observabilidade inviabiliza o MVP. Viola YAGNI e KISS no contexto atual.

### A2 — Monólito sem separação modular
Backend como um único pacote sem fronteiras explícitas entre domínios.

**Rejeitado:** Leva ao crescimento desorganizado e dificulta a evolução. A disciplina modular tem custo baixo e retorno alto em projetos que crescem além do MVP.

### A3 — Backend-for-Frontend (BFF) com GraphQL
Expor uma camada GraphQL adaptada ao frontend.

**Rejeitado:** Complexidade adicional sem benefício claro para o MVP. O número de endpoints REST é gerenciável. GraphQL pode ser avaliado em fase posterior se a variabilidade de queries justificar.

### A4 — Full-stack com Next.js (SSR + API Routes)
Unificar frontend e backend em uma única aplicação Next.js.

**Rejeitado:** Conflita com a decisão de usar Java/Spring Boot no backend, que oferece maior controle sobre segurança, tipagem de domínio e ecossistema Java.

---

## Consequências

**Positivas:**
- Deploy simplificado: um único artefato JAR no Railway.
- Debugging e rastreabilidade triviais em ambiente local.
- Módulos coesos facilitam a eventual extração como microsserviço se a escala exigir.
- Sem overhead de comunicação inter-serviço no MVP.
- Frontend desacoplado pode ser desenvolvido e deployado independentemente.

**Negativas / Riscos:**
- Exige disciplina de equipe para manter as fronteiras modulares — sem o framework enforçando, acoplamento indevido pode surgir.
- Escala vertical tem limite; se o volume crescer abruptamente, a extração de módulos será necessária antes do planejado.
- O deploy no Railway para Java requer configuração de Dockerfile adequada para build e runtime.

---

## Regras de Ouro (non-negotiable)

1. Nenhum módulo acessa diretamente o repositório de outro módulo.
2. Toda entidade de domínio carrega `company_id` e nunca é retornada sem filtragem por ele.
3. Infraestrutura (banco, storage, email) é acessada exclusivamente por interfaces — a implementação é injetada via Spring.
4. Nenhum dado específico do cliente beta (nome, endereço, referência) consta no código ou nas migrations.

---

## Referências

- [ADR-000 — Visão e Escopo](ADR-000-vision-and-scope.md)
- [Diagrama de Módulos](../architecture/module-diagram.md)
- [Modelo de Domínio](../architecture/domain-model.md)
