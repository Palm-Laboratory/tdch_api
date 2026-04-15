package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
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
import java.util.Optional

class AdminMediaPlaylistOperationalStatusQueryTest {

    private val contentMenuRepository: ContentMenuRepository = mock()
    private val youtubePlaylistRepository: YoutubePlaylistRepository = mock()
    private val playlistVideoRepository: PlaylistVideoRepository = mock()
    private val youtubeVideoRepository: YoutubeVideoRepository = mock()
    private val videoMetadataRepository: VideoMetadataRepository = mock()
    private val youtubeSyncJobRepository: YoutubeSyncJobRepository = mock()
    private val youtubeSyncJobItemRepository: YoutubeSyncJobItemRepository = mock()

    private fun newService(): AdminMediaQueryService =
        AdminMediaQueryService(
            contentMenuRepository = contentMenuRepository,
            youtubePlaylistRepository = youtubePlaylistRepository,
            playlistVideoRepository = playlistVideoRepository,
            youtubeVideoRepository = youtubeVideoRepository,
            videoMetadataRepository = videoMetadataRepository,
            youtubeSyncJobRepository = youtubeSyncJobRepository,
            youtubeSyncJobItemRepository = youtubeSyncJobItemRepository,
        )

    private fun menu(siteKey: String = "sermons"): ContentMenu =
        ContentMenu(
            id = 11L,
            siteKey = siteKey,
            menuName = "주일예배",
            slug = siteKey,
            contentKind = ContentKind.LONG_FORM,
            active = true,
        )

    @Test
    fun `sync disabled playlist reports SYNC_DISABLED on list and detail`() {
        val playlist = playlist(syncEnabled = false)
        stubCommon(menu = menu(), playlist = playlist)

        val listResponse = newService().getPlaylists(null, null, 1, 20, null, null)
        val detailResponse = newService().getPlaylist("sermons")

        assertThat(listResponse.data.single().operationStatus).isEqualTo("SYNC_DISABLED")
        assertThat(detailResponse.operationStatus).isEqualTo("SYNC_DISABLED")
    }

    @Test
    fun `newer sync failure than success reports SYNC_FAILED on list and detail`() {
        val playlist = playlist(
            lastSyncedAt = OffsetDateTime.parse("2026-04-14T00:00:00Z"),
            lastSyncSucceededAt = OffsetDateTime.parse("2026-04-14T00:00:00Z"),
            lastSyncFailedAt = OffsetDateTime.parse("2026-04-15T00:00:00Z"),
            lastSyncErrorMessage = "quota exceeded",
        )
        stubCommon(menu = menu(), playlist = playlist)

        val listResponse = newService().getPlaylists(null, null, 1, 20, null, null)
        val detailResponse = newService().getPlaylist("sermons")

        assertThat(listResponse.data.single().operationStatus).isEqualTo("SYNC_FAILED")
        assertThat(detailResponse.operationStatus).isEqualTo("SYNC_FAILED")
    }

    @Test
    fun `successful sync without newer failure reports READY on list and detail`() {
        val playlist = playlist(
            lastSyncedAt = OffsetDateTime.parse("2026-04-15T00:00:00Z"),
            lastSyncSucceededAt = OffsetDateTime.parse("2026-04-15T00:00:00Z"),
            lastSyncFailedAt = OffsetDateTime.parse("2026-04-14T00:00:00Z"),
            lastSyncErrorMessage = null,
        )
        stubCommon(menu = menu(), playlist = playlist)

        val listResponse = newService().getPlaylists(null, null, 1, 20, null, null)
        val detailResponse = newService().getPlaylist("sermons")

        assertThat(listResponse.data.single().operationStatus).isEqualTo("READY")
        assertThat(detailResponse.operationStatus).isEqualTo("READY")
    }

    @Test
    fun `sync enabled playlist without sync history reports PENDING_SYNC on list and detail`() {
        val playlist = playlist(
            lastSyncedAt = null,
            lastSyncSucceededAt = null,
            lastSyncFailedAt = null,
            lastSyncErrorMessage = null,
        )
        stubCommon(menu = menu(), playlist = playlist)

        val listResponse = newService().getPlaylists(null, null, 1, 20, null, null)
        val detailResponse = newService().getPlaylist("sermons")

        assertThat(listResponse.data.single().operationStatus).isEqualTo("PENDING_SYNC")
        assertThat(detailResponse.operationStatus).isEqualTo("PENDING_SYNC")
    }

    private fun stubCommon(menu: ContentMenu, playlist: YoutubePlaylist) {
        whenever(contentMenuRepository.findAll()).thenReturn(listOf(menu))
        whenever(contentMenuRepository.findBySiteKey(menu.siteKey)).thenReturn(menu)
        whenever(youtubePlaylistRepository.findByContentMenuId(menu.id!!)).thenReturn(playlist)
        whenever(playlistVideoRepository.findAllByYoutubePlaylistIdAndIsActiveTrueOrderByPositionAsc(playlist.id!!)).thenReturn(emptyList())
    }

    private fun playlist(
        syncEnabled: Boolean = true,
        lastSyncedAt: OffsetDateTime? = null,
        lastSyncSucceededAt: OffsetDateTime? = null,
        lastSyncFailedAt: OffsetDateTime? = null,
        lastSyncErrorMessage: String? = null,
    ): YoutubePlaylist =
        YoutubePlaylist(
            id = 21L,
            contentMenuId = 11L,
            youtubePlaylistId = "PL_SERMONS",
            title = "주일예배",
            syncEnabled = syncEnabled,
            lastSyncedAt = lastSyncedAt,
            lastSyncSucceededAt = lastSyncSucceededAt,
            lastSyncFailedAt = lastSyncFailedAt,
            lastSyncErrorMessage = lastSyncErrorMessage,
        )
}
