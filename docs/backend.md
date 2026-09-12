# Backend contracts

The backend is a Java 17 / Spring Boot Maven application. Run commands from the repository root using `mvnw.cmd` on Windows or `./mvnw` on Unix. Configuration and deployment steps are in [operations.md](operations.md); do not put credentials in YAML or SQL seed files.

## Source layout

- `src/main/java/com/web/controller`: HTTP/STOMP entry points.
- `config` and `security`: Spring configuration, JWT authentication and authorization.
- `service` / `service/impl`: business operations and transactional boundaries.
- `mapper` plus `src/main/resources/mapper`: MyBatis interfaces and XML statements.
- `model`, `dto`, `vo`, `constant`, `util`: persisted models, payloads and shared helpers.
- `src/main/resources/sql`: schema, seeds, indexes and reviewed migrations.
- `src/test/java`: regression and opt-in infrastructure integration tests.

## Authentication and errors

JSON responses use `ApiResponse`: `{code, message, data, timestamp, path}`. Success is **code 0**. Successful group delete/leave operations return HTTP 204 without JSON; the frontend interceptor normalizes these for envelope-based callers. HTTP 401 / code1002 means authentication failed; HTTP403 / code1003 means permission denied. A business or server failure must not cause automatic logout or retry of a mutation.

`/api/auth/login`, `/register`, `/forgot-password`, `/reset-password` and `/verify-reset-token` support authentication recovery. Logout and password changes revoke access tokens. An access token is purpose-bound, tied to the persisted account/password, and checked against Redis revocation state. Redis failure does not authorize a token. Reset uses a separate short-lived, single-use random credential delivered by configured SMTP; it is never an access token.

Administrative authority comes from persisted `user.type` (`ADMIN`, `USER`, `BOT`). Registration always creates USER accounts, regardless of username. A numerical achievement level is not an administrator role. User JSON never includes password hashes.

## Implemented frontend contracts

| Operation | Contract |
| --- | --- |
| Current profile | GET/PUT `/api/users/me`; PUT `/api/users/profile` for supported profile fields |
| Avatar | POST `/api/users/avatar`, multipart field `file`; returns an application upload URL |
| Follow/unfollow | POST/DELETE `/api/follow/{followeeId}`; `/api/users/{userId}/follow` are implemented aliases |
| Follow state/counts | GET `/api/follow/check/{followeeId}`, `/api/follow/stats/{targetUserId}` |
| Public following/followers | GET `/api/users/{userId}/following` and `/followers`; following visibility is enforced |
| Settings | GET `/api/users/me/settings` returns `{privacy, notifications}` |
| Save a settings section | PUT `/api/users/me/settings/privacy` or `/notifications`; all booleans in that section required; response data is the saved section |
| Level history/progress | GET `/api/user-level-history/user/{userId}` and `/api/users/me/level/upgrade-progress`; history and persisted activity supply the values |
| Chat history/read | GET `/api/chats/{sharedChatId}/messages`; POST `/api/chats/{sharedChatId}/read` |
| Message reaction | POST `/api/chats/messages/{messageId}/react?reactionType=...` |
| Message recall | DELETE `/api/chats/messages/{messageId}` |
| Group creation | POST `/api/groups` with `groupName`, `groupDescription`, optional `groupType` and `initialMemberIds` |
| Simple group search | GET `/api/groups/search?q=...&limit=10`; returns a list of public active groups |
| Created/joined groups | GET `/api/groups/my-created` or `/my-groups` with paired `page=0&size=10`; returns `{list,total,page,size}`. Size is 1–100; no-parameter legacy calls return arrays |
| Group details/members | GET `/api/groups/{groupId}` and `/members`; private groups require valid membership or the persisted system-admin role |
| Notification pages | GET `/api/notifications?page=1&size=10`; data contains `notifications`, `totalCount`, `totalPages`, `currentPage`, `pageSize` |
| Public profile details | GET `/api/users/{id}`; data contains nested `user` and `userStats` |

Controllers and DTO validation are authoritative for all remaining parameters. Group application approval is a PUT operation with the action/reason body defined by the active group API module. Article sponsorship validates finite integer amounts, the published article, actor identity and available balance in one transaction.

Group applications submit `message`; private groups are invitation-only. Initial group members are validated and deduplicated, and rejoining does not duplicate an existing chat-list entry. Comments follow article visibility; replies must reference a parent from the same article, and deletions match both article and author.

Joined-list `excludeOwned=true` requires pagination and is applied before COUNT/LIMIT. Both lists use active groups and valid accepted membership, ordered by `create_time DESC,id DESC`. Search results carry the authenticated viewer's `currentUserRole` (`OWNER`, `ADMIN`, `MEMBER`, `NON_MEMBER`), so an off-page joined group retains its correct action without downloading all memberships.

Reaction POSTs toggle the current user's selected emoji. History and broadcasts use `reactions: [{emoji, reactionType, count, userIds}]`, or an empty array. Consumers should use the authoritative aggregate after a toggle rather than incrementing locally.

## Search ordering

Paginated group/user search accepts `relevance`, `time_asc`, `time_desc`, `name_asc`, and `name_desc`. Article search accepts `created_at`, `updated_at`, `title`, or `relevance` with `sortOrder=asc|desc`. Message search accepts `relevance`, `time_asc`, `time_desc`, `username_asc`, and `username_desc`. The shared frontend selector translates its choice separately for each resource.

Message search queries current SQL membership before counting and paginating. Whitespace-separated terms match case-insensitive literal substrings; any term can match. Relevance prefers exact full-query matches, then a contiguous phrase, then more distinct matching terms, followed by newest message time/ID. `%`, `_`, and `!` in the query remain literal. This is SQL ranking, without Elasticsearch linguistic stemming or BM25 scoring.

GET `/api/search/messages?q=...` accepts a raw query of 1–100 characters, at most 10 distinct terms, a zero-based page and size 1–100. The requested page end must be at most 10,000; requests beyond these bounds return HTTP 400 and require a narrower query. SQL first derives authorized conversations, searches the V005 stored text and enriches only the selected result page with sender/group display fields. Literal substring matching still scans qualifying message text; the bounds and measured local profile are not a claim of unlimited search capacity.

A window count computes the unique-message total before display aliases; normal nonempty pages avoid a second text scan. Empty/out-of-range pages run the authorized fallback count. See the [same-data query measurements](p2-capacity.md).

## Chat ownership

Shared conversation IDs and per-user chat-list IDs are different identifiers. A private conversation belongs to its persisted participants. Group access requires an accepted membership that has not been kicked and an active group. Authorization applies before reads, writes, reactions and STOMP room subscriptions. Sender identity comes from the authenticated session.

SockJS/STOMP connects at `/ws`. Clients send private messages to `/app/chat/private` and group messages to `/app/chat.sendMessage` and subscribe to their own standard `/user/queue/...` destinations. Application authentication owns the socket lifetime; leaving the chat page does not disconnect it. Logout disconnects it. The client sends `/app/chat/heartbeat` every 30 seconds to renew the 120-second Redis session/index leases; transport heartbeat frames alone do not call this handler. Redis reconciliation keeps another live tab online and removes both presence entries after the last active session. Notification delivery uses the authenticated username for Spring user destinations.

Preferences are enforced on private-message acceptance, following visibility, presence output and notification creation. Suppressing a new-message alert does not suppress delivery or unread counts.

The former thread UI is archived under `docs/legacy/thread-prototypes`. The retained backend thread prototype is disabled by default: the maintained schema does not define its thread tables. It is not an enabled or runtime-verified feature.

## Database lifecycle and verification

Development startup initializes the database named by `MYSQL_URL`, creates tables before checks, and skips only indexes confirmed to exist. Schema errors fail startup. Production profiles skip automatic creation; apply reviewed schema migrations before deployment. No default administrator/password is seeded.

`mvnw.cmd test` runs ordinary regressions. The opt-in MySQL suite requires an explicitly named disposable `weeb_audit` database; Redis integration requires its dedicated loopback port. See [operations.md](operations.md) for the isolated verification setup. Passing compilation or mock-based tests alone does not prove database or WebSocket behavior; final evidence is recorded in [remediation-plan.md](remediation-plan.md).

## Release and failure recovery

[Schema migrations](schema-migrations.md) use one checksum registry and a standalone CLI; production startup performs read-only validation. V003 makes MySQL generations/revocations authoritative, requires one fresh login for old sessions, and retains Redis as a required fail-closed dependency. Login/renewal return epoch-ms expiresAt and remaining-seconds expiresIn. Renewal needs a still-valid Bearer token; missing storage produces503, invalid credentials401.

V004 writes message, unread count and outbox in one transaction. Stable sender-scoped client IDs deduplicate exact retries and reject conflicting reuse. READ cursors are bounded and monotonic; PUT/DELETE reactions set desired state and return versioned aggregates. Outbox retries and rechecks current access; dispatch does not prove browser receipt. `/sync` recovers new SQL messages and `/messages/state` refreshes up to100 loaded IDs after reconnect. Current Simple Broker support is one application instance. [Machine-readable HTTP/STOMP contracts](contracts.json) and [release tools](release.md) bind these behaviors to regression evidence.
