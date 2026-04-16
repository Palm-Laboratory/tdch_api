package kr.or.thejejachurch.api.menu.infrastructure.persistence

import kr.or.thejejachurch.api.menu.domain.MenuItem
import kr.or.thejejachurch.api.menu.domain.MenuStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MenuItemRepository : JpaRepository<MenuItem, Long> {
    fun findAllByOrderBySortOrderAscIdAsc(): List<MenuItem>
    fun findAllByStatusOrderBySortOrderAscIdAsc(status: MenuStatus): List<MenuItem>
    fun findBySlug(slug: String): MenuItem?
    fun existsBySlugAndIdNot(slug: String, id: Long): Boolean
    fun findByParentIdAndSlug(parentId: Long, slug: String): MenuItem?

    @Query("select m from MenuItem m where m.parentId is null and m.slug = :slug")
    fun findRootBySlug(@Param("slug") slug: String): MenuItem?
}
