import { getPublicEnv } from "@/lib/env/public-env";
import { ApiError, type ApiErrorBody } from "./errors";

const DEFAULT_TIMEOUT_MS = 8000;

type RequestOptions = RequestInit & {
  timeoutMs?: number;
};

async function parseErrorBody(
  response: Response,
): Promise<ApiErrorBody | null> {
  const contentType = response.headers.get("content-type");
  if (!contentType?.includes("application/json")) {
    return null;
  }

  try {
    return (await response.json()) as ApiErrorBody;
  } catch {
    return null;
  }
}

export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const env = getPublicEnv();
  const controller = new AbortController();
  const timeout = setTimeout(
    () => controller.abort(),
    options.timeoutMs ?? DEFAULT_TIMEOUT_MS,
  );

  try {
    const response = await fetch(new URL(path, env.NEXT_PUBLIC_API_BASE_URL), {
      ...options,
      headers: {
        Accept: "application/json",
        ...options.headers,
      },
      signal: controller.signal,
    });

    if (!response.ok) {
      const body = await parseErrorBody(response);
      throw new ApiError(
        body?.detail ?? body?.title ?? "Não foi possível carregar os dados.",
        response.status,
        body,
      );
    }

    return (await response.json()) as T;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }

    if (error instanceof DOMException && error.name === "AbortError") {
      throw new ApiError("A API demorou demasiado a responder.", 408);
    }

    throw new ApiError("A API está indisponível neste momento.", 0);
  } finally {
    clearTimeout(timeout);
  }
}
