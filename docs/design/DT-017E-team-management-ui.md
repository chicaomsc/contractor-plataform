# DT-017E — Team Management UI

## Scope

DT-017E adds the authenticated tenant team-management UI at `/dashboard/team`.
It is frontend-only and consumes the backend contracts delivered in DT-017B and
DT-017C. Public team-invitation acceptance remains deferred to DT-017F.

## Route And Authorization

- Route: `/dashboard/team`.
- Access: OWNER only.
- The page uses the DT-017D permission foundation through `canManageTeam`.
- MANAGER and MEMBER do not see the navigation item and receive the shared
  access-denied state on direct URL access.
- SUPER_ADMIN remains platform-only and does not use tenant dashboard routes.

## Navigation

The dashboard sidebar includes `Equipe` only when `canManageTeam(role)` returns
true. Desktop and mobile sidebars share the same nav item configuration.

## Members Section

`GET /team/members` lists every tenant user for the company: OWNER, MANAGER and
MEMBER. The UI displays name, email, friendly role label, status and available
actions.

OWNER rows are visible but cannot be changed or removed. ACTIVE MANAGER/MEMBER
rows can be changed between MANAGER and MEMBER or removed from the team.

## Role Change

Role changes call `PATCH /team/members/{userId}/role` with only:

- `MANAGER`
- `MEMBER`

The UI confirms the action before the request and explains that current sessions
for that collaborator will end, without exposing auth internals.

## Removal

Removal calls `DELETE /team/members/{userId}`. Backend performs a soft-disable.
The UI supports INACTIVE members remaining visible and does not expose additional
mutable actions for them in this phase.

## Pending Invitations

`GET /team/invitations` returns safe invitation projections only: id, email,
role, status, expiresAt and createdAt. No token is returned to the OWNER-facing
frontend.

The UI shows friendly role/status labels, created/expiry dates and actions for
PENDING invitations.

## Invite Flow

The invite dialog posts to `POST /team/invitations` with:

- `email`
- `role: MANAGER | MEMBER`

The default role is MEMBER. OWNER and SUPER_ADMIN are not offered. On success,
the dialog closes, the form state is discarded and invitations are invalidated.

The success copy says the invite was created and email processing will happen,
without guaranteeing inbox delivery.

## Invitation Actions

- Resend: `POST /team/invitations/{id}/resend`, no confirmation, invalidates the
  previous token server-side and refreshes invitations.
- Revoke: `DELETE /team/invitations/{id}`, confirmation required.
- USED, REVOKED and EXPIRED invitations are displayed without actions in this
  phase.

## 401 And 403

No session lifecycle semantics changed. The existing admin HTTP client handles
401 through silent refresh/session expiration. 403 remains a permission/API
failure and does not trigger the session-expired modal.

## Deferred

DT-017F will implement `/invite/team#token=...`, fragment-token handling, public
acceptance form and login/session persistence for invited collaborators.
