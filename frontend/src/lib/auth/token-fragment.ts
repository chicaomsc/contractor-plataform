/**
 * Reads a `token` value from a URL fragment (`#token=...`), never from the query
 * string — a fragment is never sent to the server (not in the request line, not in
 * Referer, not in access logs), which is why every sensitive one-time-token link in
 * this app (password reset, and — since Sprint 11B.6D — invite acceptance) uses this
 * shape instead of `?token=`. See SEC-AUTH-07 / DT-011B.2.
 */
export function readTokenFromHash(hash: string): string | null {
  const fragment = hash.startsWith("#") ? hash.slice(1) : hash;
  const params = new URLSearchParams(fragment);
  const token = params.get("token")?.trim();
  return token && token.length > 0 ? token : null;
}
