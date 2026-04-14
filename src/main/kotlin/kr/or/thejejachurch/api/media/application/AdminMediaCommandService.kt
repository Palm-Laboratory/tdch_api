package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.VideoMetadata
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.VideoMetadataRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeVideoRepository
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDetailDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoMetadataDto
import kr.or.thejejachurch.api.media.interfaces.dto.UpdatePlaylistRequest
import kr.or.thejejachurch.api.media.interfaces.dto.UpdateVideoMetadataRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AdminMediaCommandService(
    private val contentMenuRepository: ContentMenuRepository,
    private val youtubePlaylistRepository: YoutubePlaylistRepository,
    private val youtubeVideoRepository: YoutubeVideoRepository,
    private val videoMetadataRepository: VideoMetadataRepository,
    private val adminMediaQueryService: AdminMediaQueryService,
) {

    @Transactional
    fun updatePlaylist(
        siteKey: String,
        request: UpdatePlaylistRequest,
    ): AdminPlaylistDetailDto {
        val menu = contentMenuRepository.findBySiteKey(siteKey)
            ?: throw NotFoundException("Unknown siteKey: $siteKey")

        val menuName = request.menuName.trim()
        val slug = request.slug.trim().lowercase()

        require(menuName.isNotBlank()) { "menuName must not be blank" }
        require(slug.isNotBlank()) { "slug must not be blank" }

        if (slug != menu.slug) {
            val existing = contentMenuRepository.findBySlug(slug)
            if (existing != null && existing.id != menu.id) {
                throw IllegalArgumentException("이미 사용 중인 slug입니다.")
            }
        }

        menu.menuName = menuName
        menu.slug = slug
        menu.active = request.active
        contentMenuRepository.save(menu)

        menu.id?.let(youtubePlaylistRepository::findByContentMenuId)?.let { playlist ->
            playlist.syncEnabled = request.syncEnabled
            youtubePlaylistRepository.save(playlist)
        }

        return adminMediaQueryService.getPlaylist(siteKey)
    }

    @Transactional
    fun updateVideoMetadata(
        youtubeVideoId: String,
        request: UpdateVideoMetadataRequest,
    ): AdminVideoMetadataDto {
        val video = youtubeVideoRepository.findByYoutubeVideoId(youtubeVideoId)
            ?: throw NotFoundException("Unknown youtubeVideoId: $youtubeVideoId")
        val internalVideoId = video.id ?: throw IllegalStateException("youtube video id is missing")
        val metadata = videoMetadataRepository.findByYoutubeVideoId(internalVideoId)
            ?: VideoMetadata(youtubeVideoId = internalVideoId)

        metadata.visible = request.visible
        metadata.featured = request.featured
        metadata.pinnedRank = request.pinnedRank
        metadata.manualTitle = request.manualTitle.normalizedOrNull()
        metadata.manualThumbnailUrl = request.manualThumbnailUrl.normalizedOrNull()
        metadata.manualPublishedDate = request.manualPublishedDate.normalizedOrNull()?.let(LocalDate::parse)
        metadata.manualKind = request.manualKind.normalizedOrNull()?.let(ContentKind::valueOf)
        metadata.preacher = request.preacher.normalizedOrNull()
        metadata.scripture = request.scripture.normalizedOrNull()
        metadata.scriptureBody = request.scriptureBody.normalizedOrNull()
        metadata.serviceType = request.serviceType.normalizedOrNull()
        metadata.summary = request.summary.normalizedOrNull()
        metadata.tags = request.tags.mapNotNull { it.normalizedOrNull() }.distinct().toTypedArray()

        videoMetadataRepository.save(metadata)

        return adminMediaQueryService.getVideoMetadata(youtubeVideoId)
    }

    private fun String?.normalizedOrNull(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}
