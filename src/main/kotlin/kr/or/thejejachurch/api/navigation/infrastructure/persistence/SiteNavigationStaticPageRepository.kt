package kr.or.thejejachurch.api.navigation.infrastructure.persistence

import kr.or.thejejachurch.api.navigation.domain.SiteNavigationStaticPage
import org.springframework.data.jpa.repository.JpaRepository

interface SiteNavigationStaticPageRepository : JpaRepository<SiteNavigationStaticPage, Long>
