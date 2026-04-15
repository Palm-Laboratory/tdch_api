package kr.or.thejejachurch.api.navigation.infrastructure.persistence

import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import org.springframework.data.jpa.repository.JpaRepository

interface SiteNavigationItemRepository : JpaRepository<SiteNavigationItem, Long> {
    fun findAllByVisibleTrueOrderBySortOrderAscIdAsc(): List<SiteNavigationItem>
    fun findAllByOrderBySortOrderAscIdAsc(): List<SiteNavigationItem>
    fun existsByParentIdAndDefaultLandingTrue(parentId: Long): Boolean
    fun existsByParentIdAndDefaultLandingTrueAndIdNot(parentId: Long, id: Long): Boolean
    fun existsByParentId(parentId: Long): Boolean
}
