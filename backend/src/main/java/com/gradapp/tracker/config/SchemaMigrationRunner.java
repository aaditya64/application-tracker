package com.gradapp.tracker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * StageType is an evolving enum (new stages get added over time), but Hibernate's schema
 * generation bakes the enum's values into a SQLite CHECK constraint on {@code current_stage}
 * and {@code stage}. SQLite can't ALTER a CHECK constraint, and Hibernate's ddl-auto=update
 * never touches constraints on existing columns - so an old database would reject writes of any
 * new enum value forever.
 *
 * This runs once per startup (no-op once already migrated) and rewrites the stored CREATE TABLE
 * text in sqlite_master to drop that CHECK clause entirely, in place, with no data movement -
 * existing rows are untouched. Entities now map those columns with an explicit columnDefinition
 * so fresh databases never get the constraint in the first place.
 */
@Component
@Order(Integer.MIN_VALUE)
public class SchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationRunner.class);

    private static final Pattern CHECK_CLAUSE = Pattern.compile(
            "\\s*check\\s*\\(\\(\\s*\\w+\\s+in\\s+\\([^()]*\\)\\)\\)",
            Pattern.CASE_INSENSITIVE);

    private final DataSource dataSource;

    public SchemaMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            Map<String, String> tablesNeedingFix = new LinkedHashMap<>();

            try (Statement listTables = connection.createStatement();
                 ResultSet rs = listTables.executeQuery(
                         "SELECT name, sql FROM sqlite_master WHERE type='table' AND name IN ('application','stage_history')")) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String sql = rs.getString("sql");
                    if (sql != null && CHECK_CLAUSE.matcher(sql).find()) {
                        tablesNeedingFix.put(name, CHECK_CLAUSE.matcher(sql).replaceAll(""));
                    }
                }
            }

            if (tablesNeedingFix.isEmpty()) {
                return;
            }

            log.info("Migrating SQLite schema: dropping stale stage CHECK constraint on {}", tablesNeedingFix.keySet());

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA writable_schema = ON");

                for (Map.Entry<String, String> entry : tablesNeedingFix.entrySet()) {
                    try (var update = connection.prepareStatement(
                            "UPDATE sqlite_master SET sql = ? WHERE type='table' AND name = ?")) {
                        update.setString(1, entry.getValue());
                        update.setString(2, entry.getKey());
                        update.executeUpdate();
                    }
                }

                long schemaVersion;
                try (ResultSet rs = stmt.executeQuery("PRAGMA schema_version")) {
                    rs.next();
                    schemaVersion = rs.getLong(1);
                }
                stmt.execute("PRAGMA schema_version = " + (schemaVersion + 1));
                stmt.execute("PRAGMA writable_schema = OFF");
            }

            try (Statement check = connection.createStatement();
                 ResultSet rs = check.executeQuery("PRAGMA integrity_check")) {
                rs.next();
                String result = rs.getString(1);
                if (!"ok".equalsIgnoreCase(result)) {
                    log.warn("SQLite integrity_check after schema migration reported: {}", result);
                } else {
                    log.info("Schema migration complete, integrity_check ok");
                }
            }
        }
    }
}
