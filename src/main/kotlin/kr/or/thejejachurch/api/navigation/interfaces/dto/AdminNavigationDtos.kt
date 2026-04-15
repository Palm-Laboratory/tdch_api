package kr.or.thejejachurch.api.navigation.interfaces.dto

import jakarta.validation.constraints.NotBlank
import java.time.OffsetDateTime

data class AdminNavigationItemDto(
    val id: Long,
    val parentId: Long?,
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

data class AdminNavigationUpsertRequest(
    val parentId: Long? = null,
    @field:NotBlank
    val label: String,
    @field:NotBlank
    val href: String,
    val matchPath: String? = null,
    @field:NotBlank
    val linkType: String,
    val contentSiteKey: String? = null,
    val visible: Boolean = true,
    val headerVisible: Boolean = true,
    val mobileVisible: Boolean = true,
    val lnbVisible: Boolean = true,
    val breadcrumbVisible: Boolean = true,
    val defaultLanding: Boolean = false,
    val sortOrder: Int = 0,
)
