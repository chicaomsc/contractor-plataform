// Sprint 11A.2 — dedicated container healthcheck target. Deliberately trivial: no
// backend call, no database, no environment variable read, no version/hostname in
// the body — only "is the Next.js server process itself responding". See
// docs/design/DT-011A.2-production-configuration.md §6.2.4 for why this replaces `/`
// (which couples the check to the public landing page's own rendering/data-fetching).
export const dynamic = "force-dynamic";

export function GET() {
  return Response.json({ status: "UP" });
}
