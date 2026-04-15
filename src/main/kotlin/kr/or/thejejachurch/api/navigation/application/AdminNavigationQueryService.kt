package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.ContentMenuStatus
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.navigation.domain.NavigationLinkType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationItemRepository
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminContentMenuDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminContentMenusResponse
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationItemDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationTreeResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminNavigationQueryService(
    private val siteNavigationItemRepository: SiteNavigationItemRepository,
    private val contentMenuRepository: ContentMenuRepository,
) {

    @Transactional(readOnly = true)
    fun getNavigationItems(includeHidden: Boolean): AdminNavigationTreeResponse {
        val items = if (includeHidden) {
            siteNavigationItemRepository.findAllByOrderBySortOrderAscIdAsc()
        } else {
            siteNavigationItemRepository.findAllByVisibleTrueOrderBySortOrderAscIdAsc()
        }
        val itemsByParentId = items.groupBy { it.parentId }

        return AdminNavigationTreeResponse(
            groups = itemsByParentId[null].orEmpty().map { item ->
                if (item.href == SERMONS_ROOT_HREF) {
                    toAdminNavigationItemDto(
                        item = item,
                        itemsByParentId = itemsByParentId,
                        overrideChildren = contentMenuRepository
                            .findAllByActiveTrueAndNavigationVisibleTrueAndStatusOrderBySortOrderAscIdAsc(ContentMenuStatus.PUBLISHED)
                            .sortedWith(compareBy<ContentMenu> { it.sortOrder }.thenBy { it.id ?: Long.MAX_VALUE })
                            .map(::toAdminSermonNavigationItemDto),
                    )
                } else {
                    toAdminNavigationItemDto(item, itemsByParentId)
                }
            },
        )
    }

    @Transactional(readOnly = true)
    fun getNavigationItem(id: Long): AdminNavigationItemDto {
        val item = siteNavigationItemRepository.findById(id).orElse(null)
            ?: throw NotFoundException("내비게이션 항목을 찾을 수 없습니다. id=$id")

        return toAdminNavigationItemDto(item, emptyMap())
    }

    @Transactional(readOnly = true)
    fun getContentMenus(): AdminContentMenusResponse = AdminContentMenusResponse(
        items = contentMenuRepository.findAllByActiveTrueOrderByIdAsc().map { menu ->
            AdminContentMenuDto(
                siteKey = menu.siteKey,
                menuName = menu.menuName,
                slug = menu.slug,
                contentKind = menu.contentKind.name,
                active = menu.active,
            )
        },
    )

    private fun toAdminNavigationItemDto(
        item: SiteNavigationItem,
        itemsByParentId: Map<Long?, List<SiteNavigationItem>>,
        overrideChildren: List<AdminNavigationItemDto>? = null,
    ): AdminNavigationItemDto = AdminNavigationItemDto(
        id = item.id ?: throw IllegalStateException("site_navigation.id is null"),
        parentId = item.parentId,
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
        sortOrder = item.sortOrder,
        updatedAt = item.updatedAt,
        children = overrideChildren ?: itemsByParentId[item.id].orEmpty().map { child ->
            toAdminNavigationItemDto(child, itemsByParentId)
        },
    )

    private fun toAdminSermonNavigationItemDto(menu: ContentMenu): AdminNavigationItemDto = AdminNavigationItemDto(
        id = menu.id ?: throw IllegalStateException("content_menu.id is null"),
        parentId = null,
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
        sortOrder = menu.sortOrder,
        updatedAt = menu.updatedAt,
        children = emptyList(),
    )

    companion object {
        private const val SERMONS_ROOT_HREF = "/sermons"
    }
}
