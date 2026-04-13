package kr.or.thejejachurch.api.media.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(name = "media_collection_video")
class MediaCollectionVideo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "media_collection_id", nullable = false)
    var mediaCollectionId: Long,
    @Column(name = "media_video_id", nullable = false)
    var mediaVideoId: Long,
    @Column(name = "youtube_playlist_connection_id")
    var youtubePlaylistConnectionId: Long? = null,
    @Column(name = "source_position")
    var sourcePosition: Int? = null,
    @Column(name = "added_to_playlist_at")
    var addedToPlaylistAt: OffsetDateTime? = null,
    @Column(name = "sync_active", nullable = false)
    var syncActive: Boolean = true,
    @Column(nullable = false)
    var visible: Boolean = true,
    @Column(nullable = false)
    var featured: Boolean = false,
    @Column(name = "pinned_rank")
    var pinnedRank: Int? = null,
    @Column(name = "display_title", length = 255)
    var displayTitle: String? = null,
    @Column(name = "display_thumbnail_url", columnDefinition = "text")
    var displayThumbnailUrl: String? = null,
    @Column(name = "display_published_date")
    var displayPublishedDate: LocalDate? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "display_kind", length = 20)
    var displayKind: ContentKind? = null,
    @Column(name = "sort_order")
    var sortOrder: Int? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
