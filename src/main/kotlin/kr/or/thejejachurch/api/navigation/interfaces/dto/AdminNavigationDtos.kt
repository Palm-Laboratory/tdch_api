package kr.or.thejejachurch.api.navigation.interfaces.dto

import java.time.OffsetDateTime

data class AdminNavigationItemDto(
    val id: Long,
    val parentId: Long?,
    val menuKey: String,
    val label: String,
    val href: String,
    val matchPath: String?,
    val linkType: String,
    val contentSiteKey: String? = null,
    val visible: Boolean,
    val headerVisible: Boolean,
    val mobileVisible: Boolean,
    val lnbVisible: Boolean,
    val breadcrumbVisible: Boolean,
    val defaultLanding: Boolean,
    val sortOrder: Int,
    val updatedAt: OffsetDateTime,
    val children: List<AdminNavigationItemDto> = emptyList(),
)

data class AdminNavigationTreeResponse(
    val groups: List<AdminNavigationItemDto>,
)

data class AdminContentMenuDto(
    val siteKey: String,
    val menuName: String,
    val slug: String,
    val contentKind: String,
    val active: Boolean,
)

data class AdminContentMenusResponse(
    val items: List<AdminContentMenuDto>,
)
