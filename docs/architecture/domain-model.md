# Modelo de Domínio

**Versão:** 1.0 — Sprint 1  
**Data:** 2026-07-04  
**Status:** Rascunho aprovado

---

## Visão Geral

O domínio modela a operação de um **prestador de serviço** que necessita de presença digital e controle comercial básico. O isolamento entre prestadores é garantido pelo campo `company_id` presente em todas as entidades de domínio.

Todas as entidades aqui descritas são agnósticas ao segmento de atuação do prestador (pintura, elétrica, construção, etc.).

---

## Agregados e Entidades

### Agregado: Company

Raiz de agregado central. Representa o prestador de serviço como entidade organizacional.

```
Company
├── id: UUID
├── name: String                  -- nome legal ou comercial
├── slug: String (único)          -- identificador de URL (ex: "jr-pinturas")
├── email: String
├── phone: String
├── address: Address (value object)
├── country: String               -- código ISO 3166-1 alpha-2
├── active: Boolean
├── createdAt: Instant
└── updatedAt: Instant
```

**Entidade associada: Branding**  
Configurações de identidade visual da empresa. Pertence exclusivamente a uma Company.

```
Branding
├── id: UUID
├── companyId: UUID               -- FK → Company
├── logoUrl: String               -- URL no Supabase Storage
├── primaryColor: String          -- hex color (#RRGGBB)
├── secondaryColor: String
├── tagline: String               -- slogan de apresentação
├── aboutText: String             -- texto "sobre nós"
├── createdAt: Instant
└── updatedAt: Instant
```

**Entidade associada: Settings**  
Configurações operacionais da empresa (comportamento do sistema).

```
Settings
├── id: UUID
├── companyId: UUID               -- FK → Company
├── defaultCurrency: String       -- ISO 4217 (ex: "EUR", "BRL")
├── defaultTaxRate: BigDecimal    -- percentual padrão de imposto
├── estimateValidityDays: Integer -- validade padrão de orçamentos (dias)
├── estimateFooterText: String    -- texto exibido no rodapé do PDF
├── createdAt: Instant
└── updatedAt: Instant
```

---

### Agregado: User

Representa um utilizador autenticado com acesso ao painel administrativo. No MVP, cada empresa possui pelo menos um utilizador administrador.

```
User
├── id: UUID
├── companyId: UUID               -- FK → Company
├── email: String (único global)
├── passwordHash: String
├── name: String
├── role: Enum(ADMIN, OPERATOR)
├── active: Boolean
├── createdAt: Instant
└── updatedAt: Instant
```

> **Nota MVP:** Apenas o role `ADMIN` é utilizado no MVP. `OPERATOR` é reservado para iterações futuras.

---

### Agregado: Catalog (Catálogo de Oferta)

Agrupa os itens que a empresa oferece publicamente.

**Entidade: Service**  
Serviço disponibilizado pela empresa.

```
Service
├── id: UUID
├── companyId: UUID               -- FK → Company
├── name: String
├── description: String
├── imageUrl: String              -- URL no Supabase Storage (opcional)
├── displayOrder: Integer         -- ordem de exibição na landing
├── active: Boolean
├── createdAt: Instant
└── updatedAt: Instant
```

**Entidade: GalleryItem**  
Imagem de trabalho realizado, exibida na galeria da landing page.

```
GalleryItem
├── id: UUID
├── companyId: UUID               -- FK → Company
├── imageUrl: String              -- URL no Supabase Storage
├── caption: String               -- legenda opcional
├── displayOrder: Integer
├── active: Boolean
├── createdAt: Instant
└── updatedAt: Instant
```

---

### Agregado: Customer

Representa um cliente da empresa. Não é um utilizador do sistema.

```
Customer
├── id: UUID
├── companyId: UUID               -- FK → Company
├── name: String                  -- obrigatório
├── email: String                 -- opcional (mas ver invariante de contacto abaixo)
├── phone: String                 -- opcional (mas ver invariante de contacto abaixo)
├── taxNumber: String              -- opcional, não único (nem globalmente, nem por empresa)
├── address: Address (value object)
├── notes: String                 -- anotações internas, opcional
├── active: Boolean                -- default true; soft-delete (ver invariante 8)
├── createdAt: Instant
└── updatedAt: Instant
```

**Decisão (Sprint 10A):** `Customer` é referenciado por `Estimate` apenas por FK (`customerId`), sem snapshot dos seus dados. Alterar o nome/morada de um cliente depois de criado um orçamento reflete-se retroativamente ao consultar esse orçamento — risco aceite conscientemente para o MVP (ver release v1.0.0-estimate-domain-api.md). Caso isto se torne um problema real (ex: divergência visível no PDF gerado na Sprint 11), a mitigação é adicionar um snapshot mínimo (`customerNameSnapshot`, `customerContactSnapshot`) sem remodelar o agregado.

---

### Agregado: Estimate (Orçamento)

**Atualizado na Sprint 10A** — o desenho abaixo substitui a versão original deste documento (Sprint 1), que era apenas indicativa. Ver [Release v1.0.0 — Estimate Domain & API](../releases/v1.0.0-estimate-domain-api.md) para o racional completo.

Raiz do agregado de orçamento. Centraliza a proposta comercial enviada a um cliente. `EstimateItem` e `Material` são entidades filhas sem ciclo de vida próprio (cascade ALL + orphanRemoval — nunca existem sem um `Estimate`).

```
Estimate
├── id: UUID
├── companyId: UUID               -- FK → Company
├── customerId: UUID              -- FK → Customer (sem snapshot, ver nota acima)
├── number: String                 -- número legível, único por empresa (ex: "ORC-2026-0001") — ver ADR-007
├── title: String
├── description: String            -- opcional
├── status: Enum(DRAFT, SENT, APPROVED, REJECTED, EXPIRED, CANCELLED, COMPLETED)
├── issueDate: LocalDate            -- definido pelo backend na criação, nunca editável
├── validUntil: LocalDate           -- opcional; default = issueDate + Settings.estimateValidityDays
├── expectedStartDate: LocalDate    -- opcional
├── estimatedDurationDays: Integer  -- opcional
├── notes: String                  -- opcional, visível ao cliente
├── terms: String                  -- opcional, condições comerciais
│
│   -- Snapshots (nunca relidos de Settings/Branding após a criação)
├── currency: String                -- ISO 4217, copiado de Settings.defaultCurrency
├── vatRate: BigDecimal             -- copiado de Settings.defaultTaxRate, salvo override do cliente
├── upfrontPercentage: BigDecimal   -- copiado de Settings.upfrontPercentage, salvo override do cliente
│
│   -- Totais calculados pelo backend (nunca aceites do cliente)
├── laborSubtotal: BigDecimal
├── materialSubtotal: BigDecimal
├── subtotal: BigDecimal            -- = laborSubtotal + materialSubtotal
├── vatAmount: BigDecimal           -- = subtotal × vatRate / 100
├── total: BigDecimal               -- = subtotal + vatAmount
├── upfrontAmount: BigDecimal       -- = total × upfrontPercentage / 100
├── remainingAmount: BigDecimal     -- = total - upfrontAmount
├── createdAt: Instant
└── updatedAt: Instant
```

Ver [Cálculos Financeiros e Precisão Monetária](../releases/v1.0.0-estimate-domain-api.md#cálculos-financeiros) para as regras de arredondamento (`BigDecimal`, scale 2, `HALF_UP`).

**Entidade: EstimateItem**  
Linha de trabalho/serviço dentro de um orçamento. `description` e `unitPrice` são copiados no momento da criação a partir do `Service` do catálogo (quando `serviceId` é informado) — alterações futuras no catálogo nunca afetam orçamentos já criados.

```
EstimateItem
├── id: UUID
├── estimateId: UUID              -- FK → Estimate
├── serviceId: UUID                -- opcional, referência não-normativa ao catálogo (sem FK de banco)
├── description: String
├── quantity: BigDecimal
├── unit: Enum(UNIT, HOUR, DAY, M2, M3, LINEAR_METER, FIXED)
├── unitPrice: BigDecimal
├── total: BigDecimal              -- = quantity × unitPrice (calculado e persistido)
├── displayOrder: Integer
├── createdAt: Instant
└── updatedAt: Instant
```

**Entidade: Material**  
**Decisão (Sprint 10A):** pertence diretamente ao `Estimate`, não ao `EstimateItem` — corrige o desenho original deste documento (Sprint 1), que ligava `Material` a `EstimateItem`. Não há catálogo global de materiais nesta sprint.

```
Material
├── id: UUID
├── estimateId: UUID              -- FK → Estimate
├── name: String
├── description: String            -- opcional
├── quantity: BigDecimal
├── unit: Enum(UNIT, HOUR, DAY, M2, M3, LINEAR_METER, FIXED)
├── unitPrice: BigDecimal
├── total: BigDecimal              -- = quantity × unitPrice (calculado e persistido)
├── displayOrder: Integer
├── createdAt: Instant
└── updatedAt: Instant
```

---

## Value Objects

### Address

Reutilizado por `Company` e `Customer`.

```
Address
├── street: String
├── city: String
├── postalCode: String
├── region: String
└── country: String               -- código ISO 3166-1 alpha-2
```

---

## Relacionamentos

```
Company (1) ──── (1) Branding
Company (1) ──── (1) Settings
Company (1) ──── (N) User
Company (1) ──── (N) Service
Company (1) ──── (N) GalleryItem
Company (1) ──── (N) Customer
Company (1) ──── (N) Estimate
Estimate  (1) ──── (N) EstimateItem
Estimate  (1) ──── (N) Material
Estimate  (N) ──── (1) Customer
```

---

## Invariantes de Domínio

1. Toda entidade que não seja `Company` possui `companyId` não nulo (em `EstimateItem`/`Material` isto é indireto, via `Estimate.companyId`).
2. `Estimate.status` segue a máquina de estados definida em `EstimateStatusTransitionService` (Sprint 10A): `DRAFT → SENT | CANCELLED`; `SENT → APPROVED | REJECTED | EXPIRED | CANCELLED`; `APPROVED → COMPLETED | CANCELLED`; `REJECTED`, `EXPIRED`, `CANCELLED`, `COMPLETED` são terminais.
3. `EstimateItem.total` deve ser sempre igual a `quantity × unitPrice` no momento da persistência (`RoundingMode.HALF_UP`, scale 2).
4. `Material.total` deve ser sempre igual a `quantity × unitPrice` no momento da persistência (`RoundingMode.HALF_UP`, scale 2).
5. `Company.slug` é imutável após criação.
6. `User.email` é único globalmente (não apenas dentro da company).
7. Um `Estimate` só pode ser deletado se estiver em status `DRAFT`; fora de `DRAFT`, usar a transição para `CANCELLED`.
8. Um `Customer` nunca é apagado fisicamente — `DELETE /customers/{id}` faz soft-delete (`active = false`), pois `Estimate.customerId` referencia o cliente permanentemente para preservar o histórico financeiro.
9. Um `Customer` inativo (`active = false`) não pode ser associado a um novo `Estimate`, nem reatribuído a um `Estimate` existente.
10. Um `Estimate` só pode ser editado (PUT) enquanto `status = DRAFT`. Fora disso, apenas o endpoint de status é permitido.
11. `Customer` exige pelo menos um de `email` ou `phone` preenchido.
12. `Estimate.number` é único por `(companyId, number)` — ver [ADR-007](../adr/ADR-007-estimate-numbering-strategy.md) para a estratégia de geração.

---

## Decisões de Modelagem

- **`Material` pertence diretamente ao `Estimate`, não ao `EstimateItem`** (Sprint 10A — corrige o desenho original da Sprint 1): simplifica o agregado e reflete o uso real — materiais são listados por orçamento, não por linha de serviço.
- **`Branding` e `Settings` como entidades separadas de `Company`:** evita que a entidade raiz se torne um objeto de dados agregado. Cada entidade tem ciclo de vida e responsabilidade independentes.
- **`number` como campo gerenciado pela aplicação:** sequência legível por humanos (ex: `ORC-2026-0042`) gerada pelo backend via contador atômico por `(companyId, year)`, distinta do `UUID` interno — ver [ADR-007](../adr/ADR-007-estimate-numbering-strategy.md).
- **`Customer` referenciado apenas por FK, sem snapshot** (Sprint 10A): risco aceite conscientemente para o MVP — ver nota na secção Customer acima.
- **`EstimateItem.serviceId` sem FK de banco:** referência intencionalmente "solta" ao catálogo — a exclusão ou edição futura de um `Service` nunca deve afetar um orçamento já criado.

---

## Fora do MVP (Evolução Prevista)

- `Testimonial` — depoimentos de clientes para a landing page
- `Payment` — registo de pagamentos associados a orçamentos aceites
- `Invoice` — fatura formal derivada de um orçamento aceite
- `Appointment` — agendamento de visitas ou execução de serviços
- `Tag` — categorização de serviços e clientes
- `AuditLog` — rastreabilidade de alterações em entidades críticas

---

## Referências

- [ADR-000 — Visão e Escopo](../adr/ADR-000-vision-and-scope.md)
- [ADR-001 — Estilo de Arquitetura](../adr/ADR-001-architecture-style.md)
- [Diagrama de Módulos](module-diagram.md)
