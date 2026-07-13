"use client";

import Image from "next/image";
import { useId, useRef, useState } from "react";
import type { KeyboardEvent, PointerEvent } from "react";
import {
  COMPARISON_STEP,
  clampComparisonPosition,
  getComparisonValueText,
  hasCompleteComparisonImages,
} from "./utils";
import type { BeforeAfterComparisonProps } from "./types";

export function BeforeAfterComparisonBaseline({
  beforeImageUrl,
  afterImageUrl,
  beforeAlt,
  afterAlt,
  title,
  description,
  initialPosition = 50,
  titleHeadingLevel = "h2",
}: BeforeAfterComparisonProps) {
  const titleId = useId();
  const descriptionId = useId();
  const frameRef = useRef<HTMLDivElement>(null);
  const [position, setPosition] = useState(
    clampComparisonPosition(initialPosition),
  );

  const imageInput = {
    beforeImageUrl,
    afterImageUrl,
    beforeAlt,
    afterAlt,
  };
  const images = hasCompleteComparisonImages(imageInput) ? imageInput : null;
  const TitleTag = titleHeadingLevel;

  function updateFromPointer(event: PointerEvent<HTMLDivElement>) {
    const rect = frameRef.current?.getBoundingClientRect();
    if (!rect || rect.width === 0) {
      return;
    }

    const next = ((event.clientX - rect.left) / rect.width) * 100;
    setPosition(clampComparisonPosition(next));
  }

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    const actions: Record<string, number> = {
      ArrowLeft: position - COMPARISON_STEP,
      ArrowRight: position + COMPARISON_STEP,
      Home: 0,
      End: 100,
    };

    if (event.key in actions) {
      event.preventDefault();
      setPosition(clampComparisonPosition(actions[event.key]));
    }
  }

  return (
    <article
      aria-labelledby={title ? titleId : undefined}
      aria-describedby={description ? descriptionId : undefined}
      className="space-y-6"
    >
      {(title || description) && (
        <header className="max-w-2xl space-y-2 border-l-[3px] border-primary pl-4">
          {title && (
            <TitleTag
              id={titleId}
              className="m-0 font-display text-2xl font-semibold leading-tight"
            >
              {title}
            </TitleTag>
          )}
          {description && (
            <p
              id={descriptionId}
              className="m-0 text-sm leading-6 text-[var(--muted-foreground)]"
            >
              {description}
            </p>
          )}
        </header>
      )}

      {!images ? (
        <div
          role="status"
          className="border-l-[3px] border-primary bg-surface-muted p-5 text-sm"
        >
          Comparação indisponível: faltam imagens ou textos alternativos.
        </div>
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2 lg:hidden">
            <figure className="space-y-2">
              <span className="text-xs font-semibold uppercase tracking-[0.12em] text-primary">
                Antes
              </span>
              <Image
                src={images.beforeImageUrl}
                alt={images.beforeAlt}
                width={1200}
                height={900}
                sizes="100vw"
                className="aspect-[4/3] w-full border border-border object-cover"
              />
            </figure>
            <figure className="space-y-2">
              <span className="text-xs font-semibold uppercase tracking-[0.12em] text-primary">
                Depois
              </span>
              <Image
                src={images.afterImageUrl}
                alt={images.afterAlt}
                width={1200}
                height={900}
                sizes="100vw"
                className="aspect-[4/3] w-full border border-border object-cover"
              />
            </figure>
          </div>

          <div
            ref={frameRef}
            data-testid="baseline-comparison-stage"
            className="relative hidden aspect-[4/3] touch-none overflow-hidden border border-border bg-surface-muted lg:block"
            onPointerDown={(event) => {
              event.currentTarget.setPointerCapture(event.pointerId);
              updateFromPointer(event);
            }}
            onPointerMove={(event) => {
              if (event.currentTarget.hasPointerCapture(event.pointerId)) {
                updateFromPointer(event);
              }
            }}
            onPointerUp={(event) => {
              event.currentTarget.releasePointerCapture(event.pointerId);
            }}
          >
            <Image
              src={images.beforeImageUrl}
              alt={images.beforeAlt}
              fill
              sizes="(min-width: 1024px) 60vw, 100vw"
              className="object-cover"
            />
            <div
              className="absolute inset-0"
              style={{ clipPath: `inset(0 0 0 ${position}%)` }}
            >
              <Image
                src={images.afterImageUrl}
                alt={images.afterAlt}
                fill
                sizes="(min-width: 1024px) 60vw, 100vw"
                className="object-cover"
              />
            </div>
            <span className="absolute left-4 top-4 bg-surface px-3 py-2 text-xs font-semibold uppercase tracking-[0.12em]">
              Antes
            </span>
            <span className="absolute right-4 top-4 bg-surface px-3 py-2 text-xs font-semibold uppercase tracking-[0.12em]">
              Depois
            </span>
            <div
              role="slider"
              data-testid="baseline-comparison-slider"
              aria-label="Comparador antes e depois baseline"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={position}
              aria-valuetext={getComparisonValueText(position)}
              tabIndex={0}
              onKeyDown={handleKeyDown}
              className="absolute top-0 h-full w-11 -translate-x-1/2 cursor-col-resize focus-visible:outline focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-focus"
              style={{ left: `${position}%` }}
            >
              <span className="absolute left-1/2 top-0 h-full w-px -translate-x-1/2 bg-primary" />
              <span className="absolute left-1/2 top-1/2 grid h-11 w-11 -translate-x-1/2 -translate-y-1/2 place-items-center border border-foreground bg-surface text-sm font-semibold transition-transform duration-[var(--duration-fast)] hover:scale-105">
                ↔
              </span>
            </div>
          </div>
        </>
      )}
    </article>
  );
}
