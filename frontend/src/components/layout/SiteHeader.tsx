"use client";

import Image from "next/image";
import Link from "next/link";
import { Menu, MessageCircle, Phone, X } from "lucide-react";
import { useEffect, useState } from "react";
import { Container } from "./Container";
import type { PublicSiteViewModel } from "@/features/public-site/types/view-model";
import type { NavLink } from "@/features/public-site/components/landing-types";
import {
  getPhoneHref,
  getWhatsAppHref,
} from "@/features/public-site/utils/contact";
import { cn } from "@/lib/utils/cn";

type SiteHeaderProps = {
  site: PublicSiteViewModel;
  navLinks: NavLink[];
};

export function SiteHeader({ site, navLinks }: SiteHeaderProps) {
  const displayName = site.displayName;
  const logoUrl = site.branding.logoUrl;
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);
  const whatsappHref = getWhatsAppHref(site.whatsapp);
  const phoneHref = getPhoneHref(site.publicPhone);

  useEffect(() => {
    const onScroll = () => setIsScrolled(window.scrollY > 64);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => {
    if (!isMenuOpen) {
      return;
    }

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setIsMenuOpen(false);
      }
    };

    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [isMenuOpen]);

  return (
    <header
      role="banner"
      className={cn(
        "sticky top-0 z-30 border-b border-border bg-background transition-[min-height,box-shadow] duration-[var(--duration-base)]",
        isScrolled
          ? "min-h-[var(--header-height-mobile)] shadow-sm"
          : "min-h-[var(--header-height-mobile)] md:min-h-[var(--header-height-desktop)]",
      )}
    >
      <Container className="flex min-h-[inherit] items-center justify-between gap-6">
        <Link
          href="/"
          aria-label={`${displayName}, página inicial`}
          className="flex min-h-11 items-center font-display text-xl font-bold no-underline transition-colors duration-[var(--duration-fast)] hover:text-primary"
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

        <nav aria-label="Navegação principal" className="hidden lg:block">
          <ul className="flex items-center gap-8">
            {navLinks.map((link) => (
              <li key={link.href}>
                <a
                  href={link.href}
                  className="inline-flex min-h-11 items-center border-b-2 border-transparent text-sm font-semibold no-underline transition-colors duration-[var(--duration-fast)] hover:border-primary hover:text-primary"
                >
                  {link.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div className="flex items-center gap-2">
          {whatsappHref ? (
            <a
              href={whatsappHref}
              target="_blank"
              rel="noopener noreferrer"
              className="hidden min-h-11 items-center gap-2 bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground no-underline transition-colors duration-[var(--duration-fast)] hover:bg-primary-hover active:bg-[var(--primary-active)] sm:inline-flex"
            >
              <MessageCircle size={18} aria-hidden="true" />
              WhatsApp
            </a>
          ) : null}
          {!whatsappHref && phoneHref ? (
            <a
              href={phoneHref}
              aria-label={`Telefonar para ${site.publicPhone}`}
              className="hidden min-h-11 items-center gap-2 text-sm font-semibold no-underline transition-colors duration-[var(--duration-fast)] hover:text-primary sm:inline-flex"
            >
              <Phone size={18} aria-hidden="true" />
              <span>{site.publicPhone}</span>
            </a>
          ) : null}
          <button
            type="button"
            aria-label={isMenuOpen ? "Fechar menu" : "Abrir menu"}
            aria-expanded={isMenuOpen}
            aria-controls="mobile-navigation"
            onClick={() => setIsMenuOpen((value) => !value)}
            className="inline-flex h-11 w-11 items-center justify-center border border-border bg-background transition-colors duration-[var(--duration-fast)] hover:bg-surface-muted active:bg-surface-muted lg:hidden"
          >
            {isMenuOpen ? (
              <X size={22} aria-hidden="true" />
            ) : (
              <Menu size={22} aria-hidden="true" />
            )}
          </button>
        </div>
      </Container>

      <div
        id="mobile-navigation"
        role="dialog"
        aria-modal="false"
        aria-label="Menu principal"
        hidden={!isMenuOpen}
        className={cn(
          "fixed inset-x-0 top-[var(--header-height-mobile)] z-30 border-b border-t border-border bg-background shadow-sm transition-[opacity,transform] duration-[var(--duration-drawer)] lg:hidden",
          isMenuOpen
            ? "block translate-y-0 opacity-100"
            : "hidden -translate-y-2 opacity-0",
        )}
      >
        <Container className="py-4">
          <nav aria-label="Navegação móvel">
            <ul className="grid gap-1">
              {navLinks.map((link) => (
                <li key={link.href}>
                  <a
                    href={link.href}
                    onClick={() => setIsMenuOpen(false)}
                    className="flex min-h-12 items-center border-b border-border text-base font-semibold no-underline transition-colors duration-[var(--duration-fast)] active:text-primary"
                  >
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>
          {whatsappHref ? (
            <a
              href={whatsappHref}
              target="_blank"
              rel="noopener noreferrer"
              className="mt-4 inline-flex min-h-12 w-full items-center justify-center gap-2 bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground no-underline transition-colors duration-[var(--duration-fast)] hover:bg-primary-hover active:bg-[var(--primary-active)]"
            >
              <MessageCircle size={18} aria-hidden="true" />
              Contactar pelo WhatsApp
            </a>
          ) : phoneHref ? (
            <a
              href={phoneHref}
              className="mt-4 inline-flex min-h-12 w-full items-center justify-center gap-2 border-2 border-foreground px-5 py-3 text-sm font-semibold no-underline"
            >
              <Phone size={18} aria-hidden="true" />
              Telefonar
            </a>
          ) : null}
        </Container>
      </div>
    </header>
  );
}
