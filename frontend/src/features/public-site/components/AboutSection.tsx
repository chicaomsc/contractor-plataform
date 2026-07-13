import Image from "next/image";
import { Container } from "@/components/layout/Container";
import { Section } from "@/components/layout/Section";
import type {
  PublicGalleryItemViewModel,
  PublicSiteViewModel,
} from "../types/view-model";
import { SectionLabel } from "./SectionLabel";

type AboutSectionProps = {
  site: PublicSiteViewModel;
  image: PublicGalleryItemViewModel | null;
};

export function AboutSection({ site, image }: AboutSectionProps) {
  const aboutText = site.aboutText;
  const imageUrl = image?.afterImageUrl ?? image?.beforeImageUrl;

  if (!aboutText && !imageUrl) {
    return null;
  }

  return (
    <Section id="sobre" labelledBy="about-title">
      <Container>
        <div className="grid gap-10 lg:grid-cols-[0.86fr_1.14fr] lg:items-center lg:gap-14">
          {imageUrl ? (
            <div className="relative aspect-[4/3] overflow-hidden border-l-[5px] border-primary bg-surface-muted lg:aspect-[5/4]">
              <Image
                src={imageUrl}
                alt={
                  image?.afterAlt ??
                  image?.beforeAlt ??
                  "Fotografia pública de obra da empresa"
                }
                fill
                sizes="(min-width: 1024px) 42vw, 100vw"
                className="object-cover"
              />
            </div>
          ) : null}

          <div className="space-y-5">
            <SectionLabel>Sobre</SectionLabel>
            <h2
              id="about-title"
              className="m-0 font-display text-3xl font-bold md:text-4xl"
            >
              Sobre a empresa
            </h2>
            <p className="max-w-3xl text-lg leading-8 text-[var(--muted-foreground)]">
              {aboutText ??
                "Informação institucional pública ainda não configurada."}
            </p>
          </div>
        </div>
      </Container>
    </Section>
  );
}
