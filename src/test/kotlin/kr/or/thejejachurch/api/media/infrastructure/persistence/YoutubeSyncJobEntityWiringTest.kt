package kr.or.thejejachurch.api.media.infrastructure.persistence

import kr.or.thejejachurch.api.media.domain.YoutubeSyncJob
import kr.or.thejejachurch.api.media.domain.YoutubeSyncJobItem
import kr.or.thejejachurch.api.media.domain.YoutubeSyncJobItemStatus
import kr.or.thejejachurch.api.media.domain.YoutubeSyncJobStatus
import kr.or.thejejachurch.api.media.domain.YoutubeSyncTriggerType
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class YoutubeSyncJobEntityWiringTest {

    @Test
    fun `sync job entities and repositories should exist for sync history`() {
        val startedAt = OffsetDateTime.parse("2026-04-14T00:00:00Z")

        YoutubeSyncJob(
            id = null,
            startedAt = startedAt,
            triggerType = YoutubeSyncTriggerType.MANUAL,
            status = YoutubeSyncJobStatus.RUNNING,
        )

        YoutubeSyncJobItem(
            id = null,
            jobId = 1L,
            contentMenuId = 1L,
            youtubePlaylistId = 10L,
            startedAt = startedAt,
            status = YoutubeSyncJobItemStatus.SUCCEEDED,
        )

        YoutubeSyncJobRepository::class
        YoutubeSyncJobItemRepository::class
    }
}
