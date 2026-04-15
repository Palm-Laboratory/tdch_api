package kr.or.thejejachurch.api.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class FlywayYoutubeSyncJobMigrationTest {

    @Test
    fun `V12 migration creates youtube sync job tables`() {
        val expectedMigrationPath = Path.of("src", "main", "resources", "db", "migration", "V12__create_youtube_sync_job_tables.sql")

        assertThat(Files.exists(expectedMigrationPath))
            .describedAs("Expected a clean-baseline Flyway migration at %s", expectedMigrationPath)
            .isTrue()

        val sql = Files.readString(expectedMigrationPath).lowercase()

        assertThat(sql).contains("create table youtube_sync_job")
        assertThat(sql).contains("create table youtube_sync_job_item")
        assertThat(sql).contains("trigger_type")
        assertThat(sql).contains("status")
    }
}
