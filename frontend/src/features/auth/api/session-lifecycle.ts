import type { AuthResponse } from "../types/auth";
import { clearAuthSession } from "./auth-storage";

export const AUTH_SESSION_REFRESHED_EVENT = "contractor:auth-session-refreshed";
export const SESSION_EXPIRED_EVENT = "contractor:session-expired";

let sessionExpired = false;

type SessionRefreshedEvent = CustomEvent<AuthResponse>;

function isBrowser() {
  return typeof window !== "undefined";
}

export function markSessionActive() {
  sessionExpired = false;
}

export function hasSessionExpired() {
  return sessionExpired;
}

export function notifySessionRefreshed(auth: AuthResponse) {
  markSessionActive();

  if (!isBrowser()) {
    return;
  }

  window.dispatchEvent(
    new CustomEvent(AUTH_SESSION_REFRESHED_EVENT, { detail: auth }),
  );
}

export function expireSession() {
  if (sessionExpired) {
    return false;
  }

  sessionExpired = true;
  clearAuthSession();

  if (isBrowser()) {
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
  }

  return true;
}

export function subscribeToSessionRefresh(
  listener: (auth: AuthResponse) => void,
) {
  if (!isBrowser()) {
    return () => undefined;
  }

  const handler = (event: Event) => {
    listener((event as SessionRefreshedEvent).detail);
  };

  window.addEventListener(AUTH_SESSION_REFRESHED_EVENT, handler);
  return () =>
    window.removeEventListener(AUTH_SESSION_REFRESHED_EVENT, handler);
}

export function subscribeToSessionExpired(listener: () => void) {
  if (!isBrowser()) {
    return () => undefined;
  }

  window.addEventListener(SESSION_EXPIRED_EVENT, listener);
  return () => window.removeEventListener(SESSION_EXPIRED_EVENT, listener);
}
