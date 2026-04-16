package kr.or.thejejachurch.api.menu.interfaces.dto

import kr.or.thejejachurch.api.menu.application.AdminMenuSnapshot
import kr.or.thejejachurch.api.menu.application.AdminYouTubePlaylistSummary
import kr.or.thejejachurch.api.menu.application.MenuTreeNode
import kr.or.thejejachurch.api.menu.application.MenuTreeNodeInput
import kr.or.thejejachurch.api.menu.application.PublicNavigationResponse
import kr.or.thejejachurch.api.menu.application.PublicResolvedMenuPage
import kr.or.thejejachurch.api.menu.application.PublicVideoDetail
import kr.or.thejejachurch.api.menu.application.YouTubeSyncSummary
import kr.or.thejejachurch.api.menu.domain.MenuStatus
import kr.or.thejejachurch.api.menu.domain.MenuType
import kr.or.thejejachurch.api.youtube.domain.YouTubeSyncStatus

data class ReplaceMenuTreeRequest(
    val items: List<MenuTreeNodeRequest>,
)

data class MenuTreeNodeRequest(
    val id: Long? = null,
    val type: MenuType,
    val status: MenuStatus,
    val label: String,
    val slug: String,
    val staticPageKey: String? = null,
    val boardKey: String? = null,
    val externalUrl: String? = null,
    val openInNewTab: Boolean = false,
    val isAuto: Boolean = false,
    val children: List<MenuTreeNodeRequest> = emptyList(),
)

data class AdminMenuTreeNodeDto(
    val id: Long,
    val type: MenuType,
    val status: MenuStatus,
    val label: String,
    val slug: String,
    val isAuto: Boolean,
    val labelCustomized: Boolean,
    val staticPageKey: String?,
    val boardKey: String?,
    val externalUrl: String?,
    val openInNewTab: Boolean,
    val playlistTitle: String?,
    val playlistSourceTitle: String?,
    val thumbnailUrl: String?,
    val itemCount: Int?,
    val syncStatus: YouTubeSyncStatus?,
    val parentId: Long?,
    val children: List<AdminMenuTreeNodeDto>,
)

data class AdminMenuTreeResponse(
    val items: List<AdminMenuTreeNodeDto>,
)

data class AdminYouTubePlaylistDto(
    val menuId: Long,
    val playlistId: String,
    val menuLabel: String,
    val sourceTitle: String,
    val slug: String,
    val status: MenuStatus,
    val syncStatus: YouTubeSyncStatus,
    val parentId: Long?,
    val parentLabel: String?,
    val thumbnailUrl: String?,
    val itemCount: Int,
)

data class AdminYouTubePlaylistsResponse(
    val playlists: List<AdminYouTubePlaylistDto>,
)

data class PublicVideoDetailResponse(
    val title: String,
    val sourceTitle: String,
    val playlistId: String,
    val slug: String,
    val fullPath: String,
    val description: String?,
    val thumbnailUrl: String?,
    val itemCount: Int,
    val groupLabel: String?,
    val siblings: List<PublicVideoSiblingDto>,
)

data class PublicVideoSiblingDto(
    val label: String,
    val href: String,
)

data class PublicResolvedMenuPageResponse(
    val type: MenuType,
    val label: String,
    val slug: String,
    val fullPath: String,
    val parentLabel: String?,
    val staticPageKey: String?,
    val boardKey: String?,
    val redirectTo: String?,
)

data class YouTubeSyncResponse(
    val status: String,
    val totalPlaylists: Int,
    val createdMenus: Int,
    val updatedMenus: Int,
    val archivedMenus: Int,
    val restoredMenus: Int,
    val completedAt: String,
)

fun ReplaceMenuTreeRequest.toCommand(): List<MenuTreeNodeInput> = items.map { it.toCommand() }

private fun MenuTreeNodeRequest.toCommand(): MenuTreeNodeInput =
    MenuTreeNodeInput(
        id = id,
        type = type,
        status = status,
        label = label,
        slug = slug,
        staticPageKey = staticPageKey,
        boardKey = boardKey,
        externalUrl = externalUrl,
        openInNewTab = openInNewTab,
        isAuto = isAuto,
        children = children.map { it.toCommand() },
    )

fun AdminMenuSnapshot.toDto(): AdminMenuTreeResponse =
    AdminMenuTreeResponse(items = items.map { it.toDto() })

fun MenuTreeNode.toDto(): AdminMenuTreeNodeDto =
    AdminMenuTreeNodeDto(
        id = id,
        type = type,
        status = status,
        label = label,
        slug = slug,
        isAuto = isAuto,
        labelCustomized = labelCustomized,
        staticPageKey = staticPageKey,
        boardKey = boardKey,
        externalUrl = externalUrl,
        openInNewTab = openInNewTab,
        playlistTitle = playlistTitle,
        playlistSourceTitle = playlistSourceTitle,
        thumbnailUrl = thumbnailUrl,
        itemCount = itemCount,
        syncStatus = syncStatus,
        parentId = parentId,
        children = children.map { it.toDto() },
    )

fun AdminYouTubePlaylistSummary.toDto(): AdminYouTubePlaylistDto =
    AdminYouTubePlaylistDto(
        menuId = menuId,
        playlistId = playlistId,
        menuLabel = menuLabel,
        sourceTitle = sourceTitle,
        slug = slug,
        status = status,
        syncStatus = syncStatus,
        parentId = parentId,
        parentLabel = parentLabel,
        thumbnailUrl = thumbnailUrl,
        itemCount = itemCount,
    )

fun PublicNavigationResponse.toDto(): PublicNavigationResponse = this

fun PublicVideoDetail.toDto(): PublicVideoDetailResponse =
    PublicVideoDetailResponse(
        title = title,
        sourceTitle = sourceTitle,
        playlistId = playlistId,
        slug = slug,
        fullPath = fullPath,
        description = description,
        thumbnailUrl = thumbnailUrl,
        itemCount = itemCount,
        groupLabel = groupLabel,
        siblings = siblings.map { PublicVideoSiblingDto(label = it.label, href = it.href) },
    )

fun PublicResolvedMenuPage.toDto(): PublicResolvedMenuPageResponse =
    PublicResolvedMenuPageResponse(
        type = type,
        label = label,
        slug = slug,
        fullPath = fullPath,
        parentLabel = parentLabel,
        staticPageKey = staticPageKey,
        boardKey = boardKey,
        redirectTo = redirectTo,
    )

fun YouTubeSyncSummary.toDto(): YouTubeSyncResponse =
    YouTubeSyncResponse(
        status = status,
        totalPlaylists = totalPlaylists,
        createdMenus = createdMenus,
        updatedMenus = updatedMenus,
        archivedMenus = archivedMenus,
        restoredMenus = restoredMenus,
        completedAt = completedAt,
    )
