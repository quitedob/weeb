# Report remediation

Source: `docs/report.md`. Goal: repair and verify every reported bug, accounting separately for structural recommendations and operations that require control of deployed services.

## Implementation and ownership

1. Authentication agent: persisted user-type authorization, privileged API guards, password-safe serialization, token revocation, scoped single-use password reset and SMTP delivery, real password/status writes, follow APIs, avatar uploads, coin validation, focused backend tests.
2. Chat agent: participant and accepted-group membership checks for HTTP and STOMP reads/writes/subscriptions, authenticated sender identity, consistent private queue delivery, unread counts, one atomic contact-expiration task, focused backend tests.
3. Frontend agent: success envelopes and API transport, sanitized HTML, application-wide WebSocket lifetime, accessible controls and dialogs, reachable theme selection, themed chat/history pages, usable feedback, regression tests.
4. Coordinator: build wrapper, credentials/configuration, integration contracts, serial dead-code/package cleanup, documentation, complete verification and independent review after the writer barrier.

No commits, publication, production migration, credential-provider changes, or rewriting of the original repository are part of the local implementation steps. A separate local history-cleanup preview is prepared for review. Prepare concrete operational instructions before requesting any necessary external authorization. Preserve the unique legacy emoji behavior in the active component; archive abandoned thread source outside production rather than silently lose it.

## Acceptance register

| Report | Required evidence | Status |
| --- | --- | --- |
| 1 / 4.1 | Maven wrapper runs; wrapper properties are not ignored; one frontend manifest; relevant ignore rules | PASS: Windows and Git Bash wrapper bootstrap; backend tests and frontend build |
| 2.1 | Registration cannot confer admin through username or supplied type; stored admin type grants authorized access | PASS: PersistedUserRoleTest, AdminEndpointAuthorizationTest |
| 2.2 | Ordinary users denied admin operations; valid admin succeeds; invalid coin values cannot alter balances | PASS: admin/coin tests and real MySQL atomic balance assertions |
| 2.3 | Nonparticipants denied reads, writes and subscriptions; members can chat; sender identity cannot be forged | PASS: HTTP/STOMP authorization and identity regressions, real MySQL membership/search tests, outbound delivery guards |
| 2.4 | No configuration secrets/private keys in resulting source tree; environmental configuration; actual rotation/history purge separately verified | PASS for current source scan/redaction and the [isolated history preview](history-cleanup-preview.md), covering all 97 reachable commit trees; BLOCKED for live rotation/certificate replacement/history publication: deployment/provider access is not identified |
| 2.5 | Serialized users contain no password/hash | PASS: Jackson regression and actual registration/login/profile responses |
| 2.6 | Logged-out tokens and tokens predating password changes are rejected | PASS: JWT revocation/status tests and real Redis expiry/shared-revocation tests |
| 2.7 | Reset credential is short-lived, single-use, delivered privately, never an access token or log entry | PASS: reset security, Redis GETDEL/expiry and loopback SMTP delivery tests; deployed SMTP configuration remains operational work |
| 2.8 | All retained HTML rendering sanitizes attacker-controlled content; malicious fixtures tested | PASS: safeHtml fixtures, feedback tests and rendering-source inspection |
| 3.1 | Registration, article actions, profile/settings/follow handle code 0 correctly | PASS: frontend API/user-flow tests and source scan; no code-200 checks remain |
| 3.2 | Follow lists/counts/status, avatar, group application handling and reactions use implemented contracts | PASS: API/page tests and HTTP flows; 59 group checks plus 45 private-group/reaction checks, nested group profile rendering, notification pagination/routes, real MySQL capacity/rollback and comment/reaction regressions |
| 3.3 | Login/reload connects; route navigation preserves connection; logout disconnects; notifications arrive | PASS: lifecycle/realtime tests; browser login, reload and chat/settings navigation; 19 final packaged WebSocket assertions verify persisted identity, disconnect cleanup, delivery and passive logout revocation. Vite preserves browser globals for SockJS |
| 3.4 | Exactly one contact-expiration owner; expired rows retained and acceptance races cannot overwrite accepted rows | PASS: five real two-connection MySQL races plus expiry/acceptance guards; deadlock-only bounded scheduler retry |
| 4.2-4.4 | Verified unused source removed; lowercase package/resource names; references and builds remain valid | PASS: compile/test/build and old-package reference scan; unused helpers and duplicate jobs removed |
| 4.5 | Active feature paths consolidated; unique emoji behavior retained; dead source archived/removed; aliases align; debug artifact absent from production | PASS: feature paths and aliases resolve, paginated emoji regression, ignored legacy archive; thread prototype disabled by default because persistence schema is absent |
| 5.1 | Theme control reachable and persisted | PASS: browser header controls and theme reload; both theme previews reviewed |
| 5.2 | Switch works with keyboard, exposes state, respects disabled | PASS: accessibility tests; actual Space/Enter toggles, saved preference survives page reload |
| 5.3 | Dialog semantics, initial focus, Tab trap, Escape and focus restoration work | PASS: shared modal regression, New Chat page journey test and actual browser focus/Tab-wrap/Escape restoration |
| 5.4 | Active chat/history styles respond to light/dark tokens | PASS: theme token inspection and both browser previews; all 59 active CSS/Vue files cleared of purple styling |
| 5.5 | Primary flows use application notifications/confirmation without unsafe HTML | PASS: feedback tests and source inspection; final native alert replaced in diagnostic page |
| 5.6 | Correct document language/title | PASS: browser confirms zh-CN and route-specific Weeb titles |
| 6 | Maintained docs moved into docs and claims/paths/counts match actual implementation | PASS: backend/frontend/stores/SQL/Elasticsearch documentation reconciled with source; operational limitations explicit |

## Baseline

- PASS: `mvn -o test` builds the initial backend, but executes **zero tests**.
- PASS: `npm run build` in `Vue` builds the initial frontend; existing mixed static/dynamic import warnings.
- Confirmed correction to report 2.1: `user.type` is already persisted. Reuse that authority instead of introducing a redundant role column.
- Confirmed additional root causes in affected paths: password/status updates omit database writes; HTTP chat send also lacks membership checks; STOMP queue identity parsing mismatches the client.

## Final gates

Focused backend/frontend regressions, clean backend build/tests, frontend build/tests, current-tree diff review, independent security/contract review, and browser/runtime checks where dependencies can be provisioned safely. Use PASS/FAIL/BLOCKED/SKIPPED/OUT_OF_SCOPE with concrete evidence; do not equate a build with runtime verification.

## Follow-up functionality review

The user's deletion and frontend/backend compatibility recheck is recorded in [functionality-recheck.md](functionality-recheck.md). Package/page moves and unused modules were traced against their earlier callers. Missing common emoji choices and meaningful message relevance were restored. Additional fixes align password recovery, group profiles, notification metadata/navigation, article sorting and per-resource search sort parameters. No active page route was removed by the reviewed cleanup; the thread prototype's default-off backend and the SQL search semantics are disclosed.

That functionality phase passed **256 backend tests, zero skips; 101 frontend tests across 22 suites; both production builds passed**. Actual MySQL/Redis, HTTP/STOMP and isolated Chrome evidence is in [verification.md](verification.md). That JAR also passed 28 further search/notification HTTP checks. README covers setup, maintained paths, API distinctions, retained features and optional-service limits. External operational tasks in the acceptance register remain separate.

## Release-hardening follow-up

The [subsequent implementation plan](release-hardening-plan.md) adds sender-scoped idempotency and transactional outbox, bounded reads and reconnect state recovery, durable authentication generations/revocation, session and cross-tab renewal protection, versioned schema/recovery tools, executable contracts and a reproducible release gate. No active source file was deleted in this phase.

The frozen final review snapshot passed **307 backend tests, 159 frontend tests and 23 real HTTP/STOMP/browser checks** with zero skips. Both isolated builds produced identical backend/frontend hashes. The [controlled verification report](release-verification.md) records exact source identity and limitations. External credential/certificate work, production migration and publication remain unverified; P2 capacity and the wider business-boundary review are not marked complete by these local results.
