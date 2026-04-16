package kr.or.thejejachurch.api.media.video.application

import kr.or.thejejachurch.api.youtube.domain.YouTubeContentForm
import java.time.OffsetDateTime

data class PublicMediaVideoSummary(
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

data class PublicMediaVideoList(
    val form: YouTubeContentForm,
    val featured: PublicMediaVideoSummary?,
    val items: List<PublicMediaVideoSummary>,
)

data class PublicMediaVideoPlaylistLink(
    val label: String,
    val href: String,
)

data class PublicMediaVideoDetail(
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
    val playlists: List<PublicMediaVideoPlaylistLink>,
    val related: List<PublicMediaVideoSummary>,
)

data class AdminMediaVideoSummary(
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

data class AdminMediaVideoDetail(
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

data class UpdateMediaVideoMetaCommand(
    val displayTitle: String?,
    val preacherName: String?,
    val displayPublishedAt: OffsetDateTime?,
    val hidden: Boolean,
    val scriptureReference: String?,
    val scriptureBody: String?,
    val messageBody: String?,
    val summary: String?,
    val thumbnailOverrideUrl: String?,
)
