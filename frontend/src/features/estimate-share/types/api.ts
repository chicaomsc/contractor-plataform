import { z } from "zod";

const nullableString = z.string().nullable();

// io.chicaodw.platform.estimate.api.dto.PublicEstimateShareResponse — every field here is
// already display-ready (money/date/percentage formatted server-side). Never recompute.

export const publicEstimateShareSellerDtoSchema = z.object({
  displayName: nullableString,
  logoUrl: nullableString,
  phone: nullableString,
  email: nullableString,
  website: nullableString,
  addressLine: nullableString,
});

export const publicEstimateShareEstimateInfoDtoSchema = z.object({
  number: z.string(),
  title: z.string(),
  description: nullableString,
  status: z.string(),
  draft: z.boolean(),
  cancelled: z.boolean(),
  issueDate: nullableString,
  validUntil: nullableString,
});

export const publicEstimateShareCustomerDtoSchema = z.object({
  name: nullableString,
});

export const publicEstimateShareLineItemDtoSchema = z.object({
  description: z.string(),
  quantity: z.string(),
  unit: z.string(),
  unitPrice: z.string(),
  total: z.string(),
});

export const publicEstimateShareSummaryDtoSchema = z.object({
  currency: z.string(),
  laborSubtotal: z.string(),
  materialSubtotal: z.string(),
  subtotal: z.string(),
  vatLabel: z.string(),
  vatAmount: z.string(),
  total: z.string(),
  upfrontLabel: z.string(),
  upfrontAmount: z.string(),
  remaining: z.string(),
});

export const publicEstimateShareDtoSchema = z.object({
  seller: publicEstimateShareSellerDtoSchema,
  estimate: publicEstimateShareEstimateInfoDtoSchema,
  customer: publicEstimateShareCustomerDtoSchema,
  items: z.array(publicEstimateShareLineItemDtoSchema),
  materials: z.array(publicEstimateShareLineItemDtoSchema),
  summary: publicEstimateShareSummaryDtoSchema,
  notes: nullableString,
  terms: nullableString,
});

export type PublicEstimateShareDto = z.infer<typeof publicEstimateShareDtoSchema>;
export type PublicEstimateShareLineItemDto = z.infer<typeof publicEstimateShareLineItemDtoSchema>;
