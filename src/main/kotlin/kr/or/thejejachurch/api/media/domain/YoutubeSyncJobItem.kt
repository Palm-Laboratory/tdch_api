package kr.or.thejejachurch.api.media.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "youtube_sync_job_item")
class YoutubeSyncJobItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "job_id", nullable = false)
    var jobId: Long,
    @Column(name = "content_menu_id")
    var contentMenuId: Long? = null,
    @Column(name = "youtube_playlist_id")
    var youtubePlaylistId: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: YoutubeSyncJobItemStatus,
    @Column(name = "processed_items", nullable = false)
    var processedItems: Int = 0,
    @Column(name = "inserted_videos", nullable = false)
    var insertedVideos: Int = 0,
    @Column(name = "updated_videos", nullable = false)
    var updatedVideos: Int = 0,
    @Column(name = "deactivated_playlist_videos", nullable = false)
    var deactivatedPlaylistVideos: Int = 0,
    @Column(name = "error_message", columnDefinition = "text")
    var errorMessage: String? = null,
    @Column(name = "started_at", nullable = false)
    var startedAt: OffsetDateTime,
    @Column(name = "finished_at")
    var finishedAt: OffsetDateTime? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
) {
    fun markSucceeded(
        finishedAt: OffsetDateTime,
        processedItems: Int,
        insertedVideos: Int,
        updatedVideos: Int,
        deactivatedPlaylistVideos: Int,
    ) {
        this.finishedAt = finishedAt
        this.processedItems = processedItems
        this.insertedVideos = insertedVideos
        this.updatedVideos = updatedVideos
        this.deactivatedPlaylistVideos = deactivatedPlaylistVideos
        this.errorMessage = null
        this.status = YoutubeSyncJobItemStatus.SUCCEEDED
    }

    fun markFailed(
        finishedAt: OffsetDateTime,
        errorMessage: String,
    ) {
        this.finishedAt = finishedAt
        this.errorMessage = errorMessage
        this.status = YoutubeSyncJobItemStatus.FAILED
    }

    companion object {
        fun start(
            jobId: Long,
            contentMenuId: Long?,
            youtubePlaylistId: Long?,
            startedAt: OffsetDateTime,
        ): YoutubeSyncJobItem = YoutubeSyncJobItem(
            jobId = jobId,
            contentMenuId = contentMenuId,
            youtubePlaylistId = youtubePlaylistId,
            status = YoutubeSyncJobItemStatus.SUCCEEDED,
            startedAt = startedAt,
        )
    }
}
