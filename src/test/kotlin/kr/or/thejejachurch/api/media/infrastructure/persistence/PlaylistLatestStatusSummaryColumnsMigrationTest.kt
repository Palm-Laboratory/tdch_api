package kr.or.thejejachurch.api.media.infrastructure.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlaylistLatestStatusSummaryColumnsMigrationTest {

    @Test
    fun `youtube playlist latest status summary migration adds discovery and sync summary columns`() {
        val resource = javaClass.getResource("/db/migration/V14__extend_youtube_playlist_latest_status_summary.sql")

        assertThat(resource).isNotNull()

        val sql = resource!!.readText()

        assertThat(sql).contains("alter table youtube_playlist")
        assertThat(sql).contains("add column last_discovered_at timestamptz")
        assertThat(sql).contains("add column last_sync_failed_at timestamptz")
        assertThat(sql).contains("add column last_sync_error_message text")
        assertThat(sql).contains("add column last_sync_succeeded_at timestamptz")
        assertThat(sql).contains("add column discovery_source varchar(20)")
        assertThat(sql).contains("create index idx_youtube_playlist_sync_enabled")
        assertThat(sql).contains("create index idx_youtube_playlist_content_menu_id")
    }
}
