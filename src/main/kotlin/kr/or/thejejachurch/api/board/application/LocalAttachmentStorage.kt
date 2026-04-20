package kr.or.thejejachurch.api.board.application

import kr.or.thejejachurch.api.board.domain.PostAssetKind
import kr.or.thejejachurch.api.common.config.UploadProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.UUID
import javax.imageio.ImageIO

@Component
class LocalAttachmentStorage(
    private val rootPath: Path,
) : AttachmentStorage {

    @Autowired
    constructor(uploadProperties: UploadProperties) : this(Path.of(uploadProperties.rootPath))

    override fun store(
        file: MultipartFile,
        kind: PostAssetKind,
        maxByteSize: Long,
    ): StoredAttachment {
        val byteSize = file.size
        if (byteSize > maxByteSize) {
            throw IllegalArgumentException("첨부파일이 허용 크기를 초과합니다.")
        }

        val bytes = file.bytes
        val mimeType = detectMimeType(bytes)
        require(mimeType == file.contentType) { "첨부파일 MIME 타입이 일치하지 않습니다." }

        val extension = extensionForMimeType(mimeType)
        val dimensions = imageDimensions(bytes, mimeType)
        val storedPath = buildStoredPath(extension)
        val target = rootPath.resolve(storedPath)

        Files.createDirectories(target.parent)
        // TODO: Re-encode images to strip EXIF and resize to the final max-dimension policy.
        Files.write(target, bytes)

        return StoredAttachment(
            storedPath = storedPath,
            mimeType = mimeType,
            byteSize = byteSize,
            width = dimensions.first,
            height = dimensions.second,
        )
    }

    override fun delete(storedPath: String) {
        Files.deleteIfExists(rootPath.resolve(storedPath))
    }

    private fun buildStoredPath(extension: String): String {
        val now = LocalDate.now()
        return "%04d/%02d/%s.%s".format(
            now.year,
            now.monthValue,
            UUID.randomUUID().toString(),
            extension,
        )
    }

    private fun detectMimeType(bytes: ByteArray): String =
        when {
            isPng(bytes) -> "image/png"
            isJpeg(bytes) -> "image/jpeg"
            isWebp(bytes) -> "image/webp"
            isPdf(bytes) -> "application/pdf"
            else -> throw IllegalArgumentException("지원하지 않는 첨부파일 형식입니다.")
        }

    private fun extensionForMimeType(mimeType: String): String =
        when (mimeType) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            "image/webp" -> "webp"
            "application/pdf" -> "pdf"
            else -> throw IllegalArgumentException("지원하지 않는 첨부파일 형식입니다.")
        }

    private fun imageDimensions(bytes: ByteArray, mimeType: String): Pair<Int?, Int?> =
        when (mimeType) {
            "image/png", "image/jpeg" -> {
                val image = ImageIO.read(ByteArrayInputStream(bytes))
                    ?: throw IllegalArgumentException("이미지 첨부파일을 읽을 수 없습니다.")
                image.width to image.height
            }
            "image/webp" -> webpDimensions(bytes)
            else -> null to null
        }

    private fun webpDimensions(bytes: ByteArray): Pair<Int?, Int?> {
        if (bytes.size < 30) {
            return null to null
        }

        val header = String(bytes, 0, 4, Charsets.US_ASCII)
        val format = String(bytes, 8, 4, Charsets.US_ASCII)
        if (header != "RIFF" || format != "WEBP") {
            throw IllegalArgumentException("지원하지 않는 첨부파일 형식입니다.")
        }

        return null to null
    }

    private fun isPng(bytes: ByteArray): Boolean =
        bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() &&
            bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() &&
            bytes[7] == 0x0A.toByte()

    private fun isJpeg(bytes: ByteArray): Boolean =
        bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()

    private fun isWebp(bytes: ByteArray): Boolean =
        bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() &&
            bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() &&
            bytes[11] == 'P'.code.toByte()

    private fun isPdf(bytes: ByteArray): Boolean =
        bytes.size >= 5 &&
            bytes[0] == '%'.code.toByte() &&
            bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'D'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[4] == '-'.code.toByte()
}
