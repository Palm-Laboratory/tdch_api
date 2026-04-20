package kr.or.thejejachurch.api.media.video.interfaces.api

import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.common.error.ForbiddenException
import kr.or.thejejachurch.api.media.video.application.VideoService
import kr.or.thejejachurch.api.media.video.interfaces.dto.AdminVideoListResponse
import kr.or.thejejachurch.api.media.video.interfaces.dto.UpdateVideoMetaRequest
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
@RequestMapping("/api/v1/admin/videos")
class VideoAdminController(
    private val videoService: VideoService,
    private val adminProperties: AdminProperties,
) {
    @GetMapping
    fun getVideos(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestParam(required = false) form: YouTubeContentForm?,
        @RequestParam(required = false) menuId: Long?,
    ): AdminVideoListResponse {
        validateAdminKey(adminKey)
        return AdminVideoListResponse(
            items = if (menuId != null) {
                videoService.getAdminVideosByMenu(menuId).map { it.toDto() }
            } else {
                videoService.getAdminVideos(form).map { it.toDto() }
            },
        )
    }

    @GetMapping("/{videoId}")
    fun getVideoDetail(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @PathVariable videoId: String,
    ) = run {
        validateAdminKey(adminKey)
        videoService.getAdminVideoDetail(videoId).toDto()
    }

    @PutMapping("/{videoId}")
    fun updateVideoMeta(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @PathVariable videoId: String,
        @RequestBody request: UpdateVideoMetaRequest,
    ) = run {
        validateAdminKey(adminKey)
        videoService.updateAdminVideoMeta(videoId, request.toCommand()).toDto()
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
