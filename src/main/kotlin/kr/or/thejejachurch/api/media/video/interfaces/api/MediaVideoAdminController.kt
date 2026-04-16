package kr.or.thejejachurch.api.media.video.interfaces.api

import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.common.error.ForbiddenException
import kr.or.thejejachurch.api.media.video.application.MediaVideoService
import kr.or.thejejachurch.api.media.video.interfaces.dto.AdminMediaVideoListResponse
import kr.or.thejejachurch.api.media.video.interfaces.dto.UpdateMediaVideoMetaRequest
import kr.or.thejejachurch.api.media.video.interfaces.dto.toCommand
import kr.or.thejejachurch.api.media.video.interfaces.dto.toDto
import kr.or.thejejachurch.api.youtube.domain.YouTubeContentForm
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/media/videos")
class MediaVideoAdminController(
    private val mediaVideoService: MediaVideoService,
    private val adminProperties: AdminProperties,
) {
    @GetMapping
    fun getVideos(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestParam(required = false) form: YouTubeContentForm?,
    ): AdminMediaVideoListResponse {
        validateAdminKey(adminKey)
        return AdminMediaVideoListResponse(
            items = mediaVideoService.getAdminMediaVideos(form).map { it.toDto() },
        )
    }

    @GetMapping("/{videoId}")
    fun getVideoDetail(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @PathVariable videoId: String,
    ) = run {
        validateAdminKey(adminKey)
        mediaVideoService.getAdminMediaVideoDetail(videoId).toDto()
    }

    @PutMapping("/{videoId}")
    fun updateVideoMeta(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @PathVariable videoId: String,
        @RequestBody request: UpdateMediaVideoMetaRequest,
    ) = run {
        validateAdminKey(adminKey)
        mediaVideoService.updateAdminMediaVideoMeta(videoId, request.toCommand()).toDto()
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
