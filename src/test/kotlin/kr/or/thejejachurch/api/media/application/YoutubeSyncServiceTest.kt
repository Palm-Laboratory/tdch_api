package kr.or.thejejachurch.api.media.application

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
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubePlaylistItemsPage
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubeVideoResource
import kr.or.thejejachurch.api.support.NoOpPlatformTransactionManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime
import java.util.Optional
import java.util.concurrent.atomic.AtomicLong

class YoutubeSyncServiceTest {

    private val contentMenuRepository: ContentMenuRepository = mock()
    private val youtubePlaylistRepository: YoutubePlaylistRepository = mock()
    private val youtubeVideoRepository: YoutubeVideoRepository = mock()
    private val playlistVideoRepository: PlaylistVideoRepository = mock()
    private val videoMetadataRepository: VideoMetadataRepository = mock()
    private val youtubeApiClient: YoutubeApiOperations = mock()

    private val service = YoutubeSyncService(
        contentMenuRepository = contentMenuRepository,
        youtubePlaylistRepository = youtubePlaylistRepository,
        youtubeVideoRepository = youtubeVideoRepository,
        playlistVideoRepository = playlistVideoRepository,
        videoMetadataRepository = videoMetadataRepository,
        youtubeApiClient = youtubeApiClient,
        transactionManager = NoOpPlatformTransactionManager(),
    )

    @Test
    fun `syncAllMenus upserts videos playlist rows and metadata`() {
        val menu = ContentMenu(
            id = 1L,
            siteKey = "its-okay",
            menuName = "그래도 괜찮아",
            slug = "its-okay",
            contentKind = ContentKind.SHORT,
            active = true,
        )
        val playlist = YoutubePlaylist(
            id = 10L,
            contentMenuId = 1L,
            youtubePlaylistId = "PL_SHORTS",
            title = "그래도 괜찮아",
            syncEnabled = true,
        )
        val staleRow = PlaylistVideo(
            id = 500L,
            youtubePlaylistId = 10L,
            youtubeVideoId = 999L,
            position = 99,
            isActive = true,
        )
        val savedVideoId = AtomicLong(100L)

        whenever(youtubePlaylistRepository.findAllBySyncEnabledTrueOrderByIdAsc()).thenReturn(listOf(playlist))
        whenever(contentMenuRepository.findById(1L)).thenReturn(Optional.of(menu))
        whenever(youtubeApiClient.getPlaylistItems("PL_SHORTS", null, 50)).thenReturn(
            YoutubePlaylistItemsPage(
                nextPageToken = null,
                items = listOf(
                    YoutubePlaylistItem(
                        videoId = "video-1",
                        position = 0,
                        addedToPlaylistAt = OffsetDateTime.parse("2026-03-05T00:00:00Z"),
                        videoPublishedAt = OffsetDateTime.parse("2026-03-05T00:00:00Z"),
                    ),
                    YoutubePlaylistItem(
                        videoId = "video-2",
                        position = 1,
                        addedToPlaylistAt = OffsetDateTime.parse("2026-03-06T00:00:00Z"),
                        videoPublishedAt = OffsetDateTime.parse("2026-03-06T00:00:00Z"),
                    ),
                ),
            ),
        )
        whenever(youtubeApiClient.getVideos(listOf("video-1", "video-2"))).thenReturn(
            listOf(
                youtubeVideoResource("video-1", 320),
                youtubeVideoResource("video-2", 45),
            ),
        )
        whenever(youtubeVideoRepository.findAllByYoutubeVideoIdIn(listOf("video-1", "video-2"))).thenReturn(emptyList())
        whenever(youtubeVideoRepository.save(any())).thenAnswer { invocation ->
            val source = invocation.getArgument<YoutubeVideo>(0)
            YoutubeVideo(
                id = savedVideoId.getAndIncrement(),
                youtubeVideoId = source.youtubeVideoId,
                title = source.title,
                description = source.description,
                publishedAt = source.publishedAt,
                channelId = source.channelId,
                channelTitle = source.channelTitle,
                thumbnailUrl = source.thumbnailUrl,
                durationSeconds = source.durationSeconds,
                privacyStatus = source.privacyStatus,
                uploadStatus = source.uploadStatus,
                embeddable = source.embeddable,
                madeForKids = source.madeForKids,
                detectedKind = source.detectedKind,
                youtubeWatchUrl = source.youtubeWatchUrl,
                youtubeEmbedUrl = source.youtubeEmbedUrl,
                rawPayload = source.rawPayload,
                lastSyncedAt = source.lastSyncedAt,
                createdAt = source.createdAt,
                updatedAt = source.updatedAt,
            )
        }
        whenever(playlistVideoRepository.findAllByYoutubePlaylistIdOrderByPositionAsc(10L)).thenReturn(listOf(staleRow))
        whenever(videoMetadataRepository.findAllByYoutubeVideoIdIn(listOf(100L, 101L))).thenReturn(emptyList())
        whenever(videoMetadataRepository.save(any())).thenAnswer { it.getArgument<VideoMetadata>(0) }
        whenever(playlistVideoRepository.save(any())).thenAnswer { it.getArgument<PlaylistVideo>(0) }

        val result = service.syncAllMenus()

        val savedPlaylistRows = argumentCaptor<PlaylistVideo>()
        verify(playlistVideoRepository, times(2)).save(savedPlaylistRows.capture())
        verify(videoMetadataRepository, times(2)).save(any())

        assertThat(savedPlaylistRows.allValues.map { it.youtubeVideoId }).containsExactly(100L, 101L)
        assertThat(savedPlaylistRows.allValues.map { it.position }).containsExactly(0, 1)
        assertThat(staleRow.isActive).isFalse()
        assertThat(playlist.itemCount).isEqualTo(2)
        assertThat(playlist.lastSyncedAt).isNotNull()
        assertThat(playlist.channelId).isEqualTo("channel-video-1")
        assertThat(result.totalPlaylists).isEqualTo(1)
        assertThat(result.succeededPlaylists).isEqualTo(1)
        assertThat(result.failedPlaylists).isZero()
    }

    @Test
    fun `syncAllMenus reorders playlist rows without conflicting on position`() {
        val menu = ContentMenu(
            id = 2L,
            siteKey = "better-devotion",
            menuName = "더 좋은 묵상",
            slug = "better-devotion",
            contentKind = ContentKind.LONG_FORM,
            active = true,
        )
        val playlist = YoutubePlaylist(
            id = 20L,
            contentMenuId = 2L,
            youtubePlaylistId = "PL_DEVOTION",
            title = "더 좋은 묵상",
            syncEnabled = true,
        )
        val existingLeadRow = PlaylistVideo(
            id = 700L,
            youtubePlaylistId = 20L,
            youtubeVideoId = 201L,
            position = 0,
            isActive = true,
        )
        val savedVideoId = AtomicLong(202L)

        whenever(youtubePlaylistRepository.findAllBySyncEnabledTrueOrderByIdAsc()).thenReturn(listOf(playlist))
        whenever(contentMenuRepository.findById(2L)).thenReturn(Optional.of(menu))
        whenever(youtubeApiClient.getPlaylistItems("PL_DEVOTION", null, 50)).thenReturn(
            YoutubePlaylistItemsPage(
                nextPageToken = null,
                items = listOf(
                    YoutubePlaylistItem(
                        videoId = "video-new",
                        position = 0,
                        addedToPlaylistAt = OffsetDateTime.parse("2026-03-10T00:00:00Z"),
                        videoPublishedAt = OffsetDateTime.parse("2026-03-10T00:00:00Z"),
                    ),
                    YoutubePlaylistItem(
                        videoId = "video-old",
                        position = 1,
                        addedToPlaylistAt = OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                        videoPublishedAt = OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                    ),
                ),
            ),
        )
        whenever(youtubeApiClient.getVideos(listOf("video-new", "video-old"))).thenReturn(
            listOf(
                youtubeVideoResource("video-new", 320),
                youtubeVideoResource("video-old", 320),
            ),
        )
        whenever(youtubeVideoRepository.findAllByYoutubeVideoIdIn(listOf("video-new", "video-old"))).thenReturn(
            listOf(
                YoutubeVideo(
                    id = 201L,
                    youtubeVideoId = "video-old",
                    title = "title-video-old",
                    description = "desc-video-old",
                    publishedAt = OffsetDateTime.parse("2026-03-01T02:00:00Z"),
                    channelId = "channel-video-old",
                    channelTitle = "channel-title-video-old",
                    thumbnailUrl = "https://example.com/video-old.jpg",
                    durationSeconds = 320,
                    privacyStatus = "public",
                    uploadStatus = "processed",
                    embeddable = true,
                    madeForKids = false,
                    detectedKind = ContentKind.LONG_FORM,
                    youtubeWatchUrl = "https://youtube.com/watch?v=video-old",
                    youtubeEmbedUrl = "https://youtube.com/embed/video-old",
                    rawPayload = """{"id":"video-old"}""",
                ),
            ),
        )
        whenever(youtubeVideoRepository.save(any())).thenAnswer { invocation ->
            val source = invocation.getArgument<YoutubeVideo>(0)
            YoutubeVideo(
                id = savedVideoId.getAndIncrement(),
                youtubeVideoId = source.youtubeVideoId,
                title = source.title,
                description = source.description,
                publishedAt = source.publishedAt,
                channelId = source.channelId,
                channelTitle = source.channelTitle,
                thumbnailUrl = source.thumbnailUrl,
                durationSeconds = source.durationSeconds,
                privacyStatus = source.privacyStatus,
                uploadStatus = source.uploadStatus,
                embeddable = source.embeddable,
                madeForKids = source.madeForKids,
                detectedKind = source.detectedKind,
                youtubeWatchUrl = source.youtubeWatchUrl,
                youtubeEmbedUrl = source.youtubeEmbedUrl,
                rawPayload = source.rawPayload,
                lastSyncedAt = source.lastSyncedAt,
                createdAt = source.createdAt,
                updatedAt = source.updatedAt,
            )
        }
        whenever(playlistVideoRepository.findAllByYoutubePlaylistIdOrderByPositionAsc(20L)).thenReturn(listOf(existingLeadRow))
        whenever(videoMetadataRepository.findAllByYoutubeVideoIdIn(listOf(202L, 201L))).thenReturn(emptyList())
        whenever(videoMetadataRepository.save(any())).thenAnswer { it.getArgument<VideoMetadata>(0) }
        whenever(playlistVideoRepository.save(any())).thenAnswer { it.getArgument<PlaylistVideo>(0) }

        val result = service.syncAllMenus()

        val savedPlaylistRows = argumentCaptor<PlaylistVideo>()
        verify(playlistVideoRepository, times(1)).save(savedPlaylistRows.capture())
        verify(playlistVideoRepository, times(1)).flush()
        verify(videoMetadataRepository, times(2)).save(any())
        verify(playlistVideoRepository, never()).findByYoutubePlaylistIdAndYoutubeVideoId(any(), any())

        assertThat(existingLeadRow.position).isEqualTo(1)
        assertThat(existingLeadRow.isActive).isTrue()
        assertThat(savedPlaylistRows.firstValue.youtubeVideoId).isEqualTo(202L)
        assertThat(savedPlaylistRows.firstValue.position).isEqualTo(0)
        assertThat(result.totalPlaylists).isEqualTo(1)
        assertThat(result.succeededPlaylists).isEqualTo(1)
        assertThat(result.failedPlaylists).isZero()
    }

    private fun youtubeVideoResource(videoId: String, durationSeconds: Int): YoutubeVideoResource = YoutubeVideoResource(
        videoId = videoId,
        title = "title-$videoId",
        description = "desc-$videoId",
        publishedAt = OffsetDateTime.parse("2026-03-02T02:00:00Z"),
        channelId = "channel-$videoId",
        channelTitle = "channel-title-$videoId",
        thumbnailUrl = "https://example.com/$videoId.jpg",
        durationSeconds = durationSeconds,
        privacyStatus = "public",
        uploadStatus = "processed",
        embeddable = true,
        madeForKids = false,
        detectedKind = if (durationSeconds <= 60) ContentKind.SHORT else ContentKind.LONG_FORM,
        youtubeWatchUrl = "https://youtube.com/watch?v=$videoId",
        youtubeEmbedUrl = "https://youtube.com/embed/$videoId",
        rawPayload = """{"id":"$videoId"}""",
    )
}
