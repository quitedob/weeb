# Remediation verification — 2026-09-12

The checks used disposable MySQL 8 and Redis 7 containers on loopback ports 23306 and 16379. The application ran on 127.0.0.1:18080, and Vite on 127.0.0.1:5173. Production services were not modified.

## Automated checks

- Backend: `mvnw.cmd -o clean verify` passed in 27.659 seconds, producing the executable JAR. All 212 tests passed with no failures, errors or skips, including real MySQL, Redis and loopback SMTP integration tests. Group regressions cover role escalation, inactive membership, owner protection, cross-group application decisions and atomic capacity/count updates. A real Spring transaction test verifies that a failed capacity reservation rolls an approval back; two independent MySQL connections verify capacity and increment behavior. Nineteen WebSocket identity tests cover event authentication and invalid legacy presence records.
- Frontend: 56 tests passed across 15 suites; `npm run build` passed in 12.83 seconds. Four group-detail page tests exercise numeric role labels, owner/admin removal controls, self/owner protection and description saving through the actual `groupDescription` contract. Existing mixed static/dynamic import warnings remain.
- `git diff HEAD --check` passed. No obsolete uppercase Java-package/resource references or frontend success-code-200 comparisons remain in active source.
- All 59 active CSS/Vue files were scanned for purple styling, including hex/RGB/HSL colors, encoded SVG colors and faint violet grays. Zero candidates remain; see `color-audit.md`.
- Current source and documentation contain no copies of the retired configuration credentials or private-key artifacts. History and ignored recovery files are excluded from this finding.

## Actual browser and HTTP/WebSocket checks

An isolated headless Chrome profile was used because the in-app browser connection failed and the shared DevTools browser profile was occupied. Verification did not use the user's existing browser profile.

- Registration, login, profile, preferences, level progress and chat-list HTTP responses succeeded. User JSON did not contain passwords.
- Login and page reload established a SockJS/STOMP connection. Chat-to-settings navigation preserved the connection.
- Theme controls changed the rendered light/dark appearance and persisted selection. Both previews were visually inspected; no purple interface accents appeared.
- A privacy switch responded to Space and Enter; saving it survived a page reload. The test preference was restored afterward.
- The New Chat dialog exposed its accessible name and modal role, moved focus inside, wrapped Tab navigation, closed on Escape and returned focus to its trigger.
- Follow/unfollow changed status, lists and both users' counts correctly; the original follow state was restored. A valid PNG avatar uploaded, its returned URL was publicly readable, and its saved profile URL was checked before restoring the prior avatar. Ordinary accounts received 403 from account-ban and admin-configuration operations.
- All 59 final group HTTP checks passed. Outsiders and owners using another group's application ID received 403 without changing the application. Valid approval/rejection persisted the reviewer and decision; accepted users gained chat access. Ordinary members could not edit roles, group details or remove the owner; administrators could remove ordinary members, while the owner could assign numeric admin/member roles. Removed users lost chat access, and member counts followed the actual membership. Description edits persisted without changing the count. Both disposable groups were deleted after the checks.
- Two additional disposable accounts exercised private chat creation and delivery. Both participants could read history; an outsider's read/send requests returned HTTP 403 and its room subscription was rejected.
- A receiver obtained one private WebSocket delivery before logout. After logging out that exact token, HTTP returned 401 and the still-open socket received zero further private payloads during a 2.5-second observation, while the sender's subsequent message persisted successfully.
- The final rebuilt JAR passed all 19 private-chat/WebSocket runtime assertions. A username Principal registered the receiver's real persisted user ID in Redis; no invalid online IDs remained, and disconnect removed the owned identities. This reproduces and fixes the earlier null-identity registration failure outside the request thread. Authorization, delivery and passive logout revocation passed again on that final JAR.

Sanitized final chat results are retained in ignored `.local/verification/runtime-api-results-final.json`, group checks in `runtime-group-final.results.json`, and follow/avatar checks in `runtime-contract-check.results.json`. Earlier chat results remain in `runtime-api-results.json`. The earlier contract run recorded four fixture-reset failures caused by the member-removal bug; the final group run replaces those incomplete approval/removal checks. Browser previews and logs are in the same ignored verification directory. Test credentials are never part of these results or maintained documentation.

The packaged JAR was checked after the clean build: it contains all 30 create scripts, no obsolete uppercase package/resource paths and no private-key/keystore files. The temporary application, Vite server, isolated Chrome and both audit containers were stopped after verification; the containers and ignored evidence remain available for reproduction.

## Practical limits

Elasticsearch remains optional and disabled by default; a live Elasticsearch cluster and replacement certificates were not tested. Thread prototypes are disabled by default because their persistence tables are absent. No production migration, provider credential rotation, certificate replacement, repository history publication, commit or push was performed. A separate [history-cleanup preview](history-cleanup-preview.md) was verified across all 97 reachable commit trees; the original repository is unchanged. The required external actions are documented in `operations.md` and remain pending identification of the deployed services.
