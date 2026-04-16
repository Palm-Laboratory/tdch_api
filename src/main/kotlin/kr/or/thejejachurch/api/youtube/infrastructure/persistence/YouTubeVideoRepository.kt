package kr.or.thejejachurch.api.youtube.infrastructure.persistence

import kr.or.thejejachurch.api.youtube.domain.YouTubeVideo
import org.springframework.data.jpa.repository.JpaRepository

interface YouTubeVideoRepository : JpaRepository<YouTubeVideo, Long> {
    fun findByVideoId(videoId: String): YouTubeVideo?
    fun findAllByChannelIdOrderByPublishedAtDesc(channelId: Long): List<YouTubeVideo>
    fun findAllByChannelId(channelId: Long): List<YouTubeVideo>
}
