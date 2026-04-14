package kr.or.thejejachurch.api.navigation.interfaces.api

import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.common.error.ForbiddenException
import kr.or.thejejachurch.api.navigation.application.AdminNavigationCommandService
import kr.or.thejejachurch.api.navigation.application.AdminNavigationQueryService
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminContentMenusResponse
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationItemDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationSetsResponse
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationTreeResponse
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationUpsertRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/navigation")
class AdminNavigationController(
    private val adminNavigationQueryService: AdminNavigationQueryService,
    private val adminNavigationCommandService: AdminNavigationCommandService,
    private val adminProperties: AdminProperties,
) {

    @GetMapping("/sets")
    fun getSets(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
    ): AdminNavigationSetsResponse {
        validateAdminKey(adminKey)
        return adminNavigationQueryService.getNavigationSets()
    }

    @GetMapping("/items")
    fun getItems(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestParam(defaultValue = "true") includeHidden: Boolean,
    ): AdminNavigationTreeResponse {
        validateAdminKey(adminKey)
        return adminNavigationQueryService.getNavigationItems(includeHidden = includeHidden)
    }

    @GetMapping("/items/{id}")
    fun getItem(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @PathVariable id: Long,
    ): AdminNavigationItemDto {
        validateAdminKey(adminKey)
        return adminNavigationQueryService.getNavigationItem(id)
    }

    @PostMapping("/items")
    fun createItem(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @Valid @RequestBody request: AdminNavigationUpsertRequest,
    ): AdminNavigationItemDto {
        validateAdminKey(adminKey)
        return adminNavigationCommandService.createNavigationItem(request)
    }

    @PutMapping("/items/{id}")
    fun updateItem(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminNavigationUpsertRequest,
    ): AdminNavigationItemDto {
        validateAdminKey(adminKey)
        return adminNavigationCommandService.updateNavigationItem(id, request)
    }

    @DeleteMapping("/items/{id}")
    fun deleteItem(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @PathVariable id: Long,
    ) {
        validateAdminKey(adminKey)
        adminNavigationCommandService.deleteNavigationItem(id)
    }

    @GetMapping("/content-menus")
    fun getContentMenus(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
    ): AdminContentMenusResponse {
        validateAdminKey(adminKey)
        return adminNavigationQueryService.getContentMenus()
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
