package kr.or.thejejachurch.api.navigation.domain

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
@Table(name = "site_navigation_item")
class SiteNavigationItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "navigation_set_id", nullable = false)
    val navigationSetId: Long,
    @Column(name = "parent_id")
    val parentId: Long? = null,
    @Column(name = "menu_key", nullable = false, length = 64)
    val menuKey: String,
    @Column(nullable = false, length = 100)
    val label: String,
    @Column(nullable = false, length = 255)
    val href: String,
    @Column(name = "match_path", length = 255)
    val matchPath: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 20)
    val linkType: NavigationLinkType = NavigationLinkType.INTERNAL,
    @Column(nullable = false)
    val visible: Boolean = true,
    @Column(name = "header_visible", nullable = false)
    val headerVisible: Boolean = true,
    @Column(name = "mobile_visible", nullable = false)
    val mobileVisible: Boolean = true,
    @Column(name = "lnb_visible", nullable = false)
    val lnbVisible: Boolean = true,
    @Column(name = "breadcrumb_visible", nullable = false)
    val breadcrumbVisible: Boolean = true,
    @Column(name = "default_landing", nullable = false)
    val defaultLanding: Boolean = false,
    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,
    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
