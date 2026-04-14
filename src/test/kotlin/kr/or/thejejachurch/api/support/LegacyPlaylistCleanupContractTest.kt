package kr.or.thejejachurch.api.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class LegacyPlaylistCleanupContractTest {

    @Test
    fun `runtime config should no longer advertise legacy playlist env vars`() {
        val runtimeConfigFiles = listOf(
            ".env.example",
            ".env.production.example",
            "deploy/docker-compose.prod.yml",
            "src/main/resources/application.yml",
            "secrets-manager.prod.example.json",
        )

        val content = runtimeConfigFiles.joinToString("\n") { readFile(it) }

        assertThat(content).doesNotContain("YOUTUBE_MESSAGES_PLAYLIST_ID")
        assertThat(content).doesNotContain("YOUTUBE_BETTER_DEVOTION_PLAYLIST_ID")
        assertThat(content).doesNotContain("YOUTUBE_ITS_OKAY_PLAYLIST_ID")
    }

    @Test
    fun `playlist bootstrap wiring should be removed from the codebase`() {
        assertThat(Path.of("src/main/kotlin/kr/or/thejejachurch/api/media/application/bootstrap/PlaylistBootstrapService.kt"))
            .doesNotExist()
        assertThat(Path.of("src/main/kotlin/kr/or/thejejachurch/api/media/application/bootstrap/PlaylistBootstrapRunner.kt"))
            .doesNotExist()
    }

    @Test
    fun `media query should not hardcode legacy sermon site keys`() {
        val content = readFile("src/main/kotlin/kr/or/thejejachurch/api/media/application/MediaQueryService.kt")

        assertThat(content).doesNotContain("siteKey = \"messages\"")
        assertThat(content).doesNotContain("siteKey = \"better-devotion\"")
        assertThat(content).doesNotContain("siteKey = \"its-okay\"")
    }

    private fun readFile(relativePath: String): String =
        Files.readString(Path.of(relativePath))
}
