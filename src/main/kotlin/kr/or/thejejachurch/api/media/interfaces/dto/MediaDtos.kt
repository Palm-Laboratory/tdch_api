package kr.or.thejejachurch.api.media.interfaces.dto

import java.time.OffsetDateTime

data class MenuDto(
    val siteKey: String,
    val name: String,
    val slug: String,
    val contentKind: String,
)

data class MediaItemDto(
    val youtubeVideoId: String,
    val title: String,
    val displayTitle: String,
    val thumbnailUrl: String,
    val youtubeUrl: String,
    val embedUrl: String,
    val publishedAt: String,
    val displayDate: String,
    val contentKind: String,
    val preacher: String? = null,
    val scripture: String? = null,
    val serviceType: String? = null,
    val featured: Boolean = false,
)

data class MediaListResponse(
    val menu: MenuDto,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val items: List<MediaItemDto>,
)

data class HomeMediaResponse(
    val featuredSermons: List<MediaItemDto>,
    val latestMessages: List<MediaItemDto>,
    val latestDevotions: List<MediaItemDto>,
    val latestShorts: List<MediaItemDto>,
)

data class VideoDetailResponse(
    val youtubeVideoId: String,
    val title: String,
    val displayTitle: String,
    val description: String,
    val thumbnailUrl: String,
    val youtubeUrl: String,
    val embedUrl: String,
    val contentKind: String,
    val publishedAt: String,
    val preacher: String? = null,
    val scripture: String? = null,
    val scriptureBody: String? = null,
    val serviceType: String? = null,
    val summary: String? = null,
    val tags: List<String> = emptyList(),
)

data class AdminMediaSyncResponse(
    val status: String,
    val totalPlaylists: Int,
    val succeededPlaylists: Int,
    val failedPlaylists: Int,
    val completedAt: OffsetDateTime,
)
