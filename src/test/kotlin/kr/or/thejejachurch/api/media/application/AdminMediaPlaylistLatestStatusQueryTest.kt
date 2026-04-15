package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDetailDto
import kr.or.thejejachurch.api.media.interfaces.dto.AdminPlaylistDto
import org.junit.jupiter.api.Test

class AdminMediaPlaylistLatestStatusQueryTest {

    @Test
    fun `admin playlist list dto exposes latest status summary fields`() {
        val playlistDto = AdminPlaylistDto(
            id = 11L,
            menuName = "주일예배",
            siteKey = "sermons",
            slug = "sermons",
            contentKind = "LONG_FORM",
            status = "PUBLISHED",
            active = true,
            navigationVisible = true,
            sortOrder = 0,
            description = null,
            discoveredAt = null,
            publishedAt = null,
            lastModifiedBy = null,
            youtubePlaylistId = "PL_SERMONS",
            itemCount = 12,
            syncEnabled = true,
            lastSyncedAt = null,
            lastDiscoveredAt = null,
            lastSyncSucceededAt = null,
            lastSyncFailedAt = null,
            lastSyncErrorMessage = null,
            discoverySource = null,
        )

        playlistDto
    }

    @Test
    fun `admin playlist detail dto exposes latest status summary fields`() {
        val playlistDetailDto = AdminPlaylistDetailDto(
            id = 11L,
            menuName = "주일예배",
            siteKey = "sermons",
            slug = "sermons",
            contentKind = "LONG_FORM",
            status = "PUBLISHED",
            active = true,
            navigationVisible = true,
            sortOrder = 0,
            description = null,
            discoveredAt = null,
            publishedAt = null,
            lastModifiedBy = null,
            youtubePlaylistId = "PL_SERMONS",
            youtubeTitle = "주일예배",
            youtubeDescription = "",
            channelTitle = "TDCH",
            thumbnailUrl = "",
            itemCount = 12,
            syncEnabled = true,
            lastSyncedAt = null,
            lastDiscoveredAt = null,
            lastSyncSucceededAt = null,
            lastSyncFailedAt = null,
            lastSyncErrorMessage = null,
            discoverySource = null,
        )

        playlistDetailDto
    }
}
