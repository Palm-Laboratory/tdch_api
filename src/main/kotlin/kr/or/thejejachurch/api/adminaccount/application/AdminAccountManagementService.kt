package kr.or.thejejachurch.api.adminaccount.application

import kr.or.thejejachurch.api.adminaccount.domain.AdminAccount
import kr.or.thejejachurch.api.adminaccount.domain.AdminAccountRole
import kr.or.thejejachurch.api.adminaccount.infrastructure.persistence.AdminAccountRepository
import kr.or.thejejachurch.api.common.error.ForbiddenException
import kr.or.thejejachurch.api.common.error.NotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AdminAccountManagementService(
    private val adminAccountRepository: AdminAccountRepository,
    private val adminAccountBootstrapService: AdminAccountBootstrapService,
    private val passwordEncoder: PasswordEncoder,
) {
    fun getAccount(actorId: Long, accountId: Long): AdminAccountSummary {
        adminAccountBootstrapService.ensureBootstrapSuperAccount()
        requireSuperAdmin(actorId)

        val account = adminAccountRepository.findByIdOrNull(accountId)
            ?: throw NotFoundException("관리자 계정을 찾을 수 없습니다. id=$accountId")

        return account.toSummary()
    }

    fun getAccounts(actorId: Long): List<AdminAccountSummary> {
        adminAccountBootstrapService.ensureBootstrapSuperAccount()
        requireSuperAdmin(actorId)

        return adminAccountRepository.findAll()
            .sortedWith(
                compareByDescending<AdminAccount> { it.role == AdminAccountRole.SUPER_ADMIN }
                    .thenBy { it.username }
            )
            .map { it.toSummary() }
    }

    fun createAdminAccount(actorId: Long, command: CreateAdminAccountCommand): AdminAccountSummary {
        adminAccountBootstrapService.ensureBootstrapSuperAccount()
        requireSuperAdmin(actorId)

        val username = AdminAccountBootstrapService.normalizeUsername(command.username)
        val displayName = command.displayName.trim()
        val password = command.password.trim()

        require(username.isNotBlank()) { "관리자 아이디를 입력해 주세요." }
        require(displayName.isNotBlank()) { "관리자 이름을 입력해 주세요." }
        require(password.length >= 8) { "비밀번호는 8자 이상이어야 합니다." }

        if (adminAccountRepository.findByUsername(username) != null) {
            throw IllegalArgumentException("이미 사용 중인 관리자 아이디입니다.")
        }

        val account = adminAccountRepository.save(
            AdminAccount(
                username = username,
                displayName = displayName,
                passwordHash = passwordEncoder.encode(password),
                role = AdminAccountRole.ADMIN,
            )
        )

        return account.toSummary()
    }

    private fun requireSuperAdmin(actorId: Long) {
        val actor = adminAccountRepository.findByIdOrNull(actorId)
            ?: throw NotFoundException("관리자 계정을 찾을 수 없습니다. id=$actorId")

        if (actor.role != AdminAccountRole.SUPER_ADMIN || !actor.active) {
            throw ForbiddenException("슈퍼 관리자만 관리자 계정을 관리할 수 있습니다.")
        }
    }

    private fun AdminAccount.toSummary(): AdminAccountSummary =
        AdminAccountSummary(
            id = id ?: throw IllegalStateException("관리자 계정 ID가 없습니다."),
            username = username,
            displayName = displayName,
            role = role,
            active = active,
            lastLoginAt = lastLoginAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
