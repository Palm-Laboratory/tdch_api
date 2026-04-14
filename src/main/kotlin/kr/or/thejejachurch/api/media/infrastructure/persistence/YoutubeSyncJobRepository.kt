package kr.or.thejejachurch.api.media.infrastructure.persistence

import kr.or.thejejachurch.api.media.domain.YoutubeSyncJob
import org.springframework.data.jpa.repository.JpaRepository

interface YoutubeSyncJobRepository : JpaRepository<YoutubeSyncJob, Long> {
    fun findTop20ByOrderByStartedAtDesc(): List<YoutubeSyncJob>
}
