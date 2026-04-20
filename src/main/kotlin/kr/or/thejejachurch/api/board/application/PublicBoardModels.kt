package kr.or.thejejachurch.api.board.application

import kr.or.thejejachurch.api.board.domain.PostAssetKind
import java.time.OffsetDateTime

data class PublicBoardPostListResult(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val hasNext: Boolean,
    val posts: List<PublicBoardPostSummary>,
)

data class PublicBoardPostSummary(
    val id: Long?,
    val boardId: Long,
    val menuId: Long = boardId,
    val title: String,
    val contentHtml: String?,
    val isPinned: Boolean,
    val publishedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class PublicBoardPostDetail(
    val id: Long?,
    val boardId: Long,
    val menuId: Long = boardId,
    val title: String,
    val contentJson: String,
    val contentHtml: String?,
    val isPinned: Boolean,
    val publishedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val assets: List<PublicBoardPostAsset>,
)

data class PublicBoardPostAsset(
    val id: Long?,
    val kind: PostAssetKind,
    val originalFilename: String,
    val storedPath: String,
    val publicUrl: String,
    val mimeType: String?,
    val byteSize: Long,
    val width: Int?,
    val height: Int?,
    val sortOrder: Int,
)
