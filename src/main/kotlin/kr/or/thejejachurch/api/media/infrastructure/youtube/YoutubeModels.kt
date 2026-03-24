package kr.or.thejejachurch.api.media.infrastructure.youtube

import kr.or.thejejachurch.api.media.domain.ContentKind
import java.time.OffsetDateTime

data class YoutubePlaylistItemsPage(
    val nextPageToken: String?,
    val items: List<YoutubePlaylistItem>,
)

data class YoutubePlaylistItem(
    val videoId: String,
    val position: Int,
    val addedToPlaylistAt: OffsetDateTime?,
    val videoPublishedAt: OffsetDateTime?,
)

data class YoutubeVideoResource(
    val videoId: String,
    val title: String,
    val description: String?,
    val publishedAt: OffsetDateTime,
    val channelId: String?,
    val channelTitle: String?,
    val thumbnailUrl: String?,
    val durationSeconds: Int?,
    val privacyStatus: String?,
    val uploadStatus: String?,
    val embeddable: Boolean,
    val madeForKids: Boolean?,
    val detectedKind: ContentKind,
    val youtubeWatchUrl: String,
    val youtubeEmbedUrl: String,
    val rawPayload: String,
)
