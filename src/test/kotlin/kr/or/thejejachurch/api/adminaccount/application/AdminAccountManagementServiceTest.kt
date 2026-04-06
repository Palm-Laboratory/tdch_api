package kr.or.thejejachurch.api.adminaccount.application

import kr.or.thejejachurch.api.adminaccount.domain.AdminAccount
import kr.or.thejejachurch.api.adminaccount.domain.AdminAccountRole
import kr.or.thejejachurch.api.adminaccount.infrastructure.persistence.AdminAccountRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class AdminAccountManagementServiceTest {

    private val adminAccountRepository: AdminAccountRepository = mock()
    private val adminAccountBootstrapService: AdminAccountBootstrapService = mock()
    private val passwordEncoder = BCryptPasswordEncoder()
    private val service = AdminAccountManagementService(
        adminAccountRepository = adminAccountRepository,
        adminAccountBootstrapService = adminAccountBootstrapService,
        passwordEncoder = passwordEncoder,
    )

    @Test
    fun `get account returns single account for super admin actor`() {
        whenever(adminAccountRepository.findById(1L)).thenReturn(
            Optional.of(
                AdminAccount(
                    id = 1L,
                    username = "super-admin",
                    displayName = "슈퍼 관리자",
                    passwordHash = "hash",
                    role = AdminAccountRole.SUPER_ADMIN,
                )
            )
        )
        whenever(adminAccountRepository.findById(2L)).thenReturn(
            Optional.of(
                AdminAccount(
                    id = 2L,
                    username = "admin",
                    displayName = "일반 관리자",
                    passwordHash = "hash",
                    role = AdminAccountRole.ADMIN,
                )
            )
        )

        val account = service.getAccount(actorId = 1L, accountId = 2L)

        assertThat(account.id).isEqualTo(2L)
        assertThat(account.username).isEqualTo("admin")
    }

    @Test
    fun `get accounts returns super admin first`() {
        whenever(adminAccountRepository.findById(1L)).thenReturn(
            Optional.of(
                AdminAccount(
                    id = 1L,
                    username = "super-admin",
                    displayName = "슈퍼 관리자",
                    passwordHash = "hash",
                    role = AdminAccountRole.SUPER_ADMIN,
                )
            )
        )
        whenever(adminAccountRepository.findAll()).thenReturn(
            listOf(
                AdminAccount(
                    id = 2L,
                    username = "admin",
                    displayName = "일반 관리자",
                    passwordHash = "hash",
                    role = AdminAccountRole.ADMIN,
                ),
                AdminAccount(
                    id = 1L,
                    username = "super-admin",
                    displayName = "슈퍼 관리자",
                    passwordHash = "hash",
                    role = AdminAccountRole.SUPER_ADMIN,
                ),
            )
        )

        val accounts = service.getAccounts(actorId = 1L)

        assertThat(accounts.map { it.username }).containsExactly("super-admin", "admin")
    }

    @Test
    fun `create admin account stores normalized username and encoded password`() {
        whenever(adminAccountRepository.findById(1L)).thenReturn(
            Optional.of(
                AdminAccount(
                    id = 1L,
                    username = "super-admin",
                    displayName = "슈퍼 관리자",
                    passwordHash = "hash",
                    role = AdminAccountRole.SUPER_ADMIN,
                )
            )
        )
        whenever(adminAccountRepository.findByUsername("new-admin")).thenReturn(null)
        whenever(adminAccountRepository.save(any())).thenAnswer { invocation ->
            val account = invocation.arguments[0] as AdminAccount
            AdminAccount(
                id = 3L,
                username = account.username,
                displayName = account.displayName,
                passwordHash = account.passwordHash,
                role = account.role,
                active = account.active,
                lastLoginAt = account.lastLoginAt,
                createdAt = account.createdAt,
                updatedAt = account.updatedAt,
            )
        }

        val result = service.createAdminAccount(
            actorId = 1L,
            CreateAdminAccountCommand(
                username = " New-Admin ",
                displayName = "새 관리자",
                password = "password-123",
            )
        )

        assertThat(result.username).isEqualTo("new-admin")
        assertThat(result.role).isEqualTo(AdminAccountRole.ADMIN)
        verify(adminAccountRepository).save(any())
    }

    @Test
    fun `create admin account rejects duplicate username`() {
        whenever(adminAccountRepository.findById(1L)).thenReturn(
            Optional.of(
                AdminAccount(
                    id = 1L,
                    username = "super-admin",
                    displayName = "슈퍼 관리자",
                    passwordHash = "hash",
                    role = AdminAccountRole.SUPER_ADMIN,
                )
            )
        )
        whenever(adminAccountRepository.findByUsername("new-admin")).thenReturn(
            AdminAccount(
                id = 1L,
                username = "new-admin",
                displayName = "기존 관리자",
                passwordHash = "hash",
                role = AdminAccountRole.ADMIN,
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            service.createAdminAccount(
                actorId = 1L,
                CreateAdminAccountCommand(
                    username = "new-admin",
                    displayName = "새 관리자",
                    password = "password-123",
                )
            )
        }

        assertThat(exception.message).isEqualTo("이미 사용 중인 관리자 아이디입니다.")
    }

    @Test
    fun `create admin account rejects non super admin actor`() {
        whenever(adminAccountRepository.findById(2L)).thenReturn(
            Optional.of(
                AdminAccount(
                    id = 2L,
                    username = "admin",
                    displayName = "일반 관리자",
                    passwordHash = "hash",
                    role = AdminAccountRole.ADMIN,
                )
            )
        )

        val exception = assertThrows<Exception> {
            service.createAdminAccount(
                actorId = 2L,
                CreateAdminAccountCommand(
                    username = "new-admin",
                    displayName = "새 관리자",
                    password = "password-123",
                )
            )
        }

        assertThat(exception.message).isEqualTo("슈퍼 관리자만 관리자 계정을 관리할 수 있습니다.")
    }
}
