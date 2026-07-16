# Tenant Isolation

**Sprint:** 9 — MVP Hardening  
**Data:** 2026-07-16

## Política

Endpoints administrativos devem resolver a empresa pelo usuário autenticado ou pelo recurso já pertencente à empresa autenticada. O frontend não pode enviar `companyId` para substituir o principal autenticado.

## Cobertura Adicionada

`TenantIsolationIntegrationTest` cobre:

- Company: atualização restrita à empresa autenticada.
- Branding: atualização restrita por `companyId` autenticado.
- Settings: atualização restrita por `companyId` autenticado.
- Services: listagem, edição, exclusão e reorder isolados.
- Gallery: listagem, edição, exclusão, featured, reorder e upload isolados.
- Uploads: pasta de armazenamento recebe `companies/{companyId}/gallery`.

## Resultado

Cross-tenant direto por ID de outra empresa retorna erro controlado conforme o padrão atual dos serviços (`404`/recurso não encontrado em nível de aplicação).

Nenhum endpoint administrativo novo foi criado.
