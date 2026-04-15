package kr.or.thejejachurch.api.navigation.infrastructure.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

class SiteNavigationMigrationContractTest {

    @Test
    fun `clean navigation baseline should start with site navigation create migration and no seed migration`() {
        val createMigrationPath = Path.of(
            "src",
            "main",
            "resources",
            "db",
            "migration",
            "V1__create_site_navigation.sql",
        )

        assertThat(Files.exists(createMigrationPath))
            .describedAs("Expected a clean create migration at %s", createMigrationPath)
            .isTrue()

        val migrationDir = Path.of("src", "main", "resources", "db", "migration")
        val siteNavigationSeedFiles = Files.list(migrationDir)
            .use { paths ->
                paths
                    .filter { it.isRegularFile() && it.name.contains("seed_site_navigation") }
                    .map { it.fileName.toString() }
                    .toList()
            }

        assertThat(siteNavigationSeedFiles)
            .describedAs("A clean baseline should not keep a site navigation seed migration")
            .isEmpty()
    }

    @Test
    fun `clean navigation baseline should keep a contiguous new migration sequence`() {
        val migrationDir = Path.of("src", "main", "resources", "db", "migration")
        val migrationFileNames = Files.list(migrationDir)
            .use { paths ->
                paths
                    .filter { it.isRegularFile() && it.name.matches(Regex("""V\d+__.*\.sql""")) }
                    .map { it.fileName.toString() }
                    .sorted(compareBy { versionOf(it) })
                    .toList()
            }

        assertThat(migrationFileNames)
            .describedAs("A clean baseline should begin with a contiguous V1 site navigation migration sequence")
            .containsExactly(
                "V1__create_site_navigation.sql",
                "V2__create_content_menu.sql",
                "V3__create_youtube_playlist.sql",
                "V4__create_youtube_video.sql",
                "V5__create_playlist_video.sql",
                "V6__create_video_metadata.sql",
                "V7__seed_content_menus.sql",
                "V8__alter_video_metadata_scripture_to_text.sql",
                "V9__add_script_body_to_video_metadata.sql",
                "V10__create_admin_account.sql",
                "V11__create_youtube_sync_job_tables.sql",
                "V12__extend_content_menu_for_sermon_navigation.sql",
            )
    }

    private fun versionOf(filename: String): Int =
        filename.substringAfter('V').substringBefore("__").toInt()
}
