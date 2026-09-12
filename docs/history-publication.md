# Verified history publication

The original working repository and its uncommitted fixes are preserved. `scripts/history_release.py` prepares a new isolated repository containing sanitized `main` history and a new commit with the complete current source. It does not publish the older history preview or any local stash. Only an explicitly verified candidate can be published.

The reviewed remote is `git@github.com:quitedob/weeb.git`; its initial advertised inventory on 2026-09-12 contained only `refs/heads/main` at `1167c19695ccdc167e2b9a01e03d9ba0ac2c0d04`. Preparation and publication independently recheck all advertised refs. A new branch/tag or changed main blocks this single-branch workflow and requires a new inventory; the script cannot overwrite an unseen collaborator update.

The final preparation recheck detected new user commits `10008fa` and `c1e4bf3`, followed by remote commit `d0c3539` deleting `.mvn/wrapper/maven-wrapper.properties`. The original branch was safely fast-forwarded to that exact descendant while retaining the local fixes; the wrapper properties were restored as a reviewable source change because the checked-in Maven wrapper needs them. The new preparation baseline is `d0c35390e5249bc02130f997705333f41899e71a`; the intervening commits remain part of the historical comparison.

## Prepare and verify

Install pinned `git-filter-repo==2.47.0` in an isolated Python environment, then supply its Python executable. On this workspace the existing interpreter is `.local/verification/history-tools/Scripts/python.exe`.

```powershell
python -m unittest discover -s scripts -p "test_*.py"
python scripts/history_release.py prepare --filter-python .local/verification/history-tools/Scripts/python.exe
```

The command prints `.local/history-release/<run>/public/history-manifest.json`. The same directory contains a clean `candidate/` checkout and a bare `candidate.git`. The manifest binds the expected old ref, candidate commit, source inventory, every historical tree comparison and reachable-object scan. All historical commits are retained: only reviewed credential replacements and the two retired `.p12` key paths may differ. The final candidate tree must match the entire captured working source byte-for-byte after documented UTF-8 line-ending normalization.

Run the [complete release gate](release.md) from the clean candidate checkout. Then run the [capacity probe](p2-capacity.md) against that release's exact `source-a/target/WEEB-0.0.1-SNAPSHOT.jar`. When reusing the original workspace's private capacity fixture, invoke the original workspace's identical `capacity_probe.py`; its file hash must match the committed candidate tool. Do not reuse capacity results for another JAR.

## Publish the exact reviewed ref

Supply the preparation directory and both completed public manifests:

```powershell
python scripts/history_release.py dry-run --output .local/history-release/<run> --release-manifest <release-manifest.json> --capacity-manifest <capacity-result.json>
python scripts/history_release.py publish --output .local/history-release/<run> --release-manifest <release-manifest.json> --capacity-manifest <capacity-result.json>
```

These commands require clean candidate identity, identical two-build artifacts, complete zero-skip integration/runtime tests, the committed migration checksums/lockfiles/contracts, pinned MySQL/Redis images, actual CPU/RSS/query evidence and capacity results recalculated from successful raw samples. Failed latency targets block publication.

Publication sends exactly `candidateCommit:refs/heads/main` with atomic push and an [explicit expected-value force-with-lease](https://git-scm.com/docs/git-push). It does not mirror-push, send tags/stashes or bypass branch protection. Only a successful push followed by matching remote verification changes the public history manifest to `PUBLISHED`; it also records hashes of the release and capacity evidence. A prepared candidate or successful dry run is not publication evidence.

Keep recovery bundles, historical secret values, raw logs and filter callbacks in the ignored private directory. Do not copy the original repository's retired history back onto the rewritten remote. Collaborators should use a fresh clone and carefully reapply their own unshared changes; retain the original local working tree until those changes are accounted for.

## Security boundaries

The scan covers discovered historical credentials, retired key paths and private-key objects reachable from the candidate. Publication does not revoke provider credentials, replace deployed certificates, purge hosting caches/hidden refs, or clean other clones. [GitHub's sensitive-data guidance](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository) treats those as separate actions. Track provider-side evidence in [credential closure](credential-closure.md); do not treat a clean source scan as proof that old secrets are unusable.
