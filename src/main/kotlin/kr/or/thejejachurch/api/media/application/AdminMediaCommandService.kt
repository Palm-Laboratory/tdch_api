package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.common.config.YoutubeProperties
import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.ContentMenuStatus
import kr.or.thejejachurch.api.media.domain.VideoMetadata
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.VideoMetadataRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeVideoRepository
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubeApiOperations
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubeChannelPlaylistResource
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDetailDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDiscoveryItemDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDiscoveryResponse
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoMetadataDto
import kr.or.thejejachurch.api.media.interfaces.dto.CreatePlaylistRequest
import kr.or.thejejachurch.api.media.interfaces.dto.DiscoverPlaylistsRequest
import kr.or.thejejachurch.api.media.interfaces.dto.UpdatePlaylistRequest
import kr.or.thejejachurch.api.media.interfaces.dto.UpdateVideoMetadataRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime
import java.text.Normalizer

private const val DISCOVERY_SOURCE_MANUAL = "MANUAL"

@Service
class AdminMediaCommandService(
    private val contentMenuRepository: ContentMenuRepository,
    private val youtubePlaylistRepository: YoutubePlaylistRepository,
    private val youtubeVideoRepository: YoutubeVideoRepository,
    private val videoMetadataRepository: VideoMetadataRepository,
    private val adminMediaQueryService: AdminMediaQueryService,
    private val youtubeApiOperations: YoutubeApiOperations,
    private val youtubeProperties: YoutubeProperties = YoutubeProperties(),
) {

    @Transactional
    fun discoverPlaylists(
        actorId: Long,
        request: DiscoverPlaylistsRequest? = null,
    ): AdminPlaylistDiscoveryResponse {
        val channelId = request?.channelId.normalizedOrNull()
            ?: youtubeProperties.channelId.normalizedOrNull()
            ?: throw IllegalStateException("YOUTUBE_CHANNEL_ID is not configured.")

        val existingMenus = contentMenuRepository.findAll()
        val reservedSiteKeys = existingMenus.mapTo(mutableSetOf()) { it.siteKey }
        val reservedSlugs = existingMenus.mapTo(mutableSetOf()) { it.slug }
        var nextSortOrder = (existingMenus.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val discoveredItems = mutableListOf<AdminPlaylistDiscoveryItemDto>()
        var skippedCount = 0
        var pageToken: String? = null

        do {
            val page = youtubeApiOperations.getChannelPlaylists(channelId, pageToken, 50)

            page.items.forEach { playlist ->
                if (youtubePlaylistRepository.findByYoutubePlaylistId(playlist.youtubePlaylistId) != null) {
                    skippedCount += 1
                    return@forEach
                }

                val fallbackSuffix = playlist.youtubePlaylistId.takeLast(7).lowercase()
                val siteKey = allocateUniqueIdentifier(
                    baseValue = slugify(playlist.title).ifBlank { "playlist-$fallbackSuffix" },
                    reserved = reservedSiteKeys,
                )
                val slug = allocateUniqueIdentifier(
                    baseValue = slugify(playlist.title).ifBlank { "playlist-$fallbackSuffix" },
                    reserved = reservedSlugs,
                )
                val now = OffsetDateTime.now()
                val menuName = playlist.title.ifBlank { "새 재생목록" }
                val contentKind = inferContentKind(menuName, playlist.description)

                val menu = contentMenuRepository.save(
                    ContentMenu(
                        siteKey = siteKey,
                        menuName = menuName,
                        slug = slug,
                        contentKind = contentKind,
                        status = ContentMenuStatus.DRAFT,
                        active = false,
                        navigationVisible = false,
                        sortOrder = nextSortOrder++,
                        description = playlist.description.normalizedOrNull(),
                        discoveredAt = now,
                        lastModifiedBy = actorId,
                    ),
                )

                youtubePlaylistRepository.save(
                    YoutubePlaylist(
                        contentMenuId = menu.id ?: throw IllegalStateException("content menu id is missing"),
                        youtubePlaylistId = playlist.youtubePlaylistId,
                        title = menuName,
                        description = playlist.description.normalizedOrNull(),
                        channelId = playlist.channelId,
                        channelTitle = playlist.channelTitle,
                        thumbnailUrl = playlist.thumbnailUrl,
                        itemCount = playlist.itemCount,
                        syncEnabled = false,
                        lastDiscoveredAt = now,
                        discoverySource = DISCOVERY_SOURCE_MANUAL,
                    ),
                )

                discoveredItems += playlist.toDiscoveryItemDto(
                    siteKey = siteKey,
                    menuName = menuName,
                    slug = slug,
                    contentKind = contentKind,
                )
            }

            pageToken = page.nextPageToken
        } while (pageToken != null)

        return AdminPlaylistDiscoveryResponse(
            discoveredCount = discoveredItems.size,
            skippedCount = skippedCount,
            items = discoveredItems,
        )
    }

    @Transactional
    fun createPlaylist(
        request: CreatePlaylistRequest,
    ): AdminPlaylistDetailDto {
        val siteKey = request.siteKey.trim().lowercase()
        val menuName = request.menuName.trim()
        val slug = request.slug.trim().lowercase()
        val youtubePlaylistId = request.youtubePlaylistId.trim()

        require(siteKey.isNotBlank()) { "siteKey must not be blank" }
        require(menuName.isNotBlank()) { "menuName must not be blank" }
        require(slug.isNotBlank()) { "slug must not be blank" }
        require(youtubePlaylistId.isNotBlank()) { "youtubePlaylistId must not be blank" }

        require(contentMenuRepository.findBySiteKey(siteKey) == null) { "이미 사용 중인 siteKey입니다." }
        require(contentMenuRepository.findBySlug(slug) == null) { "이미 사용 중인 slug입니다." }
        require(youtubePlaylistRepository.findByYoutubePlaylistId(youtubePlaylistId) == null) { "이미 연결된 youtubePlaylistId입니다." }

        val now = OffsetDateTime.now()
        val menu = contentMenuRepository.save(
            ContentMenu(
                siteKey = siteKey,
                menuName = menuName,
                slug = slug,
                contentKind = ContentKind.valueOf(request.contentKind.trim().uppercase()),
                status = ContentMenuStatus.valueOf(request.status.trim().uppercase()),
                active = request.active,
                navigationVisible = request.navigationVisible,
                sortOrder = request.sortOrder,
                description = request.description.normalizedOrNull(),
                discoveredAt = now,
                publishedAt = if (request.status.equals("PUBLISHED", ignoreCase = true)) now else null,
            ),
        )

        youtubePlaylistRepository.save(
            YoutubePlaylist(
                contentMenuId = menu.id ?: throw IllegalStateException("content menu id is missing"),
                youtubePlaylistId = youtubePlaylistId,
                title = menuName,
                syncEnabled = request.syncEnabled,
            ),
        )

        return adminMediaQueryService.getPlaylist(siteKey)
    }

    @Transactional
    fun updatePlaylist(
        actorId: Long,
        siteKey: String,
        request: UpdatePlaylistRequest,
    ): AdminPlaylistDetailDto {
        val menu = contentMenuRepository.findBySiteKey(siteKey)
            ?: throw NotFoundException("Unknown siteKey: $siteKey")

        val menuName = request.menuName.trim()
        val slug = request.slug.trim().lowercase()
        val contentKind = ContentKind.valueOf(request.contentKind.trim().uppercase())

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
        menu.contentKind = contentKind
        menu.status = ContentMenuStatus.valueOf(request.status.trim().uppercase())
        menu.active = request.active
        menu.navigationVisible = request.navigationVisible
        menu.sortOrder = request.sortOrder
        menu.description = request.description.normalizedOrNull()
        menu.lastModifiedBy = actorId
        if (menu.status == ContentMenuStatus.PUBLISHED && menu.publishedAt == null) {
            menu.publishedAt = OffsetDateTime.now()
        }
        contentMenuRepository.save(menu)

        menu.id?.let(youtubePlaylistRepository::findByContentMenuId)?.let { playlist ->
            val requestedYoutubePlaylistId = request.youtubePlaylistId?.trim()?.takeIf { it.isNotEmpty() }
            if (requestedYoutubePlaylistId != null && requestedYoutubePlaylistId != playlist.youtubePlaylistId) {
                val existing = youtubePlaylistRepository.findByYoutubePlaylistId(requestedYoutubePlaylistId)
                if (existing != null && existing.id != playlist.id) {
                    throw IllegalArgumentException("이미 연결된 youtubePlaylistId입니다.")
                }
                playlist.youtubePlaylistId = requestedYoutubePlaylistId
            }
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

    private fun inferContentKind(title: String, description: String?): ContentKind {
        val normalized = listOf(title, description.orEmpty()).joinToString(" ").lowercase()
        return if (
            normalized.contains("shorts") ||
            normalized.contains("short") ||
            normalized.contains("쇼츠")
        ) {
            ContentKind.SHORT
        } else {
            ContentKind.LONG_FORM
        }
    }

    private fun slugify(value: String): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

        return normalized
    }

    private fun allocateUniqueIdentifier(
        baseValue: String,
        reserved: MutableSet<String>,
    ): String {
        var candidate = baseValue
        var suffix = 2
        while (reserved.contains(candidate)) {
            candidate = "$baseValue-$suffix"
            suffix += 1
        }
        reserved += candidate
        return candidate
    }

    private fun YoutubeChannelPlaylistResource.toDiscoveryItemDto(
        siteKey: String,
        menuName: String,
        slug: String,
        contentKind: ContentKind,
    ): AdminPlaylistDiscoveryItemDto = AdminPlaylistDiscoveryItemDto(
        siteKey = siteKey,
        menuName = menuName,
        slug = slug,
        contentKind = contentKind.name,
        status = ContentMenuStatus.DRAFT.name,
        navigationVisible = false,
        youtubePlaylistId = youtubePlaylistId,
        youtubeTitle = menuName,
        channelTitle = channelTitle,
        itemCount = itemCount ?: 0,
        syncEnabled = false,
    )
}
