package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.common.config.YoutubeProperties
import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.ContentMenuStatus
import kr.or.thejejachurch.api.media.domain.VideoMetadata
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubeApiOperations
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubeChannelPlaylistResource
import kr.or.thejejachurch.api.media.infrastructure.youtube.YoutubeChannelPlaylistsPage
import kr.or.thejejachurch.api.media.domain.YoutubeVideo
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.VideoMetadataRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeVideoRepository
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDetailDto
import kr.or.thejejachurch.api.media.interfaces.dto.DiscoverPlaylistsRequest
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoMetadataDto
import kr.or.thejejachurch.api.media.interfaces.dto.CreatePlaylistRequest
import kr.or.thejejachurch.api.media.interfaces.dto.UpdatePlaylistRequest
import kr.or.thejejachurch.api.media.interfaces.dto.UpdateVideoMetadataRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime
import java.util.Optional

class AdminMediaCommandServiceTest {

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
        youtubeProperties = YoutubeProperties(channelId = "DEFAULT_CHANNEL"),
    )

    @Test
    fun `discover playlists creates draft menus for unmapped youtube playlists`() {
        whenever(youtubeApiOperations.getChannelPlaylists("CHANNEL_1", null, 50)).thenReturn(
            YoutubeChannelPlaylistsPage(
                nextPageToken = null,
                items = listOf(
                    YoutubeChannelPlaylistResource(
                        youtubePlaylistId = "PL_DISCOVERED_1",
                        title = "새벽 기도회",
                        description = "새벽 기도회 재생목록",
                        channelId = "CHANNEL_1",
                        channelTitle = "The 제자교회",
                        thumbnailUrl = "https://example.com/playlist.jpg",
                        itemCount = 14,
                    ),
                    YoutubeChannelPlaylistResource(
                        youtubePlaylistId = "PL_EXISTING",
                        title = "이미 연결된 재생목록",
                        description = null,
                        channelId = "CHANNEL_1",
                        channelTitle = "The 제자교회",
                        thumbnailUrl = null,
                        itemCount = 3,
                    ),
                ),
            ),
        )
        whenever(youtubePlaylistRepository.findByYoutubePlaylistId("PL_DISCOVERED_1")).thenReturn(null)
        whenever(youtubePlaylistRepository.findByYoutubePlaylistId("PL_EXISTING")).thenReturn(
            YoutubePlaylist(
                id = 99L,
                contentMenuId = 10L,
                youtubePlaylistId = "PL_EXISTING",
                title = "이미 연결된 재생목록",
            )
        )
        whenever(contentMenuRepository.findBySiteKey("playlist-vered-1")).thenReturn(null)
        whenever(contentMenuRepository.findBySlug("playlist-vered-1")).thenReturn(null)
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
        assertThat(response.skippedCount).isEqualTo(1)
        assertThat(response.items).hasSize(1)
        assertThat(response.items.first().youtubePlaylistId).isEqualTo("PL_DISCOVERED_1")
        assertThat(response.items.first().status).isEqualTo("DRAFT")
        assertThat(response.items.first().navigationVisible).isFalse()
        assertThat(response.items.first().syncEnabled).isFalse()
    }

    @Test
    fun `create playlist creates unified sermon menu resource`() {
        whenever(contentMenuRepository.findBySiteKey("sermons")).thenReturn(null)
        whenever(contentMenuRepository.findBySlug("sermons")).thenReturn(null)
        whenever(youtubePlaylistRepository.findByYoutubePlaylistId("PL_SERMONS")).thenReturn(null)
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
        whenever(adminMediaQueryService.getPlaylist("sermons")).thenReturn(
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
                discoveredAt = "2026-04-14T06:00:00Z",
                publishedAt = "2026-04-14T06:00:00Z",
                lastModifiedBy = null,
                youtubePlaylistId = "PL_SERMONS",
                youtubeTitle = "예배 영상",
                youtubeDescription = "",
                channelTitle = "",
                thumbnailUrl = "",
                itemCount = 0,
                syncEnabled = true,
            ),
        )

        val created = service.createPlaylist(
            CreatePlaylistRequest(
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

        assertThat(created.siteKey).isEqualTo("sermons")
        assertThat(created.status).isEqualTo("PUBLISHED")
        assertThat(created.sortOrder).isEqualTo(10)
        verify(contentMenuRepository).save(any())
        verify(youtubePlaylistRepository).save(any())
    }

    @Test
    fun `update playlist updates menu and sync flag`() {
        whenever(contentMenuRepository.findBySiteKey("messages")).thenReturn(
            ContentMenu(
                id = 1L,
                siteKey = "messages",
                menuName = "말씀/설교",
                slug = "messages",
                contentKind = ContentKind.LONG_FORM,
                status = ContentMenuStatus.DRAFT,
                active = true,
            )
        )
        whenever(youtubePlaylistRepository.findByContentMenuId(1L)).thenReturn(
            YoutubePlaylist(
                id = 10L,
                contentMenuId = 1L,
                youtubePlaylistId = "PL_MESSAGES",
                title = "말씀/설교",
                syncEnabled = true,
            )
        )
        whenever(contentMenuRepository.findBySlug("messages-renewed")).thenReturn(null)
        whenever(adminMediaQueryService.getPlaylist("messages")).thenReturn(
            AdminPlaylistDetailDto(
                id = 1L,
                menuName = "새 말씀 메뉴",
                siteKey = "messages",
                slug = "messages-renewed",
                contentKind = "LONG_FORM",
                status = "PUBLISHED",
                active = false,
                navigationVisible = false,
                sortOrder = 3,
                description = "업데이트 설명",
                discoveredAt = "2026-04-14T06:00:00Z",
                publishedAt = "2026-04-15T06:00:00Z",
                lastModifiedBy = null,
                youtubePlaylistId = "PL_MESSAGES",
                youtubeTitle = "말씀/설교",
                youtubeDescription = "",
                channelTitle = "The 제자교회",
                thumbnailUrl = "",
                itemCount = 12,
                syncEnabled = false,
                lastSyncedAt = null,
            )
        )

        val result = service.updatePlaylist(
            siteKey = "messages",
            request = UpdatePlaylistRequest(
                menuName = "새 말씀 메뉴",
                slug = "messages-renewed",
                youtubePlaylistId = "PL_MESSAGES",
                syncEnabled = false,
                active = false,
                status = "PUBLISHED",
                navigationVisible = false,
                sortOrder = 3,
                description = "업데이트 설명",
            ),
        )

        assertThat(result.slug).isEqualTo("messages-renewed")
        assertThat(result.syncEnabled).isFalse()
        assertThat(result.status).isEqualTo("PUBLISHED")
        assertThat(result.navigationVisible).isFalse()
        assertThat(result.sortOrder).isEqualTo(3)
        assertThat(result.description).isEqualTo("업데이트 설명")
        assertThat(result.discoveredAt).isEqualTo("2026-04-14T06:00:00Z")
        assertThat(result.publishedAt).isEqualTo("2026-04-15T06:00:00Z")
        assertThat(result.lastModifiedBy).isNull()
        verify(contentMenuRepository).save(any())
        verify(youtubePlaylistRepository).save(any())
    }

    @Test
    fun `update playlist rejects duplicate slug`() {
        whenever(contentMenuRepository.findBySiteKey("messages")).thenReturn(
            ContentMenu(
                id = 1L,
                siteKey = "messages",
                menuName = "말씀/설교",
                slug = "messages",
                contentKind = ContentKind.LONG_FORM,
                status = ContentMenuStatus.DRAFT,
                active = true,
            )
        )
        whenever(contentMenuRepository.findBySlug("better-devotion")).thenReturn(
            ContentMenu(
                id = 2L,
                siteKey = "better-devotion",
                menuName = "더 좋은 묵상",
                slug = "better-devotion",
                contentKind = ContentKind.LONG_FORM,
                status = ContentMenuStatus.DRAFT,
                active = true,
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            service.updatePlaylist(
                siteKey = "messages",
                request = UpdatePlaylistRequest(
                    menuName = "새 말씀 메뉴",
                    slug = "better-devotion",
                    youtubePlaylistId = "PL_MESSAGES",
                    syncEnabled = true,
                    active = true,
                    status = "DRAFT",
                ),
            )
        }

        assertThat(exception.message).isEqualTo("이미 사용 중인 slug입니다.")
    }

    @Test
    fun `discover playlists uses configured default channel when request is omitted`() {
        whenever(youtubeApiOperations.getChannelPlaylists("DEFAULT_CHANNEL", null, 50)).thenReturn(
            YoutubeChannelPlaylistsPage(
                nextPageToken = null,
                items = listOf(
                    YoutubeChannelPlaylistResource(
                        youtubePlaylistId = "PL_DISCOVERED_DEFAULT",
                        title = "수요 예배",
                        description = null,
                        channelId = "DEFAULT_CHANNEL",
                        channelTitle = "The 제자교회",
                        thumbnailUrl = null,
                        itemCount = 9,
                    ),
                ),
            ),
        )
        whenever(youtubePlaylistRepository.findByYoutubePlaylistId("PL_DISCOVERED_DEFAULT")).thenReturn(null)
        whenever(contentMenuRepository.findAll()).thenReturn(emptyList())
        whenever(contentMenuRepository.save(any())).thenAnswer {
            val menu = it.getArgument<ContentMenu>(0)
            ContentMenu(
                id = 3L,
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

        val result = service.discoverPlaylists(
            actorId = 7L,
            request = null,
        )

        assertThat(result.discoveredCount).isEqualTo(1)
        assertThat(result.skippedCount).isEqualTo(0)
        assertThat(result.items).hasSize(1)
    }

    @Test
    fun `update video metadata upserts metadata and returns latest detail`() {
        whenever(youtubeVideoRepository.findByYoutubeVideoId("video-100")).thenReturn(
            YoutubeVideo(
                id = 100L,
                youtubeVideoId = "video-100",
                title = "원본 제목",
                description = "설명",
                publishedAt = OffsetDateTime.parse("2026-04-01T10:00:00Z"),
                thumbnailUrl = "https://example.com/original.jpg",
                detectedKind = ContentKind.LONG_FORM,
                youtubeWatchUrl = "https://youtube.com/watch?v=video-100",
                youtubeEmbedUrl = "https://youtube.com/embed/video-100",
            )
        )
        whenever(videoMetadataRepository.findByYoutubeVideoId(100L)).thenReturn(null)
        whenever(videoMetadataRepository.save(any())).thenAnswer { it.getArgument<VideoMetadata>(0) }
        whenever(adminMediaQueryService.getVideoMetadata("video-100")).thenReturn(
            AdminVideoMetadataDto(
                youtubeVideoId = "video-100",
                originalTitle = "원본 제목",
                originalDescription = "설명",
                publishedAt = "2026-04-01T10:00:00Z",
                watchUrl = "https://youtube.com/watch?v=video-100",
                embedUrl = "https://youtube.com/embed/video-100",
                lastSyncedAt = "2026-04-01T12:00:00Z",
                visible = false,
                featured = true,
                pinnedRank = 3,
                manualTitle = "표시 제목",
                manualThumbnailUrl = "https://example.com/manual.jpg",
                manualPublishedDate = "2026-04-06",
                manualKind = "SHORT",
                preacher = "담임목사",
                scripture = "요한복음 3:16",
                scriptureBody = "본문 전문",
                serviceType = "주일예배",
                summary = "요약",
                tags = listOf("grace", "gospel"),
            )
        )

        val result = service.updateVideoMetadata(
            youtubeVideoId = "video-100",
            request = UpdateVideoMetadataRequest(
                visible = false,
                featured = true,
                pinnedRank = 3,
                manualTitle = "표시 제목",
                manualThumbnailUrl = "https://example.com/manual.jpg",
                manualPublishedDate = "2026-04-06",
                manualKind = "SHORT",
                preacher = "담임목사",
                scripture = "요한복음 3:16",
                scriptureBody = "본문 전문",
                serviceType = "주일예배",
                summary = "요약",
                tags = listOf("grace", "gospel"),
            ),
        )

        assertThat(result.manualTitle).isEqualTo("표시 제목")
        assertThat(result.manualKind).isEqualTo("SHORT")
        verify(videoMetadataRepository).save(any())
    }

    @Test
    fun `update video metadata throws not found when video is missing`() {
        whenever(youtubeVideoRepository.findByYoutubeVideoId("missing")).thenReturn(null)

        assertThrows<NotFoundException> {
            service.updateVideoMetadata(
                youtubeVideoId = "missing",
                request = UpdateVideoMetadataRequest(
                    visible = true,
                    featured = false,
                ),
            )
        }

        verify(youtubeVideoRepository).findByYoutubeVideoId("missing")
    }
}
