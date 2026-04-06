package kr.or.thejejachurch.api.adminaccount.interfaces.api

import jakarta.validation.Valid
import kr.or.thejejachurch.api.adminaccount.application.AdminAccountManagementService
import kr.or.thejejachurch.api.adminaccount.application.CreateAdminAccountCommand
import kr.or.thejejachurch.api.adminaccount.interfaces.dto.AdminAccountCreateRequest
import kr.or.thejejachurch.api.adminaccount.interfaces.dto.AdminAccountsResponse
import kr.or.thejejachurch.api.adminaccount.interfaces.dto.toDto
import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.common.error.ForbiddenException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/accounts")
class AdminAccountController(
    private val adminAccountManagementService: AdminAccountManagementService,
    private val adminProperties: AdminProperties,
) {
    @GetMapping("/{id}")
    fun getAccount(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
        @PathVariable id: Long,
    ) = run {
        validateAdminKey(adminKey)
        adminAccountManagementService.getAccount(actorId, id).toDto()
    }

    @GetMapping
    fun getAccounts(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
    ): AdminAccountsResponse {
        validateAdminKey(adminKey)
        return AdminAccountsResponse(
            accounts = adminAccountManagementService.getAccounts(actorId).map { it.toDto() }
        )
    }

    @PostMapping
    fun createAccount(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
        @Valid @RequestBody request: AdminAccountCreateRequest,
    ) = run {
        validateAdminKey(adminKey)
        adminAccountManagementService.createAdminAccount(
            actorId = actorId,
            CreateAdminAccountCommand(
                username = request.username,
                displayName = request.displayName,
                password = request.password,
            )
        ).toDto()
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
