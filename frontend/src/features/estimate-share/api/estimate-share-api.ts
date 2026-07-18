import { ApiError } from "@/lib/api/errors";
import { apiRequest } from "@/lib/api/http-client";
import { getPublicEnv } from "@/lib/env/public-env";
import { publicEstimateShareDtoSchema, type PublicEstimateShareDto } from "../types/api";

export async function fetchPublicEstimateShare(
  token: string,
  options: { signal?: AbortSignal } = {},
): Promise<PublicEstimateShareDto> {
  const data = await apiRequest<unknown>(`/public/share/${encodeURIComponent(token)}`, {
    signal: options.signal,
  });

  const result = publicEstimateShareDtoSchema.safeParse(data);
  if (!result.success) {
    throw new ApiError("A resposta da API não tem o formato esperado.", 0, null, "invalid-response");
  }

  return result.data;
}

export function buildPublicEstimateSharePdfUrl(token: string): string {
  const env = getPublicEnv();
  return new URL(`/public/share/${encodeURIComponent(token)}/pdf`, env.NEXT_PUBLIC_API_BASE_URL).toString();
}
