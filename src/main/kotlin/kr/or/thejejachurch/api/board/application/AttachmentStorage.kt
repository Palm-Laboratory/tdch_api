package kr.or.thejejachurch.api.board.application

import kr.or.thejejachurch.api.board.domain.PostAssetKind
import org.springframework.web.multipart.MultipartFile

interface AttachmentStorage {
    fun store(
        file: MultipartFile,
        kind: PostAssetKind,
        maxByteSize: Long,
    ): StoredAttachment

    fun delete(storedPath: String)
}

data class StoredAttachment(
    val storedPath: String,
    val mimeType: String,
    val byteSize: Long,
    val width: Int?,
    val height: Int?,
)
