package kr.or.thejejachurch.api.menu.infrastructure.persistence

import kr.or.thejejachurch.api.menu.domain.MenuRevision
import org.springframework.data.jpa.repository.JpaRepository

interface MenuRevisionRepository : JpaRepository<MenuRevision, Long>
