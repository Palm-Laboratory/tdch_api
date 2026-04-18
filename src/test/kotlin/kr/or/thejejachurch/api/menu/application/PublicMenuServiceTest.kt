package kr.or.thejejachurch.api.menu.application

import kr.or.thejejachurch.api.menu.domain.MenuItem
import kr.or.thejejachurch.api.menu.domain.MenuStatus
import kr.or.thejejachurch.api.menu.domain.MenuType
import kr.or.thejejachurch.api.menu.infrastructure.persistence.MenuItemRepository
import kr.or.thejejachurch.api.youtube.domain.YouTubeContentForm
import kr.or.thejejachurch.api.youtube.domain.YouTubePlaylist
import kr.or.thejejachurch.api.youtube.infrastructure.persistence.YouTubePlaylistRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PublicMenuServiceTest {
    private val menuItemRepository = mock<MenuItemRepository>()
    private val youTubePlaylistRepository = mock<YouTubePlaylistRepository>()

    private val service = PublicMenuService(
        menuItemRepository = menuItemRepository,
        youTubePlaylistRepository = youTubePlaylistRepository,
    )

    @Test
    fun `resolveMenuPath resolves published child menu without extra slug lookups`() {
        val about = menuItem(
            id = 10L,
            type = MenuType.FOLDER,
            label = "교회소개",
            slug = "about",
        )
        val greeting = menuItem(
            id = 11L,
            parentId = about.id,
            type = MenuType.STATIC,
            label = "인사말",
            slug = "greeting",
            staticPageKey = "about.greeting",
        )
        whenever(menuItemRepository.findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED))
            .thenReturn(listOf(about, greeting))

        val resolved = service.resolveMenuPath("/about/greeting")

        assertAll(
            { assertEquals(MenuType.STATIC, resolved.type) },
            { assertEquals("인사말", resolved.label) },
            { assertEquals("greeting", resolved.slug) },
            { assertEquals("/about/greeting", resolved.fullPath) },
            { assertEquals("교회소개", resolved.parentLabel) },
            { assertEquals("about.greeting", resolved.staticPageKey) },
            { assertNull(resolved.redirectTo) },
        )

        verify(menuItemRepository).findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED)
        verify(menuItemRepository, never()).findRootBySlug("about")
        verify(menuItemRepository, never()).findByParentIdAndSlug(about.id!!, "greeting")
    }

    @Test
    fun `resolveMenuPath redirects folder to first published child without extra slug lookups`() {
        val about = menuItem(
            id = 20L,
            type = MenuType.FOLDER,
            label = "교회소개",
            slug = "about",
        )
        val greeting = menuItem(
            id = 21L,
            parentId = about.id,
            type = MenuType.STATIC,
            label = "인사말",
            slug = "greeting",
            staticPageKey = "about.greeting",
            sortOrder = 0,
        )
        val history = menuItem(
            id = 22L,
            parentId = about.id,
            type = MenuType.STATIC,
            label = "연혁",
            slug = "history",
            staticPageKey = "about.history",
            sortOrder = 1,
        )
        whenever(menuItemRepository.findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED))
            .thenReturn(listOf(about, greeting, history))

        val resolved = service.resolveMenuPath("/about")

        assertAll(
            { assertEquals(MenuType.FOLDER, resolved.type) },
            { assertEquals("교회소개", resolved.label) },
            { assertEquals("about", resolved.slug) },
            { assertEquals("/about/greeting", resolved.fullPath) },
            { assertEquals("/about/greeting", resolved.redirectTo) },
            { assertNull(resolved.parentLabel) },
        )

        verify(menuItemRepository).findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED)
        verify(menuItemRepository, never()).findRootBySlug("about")
        verify(menuItemRepository, never()).findByParentIdAndSlug(about.id!!, "greeting")
    }

    @Test
    fun `getVideoDetailByPath resolves playlist detail from published items without parent lookup`() {
        val sermons = menuItem(
            id = 31L,
            type = MenuType.YOUTUBE_PLAYLIST_GROUP,
            label = "설교",
            slug = "sermons",
        )
        val sunday = menuItem(
            id = 32L,
            parentId = sermons.id,
            type = MenuType.YOUTUBE_PLAYLIST,
            label = "주일설교",
            slug = "sunday",
            playlistId = 101L,
            playlistContentForm = YouTubeContentForm.LONGFORM,
            sortOrder = 0,
        )
        val friday = menuItem(
            id = 33L,
            parentId = sermons.id,
            type = MenuType.YOUTUBE_PLAYLIST,
            label = "금요설교",
            slug = "friday",
            playlistId = 102L,
            playlistContentForm = YouTubeContentForm.LONGFORM,
            sortOrder = 1,
        )
        whenever(menuItemRepository.findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED))
            .thenReturn(listOf(sermons, sunday, friday))
        whenever(youTubePlaylistRepository.findById(eq(101L))).thenReturn(
            java.util.Optional.of(
                YouTubePlaylist(
                    id = 101L,
                    channelId = 1L,
                    playlistId = "PL_SUNDAY",
                    title = "주일설교 원본",
                    description = "설교 모음",
                    thumbnailUrl = "https://example.com/thumb.jpg",
                    itemCount = 42,
                    publishedAt = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                ),
            ),
        )

        val detail = service.getVideoDetailByPath("/videos/sermons/sunday")

        assertAll(
            { assertEquals("주일설교", detail.title) },
            { assertEquals("주일설교 원본", detail.sourceTitle) },
            { assertEquals("PL_SUNDAY", detail.playlistId) },
            { assertEquals("/videos/sermons/sunday", detail.fullPath) },
            { assertEquals("설교", detail.groupLabel) },
            { assertEquals(2, detail.siblings.size) },
            {
                assertEquals(
                    listOf("/videos/sermons/sunday", "/videos/sermons/friday"),
                    detail.siblings.map { it.href },
                )
            },
        )

        verify(menuItemRepository).findAllByStatusOrderBySortOrderAscIdAsc(MenuStatus.PUBLISHED)
        verify(menuItemRepository, never()).findById(sermons.id!!)
    }

    private fun menuItem(
        id: Long,
        parentId: Long? = null,
        type: MenuType,
        label: String,
        slug: String,
        staticPageKey: String? = null,
        playlistId: Long? = null,
        playlistContentForm: YouTubeContentForm? = null,
        sortOrder: Int = 0,
    ): MenuItem = MenuItem(
        id = id,
        parentId = parentId,
        type = type,
        status = MenuStatus.PUBLISHED,
        label = label,
        slug = slug,
        staticPageKey = staticPageKey,
        playlistId = playlistId,
        playlistContentForm = playlistContentForm,
        sortOrder = sortOrder,
        depth = if (parentId == null) 0 else 1,
        path = slug,
    )
}
