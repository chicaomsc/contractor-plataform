import { Container } from "./Container";
import { getDisplayName } from "@/features/public-site/branding";
import type { PublicSite } from "@/types/public-site";

type SiteFooterProps = {
  site: PublicSite;
};

export function SiteFooter({ site }: SiteFooterProps) {
  const displayName = getDisplayName(site);
  const year = new Date().getFullYear();
  const footerText = site.branding?.footerText;

  return (
    <footer
      role="contentinfo"
      className="border-t border-border bg-[var(--surface-dark)] py-8 text-[var(--surface-dark-fg)]"
    >
      <Container className="grid gap-4 text-sm md:grid-cols-[1fr_auto] md:items-end">
        <div>
          <p className="m-0 font-display text-lg font-semibold">
            {displayName}
          </p>
          {footerText ? (
            <p className="m-0 mt-2 max-w-2xl text-[var(--surface-dark-fg)]/80">
              {footerText}
            </p>
          ) : null}
        </div>
        <p className="m-0 text-[var(--surface-dark-fg)]/70">
          © {year}. Todos os direitos reservados.
        </p>
      </Container>
    </footer>
  );
}
