# P2 capacity and history publication — 2026-09-12

The user requested the remaining history-publication and P2 capacity work, and asked whether campus space is implemented. Preserve existing features, the current uncommitted fixes, blue/neutral styles and the single-instance deployment scope.

## Source findings and scope

- Created/joined group APIs return complete arrays; the management page slices and excludes owned groups after download. Add explicit server pagination while retaining the legacy no-parameter list contract. Apply membership/owner filters before counting and limiting, with stable ordering. Discovery must retain authoritative membership even when that group is off the current page.
- Message search already bounds keyword length and page size. Add bounded distinct terms and depth, retain literal OR/relevance/privacy semantics, and inspect the actual query plans on representative data before changing SQL or indexes.
- Bound conversation/message caches without losing pending sends or making older history unreachable. Protect page/search responses from arriving out of order.
- Use realtime notification events with low-frequency reconciliation while connected, bounded backoff when disconnected, and visibility/session cleanup. Validate request counts and delayed-response behavior.
- Produce a reproducible local capacity baseline: default 100,000 messages, stages of 1/5/20 concurrent HTTP requests, actual latency/throughput/errors/payload and resource evidence. This is a recorded single-host baseline, not an inferred production-user capacity. User target clarification remains optional.
- The configured GitHub remote currently advertises only `refs/heads/main` at `1167c19695ccdc167e2b9a01e03d9ba0ac2c0d04`. Prepare a new sanitized history candidate containing all final fixes; do not publish the old incomplete preview or a stash. Check the remote again before any publication, use an explicit ref and expected old hash, and preserve recovery material locally.

## Ownership and verification

The coordinator owns history tooling, candidate integration, shared release configuration, documentation and serialized full builds. The backend writer owns group paging/role decoration, bounded search and focused backend regressions. The frontend writer owns paging consumers, bounded caches, polling and stale-request regressions. The independent capacity writer owns a disposable benchmark harness and baseline measurements. No concurrent writers share files.

Focused checks precede the complete release gate. Bind candidate source, artifacts, migration versions, test results and capacity results to exact hashes. Review history redactions against every original tree and scan the final candidate; source removal does not prove provider credential invalidation.

## Campus-space finding

No dedicated campus route, menu, school membership/verification schema, campus feed or school-scoped content policy exists. Generic articles, comments, profiles, following and groups are present. Message-thread prototypes are unrelated. Answer this current-state question explicitly; do not silently invent a campus feature specification or label the generic community as a completed campus module.
