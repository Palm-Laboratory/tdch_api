package kr.or.thejejachurch.api.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

class FlywayMigrationChecksumManifestTest {

    @Test
    fun `historical flyway migrations should be guarded by a checksum manifest`() {
        val manifest = javaClass.classLoader.getResourceAsStream("db/migration-checksums.txt")

        assertThat(manifest)
            .describedAs("Expected a checksum manifest for historical Flyway migrations")
            .isNotNull

        val expectedChecksums = manifest!!
            .bufferedReader()
            .useLines { lines ->
                lines
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .associate { line ->
                        val (filename, checksum) = line.split("=", limit = 2)
                        filename.trim() to checksum.trim()
                    }
            }

        val migrationDir = Path.of("src/main/resources/db/migration")
        val actualMigrationFiles = Files.list(migrationDir)
            .use { paths ->
                paths
                    .filter { it.isRegularFile() && it.name.matches(Regex("""V\d+__.*\.sql""")) }
                    .sorted(compareBy { versionOf(it.fileName.toString()) })
                    .toList()
            }

        assertThat(expectedChecksums.keys)
            .describedAs("Checksum manifest should enumerate every historical Flyway migration")
            .containsExactlyElementsOf(actualMigrationFiles.map { it.fileName.toString() })

        actualMigrationFiles.forEach { migrationFile ->
            val filename = migrationFile.fileName.toString()
            val actualChecksum = sha256(migrationFile)

            assertThat(actualChecksum)
                .describedAs(
                    "Historical Flyway migration %s changed. Add a new migration instead of editing an existing one.",
                    filename,
                )
                .isEqualTo(expectedChecksums.getValue(filename))
        }
    }

    private fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(Files.readAllBytes(path)).joinToString("") { "%02x".format(it) }
    }

    private fun versionOf(filename: String): Int =
        filename.substringAfter('V').substringBefore("__").toInt()
}
