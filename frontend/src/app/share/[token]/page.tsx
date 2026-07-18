import type { Metadata } from "next";
import { PublicEstimateSharePage } from "@/features/estimate-share";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Orçamento partilhado",
  robots: { index: false, follow: false },
};

export default async function ShareTokenPage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = await params;
  return <PublicEstimateSharePage token={token} />;
}
