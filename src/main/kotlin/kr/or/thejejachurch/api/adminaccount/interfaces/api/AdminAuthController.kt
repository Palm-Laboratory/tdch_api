package kr.or.thejejachurch.api.adminaccount.interfaces.api

import jakarta.validation.Valid
import kr.or.thejejachurch.api.adminaccount.application.AdminAccountAuthService
import kr.or.thejejachurch.api.adminaccount.interfaces.dto.AdminAccountAuthenticateRequest
import kr.or.thejejachurch.api.adminaccount.interfaces.dto.AdminAuthenticatedAccountDto
import kr.or.thejejachurch.api.adminaccount.interfaces.dto.toDto
import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.common.error.ForbiddenException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/auth")
class AdminAuthController(
    private val adminAccountAuthService: AdminAccountAuthService,
    private val adminProperties: AdminProperties,
) {
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: AdminAccountAuthenticateRequest,
    ): AdminAuthenticatedAccountDto =
        adminAccountAuthService.authenticate(
            username = request.username,
            password = request.password,
        ).toDto()

    @GetMapping("/me")
    fun me(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
    ): AdminAuthenticatedAccountDto {
        validateAdminKey(adminKey)
        return adminAccountAuthService.getCurrentAccount(actorId).toDto()
    }

    private fun validateAdminKey(adminKey: String?) {
        val configuredKey = adminProperties.syncKey.trim()
        if (configuredKey.isBlank()) {
            throw IllegalStateException("ADMIN_SYNC_KEY is not configured.")
        }

        if (adminKey.isNullOrBlank() || adminKey != configuredKey) {
            throw ForbiddenException("관리자 키가 올바르지 않습니다.")
        }
    }
}
