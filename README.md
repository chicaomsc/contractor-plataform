# Contractor Platform

Monorepo do Contractor Platform, com backend Spring Boot e frontend Next.js.

## Backend

```bash
cd backend
./mvnw test
./mvnw spring-boot:run
```

## Frontend

```bash
cd frontend
npm install
npm run dev
npm run lint
npm run typecheck
npm run test
npm run build
npm run test:e2e
```

O frontend consome exclusivamente a API REST do Spring Boot. Nenhuma regra de negócio, persistência ou autenticação paralela deve ser implementada no Next.js.

## Hardening MVP

- Uploads aceitam apenas PNG, JPEG e WebP, com limite de 5 MB e validação no backend.
- E2E usa Playwright e sobe PostgreSQL/backend/frontend locais quando necessário.
- CI frontend executa lint, typecheck, unit tests, build e smoke E2E.
- CI backend executa testes e package build.
- Documentos de segurança ficam em `docs/security/`.
