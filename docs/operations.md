# Configuration and credential remediation

Export the variables described in the repository `.env.example` before starting the backend. Spring Boot does not load that file automatically. `JWT_SECRET` and `MYSQL_PASSWORD` are required; use a newly generated random JWT secret with at least 32 bytes. Configure SMTP through `SPRING_MAIL_*`, a verified `PASSWORD_RESET_FROM`, and the deployed application's `/reset-password` URL. Password reset cannot deliver mail until these settings are valid.

The database's persisted `user.type` is the authority for roles. Registration always creates USER accounts. Provision an administrator by explicitly updating the intended account's type to ADMIN through an authorized database administration process. Never infer roles from usernames. Existing installations need the SQL migration documented in `src/main/resources/sql/README.md` before deployment. The local source cleanup does not change an existing deployed database or any cloud account.

The current worktree no longer embeds database/group passwords, the JWT signing key, or the DeepSeek API key. Retired PKCS#12 files have been moved to ignored `.local/retired-keys/` for local recovery and must not be deployed or recommitted. Treat the original values and private keys as exposed because they remain in earlier commits.

On 2026-09-12, the credential examples in `docs/report.md` were replaced with typed `[REDACTED: ...]` placeholders while preserving their original locations and audit findings. A scan of 586 current text files against the previously committed configuration found no retained retired credential values or private-key artifacts. The scan excluded `.git`, `.local`, `target`, `node_modules`, and `dist`; a matching fragment in a synthetic test UUID was verified as test data. Ignored recovery files and repository history remain outside that source scan. External credential rotation, certificate replacement, and history cleanup/publication remain pending as described below.

## Remaining operational actions

1. Rotate the database account password at the database server, replace the group password if in use, revoke/reissue the DeepSeek key in its provider account, and generate a new JWT secret. Configure the new values in the deployment environment. A changed JWT secret invalidates tokens signed with the previous key.
2. Replace Elasticsearch HTTP and transport private keys/certificates wherever the committed keystores were used. Roll out replacement trust material to the relevant clients/nodes before retiring the old keys.
3. Back up the repository and coordinate with collaborators before rewriting history. In an isolated mirror, remove `src/main/resources/es/http.p12` and `src/main/resources/es/transport.p12` from every ref and replace the exposed configuration values. Review all rewritten refs and run a secret scan, then explicitly authorize publication of the rewritten history. Do not print the replacement-text file or reuse retired values.
4. Verify the deployed application accepts new credentials, rejects old JWTs and reset credentials, connects with replacement certificates, and performs role-protected operations correctly. History cleanup alone does not revoke credentials.

Provider rotation and deployment verification require access to the actual services and remain unverified. The user separately requested repository history publication; its concrete candidate, reference inventory, complete-source preservation and final publication status are tracked in [the verified history workflow](history-publication.md). The older local-code verification phase did not publish history.

A separate local [history-cleanup preview](history-cleanup-preview.md) now verifies all 97 reachable commit trees, removes the identified credentials and keystores, and leaves the original repository unchanged. Its recovery bundle was verified. Generated credential-replacement files were removed after validation. The preview has no configured remote and excludes the current uncommitted fixes; review and integration are required before any publication. This preparation does not replace live credential rotation or server-side history cleanup.

## Local verification services

The current release gate uses the pinned images and fresh-host Compose recipe in [release instructions](release.md). Its test bootstrap migrates only `127.0.0.1`/`localhost:23306/weeb_audit`; migration tests own separate random `weeb_audit_migration_*` databases. Production uses the standalone [schema CLI](schema-migrations.md) and read-only startup validation. Credential usage and issuer-side rejection evidence remain tracked in [credential closure](credential-closure.md).

During remediation, isolated Docker containers named `weeb-audit-mysql-20260911` and `weeb-audit-redis-20260911` provide MySQL on loopback port 23306 and Redis on loopback port 16379. Their temporary database and generated credentials are separate from the application deployment. Credentials are stored only in ignored `.local/verification/mysql.env`.

The opt-in `MySqlContractIntegrationTest` reads `WEEB_TEST_MYSQL_URL`, `WEEB_TEST_MYSQL_USERNAME`, and `WEEB_TEST_MYSQL_PASSWORD`, requires a database name containing `weeb_audit`, loads the relevant production schema/mappers, and rolls back each test's data. Ordinary test runs skip that integration class unless explicitly configured.

The wrapper uses the project's existing script-only Maven Wrapper with Maven 3.9.9. See the [Apache Maven Wrapper documentation](https://maven.apache.org/tools/wrapper/) for bootstrap and checksum options.
