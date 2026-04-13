package kr.or.thejejachurch.api.media.infrastructure.persistence

import kr.or.thejejachurch.api.media.domain.MediaVideoMeta
import org.springframework.data.jpa.repository.JpaRepository

interface MediaVideoMetaRepository : JpaRepository<MediaVideoMeta, Long> {
    fun findByMediaVideoId(mediaVideoId: Long): MediaVideoMeta?
}
