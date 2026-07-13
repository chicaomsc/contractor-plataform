type SectionLabelProps = {
  children: string;
};

export function SectionLabel({ children }: SectionLabelProps) {
  return (
    <p className="m-0 text-xs font-semibold uppercase tracking-[0.12em] text-primary">
      {children}
    </p>
  );
}
