import type { ReactNode } from "react";
import { ApiUnavailable } from "@/components/feedback/ApiUnavailable";
import { SkipLink } from "@/components/navigation/SkipLink";
import { getBrandingStyle } from "@/features/public-site/branding";
import type { PublicSite } from "@/types/public-site";
import { SiteFooter } from "./SiteFooter";
import { SiteHeader } from "./SiteHeader";

type PublicLayoutProps = {
  children: ReactNode;
  site: PublicSite;
  usingFallback?: boolean;
};

export function PublicLayout({
  children,
  site,
  usingFallback = false,
}: PublicLayoutProps) {
  return (
    <div style={getBrandingStyle(site)} className="min-h-dvh bg-background">
      <SkipLink />
      <SiteHeader site={site} />
      {usingFallback ? (
        <div className="bg-surface-muted px-5 py-3">
          <div className="mx-auto max-w-container">
            <ApiUnavailable />
          </div>
        </div>
      ) : null}
      <main id="main-content" tabIndex={-1}>
        {children}
      </main>
      <SiteFooter site={site} />
    </div>
  );
}
