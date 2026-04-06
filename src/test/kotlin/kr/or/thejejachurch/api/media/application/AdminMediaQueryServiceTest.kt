package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.PlaylistVideo
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.PlaylistVideoRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.VideoMetadataRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeVideoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AdminMediaQueryServiceTest {

    private val contentMenuRepository: ContentMenuRepository = mock()
    private val youtubePlaylistRepository: YoutubePlaylistRepository = mock()
    private val playlistVideoRepository: PlaylistVideoRepository = mock()
    private val youtubeVideoRepository: YoutubeVideoRepository = mock()
    private val videoMetadataRepository: VideoMetadataRepository = mock()

    private val service = AdminMediaQueryService(
        contentMenuRepository = contentMenuRepository,
        youtubePlaylistRepository = youtubePlaylistRepository,
        playlistVideoRepository = playlistVideoRepository,
        youtubeVideoRepository = youtubeVideoRepository,
        videoMetadataRepository = videoMetadataRepository,
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
}
