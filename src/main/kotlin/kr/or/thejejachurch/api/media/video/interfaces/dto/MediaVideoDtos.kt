package kr.or.thejejachurch.api.media.video.interfaces.dto

import kr.or.thejejachurch.api.media.video.application.AdminMediaVideoDetail
import kr.or.thejejachurch.api.media.video.application.AdminMediaVideoSummary
import kr.or.thejejachurch.api.media.video.application.PublicMediaVideoDetail
import kr.or.thejejachurch.api.media.video.application.PublicMediaVideoList
import kr.or.thejejachurch.api.media.video.application.PublicMediaVideoPlaylistLink
import kr.or.thejejachurch.api.media.video.application.PublicMediaVideoSummary
import kr.or.thejejachurch.api.media.video.application.UpdateMediaVideoMetaCommand
import kr.or.thejejachurch.api.youtube.domain.YouTubeContentForm
import java.time.OffsetDateTime

data class PublicMediaVideoSummaryDto(
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

data class PublicMediaVideoListResponse(
    val form: YouTubeContentForm,
    val featured: PublicMediaVideoSummaryDto?,
    val items: List<PublicMediaVideoSummaryDto>,
)

data class PublicMediaVideoPlaylistLinkDto(
    val label: String,
    val href: String,
)

data class PublicMediaVideoDetailResponse(
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
    val playlists: List<PublicMediaVideoPlaylistLinkDto>,
    val related: List<PublicMediaVideoSummaryDto>,
)

data class AdminMediaVideoSummaryDto(
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

data class AdminMediaVideoListResponse(
    val items: List<AdminMediaVideoSummaryDto>,
)

data class AdminMediaVideoDetailResponse(
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

data class UpdateMediaVideoMetaRequest(
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

fun PublicMediaVideoSummary.toDto(): PublicMediaVideoSummaryDto =
    PublicMediaVideoSummaryDto(
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

fun PublicMediaVideoList.toDto(): PublicMediaVideoListResponse =
    PublicMediaVideoListResponse(
        form = form,
        featured = featured?.toDto(),
        items = items.map { it.toDto() },
    )

fun PublicMediaVideoPlaylistLink.toDto(): PublicMediaVideoPlaylistLinkDto =
    PublicMediaVideoPlaylistLinkDto(
        label = label,
        href = href,
    )

fun PublicMediaVideoDetail.toDto(): PublicMediaVideoDetailResponse =
    PublicMediaVideoDetailResponse(
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

fun AdminMediaVideoSummary.toDto(): AdminMediaVideoSummaryDto =
    AdminMediaVideoSummaryDto(
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

fun AdminMediaVideoDetail.toDto(): AdminMediaVideoDetailResponse =
    AdminMediaVideoDetailResponse(
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

fun UpdateMediaVideoMetaRequest.toCommand(): UpdateMediaVideoMetaCommand =
    UpdateMediaVideoMetaCommand(
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
