package kr.or.thejejachurch.api.navigation.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "site_navigation_board_page")
class SiteNavigationBoardPage(
    @Id
    @Column(name = "site_navigation_id")
    val siteNavigationId: Long,
    @Column(name = "board_key", nullable = false, length = 100)
    val boardKey: String,
    @Column(name = "list_path", nullable = false, length = 255)
    val listPath: String,
    @Column(name = "category_key", length = 100)
    val categoryKey: String? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
