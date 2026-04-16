package kr.or.thejejachurch.api.sermon.interfaces.api

import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.common.error.ForbiddenException
import kr.or.thejejachurch.api.sermon.application.SermonService
import kr.or.thejejachurch.api.sermon.interfaces.dto.AdminSermonListResponse
import kr.or.thejejachurch.api.sermon.interfaces.dto.UpdateSermonMetaRequest
import kr.or.thejejachurch.api.sermon.interfaces.dto.toCommand
import kr.or.thejejachurch.api.sermon.interfaces.dto.toDto
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
@RequestMapping("/api/v1/admin/sermons")
class SermonAdminController(
    private val sermonService: SermonService,
    private val adminProperties: AdminProperties,
) {
    @GetMapping
    fun getSermons(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestParam(required = false) form: YouTubeContentForm?,
    ): AdminSermonListResponse {
        validateAdminKey(adminKey)
        return AdminSermonListResponse(
            items = sermonService.getAdminSermons(form).map { it.toDto() },
        )
    }

    @GetMapping("/{videoId}")
    fun getSermonDetail(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @PathVariable videoId: String,
    ) = run {
        validateAdminKey(adminKey)
        sermonService.getAdminSermonDetail(videoId).toDto()
    }

    @PutMapping("/{videoId}")
    fun updateSermonMeta(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @PathVariable videoId: String,
        @RequestBody request: UpdateSermonMetaRequest,
    ) = run {
        validateAdminKey(adminKey)
        sermonService.updateAdminSermonMeta(videoId, request.toCommand()).toDto()
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
