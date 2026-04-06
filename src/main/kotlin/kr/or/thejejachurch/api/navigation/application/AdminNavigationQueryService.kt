package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationItemRepository
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationSetRepository
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminContentMenuDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminContentMenusResponse
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationItemDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationSetDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationSetsResponse
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationTreeResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminNavigationQueryService(
    private val siteNavigationItemRepository: SiteNavigationItemRepository,
    private val siteNavigationSetRepository: SiteNavigationSetRepository,
    private val contentMenuRepository: ContentMenuRepository,
) {

    @Transactional(readOnly = true)
    fun getNavigationSets(): AdminNavigationSetsResponse = AdminNavigationSetsResponse(
        sets = siteNavigationSetRepository.findAllByActiveTrueOrderByIdAsc().map { navigationSet ->
            AdminNavigationSetDto(
                id = navigationSet.id ?: throw IllegalStateException("site_navigation_set.id is null"),
                setKey = navigationSet.setKey,
                label = navigationSet.label,
                description = navigationSet.description,
                active = navigationSet.active,
            )
        }
    )

    @Transactional(readOnly = true)
    fun getNavigationItems(includeHidden: Boolean): AdminNavigationTreeResponse {
        val mainNavigationSetId = siteNavigationSetRepository.findBySetKeyAndActiveTrue(MAIN_NAVIGATION_SET_KEY)?.id
            ?: return AdminNavigationTreeResponse(groups = emptyList())
        val items = if (includeHidden) {
            siteNavigationItemRepository.findAllByNavigationSetIdOrderBySortOrderAscIdAsc(mainNavigationSetId)
        } else {
            siteNavigationItemRepository.findAllByNavigationSetIdAndVisibleTrueOrderBySortOrderAscIdAsc(mainNavigationSetId)
        }
        val itemsByParentId = items.groupBy { it.parentId }

        return AdminNavigationTreeResponse(
            groups = itemsByParentId[null].orEmpty().map { item ->
                toAdminNavigationItemDto(item, itemsByParentId)
            },
        )
    }

    @Transactional(readOnly = true)
    fun getNavigationItem(id: Long): AdminNavigationItemDto {
        val mainNavigationSetId = siteNavigationSetRepository.findBySetKeyAndActiveTrue(MAIN_NAVIGATION_SET_KEY)?.id
            ?: throw NotFoundException("메인 내비게이션 세트를 찾을 수 없습니다.")
        val item = siteNavigationItemRepository.findByNavigationSetIdAndId(mainNavigationSetId, id)
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
    ): AdminNavigationItemDto = AdminNavigationItemDto(
        id = item.id ?: throw IllegalStateException("site_navigation_item.id is null"),
        navigationSetId = item.navigationSetId,
        parentId = item.parentId,
        menuKey = item.menuKey,
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
        children = itemsByParentId[item.id].orEmpty().map { child ->
            toAdminNavigationItemDto(child, itemsByParentId)
        },
    )

    companion object {
        private const val MAIN_NAVIGATION_SET_KEY = "main"
    }
}
