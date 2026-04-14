package kr.or.thejejachurch.api.media.infrastructure.persistence

import kr.or.thejejachurch.api.media.domain.VideoMetadata
import org.springframework.data.jpa.repository.JpaRepository

interface VideoMetadataRepository : JpaRepository<VideoMetadata, Long> {
    fun findByYoutubeVideoId(youtubeVideoId: Long): VideoMetadata?
    fun findAllByYoutubeVideoIdIn(youtubeVideoIds: Collection<Long>): List<VideoMetadata>
    fun findAllByFeaturedTrueOrderByPinnedRankAscUpdatedAtDesc(): List<VideoMetadata>
}
