package kr.or.thejejachurch.api.board.infrastructure.persistence

import kr.or.thejejachurch.api.board.domain.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<Post, Long> {
    fun findAllByBoardIdOrderByCreatedAtDescIdDesc(boardId: Long): List<Post>
    fun findAllByBoardIdAndMenuIdOrderByCreatedAtDescIdDesc(boardId: Long, menuId: Long): List<Post>
    fun findAllByBoardIdAndIsPublicTrueOrderByCreatedAtDescIdDesc(boardId: Long, pageable: Pageable): Page<Post>
    fun findAllByMenuIdAndIsPublicTrueOrderByCreatedAtDescIdDesc(menuId: Long, pageable: Pageable): Page<Post>
    fun findByBoardIdAndIdAndIsPublicTrue(boardId: Long, id: Long): Post?
    fun findByMenuIdAndIdAndIsPublicTrue(menuId: Long, id: Long): Post?

    @Modifying
    @Query("update Post post set post.boardId = :boardId where post.menuId = :menuId")
    fun updateBoardIdByMenuId(@Param("menuId") menuId: Long, @Param("boardId") boardId: Long): Int
}
