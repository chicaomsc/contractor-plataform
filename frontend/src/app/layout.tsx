import type { Metadata, Viewport } from "next";
import { Barlow, DM_Sans } from "next/font/google";
import type { ReactNode } from "react";
import { Providers } from "@/providers/providers";
import "./globals.css";

const barlow = Barlow({
  subsets: ["latin"],
  display: "swap",
  variable: "--font-display",
  weight: ["600", "700", "800"],
});

const dmSans = DM_Sans({
  subsets: ["latin"],
  display: "swap",
  variable: "--font-body",
});

export const metadata: Metadata = {
  metadataBase: new URL(
    process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000",
  ),
  title: {
    default: "Contractor Platform",
    template: "%s | Contractor Platform",
  },
  description: "Presença digital pública para prestadores de serviço.",
  openGraph: {
    type: "website",
    locale: "pt_PT",
    siteName: "Contractor Platform",
  },
  icons: {
    icon: [{ url: "/icon.svg", type: "image/svg+xml" }],
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#f8f6f2",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="pt-PT" className={`${barlow.variable} ${dmSans.variable}`}>
      <head>
        <meta
          name="description"
          content="Presença digital pública para prestadores de serviço."
        />
        <link rel="icon" href="/icon.svg" type="image/svg+xml" />
      </head>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
