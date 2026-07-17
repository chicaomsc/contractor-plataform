import { adminApiRequest } from "@/lib/api/admin-http-client";
import {
  estimateDtoSchema,
  estimatesSummaryDtoSchema,
  type CreateEstimateInput,
  type EstimateDto,
  type EstimateStatus,
  type EstimateSummaryDto,
  type UpdateEstimateInput,
} from "../types/estimates";

export type EstimateFilters = {
  status?: EstimateStatus | null;
  customerId?: string | null;
};

function buildEstimatesPath(filters: EstimateFilters = {}): string {
  const params = new URLSearchParams();
  if (filters.status) params.set("status", filters.status);
  if (filters.customerId) params.set("customerId", filters.customerId);
  const query = params.toString();
  return query ? `/estimates?${query}` : "/estimates";
}

export async function fetchEstimates(
  accessToken: string,
  filters: EstimateFilters = {},
): Promise<EstimateSummaryDto[]> {
  const response = await adminApiRequest<unknown>(buildEstimatesPath(filters), {
    accessToken,
  });
  return estimatesSummaryDtoSchema.parse(response);
}

export async function fetchEstimate(
  accessToken: string,
  estimateId: string,
): Promise<EstimateDto> {
  const response = await adminApiRequest<unknown>(`/estimates/${estimateId}`, {
    accessToken,
  });
  return estimateDtoSchema.parse(response);
}

export async function createEstimate(
  accessToken: string,
  payload: CreateEstimateInput,
): Promise<EstimateDto> {
  const response = await adminApiRequest<unknown>("/estimates", {
    method: "POST",
    accessToken,
    body: JSON.stringify(payload),
  });
  return estimateDtoSchema.parse(response);
}

export async function updateEstimate(
  accessToken: string,
  estimateId: string,
  payload: UpdateEstimateInput,
): Promise<EstimateDto> {
  const response = await adminApiRequest<unknown>(`/estimates/${estimateId}`, {
    method: "PUT",
    accessToken,
    body: JSON.stringify(payload),
  });
  return estimateDtoSchema.parse(response);
}

export async function deleteEstimate(
  accessToken: string,
  estimateId: string,
): Promise<void> {
  await adminApiRequest<void>(`/estimates/${estimateId}`, {
    method: "DELETE",
    accessToken,
  });
}

export async function changeEstimateStatus(
  accessToken: string,
  estimateId: string,
  status: EstimateStatus,
): Promise<EstimateDto> {
  const response = await adminApiRequest<unknown>(
    `/estimates/${estimateId}/status`,
    {
      method: "PATCH",
      accessToken,
      body: JSON.stringify({ status }),
    },
  );
  return estimateDtoSchema.parse(response);
}
