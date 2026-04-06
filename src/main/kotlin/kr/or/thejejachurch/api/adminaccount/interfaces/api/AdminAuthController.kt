package kr.or.thejejachurch.api.adminaccount.interfaces.api

import jakarta.validation.Valid
import kr.or.thejejachurch.api.adminaccount.application.AdminAccountAuthService
import kr.or.thejejachurch.api.adminaccount.interfaces.dto.AdminAccountAuthenticateRequest
import kr.or.thejejachurch.api.adminaccount.interfaces.dto.AdminAuthenticatedAccountDto
import kr.or.thejejachurch.api.adminaccount.interfaces.dto.toDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/auth")
class AdminAuthController(
    private val adminAccountAuthService: AdminAccountAuthService,
) {
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: AdminAccountAuthenticateRequest,
    ): AdminAuthenticatedAccountDto =
        adminAccountAuthService.authenticate(
            username = request.username,
            password = request.password,
        ).toDto()
}
