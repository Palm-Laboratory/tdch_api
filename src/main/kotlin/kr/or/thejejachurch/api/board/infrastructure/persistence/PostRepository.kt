package kr.or.thejejachurch.api.board.infrastructure.persistence

import kr.or.thejejachurch.api.board.domain.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PostRepository : JpaRepository<Post, Long> {
    fun findAllByBoardIdOrderByCreatedAtDescIdDesc(boardId: Long): List<Post>
    fun findAllByBoardIdAndIsPublicTrueOrderByCreatedAtDescIdDesc(boardId: Long, pageable: Pageable): Page<Post>
    fun findByBoardIdAndIdAndIsPublicTrue(boardId: Long, id: Long): Post?
}
