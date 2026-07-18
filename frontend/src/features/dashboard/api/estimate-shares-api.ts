import { adminApiRequest } from "@/lib/api/admin-http-client";
import {
  estimateShareDtoSchema,
  type CreateEstimateShareInput,
  type EstimateShareDto,
} from "../types/estimate-shares";

export async function createEstimateShare(
  accessToken: string,
  estimateId: string,
  payload: CreateEstimateShareInput = {},
): Promise<EstimateShareDto> {
  const response = await adminApiRequest<unknown>(`/estimates/${estimateId}/share`, {
    method: "POST",
    accessToken,
    body: JSON.stringify(payload),
  });
  return estimateShareDtoSchema.parse(response);
}

export async function fetchEstimateShare(
  accessToken: string,
  estimateId: string,
): Promise<EstimateShareDto> {
  const response = await adminApiRequest<unknown>(`/estimates/${estimateId}/share`, {
    accessToken,
  });
  return estimateShareDtoSchema.parse(response);
}

export async function revokeEstimateShare(
  accessToken: string,
  estimateId: string,
): Promise<void> {
  await adminApiRequest<void>(`/estimates/${estimateId}/share`, {
    method: "DELETE",
    accessToken,
  });
}
