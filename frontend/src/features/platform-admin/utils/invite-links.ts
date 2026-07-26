export function buildInviteLink(token: string) {
  if (typeof window === "undefined") {
    return `/invite?token=${encodeURIComponent(token)}`;
  }

  return `${window.location.origin}/invite?token=${encodeURIComponent(token)}`;
}

export async function copyToClipboard(value: string) {
  await navigator.clipboard.writeText(value);
}
