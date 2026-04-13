package kr.or.thejejachurch.api.media.infrastructure.persistence

import kr.or.thejejachurch.api.media.domain.MediaCollectionVideo
import org.springframework.data.jpa.repository.JpaRepository

interface MediaCollectionVideoRepository : JpaRepository<MediaCollectionVideo, Long> {
    fun findAllByMediaCollectionIdOrderBySortOrderAscIdAsc(mediaCollectionId: Long): List<MediaCollectionVideo>
    fun findByMediaCollectionIdAndMediaVideoId(mediaCollectionId: Long, mediaVideoId: Long): MediaCollectionVideo?
}
