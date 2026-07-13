import { ExternalLink, MessageCircle, Phone } from "lucide-react";
import { Container } from "@/components/layout/Container";
import type { PublicSiteViewModel } from "../types/view-model";
import { getPhoneHref, getWhatsAppHref } from "../utils/contact";

type ContactSectionProps = {
  site: PublicSiteViewModel;
};

export function ContactSection({ site }: ContactSectionProps) {
  const whatsappHref = getWhatsAppHref(site.whatsapp);
  const phoneHref = getPhoneHref(site.publicPhone);

  return (
    <section
      id="contacto"
      aria-labelledby="contact-title"
      className="bg-[var(--surface-dark)] py-16 text-[var(--surface-dark-fg)] md:py-20 lg:py-24"
    >
      <Container className="grid gap-10 lg:grid-cols-[1.05fr_0.95fr] lg:items-end">
        <div className="max-w-3xl space-y-5">
          <p className="m-0 text-xs font-semibold uppercase tracking-[0.12em] text-[var(--primary-on-dark)]">
            Contacto
          </p>
          <h2
            id="contact-title"
            className="m-0 max-w-3xl font-display text-3xl font-bold md:text-5xl"
          >
            Peça um orçamento sem compromisso
          </h2>
          <p className="max-w-2xl text-base leading-7 text-[var(--surface-dark-fg)]/80">
            Use os contactos públicos configurados pela empresa. O pedido segue
            diretamente pelo canal disponível, sem formulários intermédios.
          </p>
        </div>

        <div className="grid gap-3 border-t border-[var(--surface-dark-fg)]/20 pt-6 lg:border-t-0 lg:pt-0">
          {whatsappHref ? (
            <a
              href={whatsappHref}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex min-h-12 items-center justify-center gap-3 bg-primary px-6 py-4 text-sm font-semibold text-primary-foreground no-underline transition-colors duration-[var(--duration-fast)] hover:bg-primary-hover active:bg-[var(--primary-active)]"
            >
              <MessageCircle size={20} aria-hidden="true" />
              Enviar pelo WhatsApp
            </a>
          ) : null}
          {phoneHref ? (
            <a
              href={phoneHref}
              className="inline-flex min-h-12 items-center justify-center gap-3 border-2 border-[var(--surface-dark-fg)] px-6 py-4 text-sm font-semibold no-underline transition-colors duration-[var(--duration-fast)] hover:bg-[var(--surface-dark-fg)] hover:text-[var(--surface-dark)] active:bg-[var(--surface-dark-fg)] active:text-[var(--surface-dark)]"
            >
              <Phone size={20} aria-hidden="true" />
              Telefonar
            </a>
          ) : null}
          {site.website ? (
            <a
              href={site.website}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex min-h-12 items-center justify-center gap-3 border border-[var(--surface-dark-fg)]/35 px-6 py-4 text-sm font-semibold no-underline transition-colors duration-[var(--duration-fast)] hover:border-[var(--surface-dark-fg)]"
            >
              <ExternalLink size={20} aria-hidden="true" />
              Website
            </a>
          ) : null}
          {!whatsappHref && !phoneHref && !site.website ? (
            <p role="status" className="m-0 text-[var(--surface-dark-fg)]/75">
              Contactos públicos ainda não configurados.
            </p>
          ) : null}
        </div>
      </Container>
    </section>
  );
}
