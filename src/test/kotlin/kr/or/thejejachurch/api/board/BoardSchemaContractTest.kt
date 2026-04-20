package kr.or.thejejachurch.api.board

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class BoardSchemaContractTest {

    @Test
    fun `V9 migration should match the BUP-002 board schema contract`() {
        val migration = Path.of("src/main/resources/db/migration/V9__create_board_and_attachments.sql")

        assertThat(migration).exists()

        val content = Files.readString(migration)
        val normalized = content.lowercase()

        assertThat(normalized).contains("create table board")
        assertThat(normalized).contains("constraint chk_board_type")
        assertThat(normalized).contains("check (type in ('notice', 'bulletin', 'album', 'general'))")
        assertThat(normalized).doesNotContain("gallery")
        assertThat(normalized).doesNotContain("discussion")

        assertThat(normalized).contains("create table post")
        assertThat(normalized).contains("content_json jsonb not null")
        assertThat(normalized).contains("content_html")
        assertThat(normalized).contains("author_id")
        assertThat(normalized).contains("published_at")
        assertThat(normalized).contains("is_public boolean not null default true")

        assertThat(normalized).contains("create table post_asset")
        assertThat(normalized).contains("original_filename")
        assertThat(normalized).contains("stored_path")
        assertThat(normalized).contains("mime_type")
        assertThat(normalized).contains("byte_size")
        assertThat(normalized).contains("width")
        assertThat(normalized).contains("height")
        assertThat(normalized).contains("sort_order")
        assertThat(normalized).contains("uploaded_by_actor_id")
        assertThat(normalized).contains("unique (stored_path)")
        assertThat(normalized).contains("constraint chk_post_asset_kind")
        assertThat(normalized).contains("check (kind in ('inline_image', 'file_attachment'))")

        assertThat(normalized).contains("create table upload_token")
        assertThat(normalized).contains("actor_id")
        assertThat(normalized).contains("max_byte_size")
        assertThat(normalized).contains("allowed_mime_types jsonb")
        assertThat(normalized).contains("expires_at")
        assertThat(normalized).contains("used_at")
        assertThat(normalized).contains("created_at")
        assertThat(normalized).contains("token_hash")
        assertThat(normalized).contains("unique")
        assertThat(normalized).doesNotContain("raw_token")
        assertThat(normalized).doesNotContain("plain_token")
        assertThat(normalized).contains("constraint chk_upload_token_asset_kind")
        assertThat(normalized).contains("check (asset_kind in ('inline_image', 'file_attachment'))")

        listOf(
            "trg_board_updated_at",
            "trg_post_updated_at",
            "trg_post_asset_updated_at",
            "trg_upload_token_updated_at",
        ).forEach { triggerName ->
            assertThat(normalized).contains(triggerName)
        }
        assertThat(normalized).contains("execute function set_current_timestamp_updated_at()")
    }

    @Test
    fun `board post post asset and upload token entities should expose the BUP-002 field names`() {
        val expectedFiles = listOf(
            "src/main/kotlin/kr/or/thejejachurch/api/board/domain/Board.kt",
            "src/main/kotlin/kr/or/thejejachurch/api/board/domain/Post.kt",
            "src/main/kotlin/kr/or/thejejachurch/api/board/domain/PostAsset.kt",
            "src/main/kotlin/kr/or/thejejachurch/api/board/domain/UploadToken.kt",
        ).map(Path::of)

        expectedFiles.forEach { assertThat(it).exists() }

        val board = Files.readString(expectedFiles[0]).lowercase()
        val post = Files.readString(expectedFiles[1]).lowercase()
        val postAsset = Files.readString(expectedFiles[2]).lowercase()
        val uploadToken = Files.readString(expectedFiles[3]).lowercase()

        assertThat(board).contains("type: boardtype")
        assertThat(post).contains("contentjson")
        assertThat(post).contains("contenthtml")
        assertThat(post).contains("ispublic")
        assertThat(postAsset).contains("originalfilename")
        assertThat(postAsset).contains("storedpath")
        assertThat(postAsset).contains("bytesize")
        assertThat(postAsset).contains("width")
        assertThat(postAsset).contains("height")
        assertThat(postAsset).contains("sortorder")
        assertThat(uploadToken).contains("actorid")
        assertThat(uploadToken).contains("maxbytesize")
        assertThat(uploadToken).contains("usedat")
    }
}
