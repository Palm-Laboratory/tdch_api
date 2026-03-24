package kr.or.thejejachurch.api.navigation.application

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
) {

    @Transactional(readOnly = true)
    fun getNavigation(): NavigationResponse {
        val items = siteNavigationItemRepository.findAllByVisibleTrueOrderBySortOrderAscIdAsc()
        val itemsByParentId = items.groupBy { it.parentId }

        val groups = itemsByParentId[null].orEmpty().map { root ->
            val children = itemsByParentId[root.id].orEmpty()
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
                items = children.map(::toNavigationItemDto),
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
}
