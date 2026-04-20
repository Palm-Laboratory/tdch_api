package kr.or.thejejachurch.api.board.interfaces.api

import kr.or.thejejachurch.api.board.application.UploadTokenIssueResult
import kr.or.thejejachurch.api.board.application.UploadTokenService
import kr.or.thejejachurch.api.board.domain.PostAssetKind
import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.common.error.ForbiddenException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.web.bind.annotation.RequestHeader

class UploadAdminControllerTest {

    private val uploadTokenService: UploadTokenService = mock()

    @Test
    fun `issue token delegates to upload token service and returns raw token value when admin key matches`() {
        val controller = UploadAdminController(
            uploadTokenService = uploadTokenService,
            adminProperties = AdminProperties(syncKey = "secret-key"),
        )
        whenever(
            uploadTokenService.issueToken(
                actorId = eq(42L),
                kind = eq(PostAssetKind.FILE_ATTACHMENT),
                maxByteSize = eq(1_234_567L),
                allowedMimeTypes = eq(listOf("application/pdf", "image/png")),
            )
        ).thenReturn(
            UploadTokenIssueResult(rawToken = "issued-upload-token"),
        )

        val response = controller.issueToken(
            adminKey = "secret-key",
            actorId = 42L,
            request = UploadTokenIssueRequest(
                kind = PostAssetKind.FILE_ATTACHMENT,
                maxByteSize = 1_234_567L,
                allowedMimeTypes = listOf("application/pdf", "image/png"),
            ),
        )

        assertThat(readTokenValue(response)).isEqualTo("issued-upload-token")
        verify(uploadTokenService).issueToken(
            actorId = 42L,
            kind = PostAssetKind.FILE_ATTACHMENT,
            maxByteSize = 1_234_567L,
            allowedMimeTypes = listOf("application/pdf", "image/png"),
        )
    }

    @Test
    fun `issue token throws forbidden when admin key is wrong and does not call service`() {
        val controller = UploadAdminController(
            uploadTokenService = uploadTokenService,
            adminProperties = AdminProperties(syncKey = "secret-key"),
        )

        assertThrows<ForbiddenException> {
            controller.issueToken(
                adminKey = "wrong-key",
                actorId = 42L,
                request = UploadTokenIssueRequest(
                    kind = PostAssetKind.FILE_ATTACHMENT,
                    maxByteSize = 1_234_567L,
                    allowedMimeTypes = listOf("application/pdf", "image/png"),
                ),
            )
        }

        verifyNoInteractions(uploadTokenService)
    }

    @Test
    fun `issue token throws forbidden when admin key is missing and does not call service`() {
        val controller = UploadAdminController(
            uploadTokenService = uploadTokenService,
            adminProperties = AdminProperties(syncKey = "secret-key"),
        )

        assertThrows<ForbiddenException> {
            controller.issueToken(
                adminKey = null,
                actorId = 42L,
                request = UploadTokenIssueRequest(
                    kind = PostAssetKind.FILE_ATTACHMENT,
                    maxByteSize = 1_234_567L,
                    allowedMimeTypes = listOf("application/pdf", "image/png"),
                ),
            )
        }

        verifyNoInteractions(uploadTokenService)
    }

    @Test
    fun `issue token throws illegal state when admin sync key is blank`() {
        val controller = UploadAdminController(
            uploadTokenService = uploadTokenService,
            adminProperties = AdminProperties(syncKey = "   "),
        )

        assertThrows<IllegalStateException> {
            controller.issueToken(
                adminKey = "secret-key",
                actorId = 42L,
                request = UploadTokenIssueRequest(
                    kind = PostAssetKind.FILE_ATTACHMENT,
                    maxByteSize = 1_234_567L,
                    allowedMimeTypes = listOf("application/pdf", "image/png"),
                ),
            )
        }

        verifyNoInteractions(uploadTokenService)
    }

    @Test
    fun `issue token endpoint is admin-key only and does not expose X Upload Token header`() {
        val hasUploadTokenHeader = UploadAdminController::class.java.declaredMethods
            .flatMap { method -> method.parameters.asList() }
            .any { parameter ->
                parameter.annotations.filterIsInstance<RequestHeader>()
                    .any { requestHeader -> requestHeader.value == "X-Upload-Token" }
            }

        assertThat(hasUploadTokenHeader).isFalse()
    }

    private fun readTokenValue(response: Any): String {
        val tokenField = response.javaClass.declaredFields.firstOrNull {
            it.name == "token" || it.name == "rawToken"
        } ?: error("Expected token or rawToken field on response")

        tokenField.isAccessible = true
        return tokenField.get(response) as String
    }
}
