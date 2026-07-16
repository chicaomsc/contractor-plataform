import { adminApiRequest } from "@/lib/api/admin-http-client";
import {
  authResponseSchema,
  meResponseSchema,
  type AuthResponse,
  type LoginFormValues,
  type MeResponse,
} from "../types/auth";

export async function login(request: LoginFormValues): Promise<AuthResponse> {
  const response = await adminApiRequest<unknown>("/auth/login", {
    method: "POST",
    body: JSON.stringify(request),
  });

  return authResponseSchema.parse(response);
}

export async function refresh(refreshToken: string): Promise<AuthResponse> {
  const response = await adminApiRequest<unknown>("/auth/refresh", {
    method: "POST",
    body: JSON.stringify({ refreshToken }),
  });

  return authResponseSchema.parse(response);
}

export async function me(accessToken: string): Promise<MeResponse> {
  const response = await adminApiRequest<unknown>("/auth/me", {
    accessToken,
  });

  return meResponseSchema.parse(response);
}
