package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.common.config.YoutubeProperties
import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.ContentMenuStatus
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.VideoMetadataRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeVideoRepository
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDetailDto
import kr.or.thejejachurch.api.media.interfaces.dto.UpdatePlaylistRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AdminMediaUpdatePlaylistCommandTest {

    private val contentMenuRepository: ContentMenuRepository = mock()
    private val youtubePlaylistRepository: YoutubePlaylistRepository = mock()
    private val youtubeVideoRepository: YoutubeVideoRepository = mock()
    private val videoMetadataRepository: VideoMetadataRepository = mock()
    private val adminMediaQueryService: AdminMediaQueryService = mock()

    private val service = AdminMediaCommandService(
        contentMenuRepository = contentMenuRepository,
        youtubePlaylistRepository = youtubePlaylistRepository,
        youtubeVideoRepository = youtubeVideoRepository,
        videoMetadataRepository = videoMetadataRepository,
        adminMediaQueryService = adminMediaQueryService,
        youtubeApiOperations = mock(),
        youtubeProperties = YoutubeProperties(channelId = "CHANNEL_1"),
    )

    @Test
    fun `update playlist stores content kind and last modified actor`() {
        val menu = ContentMenu(
            id = 11L,
            siteKey = "sermons",
            menuName = "주일예배",
            slug = "sermons",
            contentKind = ContentKind.LONG_FORM,
            status = ContentMenuStatus.DRAFT,
            active = false,
            navigationVisible = false,
            sortOrder = 0,
        )
        val playlist = YoutubePlaylist(
            id = 21L,
            contentMenuId = 11L,
            youtubePlaylistId = "PL_OLD",
            title = "주일예배",
            syncEnabled = false,
        )

        whenever(contentMenuRepository.findBySiteKey("sermons")).thenReturn(menu)
        whenever(contentMenuRepository.findBySlug("weekday-prayer")).thenReturn(null)
        whenever(contentMenuRepository.save(any())).thenAnswer { it.getArgument<ContentMenu>(0) }
        whenever(youtubePlaylistRepository.findByContentMenuId(11L)).thenReturn(playlist)
        whenever(youtubePlaylistRepository.findByYoutubePlaylistId("PL_NEW")).thenReturn(null)
        whenever(youtubePlaylistRepository.save(any())).thenAnswer { it.getArgument<YoutubePlaylist>(0) }
        whenever(adminMediaQueryService.getPlaylist("sermons")).thenReturn(
            AdminPlaylistDetailDto(
                id = 11L,
                menuName = "새벽기도회",
                siteKey = "sermons",
                slug = "weekday-prayer",
                contentKind = "SHORT",
                status = "PUBLISHED",
                active = true,
                navigationVisible = true,
                sortOrder = 3,
                description = "평일 새벽기도회",
                discoveredAt = null,
                publishedAt = null,
                lastModifiedBy = 19L,
                youtubePlaylistId = "PL_NEW",
                youtubeTitle = "새벽기도회",
                youtubeDescription = "",
                channelTitle = "TDCH",
                thumbnailUrl = "https://example.com/thumb.jpg",
                itemCount = 0,
                syncEnabled = true,
                lastSyncedAt = null,
            ),
        )

        service.updatePlaylist(
            actorId = 19L,
            siteKey = "sermons",
            request = UpdatePlaylistRequest(
                menuName = "새벽기도회",
                slug = "weekday-prayer",
                contentKind = "SHORT",
                youtubePlaylistId = "PL_NEW",
                syncEnabled = true,
                active = true,
                status = "PUBLISHED",
                navigationVisible = true,
                sortOrder = 3,
                description = "평일 새벽기도회",
            ),
        )

        assertThat(menu.contentKind).isEqualTo(ContentKind.SHORT)
        assertThat(menu.lastModifiedBy).isEqualTo(19L)
        assertThat(menu.publishedAt).isNotNull()
        assertThat(playlist.youtubePlaylistId).isEqualTo("PL_NEW")
        assertThat(playlist.syncEnabled).isTrue()
    }
}
