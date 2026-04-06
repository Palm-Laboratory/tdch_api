package kr.or.thejejachurch.api.navigation.infrastructure.persistence

import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import org.springframework.data.jpa.repository.JpaRepository

interface SiteNavigationItemRepository : JpaRepository<SiteNavigationItem, Long> {
    fun findAllByNavigationSetIdAndVisibleTrueOrderBySortOrderAscIdAsc(navigationSetId: Long): List<SiteNavigationItem>
    fun findAllByNavigationSetIdOrderBySortOrderAscIdAsc(navigationSetId: Long): List<SiteNavigationItem>
    fun findByNavigationSetIdAndId(navigationSetId: Long, id: Long): SiteNavigationItem?
    fun existsByNavigationSetIdAndMenuKey(navigationSetId: Long, menuKey: String): Boolean
    fun existsByNavigationSetIdAndMenuKeyAndIdNot(navigationSetId: Long, menuKey: String, id: Long): Boolean
    fun existsByNavigationSetIdAndParentIdAndDefaultLandingTrue(navigationSetId: Long, parentId: Long): Boolean
    fun existsByNavigationSetIdAndParentIdAndDefaultLandingTrueAndIdNot(
        navigationSetId: Long,
        parentId: Long,
        id: Long,
    ): Boolean
    fun existsByParentId(parentId: Long): Boolean
}
