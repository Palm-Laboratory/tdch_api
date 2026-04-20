package kr.or.thejejachurch.api.board.infrastructure.persistence

import kr.or.thejejachurch.api.board.domain.PostAsset
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface PostAssetRepository : JpaRepository<PostAsset, Long> {
    fun findAllByPostIdOrderBySortOrderAscIdAsc(postId: Long): List<PostAsset>

    fun findAllByPostIdIsNullAndCreatedAtBefore(cutoff: OffsetDateTime): List<PostAsset>
}
