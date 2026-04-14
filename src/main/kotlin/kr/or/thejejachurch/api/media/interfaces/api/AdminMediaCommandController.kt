package kr.or.thejejachurch.api.media.interfaces.api

import jakarta.validation.Valid
import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.common.error.ForbiddenException
import kr.or.thejejachurch.api.media.application.AdminMediaCommandService
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDetailDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoMetadataDto
import kr.or.thejejachurch.api.media.interfaces.dto.UpdatePlaylistRequest
import kr.or.thejejachurch.api.media.interfaces.dto.UpdateVideoMetadataRequest
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/media")
class AdminMediaCommandController(
    private val adminMediaCommandService: AdminMediaCommandService,
    private val adminProperties: AdminProperties,
) {
    @PutMapping("/playlists/{siteKey}")
    fun updatePlaylist(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
        @PathVariable siteKey: String,
        @Valid @RequestBody request: UpdatePlaylistRequest,
    ): AdminPlaylistDetailDto {
        validateAdminKey(adminKey)
        return adminMediaCommandService.updatePlaylist(siteKey, request)
    }

    @PutMapping("/videos/{youtubeVideoId}/metadata")
    fun updateVideoMetadata(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
        @PathVariable youtubeVideoId: String,
        @Valid @RequestBody request: UpdateVideoMetadataRequest,
    ): AdminVideoMetadataDto {
        validateAdminKey(adminKey)
        return adminMediaCommandService.updateVideoMetadata(youtubeVideoId, request)
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
