package kr.or.thejejachurch.api.media.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(name = "media_video")
class MediaVideo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, length = 20)
    var provider: String = "YOUTUBE",
    @Column(name = "provider_video_id", nullable = false, unique = true, length = 64)
    var providerVideoId: String,
    @Column(nullable = false, length = 255)
    var title: String,
    @Column(columnDefinition = "text")
    var description: String? = null,
    @Column(name = "published_at", nullable = false)
    var publishedAt: OffsetDateTime,
    @Column(name = "channel_id", length = 64)
    var channelId: String? = null,
    @Column(name = "channel_title", length = 255)
    var channelTitle: String? = null,
    @Column(name = "thumbnail_url", columnDefinition = "text")
    var thumbnailUrl: String? = null,
    @Column(name = "duration_seconds")
    var durationSeconds: Int? = null,
    @Column(name = "privacy_status", length = 30)
    var privacyStatus: String? = null,
    @Column(nullable = false)
    var embeddable: Boolean = true,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    var rawPayload: String? = null,
    @Column(name = "last_synced_at")
    var lastSyncedAt: OffsetDateTime? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
