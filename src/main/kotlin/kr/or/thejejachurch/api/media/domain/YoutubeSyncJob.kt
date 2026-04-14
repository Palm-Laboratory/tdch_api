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
@Table(name = "youtube_sync_job")
class YoutubeSyncJob(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    var triggerType: YoutubeSyncTriggerType,
    @Column(name = "started_at", nullable = false)
    var startedAt: OffsetDateTime,
    @Column(name = "finished_at")
    var finishedAt: OffsetDateTime? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: YoutubeSyncJobStatus = YoutubeSyncJobStatus.RUNNING,
    @Column(name = "total_playlists", nullable = false)
    var totalPlaylists: Int = 0,
    @Column(name = "succeeded_playlists", nullable = false)
    var succeededPlaylists: Int = 0,
    @Column(name = "failed_playlists", nullable = false)
    var failedPlaylists: Int = 0,
    @Column(name = "error_summary", columnDefinition = "text")
    var errorSummary: String? = null,
    @Column(name = "created_by")
    var createdBy: Long? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
) {
    fun finish(
        finishedAt: OffsetDateTime,
        totalPlaylists: Int,
        succeededPlaylists: Int,
        failedPlaylists: Int,
        errorSummary: String?,
    ) {
        this.finishedAt = finishedAt
        this.totalPlaylists = totalPlaylists
        this.succeededPlaylists = succeededPlaylists
        this.failedPlaylists = failedPlaylists
        this.errorSummary = errorSummary
        this.status = when {
            failedPlaylists == 0 && succeededPlaylists > 0 -> YoutubeSyncJobStatus.SUCCEEDED
            failedPlaylists > 0 && succeededPlaylists > 0 -> YoutubeSyncJobStatus.PARTIAL_FAILED
            failedPlaylists > 0 -> YoutubeSyncJobStatus.FAILED
            else -> YoutubeSyncJobStatus.SUCCEEDED
        }
    }

    companion object {
        fun startScheduled(startedAt: OffsetDateTime): YoutubeSyncJob = YoutubeSyncJob(
            triggerType = YoutubeSyncTriggerType.SCHEDULED,
            startedAt = startedAt,
        )

        fun startManual(
            startedAt: OffsetDateTime,
            createdBy: Long? = null,
        ): YoutubeSyncJob = YoutubeSyncJob(
            triggerType = YoutubeSyncTriggerType.MANUAL,
            startedAt = startedAt,
            createdBy = createdBy,
        )
    }
}
