package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
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
    private val contentMenuRepository: ContentMenuRepository,
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

        val linkType = runCatching { NavigationLinkType.valueOf(request.linkType) }
            .getOrElse { throw IllegalArgumentException("지원하지 않는 링크 타입입니다. linkType=${request.linkType}") }

        validateContentReference(request, linkType)
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
                contentSiteKey = request.contentSiteKey?.trim()?.takeIf { it.isNotEmpty() },
                visible = request.visible,
                headerVisible = request.headerVisible,
                mobileVisible = request.mobileVisible,
                lnbVisible = request.lnbVisible,
                breadcrumbVisible = request.breadcrumbVisible,
                defaultLanding = request.defaultLanding,
                sortOrder = request.sortOrder,
            )
        )

        return saved.toAdminNavigationItemDto()
    }

    private fun validateContentReference(
        request: AdminNavigationUpsertRequest,
        linkType: NavigationLinkType,
    ) {
        val contentSiteKey = request.contentSiteKey?.trim()?.takeIf { it.isNotEmpty() }
        if (linkType == NavigationLinkType.CONTENT_REF) {
            if (contentSiteKey == null) {
                throw IllegalArgumentException("CONTENT_REF 메뉴는 contentSiteKey 가 필요합니다.")
            }
            if (contentMenuRepository.findBySiteKey(contentSiteKey) == null) {
                throw IllegalArgumentException("존재하지 않는 콘텐츠 메뉴입니다. contentSiteKey=$contentSiteKey")
            }
        } else if (contentSiteKey != null) {
            throw IllegalArgumentException("contentSiteKey 는 CONTENT_REF 타입에서만 사용할 수 있습니다.")
        }
    }

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

            else -> Unit
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
        contentSiteKey = contentSiteKey,
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
