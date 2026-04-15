package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.PlaylistVideo
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import kr.or.thejejachurch.api.media.domain.YoutubeVideo
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.PlaylistVideoRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.VideoMetadataRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeSyncJobItemRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeSyncJobRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeVideoRepository
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubeApiOperations
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubePlaylistItemsPage
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubePlaylistItem
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubeVideoResource
import kr.or.thejejachurch.api.support.NoOpPlatformTransactionManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime
import java.util.Optional

class YoutubeSyncVisibilityPolicyTest {

    private val contentMenuRepository: ContentMenuRepository = mock()
    private val youtubePlaylistRepository: YoutubePlaylistRepository = mock()
    private val youtubeVideoRepository: YoutubeVideoRepository = mock()
    private val playlistVideoRepository: PlaylistVideoRepository = mock()
    private val videoMetadataRepository: VideoMetadataRepository = mock()
    private val youtubeApiClient: YoutubeApiOperations = mock()
    private val youtubeSyncJobRepository: YoutubeSyncJobRepository = mock()
    private val youtubeSyncJobItemRepository: YoutubeSyncJobItemRepository = mock()

    private fun newService(): YoutubeSyncService =
        YoutubeSyncService(
            contentMenuRepository = contentMenuRepository,
            youtubePlaylistRepository = youtubePlaylistRepository,
            youtubeVideoRepository = youtubeVideoRepository,
            playlistVideoRepository = playlistVideoRepository,
            videoMetadataRepository = videoMetadataRepository,
            youtubeApiClient = youtubeApiClient,
            youtubeSyncJobRepository = youtubeSyncJobRepository,
            youtubeSyncJobItemRepository = youtubeSyncJobItemRepository,
            transactionManager = NoOpPlatformTransactionManager(),
        )

    private fun stubPlaylistRow(videoDbId: Long = 31L): PlaylistVideo =
        PlaylistVideo(
            id = 41L,
            youtubePlaylistId = 21L,
            youtubeVideoId = videoDbId,
            position = 1,
            isActive = true,
            addedToPlaylistAt = OffsetDateTime.parse("2026-04-15T00:00:00Z"),
            createdAt = OffsetDateTime.parse("2026-04-15T00:00:00Z"),
            updatedAt = OffsetDateTime.parse("2026-04-15T00:00:00Z"),
        )

    private fun stubPlaylist(
        playlistId: Long = 21L,
        youtubePlaylistId: String = "PL_MESSAGES",
    ): YoutubePlaylist =
        YoutubePlaylist(
            id = playlistId,
            contentMenuId = 11L,
            youtubePlaylistId = youtubePlaylistId,
            title = "말씀/설교",
            syncEnabled = true,
        )

    private fun stubMenu(): ContentMenu =
        ContentMenu(
            id = 11L,
            siteKey = "messages",
            menuName = "말씀/설교",
            slug = "messages",
            contentKind = ContentKind.LONG_FORM,
            active = true,
        )

    private fun stubVideo(
        videoId: String,
        privacyStatus: String? = "public",
        uploadStatus: String? = "processed",
        embeddable: Boolean = true,
    ): YoutubeVideoResource =
        YoutubeVideoResource(
            videoId = videoId,
            title = "설교 제목",
            description = "설교 설명",
            publishedAt = OffsetDateTime.parse("2026-04-15T00:00:00Z"),
            channelId = "CHANNEL_1",
            channelTitle = "TDCH",
            thumbnailUrl = "https://example.com/thumb.jpg",
            durationSeconds = 1200,
            privacyStatus = privacyStatus,
            uploadStatus = uploadStatus,
            embeddable = embeddable,
            madeForKids = false,
            detectedKind = ContentKind.LONG_FORM,
            youtubeWatchUrl = "https://youtu.be/$videoId",
            youtubeEmbedUrl = "https://www.youtube.com/embed/$videoId",
            rawPayload = "{}",
        )

    private fun stubCommonSync(
        videoResource: YoutubeVideoResource,
        playlistRow: PlaylistVideo = stubPlaylistRow(),
    ) {
        whenever(youtubePlaylistRepository.findAllBySyncEnabledTrueOrderByIdAsc()).thenReturn(listOf(stubPlaylist()))
        whenever(contentMenuRepository.findById(11L)).thenReturn(Optional.of(stubMenu()))
        whenever(youtubeApiClient.getPlaylistItems("PL_MESSAGES", null, 50)).thenReturn(
            YoutubePlaylistItemsPage(
                nextPageToken = null,
                items = listOf(
                    YoutubePlaylistItem(
                        videoId = videoResource.videoId,
                        position = 1,
                        addedToPlaylistAt = OffsetDateTime.parse("2026-04-15T00:00:00Z"),
                        videoPublishedAt = OffsetDateTime.parse("2026-04-14T00:00:00Z"),
                    ),
                ),
            ),
        )
        whenever(youtubeApiClient.getVideos(listOf(videoResource.videoId))).thenReturn(listOf(videoResource))
        whenever(youtubeVideoRepository.findAllByYoutubeVideoIdIn(listOf(videoResource.videoId))).thenReturn(
            listOf(
                YoutubeVideo(
                    id = playlistRow.youtubeVideoId,
                    youtubeVideoId = videoResource.videoId,
                    title = "이전 제목",
                    publishedAt = OffsetDateTime.parse("2026-04-14T00:00:00Z"),
                    detectedKind = ContentKind.LONG_FORM,
                    youtubeWatchUrl = "https://youtu.be/${videoResource.videoId}",
                    youtubeEmbedUrl = "https://www.youtube.com/embed/${videoResource.videoId}",
                    privacyStatus = "public",
                    uploadStatus = "processed",
                    embeddable = true,
                ),
            ),
        )
        whenever(playlistVideoRepository.findAllByYoutubePlaylistIdOrderByPositionAsc(21L)).thenReturn(listOf(playlistRow))
        whenever(videoMetadataRepository.findAllByYoutubeVideoIdIn(listOf(playlistRow.youtubeVideoId))).thenReturn(emptyList())
        whenever(youtubePlaylistRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(youtubeVideoRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(playlistVideoRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(videoMetadataRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(youtubeSyncJobRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(youtubeSyncJobItemRepository.save(any())).thenAnswer { it.getArgument(0) }
    }

    @Test
    fun `privacy status other than public does not remain active after sync`() {
        val row = stubPlaylistRow()
        stubCommonSync(videoResource = stubVideo(videoId = "VIDEO_1", privacyStatus = "unlisted"), playlistRow = row)

        newService().syncAllMenus()

        assertThat(row.isActive).isFalse()
    }

    @Test
    fun `upload status other than processed does not remain active after sync`() {
        val row = stubPlaylistRow()
        stubCommonSync(videoResource = stubVideo(videoId = "VIDEO_2", uploadStatus = "uploaded"), playlistRow = row)

        newService().syncAllMenus()

        assertThat(row.isActive).isFalse()
    }

    @Test
    fun `non embeddable video does not remain active after sync`() {
        val row = stubPlaylistRow()
        stubCommonSync(videoResource = stubVideo(videoId = "VIDEO_3", embeddable = false), playlistRow = row)

        newService().syncAllMenus()

        assertThat(row.isActive).isFalse()
    }

    @Test
    fun `eligible video remains active after sync`() {
        val row = stubPlaylistRow()
        stubCommonSync(videoResource = stubVideo(videoId = "VIDEO_4"), playlistRow = row)

        newService().syncAllMenus()

        assertThat(row.isActive).isTrue()
    }
}
