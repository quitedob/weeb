const uploadedAvatar = /^\/uploads\/avatars\/[0-9a-f-]{36}\.png$/i;

// Development serves these paths through Vite's upload proxy. Production may use a separate API origin.
export function resolveAssetUrl(value) {
  if (typeof value !== 'string' || !uploadedAvatar.test(value) || import.meta.env.DEV) return value;
  const apiBase = import.meta.env.VITE_API_BASE_URL || '/';
  return new URL(value, new URL(apiBase, window.location.origin)).href;
}

// Only the server-owned avatar field is a URL contract; message/article text stays untouched.
export function resolveAvatarUrls(value) {
  if (Array.isArray(value)) return value.map(resolveAvatarUrls);
  if (value === null || typeof value !== 'object') return value;
  return Object.fromEntries(Object.entries(value).map(([key, child]) => [
    key, key === 'avatar' && typeof child === 'string' ? resolveAssetUrl(child) : resolveAvatarUrls(child)
  ]));
}
