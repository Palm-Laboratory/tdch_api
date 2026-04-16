package kr.or.thejejachurch.api.sermon.interfaces.dto

import kr.or.thejejachurch.api.sermon.application.AdminSermonDetail
import kr.or.thejejachurch.api.sermon.application.AdminSermonSummary
import kr.or.thejejachurch.api.sermon.application.PublicSermonDetail
import kr.or.thejejachurch.api.sermon.application.PublicSermonList
import kr.or.thejejachurch.api.sermon.application.PublicSermonPlaylistLink
import kr.or.thejejachurch.api.sermon.application.PublicSermonSummary
import kr.or.thejejachurch.api.sermon.application.UpdateSermonMetaCommand
import kr.or.thejejachurch.api.youtube.domain.YouTubeContentForm
import java.time.OffsetDateTime

data class PublicSermonSummaryDto(
    val videoId: String,
    val title: String,
    val preacherName: String?,
    val publishedAt: OffsetDateTime?,
    val thumbnailUrl: String?,
    val scriptureReference: String?,
    val summary: String?,
    val contentForm: YouTubeContentForm,
    val href: String,
)

data class PublicSermonListResponse(
    val form: YouTubeContentForm,
    val featured: PublicSermonSummaryDto?,
    val items: List<PublicSermonSummaryDto>,
)

data class PublicSermonPlaylistLinkDto(
    val label: String,
    val href: String,
)

data class PublicSermonDetailResponse(
    val videoId: String,
    val title: String,
    val sourceTitle: String,
    val preacherName: String?,
    val publishedAt: OffsetDateTime?,
    val thumbnailUrl: String?,
    val scriptureReference: String?,
    val scriptureBody: String?,
    val messageBody: String?,
    val summary: String?,
    val description: String?,
    val contentForm: YouTubeContentForm,
    val playlists: List<PublicSermonPlaylistLinkDto>,
    val related: List<PublicSermonSummaryDto>,
)

data class AdminSermonSummaryDto(
    val videoId: String,
    val title: String,
    val sourceTitle: String,
    val preacherName: String?,
    val publishedAt: OffsetDateTime?,
    val hidden: Boolean,
    val contentForm: YouTubeContentForm,
    val thumbnailUrl: String?,
    val scriptureReference: String?,
)

data class AdminSermonListResponse(
    val items: List<AdminSermonSummaryDto>,
)

data class AdminSermonDetailResponse(
    val videoId: String,
    val sourceTitle: String,
    val sourceDescription: String?,
    val sourcePublishedAt: OffsetDateTime?,
    val sourceThumbnailUrl: String?,
    val title: String,
    val preacherName: String?,
    val publishedAt: OffsetDateTime?,
    val hidden: Boolean,
    val scriptureReference: String?,
    val scriptureBody: String?,
    val messageBody: String?,
    val summary: String?,
    val thumbnailOverrideUrl: String?,
    val contentForm: YouTubeContentForm,
)

data class UpdateSermonMetaRequest(
    val displayTitle: String? = null,
    val preacherName: String? = null,
    val displayPublishedAt: OffsetDateTime? = null,
    val hidden: Boolean = false,
    val scriptureReference: String? = null,
    val scriptureBody: String? = null,
    val messageBody: String? = null,
    val summary: String? = null,
    val thumbnailOverrideUrl: String? = null,
)

fun PublicSermonSummary.toDto(): PublicSermonSummaryDto =
    PublicSermonSummaryDto(
        videoId = videoId,
        title = title,
        preacherName = preacherName,
        publishedAt = publishedAt,
        thumbnailUrl = thumbnailUrl,
        scriptureReference = scriptureReference,
        summary = summary,
        contentForm = contentForm,
        href = href,
    )

fun PublicSermonList.toDto(): PublicSermonListResponse =
    PublicSermonListResponse(
        form = form,
        featured = featured?.toDto(),
        items = items.map { it.toDto() },
    )

fun PublicSermonPlaylistLink.toDto(): PublicSermonPlaylistLinkDto =
    PublicSermonPlaylistLinkDto(
        label = label,
        href = href,
    )

fun PublicSermonDetail.toDto(): PublicSermonDetailResponse =
    PublicSermonDetailResponse(
        videoId = videoId,
        title = title,
        sourceTitle = sourceTitle,
        preacherName = preacherName,
        publishedAt = publishedAt,
        thumbnailUrl = thumbnailUrl,
        scriptureReference = scriptureReference,
        scriptureBody = scriptureBody,
        messageBody = messageBody,
        summary = summary,
        description = description,
        contentForm = contentForm,
        playlists = playlists.map { it.toDto() },
        related = related.map { it.toDto() },
    )

fun AdminSermonSummary.toDto(): AdminSermonSummaryDto =
    AdminSermonSummaryDto(
        videoId = videoId,
        title = title,
        sourceTitle = sourceTitle,
        preacherName = preacherName,
        publishedAt = publishedAt,
        hidden = hidden,
        contentForm = contentForm,
        thumbnailUrl = thumbnailUrl,
        scriptureReference = scriptureReference,
    )

fun AdminSermonDetail.toDto(): AdminSermonDetailResponse =
    AdminSermonDetailResponse(
        videoId = videoId,
        sourceTitle = sourceTitle,
        sourceDescription = sourceDescription,
        sourcePublishedAt = sourcePublishedAt,
        sourceThumbnailUrl = sourceThumbnailUrl,
        title = title,
        preacherName = preacherName,
        publishedAt = publishedAt,
        hidden = hidden,
        scriptureReference = scriptureReference,
        scriptureBody = scriptureBody,
        messageBody = messageBody,
        summary = summary,
        thumbnailOverrideUrl = thumbnailOverrideUrl,
        contentForm = contentForm,
    )

fun UpdateSermonMetaRequest.toCommand(): UpdateSermonMetaCommand =
    UpdateSermonMetaCommand(
        displayTitle = displayTitle,
        preacherName = preacherName,
        displayPublishedAt = displayPublishedAt,
        hidden = hidden,
        scriptureReference = scriptureReference,
        scriptureBody = scriptureBody,
        messageBody = messageBody,
        summary = summary,
        thumbnailOverrideUrl = thumbnailOverrideUrl,
    )
