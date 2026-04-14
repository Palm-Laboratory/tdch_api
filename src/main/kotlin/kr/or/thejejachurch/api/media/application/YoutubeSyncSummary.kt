package kr.or.thejejachurch.api.media.application

data class YoutubeSyncSummary(
    val totalPlaylists: Int,
    val succeededPlaylists: Int,
    val failedPlaylists: Int,
)
