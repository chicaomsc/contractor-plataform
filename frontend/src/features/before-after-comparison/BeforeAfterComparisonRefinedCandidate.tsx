"use client";

import Image from "next/image";
import { MoveHorizontal } from "lucide-react";
import { useId, useRef, useState } from "react";
import type { KeyboardEvent, PointerEvent } from "react";
import {
  COMPARISON_STEP,
  clampComparisonPosition,
  getComparisonValueText,
  hasCompleteComparisonImages,
} from "./utils";
import type { BeforeAfterComparisonProps } from "./types";

export function BeforeAfterComparisonRefinedCandidate({
  beforeImageUrl,
  afterImageUrl,
  beforeAlt,
  afterAlt,
  title,
  description,
  initialPosition = 50,
}: BeforeAfterComparisonProps) {
  const headingId = useId();
  const copyId = useId();
  const stageRef = useRef<HTMLDivElement>(null);
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

  function setPositionFromPointer(event: PointerEvent<HTMLDivElement>) {
    const bounds = stageRef.current?.getBoundingClientRect();
    if (!bounds || bounds.width === 0) {
      return;
    }

    setPosition(
      clampComparisonPosition(
        ((event.clientX - bounds.left) / bounds.width) * 100,
      ),
    );
  }

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === "ArrowLeft") {
      event.preventDefault();
      setPosition((current) =>
        clampComparisonPosition(current - COMPARISON_STEP),
      );
    }

    if (event.key === "ArrowRight") {
      event.preventDefault();
      setPosition((current) =>
        clampComparisonPosition(current + COMPARISON_STEP),
      );
    }

    if (event.key === "Home") {
      event.preventDefault();
      setPosition(0);
    }

    if (event.key === "End") {
      event.preventDefault();
      setPosition(100);
    }
  }

  return (
    <article
      aria-labelledby={title ? headingId : undefined}
      aria-describedby={description ? copyId : undefined}
      className="space-y-6"
    >
      {(title || description) && (
        <header className="grid gap-3 border-l-[3px] border-primary pl-5">
          {title && (
            <h2 id={headingId} className="font-display text-2xl font-bold">
              {title}
            </h2>
          )}
          {description && (
            <p
              id={copyId}
              className="max-w-2xl text-sm text-[var(--muted-foreground)]"
            >
              {description}
            </p>
          )}
        </header>
      )}

      {!images ? (
        <div
          role="status"
          className="grid min-h-48 place-items-center bg-surface-muted p-6 text-center text-sm"
        >
          <p>Comparação suspensa até existir um par completo antes/depois.</p>
        </div>
      ) : (
        <>
          <div className="grid gap-5 md:grid-cols-2 lg:hidden">
            <figure>
              <Image
                src={images.beforeImageUrl}
                alt={images.beforeAlt}
                width={1200}
                height={900}
                sizes="100vw"
                className="aspect-[4/3] w-full object-cover"
              />
              <figcaption className="mt-2 border-l-[3px] border-primary pl-3 text-xs font-semibold uppercase tracking-[0.12em]">
                Antes
              </figcaption>
            </figure>
            <figure>
              <Image
                src={images.afterImageUrl}
                alt={images.afterAlt}
                width={1200}
                height={900}
                sizes="100vw"
                className="aspect-[4/3] w-full object-cover"
              />
              <figcaption className="mt-2 border-l-[3px] border-primary pl-3 text-xs font-semibold uppercase tracking-[0.12em]">
                Depois
              </figcaption>
            </figure>
          </div>

          <div
            ref={stageRef}
            data-testid="refined-candidate-comparison-stage"
            className="relative hidden aspect-[4/3] touch-none overflow-hidden bg-surface-muted lg:block"
            onPointerDown={(event) => {
              event.currentTarget.setPointerCapture(event.pointerId);
              setPositionFromPointer(event);
            }}
            onPointerMove={(event) => {
              if (event.currentTarget.hasPointerCapture(event.pointerId)) {
                setPositionFromPointer(event);
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
            <div className="absolute inset-x-0 top-0 flex justify-between p-4">
              <span className="bg-[var(--surface-dark)] px-3 py-2 text-xs font-semibold uppercase tracking-[0.12em] text-[var(--surface-dark-fg)]">
                Antes
              </span>
              <span className="bg-[var(--surface-dark)] px-3 py-2 text-xs font-semibold uppercase tracking-[0.12em] text-[var(--surface-dark-fg)]">
                Depois
              </span>
            </div>
            <div
              role="slider"
              data-testid="refined-candidate-comparison-slider"
              aria-label="Comparador antes e depois candidato refinado"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={position}
              aria-valuetext={getComparisonValueText(position)}
              tabIndex={0}
              onKeyDown={handleKeyDown}
              className="absolute top-0 h-full w-11 -translate-x-1/2 cursor-col-resize focus-visible:outline focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-focus motion-reduce:transition-none"
              style={{ left: `${position}%` }}
            >
              <span className="absolute left-1/2 top-0 h-full w-0.5 -translate-x-1/2 bg-primary" />
              <span className="absolute left-1/2 top-1/2 grid h-11 w-11 -translate-x-1/2 -translate-y-1/2 place-items-center border-2 border-primary bg-surface text-primary">
                <MoveHorizontal
                  aria-hidden="true"
                  size={20}
                  strokeWidth={2.25}
                />
              </span>
            </div>
          </div>
        </>
      )}
    </article>
  );
}
