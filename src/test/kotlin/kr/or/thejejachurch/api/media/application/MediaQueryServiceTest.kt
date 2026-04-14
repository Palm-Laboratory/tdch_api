package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.PlaylistVideo
import kr.or.thejejachurch.api.media.domain.VideoMetadata
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import kr.or.thejejachurch.api.media.domain.YoutubeVideo
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.PlaylistVideoRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.VideoMetadataRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubeVideoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

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
    fun `getVideos joins playlist order with video metadata`() {
        val menu = menu(id = 1L, siteKey = "messages", contentKind = ContentKind.LONG_FORM)
        val playlist = playlist(id = 10L, contentMenuId = 1L)
        val firstVideo = video(id = 100L, youtubeVideoId = "video-100", title = "원본 제목 1")
        val secondVideo = video(id = 101L, youtubeVideoId = "video-101", title = "원본 제목 2")
        val secondMetadata = metadata(
            youtubeVideoId = 101L,
            manualTitle = "수정 제목",
            manualThumbnailUrl = "https://example.com/manual-thumb.jpg",
            manualPublishedDate = LocalDate.of(2026, 3, 4),
            manualKind = ContentKind.SHORT,
            featured = true,
        )

        whenever(contentMenuRepository.findBySiteKey("messages")).thenReturn(menu)
        whenever(youtubePlaylistRepository.findByContentMenuIdAndSyncEnabledTrue(1L)).thenReturn(playlist)
        whenever(playlistVideoRepository.findAllByYoutubePlaylistIdAndIsActiveTrueOrderByPositionAsc(eq(10L), any())).thenReturn(
            PageImpl(
                listOf(
                    playlistVideo(10L, 100L, 0),
                    playlistVideo(10L, 101L, 1),
                ),
            ),
        )
        whenever(youtubeVideoRepository.findAllById(listOf(100L, 101L))).thenReturn(listOf(firstVideo, secondVideo))
        whenever(videoMetadataRepository.findAllByYoutubeVideoIdIn(listOf(100L, 101L))).thenReturn(listOf(secondMetadata))

        val response = service.getVideos(siteKey = "messages", page = 0, size = 12)

        assertThat(response.totalElements).isEqualTo(2)
        assertThat(response.items).hasSize(2)
        assertThat(response.items[0].displayTitle).isEqualTo("원본 제목 1")
        assertThat(response.items[1].displayTitle).isEqualTo("수정 제목")
        assertThat(response.items[1].thumbnailUrl).isEqualTo("https://example.com/manual-thumb.jpg")
        assertThat(response.items[1].displayDate).isEqualTo("2026-03-04")
        assertThat(response.items[1].contentKind).isEqualTo("SHORT")
        assertThat(response.items[1].featured).isTrue()
    }

    @Test
    fun `getVideo prefers manual fields from metadata`() {
        val video = video(id = 100L, youtubeVideoId = "video-100", title = "원본 제목")
        val metadata = metadata(
            youtubeVideoId = 100L,
            manualTitle = "표시 제목",
            manualThumbnailUrl = "https://example.com/manual.jpg",
            tags = arrayOf("messages", "grace"),
        )

        whenever(youtubeVideoRepository.findByYoutubeVideoId("video-100")).thenReturn(video)
        whenever(videoMetadataRepository.findByYoutubeVideoId(100L)).thenReturn(metadata)

        val response = service.getVideo("video-100")

        assertThat(response.displayTitle).isEqualTo("표시 제목")
        assertThat(response.thumbnailUrl).isEqualTo("https://example.com/manual.jpg")
        assertThat(response.tags).containsExactly("messages", "grace")
    }

    @Test
    fun `getHome aggregates latest long form items without legacy site key assumptions`() {
        val sermonsMenu = menu(id = 1L, siteKey = "sermons-main", contentKind = ContentKind.LONG_FORM)
        val devotionMenu = menu(id = 2L, siteKey = "weekday-devotion", contentKind = ContentKind.LONG_FORM)
        val shortsMenu = menu(id = 3L, siteKey = "short-clips", contentKind = ContentKind.SHORT)
        val sermonsPlaylist = playlist(id = 10L, contentMenuId = 1L)
        val devotionPlaylist = playlist(id = 11L, contentMenuId = 2L)
        val sermonsVideo = video(
            id = 100L,
            youtubeVideoId = "video-100",
            title = "주일 예배",
            publishedAt = OffsetDateTime.of(2026, 4, 10, 5, 0, 0, 0, ZoneOffset.UTC),
        )
        val devotionVideo = video(
            id = 101L,
            youtubeVideoId = "video-101",
            title = "새벽 묵상",
            publishedAt = OffsetDateTime.of(2026, 4, 12, 5, 0, 0, 0, ZoneOffset.UTC),
        )
        val featuredMetadata = metadata(youtubeVideoId = 100L, featured = true)

        whenever(contentMenuRepository.findAllByActiveTrueOrderByIdAsc()).thenReturn(
            listOf(sermonsMenu, devotionMenu, shortsMenu),
        )
        whenever(contentMenuRepository.findBySiteKey("sermons-main")).thenReturn(sermonsMenu)
        whenever(contentMenuRepository.findBySiteKey("weekday-devotion")).thenReturn(devotionMenu)
        whenever(contentMenuRepository.findBySiteKey("short-clips")).thenReturn(shortsMenu)
        whenever(youtubePlaylistRepository.findByContentMenuIdAndSyncEnabledTrue(1L)).thenReturn(sermonsPlaylist)
        whenever(youtubePlaylistRepository.findByContentMenuIdAndSyncEnabledTrue(2L)).thenReturn(devotionPlaylist)
        whenever(youtubePlaylistRepository.findByContentMenuIdAndSyncEnabledTrue(3L)).thenReturn(null)
        whenever(playlistVideoRepository.findAllByYoutubePlaylistIdAndIsActiveTrueOrderByPositionAsc(10L)).thenReturn(
            listOf(playlistVideo(10L, 100L, 0)),
        )
        whenever(playlistVideoRepository.findAllByYoutubePlaylistIdAndIsActiveTrueOrderByPositionAsc(11L)).thenReturn(
            listOf(playlistVideo(11L, 101L, 0)),
        )
        whenever(youtubeVideoRepository.findAllById(listOf(100L))).thenReturn(listOf(sermonsVideo))
        whenever(youtubeVideoRepository.findAllById(listOf(101L))).thenReturn(listOf(devotionVideo))
        whenever(videoMetadataRepository.findAllByYoutubeVideoIdIn(listOf(100L))).thenReturn(listOf(featuredMetadata))
        whenever(videoMetadataRepository.findAllByYoutubeVideoIdIn(listOf(101L))).thenReturn(emptyList())

        val response = service.getHome()

        assertThat(response.latestSermons.map { it.menuSiteKey }).containsExactly("weekday-devotion", "sermons-main")
        assertThat(response.latestSermons.map { it.menuSlug }).containsExactly("weekday-devotion", "sermons-main")
        assertThat(response.featuredSermons.map { it.youtubeVideoId }).containsExactly("video-100")
    }

    private fun menu(id: Long, siteKey: String, contentKind: ContentKind): ContentMenu = ContentMenu(
        id = id,
        siteKey = siteKey,
        menuName = siteKey,
        slug = siteKey,
        contentKind = contentKind,
        active = true,
    )

    private fun playlist(id: Long, contentMenuId: Long): YoutubePlaylist = YoutubePlaylist(
        id = id,
        contentMenuId = contentMenuId,
        youtubePlaylistId = "PL_TEST",
        title = "테스트 재생목록",
        syncEnabled = true,
    )

    private fun playlistVideo(playlistId: Long, videoId: Long, position: Int): PlaylistVideo = PlaylistVideo(
        youtubePlaylistId = playlistId,
        youtubeVideoId = videoId,
        position = position,
        isActive = true,
    )

    private fun video(
        id: Long,
        youtubeVideoId: String,
        title: String,
        publishedAt: OffsetDateTime = OffsetDateTime.parse("2026-03-02T02:00:00Z"),
    ): YoutubeVideo = YoutubeVideo(
        id = id,
        youtubeVideoId = youtubeVideoId,
        title = title,
        description = "설명",
        publishedAt = publishedAt,
        thumbnailUrl = "https://example.com/default-thumb.jpg",
        detectedKind = ContentKind.LONG_FORM,
        youtubeWatchUrl = "https://youtube.com/watch?v=$youtubeVideoId",
        youtubeEmbedUrl = "https://youtube.com/embed/$youtubeVideoId",
    )

    private fun metadata(
        youtubeVideoId: Long,
        manualTitle: String? = null,
        manualThumbnailUrl: String? = null,
        manualPublishedDate: LocalDate? = null,
        manualKind: ContentKind? = null,
        featured: Boolean = false,
        tags: Array<String> = emptyArray(),
    ): VideoMetadata = VideoMetadata(
        youtubeVideoId = youtubeVideoId,
        manualTitle = manualTitle,
        manualThumbnailUrl = manualThumbnailUrl,
        manualPublishedDate = manualPublishedDate,
        manualKind = manualKind,
        featured = featured,
        tags = tags,
    )
}
