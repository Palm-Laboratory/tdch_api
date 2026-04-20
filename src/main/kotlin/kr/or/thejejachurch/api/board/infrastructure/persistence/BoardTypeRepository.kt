package kr.or.thejejachurch.api.board.infrastructure.persistence

import kr.or.thejejachurch.api.board.domain.BoardTypeDefinition
import org.springframework.data.jpa.repository.JpaRepository

interface BoardTypeRepository : JpaRepository<BoardTypeDefinition, Long> {
    fun findByKey(key: String): BoardTypeDefinition?
    fun findAllByOrderBySortOrderAscIdAsc(): List<BoardTypeDefinition>
}
