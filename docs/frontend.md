# Frontend

The maintained application is `Vue/`, built with Vue 3, Vite, Pinia and Vue Router. Install dependencies from `Vue/package-lock.json`; there is no root Node package.

## Commands

- `npm run dev`: development server.
- `npm run build`: production build in `Vue/dist`.
- `npm run test:run`: Vitest regression suite using jsdom and Vue Test Utils.

ESLint and Prettier are not configured. A successful build is not a substitute for the behavioral tests or browser verification.

## Routing and application state

`src/router/index.js` is the authoritative route registry. Authenticated pages render inside `src/layout/Layout.vue`; authentication pages are outside it. The active pages include chat, contacts, articles, groups, profiles, level history, settings, search and notifications. The reset form is reached at `/reset-password?token=...`. There are no role-permission administration pages registered by a separate admin route module.

`src/stores/index.js` initializes application-level stores and authentication-dependent WebSocket lifetime. Restoring a session connects the socket; navigating away from chat preserves it; logout tears it down. Components subscribe to the stores rather than creating another global transport. STOMP subscriptions use the authenticated user's `/user/queue/...` destinations; server-side membership and identity checks authorize traffic.

## API contracts

`src/api/axiosInstance.js` returns the backend envelope `{code,message,data}`. `code === 0` is success; business failures reject. HTTP status 200 is not the business success code. HTTP 401 clears local authentication; 403 and ordinary business failures do not log the user out. Mutating requests are not retried automatically.

In development, HTTP requests use relative `/api` paths and Vite forwards them to `VITE_API_BASE_URL` (default `http://localhost:8080`). Production HTTP requests use that base URL directly. SockJS uses the full `VITE_WS_URL` endpoint (default `http://localhost:8080/ws`) directly from the browser. Although Vite also declares a `/ws` proxy, the default chat connection bypasses it. Set the SockJS variable to a browser-reachable HTTP(S) URL including `/ws`; do not store secrets in client environment variables.

The actual HTTP routes are documented in `docs/backend.md` and declared in backend controllers. API modules must match their HTTP verbs, path parameters, query parameters and JSON bodies. In particular:

- Registration/login use `/api/auth`; password reset submits `resetToken`, `newPassword`, `confirmPassword`.
- User profiles use `/api/users/me`; avatar upload uses multipart field `file` at `/api/users/avatar` and returns an avatar URL.
- Following uses the implemented `/api/users/{id}/follow`, `/following`, `/followers` and count/status contracts.
- Group applications are processed with `PUT /api/groups/{groupId}/applications/{applicationId}` and JSON `{action,reason}`.
- Message reactions pass `reactionType` as a query parameter.
- User level history/current/count use `/api/user-level-history/user/{userId}` and its subpaths; upgrade progress comes from `/api/users/me/level/upgrade-progress`.

## Shared UI and security

Retained Apple components live in `src/components/common`. The switch exposes keyboard interaction and checked/disabled state. The modal provides dialog semantics, initial focus, a focus trap and focus restoration. Notifications and confirmation render text with Vue components; untrusted strings are not inserted as HTML.

The header provides the light/dark/system theme control. Theme variables live in `src/assets/apple-style.css`, imported by `src/assets/main.css`; active chat/history styles use them. Article preview, moderation and version rendering use `src/utils/safeHtml.js` and DOMPurify. The article reader applies its own DOMPurify sanitization before rendering HTML.

Interface accents use blue and neutral shades in every theme. Purple tokens, gradients, badges and faint violet grays were removed from all active CSS/Vue files; see `docs/color-audit.md` for the inventory and verification.

Live emoji data is in `src/constant/emoji`; both Vite and IDE/test aliases map `@constant` there. The active paginated picker preserves searchable emoji packages from the old unrouted chat page. Abandoned thread UI prototypes are preserved as non-executable text in `docs/legacy/thread-prototypes`, outside the production source tree. `src/api/modules/messageThread.js` and backend `/api/threads` handlers remain, but the SQL create scripts omit their `message_threads` and `thread_participants` tables. Thread functionality is unsupported in a fresh deployment.

Debug commands are in `Vue/scripts/debug-commands.js`, outside `public`, so production builds do not copy them. See `docs/remediation-plan.md` for actual verification status and any outstanding integration work.
