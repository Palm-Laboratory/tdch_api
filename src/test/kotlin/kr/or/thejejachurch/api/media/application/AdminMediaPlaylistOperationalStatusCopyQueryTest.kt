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

class AdminMediaPlaylistOperationalStatusCopyQueryTest {

    private val contentMenuRepository: ContentMenuRepository = mock()
    private val youtubePlaylistRepository: YoutubePlaylistRepository = mock()
    private val playlistVideoRepository: PlaylistVideoRepository = mock()
    private val youtubeVideoRepository: YoutubeVideoRepository = mock()
    private val videoMetadataRepository: VideoMetadataRepository = mock()
    private val youtubeSyncJobRepository: YoutubeSyncJobRepository = mock()
    private val youtubeSyncJobItemRepository: YoutubeSyncJobItemRepository = mock()

    private fun service(): AdminMediaQueryService =
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

    private fun playlist(
        syncEnabled: Boolean = true,
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
            lastSyncSucceededAt = lastSyncSucceededAt,
            lastSyncFailedAt = lastSyncFailedAt,
            lastSyncErrorMessage = lastSyncErrorMessage,
        )

    private fun stubCommon(menu: ContentMenu, playlist: YoutubePlaylist) {
        whenever(contentMenuRepository.findAll()).thenReturn(listOf(menu))
        whenever(contentMenuRepository.findBySiteKey(menu.siteKey)).thenReturn(menu)
        whenever(youtubePlaylistRepository.findByContentMenuId(menu.id!!)).thenReturn(playlist)
        whenever(playlistVideoRepository.findAllByYoutubePlaylistIdAndIsActiveTrueOrderByPositionAsc(playlist.id!!))
            .thenReturn(emptyList())
    }

    @Test
    fun `sync disabled playlist exposes Sync 꺼짐 copy on list and detail`() {
        val playlist = playlist(syncEnabled = false)
        stubCommon(menu(), playlist)

        val listResponse = service().getPlaylists(null, null, 1, 20, null, null)
        val detailResponse = service().getPlaylist("sermons")

        assertThat(listResponse.data.single().operationStatusLabel).isEqualTo("Sync 꺼짐")
        assertThat(listResponse.data.single().operationStatusDescription).isEqualTo("정기 sync 대상에서 제외되어 있습니다.")
        assertThat(detailResponse.operationStatusLabel).isEqualTo("Sync 꺼짐")
        assertThat(detailResponse.operationStatusDescription).isEqualTo("정기 sync 대상에서 제외되어 있습니다.")
    }

    @Test
    fun `failed sync exposes 점검 필요 copy on list and detail`() {
        val playlist = playlist(
            lastSyncSucceededAt = OffsetDateTime.parse("2026-04-14T00:00:00Z"),
            lastSyncFailedAt = OffsetDateTime.parse("2026-04-15T00:00:00Z"),
            lastSyncErrorMessage = "quota exceeded",
        )
        stubCommon(menu(), playlist)

        val listResponse = service().getPlaylists(null, null, 1, 20, null, null)
        val detailResponse = service().getPlaylist("sermons")

        assertThat(listResponse.data.single().operationStatusLabel).isEqualTo("점검 필요")
        assertThat(listResponse.data.single().operationStatusDescription).isEqualTo("최근 sync 실패를 먼저 확인해야 합니다.")
        assertThat(detailResponse.operationStatusLabel).isEqualTo("점검 필요")
        assertThat(detailResponse.operationStatusDescription).isEqualTo("최근 sync 실패를 먼저 확인해야 합니다.")
    }

    @Test
    fun `ready playlist exposes 정상 운영 copy on list and detail`() {
        val playlist = playlist(
            lastSyncSucceededAt = OffsetDateTime.parse("2026-04-15T00:00:00Z"),
            lastSyncFailedAt = OffsetDateTime.parse("2026-04-14T00:00:00Z"),
        )
        stubCommon(menu(), playlist)

        val listResponse = service().getPlaylists(null, null, 1, 20, null, null)
        val detailResponse = service().getPlaylist("sermons")

        assertThat(listResponse.data.single().operationStatusLabel).isEqualTo("정상 운영")
        assertThat(listResponse.data.single().operationStatusDescription).isEqualTo("최근 sync 기준으로 운영 상태가 정상입니다.")
        assertThat(detailResponse.operationStatusLabel).isEqualTo("정상 운영")
        assertThat(detailResponse.operationStatusDescription).isEqualTo("최근 sync 기준으로 운영 상태가 정상입니다.")
    }

    @Test
    fun `pending sync exposes 첫 sync 대기 copy on list and detail`() {
        val playlist = playlist()
        stubCommon(menu(), playlist)

        val listResponse = service().getPlaylists(null, null, 1, 20, null, null)
        val detailResponse = service().getPlaylist("sermons")

        assertThat(listResponse.data.single().operationStatusLabel).isEqualTo("첫 sync 대기")
        assertThat(listResponse.data.single().operationStatusDescription).isEqualTo("아직 유효한 sync 이력이 없습니다.")
        assertThat(detailResponse.operationStatusLabel).isEqualTo("첫 sync 대기")
        assertThat(detailResponse.operationStatusDescription).isEqualTo("아직 유효한 sync 이력이 없습니다.")
    }
}
