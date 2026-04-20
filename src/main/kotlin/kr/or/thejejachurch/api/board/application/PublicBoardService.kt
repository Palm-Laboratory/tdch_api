package kr.or.thejejachurch.api.board.application

import kr.or.thejejachurch.api.board.domain.Board
import kr.or.thejejachurch.api.board.domain.Post
import kr.or.thejejachurch.api.board.domain.PostAsset
import kr.or.thejejachurch.api.board.infrastructure.persistence.BoardRepository
import kr.or.thejejachurch.api.board.infrastructure.persistence.PostAssetRepository
import kr.or.thejejachurch.api.board.infrastructure.persistence.PostRepository
import kr.or.thejejachurch.api.common.config.UploadProperties
import kr.or.thejejachurch.api.common.error.NotFoundException
import kr.or.thejejachurch.api.menu.domain.MenuStatus
import kr.or.thejejachurch.api.menu.domain.MenuType
import kr.or.thejejachurch.api.menu.infrastructure.persistence.MenuItemRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PublicBoardService(
    private val boardRepository: BoardRepository,
    private val postRepository: PostRepository,
    private val postAssetRepository: PostAssetRepository,
    private val menuItemRepository: MenuItemRepository,
    private val uploadProperties: UploadProperties,
) {

    @Transactional(readOnly = true)
    fun listPosts(boardSlug: String, page: Int, size: Int, menuId: Long? = null): PublicBoardPostListResult {
        val board = requirePublishedBoard(boardSlug, menuId)
        val boardId = requireBoardId(board)
        val pageRequest = PageRequest.of(page, size)
        val posts = menuId
            ?.let { postRepository.findAllByMenuIdAndIsPublicTrueOrderByIsPinnedDescCreatedAtDescIdDesc(it, pageRequest) }
            ?: postRepository.findAllByBoardIdAndIsPublicTrueOrderByIsPinnedDescCreatedAtDescIdDesc(boardId, pageRequest)

        return PublicBoardPostListResult(
            page = posts.number,
            size = posts.size,
            totalElements = posts.totalElements,
            hasNext = posts.totalElements > page.toLong() * size,
            posts = posts.content.map { it.toPublicSummary() },
        )
    }

    @Transactional(readOnly = true)
    fun getPost(boardSlug: String, postId: Long, menuId: Long? = null): PublicBoardPostDetail {
        val board = requirePublishedBoard(boardSlug, menuId)
        val boardId = requireBoardId(board)
        val post = if (menuId != null) {
            postRepository.findByMenuIdAndIdAndIsPublicTrue(menuId, postId)
        } else {
            postRepository.findByBoardIdAndIdAndIsPublicTrue(boardId, postId)
        }
            ?: throw NotFoundException("게시글을 찾을 수 없습니다. id=$postId")
        val assets = postAssetRepository.findAllByPostIdOrderBySortOrderAscIdAsc(postId)

        return PublicBoardPostDetail(
            id = post.id,
            boardId = post.boardId,
            menuId = post.menuId,
            title = post.title,
            contentJson = post.contentJson,
            contentHtml = post.contentHtml,
            isPinned = post.isPinned,
            publishedAt = post.publishedAt,
            createdAt = post.createdAt,
            updatedAt = post.updatedAt,
            assets = assets.map { it.toPublicAsset() },
        )
    }

    private fun requirePublishedBoard(slug: String, menuId: Long? = null): Board {
        val board = boardRepository.findBySlug(slug)
            ?: throw NotFoundException("게시판을 찾을 수 없습니다. slug=$slug")

        val exists = menuId?.let {
            menuItemRepository.existsByIdAndTypeAndStatusAndBoardKey(
                it,
                MenuType.BOARD,
                MenuStatus.PUBLISHED,
                slug,
            )
        } ?: menuItemRepository.existsByTypeAndStatusAndBoardKey(MenuType.BOARD, MenuStatus.PUBLISHED, slug)

        if (!exists) {
            throw NotFoundException("게시판을 찾을 수 없습니다. slug=$slug")
        }

        return board
    }

    private fun requireBoardId(board: Board): Long =
        board.id ?: throw IllegalStateException("게시판 id가 없습니다.")

    private fun Post.toPublicSummary(): PublicBoardPostSummary =
        PublicBoardPostSummary(
            id = id,
            boardId = boardId,
            menuId = menuId,
            title = title,
            contentHtml = contentHtml,
            isPinned = isPinned,
            publishedAt = publishedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun PostAsset.toPublicAsset(): PublicBoardPostAsset =
        PublicBoardPostAsset(
            id = id,
            kind = kind,
            originalFilename = originalFilename,
            storedPath = storedPath,
            publicUrl = publicUrl(storedPath),
            mimeType = mimeType,
            byteSize = byteSize,
            width = width,
            height = height,
            sortOrder = sortOrder,
        )

    private fun publicUrl(storedPath: String): String =
        "${uploadProperties.publicBaseUrl.trimEnd('/')}/${storedPath.trimStart('/')}"
}
