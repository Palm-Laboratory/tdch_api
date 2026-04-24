package kr.or.thejejachurch.api.board.application

import kr.or.thejejachurch.api.board.domain.Board
import kr.or.thejejachurch.api.board.domain.BoardType
import kr.or.thejejachurch.api.board.domain.Post
import kr.or.thejejachurch.api.board.domain.PostAsset
import kr.or.thejejachurch.api.board.domain.PostAssetKind
import kr.or.thejejachurch.api.board.infrastructure.persistence.BoardRepository
import kr.or.thejejachurch.api.board.infrastructure.persistence.PostAssetRepository
import kr.or.thejejachurch.api.board.infrastructure.persistence.PostRepository
import kr.or.thejejachurch.api.common.config.UploadProperties
import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.menu.domain.MenuStatus
import kr.or.thejejachurch.api.menu.domain.MenuType
import kr.or.thejejachurch.api.menu.infrastructure.persistence.MenuItemRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class PublicBoardServiceTest {

    private val boardRepository: BoardRepository = mock()
    private val postRepository: PostRepository = mock()
    private val postAssetRepository: PostAssetRepository = mock()
    private val menuItemRepository: MenuItemRepository = mock()
    private val uploadProperties: UploadProperties = mock()

    private val service = PublicBoardService(
        boardRepository = boardRepository,
        postRepository = postRepository,
        postAssetRepository = postAssetRepository,
        menuItemRepository = menuItemRepository,
        uploadProperties = uploadProperties,
    )

    @Test
    fun `list posts throws not found when board slug does not exist`() {
        whenever(boardRepository.findBySlug("missing")).thenReturn(null)

        assertThrows<NotFoundException> {
            service.listPosts(boardSlug = "missing", page = 0, size = 20)
        }

        verify(menuItemRepository, never()).existsByTypeAndStatusAndBoardKey(
            eq(MenuType.BOARD),
            eq(MenuStatus.PUBLISHED),
            eq("missing"),
        )
        verify(postRepository, never()).findAllByBoardIdAndIsPublicTrueOrderByIsPinnedDescCreatedAtDescIdDesc(
            eq(10L),
            eq(PageRequest.of(0, 20)),
        )
    }

    @Test
    fun `list posts throws not found when board is not connected to a published board menu item`() {
        val board = board(slug = "notice")
        whenever(boardRepository.findBySlug("notice")).thenReturn(board)
        whenever(
            menuItemRepository.existsByTypeAndStatusAndBoardKey(
                MenuType.BOARD,
                MenuStatus.PUBLISHED,
                "notice",
            )
        ).thenReturn(false)

        assertThrows<NotFoundException> {
            service.listPosts(boardSlug = "notice", page = 0, size = 20)
        }

        verify(postRepository, never()).findAllByBoardIdAndIsPublicTrueOrderByIsPinnedDescCreatedAtDescIdDesc(
            eq(board.id!!),
            eq(PageRequest.of(0, 20)),
        )
    }

    @Test
    fun `list posts returns only public posts for a published board menu connection`() {
        val board = board(slug = "notice")
        val pageRequest = PageRequest.of(1, 2)
        val publicPost = post(id = 101L, boardId = board.id!!, title = "공개 소식")
        whenever(boardRepository.findBySlug("notice")).thenReturn(board)
        whenever(
            menuItemRepository.existsByTypeAndStatusAndBoardKey(
                MenuType.BOARD,
                MenuStatus.PUBLISHED,
                "notice",
            )
        ).thenReturn(true)
        whenever(
            postRepository.findAllByBoardIdAndIsPublicTrueOrderByIsPinnedDescCreatedAtDescIdDesc(
                board.id!!,
                pageRequest,
            )
        ).thenReturn(PageImpl(listOf(publicPost), pageRequest, 3))

        val result = service.listPosts(boardSlug = "notice", page = 1, size = 2)

        assertThat(result.page).isEqualTo(1)
        assertThat(result.size).isEqualTo(2)
        assertThat(result.totalElements).isEqualTo(3)
        assertThat(result.hasNext).isFalse()
        assertThat(result.posts).hasSize(1)
        assertThat(result.posts[0].id).isEqualTo(101L)
        assertThat(result.posts[0].title).isEqualTo("공개 소식")
        verify(postRepository).findAllByBoardIdAndIsPublicTrueOrderByIsPinnedDescCreatedAtDescIdDesc(
            board.id!!,
            pageRequest,
        )
        verify(postRepository, never()).findAllByBoardIdOrderByIsPinnedDescCreatedAtDescIdDesc(board.id!!)
    }

    @Test
    fun `list posts returns hasNext false on last page and hasNext true when more pages exist`() {
        val board = board(slug = "notice")
        whenever(boardRepository.findBySlug("notice")).thenReturn(board)
        whenever(
            menuItemRepository.existsByTypeAndStatusAndBoardKey(MenuType.BOARD, MenuStatus.PUBLISHED, "notice")
        ).thenReturn(true)

        // 마지막 페이지: page=0, size=3, total=3 → hasNext=false
        whenever(
            postRepository.findAllByBoardIdAndIsPublicTrueOrderByIsPinnedDescCreatedAtDescIdDesc(
                board.id!!,
                PageRequest.of(0, 3),
            )
        ).thenReturn(PageImpl(listOf(post(1L, board.id!!, title = "글1"), post(2L, board.id!!, title = "글2"), post(3L, board.id!!, title = "글3")), PageRequest.of(0, 3), 3))

        val lastPage = service.listPosts(boardSlug = "notice", page = 0, size = 3)
        assertThat(lastPage.hasNext).isFalse()

        // 다음 페이지 있음: page=0, size=2, total=3 → hasNext=true
        whenever(
            postRepository.findAllByBoardIdAndIsPublicTrueOrderByIsPinnedDescCreatedAtDescIdDesc(
                board.id!!,
                PageRequest.of(0, 2),
            )
        ).thenReturn(PageImpl(listOf(post(1L, board.id!!, title = "글1"), post(2L, board.id!!, title = "글2")), PageRequest.of(0, 2), 3))

        val firstPage = service.listPosts(boardSlug = "notice", page = 0, size = 2)
        assertThat(firstPage.hasNext).isTrue()
    }

    @Test
    fun `list posts receives menu id and returns only public posts under that menu`() {
        val board = board(slug = "notice")
        val pageRequest = PageRequest.of(0, 20)
        val publicPost = post(
            id = 101L,
            boardId = board.id!!,
            menuId = 1001L,
            title = "첫 번째 공지 메뉴 공개 글",
        )
        whenever(boardRepository.findBySlug("notice")).thenReturn(board)
        whenever(
            menuItemRepository.existsByIdAndTypeAndStatusAndBoardKey(
                1001L,
                MenuType.BOARD,
                MenuStatus.PUBLISHED,
                "notice",
            )
        ).thenReturn(true)
        whenever(
            postRepository.findAllByMenuIdAndIsPublicTrueOrderByIsPinnedDescCreatedAtDescIdDesc(
                1001L,
                pageRequest,
            )
        ).thenReturn(PageImpl(listOf(publicPost), pageRequest, 1))

        val result = service.listPosts(boardSlug = "notice", menuId = 1001L, page = 0, size = 20)

        assertThat(result.posts).hasSize(1)
        assertThat(result.posts[0].id).isEqualTo(101L)
        verify(postRepository).findAllByMenuIdAndIsPublicTrueOrderByIsPinnedDescCreatedAtDescIdDesc(1001L, pageRequest)
        verify(postRepository, never()).findAllByBoardIdAndIsPublicTrueOrderByIsPinnedDescCreatedAtDescIdDesc(
            board.id!!,
            pageRequest,
        )
    }

    @Test
    fun `get post throws not found for private post or post under another board`() {
        val board = board(slug = "notice")
        whenever(boardRepository.findBySlug("notice")).thenReturn(board)
        whenever(
            menuItemRepository.existsByTypeAndStatusAndBoardKey(
                MenuType.BOARD,
                MenuStatus.PUBLISHED,
                "notice",
            )
        ).thenReturn(true)
        whenever(postRepository.findByBoardIdAndIdAndIsPublicTrue(board.id!!, 99L)).thenReturn(null)

        assertThrows<NotFoundException> {
            service.getPost(boardSlug = "notice", postId = 99L)
        }

        verify(postAssetRepository, never()).findAllByPostIdOrderBySortOrderAscIdAsc(99L)
    }

    @Test
    fun `get post returns content and attached assets with public urls composed from upload base url`() {
        val board = board(slug = "notice")
        val post = post(
            id = 99L,
            boardId = board.id!!,
            title = "첨부 있는 글",
            contentJson = """{"type":"doc","content":[]}""",
            contentHtml = "<p>첨부 있는 글</p>",
        )
        val image = asset(
            id = 201L,
            postId = 99L,
            storedPath = "uploads/2026/notice/image.png",
            sortOrder = 0,
        )
        whenever(boardRepository.findBySlug("notice")).thenReturn(board)
        whenever(
            menuItemRepository.existsByTypeAndStatusAndBoardKey(
                MenuType.BOARD,
                MenuStatus.PUBLISHED,
                "notice",
            )
        ).thenReturn(true)
        whenever(postRepository.findByBoardIdAndIdAndIsPublicTrue(board.id!!, 99L)).thenReturn(post)
        whenever(postAssetRepository.findAllByPostIdOrderBySortOrderAscIdAsc(99L)).thenReturn(listOf(image))
        whenever(uploadProperties.publicBaseUrl).thenReturn("https://cdn.example.com/upload")

        val result = service.getPost(boardSlug = "notice", postId = 99L)

        assertThat(result.id).isEqualTo(99L)
        assertThat(result.title).isEqualTo("첨부 있는 글")
        assertThat(result.contentJson).isEqualTo("""{"type":"doc","content":[]}""")
        assertThat(result.contentHtml).isEqualTo("<p>첨부 있는 글</p>")
        assertThat(result.assets).hasSize(1)
        assertThat(result.assets[0].id).isEqualTo(201L)
        assertThat(result.assets[0].kind).isEqualTo(PostAssetKind.INLINE_IMAGE)
        assertThat(result.assets[0].storedPath).isEqualTo("uploads/2026/notice/image.png")
        assertThat(result.assets[0].publicUrl).isEqualTo(
            "https://cdn.example.com/upload/uploads/2026/notice/image.png"
        )
    }

    @Test
    fun `get post receives menu id and does not return a post from another menu with the same board`() {
        val board = board(slug = "notice")
        whenever(boardRepository.findBySlug("notice")).thenReturn(board)
        whenever(
            menuItemRepository.existsByIdAndTypeAndStatusAndBoardKey(
                1001L,
                MenuType.BOARD,
                MenuStatus.PUBLISHED,
                "notice",
            )
        ).thenReturn(true)
        whenever(postRepository.findByMenuIdAndIdAndIsPublicTrue(1001L, 99L)).thenReturn(null)

        assertThrows<NotFoundException> {
            service.getPost(boardSlug = "notice", menuId = 1001L, postId = 99L)
        }

        verify(postRepository).findByMenuIdAndIdAndIsPublicTrue(1001L, 99L)
        verify(postRepository, never()).findByBoardIdAndIdAndIsPublicTrue(board.id!!, 99L)
        verify(postAssetRepository, never()).findAllByPostIdOrderBySortOrderAscIdAsc(99L)
    }

    @Test
    fun `get post public url composition avoids duplicate slash when upload base url ends with slash`() {
        val board = board(slug = "notice")
        val post = post(id = 99L, boardId = board.id!!, title = "첨부 있는 글")
        val attachment = asset(
            id = 202L,
            postId = 99L,
            kind = PostAssetKind.FILE_ATTACHMENT,
            originalFilename = "bulletin.pdf",
            storedPath = "uploads/2026/notice/bulletin.pdf",
            sortOrder = 0,
        )
        whenever(boardRepository.findBySlug("notice")).thenReturn(board)
        whenever(
            menuItemRepository.existsByTypeAndStatusAndBoardKey(
                MenuType.BOARD,
                MenuStatus.PUBLISHED,
                "notice",
            )
        ).thenReturn(true)
        whenever(postRepository.findByBoardIdAndIdAndIsPublicTrue(board.id!!, 99L)).thenReturn(post)
        whenever(postAssetRepository.findAllByPostIdOrderBySortOrderAscIdAsc(99L)).thenReturn(listOf(attachment))
        whenever(uploadProperties.publicBaseUrl).thenReturn("https://cdn.example.com/upload/")

        val result = service.getPost(boardSlug = "notice", postId = 99L)

        assertThat(result.assets[0].storedPath).isEqualTo("uploads/2026/notice/bulletin.pdf")
        assertThat(result.assets[0].publicUrl).isEqualTo(
            "https://cdn.example.com/upload/uploads/2026/notice/bulletin.pdf"
        )
    }

    private fun board(
        id: Long = 10L,
        slug: String,
    ) = Board(
        id = id,
        slug = slug,
        title = "공지사항",
        type = BoardType.NOTICE,
    )

    private fun post(
        id: Long,
        boardId: Long,
        menuId: Long = 1001L,
        title: String,
        contentJson: String = """{"type":"doc"}""",
        contentHtml: String? = "<p>$title</p>",
        isPublic: Boolean = true,
    ) = Post(
        id = id,
        boardId = boardId,
        menuId = menuId,
        title = title,
        contentJson = contentJson,
        contentHtml = contentHtml,
        isPublic = isPublic,
        authorId = 1L,
    )

    private fun asset(
        id: Long,
        postId: Long,
        kind: PostAssetKind = PostAssetKind.INLINE_IMAGE,
        originalFilename: String = "image.png",
        storedPath: String,
        sortOrder: Int,
    ) = PostAsset(
        id = id,
        uploadedByActorId = 1L,
        kind = kind,
        originalFilename = originalFilename,
        storedPath = storedPath,
        byteSize = 123L,
        postId = postId,
        mimeType = if (kind == PostAssetKind.INLINE_IMAGE) "image/png" else "application/pdf",
        width = if (kind == PostAssetKind.INLINE_IMAGE) 640 else null,
        height = if (kind == PostAssetKind.INLINE_IMAGE) 480 else null,
        sortOrder = sortOrder,
    )
}
