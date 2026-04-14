package kr.or.thejejachurch.api.media.interfaces.api

import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.common.error.ForbiddenException
import kr.or.thejejachurch.api.media.application.AdminMediaQueryService
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDetailDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistListResponse
import kr.or.thejejachurch.api.media.interfaces.dto.AdminSyncJobDetailDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminSyncJobListResponse
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoListResponse
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoMetadataDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/media")
class AdminMediaQueryController(
    private val adminMediaQueryService: AdminMediaQueryService,
    private val adminProperties: AdminProperties,
) {
    @GetMapping("/playlists")
    fun getPlaylists(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
        @RequestParam(required = false) kind: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) order: String?,
    ): AdminPlaylistListResponse {
        validateAdminKey(adminKey)
        return adminMediaQueryService.getPlaylists(kind, search, page, size, sort, order)
    }

    @GetMapping("/playlists/{siteKey}")
    fun getPlaylist(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
        @PathVariable siteKey: String,
    ): AdminPlaylistDetailDto {
        validateAdminKey(adminKey)
        return adminMediaQueryService.getPlaylist(siteKey)
    }

    @GetMapping("/playlists/{siteKey}/videos")
    fun getVideos(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
        @PathVariable siteKey: String,
        @RequestParam(required = false) visible: String?,
        @RequestParam(required = false) featured: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): AdminVideoListResponse {
        validateAdminKey(adminKey)
        return adminMediaQueryService.getPlaylistVideos(siteKey, visible, featured, search, page, size)
    }

    @GetMapping("/videos/{youtubeVideoId}/metadata")
    fun getVideoMetadata(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
        @PathVariable youtubeVideoId: String,
    ): AdminVideoMetadataDto {
        validateAdminKey(adminKey)
        return adminMediaQueryService.getVideoMetadata(youtubeVideoId)
    }

    @GetMapping("/sync-jobs")
    fun getSyncJobs(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
    ): AdminSyncJobListResponse {
        validateAdminKey(adminKey)
        return adminMediaQueryService.getSyncJobs()
    }

    @GetMapping("/sync-jobs/{jobId}")
    fun getSyncJob(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?,
        @RequestHeader("X-Admin-Actor-Id") actorId: Long,
        @PathVariable jobId: Long,
    ): AdminSyncJobDetailDto {
        validateAdminKey(adminKey)
        return adminMediaQueryService.getSyncJob(jobId)
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
