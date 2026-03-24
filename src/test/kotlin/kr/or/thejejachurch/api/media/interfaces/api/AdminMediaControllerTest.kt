package kr.or.thejejachurch.api.media.interfaces.api

import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.common.error.ForbiddenException
import kr.or.thejejachurch.api.media.application.YoutubeSyncService
import kr.or.thejejachurch.api.media.application.YoutubeSyncSummary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AdminMediaControllerTest {

    private val youtubeSyncService: YoutubeSyncService = mock()

    @Test
    fun `sync runs when admin key matches`() {
        val controller = AdminMediaController(
            youtubeSyncService = youtubeSyncService,
            adminProperties = AdminProperties(syncKey = "secret-key"),
        )
        whenever(youtubeSyncService.syncAllMenus()).thenReturn(
            YoutubeSyncSummary(
                totalPlaylists = 3,
                succeededPlaylists = 3,
                failedPlaylists = 0,
            ),
        )

        val response = controller.sync("secret-key")

        verify(youtubeSyncService, times(1)).syncAllMenus()
        assertThat(response.status).isEqualTo("ok")
        assertThat(response.totalPlaylists).isEqualTo(3)
        assertThat(response.failedPlaylists).isZero()
    }

    @Test
    fun `sync throws forbidden when admin key mismatches`() {
        val controller = AdminMediaController(
            youtubeSyncService = youtubeSyncService,
            adminProperties = AdminProperties(syncKey = "secret-key"),
        )

        assertThrows<ForbiddenException> {
            controller.sync("wrong-key")
        }
    }

    @Test
    fun `sync throws when admin key is not configured`() {
        val controller = AdminMediaController(
            youtubeSyncService = youtubeSyncService,
            adminProperties = AdminProperties(syncKey = ""),
        )

        val exception = assertThrows<IllegalStateException> {
            controller.sync("any-key")
        }

        assertThat(exception.message).isEqualTo("ADMIN_SYNC_KEY is not configured.")
    }
}
