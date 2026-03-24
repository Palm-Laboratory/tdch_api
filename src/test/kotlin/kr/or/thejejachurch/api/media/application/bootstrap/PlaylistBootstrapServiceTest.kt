package kr.or.thejejachurch.api.media.application.bootstrap

import kr.or.thejejachurch.api.common.config.YoutubeProperties
import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PlaylistBootstrapServiceTest {

    private val contentMenuRepository: ContentMenuRepository = mock()
    private val youtubePlaylistRepository: YoutubePlaylistRepository = mock()

    @Test
    fun `creates playlist mapping when env value exists and row is missing`() {
        val properties = YoutubeProperties(
            playlists = YoutubeProperties.PlaylistProperties(messages = "PL_MESSAGES_001"),
        )
        val service = PlaylistBootstrapService(properties, contentMenuRepository, youtubePlaylistRepository)
        val menu = contentMenu(id = 1L, siteKey = "messages", contentKind = ContentKind.LONG_FORM)

        whenever(contentMenuRepository.findBySiteKey("messages")).thenReturn(menu)
        whenever(youtubePlaylistRepository.findByContentMenuId(1L)).thenReturn(null)
        whenever(youtubePlaylistRepository.findByYoutubePlaylistId("PL_MESSAGES_001")).thenReturn(null)

        service.bootstrap()

        val captor = argumentCaptor<YoutubePlaylist>()
        verify(youtubePlaylistRepository).save(captor.capture())
        verify(youtubePlaylistRepository, never()).findByContentMenuId(eq(2L))

        assertThat(captor.firstValue.contentMenuId).isEqualTo(1L)
        assertThat(captor.firstValue.youtubePlaylistId).isEqualTo("PL_MESSAGES_001")
        assertThat(captor.firstValue.title).isEqualTo("말씀/설교")
        assertThat(captor.firstValue.syncEnabled).isTrue()
    }

    @Test
    fun `updates existing playlist mapping when menu already has a row`() {
        val properties = YoutubeProperties(
            playlists = YoutubeProperties.PlaylistProperties(itsOkay = "PL_SHORTS_002"),
        )
        val service = PlaylistBootstrapService(properties, contentMenuRepository, youtubePlaylistRepository)
        val menu = contentMenu(id = 3L, siteKey = "its-okay", contentKind = ContentKind.SHORT)
        val existing = YoutubePlaylist(
            id = 99L,
            contentMenuId = 3L,
            youtubePlaylistId = "OLD_PLAYLIST",
            title = "그래도 괜찮아",
            syncEnabled = false,
        )

        whenever(contentMenuRepository.findBySiteKey("its-okay")).thenReturn(menu)
        whenever(youtubePlaylistRepository.findByContentMenuId(3L)).thenReturn(existing)
        whenever(youtubePlaylistRepository.findByYoutubePlaylistId("PL_SHORTS_002")).thenReturn(null)

        service.bootstrap()

        verify(youtubePlaylistRepository, times(0)).save(org.mockito.kotlin.any())
        assertThat(existing.youtubePlaylistId).isEqualTo("PL_SHORTS_002")
        assertThat(existing.syncEnabled).isTrue()
        assertThat(existing.contentMenuId).isEqualTo(3L)
    }

    private fun contentMenu(
        id: Long,
        siteKey: String,
        contentKind: ContentKind,
    ): ContentMenu = ContentMenu(
        id = id,
        siteKey = siteKey,
        menuName = siteKey,
        slug = siteKey,
        contentKind = contentKind,
    )
}
