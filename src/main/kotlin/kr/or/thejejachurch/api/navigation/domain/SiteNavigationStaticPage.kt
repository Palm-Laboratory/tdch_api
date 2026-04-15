package kr.or.thejejachurch.api.navigation.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "site_navigation_static_page")
class SiteNavigationStaticPage(
    @Id
    @Column(name = "site_navigation_id")
    val siteNavigationId: Long,
    @Column(name = "page_key", length = 100)
    val pageKey: String? = null,
    @Column(name = "page_path", length = 255)
    val pagePath: String? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
