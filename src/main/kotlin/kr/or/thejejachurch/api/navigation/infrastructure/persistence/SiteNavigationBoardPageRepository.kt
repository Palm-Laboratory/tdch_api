package kr.or.thejejachurch.api.navigation.infrastructure.persistence

import kr.or.thejejachurch.api.navigation.domain.SiteNavigationBoardPage
import org.springframework.data.jpa.repository.JpaRepository

interface SiteNavigationBoardPageRepository : JpaRepository<SiteNavigationBoardPage, Long>
