// REST callers already include /api; the base is an origin, not an API prefix.
export function apiBaseUrl(env = import.meta.env) {
  return env.DEV ? '/' : (env.VITE_API_BASE_URL || '/');
}

export function socketUrl(env = import.meta.env, origin = window.location.origin) {
  return new URL(env.VITE_WS_URL || '/ws', origin).href;
}

// Proxy paths supply /api, /uploads and /ws themselves.
export function proxyOrigin(value, fallback = 'http://localhost:8080') {
  try { return new URL(value || fallback).origin; }
  catch { return new URL(fallback).origin; }
}
