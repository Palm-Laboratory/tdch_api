package kr.or.thejejachurch.api.menu.interfaces.api

import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.common.error.ForbiddenException
import kr.or.thejejachurch.api.menu.application.MenuManagementService
import kr.or.thejejachurch.api.menu.interfaces.dto.ReplaceMenuTreeRequest
import kr.or.thejejachurch.api.menu.interfaces.dto.toCommand
import kr.or.thejejachurch.api.menu.interfaces.dto.toDto
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/menu")
class MenuAdminController(
    private val menuManagementService: MenuManagementService,
    private val adminProperties: AdminProperties,
) {
    @GetMapping
    fun getMenuTree(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
    ) =
        run {
        validateAdminKey(adminKey)
        menuManagementService.getAdminSnapshot(actorId).toDto()
        }

    @PutMapping("/tree")
    fun replaceTree(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
        @RequestBody request: ReplaceMenuTreeRequest,
    ) =
        run {
        validateAdminKey(adminKey)
        menuManagementService.replaceTree(actorId, request.toCommand()).toDto()
        }

    @DeleteMapping("/{id}")
    fun deleteMenu(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
        @PathVariable id: Long,
    ) {
        validateAdminKey(adminKey)
        menuManagementService.deleteMenuItem(actorId, id)
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
