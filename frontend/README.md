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

Gallery é consumida no dashboard apenas para contagem. A gestão de galeria ainda não faz parte do frontend.

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
```

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

O dashboard consome apenas endpoints existentes do Spring Boot. Não há Route Handlers, Server Actions, mocks permanentes ou backend paralelo no Next.js.

Contratos administrativos ficam em `src/features/dashboard/types`, chamadas HTTP em `src/features/dashboard/api`, hooks TanStack Query em `src/features/dashboard/hooks`, e componentes em `src/features/dashboard/components`.

Gestão de serviços:

- `GET /services` para listagem;
- `POST /services` para criação;
- `PUT /services/{id}` para edição e ativação/desativação;
- `DELETE /services/{id}` para exclusão;
- `PATCH /services/{id}/reorder` para ordenação.

A ordenação no frontend usa mover para cima e mover para baixo. Drag and drop não foi adicionado porque não existe infraestrutura dessa interação no projeto.

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
