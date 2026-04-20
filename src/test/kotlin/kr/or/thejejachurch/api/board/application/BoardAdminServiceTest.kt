package kr.or.thejejachurch.api.board.application

import kr.or.thejejachurch.api.adminaccount.domain.AdminAccount
import kr.or.thejejachurch.api.adminaccount.domain.AdminAccountRole
import kr.or.thejejachurch.api.adminaccount.infrastructure.persistence.AdminAccountRepository
import kr.or.thejejachurch.api.board.domain.Board
import kr.or.thejejachurch.api.board.domain.BoardType
import kr.or.thejejachurch.api.board.domain.Post
import kr.or.thejejachurch.api.board.domain.PostAsset
import kr.or.thejejachurch.api.board.domain.PostAssetKind
import kr.or.thejejachurch.api.board.infrastructure.persistence.BoardRepository
import kr.or.thejejachurch.api.board.infrastructure.persistence.PostAssetRepository
import kr.or.thejejachurch.api.board.infrastructure.persistence.PostRepository
import kr.or.thejejachurch.api.common.error.ForbiddenException
import kr.or.thejejachurch.api.common.error.NotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class BoardAdminServiceTest {

    private val boardRepository: BoardRepository = mock()
    private val postRepository: PostRepository = mock()
    private val postAssetRepository: PostAssetRepository = mock()
    private val adminAccountRepository: AdminAccountRepository = mock()

    private val service = BoardAdminService(
        boardRepository = boardRepository,
        postRepository = postRepository,
        postAssetRepository = postAssetRepository,
        adminAccountRepository = adminAccountRepository,
    )

    @Test
    fun `list boards requires active admin and returns board summaries`() {
        whenever(adminAccountRepository.findById(1L)).thenReturn(Optional.of(activeAdmin(1L)))
        whenever(boardRepository.findAll()).thenReturn(
            listOf(
                Board(
                    id = 10L,
                    slug = "notice",
                    title = "공지사항",
                    type = BoardType.NOTICE,
                    description = "교회 공지",
                ),
                Board(
                    id = 20L,
                    slug = "album",
                    title = "앨범",
                    type = BoardType.ALBUM,
                    description = null,
                ),
            )
        )

        val result = service.listBoards(actorId = 1L)

        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo(10L)
        assertThat(result[0].slug).isEqualTo("notice")
        assertThat(result[0].title).isEqualTo("공지사항")
        assertThat(result[0].type).isEqualTo(BoardType.NOTICE)
        assertThat(result[0].description).isEqualTo("교회 공지")
        assertThat(result[1].slug).isEqualTo("album")
    }

    @Test
    fun `list boards rejects inactive admin`() {
        whenever(adminAccountRepository.findById(1L)).thenReturn(
            Optional.of(activeAdmin(1L).copyForTest(active = false))
        )

        assertThrows<ForbiddenException> {
            service.listBoards(actorId = 1L)
        }

        verify(boardRepository, never()).findAll()
    }

    @Test
    fun `list posts throws not found for unknown board slug`() {
        whenever(adminAccountRepository.findById(1L)).thenReturn(Optional.of(activeAdmin(1L)))
        whenever(boardRepository.findBySlug("missing")).thenReturn(null)

        assertThrows<NotFoundException> {
            service.listPosts(actorId = 1L, boardSlug = "missing")
        }

        verify(postRepository, never()).findAll()
    }

    @Test
    fun `create post requires active admin validates json creates post and attaches provided assets`() {
        val board = Board(id = 10L, slug = "notice", title = "공지사항", type = BoardType.NOTICE)
        val asset1 = uploadedAsset(id = 100L, actorId = 1L)
        val asset2 = uploadedAsset(id = 200L, actorId = 1L)
        whenever(adminAccountRepository.findById(1L)).thenReturn(Optional.of(activeAdmin(1L)))
        whenever(boardRepository.findBySlug("notice")).thenReturn(board)
        whenever(postAssetRepository.findAllById(listOf(100L, 200L))).thenReturn(listOf(asset1, asset2))
        whenever(postRepository.save(any())).thenReturn(
            Post(
                id = 99L,
                boardId = 10L,
                title = "새 소식",
                contentJson = """{"type":"doc","content":[]}""",
                contentHtml = "<p>새 소식</p>",
                isPublic = true,
                authorId = 1L,
            )
        )

        val result = service.createPost(
            actorId = 1L,
            boardSlug = "notice",
            command = BoardPostSaveCommand(
                title = "새 소식",
                contentJson = """{"type":"doc","content":[]}""",
                contentHtml = "<p>새 소식</p>",
                isPublic = true,
                assetIds = listOf(100L, 200L),
            ),
        )

        val savedPost = argumentCaptor<Post>().apply {
            verify(postRepository).save(capture())
        }.firstValue
        assertThat(savedPost.boardId).isEqualTo(10L)
        assertThat(savedPost.title).isEqualTo("새 소식")
        assertThat(savedPost.contentJson).isEqualTo("""{"type":"doc","content":[]}""")
        assertThat(savedPost.contentHtml).isEqualTo("<p>새 소식</p>")
        assertThat(savedPost.isPublic).isTrue()
        assertThat(savedPost.authorId).isEqualTo(1L)

        val savedAssets = argumentCaptor<Iterable<PostAsset>>().apply {
            verify(postAssetRepository).saveAll(capture())
        }.firstValue.toList()
        assertThat(savedAssets.map { it.id }).containsExactly(100L, 200L)
        assertThat(savedAssets.map { it.postId }).containsExactly(99L, 99L)
        assertThat(savedAssets.map { it.sortOrder }).containsExactly(0, 1)
        assertThat(result.id).isEqualTo(99L)
    }

    @Test
    fun `create post attaches inline image asset referenced only in content json`() {
        val board = Board(id = 10L, slug = "notice", title = "공지사항", type = BoardType.NOTICE)
        val asset = uploadedAsset(id = 100L, actorId = 1L)
        whenever(adminAccountRepository.findById(1L)).thenReturn(Optional.of(activeAdmin(1L)))
        whenever(boardRepository.findBySlug("notice")).thenReturn(board)
        whenever(postAssetRepository.findById(100L)).thenReturn(Optional.of(asset))
        whenever(postRepository.save(any())).thenReturn(
            Post(
                id = 99L,
                boardId = 10L,
                title = "새 소식",
                contentJson = docWithImage(assetId = 100L),
                contentHtml = "<p>새 소식</p>",
                isPublic = true,
                authorId = 1L,
            )
        )

        service.createPost(
            actorId = 1L,
            boardSlug = "notice",
            command = BoardPostSaveCommand(
                title = "새 소식",
                contentJson = docWithImage(assetId = 100L),
                contentHtml = "<p>새 소식</p>",
                isPublic = true,
                assetIds = emptyList(),
            ),
        )

        val savedAssets = argumentCaptor<Iterable<PostAsset>>().apply {
            verify(postAssetRepository).saveAll(capture())
        }.firstValue.toList()
        assertThat(savedAssets.map { it.id }).containsExactly(100L)
        assertThat(savedAssets.map { it.postId }).containsExactly(99L)
        assertThat(savedAssets.map { it.sortOrder }).containsExactly(0)
    }

    @Test
    fun `create post deduplicates inline image asset when also provided in command asset ids`() {
        val board = Board(id = 10L, slug = "notice", title = "공지사항", type = BoardType.NOTICE)
        val asset = uploadedAsset(id = 100L, actorId = 1L)
        whenever(adminAccountRepository.findById(1L)).thenReturn(Optional.of(activeAdmin(1L)))
        whenever(boardRepository.findBySlug("notice")).thenReturn(board)
        whenever(postAssetRepository.findById(100L)).thenReturn(Optional.of(asset))
        whenever(postAssetRepository.findAllById(listOf(100L))).thenReturn(listOf(asset))
        whenever(postRepository.save(any())).thenReturn(
            Post(
                id = 99L,
                boardId = 10L,
                title = "새 소식",
                contentJson = docWithImage(assetId = 100L),
                contentHtml = "<p>새 소식</p>",
                isPublic = true,
                authorId = 1L,
            )
        )

        service.createPost(
            actorId = 1L,
            boardSlug = "notice",
            command = BoardPostSaveCommand(
                title = "새 소식",
                contentJson = docWithImage(assetId = 100L),
                contentHtml = "<p>새 소식</p>",
                isPublic = true,
                assetIds = listOf(100L),
            ),
        )

        val savedAssets = argumentCaptor<Iterable<PostAsset>>().apply {
            verify(postAssetRepository).saveAll(capture())
        }.firstValue.toList()
        assertThat(savedAssets.map { it.id }).containsExactly(100L)
        assertThat(savedAssets.map { it.postId }).containsExactly(99L)
        assertThat(savedAssets.map { it.sortOrder }).containsExactly(0)
    }

    @Test
    fun `create post rejects invalid content json`() {
        whenever(adminAccountRepository.findById(1L)).thenReturn(Optional.of(activeAdmin(1L)))
        whenever(boardRepository.findBySlug("notice")).thenReturn(
            Board(id = 10L, slug = "notice", title = "공지사항", type = BoardType.NOTICE)
        )

        assertThrows<IllegalArgumentException> {
            service.createPost(
                actorId = 1L,
                boardSlug = "notice",
                command = BoardPostSaveCommand(
                    title = "새 소식",
                    contentJson = "{not-json",
                    contentHtml = "<p>새 소식</p>",
                    isPublic = true,
                    assetIds = emptyList(),
                ),
            )
        }

        verify(postRepository, never()).save(any())
    }

    @Test
    fun `create post rejects public url only image content before saving post`() {
        whenever(adminAccountRepository.findById(1L)).thenReturn(Optional.of(activeAdmin(1L)))
        whenever(boardRepository.findBySlug("notice")).thenReturn(
            Board(id = 10L, slug = "notice", title = "공지사항", type = BoardType.NOTICE)
        )

        assertThrows<IllegalArgumentException> {
            service.createPost(
                actorId = 1L,
                boardSlug = "notice",
                command = BoardPostSaveCommand(
                    title = "새 소식",
                    contentJson = """
                        {
                          "type": "doc",
                          "content": [
                            {
                              "type": "image",
                              "attrs": {
                                "publicUrl": "https://cdn.example.com/uploads/asset-100.png"
                              }
                            }
                          ]
                        }
                    """.trimIndent(),
                    contentHtml = null,
                    isPublic = true,
                    assetIds = emptyList(),
                ),
            )
        }

        verify(postRepository, never()).save(any())
    }

    @Test
    fun `create post rejects asset uploaded by another actor`() {
        whenever(adminAccountRepository.findById(1L)).thenReturn(Optional.of(activeAdmin(1L)))
        whenever(boardRepository.findBySlug("notice")).thenReturn(
            Board(id = 10L, slug = "notice", title = "공지사항", type = BoardType.NOTICE)
        )
        whenever(postAssetRepository.findAllById(listOf(100L))).thenReturn(
            listOf(uploadedAsset(id = 100L, actorId = 2L))
        )

        assertThrows<ForbiddenException> {
            service.createPost(
                actorId = 1L,
                boardSlug = "notice",
                command = BoardPostSaveCommand(
                    title = "새 소식",
                    contentJson = """{"type":"doc"}""",
                    contentHtml = null,
                    isPublic = false,
                    assetIds = listOf(100L),
                ),
            )
        }

        verify(postRepository, never()).save(any())
    }

    @Test
    fun `create post rejects asset already assigned to a different post`() {
        whenever(adminAccountRepository.findById(1L)).thenReturn(Optional.of(activeAdmin(1L)))
        whenever(boardRepository.findBySlug("notice")).thenReturn(
            Board(id = 10L, slug = "notice", title = "공지사항", type = BoardType.NOTICE)
        )
        whenever(postAssetRepository.findAllById(listOf(100L))).thenReturn(
            listOf(uploadedAsset(id = 100L, actorId = 1L, postId = 77L))
        )

        assertThrows<ForbiddenException> {
            service.createPost(
                actorId = 1L,
                boardSlug = "notice",
                command = BoardPostSaveCommand(
                    title = "새 소식",
                    contentJson = """{"type":"doc"}""",
                    contentHtml = null,
                    isPublic = false,
                    assetIds = listOf(100L),
                ),
            )
        }

        verify(postRepository, never()).save(any())
    }

    @Test
    fun `update post rejects posts that are not under the requested board slug`() {
        whenever(adminAccountRepository.findById(1L)).thenReturn(Optional.of(activeAdmin(1L)))
        whenever(boardRepository.findBySlug("notice")).thenReturn(
            Board(id = 10L, slug = "notice", title = "공지사항", type = BoardType.NOTICE)
        )
        whenever(postRepository.findById(99L)).thenReturn(
            Optional.of(
                Post(
                    id = 99L,
                    boardId = 20L,
                    title = "다른 게시판 글",
                    contentJson = """{"type":"doc"}""",
                    authorId = 1L,
                )
            )
        )

        assertThrows<NotFoundException> {
            service.updatePost(
                actorId = 1L,
                boardSlug = "notice",
                postId = 99L,
                command = BoardPostSaveCommand(
                    title = "수정",
                    contentJson = """{"type":"doc"}""",
                    contentHtml = null,
                    isPublic = true,
                    assetIds = emptyList(),
                ),
            )
        }

        verify(postRepository, never()).save(any())
    }

    @Test
    fun `get post returns attached assets ordered by sort order and id`() {
        val board = Board(id = 10L, slug = "notice", title = "공지사항", type = BoardType.NOTICE)
        whenever(adminAccountRepository.findById(1L)).thenReturn(Optional.of(activeAdmin(1L)))
        whenever(boardRepository.findBySlug("notice")).thenReturn(board)
        whenever(postRepository.findById(99L)).thenReturn(
            Optional.of(
                Post(
                    id = 99L,
                    boardId = 10L,
                    title = "첨부 있는 글",
                    contentJson = """{"type":"doc"}""",
                    contentHtml = "<p>첨부 있는 글</p>",
                    isPublic = true,
                    authorId = 1L,
                )
            )
        )
        val image = uploadedAsset(id = 100L, actorId = 1L, postId = 99L).apply {
            sortOrder = 0
        }
        val download = uploadedAsset(id = 101L, actorId = 1L, postId = 99L).apply {
            kind = PostAssetKind.FILE_ATTACHMENT
            originalFilename = "bulletin.pdf"
            storedPath = "uploads/bulletin.pdf"
            mimeType = "application/pdf"
            byteSize = 456L
            width = null
            height = null
            sortOrder = 1
        }
        whenever(postAssetRepository.findAllByPostIdOrderBySortOrderAscIdAsc(99L)).thenReturn(
            listOf(image, download)
        )

        val result = service.getPost(actorId = 1L, boardSlug = "notice", postId = 99L)

        assertThat(result.assets).hasSize(2)
        assertThat(result.assets.map { it.id }).containsExactly(100L, 101L)
        assertThat(result.assets.map { it.kind }).containsExactly(PostAssetKind.INLINE_IMAGE, PostAssetKind.FILE_ATTACHMENT)
        assertThat(result.assets.map { it.originalFilename }).containsExactly("asset-100.png", "bulletin.pdf")
        assertThat(result.assets.map { it.storedPath }).containsExactly("uploads/asset-100.png", "uploads/bulletin.pdf")
        assertThat(result.assets.map { it.mimeType }).containsExactly("image/png", "application/pdf")
        assertThat(result.assets.map { it.byteSize }).containsExactly(123L, 456L)
        assertThat(result.assets.map { it.width }).containsExactly(640, null)
        assertThat(result.assets.map { it.height }).containsExactly(480, null)
        assertThat(result.assets.map { it.sortOrder }).containsExactly(0, 1)
        verify(postAssetRepository).findAllByPostIdOrderBySortOrderAscIdAsc(99L)
    }

    @Test
    fun `delete post deletes the post after active admin and board ownership checks`() {
        val board = Board(id = 10L, slug = "notice", title = "공지사항", type = BoardType.NOTICE)
        val post = Post(
            id = 99L,
            boardId = 10L,
            title = "삭제할 글",
            contentJson = """{"type":"doc"}""",
            authorId = 1L,
        )
        whenever(adminAccountRepository.findById(1L)).thenReturn(Optional.of(activeAdmin(1L)))
        whenever(boardRepository.findBySlug("notice")).thenReturn(board)
        whenever(postRepository.findById(99L)).thenReturn(Optional.of(post))

        service.deletePost(actorId = 1L, boardSlug = "notice", postId = 99L)

        verify(postRepository).delete(post)
    }

    private fun activeAdmin(id: Long) = AdminAccount(
        id = id,
        username = "admin$id",
        displayName = "관리자$id",
        passwordHash = "hash",
        role = AdminAccountRole.ADMIN,
        active = true,
    )

    private fun AdminAccount.copyForTest(active: Boolean) = AdminAccount(
        id = id,
        username = username,
        displayName = displayName,
        passwordHash = passwordHash,
        role = role,
        active = active,
        lastLoginAt = lastLoginAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun uploadedAsset(
        id: Long,
        actorId: Long,
        postId: Long? = null,
    ) = PostAsset(
        id = id,
        uploadedByActorId = actorId,
        kind = PostAssetKind.INLINE_IMAGE,
        originalFilename = "asset-$id.png",
        storedPath = "uploads/asset-$id.png",
        byteSize = 123L,
        postId = postId,
        mimeType = "image/png",
        width = 640,
        height = 480,
    )

    private fun docWithImage(assetId: Long) = """
        {
          "type": "doc",
          "content": [
            {
              "type": "image",
              "attrs": {
                "assetId": $assetId,
                "storedPath": "uploads/asset-$assetId.png"
              }
            }
          ]
        }
    """.trimIndent()
}
