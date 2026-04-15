package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.navigation.domain.NavigationLinkType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationBoardPage
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationMenuType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationStaticPage
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationVideoPage
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationBoardPageRepository
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationItemRepository
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationStaticPageRepository
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationVideoPageRepository
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationItemDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationUpsertRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminNavigationCommandService(
    private val siteNavigationItemRepository: SiteNavigationItemRepository,
    private val contentMenuRepository: ContentMenuRepository,
    private val siteNavigationStaticPageRepository: SiteNavigationStaticPageRepository,
    private val siteNavigationBoardPageRepository: SiteNavigationBoardPageRepository,
    private val siteNavigationVideoPageRepository: SiteNavigationVideoPageRepository,
) {

    @Transactional
    fun createNavigationItem(request: AdminNavigationUpsertRequest): AdminNavigationItemDto {
        val parent = request.parentId?.let { parentId ->
            siteNavigationItemRepository.findById(parentId)
                .orElseThrow { NotFoundException("상위 메뉴를 찾을 수 없습니다. id=$parentId") }
        }

        validateParent(parent, request.defaultLanding)

        val linkType = resolveLinkType(request.linkType)
        val menuType = parseMenuType(request.menuType)
        validateContentReference(request, linkType)
        validateLinkFields(request, linkType)
        validateMenuTypeSpecificFields(menuType, request)

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
                menuType = menuType,
                visible = request.visible,
                headerVisible = request.headerVisible,
                mobileVisible = request.mobileVisible,
                lnbVisible = request.lnbVisible,
                breadcrumbVisible = request.breadcrumbVisible,
                defaultLanding = request.defaultLanding,
                sortOrder = request.sortOrder,
            ),
        )
        persistMenuTypeSpecificDetail(saved.id ?: throw IllegalStateException("site_navigation.id is null"), menuType, request)

        return loadNavigationItemDto(saved.id ?: throw IllegalStateException("site_navigation.id is null"))
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
        val menuType = parseMenuType(request.menuType)
        validateContentReference(request, linkType)
        validateLinkFields(request, linkType)
        validateMenuTypeSpecificFields(menuType, request)

        val updated = siteNavigationItemRepository.save(
            SiteNavigationItem(
                id = currentItem.id,
                parentId = parent?.id,
                label = request.label,
                href = request.href,
                matchPath = request.matchPath?.trim()?.takeIf { it.isNotEmpty() },
                linkType = linkType,
                contentSiteKey = request.contentSiteKey?.trim()?.takeIf { it.isNotEmpty() },
                menuType = menuType,
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
        persistMenuTypeSpecificDetail(updated.id ?: throw IllegalStateException("site_navigation.id is null"), menuType, request)

        return loadNavigationItemDto(updated.id ?: throw IllegalStateException("site_navigation.id is null"))
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

    private fun validateMenuTypeSpecificFields(
        menuType: SiteNavigationMenuType,
        request: AdminNavigationUpsertRequest,
    ) {
        when (menuType) {
            SiteNavigationMenuType.BOARD_PAGE -> {
                if (normalizeOptional(request.boardKey) == null || normalizeOptional(request.listPath) == null) {
                    throw IllegalArgumentException("BOARD_PAGE 메뉴는 boardKey 와 listPath 가 필요합니다.")
                }
            }

            SiteNavigationMenuType.VIDEO_PAGE -> {
                if (normalizeOptional(request.videoRootKey) == null) {
                    throw IllegalArgumentException("VIDEO_PAGE 메뉴는 videoRootKey 가 필요합니다.")
                }
            }

            SiteNavigationMenuType.STATIC_PAGE -> Unit
        }
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

    private fun loadNavigationItemDto(id: Long): AdminNavigationItemDto {
        val item = siteNavigationItemRepository.findById(id)
            .orElseThrow { NotFoundException("내비게이션 항목을 찾을 수 없습니다. id=$id") }
        return item.toAdminNavigationItemDto(
            details = loadNavigationDetailSnapshot(id),
            children = emptyList(),
        )
    }

    private fun loadNavigationDetailSnapshot(siteNavigationId: Long): AdminNavigationDetailSnapshot =
        AdminNavigationDetailSnapshot(
            staticPage = siteNavigationStaticPageRepository.findById(siteNavigationId).orElse(null),
            boardPage = siteNavigationBoardPageRepository.findById(siteNavigationId).orElse(null),
            videoPage = siteNavigationVideoPageRepository.findById(siteNavigationId).orElse(null),
        )

    private fun persistMenuTypeSpecificDetail(
        siteNavigationId: Long,
        menuType: SiteNavigationMenuType,
        request: AdminNavigationUpsertRequest,
    ) {
        siteNavigationStaticPageRepository.deleteById(siteNavigationId)
        siteNavigationBoardPageRepository.deleteById(siteNavigationId)
        siteNavigationVideoPageRepository.deleteById(siteNavigationId)

        when (menuType) {
            SiteNavigationMenuType.STATIC_PAGE -> {
                val pageKey = normalizeOptional(request.pageKey)
                val pagePath = normalizeOptional(request.pagePath)
                if (pageKey != null || pagePath != null) {
                    siteNavigationStaticPageRepository.save(
                        SiteNavigationStaticPage(
                            siteNavigationId = siteNavigationId,
                            pageKey = pageKey,
                            pagePath = pagePath,
                        ),
                    )
                }
            }

            SiteNavigationMenuType.BOARD_PAGE -> {
                siteNavigationBoardPageRepository.save(
                    SiteNavigationBoardPage(
                        siteNavigationId = siteNavigationId,
                        boardKey = normalizeOptional(request.boardKey)
                            ?: throw IllegalArgumentException("BOARD_PAGE 메뉴는 boardKey 가 필요합니다."),
                        listPath = normalizeOptional(request.listPath)
                            ?: throw IllegalArgumentException("BOARD_PAGE 메뉴는 listPath 가 필요합니다."),
                        categoryKey = normalizeOptional(request.categoryKey),
                    ),
                )
            }

            SiteNavigationMenuType.VIDEO_PAGE -> {
                siteNavigationVideoPageRepository.save(
                    SiteNavigationVideoPage(
                        siteNavigationId = siteNavigationId,
                        videoRootKey = normalizeOptional(request.videoRootKey)
                            ?: throw IllegalArgumentException("VIDEO_PAGE 메뉴는 videoRootKey 가 필요합니다."),
                        landingMode = normalizeOptional(request.landingMode),
                        contentKindFilter = parseContentKind(request.contentKindFilter),
                    ),
                )
            }
        }
    }

    private fun parseMenuType(raw: String?): SiteNavigationMenuType =
        raw?.trim()?.takeIf { it.isNotEmpty() }?.let {
            runCatching { SiteNavigationMenuType.valueOf(it) }
                .getOrElse { throw IllegalArgumentException("지원하지 않는 메뉴 타입입니다. menuType=$it") }
        } ?: SiteNavigationMenuType.STATIC_PAGE

    private fun normalizeOptional(raw: String?): String? =
        raw?.trim()?.takeIf { it.isNotEmpty() }

    private fun parseContentKind(raw: String?): ContentKind? =
        normalizeOptional(raw)?.let {
            runCatching { ContentKind.valueOf(it) }
                .getOrElse { throw IllegalArgumentException("지원하지 않는 콘텐츠 유형입니다. contentKindFilter=$it") }
        }
}
