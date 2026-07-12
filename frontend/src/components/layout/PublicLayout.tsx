import type { ReactNode } from "react";
import { SkipLink } from "@/components/navigation/SkipLink";
import type { PublicSiteViewModel } from "@/features/public-site/types/view-model";
import { SiteFooter } from "./SiteFooter";
import { SiteHeader } from "./SiteHeader";

type PublicLayoutProps = {
  children: ReactNode;
  site: PublicSiteViewModel;
  style?: Record<string, string>;
};

export function PublicLayout({ children, site, style }: PublicLayoutProps) {
  return (
    <div style={style} className="min-h-dvh bg-background">
      <SkipLink />
      <SiteHeader site={site} />
      <main id="main-content" tabIndex={-1}>
        {children}
      </main>
      <SiteFooter site={site} />
    </div>
  );
}
