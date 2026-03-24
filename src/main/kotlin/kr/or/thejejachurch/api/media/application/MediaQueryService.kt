package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.PlaylistVideo
import kr.or.thejejachurch.api.media.domain.VideoMetadata
import kr.or.thejejachurch.api.media.domain.YoutubeVideo
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.PlaylistVideoRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.VideoMetadataRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeVideoRepository
import kr.or.thejejachurch.api.media.interfaces.dto.HomeMediaResponse
import kr.or.thejejachurch.api.media.interfaces.dto.MediaItemDto
import kr.or.thejejachurch.api.media.interfaces.dto.MediaListResponse
import kr.or.thejejachurch.api.media.interfaces.dto.MenuDto
import kr.or.thejejachurch.api.media.interfaces.dto.VideoDetailResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Service
class MediaQueryService(
    private val contentMenuRepository: ContentMenuRepository,
    private val youtubePlaylistRepository: YoutubePlaylistRepository,
    private val playlistVideoRepository: PlaylistVideoRepository,
    private val youtubeVideoRepository: YoutubeVideoRepository,
    private val videoMetadataRepository: VideoMetadataRepository,
) {

    @Transactional(readOnly = true)
    fun getMenus(): List<MenuDto> =
        contentMenuRepository.findAllByActiveTrueOrderByIdAsc().map(::toMenuDto)

    @Transactional(readOnly = true)
    fun getHome(): HomeMediaResponse {
        val allMessageItems = getMenuItems(siteKey = "messages")
        val latestMessages = allMessageItems.take(HOME_SECTION_SIZE)
        val latestDevotions = getMenuItems(siteKey = "better-devotion", limit = HOME_SECTION_SIZE)
        val latestShorts = getMenuItems(siteKey = "its-okay", limit = HOME_SECTION_SIZE)

        return HomeMediaResponse(
            featuredSermons = allMessageItems.filter { it.featured }.take(HOME_SECTION_SIZE),
            latestMessages = latestMessages,
            latestDevotions = latestDevotions,
            latestShorts = latestShorts,
        )
    }

    @Transactional(readOnly = true)
    fun getVideos(siteKey: String, page: Int, size: Int): MediaListResponse {
        val menu = getActiveMenu(siteKey)
        val playlist = menu.id?.let(youtubePlaylistRepository::findByContentMenuIdAndSyncEnabledTrue)
        if (playlist?.id == null) {
            return MediaListResponse(
                menu = toMenuDto(menu),
                page = page,
                size = size,
                totalElements = 0,
                totalPages = 0,
                items = emptyList(),
            )
        }

        val playlistPage = playlistVideoRepository.findAllByYoutubePlaylistIdAndIsActiveTrueOrderByPositionAsc(
            playlist.id,
            PageRequest.of(page, size),
        )
        val orderedVideos = loadOrderedVideos(playlistPage.content)
        val orderedMetadata = loadMetadataMap(orderedVideos.values.mapNotNull { it.id })
        val items = orderedVideos.values.map { video ->
            toMediaItemDto(
                video = video,
                metadata = video.id?.let(orderedMetadata::get),
            )
        }

        return MediaListResponse(
            menu = toMenuDto(menu),
            page = page,
            size = size,
            totalElements = playlistPage.totalElements,
            totalPages = playlistPage.totalPages,
            items = items,
        )
    }

    @Transactional(readOnly = true)
    fun getVideo(youtubeVideoId: String): VideoDetailResponse {
        val video = youtubeVideoRepository.findByYoutubeVideoId(youtubeVideoId)
            ?: throw NotFoundException("Unknown youtubeVideoId: $youtubeVideoId")
        val metadata = video.id?.let(videoMetadataRepository::findByYoutubeVideoId)

        return VideoDetailResponse(
            youtubeVideoId = video.youtubeVideoId,
            title = video.title,
            displayTitle = resolveDisplayTitle(video, metadata),
            description = video.description.orEmpty(),
            thumbnailUrl = resolveThumbnailUrl(video, metadata),
            youtubeUrl = video.youtubeWatchUrl,
            embedUrl = video.youtubeEmbedUrl,
            contentKind = resolveContentKind(video, metadata).name,
            publishedAt = video.publishedAt.toString(),
            preacher = metadata?.preacher,
            scripture = metadata?.scripture,
            serviceType = metadata?.serviceType,
            summary = metadata?.summary,
            tags = metadata?.tags?.toList().orEmpty(),
        )
    }

    private fun getMenuItems(siteKey: String, limit: Int? = null): List<MediaItemDto> {
        val menu = getActiveMenu(siteKey)
        val playlist = menu.id?.let(youtubePlaylistRepository::findByContentMenuIdAndSyncEnabledTrue)
            ?: return emptyList()

        val activePlaylistVideos = playlist.id?.let(playlistVideoRepository::findAllByYoutubePlaylistIdAndIsActiveTrueOrderByPositionAsc)
            .orEmpty()
        if (activePlaylistVideos.isEmpty()) {
            return emptyList()
        }

        val orderedVideos = loadOrderedVideos(activePlaylistVideos)
        val orderedMetadata = loadMetadataMap(orderedVideos.values.mapNotNull { it.id })

        return orderedVideos.values.map { video ->
            toMediaItemDto(
                video = video,
                metadata = video.id?.let(orderedMetadata::get),
            )
        }.let { items ->
            if (limit == null) items else items.take(limit)
        }
    }

    private fun loadOrderedVideos(playlistVideos: List<PlaylistVideo>): LinkedHashMap<Long, YoutubeVideo> {
        val videoIds = playlistVideos.map { it.youtubeVideoId }
        if (videoIds.isEmpty()) {
            return linkedMapOf()
        }

        val videosById = youtubeVideoRepository.findAllById(videoIds).associateByNotNull { it.id }
        val ordered = linkedMapOf<Long, YoutubeVideo>()

        playlistVideos.forEach { playlistVideo ->
            val video = videosById[playlistVideo.youtubeVideoId] ?: return@forEach
            ordered[playlistVideo.youtubeVideoId] = video
        }

        return ordered
    }

    private fun loadMetadataMap(videoIds: List<Long>): Map<Long, VideoMetadata> {
        if (videoIds.isEmpty()) {
            return emptyMap()
        }
        return videoMetadataRepository.findAllByYoutubeVideoIdIn(videoIds).associateBy { it.youtubeVideoId }
    }

    private fun getActiveMenu(siteKey: String): ContentMenu =
        contentMenuRepository.findBySiteKey(siteKey)
            ?.takeIf { it.active }
            ?: throw NotFoundException("Unknown siteKey: $siteKey")

    private fun toMenuDto(menu: ContentMenu): MenuDto = MenuDto(
        siteKey = menu.siteKey,
        name = menu.menuName,
        slug = menu.slug,
        contentKind = menu.contentKind.name,
    )

    private fun toMediaItemDto(
        video: YoutubeVideo,
        metadata: VideoMetadata?,
    ): MediaItemDto = MediaItemDto(
        youtubeVideoId = video.youtubeVideoId,
        title = video.title,
        displayTitle = resolveDisplayTitle(video, metadata),
        thumbnailUrl = resolveThumbnailUrl(video, metadata),
        youtubeUrl = video.youtubeWatchUrl,
        embedUrl = video.youtubeEmbedUrl,
        publishedAt = video.publishedAt.toString(),
        displayDate = resolveDisplayDate(video.publishedAt, metadata),
        contentKind = resolveContentKind(video, metadata).name,
        preacher = metadata?.preacher,
        scripture = metadata?.scripture,
        serviceType = metadata?.serviceType,
        featured = metadata?.featured ?: false,
    )

    private fun resolveDisplayTitle(
        video: YoutubeVideo,
        metadata: VideoMetadata?,
    ): String = metadata?.manualTitle?.takeIf { it.isNotBlank() } ?: video.title

    private fun resolveThumbnailUrl(
        video: YoutubeVideo,
        metadata: VideoMetadata?,
    ): String = metadata?.manualThumbnailUrl?.takeIf { it.isNotBlank() }
        ?: video.thumbnailUrl
        ?: ""

    private fun resolveDisplayDate(
        publishedAt: OffsetDateTime,
        metadata: VideoMetadata?,
    ): String = metadata?.manualPublishedDate?.toString() ?: publishedAt.toLocalDate().format(DATE_FORMATTER)

    private fun resolveContentKind(
        video: YoutubeVideo,
        metadata: VideoMetadata?,
    ): ContentKind = metadata?.manualKind ?: video.detectedKind

    private fun <K, V> Iterable<V>.associateByNotNull(keySelector: (V) -> K?): Map<K, V> {
        val map = linkedMapOf<K, V>()
        for (element in this) {
            val key = keySelector(element) ?: continue
            map[key] = element
        }
        return map
    }

    companion object {
        private const val HOME_SECTION_SIZE = 12
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
