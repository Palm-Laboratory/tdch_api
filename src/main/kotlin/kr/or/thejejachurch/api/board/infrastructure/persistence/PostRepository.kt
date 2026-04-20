package kr.or.thejejachurch.api.board.infrastructure.persistence

import kr.or.thejejachurch.api.board.domain.Post
import org.springframework.data.jpa.repository.JpaRepository

interface PostRepository : JpaRepository<Post, Long> {
    fun findAllByBoardIdOrderByCreatedAtDescIdDesc(boardId: Long): List<Post>
}
