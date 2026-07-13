import { MessageCircle } from "lucide-react";
import type { PublicSiteViewModel } from "../types/view-model";
import { getWhatsAppHref } from "../utils/contact";

type MobileWhatsAppActionProps = {
  site: PublicSiteViewModel;
};

export function MobileWhatsAppAction({ site }: MobileWhatsAppActionProps) {
  const href = getWhatsAppHref(site.whatsapp);

  if (!href) {
    return null;
  }

  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={`Contactar ${site.displayName} pelo WhatsApp`}
      className="fixed bottom-[calc(1rem+env(safe-area-inset-bottom))] right-4 z-40 inline-flex h-14 w-14 items-center justify-center bg-primary text-primary-foreground no-underline shadow-sm transition-colors duration-[var(--duration-fast)] hover:bg-primary-hover active:bg-[var(--primary-active)] md:hidden"
    >
      <MessageCircle size={24} aria-hidden="true" />
      <span className="sr-only">Abre o WhatsApp numa nova janela</span>
    </a>
  );
}
