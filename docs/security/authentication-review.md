# Authentication Review

**Sprint:** 9 — MVP Hardening  
**Data:** 2026-07-16

## Estratégia Atual

- O backend Spring Boot emite access token JWT e refresh token opaco via `POST /auth/login`, `POST /auth/register` e `POST /auth/refresh`.
- O frontend persiste `accessToken`, `refreshToken`, `user` e `company` em `localStorage`.
- O frontend cria apenas um cookie `contractor_session=active` sem token, usado para sinalização local de sessão.
- Logout remove tokens do `localStorage`, remove o cookie de sessão e limpa caches TanStack Query.
- Rotas `/dashboard/*` usam `AuthGuard` e redirecionam para `/login?next=...` sem sessão.

## Achados

| Achado | Severidade | Decisão |
|---|---:|---|
| Refresh token em `localStorage` fica exposto em caso de XSS | Alta | Risco aceito temporariamente |
| Não há rotação automática de access token no frontend | Média | Adiado |
| Refresh endpoint existe, mas múltiplas requisições durante refresh ainda não têm fila única no frontend | Média | Adiado |
| Logout limpa storage local e bloqueia rota administrativa após logout | Baixa | Validado por E2E |
| Tokens não são registrados em logs do frontend | Baixa | Validado por revisão |

## Correção Recomendada Futura

Migrar o refresh token para cookie `HttpOnly`, `Secure`, `SameSite=Lax/Strict` emitido pelo backend, manter access token em memória e implementar refresh único com fila para requisições concorrentes.

Essa alteração exige contrato backend/frontend novo e foi adiada para não reescrever autenticação nesta sprint.

## Testes

- `e2e/smoke.spec.ts`: login inválido, dashboard sem sessão, logout, bloqueio após logout.
- `e2e/mvp-flow.spec.ts`: registro, login e fluxo autenticado completo.
