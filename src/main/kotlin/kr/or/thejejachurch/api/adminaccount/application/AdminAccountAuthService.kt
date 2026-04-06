package kr.or.thejejachurch.api.adminaccount.application

import kr.or.thejejachurch.api.common.error.UnauthorizedException
import kr.or.thejejachurch.api.adminaccount.infrastructure.persistence.AdminAccountRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AdminAccountAuthService(
    private val adminAccountRepository: AdminAccountRepository,
    private val adminAccountBootstrapService: AdminAccountBootstrapService,
    private val passwordEncoder: PasswordEncoder,
) {
    fun authenticate(username: String, password: String): AuthenticatedAdminAccount {
        adminAccountBootstrapService.ensureBootstrapSuperAccount()

        val normalizedUsername = AdminAccountBootstrapService.normalizeUsername(username)
        if (normalizedUsername.isBlank() || password.isBlank()) {
            throw UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.")
        }

        val account = adminAccountRepository.findByUsername(normalizedUsername)
            ?: throw UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.")

        if (!account.active || !passwordEncoder.matches(password, account.passwordHash)) {
            throw UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.")
        }

        return AuthenticatedAdminAccount(
            id = account.id ?: throw IllegalStateException("관리자 계정 ID가 없습니다."),
            username = account.username,
            displayName = account.displayName,
            role = account.role,
        )
    }
}
