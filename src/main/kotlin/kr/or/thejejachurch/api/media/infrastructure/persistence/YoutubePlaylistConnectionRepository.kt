package kr.or.thejejachurch.api.media.infrastructure.persistence

import kr.or.thejejachurch.api.media.domain.YoutubePlaylistConnection
import org.springframework.data.jpa.repository.JpaRepository

interface YoutubePlaylistConnectionRepository : JpaRepository<YoutubePlaylistConnection, Long> {
    fun findAllByMediaCollectionIdOrderByIdAsc(mediaCollectionId: Long): List<YoutubePlaylistConnection>
    fun findByYoutubePlaylistId(youtubePlaylistId: String): YoutubePlaylistConnection?
}
