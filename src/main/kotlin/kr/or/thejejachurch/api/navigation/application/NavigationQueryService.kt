package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.ContentMenuStatus
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.navigation.domain.NavigationLinkType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationItemRepository
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationSetRepository
import kr.or.thejejachurch.api.navigation.interfaces.dto.NavigationGroupDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.NavigationItemDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.NavigationResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NavigationQueryService(
    private val siteNavigationItemRepository: SiteNavigationItemRepository,
    private val siteNavigationSetRepository: SiteNavigationSetRepository,
    private val contentMenuRepository: ContentMenuRepository,
) {

    @Transactional(readOnly = true)
    fun getNavigation(): NavigationResponse {
        val mainNavigationSetId = siteNavigationSetRepository.findBySetKeyAndActiveTrue(MAIN_NAVIGATION_SET_KEY)?.id
            ?: return NavigationResponse(groups = emptyList())
        val items = siteNavigationItemRepository.findAllByNavigationSetIdAndVisibleTrueOrderBySortOrderAscIdAsc(mainNavigationSetId)
        val itemsByParentId = items.groupBy { it.parentId }

        val groups = itemsByParentId[null].orEmpty().map { root ->
            val children = if (root.menuKey == SERMONS_ROOT_KEY) {
                contentMenuRepository.findAllByActiveTrueAndNavigationVisibleTrueAndStatusOrderBySortOrderAscIdAsc(ContentMenuStatus.PUBLISHED)
                    .sortedWith(compareBy<ContentMenu> { it.sortOrder }.thenBy { it.id ?: Long.MAX_VALUE })
                    .map(::toSermonNavigationItemDto)
            } else {
                itemsByParentId[root.id].orEmpty().map(::toNavigationItemDto)
            }
            NavigationGroupDto(
                key = root.menuKey,
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
        key = item.menuKey,
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
        key = "$SERMONS_ROOT_KEY-${menu.slug}",
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

    companion object {
        private const val MAIN_NAVIGATION_SET_KEY = "main"
        private const val SERMONS_ROOT_KEY = "sermons"
    }
}
