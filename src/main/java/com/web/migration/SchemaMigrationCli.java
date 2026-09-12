package com.web.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Arrays;
import java.util.Set;

/** Standalone migration entry point: no web server, schedulers, JWT secret or Redis connection. */
public final class SchemaMigrationCli {
    private SchemaMigrationCli() {}

    public static void main(String[] args) throws Exception {
        Set<String> allowed = Set.of("--plan", "--apply", "--resume-failed", "--validate");
        if (Arrays.stream(args).anyMatch(arg -> !allowed.contains(arg))) throw new IllegalArgumentException("Use --plan (default), --validate, or --apply [--resume-failed]");
        boolean apply = Arrays.asList(args).contains("--apply");
        boolean resume = Arrays.asList(args).contains("--resume-failed");
        boolean validate = Arrays.asList(args).contains("--validate");
        if ((resume && !apply) || (validate && apply) || (Arrays.asList(args).contains("--plan") && (apply || validate))) throw new IllegalArgumentException("Conflicting migration options");
        String url = required("MYSQL_URL");
        if (!url.matches("jdbc:mysql://[^/]+/[A-Za-z0-9_]+(?:\\?.*)?")) throw new IllegalArgumentException("MYSQL_URL must name an existing MySQL database");
        var source = new DriverManagerDataSource(url, required("MYSQL_USERNAME"), required("MYSQL_PASSWORD"));
        var migrator = new SchemaMigrator(source);
        if (validate) migrator.validate();
        var result = apply ? migrator.migrate(resume) : migrator.plan();
        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must be configured");
        return value;
    }
}
