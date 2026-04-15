package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.ContentMenuStatus
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.navigation.domain.NavigationLinkType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationItemRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class NavigationQueryServiceTest {

    private val siteNavigationItemRepository: SiteNavigationItemRepository = mock()
    private val contentMenuRepository: ContentMenuRepository = mock()

    private val service = NavigationQueryService(
        siteNavigationItemRepository = siteNavigationItemRepository,
        contentMenuRepository = contentMenuRepository,
    )

    @Test
    fun `getNavigation uses href sermon root marker instead of menu key`() {
        val sermonRoot = item(id = 10L, label = "예배 영상", href = "/sermons", matchPath = "/sermons")

        whenever(siteNavigationItemRepository.findAllByVisibleTrueOrderBySortOrderAscIdAsc()).thenReturn(
            listOf(sermonRoot),
        )
        whenever(contentMenuRepository.findAllByActiveTrueAndNavigationVisibleTrueAndStatusOrderBySortOrderAscIdAsc(ContentMenuStatus.PUBLISHED))
            .thenReturn(
                listOf(
                    ContentMenu(
                        id = 101L,
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
                        id = 102L,
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

        val response = service.getNavigation()

        assertThat(response.groups).hasSize(1)
        assertThat(response.groups[0].key).isEqualTo("navigation-10")
        assertThat(response.groups[0].defaultLandingHref).isEqualTo("/sermons/messages")
        assertThat(response.groups[0].items.map { it.href }).containsExactly("/sermons/messages", "/sermons/shorts")
        assertThat(response.groups[0].items.map { it.key }).containsExactly("sermons-messages", "sermons-shorts")
    }

    @Test
    fun `getNavigation should only use site navigation repository for roots`() {
        val aboutRoot = item(id = 1L, label = "교회 소개", href = "/about", matchPath = "/about")
        whenever(siteNavigationItemRepository.findAllByVisibleTrueOrderBySortOrderAscIdAsc()).thenReturn(listOf(aboutRoot))

        service.getNavigation()

        verifyNoInteractions(contentMenuRepository)
    }

    private fun item(
        id: Long,
        label: String,
        href: String,
        matchPath: String? = null,
        parentId: Long? = null,
        defaultLanding: Boolean = false,
        linkType: NavigationLinkType = NavigationLinkType.INTERNAL,
    ): SiteNavigationItem = SiteNavigationItem(
        id = id,
        parentId = parentId,
        label = label,
        href = href,
        matchPath = matchPath,
        linkType = linkType,
        defaultLanding = defaultLanding,
        visible = true,
    )
}
