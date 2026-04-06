package kr.or.thejejachurch.api.adminaccount.application

import kr.or.thejejachurch.api.adminaccount.domain.AdminAccount
import kr.or.thejejachurch.api.adminaccount.domain.AdminAccountRole
import kr.or.thejejachurch.api.adminaccount.infrastructure.persistence.AdminAccountRepository
import kr.or.thejejachurch.api.common.config.AdminProperties
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AdminAccountBootstrapService(
    private val adminAccountRepository: AdminAccountRepository,
    private val adminProperties: AdminProperties,
    private val passwordEncoder: PasswordEncoder,
) {
    fun ensureBootstrapSuperAccount() {
        if (adminAccountRepository.existsByRole(AdminAccountRole.SUPER_ADMIN)) {
            return
        }

        val username = normalizeUsername(adminProperties.bootstrap.username)
        val password = adminProperties.bootstrap.password.trim()
        val displayName = adminProperties.bootstrap.displayName.trim().ifBlank { "슈퍼 관리자" }

        if (username.isBlank() || password.isBlank()) {
            throw IllegalStateException("ADMIN_BOOTSTRAP_USERNAME and ADMIN_BOOTSTRAP_PASSWORD must be configured.")
        }

        adminAccountRepository.save(
            AdminAccount(
                username = username,
                displayName = displayName,
                passwordHash = passwordEncoder.encode(password),
                role = AdminAccountRole.SUPER_ADMIN,
            )
        )
    }

    companion object {
        fun normalizeUsername(value: String?): String = value?.trim()?.lowercase() ?: ""
    }
}
