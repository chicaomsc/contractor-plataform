import { getPublicEnv } from "@/lib/env/public-env";
import { ApiError, type ApiErrorBody } from "./errors";

const DEFAULT_TIMEOUT_MS = 8000;

type RequestOptions = RequestInit & {
  timeoutMs?: number;
};

function composeAbortSignal(
  timeoutSignal: AbortSignal,
  requestSignal?: AbortSignal | null,
) {
  if (!requestSignal) {
    return timeoutSignal;
  }

  if (requestSignal.aborted) {
    return requestSignal;
  }

  const controller = new AbortController();
  const abort = () => controller.abort();
  timeoutSignal.addEventListener("abort", abort, { once: true });
  requestSignal.addEventListener("abort", abort, { once: true });
  return controller.signal;
}

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
  const timeoutController = new AbortController();
  const timeout = setTimeout(
    () => timeoutController.abort(),
    options.timeoutMs ?? DEFAULT_TIMEOUT_MS,
  );
  const signal = composeAbortSignal(timeoutController.signal, options.signal);

  try {
    const response = await fetch(new URL(path, env.NEXT_PUBLIC_API_BASE_URL), {
      ...options,
      headers: {
        Accept: "application/json",
        ...options.headers,
      },
      signal,
    });

    if (!response.ok) {
      const body = await parseErrorBody(response);
      throw new ApiError(
        response.status === 404
          ? "Não encontrámos este recurso público."
          : "Não foi possível carregar os dados públicos.",
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
      throw new ApiError(
        "A API demorou demasiado a responder.",
        408,
        null,
        "timeout",
      );
    }

    throw new ApiError(
      "A API está indisponível neste momento.",
      0,
      null,
      "network",
    );
  } finally {
    clearTimeout(timeout);
  }
}
