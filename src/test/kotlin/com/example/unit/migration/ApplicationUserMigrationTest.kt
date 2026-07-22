package com.example.unit.migration

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationUserMigrationTest {
    @Test
    fun `V8 locks membership writes before duplicate and exact named-index inspection`() {
        val sql = migrationSQL()
        val executable = normalizedExecutableSQL(sql)
        val lockPosition = executable.indexOf("LOCK TABLES APPLICATION_USER WRITE;")
        val duplicateInspection = executable.indexOf("GROUP BY APPLICATION_ID, USER_ID HAVING COUNT(*) > 1")
        val indexInspection = executable.indexOf("FROM INFORMATION_SCHEMA.STATISTICS")

        assertTrue(sql.contains("LOCK TABLES privilege"), "required migration privilege must be documented")
        assertTrue(lockPosition >= 0, "migration must acquire an application_user WRITE lock")
        assertTrue(
            duplicateInspection > lockPosition,
            "duplicate inspection must run after the WRITE lock on the same Flyway connection",
        )
        assertTrue(
            indexInspection > lockPosition,
            "named-index inspection must run after the WRITE lock",
        )

        listOf(
            "TABLE_SCHEMA = DATABASE()",
            "TABLE_NAME = 'APPLICATION_USER'",
            "INDEX_NAME = 'UQ_APPLICATION_USER_APPLICATION_USER'",
            "NON_UNIQUE = 0",
            "SEQ_IN_INDEX = 1 AND COLUMN_NAME = 'APPLICATION_ID'",
            "SEQ_IN_INDEX = 2 AND COLUMN_NAME = 'USER_ID'",
            "SUB_PART IS NULL",
            "INDEX_TYPE = 'BTREE'",
            "COLLATION = 'A'",
            "IS_VISIBLE = 'YES'",
            "@APPLICATION_USER_INDEX_COLUMN_COUNT = 2",
            "@APPLICATION_USER_EXACT_COLUMN_COUNT = 2",
            "THEN 'CORRECT'",
            "THEN 'MISSING'",
            "ELSE 'WRONG'",
        ).forEach { required ->
            assertTrue(executable.contains(required), "exact named-index state check is missing: $required")
        }
    }

    @Test
    fun `V8 conditionally creates the unique index or noops while the table is locked`() {
        val executable = normalizedExecutableSQL(migrationSQL())

        listOf(
            "@APPLICATION_USER_DUPLICATE_COUNT = 0 AND @APPLICATION_USER_INDEX_STATE = 'MISSING'",
            "CREATE UNIQUE INDEX UQ_APPLICATION_USER_APPLICATION_USER ON APPLICATION_USER " +
                "(APPLICATION_ID, USER_ID) ALGORITHM=INPLACE LOCK=EXCLUSIVE",
            "LOCK=EXCLUSIVE', 'SELECT 1' )",
            "PREPARE APPLICATION_USER_INDEX_STATEMENT FROM @APPLICATION_USER_INDEX_SQL",
            "EXECUTE APPLICATION_USER_INDEX_STATEMENT",
            "DEALLOCATE PREPARE APPLICATION_USER_INDEX_STATEMENT",
        ).forEach { required ->
            assertTrue(executable.contains(required), "conditional idempotent index DDL is missing: $required")
        }

        assertTrue(
            executable.indexOf("EXECUTE APPLICATION_USER_INDEX_STATEMENT") < executable.indexOf("UNLOCK TABLES;"),
            "conditional DDL/no-op must execute before releasing the WRITE lock",
        )
    }

    @Test
    fun `V8 unlocks before duplicate or malformed-index failures use a generic sentinel`() {
        val executable = normalizedExecutableSQL(migrationSQL())
        val unlockPosition = executable.indexOf("UNLOCK TABLES;")
        val guardPosition = executable.indexOf("CREATE TEMPORARY TABLE TMP_V8_FAILURE_GUARD")
        val failureInsert = executable.indexOf("INSERT INTO TMP_V8_FAILURE_GUARD (FAILURE_SENTINEL)")
        val failureSection = executable.substring(failureInsert)

        assertTrue(
            executable.contains(
                "@APPLICATION_USER_DUPLICATE_COUNT > 0 OR @APPLICATION_USER_INDEX_STATE = 'WRONG'",
            ),
            "duplicates and malformed named indexes must both select the failure path",
        )
        assertTrue(
            unlockPosition >= 0 && guardPosition > unlockPosition && failureInsert > guardPosition,
            "the intentional failure guard must be created and used only after UNLOCK TABLES",
        )
        assertTrue(
            executable.contains("CONSTRAINT UQ_V8_FAILURE_GUARD UNIQUE (FAILURE_SENTINEL)"),
            "failure must use a generic named sentinel constraint",
        )
        assertTrue(
            executable.contains("SELECT 1 AS FAILURE_SENTINEL UNION ALL SELECT 1"),
            "failure must collide generic constants without selecting raw membership identifiers",
        )
        assertTrue(
            executable.contains("WHERE @APPLICATION_USER_MIGRATION_SHOULD_FAIL = 1"),
            "guard collision must occur only for duplicate or malformed-index states",
        )
        assertFalse(failureSection.contains("APPLICATION_ID"), "failure guard must not select raw application IDs")
        assertFalse(failureSection.contains("USER_ID"), "failure guard must not select raw user IDs")
    }

    @Test
    fun `V8 never deletes or rewrites duplicate membership data`() {
        val executable = normalizedExecutableSQL(migrationSQL())

        listOf("DELETE ", "UPDATE APPLICATION_USER", "INSERT IGNORE", "ON DUPLICATE KEY").forEach { forbidden ->
            assertFalse(executable.contains(forbidden), "migration must not auto-resolve duplicates: $forbidden")
        }
    }

    private fun migrationSQL(): String {
        val migration = Paths.get("src/main/resources/db/migration/V8__unique_application_user.sql")
        assertTrue(Files.isRegularFile(migration), "V8 membership migration is missing")
        return Files.readString(migration)
    }

    private fun normalizedExecutableSQL(sql: String): String =
        sql.lineSequence()
            .filterNot { it.trimStart().startsWith("--") }
            .joinToString("\n")
            .replace(Regex("\\s+"), " ")
            .uppercase()
}
