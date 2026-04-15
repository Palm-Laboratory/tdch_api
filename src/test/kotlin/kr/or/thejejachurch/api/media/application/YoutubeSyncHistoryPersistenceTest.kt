package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubePlaylistItemsPage
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.PlaylistVideoRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.VideoMetadataRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeSyncJobItemRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeSyncJobRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeVideoRepository
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubeApiOperations
import kr.or.thejejachurch.api.support.NoOpPlatformTransactionManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import java.time.OffsetDateTime

class YoutubeSyncHistoryPersistenceTest {

    private val contentMenuRepository: ContentMenuRepository = mock()
    private val youtubePlaylistRepository: YoutubePlaylistRepository = mock()
    private val youtubeVideoRepository: YoutubeVideoRepository = mock()
    private val playlistVideoRepository: PlaylistVideoRepository = mock()
    private val videoMetadataRepository: VideoMetadataRepository = mock()
    private val youtubeApiClient: YoutubeApiOperations = mock()
    private val youtubeSyncJobRepository: YoutubeSyncJobRepository = mock()
    private val youtubeSyncJobItemRepository: YoutubeSyncJobItemRepository = mock()

    @Test
    fun `syncAllMenus should persist sync job history on success`() {
        val service = YoutubeSyncService(
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

        whenever(youtubePlaylistRepository.findAllBySyncEnabledTrueOrderByIdAsc()).thenReturn(
            listOf(
                YoutubePlaylist(
                    id = 10L,
                    contentMenuId = 1L,
                    youtubePlaylistId = "PL_MESSAGES",
                    title = "말씀/설교",
                    syncEnabled = true,
                ),
            ),
        )
        whenever(contentMenuRepository.findById(1L)).thenReturn(
            Optional.of(
                ContentMenu(
                    id = 1L,
                    siteKey = "messages",
                    menuName = "말씀/설교",
                    slug = "messages",
                    contentKind = ContentKind.LONG_FORM,
                    active = true,
                ),
            ),
        )
        whenever(youtubeApiClient.getPlaylistItems("PL_MESSAGES", null, 50)).thenReturn(
            YoutubePlaylistItemsPage(
                nextPageToken = null,
                items = emptyList(),
            ),
        )
        whenever(youtubeApiClient.getVideos(emptyList())).thenReturn(emptyList())
        whenever(youtubeSyncJobRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(youtubeSyncJobItemRepository.save(any())).thenAnswer { it.getArgument(0) }

        service.syncAllMenus()

        verify(youtubeSyncJobRepository, times(2)).save(any())
        verify(youtubeSyncJobItemRepository, times(1)).save(any())
    }

    @Test
    fun `syncAllMenus should persist failed sync job history when playlist sync fails`() {
        val service = YoutubeSyncService(
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

        whenever(youtubePlaylistRepository.findAllBySyncEnabledTrueOrderByIdAsc()).thenReturn(
            listOf(
                YoutubePlaylist(
                    id = 10L,
                    contentMenuId = 1L,
                    youtubePlaylistId = "PL_MESSAGES",
                    title = "말씀/설교",
                    syncEnabled = true,
                ),
            ),
        )
        whenever(contentMenuRepository.findById(1L)).thenReturn(
            Optional.of(
                ContentMenu(
                    id = 1L,
                    siteKey = "messages",
                    menuName = "말씀/설교",
                    slug = "messages",
                    contentKind = ContentKind.LONG_FORM,
                    active = true,
                ),
            ),
        )
        whenever(youtubeApiClient.getPlaylistItems("PL_MESSAGES", null, 50)).thenThrow(RuntimeException("boom"))
        whenever(youtubeSyncJobRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(youtubeSyncJobItemRepository.save(any())).thenAnswer { it.getArgument(0) }

        service.syncAllMenus()

        verify(youtubeSyncJobRepository, times(2)).save(any())
        verify(youtubeSyncJobItemRepository, times(1)).save(any())
    }

    @Test
    fun `syncAllMenus should persist latest sync success summary on playlist`() {
        val service = YoutubeSyncService(
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

        val playlist = YoutubePlaylist(
            id = 11L,
            contentMenuId = 1L,
            youtubePlaylistId = "PL_MESSAGES",
            title = "말씀/설교",
            syncEnabled = true,
            lastSyncedAt = null,
            lastSyncSucceededAt = null,
            lastSyncFailedAt = OffsetDateTime.parse("2026-04-14T00:00:00Z"),
            lastSyncErrorMessage = "quota exceeded",
            lastDiscoveredAt = OffsetDateTime.parse("2026-04-13T00:00:00Z"),
            discoverySource = "MANUAL",
        )

        whenever(youtubePlaylistRepository.findAllBySyncEnabledTrueOrderByIdAsc()).thenReturn(listOf(playlist))
        whenever(contentMenuRepository.findById(1L)).thenReturn(
            Optional.of(
                ContentMenu(
                    id = 1L,
                    siteKey = "messages",
                    menuName = "말씀/설교",
                    slug = "messages",
                    contentKind = ContentKind.LONG_FORM,
                    active = true,
                ),
            ),
        )
        whenever(youtubeApiClient.getPlaylistItems("PL_MESSAGES", null, 50)).thenReturn(
            YoutubePlaylistItemsPage(
                nextPageToken = null,
                items = emptyList(),
            ),
        )
        whenever(youtubeApiClient.getVideos(emptyList())).thenReturn(emptyList())
        whenever(youtubeSyncJobRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(youtubeSyncJobItemRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(youtubePlaylistRepository.save(any())).thenAnswer {
            val saved = it.getArgument<YoutubePlaylist>(0)
            assertThat(saved.lastSyncSucceededAt).isNotNull()
            assertThat(saved.lastSyncFailedAt).isNull()
            assertThat(saved.lastSyncErrorMessage).isNull()
            saved
        }

        service.syncAllMenus()

        verify(youtubePlaylistRepository).save(any())
    }

    @Test
    fun `syncAllMenus should persist latest sync failure summary on playlist`() {
        val service = YoutubeSyncService(
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

        val playlist = YoutubePlaylist(
            id = 12L,
            contentMenuId = 1L,
            youtubePlaylistId = "PL_MESSAGES",
            title = "말씀/설교",
            syncEnabled = true,
            lastSyncedAt = null,
            lastSyncSucceededAt = OffsetDateTime.parse("2026-04-14T00:00:00Z"),
            lastSyncFailedAt = null,
            lastSyncErrorMessage = null,
            lastDiscoveredAt = OffsetDateTime.parse("2026-04-13T00:00:00Z"),
            discoverySource = "MANUAL",
        )

        whenever(youtubePlaylistRepository.findAllBySyncEnabledTrueOrderByIdAsc()).thenReturn(listOf(playlist))
        whenever(contentMenuRepository.findById(1L)).thenReturn(
            Optional.of(
                ContentMenu(
                    id = 1L,
                    siteKey = "messages",
                    menuName = "말씀/설교",
                    slug = "messages",
                    contentKind = ContentKind.LONG_FORM,
                    active = true,
                ),
            ),
        )
        whenever(youtubeApiClient.getPlaylistItems("PL_MESSAGES", null, 50)).thenThrow(RuntimeException("quota exceeded"))
        whenever(youtubeSyncJobRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(youtubeSyncJobItemRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(youtubePlaylistRepository.save(any())).thenAnswer {
            val saved = it.getArgument<YoutubePlaylist>(0)
            assertThat(saved.lastSyncFailedAt).isNotNull()
            assertThat(saved.lastSyncErrorMessage).isEqualTo("quota exceeded")
            saved
        }

        service.syncAllMenus()

        verify(youtubePlaylistRepository).save(any())
    }
}
