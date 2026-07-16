# Dependency Audit

**Sprint:** 9 — MVP Hardening  
**Data:** 2026-07-16

## Frontend

Comandos:

```bash
npm audit --json
npm outdated --json
```

Achados principais:

| Dependência | Achado | Classificação |
|---|---|---|
| `vitest` / `vite` / `vite-node` | 1 crítico, 1 high e moderados em tooling de teste/dev server | Risco aceito temporariamente; correção exige major para Vitest 4 |
| `next` / `postcss` vendorizado | Advisory moderado via PostCSS interno do Next | Risco aceito; corrigir com upgrade planejado do Next, não downgrade |
| `postcss` direto | Patch disponível `8.5.18 -> 8.5.19` | Baixo risco, mas não resolve advisory vendorizado do Next |
| `autoprefixer` | Patch disponível `10.5.2 -> 10.5.4` | Pode ser atualizado em manutenção técnica |
| Next 16, Tailwind 4, Zod 4, TypeScript 7 | Majors disponíveis | Exigem mudança maior |

Nenhum `npm audit fix --force` foi executado.

## Backend

Comandos:

```bash
./mvnw versions:display-dependency-updates
./mvnw versions:display-plugin-updates
```

Achados principais:

| Dependência | Achado | Classificação |
|---|---|---|
| Spring Boot `3.5.0 -> 4.1.0` | Major disponível | Exige mudança maior |
| Spring Security `6.5.0 -> 7.1.0` | Major disponível via Boot 4 | Exige mudança maior |
| PostgreSQL driver `42.7.5 -> 42.7.13` | Patch disponível via BOM/override | Aceitar temporariamente; atualizar junto ao BOM |
| Testcontainers `1.21.0 -> 1.21.4` | Patch disponível via BOM/override | Aceitar temporariamente |
| Lombok `1.18.38 -> 1.18.46` | Patch disponível via BOM/override | Aceitar temporariamente |
| JJWT `0.12.6 -> 0.13.0` | Minor disponível | Adiado por tocar autenticação/token |
| Maven Enforcer | Ausente versão mínima de Maven | Risco baixo; recomendado para próxima sprint infra |

Plugins Maven com versão especificada estão atualizados.
