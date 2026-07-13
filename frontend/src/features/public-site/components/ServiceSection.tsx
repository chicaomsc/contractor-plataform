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
      <Container className="space-y-12">
        <header className="grid gap-6 lg:grid-cols-[0.72fr_1fr] lg:items-end">
          <div className="max-w-2xl space-y-4">
            <SectionLabel>Serviços</SectionLabel>
            <h2
              id="services-title"
              className="m-0 font-display text-3xl font-bold md:text-4xl"
            >
              O que fazemos
            </h2>
          </div>
          <p className="m-0 max-w-2xl text-base leading-7 text-[var(--muted-foreground)]">
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
                className="mt-4 inline-flex min-h-11 items-center bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground no-underline transition-colors duration-[var(--duration-fast)] hover:bg-primary-hover active:bg-[var(--primary-active)]"
              >
                Contactar pelo WhatsApp
              </a>
            ) : null}
          </div>
        ) : null}

        {services.length > 0 ? (
          <ol className="divide-y divide-border border-y border-border">
            {services.map((service, index) => (
              <li
                key={service.id}
                className="group grid gap-4 py-7 transition-colors duration-[var(--duration-fast)] md:grid-cols-[4rem_minmax(0,0.85fr)_minmax(0,1.15fr)] md:items-baseline md:py-8"
              >
                <span className="font-display text-sm font-semibold text-primary transition-colors duration-[var(--duration-fast)] md:text-base">
                  {String(index + 1).padStart(2, "0")}
                </span>
                <h3 className="m-0 font-display text-2xl font-semibold leading-tight transition-colors duration-[var(--duration-fast)] group-hover:text-primary">
                  {service.name}
                </h3>
                <p className="m-0 max-w-2xl text-sm leading-6 text-[var(--muted-foreground)] md:justify-self-end">
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
