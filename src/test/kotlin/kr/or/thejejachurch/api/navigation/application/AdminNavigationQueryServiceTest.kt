package kr.or.thejejachurch.api.navigation.application

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
    private val service = AdminNavigationQueryService(
        siteNavigationItemRepository = siteNavigationItemRepository,
        siteNavigationSetRepository = siteNavigationSetRepository,
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
