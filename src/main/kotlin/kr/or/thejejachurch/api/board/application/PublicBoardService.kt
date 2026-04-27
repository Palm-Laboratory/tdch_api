package kr.or.thejejachurch.api.board.application

import kr.or.thejejachurch.api.adminaccount.infrastructure.persistence.AdminAccountRepository
import kr.or.thejejachurch.api.board.domain.Board
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
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PublicBoardService(
    private val adminAccountRepository: AdminAccountRepository,
    private val boardRepository: BoardRepository,
    private val postRepository: PostRepository,
    private val postAssetRepository: PostAssetRepository,
    private val menuItemRepository: MenuItemRepository,
    private val uploadProperties: UploadProperties,
) {

    @Transactional(readOnly = true)
    fun listPosts(boardSlug: String, page: Int, size: Int, menuId: Long? = null, title: String? = null): PublicBoardPostListResult {
        val board = requirePublishedBoard(boardSlug, menuId)
        val boardId = requireBoardId(board)
        val pageRequest = PageRequest.of(page, size)
        val normalizedTitle = title?.trim()?.takeIf { it.isNotEmpty() }
        val posts = when {
            normalizedTitle != null && menuId != null ->
                postRepository.findPublicPostsByMenuIdAndTitle(menuId, normalizedTitle, pageRequest)

            normalizedTitle != null ->
                postRepository.findPublicPostsByBoardIdAndTitle(boardId, normalizedTitle, pageRequest)

            menuId != null ->
                postRepository.findAllByMenuIdAndIsPublicTrueOrderByIsPinnedDescCreatedAtDescIdDesc(menuId, pageRequest)

            else ->
                postRepository.findAllByBoardIdAndIsPublicTrueOrderByIsPinnedDescCreatedAtDescIdDesc(boardId, pageRequest)
        }
        val postIds = posts.content.mapNotNull { it.id }
        val assetsByPostId = if (postIds.isEmpty()) {
            emptyMap()
        } else {
            postAssetRepository.findAllByPostIdIn(postIds).groupBy { it.postId }
        }
        val authorNamesById = adminAccountRepository.findAllById(posts.content.map { it.authorId }.distinct())
            .associateBy({ it.id ?: -1L }, { it.displayName })

        return PublicBoardPostListResult(
            page = posts.number,
            size = posts.size,
            totalElements = posts.totalElements,
            hasNext = posts.hasNext(),
            posts = posts.content.map { post ->
                post.toPublicSummary(
                    authorName = authorNamesById[post.authorId] ?: "",
                    assets = assetsByPostId[post.id].orEmpty(),
                )
            },
        )
    }

    @Transactional
    fun getPost(boardSlug: String, postId: Long, menuId: Long? = null): PublicBoardPostDetail {
        val board = requirePublishedBoard(boardSlug, menuId)
        val boardId = requireBoardId(board)
        val post = if (menuId != null) {
            postRepository.findByMenuIdAndIdAndIsPublicTrue(menuId, postId)
        } else {
            postRepository.findByBoardIdAndIdAndIsPublicTrue(boardId, postId)
        }
            ?: throw NotFoundException("게시글을 찾을 수 없습니다. id=$postId")
        post.viewCount += 1
        val assets = postAssetRepository.findAllByPostIdOrderBySortOrderAscIdAsc(postId)
        val authorName = adminAccountRepository.findById(post.authorId)
            .map { it.displayName }
            .orElse("")

        return PublicBoardPostDetail(
            id = post.id,
            boardId = post.boardId,
            menuId = post.menuId,
            title = post.title,
            authorName = authorName,
            viewCount = post.viewCount,
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

    private fun Post.toPublicSummary(authorName: String, assets: List<PostAsset>): PublicBoardPostSummary =
        PublicBoardPostSummary(
            id = id,
            boardId = boardId,
            menuId = menuId,
            title = title,
            authorName = authorName,
            viewCount = viewCount,
            contentHtml = contentHtml,
            hasInlineImage = assets.any { it.kind == PostAssetKind.INLINE_IMAGE },
            hasVideoEmbed = hasVideoEmbed(),
            hasAttachments = assets.any { it.kind == PostAssetKind.FILE_ATTACHMENT },
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

    private fun Post.hasVideoEmbed(): Boolean =
        VIDEO_EMBED_REGEX.containsMatchIn(contentHtml.orEmpty()) || VIDEO_EMBED_REGEX.containsMatchIn(contentJson)

    companion object {
        private val VIDEO_EMBED_REGEX = Regex("""youtube(?:-nocookie)?\.com|youtu\.be|<iframe\b|"type"\s*:\s*"(?:youtube|youtubeEmbed)"""")
    }
}
