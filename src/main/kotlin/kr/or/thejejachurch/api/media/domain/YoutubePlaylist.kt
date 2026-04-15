package kr.or.thejejachurch.api.media.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "youtube_playlist")
class YoutubePlaylist(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "content_menu_id", nullable = false)
    var contentMenuId: Long,
    @Column(name = "youtube_playlist_id", nullable = false, unique = true, length = 64)
    var youtubePlaylistId: String,
    @Column(nullable = false, length = 255)
    var title: String,
    @Column(columnDefinition = "text")
    var description: String? = null,
    @Column(name = "channel_id", length = 64)
    var channelId: String? = null,
    @Column(name = "channel_title", length = 255)
    var channelTitle: String? = null,
    @Column(name = "thumbnail_url", columnDefinition = "text")
    var thumbnailUrl: String? = null,
    @Column(name = "item_count")
    var itemCount: Int? = null,
    @Column(name = "sync_enabled", nullable = false)
    var syncEnabled: Boolean = true,
    @Column(name = "last_synced_at")
    var lastSyncedAt: OffsetDateTime? = null,
    @Column(name = "last_discovered_at")
    var lastDiscoveredAt: OffsetDateTime? = null,
    @Column(name = "last_sync_succeeded_at")
    var lastSyncSucceededAt: OffsetDateTime? = null,
    @Column(name = "last_sync_failed_at")
    var lastSyncFailedAt: OffsetDateTime? = null,
    @Column(name = "last_sync_error_message", columnDefinition = "text")
    var lastSyncErrorMessage: String? = null,
    @Column(name = "discovery_source", length = 20)
    var discoverySource: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
