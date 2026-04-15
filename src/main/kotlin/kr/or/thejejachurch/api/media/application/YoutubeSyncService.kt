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
    private val youtubeSyncJobRepository: YoutubeSyncJobRepository,
    private val youtubeSyncJobItemRepository: YoutubeSyncJobItemRepository,
    private val youtubeApiClient: YoutubeApiOperations,
    transactionManager: PlatformTransactionManager,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val transactionTemplate = TransactionTemplate(transactionManager)

    fun syncAllMenus(): YoutubeSyncSummary {
        val playlists = youtubePlaylistRepository.findAllBySyncEnabledTrueOrderByIdAsc()
        if (playlists.isEmpty()) {
            log.info("YouTube sync skipped because no sync-enabled playlists were found.")
            return YoutubeSyncSummary(
                totalPlaylists = 0,
                succeededPlaylists = 0,
                failedPlaylists = 0,
            )
        }

        val job = YoutubeSyncJob.startScheduled(OffsetDateTime.now())
        youtubeSyncJobRepository.save(job)
        var succeeded = 0
        var failed = 0

        playlists.forEach { playlist ->
            try {
                val item = YoutubeSyncJobItem.start(
                    jobId = job.id ?: 0L,
                    contentMenuId = playlist.contentMenuId,
                    youtubePlaylistId = playlist.id,
                    startedAt = OffsetDateTime.now(),
                )
                val processedItems = transactionTemplate.execute {
                    syncPlaylist(playlist)
                } ?: 0
                item.markSucceeded(
                    finishedAt = OffsetDateTime.now(),
                    processedItems = processedItems,
                    insertedVideos = 0,
                    updatedVideos = processedItems,
                    deactivatedPlaylistVideos = 0,
                )
                youtubeSyncJobItemRepository.save(item)
                succeeded += 1
            } catch (ex: Exception) {
                val errorMessage = resolveSyncErrorMessage(ex)
                val item = YoutubeSyncJobItem.start(
                    jobId = job.id ?: 0L,
                    contentMenuId = playlist.contentMenuId,
                    youtubePlaylistId = playlist.id,
                    startedAt = OffsetDateTime.now(),
                )
                playlist.markSyncFailed(OffsetDateTime.now(), errorMessage)
                youtubePlaylistRepository.save(playlist)
                item.markFailed(
                    finishedAt = OffsetDateTime.now(),
                    errorMessage = errorMessage,
                )
                youtubeSyncJobItemRepository.save(item)
                failed += 1
                log.error(
                    "YouTube sync failed for playlistId={} contentMenuId={}: {}",
                    playlist.youtubePlaylistId,
                    playlist.contentMenuId,
                    ex.message,
                    ex,
                )
            }
        }

        job.finish(
            finishedAt = OffsetDateTime.now(),
            totalPlaylists = playlists.size,
            succeededPlaylists = succeeded,
            failedPlaylists = failed,
            errorSummary = if (failed > 0) "$failed playlist failed" else null,
        )
        youtubeSyncJobRepository.save(job)

        return YoutubeSyncSummary(
            totalPlaylists = playlists.size,
            succeededPlaylists = succeeded,
            failedPlaylists = failed,
        )
    }

    private fun syncPlaylist(playlist: YoutubePlaylist): Int {
        val menu = contentMenuRepository.findById(playlist.contentMenuId)
            .orElseThrow { NotFoundException("content_menu not found: id=${playlist.contentMenuId}") }

        val playlistItems = fetchAllPlaylistItems(playlist.youtubePlaylistId)
        val videoResources = youtubeApiClient.getVideos(playlistItems.map(YoutubePlaylistItem::videoId))
        val videosByYoutubeId = upsertVideos(menu, videoResources)
        upsertPlaylistVideos(playlist, playlistItems, videosByYoutubeId)
        ensureVideoMetadataRows(videosByYoutubeId.values.mapNotNull { it.id })
        updatePlaylistSyncState(playlist, playlistItems.size, videosByYoutubeId.values.firstOrNull())
        youtubePlaylistRepository.save(playlist)

        log.info(
            "YouTube sync completed for siteKey={} playlistId={} items={} videos={}",
            menu.siteKey,
            playlist.youtubePlaylistId,
            playlistItems.size,
            videosByYoutubeId.size,
        )

        return playlistItems.size
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
        val existingRowsByVideoId = existingRows.associateBy { it.youtubeVideoId }
        val activeVideoIds = mutableSetOf<Long>()

        // Free current positions first so inserts and reorders do not collide with the unique
        // (youtube_playlist_id, position) constraint while this playlist is being rewritten.
        existingRows.forEachIndexed { index, row ->
            row.position = -(index + 1)
            row.updatedAt = now
        }
        if (existingRows.isNotEmpty()) {
            playlistVideoRepository.flush()
        }

        playlistItems.forEach { item ->
            val video = videosByYoutubeId[item.videoId] ?: return@forEach
            val videoDbId = video.id ?: return@forEach
            val shouldActivateInPlaylist = video.shouldBeActiveInPlaylist()

            val existing = existingRowsByVideoId[videoDbId]
            if (existing == null) {
                playlistVideoRepository.save(
                    PlaylistVideo(
                        youtubePlaylistId = playlistDbId,
                        youtubeVideoId = videoDbId,
                        position = item.position,
                        addedToPlaylistAt = item.addedToPlaylistAt,
                        isActive = shouldActivateInPlaylist,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } else {
                existing.position = item.position
                existing.addedToPlaylistAt = item.addedToPlaylistAt
                existing.isActive = shouldActivateInPlaylist
                existing.updatedAt = now
            }

            if (shouldActivateInPlaylist) {
                activeVideoIds += videoDbId
            }
        }

        existingRows
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
        playlist.lastSyncSucceededAt = now
        playlist.lastSyncFailedAt = null
        playlist.lastSyncErrorMessage = null
        playlist.updatedAt = now
        if (representativeVideo != null) {
            playlist.channelId = representativeVideo.channelId
            playlist.channelTitle = representativeVideo.channelTitle
        }
    }

    private fun YoutubePlaylist.markSyncFailed(
        now: OffsetDateTime,
        errorMessage: String,
    ) {
        lastSyncSucceededAt = null
        lastSyncFailedAt = now
        lastSyncErrorMessage = errorMessage
        updatedAt = now
    }

    private fun YoutubeVideo.shouldBeActiveInPlaylist(): Boolean =
        privacyStatus == "public" &&
            uploadStatus == "processed" &&
            embeddable

    private fun resolveSyncErrorMessage(ex: Exception): String =
        ex.message ?: ex.javaClass.simpleName

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
