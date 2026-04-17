package kr.or.thejejachurch.api.media.video.infrastructure.persistence

import kr.or.thejejachurch.api.media.video.domain.VideoMeta
import org.springframework.data.jpa.repository.JpaRepository

interface VideoMetaRepository : JpaRepository<VideoMeta, Long> {
    fun findByVideoId(videoId: Long): VideoMeta?
}
