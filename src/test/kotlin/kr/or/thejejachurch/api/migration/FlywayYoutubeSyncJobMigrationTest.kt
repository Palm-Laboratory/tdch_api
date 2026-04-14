package kr.or.thejejachurch.api.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class FlywayYoutubeSyncJobMigrationTest {

    @Test
    fun `V17 migration creates youtube sync job tables`() {
        val migrationPath = Path.of("src", "main", "resources", "db", "migration", "V17__create_youtube_sync_job_tables.sql")

        assertThat(Files.exists(migrationPath))
            .describedAs("Expected a new Flyway migration at %s", migrationPath)
            .isTrue()

        val sql = Files.readString(migrationPath).lowercase()

        assertThat(sql).contains("create table youtube_sync_job")
        assertThat(sql).contains("create table youtube_sync_job_item")
        assertThat(sql).contains("trigger_type")
        assertThat(sql).contains("status")
    }
}
