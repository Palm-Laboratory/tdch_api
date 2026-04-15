package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.media.domain.ContentKind
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.ContentMenuStatus
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.navigation.domain.NavigationLinkType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationItemRepository
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationItemDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AdminNavigationQueryServiceTest {

    private val contentMenuRepository: ContentMenuRepository = mock()
    private val siteNavigationItemRepository: SiteNavigationItemRepository = mock()
    private val service = AdminNavigationQueryService(
        siteNavigationItemRepository = siteNavigationItemRepository,
        contentMenuRepository = contentMenuRepository,
    )

    @Test
    fun `getNavigationItems includes hidden items when requested`() {
        val root = item(id = 1L, label = "교회 소개", href = "/about")
        val hiddenChild = item(
            id = 2L,
            parentId = 1L,
            label = "숨김 메뉴",
            href = "/about/hidden",
            visible = false,
        )
        whenever(siteNavigationItemRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(listOf(root, hiddenChild))

        val response = service.getNavigationItems(includeHidden = true)

        assertThat(response.groups).hasSize(1)
        assertThat(response.groups[0].children).hasSize(1)
        assertThat(response.groups[0].children[0].visible).isFalse()
    }

    @Test
    fun `getNavigationItems uses href sermon root marker instead of menu key`() {
        val root = item(id = 1L, label = "예배 영상", href = "/sermons")
        whenever(siteNavigationItemRepository.findAllByVisibleTrueOrderBySortOrderAscIdAsc()).thenReturn(listOf(root))
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
        assertThat(response.groups[0].children).hasSize(2)
        assertThat(response.groups[0].children.map { it.href }).containsExactly("/sermons/messages", "/sermons/shorts")
    }

    @Test
    fun `admin navigation dto should not expose navigation set or menu key`() {
        val fieldNames = AdminNavigationItemDto::class.java.declaredFields.map { it.name }

        assertThat(fieldNames)
            .doesNotContain("navigationSetId")
            .doesNotContain("menuKey")
    }

    @Test
    fun `getNavigationItem reads directly by id`() {
        val item = item(id = 5L, label = "교회 소개", href = "/about")
        whenever(siteNavigationItemRepository.findById(5L)).thenReturn(java.util.Optional.of(item))

        val response = service.getNavigationItem(5L)

        assertThat(response.id).isEqualTo(5L)
        assertThat(response.label).isEqualTo("교회 소개")
        verify(siteNavigationItemRepository).findById(5L)
    }

    private fun item(
        id: Long,
        label: String,
        href: String,
        parentId: Long? = null,
        visible: Boolean = true,
    ): SiteNavigationItem = SiteNavigationItem(
        id = id,
        parentId = parentId,
        label = label,
        href = href,
        matchPath = href,
        linkType = NavigationLinkType.INTERNAL,
        visible = visible,
    )
}
