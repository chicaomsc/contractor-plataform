import { Container } from "./Container";
import type { PublicSiteViewModel } from "@/features/public-site/types/view-model";
import type { NavLink } from "@/features/public-site/components/landing-types";
import {
  getPhoneHref,
  getWhatsAppHref,
} from "@/features/public-site/utils/contact";

type SiteFooterProps = {
  site: PublicSiteViewModel;
  navLinks: NavLink[];
};

export function SiteFooter({ site, navLinks }: SiteFooterProps) {
  const displayName = site.displayName;
  const year = new Date().getFullYear();
  const footerText = site.footerText;
  const whatsappHref = getWhatsAppHref(site.whatsapp);
  const phoneHref = getPhoneHref(site.publicPhone);

  return (
    <footer
      role="contentinfo"
      className="border-t border-border bg-background py-10 text-foreground"
    >
      <Container className="grid gap-8 text-sm md:grid-cols-[1fr_auto]">
        <div className="space-y-4">
          <p className="m-0 font-display text-lg font-semibold">
            {displayName}
          </p>
          {footerText ? (
            <p className="m-0 max-w-2xl text-[var(--muted-foreground)]">
              {footerText}
            </p>
          ) : null}
          <p className="m-0 text-[var(--muted-foreground)]">
            © {year} {displayName}. Todos os direitos reservados.
          </p>
        </div>

        <div className="grid gap-4 md:text-right">
          <nav aria-label="Links do rodapé">
            <ul className="grid gap-2 md:justify-items-end">
              {navLinks.map((link) => (
                <li key={link.href}>
                  <a
                    href={link.href}
                    className="font-semibold hover:text-primary"
                  >
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>
          <div className="grid gap-2">
            {whatsappHref ? (
              <a href={whatsappHref} target="_blank" rel="noopener noreferrer">
                WhatsApp
              </a>
            ) : null}
            {phoneHref ? <a href={phoneHref}>{site.publicPhone}</a> : null}
            {site.website ? (
              <a href={site.website} target="_blank" rel="noopener noreferrer">
                Website
              </a>
            ) : null}
          </div>
        </div>
      </Container>
    </footer>
  );
}
