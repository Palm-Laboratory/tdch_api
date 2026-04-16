package kr.or.thejejachurch.api.sermon.infrastructure.persistence

import kr.or.thejejachurch.api.sermon.domain.SermonVideoMeta
import org.springframework.data.jpa.repository.JpaRepository

interface SermonVideoMetaRepository : JpaRepository<SermonVideoMeta, Long> {
    fun findByVideoId(videoId: Long): SermonVideoMeta?
}
