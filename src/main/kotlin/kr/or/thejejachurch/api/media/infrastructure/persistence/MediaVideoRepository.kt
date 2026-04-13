package kr.or.thejejachurch.api.media.infrastructure.persistence

import kr.or.thejejachurch.api.media.domain.MediaVideo
import org.springframework.data.jpa.repository.JpaRepository

interface MediaVideoRepository : JpaRepository<MediaVideo, Long> {
    fun findByProviderVideoId(providerVideoId: String): MediaVideo?
    fun findAllByProviderVideoIdIn(providerVideoIds: Collection<String>): List<MediaVideo>
}
