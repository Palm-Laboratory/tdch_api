package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.VideoMetadata
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import kr.or.thejejachurch.api.media.domain.YoutubeVideo
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.VideoMetadataRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeVideoRepository
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDetailDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminVideoMetadataDto
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

    private val service = AdminMediaCommandService(
        contentMenuRepository = contentMenuRepository,
        youtubePlaylistRepository = youtubePlaylistRepository,
        youtubeVideoRepository = youtubeVideoRepository,
        videoMetadataRepository = videoMetadataRepository,
        adminMediaQueryService = adminMediaQueryService,
    )

    @Test
    fun `update playlist updates menu and sync flag`() {
        whenever(contentMenuRepository.findBySiteKey("messages")).thenReturn(
            ContentMenu(
                id = 1L,
                siteKey = "messages",
                menuName = "말씀/설교",
                slug = "messages",
                contentKind = ContentKind.LONG_FORM,
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
                active = false,
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
                syncEnabled = false,
                active = false,
            ),
        )

        assertThat(result.slug).isEqualTo("messages-renewed")
        assertThat(result.syncEnabled).isFalse()
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
                active = true,
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            service.updatePlaylist(
                siteKey = "messages",
                request = UpdatePlaylistRequest(
                    menuName = "새 말씀 메뉴",
                    slug = "better-devotion",
                    syncEnabled = true,
                    active = true,
                ),
            )
        }

        assertThat(exception.message).isEqualTo("이미 사용 중인 slug입니다.")
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
