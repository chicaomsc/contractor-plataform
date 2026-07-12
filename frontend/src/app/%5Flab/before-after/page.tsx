import type { Metadata } from "next";
import { BeforeAfterLab } from "@/features/before-after-comparison";

export const metadata: Metadata = {
  title: "Laboratorio before/after",
  description:
    "Rota experimental isolada para avaliar o comparador antes/depois.",
  robots: {
    index: false,
    follow: false,
    nocache: true,
  },
};

export default function BeforeAfterLabPage() {
  return <BeforeAfterLab />;
}
