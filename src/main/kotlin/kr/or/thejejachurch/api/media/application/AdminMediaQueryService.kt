package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.PlaylistVideo
import kr.or.thejejachurch.api.media.domain.VideoMetadata
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import kr.or.thejejachurch.api.media.domain.YoutubeSyncJob
import kr.or.thejejachurch.api.media.domain.YoutubeSyncJobItem
import kr.or.thejejachurch.api.media.domain.YoutubeVideo
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.PlaylistVideoRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.VideoMetadataRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeSyncJobItemRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeSyncJobRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeVideoRepository
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPaginationDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDetailDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistListResponse
import kr.or.thejejachurch.api.media.interfaces.dto.AdminSyncJobDetailDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminSyncJobDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminSyncJobItemDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminSyncJobListResponse
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoListResponse
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoMetadataDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Service
class AdminMediaQueryService(
    private val contentMenuRepository: ContentMenuRepository,
    private val youtubePlaylistRepository: YoutubePlaylistRepository,
    private val playlistVideoRepository: PlaylistVideoRepository,
    private val youtubeVideoRepository: YoutubeVideoRepository,
    private val videoMetadataRepository: VideoMetadataRepository,
    private val youtubeSyncJobRepository: YoutubeSyncJobRepository,
    private val youtubeSyncJobItemRepository: YoutubeSyncJobItemRepository,
) {

    @Transactional(readOnly = true)
    fun getPlaylists(
        kind: String?,
        search: String?,
        page: Int,
        size: Int,
        sort: String?,
        order: String?,
    ): AdminPlaylistListResponse {
        val normalizedPage = page.coerceAtLeast(1)
        val normalizedSize = size.coerceAtLeast(1)
        val normalizedKind = kind?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedSearch = search?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

        val playlists = contentMenuRepository.findAll()
            .map { menu ->
                val playlist = menu.id?.let(youtubePlaylistRepository::findByContentMenuId)
                menu.toAdminPlaylistDto(playlist)
            }
            .filter { item ->
                (normalizedKind == null || item.contentKind == normalizedKind) &&
                    (normalizedSearch == null || item.matchesSearch(normalizedSearch))
            }
            .sortedWith(resolvePlaylistComparator(sort, order))

        return AdminPlaylistListResponse(
            data = paginate(playlists, normalizedPage, normalizedSize),
            pagination = playlists.toPagination(normalizedPage, normalizedSize),
        )
    }

    @Transactional(readOnly = true)
    fun getPlaylist(siteKey: String): AdminPlaylistDetailDto {
        val menu = getMenu(siteKey)
        val playlist = menu.id?.let(youtubePlaylistRepository::findByContentMenuId)
        return menu.toAdminPlaylistDetailDto(playlist)
    }

    @Transactional(readOnly = true)
    fun getPlaylistVideos(
        siteKey: String,
        visible: String?,
        featured: String?,
        search: String?,
        page: Int,
        size: Int,
    ): AdminVideoListResponse {
        val normalizedPage = page.coerceAtLeast(1)
        val normalizedSize = size.coerceAtLeast(1)
        val normalizedSearch = search?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val menu = getMenu(siteKey)
        val playlist = menu.id?.let(youtubePlaylistRepository::findByContentMenuId)

        val videos = if (playlist?.id == null) {
            emptyList()
        } else {
            val playlistVideos = playlistVideoRepository.findAllByYoutubePlaylistIdAndIsActiveTrueOrderByPositionAsc(playlist.id)
            val videoMap = loadVideoMap(playlistVideos)
            val metadataMap = loadMetadataMap(videoMap.values.mapNotNull { it.id })

            playlistVideos.mapNotNull { playlistVideo ->
                val video = videoMap[playlistVideo.youtubeVideoId] ?: return@mapNotNull null
                val metadata = video.id?.let(metadataMap::get)
                playlistVideo.toAdminVideoDto(video, metadata)
            }
                .filter { item ->
                    (visible == null || item.visible.toString() == visible) &&
                        (featured == null || item.featured.toString() == featured) &&
                        (normalizedSearch == null || item.matchesSearch(normalizedSearch))
                }
        }

        return AdminVideoListResponse(
            data = paginate(videos, normalizedPage, normalizedSize),
            pagination = videos.toPagination(normalizedPage, normalizedSize),
        )
    }

    @Transactional(readOnly = true)
    fun getVideoMetadata(youtubeVideoId: String): AdminVideoMetadataDto {
        val video = youtubeVideoRepository.findByYoutubeVideoId(youtubeVideoId)
            ?: throw NotFoundException("Unknown youtubeVideoId: $youtubeVideoId")
        val metadata = video.id?.let(videoMetadataRepository::findByYoutubeVideoId)

        return AdminVideoMetadataDto(
            youtubeVideoId = video.youtubeVideoId,
            originalTitle = video.title,
            originalDescription = video.description.orEmpty(),
            publishedAt = video.publishedAt.toString(),
            watchUrl = video.youtubeWatchUrl,
            embedUrl = video.youtubeEmbedUrl,
            lastSyncedAt = video.lastSyncedAt.toString(),
            visible = metadata?.visible ?: true,
            featured = metadata?.featured ?: false,
            pinnedRank = metadata?.pinnedRank,
            manualTitle = metadata?.manualTitle,
            manualThumbnailUrl = metadata?.manualThumbnailUrl,
            manualPublishedDate = metadata?.manualPublishedDate?.toString(),
            manualKind = metadata?.manualKind?.name,
            preacher = metadata?.preacher,
            scripture = metadata?.scripture,
            scriptureBody = metadata?.scriptureBody,
            serviceType = metadata?.serviceType,
            summary = metadata?.summary,
            tags = metadata?.tags?.toList().orEmpty(),
        )
    }

    @Transactional(readOnly = true)
    fun getSyncJobs(): AdminSyncJobListResponse {
        val jobs = youtubeSyncJobRepository.findTop20ByOrderByStartedAtDesc()
        val itemCountsByJobId = jobs.associate { job ->
            val items = job.id?.let(youtubeSyncJobItemRepository::findAllByJobIdOrderByIdAsc).orEmpty()
            val failedItemCount = items.count { it.errorMessage != null || it.status.name == "FAILED" }
            (job.id ?: 0L) to (items.size to failedItemCount)
        }

        return AdminSyncJobListResponse(
            data = jobs.map { job ->
                val counts = itemCountsByJobId[job.id ?: 0L] ?: (0 to 0)
                job.toAdminSyncJobDto(
                    itemCount = counts.first,
                    failedItemCount = counts.second,
                )
            },
        )
    }

    @Transactional(readOnly = true)
    fun getSyncJob(jobId: Long): AdminSyncJobDetailDto {
        val job = youtubeSyncJobRepository.findById(jobId)
            .orElseThrow { NotFoundException("Unknown syncJobId: $jobId") }
        val items = youtubeSyncJobItemRepository.findAllByJobIdOrderByIdAsc(jobId)

        val menuIds = items.mapNotNull { it.contentMenuId }.distinct()
        val playlistIds = items.mapNotNull { it.youtubePlaylistId }.distinct()
        val menusById = contentMenuRepository.findAllById(menuIds).associateBy { it.id }
        val playlistsById = youtubePlaylistRepository.findAllById(playlistIds).associateBy { it.id }

        return AdminSyncJobDetailDto(
            id = job.id ?: throw IllegalStateException("sync job id is missing"),
            triggerType = job.triggerType.name,
            status = job.status.name,
            startedAt = job.startedAt.toString(),
            finishedAt = job.finishedAt?.toString(),
            totalPlaylists = job.totalPlaylists,
            succeededPlaylists = job.succeededPlaylists,
            failedPlaylists = job.failedPlaylists,
            errorSummary = job.errorSummary,
            items = items.map { item ->
                item.toAdminSyncJobItemDto(
                    menu = item.contentMenuId?.let(menusById::get),
                    playlist = item.youtubePlaylistId?.let(playlistsById::get),
                )
            },
        )
    }

    private fun getMenu(siteKey: String): ContentMenu =
        contentMenuRepository.findBySiteKey(siteKey)
            ?: throw NotFoundException("Unknown siteKey: $siteKey")

    private fun loadVideoMap(playlistVideos: List<PlaylistVideo>): Map<Long, YoutubeVideo> {
        val videoIds = playlistVideos.map { it.youtubeVideoId }
        if (videoIds.isEmpty()) {
            return emptyMap()
        }

        return youtubeVideoRepository.findAllById(videoIds).associateBy { video ->
            video.id ?: throw IllegalStateException("youtube video id is missing")
        }
    }

    private fun loadMetadataMap(videoIds: List<Long>): Map<Long, VideoMetadata> {
        if (videoIds.isEmpty()) {
            return emptyMap()
        }

        return videoMetadataRepository.findAllByYoutubeVideoIdIn(videoIds).associateBy { it.youtubeVideoId }
    }

    private fun resolvePlaylistComparator(sort: String?, order: String?): Comparator<AdminPlaylistDto> {
        val descending = order?.equals("desc", ignoreCase = true) == true
        val comparator = when (sort?.trim()) {
            "itemCount" -> compareBy<AdminPlaylistDto> { it.itemCount }
            "lastSyncedAt" -> compareBy { it.lastSyncedAt ?: "" }
            "siteKey" -> compareBy { it.siteKey }
            else -> compareBy { it.menuName }
        }

        return if (descending) comparator.reversed() else comparator
    }

    private fun ContentMenu.toAdminPlaylistDto(playlist: YoutubePlaylist?): AdminPlaylistDto =
        AdminPlaylistDto(
            id = id ?: throw IllegalStateException("content menu id is missing"),
            menuName = menuName,
            siteKey = siteKey,
            slug = slug,
            contentKind = contentKind.name,
            status = status.name,
            active = active,
            navigationVisible = navigationVisible,
            sortOrder = sortOrder,
            description = description,
            discoveredAt = discoveredAt?.toString(),
            publishedAt = publishedAt?.toString(),
            lastModifiedBy = lastModifiedBy,
            youtubePlaylistId = playlist?.youtubePlaylistId.orEmpty(),
            itemCount = playlist?.id?.let { playlistVideoRepository.findAllByYoutubePlaylistIdAndIsActiveTrueOrderByPositionAsc(it).size }
                ?: 0,
            syncEnabled = playlist?.syncEnabled ?: false,
            lastSyncedAt = playlist?.lastSyncedAt?.toString(),
        )

    private fun ContentMenu.toAdminPlaylistDetailDto(playlist: YoutubePlaylist?): AdminPlaylistDetailDto =
        AdminPlaylistDetailDto(
            id = id ?: throw IllegalStateException("content menu id is missing"),
            menuName = menuName,
            siteKey = siteKey,
            slug = slug,
            contentKind = contentKind.name,
            status = status.name,
            active = active,
            navigationVisible = navigationVisible,
            sortOrder = sortOrder,
            description = description,
            discoveredAt = discoveredAt?.toString(),
            publishedAt = publishedAt?.toString(),
            lastModifiedBy = lastModifiedBy,
            youtubePlaylistId = playlist?.youtubePlaylistId.orEmpty(),
            youtubeTitle = playlist?.title.orEmpty(),
            youtubeDescription = playlist?.description.orEmpty(),
            channelTitle = playlist?.channelTitle.orEmpty(),
            thumbnailUrl = playlist?.thumbnailUrl.orEmpty(),
            itemCount = playlist?.id?.let { playlistVideoRepository.findAllByYoutubePlaylistIdAndIsActiveTrueOrderByPositionAsc(it).size }
                ?: 0,
            syncEnabled = playlist?.syncEnabled ?: false,
            lastSyncedAt = playlist?.lastSyncedAt?.toString(),
        )

    private fun PlaylistVideo.toAdminVideoDto(video: YoutubeVideo, metadata: VideoMetadata?): AdminVideoDto =
        AdminVideoDto(
            youtubeVideoId = video.youtubeVideoId,
            position = position + 1,
            visible = metadata?.visible ?: true,
            featured = metadata?.featured ?: false,
            displayTitle = metadata?.manualTitle?.takeIf { it.isNotBlank() } ?: video.title,
            displayThumbnailUrl = metadata?.manualThumbnailUrl?.takeIf { it.isNotBlank() } ?: video.thumbnailUrl.orEmpty(),
            displayPublishedDate = metadata?.manualPublishedDate?.format(DATE_FORMATTER)
                ?: video.publishedAt.toLocalDate().format(DATE_FORMATTER),
            originalTitle = video.title,
            publishedAt = video.publishedAt.toString(),
            thumbnailUrl = video.thumbnailUrl.orEmpty(),
            preacher = metadata?.preacher,
            scripture = metadata?.scripture,
            pinnedRank = metadata?.pinnedRank,
        )

    private fun AdminPlaylistDto.matchesSearch(search: String): Boolean =
        menuName.lowercase().contains(search) ||
            siteKey.lowercase().contains(search) ||
            slug.lowercase().contains(search) ||
            youtubePlaylistId.lowercase().contains(search)

    private fun AdminVideoDto.matchesSearch(search: String): Boolean =
        displayTitle.lowercase().contains(search) ||
            originalTitle.lowercase().contains(search) ||
            preacher?.lowercase()?.contains(search) == true ||
            scripture?.lowercase()?.contains(search) == true

    private fun YoutubeSyncJob.toAdminSyncJobDto(
        itemCount: Int,
        failedItemCount: Int,
    ): AdminSyncJobDto = AdminSyncJobDto(
        id = id ?: throw IllegalStateException("sync job id is missing"),
        triggerType = triggerType.name,
        status = status.name,
        startedAt = startedAt.toString(),
        finishedAt = finishedAt?.toString(),
        totalPlaylists = totalPlaylists,
        succeededPlaylists = succeededPlaylists,
        failedPlaylists = failedPlaylists,
        itemCount = itemCount,
        failedItemCount = failedItemCount,
        errorSummary = errorSummary,
    )

    private fun YoutubeSyncJobItem.toAdminSyncJobItemDto(
        menu: ContentMenu?,
        playlist: YoutubePlaylist?,
    ): AdminSyncJobItemDto = AdminSyncJobItemDto(
        id = id ?: throw IllegalStateException("sync job item id is missing"),
        status = status.name,
        siteKey = menu?.siteKey,
        menuName = menu?.menuName,
        youtubePlaylistId = playlist?.youtubePlaylistId,
        processedItems = processedItems,
        insertedVideos = insertedVideos,
        updatedVideos = updatedVideos,
        deactivatedPlaylistVideos = deactivatedPlaylistVideos,
        errorMessage = errorMessage,
        startedAt = startedAt.toString(),
        finishedAt = finishedAt?.toString(),
    )

    private fun <T> paginate(items: List<T>, page: Int, size: Int): List<T> {
        val fromIndex = ((page - 1) * size).coerceAtMost(items.size)
        val toIndex = (fromIndex + size).coerceAtMost(items.size)
        return items.subList(fromIndex, toIndex)
    }

    private fun List<*>.toPagination(page: Int, size: Int): AdminPaginationDto =
        AdminPaginationDto(
            page = page,
            size = size,
            totalElements = size.toLong().let { this.size.toLong() },
            totalPages = if (isEmpty()) 0 else ceil(size.toDouble().let { this.size / it }).toInt(),
        )

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
