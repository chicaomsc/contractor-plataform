import { adminApiRequest } from "@/lib/api/admin-http-client";
import {
  authResponseSchema,
  forgotPasswordResponseSchema,
  meResponseSchema,
  resetPasswordResponseSchema,
  type AuthResponse,
  type ForgotPasswordRequest,
  type ForgotPasswordResponse,
  type LoginFormValues,
  type MeResponse,
  type ResetPasswordRequest,
  type ResetPasswordResponse,
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

export async function acceptInvite(request: {
  token: string;
  password: string;
}): Promise<AuthResponse> {
  const response = await adminApiRequest<unknown>("/auth/invites/accept", {
    method: "POST",
    body: JSON.stringify(request),
  });

  return authResponseSchema.parse(response);
}

export async function forgotPassword(
  request: ForgotPasswordRequest,
): Promise<ForgotPasswordResponse> {
  const response = await adminApiRequest<unknown>("/auth/password/forgot", {
    method: "POST",
    body: JSON.stringify(request),
  });

  return forgotPasswordResponseSchema.parse(response);
}

export async function resetPassword(
  request: ResetPasswordRequest,
): Promise<ResetPasswordResponse> {
  const response = await adminApiRequest<unknown>("/auth/password/reset", {
    method: "POST",
    body: JSON.stringify(request),
  });

  return resetPasswordResponseSchema.parse(response);
}
