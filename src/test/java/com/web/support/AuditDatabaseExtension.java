package com.web.support;

import com.web.migration.SchemaMigrator;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Explicitly registered test bootstrap; never migrates anything except the opted-in audit database. */
public final class AuditDatabaseExtension implements BeforeAllCallback {
    private static boolean initialized;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        String url = System.getenv("WEEB_TEST_MYSQL_URL");
        if (url == null || url.isBlank()) return;
        if (!url.matches("jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):23306/weeb_audit(?:\\?.*)?")) {
            throw new IllegalArgumentException("Integration bootstrap requires the dedicated loopback23306/weeb_audit database");
        }
        synchronized (AuditDatabaseExtension.class) {
            if (initialized) return;
            var source = new DriverManagerDataSource(url, System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"),
                    System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
            new SchemaMigrator(source).migrate(false);
            initialized = true;
        }
    }
}
