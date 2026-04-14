package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.ContentMenuStatus
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.navigation.domain.NavigationLinkType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationSet
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationItemRepository
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationSetRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AdminNavigationQueryServiceTest {

    private val siteNavigationItemRepository: SiteNavigationItemRepository = mock()
    private val siteNavigationSetRepository: SiteNavigationSetRepository = mock()
    private val contentMenuRepository: ContentMenuRepository = mock()

    private val service = AdminNavigationQueryService(
        siteNavigationItemRepository = siteNavigationItemRepository,
        siteNavigationSetRepository = siteNavigationSetRepository,
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

        whenever(siteNavigationSetRepository.findBySetKeyAndActiveTrue("main")).thenReturn(
            SiteNavigationSet(id = 1L, setKey = "main", label = "메인 사이트 메뉴"),
        )
        whenever(siteNavigationItemRepository.findAllByNavigationSetIdOrderBySortOrderAscIdAsc(1L)).thenReturn(
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

    @Test
    fun `getNavigationItems composes sermon children from active content menus`() {
        val root = item(id = 1L, key = "sermons", label = "예배 영상", href = "/sermons")
        whenever(siteNavigationSetRepository.findBySetKeyAndActiveTrue("main")).thenReturn(
            SiteNavigationSet(id = 1L, setKey = "main", label = "메인 사이트 메뉴"),
        )
        whenever(siteNavigationItemRepository.findAllByNavigationSetIdAndVisibleTrueOrderBySortOrderAscIdAsc(1L)).thenReturn(
            listOf(root),
        )
        whenever(contentMenuRepository.findAllByActiveTrueAndNavigationVisibleTrueAndStatusOrderBySortOrderAscIdAsc(ContentMenuStatus.PUBLISHED)).thenReturn(
            listOf(
                ContentMenu(
                    id = 11L,
                    siteKey = "messages",
                    menuName = "말씀/설교",
                    slug = "messages",
                    contentKind = ContentKind.LONG_FORM,
                    status = ContentMenuStatus.PUBLISHED,
                    active = true,
                    navigationVisible = true,
                    sortOrder = 10,
                ),
                ContentMenu(
                    id = 12L,
                    siteKey = "shorts",
                    menuName = "짧은 영상",
                    slug = "shorts",
                    contentKind = ContentKind.SHORT,
                    status = ContentMenuStatus.PUBLISHED,
                    active = true,
                    navigationVisible = true,
                    sortOrder = 20,
                ),
            ),
        )

        val response = service.getNavigationItems(includeHidden = false)

        assertThat(response.groups).hasSize(1)
        assertThat(response.groups[0].menuKey).isEqualTo("sermons")
        assertThat(response.groups[0].children).hasSize(2)
        assertThat(response.groups[0].children.map { it.menuKey }).containsExactly("messages", "shorts")
        assertThat(response.groups[0].children.map { it.label }).containsExactly("말씀/설교", "짧은 영상")
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
        navigationSetId = 1L,
        parentId = parentId,
        menuKey = key,
        label = label,
        href = href,
        matchPath = href,
        linkType = NavigationLinkType.INTERNAL,
        visible = visible,
    )
}
