package kr.or.thejejachurch.api.navigation.infrastructure.persistence

import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import org.springframework.data.jpa.repository.JpaRepository

interface SiteNavigationItemRepository : JpaRepository<SiteNavigationItem, Long> {
    fun findAllByVisibleTrueOrderBySortOrderAscIdAsc(): List<SiteNavigationItem>
}
