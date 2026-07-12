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
```

O frontend consome exclusivamente a API REST do Spring Boot. Nenhuma regra de negócio, persistência ou autenticação paralela deve ser implementada no Next.js.
