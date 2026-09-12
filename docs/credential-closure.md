# Credential closure evidence

Current worktree removal, ignored recovery files and a scrubbed-history preview do not revoke an issued credential. Actual deployed targets and providers have not yet been supplied. Do not paste secret values into this table, issues, logs or chat. This inventory is an evidence checklist, not a claim that every configured optional service was deployed.

| Credential / material | Known source use | Actual target / owner | Required invalidation evidence | Status |
| --- | --- | --- | --- | --- |
| MySQL password | Application/test database configuration | Not supplied | Old password rejected on the identified server; replacement deployment works | UNVERIFIED |
| Redis credentials | Required cache/auth dependency; environment configurable | Not supplied | Rotation/access review on actual Redis service, if used | UNVERIFIED |
| JWT signing key | Bearer authentication | Deployment nodes not supplied | All nodes use new key; old signed token denied | UNVERIFIED |
| DeepSeek/API credentials | Optional AI provider configuration | Provider account not supplied | Provider confirms revocation; old key rejected | UNVERIFIED |
| SMTP credentials | Password-reset email configuration | Mail provider not supplied | Rotation if previously exposed/used; delivery with replacement verified | UNVERIFIED |
| Certificates/private keys | Historical repository material, local retired recovery copies | Certificate nodes/issuer not supplied | Replace deployed keys/certificates and revoke when applicable; old key no longer used | UNVERIFIED |
| Password-reset tokens | Independent random reset credentials in Redis | Deployment store not supplied | Review/revoke outstanding reset credentials separately from JWT key replacement | UNVERIFIED |
| Repository history | Local scrubbed preview exists separately from current fixes | Hosting platform/references not supplied | Reviewed scope preserves fixes; coordinated history publication and collaborator recovery | NOT PUBLISHED |

For each used deployment, record the operator, target, execution time, provider/deployment evidence reference and a sanitized old-credential rejection result. Confirm never-used credentials explicitly rather than marking them revoked. Keep recovery material access restricted; `.gitignore` is an exclusion rule, not encryption or access control.

Revoke or rotate at the issuer/service first, then handle historical copies and references. [GitHub's sensitive-data guide](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository) describes that general order; it does not establish this repository's hosting provider. Do not push the preview mirror blindly: it lacks the current uncommitted fixes and is not a release candidate. No production credentials, certificates, historical references or external deployments were changed by this local implementation.
