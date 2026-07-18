# Contractor Platform Frontend

## Requisitos

- Node.js 22+
- npm
- Backend Spring Boot acessível pela URL configurada

## Instalação

```bash
npm install
```

## Variáveis

Crie `.env.local` a partir de `.env.example`.

```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_COMPANY_SLUG=jr-pinturas
NEXT_PUBLIC_SITE_URL=http://localhost:3000
```

`NEXT_PUBLIC_COMPANY_SLUG` define o tenant inicial. No futuro, o slug poderá ser resolvido por domínio/host, mas essa lógica ainda não faz parte do frontend.

## Execução

Backend:

```bash
cd ../backend
./mvnw spring-boot:run
```

Frontend:

```bash
npm run dev
```

Se a API estiver indisponível, o frontend mostra erro bloqueante para o site público. Se apenas serviços ou galeria falharem, o header/footer, hero e dados principais continuam renderizados com aviso parcial.

A rota `/` renderiza a landing pública final:

- header e footer definitivos;
- hero orientado por dados públicos;
- barra de confiança sem métricas inventadas;
- serviços em apresentação editorial;
- galeria antes/depois com comparador acessível;
- processo de trabalho genérico;
- sobre e área geográfica apenas quando houver dados públicos;
- contacto por WhatsApp, telefone ou website público;
- CTA fixo de WhatsApp no mobile quando houver número válido.

A rota `/login` inicia sessão contra a autenticação existente do backend. A área `/dashboard` é protegida e renderiza a fundação administrativa:

- layout com sidebar, header, breadcrumb, user menu e logout;
- dashboard home com empresa, status, branding, contagem de serviços e contagem de imagens;
- edição de Company via `GET/PUT /company/me`;
- edição de Branding via `GET/PUT /branding/me`;
- edição de Settings via `GET/PUT /settings/me`.
- gestão de Services via endpoints administrativos existentes.
- gestão de Gallery via endpoints administrativos existentes.
- criação e edição de orçamentos (Estimate Builder) via endpoints administrativos existentes de `customer` e `estimate`.

Alterações em Services e Gallery refletem na landing pública pelos endpoints públicos existentes do backend.

## Build

```bash
npm run build
npm run start
```

## Testes

```bash
npm run lint
npm run typecheck
npm run test
npm run test:e2e:smoke
npm run test:e2e
```

`npm run test:e2e:smoke` roda os smokes críticos e é o alvo do CI. `npm run test:e2e` roda também o fluxo principal completo do MVP. A suíte E2E usa Playwright, Docker Compose para PostgreSQL e o backend Spring Boot local.

## Estrutura

- `src/app`: App Router, layouts, estados globais, robots e sitemap
- `src/components`: layout, navegação, feedback e UI base
- `src/features`: regras de apresentação por feature
- `src/lib/api`: cliente REST tipado para Spring Boot
- `src/lib/env`: validação de variáveis públicas
- `src/providers`: providers client-side mínimos
- `src/styles`: tokens visuais
- `src/types`: contratos TypeScript da API pública

## Dashboard administrativo

Fluxo autenticado:

```text
/login -> POST /auth/login -> sessão local -> /dashboard -> GET /auth/me
```

Rotas disponíveis:

- `/dashboard`
- `/dashboard/company`
- `/dashboard/branding`
- `/dashboard/settings`
- `/dashboard/services`
- `/dashboard/gallery`
- `/dashboard/estimates`
- `/dashboard/estimates/new`
- `/dashboard/estimates/[id]`

O dashboard consome apenas endpoints existentes do Spring Boot. Não há Route Handlers, Server Actions, mocks permanentes ou backend paralelo no Next.js.

Contratos administrativos ficam em `src/features/dashboard/types`, chamadas HTTP em `src/features/dashboard/api`, hooks TanStack Query em `src/features/dashboard/hooks`, e componentes em `src/features/dashboard/components`.

Gestão de serviços:

- `GET /services` para listagem;
- `POST /services` para criação;
- `PUT /services/{id}` para edição e ativação/desativação;
- `DELETE /services/{id}` para exclusão;
- `PATCH /services/{id}/reorder` para ordenação.

A ordenação no frontend usa mover para cima e mover para baixo. Drag and drop não foi adicionado porque não existe infraestrutura dessa interação no projeto.

Gestão de galeria:

- `GET /gallery` para listagem;
- `POST /gallery` para criação de item;
- `PUT /gallery/{id}` para edição e ativação/desativação;
- `DELETE /gallery/{id}` para exclusão;
- `PATCH /gallery/{id}/feature` para destaque;
- `PATCH /gallery/{id}/reorder` para ordenação;
- `POST /gallery/{id}/before-image` para upload before;
- `POST /gallery/{id}/after-image` para upload after;
- `DELETE /gallery/{id}/before-image` para remover before;
- `DELETE /gallery/{id}/after-image` para remover after.

O par before/after usa o modelo existente do backend: um item de galeria com dois slots de imagem. Não há crop, editor de imagem, compressão ou filtros no frontend.

Uploads administrativos aceitam somente PNG, JPEG e WebP até 5 MB. A validação do frontend é apenas UX; o backend continua sendo a autoridade da política.

Estimate Builder (`/dashboard/estimates`, `/dashboard/estimates/new`, `/dashboard/estimates/[id]`):

- `GET /customers` e `POST /customers` para seleção/criação rápida de cliente no wizard;
- `GET /estimates?status=&customerId=` para a listagem (filtros aplicados no backend; pesquisa por texto é apenas client-side sobre o resultado já filtrado — não existe endpoint de busca);
- `GET /estimates/{id}` para o detalhe (itens e materiais completos);
- `POST /estimates` para criação — o wizard nunca envia `number`, `total`, `subtotal`, `vatAmount` ou qualquer campo calculado;
- `PUT /estimates/{id}` para edição — só disponível enquanto `status = DRAFT`; itens/materiais são substituídos por completo a cada guardar;
- `DELETE /estimates/{id}` — só disponível em `DRAFT`;
- `PATCH /estimates/{id}/status` — a UI oferece todas as transições possíveis e delega inteiramente ao backend a validação (`EstimateStatusTransitionService`); uma transição inválida resulta em 409, mostrado ao utilizador sem que o frontend tente adivinhar a máquina de estados;
- `GET /estimates/{id}/pdf` — botão "Baixar PDF" em `/dashboard/estimates/[id]`. Download autenticado via `adminApiRequestBlob` (variante de `adminApiRequest` que devolve `Blob` + filename do `Content-Disposition`, em vez de JSON) — nunca via `<a href>` simples, que perderia o header `Authorization`. O ficheiro é salvo via `URL.createObjectURL` + `<a download>` sintético + `URL.revokeObjectURL`; o PDF nunca é renderizado nem pré-visualizado no browser, e nenhuma biblioteca de PDF foi adicionada ao frontend.

Todo valor financeiro exibido (mão de obra, materiais, subtotal, IVA, total, entrada, saldo, e o total de cada item/material) vem exclusivamente do `EstimateResponse`/`EstimateSummaryResponse` devolvido pela API. O frontend nunca soma, multiplica ou arredonda um valor monetário — inclusive no passo de revisão do wizard, que lista itens/materiais sem nenhum total calculado, já que esse número só existe após o backend criar o orçamento. O PDF gerado pelo backend segue a mesma regra: nenhum valor é recalculado, nem no backend (renderer) nem no frontend (botão de download).

Reordenação de itens/materiais no editor usa mover para cima/para baixo, seguindo o mesmo padrão de Services/Gallery (drag and drop não implementado por falta de infraestrutura no projeto).

## Sessão e segurança

O dashboard persiste tokens no browser conforme o contrato atual do backend. O risco de refresh token em `localStorage` está documentado em `docs/security/authentication-review.md`; a migração recomendada é refresh token em cookie HttpOnly emitido pelo Spring Boot.

Security headers são definidos em `next.config.ts` para a camada Next.js. Headers equivalentes da API são configurados no Spring Security.

O build não depende de `next/font/google`; a tipografia usa system fonts nos tokens CSS.

## Integração pública

`NEXT_PUBLIC_COMPANY_SLUG` define o tenant carregado em `/`. Os três endpoints consumidos são:

- `GET /public/sites/{companySlug}`
- `GET /public/sites/{companySlug}/services`
- `GET /public/sites/{companySlug}/gallery`

Fluxo de dados:

```text
API DTO -> Mapper -> ViewModel -> Component
```

Componentes não consomem DTOs brutos. Hooks TanStack Query ficam em `src/features/public-site/hooks`, query keys em `src/features/public-site/api/query-keys.ts`, e mappers em `src/features/public-site/mappers`.

A transformação para apresentação fica em `src/features/public-site/mappers` e `src/features/public-site/utils`. Componentes recebem ViewModels já normalizados e não duplicam validações de domínio do backend.

## Landing pública

Componentes principais:

- `HeroSection`
- `TrustStrip`
- `ServiceSection`
- `GallerySection`
- `ProcessSection`
- `AboutSection`
- `ServiceAreaSection`
- `ContactSection`
- `MobileWhatsAppAction`

O preview técnico da Sprint 7B.5 permanece isolado no código de testes, mas não é usado pela rota pública.

O formulário de contacto não foi implementado porque não existe endpoint backend público para envio.

## Fronteira Next.js e Spring Boot

Next.js é apenas camada de apresentação, renderização e integração HTTP. Toda operação de negócio deve consumir a API REST do Spring Boot.

Não implementar no frontend:

- regras de negócio;
- acesso direto ao PostgreSQL;
- persistência;
- autenticação paralela ao Spring Security;
- Route Handlers como backend-sombra;
- Server Actions para substituir endpoints Spring Boot.
