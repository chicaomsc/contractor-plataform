"use client";

import { ArrowDown, ArrowUp, Pencil, Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import {
  estimateItemFormSchema,
  estimateUnitLabels,
  estimateUnitValues,
  type EstimateItemFormInput,
} from "../../types/estimates";
import { zodResolver } from "../../utils/zod-resolver";
import { Field, inputClassName } from "../FormControls";

const emptyItem: EstimateItemFormInput = {
  serviceId: null,
  description: "",
  quantity: 1,
  unit: "UNIT",
  unitPrice: 0,
};

type EstimateItemsEditorProps = {
  items: EstimateItemFormInput[];
  onChange: (items: EstimateItemFormInput[]) => void;
};

function ItemForm({
  initialValues,
  onSave,
  onCancel,
}: {
  initialValues: EstimateItemFormInput;
  onSave: (values: EstimateItemFormInput) => void;
  onCancel: () => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<EstimateItemFormInput>({
    resolver: zodResolver(estimateItemFormSchema),
    defaultValues: initialValues,
  });

  return (
    <form
      className="space-y-4 border border-border bg-background p-4"
      onSubmit={handleSubmit(onSave)}
      noValidate
    >
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
          Guardar item
        </Button>
      </div>
    </form>
  );
}

export function EstimateItemsEditor({ items, onChange }: EstimateItemsEditorProps) {
  const [editingIndex, setEditingIndex] = useState<number | "new" | null>(null);

  function saveItem(values: EstimateItemFormInput) {
    if (editingIndex === "new") {
      onChange([...items, values]);
    } else if (typeof editingIndex === "number") {
      onChange(items.map((item, index) => (index === editingIndex ? values : item)));
    }
    setEditingIndex(null);
  }

  function removeItem(index: number) {
    onChange(items.filter((_, i) => i !== index));
  }

  function moveItem(index: number, direction: "up" | "down") {
    const targetIndex = direction === "up" ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= items.length) {
      return;
    }
    const next = [...items];
    [next[index], next[targetIndex]] = [next[targetIndex], next[index]];
    onChange(next);
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="m-0 font-display text-lg font-semibold">
          Itens de mão de obra / serviço
        </h3>
        {editingIndex === null ? (
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={() => setEditingIndex("new")}
          >
            <Plus size={16} aria-hidden="true" />
            Adicionar item
          </Button>
        ) : null}
      </div>

      {items.length === 0 && editingIndex === null ? (
        <p className="m-0 border border-border bg-background p-4 text-sm text-[var(--muted-foreground)]">
          Nenhum item adicionado. Um orçamento pode ter apenas materiais, mas normalmente
          inclui também itens de mão de obra.
        </p>
      ) : (
        <ul className="m-0 list-none space-y-2 p-0">
          {items.map((item, index) =>
            editingIndex === index ? (
              <li key={index}>
                <ItemForm
                  initialValues={item}
                  onSave={saveItem}
                  onCancel={() => setEditingIndex(null)}
                />
              </li>
            ) : (
              <li
                key={index}
                className="flex flex-col gap-3 border border-border bg-surface p-4 sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="min-w-0">
                  <p className="m-0 truncate font-semibold">{item.description}</p>
                  <p className="m-0 mt-1 text-sm text-[var(--muted-foreground)]">
                    {item.quantity} {estimateUnitLabels[item.unit]} ·{" "}
                    {item.unitPrice.toFixed(2)} / unidade
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-1">
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    aria-label="Mover para cima"
                    disabled={index === 0}
                    onClick={() => moveItem(index, "up")}
                  >
                    <ArrowUp size={16} aria-hidden="true" />
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    aria-label="Mover para baixo"
                    disabled={index === items.length - 1}
                    onClick={() => moveItem(index, "down")}
                  >
                    <ArrowDown size={16} aria-hidden="true" />
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    aria-label="Editar item"
                    onClick={() => setEditingIndex(index)}
                  >
                    <Pencil size={16} aria-hidden="true" />
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    aria-label="Excluir item"
                    onClick={() => removeItem(index)}
                  >
                    <Trash2 size={16} aria-hidden="true" />
                  </Button>
                </div>
              </li>
            ),
          )}
          {editingIndex === "new" ? (
            <li>
              <ItemForm
                initialValues={emptyItem}
                onSave={saveItem}
                onCancel={() => setEditingIndex(null)}
              />
            </li>
          ) : null}
        </ul>
      )}
    </div>
  );
}
