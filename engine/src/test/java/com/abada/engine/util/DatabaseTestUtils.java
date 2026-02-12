package com.abada.engine.util;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Utility class for cleaning database tables during tests.
 */
public final class DatabaseTestUtils {

    private DatabaseTestUtils() {
        // Utility class, no instantiation
    }

    /**
     * Deletes all rows from key engine tables to ensure a clean test database state.
     * @param jdbcTemplate the JdbcTemplate to execute SQL
     */
    public static void cleanDatabase(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DELETE FROM TASK_CANDIDATE_USERS");
        jdbcTemplate.execute("DELETE FROM TASK_CANDIDATE_GROUPS");
        jdbcTemplate.execute("DELETE FROM TASKS");
        jdbcTemplate.execute("DELETE FROM PROCESS_INSTANCES");
        jdbcTemplate.execute("DELETE FROM PROCESS_DEFINITIONS");
    }
}
