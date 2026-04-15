package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.common.error.NotFoundException
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
import kr.or.thejejachurch.api.media.interfaces.dto.VideoDetailResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.lang.reflect.InvocationTargetException
import java.time.OffsetDateTime

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

    @Test
    fun `getVideo returns detail only when slug owns the requested video through its synced playlist`() {
        val menu = contentMenu(
            id = 11L,
            siteKey = "messages",
            slug = "public-sermon-2024",
        )
        val playlist = YoutubePlaylist(
            id = 21L,
            contentMenuId = 11L,
            youtubePlaylistId = "PL_1",
            title = "예배 영상",
        )
        val video = youtubeVideo(
            id = 31L,
            youtubeVideoId = "YT_1",
        )
        val metadata = videoMetadata(videoId = 31L)

        whenever(contentMenuRepository.findBySlug(menu.slug)).thenReturn(menu)
        whenever(youtubePlaylistRepository.findByContentMenuIdAndSyncEnabledTrue(menu.id!!)).thenReturn(playlist)
        whenever(
            playlistVideoRepository.findByYoutubePlaylistIdAndYoutubeVideoId(
                playlist.id!!,
                video.id!!,
            ),
        ).thenReturn(
            PlaylistVideo(
                youtubePlaylistId = playlist.id!!,
                youtubeVideoId = video.id!!,
                position = 1,
                isActive = true,
            ),
        )
        whenever(youtubeVideoRepository.findByYoutubeVideoId(video.youtubeVideoId)).thenReturn(video)
        whenever(videoMetadataRepository.findByYoutubeVideoId(video.id!!)).thenReturn(metadata)

        val response = invokeOwnershipAwareVideoDetail(menu.slug, video.youtubeVideoId)

        assertThat(response.youtubeVideoId).isEqualTo(video.youtubeVideoId)
        assertThat(response.displayTitle).isEqualTo("새벽예배")
        verify(playlistVideoRepository).findByYoutubePlaylistIdAndYoutubeVideoId(playlist.id!!, video.id!!)
    }

    @Test
    fun `getVideo throws NotFound when slug exists but the video is not in that menu's active playlist`() {
        val menu = contentMenu(
            id = 11L,
            siteKey = "messages",
            slug = "public-sermon-2024",
        )
        val playlist = YoutubePlaylist(
            id = 21L,
            contentMenuId = 11L,
            youtubePlaylistId = "PL_1",
            title = "예배 영상",
        )
        val video = youtubeVideo(
            id = 31L,
            youtubeVideoId = "YT_2",
        )

        whenever(contentMenuRepository.findBySlug(menu.slug)).thenReturn(menu)
        whenever(youtubePlaylistRepository.findByContentMenuIdAndSyncEnabledTrue(menu.id!!)).thenReturn(playlist)
        whenever(
            playlistVideoRepository.findByYoutubePlaylistIdAndYoutubeVideoId(
                playlist.id!!,
                video.id!!,
            ),
        ).thenReturn(null)
        whenever(youtubeVideoRepository.findByYoutubeVideoId(video.youtubeVideoId)).thenReturn(video)

        assertThatThrownBy {
            invokeOwnershipAwareVideoDetail(menu.slug, video.youtubeVideoId)
        }
            .isInstanceOf(NotFoundException::class.java)

        verify(playlistVideoRepository).findByYoutubePlaylistIdAndYoutubeVideoId(playlist.id!!, video.id!!)
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

    private fun youtubeVideo(
        id: Long,
        youtubeVideoId: String,
    ): YoutubeVideo = YoutubeVideo(
        id = id,
        youtubeVideoId = youtubeVideoId,
        title = "설교 제목",
        description = "설교 설명",
        publishedAt = OffsetDateTime.parse("2024-01-01T10:00:00Z"),
        channelId = "CHANNEL_1",
        channelTitle = "TDCH",
        thumbnailUrl = "https://example.com/thumb.jpg",
        detectedKind = ContentKind.LONG_FORM,
        youtubeWatchUrl = "https://youtu.be/$youtubeVideoId",
        youtubeEmbedUrl = "https://www.youtube.com/embed/$youtubeVideoId",
    )

    private fun videoMetadata(videoId: Long): VideoMetadata = VideoMetadata(
        youtubeVideoId = videoId,
        manualTitle = "새벽예배",
        manualThumbnailUrl = "https://example.com/manual-thumb.jpg",
        preacher = "김목사",
    )

    private fun invokeOwnershipAwareVideoDetail(
        slug: String,
        youtubeVideoId: String,
    ): VideoDetailResponse {
        val method = service::class.java.methods.firstOrNull { candidate ->
            candidate.name == "getVideo" &&
                candidate.parameterTypes.contentEquals(
                    arrayOf(String::class.java, String::class.java),
                )
        } ?: throw AssertionError("Expected an ownership-aware getVideo(slug, youtubeVideoId) overload")

        return try {
            method.invoke(service, slug, youtubeVideoId) as VideoDetailResponse
        } catch (error: InvocationTargetException) {
            throw (error.targetException as? RuntimeException ?: error)
        }
    }
}
