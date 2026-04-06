package kr.or.thejejachurch.api.navigation.infrastructure.persistence

import kr.or.thejejachurch.api.navigation.domain.SiteNavigationSet
import org.springframework.data.jpa.repository.JpaRepository

interface SiteNavigationSetRepository : JpaRepository<SiteNavigationSet, Long> {
    fun findBySetKeyAndActiveTrue(setKey: String): SiteNavigationSet?
}
