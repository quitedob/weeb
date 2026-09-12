# Reproducible release review

Use Python 3.10+, Java 17, Node/npm compatible with the lockfile, an installed Chrome/Chromium and the Maven wrapper. Start the dedicated loopback MySQL `23306/weeb_audit` and Redis `16379` fixtures described in [operations](operations.md#local-verification-services). `ci/release-policy.json` records the pinned service image digests and required integration suites.

Export `WEEB_TEST_MYSQL_URL`, `WEEB_TEST_MYSQL_USERNAME`, `WEEB_TEST_MYSQL_PASSWORD`, and `WEEB_TEST_REDIS_PORT=16379`. The audit account needs permission to create/drop the test-owned `weeb_audit_migration_<random>` databases. Never point these tests at a deployed database. Chrome and the test application/preview use otherwise-free loopback ports 18082, 18080 and 18081.

On a fresh CI host, set a newly generated disposable `WEEB_AUDIT_MYSQL_ROOT_PASSWORD`, then run `docker compose -f ci/audit.compose.yml up -d --wait`. Use `jdbc:mysql://127.0.0.1:23306/weeb_audit?serverTimezone=Asia/Shanghai`, username `root`, and that same disposable password for the test variables. Tear down only this owned Compose project after evidence collection. Existing local audit containers can be reused when the ports and pinned image digests match; never stop unrelated services to free these ports.

```powershell
python -m unittest discover -s scripts -p test_release_gate.py
python scripts/release_gate.py --chrome "C:/Program Files/Google/Chrome/Application/chrome.exe"
```

For an explicitly uncommitted review use `--allow-dirty`. For a machine with complete Maven/npm caches add `--offline`; missing cached dependencies fail the build. Ordinary `mvn verify` can skip optional infrastructure tests; this release command rejects any skipped test or missing required infrastructure suite. It does not infer completion from a green unit-test-only run.

The gate snapshots all tracked and nonignored untracked files into two new isolated directories, canonicalizes UTF-8 CRLF to LF, then builds from those frozen inputs using `npm ci` and the Maven wrapper. It preserves the original worktree and never commits/pushes. Both directories have independent build/dependency output; caches may contain downloaded dependencies, and that offline mode is recorded. Maven archive timestamps derive from the parent commit. JAR SHA-256 and the sorted frontend file manifest must match across both builds.

The first build runs all backend/frontend tests, the packaged standalone schema validator, and real registration, HTTP authorization, message retries/conflicts, sender/receiver STOMP delivery, SQL sync, read, logout and browser login/chat journeys. Runtime uses a fresh random JWT key and newly generated disposable accounts; credentials and raw logs are not published.

Campus acceptance also exercises school creation, manual verification, protected uploads, publication review, reactions, replies, reports, current-permission notification delivery and revocation. Real browser journeys cover student/admin account switching, private images, draft edits, application and post review, comments/bookmarks and mobile layout. Its fixture helper discovers the single owned loopback MySQL container by port, verifies the audit URL, and elevates only a newly generated fixture account; cleanup removes only its generated schools and restores that account's role. It does not depend on a developer-specific container name.

Outputs are under `.local/release/<run>/`. `public/release-manifest.json` binds the parent commit, actual source tree/file hashes, worktree status, lockfiles, schema registry, artifact hashes, tool versions and sanitized test/runtime summaries. The browser image only contains generated fixture content. Upload reviewed `public/` files as CI artifacts; keep raw logs, source copies, browser profiles and secrets private. Retain the manifest beside the exact JAR and frontend tree it identifies. The provider-neutral command can be called by the repository's eventual CI platform; no hosting provider or CI account has been assumed.

`review_snapshot` means a dirty source tree was tested, even if every local gate passed. `release_candidate` requires clean Git state. Neither means `productionReady`: provider revocations, certificate replacement, approved history publication, actual production migration and formal-domain/device checks require deployment-side evidence. An independent authorized developer should rerun this command on the reviewed candidate commit before publishing. The local two-copy build checks deterministic artifacts within the recorded toolchain; cross-machine reproducibility still needs that second-machine run.

The deployment topology remains one application instance. MySQL/Redis durability, backup restore and credentials are operating requirements; this work does not add clustered STOMP routing, Elasticsearch validation, a production load target or a universal database downgrade.

The [P2 capacity probe](p2-capacity.md) measures a separate owned 100,000-message fixture against an exact JAR. [History publication](history-publication.md) prepares all current fixes on sanitized history and requires both this clean release gate and the committed local capacity target before publishing only the expected remote main. Public manifests, rather than a prepared preview or stale test count, establish the completion state.

Vite explicitly enables CommonJS `strictRequires` wrapping. The two-directory comparison exposed different SockJS conditional/cyclic module wrappers under automatic detection; [Rollup's CommonJS documentation](https://github.com/rollup/plugins/tree/master/packages/commonjs#strictrequires) describes this source of build races. This setting preserves required-module initialization semantics and is verified with the real browser transport, rather than merely accepting differing output hashes.
