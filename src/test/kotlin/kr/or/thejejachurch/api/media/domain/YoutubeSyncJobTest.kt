package kr.or.thejejachurch.api.media.domain

import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeSyncJobItemRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeSyncJobRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class YoutubeSyncJobTest {

    @Test
    fun `job can be completed with partial failure summary`() {
        val startedAt = OffsetDateTime.parse("2026-04-14T06:00:00Z")
        val finishedAt = startedAt.plusMinutes(3)

        val job = YoutubeSyncJob.startScheduled(startedAt)

        job.finish(
            finishedAt = finishedAt,
            totalPlaylists = 3,
            succeededPlaylists = 2,
            failedPlaylists = 1,
            errorSummary = "1 playlist failed",
        )

        assertThat(job.triggerType).isEqualTo(YoutubeSyncTriggerType.SCHEDULED)
        assertThat(job.status).isEqualTo(YoutubeSyncJobStatus.PARTIAL_FAILED)
        assertThat(job.startedAt).isEqualTo(startedAt)
        assertThat(job.finishedAt).isEqualTo(finishedAt)
        assertThat(job.totalPlaylists).isEqualTo(3)
        assertThat(job.succeededPlaylists).isEqualTo(2)
        assertThat(job.failedPlaylists).isEqualTo(1)
        assertThat(job.errorSummary).isEqualTo("1 playlist failed")
    }

    @Test
    fun `job item can record succeeded and failed sync outcomes`() {
        val startedAt = OffsetDateTime.parse("2026-04-14T06:01:00Z")
        val finishedAt = startedAt.plusSeconds(40)

        val succeeded = YoutubeSyncJobItem.start(
            jobId = 11L,
            contentMenuId = 21L,
            youtubePlaylistId = 31L,
            startedAt = startedAt,
        )
        succeeded.markSucceeded(
            finishedAt = finishedAt,
            processedItems = 12,
            insertedVideos = 3,
            updatedVideos = 9,
            deactivatedPlaylistVideos = 1,
        )

        val failed = YoutubeSyncJobItem.start(
            jobId = 11L,
            contentMenuId = 22L,
            youtubePlaylistId = 32L,
            startedAt = startedAt,
        )
        failed.markFailed(
            finishedAt = finishedAt,
            errorMessage = "quota exceeded",
        )

        assertThat(succeeded.status).isEqualTo(YoutubeSyncJobItemStatus.SUCCEEDED)
        assertThat(succeeded.processedItems).isEqualTo(12)
        assertThat(succeeded.insertedVideos).isEqualTo(3)
        assertThat(succeeded.updatedVideos).isEqualTo(9)
        assertThat(succeeded.deactivatedPlaylistVideos).isEqualTo(1)
        assertThat(succeeded.errorMessage).isNull()

        assertThat(failed.status).isEqualTo(YoutubeSyncJobItemStatus.FAILED)
        assertThat(failed.errorMessage).isEqualTo("quota exceeded")
        assertThat(failed.finishedAt).isEqualTo(finishedAt)
    }

    @Test
    fun `sync repositories expose recent history queries`() {
        val jobRepositoryClass = YoutubeSyncJobRepository::class.java
        val itemRepositoryClass = YoutubeSyncJobItemRepository::class.java

        assertThat(jobRepositoryClass.methods.map { it.name })
            .contains("findTop20ByOrderByStartedAtDesc")
        assertThat(itemRepositoryClass.methods.map { it.name })
            .contains("findAllByJobIdOrderByIdAsc")
    }
}
