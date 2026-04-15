package kr.or.thejejachurch.api.media.infrastructure.persistence

import kr.or.thejejachurch.api.media.domain.ContentMenuStatus
import kr.or.thejejachurch.api.media.domain.ContentMenu
import org.springframework.data.jpa.repository.JpaRepository

interface ContentMenuRepository : JpaRepository<ContentMenu, Long> {
    fun findAllByActiveTrueOrderByIdAsc(): List<ContentMenu>
    fun findAllByActiveTrueAndNavigationVisibleTrueAndStatusOrderBySortOrderAscIdAsc(status: ContentMenuStatus): List<ContentMenu>
    fun findAllByVideoRootKeyAndActiveTrueAndNavigationVisibleTrueAndStatusOrderBySortOrderAscIdAsc(
        videoRootKey: String,
        status: ContentMenuStatus,
    ): List<ContentMenu>
    fun findBySiteKey(siteKey: String): ContentMenu?
    fun findBySlug(slug: String): ContentMenu?
}
