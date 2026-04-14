package kr.or.thejejachurch.api.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class FlywayYoutubeSyncJobMigrationTest {

    @Test
    fun `V6 migration creates youtube sync job tables`() {
        val migrationPath = Path.of("src", "main", "resources", "db", "migration", "V6__create_youtube_sync_job.sql")

        assertThat(Files.exists(migrationPath))
            .describedAs("Expected a new Flyway migration at %s", migrationPath)
            .isTrue()

        val sql = Files.readString(migrationPath).lowercase()

        assertThat(sql).contains("create table youtube_sync_job")
        assertThat(sql).contains("create table youtube_sync_job_item")
    }
}
