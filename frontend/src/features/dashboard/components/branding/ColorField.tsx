import { ChevronDown } from "lucide-react";
import { useId, useRef, useState } from "react";
import {
  useController,
  type Control,
  type FieldPath,
  type FieldValues,
} from "react-hook-form";
import { inputClassName } from "../FormControls";
import {
  getReadableTextColor,
  isValidHexColor,
  normalizeHexColor,
  safeHexOrFallback,
} from "../../utils/colors";
import { ColorPickerPopover } from "./ColorPickerPopover";

type ColorFieldProps<TFieldValues extends FieldValues> = {
  control: Control<TFieldValues>;
  name: FieldPath<TFieldValues>;
  label: string;
  triggerLabel: string;
  expandLabel: string;
  fallback: string;
  placeholder: string;
};

export function ColorField<TFieldValues extends FieldValues>({
  control,
  name,
  label,
  triggerLabel,
  expandLabel,
  fallback,
  placeholder,
}: ColorFieldProps<TFieldValues>) {
  const pickerId = useId();
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const {
    field,
    fieldState: { error },
  } = useController({ control, name });
  const value = typeof field.value === "string" ? field.value : "";
  const swatchColor = safeHexOrFallback(value, fallback);

  function closePicker() {
    setIsOpen(false);
    triggerRef.current?.focus();
  }

  function updateColor(nextValue: string) {
    field.onChange(nextValue);
  }

  function normalizeIfValid(nextValue: string) {
    field.onBlur();
    if (isValidHexColor(nextValue)) {
      field.onChange(normalizeHexColor(nextValue));
    }
  }

  return (
    <div className="relative space-y-2">
      <label htmlFor={field.name} className="block text-sm font-semibold">
        {label}
      </label>
      <div className="flex min-w-0 items-stretch border border-border bg-background focus-within:border-primary">
        <button
          ref={triggerRef}
          type="button"
          className="flex min-h-12 w-12 shrink-0 items-center justify-center outline-none focus:ring-2 focus:ring-inset focus:ring-[var(--focus)]"
          aria-label={triggerLabel}
          aria-expanded={isOpen}
          aria-controls={isOpen ? pickerId : undefined}
          onClick={() => setIsOpen((current) => !current)}
        >
          <span
            className="h-6 w-6 border border-border"
            style={{
              backgroundColor: swatchColor,
              color: getReadableTextColor(swatchColor),
            }}
            aria-hidden="true"
          />
        </button>
        <input
          id={field.name}
          name={field.name}
          ref={field.ref}
          type="text"
          value={value}
          onChange={(event) => updateColor(event.target.value)}
          onBlur={(event) => normalizeIfValid(event.currentTarget.value)}
          className={`${inputClassName} min-w-0 flex-1 border-0 px-0 font-mono text-sm tracking-normal`}
          placeholder={placeholder}
          aria-invalid={Boolean(error)}
        />
        <button
          type="button"
          className="flex min-h-12 w-10 shrink-0 items-center justify-center outline-none focus:ring-2 focus:ring-inset focus:ring-[var(--focus)]"
          aria-label={expandLabel}
          aria-expanded={isOpen}
          aria-controls={isOpen ? pickerId : undefined}
          onClick={() => setIsOpen((current) => !current)}
        >
          <ChevronDown size={16} aria-hidden="true" />
        </button>
      </div>
      {error ? (
        <span className="block text-sm font-semibold text-error">
          {error.message}
        </span>
      ) : null}

      {isOpen ? (
        <ColorPickerPopover
          id={pickerId}
          label={label}
          value={value}
          onChange={updateColor}
          onClose={closePicker}
        />
      ) : null}
    </div>
  );
}
