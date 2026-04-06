package kr.or.thejejachurch.api.adminaccount.infrastructure.persistence

import kr.or.thejejachurch.api.adminaccount.domain.AdminAccount
import kr.or.thejejachurch.api.adminaccount.domain.AdminAccountRole
import org.springframework.data.jpa.repository.JpaRepository

interface AdminAccountRepository : JpaRepository<AdminAccount, Long> {
    fun findByUsername(username: String): AdminAccount?
    fun existsByRole(role: AdminAccountRole): Boolean
}
