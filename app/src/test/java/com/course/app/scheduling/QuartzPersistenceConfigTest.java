package com.course.app.scheduling;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Розклад зберігається в БД: таблиці QRTZ_* створює Liquibase, а Spring Boot їх не створює.
 */
class QuartzPersistenceConfigTest {

    private static final List<String> QUARTZ_TABLES = List.of(
            "QRTZ_JOB_DETAILS", "QRTZ_TRIGGERS", "QRTZ_SIMPLE_TRIGGERS", "QRTZ_CRON_TRIGGERS",
            "QRTZ_SIMPROP_TRIGGERS", "QRTZ_BLOB_TRIGGERS", "QRTZ_CALENDARS", "QRTZ_PAUSED_TRIGGER_GRPS",
            "QRTZ_FIRED_TRIGGERS", "QRTZ_SCHEDULER_STATE", "QRTZ_LOCKS");

    @Test
    void applicationYml_usesJdbcJobStoreAndNeverInitializesSchema() throws Exception {
        PropertySource<?> yml = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml")).get(0);

        assertThat(String.valueOf(yml.getProperty("spring.quartz.job-store-type"))).isEqualTo("jdbc");
        assertThat(String.valueOf(yml.getProperty("spring.quartz.jdbc.initialize-schema"))).isEqualTo("never");
        assertThat(String.valueOf(yml.getProperty("spring.quartz.overwrite-existing-jobs"))).isEqualTo("true");
        assertThat(String.valueOf(yml.getProperty("spring.quartz.properties.org.quartz.jobStore.driverDelegateClass")))
                .isEqualTo("org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
    }

    @Test
    void liquibaseChangeset_createsAllQuartzTables() throws Exception {
        String sql = read("db/changelog/changes/002-quartz-tables.sql");

        for (String table : QUARTZ_TABLES) {
            assertThat(sql).contains("CREATE TABLE " + table + " (");
        }
    }

    @Test
    void masterChangelog_includesQuartzChangeset() throws Exception {
        String master = read("db/changelog/db.changelog-master.yaml");

        assertThat(master).contains("002-quartz-tables.yaml");
        assertThat(read("db/changelog/changes/002-quartz-tables.yaml")).contains("002-quartz-tables.sql");
    }

    private String read(String path) throws Exception {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
