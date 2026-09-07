import { getPublicEnv } from "@/lib/env/public-env";
import {
  expireSession,
  hasSessionExpired,
  notifySessionRefreshed,
} from "@/features/auth/api/session-lifecycle";
import {
  getRefreshToken,
  persistAuthSession,
} from "@/features/auth/api/auth-storage";
import type { AuthResponse } from "@/features/auth/types/auth";
import { withApiPrefix } from "./api-path";
import { ApiError, type ApiErrorBody } from "./errors";

const DEFAULT_TIMEOUT_MS = 8000;
const AUTH_REFRESH_PATH = "/auth/refresh";
const PUBLIC_AUTH_PATHS = new Set([
  "/auth/login",
  AUTH_REFRESH_PATH,
  "/auth/password/forgot",
  "/auth/password/reset",
  "/auth/invites/accept",
]);

let refreshPromise: Promise<AuthResponse> | null = null;

type AdminRequestOptions = RequestInit & {
  accessToken?: string | null;
  timeoutMs?: number;
  skipAuthRefresh?: boolean;
  hasRetriedAfterRefresh?: boolean;
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

function createSessionExpiredError() {
  return new ApiError("Sessão expirada. Inicie sessão novamente.", 401);
}

function getErrorMessage(status: number) {
  return status === 401
    ? "Sessão expirada. Inicie sessão novamente."
    : "Não foi possível concluir a operação.";
}

function isAuthResponse(value: unknown): value is AuthResponse {
  if (!value || typeof value !== "object") {
    return false;
  }

  const candidate = value as Partial<AuthResponse>;
  return (
    typeof candidate.accessToken === "string" &&
    typeof candidate.refreshToken === "string" &&
    Boolean(candidate.user) &&
    "company" in candidate
  );
}

function shouldSkipAuthRefresh(path: string, options: AdminRequestOptions) {
  return (
    options.skipAuthRefresh === true ||
    PUBLIC_AUTH_PATHS.has(path) ||
    !options.accessToken
  );
}

async function fetchRefreshSession(
  refreshToken: string,
): Promise<AuthResponse> {
  const response = await adminApiRequest<unknown>(AUTH_REFRESH_PATH, {
    method: "POST",
    body: JSON.stringify({ refreshToken }),
    skipAuthRefresh: true,
  });

  if (!isAuthResponse(response)) {
    throw new ApiError(
      "Não foi possível renovar a sessão.",
      0,
      null,
      "invalid-response",
    );
  }

  persistAuthSession(response);
  notifySessionRefreshed(response);
  return response;
}

function refreshSessionSingleFlight(): Promise<AuthResponse> {
  if (hasSessionExpired()) {
    return Promise.reject(createSessionExpiredError());
  }

  if (refreshPromise) {
    return refreshPromise;
  }

  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    expireSession();
    return Promise.reject(createSessionExpiredError());
  }

  refreshPromise = fetchRefreshSession(refreshToken)
    .catch((error) => {
      expireSession();
      throw error instanceof ApiError ? error : createSessionExpiredError();
    })
    .finally(() => {
      refreshPromise = null;
    });

  return refreshPromise;
}

export type AdminBlobResult = {
  blob: Blob;
  filename: string | null;
};

function extractFilename(disposition: string | null): string | null {
  if (!disposition) {
    return null;
  }

  // RFC 6266: filename*=UTF-8''... takes precedence over the plain filename= fallback.
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match) {
    try {
      return decodeURIComponent(utf8Match[1]);
    } catch {
      // Malformed percent-encoding — fall through to the plain filename below.
    }
  }

  const plainMatch = disposition.match(/filename="?([^";]+)"?/i);
  return plainMatch ? plainMatch[1] : null;
}

/**
 * Like {@link adminApiRequest}, but for binary downloads (e.g. the estimate PDF): returns
 * the raw Blob plus the filename suggested by the server's Content-Disposition header,
 * instead of parsing the body as JSON.
 */
export async function adminApiRequestBlob(
  path: string,
  options: AdminRequestOptions = {},
): Promise<AdminBlobResult> {
  const env = getPublicEnv();
  const timeoutController = new AbortController();
  const timeout = setTimeout(
    () => timeoutController.abort(),
    options.timeoutMs ?? DEFAULT_TIMEOUT_MS,
  );
  const signal = composeAbortSignal(timeoutController.signal, options.signal);

  try {
    const headers = new Headers(options.headers);

    if (options.accessToken) {
      headers.set("Authorization", `Bearer ${options.accessToken}`);
    }

    const response = await fetch(
      new URL(withApiPrefix(path), env.NEXT_PUBLIC_API_BASE_URL),
      {
        ...options,
        headers,
        signal,
      },
    );

    if (
      response.status === 401 &&
      !options.hasRetriedAfterRefresh &&
      !shouldSkipAuthRefresh(path, options)
    ) {
      const auth = await refreshSessionSingleFlight();
      return adminApiRequestBlob(path, {
        ...options,
        accessToken: auth.accessToken,
        hasRetriedAfterRefresh: true,
      });
    }

    if (response.status === 401 && options.hasRetriedAfterRefresh) {
      expireSession();
    }

    if (!response.ok) {
      const body = await parseErrorBody(response);
      throw new ApiError(
        getErrorMessage(response.status),
        response.status,
        body,
      );
    }

    const blob = await response.blob();
    return {
      blob,
      filename: extractFilename(response.headers.get("Content-Disposition")),
    };
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

export async function adminApiRequest<T>(
  path: string,
  options: AdminRequestOptions = {},
): Promise<T> {
  const env = getPublicEnv();
  const timeoutController = new AbortController();
  const timeout = setTimeout(
    () => timeoutController.abort(),
    options.timeoutMs ?? DEFAULT_TIMEOUT_MS,
  );
  const signal = composeAbortSignal(timeoutController.signal, options.signal);

  try {
    const headers = new Headers(options.headers);
    headers.set("Accept", "application/json");

    if (
      options.body &&
      !(options.body instanceof FormData) &&
      !headers.has("Content-Type")
    ) {
      headers.set("Content-Type", "application/json");
    }

    if (options.accessToken) {
      headers.set("Authorization", `Bearer ${options.accessToken}`);
    }

    const response = await fetch(
      new URL(withApiPrefix(path), env.NEXT_PUBLIC_API_BASE_URL),
      {
        ...options,
        headers,
        signal,
      },
    );

    if (
      response.status === 401 &&
      !options.hasRetriedAfterRefresh &&
      !shouldSkipAuthRefresh(path, options)
    ) {
      const auth = await refreshSessionSingleFlight();
      return adminApiRequest<T>(path, {
        ...options,
        accessToken: auth.accessToken,
        hasRetriedAfterRefresh: true,
      });
    }

    if (response.status === 401 && options.hasRetriedAfterRefresh) {
      expireSession();
    }

    if (!response.ok) {
      const body = await parseErrorBody(response);
      throw new ApiError(
        getErrorMessage(response.status),
        response.status,
        body,
      );
    }

    if (response.status === 204) {
      return undefined as T;
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
