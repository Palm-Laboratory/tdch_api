package kr.or.thejejachurch.api.media.infrastructure.persistence

import kr.or.thejejachurch.api.media.domain.YoutubeSyncJobItem
import org.springframework.data.jpa.repository.JpaRepository

interface YoutubeSyncJobItemRepository : JpaRepository<YoutubeSyncJobItem, Long> {
    fun findAllByJobIdOrderByIdAsc(jobId: Long): List<YoutubeSyncJobItem>
}
