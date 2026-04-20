package kr.or.thejejachurch.api.board.infrastructure.persistence

import kr.or.thejejachurch.api.board.domain.PostAsset
import org.springframework.data.jpa.repository.JpaRepository

interface PostAssetRepository : JpaRepository<PostAsset, Long>
