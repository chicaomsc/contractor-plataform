import Image from "next/image";
import Link from "next/link";
import { Phone } from "lucide-react";
import { Container } from "./Container";
import type { PublicSiteViewModel } from "@/features/public-site/types/view-model";

type SiteHeaderProps = {
  site: PublicSiteViewModel;
};

export function SiteHeader({ site }: SiteHeaderProps) {
  const displayName = site.displayName;
  const logoUrl = site.branding.logoUrl;

  return (
    <header
      role="banner"
      className="min-h-[var(--header-height-mobile)] border-b border-border bg-background md:min-h-[var(--header-height-desktop)]"
    >
      <Container className="flex min-h-[inherit] items-center justify-between gap-6">
        <Link
          href="/"
          aria-label={`${displayName}, página inicial`}
          className="flex min-h-11 items-center font-display text-xl font-bold no-underline"
        >
          {logoUrl ? (
            <Image
              src={logoUrl}
              alt={`Logótipo ${displayName}`}
              width={144}
              height={48}
              className="h-10 w-auto object-contain"
            />
          ) : (
            <span className="max-w-56 truncate">{displayName}</span>
          )}
        </Link>

        <nav aria-label="Navegação principal" className="hidden md:block">
          <span className="text-sm text-[var(--muted-foreground)]">
            Fundação técnica
          </span>
        </nav>

        {site.publicPhone ? (
          <a
            href={`tel:${site.publicPhone.replace(/\s/g, "")}`}
            className="inline-flex min-h-11 items-center gap-2 text-sm font-semibold no-underline hover:text-primary"
          >
            <Phone size={18} aria-hidden="true" />
            <span className="hidden sm:inline">{site.publicPhone}</span>
            <span className="sr-only">Telefonar para {displayName}</span>
          </a>
        ) : null}
      </Container>
    </header>
  );
}
