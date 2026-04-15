package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.common.config.YoutubeProperties
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.VideoMetadataRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeVideoRepository
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubeApiOperations
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubeChannelPlaylistResource
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubeChannelPlaylistsPage
import kr.or.thejejachurch.api.media.interfaces.dto.DiscoverPlaylistsRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AdminMediaPlaylistDiscoveryCommandTest {

    private val contentMenuRepository: ContentMenuRepository = mock()
    private val youtubePlaylistRepository: YoutubePlaylistRepository = mock()
    private val youtubeVideoRepository: YoutubeVideoRepository = mock()
    private val videoMetadataRepository: VideoMetadataRepository = mock()
    private val adminMediaQueryService: AdminMediaQueryService = mock()
    private val youtubeApiOperations: YoutubeApiOperations = mock()

    private val service = AdminMediaCommandService(
        contentMenuRepository = contentMenuRepository,
        youtubePlaylistRepository = youtubePlaylistRepository,
        youtubeVideoRepository = youtubeVideoRepository,
        videoMetadataRepository = videoMetadataRepository,
        adminMediaQueryService = adminMediaQueryService,
        youtubeApiOperations = youtubeApiOperations,
        youtubeProperties = YoutubeProperties(channelId = "CHANNEL_1"),
    )

    @Test
    fun `discover playlists persists manual discovery results`() {
        whenever(youtubeApiOperations.getChannelPlaylists("CHANNEL_1", null, 50)).thenReturn(
            YoutubeChannelPlaylistsPage(
                nextPageToken = null,
                items = listOf(
                    YoutubeChannelPlaylistResource(
                        youtubePlaylistId = "PL_DISCOVERED_1",
                        title = "새벽 기도회",
                        description = null,
                        channelId = "CHANNEL_1",
                        channelTitle = "The 제자교회",
                        thumbnailUrl = null,
                        itemCount = 1,
                    ),
                ),
            ),
        )
        whenever(youtubePlaylistRepository.findByYoutubePlaylistId("PL_DISCOVERED_1")).thenReturn(null)
        whenever(contentMenuRepository.findAll()).thenReturn(emptyList())
        whenever(contentMenuRepository.save(any())).thenAnswer {
            val menu = it.getArgument<ContentMenu>(0)
            ContentMenu(
                id = 1L,
                siteKey = menu.siteKey,
                menuName = menu.menuName,
                slug = menu.slug,
                contentKind = menu.contentKind,
                status = menu.status,
                active = menu.active,
                navigationVisible = menu.navigationVisible,
                sortOrder = menu.sortOrder,
                description = menu.description,
                discoveredAt = menu.discoveredAt,
                publishedAt = menu.publishedAt,
                lastModifiedBy = menu.lastModifiedBy,
            )
        }
        whenever(youtubePlaylistRepository.save(any())).thenAnswer { it.getArgument<YoutubePlaylist>(0) }

        val response = service.discoverPlaylists(
            actorId = 7L,
            request = DiscoverPlaylistsRequest(channelId = "CHANNEL_1"),
        )

        assertThat(response.discoveredCount).isEqualTo(1)
        assertThat(response.skippedCount).isEqualTo(0)
        assertThat(response.items).hasSize(1)
    }

    @Test
    fun `discover playlists writes latest discovery summary onto youtube playlist`() {
        whenever(youtubeApiOperations.getChannelPlaylists("CHANNEL_1", null, 50)).thenReturn(
            YoutubeChannelPlaylistsPage(
                nextPageToken = null,
                items = listOf(
                    YoutubeChannelPlaylistResource(
                        youtubePlaylistId = "PL_DISCOVERED_2",
                        title = "수요예배",
                        description = "주중 예배",
                        channelId = "CHANNEL_1",
                        channelTitle = "The 제자교회",
                        thumbnailUrl = null,
                        itemCount = 3,
                    ),
                ),
            ),
        )
        whenever(youtubePlaylistRepository.findByYoutubePlaylistId("PL_DISCOVERED_2")).thenReturn(null)
        whenever(contentMenuRepository.findAll()).thenReturn(emptyList())
        whenever(contentMenuRepository.save(any())).thenAnswer {
            val menu = it.getArgument<ContentMenu>(0)
            ContentMenu(
                id = 2L,
                siteKey = menu.siteKey,
                menuName = menu.menuName,
                slug = menu.slug,
                contentKind = menu.contentKind,
                status = menu.status,
                active = menu.active,
                navigationVisible = menu.navigationVisible,
                sortOrder = menu.sortOrder,
                description = menu.description,
                discoveredAt = menu.discoveredAt,
                publishedAt = menu.publishedAt,
                lastModifiedBy = menu.lastModifiedBy,
            )
        }
        whenever(youtubePlaylistRepository.save(any())).thenAnswer {
            val playlist = it.getArgument<YoutubePlaylist>(0)
            assertThat(playlist.lastDiscoveredAt).isNotNull()
            assertThat(playlist.discoverySource).isEqualTo("MANUAL")
            playlist
        }

        service.discoverPlaylists(
            actorId = 7L,
            request = DiscoverPlaylistsRequest(channelId = "CHANNEL_1"),
        )

        verify(youtubePlaylistRepository).save(any())
    }
}
