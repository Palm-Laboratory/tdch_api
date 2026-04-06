package kr.or.thejejachurch.api.media.interfaces.api

import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.media.application.AdminMediaQueryService
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPaginationDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDetailDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistListResponse
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoListResponse
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoMetadataDto
import kr.or.thejejachurch.api.media.application.YoutubeSyncService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AdminMediaReadEndpointsTest {

    private val youtubeSyncService: YoutubeSyncService = mock()
    private val adminMediaQueryService: AdminMediaQueryService = mock()
    private val syncController = AdminMediaController(
        youtubeSyncService = youtubeSyncService,
        adminProperties = AdminProperties(syncKey = "secret-key"),
    )
    private val queryController = AdminMediaQueryController(
        adminMediaQueryService = adminMediaQueryService,
        adminProperties = AdminProperties(syncKey = "secret-key"),
    )

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        whenever(adminMediaQueryService.getPlaylists(null, null, 1, 20, null, null)).thenReturn(
            AdminPlaylistListResponse(
                data = listOf(
                    AdminPlaylistDto(
                        id = 1L,
                        menuName = "말씀/설교",
                        siteKey = "messages",
                        slug = "messages",
                        contentKind = "LONG_FORM",
                        active = true,
                        youtubePlaylistId = "PL_MESSAGES",
                        itemCount = 12,
                        syncEnabled = true,
                    )
                ),
                pagination = AdminPaginationDto(page = 1, size = 20, totalElements = 1, totalPages = 1),
            )
        )
        whenever(adminMediaQueryService.getPlaylist("messages")).thenReturn(
            AdminPlaylistDetailDto(
                id = 1L,
                menuName = "말씀/설교",
                siteKey = "messages",
                slug = "messages",
                contentKind = "LONG_FORM",
                active = true,
                youtubePlaylistId = "PL_MESSAGES",
                youtubeTitle = "말씀/설교",
                youtubeDescription = "",
                channelTitle = "The 제자교회",
                thumbnailUrl = "https://example.com/thumb.jpg",
                itemCount = 12,
                syncEnabled = true,
            )
        )
        whenever(adminMediaQueryService.getPlaylistVideos("messages", null, null, null, 1, 20)).thenReturn(
            AdminVideoListResponse(
                data = listOf(
                    AdminVideoDto(
                        youtubeVideoId = "video-100",
                        position = 1,
                        visible = true,
                        featured = false,
                        displayTitle = "영상 제목",
                        displayThumbnailUrl = "https://example.com/thumb.jpg",
                        displayPublishedDate = "2026-04-01",
                        originalTitle = "영상 제목",
                        publishedAt = "2026-04-01T10:00:00Z",
                        thumbnailUrl = "https://example.com/thumb.jpg",
                    )
                ),
                pagination = AdminPaginationDto(page = 1, size = 20, totalElements = 1, totalPages = 1),
            )
        )
        whenever(adminMediaQueryService.getVideoMetadata("video-100")).thenReturn(
            AdminVideoMetadataDto(
                youtubeVideoId = "video-100",
                originalTitle = "영상 제목",
                originalDescription = "설명",
                publishedAt = "2026-04-01T10:00:00Z",
                watchUrl = "https://youtube.com/watch?v=video-100",
                embedUrl = "https://youtube.com/embed/video-100",
                lastSyncedAt = "2026-04-01T12:00:00Z",
                visible = true,
                featured = false,
                tags = emptyList(),
            )
        )

        mockMvc = MockMvcBuilders.standaloneSetup(syncController, queryController).build()
    }

    @Test
    fun `playlist list endpoint should respond with admin media payload`() {
        mockMvc.perform(
            get("/api/v1/admin/media/playlists")
                .header("X-Admin-Key", "secret-key")
                .header("X-Admin-Actor-Id", "1"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    fun `playlist detail endpoint should respond with a single playlist payload`() {
        mockMvc.perform(
            get("/api/v1/admin/media/playlists/messages")
                .header("X-Admin-Key", "secret-key")
                .header("X-Admin-Actor-Id", "1"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.siteKey").value("messages"))
    }

    @Test
    fun `playlist videos endpoint should respond with paged videos payload`() {
        mockMvc.perform(
            get("/api/v1/admin/media/playlists/messages/videos")
                .header("X-Admin-Key", "secret-key")
                .header("X-Admin-Actor-Id", "1")
                .param("page", "1")
                .param("size", "20"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    fun `video metadata endpoint should respond with metadata payload`() {
        mockMvc.perform(
            get("/api/v1/admin/media/videos/video-100/metadata")
                .header("X-Admin-Key", "secret-key")
                .header("X-Admin-Actor-Id", "1"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.youtubeVideoId").value("video-100"))
    }
}
