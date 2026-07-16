export function FeaturedBadge({ featured }: { featured: boolean }) {
  if (!featured) {
    return null;
  }

  return (
    <span className="inline-flex min-h-7 items-center border border-primary bg-primary px-3 text-xs font-bold uppercase tracking-[0.12em] text-primary-foreground">
      Destaque
    </span>
  );
}
