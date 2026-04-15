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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class MediaQueryServiceTest {

    private val contentMenuRepository: ContentMenuRepository = mock()
    private val youtubePlaylistRepository: YoutubePlaylistRepository = mock()
    private val playlistVideoRepository: PlaylistVideoRepository = mock()
    private val youtubeVideoRepository: YoutubeVideoRepository = mock()
    private val videoMetadataRepository: VideoMetadataRepository = mock()

    private val service = MediaQueryService(
        contentMenuRepository = contentMenuRepository,
        youtubePlaylistRepository = youtubePlaylistRepository,
        playlistVideoRepository = playlistVideoRepository,
        youtubeVideoRepository = youtubeVideoRepository,
        videoMetadataRepository = videoMetadataRepository,
    )

    @Test
    fun `getVideos resolves the public sermon route by slug`() {
        val siteKey = "messages"
        val slug = "public-sermon-2024"
        val menu = contentMenu(
            id = 11L,
            siteKey = siteKey,
            slug = slug,
        )
        val playlist = YoutubePlaylist(
            id = 21L,
            contentMenuId = 11L,
            youtubePlaylistId = "PL_1",
            title = "예배 영상",
        )

        whenever(contentMenuRepository.findBySiteKey(slug)).thenReturn(menu)
        whenever(contentMenuRepository.findBySlug(slug)).thenReturn(menu)
        whenever(youtubePlaylistRepository.findByContentMenuIdAndSyncEnabledTrue(11L)).thenReturn(playlist)
        whenever(
            playlistVideoRepository.findAllByYoutubePlaylistIdAndIsActiveTrueOrderByPositionAsc(
                21L,
                PageRequest.of(0, 12),
            ),
        ).thenReturn(PageImpl(emptyList<PlaylistVideo>(), PageRequest.of(0, 12), 0))

        val response = service.getVideos(slug, 0, 12)

        assertThat(response.menu.slug).isEqualTo(slug)
        verify(contentMenuRepository).findBySlug(slug)
    }

    private fun contentMenu(
        id: Long,
        siteKey: String,
        slug: String,
    ): ContentMenu = ContentMenu(
        id = id,
        siteKey = siteKey,
        menuName = "주일 예배",
        slug = slug,
        contentKind = ContentKind.LONG_FORM,
        active = true,
    )
}
