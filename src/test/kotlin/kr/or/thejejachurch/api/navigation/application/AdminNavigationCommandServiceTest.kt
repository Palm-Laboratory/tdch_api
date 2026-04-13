package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.navigation.domain.NavigationLinkType
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationItem
import kr.or.thejejachurch.api.navigation.domain.SiteNavigationSet
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationItemRepository
import kr.or.thejejachurch.api.navigation.infrastructure.persistence.SiteNavigationSetRepository
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationUpsertRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime

class AdminNavigationCommandServiceTest {

    private val siteNavigationItemRepository: SiteNavigationItemRepository = mock()
    private val siteNavigationSetRepository: SiteNavigationSetRepository = mock()
    private val service = AdminNavigationCommandService(
        siteNavigationItemRepository = siteNavigationItemRepository,
        siteNavigationSetRepository = siteNavigationSetRepository,
    )

    @Test
    fun `createNavigationItem creates root item`() {
        whenever(siteNavigationSetRepository.findById(1L)).thenReturn(
            java.util.Optional.of(SiteNavigationSet(id = 1L, setKey = "main", label = "메인 사이트 메뉴")),
        )
        whenever(siteNavigationItemRepository.existsByNavigationSetIdAndMenuKey(1L, "test-root")).thenReturn(false)
        whenever(siteNavigationItemRepository.save(any())).thenAnswer { invocation ->
            val item = invocation.getArgument<SiteNavigationItem>(0)
            SiteNavigationItem(
                id = 100L,
                navigationSetId = item.navigationSetId,
                parentId = item.parentId,
                menuKey = item.menuKey,
                label = item.label,
                href = item.href,
                matchPath = item.matchPath,
                linkType = item.linkType,
                visible = item.visible,
                headerVisible = item.headerVisible,
                mobileVisible = item.mobileVisible,
                lnbVisible = item.lnbVisible,
                breadcrumbVisible = item.breadcrumbVisible,
                defaultLanding = item.defaultLanding,
                sortOrder = item.sortOrder,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
        }

        val created = service.createNavigationItem(
            AdminNavigationUpsertRequest(
                navigationSetId = 1L,
                parentId = null,
                menuKey = "test-root",
                label = "테스트 루트",
                href = "/test",
                matchPath = "/test",
                linkType = "INTERNAL",
                visible = true,
                headerVisible = true,
                mobileVisible = true,
                lnbVisible = true,
                breadcrumbVisible = true,
                defaultLanding = false,
                sortOrder = 70,
            )
        )

        assertThat(created.id).isEqualTo(100L)
        assertThat(created.navigationSetId).isEqualTo(1L)
        assertThat(created.menuKey).isEqualTo("test-root")
        assertThat(created.parentId).isNull()
    }

    @Test
    fun `createNavigationItem rejects duplicate menu key`() {
        whenever(siteNavigationSetRepository.findById(1L)).thenReturn(
            java.util.Optional.of(SiteNavigationSet(id = 1L, setKey = "main", label = "메인 사이트 메뉴")),
        )
        whenever(siteNavigationItemRepository.existsByNavigationSetIdAndMenuKey(1L, "about")).thenReturn(true)

        assertThatThrownBy {
            service.createNavigationItem(
                AdminNavigationUpsertRequest(
                    navigationSetId = 1L,
                    parentId = null,
                    menuKey = "about",
                    label = "중복 메뉴",
                    href = "/dup",
                    linkType = "INTERNAL",
                )
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("이미 사용 중인 menuKey")
    }

    @Test
    fun `updateNavigationItem updates existing item`() {
        whenever(siteNavigationItemRepository.findById(10L)).thenReturn(
            java.util.Optional.of(
                SiteNavigationItem(
                    id = 10L,
                    navigationSetId = 1L,
                    parentId = null,
                    menuKey = "test-root",
                    label = "테스트",
                    href = "/test",
                    matchPath = "/test",
                    linkType = NavigationLinkType.INTERNAL,
                )
            )
        )
        whenever(siteNavigationSetRepository.findById(1L)).thenReturn(
            java.util.Optional.of(SiteNavigationSet(id = 1L, setKey = "main", label = "메인 사이트 메뉴")),
        )
        whenever(siteNavigationItemRepository.existsByNavigationSetIdAndMenuKeyAndIdNot(1L, "test-root-updated", 10L)).thenReturn(false)
        whenever(siteNavigationItemRepository.save(any())).thenAnswer { invocation ->
            invocation.getArgument<SiteNavigationItem>(0).copyForTest(id = 10L)
        }

        val updated = service.updateNavigationItem(
            10L,
            AdminNavigationUpsertRequest(
                navigationSetId = 1L,
                parentId = null,
                menuKey = "test-root-updated",
                label = "수정된 테스트",
                href = "/test-updated",
                matchPath = "/test-updated",
                linkType = "INTERNAL",
                visible = true,
                headerVisible = false,
                mobileVisible = true,
                lnbVisible = true,
                breadcrumbVisible = false,
                defaultLanding = false,
                sortOrder = 80,
            )
        )

        assertThat(updated.menuKey).isEqualTo("test-root-updated")
        assertThat(updated.label).isEqualTo("수정된 테스트")
        assertThat(updated.href).isEqualTo("/test-updated")
        assertThat(updated.headerVisible).isFalse()
        assertThat(updated.breadcrumbVisible).isFalse()
    }

    @Test
    fun `updateNavigationItem rejects moving root with children to child`() {
        whenever(siteNavigationItemRepository.findById(10L)).thenReturn(
            java.util.Optional.of(
                SiteNavigationItem(
                    id = 10L,
                    navigationSetId = 1L,
                    parentId = null,
                    menuKey = "test-root",
                    label = "테스트",
                    href = "/test",
                    matchPath = "/test",
                    linkType = NavigationLinkType.INTERNAL,
                )
            )
        )
        whenever(siteNavigationSetRepository.findById(1L)).thenReturn(
            java.util.Optional.of(SiteNavigationSet(id = 1L, setKey = "main", label = "메인 사이트 메뉴")),
        )
        whenever(siteNavigationItemRepository.existsByNavigationSetIdAndMenuKeyAndIdNot(1L, "test-root", 10L)).thenReturn(false)
        whenever(siteNavigationItemRepository.findByNavigationSetIdAndId(1L, 20L)).thenReturn(
            SiteNavigationItem(
                id = 20L,
                navigationSetId = 1L,
                parentId = null,
                menuKey = "about",
                label = "교회 소개",
                href = "/about",
                matchPath = "/about",
                linkType = NavigationLinkType.INTERNAL,
            )
        )
        whenever(siteNavigationItemRepository.existsByParentId(10L)).thenReturn(true)

        assertThatThrownBy {
            service.updateNavigationItem(
                10L,
                AdminNavigationUpsertRequest(
                    navigationSetId = 1L,
                    parentId = 20L,
                    menuKey = "test-root",
                    label = "테스트",
                    href = "/test",
                    matchPath = "/test",
                    linkType = "INTERNAL",
                    defaultLanding = false,
                )
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("하위 메뉴가 있는 1depth 메뉴는 2depth로 변경할 수 없습니다")
    }

    @Test
    fun `updateNavigationItem rejects duplicate default landing in same parent`() {
        whenever(siteNavigationItemRepository.findById(11L)).thenReturn(
            java.util.Optional.of(
                SiteNavigationItem(
                    id = 11L,
                    navigationSetId = 1L,
                    parentId = 20L,
                    menuKey = "about-pastor",
                    label = "담임목사 소개",
                    href = "/about/pastor",
                    matchPath = "/about/pastor",
                    linkType = NavigationLinkType.INTERNAL,
                    defaultLanding = false,
                )
            )
        )
        whenever(siteNavigationSetRepository.findById(1L)).thenReturn(
            java.util.Optional.of(SiteNavigationSet(id = 1L, setKey = "main", label = "메인 사이트 메뉴")),
        )
        whenever(siteNavigationItemRepository.existsByNavigationSetIdAndMenuKeyAndIdNot(1L, "about-pastor", 11L)).thenReturn(false)
        whenever(siteNavigationItemRepository.findByNavigationSetIdAndId(1L, 20L)).thenReturn(
            SiteNavigationItem(
                id = 20L,
                navigationSetId = 1L,
                parentId = null,
                menuKey = "about",
                label = "교회 소개",
                href = "/about",
                matchPath = "/about",
                linkType = NavigationLinkType.INTERNAL,
            )
        )
        whenever(
            siteNavigationItemRepository.existsByNavigationSetIdAndParentIdAndDefaultLandingTrueAndIdNot(1L, 20L, 11L)
        ).thenReturn(true)

        assertThatThrownBy {
            service.updateNavigationItem(
                11L,
                AdminNavigationUpsertRequest(
                    navigationSetId = 1L,
                    parentId = 20L,
                    menuKey = "about-pastor",
                    label = "담임목사 소개",
                    href = "/about/pastor",
                    matchPath = "/about/pastor",
                    linkType = "INTERNAL",
                    defaultLanding = true,
                )
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("이미 기본 랜딩으로 지정된 하위 메뉴가 있습니다")
    }

    @Test
    fun `deleteNavigationItem deletes item without children`() {
        whenever(siteNavigationItemRepository.findById(10L)).thenReturn(
            java.util.Optional.of(
                SiteNavigationItem(
                    id = 10L,
                    navigationSetId = 1L,
                    parentId = null,
                    menuKey = "test-root",
                    label = "테스트",
                    href = "/test",
                    matchPath = "/test",
                    linkType = NavigationLinkType.INTERNAL,
                )
            )
        )
        whenever(siteNavigationItemRepository.existsByParentId(10L)).thenReturn(false)

        service.deleteNavigationItem(10L)

        verify(siteNavigationItemRepository).delete(any())
    }

    @Test
    fun `deleteNavigationItem rejects when children exist`() {
        whenever(siteNavigationItemRepository.findById(10L)).thenReturn(
            java.util.Optional.of(
                SiteNavigationItem(
                    id = 10L,
                    navigationSetId = 1L,
                    parentId = null,
                    menuKey = "test-root",
                    label = "테스트",
                    href = "/test",
                    matchPath = "/test",
                    linkType = NavigationLinkType.INTERNAL,
                )
            )
        )
        whenever(siteNavigationItemRepository.existsByParentId(10L)).thenReturn(true)

        assertThatThrownBy { service.deleteNavigationItem(10L) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("하위 메뉴가 있는 메뉴는 삭제할 수 없습니다")

        verify(siteNavigationItemRepository, never()).delete(any())
    }

    private fun SiteNavigationItem.copyForTest(id: Long): SiteNavigationItem = SiteNavigationItem(
        id = id,
        navigationSetId = navigationSetId,
        parentId = parentId,
        menuKey = menuKey,
        label = label,
        href = href,
        matchPath = matchPath,
        linkType = linkType,
        visible = visible,
        headerVisible = headerVisible,
        mobileVisible = mobileVisible,
        lnbVisible = lnbVisible,
        breadcrumbVisible = breadcrumbVisible,
        defaultLanding = defaultLanding,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
