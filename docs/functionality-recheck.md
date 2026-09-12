# Functionality and frontend/backend recheck — 2026-09-12

This review responds to the concern that the cleanup deleted too many functions. The comparison is `da3cf23` to `1167c19`, followed by the current corrective changes. During the review, HEAD changed to `1167c19`; comparing only the working tree with HEAD would have missed the cleanup. The review checked earlier and current routes, imports, callers, controller mappings, payloads, and visible controls.

## What the large deletion diff contains

Git detects 113 file renames at its 50% similarity threshold. Seventy-eight Java paths are case-only package moves. Heavily rewritten files can appear as a deletion and a new file even when the same service remains in the application.

| Area | Finding and resulting behavior |
| --- | --- |
| Active pages | Article, contact, group, and search pages moved into `Vue/src/views`; their routes remain. The old baseline already routed chat to `views/chat/ChatPage.vue`. |
| Deleted frontend modules | The 31 wholly removed production JS/Vue modules were unreachable from the baseline entry point, route registry, and import graph. They include duplicate chat/registration/group pages and unused Apple wrapper components. |
| Shared socket | Chat and contact pages use the authenticated application's shared socket. Offline fetching remains in `chatStore.onConnect`; leaving a page preserves the connection and logout disconnects it. |
| HTTP/STOMP entry points | Existing mapping paths remain, including all six search routes and nine WebSocket mappings. This alone does not prove identical behavior; actual losses identified below were corrected. |
| Contact expiration | Duplicate background jobs were consolidated. The active job atomically marks pending contacts EXPIRED, preserves their rows, and cannot overwrite an accepted contact. The old destructive scheduler was genuinely active; replacing its behavior was intentional. |
| Unused backend helpers | Removed standalone moderation, debug, token-refresh, result-formatting, and stats-sync helpers had no historical active production callers. The retained authentication, moderation-controller, and level/statistics paths remain. Three removed JWT reset helpers were uncalled stubs; the working reset service remains. |
| Offline-message helper | The removed helper's scheduled cleanup shortened Redis queue retention. Active broadcast storage and `UserOnlineStatusService` delivery remain, with Redis expiry managing retention. |
| Thread prototype | Three unique UI prototypes are archived as `.vue.txt` in `docs/legacy/thread-prototypes`. The retained backend is now disabled by default because its tables are absent from the maintained schema. This is an explicit API-availability change, not a claim that thread functionality was preserved as an enabled feature. |

The deletion recheck found two functional losses that required correction:

- The shared emoji picker omitted 10 choices from the previous 15 common emojis. All 15 are now available, searchable, and selectable without duplicate icons.
- Message search still displayed a relevance option after its backend treated that option as newest-first. It now ranks exact query matches, contiguous phrases, and distinct matching terms, with stable time/ID tie breakers. Whitespace-separated terms match case-insensitive literal substrings with OR semantics. SQL search does not reproduce Elasticsearch stemming or BM25 scoring; this difference is explicit.

## Frontend/backend corrections

| Flow | Confirmed mismatch corrected |
| --- | --- |
| Password recovery | `/forget` used undeclared imports, nested the email incorrectly, and submitted the wrong reset fields. It now sends `{email}` and `{resetToken,newPassword,confirmPassword}`, handles rejected credentials, and points to the emailed reset link. |
| Group creation and discovery | Description fields, simple-search `q`, numeric pagination, and the canonical shared conversation ID now match the backend. Unpaginated owned/joined lists are sliced for their visible pagers. |
| Group state and privacy | Creation saves visibility and deduplicated initial members. Rejoining reuses the existing chat entry. Inactive/revoked members do not appear as accepted members. Private details/member reads and applications enforce private-group policy. |
| Group profile data | Owner and applicant rendering unwraps the actual `UserWithStats` response's `data.user` before using username/avatar. Public nonmembers get an application action without member-only controls or member fetches. |
| Delete/leave responses | Successful HTTP 204 responses are normalized to the same success convention used by their callers. |
| Notifications | Pagination uses `totalCount`/`totalPages`, and deleting read items reloads server pagination. Actual lowercase `follow` events open the actor's profile; legacy type aliases remain. Article links use `/article/read/{id}`. The explicit self-targeted test notification now reaches the normal storage/delivery path; ordinary self-events remain suppressed. |
| Sorting | Article list calls retain `sortBy`/`sortOrder`. Shared search controls translate to each resource's accepted field names and directions, including pagination requests. |
| Comments | Reads/counts/creation enforce article visibility. Parent replies and deletion predicates are scoped to the requested article, and deletion also requires the comment author. |
| Reactions | SQL uses the existing `create_time` column; message history and broadcasts include emoji/count/user-ID aggregates. The UI follows the server's toggle result without inventing an extra local increment. |
| Presence | The application sends 30-second heartbeats. Redis updates renew both leases atomically; cleanup preserves other live tabs and clears both presence sets after the final tab closes. |

Focused regressions exercise actual Axios adapters, mounted pages, MySQL statements, and Redis serializers. Full suite/build results and browser/HTTP/WebSocket evidence are recorded in [verification.md](verification.md). These checks establish the reviewed flows; they do not establish equivalence for every dormant prototype or external integration.
