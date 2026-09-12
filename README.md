# Weeb

Weeb is a chat and community application with a Spring Boot 3 / Java 17 backend and a Vue 3 frontend in `Vue/`. It provides private and group chat, message reactions, articles and comments, campus communities, contacts/following, notifications, profiles, and account settings.

The interface supports light, dark, and system themes with blue and neutral accents. Active CSS and Vue components do not use purple styling.

## Requirements and layout

- Java 17; the Maven wrapper downloads Maven 3.9.9.
- Node.js and npm compatible with `Vue/package-lock.json`. Verification used Node 20.19.4 and npm 10.8.2.
- MySQL 8 and Redis. Elasticsearch is optional; SMTP is needed to deliver password-reset emails.

| Location | Purpose |
| --- | --- |
| `src/main/java/com/web` | Controllers, services, authorization, models, and MyBatis mappers |
| `src/main/resources/mapper` | SQL mapper statements |
| `src/main/resources/sql` | Ordered schema registry, frozen baseline, and versioned migrations |
| `Vue/src/views`, `Vue/src/router/index.js` | Active pages and routes |
| `Vue/src/api`, `Vue/src/stores` | Backend contracts and shared application state |
| `src/test/java`, `Vue/src/**/*.spec.js` | Backend and frontend regression tests |
| `docs` | Maintained API, deployment, and verification documentation |

## Build and verify

```powershell
.\mvnw.cmd clean verify
cd Vue
npm ci
npm run test:run
npm run build
```

On POSIX shells use `./mvnw` in place of `mvnw.cmd`. The executable backend is `target/WEEB-0.0.1-SNAPSHOT.jar`; frontend assets are in `Vue/dist`. `Vue/package.json` is the frontend manifest; run npm commands inside `Vue/`.

Ordinary tests skip the opt-in MySQL/Redis suites unless their dedicated environment variables are configured. Use a disposable `weeb_audit` database and Redis on loopback port 16379 for these checks; see [verification setup](docs/operations.md#local-verification-services). The [verification record](docs/verification.md) distinguishes full infrastructure tests, browser checks, and deployment limitations.

For a release review, use `python scripts/release_gate.py --chrome "<installed Chrome executable>"` with the audit environment configured. This requires clean Git state, builds two isolated copies, compares JAR/frontend hashes, fails on missing or skipped integration suites, and exercises real HTTP, STOMP and browser flows. `--allow-dirty` explicitly produces a **review snapshot**, not a committed release candidate. See [release instructions](docs/release.md). This command does not commit, publish, deploy, rotate provider credentials or rewrite history.

The [2026-09-12 frozen-snapshot verification](docs/release-verification.md) passed **307 backend tests, 159 frontend tests and 23 HTTP/STOMP/browser assertions**, with zero skipped tests and identical artifacts from both builds. Its controlled evidence binds the exact source and artifact hashes; external production acceptance remains open.

The subsequent [P2 capacity work](docs/p2-capacity.md) adds real group pagination, bounded message/notification caches, stale-response protection, reduced polling and measured message-search optimization. [Verified history publication](docs/history-publication.md) binds the complete clean candidate, migration versions, reproducible artifacts and exact-JAR capacity results before updating remote main. Earlier snapshot counts above describe their own verification phase.

The [P2 clean-candidate review](docs/p2-verification.md) passed **324 backend tests, 191 frontend tests and 23 real HTTP/STOMP/browser assertions**, with zero skips and identical two-build artifacts. The local 100,000-message capacity comparison passed at 1/5/20 concurrency, improving overall 20-concurrent P95 from 1,907 to 797 ms. Publication still checks the final candidate's own manifests and exact JAR.

## Run locally

Start MySQL and Redis and create the intended local database. Export the environment variables documented in [.env.example](.env.example), including a newly generated `JWT_SECRET` of at least 32 bytes and `MYSQL_PASSWORD`. Also set `MYSQL_URL`, `MYSQL_USERNAME`, `REDIS_HOST`, and `REDIS_PORT` when their defaults do not match your local services. The backend does not automatically load `.env.example` or `.env`.

Development startup applies the ordered, checksum-verified schema registry. Production startup performs read-only validation and rejects pending or changed migrations. Upgrade an existing database with the dedicated migration CLI before starting the new application; do not run arbitrary create scripts to upgrade columns. Back up and inspect the migration plan first; see [migration and recovery instructions](docs/schema-migrations.md).

```powershell
.\mvnw.cmd spring-boot:run
```

In a separate terminal:

```powershell
cd Vue
npm run dev
```

HTTP and SockJS default to the browser's own origin (`/api` and `/ws`). Vite development proxies `/api`, `/uploads` and `/ws` to the backend, defaulting to `http://localhost:8080`. Production needs a reverse proxy serving these paths, including WebSocket upgrades and SockJS fallback HTTP requests. Explicit `VITE_API_BASE_URL` and `VITE_WS_URL` overrides remain available; the latter is an HTTP(S) endpoint including `/ws`. Set password-reset links separately with `PASSWORD_RESET_FRONTEND_URL`. Only variables prefixed `VITE_` are exposed to client code; do not put credentials in them.

Accounts are created through registration. An operator must explicitly provision an administrator using the stored user type; no default password or username convention grants administrator privileges. Password-reset email requires SMTP configuration and the correct frontend reset URL.

## Frontend and backend contracts

The active API modules and controller/DTO definitions are checked together. These distinctions matter when adding or changing a page:

| Flow | Contract |
| --- | --- |
| Responses | JSON success uses `code: 0`; Axios also normalizes successful HTTP 204 responses for delete/leave operations |
| Password recovery | `/forget` sends `{email}`; `/reset-password?token=...` submits `{resetToken,newPassword,confirmPassword}` |
| Group creation | Use `groupName`, `groupDescription`, optional `groupType` (`PUBLIC`/`PRIVATE`) and `initialMemberIds` |
| Group discovery | `/api/groups/search` takes `q`; paginated `/api/search/group` takes `keyword`, zero-based `page`, and `size` |
| My groups | `/api/groups/my-created` and `/api/groups/my-groups` accept zero-based `page` and `size` together; joined lists support `excludeOwned=true`; no-parameter legacy callers still receive arrays |
| Group applications | Submit `{message}`; reviewers use PUT with `{action,reason}` and the matching group/application IDs |
| Chat identifiers | Message endpoints use `sharedChatId`; group metadata uses `groupId`; chat-list row IDs are separate |
| Reactions | PUT/DELETE `/api/chats/messages/{messageId}/react?reactionType=...` explicitly add/remove; legacy POST toggles and must not be automatically replayed |
| Following | POST/DELETE `/api/follow/{id}` and `/api/users/{id}/follow` are implemented aliases |
| Message retry | Keep `clientMessageId` unchanged for one send; uniqueness is scoped to its sender, and changed content/chat under the same ID conflicts |
| Message recovery | GET `/api/chats/{sharedChatId}/sync?afterMessageId=0&size=100` returns persisted messages with an ascending cursor |
| Read boundary | POST `/api/chats/{sharedChatId}/read` with `{lastReadMessageId}` advances only through that visible message |
| Profiles | GET `/api/users/{id}` returns `data.user` and `data.userStats`; unwrap the user before reading username/avatar |

Private group discovery is hidden, and private details/member lists require valid membership or the persisted system-admin role. Public group previews let nonmembers apply. Chat reads, writes, and subscriptions require authorized participation. Comments follow the article's visibility rules, and replies/deletions are bound to their article.

The authenticated application owns the SockJS/STOMP connection across page navigation. It sends an application heartbeat every 30 seconds to renew Redis presence; closing one tab keeps another active tab online. Logout tears down the frontend connection and revokes the token.

Private STOMP sends use `/app/chat/private`; group sends use `/app/chat.sendMessage`. Message, unread and outbox writes share a MySQL transaction. Outbox retries broker dispatch; SQL sync recovers persisted messages after disconnect. Broker dispatch is not proof of browser receipt. The current Simple Broker supports **one application instance**; two tabs do not establish cross-instance support. See [HTTP/event contracts](docs/contracts.json).

Authentication renews a still-valid Bearer token; there is no independent refresh credential. `expiresAt` is epoch milliseconds and `expiresIn` is remaining seconds. MySQL stores authoritative session generations and token revocations; Redis remains a required authorization dependency. Dependency failures return 503 rather than misclassifying the session as invalid. Existing users must sign in once after migration V003. Frontend request/socket generations prevent late responses from an old account from mutating the current session. Tokens still use localStorage; this iteration does not implement HttpOnly cookie sessions.

Automatic renewal uses browser Web Locks to serialize competing tabs. Without that API, the existing token remains usable until its normal expiry, then sign-in is required. Deploy over HTTPS; see [session details](docs/stores.md).

## Cleanup and retained functionality

The large cleanup includes lowercase Java package moves, pages moved into `Vue/src/views`, unused duplicate pages/helpers, and consolidation of duplicate contact-expiration jobs. Active chat, account, article, contact, group, and search routes remain. The deletion recheck restored the old common emoji choices in the shared searchable picker; follow-up compatibility findings and validation are recorded in [the recheck](docs/functionality-recheck.md).

Thread UI prototypes are preserved under `docs/legacy/thread-prototypes`. Their backend is disabled by default because the maintained schema does not include its required tables; this is not an enabled feature on a fresh installation. Elasticsearch is disabled by default; enable `ELASTICSEARCH_ENABLED=true` only with a configured service. Search behavior and optional-service limits are documented in [backend contracts](docs/backend.md).

## Campus space / 校园空间

Open **校园空间** in the navigation or visit `/campus`. The module includes school discovery, manual student verification, private campus posts with up to six images, drafts and publication review, search/category/sort/pagination, likes, bookmarks, paginated comments/replies, reports, member administration and audit records. It uses its own tables and permissions; campus posts do not enter public articles, global article search or article point totals.

1. An operator provisions a site administrator through the existing persisted `user.type` procedure. The administrator opens `/campus/admin`, creates a school and configures pre-moderation (enabled by default).
2. Students select a school, submit their name, student number, department, enrollment year and statement, then view or withdraw their application. Only the applicant and authorized reviewers can see these details. Rejected applications can be resubmitted; a suspended membership requires administrator restoration.
3. A campus administrator approves or rejects applications from the school's management page. Verified members can read and publish in that school; users may hold separate verified memberships in multiple schools. This is **site-administered manual review**, not a connection to a university registrar or official student identity provider.
4. Members save drafts or submit posts. With pre-moderation enabled, another authorized administrator must approve the current version before publication; authors cannot approve themselves. Editing a published post takes it out of the feed and submits the new version for review. Announcements are restricted to administrators.
5. Management provides review queues, pinning, actual report removal/dismissal, membership suspension/restoration, school settings and audit history. Only a site administrator can appoint or revoke campus administrators. Leaving, suspension, school deactivation and content removal affect subsequent API, image and notification access.

Apply **V006** through the existing [migration process](docs/schema-migrations.md) before production startup. Keep V001–V005 unchanged. Set `CAMPUS_MEDIA_DIRECTORY` to a **private durable directory outside static web mappings**, and back it up together with MySQL. Images require authenticated reads and are re-encoded as PNG; source and encoded size are limited to 5 MB, dimensions to 4096×4096 / 16 million pixels, pending uploads to 12 per user per school and total image storage to 100 MB per user per school. Unused images and images of removed posts are reclaimed after 24 hours by an hourly bounded cleanup.

Campus HTTP endpoints use `/api/campus`, success code 0 and zero-based `{list,total,page,size}` pagination. Optimistic edits/reviews require `version`; conflicts return 409 and the editor preserves input. Notification records commit with the business transaction and push after commit; the durable inbox recovers missed socket delivery. Current post access is checked for notification listing/counting and delivery. Full [API and state contracts](docs/campus-api.md) and [implementation/acceptance scope](docs/campus-space-plan.md) describe the details. The separate Thread prototype remains outside this feature.

The [campus verification record](docs/campus-verification.md) covers real MySQL, HTTP, STOMP and browser journeys, 217 frontend tests, migration recovery and permission changes during requests. The clean release gate requires the campus suites and 58 runtime checks and binds results to the exact candidate. Campus API timestamps are UTC instants displayed in the browser's local timezone. This module supports the existing single-instance deployment; official university identity integration and campus-scale capacity certification are outside the verified scope.

See [backend API documentation](docs/backend.md), [frontend documentation](docs/frontend.md), [store documentation](docs/stores.md), and [credential remediation](docs/operations.md). The original audit is in [docs/report.md](docs/report.md); fixes and evidence are tracked in [docs/remediation-plan.md](docs/remediation-plan.md) and [docs/verification.md](docs/verification.md). Live credential rotation, certificate replacement and actual production migrations still require deployment-side evidence. Repository-history completion is recorded separately in its verified publication manifest.
