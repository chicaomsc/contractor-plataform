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

`NEXT_PUBLIC_COMPANY_SLUG` define o tenant inicial. No futuro, o slug poderá ser resolvido por domínio/host, mas essa lógica não faz parte da Sprint 7B.

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

Se a API estiver indisponível, o frontend mostra erro bloqueante para o site público. Se apenas serviços ou galeria falharem, o header/footer e os dados principais continuam renderizados com aviso parcial.

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

## Fronteira Next.js e Spring Boot

Next.js é apenas camada de apresentação, renderização e integração HTTP. Toda operação de negócio deve consumir a API REST do Spring Boot.

Não implementar no frontend:

- regras de negócio;
- acesso direto ao PostgreSQL;
- persistência;
- autenticação paralela ao Spring Security;
- Route Handlers como backend-sombra;
- Server Actions para substituir endpoints Spring Boot.
