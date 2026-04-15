package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.ContentMenuStatus
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationMenuType
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationBoardPageRepository
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationItemRepository
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationStaticPageRepository
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationVideoPageRepository
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
    private val siteNavigationStaticPageRepository: SiteNavigationStaticPageRepository,
    private val siteNavigationBoardPageRepository: SiteNavigationBoardPageRepository,
    private val siteNavigationVideoPageRepository: SiteNavigationVideoPageRepository,
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
                val details = loadNavigationDetailSnapshot(item.id ?: throw IllegalStateException("site_navigation.id is null"))
                if (item.menuType == SiteNavigationMenuType.VIDEO_PAGE) {
                    val videoMenus = findVideoMenus(details.videoPage?.videoRootKey).sortedForNavigation()
                    item.toAdminNavigationItemDto(
                        details = details,
                        children = videoMenus.map { menu -> menu.toAdminVideoNavigationItemDto(item.href) },
                    )
                } else {
                    item.toAdminNavigationItemDto(
                        details = details,
                        children = itemsByParentId[item.id].orEmpty().map { child ->
                            toNavigationTreeItem(child, itemsByParentId)
                        },
                    )
                }
            },
        )
    }

    @Transactional(readOnly = true)
    fun getNavigationItem(id: Long): AdminNavigationItemDto {
        val item = siteNavigationItemRepository.findById(id).orElse(null)
            ?: throw NotFoundException("내비게이션 항목을 찾을 수 없습니다. id=$id")

        return toNavigationTreeItem(item, emptyMap())
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

    private fun toNavigationTreeItem(
        item: SiteNavigationItem,
        itemsByParentId: Map<Long?, List<SiteNavigationItem>>,
        overrideChildren: List<AdminNavigationItemDto>? = null,
    ): AdminNavigationItemDto {
        val details = loadNavigationDetailSnapshot(item.id ?: throw IllegalStateException("site_navigation.id is null"))
        return item.toAdminNavigationItemDto(
            details = details,
            children = overrideChildren ?: itemsByParentId[item.id].orEmpty().map { child ->
                toNavigationTreeItem(child, itemsByParentId)
            },
        )
    }

    private fun loadNavigationDetailSnapshot(siteNavigationId: Long): AdminNavigationDetailSnapshot =
        AdminNavigationDetailSnapshot(
            staticPage = siteNavigationStaticPageRepository.findById(siteNavigationId).orElse(null),
            boardPage = siteNavigationBoardPageRepository.findById(siteNavigationId).orElse(null),
            videoPage = siteNavigationVideoPageRepository.findById(siteNavigationId).orElse(null),
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
}
