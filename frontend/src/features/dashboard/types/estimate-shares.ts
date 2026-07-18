import { z } from "zod";

export const estimateShareStatusValues = ["ACTIVE", "EXPIRED", "REVOKED"] as const;
export const estimateShareStatusSchema = z.enum(estimateShareStatusValues);
export type EstimateShareStatus = z.infer<typeof estimateShareStatusSchema>;

// io.chicaodw.platform.estimate.api.dto.EstimateShareResponse — `token` is only ever
// populated immediately after POST /estimates/{id}/share: only its hash is persisted
// server-side, so it cannot be retrieved again on a later GET.
export const estimateShareDtoSchema = z.object({
  id: z.string(),
  status: estimateShareStatusSchema,
  token: z.string().nullable(),
  createdAt: z.string(),
  expiresAt: z.string(),
  revokedAt: z.string().nullable(),
  lastAccessAt: z.string().nullable(),
  accessCount: z.number(),
});
export type EstimateShareDto = z.infer<typeof estimateShareDtoSchema>;

export type CreateEstimateShareInput = {
  expiresInDays?: number | null;
};
