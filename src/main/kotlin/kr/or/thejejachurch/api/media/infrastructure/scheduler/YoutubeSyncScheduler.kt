package kr.or.thejejachurch.api.media.infrastructure.scheduler

import kr.or.thejejachurch.api.media.application.YoutubeSyncService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class YoutubeSyncScheduler(
    private val youtubeSyncService: YoutubeSyncService,
) {

    // 운영 정책에 맞춰 YouTube sync는 매일 06:00, 23:00에 실행한다.
    @Scheduled(cron = "0 0 6,23 * * *")
    fun sync() {
        youtubeSyncService.syncAllMenus()
    }
}
