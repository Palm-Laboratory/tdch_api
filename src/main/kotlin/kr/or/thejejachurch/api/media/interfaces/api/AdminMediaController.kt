package kr.or.thejejachurch.api.media.interfaces.api

import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.common.error.ForbiddenException
import kr.or.thejejachurch.api.media.application.YoutubeSyncService
import kr.or.thejejachurch.api.media.interfaces.dto.AdminMediaSyncResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/v1/admin/media")
class AdminMediaController(
    private val youtubeSyncService: YoutubeSyncService,
    private val adminProperties: AdminProperties,
) {

    @PostMapping("/sync")
    fun sync(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
    ): AdminMediaSyncResponse {
        validateAdminKey(adminKey)
        val summary = youtubeSyncService.syncAllMenus()

        return AdminMediaSyncResponse(
            status = if (summary.failedPlaylists == 0) "ok" else "partial_failure",
            totalPlaylists = summary.totalPlaylists,
            succeededPlaylists = summary.succeededPlaylists,
            failedPlaylists = summary.failedPlaylists,
            completedAt = OffsetDateTime.now(),
        )
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
