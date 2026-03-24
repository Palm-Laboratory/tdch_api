package kr.or.thejejachurch.api.media.infrastructure.persistence

import kr.or.thejejachurch.api.media.domain.YoutubeVideo
import org.springframework.data.jpa.repository.JpaRepository

interface YoutubeVideoRepository : JpaRepository<YoutubeVideo, Long> {
    fun findByYoutubeVideoId(youtubeVideoId: String): YoutubeVideo?
    fun findAllByYoutubeVideoIdIn(youtubeVideoIds: Collection<String>): List<YoutubeVideo>
}
