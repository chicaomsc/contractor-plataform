import Image from "next/image";
import { ArrowDownRight, MapPin } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { Section } from "@/components/layout/Section";
import type {
  PublicGalleryItemViewModel,
  PublicSiteViewModel,
} from "../types/view-model";
import { getWhatsAppHref } from "../utils/contact";
import { getHeroHeadline, getHeroLead } from "../utils/content";

type HeroSectionProps = {
  site: PublicSiteViewModel;
  heroImage: PublicGalleryItemViewModel | null;
};

export function HeroSection({ site, heroImage }: HeroSectionProps) {
  const whatsappHref = getWhatsAppHref(site.whatsapp);
  const imageUrl = heroImage?.afterImageUrl ?? heroImage?.beforeImageUrl;
  const imageAlt =
    heroImage?.afterAlt ??
    heroImage?.beforeAlt ??
    "Fotografia pública de uma obra concluída";

  return (
    <Section
      labelledBy="hero-title"
      className="overflow-hidden pt-8 md:pt-12 lg:pt-20"
    >
      <Container>
        <div className="grid min-w-0 gap-12 lg:grid-cols-[minmax(0,0.96fr)_minmax(360px,1.04fr)] lg:items-center xl:gap-16">
          <div className="order-2 w-full max-w-full min-w-0 space-y-8 lg:order-1 lg:pb-8">
            <div className="h-[3px] w-24 bg-primary" aria-hidden="true" />
            <div className="space-y-6">
              <h1
                id="hero-title"
                className="m-0 max-w-4xl break-words [overflow-wrap:anywhere] font-display text-3xl font-bold leading-[1.08] md:text-5xl lg:text-6xl"
              >
                {getHeroHeadline(site)}
              </h1>
              <p className="max-w-2xl break-words [overflow-wrap:anywhere] text-base leading-8 text-[var(--muted-foreground)] md:text-lg">
                {getHeroLead(site)}
              </p>
              {site.locationLabel ? (
                <p className="inline-flex max-w-full items-start gap-2 border-l-[3px] border-primary pl-4 text-sm font-semibold leading-6">
                  <MapPin size={18} aria-hidden="true" />
                  {site.locationLabel}
                </p>
              ) : null}
            </div>

            <div className="flex flex-col gap-3 sm:flex-row">
              {whatsappHref ? (
                <a
                  href={whatsappHref}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex min-h-12 w-full items-center justify-center bg-primary px-7 py-4 text-sm font-semibold text-primary-foreground no-underline transition-colors duration-[var(--duration-fast)] hover:bg-primary-hover active:bg-[var(--primary-active)] sm:w-auto"
                >
                  Pedir orçamento
                </a>
              ) : (
                <a
                  href="#contacto"
                  className="inline-flex min-h-12 w-full items-center justify-center bg-primary px-7 py-4 text-sm font-semibold text-primary-foreground no-underline transition-colors duration-[var(--duration-fast)] hover:bg-primary-hover active:bg-[var(--primary-active)] sm:w-auto"
                >
                  Contactar
                </a>
              )}
              <a
                href="#trabalhos"
                className="inline-flex min-h-12 w-full items-center justify-center gap-2 border-2 border-foreground px-7 py-4 text-sm font-semibold no-underline transition-colors duration-[var(--duration-fast)] hover:bg-foreground hover:text-surface active:bg-foreground active:text-surface sm:w-auto"
              >
                Ver trabalhos
                <ArrowDownRight size={18} aria-hidden="true" />
              </a>
            </div>
          </div>

          <div className="order-1 w-full max-w-full min-w-0 md:max-w-none lg:order-2">
            {imageUrl ? (
              <div className="relative aspect-[4/3] w-full max-w-full min-w-0 overflow-hidden border-l-[5px] border-primary bg-surface-muted lg:aspect-[5/6]">
                <Image
                  src={imageUrl}
                  alt={imageAlt}
                  fill
                  priority
                  sizes="(min-width: 1024px) 42vw, 100vw"
                  className="object-cover"
                />
              </div>
            ) : (
              <div className="flex aspect-[4/3] w-full max-w-full min-w-0 items-end border-l-[5px] border-primary bg-surface-muted p-6 lg:aspect-[5/6]">
                <p className="max-w-sm text-sm font-semibold text-[var(--muted-foreground)]">
                  Fotografia pública ainda não configurada.
                </p>
              </div>
            )}
          </div>
        </div>
      </Container>
    </Section>
  );
}
