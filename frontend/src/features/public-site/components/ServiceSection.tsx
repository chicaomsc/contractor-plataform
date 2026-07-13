import { Container } from "@/components/layout/Container";
import { Section } from "@/components/layout/Section";
import type { PublicServiceViewModel } from "../types/view-model";
import { getWhatsAppHref } from "../utils/contact";
import { SectionLabel } from "./SectionLabel";

type ServiceSectionProps = {
  services: PublicServiceViewModel[];
  whatsapp: string | null;
  isPartialError?: boolean;
};

export function ServiceSection({
  services,
  whatsapp,
  isPartialError = false,
}: ServiceSectionProps) {
  const whatsappHref = getWhatsAppHref(whatsapp);

  return (
    <Section id="servicos" labelledBy="services-title" variant="default">
      <Container className="space-y-10">
        <header className="max-w-3xl space-y-4">
          <SectionLabel>Serviços</SectionLabel>
          <h2
            id="services-title"
            className="m-0 font-display text-3xl font-bold md:text-4xl"
          >
            O que fazemos
          </h2>
          <p className="text-[var(--muted-foreground)]">
            Serviços publicados pela empresa, apresentados sem promessas que não
            estejam nos dados públicos.
          </p>
        </header>

        {isPartialError ? (
          <p role="status" className="border-l-[3px] border-warning pl-4">
            Não foi possível carregar a lista de serviços neste momento.
          </p>
        ) : null}

        {!isPartialError && services.length === 0 ? (
          <div
            role="status"
            className="max-w-2xl border-l-[3px] border-primary bg-surface-muted p-6"
          >
            <p className="m-0 font-semibold">
              Serviços públicos ainda não configurados.
            </p>
            {whatsappHref ? (
              <a
                href={whatsappHref}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-4 inline-flex min-h-11 items-center bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground no-underline hover:bg-primary-hover"
              >
                Contactar pelo WhatsApp
              </a>
            ) : null}
          </div>
        ) : null}

        {services.length > 0 ? (
          <ol className="grid gap-x-10 border-y border-border md:grid-cols-2 lg:grid-cols-3">
            {services.map((service, index) => (
              <li
                key={service.id}
                className="grid gap-3 border-b border-border py-7 md:[&:nth-last-child(-n+2)]:border-b-0 lg:[&:nth-last-child(-n+3)]:border-b-0"
              >
                <span className="font-display text-sm font-semibold text-primary">
                  {String(index + 1).padStart(2, "0")}
                </span>
                <h3 className="m-0 font-display text-2xl font-semibold">
                  {service.name}
                </h3>
                <p className="m-0 text-sm leading-6 text-[var(--muted-foreground)]">
                  {service.summary ?? "Sem descrição pública disponível."}
                </p>
              </li>
            ))}
          </ol>
        ) : null}
      </Container>
    </Section>
  );
}
