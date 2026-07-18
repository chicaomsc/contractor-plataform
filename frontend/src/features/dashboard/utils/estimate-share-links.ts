import { getPublicEnv } from "@/lib/env/public-env";

/** Builds the public share URL for a token. Falls back to the current origin when
 * NEXT_PUBLIC_SITE_URL isn't configured (it's optional — see public-env.ts). */
export function buildEstimateShareUrl(token: string): string {
  let origin: string;
  try {
    origin = getPublicEnv().NEXT_PUBLIC_SITE_URL ?? window.location.origin;
  } catch {
    origin = window.location.origin;
  }
  return `${origin}/share/${token}`;
}

/** No WhatsApp Business API integration — just a wa.me deep link with the public URL prefilled. */
export function buildEstimateShareWhatsAppHref(shareUrl: string): string {
  const message = `Segue o link do seu orçamento: ${shareUrl}`;
  return `https://wa.me/?text=${encodeURIComponent(message)}`;
}
