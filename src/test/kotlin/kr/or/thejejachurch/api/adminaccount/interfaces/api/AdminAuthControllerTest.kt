package kr.or.thejejachurch.api.adminaccount.interfaces.api

import kr.or.thejejachurch.api.adminaccount.application.AdminAccountAuthService
import kr.or.thejejachurch.api.adminaccount.application.AuthenticatedAdminAccount
import kr.or.thejejachurch.api.adminaccount.domain.AdminAccountRole
import kr.or.thejejachurch.api.adminaccount.interfaces.dto.AdminAccountAuthenticateRequest
import kr.or.thejejachurch.api.common.error.UnauthorizedException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AdminAuthControllerTest {

    private val adminAccountAuthService: AdminAccountAuthService = mock()

    @Test
    fun `login returns account when credentials are valid`() {
        whenever(adminAccountAuthService.authenticate("super-admin", "password-123")).thenReturn(
            AuthenticatedAdminAccount(
                id = 1L,
                username = "super-admin",
                displayName = "슈퍼 관리자",
                role = AdminAccountRole.SUPER_ADMIN,
            )
        )
        val controller = AdminAuthController(adminAccountAuthService)

        val response = controller.login(
            AdminAccountAuthenticateRequest(username = "super-admin", password = "password-123"),
        )

        assertThat(response.username).isEqualTo("super-admin")
        assertThat(response.role).isEqualTo(AdminAccountRole.SUPER_ADMIN)
    }

    @Test
    fun `login throws unauthorized when credentials are invalid`() {
        whenever(adminAccountAuthService.authenticate("super-admin", "wrong-password")).thenThrow(
            UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.")
        )
        val controller = AdminAuthController(adminAccountAuthService)

        assertThrows<UnauthorizedException> {
            controller.login(
                AdminAccountAuthenticateRequest(username = "super-admin", password = "wrong-password"),
            )
        }
    }
}
