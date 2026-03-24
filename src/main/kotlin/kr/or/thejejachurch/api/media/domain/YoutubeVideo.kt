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
@Table(name = "youtube_video")
class YoutubeVideo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "youtube_video_id", nullable = false, unique = true, length = 32)
    var youtubeVideoId: String,
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
    @Column(name = "privacy_status", length = 20)
    var privacyStatus: String? = null,
    @Column(name = "upload_status", length = 20)
    var uploadStatus: String? = null,
    @Column(nullable = false)
    var embeddable: Boolean = true,
    @Column(name = "made_for_kids")
    var madeForKids: Boolean? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "detected_kind", nullable = false, length = 20)
    var detectedKind: ContentKind,
    @Column(name = "youtube_watch_url", nullable = false, columnDefinition = "text")
    var youtubeWatchUrl: String,
    @Column(name = "youtube_embed_url", nullable = false, columnDefinition = "text")
    var youtubeEmbedUrl: String,
    @Column(name = "raw_payload", columnDefinition = "text")
    var rawPayload: String? = null,
    @Column(name = "last_synced_at", nullable = false)
    var lastSyncedAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
