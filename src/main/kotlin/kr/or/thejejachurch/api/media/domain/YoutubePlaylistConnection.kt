package kr.or.thejejachurch.api.media.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "youtube_playlist_connection")
class YoutubePlaylistConnection(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "media_collection_id", nullable = false)
    var mediaCollectionId: Long,
    @Column(name = "youtube_playlist_id", nullable = false, unique = true, length = 64)
    var youtubePlaylistId: String,
    @Column(length = 255)
    var title: String? = null,
    @Column(columnDefinition = "text")
    var description: String? = null,
    @Column(name = "channel_id", length = 64)
    var channelId: String? = null,
    @Column(name = "channel_title", length = 255)
    var channelTitle: String? = null,
    @Column(name = "thumbnail_url", columnDefinition = "text")
    var thumbnailUrl: String? = null,
    @Column(name = "sync_enabled", nullable = false)
    var syncEnabled: Boolean = true,
    @Column(name = "created_via", nullable = false, length = 20)
    var createdVia: String = "MANUAL",
    @Column(name = "last_synced_at")
    var lastSyncedAt: OffsetDateTime? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
