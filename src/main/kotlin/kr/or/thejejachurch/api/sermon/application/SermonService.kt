package kr.or.thejejachurch.api.sermon.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.menu.infrastructure.persistence.MenuItemRepository
import kr.or.thejejachurch.api.sermon.domain.SermonVideoMeta
import kr.or.thejejachurch.api.sermon.infrastructure.persistence.SermonVideoMetaRepository
import kr.or.thejejachurch.api.youtube.domain.YouTubeContentForm
import kr.or.thejejachurch.api.youtube.domain.YouTubePrivacyStatus
import kr.or.thejejachurch.api.youtube.domain.YouTubeSyncStatus
import kr.or.thejejachurch.api.youtube.domain.YouTubeVideo
import kr.or.thejejachurch.api.youtube.infrastructure.persistence.YouTubePlaylistItemRepository
import kr.or.thejejachurch.api.youtube.infrastructure.persistence.YouTubePlaylistRepository
import kr.or.thejejachurch.api.youtube.infrastructure.persistence.YouTubeVideoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class SermonService(
    private val youTubeVideoRepository: YouTubeVideoRepository,
    private val sermonVideoMetaRepository: SermonVideoMetaRepository,
    private val youTubePlaylistItemRepository: YouTubePlaylistItemRepository,
    private val youTubePlaylistRepository: YouTubePlaylistRepository,
    private val menuItemRepository: MenuItemRepository,
) {
    @Transactional(readOnly = true)
    fun getPublicSermons(form: YouTubeContentForm): PublicSermonList {
        val videos = loadDisplayableVideos()
            .filter { it.video.contentForm == form }
            .sortedByDescending { effectivePublishedAt(it.video, it.meta) ?: OffsetDateTime.MIN }

        val summaries = videos.map { toPublicSummary(it.video, it.meta) }
        return PublicSermonList(
            form = form,
            featured = if (form == YouTubeContentForm.LONGFORM) summaries.firstOrNull() else null,
            items = if (form == YouTubeContentForm.LONGFORM) summaries.drop(1) else summaries,
        )
    }

    @Transactional(readOnly = true)
    fun getPublicSermonDetail(videoId: String): PublicSermonDetail {
        val video = youTubeVideoRepository.findByVideoId(videoId)
            ?: throw NotFoundException("영상을 찾을 수 없습니다. videoId=$videoId")
        val meta = sermonVideoMetaRepository.findByVideoId(video.id!!)
        if (!isDisplayable(video, meta)) {
            throw NotFoundException("공개된 영상을 찾을 수 없습니다. videoId=$videoId")
        }

        val playlistLinks = youTubePlaylistItemRepository.findAllByVideoId(video.id)
            .mapNotNull { playlistItem ->
                val playlist = youTubePlaylistRepository.findById(playlistItem.playlistId).orElse(null) ?: return@mapNotNull null
                val menu = menuItemRepository.findAllByOrderBySortOrderAscIdAsc()
                    .firstOrNull { it.playlistId == playlist.id }
                    ?: return@mapNotNull null
                PublicSermonPlaylistLink(
                    label = menu.label,
                    href = "/videos/${menu.slug}",
                )
            }
            .distinctBy { it.href }

        val related = loadDisplayableVideos()
            .filter { it.video.id != video.id && it.video.contentForm == video.contentForm }
            .sortedByDescending { effectivePublishedAt(it.video, it.meta) ?: OffsetDateTime.MIN }
            .take(6)
            .map { toPublicSummary(it.video, it.meta) }

        return PublicSermonDetail(
            videoId = video.videoId,
            title = meta?.displayTitle?.takeIf { it.isNotBlank() } ?: video.title,
            sourceTitle = video.title,
            preacherName = meta?.preacherName,
            publishedAt = effectivePublishedAt(video, meta),
            thumbnailUrl = effectiveThumbnailUrl(video, meta),
            scriptureReference = meta?.scriptureReference,
            scriptureBody = meta?.scriptureBody,
            messageBody = meta?.messageBody,
            summary = meta?.summary,
            description = video.description,
            contentForm = video.contentForm,
            playlists = playlistLinks,
            related = related,
        )
    }

    @Transactional(readOnly = true)
    fun getAdminSermons(form: YouTubeContentForm?): List<AdminSermonSummary> =
        loadAllVideos()
            .filter { form == null || it.video.contentForm == form }
            .sortedByDescending { effectivePublishedAt(it.video, it.meta) ?: OffsetDateTime.MIN }
            .map { (video, meta) ->
                AdminSermonSummary(
                    videoId = video.videoId,
                    title = meta?.displayTitle?.takeIf { it.isNotBlank() } ?: video.title,
                    sourceTitle = video.title,
                    preacherName = meta?.preacherName,
                    publishedAt = effectivePublishedAt(video, meta),
                    hidden = meta?.hidden ?: false,
                    contentForm = video.contentForm,
                    thumbnailUrl = effectiveThumbnailUrl(video, meta),
                    scriptureReference = meta?.scriptureReference,
                )
            }

    @Transactional(readOnly = true)
    fun getAdminSermonDetail(videoId: String): AdminSermonDetail {
        val video = youTubeVideoRepository.findByVideoId(videoId)
            ?: throw NotFoundException("영상을 찾을 수 없습니다. videoId=$videoId")
        val meta = sermonVideoMetaRepository.findByVideoId(video.id!!)

        return AdminSermonDetail(
            videoId = video.videoId,
            sourceTitle = video.title,
            sourceDescription = video.description,
            sourcePublishedAt = video.publishedAt,
            sourceThumbnailUrl = video.thumbnailUrl,
            title = meta?.displayTitle?.takeIf { it.isNotBlank() } ?: video.title,
            preacherName = meta?.preacherName,
            publishedAt = effectivePublishedAt(video, meta),
            hidden = meta?.hidden ?: false,
            scriptureReference = meta?.scriptureReference,
            scriptureBody = meta?.scriptureBody,
            messageBody = meta?.messageBody,
            summary = meta?.summary,
            thumbnailOverrideUrl = meta?.thumbnailOverrideUrl,
            contentForm = video.contentForm,
        )
    }

    @Transactional
    fun updateAdminSermonMeta(videoId: String, command: UpdateSermonMetaCommand): AdminSermonDetail {
        val video = youTubeVideoRepository.findByVideoId(videoId)
            ?: throw NotFoundException("영상을 찾을 수 없습니다. videoId=$videoId")
        val existing = sermonVideoMetaRepository.findByVideoId(video.id!!)
        val meta = existing ?: SermonVideoMeta(videoId = video.id)

        meta.displayTitle = command.displayTitle?.trim()?.ifBlank { null }
        meta.preacherName = command.preacherName?.trim()?.ifBlank { null }
        meta.displayPublishedAt = command.displayPublishedAt
        meta.hidden = command.hidden
        meta.scriptureReference = command.scriptureReference?.trim()?.ifBlank { null }
        meta.scriptureBody = command.scriptureBody?.trim()?.ifBlank { null }
        meta.messageBody = command.messageBody?.trim()?.ifBlank { null }
        meta.summary = command.summary?.trim()?.ifBlank { null }
        meta.thumbnailOverrideUrl = command.thumbnailOverrideUrl?.trim()?.ifBlank { null }

        sermonVideoMetaRepository.save(meta)
        return getAdminSermonDetail(videoId)
    }

    private fun loadAllVideos(): List<VideoWithMeta> {
        val metasByVideoId = sermonVideoMetaRepository.findAll().associateBy { it.videoId }
        return youTubeVideoRepository.findAll()
            .map { video -> VideoWithMeta(video = video, meta = metasByVideoId[video.id]) }
    }

    private fun loadDisplayableVideos(): List<VideoWithMeta> =
        loadAllVideos().filter { isDisplayable(it.video, it.meta) }

    private fun isDisplayable(video: YouTubeVideo, meta: SermonVideoMeta?): Boolean =
        video.syncStatus == YouTubeSyncStatus.ACTIVE &&
            video.privacyStatus != YouTubePrivacyStatus.PRIVATE &&
            !(meta?.hidden ?: false)

    private fun effectivePublishedAt(video: YouTubeVideo, meta: SermonVideoMeta?): OffsetDateTime? =
        meta?.displayPublishedAt ?: video.publishedAt

    private fun effectiveThumbnailUrl(video: YouTubeVideo, meta: SermonVideoMeta?): String? =
        meta?.thumbnailOverrideUrl ?: video.thumbnailUrl

    private fun toPublicSummary(video: YouTubeVideo, meta: SermonVideoMeta?): PublicSermonSummary =
        PublicSermonSummary(
            videoId = video.videoId,
            title = meta?.displayTitle?.takeIf { it.isNotBlank() } ?: video.title,
            preacherName = meta?.preacherName,
            publishedAt = effectivePublishedAt(video, meta),
            thumbnailUrl = effectiveThumbnailUrl(video, meta),
            scriptureReference = meta?.scriptureReference,
            summary = meta?.summary ?: video.description,
            contentForm = video.contentForm,
            href = if (video.contentForm == YouTubeContentForm.SHORTFORM) {
                "/sermons/shorts/${video.videoId}"
            } else {
                "/sermons/${video.videoId}"
            },
        )

    private data class VideoWithMeta(
        val video: YouTubeVideo,
        val meta: SermonVideoMeta?,
    )
}
