"use client";

import {
  Building2,
  ChevronRight,
  Home,
  LogOut,
  Menu,
  Shield,
  X,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, type ReactNode } from "react";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/features/auth/hooks/auth-context";
import { cn } from "@/lib/utils/cn";

const navItems = [
  { href: "/admin", label: "Visão geral", icon: Home },
  { href: "/admin/companies", label: "Empresas", icon: Building2 },
];

const breadcrumbLabels = new Map([
  ["admin", "Platform Admin"],
  ["companies", "Empresas"],
  ["new", "Nova empresa"],
]);

function Breadcrumb() {
  const pathname = usePathname();
  const parts = pathname.split("/").filter(Boolean);

  return (
    <nav aria-label="Breadcrumb" className="min-w-0">
      <ol className="flex min-w-0 items-center gap-2 text-sm text-[var(--muted-foreground)]">
        {parts.map((part, index) => {
          const href = `/${parts.slice(0, index + 1).join("/")}`;
          const isLast = index === parts.length - 1;

          return (
            <li key={href} className="flex min-w-0 items-center gap-2">
              {index > 0 ? <ChevronRight size={14} aria-hidden="true" /> : null}
              {isLast ? (
                <span className="truncate font-semibold text-foreground">
                  {breadcrumbLabels.get(part) ?? part}
                </span>
              ) : (
                <Link href={href} className="truncate no-underline">
                  {breadcrumbLabels.get(part) ?? part}
                </Link>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

function SidebarContent({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname();
  const { logout, session } = useAuth();

  return (
    <div className="flex h-full flex-col bg-[var(--surface-dark)] text-[var(--surface-dark-fg)]">
      <div className="border-b border-white/10 px-5 py-5">
        <Link href="/admin" className="block no-underline">
          <span className="block font-display text-xl font-bold">
            Contractor
          </span>
          <span className="block text-xs font-semibold uppercase tracking-[0.18em] text-white/55">
            Platform Admin
          </span>
        </Link>
      </div>

      <nav
        className="min-h-0 flex-1 overflow-y-auto px-3 py-5"
        aria-label="Navegação da plataforma"
      >
        <ul className="m-0 space-y-1 p-0">
          {navItems.map((item) => {
            const Icon = item.icon;
            const active =
              pathname === item.href ||
              (item.href !== "/admin" && pathname.startsWith(item.href));

            return (
              <li key={item.href} className="list-none">
                <Link
                  href={item.href}
                  onClick={onNavigate}
                  className={cn(
                    "flex min-h-11 items-center gap-3 px-3 text-sm font-semibold no-underline transition-colors",
                    active
                      ? "bg-primary text-primary-foreground"
                      : "text-white/75 hover:bg-white/10 hover:text-white",
                  )}
                >
                  <Icon size={18} aria-hidden="true" />
                  {item.label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>

      <div
        className="shrink-0 border-t border-white/10 p-5"
        data-testid="admin-sidebar-account"
      >
        <p className="m-0 truncate text-sm font-semibold">
          {session?.user.name}
        </p>
        <p className="m-0 mt-1 truncate text-xs text-white/55">
          {session?.user.email}
        </p>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="mt-4 w-full justify-start text-white/75 hover:bg-white/10 hover:text-white"
          onClick={logout}
        >
          <LogOut size={16} aria-hidden="true" />
          Sair
        </Button>
      </div>
    </div>
  );
}

export function AdminShell({ children }: { children: ReactNode }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-background lg:grid lg:grid-cols-[260px_minmax(0,1fr)] lg:bg-[linear-gradient(to_right,var(--surface-dark)_0,var(--surface-dark)_260px,var(--background)_260px,var(--background)_100%)]">
      <aside
        className="sticky top-0 hidden h-screen self-start bg-[var(--surface-dark)] lg:block"
        data-testid="admin-sidebar"
      >
        <SidebarContent />
      </aside>

      {sidebarOpen ? (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            type="button"
            className="absolute inset-0 bg-black/40"
            aria-label="Fechar menu"
            onClick={() => setSidebarOpen(false)}
          />
          <aside className="relative h-full w-[min(320px,86vw)] shadow-sm">
            <div className="absolute right-3 top-3 z-10">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                aria-label="Fechar menu"
                onClick={() => setSidebarOpen(false)}
                className="text-white hover:bg-white/10 hover:text-white"
              >
                <X size={18} aria-hidden="true" />
              </Button>
            </div>
            <SidebarContent onNavigate={() => setSidebarOpen(false)} />
          </aside>
        </div>
      ) : null}

      <div className="min-w-0">
        <header className="sticky top-0 z-30 border-b border-border bg-background/95 backdrop-blur">
          <div className="flex min-h-16 items-center justify-between gap-4 px-4 md:px-8">
            <div className="flex min-w-0 items-center gap-3">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="lg:hidden"
                aria-label="Abrir menu"
                onClick={() => setSidebarOpen(true)}
              >
                <Menu size={20} aria-hidden="true" />
              </Button>
              <div className="min-w-0">
                <p className="m-0 inline-flex items-center gap-2 text-sm font-semibold">
                  <Shield size={16} aria-hidden="true" />
                  Administração da plataforma
                </p>
                <Breadcrumb />
              </div>
            </div>
          </div>
        </header>

        <main className="mx-auto w-full max-w-[1440px] px-4 py-8 md:px-8 lg:px-10">
          {children}
        </main>
      </div>
    </div>
  );
}
