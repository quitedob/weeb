# Release-hardening verification — 2026-09-12

The local release gate passed on a frozen **uncommitted review snapshot**. Both isolated builds produced identical backend and frontend artifacts. This is not a published candidate or production acceptance: external credential/certificate work, target migration and domain/device validation remain unverified.

## Exact source and artifacts

| Identity | Value |
| --- | --- |
| Parent commit | `1167c19695ccdc167e2b9a01e03d9ba0ac2c0d04` |
| Run | `20260912T052939Z-b4fc8889` |
| Runtime completion, UTC | `2026-09-12T05:32:57.761Z` |
| Source inventory SHA-256 | `f4afc1f4fea9c5db99d3621b2552846dfde163bf7bf6204dfc3d8eb8cdf3e0f8` |
| Executable JAR SHA-256 | `06e796a8835a3b197bd651abc6afe4cda0aa0b9651bb5b8e5fed05a529cc928f` |
| Frontend tree SHA-256, 60 files | `030dcb03713dd81ac0bba64e1ec9c429cc04d2d32e64d248b1d9aa5797346d5b` |
| Evidence manifest SHA-256 | `b1493401f6e5fb43bd2598e318f17a485e1c6b2211af9e6abb59ecbf7eac18dd` |

The controlled [machine-readable manifest](evidence/release-2026-09-12/release-manifest.json) records every suite, all runtime checks, lockfile hashes, migration checksums, pinned service images, tool versions, frontend file hashes and 198 resolved backend dependency hashes. The [source inventory](evidence/release-2026-09-12/source-files.json) identifies every frozen input by normalized content and file mode. Its hash above is an inventory hash, not a Git tree object or a claim that the parent commit contains these fixes.

The original two source copies, artifacts and private logs remain under ignored `.local/release/20260912T052939Z-b4fc8889/`. Only the reviewed manifest, inventory and [browser screenshot](evidence/release-2026-09-12/browser-chat.png) were copied into `docs/evidence`; they contain no test passwords or tokens. The screenshot contains generated fixture content only; its SHA-256 is `36ee7a807b093ed4ea43fd3f0e018378fc4d800754ba9259ca5c3dcdd54709b6`.

This report and final documentation/evidence updates were added **after** source capture. Production code, build configuration and tests were not changed after that capture. The working tree remains dirty; no commit, push, deployment or original-history rewrite was performed.

The final source comparison confirmed that only README/documentation/evidence changed after capture, and `git diff --check` passed. The owned application, preview and isolated browser exited; both named audit containers were stopped after verification. Their data and ignored recovery/evidence files were retained.

## Executed checks

| Check | Result |
| --- | --- |
| Backend clean verify | **307 tests / 53 suites**, zero failures, errors or skips |
| Frontend full Vitest run | **159 tests / 28 suites**, zero failures, errors or skips |
| Required HTTP/STOMP/browser assertions | **23/23 passed** on the packaged application and built frontend |
| Two independent builds | Identical JAR SHA-256 and frontend file manifest |
| Packaged standalone migration CLI | Versions `001`–`004` validated as APPLIED with matching checksums |
| Active CSS/Vue color scan | **59 files / 454 literal occurrences**, no purple styling findings |
| Release-gate negative tests | **5 passed**, rejecting missing/skipped/failed/empty JUnit results and missing/invalid runtime evidence |

The audit used disposable loopback MySQL 8 and Redis 7 containers with the exact image digests in the manifest, loopback SMTP tests, Java 17.0.12, Maven 3.9.9, Node 20.19.4, npm 10.8.2 and Python 3.12.0. The full command was:

```powershell
python -m unittest discover -s scripts -p test_release_gate.py
python scripts/release_gate.py --allow-dirty --offline --chrome "C:/Program Files/Google/Chrome/Application/chrome.exe"
```

Environment setup and fresh CI fixtures are documented in [release instructions](release.md). Each source directory ran a separate `npm ci` and clean Maven build; the second build skipped tests after the first had passed the full suites. Download caches were reused, as explicitly recorded by `offlineDependencyCache`. Reproducibility was proved for two directories on this recorded host/toolchain; another authorized developer still needs to reproduce the eventual clean candidate on another machine.

Earlier gate attempts failed on an incomplete offline npm cache and then nondeterministic SockJS CommonJS wrappers. Those attempts are not the passing result. The final source fixes the wrapper configuration, and this complete run rechecks real browser transport after both hashes match. Existing mixed static/dynamic import warnings remain; no performance or load claim is inferred from them.

## What the evidence establishes

- Real SQL tests cover sender-scoped message retry deduplication, conflicting payload rejection, message/unread/outbox rollback, dispatch failure recovery, expired worker leases, authorization changes before delivery, concurrent bounded reads, SQL cursor recovery, desired-state reactions and refresh of old read/reaction/recall state. Worker failure windows are exercised through controlled faults and lease state; this does not claim an operating-system kill/restart chaos campaign.
- Real MySQL/Redis tests cover durable logout and account-wide revocation across partial cache loss/restoration, one-winner renewal, expiry while waiting for a row lock, and dependency failure rollback. Outbound STOMP authorization rechecks existing subscribers. Redis remains required; the ledger is not protection against loss or rollback of the authoritative MySQL data itself.
- Seven migration tests cover fresh install/repeat, known historical `da3cf23` schema/data retention, custom category IDs, checksum/newer-version rejection, committed-DDL failure and explicit resume, lock exclusion and read-only production validation. They do not establish compatibility with an unseen production schema or data set.
- Deferred-response frontend tests cover A-to-B account changes, token rotation, old HTTP/socket results and cross-tab renewal. Six cross-tab cases include a loser response before winner storage, queued logout, 401/business-1002 races and Web Locks fallback. These are deterministic automated cases; the live browser journey uses one isolated browser account.
- Runtime verification checks actual registration/login, expiry units, nested profile and follow aliases, private chat creation, outsider denial, same-ID retry/conflict, recipient delivery and sender acknowledgement, SQL recovery, and a read boundary that preserves a later unread message. Chrome logs in, sends through real STOMP, and displays one persisted message through same-origin `/api` and `/ws`. After recipient logout, its token is rejected and its still-open socket receives no subsequent private payload during the observation window.

The final screenshot was visually inspected: the interface remains blue/neutral. Current CSS/Vue changes preserve the existing style blocks; the purple-heart emoji remains a user message-content choice.

## Remaining acceptance boundaries

| Area | Remaining work |
| --- | --- |
| External security | Identify actual deployments/providers; revoke or rotate exposed credentials, replace deployed keys/certificates, and record old-credential rejection. See [credential evidence checklist](credential-closure.md). |
| Publication | Review and commit the fixes, rebuild the clean candidate, arrange approved history publication without losing these fixes, and retain the exact release artifacts. No remote history was changed. |
| Production migration | Back up and rehearse the actual old database; apply using a migration account; verify rollback/recovery and formal-domain/device API, SockJS, uploads and reset links. Local migration tests do not substitute for this. |
| Session model | Still-valid Bearer renewal and localStorage remain. No independent refresh credential, HttpOnly cookie migration or absolute session lifetime was introduced. Automatic cross-tab renewal requires Web Locks; without it the token expires normally and the user signs in again. |
| Delivery topology | Supported deployment remains one application instance with Simple Broker. Outbox dispatch is not browser receipt; retries may repeat delivery events. Legacy clients without a stable `clientMessageId` do not gain send idempotency. |
| P2 and other business boundaries | Server pagination for owned/joined groups, representative search execution plans/load, cache bounds, polling cost, upload-abuse limits and remaining article/coin/governance scenarios are still separate work. No target-load or complete-business-boundary acceptance is claimed. |
| Optional services | Thread remains disabled without its maintained persistence chain. A live Elasticsearch cluster and multi-instance STOMP delivery were not tested. |

This phase closes the documented local source, contract, migration-test and reliability-test gaps; it deliberately keeps the external and broader acceptance boundaries visible.
