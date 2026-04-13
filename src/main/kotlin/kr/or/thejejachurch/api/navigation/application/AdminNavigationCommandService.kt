package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.navigation.domain.NavigationLinkType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationItemRepository
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationSetRepository
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationItemDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationUpsertRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminNavigationCommandService(
    private val siteNavigationItemRepository: SiteNavigationItemRepository,
    private val siteNavigationSetRepository: SiteNavigationSetRepository,
) {

    @Transactional
    fun createNavigationItem(request: AdminNavigationUpsertRequest): AdminNavigationItemDto {
        val navigationSetId = request.navigationSetId
            ?: throw IllegalArgumentException("navigationSetId 는 필수입니다.")
        val navigationSet = siteNavigationSetRepository.findById(navigationSetId)
            .orElseThrow { NotFoundException("내비게이션 세트를 찾을 수 없습니다. id=$navigationSetId") }

        if (!navigationSet.active) {
            throw IllegalArgumentException("비활성화된 내비게이션 세트에는 메뉴를 생성할 수 없습니다.")
        }

        if (siteNavigationItemRepository.existsByNavigationSetIdAndMenuKey(navigationSetId, request.menuKey)) {
            throw IllegalArgumentException("이미 사용 중인 menuKey 입니다. menuKey=${request.menuKey}")
        }

        val parent = request.parentId?.let { parentId ->
            siteNavigationItemRepository.findByNavigationSetIdAndId(navigationSetId, parentId)
                ?: throw NotFoundException("상위 메뉴를 찾을 수 없습니다. id=$parentId")
        }

        if (parent?.parentId != null) {
            throw IllegalArgumentException("상위 메뉴는 1depth 메뉴만 선택할 수 있습니다.")
        }

        if (parent == null && request.defaultLanding) {
            throw IllegalArgumentException("1depth 메뉴는 기본 랜딩으로 지정할 수 없습니다.")
        }

        if (
            parent != null &&
            request.defaultLanding &&
            siteNavigationItemRepository.existsByNavigationSetIdAndParentIdAndDefaultLandingTrue(navigationSetId, parent.id!!)
        ) {
            throw IllegalArgumentException("이미 기본 랜딩으로 지정된 하위 메뉴가 있습니다.")
        }

        val linkType = parseLinkType(request.linkType)
        validateLinkFields(request, linkType)

        val saved = siteNavigationItemRepository.save(
            SiteNavigationItem(
                navigationSetId = navigationSetId,
                parentId = parent?.id,
                menuKey = request.menuKey,
                label = request.label,
                href = request.href,
                matchPath = request.matchPath?.trim()?.takeIf { it.isNotEmpty() },
                linkType = linkType,
                visible = request.visible,
                headerVisible = request.headerVisible,
                mobileVisible = request.mobileVisible,
                lnbVisible = request.lnbVisible,
                breadcrumbVisible = request.breadcrumbVisible,
                defaultLanding = request.defaultLanding,
                sortOrder = request.sortOrder,
            ),
        )

        return saved.toAdminNavigationItemDto()
    }

    @Transactional
    fun updateNavigationItem(
        id: Long,
        request: AdminNavigationUpsertRequest,
    ): AdminNavigationItemDto {
        val currentItem = siteNavigationItemRepository.findById(id)
            .orElseThrow { NotFoundException("수정할 메뉴를 찾을 수 없습니다. id=$id") }
        val navigationSetId = request.navigationSetId
            ?: throw IllegalArgumentException("navigationSetId 는 필수입니다.")

        if (currentItem.navigationSetId != navigationSetId) {
            throw IllegalArgumentException("메뉴 세트는 변경할 수 없습니다.")
        }

        val navigationSet = siteNavigationSetRepository.findById(navigationSetId)
            .orElseThrow { NotFoundException("내비게이션 세트를 찾을 수 없습니다. id=$navigationSetId") }

        if (!navigationSet.active) {
            throw IllegalArgumentException("비활성화된 내비게이션 세트의 메뉴는 수정할 수 없습니다.")
        }

        if (siteNavigationItemRepository.existsByNavigationSetIdAndMenuKeyAndIdNot(navigationSetId, request.menuKey, id)) {
            throw IllegalArgumentException("이미 사용 중인 menuKey 입니다. menuKey=${request.menuKey}")
        }

        val parent = request.parentId?.let { parentId ->
            if (parentId == id) {
                throw IllegalArgumentException("메뉴 자신을 상위 메뉴로 선택할 수 없습니다.")
            }

            siteNavigationItemRepository.findByNavigationSetIdAndId(navigationSetId, parentId)
                ?: throw NotFoundException("상위 메뉴를 찾을 수 없습니다. id=$parentId")
        }

        if (parent?.parentId != null) {
            throw IllegalArgumentException("상위 메뉴는 1depth 메뉴만 선택할 수 있습니다.")
        }

        if (siteNavigationItemRepository.existsByParentId(id) && parent != null) {
            throw IllegalArgumentException("하위 메뉴가 있는 1depth 메뉴는 2depth로 변경할 수 없습니다.")
        }

        if (parent == null && request.defaultLanding) {
            throw IllegalArgumentException("1depth 메뉴는 기본 랜딩으로 지정할 수 없습니다.")
        }

        if (
            parent != null &&
            request.defaultLanding &&
            siteNavigationItemRepository.existsByNavigationSetIdAndParentIdAndDefaultLandingTrueAndIdNot(
                navigationSetId,
                parent.id!!,
                id,
            )
        ) {
            throw IllegalArgumentException("이미 기본 랜딩으로 지정된 하위 메뉴가 있습니다.")
        }

        val linkType = parseLinkType(request.linkType)
        validateLinkFields(request, linkType)

        val updated = siteNavigationItemRepository.save(
            SiteNavigationItem(
                id = currentItem.id,
                navigationSetId = currentItem.navigationSetId,
                parentId = parent?.id,
                menuKey = request.menuKey,
                label = request.label,
                href = request.href,
                matchPath = request.matchPath?.trim()?.takeIf { it.isNotEmpty() },
                linkType = linkType,
                visible = request.visible,
                headerVisible = request.headerVisible,
                mobileVisible = request.mobileVisible,
                lnbVisible = request.lnbVisible,
                breadcrumbVisible = request.breadcrumbVisible,
                defaultLanding = request.defaultLanding,
                sortOrder = request.sortOrder,
                createdAt = currentItem.createdAt,
                updatedAt = currentItem.updatedAt,
            ),
        )

        return updated.toAdminNavigationItemDto()
    }

    @Transactional
    fun deleteNavigationItem(id: Long) {
        val item = siteNavigationItemRepository.findById(id)
            .orElseThrow { NotFoundException("삭제할 메뉴를 찾을 수 없습니다. id=$id") }

        if (siteNavigationItemRepository.existsByParentId(id)) {
            throw IllegalArgumentException("하위 메뉴가 있는 메뉴는 삭제할 수 없습니다.")
        }

        siteNavigationItemRepository.delete(item)
    }

    private fun parseLinkType(raw: String): NavigationLinkType =
        runCatching { NavigationLinkType.valueOf(raw) }
            .getOrElse { throw IllegalArgumentException("지원하지 않는 링크 타입입니다. linkType=$raw") }

    private fun validateLinkFields(
        request: AdminNavigationUpsertRequest,
        linkType: NavigationLinkType,
    ) {
        when (linkType) {
            NavigationLinkType.ANCHOR -> {
                if (!request.href.contains("#")) {
                    throw IllegalArgumentException("ANCHOR 링크는 href 에 hash(#)가 포함되어야 합니다.")
                }
                if (request.matchPath.isNullOrBlank()) {
                    throw IllegalArgumentException("ANCHOR 링크는 matchPath 가 필요합니다.")
                }
                if (request.matchPath.contains("#")) {
                    throw IllegalArgumentException("matchPath 는 hash(#) 없는 경로여야 합니다.")
                }
            }

            NavigationLinkType.EXTERNAL -> {
                val href = request.href.trim()
                if (!(href.startsWith("http://") || href.startsWith("https://"))) {
                    throw IllegalArgumentException("EXTERNAL 링크는 http:// 또는 https:// 로 시작해야 합니다.")
                }
            }

            NavigationLinkType.INTERNAL -> Unit
        }
    }

    private fun SiteNavigationItem.toAdminNavigationItemDto(): AdminNavigationItemDto = AdminNavigationItemDto(
        id = id ?: throw IllegalStateException("site_navigation_item.id is null"),
        navigationSetId = navigationSetId,
        parentId = parentId,
        menuKey = menuKey,
        label = label,
        href = href,
        matchPath = matchPath,
        linkType = linkType.name,
        visible = visible,
        headerVisible = headerVisible,
        mobileVisible = mobileVisible,
        lnbVisible = lnbVisible,
        breadcrumbVisible = breadcrumbVisible,
        defaultLanding = defaultLanding,
        sortOrder = sortOrder,
        updatedAt = updatedAt,
        children = emptyList(),
    )
}
