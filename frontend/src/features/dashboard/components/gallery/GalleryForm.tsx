"use client";

import { Save, X } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import {
  galleryFormSchema,
  type GalleryDto,
  type GalleryFormInput,
} from "../../types/admin";
import { nullableText } from "../../utils/forms";
import { zodResolver } from "../../utils/zod-resolver";
import { SaveFeedback } from "../DashboardState";
import { Field, inputClassName, textareaClassName } from "../FormControls";

type GalleryFormProps = {
  item: GalleryDto | null;
  nextDisplayOrder: number;
  isSaving: boolean;
  isError: boolean;
  isSuccess: boolean;
  onSubmit: (values: GalleryFormInput) => Promise<void>;
  onCancel: () => void;
};

function toFormValues(
  item: GalleryDto | null,
  nextDisplayOrder: number,
): GalleryFormInput {
  return {
    title: item?.title ?? "",
    description: nullableText(item?.description),
    displayOrder: item?.displayOrder ?? nextDisplayOrder,
    featured: item?.featured ?? false,
    active: item?.active ?? true,
  };
}

export function GalleryForm({
  item,
  nextDisplayOrder,
  isSaving,
  isError,
  isSuccess,
  onSubmit,
  onCancel,
}: GalleryFormProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<GalleryFormInput>({
    resolver: zodResolver(galleryFormSchema),
    defaultValues: toFormValues(item, nextDisplayOrder),
  });

  useEffect(() => {
    reset(toFormValues(item, nextDisplayOrder));
  }, [item, nextDisplayOrder, reset]);

  return (
    <form
      className="space-y-6 border border-border bg-surface p-6"
      onSubmit={handleSubmit(onSubmit)}
      noValidate
    >
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="m-0 font-display text-2xl font-semibold">
            {item ? "Editar imagem" : "Criar item de galeria"}
          </h2>
          <p className="m-0 mt-1 text-sm text-[var(--muted-foreground)]">
            Metadados usados para organizar e publicar imagens na landing.
          </p>
        </div>
        <Button type="button" variant="ghost" size="sm" onClick={onCancel}>
          <X size={16} aria-hidden="true" />
          Fechar
        </Button>
      </div>

      <SaveFeedback isError={isError} isSuccess={isSuccess && !isDirty} />

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_180px]">
        <Field label="Título" error={errors.title}>
          <input className={inputClassName} {...register("title")} />
        </Field>
        <Field label="Ordem" error={errors.displayOrder}>
          <input
            type="number"
            min={0}
            className={inputClassName}
            {...register("displayOrder")}
          />
        </Field>
      </div>

      <Field label="Descrição" error={errors.description}>
        <textarea className={textareaClassName} {...register("description")} />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <label className="flex min-h-12 items-center gap-3 border border-border bg-background px-4 text-sm font-semibold">
          <input
            type="checkbox"
            className="size-4 accent-[var(--primary)]"
            {...register("active")}
          />
          Imagem ativa
        </label>
        <label className="flex min-h-12 items-center gap-3 border border-border bg-background px-4 text-sm font-semibold">
          <input
            type="checkbox"
            className="size-4 accent-[var(--primary)]"
            {...register("featured")}
          />
          Imagem destaque
        </label>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="submit" disabled={isSaving || (!isDirty && Boolean(item))}>
          <Save size={16} aria-hidden="true" />
          {isSaving ? "A guardar" : "Guardar imagem"}
        </Button>
      </div>
    </form>
  );
}
