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
├── name: String
├── email: String
├── phone: String
├── address: Address (value object)
├── notes: String                 -- anotações internas
├── createdAt: Instant
└── updatedAt: Instant
```

---

### Agregado: Estimate (Orçamento)

Raiz do agregado de orçamento. Centraliza a proposta comercial enviada a um cliente.

```
Estimate
├── id: UUID
├── companyId: UUID               -- FK → Company
├── customerId: UUID              -- FK → Customer
├── referenceNumber: String       -- número legível (ex: "ORC-2026-0042")
├── title: String                 -- descrição sumária do trabalho
├── status: Enum(DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED)
├── validUntil: LocalDate
├── taxRate: BigDecimal           -- percentual aplicado (pode diferir do Settings)
├── discountAmount: BigDecimal
├── notes: String                 -- observações ao cliente
├── internalNotes: String         -- notas internas (não aparecem no PDF)
├── createdAt: Instant
└── updatedAt: Instant
```

**Valores calculados (não persistidos):**
- `subtotal` = soma de `EstimateItem.totalPrice`
- `taxAmount` = `subtotal × taxRate / 100`
- `total` = `subtotal + taxAmount - discountAmount`

**Entidade: EstimateItem**  
Linha de item dentro de um orçamento.

```
EstimateItem
├── id: UUID
├── companyId: UUID               -- FK → Company
├── estimateId: UUID              -- FK → Estimate
├── description: String
├── quantity: BigDecimal
├── unitPrice: BigDecimal
├── unit: String                  -- ex: "m²", "hora", "unidade"
├── totalPrice: BigDecimal        -- = quantity × unitPrice (calculado e persistido)
├── displayOrder: Integer
└── createdAt: Instant
```

**Entidade: Material**  
Material utilizado em um item de orçamento (custo interno, não necessariamente exibido ao cliente).

```
Material
├── id: UUID
├── companyId: UUID               -- FK → Company
├── estimateItemId: UUID          -- FK → EstimateItem
├── name: String
├── quantity: BigDecimal
├── unit: String
├── unitCost: BigDecimal
├── totalCost: BigDecimal         -- = quantity × unitCost (calculado e persistido)
└── createdAt: Instant
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
EstimateItem (1) ─── (N) Material
Estimate  (N) ──── (1) Customer
```

---

## Invariantes de Domínio

1. Toda entidade que não seja `Company` possui `companyId` não nulo.
2. `Estimate.status` só pode avançar no sentido: `DRAFT → SENT → ACCEPTED | REJECTED`; ou `DRAFT → EXPIRED`.
3. `EstimateItem.totalPrice` deve ser sempre igual a `quantity × unitPrice` no momento da persistência.
4. `Material.totalCost` deve ser sempre igual a `quantity × unitCost` no momento da persistência.
5. `Company.slug` é imutável após criação.
6. `User.email` é único globalmente (não apenas dentro da company).
7. Um `Estimate` só pode ser deletado se estiver em status `DRAFT`.

---

## Decisões de Modelagem

- **`companyId` em `EstimateItem` e `Material`:** redundante relativamente à navegação, mas facilita queries diretas e auditoria sem joins adicionais.
- **`Material` separado de `EstimateItem`:** permite registar custos de insumos sem que sejam necessariamente itemizados para o cliente, preservando margem comercial.
- **`Branding` e `Settings` como entidades separadas de `Company`:** evita que a entidade raiz se torne um objeto de dados agregado. Cada entidade tem ciclo de vida e responsabilidade independentes.
- **`referenceNumber` como campo gerenciado pela aplicação:** sequência legível por humanos (ex: `ORC-2026-0042`) gerada pelo backend, distinta do `UUID` interno.

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
