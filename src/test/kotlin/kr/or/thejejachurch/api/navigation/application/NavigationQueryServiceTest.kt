package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.navigation.domain.NavigationLinkType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationItemRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class NavigationQueryServiceTest {

    private val siteNavigationItemRepository: SiteNavigationItemRepository = mock()

    private val service = NavigationQueryService(
        siteNavigationItemRepository = siteNavigationItemRepository,
    )

    @Test
    fun `getNavigation builds grouped response with default landing`() {
        val root = item(id = 1L, key = "about", label = "교회 소개", href = "/about", matchPath = "/about")
        val firstChild = item(
            id = 2L,
            parentId = 1L,
            key = "about-greeting",
            label = "인사말/비전",
            href = "/about/greeting",
            matchPath = "/about/greeting",
            defaultLanding = true,
        )
        val secondChild = item(
            id = 3L,
            parentId = 1L,
            key = "about-location",
            label = "오시는 길",
            href = "/about/location#map",
            matchPath = "/about/location",
            linkType = NavigationLinkType.ANCHOR,
        )

        whenever(siteNavigationItemRepository.findAllByVisibleTrueOrderBySortOrderAscIdAsc()).thenReturn(
            listOf(root, firstChild, secondChild),
        )

        val response = service.getNavigation()

        assertThat(response.groups).hasSize(1)
        assertThat(response.groups[0].key).isEqualTo("about")
        assertThat(response.groups[0].defaultLandingHref).isEqualTo("/about/greeting")
        assertThat(response.groups[0].items).hasSize(2)
        assertThat(response.groups[0].items[1].matchPath).isEqualTo("/about/location")
        assertThat(response.groups[0].items[1].linkType).isEqualTo("ANCHOR")
    }

    private fun item(
        id: Long,
        key: String,
        label: String,
        href: String,
        matchPath: String? = null,
        parentId: Long? = null,
        defaultLanding: Boolean = false,
        linkType: NavigationLinkType = NavigationLinkType.INTERNAL,
    ): SiteNavigationItem = SiteNavigationItem(
        id = id,
        parentId = parentId,
        menuKey = key,
        label = label,
        href = href,
        matchPath = matchPath,
        linkType = linkType,
        defaultLanding = defaultLanding,
        visible = true,
    )
}
