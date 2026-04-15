package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.navigation.domain.NavigationLinkType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationItemRepository
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationItemDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationUpsertRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminNavigationCommandService(
    private val siteNavigationItemRepository: SiteNavigationItemRepository,
    private val contentMenuRepository: ContentMenuRepository,
) {

    @Transactional
    fun createNavigationItem(request: AdminNavigationUpsertRequest): AdminNavigationItemDto {
        val parent = request.parentId?.let { parentId ->
            siteNavigationItemRepository.findById(parentId)
                .orElseThrow { NotFoundException("상위 메뉴를 찾을 수 없습니다. id=$parentId") }
        }

        validateParent(parent, request.defaultLanding)

        val linkType = resolveLinkType(request.linkType)
        validateContentReference(request, linkType)
        validateLinkFields(request, linkType)

        if (
            parent != null &&
            request.defaultLanding &&
            siteNavigationItemRepository.existsByParentIdAndDefaultLandingTrue(parent.id!!)
        ) {
            throw IllegalArgumentException("이미 기본 랜딩으로 지정된 하위 메뉴가 있습니다.")
        }

        val saved = siteNavigationItemRepository.save(
            SiteNavigationItem(
                id = null,
                parentId = parent?.id,
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

        val parent = request.parentId?.let { parentId ->
            if (parentId == id) {
                throw IllegalArgumentException("메뉴 자신을 상위 메뉴로 선택할 수 없습니다.")
            }

            siteNavigationItemRepository.findById(parentId)
                .orElseThrow { NotFoundException("상위 메뉴를 찾을 수 없습니다. id=$parentId") }
        }

        if (siteNavigationItemRepository.existsByParentId(id) && parent != null) {
            throw IllegalArgumentException("하위 메뉴가 있는 1depth 메뉴는 2depth로 변경할 수 없습니다.")
        }

        validateParent(parent, request.defaultLanding)

        if (
            parent != null &&
            request.defaultLanding &&
            siteNavigationItemRepository.existsByParentIdAndDefaultLandingTrueAndIdNot(parent.id!!, id)
        ) {
            throw IllegalArgumentException("이미 기본 랜딩으로 지정된 하위 메뉴가 있습니다.")
        }

        val linkType = resolveLinkType(request.linkType)
        validateContentReference(request, linkType)
        validateLinkFields(request, linkType)

        val updated = siteNavigationItemRepository.save(
            SiteNavigationItem(
                id = currentItem.id,
                parentId = parent?.id,
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

    private fun validateParent(
        parent: SiteNavigationItem?,
        defaultLanding: Boolean,
    ) {
        if (parent?.parentId != null) {
            throw IllegalArgumentException("상위 메뉴는 1depth 메뉴만 선택할 수 있습니다.")
        }

        if (parent == null && defaultLanding) {
            throw IllegalArgumentException("1depth 메뉴는 기본 랜딩으로 지정할 수 없습니다.")
        }
    }

    private fun resolveLinkType(raw: String): NavigationLinkType = runCatching {
        NavigationLinkType.valueOf(raw)
    }.getOrElse {
        throw IllegalArgumentException("지원하지 않는 링크 타입입니다. linkType=$raw")
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
        id = id ?: throw IllegalStateException("site_navigation.id is null"),
        parentId = parentId,
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
