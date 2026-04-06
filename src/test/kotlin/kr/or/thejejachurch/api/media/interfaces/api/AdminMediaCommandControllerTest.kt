package kr.or.thejejachurch.api.media.interfaces.api

import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.media.application.AdminMediaCommandService
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDetailDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoMetadataDto
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
                active = true,
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
                syncEnabled = true,
                active = true,
            ),
        )

        assertThat(response.slug).isEqualTo("messages-renewed")
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
