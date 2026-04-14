package kr.or.thejejachurch.api.media.interfaces.api

import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.media.application.AdminMediaQueryService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AdminMediaSyncHistoryReadEndpointsTest {

    private val adminMediaQueryService: AdminMediaQueryService = mock()
    private val controller = AdminMediaQueryController(
        adminMediaQueryService = adminMediaQueryService,
        adminProperties = AdminProperties(syncKey = "secret-key"),
    )

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `sync job list endpoint should respond with recent sync jobs`() {
        mockMvc.perform(
            get("/api/v1/admin/media/sync-jobs")
                .header("X-Admin-Key", "secret-key")
                .header("X-Admin-Actor-Id", "1"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `sync job detail endpoint should respond with items`() {
        mockMvc.perform(
            get("/api/v1/admin/media/sync-jobs/1")
                .header("X-Admin-Key", "secret-key")
                .header("X-Admin-Actor-Id", "1"),
        )
            .andExpect(status().isOk)
    }
}
