import type { Control } from "react-hook-form";
import type { UpdateBrandingInput } from "../../types/admin";
import { ColorField } from "./ColorField";

type BrandColorFieldsProps = {
  control: Control<UpdateBrandingInput>;
};

export function BrandColorFields({ control }: BrandColorFieldsProps) {
  return (
    <div className="grid gap-5 lg:col-span-3 lg:grid-cols-3">
      <ColorField
        control={control}
        name="primaryColor"
        label="Cor primária"
        triggerLabel="Abrir seletor visual da cor principal"
        expandLabel="Expandir opções da cor principal"
        fallback="#1C1C1A"
        placeholder="#1E40AF"
      />
      <ColorField
        control={control}
        name="secondaryColor"
        label="Cor secundária"
        triggerLabel="Abrir seletor visual da cor auxiliar"
        expandLabel="Expandir opções da cor auxiliar"
        fallback="#3B82F6"
        placeholder="#3B82F6"
      />
      <ColorField
        control={control}
        name="accentColor"
        label="Cor de acento"
        triggerLabel="Abrir seletor visual da cor de destaque"
        expandLabel="Expandir opções da cor de destaque"
        fallback="#B43F08"
        placeholder="#F59E0B"
      />
    </div>
  );
}
