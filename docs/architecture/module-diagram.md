# Diagrama de Módulos

**Versão:** 1.0 — Sprint 1  
**Data:** 2026-07-04  
**Status:** Rascunho aprovado

---

## Visão Geral da Arquitetura

O sistema é composto por três camadas de implantação independentes que se comunicam via HTTP/REST:

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENTE (Browser)                            │
│                                                                     │
│  ┌──────────────────────────┐    ┌──────────────────────────────┐   │
│  │     Landing Pública      │    │    Painel Administrativo     │   │
│  │  (React + Vite + TS)     │    │    (React + Vite + TS)       │   │
│  └──────────┬───────────────┘    └──────────────┬───────────────┘   │
│             │ HTTP REST                         │ HTTP REST + JWT   │
└─────────────┼──────────────────────────────────-┼───────────────────┘
              │                                   │
              ▼                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    BACKEND  (Railway)                               │
│              Spring Boot — Monólito Modular                         │
│                                                                     │
│  ┌──────────────┐  ┌──────────────────────────────────────────┐     │
│  │   public     │  │              api (v1)                    │     │
│  │  (sem auth)  │  │          (requer JWT)                    │     │
│  └──────┬───────┘  └────┬───────────┬──────────────┬──────────┘     │
│         │               │           │              │                │
│         ▼               ▼           ▼              ▼                │
│  ┌──────────┐     ┌──────────┐  ┌─────────┐  ┌────────────┐         │
│  │ company  │     │   auth   │  │ catalog │  │  customer  │         │
│  │(Company  │     │ (JWT,    │  │(Service,│  │ (Customer) │         │
│  │ Branding │     │  Spring  │  │ Gallery)│  │            │         │
│  │ Settings)│     │ Security)│  └────┬────┘  └─────┬──────┘         │
│  └────┬─────┘     └─────┬────┘       │             │                │
│       │                 │            │             │                │
│       └─────────────────┴────────────┴──────┬──────┘                │
│                                             │                       │
│                                    ┌────────┴────────┐              │
│                                    │    estimate     │              │
│                                    │ (Estimate, Item │              │
│                                    │  Material, PDF) │              │
│                                    └─────────────────┘              │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                        shared                               │   │
│   │        (exceções, DTOs comuns, utilitários, auditoria)      │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
              ┌────────────────┴────────────────┐
              │                                 │
              ▼                                 ▼
┌─────────────────────────┐       ┌─────────────────────────────┐
│   PostgreSQL (Railway)  │       │   Supabase Storage          │
│   Base de dados         │       │   (imagens: logo, galeria)  │
└─────────────────────────┘       └─────────────────────────────┘
```

---

## Módulos do Backend

### `io.chicaodw.platform.shared`

Módulo transversal. Não depende de nenhum outro módulo do domínio.

**Responsabilidades:**
- Exceções de domínio base (`ResourceNotFoundException`, `BusinessRuleException`, etc.)
- DTOs de resposta padrão (`ApiResponse<T>`, `PagedResponse<T>`)
- Utilitários de paginação, formatação e validação
- Anotações e interfaces de auditoria (`Auditable`)
- Constantes globais (moedas suportadas, códigos de status)

**Regra:** Nenhum módulo de domínio pode ser importado por `shared`.

---

### `io.chicaodw.platform.auth`

Módulo de autenticação e segurança.

**Responsabilidades:**
- Configuração do Spring Security
- Geração, validação e renovação de tokens JWT
- Endpoints: `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`
- Filtro de autenticação HTTP (`JwtAuthenticationFilter`)
- Resolução de `company_id` a partir do token JWT

**Dependências:** `shared`  
**Expõe para outros módulos:** `AuthenticatedUser` (contexto de segurança do utilizador corrente)

---

### `io.chicaodw.platform.company`

Módulo central de configuração da empresa.

**Responsabilidades:**
- CRUD de `Company`, `Branding` e `Settings`
- Validação de `slug` único
- Endpoints administrativos: `/api/v1/companies/{id}/...`
- Upload de logo (delega ao módulo de storage)

**Dependências:** `shared`, `auth`

---

### `io.chicaodw.platform.catalog`

Módulo de catálogo público da empresa.

**Responsabilidades:**
- CRUD de `Service` e `GalleryItem`
- Gestão de `displayOrder`
- Upload de imagens de serviço e galeria

**Dependências:** `shared`, `auth`

---

### `io.chicaodw.platform.customer`

Módulo de gestão de clientes.

**Responsabilidades:**
- CRUD de `Customer`
- Pesquisa e listagem de clientes da empresa

**Dependências:** `shared`, `auth`

---

### `io.chicaodw.platform.estimate`

Módulo de orçamentação — o de maior complexidade de negócio no MVP.

**Responsabilidades:**
- CRUD completo de `Estimate`, `EstimateItem` e `Material`
- Geração de `referenceNumber` sequencial por empresa
- Cálculo de totais (`subtotal`, `taxAmount`, `total`)
- Transições de `status` com validação de invariantes
- Geração de PDF do orçamento
- Endpoints: `/api/v1/estimates/...`

**Dependências:** `shared`, `auth`, `customer` (somente para leitura do Customer)  
**Nota:** O módulo `estimate` não acessa o repositório de `Customer` diretamente. Resolve o cliente via ID e delega a validação à interface exposta pelo módulo `customer`.

---

### `io.chicaodw.platform.public` (ou `landing`)

Módulo de API pública sem autenticação, consumido pela landing page.

**Responsabilidades:**
- Endpoint de perfil público da empresa: `GET /public/{slug}`
- Endpoint de serviços públicos: `GET /public/{slug}/services`
- Endpoint de galeria pública: `GET /public/{slug}/gallery`

**Dependências:** `shared`, `company`, `catalog`  
**Regra:** Nunca expõe dados sensíveis (email de clientes, valores de orçamentos, notas internas).

---

## Módulos do Frontend

```
frontend/
├── src/
│   ├── landing/          # Landing page pública (rotas sem auth)
│   │   ├── pages/
│   │   └── components/
│   ├── admin/            # Painel administrativo (rotas protegidas)
│   │   ├── pages/
│   │   └── components/
│   ├── api/              # Clientes HTTP (axios/fetch wrappers por módulo)
│   ├── auth/             # Contexto de autenticação, guards de rota
│   ├── shared/           # Componentes, hooks e utilitários reutilizáveis
│   └── config/           # Variáveis de ambiente, constantes
```

**Regras:**
- `landing/` e `admin/` são independentes entre si — sem importações cruzadas.
- `shared/` é acessível por ambos.
- `api/` é organizado por módulo do backend (ex: `api/estimates.ts`, `api/catalog.ts`).

---

## Fluxo de Autenticação

```
Frontend (Admin)
     │
     │  POST /api/v1/auth/login  {email, password}
     ▼
  auth module
     │  valida credenciais → gera JWT com { sub, company_id, role }
     ▼
  responde com { accessToken, refreshToken }
     │
     ◄──────────────────────────────────────────
     │
     │  requisições subsequentes:
     │  Authorization: Bearer <accessToken>
     ▼
  JwtAuthenticationFilter
     │  valida token → popula SecurityContext com AuthenticatedUser
     ▼
  qualquer controller → obtém company_id do contexto → filtra dados
```

---

## Fluxo de Geração de PDF

```
Frontend (Admin)
     │
     │  POST /api/v1/estimates/{id}/pdf
     ▼
  estimate module
     │  carrega Estimate + EstimateItems + Materials + Company + Branding
     │  monta template (HTML ou LaTeX/iText)
     │  calcula totais
     ▼
  retorna PDF como application/pdf
     │
     ◄──────────────────────────────────────────
     │  Frontend faz download do arquivo
```

---

## Fluxo de Upload de Imagem

```
Frontend (Admin)
     │
     │  POST /api/v1/catalog/gallery  multipart/form-data
     ▼
  catalog module
     │  valida tipo e tamanho do arquivo
     │  envia para Supabase Storage via SDK
     │  recebe URL pública
     │  persiste GalleryItem com imageUrl
     ▼
  responde com GalleryItem criado
```

---

## Dependências entre Módulos (Grafo)

```
                     shared
                    / |  | \
                   /  |  |  \
                  /   |  |   \
               auth  company catalog customer
                 \      |      /      |
                  \     |     /       |
                   \    |    /        |
                    estimate ─────────┘
                        |
                     public
```

**Invariante:** O grafo de dependências é um DAG (sem ciclos). Qualquer ciclo introduzido é tratado como defeito arquitetural.

---

## Convenções de Pacotes

Cada módulo segue a estrutura interna:

```
io.chicaodw.platform.<módulo>/
├── api/          # Controllers REST (@RestController)
├── application/  # Serviços de aplicação (@Service), casos de uso
├── domain/       # Entidades, value objects, interfaces de repositório
├── infrastructure/
│   ├── persistence/   # Implementações JPA dos repositórios
│   └── storage/       # Integração com Supabase Storage (se aplicável)
└── dto/          # Request/Response DTOs (package-public)
```

**Visibilidade:**
- `domain/` e `infrastructure/` são **package-private** para o módulo.
- `dto/` e interfaces de `application/` podem ser referenciados por outros módulos se necessário.
- Controllers em `api/` são públicos apenas para o framework (Spring).

---

## Referências

- [ADR-001 — Estilo de Arquitetura](../adr/ADR-001-architecture-style.md)
- [Modelo de Domínio](domain-model.md)
- [Roadmap](../roadmap.md)
