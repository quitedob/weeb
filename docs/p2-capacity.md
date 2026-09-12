# P2 local capacity validation

This profile measures a single Spring Boot JVM against the pinned local MySQL/Redis services. Its scope is `LOCAL_SINGLE_JVM_BASELINE`, not a production-user capacity claim or a clustered-chat test. The initial target is zero response/contract/privacy errors and P95 at most 1,000 ms for each endpoint and each overall stage.

## Reproduce

Use Python 3.10+, Docker CLI and Java 17. Export the private audit MySQL password as `WEEB_TEST_MYSQL_PASSWORD`; use only the owned loopback MySQL port 23306 and Redis port 16379. See [release service setup](release.md). The harness creates a random owned database with an ownership marker and uses Redis logical database 15. It never flushes Redis, drops the shared audit database or stops an unrelated process/container.

```powershell
python scripts/capacity_probe.py --jar <exact-release.jar> --paged --docker-stats
# Compare another JAR using the same generated data and accounts:
python scripts/capacity_probe.py --jar <other-release.jar> --fixture .local/capacity/<first-run>/private/fixture.json --paged --docker-stats
```

The default fixture has 100,000 messages (half private, half group), 1,000 groups, two fixture accounts and 1,000 group memberships. The measured actor can see all messages. Stages run 1, 5 and 20 concurrent HTTP requests, each with five separate warmup requests and 75 measured requests equally distributed over message relevance search, chat history, unread stats, created groups and joined groups. Pagination is page zero/size ten for search/groups and page one/size ten for history; joined groups exclude owned groups before counting.

This is a short closed-loop burst with one actor, no think time and a new HTTP connection per request. It preserves normal server rate limits. It does not certify sustained traffic, multiple accounts, all search terms/date filters/deep pages, a production data distribution, multi-instance routing, or long-duration memory stability. Record a deployment-specific workload before making those claims.

`public/result.json` records every measured latency/status/business code, nearest-rank P50/P95/P99, throughput, response bytes, JVM CPU/RSS samples, optional container samples, image IDs, exact JAR/tool hashes and unchanged before/after data fingerprints. Actual selected-JAR SQL is captured through the production search builder and explained by MySQL; no handwritten approximation substitutes for its plans. The capture-only JDBC fixture also exercises empty-page count fallback. Credentials, SQL fixture inputs and JVM logs remain in ignored `private/` files. A completed failed target exits nonzero and retains its evidence.

## Implemented changes

- Created/joined groups use server COUNT/LIMIT and stable ordering. No-parameter array endpoints remain compatible; page consumers request bounded results and authoritative membership roles.
- Search bounds raw query length, distinct terms and result depth. V005 stores the original lowercase JSON text with binary collation, preserving literal OR/ranking behavior, accents, emoji, null/missing content and automatic updates. It is not a full-text search migration.
- Chat retains a 50-conversation LRU and 200 persisted-message windows. Active and pending/failed sends are pinned; older/newer/latest navigation reloads history without discarding unsent work. Initial connection fetches only the active conversation's history.
- Notifications retain a 100-item history window; connected unread reconciliation falls from once every 30 seconds to once every five minutes. Hidden/offline polling stops, failures back off, and event bursts coalesce. Request/session generations prevent stale search/page results from replacing active data.

Measured result snapshots and their exact artifact identities are retained under `docs/evidence/p2-2026-09-12/`. Final publication additionally requires a passing capacity manifest for the exact clean release JAR, checked by [history publication](history-publication.md). Earlier failed measurements remain historical evidence rather than being relabeled as passes.
