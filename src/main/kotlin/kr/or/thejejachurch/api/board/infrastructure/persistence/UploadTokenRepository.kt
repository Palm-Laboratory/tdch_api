package kr.or.thejejachurch.api.board.infrastructure.persistence

import kr.or.thejejachurch.api.board.domain.UploadToken
import org.springframework.data.jpa.repository.JpaRepository

interface UploadTokenRepository : JpaRepository<UploadToken, Long> {
    fun findByTokenHash(tokenHash: String): UploadToken?

    fun deleteByExpiresAtBefore(cutoff: java.time.OffsetDateTime): Long
}
