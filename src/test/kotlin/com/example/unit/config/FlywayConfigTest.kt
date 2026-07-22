package com.example.unit.config

import com.example.auth.common.config.FlywayConfig
import io.mockk.mockk
import io.mockk.verify
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test

class FlywayConfigTest {
    @Test
    fun `startup migration never repairs failed history automatically`() {
        val flyway = mockk<Flyway>(relaxed = true)

        FlywayConfig().cleanMigrateStrategy().migrate(flyway)

        verify(exactly = 1) { flyway.migrate() }
        verify(exactly = 0) { flyway.repair() }
    }
}
