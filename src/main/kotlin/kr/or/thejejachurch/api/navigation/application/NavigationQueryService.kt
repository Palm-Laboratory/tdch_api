package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.ContentMenuStatus
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.navigation.domain.NavigationLinkType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationMenuType
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationVideoPageRepository
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationItemRepository
import kr.or.thejejachurch.api.navigation.interfaces.dto.NavigationGroupDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.NavigationItemDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.NavigationResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NavigationQueryService(
    private val siteNavigationItemRepository: SiteNavigationItemRepository,
    private val contentMenuRepository: ContentMenuRepository,
    private val siteNavigationVideoPageRepository: SiteNavigationVideoPageRepository,
) {

    @Transactional(readOnly = true)
    fun getNavigation(): NavigationResponse {
        val items = siteNavigationItemRepository.findAllByVisibleTrueOrderBySortOrderAscIdAsc()
        val itemsByParentId = items.groupBy { it.parentId }

        val groups = itemsByParentId[null].orEmpty().map { root ->
            val videoRootKey = root.id
                ?.let { siteNavigationVideoPageRepository.findById(it).orElse(null) }
                ?.videoRootKey
            val children = if (root.menuType == SiteNavigationMenuType.VIDEO_PAGE) {
                findVideoMenus(videoRootKey)
                    .sortedForNavigation()
                    .map { menu -> toVideoNavigationItemDto(root.href, menu) }
            } else {
                itemsByParentId[root.id].orEmpty().map(::toNavigationItemDto)
            }
            NavigationGroupDto(
                key = itemKey(root),
                label = root.label,
                href = root.href,
                matchPath = root.matchPath,
                linkType = root.linkType.name,
                contentSiteKey = root.contentSiteKey,
                visible = root.visible,
                headerVisible = root.headerVisible,
                mobileVisible = root.mobileVisible,
                lnbVisible = root.lnbVisible,
                breadcrumbVisible = root.breadcrumbVisible,
                defaultLandingHref = children.firstOrNull { it.defaultLanding }?.href ?: children.firstOrNull()?.href,
                items = children,
            )
        }

        return NavigationResponse(groups = groups)
    }

    private fun toNavigationItemDto(item: SiteNavigationItem): NavigationItemDto = NavigationItemDto(
        key = itemKey(item),
        label = item.label,
        href = item.href,
        matchPath = item.matchPath,
        linkType = item.linkType.name,
        contentSiteKey = item.contentSiteKey,
        visible = item.visible,
        headerVisible = item.headerVisible,
        mobileVisible = item.mobileVisible,
        lnbVisible = item.lnbVisible,
        breadcrumbVisible = item.breadcrumbVisible,
        defaultLanding = item.defaultLanding,
    )

    private fun toVideoNavigationItemDto(rootHref: String, menu: ContentMenu): NavigationItemDto = NavigationItemDto(
        key = "video-${menu.slug}",
        label = menu.menuName,
        href = "${rootHref.trimEnd('/')}/${menu.slug}",
        matchPath = "${rootHref.trimEnd('/')}/${menu.slug}",
        linkType = NavigationLinkType.CONTENT_REF.name,
        contentSiteKey = menu.siteKey,
        visible = true,
        headerVisible = true,
        mobileVisible = true,
        lnbVisible = true,
        breadcrumbVisible = true,
        defaultLanding = false,
    )

    private fun findVideoMenus(videoRootKey: String?): List<ContentMenu> =
        videoRootKey?.takeIf { it.isNotBlank() }?.let {
            contentMenuRepository.findAllByVideoRootKeyAndActiveTrueAndNavigationVisibleTrueAndStatusOrderBySortOrderAscIdAsc(
                videoRootKey = it,
                status = ContentMenuStatus.PUBLISHED,
            )
        } ?: emptyList()

    private fun List<ContentMenu>.sortedForNavigation(): List<ContentMenu> =
        sortedWith(compareBy<ContentMenu> { it.sortOrder }.thenBy { it.id ?: Long.MAX_VALUE })

    private fun itemKey(item: SiteNavigationItem): String = item.id?.let { "navigation-$it" } ?: item.href
}
