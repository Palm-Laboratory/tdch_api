package kr.or.thejejachurch.api.board.application

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import kr.or.thejejachurch.api.adminaccount.infrastructure.persistence.AdminAccountRepository
import kr.or.thejejachurch.api.board.domain.Board
import kr.or.thejejachurch.api.board.domain.Post
import kr.or.thejejachurch.api.board.domain.PostAsset
import kr.or.thejejachurch.api.board.infrastructure.persistence.BoardRepository
import kr.or.thejejachurch.api.board.infrastructure.persistence.PostAssetRepository
import kr.or.thejejachurch.api.board.infrastructure.persistence.PostRepository
import kr.or.thejejachurch.api.common.error.ForbiddenException
import kr.or.thejejachurch.api.common.error.NotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BoardAdminService(
    private val boardRepository: BoardRepository,
    private val postRepository: PostRepository,
    private val postAssetRepository: PostAssetRepository,
    private val adminAccountRepository: AdminAccountRepository,
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {

    @Transactional(readOnly = true)
    fun listBoards(actorId: Long): List<BoardAdminBoardSummary> {
        requireActiveAdmin(actorId)
        return boardRepository.findAll().map { board ->
            BoardAdminBoardSummary(
                id = board.id,
                slug = board.slug,
                title = board.title,
                type = board.type,
                description = board.description,
            )
        }
    }

    @Transactional(readOnly = true)
    fun listPosts(actorId: Long, boardSlug: String): List<BoardAdminPostSummary> {
        requireActiveAdmin(actorId)
        val board = requireBoard(boardSlug)
        val boardId = requireBoardId(board)

        return postRepository.findAllByBoardIdOrderByCreatedAtDescIdDesc(boardId).map { post ->
            BoardAdminPostSummary(
                id = post.id,
                boardId = post.boardId,
                title = post.title,
                isPublic = post.isPublic,
                authorId = post.authorId,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
            )
        }
    }

    @Transactional(readOnly = true)
    fun getPost(actorId: Long, boardSlug: String, postId: Long): BoardAdminPostDetail {
        requireActiveAdmin(actorId)
        val board = requireBoard(boardSlug)
        val post = requirePostInBoard(postId, board)
        val assets = postAssetRepository.findAllByPostIdOrderBySortOrderAscIdAsc(postId)

        return BoardAdminPostDetail(
            id = post.id,
            boardId = post.boardId,
            title = post.title,
            contentJson = post.contentJson,
            contentHtml = post.contentHtml,
            isPublic = post.isPublic,
            authorId = post.authorId,
            createdAt = post.createdAt,
            updatedAt = post.updatedAt,
            assets = assets.map { asset ->
                BoardAdminPostAsset(
                    id = asset.id,
                    kind = asset.kind,
                    originalFilename = asset.originalFilename,
                    storedPath = asset.storedPath,
                    mimeType = asset.mimeType,
                    byteSize = asset.byteSize,
                    width = asset.width,
                    height = asset.height,
                    sortOrder = asset.sortOrder,
                )
            },
        )
    }

    @Transactional
    fun createPost(actorId: Long, boardSlug: String, command: BoardPostSaveCommand): BoardAdminPostSaveResult {
        requireActiveAdmin(actorId)
        val board = requireBoard(boardSlug)
        validateContentJson(command.contentJson)

        val assets = resolveAssetsForCreate(
            assetIds = command.assetIds,
            actorId = actorId,
        )

        val saved = postRepository.save(
            Post(
                boardId = requireBoardId(board),
                title = command.title,
                contentJson = command.contentJson,
                contentHtml = command.contentHtml,
                isPublic = command.isPublic,
                authorId = actorId,
            )
        )
        val postId = saved.id ?: throw IllegalStateException("저장된 게시글 id가 없습니다.")

        attachAssets(assets, command.assetIds, postId)

        return BoardAdminPostSaveResult(id = postId)
    }

    @Transactional
    fun updatePost(
        actorId: Long,
        boardSlug: String,
        postId: Long,
        command: BoardPostSaveCommand,
    ): BoardAdminPostSaveResult {
        requireActiveAdmin(actorId)
        val board = requireBoard(boardSlug)
        validateContentJson(command.contentJson)

        val post = requirePostInBoard(postId, board)
        val savedPostId = post.id ?: throw IllegalStateException("게시글 id가 없습니다.")
        val assets = resolveAssetsForUpdate(
            assetIds = command.assetIds,
            actorId = actorId,
            postId = savedPostId,
        )

        post.title = command.title
        post.contentJson = command.contentJson
        post.contentHtml = command.contentHtml
        post.isPublic = command.isPublic
        val saved = postRepository.save(post)
        val resultId = saved.id ?: savedPostId

        attachAssets(assets, command.assetIds, resultId)

        return BoardAdminPostSaveResult(id = resultId)
    }

    @Transactional
    fun deletePost(actorId: Long, boardSlug: String, postId: Long) {
        requireActiveAdmin(actorId)
        val board = requireBoard(boardSlug)
        val post = requirePostInBoard(postId, board)
        postRepository.delete(post)
    }

    private fun requireActiveAdmin(actorId: Long) {
        val actor = adminAccountRepository.findByIdOrNull(actorId)
            ?: throw NotFoundException("관리자 계정을 찾을 수 없습니다. id=$actorId")

        if (!actor.active) {
            throw ForbiddenException("비활성화된 계정은 게시판을 관리할 수 없습니다.")
        }
    }

    private fun requireBoard(slug: String): Board =
        boardRepository.findBySlug(slug)
            ?: throw NotFoundException("게시판을 찾을 수 없습니다. slug=$slug")

    private fun requireBoardId(board: Board): Long =
        board.id ?: throw IllegalStateException("게시판 id가 없습니다.")

    private fun requirePostInBoard(postId: Long, board: Board): Post {
        val post = postRepository.findByIdOrNull(postId)
            ?: throw NotFoundException("게시글을 찾을 수 없습니다. id=$postId")

        if (post.boardId != requireBoardId(board)) {
            throw NotFoundException("게시글을 찾을 수 없습니다. id=$postId")
        }

        return post
    }

    private fun validateContentJson(contentJson: String) {
        try {
            objectMapper.readTree(contentJson)
        } catch (ex: JsonProcessingException) {
            throw IllegalArgumentException("contentJson은 올바른 JSON이어야 합니다.", ex)
        }
    }

    private fun resolveAssetsForCreate(assetIds: List<Long>, actorId: Long): Map<Long, PostAsset> {
        if (assetIds.isEmpty()) {
            return emptyMap()
        }

        return resolveAssets(assetIds).also { assets ->
            assetIds.forEach { assetId ->
                val asset = assets.getValue(assetId)
                if (asset.uploadedByActorId != actorId || asset.postId != null) {
                    throw ForbiddenException("첨부 파일을 게시글에 연결할 수 없습니다. id=$assetId")
                }
            }
        }
    }

    private fun resolveAssetsForUpdate(assetIds: List<Long>, actorId: Long, postId: Long): Map<Long, PostAsset> {
        if (assetIds.isEmpty()) {
            return emptyMap()
        }

        return resolveAssets(assetIds).also { assets ->
            assetIds.forEach { assetId ->
                val asset = assets.getValue(assetId)
                if (asset.uploadedByActorId != actorId || (asset.postId != null && asset.postId != postId)) {
                    throw ForbiddenException("첨부 파일을 게시글에 연결할 수 없습니다. id=$assetId")
                }
            }
        }
    }

    private fun resolveAssets(assetIds: List<Long>): Map<Long, PostAsset> {
        val assets = postAssetRepository.findAllById(assetIds).associateBy { it.id }
        val missingIds = assetIds.filter { it !in assets.keys }
        if (missingIds.isNotEmpty()) {
            throw NotFoundException("첨부 파일을 찾을 수 없습니다. ids=${missingIds.joinToString(",")}")
        }
        return assets.mapKeys { (id, _) -> id ?: throw IllegalStateException("첨부 파일 id가 없습니다.") }
    }

    private fun attachAssets(assetsById: Map<Long, PostAsset>, assetIds: List<Long>, postId: Long) {
        if (assetIds.isEmpty()) {
            return
        }

        val attachedAssets = assetIds.mapIndexed { index, assetId ->
            assetsById.getValue(assetId).apply {
                this.postId = postId
                this.sortOrder = index
            }
        }
        postAssetRepository.saveAll(attachedAssets)
    }
}
