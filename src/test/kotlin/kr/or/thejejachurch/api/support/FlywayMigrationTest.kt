package kr.or.thejejachurch.api.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlywayMigrationTest {

    @Test
    fun `V17 migration creates youtube sync job tables`() {
        val resource = javaClass.classLoader.getResourceAsStream("db/migration/V17__create_youtube_sync_job_tables.sql")

        assertThat(resource).describedAs("V17 migration should exist on the test classpath").isNotNull

        val sql = resource!!.bufferedReader().use { it.readText() }

        assertThat(sql).contains("create table youtube_sync_job")
        assertThat(sql).contains("create table youtube_sync_job_item")
    }
}
