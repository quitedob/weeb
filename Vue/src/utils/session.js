import { useAuthStore } from '@/stores/authStore'

const renewalLockName = 'weeb-auth-renewal'

export function withRenewalLock(operation) {
  // A localStorage lease cannot atomically elect one renewing tab. Browsers without
  // Web Locks keep their existing token until normal expiry instead of racing rotation.
  if (typeof globalThis.navigator?.locks?.request !== 'function') return Promise.resolve(null)
  return globalThis.navigator.locks.request(renewalLockName, operation)
}

export async function waitForCredentialRenewal(session) {
  if (session?.token && isCurrentCredential(session)) await withRenewalLock(() => {})
}

export function captureSession() {
  const auth = useAuthStore()
  return { epoch: auth.sessionEpoch, token: auth.accessToken ?? null, storedToken: localStorage.getItem('jwt_token') }
}

export function isCurrentSession(session) {
  const auth = useAuthStore()
  const storedToken = localStorage.getItem('jwt_token')
  if (session.storedToken !== undefined && storedToken !== session.storedToken && storedToken !== (auth.accessToken ?? null)) {
    auth.syncAuthStatus()
  }
  // Token comparison also supports isolated callers without the auth-store epoch.
  return session.epoch === undefined
    ? session.token === (auth.accessToken ?? null)
    : session.epoch === auth.sessionEpoch
}

export function isCurrentCredential(session) {
  return isCurrentSession(session) && session.token === (useAuthStore().accessToken ?? null)
}

export function tokenExpiresAt(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return Number.isFinite(payload.exp) ? payload.exp * 1000 : null
  } catch { return null }
}

export function normalizeTokenExpiry(data, now = Date.now()) {
  const explicit = Number(data.expiresAt)
  if (Number.isFinite(explicit) && explicit > 0) return String(explicit)
  const lifetime = Number(data.expiresIn)
  if (Number.isFinite(lifetime) && lifetime > 0) {
    // Older releases mislabeled epoch milliseconds as expiresIn.
    return String(lifetime >= 1e12 ? lifetime : now + lifetime * 1000)
  }
  const expiry = tokenExpiresAt(data.token)
  return expiry === null ? null : String(expiry)
}
