package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.media.interfaces.dto.HomeMediaResponse
import kr.or.thejejachurch.api.media.interfaces.dto.MediaItemDto
import kr.or.thejejachurch.api.media.interfaces.dto.MediaListResponse
import kr.or.thejejachurch.api.media.interfaces.dto.MenuDto
import kr.or.thejejachurch.api.media.interfaces.dto.VideoDetailResponse
import org.springframework.stereotype.Service

@Service
class MediaQueryService {

    private val menus = listOf(
        MenuDto(siteKey = "messages", name = "말씀/설교", slug = "messages", contentKind = "LONG_FORM"),
        MenuDto(siteKey = "better-devotion", name = "더 좋은 묵상", slug = "better-devotion", contentKind = "LONG_FORM"),
        MenuDto(siteKey = "its-okay", name = "그래도 괜찮아", slug = "its-okay", contentKind = "SHORT"),
    )

    private val sampleItemsByMenu = mapOf(
        "messages" to listOf(
            MediaItemDto(
                youtubeVideoId = "abc123xyz",
                title = "목마름을 채우시는 사랑",
                displayTitle = "목마름을 채우시는 사랑",
                thumbnailUrl = "https://i.ytimg.com/vi/abc123xyz/hqdefault.jpg",
                youtubeUrl = "https://www.youtube.com/watch?v=abc123xyz",
                embedUrl = "https://www.youtube.com/embed/abc123xyz",
                publishedAt = "2026-03-02T02:00:00Z",
                displayDate = "2026-03-02",
                contentKind = "LONG_FORM",
                preacher = "이진욱 목사",
                scripture = "요한복음 4:1~42",
                serviceType = "주일예배",
                featured = true,
            ),
        ),
        "better-devotion" to listOf(
            MediaItemDto(
                youtubeVideoId = "devotion001",
                title = "아침을 여는 묵상",
                displayTitle = "아침을 여는 묵상",
                thumbnailUrl = "https://i.ytimg.com/vi/devotion001/hqdefault.jpg",
                youtubeUrl = "https://www.youtube.com/watch?v=devotion001",
                embedUrl = "https://www.youtube.com/embed/devotion001",
                publishedAt = "2026-03-03T23:00:00Z",
                displayDate = "2026-03-04",
                contentKind = "LONG_FORM",
                preacher = "이진욱 목사",
                scripture = "시편 23:1~6",
                serviceType = "묵상",
            ),
        ),
        "its-okay" to listOf(
            MediaItemDto(
                youtubeVideoId = "short001",
                title = "그래도 괜찮아 1화",
                displayTitle = "그래도 괜찮아 1화",
                thumbnailUrl = "https://i.ytimg.com/vi/short001/hqdefault.jpg",
                youtubeUrl = "https://www.youtube.com/shorts/short001",
                embedUrl = "https://www.youtube.com/embed/short001",
                publishedAt = "2026-03-05T03:00:00Z",
                displayDate = "2026-03-05",
                contentKind = "SHORT",
            ),
        ),
    )

    fun getMenus(): List<MenuDto> = menus

    fun getHome(): HomeMediaResponse = HomeMediaResponse(
        featuredSermons = sampleItemsByMenu["messages"].orEmpty().filter { it.featured },
        latestMessages = sampleItemsByMenu["messages"].orEmpty(),
        latestDevotions = sampleItemsByMenu["better-devotion"].orEmpty(),
        latestShorts = sampleItemsByMenu["its-okay"].orEmpty(),
    )

    fun getVideos(siteKey: String, page: Int, size: Int): MediaListResponse {
        val menu = menus.firstOrNull { it.siteKey == siteKey }
            ?: throw NotFoundException("Unknown siteKey: $siteKey")
        val items = sampleItemsByMenu[siteKey].orEmpty()
        return MediaListResponse(
            menu = menu,
            page = page,
            size = size,
            totalElements = items.size.toLong(),
            totalPages = if (items.isEmpty()) 0 else 1,
            items = items.take(size),
        )
    }

    fun getVideo(youtubeVideoId: String): VideoDetailResponse {
        val item = sampleItemsByMenu.values.flatten().firstOrNull { it.youtubeVideoId == youtubeVideoId }
            ?: throw NotFoundException("Unknown youtubeVideoId: $youtubeVideoId")
        return VideoDetailResponse(
            youtubeVideoId = item.youtubeVideoId,
            title = item.title,
            displayTitle = item.displayTitle,
            description = "초기 부트스트랩 단계의 샘플 응답입니다. 이후 DB 조회로 대체됩니다.",
            thumbnailUrl = item.thumbnailUrl,
            youtubeUrl = item.youtubeUrl,
            embedUrl = item.embedUrl,
            contentKind = item.contentKind,
            publishedAt = item.publishedAt,
            preacher = item.preacher,
            scripture = item.scripture,
            serviceType = item.serviceType,
            summary = "이 필드는 video_metadata.summary 로 옮겨질 예정입니다.",
            tags = listOf(item.contentKind.lowercase()),
        )
    }
}
