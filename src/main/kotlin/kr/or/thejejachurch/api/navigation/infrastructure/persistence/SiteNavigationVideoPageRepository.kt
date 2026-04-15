package kr.or.thejejachurch.api.navigation.infrastructure.persistence

import kr.or.thejejachurch.api.navigation.domain.SiteNavigationVideoPage
import org.springframework.data.jpa.repository.JpaRepository

interface SiteNavigationVideoPageRepository : JpaRepository<SiteNavigationVideoPage, Long>
