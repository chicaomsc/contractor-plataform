import type { AuthResponse } from "../types/auth";

const ACCESS_TOKEN_KEY = "contractor.accessToken";
const REFRESH_TOKEN_KEY = "contractor.refreshToken";
const USER_KEY = "contractor.user";
const COMPANY_KEY = "contractor.company";
const SESSION_COOKIE = "contractor_session";

function isBrowser() {
  return typeof window !== "undefined";
}

export function getAccessToken() {
  if (!isBrowser()) {
    return null;
  }

  return window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken() {
  if (!isBrowser()) {
    return null;
  }

  return window.localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function persistAuthSession(auth: AuthResponse) {
  if (!isBrowser()) {
    return;
  }

  window.localStorage.setItem(ACCESS_TOKEN_KEY, auth.accessToken);
  window.localStorage.setItem(REFRESH_TOKEN_KEY, auth.refreshToken);
  window.localStorage.setItem(USER_KEY, JSON.stringify(auth.user));
  window.localStorage.setItem(COMPANY_KEY, JSON.stringify(auth.company));
  document.cookie = `${SESSION_COOKIE}=active; path=/; SameSite=Lax`;
}

export function clearAuthSession() {
  if (!isBrowser()) {
    return;
  }

  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
  window.localStorage.removeItem(USER_KEY);
  window.localStorage.removeItem(COMPANY_KEY);
  document.cookie = `${SESSION_COOKIE}=; path=/; max-age=0; SameSite=Lax`;
}
