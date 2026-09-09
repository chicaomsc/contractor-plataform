import { useEffect } from "react";
import { inputClassName } from "../FormControls";
import {
  getReadableTextColor,
  isValidHexColor,
  normalizeHexColor,
} from "../../utils/colors";

export const COLOR_SWATCHES = [
  "#1E40AF",
  "#2563EB",
  "#0F766E",
  "#166534",
  "#65A30D",
  "#CA8A04",
  "#D97706",
  "#F59E0B",
  "#B45309",
  "#BE123C",
  "#DB2777",
  "#7C3AED",
  "#312E81",
  "#374151",
  "#6B7280",
  "#111827",
  "#F97316",
  "#14B8A6",
];

type ColorPickerPopoverProps = {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  onClose: () => void;
};

export function ColorPickerPopover({
  id,
  label,
  value,
  onChange,
  onClose,
}: ColorPickerPopoverProps) {
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        onClose();
      }
    };

    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  function normalizeIfValid(nextValue: string) {
    if (isValidHexColor(nextValue)) {
      onChange(normalizeHexColor(nextValue));
    }
  }

  return (
    <div
      id={id}
      role="dialog"
      aria-label={`Selecionar ${label.toLowerCase()}`}
      className="absolute left-0 right-0 top-[calc(100%+0.5rem)] z-40 border border-border bg-surface p-4 shadow-sm sm:right-auto sm:w-80"
    >
      <div className="grid grid-cols-6 gap-2" aria-label="Cores disponíveis">
        {COLOR_SWATCHES.map((color) => {
          const normalizedValue = isValidHexColor(value)
            ? normalizeHexColor(value)
            : "";
          const isSelected = normalizedValue === color;

          return (
            <button
              key={color}
              type="button"
              className="flex h-10 w-full items-center justify-center border border-border text-xs font-semibold outline-none transition-transform focus:border-primary focus:ring-2 focus:ring-[var(--focus)]"
              style={{
                backgroundColor: color,
                color: getReadableTextColor(color),
              }}
              aria-label={`Selecionar ${color}`}
              aria-pressed={isSelected}
              onClick={() => onChange(color)}
            >
              {isSelected ? "✓" : ""}
            </button>
          );
        })}
      </div>

      <label className="mt-4 block space-y-2">
        <span className="text-sm font-semibold">HEX</span>
        <input
          type="text"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          onBlur={(event) => normalizeIfValid(event.currentTarget.value)}
          className={inputClassName}
          placeholder="#1E40AF"
          aria-label={`${label} HEX no seletor`}
        />
      </label>

      <div className="mt-4 flex justify-end">
        <button
          type="button"
          className="inline-flex min-h-10 items-center border border-border px-4 text-sm font-semibold transition-colors hover:border-primary"
          onClick={onClose}
        >
          Concluir
        </button>
      </div>
    </div>
  );
}
