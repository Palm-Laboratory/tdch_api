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
            val groupHref = resolveHref(root, childrenByParent, itemsById)
            val defaultLandingHref = resolveDefaultLandingHref(root, childrenByParent, itemsById)
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
                    val href = resolveHref(child, childrenByParent, itemsById)
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
        val publishedItems = menuItemRepository.findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED)
        val itemsById = publishedItems.associateBy { it.id!! }
        val menu = menuItemRepository.findByTypeAndStatusAndSlug(
            type = MenuType.YOUTUBE_PLAYLIST,
            status = MenuStatus.PUBLISHED,
            slug = slug,
        )
            ?: throw NotFoundException("재생목록을 찾을 수 없습니다. slug=$slug")
        return buildVideoDetail(menu, publishedItems, itemsById)
    }

    @Transactional(readOnly = true)
    fun getVideoDetailByPath(path: String): PublicVideoDetail {
        val publishedItems = menuItemRepository.findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED)
        val itemsById = publishedItems.associateBy { it.id!! }
        val menu = resolvePublishedVideoMenu(path, publishedItems, itemsById)
        return buildVideoDetail(menu, publishedItems, itemsById)
    }

    @Transactional(readOnly = true)
    fun resolveMenuPath(path: String): PublicResolvedMenuPage {
        val publishedItems = menuItemRepository.findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED)
        val childrenByParent = publishedItems.groupBy { it.parentId }
        val itemsById = publishedItems.associateBy { it.id!! }
        val normalizedPath = normalizeLookupPath(path)
        val menu = resolvePublishedMenu(normalizedPath)

        if (menu.type == MenuType.FOLDER || menu.type == MenuType.YOUTUBE_PLAYLIST_GROUP) {
            val redirectTo = resolveDefaultLandingHref(menu, childrenByParent, itemsById)
                ?: throw NotFoundException("연결된 공개 페이지를 찾을 수 없습니다. path=$path")

            return PublicResolvedMenuPage(
                type = menu.type,
                label = menu.label,
                slug = menu.slug,
                fullPath = redirectTo,
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
            fullPath = resolveHref(menu, childrenByParent, itemsById),
            parentLabel = menu.parentId?.let { parentId ->
                publishedItems.firstOrNull { it.id == parentId }?.label
            },
            staticPageKey = menu.staticPageKey,
            boardKey = menu.boardKey,
            redirectTo = null,
        )
    }

    private fun buildVideoDetail(
        menu: MenuItem,
        publishedItems: List<MenuItem>,
        itemsById: Map<Long, MenuItem>,
    ): PublicVideoDetail {
        if (menu.type != MenuType.YOUTUBE_PLAYLIST || menu.status != MenuStatus.PUBLISHED) {
            throw NotFoundException("공개된 재생목록을 찾을 수 없습니다. menuId=${menu.id}")
        }

        val playlist = menu.playlistId?.let { youTubePlaylistRepository.findByIdOrNull(it) }
            ?: throw NotFoundException("유튜브 재생목록 정보를 찾을 수 없습니다. menuId=${menu.id}")

        val siblings = menu.parentId?.let { parentId ->
            publishedItems
            .filter { it.parentId == parentId && it.type == MenuType.YOUTUBE_PLAYLIST }
            .sortedWith(compareBy<MenuItem> { it.sortOrder }.thenBy { it.id })
            .map {
                VideoSiblingLink(
                    label = it.label,
                    href = buildStableHref(it, itemsById),
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
            fullPath = buildStableHref(menu, itemsById),
            description = playlist.description,
            thumbnailUrl = playlist.thumbnailUrl,
            itemCount = playlist.itemCount,
            groupLabel = groupLabel,
            siblings = siblings,
        )
    }

    private fun resolveHref(
        item: MenuItem,
        childrenByParent: Map<Long?, List<MenuItem>>,
        itemsById: Map<Long, MenuItem>,
    ): String =
        when (item.type) {
            MenuType.STATIC -> buildMenuPath(item, itemsById)
            MenuType.BOARD -> item.boardKey?.trim()?.takeIf { it.isNotBlank() }?.let { "/news#$it" } ?: "/news"
            MenuType.YOUTUBE_PLAYLIST -> buildStableHref(item, itemsById)
            MenuType.EXTERNAL_LINK -> item.externalUrl ?: "/"
            MenuType.FOLDER,
            MenuType.YOUTUBE_PLAYLIST_GROUP -> resolveDefaultLandingHref(item, childrenByParent, itemsById) ?: "/"
        }

    private fun resolveDefaultLandingHref(
        item: MenuItem,
        childrenByParent: Map<Long?, List<MenuItem>>,
        itemsById: Map<Long, MenuItem>,
    ): String? {
        val firstChild = childrenByParent[item.id]
            .orEmpty()
            .sortedWith(compareBy<MenuItem> { it.sortOrder }.thenBy { it.id })
            .firstOrNull()
            ?: return null

        return resolveHref(firstChild, childrenByParent, itemsById)
    }

    private fun resolveLinkType(item: MenuItem): NavigationLinkType =
        if (item.type == MenuType.EXTERNAL_LINK) NavigationLinkType.EXTERNAL else NavigationLinkType.INTERNAL

    private fun normalizeMatchPath(href: String): String? =
        href.takeIf { !it.startsWith("http://") && !it.startsWith("https://") }?.substringBefore('#')

    private fun buildStableHref(item: MenuItem, itemsById: Map<Long, MenuItem>): String =
        when (item.type) {
            MenuType.YOUTUBE_PLAYLIST -> buildVideoPath(item, itemsById)
            else -> "/"
        }

    private fun buildVideoPath(item: MenuItem, itemsById: Map<Long, MenuItem>): String {
        val segments = mutableListOf<String>()
        var current: MenuItem? = item

        while (current != null) {
            segments += current.slug
            current = current.parentId?.let(itemsById::get)
        }

        return "/videos/${segments.asReversed().joinToString("/")}"
    }

    private fun buildMenuPath(item: MenuItem, itemsById: Map<Long, MenuItem>): String {
        val segments = mutableListOf<String>()
        var current: MenuItem? = item

        while (current != null) {
            segments += current.slug
            current = current.parentId?.let(itemsById::get)
        }

        return "/${segments.asReversed().joinToString("/")}"
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

    private fun resolvePublishedVideoMenu(
        path: String,
        publishedItems: List<MenuItem>,
        itemsById: Map<Long, MenuItem>,
    ): MenuItem {
        val normalizedPath = normalizeLookupPath(path)

        return publishedItems
            .firstOrNull { item ->
                item.type == MenuType.YOUTUBE_PLAYLIST && buildVideoPath(item, itemsById) == normalizedPath
            }
            ?: throw NotFoundException("공개된 재생목록을 찾을 수 없습니다. path=$path")
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
