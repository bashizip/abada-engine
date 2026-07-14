package com.abada.engine.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PostgresSchemaUpgradeTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("abada_schema_upgrades")
            .withUsername("abada")
            .withPassword("abada");

    @ParameterizedTest(name = "upgrades schema v{0} to latest")
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void upgradesEveryPreviouslyPublishedSchemaVersion(int sourceVersion) throws Exception {
        String schema = "upgrade_from_v" + sourceVersion;
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema)
                .createSchemas(true)
                .target(MigrationVersion.fromVersion(Integer.toString(sourceVersion)))
                .load()
                .migrate();

        Flyway latest = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema)
                .load();
        assertThat(latest.migrate().success).isTrue();
        assertThat(latest.validateWithResult().validationSuccessful).isTrue();
        assertThat(latest.info().current().getVersion().getVersion()).isEqualTo("6");

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var columns = connection.getMetaData().getColumns(null, schema, "process_instances",
                     "process_definition_deployment_id")) {
            assertThat(columns.next()).isTrue();
        }
    }
}
