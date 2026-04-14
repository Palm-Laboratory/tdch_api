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
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

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
}
