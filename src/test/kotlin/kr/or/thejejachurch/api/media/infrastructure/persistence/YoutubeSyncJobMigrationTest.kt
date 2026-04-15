package kr.or.thejejachurch.api.media.infrastructure.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class YoutubeSyncJobMigrationTest {

    @Test
    fun `sync job migration creates job and job item tables with key constraints`() {
        val resource = javaClass.getResource("/db/migration/V11__create_youtube_sync_job_tables.sql")

        assertThat(resource).isNotNull()

        val sql = resource!!.readText()

        assertThat(sql).contains("create table youtube_sync_job")
        assertThat(sql).contains("check (trigger_type in ('SCHEDULED', 'MANUAL'))")
        assertThat(sql).contains("check (status in ('RUNNING', 'SUCCEEDED', 'PARTIAL_FAILED', 'FAILED'))")
        assertThat(sql).contains("create table youtube_sync_job_item")
        assertThat(sql).contains("references youtube_sync_job(id) on delete cascade")
        assertThat(sql).contains("references content_menu(id)")
        assertThat(sql).contains("references youtube_playlist(id)")
        assertThat(sql).contains("check (status in ('SUCCEEDED', 'FAILED'))")
        assertThat(sql).contains("create index idx_youtube_sync_job_started_at")
        assertThat(sql).contains("create index idx_youtube_sync_job_item_playlist")
    }
}
