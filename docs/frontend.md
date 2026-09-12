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

`src/api/axiosInstance.js` returns the backend envelope `{code,message,data}`. `code === 0` is success; business failures reject. Successful HTTP 204 responses are normalized to `{code:0,message:'',data:null}` for delete/leave callers. HTTP status 200 is not the business success code. HTTP 401 clears local authentication; 403 and ordinary business failures do not log the user out. Mutating requests are not retried automatically.

HTTP and SockJS default to the current browser origin at `/api` and `/ws`. Vite proxies API, uploads and SockJS traffic to the local backend (default port8080); production needs the corresponding reverse proxy. Explicit `VITE_API_BASE_URL` and full HTTP(S) `VITE_WS_URL` overrides remain supported. The proxy targets the endpoint origin without duplicating `/ws`. Client variables contain no secrets.

The actual HTTP routes are documented in `docs/backend.md` and declared in backend controllers. API modules must match their HTTP verbs, path parameters, query parameters and JSON bodies. In particular:

- Registration/login use `/api/auth`; `/forget` sends a scalar email through the API wrapper, producing `{email}`; password reset submits `resetToken`, `newPassword`, `confirmPassword`.
- User profiles use `/api/users/me`; avatar upload uses multipart field `file` at `/api/users/avatar` and returns an avatar URL.
- Following uses `/api/users/{id}/follow`; POST/DELETE `/api/follow/{id}` are implemented aliases. Following/follower lists and count/status contracts remain supported.
- Group applications are processed with `PUT /api/groups/{groupId}/applications/{applicationId}` and JSON `{action,reason}`.
- Group creation sends `groupDescription`; simple discovery sends `q`, while paginated discovery sends `keyword` and a zero-based page. Group chat navigation keeps both canonical `sharedChatId` and `groupId`.
- Created/joined group pages request server `page` and `size`; the joined tab sends `excludeOwned=true` before server counting. They no longer download complete lists to slice locally. Search actions use each result's authoritative `currentUserRole`, including memberships outside the currently loaded page.
- GET `/api/users/{id}` returns `data.user` and `data.userStats`; group owner/applicant displays unwrap the user fields.
- Message reactions pass `reactionType` as a query parameter and render server aggregates after the toggle; history preserves the same emoji/count/user-ID structure.
- Notifications use server `totalCount` and `totalPages`; deleting read items reloads server pagination. Shared presentation maps actual lowercase social event types and retained uppercase aliases to text and active routes.
- Shared search sort controls translate into each resource's supported sort fields/directions. Article list calls retain their selected sorting parameters; see [backend search semantics](backend.md#search-ordering).
- User level history/current/count use `/api/user-level-history/user/{userId}` and its subpaths; upgrade progress comes from `/api/users/me/level/upgrade-progress`.

## Shared UI and security

Retained Apple components live in `src/components/common`. The switch exposes keyboard interaction and checked/disabled state. The modal provides dialog semantics, initial focus, a focus trap and focus restoration. Notifications and confirmation render text with Vue components; untrusted strings are not inserted as HTML.

The header provides the light/dark/system theme control. Theme variables live in `src/assets/apple-style.css`, imported by `src/assets/main.css`; active chat/history styles use them. Article preview, moderation and version rendering use `src/utils/safeHtml.js` and DOMPurify. The article reader applies its own DOMPurify sanitization before rendering HTML.

Interface accents use blue and neutral shades in every theme. Purple tokens, gradients, badges and faint violet grays were removed from all active CSS/Vue files; see `docs/color-audit.md` for the inventory and verification.

Live emoji data is in `src/constant/emoji`; both Vite and IDE/test aliases map `@constant` there. The active paginated picker preserves searchable emoji packages and all 15 formerly available common icons, with a regression that searches and selects each. Abandoned thread UI prototypes are preserved as non-executable text in `docs/legacy/thread-prototypes`, outside the production source tree. `src/api/modules/messageThread.js` and backend `/api/threads` handlers remain, but the SQL create scripts omit their `message_threads` and `thread_participants` tables. Thread functionality is unsupported in a fresh deployment.

Debug commands are in `Vue/scripts/debug-commands.js`, outside `public`, so production builds do not copy them. See `docs/remediation-plan.md` for actual verification status and any outstanding integration work.

## Session and recovery contracts

The nonpersisted session epoch, captured request token and socket instance fence late responses, retries, errors and cleanup after account changes. Login, renewal, router/profile/settings continuations share this boundary. Temporary auth dependency failures preserve the current session; a late401 from an old account or superseded token cannot clear a new session.

Chat sends keep a stable clientMessageId across retry/fallback, reconcile sender confirmations before duplicate filtering, and preserve persistence versus receipt states. Reconnect catches up with SQL cursors and refreshes loaded message state; bounded read receipts preserve newer unread messages. Explicit PUT/DELETE reactions and monotonic reactionVersion merging avoid toggle replay and stale aggregate replacement. See [contracts](contracts.json) and [release gates](release.md).

Search and group pagination use request generations and abort signals so an older response cannot replace the active query/page or another account's data. Chat retains at most 50 ordinary conversation caches and 200 persisted messages per cached conversation; pending/failed sends and the active conversation are preserved. Older/newer/latest navigation recovers history from the server after window movement or eviction. Notifications retain a 100-item window and reconcile unread counts every five minutes while the socket is healthy; disconnected polling starts at 30 seconds with bounded failure backoff, and hidden/offline pages stop polling. See [store lifecycle details](stores.md).
