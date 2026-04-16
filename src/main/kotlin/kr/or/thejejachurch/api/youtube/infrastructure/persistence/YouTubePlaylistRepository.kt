package kr.or.thejejachurch.api.youtube.infrastructure.persistence

import kr.or.thejejachurch.api.youtube.domain.YouTubePlaylist
import org.springframework.data.jpa.repository.JpaRepository

interface YouTubePlaylistRepository : JpaRepository<YouTubePlaylist, Long> {
    fun findByPlaylistId(playlistId: String): YouTubePlaylist?
    fun findAllByChannelId(channelId: Long): List<YouTubePlaylist>
}
