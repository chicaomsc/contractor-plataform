"use client";

import { ArrowDown, ArrowUp, Pencil, Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import {
  materialFormSchema,
  estimateUnitLabels,
  estimateUnitValues,
  type MaterialFormInput,
} from "../../types/estimates";
import { zodResolver } from "../../utils/zod-resolver";
import { Field, inputClassName } from "../FormControls";

const emptyMaterial: MaterialFormInput = {
  name: "",
  description: "",
  quantity: 1,
  unit: "UNIT",
  unitPrice: 0,
};

type MaterialsEditorProps = {
  materials: MaterialFormInput[];
  onChange: (materials: MaterialFormInput[]) => void;
};

function MaterialForm({
  initialValues,
  onSave,
  onCancel,
}: {
  initialValues: MaterialFormInput;
  onSave: (values: MaterialFormInput) => void;
  onCancel: () => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<MaterialFormInput>({
    resolver: zodResolver(materialFormSchema),
    defaultValues: initialValues,
  });

  return (
    <form
      className="space-y-4 border border-border bg-background p-4"
      onSubmit={handleSubmit(onSave)}
      noValidate
    >
      <Field label="Nome" error={errors.name}>
        <input className={inputClassName} {...register("name")} />
      </Field>

      <Field label="Descrição" error={errors.description}>
        <input className={inputClassName} {...register("description")} />
      </Field>

      <div className="grid gap-4 sm:grid-cols-3">
        <Field label="Quantidade" error={errors.quantity}>
          <input
            type="number"
            step="any"
            min={0}
            className={inputClassName}
            {...register("quantity")}
          />
        </Field>
        <Field label="Unidade" error={errors.unit}>
          <select className={inputClassName} {...register("unit")}>
            {estimateUnitValues.map((unit) => (
              <option key={unit} value={unit}>
                {estimateUnitLabels[unit]}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Preço unitário" error={errors.unitPrice}>
          <input
            type="number"
            step="any"
            min={0}
            className={inputClassName}
            {...register("unitPrice")}
          />
        </Field>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
        <Button type="button" variant="secondary" size="sm" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="submit" size="sm">
          Guardar material
        </Button>
      </div>
    </form>
  );
}

export function MaterialsEditor({ materials, onChange }: MaterialsEditorProps) {
  const [editingIndex, setEditingIndex] = useState<number | "new" | null>(null);

  function saveMaterial(values: MaterialFormInput) {
    if (editingIndex === "new") {
      onChange([...materials, values]);
    } else if (typeof editingIndex === "number") {
      onChange(
        materials.map((material, index) =>
          index === editingIndex ? values : material,
        ),
      );
    }
    setEditingIndex(null);
  }

  function removeMaterial(index: number) {
    onChange(materials.filter((_, i) => i !== index));
  }

  function moveMaterial(index: number, direction: "up" | "down") {
    const targetIndex = direction === "up" ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= materials.length) {
      return;
    }
    const next = [...materials];
    [next[index], next[targetIndex]] = [next[targetIndex], next[index]];
    onChange(next);
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="m-0 font-display text-lg font-semibold">Materiais</h3>
        {editingIndex === null ? (
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={() => setEditingIndex("new")}
          >
            <Plus size={16} aria-hidden="true" />
            Adicionar material
          </Button>
        ) : null}
      </div>

      {materials.length === 0 && editingIndex === null ? (
        <p className="m-0 border border-border bg-background p-4 text-sm text-[var(--muted-foreground)]">
          Nenhum material adicionado. Opcional — nem todo orçamento precisa de materiais.
        </p>
      ) : (
        <ul className="m-0 list-none space-y-2 p-0">
          {materials.map((material, index) =>
            editingIndex === index ? (
              <li key={index}>
                <MaterialForm
                  initialValues={material}
                  onSave={saveMaterial}
                  onCancel={() => setEditingIndex(null)}
                />
              </li>
            ) : (
              <li
                key={index}
                className="flex flex-col gap-3 border border-border bg-surface p-4 sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="min-w-0">
                  <p className="m-0 truncate font-semibold">{material.name}</p>
                  <p className="m-0 mt-1 text-sm text-[var(--muted-foreground)]">
                    {material.quantity} {estimateUnitLabels[material.unit]} ·{" "}
                    {material.unitPrice.toFixed(2)} / unidade
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-1">
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    aria-label="Mover para cima"
                    disabled={index === 0}
                    onClick={() => moveMaterial(index, "up")}
                  >
                    <ArrowUp size={16} aria-hidden="true" />
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    aria-label="Mover para baixo"
                    disabled={index === materials.length - 1}
                    onClick={() => moveMaterial(index, "down")}
                  >
                    <ArrowDown size={16} aria-hidden="true" />
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    aria-label="Editar material"
                    onClick={() => setEditingIndex(index)}
                  >
                    <Pencil size={16} aria-hidden="true" />
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    aria-label="Excluir material"
                    onClick={() => removeMaterial(index)}
                  >
                    <Trash2 size={16} aria-hidden="true" />
                  </Button>
                </div>
              </li>
            ),
          )}
          {editingIndex === "new" ? (
            <li>
              <MaterialForm
                initialValues={emptyMaterial}
                onSave={saveMaterial}
                onCancel={() => setEditingIndex(null)}
              />
            </li>
          ) : null}
        </ul>
      )}
    </div>
  );
}
