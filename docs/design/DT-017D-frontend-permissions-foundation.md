# DT-017D — Frontend Roles & Permissions Foundation

## Supported roles

Technical roles accepted by the frontend:

- `SUPER_ADMIN`
- `OWNER`
- `MANAGER`
- `MEMBER`

There is intentionally no `ADMIN` technical role.

## Role labels

- `SUPER_ADMIN`: Administrador da plataforma
- `OWNER`: Proprietário
- `MANAGER`: Administrador
- `MEMBER`: Colaborador

Labels are centralized in `src/features/auth/permissions.ts`.

## Tenant vs platform administration

`SUPER_ADMIN` belongs to platform administration and is not treated as a tenant user with universal access. Tenant dashboard access is reserved for `OWNER`, `MANAGER`, and `MEMBER` users with a company.

## Permission matrix

| Area               | OWNER | MANAGER | MEMBER | SUPER_ADMIN |
| ------------------ | ----- | ------- | ------ | ----------- |
| Dashboard/API base | yes   | yes     | yes    | no          |
| Clientes           | yes   | yes     | yes    | no          |
| Orçamentos         | yes   | yes     | yes    | no          |
| Serviços           | yes   | yes     | no     | no          |
| Galeria            | yes   | yes     | no     | no          |
| Branding           | yes   | yes     | no     | no          |
| Empresa leitura    | yes   | yes     | yes    | no          |
| Empresa alteração  | yes   | no      | no     | no          |
| Settings           | yes   | no      | no     | no          |
| Equipe             | yes   | no      | no     | no          |
| Platform admin     | no    | no      | no     | yes         |

## Navigation filtering

Dashboard navigation uses permission helpers from `src/features/auth/permissions.ts`. Desktop and mobile sidebars share the same filtered item list.

`Equipe` is intentionally not added in DT-017D. `canManageTeam()` exists for DT-017E/F.

## Route protection

Dashboard pages use a small `PermissionGuard` wrapper. Hidden navigation is not the only protection; direct URL access returns a consistent access denied state.

## Company read-only behavior

`OWNER` can edit Company data. `MANAGER` and `MEMBER` can view the same data but inputs are disabled and the save action is not rendered.

## DashboardHome conditional queries

DashboardHome only enables queries needed by the current role. Limited roles avoid unnecessary calls that the backend would reject with 403.

## 401 vs 403 semantics

401 remains part of the authentication/session lifecycle and may trigger silent refresh. 403 means authenticated but not authorized and must not trigger refresh or the session-expired modal.

## RBAC scope

This is not a generic RBAC engine. The frontend uses explicit helpers such as `canManageBranding()`, `canManageSettings()`, and `canManageTeam()`.

## Deferred

Deferred to DT-017E/F:

- Team page UI
- Invite dialog
- Member table
- Pending invitations UI
- Role-change UI
- Remove-member UI
- Accept team invitation page
- Complete team management E2E
