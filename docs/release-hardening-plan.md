# Release and failure-recovery work — 2026-09-12

The input review distinguishes confirmed local fixes from release/deployment gaps and proposed architecture improvements. This iteration verifies the current source before changing behavior. It preserves the existing Spring Boot/MyBatis/Vue stack, active routes, blue/neutral styling, and the previous uncommitted fixes. No source cleanup, UI redesign, commit, push, production migration, provider credential change, or repository-history publication is implicit in these local steps.

## Acceptance and ownership

| Area | Local acceptance | Owner |
| --- | --- | --- |
| Source and release identity | Reproducible clean snapshot builds; source/dependency/schema/HTTP-event contract and artifact hashes; failed/missing/skipped integration suites cannot pass a release gate | Coordinator |
| Schema lifecycle | One ordered, checksum-verified migration registry; dry-run has no writes; fresh install, known historical-schema upgrade, repeat execution, checksum mismatch and interrupted migration recovery tests | Coordinator |
| Authentication | Correct expiry units; existing valid-Bearer renewal has one winner; datastore failure denies access with temporary-service status; durable generations/revocations prevent Redis rollback from resurrecting invalid tokens | Authentication agent |
| Frontend session ownership | A's late HTTP/socket success, error, 401, refresh or cleanup cannot overwrite or log out B; same-account rotation also protects its new token | Frontend agent |
| Message consistency | Sender-scoped stable client ID; same operation returns one row; changed payload conflicts; message/unread/outbox commit together; bounded read cursor cannot retreat or erase newer unread | Chat agent |
| Realtime contracts | Actual HTTP aliases, page bases, STOMP send/subscription/ack shapes, auth and status semantics recorded and covered by executable checks | Coordinator and frontend agent |
| Dynamic delivery | Recheck current authorization on delayed delivery; preserve terminal membership/dissolution notices; document single-instance broker support | Authentication/chat agents |
| External security closure | Usage inventory, rotation/revocation evidence and reviewed publication plan, tied to identified deployments/providers | Coordinator; actual target details still required |

Writers have disjoint file ownership. The coordinator owns build configuration, schema registry/initializer, release tools, maintained docs and final integration. Backend test execution is serialized. Code barriers precede full builds and independent reviews.

## Compatibility decisions

- Continue the existing Authorization Bearer model. Renewal requires an unexpired valid token; this iteration does not introduce independent refresh credentials or cookies.
- Add explicit `expiresAt` milliseconds and correct `expiresIn` seconds; the frontend accepts the previous absolute-millisecond `expiresIn` shape during compatibility transition.
- Preserve existing HTTP/STOMP routes and optional legacy send bodies. Clients supplying a stable operation ID gain retry idempotency; absent IDs cannot provide the same cross-request guarantee.
- Preserve legacy reaction toggle while adding explicit target-state operations; distinguish durable persistence, transport dispatch, client receipt and user read.
- Use MySQL as durable authority for messages and auth invalidation. Redis lease/cache data does not replace committed records.
- Keep the simple broker's supported topology to one application instance. A multi-instance deployment is not accepted by a successful two-tab test.
- Standardize the existing migration mechanism without introducing another schema framework. MySQL DDL is not transactionally reversible; failures remain recorded and require same-checksum resumable execution or a verified backup restore.
- A dirty-worktree snapshot may be built and hashed for review, but it is not silently labeled a clean committed release candidate. No local result proves live credential revocation or production readiness.

## Verification

Use real disposable MySQL/Redis plus deterministic deferred-response frontend tests and actual HTTP/STOMP failure scenarios. Retain portable scripts and sanitized machine-readable summaries as build artifacts. Run the final suites against an immutable source snapshot, record test inventory and hashes, then check rebuilt artifact equality in a second isolated directory. Existing production schemas and cloud credentials are outside disposable tests; their remaining gates must remain explicit.

## Local completion record

The final [frozen-snapshot gate](release-verification.md) passed 307 backend tests, 159 frontend tests and all 23 required HTTP/STOMP/browser checks. The second isolated build matched both artifact hashes. Independent authentication/chat/frontend contract reviews completed, including the cross-tab renewal race and stale-session responses. The source inventory, complete sanitized result manifest and reviewed browser screenshot are retained under `docs/evidence/release-2026-09-12/`.

The local implementation/verification rows above are complete for the stated acceptance scope. No production acceptance is implied: provider/deployment details are still missing for external security closure, the tested source remains uncommitted, cross-machine reproduction and target deployment checks remain pending, and P2 capacity/wider business boundaries have not been implemented in this phase.
