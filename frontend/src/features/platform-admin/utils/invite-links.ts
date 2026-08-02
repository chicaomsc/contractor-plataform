// Fragment, not query string (SEC-AUTH-07, Sprint 11B.6D) — same reasoning as the
// password-reset link built by the backend's PasswordResetTokenService.buildResetLink:
// a fragment is never sent to the server, so it never reaches an access log.
export function buildInviteLink(token: string) {
  if (typeof window === "undefined") {
    return `/invite#token=${encodeURIComponent(token)}`;
  }

  return `${window.location.origin}/invite#token=${encodeURIComponent(token)}`;
}

export async function copyToClipboard(value: string) {
  await navigator.clipboard.writeText(value);
}
