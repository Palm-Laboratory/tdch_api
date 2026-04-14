package kr.or.thejejachurch.api.media.infrastructure.youtube

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kr.or.thejejachurch.api.common.config.YoutubeProperties
import kr.or.thejejachurch.api.media.domain.ContentKind
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.OffsetDateTime

@Component
class YoutubeApiClient(
    private val youtubeProperties: YoutubeProperties,
    private val objectMapper: ObjectMapper,
) : YoutubeApiOperations {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build()

    override fun getChannelPlaylists(
        channelId: String,
        pageToken: String?,
        maxResults: Int,
    ): YoutubeChannelPlaylistsPage {
        require(channelId.isNotBlank()) { "channelId must not be blank." }
        require(maxResults in 1..MAX_RESULTS_PER_PAGE) { "maxResults must be between 1 and $MAX_RESULTS_PER_PAGE." }

        val responseBody = executeGet(
            UriComponentsBuilder.fromUriString(YOUTUBE_DATA_API_BASE_URL)
                .path("/playlists")
                .queryParam("part", "snippet,contentDetails,status")
                .queryParam("channelId", channelId)
                .queryParam("maxResults", maxResults)
                .apply {
                    if (!pageToken.isNullOrBlank()) {
                        queryParam("pageToken", pageToken)
                    }
                }
                .build(true)
                .toUri(),
        )

        val root = parseJson(responseBody)
        val items = root.path("items").mapNotNull(::toChannelPlaylistResource)

        return YoutubeChannelPlaylistsPage(
            nextPageToken = root.path("nextPageToken").asText(null)?.takeIf { it.isNotBlank() },
            items = items,
        )
    }

    override fun getPlaylistItems(
        playlistId: String,
        pageToken: String?,
        maxResults: Int,
    ): YoutubePlaylistItemsPage {
        require(playlistId.isNotBlank()) { "playlistId must not be blank." }
        require(maxResults in 1..MAX_RESULTS_PER_PAGE) { "maxResults must be between 1 and $MAX_RESULTS_PER_PAGE." }

        val responseBody = executeGet(
            UriComponentsBuilder.fromUriString(YOUTUBE_DATA_API_BASE_URL)
                .path("/playlistItems")
                .queryParam("part", "snippet,contentDetails,status")
                .queryParam("playlistId", playlistId)
                .queryParam("maxResults", maxResults)
                .apply {
                    if (!pageToken.isNullOrBlank()) {
                        queryParam("pageToken", pageToken)
                    }
                }
                .build(true)
                .toUri(),
        )

        val root = parseJson(responseBody)
        val items = root.path("items").mapNotNull(::toPlaylistItem)

        return YoutubePlaylistItemsPage(
            nextPageToken = root.path("nextPageToken").asText(null)?.takeIf { it.isNotBlank() },
            items = items,
        )
    }

    override fun getVideos(videoIds: Collection<String>): List<YoutubeVideoResource> {
        val normalizedIds = videoIds.map(String::trim).filter(String::isNotBlank).distinct()
        if (normalizedIds.isEmpty()) {
            return emptyList()
        }

        return normalizedIds.chunked(MAX_VIDEO_IDS_PER_REQUEST).flatMap(::getVideosChunk)
    }

    private fun getVideosChunk(videoIds: List<String>): List<YoutubeVideoResource> {
        val responseBody = executeGet(
            UriComponentsBuilder.fromUriString(YOUTUBE_DATA_API_BASE_URL)
                .path("/videos")
                .queryParam("part", "snippet,contentDetails,status")
                .queryParam("id", videoIds.joinToString(","))
                .queryParam("maxResults", videoIds.size)
                .build(true)
                .toUri(),
        )

        val root = parseJson(responseBody)
        return root.path("items").mapNotNull(::toYoutubeVideoResource)
    }

    private fun executeGet(uri: URI): String {
        val apiKey = youtubeProperties.apiKey.takeIf(String::isNotBlank)
            ?: throw YoutubeApiClientException("YOUTUBE_API_KEY is missing.")

        val request = HttpRequest.newBuilder(
            UriComponentsBuilder.fromUri(uri)
                .queryParam("key", apiKey)
                .build(true)
                .toUri(),
        )
            .GET()
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", "application/json")
            .build()

        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (ex: Exception) {
            throw YoutubeApiClientException("Failed to call YouTube API: ${ex.message}")
        }

        if (response.statusCode() !in 200..299) {
            throw YoutubeApiClientException(
                "YouTube API request failed with status=${response.statusCode()}: ${extractErrorMessage(response.body())}",
            )
        }

        return response.body()
    }

    private fun toPlaylistItem(node: JsonNode): YoutubePlaylistItem? {
        val videoId = node.path("contentDetails").path("videoId").asText("")
            .ifBlank { node.path("snippet").path("resourceId").path("videoId").asText("") }
            .trim()
        if (videoId.isBlank()) {
            return null
        }

        return YoutubePlaylistItem(
            videoId = videoId,
            position = node.path("snippet").path("position").asInt(0),
            addedToPlaylistAt = node.path("snippet").path("publishedAt").asOffsetDateTime(),
            videoPublishedAt = node.path("contentDetails").path("videoPublishedAt").asOffsetDateTime(),
        )
    }

    private fun toChannelPlaylistResource(node: JsonNode): YoutubeChannelPlaylistResource? {
        val playlistId = node.path("id").asText("").trim()
        if (playlistId.isBlank()) {
            return null
        }

        val snippet = node.path("snippet")
        val contentDetails = node.path("contentDetails")

        return YoutubeChannelPlaylistResource(
            youtubePlaylistId = playlistId,
            title = snippet.path("title").asText("").trim(),
            description = snippet.path("description").asNullableText(),
            channelId = snippet.path("channelId").asNullableText(),
            channelTitle = snippet.path("channelTitle").asNullableText(),
            thumbnailUrl = extractThumbnailUrl(snippet.path("thumbnails")),
            itemCount = contentDetails.path("itemCount").takeIf(JsonNode::canConvertToInt)?.asInt(),
        )
    }

    private fun toYoutubeVideoResource(node: JsonNode): YoutubeVideoResource? {
        val videoId = node.path("id").asText("").trim()
        if (videoId.isBlank()) {
            return null
        }

        val snippet = node.path("snippet")
        val status = node.path("status")
        val durationSeconds = parseDurationSeconds(node.path("contentDetails").path("duration").asText(null))

        return YoutubeVideoResource(
            videoId = videoId,
            title = snippet.path("title").asText(""),
            description = snippet.path("description").asNullableText(),
            publishedAt = snippet.path("publishedAt").asOffsetDateTime()
                ?: throw YoutubeApiClientException("publishedAt is missing for videoId=$videoId"),
            channelId = snippet.path("channelId").asNullableText(),
            channelTitle = snippet.path("channelTitle").asNullableText(),
            thumbnailUrl = extractThumbnailUrl(snippet.path("thumbnails")),
            durationSeconds = durationSeconds,
            privacyStatus = status.path("privacyStatus").asNullableText(),
            uploadStatus = status.path("uploadStatus").asNullableText(),
            embeddable = status.path("embeddable").asBoolean(true),
            madeForKids = status.path("madeForKids").takeIf(JsonNode::isBoolean)?.asBoolean(),
            detectedKind = detectContentKind(durationSeconds),
            youtubeWatchUrl = "https://www.youtube.com/watch?v=$videoId",
            youtubeEmbedUrl = "https://www.youtube.com/embed/$videoId",
            rawPayload = objectMapper.writeValueAsString(node),
        )
    }

    private fun detectContentKind(durationSeconds: Int?): ContentKind =
        if (durationSeconds != null && durationSeconds <= SHORTS_MAX_DURATION_SECONDS) {
            ContentKind.SHORT
        } else {
            ContentKind.LONG_FORM
        }

    private fun extractThumbnailUrl(thumbnails: JsonNode): String? {
        THUMBNAIL_PRIORITY_KEYS.forEach { key ->
            val url = thumbnails.path(key).path("url").asNullableText()
            if (!url.isNullOrBlank()) {
                return url
            }
        }
        return null
    }

    private fun parseJson(body: String): JsonNode = try {
        objectMapper.readTree(body)
    } catch (ex: Exception) {
        throw YoutubeApiClientException("Failed to parse YouTube API response: ${ex.message}")
    }

    private fun extractErrorMessage(body: String): String {
        val root = runCatching { objectMapper.readTree(body) }.getOrNull() ?: return body
        return root.path("error").path("message").asText(body).ifBlank { body }
    }

    private fun parseDurationSeconds(durationText: String?): Int? = try {
        durationText?.takeIf { it.isNotBlank() }?.let { Duration.parse(it).seconds.toInt() }
    } catch (_: Exception) {
        null
    }

    private fun JsonNode.asOffsetDateTime(): OffsetDateTime? =
        asNullableText()?.let(OffsetDateTime::parse)

    private fun JsonNode.asNullableText(): String? =
        asText(null)?.takeIf { it.isNotBlank() && it != "null" }

    companion object {
        private const val YOUTUBE_DATA_API_BASE_URL = "https://www.googleapis.com/youtube/v3"
        private const val MAX_RESULTS_PER_PAGE = 50
        private const val MAX_VIDEO_IDS_PER_REQUEST = 50
        private const val SHORTS_MAX_DURATION_SECONDS = 60
        private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(5)
        private val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(15)
        private val THUMBNAIL_PRIORITY_KEYS = listOf("maxres", "standard", "high", "medium", "default")
    }
}
