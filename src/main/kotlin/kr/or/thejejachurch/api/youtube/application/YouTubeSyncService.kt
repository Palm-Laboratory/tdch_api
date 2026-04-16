package kr.or.thejejachurch.api.youtube.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kr.or.thejejachurch.api.common.config.YouTubeProperties
import kr.or.thejejachurch.api.menu.application.AdminYouTubePlaylistSummary
import kr.or.thejejachurch.api.menu.application.YouTubeSyncSummary
import kr.or.thejejachurch.api.menu.domain.MenuItem
import kr.or.thejejachurch.api.menu.domain.MenuStatus
import kr.or.thejejachurch.api.menu.domain.MenuType
import kr.or.thejejachurch.api.menu.infrastructure.persistence.MenuItemRepository
import kr.or.thejejachurch.api.youtube.domain.YouTubeChannel
import kr.or.thejejachurch.api.youtube.domain.YouTubePlaylist
import kr.or.thejejachurch.api.youtube.domain.YouTubeSyncStatus
import kr.or.thejejachurch.api.youtube.infrastructure.persistence.YouTubeChannelRepository
import kr.or.thejejachurch.api.youtube.infrastructure.persistence.YouTubePlaylistRepository
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class YouTubeSyncService(
    private val youTubeProperties: YouTubeProperties,
    private val youTubeChannelRepository: YouTubeChannelRepository,
    private val youTubePlaylistRepository: YouTubePlaylistRepository,
    private val menuItemRepository: MenuItemRepository,
    private val objectMapper: ObjectMapper,
) {
    private val restClient = RestClient.builder()
        .baseUrl("https://www.googleapis.com/youtube/v3")
        .build()

    @Scheduled(cron = "0 0 8,23 * * *", zone = "Asia/Seoul")
    fun scheduledSync() {
        if (!isConfigured()) {
            return
        }
        sync()
    }

    @Transactional
    fun sync(): YouTubeSyncSummary {
        require(isConfigured()) { "YOUTUBE_API_KEY와 YOUTUBE_CHANNEL_ID를 먼저 설정해 주세요." }

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val playlistPayloads = fetchAllPlaylists()

        val channelTitle = playlistPayloads.firstOrNull()?.channelTitle ?: "The 제자교회"
        val channel = upsertChannel(channelTitle, now)
        val existingPlaylists = youTubePlaylistRepository.findAllByChannelId(channel.id!!)
        val existingByPlaylistId = existingPlaylists.associateBy { it.playlistId }
        val seenPlaylistIds = linkedSetOf<String>()

        var createdMenus = 0
        var updatedMenus = 0
        var archivedMenus = 0
        var restoredMenus = 0

        playlistPayloads.forEach { payload ->
            seenPlaylistIds += payload.playlistId
            val playlist = existingByPlaylistId[payload.playlistId]
            val persistedPlaylist = if (playlist == null) {
                youTubePlaylistRepository.save(
                    YouTubePlaylist(
                        channelId = channel.id,
                        playlistId = payload.playlistId,
                        title = payload.title,
                        description = payload.description,
                        thumbnailUrl = payload.thumbnailUrl,
                        itemCount = payload.itemCount,
                        publishedAt = payload.publishedAt,
                        syncStatus = YouTubeSyncStatus.ACTIVE,
                        lastSyncedAt = now,
                    )
                )
            } else {
                playlist.title = payload.title
                playlist.description = payload.description
                playlist.thumbnailUrl = payload.thumbnailUrl
                playlist.itemCount = payload.itemCount
                playlist.publishedAt = payload.publishedAt
                playlist.syncStatus = YouTubeSyncStatus.ACTIVE
                playlist.lastSyncedAt = now
                youTubePlaylistRepository.save(playlist)
            }

            val menu = menuItemRepository.findAllByOrderBySortOrderAscIdAsc()
                .firstOrNull { it.playlistId == persistedPlaylist.id }

            if (menu == null) {
                menuItemRepository.save(
                    MenuItem(
                        parentId = null,
                        type = MenuType.YOUTUBE_PLAYLIST,
                        status = MenuStatus.DRAFT,
                        label = persistedPlaylist.title,
                        slug = generateUniqueSlug(persistedPlaylist.title),
                        playlistId = persistedPlaylist.id,
                        isAuto = true,
                    )
                )
                createdMenus += 1
            } else {
                if (!menu.labelCustomized && menu.label != persistedPlaylist.title) {
                    menu.label = persistedPlaylist.title
                    updatedMenus += 1
                }
                if (menu.status == MenuStatus.ARCHIVED && persistedPlaylist.syncStatus == YouTubeSyncStatus.ACTIVE) {
                    menu.status = MenuStatus.DRAFT
                    restoredMenus += 1
                }
                menuItemRepository.save(menu)
            }
        }

        existingPlaylists
            .filter { it.playlistId !in seenPlaylistIds }
            .forEach { playlist ->
                if (playlist.syncStatus != YouTubeSyncStatus.REMOVED) {
                    playlist.syncStatus = YouTubeSyncStatus.REMOVED
                    playlist.lastSyncedAt = now
                    youTubePlaylistRepository.save(playlist)
                }

                menuItemRepository.findAllByOrderBySortOrderAscIdAsc()
                    .firstOrNull { it.playlistId == playlist.id }
                    ?.let { menu ->
                        if (menu.status != MenuStatus.ARCHIVED) {
                            menu.status = MenuStatus.ARCHIVED
                            menuItemRepository.save(menu)
                            archivedMenus += 1
                        }
                    }
            }

        channel.lastSyncedAt = now
        youTubeChannelRepository.save(channel)

        recomputeMenuPaths()

        return YouTubeSyncSummary(
            status = "OK",
            totalPlaylists = playlistPayloads.size,
            createdMenus = createdMenus,
            updatedMenus = updatedMenus,
            archivedMenus = archivedMenus,
            restoredMenus = restoredMenus,
            completedAt = now.toString(),
        )
    }

    @Transactional(readOnly = true)
    fun getPlaylistSummaries(): List<AdminYouTubePlaylistSummary> {
        val menus = menuItemRepository.findAllByOrderBySortOrderAscIdAsc()
        val menusById = menus.associateBy { it.id!! }
        val playlistsById = youTubePlaylistRepository.findAll().associateBy { it.id!! }

        return menus
            .filter { it.type == MenuType.YOUTUBE_PLAYLIST && it.playlistId != null }
            .sortedWith(compareBy<MenuItem> { it.status }.thenBy { it.sortOrder }.thenBy { it.id })
            .mapNotNull { menu ->
                val playlist = playlistsById[menu.playlistId] ?: return@mapNotNull null
                val parent = menu.parentId?.let(menusById::get)
                AdminYouTubePlaylistSummary(
                    menuId = menu.id!!,
                    playlistId = playlist.playlistId,
                    menuLabel = menu.label,
                    sourceTitle = playlist.title,
                    slug = menu.slug,
                    status = menu.status,
                    syncStatus = playlist.syncStatus,
                    parentId = parent?.id,
                    parentLabel = parent?.label,
                    thumbnailUrl = playlist.thumbnailUrl,
                    itemCount = playlist.itemCount,
                )
            }
    }

    private fun isConfigured(): Boolean =
        youTubeProperties.apiKey.isNotBlank() && youTubeProperties.channelId.isNotBlank()

    private fun upsertChannel(channelTitle: String, now: OffsetDateTime): YouTubeChannel {
        val existing = youTubeChannelRepository.findByChannelId(youTubeProperties.channelId)
        return if (existing == null) {
            youTubeChannelRepository.save(
                YouTubeChannel(
                    channelId = youTubeProperties.channelId,
                    title = channelTitle,
                    lastSyncedAt = now,
                )
            )
        } else {
            existing.title = channelTitle
            existing.lastSyncedAt = now
            youTubeChannelRepository.save(existing)
        }
    }

    private fun fetchAllPlaylists(): List<PlaylistPayload> {
        val playlists = mutableListOf<PlaylistPayload>()
        var pageToken: String? = null

        do {
            val responseBody = restClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/playlists")
                        .queryParam("part", "snippet,contentDetails")
                        .queryParam("maxResults", "50")
                        .queryParam("channelId", youTubeProperties.channelId)
                        .queryParam("key", youTubeProperties.apiKey)
                        .apply {
                            if (!pageToken.isNullOrBlank()) {
                                queryParam("pageToken", pageToken)
                            }
                        }
                        .build()
                }
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String::class.java)
                ?: throw IllegalStateException("유튜브 API 응답이 비어 있습니다.")

            val json = objectMapper.readTree(responseBody)
            val items = json["items"] ?: objectMapper.createArrayNode()
            items.forEach { item ->
                playlists += item.toPlaylistPayload()
            }
            pageToken = json["nextPageToken"]?.asText()
        } while (!pageToken.isNullOrBlank())

        return playlists
    }

    private fun JsonNode.toPlaylistPayload(): PlaylistPayload {
        val snippet = this["snippet"]
        val contentDetails = this["contentDetails"]
        val thumbnails = snippet["thumbnails"]
        val thumbnailUrl =
            thumbnails["maxres"]?.get("url")?.asText()
                ?: thumbnails["high"]?.get("url")?.asText()
                ?: thumbnails["medium"]?.get("url")?.asText()
                ?: thumbnails["default"]?.get("url")?.asText()

        return PlaylistPayload(
            playlistId = this["id"].asText(),
            title = snippet["title"].asText(),
            description = snippet["description"]?.asText(),
            thumbnailUrl = thumbnailUrl,
            itemCount = contentDetails["itemCount"]?.asInt() ?: 0,
            publishedAt = snippet["publishedAt"]?.asText()?.let(OffsetDateTime::parse),
            channelTitle = snippet["channelTitle"]?.asText() ?: "The 제자교회",
        )
    }

    private fun generateUniqueSlug(label: String): String {
        val baseSlug = label
            .lowercase()
            .replace(Regex("[^a-z0-9가-힣]+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .ifBlank { "playlist" }
        var candidate = baseSlug
        var suffix = 1
        while (menuItemRepository.findBySlug(candidate) != null) {
            suffix += 1
            candidate = "$baseSlug-$suffix"
        }
        return candidate
    }

    private fun recomputeMenuPaths() {
        val items = menuItemRepository.findAllByOrderBySortOrderAscIdAsc()
        val itemsByParent = items.groupBy { it.parentId }

        fun visit(parentId: Long?, parentPath: String, depth: Int) {
            itemsByParent[parentId]
                .orEmpty()
                .sortedWith(compareBy<MenuItem> { it.sortOrder }.thenBy { it.id })
                .forEach { item ->
                    val itemId = item.id ?: return@forEach
                    item.depth = depth
                    item.path = "$parentPath$itemId/"
                    menuItemRepository.save(item)
                    visit(itemId, item.path, depth + 1)
                }
        }

        visit(parentId = null, parentPath = "/", depth = 0)
    }
}

private data class PlaylistPayload(
    val playlistId: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val itemCount: Int,
    val publishedAt: OffsetDateTime?,
    val channelTitle: String,
)
