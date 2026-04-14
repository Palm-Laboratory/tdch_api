package kr.or.thejejachurch.api.media.infrastructure.youtube

data class YoutubeChannelPlaylistsPage(
    val nextPageToken: String?,
    val items: List<YoutubeChannelPlaylistResource>,
)

data class YoutubeChannelPlaylistResource(
    val youtubePlaylistId: String,
    val title: String,
    val description: String?,
    val channelId: String?,
    val channelTitle: String?,
    val thumbnailUrl: String?,
    val itemCount: Int?,
)
