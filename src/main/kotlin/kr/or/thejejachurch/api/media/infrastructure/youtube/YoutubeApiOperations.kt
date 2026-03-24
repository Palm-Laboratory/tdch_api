package kr.or.thejejachurch.api.media.infrastructure.youtube

interface YoutubeApiOperations {
    fun getPlaylistItems(
        playlistId: String,
        pageToken: String? = null,
        maxResults: Int = 50,
    ): YoutubePlaylistItemsPage

    fun getVideos(videoIds: Collection<String>): List<YoutubeVideoResource>
}
