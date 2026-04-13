package kr.or.thejejachurch.api.navigation.interfaces.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime

data class AdminNavigationSetDto(
    val id: Long,
    val setKey: String,
    val label: String,
    val description: String?,
    val active: Boolean,
)

data class AdminNavigationSetsResponse(
    val sets: List<AdminNavigationSetDto>,
)

data class AdminNavigationItemDto(
    val id: Long,
    val navigationSetId: Long,
    val parentId: Long?,
    val menuKey: String,
    val label: String,
    val href: String,
    val matchPath: String?,
    val linkType: String,
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

data class AdminNavigationUpsertRequest(
    @field:NotNull
    val navigationSetId: Long?,
    val parentId: Long? = null,
    @field:NotBlank
    val menuKey: String,
    @field:NotBlank
    val label: String,
    @field:NotBlank
    val href: String,
    val matchPath: String? = null,
    @field:NotBlank
    val linkType: String,
    val visible: Boolean = true,
    val headerVisible: Boolean = true,
    val mobileVisible: Boolean = true,
    val lnbVisible: Boolean = true,
    val breadcrumbVisible: Boolean = true,
    val defaultLanding: Boolean = false,
    val sortOrder: Int = 0,
)
