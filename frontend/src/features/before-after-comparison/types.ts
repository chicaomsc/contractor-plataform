export type BeforeAfterComparisonProps = {
  beforeImageUrl?: string | null;
  afterImageUrl?: string | null;
  beforeAlt?: string | null;
  afterAlt?: string | null;
  title?: string | null;
  description?: string | null;
  initialPosition?: number;
};

export type CompleteBeforeAfterComparisonProps = Required<
  Pick<
    BeforeAfterComparisonProps,
    "beforeImageUrl" | "afterImageUrl" | "beforeAlt" | "afterAlt"
  >
> &
  Pick<BeforeAfterComparisonProps, "title" | "description" | "initialPosition">;
