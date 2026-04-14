package kr.or.thejejachurch.api.media.interfaces.api

import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.media.application.AdminMediaCommandService
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDetailDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDiscoveryItemDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDiscoveryResponse
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoMetadataDto
import kr.or.thejejachurch.api.media.interfaces.dto.CreatePlaylistRequest
import kr.or.thejejachurch.api.media.interfaces.dto.DiscoverPlaylistsRequest
import kr.or.thejejachurch.api.media.interfaces.dto.UpdatePlaylistRequest
import kr.or.thejejachurch.api.media.interfaces.dto.UpdateVideoMetadataRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AdminMediaCommandControllerTest {

    private val adminMediaCommandService: AdminMediaCommandService = mock()

    @Test
    fun `create playlist delegates to command service`() {
        val controller = AdminMediaCommandController(
            adminMediaCommandService = adminMediaCommandService,
            adminProperties = AdminProperties(syncKey = "secret-key"),
        )
        whenever(adminMediaCommandService.createPlaylist(any())).thenReturn(
            AdminPlaylistDetailDto(
                id = 1L,
                menuName = "예배 영상",
                siteKey = "sermons",
                slug = "sermons",
                contentKind = "LONG_FORM",
                status = "PUBLISHED",
                active = true,
                navigationVisible = true,
                sortOrder = 10,
                description = "메인 설교 모음",
                discoveredAt = null,
                publishedAt = null,
                lastModifiedBy = null,
                youtubePlaylistId = "PL_SERMONS",
                youtubeTitle = "예배 영상",
                youtubeDescription = "",
                channelTitle = "",
                thumbnailUrl = "",
                itemCount = 0,
                syncEnabled = true,
            )
        )

        val response = controller.createPlaylist(
            adminKey = "secret-key",
            actorId = 1L,
            request = CreatePlaylistRequest(
                siteKey = "sermons",
                menuName = "예배 영상",
                slug = "sermons",
                contentKind = "LONG_FORM",
                youtubePlaylistId = "PL_SERMONS",
                syncEnabled = true,
                active = true,
                status = "PUBLISHED",
                navigationVisible = true,
                sortOrder = 10,
                description = "메인 설교 모음",
            ),
        )

        assertThat(response.siteKey).isEqualTo("sermons")
    }

    @Test
    fun `discover playlists delegates to command service`() {
        val controller = AdminMediaCommandController(
            adminMediaCommandService = adminMediaCommandService,
            adminProperties = AdminProperties(syncKey = "secret-key"),
        )
        whenever(
            adminMediaCommandService.discoverPlaylists(
                eq(1L),
                eq(DiscoverPlaylistsRequest(channelId = "CHANNEL_1")),
            ),
        ).thenReturn(
            AdminPlaylistDiscoveryResponse(
                discoveredCount = 1,
                skippedCount = 0,
                items = listOf(
                    AdminPlaylistDiscoveryItemDto(
                        siteKey = "playlist-vered-1",
                        menuName = "새벽 기도회",
                        slug = "playlist-vered-1",
                        contentKind = "LONG_FORM",
                        status = "DRAFT",
                        navigationVisible = false,
                        youtubePlaylistId = "PL_DISCOVERED_1",
                        youtubeTitle = "새벽 기도회",
                        channelTitle = "The 제자교회",
                        itemCount = 14,
                        syncEnabled = false,
                    ),
                ),
            ),
        )

        val response = controller.discoverPlaylists(
            adminKey = "secret-key",
            actorId = 1L,
            request = DiscoverPlaylistsRequest(channelId = "CHANNEL_1"),
        )

        assertThat(response.discoveredCount).isEqualTo(1)
        assertThat(response.items.first().youtubePlaylistId).isEqualTo("PL_DISCOVERED_1")
    }

    @Test
    fun `update playlist delegates to command service`() {
        val controller = AdminMediaCommandController(
            adminMediaCommandService = adminMediaCommandService,
            adminProperties = AdminProperties(syncKey = "secret-key"),
        )
        whenever(adminMediaCommandService.updatePlaylist(eq("messages"), any())).thenReturn(
            AdminPlaylistDetailDto(
                id = 1L,
                menuName = "새 말씀 메뉴",
                siteKey = "messages",
                slug = "messages-renewed",
                contentKind = "LONG_FORM",
                status = "PUBLISHED",
                active = true,
                navigationVisible = false,
                sortOrder = 3,
                description = "업데이트 설명",
                discoveredAt = null,
                publishedAt = null,
                lastModifiedBy = null,
                youtubePlaylistId = "PL_MESSAGES",
                youtubeTitle = "말씀/설교",
                youtubeDescription = "",
                channelTitle = "The 제자교회",
                thumbnailUrl = "",
                itemCount = 12,
                syncEnabled = true,
                lastSyncedAt = null,
            )
        )

        val response = controller.updatePlaylist(
            adminKey = "secret-key",
            actorId = 1L,
            siteKey = "messages",
            request = UpdatePlaylistRequest(
                menuName = "새 말씀 메뉴",
                slug = "messages-renewed",
                youtubePlaylistId = "PL_MESSAGES",
                syncEnabled = true,
                active = true,
                status = "PUBLISHED",
                navigationVisible = false,
                sortOrder = 3,
                description = "업데이트 설명",
            ),
        )

        assertThat(response.slug).isEqualTo("messages-renewed")
        assertThat(response.status).isEqualTo("PUBLISHED")
    }

    @Test
    fun `update video metadata delegates to command service`() {
        val controller = AdminMediaCommandController(
            adminMediaCommandService = adminMediaCommandService,
            adminProperties = AdminProperties(syncKey = "secret-key"),
        )
        whenever(adminMediaCommandService.updateVideoMetadata(eq("video-100"), any())).thenReturn(
            AdminVideoMetadataDto(
                youtubeVideoId = "video-100",
                originalTitle = "원본 제목",
                originalDescription = "설명",
                publishedAt = "2026-04-01T10:00:00Z",
                watchUrl = "https://youtube.com/watch?v=video-100",
                embedUrl = "https://youtube.com/embed/video-100",
                lastSyncedAt = null,
                visible = true,
                featured = false,
                tags = emptyList(),
            )
        )

        val response = controller.updateVideoMetadata(
            adminKey = "secret-key",
            actorId = 1L,
            youtubeVideoId = "video-100",
            request = UpdateVideoMetadataRequest(
                visible = true,
                featured = false,
            ),
        )

        assertThat(response.youtubeVideoId).isEqualTo("video-100")
    }
}
