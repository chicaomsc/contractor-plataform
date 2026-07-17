import { z } from "zod";

const nullableString = z.string().nullable();
const nullableNumber = z.number().nullable();
const emptyToNull = (value: unknown) => (value === "" ? null : value);
const nullableTextInput = (max: number) =>
  z.preprocess(emptyToNull, z.string().max(max).nullable());

// ── Enums (mirror io.chicaodw.platform.estimate.domain) ────────────────────

export const estimateStatusValues = [
  "DRAFT",
  "SENT",
  "APPROVED",
  "REJECTED",
  "EXPIRED",
  "CANCELLED",
  "COMPLETED",
] as const;
export const estimateStatusSchema = z.enum(estimateStatusValues);
export type EstimateStatus = z.infer<typeof estimateStatusSchema>;

export const estimateUnitValues = [
  "UNIT",
  "HOUR",
  "DAY",
  "M2",
  "M3",
  "LINEAR_METER",
  "FIXED",
] as const;
export const estimateUnitSchema = z.enum(estimateUnitValues);
export type EstimateUnit = z.infer<typeof estimateUnitSchema>;

export const estimateUnitLabels: Record<EstimateUnit, string> = {
  UNIT: "Unidade",
  HOUR: "Hora",
  DAY: "Dia",
  M2: "m²",
  M3: "m³",
  LINEAR_METER: "Metro linear",
  FIXED: "Fixo",
};

export const estimateStatusLabels: Record<EstimateStatus, string> = {
  DRAFT: "Rascunho",
  SENT: "Enviado",
  APPROVED: "Aprovado",
  REJECTED: "Rejeitado",
  EXPIRED: "Expirado",
  CANCELLED: "Cancelado",
  COMPLETED: "Concluído",
};

// ── Response DTOs (io.chicaodw.platform.estimate.api.dto) ──────────────────
// All financial fields (quantity/unitPrice/total/subtotal/vatAmount/...) are calculated
// exclusively by the backend. The frontend only ever displays them — never recomputes.

export const estimateItemDtoSchema = z.object({
  id: z.string(),
  serviceId: z.string().nullable(),
  description: z.string(),
  quantity: z.number(),
  unit: estimateUnitSchema,
  unitPrice: z.number(),
  total: z.number(),
  displayOrder: z.number(),
});

export const materialDtoSchema = z.object({
  id: z.string(),
  name: z.string(),
  description: nullableString,
  quantity: z.number(),
  unit: estimateUnitSchema,
  unitPrice: z.number(),
  total: z.number(),
  displayOrder: z.number(),
});

export const estimateDtoSchema = z.object({
  id: z.string(),
  companyId: z.string(),
  customerId: z.string(),
  number: z.string(),
  title: z.string(),
  description: nullableString,
  status: estimateStatusSchema,
  issueDate: z.string(),
  validUntil: nullableString,
  expectedStartDate: nullableString,
  estimatedDurationDays: nullableNumber,
  notes: nullableString,
  terms: nullableString,
  currency: z.string(),
  vatRate: z.number(),
  laborSubtotal: z.number(),
  materialSubtotal: z.number(),
  subtotal: z.number(),
  vatAmount: z.number(),
  total: z.number(),
  upfrontPercentage: z.number(),
  upfrontAmount: z.number(),
  remainingAmount: z.number(),
  items: z.array(estimateItemDtoSchema),
  materials: z.array(materialDtoSchema),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export const estimateSummaryDtoSchema = z.object({
  id: z.string(),
  companyId: z.string(),
  customerId: z.string(),
  number: z.string(),
  title: z.string(),
  status: estimateStatusSchema,
  issueDate: z.string(),
  validUntil: nullableString,
  currency: z.string(),
  total: z.number(),
  upfrontAmount: z.number(),
  remainingAmount: z.number(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export const estimatesSummaryDtoSchema = z.array(estimateSummaryDtoSchema);

export type EstimateItemDto = z.infer<typeof estimateItemDtoSchema>;
export type MaterialDto = z.infer<typeof materialDtoSchema>;
export type EstimateDto = z.infer<typeof estimateDtoSchema>;
export type EstimateSummaryDto = z.infer<typeof estimateSummaryDtoSchema>;

// ── Request/form schemas (io.chicaodw.platform.estimate.api.dto) ───────────
// quantity/unitPrice are the only numeric inputs the client provides for a line —
// `total` is never sent; the backend always computes and returns it.

export const estimateItemFormSchema = z.object({
  serviceId: z.string().nullable(),
  description: z.string().min(1, "Indique a descrição do item."),
  quantity: z.coerce.number().positive("A quantidade deve ser maior que zero."),
  unit: estimateUnitSchema,
  unitPrice: z.coerce.number().min(0, "O preço não pode ser negativo."),
});
export type EstimateItemFormInput = z.infer<typeof estimateItemFormSchema>;

export const materialFormSchema = z.object({
  name: z.string().min(1, "Indique o nome do material.").max(255),
  description: nullableTextInput(2000),
  quantity: z.coerce.number().positive("A quantidade deve ser maior que zero."),
  unit: estimateUnitSchema,
  unitPrice: z.coerce.number().min(0, "O preço não pode ser negativo."),
});
export type MaterialFormInput = z.infer<typeof materialFormSchema>;

export const estimateGeneralInfoSchema = z.object({
  title: z.string().min(1, "Indique o título do orçamento.").max(255),
  description: nullableTextInput(4000),
  validUntil: nullableTextInput(20),
  notes: nullableTextInput(4000),
  terms: nullableTextInput(4000),
});
export type EstimateGeneralInfoInput = z.infer<typeof estimateGeneralInfoSchema>;

/** Assembled client-side, sent as-is to POST /estimates. Never includes computed totals. */
export type CreateEstimateInput = EstimateGeneralInfoInput & {
  customerId: string;
  items: EstimateItemFormInput[];
  materials: MaterialFormInput[];
};

/** Sent as-is to PUT /estimates/{id}. `items`/`materials` fully replace the existing collections. */
export type UpdateEstimateInput = EstimateGeneralInfoInput & {
  customerId: string;
  items: EstimateItemFormInput[];
  materials: MaterialFormInput[];
};
