package kr.or.thejejachurch.api.menu.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.menu.domain.MenuItem
import kr.or.thejejachurch.api.menu.domain.MenuStatus
import kr.or.thejejachurch.api.menu.domain.MenuType
import kr.or.thejejachurch.api.menu.infrastructure.persistence.MenuItemRepository
import kr.or.thejejachurch.api.youtube.infrastructure.persistence.YouTubePlaylistRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PublicMenuService(
    private val menuItemRepository: MenuItemRepository,
    private val youTubePlaylistRepository: YouTubePlaylistRepository,
) {
    @Transactional(readOnly = true)
    fun getNavigation(): PublicNavigationResponse {
        val publishedItems = menuItemRepository.findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED)
        val childrenByParent = publishedItems.groupBy { it.parentId }
        val itemsById = publishedItems.associateBy { it.id!! }
        val rootItems = childrenByParent[null].orEmpty()
            .sortedWith(compareBy<MenuItem> { it.sortOrder }.thenBy { it.id })

        val groups = rootItems.map { root ->
            val directChildren = childrenByParent[root.id].orEmpty()
                .sortedWith(compareBy<MenuItem> { it.sortOrder }.thenBy { it.id })
            val groupHref = resolveHref(root, itemsById, childrenByParent)
            val defaultLandingHref = resolveDefaultLandingHref(root, itemsById, childrenByParent)
            NavigationGroupDto(
                key = root.slug,
                label = root.label,
                href = groupHref,
                matchPath = normalizeMatchPath(groupHref),
                linkType = resolveLinkType(root),
                visible = true,
                headerVisible = true,
                mobileVisible = true,
                lnbVisible = true,
                breadcrumbVisible = true,
                defaultLandingHref = defaultLandingHref,
                items = directChildren.map { child ->
                    val href = resolveHref(child, itemsById, childrenByParent)
                    NavigationItemDto(
                        key = child.slug,
                        label = child.label,
                        href = href,
                        matchPath = normalizeMatchPath(href),
                        linkType = resolveLinkType(child),
                        contentSiteKey = child.staticPageKey ?: child.boardKey,
                        visible = true,
                        headerVisible = true,
                        mobileVisible = true,
                        lnbVisible = true,
                        breadcrumbVisible = true,
                        defaultLanding = false,
                    )
                },
            )
        }

        return PublicNavigationResponse(groups = groups)
    }

    @Transactional(readOnly = true)
    fun getVideoDetail(slug: String): PublicVideoDetail {
        val menu = menuItemRepository.findBySlug(slug)
            ?: throw NotFoundException("재생목록을 찾을 수 없습니다. slug=$slug")
        return buildVideoDetail(menu)
    }

    @Transactional(readOnly = true)
    fun getVideoDetailByPath(path: String): PublicVideoDetail {
        val menu = resolvePublishedMenu(path)
        if (menu.type != MenuType.YOUTUBE_PLAYLIST) {
            throw NotFoundException("공개된 재생목록을 찾을 수 없습니다. path=$path")
        }
        return buildVideoDetail(menu)
    }

    @Transactional(readOnly = true)
    fun resolveMenuPath(path: String): PublicResolvedMenuPage {
        val publishedItems = menuItemRepository.findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED)
        val itemsById = publishedItems.associateBy { it.id!! }
        val childrenByParent = publishedItems.groupBy { it.parentId }
        val normalizedPath = normalizeLookupPath(path)
        val menu = resolvePublishedMenu(normalizedPath)

        if (menu.type == MenuType.FOLDER || menu.type == MenuType.YOUTUBE_PLAYLIST_GROUP) {
            val redirectTo = resolveDefaultLandingHref(menu, itemsById, childrenByParent)
                ?: throw NotFoundException("연결된 공개 페이지를 찾을 수 없습니다. path=$path")

            return PublicResolvedMenuPage(
                type = menu.type,
                label = menu.label,
                slug = menu.slug,
                fullPath = buildInternalPath(menu, itemsById),
                parentLabel = null,
                staticPageKey = null,
                boardKey = null,
                redirectTo = redirectTo,
            )
        }

        return PublicResolvedMenuPage(
            type = menu.type,
            label = menu.label,
            slug = menu.slug,
            fullPath = buildInternalPath(menu, itemsById),
            parentLabel = menu.parentId?.let { parentId -> itemsById[parentId]?.label },
            staticPageKey = menu.staticPageKey,
            boardKey = menu.boardKey,
            redirectTo = null,
        )
    }

    private fun buildVideoDetail(menu: MenuItem): PublicVideoDetail {
        if (menu.type != MenuType.YOUTUBE_PLAYLIST || menu.status != MenuStatus.PUBLISHED) {
            throw NotFoundException("공개된 재생목록을 찾을 수 없습니다. menuId=${menu.id}")
        }

        val playlist = menu.playlistId?.let { youTubePlaylistRepository.findByIdOrNull(it) }
            ?: throw NotFoundException("유튜브 재생목록 정보를 찾을 수 없습니다. menuId=${menu.id}")
        val publishedItems = menuItemRepository.findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED)
        val itemsById = publishedItems.associateBy { it.id!! }

        val siblings = menu.parentId?.let { parentId ->
            publishedItems
                .filter { it.parentId == parentId && it.type == MenuType.YOUTUBE_PLAYLIST }
                .sortedWith(compareBy<MenuItem> { it.sortOrder }.thenBy { it.id })
                .map {
                    VideoSiblingLink(
                        label = it.label,
                        href = buildInternalPath(it, itemsById),
                    )
                }
        }.orEmpty()

        val groupLabel = menu.parentId?.let { parentId ->
            menuItemRepository.findByIdOrNull(parentId)?.label
        }

        return PublicVideoDetail(
            title = menu.label,
            sourceTitle = playlist.title,
            playlistId = playlist.playlistId,
            slug = menu.slug,
            fullPath = buildInternalPath(menu, itemsById),
            description = playlist.description,
            thumbnailUrl = playlist.thumbnailUrl,
            itemCount = playlist.itemCount,
            groupLabel = groupLabel,
            siblings = siblings,
        )
    }

    private fun resolveHref(
        item: MenuItem,
        itemsById: Map<Long, MenuItem>,
        childrenByParent: Map<Long?, List<MenuItem>>,
    ): String =
        when (item.type) {
            MenuType.STATIC,
            MenuType.BOARD,
            MenuType.YOUTUBE_PLAYLIST -> buildInternalPath(item, itemsById)
            MenuType.EXTERNAL_LINK -> item.externalUrl ?: "/"
            MenuType.FOLDER,
            MenuType.YOUTUBE_PLAYLIST_GROUP -> buildInternalPath(item, itemsById)
        }

    private fun resolveDefaultLandingHref(
        item: MenuItem,
        itemsById: Map<Long, MenuItem>,
        childrenByParent: Map<Long?, List<MenuItem>>,
    ): String? {
        val firstChild = childrenByParent[item.id]
            .orEmpty()
            .sortedWith(compareBy<MenuItem> { it.sortOrder }.thenBy { it.id })
            .firstOrNull()
            ?: return null

        return resolveHref(firstChild, itemsById, childrenByParent)
    }

    private fun resolveLinkType(item: MenuItem): NavigationLinkType =
        if (item.type == MenuType.EXTERNAL_LINK) NavigationLinkType.EXTERNAL else NavigationLinkType.INTERNAL

    private fun normalizeMatchPath(href: String): String? =
        href.takeIf { !it.startsWith("http://") && !it.startsWith("https://") }?.substringBefore('#')

    private fun buildInternalPath(
        item: MenuItem,
        itemsById: Map<Long, MenuItem>,
    ): String {
        val segments = generateSequence(item) { current ->
            current.parentId?.let(itemsById::get)
        }
            .toList()
            .asReversed()
            .map { it.slug }

        return "/${segments.joinToString("/")}"
    }

    private fun resolvePublishedMenu(path: String): MenuItem {
        val normalizedPath = normalizeLookupPath(path)
        val segments = normalizedPath.trim('/').split('/').filter { it.isNotBlank() }
        val rootSlug = segments.firstOrNull()
            ?: throw NotFoundException("공개 메뉴를 찾을 수 없습니다. path=$path")

        val root = menuItemRepository.findRootBySlug(rootSlug)
            ?.takeIf { it.status == MenuStatus.PUBLISHED }
            ?: throw NotFoundException("공개 메뉴를 찾을 수 없습니다. path=$path")

        if (segments.size == 1) {
            return root
        }

        if (segments.size > 2) {
            throw NotFoundException("공개 메뉴를 찾을 수 없습니다. path=$path")
        }

        return menuItemRepository.findByParentIdAndSlug(root.id!!, segments[1])
            ?.takeIf { it.status == MenuStatus.PUBLISHED }
            ?: throw NotFoundException("공개 메뉴를 찾을 수 없습니다. path=$path")
    }

    private fun normalizeLookupPath(path: String): String {
        val trimmed = path.substringBefore('?').substringBefore('#').trim()
        if (trimmed.isBlank()) {
            return "/"
        }
        return if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    }
}

data class PublicNavigationResponse(
    val groups: List<NavigationGroupDto>,
)

enum class NavigationLinkType {
    INTERNAL,
    EXTERNAL,
}

data class NavigationItemDto(
    val key: String,
    val label: String,
    val href: String,
    val matchPath: String?,
    val linkType: NavigationLinkType,
    val contentSiteKey: String? = null,
    val visible: Boolean,
    val headerVisible: Boolean,
    val mobileVisible: Boolean,
    val lnbVisible: Boolean,
    val breadcrumbVisible: Boolean,
    val defaultLanding: Boolean,
)

data class NavigationGroupDto(
    val key: String,
    val label: String,
    val href: String,
    val matchPath: String?,
    val linkType: NavigationLinkType,
    val contentSiteKey: String? = null,
    val visible: Boolean,
    val headerVisible: Boolean,
    val mobileVisible: Boolean,
    val lnbVisible: Boolean,
    val breadcrumbVisible: Boolean,
    val defaultLandingHref: String?,
    val items: List<NavigationItemDto>,
)
