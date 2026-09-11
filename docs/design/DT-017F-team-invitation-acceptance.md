# DT-017F — Team Invitation Acceptance

## Public Route

DT-017F adds `/invite/team` as the public route for accepting tenant team
invitations. It is separate from the existing owner-invite route `/invite` and
does not require an authenticated session.

## Hash Token Handling

The invitation token is accepted only from the URL fragment:

- expected link shape: `/invite/team#token=...`
- query-string token handling is not supported as the primary flow
- the fragment is read once on mount
- `history.replaceState` immediately removes the fragment from the visible URL
- the raw token is kept only in component memory until submit

The token is never written to localStorage, sessionStorage, cookies, IndexedDB,
DOM text, logs, or analytics by the frontend.

## Request Contract

The frontend calls:

`POST /auth/team-invitations/accept`

with only:

- `token`
- `name`
- `password`

The frontend never sends email, role, companyId or password confirmation to this
endpoint. Those values are either stored in the backend invitation or are local
form-only state.

## AuthResponse Integration

The endpoint returns the existing `AuthResponse`. The frontend validates it with
the existing auth schemas, persists it with `persistAuthSession`, and then
navigates to `/dashboard`. There is no special invited-user session.

MANAGER and MEMBER sessions are handled by the DT-017D permission foundation and
the existing dashboard guards.

## Public Error Collapsing

The backend intentionally collapses invalid, expired, used, revoked, inactive
company and email-claimed cases into a generic public failure. The UI preserves
that behavior and displays:

`Este convite é inválido ou não está mais disponível.`

It does not reveal whether the email exists, which company the invitation
belonged to, the role, status, expiry reason, or whether the token was reused.

## One-Time Semantics

The backend remains the authority for one-time token consumption. After a
successful acceptance, the frontend clears its in-memory token and relies on the
backend to reject future attempts with the same token.

## E2E Strategy

Production never returns the raw token from team-invitation creation, and the
database persists only the token hash. The current E2E harness also disables
email delivery and does not expose a safe captured-email mailbox.

For that reason, DT-017F does not add a debug token, does not alter backend DTOs,
and does not make production responses expose raw tokens. End-to-end browser
coverage for `/invite/team` uses Playwright network mocks for the public accept
endpoint, while the existing DT-017E E2E continues to exercise real OWNER-side
invitation creation.

Backend integration tests already validate real token capture via mocked
`EmailService`, one-time consumption, revoked/expired/used behavior, and
MANAGER/MEMBER creation against the real database.

## Security Constraints

- No `?token=` flow.
- No token persistence.
- No raw token in production DTOs.
- No Resend dependency in automated tests.
- No owner invitation semantics merged into team invitations.
- No ADMIN role introduced.
