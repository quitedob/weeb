# P2 release review — 2026-09-12

The complete P2 source passed the clean-candidate release gate at commit `192ed574b81adf7cc9ba278bfb6682ab92011537`, source inventory SHA-256 `841cbc2d1dab229ba30f30d35f257b1fcd35a9a59f220d69c03e01be1841fa6d`. [Controlled evidence](evidence/p2-release-review-2026-09-12/release-manifest.json) records its exact environment, dependency locks, schema checksums and artifact identities.

| Check | Result |
| --- | --- |
| Backend | 324 tests in 56 suites; zero failures/errors/skips, including real MySQL/Redis |
| Frontend | 191 tests in 30 suites; zero failures/errors/skips |
| Runtime | 23 real HTTP/STOMP/browser assertions passed |
| Reproducibility | Two independent frozen-directory builds produced identical JAR and frontend files |
| Schema | Versions 001–005 applied and packaged production validator passed |
| CSS/Vue color audit | 59 files, 454 literal occurrences, zero purple findings |
| Release/capacity/history helper tests | 30 Python tests passed, including real isolated history filtering and preservation of source/index/stash |

The tested JAR SHA-256 is `7a2892265bcb16c2f5a4a68a9884b27843522f6c9f6faa841c540e8197caaa4d`; the frontend tree SHA-256 is `53a8d53a028f0f47edbe5228ddf9956032ead7422170a578df1e6c247a87da1b`. The [source file inventory](evidence/p2-release-review-2026-09-12/source-files.json) and [generated-fixture browser capture](evidence/p2-release-review-2026-09-12/browser-chat.png) accompany the manifest.

The [capacity comparison](p2-capacity.md) records actual baseline failures and the subsequent passing single-scan implementation using unchanged 100,000-message data. Its measured P95 improved from 1,907 to 797 ms at concurrency 20, with zero response/contract/privacy errors. Those comparison JARs are separately identified; they are not substituted for the eventual published candidate's exact-JAR measurement.

This report, its controlled artifacts and clarified operational documentation were added after this candidate's source capture. Production code/build/tests were unchanged after that capture. Publication therefore requires another clean candidate containing these documentation changes, the complete release gate on that candidate and capacity results for its exact JAR. Only the [history publication manifest](history-publication.md) can establish that a reviewed ref was actually pushed and remotely verified.

These checks cover one application instance and a local short-burst workload. Provider credential invalidation, deployed certificate replacement, actual production migration, production-domain/device checks, cross-machine reproduction, sustained target load and clustered STOMP routing remain separate acceptance items. Dedicated campus space is not implemented: the current generic community lacks school affiliation, student verification, a campus feed and school-scoped visibility.
