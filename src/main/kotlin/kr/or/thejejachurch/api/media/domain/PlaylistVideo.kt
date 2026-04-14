package kr.or.thejejachurch.api.media.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "playlist_video")
class PlaylistVideo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "youtube_playlist_id", nullable = false)
    var youtubePlaylistId: Long,
    @Column(name = "youtube_video_id", nullable = false)
    var youtubeVideoId: Long,
    @Column(nullable = false)
    var position: Int,
    @Column(name = "added_to_playlist_at")
    var addedToPlaylistAt: OffsetDateTime? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
