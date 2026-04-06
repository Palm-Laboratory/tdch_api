package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.navigation.domain.NavigationLinkType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationItemRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AdminNavigationQueryServiceTest {

    private val siteNavigationItemRepository: SiteNavigationItemRepository = mock()
    private val contentMenuRepository: ContentMenuRepository = mock()

    private val service = AdminNavigationQueryService(
        siteNavigationItemRepository = siteNavigationItemRepository,
        contentMenuRepository = contentMenuRepository,
    )

    @Test
    fun `getNavigationItems includes hidden items when requested`() {
        val root = item(id = 1L, key = "about", label = "교회 소개", href = "/about")
        val hiddenChild = item(
            id = 2L,
            parentId = 1L,
            key = "about-hidden",
            label = "숨김 메뉴",
            href = "/about/hidden",
            visible = false,
        )

        whenever(siteNavigationItemRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(
            listOf(root, hiddenChild),
        )

        val response = service.getNavigationItems(includeHidden = true)

        assertThat(response.groups).hasSize(1)
        assertThat(response.groups[0].children).hasSize(1)
        assertThat(response.groups[0].children[0].visible).isFalse()
    }

    @Test
    fun `getContentMenus returns active menus in order`() {
        whenever(contentMenuRepository.findAllByActiveTrueOrderByIdAsc()).thenReturn(
            listOf(
                ContentMenu(
                    id = 1L,
                    siteKey = "messages",
                    menuName = "말씀/설교",
                    slug = "messages",
                    contentKind = ContentKind.LONG_FORM,
                    active = true,
                ),
            ),
        )

        val response = service.getContentMenus()

        assertThat(response.items).hasSize(1)
        assertThat(response.items[0].siteKey).isEqualTo("messages")
        assertThat(response.items[0].contentKind).isEqualTo("LONG_FORM")
    }

    private fun item(
        id: Long,
        key: String,
        label: String,
        href: String,
        parentId: Long? = null,
        visible: Boolean = true,
    ): SiteNavigationItem = SiteNavigationItem(
        id = id,
        parentId = parentId,
        menuKey = key,
        label = label,
        href = href,
        matchPath = href,
        linkType = NavigationLinkType.INTERNAL,
        visible = visible,
    )
}
