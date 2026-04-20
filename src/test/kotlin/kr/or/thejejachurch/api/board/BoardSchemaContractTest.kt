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
    fun `V10 migration should scope posts to board menu`() {
        val migration = Path.of("src/main/resources/db/migration/V10__scope_posts_to_board_menu.sql")

        assertThat(migration).exists()

        val content = Files.readString(migration)
        val normalized = content.lowercase()

        assertThat(normalized).contains("alter table post")
        assertThat(normalized).contains("add column if not exists menu_id bigint")
        assertThat(normalized).contains("alter column menu_id set not null")
        assertThat(normalized).contains("foreign key (menu_id) references menu_item(id)")
        assertThat(normalized).contains("idx_post_menu_id_created_at")
        assertThat(normalized).contains("idx_post_menu_id_public_created_at")
        assertThat(normalized).contains("legacy-board-posts")
        assertThat(normalized).contains("'board'")
        assertThat(normalized).contains("'hidden'")
    }

    @Test
    fun `V11 migration should create menu scoped boards`() {
        val migration = Path.of("src/main/resources/db/migration/V11__scope_boards_to_board_menus.sql")

        assertThat(migration).exists()

        val content = Files.readString(migration)
        val normalized = content.lowercase()

        assertThat(normalized).contains("insert into board")
        assertThat(normalized).contains("concat('menu-', menu_item.id)")
        assertThat(normalized).contains("from menu_item")
        assertThat(normalized).contains("where menu_item.type = 'board'")
        assertThat(normalized).contains("update post")
        assertThat(normalized).contains("post.menu_id = menu_item.id")
        assertThat(normalized).contains("update menu_item")
        assertThat(normalized).contains("set board_key = concat('menu-', id)")
    }

    @Test
    fun `V12 migration should remove legacy seed boards`() {
        val migration = Path.of("src/main/resources/db/migration/V12__remove_legacy_seed_boards.sql")

        assertThat(migration).exists()

        val content = Files.readString(migration)
        val normalized = content.lowercase()

        assertThat(normalized).contains("delete from board")
        assertThat(normalized).contains("'notice', 'bulletin', 'album', 'general'")
        assertThat(normalized).contains("legacy-board-posts")
        assertThat(normalized).contains("delete from menu_item")
        assertThat(normalized).contains("not exists")
        assertThat(normalized).contains("post.board_id")
        assertThat(normalized).contains("post.menu_id")
        assertThat(normalized).contains("menu_item.board_key")
    }

    @Test
    fun `V13 migration should create board type table and attach boards to menus`() {
        val migration = Path.of("src/main/resources/db/migration/V13__create_board_type_and_attach_boards_to_menus.sql")

        assertThat(migration).exists()

        val content = Files.readString(migration)
        val normalized = content.lowercase()

        assertThat(normalized).contains("create table if not exists board_type")
        assertThat(normalized).contains("'notice', '공지사항'")
        assertThat(normalized).contains("'bulletin', '주보'")
        assertThat(normalized).contains("'album', '행사 앨범'")
        assertThat(normalized).contains("'general', '자유게시판'")
        assertThat(normalized).contains("add column if not exists menu_id")
        assertThat(normalized).contains("add column if not exists board_type_id")
        assertThat(normalized).contains("foreign key (board_type_id) references board_type(id)")
        assertThat(normalized).contains("foreign key (menu_id) references menu_item(id)")
        assertThat(normalized).contains("uq_board_menu_id")
        assertThat(normalized).contains("update menu_item")
        assertThat(normalized).contains("set board_key = board.slug")
    }

    @Test
    fun `V14 migration should remove orphan menu slug boards`() {
        val migration = Path.of("src/main/resources/db/migration/V14__remove_orphan_menu_slug_boards.sql")

        assertThat(migration).exists()

        val content = Files.readString(migration)
        val normalized = content.lowercase()

        assertThat(normalized).contains("update board")
        assertThat(normalized).contains("menu_item.type = 'board'")
        assertThat(normalized).contains("board.slug = concat('menu-', menu_item.id)")
        assertThat(normalized).contains("update menu_item")
        assertThat(normalized).contains("set board_key = board.slug")
        assertThat(normalized).contains("delete from board")
        assertThat(normalized).contains("slug like 'menu-%'")
        assertThat(normalized).contains("post.board_id")
        assertThat(normalized).contains("upload_token.board_id")
    }

    @Test
    fun `V15 migration should repair board menus whose board row is missing`() {
        val migration = Path.of("src/main/resources/db/migration/V15__repair_missing_menu_scoped_boards.sql")

        assertThat(migration).exists()

        val content = Files.readString(migration)
        val normalized = content.lowercase()

        assertThat(normalized).contains("missing_board_menu")
        assertThat(normalized).contains("menu_item.type = 'board'")
        assertThat(normalized).contains("board_by_menu.menu_id = menu_item.id")
        assertThat(normalized).contains("board_by_key.slug = menu_item.board_key")
        assertThat(normalized).contains("insert into board")
        assertThat(normalized).contains("where key = 'general'")
        assertThat(normalized).contains("menu_id")
        assertThat(normalized).contains("board_type_id")
        assertThat(normalized).contains("update menu_item")
        assertThat(normalized).contains("set board_key = board.slug")
        assertThat(normalized).contains("update post")
        assertThat(normalized).contains("post.menu_id = board.menu_id")
    }

    @Test
    fun `board repository should support deleting boards by menu ids before menu deletion`() {
        val repository = Path.of("src/main/kotlin/kr/or/thejejachurch/api/board/infrastructure/persistence/BoardRepository.kt")

        assertThat(repository).exists()

        val normalized = Files.readString(repository).lowercase()

        assertThat(normalized).contains("findallbymenuidin")
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
        assertThat(post).contains("menuid")
        assertThat(post).contains("name = \"menu_id\"")
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
