package kr.or.thejejachurch.api.navigation.application

import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationItemDto
import kr.or.thejejachurch.api.navigation.interfaces.dto.AdminNavigationUpsertRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class AdminNavigationCommandServiceTest {

    @Test
    fun `admin navigation request and response dto should not expose navigation set or menu key`() {
        val requestFields = AdminNavigationUpsertRequest::class.java.declaredFields.map { it.name }
        val responseFields = AdminNavigationItemDto::class.java.declaredFields.map { it.name }

        assertThat(requestFields)
            .doesNotContain("navigationSetId")
            .doesNotContain("menuKey")

        assertThat(responseFields)
            .doesNotContain("navigationSetId")
            .doesNotContain("menuKey")
    }

    @Test
    fun `command service source should no longer depend on navigation sets or menu keys`() {
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

        val source = Files.readString(sourcePath).lowercase()

        assertThat(source).doesNotContain("navigationsetid")
        assertThat(source).doesNotContain("menukey")
        assertThat(source).doesNotContain("sitenavigationsetrepository")
    }
}
