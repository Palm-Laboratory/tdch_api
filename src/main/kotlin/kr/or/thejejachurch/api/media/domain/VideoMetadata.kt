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
@Table(name = "video_metadata")
class VideoMetadata(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "youtube_video_id", nullable = false, unique = true)
    var youtubeVideoId: Long,
    @Column(length = 120)
    var preacher: String? = null,
    @Column(length = 255)
    var scripture: String? = null,
    @Column(name = "service_type", length = 100)
    var serviceType: String? = null,
    @Column(columnDefinition = "text")
    var summary: String? = null,
    @Column(nullable = false)
    var visible: Boolean = true,
    @Column(nullable = false)
    var featured: Boolean = false,
    @Column(name = "pinned_rank")
    var pinnedRank: Int? = null,
    @Column(name = "manual_title", length = 255)
    var manualTitle: String? = null,
    @Column(name = "manual_thumbnail_url", columnDefinition = "text")
    var manualThumbnailUrl: String? = null,
    @Column(name = "manual_published_date")
    var manualPublishedDate: LocalDate? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "manual_kind", length = 20)
    var manualKind: ContentKind? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
