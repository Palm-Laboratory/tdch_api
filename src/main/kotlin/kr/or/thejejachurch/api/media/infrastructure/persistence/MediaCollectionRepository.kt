package kr.or.thejejachurch.api.media.infrastructure.persistence

import kr.or.thejejachurch.api.media.domain.MediaCollection
import org.springframework.data.jpa.repository.JpaRepository

interface MediaCollectionRepository : JpaRepository<MediaCollection, Long> {
    fun findAllByActiveTrueOrderBySortOrderAscIdAsc(): List<MediaCollection>
    fun findByCollectionKey(collectionKey: String): MediaCollection?
}
