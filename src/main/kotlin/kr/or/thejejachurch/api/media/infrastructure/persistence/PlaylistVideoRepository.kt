package kr.or.thejejachurch.api.media.infrastructure.persistence

import kr.or.thejejachurch.api.media.domain.PlaylistVideo
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PlaylistVideoRepository : JpaRepository<PlaylistVideo, Long> {
    fun findByYoutubePlaylistIdAndYoutubeVideoId(youtubePlaylistId: Long, youtubeVideoId: Long): PlaylistVideo?
    fun findAllByYoutubePlaylistIdOrderByPositionAsc(youtubePlaylistId: Long): List<PlaylistVideo>
    fun findAllByYoutubePlaylistIdAndIsActiveTrueOrderByPositionAsc(youtubePlaylistId: Long): List<PlaylistVideo>
    fun findAllByYoutubePlaylistIdAndIsActiveTrueOrderByPositionAsc(youtubePlaylistId: Long, pageable: Pageable): Page<PlaylistVideo>
}
