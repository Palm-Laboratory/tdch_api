package kr.or.thejejachurch.api.media.video.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.menu.domain.MenuItem
import kr.or.thejejachurch.api.menu.domain.MenuStatus
import kr.or.thejejachurch.api.menu.domain.MenuType
import kr.or.thejejachurch.api.menu.infrastructure.persistence.MenuItemRepository
import kr.or.thejejachurch.api.media.video.domain.VideoMeta
import kr.or.thejejachurch.api.media.video.infrastructure.persistence.VideoMetaRepository
import kr.or.thejejachurch.api.youtube.domain.YouTubeContentForm
import kr.or.thejejachurch.api.youtube.domain.YouTubePrivacyStatus
import kr.or.thejejachurch.api.youtube.domain.YouTubeSyncStatus
import kr.or.thejejachurch.api.youtube.domain.YouTubeVideo
import kr.or.thejejachurch.api.youtube.infrastructure.persistence.YouTubePlaylistItemRepository
import kr.or.thejejachurch.api.youtube.infrastructure.persistence.YouTubePlaylistRepository
import kr.or.thejejachurch.api.youtube.infrastructure.persistence.YouTubeVideoRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class VideoService(
    private val youTubeVideoRepository: YouTubeVideoRepository,
    private val videoMetaRepository: VideoMetaRepository,
    private val youTubePlaylistItemRepository: YouTubePlaylistItemRepository,
    private val youTubePlaylistRepository: YouTubePlaylistRepository,
    private val menuItemRepository: MenuItemRepository,
) {
    @Transactional(readOnly = true)
    fun getPublicPlaylistVideos(slug: String): PublicVideoList {
        val context = loadPlaylistContext(slug)
        val summaries = loadPlaylistSummaries(context.menu)

        return toPublicVideoList(summaries)
    }

    @Transactional(readOnly = true)
    fun getPublicPlaylistVideosByPath(path: String): PublicVideoList {
        val menu = resolvePublishedPlaylistMenuByPath(path)
        return toPublicVideoList(loadPlaylistSummaries(menu))
    }

    @Transactional(readOnly = true)
    fun getPublicPlaylistVideoDetail(slug: String, videoId: String): PublicVideoDetail {
        val context = loadPlaylistContext(slug)
        return buildPlaylistVideoDetail(context.menu, videoId)
    }

    @Transactional(readOnly = true)
    fun getPublicPlaylistVideoDetailByPath(path: String, videoId: String): PublicVideoDetail {
        val menu = resolvePublishedPlaylistMenuByPath(path)
        return buildPlaylistVideoDetail(menu, videoId)
    }

    private fun buildPlaylistVideoDetail(menu: MenuItem, videoId: String): PublicVideoDetail {
        val orderedVideos = loadOrderedPlaylistVideos(menu)
        val target = orderedVideos.firstOrNull { it.video.videoId == videoId }
            ?: throw NotFoundException("재생목록에서 영상을 찾을 수 없습니다. menuId=${menu.id}, videoId=$videoId")

        val related = orderedVideos
            .filter { it.video.videoId != videoId }
            .take(6)
            .map { toPublicSummary(it.video, it.meta, buildPlaylistVideoHref(menu, it.video.videoId)) }

        return PublicVideoDetail(
            videoId = target.video.videoId,
            title = effectiveDisplayTitle(target.video, target.meta),
            sourceTitle = menu.label,
            preacherName = target.meta?.preacherName,
            publishedAt = effectivePublishedAt(target.video, target.meta),
            thumbnailUrl = effectiveThumbnailUrl(target.video, target.meta),
            scriptureReference = target.meta?.scriptureReference,
            scriptureBody = target.meta?.scriptureBody,
            messageBody = target.meta?.messageBody,
            summary = target.meta?.summary,
            description = target.video.description,
            contentForm = target.video.contentForm,
            playlists = buildPlaylistLinks(target.video.id!!, menu.id!!),
            related = related,
        )
    }

    @Transactional(readOnly = true)
    fun resolvePublicVideoHref(videoId: String): String? {
        val video = youTubeVideoRepository.findByVideoId(videoId) ?: return null
        val meta = videoMetaRepository.findByVideoId(video.id!!)

        if (!isDisplayable(video, meta)) {
            return null
        }

        return findPublicMenuHrefForVideoEntity(video.id, video.videoId)
    }

    @Transactional(readOnly = true)
    fun getAdminVideos(form: YouTubeContentForm?): List<AdminVideoSummary> =
        loadAllVideos()
            .filter { form == null || it.video.contentForm == form }
            .sortedByDescending { effectivePublishedAt(it.video, it.meta) ?: OffsetDateTime.MIN }
            .map { (video, meta) ->
                AdminVideoSummary(
                    videoId = video.videoId,
                    title = effectiveDisplayTitle(video, meta),
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
    fun getAdminVideoDetail(videoId: String): AdminVideoDetail {
        val video = youTubeVideoRepository.findByVideoId(videoId)
            ?: throw NotFoundException("영상을 찾을 수 없습니다. videoId=$videoId")
        val meta = videoMetaRepository.findByVideoId(video.id!!)

        return AdminVideoDetail(
            videoId = video.videoId,
            sourceTitle = video.title,
            sourceDescription = video.description,
            sourcePublishedAt = video.publishedAt,
            sourceThumbnailUrl = video.thumbnailUrl,
            title = effectiveDisplayTitle(video, meta),
            preacherName = meta?.preacherName,
            publishedAt = effectivePublishedAt(video, meta),
            hidden = meta?.hidden ?: false,
            scriptureReference = meta?.scriptureReference,
            scriptureBody = meta?.scriptureBody,
            messageBody = meta?.messageBody,
            summary = meta?.summary,
            thumbnailOverrideUrl = meta?.thumbnailOverrideUrl,
            contentForm = video.contentForm,
            publicHref = buildAdminPublicHref(video.id, video.videoId),
        )
    }

    @Transactional
    fun updateAdminVideoMeta(videoId: String, command: UpdateVideoMetaCommand): AdminVideoDetail {
        val video = youTubeVideoRepository.findByVideoId(videoId)
            ?: throw NotFoundException("영상을 찾을 수 없습니다. videoId=$videoId")
        val existing = videoMetaRepository.findByVideoId(video.id!!)
        val meta = existing ?: VideoMeta(videoId = video.id)

        meta.displayTitle = command.displayTitle?.trim()?.ifBlank { null }
        meta.preacherName = command.preacherName?.trim()?.ifBlank { null }
        meta.displayPublishedAt = command.displayPublishedAt
        meta.hidden = command.hidden
        meta.scriptureReference = command.scriptureReference?.trim()?.ifBlank { null }
        meta.scriptureBody = command.scriptureBody?.trim()?.ifBlank { null }
        meta.messageBody = command.messageBody?.trim()?.ifBlank { null }
        meta.summary = command.summary?.trim()?.ifBlank { null }
        meta.thumbnailOverrideUrl = command.thumbnailOverrideUrl?.trim()?.ifBlank { null }

        videoMetaRepository.save(meta)
        return getAdminVideoDetail(videoId)
    }

    private fun loadAllVideos(): List<VideoWithMeta> {
        val metasByVideoId = videoMetaRepository.findAll().associateBy { it.videoId }
        return youTubeVideoRepository.findAll()
            .map { video -> VideoWithMeta(video = video, meta = metasByVideoId[video.id]) }
    }

    private fun loadPlaylistContext(slug: String): PlaylistContext {
        val menu = menuItemRepository.findByTypeAndStatusAndSlug(
            type = MenuType.YOUTUBE_PLAYLIST,
            status = MenuStatus.PUBLISHED,
            slug = slug,
        ) ?: throw NotFoundException("재생목록을 찾을 수 없습니다. slug=$slug")

        val playlist = menu.playlistId?.let { youTubePlaylistRepository.findByIdOrNull(it) }
            ?: throw NotFoundException("유튜브 재생목록 정보를 찾을 수 없습니다. slug=$slug")

        return PlaylistContext(menu = menu, playlistId = playlist.id!!)
    }

    private fun loadPlaylistSummaries(menu: MenuItem): List<PublicVideoSummary> =
        loadOrderedPlaylistVideos(menu)
            .map { toPublicSummary(it.video, it.meta, buildPlaylistVideoHref(menu, it.video.videoId)) }

    private fun loadOrderedPlaylistVideos(menu: MenuItem): List<VideoWithMeta> {
        val playlistId = menu.playlistId
            ?: throw NotFoundException("재생목록 정보가 연결되지 않았습니다. menuId=${menu.id}")
        val displayableByVideoId = loadDisplayableVideos().associateBy { it.video.id!! }

        return youTubePlaylistItemRepository.findAllByPlaylistIdOrderByPositionAsc(playlistId)
            .mapNotNull { playlistItem -> displayableByVideoId[playlistItem.videoId] }
    }

    private fun loadDisplayableVideos(): List<VideoWithMeta> =
        loadAllVideos().filter { isDisplayable(it.video, it.meta) }

    private fun toPublicVideoList(summaries: List<PublicVideoSummary>): PublicVideoList =
        PublicVideoList(
            form = summaries.firstOrNull()?.contentForm ?: YouTubeContentForm.LONGFORM,
            featured = summaries.firstOrNull(),
            items = summaries.drop(1),
        )

    private fun isDisplayable(video: YouTubeVideo, meta: VideoMeta?): Boolean =
        video.syncStatus == YouTubeSyncStatus.ACTIVE &&
            video.privacyStatus != YouTubePrivacyStatus.PRIVATE &&
            !(meta?.hidden ?: false)

    private fun effectiveDisplayTitle(video: YouTubeVideo, meta: VideoMeta?): String =
        meta?.displayTitle?.takeIf { it.isNotBlank() } ?: video.title

    private fun effectivePublishedAt(video: YouTubeVideo, meta: VideoMeta?): OffsetDateTime? =
        meta?.displayPublishedAt ?: video.publishedAt

    private fun effectiveThumbnailUrl(video: YouTubeVideo, meta: VideoMeta?): String? =
        meta?.thumbnailOverrideUrl ?: video.thumbnailUrl

    private fun buildPlaylistLinks(videoEntityId: Long, currentMenuId: Long): List<PublicVideoPlaylistLink> =
        run {
            val publishedItems = menuItemRepository.findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED)
            val itemsById = publishedItems.associateBy { it.id!! }

            youTubePlaylistItemRepository.findAllByVideoId(videoEntityId)
            .mapNotNull { playlistItem ->
                val playlist = youTubePlaylistRepository.findById(playlistItem.playlistId).orElse(null) ?: return@mapNotNull null
                val menu = publishedItems
                    .firstOrNull { it.playlistId == playlist.id }
                    ?: return@mapNotNull null
                PublicVideoPlaylistLink(
                    label = menu.label,
                    href = buildPlaylistPath(menu, itemsById),
                )
            }
            .distinctBy { it.href }
        }

    private fun buildAdminPublicHref(videoEntityId: Long, videoId: String): String? =
        findPublicMenuHrefForVideoEntity(videoEntityId, videoId)

    private fun findPublicMenuHrefForVideoEntity(videoEntityId: Long, videoId: String): String? =
        run {
            val publishedItems = menuItemRepository.findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED)
            val itemsById = publishedItems.associateBy { it.id!! }

            youTubePlaylistItemRepository.findAllByVideoId(videoEntityId)
            .mapNotNull { playlistItem ->
                val playlist = youTubePlaylistRepository.findByIdOrNull(playlistItem.playlistId) ?: return@mapNotNull null
                val menu = publishedItems
                    .firstOrNull {
                        it.playlistId == playlist.id &&
                            it.type == MenuType.YOUTUBE_PLAYLIST &&
                            it.status == MenuStatus.PUBLISHED
                    }
                    ?: return@mapNotNull null
                "${buildPlaylistPath(menu, itemsById)}/$videoId"
            }
            .firstOrNull()
        }

    private fun toPublicSummary(
        video: YouTubeVideo,
        meta: VideoMeta?,
        href: String,
    ): PublicVideoSummary =
        PublicVideoSummary(
            videoId = video.videoId,
            title = effectiveDisplayTitle(video, meta),
            preacherName = meta?.preacherName,
            publishedAt = effectivePublishedAt(video, meta),
            thumbnailUrl = effectiveThumbnailUrl(video, meta),
            scriptureReference = meta?.scriptureReference,
            summary = meta?.summary ?: video.description,
            contentForm = video.contentForm,
            href = href,
        )

    private fun buildPlaylistVideoHref(menu: MenuItem, videoId: String): String {
        val publishedItems = menuItemRepository.findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED)
        val itemsById = publishedItems.associateBy { it.id!! }
        return "${buildPlaylistPath(menu, itemsById)}/$videoId"
    }

    private fun buildPlaylistPath(menu: MenuItem, itemsById: Map<Long, MenuItem>): String {
        val segments = mutableListOf<String>()
        var current: MenuItem? = menu

        while (current != null) {
            segments += current.slug
            current = current.parentId?.let(itemsById::get)
        }

        return "/videos/${segments.asReversed().joinToString("/")}"
    }

    private fun resolvePublishedPlaylistMenuByPath(path: String): MenuItem {
        val publishedItems = menuItemRepository.findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED)
        val itemsById = publishedItems.associateBy { it.id!! }
        val normalizedPath = normalizeLookupPath(path)

        return publishedItems.firstOrNull { item ->
            item.type == MenuType.YOUTUBE_PLAYLIST && buildPlaylistPath(item, itemsById) == normalizedPath
        } ?: throw NotFoundException("재생목록을 찾을 수 없습니다. path=$path")
    }

    private fun normalizeLookupPath(path: String): String {
        val trimmed = path.substringBefore('?').substringBefore('#').trim()
        if (trimmed.isBlank()) {
            return "/"
        }
        return if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    }

    private data class VideoWithMeta(
        val video: YouTubeVideo,
        val meta: VideoMeta?,
    )

    private data class PlaylistContext(
        val menu: MenuItem,
        val playlistId: Long,
    )
}
