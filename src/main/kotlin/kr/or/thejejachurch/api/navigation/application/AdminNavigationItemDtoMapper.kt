package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.navigation.domain.NavigationLinkType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationBoardPage
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationMenuType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationStaticPage
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationVideoPage
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationItemDto

internal data class AdminNavigationDetailSnapshot(
    val staticPage: SiteNavigationStaticPage? = null,
    val boardPage: SiteNavigationBoardPage? = null,
    val videoPage: SiteNavigationVideoPage? = null,
)

internal fun SiteNavigationItem.toAdminNavigationItemDto(
    details: AdminNavigationDetailSnapshot,
    children: List<AdminNavigationItemDto>,
): AdminNavigationItemDto = AdminNavigationItemDto(
    id = id ?: throw IllegalStateException("site_navigation.id is null"),
    parentId = parentId,
    label = label,
    href = href,
    matchPath = matchPath,
    linkType = linkType.name,
    contentSiteKey = contentSiteKey,
    menuType = menuType.name,
    pageKey = details.staticPage?.pageKey,
    pagePath = details.staticPage?.pagePath,
    boardKey = details.boardPage?.boardKey,
    listPath = details.boardPage?.listPath,
    categoryKey = details.boardPage?.categoryKey,
    videoRootKey = details.videoPage?.videoRootKey,
    landingMode = details.videoPage?.landingMode,
    contentKindFilter = details.videoPage?.contentKindFilter?.name,
    visible = visible,
    headerVisible = headerVisible,
    mobileVisible = mobileVisible,
    lnbVisible = lnbVisible,
    breadcrumbVisible = breadcrumbVisible,
    defaultLanding = defaultLanding,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    children = children,
)

internal fun ContentMenu.toAdminSermonNavigationItemDto(): AdminNavigationItemDto = AdminNavigationItemDto(
    id = id ?: throw IllegalStateException("content_menu.id is null"),
    parentId = null,
    label = menuName,
    href = "/sermons/$slug",
    matchPath = "/sermons/$slug",
    linkType = NavigationLinkType.CONTENT_REF.name,
    contentSiteKey = siteKey,
    menuType = SiteNavigationMenuType.VIDEO_PAGE.name,
    videoRootKey = videoRootKey,
    landingMode = null,
    contentKindFilter = contentKind.name,
    visible = true,
    headerVisible = true,
    mobileVisible = true,
    lnbVisible = true,
    breadcrumbVisible = true,
    defaultLanding = false,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    children = emptyList(),
)
