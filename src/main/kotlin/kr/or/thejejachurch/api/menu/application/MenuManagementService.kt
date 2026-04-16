package kr.or.thejejachurch.api.menu.application

import com.fasterxml.jackson.databind.ObjectMapper
import kr.or.thejejachurch.api.adminaccount.infrastructure.persistence.AdminAccountRepository
import kr.or.thejejachurch.api.common.error.ForbiddenException
import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.menu.domain.MenuItem
import kr.or.thejejachurch.api.menu.domain.MenuRevision
import kr.or.thejejachurch.api.menu.domain.MenuStatus
import kr.or.thejejachurch.api.menu.domain.MenuType
import kr.or.thejejachurch.api.menu.infrastructure.persistence.MenuItemRepository
import kr.or.thejejachurch.api.menu.infrastructure.persistence.MenuRevisionRepository
import kr.or.thejejachurch.api.youtube.infrastructure.persistence.YouTubePlaylistRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MenuManagementService(
    private val menuItemRepository: MenuItemRepository,
    private val menuRevisionRepository: MenuRevisionRepository,
    private val adminAccountRepository: AdminAccountRepository,
    private val youTubePlaylistRepository: YouTubePlaylistRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
    fun getAdminSnapshot(actorId: Long): AdminMenuSnapshot {
        requireActiveAdmin(actorId)
        return AdminMenuSnapshot(items = buildAdminTree())
    }

    @Transactional
    fun replaceTree(actorId: Long, items: List<MenuTreeNodeInput>): AdminMenuSnapshot {
        requireActiveAdmin(actorId)

        val existingItems = menuItemRepository.findAllByOrderBySortOrderAscIdAsc()
        val existingById = existingItems.associateBy { it.id!! }
        val seenIds = linkedSetOf<Long>()

        validateInputTree(items)
        val persistedRoots = persistNodes(
            nodes = items,
            parentId = null,
            depth = 0,
            parentType = null,
            existingById = existingById,
            seenIds = seenIds,
        )

        val removedItems = existingItems.filter { it.id !in seenIds }
        if (removedItems.any { it.isAuto }) {
            throw IllegalArgumentException("자동 생성 메뉴는 트리에서 제거할 수 없습니다.")
        }

        val removedIds = removedItems.mapNotNull { it.id }.toSet()
        val rootRemovedIds = removedItems
            .filter { it.parentId == null || it.parentId !in removedIds }
            .mapNotNull { it.id }
        if (rootRemovedIds.isNotEmpty()) {
            menuItemRepository.deleteAllByIdInBatch(rootRemovedIds)
        }

        val allPersisted = menuItemRepository.findAllByOrderBySortOrderAscIdAsc()
        recomputePaths(allPersisted)

        val snapshot = AdminMenuSnapshot(items = buildAdminTree())
        menuRevisionRepository.save(
            MenuRevision(
                snapshot = objectMapper.writeValueAsString(snapshot),
                summary = "메뉴 트리 일괄 저장",
                createdBy = actorId,
            )
        )

        return snapshot
    }

    @Transactional
    fun deleteMenuItem(actorId: Long, menuId: Long) {
        requireActiveAdmin(actorId)

        val menu = menuItemRepository.findByIdOrNull(menuId)
            ?: throw NotFoundException("메뉴를 찾을 수 없습니다. id=$menuId")

        if (menu.isAuto) {
            throw IllegalArgumentException("자동 생성 메뉴는 수동 삭제할 수 없습니다.")
        }

        menuItemRepository.delete(menu)

        val remaining = menuItemRepository.findAllByOrderBySortOrderAscIdAsc()
        recomputePaths(remaining)
    }

    private fun validateInputTree(nodes: List<MenuTreeNodeInput>) {
        fun visitSiblings(siblings: List<MenuTreeNodeInput>, depth: Int, parentType: MenuType?) {
            val siblingSlugs = linkedSetOf<String>()

            siblings.forEach { node ->
                val normalizedSlug = normalizeSlug(node.slug, node.label)
                if (!siblingSlugs.add(normalizedSlug)) {
                    throw IllegalArgumentException("같은 메뉴 아래에 중복된 slug가 있습니다: $normalizedSlug")
                }

                if (node.status == MenuStatus.ARCHIVED && !node.isAuto) {
                    throw IllegalArgumentException("ARCHIVED 상태는 자동 유튜브 메뉴에만 사용할 수 있습니다.")
                }

                validatePlacement(
                    type = node.type,
                    depth = depth,
                    parentType = parentType,
                    isAuto = node.isAuto,
                    status = node.status,
                    hasChildren = node.children.isNotEmpty(),
                )

                visitSiblings(node.children, depth + 1, node.type)
            }
        }

        visitSiblings(nodes, depth = 0, parentType = null)
    }

    private fun persistNodes(
        nodes: List<MenuTreeNodeInput>,
        parentId: Long?,
        depth: Int,
        parentType: MenuType?,
        existingById: Map<Long, MenuItem>,
        seenIds: MutableSet<Long>,
    ): List<MenuItem> =
        nodes.mapIndexed { index, node ->
            val item = node.id?.let(existingById::get)
                ?: MenuItem(
                    type = node.type,
                    label = node.label.trim(),
                    slug = normalizeSlug(node.slug, node.label),
                )

            val menuId = item.id
            if (menuId != null && !seenIds.add(menuId)) {
                throw IllegalArgumentException("동일한 메뉴가 중복 포함되어 있습니다. id=$menuId")
            }

            if (item.isAuto && node.type != item.type) {
                throw IllegalArgumentException("자동 생성 메뉴의 타입은 변경할 수 없습니다.")
            }

            val normalizedLabel = node.label.trim()
            if (normalizedLabel.isBlank()) {
                throw IllegalArgumentException("메뉴 이름은 비어 있을 수 없습니다.")
            }

            val normalizedSlug = normalizeSlug(node.slug, node.label)
            val effectiveSlug = if (item.id == null) normalizedSlug else item.slug
            ensureSlugAvailable(effectiveSlug, item.id, parentId)

            val resolvedType = if (item.isAuto) item.type else node.type
            validatePlacement(
                type = resolvedType,
                depth = depth,
                parentType = parentType,
                isAuto = item.isAuto,
                status = node.status,
                hasChildren = node.children.isNotEmpty(),
            )

            item.parentId = parentId
            item.type = resolvedType
            item.status = normalizeStatus(node, item, parentId)
            item.label = normalizedLabel
            item.slug = effectiveSlug
            item.sortOrder = index
            item.depth = depth
            item.path = ""
            item.openInNewTab = node.openInNewTab

            when (item.type) {
                MenuType.STATIC -> {
                    val staticPageKey = node.staticPageKey?.trim()
                    if (staticPageKey.isNullOrBlank()) {
                        throw IllegalArgumentException("정적 페이지 메뉴는 staticPageKey가 필요합니다.")
                    }
                    if (staticPageKey !in MenuRouteRegistry.allStaticPageKeys()) {
                        throw IllegalArgumentException("지원하지 않는 staticPageKey 입니다: $staticPageKey")
                    }
                    item.staticPageKey = staticPageKey
                    item.boardKey = null
                    item.externalUrl = null
                    item.openInNewTab = false
                }

                MenuType.BOARD -> {
                    val boardKey = node.boardKey?.trim()
                    if (boardKey.isNullOrBlank()) {
                        throw IllegalArgumentException("게시판 메뉴는 boardKey가 필요합니다.")
                    }
                    item.boardKey = boardKey
                    item.staticPageKey = null
                    item.externalUrl = null
                    item.openInNewTab = false
                }

                MenuType.EXTERNAL_LINK -> {
                    val externalUrl = node.externalUrl?.trim()
                    if (externalUrl.isNullOrBlank()) {
                        throw IllegalArgumentException("외부 링크 메뉴는 URL이 필요합니다.")
                    }
                    item.externalUrl = externalUrl
                    item.staticPageKey = null
                    item.boardKey = null
                }

                MenuType.FOLDER,
                MenuType.YOUTUBE_PLAYLIST_GROUP -> {
                    item.staticPageKey = null
                    item.boardKey = null
                    item.externalUrl = null
                    item.openInNewTab = false
                }

                MenuType.YOUTUBE_PLAYLIST -> {
                    if (!item.isAuto) {
                        throw IllegalArgumentException("유튜브 재생목록 메뉴는 동기화로만 생성할 수 있습니다.")
                    }
                    item.staticPageKey = null
                    item.boardKey = null
                    item.externalUrl = null
                    item.openInNewTab = false
                    val sourceTitle = item.playlistId?.let { playlistId ->
                        youTubePlaylistRepository.findByIdOrNull(playlistId)?.title
                    }
                    item.labelCustomized = sourceTitle?.let { normalizedLabel != it } ?: false
                }
            }

            val saved = menuItemRepository.save(item)
            if (saved.id != null) {
                seenIds.add(saved.id)
            }
            persistNodes(
                nodes = node.children,
                parentId = saved.id,
                depth = depth + 1,
                parentType = saved.type,
                existingById = existingById + (saved.id!! to saved),
                seenIds = seenIds,
            )
            saved
        }

    private fun normalizeStatus(node: MenuTreeNodeInput, existing: MenuItem, parentId: Long?): MenuStatus {
        if (existing.isAuto && existing.status == MenuStatus.ARCHIVED && node.status != MenuStatus.ARCHIVED) {
            throw IllegalArgumentException("ARCHIVED 상태는 유튜브 동기화로만 해제됩니다.")
        }

        if (existing.type == MenuType.YOUTUBE_PLAYLIST && node.status == MenuStatus.PUBLISHED && parentId == null) {
            throw IllegalArgumentException("유튜브 재생목록은 그룹을 지정한 뒤에만 노출할 수 있습니다.")
        }

        return when {
            existing.isAuto -> node.status
            node.status == MenuStatus.ARCHIVED -> throw IllegalArgumentException("ARCHIVED 상태는 자동 유튜브 메뉴에만 사용할 수 있습니다.")
            else -> node.status
        }
    }

    private fun validatePlacement(
        type: MenuType,
        depth: Int,
        parentType: MenuType?,
        isAuto: Boolean,
        status: MenuStatus,
        hasChildren: Boolean,
    ) {
        if (depth > 1) {
            throw IllegalArgumentException("메뉴는 GNB/LNB 2단계까지만 구성할 수 있습니다.")
        }

        when (depth) {
            0 -> when (type) {
                MenuType.FOLDER,
                MenuType.YOUTUBE_PLAYLIST_GROUP -> Unit

                MenuType.YOUTUBE_PLAYLIST -> {
                    if (!isAuto) {
                        throw IllegalArgumentException("유튜브 재생목록 메뉴는 동기화로만 생성할 수 있습니다.")
                    }
                    if (status == MenuStatus.PUBLISHED) {
                        throw IllegalArgumentException("유튜브 재생목록은 그룹을 지정한 뒤에만 노출할 수 있습니다.")
                    }
                }

                else -> throw IllegalArgumentException("GNB는 일반 메뉴 그룹 또는 영상 그룹으로만 만들 수 있습니다.")
            }

            1 -> when (parentType) {
                MenuType.FOLDER -> {
                    if (type !in setOf(MenuType.STATIC, MenuType.BOARD, MenuType.EXTERNAL_LINK)) {
                        throw IllegalArgumentException("일반 메뉴 그룹 아래에는 정적 페이지, 게시판, 외부 링크만 둘 수 있습니다.")
                    }
                }

                MenuType.YOUTUBE_PLAYLIST_GROUP -> {
                    if (type != MenuType.YOUTUBE_PLAYLIST) {
                        throw IllegalArgumentException("영상 그룹 아래에는 유튜브 재생목록만 둘 수 있습니다.")
                    }
                }

                else -> throw IllegalArgumentException("하위 메뉴를 둘 수 없는 상위 타입입니다.")
            }
        }

        if (type in setOf(MenuType.STATIC, MenuType.BOARD, MenuType.EXTERNAL_LINK, MenuType.YOUTUBE_PLAYLIST) && hasChildren) {
            throw IllegalArgumentException("링크 메뉴는 하위 메뉴를 가질 수 없습니다.")
        }
    }

    private fun recomputePaths(items: List<MenuItem>) {
        val itemsByParent = items.groupBy { it.parentId }

        fun visit(parentId: Long?, parentPath: String, depth: Int) {
            itemsByParent[parentId]
                .orEmpty()
                .sortedWith(compareBy<MenuItem> { it.sortOrder }.thenBy { it.id })
                .forEach { item ->
                    val itemId = item.id ?: return@forEach
                    item.depth = depth
                    item.path = "$parentPath$itemId/"
                    menuItemRepository.save(item)
                    visit(itemId, item.path, depth + 1)
                }
        }

        visit(parentId = null, parentPath = "/", depth = 0)
    }

    private fun buildAdminTree(): List<MenuTreeNode> {
        val items = menuItemRepository.findAllByOrderBySortOrderAscIdAsc()
        val playlistsById = youTubePlaylistRepository.findAll().associateBy { it.id!! }
        val childrenByParent = items.groupBy { it.parentId }

        fun build(parentId: Long?): List<MenuTreeNode> =
            childrenByParent[parentId]
                .orEmpty()
                .sortedWith(compareBy<MenuItem> { it.sortOrder }.thenBy { it.id })
                .map { item ->
                    val playlist = item.playlistId?.let(playlistsById::get)
                    MenuTreeNode(
                        id = item.id!!,
                        type = item.type,
                        status = item.status,
                        label = item.label,
                        slug = item.slug,
                        isAuto = item.isAuto,
                        labelCustomized = item.labelCustomized,
                        staticPageKey = item.staticPageKey,
                        boardKey = item.boardKey,
                        externalUrl = item.externalUrl,
                        openInNewTab = item.openInNewTab,
                        playlistTitle = playlist?.title,
                        playlistSourceTitle = playlist?.title,
                        thumbnailUrl = playlist?.thumbnailUrl,
                        itemCount = playlist?.itemCount,
                        syncStatus = playlist?.syncStatus,
                        parentId = item.parentId,
                        children = build(item.id),
                    )
                }

        return build(parentId = null)
    }

    private fun ensureSlugAvailable(slug: String, currentId: Long?, parentId: Long?) {
        val existing = if (parentId == null) {
            menuItemRepository.findRootBySlug(slug)
        } else {
            menuItemRepository.findByParentIdAndSlug(parentId, slug)
        }
        if (existing != null && existing.id != currentId) {
            throw IllegalArgumentException("같은 메뉴 아래에서 이미 사용 중인 slug 입니다: $slug")
        }
    }

    private fun normalizeSlug(rawSlug: String?, rawLabel: String): String {
        val base = rawSlug?.trim()?.ifBlank { rawLabel } ?: rawLabel
        val normalized = base
            .lowercase()
            .replace(Regex("[^a-z0-9가-힣]+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
        if (normalized.isBlank()) {
            throw IllegalArgumentException("slug를 생성할 수 없습니다.")
        }
        return normalized
    }

    private fun requireActiveAdmin(actorId: Long) {
        val actor = adminAccountRepository.findByIdOrNull(actorId)
            ?: throw NotFoundException("관리자 계정을 찾을 수 없습니다. id=$actorId")

        if (!actor.active) {
            throw ForbiddenException("비활성화된 계정은 메뉴를 관리할 수 없습니다.")
        }
    }
}
