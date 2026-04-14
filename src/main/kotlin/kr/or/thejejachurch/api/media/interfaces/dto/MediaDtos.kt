package kr.or.thejejachurch.api.media.interfaces.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
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

data class AdminPaginationDto(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class AdminPlaylistDto(
    val id: Long,
    val menuName: String,
    val siteKey: String,
    val slug: String,
    val contentKind: String,
    val active: Boolean,
    val youtubePlaylistId: String,
    val itemCount: Int,
    val syncEnabled: Boolean,
    val lastSyncedAt: String? = null,
)

data class AdminPlaylistListResponse(
    val data: List<AdminPlaylistDto>,
    val pagination: AdminPaginationDto,
)

data class AdminPlaylistDetailDto(
    val id: Long,
    val menuName: String,
    val siteKey: String,
    val slug: String,
    val contentKind: String,
    val active: Boolean,
    val youtubePlaylistId: String,
    val youtubeTitle: String,
    val youtubeDescription: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val itemCount: Int,
    val syncEnabled: Boolean,
    val lastSyncedAt: String? = null,
)

data class AdminVideoDto(
    val youtubeVideoId: String,
    val position: Int,
    val visible: Boolean,
    val featured: Boolean,
    val displayTitle: String,
    val displayThumbnailUrl: String,
    val displayPublishedDate: String,
    val originalTitle: String,
    val publishedAt: String,
    val thumbnailUrl: String,
    val preacher: String? = null,
    val scripture: String? = null,
    val pinnedRank: Int? = null,
)

data class AdminVideoListResponse(
    val data: List<AdminVideoDto>,
    val pagination: AdminPaginationDto,
)

data class AdminVideoMetadataDto(
    val youtubeVideoId: String,
    val originalTitle: String,
    val originalDescription: String,
    val publishedAt: String,
    val watchUrl: String,
    val embedUrl: String,
    val lastSyncedAt: String? = null,
    val visible: Boolean,
    val featured: Boolean,
    val pinnedRank: Int? = null,
    val manualTitle: String? = null,
    val manualThumbnailUrl: String? = null,
    val manualPublishedDate: String? = null,
    val manualKind: String? = null,
    val preacher: String? = null,
    val scripture: String? = null,
    val scriptureBody: String? = null,
    val serviceType: String? = null,
    val summary: String? = null,
    val tags: List<String> = emptyList(),
)

data class UpdatePlaylistRequest(
    @field:NotBlank(message = "menuName must not be blank")
    val menuName: String,
    @field:NotBlank(message = "slug must not be blank")
    val slug: String,
    val syncEnabled: Boolean,
    val active: Boolean,
)

data class UpdateVideoMetadataRequest(
    val visible: Boolean,
    val featured: Boolean,
    val pinnedRank: Int? = null,
    @field:Size(max = 255, message = "manualTitle must be 255 characters or less")
    val manualTitle: String? = null,
    val manualThumbnailUrl: String? = null,
    val manualPublishedDate: String? = null,
    val manualKind: String? = null,
    @field:Size(max = 120, message = "preacher must be 120 characters or less")
    val preacher: String? = null,
    val scripture: String? = null,
    val scriptureBody: String? = null,
    @field:Size(max = 100, message = "serviceType must be 100 characters or less")
    val serviceType: String? = null,
    val summary: String? = null,
    val tags: List<String> = emptyList(),
)
