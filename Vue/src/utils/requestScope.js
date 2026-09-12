import { captureSession, isCurrentSession } from './session'

// Latest response wins within one UI resource, including error and loading state.
export function createRequestScope() {
  let generation = 0
  let controller
  return {
    begin() {
      controller?.abort()
      controller = new AbortController()
      const current = ++generation
      const session = captureSession()
      return { signal: controller.signal, isCurrent: () => current === generation && isCurrentSession(session) }
    },
    cancel() { ++generation; controller?.abort() }
  }
}
