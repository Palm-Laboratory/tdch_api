package kr.or.thejejachurch.api.navigation.domain

import kr.or.thejejachurch.api.media.domain.ContentKind
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "site_navigation_video_page")
class SiteNavigationVideoPage(
    @Id
    @Column(name = "site_navigation_id")
    val siteNavigationId: Long,
    @Column(name = "video_root_key", nullable = false, length = 64)
    val videoRootKey: String,
    @Column(name = "landing_mode", length = 20)
    val landingMode: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "content_kind_filter", length = 20)
    val contentKindFilter: ContentKind? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
