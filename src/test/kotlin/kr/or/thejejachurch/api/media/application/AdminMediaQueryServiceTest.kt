package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.PlaylistVideo
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import kr.or.thejejachurch.api.media.domain.YoutubeSyncJob
import kr.or.thejejachurch.api.media.domain.YoutubeSyncJobItem
import kr.or.thejejachurch.api.media.domain.YoutubeSyncJobItemStatus
import kr.or.thejejachurch.api.media.domain.YoutubeSyncJobStatus
import kr.or.thejejachurch.api.media.domain.YoutubeSyncTriggerType
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.PlaylistVideoRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.VideoMetadataRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeSyncJobItemRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeSyncJobRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeVideoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime

class AdminMediaQueryServiceTest {

    private val contentMenuRepository: ContentMenuRepository = mock()
    private val youtubePlaylistRepository: YoutubePlaylistRepository = mock()
    private val playlistVideoRepository: PlaylistVideoRepository = mock()
    private val youtubeVideoRepository: YoutubeVideoRepository = mock()
    private val videoMetadataRepository: VideoMetadataRepository = mock()
    private val youtubeSyncJobRepository: YoutubeSyncJobRepository = mock()
    private val youtubeSyncJobItemRepository: YoutubeSyncJobItemRepository = mock()

    private val service = AdminMediaQueryService(
        contentMenuRepository = contentMenuRepository,
        youtubePlaylistRepository = youtubePlaylistRepository,
        playlistVideoRepository = playlistVideoRepository,
        youtubeVideoRepository = youtubeVideoRepository,
        videoMetadataRepository = videoMetadataRepository,
        youtubeSyncJobRepository = youtubeSyncJobRepository,
        youtubeSyncJobItemRepository = youtubeSyncJobItemRepository,
    )

    @Test
    fun `get playlists uses actual playlist video count instead of playlist itemCount`() {
        whenever(contentMenuRepository.findAll()).thenReturn(
            listOf(
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
        whenever(youtubePlaylistRepository.findByContentMenuId(1L)).thenReturn(
            YoutubePlaylist(
                id = 10L,
                contentMenuId = 1L,
                youtubePlaylistId = "PL_MESSAGES",
                title = "말씀/설교",
                itemCount = 0,
                syncEnabled = true,
            ),
        )
        whenever(playlistVideoRepository.findAllByYoutubePlaylistIdAndIsActiveTrueOrderByPositionAsc(10L)).thenReturn(
            listOf(
                PlaylistVideo(youtubePlaylistId = 10L, youtubeVideoId = 100L, position = 0, isActive = true),
                PlaylistVideo(youtubePlaylistId = 10L, youtubeVideoId = 101L, position = 1, isActive = true),
            ),
        )

        val response = service.getPlaylists(kind = null, search = null, page = 1, size = 20, sort = null, order = null)

        assertThat(response.data).hasSize(1)
        assertThat(response.data.first().itemCount).isEqualTo(2)
    }

    @Test
    fun `get sync jobs returns recent jobs with playlist item counts`() {
        val startedAt = OffsetDateTime.parse("2026-04-14T06:00:00Z")
        whenever(youtubeSyncJobRepository.findTop20ByOrderByStartedAtDesc()).thenReturn(
            listOf(
                YoutubeSyncJob(
                    id = 1L,
                    triggerType = YoutubeSyncTriggerType.SCHEDULED,
                    startedAt = startedAt,
                    finishedAt = startedAt.plusMinutes(3),
                    status = YoutubeSyncJobStatus.PARTIAL_FAILED,
                    totalPlaylists = 3,
                    succeededPlaylists = 2,
                    failedPlaylists = 1,
                    errorSummary = "1 playlist failed",
                ),
            ),
        )
        whenever(youtubeSyncJobItemRepository.findAllByJobIdOrderByIdAsc(1L)).thenReturn(
            listOf(
                YoutubeSyncJobItem(
                    id = 10L,
                    jobId = 1L,
                    contentMenuId = 101L,
                    youtubePlaylistId = 201L,
                    status = YoutubeSyncJobItemStatus.SUCCEEDED,
                    startedAt = startedAt,
                ),
                YoutubeSyncJobItem(
                    id = 11L,
                    jobId = 1L,
                    contentMenuId = 102L,
                    youtubePlaylistId = 202L,
                    status = YoutubeSyncJobItemStatus.FAILED,
                    startedAt = startedAt,
                ),
            ),
        )

        val response = service.getSyncJobs()

        assertThat(response.data).hasSize(1)
        assertThat(response.data.first().status).isEqualTo("PARTIAL_FAILED")
        assertThat(response.data.first().itemCount).isEqualTo(2)
        assertThat(response.data.first().failedItemCount).isEqualTo(1)
    }

    @Test
    fun `get sync job detail returns job and playlist item details`() {
        val startedAt = OffsetDateTime.parse("2026-04-14T06:00:00Z")
        whenever(youtubeSyncJobRepository.findById(1L)).thenReturn(
            java.util.Optional.of(
                YoutubeSyncJob(
                    id = 1L,
                    triggerType = YoutubeSyncTriggerType.SCHEDULED,
                    startedAt = startedAt,
                    finishedAt = startedAt.plusMinutes(3),
                    status = YoutubeSyncJobStatus.PARTIAL_FAILED,
                    totalPlaylists = 2,
                    succeededPlaylists = 1,
                    failedPlaylists = 1,
                    errorSummary = "1 playlist failed",
                ),
            ),
        )
        whenever(youtubeSyncJobItemRepository.findAllByJobIdOrderByIdAsc(1L)).thenReturn(
            listOf(
                YoutubeSyncJobItem(
                    id = 10L,
                    jobId = 1L,
                    contentMenuId = 101L,
                    youtubePlaylistId = 201L,
                    status = YoutubeSyncJobItemStatus.SUCCEEDED,
                    processedItems = 12,
                    startedAt = startedAt,
                    finishedAt = startedAt.plusMinutes(1),
                ),
                YoutubeSyncJobItem(
                    id = 11L,
                    jobId = 1L,
                    contentMenuId = 102L,
                    youtubePlaylistId = 202L,
                    status = YoutubeSyncJobItemStatus.FAILED,
                    errorMessage = "quota exceeded",
                    startedAt = startedAt.plusMinutes(1),
                    finishedAt = startedAt.plusMinutes(2),
                ),
            ),
        )
        whenever(contentMenuRepository.findAllById(listOf(101L, 102L))).thenReturn(
            listOf(
                ContentMenu(
                    id = 101L,
                    siteKey = "messages",
                    menuName = "말씀/설교",
                    slug = "messages",
                    contentKind = ContentKind.LONG_FORM,
                    active = true,
                ),
                ContentMenu(
                    id = 102L,
                    siteKey = "its-okay",
                    menuName = "그래도 괜찮아",
                    slug = "its-okay",
                    contentKind = ContentKind.SHORT,
                    active = true,
                ),
            ),
        )
        whenever(youtubePlaylistRepository.findAllById(listOf(201L, 202L))).thenReturn(
            listOf(
                YoutubePlaylist(
                    id = 201L,
                    contentMenuId = 101L,
                    youtubePlaylistId = "PL_MESSAGES",
                    title = "말씀/설교",
                    syncEnabled = true,
                ),
                YoutubePlaylist(
                    id = 202L,
                    contentMenuId = 102L,
                    youtubePlaylistId = "PL_SHORTS",
                    title = "그래도 괜찮아",
                    syncEnabled = true,
                ),
            ),
        )

        val detail = service.getSyncJob(1L)

        assertThat(detail.id).isEqualTo(1L)
        assertThat(detail.status).isEqualTo("PARTIAL_FAILED")
        assertThat(detail.items).hasSize(2)
        assertThat(detail.items.first().siteKey).isEqualTo("messages")
        assertThat(detail.items.last().youtubePlaylistId).isEqualTo("PL_SHORTS")
        assertThat(detail.items.last().errorMessage).isEqualTo("quota exceeded")
    }
}
