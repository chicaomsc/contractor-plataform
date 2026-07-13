import type { Metadata } from "next";
import type { ReactNode } from "react";

export const metadata: Metadata = {
  description: "Presença digital pública para prestadores de serviço.",
  openGraph: {
    description: "Presença digital pública para prestadores de serviço.",
    locale: "pt_PT",
    type: "website",
  },
};

export default function PublicRouteLayout({
  children,
}: {
  children: ReactNode;
}) {
  return children;
}
