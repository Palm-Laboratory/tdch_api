package kr.or.thejejachurch.api.media.video.infrastructure.persistence

import kr.or.thejejachurch.api.media.video.domain.MediaVideoMeta
import org.springframework.data.jpa.repository.JpaRepository

interface MediaVideoMetaRepository : JpaRepository<MediaVideoMeta, Long> {
    fun findByVideoId(videoId: Long): MediaVideoMeta?
}
