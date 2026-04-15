package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationItemDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationUpsertRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class AdminNavigationCommandServiceBehaviorTest {

    @Test
    fun `admin navigation upsert request should expose menu type and video page detail fields`() {
        val requestFields = AdminNavigationUpsertRequest::class.java.declaredFields.map { it.name }
        val responseFields = AdminNavigationItemDto::class.java.declaredFields.map { it.name }

        assertThat(requestFields)
            .contains("menuType")
            .contains("videoRootKey")
            .contains("landingMode")
            .contains("contentKindFilter")

        assertThat(responseFields)
            .contains("menuType")
            .contains("videoRootKey")
            .contains("landingMode")
            .contains("contentKindFilter")
    }

    @Test
    fun `command service source should depend on menu type based video page contract`() {
        val sourcePath = Path.of(
            "src",
            "main",
            "kotlin",
            "kr",
            "or",
            "thejejachurch",
            "api",
            "navigation",
            "application",
            "AdminNavigationCommandService.kt",
        )

        val source = Files.readString(sourcePath)

        assertThat(source).contains("menuType")
        assertThat(source).contains("videoRootKey")
        assertThat(source).contains("landingMode")
        assertThat(source).contains("contentKindFilter")
    }
}
