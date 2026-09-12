# Schema lifecycle and recovery

The single active registry is `src/main/resources/sql/schema-manifest.json`. The dedicated `SchemaMigrationCli` loads exactly that registry without starting Spring, HTTP, scheduled jobs, JWT or Redis. Development initialization and disposable integration tests use the same runner. Production startup only validates its ledger and required tables.

| Version | Purpose |
| --- | --- |
| 001 | Frozen repository baseline: 30 table scripts, reference data and indexes |
| 002 | Persisted role normalization; old reaction timestamp and missing constraints/index compatibility |
| 003 | Durable `auth_user_session` and `auth_token_revocation`; pre-migration tokens must sign in again |
| 004 | Sender-scoped client message IDs, reaction revisions and durable message outbox |

Baseline category seeding resolves parent IDs by unique category name (`04_insert_article_categories_by_name.sql`). This preserves existing custom categories and supports legacy catalogs whose IDs differ from fresh-install defaults; the old hardcoded-ID seed remains historical reference only.

The ledger `weeb_schema_history` records version, description, SHA-256 and APPLYING/APPLIED/FAILED state. Resource paths and UTF-8 contents (CRLF normalized to LF) form the checksum. Never edit an applied version; add a subsequent migration. A MySQL named lock serializes schema work. Unknown newer versions, checksum mismatches and incomplete migrations block normal execution.

## Operator commands

Set `MYSQL_URL`, `MYSQL_USERNAME` and `MYSQL_PASSWORD` in the process environment for the **explicit target database**. The CLI expects that database to exist. Use a migration account with schema privileges; the production application account only needs normal runtime privileges and read access to the ledger/metadata.

```powershell
java -Dloader.main=com.web.migration.SchemaMigrationCli -cp target/WEEB-0.0.1-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher --plan
java -Dloader.main=com.web.migration.SchemaMigrationCli -cp target/WEEB-0.0.1-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher --apply
java -Dloader.main=com.web.migration.SchemaMigrationCli -cp target/WEEB-0.0.1-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher --validate
```

`--plan` (also the default) reads metadata and does not create the ledger or acquire a lock. It lists pending resources, not a complete old-data compatibility proof. Before production apply: back up, inspect existing schema and role values, check orphan reactions and sender/client-ID duplicates, stop old writers, and rehearse the upgrade on a restored copy. Arbitrary drift outside the documented baseline requires a reviewed migration; table existence alone cannot prove full schema equivalence. Ambiguous legacy reaction timestamp columns fail instead of choosing which values to discard.

The old `migration` application profile now refuses to perform migration and directs operators to this standalone CLI. It previously referenced a missing SQL file and would start unrelated application components. Do not use `db/migration` prototypes or individual `sql/create` scripts as a second live migration mechanism.

## Failure and rollback

MySQL DDL may already have committed when a step fails. The runner records FAILED (or a crash may leave APPLYING); ordinary startup/apply remains blocked. Inspect the failed resource and existing data, restore a verified backup or correct the external condition, then explicitly resume the **same checksum**:

```powershell
java -Dloader.main=com.web.migration.SchemaMigrationCli -cp target/WEEB-0.0.1-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher --apply --resume-failed
```

There is no automatic destructive repair or rollback. Adding unique/FK constraints fails on conflicting data rather than deleting it. Returning to an older JAR is not a database rollback: older registries reject newer versions, and old auth/message code does not implement V003/V004 guarantees. Restore a consistent database/application pair according to the deployment backup plan.

`SchemaMigrationIntegrationTest` uses isolated randomly named databases on the owned local MySQL audit service. Its fixtures include historical DDL from `da3cf23`, with checksums under `src/test/resources/sql/legacy/da3cf23/manifest.json`. It covers empty install, legacy data retention, repeated apply, checksum/new-version rejection, committed-DDL failure and explicit recovery, lock exclusion and production read-only validation. These rehearsals do not certify a production database whose actual structure/data have not been supplied.
