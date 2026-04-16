package kr.or.thejejachurch.api.youtube.infrastructure.persistence

import kr.or.thejejachurch.api.youtube.domain.YouTubeChannel
import org.springframework.data.jpa.repository.JpaRepository

interface YouTubeChannelRepository : JpaRepository<YouTubeChannel, Long> {
    fun findByChannelId(channelId: String): YouTubeChannel?
}
