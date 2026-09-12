# History cleanup preview — 2026-09-12

A sanitized history copy is prepared at `.local/verification/history-preview-20260912.git`. This is a local review artifact. The original repository, index, working changes, stash and remote were not rewritten or published. The preview does not include the current uncommitted bug fixes.

## Verified scope

| Check | Result |
| --- | --- |
| Reachable original commits inspected | 97, including the local stash |
| Historical application configuration versions inspected | 13 |
| Reported credential categories addressed | MySQL password, group password, JWT signing key, DeepSeek API key |
| Longer credential values removed from file and commit contents | 5, including historical variants |
| Short historical credential assignments | Redacted in explicit credential contexts; unrelated usernames and identifiers preserved |
| Reachable objects after rewriting | 3,247 |
| Remaining identified credential values/assignments in that copy | 0 |
| Retired keystore paths in that copy | 0 |
| Original keystore blob versions still addressable in that copy | 0 of 4 |
| Commit trees compared with originals | All 97; changes limited to credential redaction and the two keystore removals |
| Content-edited paths | `env-example.txt`, `src/main/resources/application.yml` |
| Integrity | `git fsck --full` passed |
| Original repository refs/index/worktree snapshot comparison | Unchanged |

The original `main` was `da3cf2391ff9ae2e67bd741e500bce6b2f631c46`; the preview's rewritten `main` is `b19adfe653ddefef38ab7c2df99049e57e18fc32`. The preview retains a rewritten local stash. The duplicate remote-tracking main was normalized by the history tool; the original repository's remote-tracking ref and stash remain intact.

The process used git-filter-repo 2.47.0 with sensitive-data removal, no fetch, exact keystore-path removal and credential replacements. Short common values were restricted to credential assignments to preserve unrelated text. The clone inherited a stash ref, so the tool's fresh-clone check stopped the first attempt. The retry verified the exact disposable bare-clone path, local origin and unchanged refs before permitting the rewrite there. See the [upstream history-removal documentation](https://github.com/newren/git-filter-repo/blob/main/Documentation/git-filter-repo.txt).

## Remaining work before publication

1. Identify the actual deployed services and rotate/revoke their credentials and affected Elasticsearch certificates. The preview does not perform or prove rotation.
2. Preserve the current fixes and any collaborator changes, then integrate the approved source changes onto the rewritten branch and verify that final candidate. This preview alone is not a deployment candidate.
3. Confirm the hosting provider and complete current remote-ref inventory. This local-only snapshot does not prove which refs or cached objects exist on the server.
4. Review the exact publication refspecs and coordinate affected clones before authorizing a history replacement. Do not blindly mirror-push a local snapshot or publish the stash.
5. Verify server-side history cleanup and provider-side credential rejection after the authorized changes.

No remote is configured in the preview, and no fetch, push or provider credential operation occurred. An original-history recovery bundle is retained in ignored `.local/verification/before-history-preview.bundle`; it contains exposed historical material and is not a deployment artifact. Sanitized evidence is in `.local/verification/history-preview-results.json`.
