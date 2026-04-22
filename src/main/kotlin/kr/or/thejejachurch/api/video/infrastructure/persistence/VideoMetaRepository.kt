package kr.or.thejejachurch.api.video.infrastructure.persistence

import kr.or.thejejachurch.api.video.domain.VideoMeta
import org.springframework.data.jpa.repository.JpaRepository

interface VideoMetaRepository : JpaRepository<VideoMeta, Long> {
    fun findByVideoId(videoId: Long): VideoMeta?
    fun findAllByVideoIdIn(videoIds: Collection<Long>): List<VideoMeta>
}
