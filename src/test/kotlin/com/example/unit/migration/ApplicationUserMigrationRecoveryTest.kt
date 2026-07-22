package com.example.unit.migration

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertTrue

class ApplicationUserMigrationRecoveryTest {
    @Test
    fun `V8 comments require approved duplicate cleanup and Flyway repair before rerun`() {
        val comments = Files.readString(
            Paths.get("src/main/resources/db/migration/V8__unique_application_user.sql"),
        ).lineSequence()
            .filter { it.trimStart().startsWith("--") }
            .joinToString("\n")
            .lowercase()

        listOf("승인", "중복", "flyway repair", "schema history", "정확", "멱등 재실행").forEach { required ->
            assertTrue(comments.contains(required), "V8 recovery comment is missing: $required")
        }
    }

    @Test
    fun `operations runbook fixes failed-history and index-without-history recovery order`() {
        val runbookPath = Paths.get("docs/operations/application-user-unique-index.md")
        assertTrue(Files.isRegularFile(runbookPath), "application_user V8 operations runbook is missing")
        val runbook = Files.readString(runbookPath)
        val normalized = runbook.replace(Regex("\\s+"), " ").lowercase()

        listOf(
            "lock tables privilege",
            "create temporary tables",
            "select application_id, user_id, count(*)",
            "information_schema.statistics",
            "uq_application_user_application_user",
            "flyway repair",
            "승인",
            "자동 삭제",
        ).forEach { required ->
            assertTrue(normalized.contains(required), "V8 operations runbook is missing: $required")
        }

        val duplicateRecovery = normalized.substringAfter("중복으로 v8 실패")
        assertInOrder(
            duplicateRecovery,
            "승인",
            "중복 정리",
            "flyway repair",
            "멱등 재실행",
        )

        val historyRecovery = normalized.substringAfter("인덱스는 존재하지만 schema history 누락")
        assertInOrder(
            historyRecovery,
            "information_schema.statistics",
            "정확",
            "flyway repair",
            "멱등 재실행",
        )
    }

    private fun assertInOrder(text: String, vararg fragments: String) {
        var previous = -1
        fragments.forEach { fragment ->
            val position = text.indexOf(fragment, startIndex = previous + 1)
            assertTrue(position > previous, "recovery step is missing or out of order: $fragment")
            previous = position
        }
    }
}
