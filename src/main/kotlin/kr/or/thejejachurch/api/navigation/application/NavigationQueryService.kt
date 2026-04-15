package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.ContentMenuStatus
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.navigation.domain.NavigationLinkType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
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
) {

    @Transactional(readOnly = true)
    fun getNavigation(): NavigationResponse {
        val items = siteNavigationItemRepository.findAllByVisibleTrueOrderBySortOrderAscIdAsc()
        val itemsByParentId = items.groupBy { it.parentId }

        val groups = itemsByParentId[null].orEmpty().map { root ->
            val children = if (root.href == SERMONS_ROOT_HREF) {
                contentMenuRepository.findAllByActiveTrueAndNavigationVisibleTrueAndStatusOrderBySortOrderAscIdAsc(ContentMenuStatus.PUBLISHED)
                    .sortedWith(compareBy<ContentMenu> { it.sortOrder }.thenBy { it.id ?: Long.MAX_VALUE })
                    .map(::toSermonNavigationItemDto)
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

    private fun toSermonNavigationItemDto(menu: ContentMenu): NavigationItemDto = NavigationItemDto(
        key = "sermons-${menu.slug}",
        label = menu.menuName,
        href = "/sermons/${menu.slug}",
        matchPath = "/sermons/${menu.slug}",
        linkType = NavigationLinkType.CONTENT_REF.name,
        contentSiteKey = menu.siteKey,
        visible = true,
        headerVisible = true,
        mobileVisible = true,
        lnbVisible = true,
        breadcrumbVisible = true,
        defaultLanding = false,
    )

    private fun itemKey(item: SiteNavigationItem): String = item.id?.let { "navigation-$it" } ?: item.href

    companion object {
        private const val SERMONS_ROOT_HREF = "/sermons"
    }
}
