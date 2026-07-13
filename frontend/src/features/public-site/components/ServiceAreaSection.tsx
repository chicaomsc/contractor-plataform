import { MapPin } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { Section } from "@/components/layout/Section";
import type { PublicSiteViewModel } from "../types/view-model";
import { getWhatsAppHref } from "../utils/contact";
import { SectionLabel } from "./SectionLabel";

type ServiceAreaSectionProps = {
  site: PublicSiteViewModel;
};

export function ServiceAreaSection({ site }: ServiceAreaSectionProps) {
  if (!site.locationLabel) {
    return null;
  }

  const whatsappHref = getWhatsAppHref(
    site.whatsapp,
    "Perguntar disponibilidade",
  );

  return (
    <Section id="area" labelledBy="area-title" variant="muted">
      <Container className="grid gap-8 lg:grid-cols-[0.72fr_1.28fr] lg:items-start">
        <div className="space-y-4">
          <SectionLabel>Área</SectionLabel>
          <h2
            id="area-title"
            className="m-0 font-display text-3xl font-bold md:text-4xl"
          >
            Onde trabalhamos
          </h2>
        </div>
        <div className="border-l-[3px] border-primary bg-background p-6 md:p-8">
          <p className="m-0 flex items-start gap-3 text-lg font-semibold">
            <MapPin size={22} aria-hidden="true" className="mt-1 shrink-0" />
            {site.locationLabel}
          </p>
          <p className="mb-0 mt-4 text-sm leading-6 text-[var(--muted-foreground)]">
            A informação pública de área de atuação está limitada à localização
            configurada. Para confirmar disponibilidade noutra zona, contacte a
            empresa diretamente.
          </p>
          {whatsappHref ? (
            <a
              href={whatsappHref}
              target="_blank"
              rel="noopener noreferrer"
              className="mt-5 inline-flex min-h-11 items-center border-2 border-foreground px-5 py-3 text-sm font-semibold no-underline transition-colors duration-[var(--duration-fast)] hover:bg-foreground hover:text-surface active:bg-foreground active:text-surface"
            >
              Perguntar disponibilidade
            </a>
          ) : null}
        </div>
      </Container>
    </Section>
  );
}
