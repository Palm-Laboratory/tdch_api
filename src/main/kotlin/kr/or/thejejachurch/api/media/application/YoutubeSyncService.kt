package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.PlaylistVideo
import kr.or.thejejachurch.api.media.domain.VideoMetadata
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import kr.or.thejejachurch.api.media.domain.YoutubeVideo
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.PlaylistVideoRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.VideoMetadataRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeVideoRepository
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubeApiOperations
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubePlaylistItem
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubeVideoResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.OffsetDateTime

@Service
class YoutubeSyncService(
    private val contentMenuRepository: ContentMenuRepository,
    private val youtubePlaylistRepository: YoutubePlaylistRepository,
    private val youtubeVideoRepository: YoutubeVideoRepository,
    private val playlistVideoRepository: PlaylistVideoRepository,
    private val videoMetadataRepository: VideoMetadataRepository,
    private val youtubeApiClient: YoutubeApiOperations,
    transactionManager: PlatformTransactionManager,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val transactionTemplate = TransactionTemplate(transactionManager)

    fun syncAllMenus() {
        val playlists = youtubePlaylistRepository.findAllBySyncEnabledTrueOrderByIdAsc()
        if (playlists.isEmpty()) {
            log.info("YouTube sync skipped because no sync-enabled playlists were found.")
            return
        }

        playlists.forEach { playlist ->
            try {
                transactionTemplate.executeWithoutResult {
                    syncPlaylist(playlist)
                }
            } catch (ex: Exception) {
                log.error(
                    "YouTube sync failed for playlistId={} contentMenuId={}: {}",
                    playlist.youtubePlaylistId,
                    playlist.contentMenuId,
                    ex.message,
                    ex,
                )
            }
        }
    }

    private fun syncPlaylist(playlist: YoutubePlaylist) {
        val menu = contentMenuRepository.findById(playlist.contentMenuId)
            .orElseThrow { NotFoundException("content_menu not found: id=${playlist.contentMenuId}") }

        val playlistItems = fetchAllPlaylistItems(playlist.youtubePlaylistId)
        val videoResources = youtubeApiClient.getVideos(playlistItems.map(YoutubePlaylistItem::videoId))
        val videosByYoutubeId = upsertVideos(menu, videoResources)
        upsertPlaylistVideos(playlist, playlistItems, videosByYoutubeId)
        ensureVideoMetadataRows(videosByYoutubeId.values.mapNotNull { it.id })
        updatePlaylistSyncState(playlist, playlistItems.size, videosByYoutubeId.values.firstOrNull())

        log.info(
            "YouTube sync completed for siteKey={} playlistId={} items={} videos={}",
            menu.siteKey,
            playlist.youtubePlaylistId,
            playlistItems.size,
            videosByYoutubeId.size,
        )
    }

    private fun fetchAllPlaylistItems(playlistId: String): List<YoutubePlaylistItem> {
        val items = mutableListOf<YoutubePlaylistItem>()
        var pageToken: String? = null

        do {
            val page = youtubeApiClient.getPlaylistItems(
                playlistId = playlistId,
                pageToken = pageToken,
            )
            items += page.items
            pageToken = page.nextPageToken
        } while (!pageToken.isNullOrBlank())

        return items
    }

    private fun upsertVideos(
        menu: ContentMenu,
        videoResources: List<YoutubeVideoResource>,
    ): Map<String, YoutubeVideo> {
        if (videoResources.isEmpty()) {
            return emptyMap()
        }

        val now = OffsetDateTime.now()
        val existingVideos = youtubeVideoRepository.findAllByYoutubeVideoIdIn(videoResources.map(YoutubeVideoResource::videoId))
            .associateBy { it.youtubeVideoId }

        val syncedVideos = videoResources.map { resource ->
            val existing = existingVideos[resource.videoId]
            if (existing == null) {
                youtubeVideoRepository.save(resource.toEntity(menu, now))
            } else {
                existing.applyResource(resource, menu, now)
                existing
            }
        }

        return syncedVideos.associateBy { it.youtubeVideoId }
    }

    private fun upsertPlaylistVideos(
        playlist: YoutubePlaylist,
        playlistItems: List<YoutubePlaylistItem>,
        videosByYoutubeId: Map<String, YoutubeVideo>,
    ) {
        val playlistDbId = playlist.id ?: throw IllegalStateException("youtube_playlist.id is null")
        val now = OffsetDateTime.now()
        val existingRows = playlistVideoRepository.findAllByYoutubePlaylistIdOrderByPositionAsc(playlistDbId)
            .associateBy { it.youtubeVideoId }
        val activeVideoIds = mutableSetOf<Long>()

        playlistItems.forEach { item ->
            val video = videosByYoutubeId[item.videoId] ?: return@forEach
            val videoDbId = video.id ?: return@forEach
            activeVideoIds += videoDbId

            val existing = existingRows[videoDbId]
            if (existing == null) {
                playlistVideoRepository.save(
                    PlaylistVideo(
                        youtubePlaylistId = playlistDbId,
                        youtubeVideoId = videoDbId,
                        position = item.position,
                        addedToPlaylistAt = item.addedToPlaylistAt,
                        isActive = true,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } else {
                existing.position = item.position
                existing.addedToPlaylistAt = item.addedToPlaylistAt
                existing.isActive = true
                existing.updatedAt = now
            }
        }

        existingRows.values
            .filter { it.youtubeVideoId !in activeVideoIds }
            .forEach { row ->
                row.isActive = false
                row.updatedAt = now
            }
    }

    private fun ensureVideoMetadataRows(videoIds: List<Long>) {
        if (videoIds.isEmpty()) {
            return
        }

        val now = OffsetDateTime.now()
        val existingMetadataIds = videoMetadataRepository.findAllByYoutubeVideoIdIn(videoIds)
            .map(VideoMetadata::youtubeVideoId)
            .toSet()

        videoIds
            .filterNot(existingMetadataIds::contains)
            .forEach { videoId ->
                videoMetadataRepository.save(
                    VideoMetadata(
                        youtubeVideoId = videoId,
                        tags = emptyArray(),
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
    }

    private fun updatePlaylistSyncState(
        playlist: YoutubePlaylist,
        itemCount: Int,
        representativeVideo: YoutubeVideo?,
    ) {
        val now = OffsetDateTime.now()
        playlist.itemCount = itemCount
        playlist.lastSyncedAt = now
        playlist.updatedAt = now
        if (representativeVideo != null) {
            playlist.channelId = representativeVideo.channelId
            playlist.channelTitle = representativeVideo.channelTitle
        }
    }

    private fun YoutubeVideoResource.toEntity(
        menu: ContentMenu,
        now: OffsetDateTime,
    ): YoutubeVideo = YoutubeVideo(
        youtubeVideoId = videoId,
        title = title,
        description = description,
        publishedAt = publishedAt,
        channelId = channelId,
        channelTitle = channelTitle,
        thumbnailUrl = thumbnailUrl,
        durationSeconds = durationSeconds,
        privacyStatus = privacyStatus,
        uploadStatus = uploadStatus,
        embeddable = embeddable,
        madeForKids = madeForKids,
        detectedKind = resolveDetectedKind(menu, detectedKind),
        youtubeWatchUrl = youtubeWatchUrl,
        youtubeEmbedUrl = youtubeEmbedUrl,
        rawPayload = rawPayload,
        lastSyncedAt = now,
        createdAt = now,
        updatedAt = now,
    )

    private fun YoutubeVideo.applyResource(
        resource: YoutubeVideoResource,
        menu: ContentMenu,
        now: OffsetDateTime,
    ) {
        title = resource.title
        description = resource.description
        publishedAt = resource.publishedAt
        channelId = resource.channelId
        channelTitle = resource.channelTitle
        thumbnailUrl = resource.thumbnailUrl
        durationSeconds = resource.durationSeconds
        privacyStatus = resource.privacyStatus
        uploadStatus = resource.uploadStatus
        embeddable = resource.embeddable
        madeForKids = resource.madeForKids
        detectedKind = resolveDetectedKind(menu, resource.detectedKind)
        youtubeWatchUrl = resource.youtubeWatchUrl
        youtubeEmbedUrl = resource.youtubeEmbedUrl
        rawPayload = resource.rawPayload
        lastSyncedAt = now
        updatedAt = now
    }

    private fun resolveDetectedKind(
        menu: ContentMenu,
        detectedKind: ContentKind,
    ): ContentKind = if (menu.contentKind == ContentKind.SHORT) {
        ContentKind.SHORT
    } else {
        detectedKind
    }
}
